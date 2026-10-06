package run.yigou.gxzy.manager;

import com.tencent.mmkv.MMKV;

/**
 * 启动硬门：隐私协议同意状态的<b>唯一事实来源</b>与唯一写入口。
 *
 * <p><b>为什么单独成类</b>：这个状态有两个读者（闪屏要判断能不能放行、引导页要判断能不能
 * 放行回上游）和一个写者（引导页点「同意」时）。若各自持有字面量，改一处忘另一处就会
 * 出现「协议没同意却被放行」这类静默漏洞 —— 评审已实测到过一次
 * （{@code onRestart} 只判权限不判协议，用户可以去设置页授完权限就绕过协议）。</p>
 *
 * <p><b>设计约束</b>：</p>
 * <ul>
 *   <li>状态<b>持久化</b>在 MMKV：协议同意是一次性的，进程被杀 / 设备重启后仍应有效；</li>
 *   <li>读接口 {@link #isAgreed()} 供各 Activity 自行判断（多处需要），</li>
 *       写接口 {@link #markAgreed()} 只应在本类内部被调用（唯一写入口），</li>
 *   <li>本类<b>不缓存到内存字段</b>：同意是在引导页里发生的，而各 Activity 的 onCreate
 *       都可能早于那次写入；缓存会读到过期值。多读一次 MMKV 的开销可忽略
 *       （引导页与放行判断都是一次性动作，不在高频路径上）。</li>
 * </ul>
 */
public final class PrivacyAgreement {

    /** MMKV 键名。改名会静默丢失所有用户的同意状态，须谨慎。 */
    private static final String KEY_AGREED = "is_privacy_agreed";

    private PrivacyAgreement() {
    }

    /**
     * 隐私协议是否已同意。
     *
     * <p>注意：协议同意是「同意」，**不等于**「系统权限已授予」——那是另一个条件，
     * 两者都通过才算放行（见 {@code SplashActivity.onConsentAndPermissionGranted}）。</p>
     */
    public static boolean isAgreed() {
        return MMKV.defaultMMKV().decodeBool(KEY_AGREED, false);
    }

    /**
     * 标记隐私协议已同意。只应在用户点击引导页「同意」时调用。
     *
     * <p>写失败（MMKV 异常）不抛：调用方紧接着就会申请系统权限并放行，用户感知上已是
     * 「同意了」；而这里若抛异常会打断放行流程，用户会困在引导页。极端后果仅是下次启动
     * 再问一次协议，可接受。</p>
     */
    public static void markAgreed() {
        MMKV.defaultMMKV().encode(KEY_AGREED, true);
    }
}
