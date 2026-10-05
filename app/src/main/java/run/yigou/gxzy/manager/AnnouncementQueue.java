package run.yigou.gxzy.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import run.yigou.gxzy.data.remote.model.Announcement;

/**
 * 公告的<b>挑选</b>逻辑：滤掉已读、按优先级排序、截断到本次上限（DESIGN §6.4）。
 *
 * <p>纯逻辑，<b>不碰 MMKV / Android / 网络</b>，因此可被纯 JVM 单测直接覆盖。
 * 这正是它与 {@link AnnouncementStore} 分开的原因：Store 负责「已读记忆」，
 * 这里负责「这一次该弹哪几条」，两者混在一起就没法离线测挑选规则了。</p>
 *
 * <p><b>为什么要有上限 K</b>：一次启动最多打扰用户 N 次。后端最多下发 20 条，
 * 若全弹，用户要连续关掉十几个框才能用 App。未弹的不丢——它们仍是「未读」，
 * 下次启动继续补弹（DESIGN D3 静默递延，不提示剩余数）。</p>
 */
public final class AnnouncementQueue {

    /** 一次启动最多弹几条（DESIGN D1）。可调，但改动会改变打扰频次。 */
    public static final int MAX_POP_PER_LAUNCH = 3;

    private AnnouncementQueue() {
    }

    /**
     * 从下发的列表里挑出本次要弹的公告（用 {@link AnnouncementStore} 的真实已读表）。
     *
     * <p>这个重载会碰 MMKV，<b>不能</b>在纯 JVM 单测里用。单测请用
     * {@link #select(List, int, SeenFilter)} 注入假的已读判定。</p>
     */
    public static List<Announcement> select(List<Announcement> announcements, int max) {
        return select(announcements, max, new SeenFilter() {
            @Override
            public boolean isSeen(int id, int version) {
                return AnnouncementStore.isSeen(id, version);
            }
        });
    }

    /**
     * 从下发的列表里挑出本次要弹的公告。<b>纯逻辑</b>：已读判定由调用方注入，
     * 因此本重载既不碰 MMKV 也不碰 Android，可被纯 JVM 单测完整覆盖。
     *
     * <p>顺序：丢弃无标题的脏数据 → 丢弃已读（按 id+version 判）→ 按
     * {@code priority DESC, id ASC} 排序 → 截断到 {@code max}。
     *
     * <p><b>为什么平手时按 id 升序</b>：后端已按 {@code priority DESC, updated_at DESC}
     * 排过序，但 {@code updated_at} 不在响应体里，客户端只能自己定一个确定的次序。
     * 用 id 升序（同批插入时约等于插入序）是稳定且可预期的；不指定平手次序的话，
     * {@code Collections.sort} 的结果依赖输入顺序，同一份数据可能两次弹出的顺序不同。</p>
     *
     * @param announcements 后端下发的公告（可为 null / 含 null 元素）
     * @param max           本次上限；{@code <= 0} 视为 1（宁可少弹也不弹一堆）
     * @param seenFilter    「这条是否已读」的判定（生产传 MMKV 表，单测传假实现）
     * @return 本次要弹的队列，按弹出的先后顺序排列
     */
    public static List<Announcement> select(
            List<Announcement> announcements, int max, SeenFilter seenFilter) {
        if (announcements == null || announcements.isEmpty()) {
            return Collections.emptyList();
        }
        int limit = max <= 0 ? 1 : max;
        List<Announcement> unseen = new ArrayList<>();
        for (Announcement announcement : announcements) {
            if (announcement == null || !announcement.hasTitle()) {
                // 无标题的条目弹出来是个空框标题，比不弹更糟；后端已校验，这里只防脏数据。
                continue;
            }
            if (seenFilter.isSeen(announcement.getId(), announcement.getVersion())) {
                continue;
            }
            unseen.add(announcement);
        }
        Collections.sort(unseen, new Comparator<Announcement>() {
            @Override
            public int compare(Announcement left, Announcement right) {
                if (left.getPriority() != right.getPriority()) {
                    // 优先级大的先弹。
                    return right.getPriority() - left.getPriority();
                }
                return left.getId() - right.getId();
            }
        });
        return unseen.size() > limit
                ? new ArrayList<>(unseen.subList(0, limit))
                : unseen;
    }

    /** 「某条公告的某版本是否已读」的判定。抽成接口只为让 {@link #select} 可离线测。 */
    public interface SeenFilter {
        boolean isSeen(int id, int version);
    }
}
