package run.yigou.gxzy.manager.mingci;

import android.app.Activity;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;

import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.data.remote.api.mingci.MingCiPermissionApi;
import run.yigou.gxzy.data.remote.model.MingCiPermissionState;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.network.server.VersionRequestServer;
import run.yigou.gxzy.manager.concurrency.RetryScheduler;
import run.yigou.gxzy.manager.launch.LaunchOnceGate;
import run.yigou.gxzy.manager.lifecycle.ActivityManager;
import run.yigou.gxzy.manager.lifecycle.ForegroundActivities;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 名词解释查看权限的拉取与判定（G3 全局单开关 → 一个布尔 {@code allowed}）。
 *
 * <p>严格镜像 {@link run.yigou.gxzy.manager.search.SearchPermissionManager}：结构、不变量、接线点
 * 完全同构，仅将「两个搜索入口布尔」收敛为「一个名词解释布尔」。设计依据在 <b>microfeed 仓</b>：
 * {@code .scratch/search-permission/DESIGN.md}（搜索权限蓝图），本特性逐文件镜像它。</p>
 *
 * <p>四条硬性不变量（同 search-permission DESIGN §2）：</p>
 * <ol>
 *   <li><b>静默失败</b>（INV-1）：拉取失败（网络 / 解析 / 字段缺失 / 401 / 403 / 429 / 5xx）
 *       → 只打 log，不弹窗、不 toast。失败只让本次启动的名词解释保持关闭，并在 30 分钟后退避重试。</li>
 *   <li><b>默认关闭</b>（INV-2）：{@link #isAllowed} 先判进程内 {@code sFetchOk}，为假
 *       直接返回 false——即使 MMKV 里有昨天成功的缓存也不读。冷启动、尚未拉到、或上次拉取
 *       失败时，名词解释一律禁用（fail-closed）。</li>
 *   <li><b>一次启动一拉</b>（INV-3）：{@link LaunchOnceGate} 进程级闸门，回到前台不重复拉。</li>
 *   <li><b>失败退避</b>（INV-5）：失败后 30 分钟再试一次，不疯狂重试。</li>
 *   <li><b>懒加载单飞</b>（INV-4）：权限确认（allowed=true）后至多触发一次补载监听器，
 *       防止冷启动期间（isAllowed=false）未装载的名词列表在权限到达后重复全量加载。</li>
 * </ol>
 */
public final class MingCiPermissionManager {

    private static final String TAG = "MingCiPermissionManager";

    /**
     * 进程内「本次启动是否已成功拉到权限判定」。默认 false = 名词解释默认关闭（INV-2）。
     * 只在 {@link #fetchAndEvaluate} 成功落盘后翻 true；任何失败都翻回 false（fail-closed）。
     */
    public static boolean sFetchOk = false;

    /**
     * 最近一次成功拉到的权限状态（进程内持有，网关直接读它，不碰磁盘，见
     * {@link MingCiPermissionStore} 的说明）。
     */
    public static MingCiPermissionState sState = null;

    /** 进程级「本次启动已拉过」闸门（INV-3）。 */
    private static final LaunchOnceGate sLaunchGate = new LaunchOnceGate();

    /**
     * 退避重试的排程器（单飞 + 主线程定时），与搜索/公告刷新共用 {@link RetryScheduler}。
     *
     * <p>到点执行的就是 {@link #fetchAndEvaluate} 本身，它开头的节流判定会拦住过于频繁的
     * 尝试，不会打转。</p>
     */
    private static final RetryScheduler sRetryScheduler = new RetryScheduler(
            MingCiPermissionStore.RETRY_INTERVAL_MS,
            () -> fetchAndEvaluate(ForegroundActivities.topIfUsable()));

    /** 懒加载监听器：权限确认（allowed=true）后补载名词数据列表（由 AppDataManager 注册，T08）。 */
    private static final CopyOnWriteArrayList<Runnable> sOnAllowedListeners = new CopyOnWriteArrayList<>();

    /** 懒加载单飞标志（INV-4）：进程内 allowed=true→补载 恰好触发一次。 */
    private static final AtomicBoolean sLazyLoadTriggered = new AtomicBoolean(false);

    /** 「权限被回收（allowed=false）」监听器：用于清空已装载的名词底表，防止残留旧数据。 */
    private static final CopyOnWriteArrayList<Runnable> sOnDeniedListeners = new CopyOnWriteArrayList<>();

    private MingCiPermissionManager() {
    }

    /** 进程级「是否已成功拉取过」（仅供诊断与单测）。 */
    static boolean isFetchOk() {
        return sFetchOk;
    }

    /**
     * 名词解释查看闸门：当前账号是否被允许查看名词解释（底层名词数据列表的加载开关）。
     *
     * <p>顺序即 INV-2：先判 {@code sFetchOk}（进程内是否成功拉到过），为假直接 false、
     * <b>不读</b>任何缓存；为真再取进程内 {@code sState.allowed}。任一缺失都返回 false。
     * 读取走 {@code getAllowed()} 而非直接取字段：字段是包装类型，缺字段时必须落到
     * 「否」而不是拆箱 NPE。</p>
     */
    public static boolean isAllowed() {
        if (!sFetchOk) {
            return false;
        }
        if (sState == null) {
            return false;
        }
        return sState.getAllowed();
    }

    /** 注册「权限确认后补载名词列表」的监听器（由 AppDataManager 初始化时调用，T08）。 */
    public static void addOnAllowedListener(Runnable listener) {
        if (listener != null) {
            sOnAllowedListeners.add(listener);
        }
    }

    /** 注册「权限被回收（allowed=false）」的监听器（由 AppDataManager 注册，用于清空名词底表）。 */
    public static void addOnDeniedListener(Runnable listener) {
        if (listener != null) {
            sOnDeniedListeners.add(listener);
        }
    }

    /**
     * 把名词解释权限拉取挂到<b>应用级</b>前台回调，与搜索权限同一套路。由 {@code AppApplication}
     * 在启动时调用一次（T09）。
     */
    public static void registerForegroundCheck() {
        EasyLog.print(TAG, "名词解释权限拉取已挂到应用级前台回调，本次启动只拉一次");
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
        // 每个 Activity resume 必定带可用宿主，作为首个前台回调的兜底（同 SearchPermissionManager）。
        ActivityManager.getInstance().registerActivityResumeCallback(
                new ActivityManager.ActivityResumeCallback() {
                    @Override
                    public void onActivityResumed(Activity activity) {
                        tryLaunchFetch(activity);
                    }
                });
    }

    /**
     * 「本次启动拉一次」的唯一入口：宿主可用**且**闸门未置位时才拉（同 SearchPermissionManager：
     * 先判宿主、再置位，失败不消耗机会）。
     */
    private static void tryLaunchFetch(Activity activity) {
        if (sLaunchGate.runOnceIfHostUsable(activity, () -> onLaunchReady(activity))) {
            EasyLog.print(TAG, "本次启动首次拉取名词解释权限");
        }
    }

    private static void onLaunchReady(Activity activity) {
        fetchAndEvaluate(activity);
    }

    /**
     * 拉取名词解释权限：按节流规则决定是否请求，成功后写缓存 + 翻 sFetchOk，失败静默 + 退避，
     * 权限确认后触发懒加载补载（INV-4）。
     */
    static void fetchAndEvaluate(Activity activity) {
        if (!MingCiPermissionStore.shouldFetchNow()) {
            // 处于失败退避期。若这是重试链自己触发的，直接返回会让整条链断在这里，
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
        String url = AppConfig.getHostUrl() + "/api/app/mingci-permission";
        EasyLog.print(TAG, "拉取名词解释权限: url=" + url);
        EasyHttp.get((LifecycleOwner) activity)
                .server(new VersionRequestServer())
                .api(new MingCiPermissionApi())
                .request(new HttpCallback<MingCiPermissionState>(null) {

                    @Override
                    public void onSucceed(MingCiPermissionState response) {
                        // 契约不符（空壳 / 缺 allowed）按**失败**处理，绝不能当成
                        // 「服务端说不允许」——那会错误禁用名词解释且不再重试。
                        if (response == null || !response.isComplete()) {
                            EasyLog.print(TAG, "名词解释权限响应不完整（缺字段），按失败处理");
                            onFetchFailed("响应不完整");
                            return;
                        }
                        long now = System.currentTimeMillis();
                        // 写盘失败同样按失败处理：不允许「写盘失败但标记成功」。
                        if (!MingCiPermissionStore.writeCache(response, now)) {
                            onFetchFailed("写盘失败");
                            return;
                        }
                        // 到这里才是有效答案（含 allowed=false = 角色不命中，是 200 的有效结果）。
                        sState = response;
                        MingCiPermissionStore.markFetchSuccess(now);
                        sFetchOk = true;
                        EasyLog.print(TAG, "名词解释权限已更新 allowed=" + response.getAllowed());
                        if (response.getAllowed()) {
                            // 权限确认（allowed=true）后，以单飞标志守卫触发一次懒加载补载（INV-4）。
                            if (sLazyLoadTriggered.compareAndSet(false, true)) {
                                for (Runnable listener : sOnAllowedListeners) {
                                    listener.run();
                                }
                            }
                        } else {
                            // 角色不命中（有效答案）：复位懒加载单飞，使将来翻回 true 能重新补载；
                            // 并通知订阅方清空底表，防止残留旧名词数据（fail-closed）。
                            sLazyLoadTriggered.set(false);
                            for (Runnable listener : sOnDeniedListeners) {
                                listener.run();
                            }
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        // INV-1 静默失败：只记退避时间点，不弹窗、不提示。
                        onFetchFailed(e.getClass().getSimpleName() + " / " + e.getMessage());
                    }
                });
    }

    /**
     * 统一的失败处理：翻 {@code sFetchOk=false}（fail-closed）+ 推进退避时钟 + 复位懒加载单飞 + 排重试。
     *
     * <p>成功与失败两条路都必须经过它，才不会漏掉「失败不置真」或「失败不重试」其中之一。
     * 调用方负责先打上区分日志。</p>
     */
    private static void onFetchFailed(String reason) {
        long now = System.currentTimeMillis();
        MingCiPermissionStore.markFetchFailure(now);
        sFetchOk = false;
        // 权限翻转 false：复位懒加载单飞，防止残留旧数据（待再次翻转回 true 能重新补载）。
        sLazyLoadTriggered.set(false);
        // INV-1 静默失败：只打日志，不弹窗、不 toast。
        EasyLog.print(TAG, "拉取名词解释权限失败(已静默，退避 "
                + (MingCiPermissionStore.RETRY_INTERVAL_MS / 60000) + " 分钟): " + reason
                + " / url=" + AppConfig.getHostUrl() + "/api/app/mingci-permission");
        scheduleRetry();
    }

    /**
     * 拉取失败后，安排一次「退避期到点再试」。只靠下次冷启动是不够的：进程可能被用户留在后台
     * 几小时甚至过夜，而 30 分钟退避期早就过了——那条权限判定要等到明天才有机会刷新。重试本身
     * 仍走 {@link #fetchAndEvaluate}，它开头的节流判定会拦住过于频繁的尝试，不会打转。
     */
    private static void scheduleRetry() {
        sRetryScheduler.schedule();
    }
}
