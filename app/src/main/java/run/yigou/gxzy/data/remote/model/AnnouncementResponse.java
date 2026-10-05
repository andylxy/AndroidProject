package run.yigou.gxzy.data.remote.model;

import java.util.List;

/**
 * {@code GET /api/app/announcements} 的响应体（公告，DESIGN §5.3）。
 *
 * <p>字段名与后端 JSON 逐字对应（小驼峰），本项目统一用 Gson 默认字段名映射，
 * **不加** {@code @SerializedName}——改名会静默解析成 null/0。</p>
 *
 * <p>外层是 {@code {"announcements": [...]}} 而不是数组本身：后端契约固定用对象
 * （便于以后加 {@code serverTime} 之类字段而不破坏兼容），空列表时返回
 * {@code {"announcements": []}}。</p>
 */
public class AnnouncementResponse {

    private List<Announcement> announcements;

    public List<Announcement> getAnnouncements() {
        return announcements;
    }
}
