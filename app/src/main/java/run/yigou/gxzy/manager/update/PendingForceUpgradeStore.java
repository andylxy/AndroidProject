package run.yigou.gxzy.manager.update;

import com.google.gson.Gson;
import com.tencent.mmkv.MMKV;

import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.data.remote.model.UpdateInfo;
import run.yigou.gxzy.log.EasyLog;

/**
 * 「欠升级记录」的落盘存储（ADR-0001 追加决议，2026-10-05）。
 *
 * <p>记的是<b>客观事实</b>：「后端上一次说本机低于硬地板」。刻意<b>不</b>记
 * 「用户按过取消」—— 那是另一条记忆，只在本进程有效（{@code sForceUpgradeDismissed}）。
 * 两条记忆会互相抵消：欠升级让 App 弹框，已取消让 App 闭嘴。</p>
 *
 * <p><b>为什么必须落盘</b>：原先「欠升级」标记只在版本检查的<b>成功回调</b>里置位，
 * 于是「冷启动离线 / 版本检查失败 / 用户抢在返回前点书」这三种情况下标记从未置位 →
 * {@link UpdateManager#checkForceOnReading} 返回 false → 阅读入口不重弹 →
 * 正是需求 2 要避免的「用户点了阅读却无任何提示」。落盘后这条路径不再依赖网络。</p>
 *
 * <p><b>失效判据两道，命中任一即清除</b>（ADR-0001）：
 * <ol>
 *   <li>本机 {@code versionCode} ≥ 记录里的 {@code minVersionCode} → 已到地板之上，失效；</li>
 *   <li>远端版本检查返回 {@code force=false} → 后端已解除强制，失效。</li>
 * </ol>
 * 只用 1 不够（后端下调地板后仍按旧记录误拦）；只用 2 不够（用户升级后长期离线，
 * 旧记录永远有效，点阅读被永久误拦）。</p>
 *
 * <p>存储用 MMKV（与 {@code DeviceIdStore} 同一惯例：进程级单例 + 方法内获取，不持 Context），
 * Gson 默认字段名映射 —— {@link UpdateInfo} 刻意不加 {@code @SerializedName}，
 * 改名会静默解析成 null/0。</p>
 *
 * <p><b>Q3 语义（容易被误改，务必读）</b>：本类只按「判据 1」把记录丢弃 ——
 * 即<b>本机版本码已达地板</b>（用户真的升级过了）。而「后端已把地板下调」这种过期
 * （判据 2）**必须继续拦**：那时判据 2 只能靠联网发现，一旦离线就没人纠正，
 * 继续按旧记录拦虽然可能误伤，但反过来（放行）会让升级门形同虚设。
 * 换言之：<b>本类返回 null 只意味着「本机已达标」，不意味着「后端已解除」</b>。</p>
 */
public final class PendingForceUpgradeStore {

    private static final String TAG = "PendingForceUpgradeStore";

    private static final String MMKV_ID = "force_upgrade_state";
    private static final String KEY_PENDING_INFO = "pending_force_upgrade_info";

    private static final Gson GSON = new Gson();

    private PendingForceUpgradeStore() {
    }

    /**
     * 取本类的 MMKV 实例。
     *
     * <p>在方法内获取（与 {@code DeviceIdStore} 同一写法）而不是存成静态字段或 Holder：
     * 静态初始化会先于任何调用执行，而 MMKV 需要 native 库，JVM 单测里一触发就
     * {@code UnsatisfiedLinkError}。方法内获取天然把加载推迟到真正读写时，
     * 使 {@link #isStale} 可被纯 JVM 单测直接调用。</p>
     *
     * <p>{@code MMKV.mmkvWithID(id)} 返回进程级单例，逐次调用无代价。</p>
     */
    private static MMKV kv() {
        return MMKV.mmkvWithID(MMKV_ID);
    }

    /**
     * 这条欠升级记录是否应当丢弃（纯逻辑，<b>不碰 MMKV / Android</b>，可 JVM 单测）。
     *
     * <p>两种「该丢」：本机已达标（{@code versionCode >= minVersionCode}），
     * 或它压根不是一条强制升级记录（{@code force=false}，例如只存了软更新）。
     * 后者实际不可达 —— {@link #write} 已挡 —— 属于防御。</p>
     *
     * <p><b>不</b>包含「后端是否已解除强制」：那要联网才知道，本方法刻意不碰。
     * 详见类注释的 Q3 语义。</p>
     *
     * @param info               落盘的升级信息；{@code null} 视为无记录
     * @param currentVersionCode 本机 {@code BuildConfig.VERSION_CODE}
     * @return {@code true} 表示该丢弃（读出方应清除且不再用于拦截）
     */
    public static boolean isStale(UpdateInfo info, int currentVersionCode) {
        if (info == null) {
            return true;
        }
        if (!info.isForce()) {
            return true;
        }
        return currentVersionCode >= info.getMinVersionCode();
    }

    /**
     * 读出欠升级记录，已失效则清除并返回 {@code null}。
     *
     * <p>读出脏数据（JSON 损坏 / 字段缺失）同样按「无记录」处理并清除 ——
     * 宁可退回改动前的行为，也不能因为一条坏记录让用户永远读不了书。</p>
     */
    public static UpdateInfo read() {
        try {
            String raw = kv().decodeString(KEY_PENDING_INFO);
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            UpdateInfo info = GSON.fromJson(raw, UpdateInfo.class);
            if (isStale(info, AppConfig.getVersionCode())) {
                if (info != null) {
                    EasyLog.print(TAG, "欠升级记录已失效（本机 " + AppConfig.getVersionCode()
                            + " vs 记录 minVersionCode=" + info.getMinVersionCode() + "），清除");
                }
                clear();
                return null;
            }
            return info;
        } catch (Throwable error) {
            // 读盘失败不得连带影响阅读功能：降级为「无记录」。
            EasyLog.print(TAG, "读取欠升级记录失败，降级为无记录");
            EasyLog.print(error);
            clear();
            return null;
        }
    }

    /**
     * 写入欠升级记录；仅当后端本次判为强制升级时才写。
     *
     * <p>写失败只记日志，不阻断版本检查主流程 —— 落盘是增强，不能反过来弄挂检查。</p>
     */
    public static void write(UpdateInfo info) {
        if (info == null || !info.isForce()) {
            return;
        }
        try {
            kv().encode(KEY_PENDING_INFO, GSON.toJson(info));
            EasyLog.print(TAG, "已落盘欠升级记录: latest=" + info.getLatestVersionName()
                    + ", minVersionCode=" + info.getMinVersionCode());
        } catch (Throwable error) {
            EasyLog.print(TAG, "写入欠升级记录失败（不阻断检查）");
            EasyLog.print(error);
        }
    }

    /**
     * 清除欠升级记录。
     *
     * <p>由远端 {@code force=false} 与失效判据共同调用。</p>
     */
    public static void clear() {
        try {
            kv().removeValueForKey(KEY_PENDING_INFO);
        } catch (Throwable error) {
            EasyLog.print(TAG, "清除欠升级记录失败");
            EasyLog.print(error);
        }
    }
}
