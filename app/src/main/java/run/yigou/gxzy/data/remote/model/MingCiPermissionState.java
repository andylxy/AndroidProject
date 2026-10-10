package run.yigou.gxzy.data.remote.model;

/**
 * {@code GET /api/app/mingci-permission} 的响应体。
 *
 * <p>严格镜像 {@link SearchPermissionState}：G3 全局单开关 → 只有一个布尔 {@code allowed}。
 * 字段名与后端 JSON 逐字对应（小驼峰），本项目统一用 Gson 默认字段名映射，
 * <b>不加</b> {@code @SerializedName}——改名会静默解析成 false/null（布尔默认值），
 * 从而把「服务端说不允许」与「响应缺字段」混为一谈。</p>
 *
 * <p>同时作为客户端进程内 / MMKV 持久化的权限状态载体：{@code allowed} 来自服务端的 RBAC 判定，
 * 由 {@code MingCiPermissionManager} 在拉取成功后持有，{@code MingCiPermissionStore}
 * 落盘只存最新一份。</p>
 */
public class MingCiPermissionState {

    /**
     * 是否允许查看名词解释（底层名词数据列表的加载闸门）。
     *
     * <p><b>刻意用包装类型 {@link Boolean} 而非基本类型 {@code boolean}</b>：Gson 解析
     * 缺失字段时会留 {@code null}，而基本类型会被填成 {@code false}——两者都表示
     * 「服务端说不行」，无法区分「服务端明确否了」与「响应缺字段（契约不符）」。
     * 用包装类型让 {@link #isComplete()} 能在缺字段时识别出来，走<b>拉取失败</b>分支
     * （INV-1/INV-2：字段缺失按失败处理），而不是误当成有效答案把名词解释锁死。</p>
     */
    public Boolean allowed;

    public MingCiPermissionState() {
    }

    public MingCiPermissionState(boolean allowed) {
        this.allowed = allowed;
    }

    /**
     * 字段真实存在（契约完整）。
     *
     * <p>只有完整响应才允许被当成「有效答案」；缺字段按拉取失败处理
     * → {@code fetchOk=false} + 退避重试。</p>
     */
    public boolean isComplete() {
        return allowed != null;
    }

    /** 名词解释查看许可（缺字段一律按「否」= fail-closed）。 */
    public boolean getAllowed() {
        return Boolean.TRUE.equals(allowed);
    }
}
