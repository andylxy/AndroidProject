package run.yigou.gxzy.data.remote.model;

/**
 * {@code /api/app/version} 的响应体（spec §6.5）。
 *
 * <p>字段名与后端 JSON 逐字对应（小驼峰），本项目统一用 Gson 默认字段名映射，
 * **不加** {@code @SerializedName}——改名会静默解析成 null/0。</p>
 */
public class UpdateInfo {

    /** 最新版本码（整数，后端一律整数比较，ADR-0002）。 */
    private int latestVersionCode;
    /** 最新版本名（用于弹窗标题）。 */
    private String latestVersionName;
    /** 该请求的最低版本码地板（全局地板与灰度规则取 max）。 */
    private int minVersionCode;
    /**
     * 后端判定的强制升级标志。**客户端必须直接采信这个值，不要用
     * `本机版本码 < minVersionCode` 自行推断**：地板违规后端已恒置 true，而地板之上的
     * 灰度规则可以是 `force=false` 的软提示（ADR-0008 §5）；自行推断会把软提示硬化成硬阻。
     */
    private boolean force;
    /** 新版本 APK 下载地址。 */
    private String downloadUrl;
    /** APK 的 MD5（下载校验用，可为空）。 */
    private String md5;
    /** 更新日志。 */
    private String updateLog;

    public int getLatestVersionCode() {
        return latestVersionCode;
    }

    public String getLatestVersionName() {
        return latestVersionName;
    }

    public int getMinVersionCode() {
        return minVersionCode;
    }

    public boolean isForce() {
        return force;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getMd5() {
        return md5;
    }

    public String getUpdateLog() {
        return updateLog;
    }

    /** 本机版本码低于最新版 → 有更新可提示。 */
    public boolean hasUpdate(int currentVersionCode) {
        return latestVersionCode > currentVersionCode;
    }
}
