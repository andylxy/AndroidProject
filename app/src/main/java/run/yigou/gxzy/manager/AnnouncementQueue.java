package run.yigou.gxzy.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import run.yigou.gxzy.data.remote.model.Announcement;

/**
 * 公告的挑选与排序：哪些能在本次启动时弹、按什么顺序弹。
 *
 * <p>纯逻辑，<b>不碰 MMKV / Android / 网络</b>，因此可被纯 JVM 单测直接覆盖。
 * 与 {@link AnnouncementStore} 分开的原因：那边管「存什么、什么时候拉」，这里管
 * 「这一次弹哪几条、什么顺序」。</p>
 *
 * <p><b>需求：每次 App 启动都弹出全部有效公告</b>，所以这里
 * <b>不</b>做已读过滤，也<b>不</b>设条数上限——服务器当前有几条有效公告就弹几条。
 * 剩下的唯一职责是「剔除无效的 + 排序」。</p>
 */
public final class AnnouncementQueue {

    private AnnouncementQueue() {
    }

    /**
     * 挑出本次要弹的公告，<b>返回全部</b>（无上限）。
     *
     * <p>「有效」= 有标题、且在 {@code now} 时刻尚未过期（口径见
     * {@link AnnouncementStore#isExpired}，与后端 SQL 的 {@code valid_to >= ?} 一致）。</p>
     *
     * <p>排序 {@code priority DESC, id ASC}：优先级高的先弹；同优先级按 id 升序，
     * 也就是同批插入的按插入序。不指定平手次序的话 {@code Collections.sort} 的结果
     * 依赖输入顺序，同一份数据可能两次弹出的顺序不同。</p>
     *
     * @param announcements 本地缓存读出的公告（可能含 null 元素与脏数据）
     * @param now            判断过期用的时刻
     * @return 本次要弹的队列，按弹出的先后顺序排列
     */
    public static List<Announcement> select(List<Announcement> announcements, long now) {
        if (announcements == null || announcements.isEmpty()) {
            return Collections.emptyList();
        }
        List<Announcement> valid = new ArrayList<>();
        for (Announcement announcement : announcements) {
            if (announcement == null || !announcement.hasTitle()) {
                // 无标题的条目弹出来是个空框标题，比不弹更糟；后端已校验，这里只防脏数据。
                continue;
            }
            if (AnnouncementStore.isExpired(announcement, now)) {
                continue;
            }
            valid.add(announcement);
        }
        Collections.sort(valid, new Comparator<Announcement>() {
            @Override
            public int compare(Announcement left, Announcement right) {
                if (left.getPriority() != right.getPriority()) {
                    return right.getPriority() - left.getPriority();
                }
                return left.getId() - right.getId();
            }
        });
        return valid;
    }
}
