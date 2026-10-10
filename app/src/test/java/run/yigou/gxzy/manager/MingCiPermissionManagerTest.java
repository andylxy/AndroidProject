package run.yigou.gxzy.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import run.yigou.gxzy.data.remote.model.MingCiPermissionState;
import run.yigou.gxzy.manager.mingci.MingCiPermissionManager;

/**
 * {@link MingCiPermissionManager} 的「网关判定」纯 JVM 单测。
 *
 * <p>严格镜像 {@link SearchPermissionManagerTest}：G3 单开关只有一个布尔 {@code allowed}，
 * 不变量（INV-1 静默失败 / INV-2 默认关闭 / INV-3 一次启动一拉）与 search 完全一致。</p>
 *
 * <p>被测的 {@code isAllowed} 走进程内静态状态（不碰 MMKV / Android），因此可直接跑、
 * 也不需要 Robolectric。网关写反会让名词解释在「没权限」时错误放开、或在「有权限」时被锁死，
 * 故每条分支都得钉住。</p>
 */
public final class MingCiPermissionManagerTest {

    /** 网关状态是进程级静态，单测共享，每个用例前清干净。 */
    @Before
    public void reset() {
        MingCiPermissionManager.sFetchOk = false;
        MingCiPermissionManager.sState = null;
    }

    @Test
    public void gateClosedWhenNotFetched() {
        // fetchOk=false 时**不读**缓存：即便内存里有 true，也必须返回 false。
        MingCiPermissionManager.sState = new MingCiPermissionState(true);
        MingCiPermissionManager.sFetchOk = false;
        assertFalse(MingCiPermissionManager.isAllowed());
    }

    @Test
    public void gateReadsStateWhenFetched() {
        MingCiPermissionManager.sState = new MingCiPermissionState(true);
        MingCiPermissionManager.sFetchOk = true;
        assertTrue(MingCiPermissionManager.isAllowed());
        // 取反 state 验证读的是进程内 sState 而非常量。
        MingCiPermissionManager.sState = new MingCiPermissionState(false);
        assertFalse(MingCiPermissionManager.isAllowed());
    }

    @Test
    public void gateClosesImmediatelyAfterFailure() {
        MingCiPermissionManager.sState = new MingCiPermissionState(true);
        MingCiPermissionManager.sFetchOk = true;
        assertTrue(MingCiPermissionManager.isAllowed());
        // 拉取失败 → fail-closed：本次启动名词解释立即禁用。
        MingCiPermissionManager.sFetchOk = false;
        assertFalse(MingCiPermissionManager.isAllowed());
    }

    /**
     * 回归：缺字段的响应被 {@link MingCiPermissionState#isComplete()} 判为契约不符，
     * 上层据此走「拉取失败」而不是当成有效答案。缺字段时读取必须落到「否」而非 NPE。
     */
    @Test
    public void incompleteStateIsTreatedAsFailureAndGatesClosed() {
        MingCiPermissionState missing = new MingCiPermissionState();
        // allowed 缺字段 → null；isComplete() 判为契约不符
        assertFalse(missing.isComplete());
        assertFalse(missing.getAllowed()); // 缺失字段 fail-closed
        // 即便 sState 里存着它，缺字段也绝不能被当作有效授权。
        MingCiPermissionManager.sState = missing;
        MingCiPermissionManager.sFetchOk = true;
        assertFalse(MingCiPermissionManager.isAllowed());
    }

    @Test
    public void completeStateIsRecognized() {
        assertTrue(new MingCiPermissionState(true).isComplete());
        assertTrue(new MingCiPermissionState(false).isComplete());
    }

    /**
     * 独立钉住「sState 为 null」这一分支：默认启动时 {@code sState} 就是 null，
     * 网关必须返回 false，且取值过程不能抛异常。
     *
     * <p>与 {@link #gateClosedWhenNotFetched} 不同——那条把 sState 显式置为
     * {@code new MingCiPermissionState(true)} 来验证「fetchOk=false 时不读 sState」，
     * null 分支在原有用例里没有被覆盖到。</p>
     */
    @Test
    public void gateClosedWhenStateIsNull() {
        MingCiPermissionManager.sState = null;
        MingCiPermissionManager.sFetchOk = true;
        assertFalse(MingCiPermissionManager.isAllowed());
    }

    /**
     * 独立钉住「sFetchOk=false 且 sState=null」这一组合：即便 sState 里没有任何值，
     * 取值也必须走 {@code sFetchOk} 短路而返回 false，不能因解引用 null 而抛异常。
     * 与 {@link #gateClosedWhenStateIsNull} 不同——那条把 sFetchOk 设为 true 才能走到 null 分支。
     */
    @Test
    public void gateShortCircuitsBeforeReadingNullState() {
        MingCiPermissionManager.sState = null;
        MingCiPermissionManager.sFetchOk = false;
        assertFalse(MingCiPermissionManager.isAllowed());
    }
}
