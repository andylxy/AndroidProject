package run.yigou.gxzy.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import run.yigou.gxzy.data.remote.model.Announcement;

/**
 * 公告「已读判定」与「本次该弹哪几条」的单元测试（DESIGN §6.2/§6.4）。
 *
 * <p>纯 JVM：被测的 {@link AnnouncementStore#isSeenVersion(Integer, int)} 与
 * {@link AnnouncementQueue#select(List, int, AnnouncementQueue.SeenFilter)} 刻意不碰
 * MMKV / Android（已读判定由 {@code SeenFilter} 注入），所以调用它们不会触发 native
 * 库加载 —— 这条是既有教训：把纯逻辑放进持有 Android 依赖的类里，单测直接
 * {@code ExceptionInInitializerError}。</p>
 *
 * <p>用 Gson 从 JSON 构造 {@link Announcement}（它没有 setter，是只读响应模型），
 * 顺带验证字段名映射 —— 该模型刻意不加 {@code @SerializedName}，字段名一旦与后端
 * JSON 不符就会静默变成 0，而 {@code version=0} 正好会让「改过内容就重弹」失效。</p>
 */
public class AnnouncementQueueTest {

    private static final Gson GSON = new Gson();

    private static Announcement item(int id, int priority, int version) {
        return GSON.fromJson(
                "{\"id\":" + id + ",\"title\":\"公告" + id + "\",\"body\":\"正文\","
                        + "\"priority\":" + priority + ",\"version\":" + version
                        + ",\"validFrom\":null,\"validTo\":null}",
                Announcement.class);
    }

    /** 已读表：id → seenVersion。 */
    private static AnnouncementQueue.SeenFilter seen(final Map<Integer, Integer> table) {
        return new AnnouncementQueue.SeenFilter() {
            @Override
            public boolean isSeen(int id, int version) {
                return AnnouncementStore.isSeenVersion(table.get(id), version);
            }
        };
    }

    private static AnnouncementQueue.SeenFilter noneSeen() {
        return seen(new HashMap<Integer, Integer>());
    }

    private static List<Integer> ids(List<Announcement> list) {
        List<Integer> result = new ArrayList<>();
        for (Announcement announcement : list) {
            result.add(announcement.getId());
        }
        return result;
    }

    @Test
    public void gsonMapsFieldsAsExpected() {
        // 先钉住映射本身：若这条挂了，下面所有断言都失去意义（version 会静默变 0）。
        Announcement announcement = item(7, 3, 2);
        assertEquals(7, announcement.getId());
        assertEquals("公告7", announcement.getTitle());
        assertEquals("正文", announcement.getBody());
        assertEquals(3, announcement.getPriority());
        assertEquals(2, announcement.getVersion());
        assertTrue(announcement.getValidFrom() == null);
        assertTrue(announcement.getValidTo() == null);
        assertTrue(announcement.hasTitle());
    }

    // ---------- 已读判定 ----------

    @Test
    public void neverSeenIsNotSeen() {
        assertFalse(AnnouncementStore.isSeenVersion(null, 1));
    }

    @Test
    public void sameVersionCountsAsSeen() {
        assertTrue(AnnouncementStore.isSeenVersion(1, 1));
    }

    @Test
    public void higherSeenVersionStillCountsAsSeen() {
        // 见过 v3 而后端仍下发 v2（后端回滚版本号）不该重弹。
        assertTrue(AnnouncementStore.isSeenVersion(3, 2));
    }

    @Test
    public void lowerSeenVersionMeansContentChangedSoNotSeen() {
        // 需求 D2：后端改了标题/正文 → version+1 → 必须重弹一次。
        assertFalse(AnnouncementStore.isSeenVersion(1, 2));
    }

    // ---------- 挑选 ----------

    @Test
    public void emptyAndNullInputsYieldEmptyQueue() {
        // 后端返回 {"announcements":[]} 时（INV-2）不得弹任何东西。
        assertTrue(AnnouncementQueue.select(null, 3, noneSeen()).isEmpty());
        assertTrue(AnnouncementQueue.select(Collections.<Announcement>emptyList(), 3, noneSeen())
                .isEmpty());
    }

    @Test
    public void ordersByPriorityDesc() {
        List<Announcement> input = Arrays.asList(
                item(1, 0, 1), item(2, 10, 1), item(3, 5, 1));
        assertEquals(Arrays.asList(2, 3, 1), ids(AnnouncementQueue.select(input, 3, noneSeen())));
    }

    @Test
    public void breaksPriorityTiesByIdAscForStableOrder() {
        // 不指定平手次序的话排序结果依赖输入顺序，同一份数据两次弹出顺序可能不同。
        List<Announcement> input = Arrays.asList(
                item(9, 5, 1), item(3, 5, 1), item(6, 5, 1));
        assertEquals(Arrays.asList(3, 6, 9), ids(AnnouncementQueue.select(input, 3, noneSeen())));
    }

    @Test
    public void skipsAlreadySeen() {
        Map<Integer, Integer> table = new HashMap<>();
        table.put(1, 1);
        table.put(2, 1);
        List<Announcement> input = Arrays.asList(
                item(1, 10, 1), item(2, 5, 1), item(3, 1, 1));
        assertEquals(Collections.singletonList(3), ids(AnnouncementQueue.select(input, 3, seen(table))));
    }

    @Test
    public void reShowsSeenItemWhoseContentChanged() {
        // 已关闭过 v1，后端把内容改成 v2 → 必须再弹一次（需求 D2 的核心）。
        Map<Integer, Integer> table = new HashMap<>();
        table.put(1, 1);
        List<Announcement> input = Collections.singletonList(item(1, 0, 2));
        assertEquals(Collections.singletonList(1), ids(AnnouncementQueue.select(input, 3, seen(table))));
    }

    @Test
    public void capsAtMaxPerLaunch() {
        // 「太多公告」的处置：一次启动最多弹 N 条，其余不丢、下次补弹（DESIGN §6.4）。
        List<Announcement> input = Arrays.asList(
                item(1, 5, 1), item(2, 4, 1), item(3, 3, 1), item(4, 2, 1), item(5, 1, 1));
        List<Announcement> queue = AnnouncementQueue.select(input, 3, noneSeen());
        assertEquals(3, queue.size());
        // 取的是优先级最高的三条，剩下的仍在后端、未读，下次启动继续。
        assertEquals(Arrays.asList(1, 2, 3), ids(queue));
    }

    @Test
    public void treatsNonPositiveMaxAsOne() {
        // 上限传 0/负数时宁可只弹一条，也不要「不弹」或「全弹」。
        List<Announcement> input = Arrays.asList(item(1, 1, 1), item(2, 2, 1));
        assertEquals(1, AnnouncementQueue.select(input, 0, noneSeen()).size());
        assertEquals(1, AnnouncementQueue.select(input, -5, noneSeen()).size());
    }

    @Test
    public void dropsEntriesWithoutTitleAndNulls() {
        // 脏数据不得弹成空框标题。
        Announcement noTitle = GSON.fromJson("{\"id\":9,\"title\":\"  \",\"version\":1}",
                Announcement.class);
        List<Announcement> input = Arrays.asList(noTitle, null, item(1, 0, 1));
        assertEquals(Collections.singletonList(1), ids(AnnouncementQueue.select(input, 3, noneSeen())));
    }

    @Test
    public void maxPopPerLaunchIsThree() {
        // D1 已锁定 3；改动它会改变打扰频次，必须显式改测试与设计文档。
        assertEquals(3, AnnouncementQueue.MAX_POP_PER_LAUNCH);
    }
}
