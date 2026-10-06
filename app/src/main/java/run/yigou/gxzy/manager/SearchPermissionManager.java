package run.yigou.gxzy.manager;

import android.app.Activity;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;

import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.data.remote.api.SearchPermissionApi;
import run.yigou.gxzy.data.remote.model.SearchPermissionState;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.network.server.VersionRequestServer;

/**
 * 搜索权限的拉取与判定。严格镜像 {@link AnnouncementManager} 的结构，
 * 但更精简：搜索不需要弹窗，只需要在 App 冷启动时拉一次「当前账号是否有搜索权限」，
 * 并据此开启 / 禁用搜索入口。
 *
 * <p>设计依据在 <b>microfeed 仓</b>（本仓不含该文件）：
 * {@code .scratch/search-permission/DESIGN.md}，下文「DESIGN §x.y」均指该文件。</p>
 *
 * <p>四条硬性不变量（DESIGN §2）：</p>
 * <ol>
 *   <li><b>静默失败</b>（INV-1）：拉取失败（网络 / 解析 / 字段缺失 / 401 / 403 / 429 / 5xx）
 *       → 只打 log，不弹窗、不 toast。失败只让本次启动的搜索保持关闭，并在 30 分钟后退避重试。</li>
 *   <li><b>默认关闭</b>（INV-2）：{@link #isSearchAllowed} 先判进程内 {@code sFetchOk}，为假
 *       直接返回 false——即使 MMKV 里有昨天成功的缓存也不读。冷启动、尚未拉到、或上次拉取
 *       失败时，搜索一律禁用（fail-closed）。</li>
 *   <li><b>一次启动一拉</b>（INV-3）：{@link LaunchOnceGate} 进程级闸门，回到前台不重复拉。</li>
 *   <li><b>失败退避</b>（INV-5）：失败后 30 分钟再试一次，不疯狂重试。</li>
 * </ol>
 */
public final class SearchPermissionManager {

    private static final String TAG = "SearchPermissionManager";

    /**
     * 进程内「本次启动是否已成功拉到权限判定」。默认 false = 搜索默认关闭（INV-2）。
     * 只在 {@link #fetchAndEvaluate} 成功落盘后翻 true；任何失败都翻回 false（fail-closed）。
     */
    static boolean sFetchOk = false;

    /**
     * 最近一次成功拉到的权限状态（进程内持有，网关直接读它，不碰磁盘，见
     * {@link SearchPermissionStore} 的说明）。
     */
    static SearchPermissionState sState = null;

    /** 进程级「本次启动已拉过」闸门（INV-3）。 */
    private static final LaunchOnceGate sLaunchGate = new LaunchOnceGate();

    /**
     * 退避重试的排程器（单飞 + 主线程定时），与公告刷新共用 {@link RetryScheduler}。
     *
     * <p>到点执行的就是 {@link #fetchAndEvaluate} 本身，它开头的节流判定会拦住过于频繁的
     * 尝试，不会打转。</p>
     */
    private static final RetryScheduler sRetryScheduler = new RetryScheduler(
            SearchPermissionStore.RETRY_INTERVAL_MS,
            () -> fetchAndEvaluate(ForegroundActivities.topIfUsable()));

    private SearchPermissionManager() {
    }

    /** 进程级「是否已成功拉取过」（仅供诊断与单测）。 */
    static boolean isFetchOk() {
        return sFetchOk;
    }

    /**
     * 搜索入口闸门：当前账号在某入口是否被允许搜索。
     *
     * <p>顺序即 INV-2：先判 {@code sFetchOk}（进程内是否成功拉到过），为假直接 false、
     * <b>不读</b>任何缓存；为真再取进程内 {@code sState} 对应入口的布尔。两者任一缺失都返回 false。
     * 读取走 {@code getXxx()} 而非直接取字段：字段是包装类型，缺字段时必须落到
     * 「否」而不是拆箱 NPE。</p>
     */
    public static boolean isSearchAllowed(SearchEntry entry) {
        if (!sFetchOk) {
            return false;
        }
        if (sState == null) {
            return false;
        }
        return entry == SearchEntry.GLOBAL ? sState.getGlobal() : sState.getBook();
    }

    /**
     * 把搜索权限拉取挂到<b>应用级</b>前台回调（DESIGN §5.2），与版本检查 / 公告同一套路。
     * 由 {@code AppApplication} 在启动时调用一次。
     */
    public static void registerForegroundCheck() {
        EasyLog.print(TAG, "搜索权限拉取已挂到应用级前台回调，本次启动只拉一次");
        ActivityManager.getInstance().registerApplicationLifecycleCallback(
                new ActivityManager.ApplicationLifecycleCallback() {
                    @Override
                    public void onApplicationCreate(Activity activity) {
                        // 冷启动也会走 onApplicationForeground，这里无需处理。
                    }

                    @Override
                    public void onApplicationDestroy(Activity activity) {
                        // 不在此做任何清理：状态留在内存里随进程一起消失即可。
                    }

                    @Override
                    public void onApplicationBackground(Activity activity) {
                        // 退到后台不拉：那会在用户切出去时强打一个请求。
                    }

                    @Override
                    public void onApplicationForeground(Activity activity) {
                        tryLaunchFetch(activity);
                    }
                });
        // 每个 Activity resume 必定带可用宿主，作为首个前台回调的兜底（同 AnnouncementManager）。
        ActivityManager.getInstance().registerActivityResumeCallback(
                new ActivityManager.ActivityResumeCallback() {
                    @Override
                    public void onActivityResumed(Activity activity) {
                        tryLaunchFetch(activity);
                    }
                });
    }

    /**
     * 「本次启动拉一次」的唯一入口：宿主可用**且**闸门未置位时才拉（与
     * {@code AnnouncementManager.tryLaunchFetch} 同顺序：先判宿主、再置位，失败不消耗机会）。
     */
    private static void tryLaunchFetch(Activity activity) {
        if (sLaunchGate.runOnceIfHostUsable(activity, () -> onLaunchReady(activity))) {
            EasyLog.print(TAG, "本次启动首次拉取搜索权限");
        }
    }

    private static void onLaunchReady(Activity activity) {
        fetchAndEvaluate(activity);
    }

    /**
     * 拉取搜索权限：按节流规则决定是否请求，成功后写缓存 + 翻 sFetchOk，失败静默 + 退避。
     */
    static void fetchAndEvaluate(Activity activity) {
        if (!SearchPermissionStore.shouldFetchNow()) {
            // 处于失败退避期。若这是重试链自己触发的（Handler 提前了 1ms、或时钟被回拨），
            // 直接返回会让整条链断在这里——搜索要等到下次冷启动才有机会刷新。
            // 所以这里补排一次；单飞标志保证不会排出多个任务。
            EasyLog.print(TAG, "处于失败退避期，本次不请求");
            scheduleRetry();
            return;
        }
        if (activity == null || !(activity instanceof LifecycleOwner)) {
            // 节流已放行、只是没有可用宿主：必须排下一次，否则整条重试链到此为止。
            EasyLog.print(TAG, "宿主不可用，排一次退避后重试");
            scheduleRetry();
            return;
        }
        String url = AppConfig.getHostUrl() + "/api/app/search-permission";
        EasyLog.print(TAG, "拉取搜索权限: url=" + url);
        EasyHttp.get((LifecycleOwner) activity)
                .server(new VersionRequestServer())
                .api(new SearchPermissionApi())
                .request(new HttpCallback<SearchPermissionState>(null) {

                    @Override
                    public void onSucceed(SearchPermissionState response) {
                        // 契约不符（空壳 / 缺 global 或 book）按**失败**处理，绝不能当成
                        // 「服务端说不允许」——那会错误禁用搜索且不再重试。
                        if (response == null || !response.isComplete()) {
                            EasyLog.print(TAG, "搜索权限响应不完整（缺字段），按失败处理");
                            onFetchFailed("响应不完整");
                            return;
                        }
                        long now = System.currentTimeMillis();
                        // 写盘失败同样按失败处理（DESIGN §6.1）：不允许「写盘失败但标记成功」，
                        // 否则退避时钟停摆，会在一个坏盘上无限重试。
                        if (!SearchPermissionStore.writeCache(response, now)) {
                            onFetchFailed("写盘失败");
                            return;
                        }
                        // 到这里才是有效答案（含双 false = 角色不命中，是 200 的有效结果）。
                        sState = response;
                        SearchPermissionStore.markFetchSuccess(now);
                        sFetchOk = true;
                        EasyLog.print(TAG, "搜索权限已更新 global=" + response.getGlobal()
                                + " book=" + response.getBook());
                    }

                    @Override
                    public void onFail(Exception e) {
                        // INV-1 静默失败：只记退避时间点，不弹窗、不提示。
                        //
                        // 关于 401：端点对无效/缺失 Bearer 回 401（**不带** `X-Device-Revoked`），
                        // 故 `HandledHttpFailure.isHandled` 判为 false —— 不会走
                        // `DeviceNoticeManager.onDeviceRevoked()` 弹「设备已被禁用」。
                        // 且本回调构造时传 `null`（无 LifecycleOwner），EasyHttp 不会自行弹框。
                        // 只有设备**真被管理员吊销**（401 + 该头）才弹窗，那是应该通知的。
                        onFetchFailed(e.getClass().getSimpleName() + " / " + e.getMessage());
                    }
                });
    }

    /**
     * 统一的失败处理：翻 {@code sFetchOk=false}（fail-closed）+ 推进退避时钟 + 排重试。
     *
     * <p>成功与失败两条路都必须经过它，才不会漏掉「失败不置真」或「失败不重试」其中之一。
     * 调用方负责先打上区分日志。</p>
     */
    private static void onFetchFailed(String reason) {
        long now = System.currentTimeMillis();
        SearchPermissionStore.markFetchFailure(now);
        sFetchOk = false;
        // INV-1 静默失败：只打日志，不弹窗、不 toast。
        EasyLog.print(TAG, "拉取搜索权限失败(已静默，退避 "
                + (SearchPermissionStore.RETRY_INTERVAL_MS / 60000) + " 分钟): " + reason
                + " / url=" + AppConfig.getHostUrl() + "/api/app/search-permission");
        scheduleRetry();
    }

    /**
     * 拉取失败后，安排一次「退避期到点再试」（需求：获取不成功则延后重新获取）。
     * 只靠下次冷启动是不够的：进程可能被用户留在后台几小时甚至过夜，而 30 分钟退避期
     * 早就过了——那条权限判定要等到明天才有机会刷新。重试本身仍走 {@link #fetchAndEvaluate}，
     * 它开头的节流判定会拦住过于频繁的尝试，不会打转。
     */
    private static void scheduleRetry() {
        sRetryScheduler.schedule();
    }
}
