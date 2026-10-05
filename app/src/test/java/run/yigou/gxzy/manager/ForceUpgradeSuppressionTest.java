package run.yigou.gxzy.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 需求 2「取消强制升级后不再重弹」的抑制规则单测。
 *
 * <p>纯 JVM，不碰 Android 运行时，也不引入 mock 库 —— 被测的
 * {@link ForceUpgradeSuppression} 刻意不持有任何 Handler/Activity 依赖，
 * 所以引用它不会触发静态初始化失败。</p>
 *
 * <p><b>为什么这条规则必须有单测</b>：它一旦写错，用户按了「下次再说」之后点「发现」tab
 * 拉 {@code GetNav} 会收到 426 → 又弹升级框，<b>取消等于无效</b>（这正是修复前的漏网之处：
 * 初版只抑制 FOREGROUND）。而真机复现依赖时序与缓存命中 —— 内容命中缓存时压根不发请求，
 * 426 不会来，手验极不稳。纯布尔逻辑则完全可测。</p>
 */
public class ForceUpgradeSuppressionTest {

    @Test
    public void suppressesForceUpgradeAfterUserCancelled() {
        assertTrue(ForceUpgradeSuppression.shouldSuppressAfterCancel(true, true, false));
    }

    @Test
    public void suppressesThe426PathAfterCancel() {
        // 修复前只抑制 FOREGROUND，426 由所有内容端点下发 → 取消后点「发现」tab 又弹。
        // 「非手动」这一位必须让 426 也落进抑制，否则取消等于无效。
        assertTrue(ForceUpgradeSuppression.shouldSuppressAfterCancel(true, true, false));
    }

    @Test
    public void keepsManualCheckDialogueVisibleAfterCancel() {
        // 用户主动点「检查更新」是显式求结果，不能被静默 —— 那是需求 1 的入口。
        assertFalse(ForceUpgradeSuppression.shouldSuppressAfterCancel(true, true, true));
    }

    @Test
    public void showsDialogueWhenUserNeverCancelled() {
        assertFalse(ForceUpgradeSuppression.shouldSuppressAfterCancel(true, false, false));
        assertFalse(ForceUpgradeSuppression.shouldSuppressAfterCancel(true, false, true));
    }

    @Test
    public void neverSuppressesSoftUpgrade() {
        // 软提示（force=false）本来就可取消，取消与否都不该由这个标记决定。
        assertFalse(ForceUpgradeSuppression.shouldSuppressAfterCancel(false, true, false));
        assertFalse(ForceUpgradeSuppression.shouldSuppressAfterCancel(false, true, true));
    }

    @Test
    public void neverSuppressesWhenNothingToUpgrade() {
        assertFalse(ForceUpgradeSuppression.shouldSuppressAfterCancel(false, false, false));
    }
}
