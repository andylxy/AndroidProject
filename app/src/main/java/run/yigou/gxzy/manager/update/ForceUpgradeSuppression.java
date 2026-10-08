package run.yigou.gxzy.manager.update;

/**
 * 需求 2「取消强制升级后不再重弹」的判定规则（纯逻辑，无 Android 依赖）。
 *
 * <p><b>为什么独立成类</b>：本规则原本内联在 {@code UpdateManager} 里，但引用该类会触发
 * 它的静态初始化（{@code new Handler(Looper.getMainLooper())}），在纯 JVM 单测下抛
 * {@code ExceptionInInitializerError} —— 于是「这条规则写错了会怎样」就只能靠真机
 * 手验，而真机复现还依赖时序与缓存命中（内容命中缓存时压根不发请求，426 不会来）。
 * 抽到这里后，规则本身可被单测直接钉死。</p>
 *
 * <p><b>规则</b>：用户按过「取消」后，<b>除手动点「检查更新」外</b>，任何触发都不再弹
 * 强制升级框——<b>包括内容门 426</b>。阅读侧不受影响：它由
 * {@code UpdateManager.checkForceOnReading} 在阅读意图入口独立把关（不依赖 426、
 * 不依赖网络），所以这里不必为阅读留例外。</p>
 */
public final class ForceUpgradeSuppression {

    private ForceUpgradeSuppression() {
    }

    /**
     * 本次强制升级提示是否应当被抑制（静默跳过、不弹窗）。
     *
     * @param force     后端本次是否判为强制升级；软提示（{@code false}）永不受影响
     * @param dismissed 用户本次进程生命周期内是否已按过「取消」
     * @param manual    本次是否由用户手动点「检查更新」触发（需求 1 的入口，须保留反馈）
     * @return {@code true} 表示应当静默跳过
     */
    public static boolean shouldSuppressAfterCancel(
            boolean force, boolean dismissed, boolean manual) {
        return force && dismissed && !manual;
    }
}
