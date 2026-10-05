package run.yigou.gxzy.data.remote.model;

/**
 * 一条公告（运营/系统消息）。
 *
 * <p>后端只下发「当前生效」的公告（{@code status=2} 且落在生效窗口内），所以本类
 * <b>不</b>带 {@code status}：客户端不需要、也不该知道草稿/已删除这些后台概念。</p>
 *
 * <p><b>version 是去重的关键</b>：后端每次改动 title/body 就把它 +1，客户端按
 * {@code id→seenVersion} 记忆已读，{@code seenVersion < version} 即视为「内容改过 →
 * 重新弹一次」（DESIGN D2）。改名会静默变成 0 → 每次启动都重弹。</p>
 */
public class Announcement {

    /** 公告 id（后端自增主键）。 */
    private int id;
    /** 标题。 */
    private String title;
    /** 正文，多行纯文本。 */
    private String body;
    /** 优先级，越大越先弹。 */
    private int priority;
    /** 内容版本号，用于「改过内容就重弹」。 */
    private int version;
    /** 生效开始（unix 毫秒），可空。客户端不参与判定，仅作展示。 */
    private Long validFrom;
    /** 生效结束（unix 毫秒），可空。 */
    private Long validTo;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public int getPriority() {
        return priority;
    }

    public int getVersion() {
        return version;
    }

    public Long getValidFrom() {
        return validFrom;
    }

    public Long getValidTo() {
        return validTo;
    }

    /** 标题非空才算一条可展示的公告（后端已校验，这里只防脏数据）。 */
    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }
}
