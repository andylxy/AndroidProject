package run.yigou.gxzy.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import run.yigou.gxzy.data.remote.model.SearchPermissionState;
import run.yigou.gxzy.manager.search.SearchEntry;
import run.yigou.gxzy.manager.search.SearchPermissionManager;

/**
 * {@link SearchPermissionManager} 的「网关判定」纯 JVM 单测。
 *
 * <p>设计依据在 microfeed 仓（本仓不含该文件）：
 * {@code .scratch/search-permission/DESIGN.md} §2 不变式与 §5.2 客户端时序。</p>
 *
 * <p>被测的 {@code isSearchAllowed} 走进程内静态状态（不碰 MMKV / Android），
 * 因此可直接跑、也不需要 Robolectric。网关写反会让搜索在「没权限」时错误放开、
 * 或在「有权限」时被锁死，故每条分支都得钉住。</p>
 */
public final class SearchPermissionManagerTest {

    /** 网关状态是进程级静态，单测共享，每个用例前清干净。 */
    @Before
    public void reset() {
        SearchPermissionManager.sFetchOk = false;
        SearchPermissionManager.sState = null;
    }

    @Test
    public void gateClosedWhenNotFetched() {
        // fetchOk=false 时**不读**缓存：即便内存里有双 true，也必须返回 false。
        SearchPermissionManager.sState = new SearchPermissionState(true, true);
        SearchPermissionManager.sFetchOk = false;
        assertFalse(SearchPermissionManager.isSearchAllowed(SearchEntry.GLOBAL));
        assertFalse(SearchPermissionManager.isSearchAllowed(SearchEntry.BOOK));
    }

    @Test
    public void gateReadsStateWhenFetched() {
        SearchPermissionManager.sState = new SearchPermissionState(true, false);
        SearchPermissionManager.sFetchOk = true;
        assertTrue(SearchPermissionManager.isSearchAllowed(SearchEntry.GLOBAL));
        assertFalse(SearchPermissionManager.isSearchAllowed(SearchEntry.BOOK));
    }

    @Test
    public void gateClosesImmediatelyAfterFailure() {
        SearchPermissionManager.sState = new SearchPermissionState(true, true);
        SearchPermissionManager.sFetchOk = true;
        assertTrue(SearchPermissionManager.isSearchAllowed(SearchEntry.GLOBAL));
        // 拉取失败 → fail-closed：本次启动搜索立即全部禁用。
        SearchPermissionManager.sFetchOk = false;
        assertFalse(SearchPermissionManager.isSearchAllowed(SearchEntry.GLOBAL));
        assertFalse(SearchPermissionManager.isSearchAllowed(SearchEntry.BOOK));
    }

    /**
     * 回归：缺字段的响应被 {@link SearchPermissionState#isComplete()} 判为契约不符，
     * 上层据此走「拉取失败」而不是当成有效答案。缺字段时读取必须落到「否」而非 NPE。
     */
    @Test
    public void incompleteStateIsTreatedAsFailureAndGatesClosed() {
        SearchPermissionState missingBook = new SearchPermissionState();
        missingBook.global = Boolean.TRUE;
        assertFalse(missingBook.isComplete());
        assertTrue(missingBook.getGlobal()); // 已解析到的字段仍可读
        assertFalse(missingBook.getBook()); // 缺失字段 fail-closed
        // 即便 sState 里存着它，缺字段也绝不能被当作有效授权。
        SearchPermissionManager.sState = missingBook;
        SearchPermissionManager.sFetchOk = true;
        assertFalse(SearchPermissionManager.isSearchAllowed(SearchEntry.BOOK));
    }

    @Test
    public void completeStateIsRecognized() {
        assertTrue(new SearchPermissionState(true, false).isComplete());
        assertTrue(new SearchPermissionState(false, false).isComplete());
    }
}
