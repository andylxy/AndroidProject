package run.yigou.gxzy.data.remote.model;

/**
 * {@code GET /api/app/search-permission} 的响应体。
 *
 * <p>设计依据在 <b>microfeed 仓</b>：{@code .scratch/search-permission/DESIGN.md} §5.2。</p>
 *
 * <p>字段名与后端 JSON 逐字对应（小驼峰），本项目统一用 Gson 默认字段名映射，
 * <b>不加</b> {@code @SerializedName}——改名会静默解析成 false/null（布尔默认值），
 * 从而把「服务端说不允许」与「响应缺字段」混为一谈。</p>
 *
 * <p>同时作为客户端进程内 / MMKV 持久化的权限状态载体：两个布尔来自服务端的 RBAC 判定，
 * 由 {@code SearchPermissionManager} 在拉取成功后持有，{@code SearchPermissionStore}
 * 落盘只存最新一份。</p>
 */
public class SearchPermissionState {

    /**
     * 是否允许「首页全局搜索」。
     *
     * <p><b>刻意用包装类型 {@link Boolean} 而非基本类型 {@code boolean}</b>：Gson 解析
     * 缺失字段时会留 {@code null}，而基本类型会被填成 {@code false}——两者都表示
     * 「服务端说不行」，无法区分「服务端明确否了」与「响应缺字段（契约不符）」。
     * 用包装类型让 {@link #isComplete()} 能在缺字段时识别出来，走<b>拉取失败</b>分支
     * （DESIGN §6.1 要求字段缺失按失败处理），而不是误当成有效答案把搜索锁死。</p>
     */
    public Boolean global;

    /** 是否允许「书内搜索」；{@code null} 表示服务端没给这个字段（见 {@link #global}）。 */
    public Boolean book;

    public SearchPermissionState() {
    }

    public SearchPermissionState(boolean global, boolean book) {
        this.global = global;
        this.book = book;
    }

    /**
     * 两个字段都真实存在（契约完整）。
     *
     * <p>只有完整响应才允许被当成「有效答案」；缺任一字段都按拉取失败处理
     * → {@code fetchOk=false} + 退避重试。</p>
     */
    public boolean isComplete() {
        return global != null && book != null;
    }

    /** 首页搜索许可（缺字段一律按「否」= fail-closed）。 */
    public boolean getGlobal() {
        return Boolean.TRUE.equals(global);
    }

    /** 书内搜索许可（缺字段一律按「否」= fail-closed）。 */
    public boolean getBook() {
        return Boolean.TRUE.equals(book);
    }
}
