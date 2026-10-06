package run.yigou.gxzy.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import run.yigou.gxzy.data.remote.model.Announcement;

/**
 * 公告挑选与排序的单元测试。
 *
 * <p>纯 JVM：被测的 {@code AnnouncementQueue.select} 刻意不碰 MMKV / Android / 网络。</p>
 *
 * <p>需求是「<b>每次 App 启动都弹出全部有效公告</b>」，所以这里锁的是：
 * 不做已读过滤（关掉过也照弹）、不设条数上限（几条就弹几条）、
 * 只剔除无效的（无标题、已过期）、按优先级排序。</p>
 */
public class AnnouncementQueueTest {

    private static final Gson GSON = new Gson();
    private static final long NOW = 1_000_000L;

    /** 造一条公告；validTo 为 null 表示无结束时间（永不过期）。 */
    private static Announcement item(int id, int priority, int version, Long validTo) {
        String to = validTo == null ? "null" : String.valueOf(validTo);
        return GSON.fromJson(
                "{\"id\":" + id + ",\"title\":\"公告" + id + "\",\"body\":\"正文\","
                        + "\"priority\":" + priority + ",\"version\":" + version
                        + ",\"validFrom\":null,\"validTo\":" + to + "}",
                Announcement.class);
    }

    private static Announcement item(int id, int priority, int version) {
        return item(id, priority, version, null);
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
        // 字段名映射一旦不符，version/priority 会静默变 0，排序就全乱了。
        Announcement announcement = item(7, 3, 2);
        assertEquals(7, announcement.getId());
        assertEquals("公告7", announcement.getTitle());
        assertEquals("正文", announcement.getBody());
        assertEquals(3, announcement.getPriority());
        assertEquals(2, announcement.getVersion());
        assertTrue(announcement.getValidTo() == null);
        assertTrue(announcement.hasTitle());
    }

    // ---------- 全选、无上限 ----------

    @Test
    public void returnsAllValidAnnouncements() {
        // 需求：几条有效就弹几条，不设上限。
        List<Announcement> input = Arrays.asList(
                item(1, 0, 1), item(2, 1, 1), item(3, 2, 1), item(4, 3, 1));
        assertEquals(4, AnnouncementQueue.select(input, NOW).size());
    }

    @Test
    public void doesNotCapAtAnyNumber() {
        // 曾经的 K=3 上限已按需求移除：8 条也要全弹。
        List<Announcement> input = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            input.add(item(i, 0, 1));
        }
        assertEquals(8, AnnouncementQueue.select(input, NOW).size());
    }

    @Test
    public void ignoresSeenStateBecauseEveryLaunchPopsAgain() {
        // 同一条公告连着两次启动都要弹：这里用「上一版已读过」模拟上次启动关闭过，
        // 期望仍被选中——已读记忆已整套删除，select 不再接收任何已读判定。
        List<Announcement> input = Collections.singletonList(item(5, 0, 2));
        assertEquals(Collections.singletonList(5),
                ids(AnnouncementQueue.select(input, NOW)));
    }

    // ---------- 有效性过滤 ----------

    @Test
    public void dropsExpiredAnnouncements() {
        // 「有效」的第一层：未过期。validTo 小于 now 即无效。
        List<Announcement> input = Arrays.asList(
                item(1, 10, 1, NOW - 1),      // 过期
                item(2, 5, 1, NOW + 1));      // 仍有效
        assertEquals(Collections.singletonList(2),
                ids(AnnouncementQueue.select(input, NOW)));
    }

    @Test
    public void keepsAnnouncementExpiringExactlyNow() {
        // 与后端 SQL 的 valid_to >= now 口径一致：等于 now 仍算有效。
        List<Announcement> input = Collections.singletonList(item(1, 0, 1, NOW));
        assertEquals(1, AnnouncementQueue.select(input, NOW).size());
    }

    @Test
    public void keepsAnnouncementWithoutEndTime() {
        assertEquals(1, AnnouncementQueue.select(
                Collections.singletonList(item(1, 0, 1, null)), NOW).size());
    }

    @Test
    public void dropsEntriesWithoutTitleAndNulls() {
        // 脏数据不得弹成空框标题。
        Announcement noTitle = GSON.fromJson(
                "{\"id\":9,\"title\":\"  \",\"priority\":0,\"version\":1}", Announcement.class);
        List<Announcement> input = Arrays.asList(noTitle, null, item(1, 0, 1));
        assertEquals(Collections.singletonList(1),
                ids(AnnouncementQueue.select(input, NOW)));
    }

    @Test
    public void emptyAndNullInputsYieldEmptyQueue() {
        assertTrue(AnnouncementQueue.select(null, NOW).isEmpty());
        assertTrue(AnnouncementQueue.select(
                Collections.<Announcement>emptyList(), NOW).isEmpty());
    }

    // ---------- 排序 ----------

    @Test
    public void ordersByPriorityDesc() {
        List<Announcement> input = Arrays.asList(
                item(1, 0, 1), item(2, 10, 1), item(3, 5, 1));
        assertEquals(Arrays.asList(2, 3, 1), ids(AnnouncementQueue.select(input, NOW)));
    }

    @Test
    public void breaksPriorityTiesByIdAscForStableOrder() {
        // 不指定平手次序的话排序结果依赖输入顺序，同一份数据两次弹出顺序可能不同。
        List<Announcement> input = Arrays.asList(
                item(9, 5, 1), item(3, 5, 1), item(6, 5, 1));
        assertEquals(Arrays.asList(3, 6, 9), ids(AnnouncementQueue.select(input, NOW)));
    }

    @Test
    public void sortingIsIndependentOfInputOrder() {
        List<Announcement> input = Arrays.asList(
                item(1, 1, 1), item(2, 9, 1), item(3, 5, 1));
        List<Integer> forward = ids(AnnouncementQueue.select(input, NOW));
        List<Announcement> reversed = new ArrayList<>(input);
        Collections.reverse(reversed);
        List<Integer> backward = ids(AnnouncementQueue.select(reversed, NOW));
        assertEquals(forward, backward);
    }

    @Test
    public void expiredHighPriorityAnnouncementYieldsToValidLowPriorityOne() {
        // 过期的高优先级不该挤掉有效的低优先级。
        List<Announcement> input = Arrays.asList(
                item(1, 100, 1, NOW - 1), item(2, 0, 1));
        assertEquals(Collections.singletonList(2),
                ids(AnnouncementQueue.select(input, NOW)));
    }
}
