package run.yigou.gxzy.manager;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.base.BaseDialog;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.toast.Toaster;

import java.util.concurrent.atomic.AtomicBoolean;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.data.remote.api.UpdateApi;
import run.yigou.gxzy.data.remote.model.UpdateInfo;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.network.exception.NetworkFailure;
import run.yigou.gxzy.network.server.VersionRequestServer;
import run.yigou.gxzy.dialog.UpdateDialog;

/**
 * 版本升级提示的统一入口（spec §7）。
 *
 * <p>把「写死的版本判断」换成后端驱动：{@code /api/app/version} 下发
 * 最新版本 / 最低版本 / 下载地址 / MD5 / 更新日志，本类据此弹
 * {@link UpdateDialog}。</p>
 *
 * <p>三个触发源：</p>
 * <ul>
 *   <li>用户手动点「检查更新」（{@link #checkManually}）；</li>
 *   <li><b>启动完成时自动检查一次</b>（{@link #registerForegroundCheck}）——
 *       每次启动只查一次，从后台回到前台不重复；</li>
 *   <li>{@code RequestHandler} 收到 HTTP 426（{@link #onVersionTooLow}）——
 *       版本门只作用于 App 内容端点，426 是「低于地板」的信号（ADR-0005/0008）。</li>
 * </ul>
 *
 * <p><b>去重放在「弹窗」这一层，不在「拉取」这一层</b>：App 启动会并发多个内容请求、
 * 可能同时收到多个 426，必须保证只有一个弹窗；但如果在拉取层就把后来的请求丢掉，
 * 426 触发的**强制**检查会被在飞的前台检查挤掉，结果一个弹窗都不弹。拉取是幂等的只读
 * GET，重复几次没有代价。</p>
 *
 * <p><b>职责边界</b>：本类只管版本升级；「设备已被禁用」的提示由
 * {@link DeviceNoticeManager} 处理（{@code 401 + X-Device-Revoked: 1}，ADR-0003）。</p>
 */
public final class UpdateManager {

    private static final String TAG = "UpdateManager";

    /**
     * 「本次进程启动的自动版本检查已经跑过」。
     *
     * <p>需求（2026-10-04）：自动检查**只在启动完成时做一次**，多次启动才检查多次。
     * 进程级而非持久化——冷启动自然归零，正好等于「一次启动一次检查」；同一次启动内
     * 从后台回到前台不再重复检查（{@code onApplicationForeground} 会反复触发）。</p>
     *
     * <p>只闸自动检查：手动「检查更新」({@link #checkManually}) 与 426
     * ({@link #onVersionTooLow}) 各自独立，不受影响。</p>
     */
    private static final LaunchOnceGate sLaunchGate = new LaunchOnceGate();

    /**
     * 弹窗去重：同一时刻只允许一个升级弹窗。弹窗消失后复位。
     */
    private static final AtomicBoolean sDialogShowing = new AtomicBoolean(false);

    /**
     * 在屏的那个弹窗是不是**强制**升级框（2026-10-05 评审新增）。
     *
     * <p>{@link #sDialogShowing} 只说「有弹窗」，不说是哪一种；而阅读入口的阻断决策
     * 必须区分二者：强制弹窗在场 → 拦得住（用户看得见「必须升级」），软提示在场 →
     * 不该拦（那只是个可取消的更新建议，拦了就是无理由地不让读书）。</p>
     */
    private static final AtomicBoolean sDialogShowingIsForce = new AtomicBoolean(false);

    /**
     * 「有个强制升级提示该弹，但当时没有可用宿主」（票据 24 方案 C）。
     *
     * <p>只在**强制**升级时置位：软提示错过了可以等下次检查，而"必须升级"错过就等于
     * 用户被内容门拦死却看不到原因。</p>
     */
    private static final AtomicBoolean sPendingForceUpgrade = new AtomicBoolean(false);

    /**
     * 「用户**取消**了强制升级框」（需求 2）。
     *
     * <p>置位后，本次进程生命周期内**普通导航 / 前台版本检查不再重弹**（满足"取消后不再显示"）；
     * 但**不**抑制「阅读功能」的重弹（阅读是例外，见 {@link #checkForceOnReading}）。</p>
     */
    private static final AtomicBoolean sForceUpgradeDismissed = new AtomicBoolean(false);

    /**
     * 最近一次确认 {@code force=true} 时拿到的升级信息 —— 它<b>同时就是</b>「本进程欠升级」
     * 这个事实的载体（2026-10-05 评审精简：原先另有一个 {@code sForceUpgradeRequired}，
     * 但两者永远同生同灭，任何漏改就成不一致的根源，故合并为这一个字段）。
     *
     * <p>用于「阅读功能重弹」时**不依赖网络**直接弹出（内容可能已本地缓存、甚至整机断网，
     * 但内存里还记着上次后端下发的强制升级信息）。<b>非 null 即表示本进程仍欠升级</b>；
     * 置位后**展示或取消都不清除**，冷启动自然归零。</p>
     */
    private static volatile UpdateInfo sLastForceUpdateInfo = null;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private UpdateManager() {
    }

    /**
     * 把版本检查挂到**应用级**前台回调（spec §7）。
     *
     * <p>{@code onApplicationForeground} 在冷启动（首个 Activity resume）与「从后台回到前台」时
     * 都会触发；需求只要求**启动完成时检查一次**，所以用 {@link LaunchOnceGate}
     * 做一次性闸门：首个前台检查并置位，之后的前台转换只跳过。</p>
     *
     * <p>由 {@code AppApplication} 在启动时调用一次。弹窗由 {@code sDialogShowing} 去重（**拉取层不去重**，否则 426 触发的强制检查会被挤掉，见 spec §7.1 坑 3），
     * 最多一个弹窗。</p>
     */
    public static void registerForegroundCheck() {
        EasyLog.print(TAG, "版本检查已挂到应用级前台回调（spec §7），本次启动只检查一次");
        ActivityManager.getInstance().registerApplicationLifecycleCallback(
                new ActivityManager.ApplicationLifecycleCallback() {
                    @Override
                    public void onApplicationCreate(Activity activity) {
                        // 冷启动也走 onApplicationForeground，这里无需处理。
                    }

                    @Override
                    public void onApplicationDestroy(Activity activity) {
                    }

                    @Override
                    public void onApplicationBackground(Activity activity) {
                    }

                    @Override
                    public void onApplicationForeground(Activity activity) {
                        tryLaunchCheck(activity);
                    }
                });
        // 补弹的挂载点（票据 24 方案 C）：上面那个回调只在**首个 Activity resume** 时触发一次，
        // 冷启动 Splash→Home 的切换不会再触发。而启动页恰恰不能承载 Dialog，
        // 所以必须额外在**每个** Activity resume 时补一次，否则冷启动的强制升级提示会丢。
        // 该回调同时充当「首次检查」的兜底触发点：实测存在整个进程只有一次
        // onApplicationForeground、且那次宿主不是 LifecycleOwner 的情况，只靠它会一次都不检查。
        // 闸门空了就把「待补弹的强制升级」也试一次（2026-10-05 评审修复）。
        // 为什么必须订阅闸门而不是只靠 resume：公告框在屏时**不会**再有 resume 事件，
        // 而强制升级走的是 sPendingForceUpgrade + resume 补弹那条路 → 公告关掉后
        // 没人再触发它，用户被 426 内容门拦死却看不到提示（实测复现）。
        AppModalGate.addOnReleasedListener(() -> {
            Activity host = ForegroundActivities.topIfUsable();
            if (host != null) {
                showPendingDialogIfAny(host);
            }
        });
        ActivityManager.getInstance().registerActivityResumeCallback(
                new ActivityManager.ActivityResumeCallback() {
                    @Override
                    public void onActivityResumed(Activity activity) {
                        tryLaunchCheck(activity);
                        showPendingDialogIfAny(activity);
                    }
                });
    }

    /**
     * 「本次启动检查一次」的唯一入口：宿主可用**且**闸门未置位时才检查。
     *
     * <p>⚠️ 顺序不能反（2026-10-05 adb 实测）：原先先 CAS 置位、再发现宿主不是
     * {@code LifecycleOwner}（实测日志「宿主不是 LifecycleOwner，跳过版本检查」），
     * 「本次启动唯一一次」机会被白白烧掉 → 本次启动永远不再检查版本。
     * 冷启动后每次 App 启动都漏检一次，直到用户碰巧再回前台才补上。</p>
     */
    private static void tryLaunchCheck(Activity activity) {
        // 顺序与语义都封装在 LaunchOnceGate：先判宿主可用、再置位，失败不消耗机会。
        sLaunchGate.runOnceIfHostUsable(activity, () -> checkOnForeground(activity));
    }

    static void showPendingDialogIfAny(Activity activity) {
        // 需求 2：用户已取消强制升级则不再补弹（仅抑制普通导航/前台，不抑制阅读）。
        if (!sPendingForceUpgrade.get() || sForceUpgradeDismissed.get()
                || !ForegroundActivities.isUsableHost(activity)) {
            return;
        }
        EasyLog.print(TAG, "宿主已就绪，补弹之前因无可用宿主而跳过的强制升级提示");
        // 必须延后一拍：onResume 触发时页面往往还没真正可见（冷启动的 HomeActivity 此刻
        // 仍在启动流程里，上一个 Activity 也还没走 onDestroy）。此时立刻弹窗，Dialog 会
        // 在宿主尚未稳定时被连带丢弃——实测这正是「补弹触发了、屏幕上没有」的第二个原因。
        // 投到主线程队列末尾，等本轮 resume 走完再弹。
        MAIN_HANDLER.post(() -> {
            UpdateInfo info = sLastForceUpdateInfo;
            if (!sPendingForceUpgrade.get() || info == null) {
                return;
            }
            // 直接用手上已有的强制升级信息弹，**不重新拉取**：
            //   ① 重新拉取会让「每次 Activity resume 都发一次网络请求」，破坏「一次启动只检查一次」；
            //   ② 服务端把每次 `/api/app/version` 都记成一次登录，重拉会把「登录次数」灌水。
            // 这份信息在判定 force=true 时就已经存进 `sLastForceUpdateInfo`（需求 2 为
            // 「阅读重弹不依赖网络」引入的同一份缓存），这里正好复用，也省掉一次往返。
            showUpdateDialog(activity, info, true);
        });
    }

    /** 设置页「检查更新」：拉取后无更新则 toast 提示已是最新。 */
    public static void checkManually(Activity activity) {
        fetchAndShow(activity, Trigger.MANUAL);
    }

    /**
     * 进入前台自动检查版本（spec §7）。
     *
     * <p>静默：无更新或请求失败都不给用户任何打扰；只有确实要提示时才弹窗。</p>
     */
    public static void checkOnForeground(Activity activity) {
        fetchAndShow(activity, Trigger.FOREGROUND);
    }

    /**
     * 收到 HTTP 426：拉版本信息并弹**强制**升级框。
     *
     * <p>可能在 OkHttp 线程被调用，统一切回主线程做 UI。</p>
     */
    public static void onVersionTooLow() {
        EasyLog.print(TAG, "收到 HTTP 426（版本门），拉取 /api/app/version 并弹强制升级");
        MAIN_HANDLER.post(() -> fetchAndShow(ForegroundActivities.topIfUsable(), Trigger.FORCE_426));
    }

    /**
     * 取「后端说本机欠升级」这条信息：<b>内存镜像优先，镜像空则回落落盘</b>。
     *
     * <p>2026-10-05（ADR-0001 追加决议）新增的回落分支是本轮改造的核心：原先
     * {@code checkForceOnReading} 只读两个静态字段，而它们<b>仅在版本检查的成功回调里</b>置位，
     * 于是「冷启动时离线 / 版本检查失败 / 用户抢在版本检查返回前就点书」这三种情况下
     * 字段为空 → 阅读入口不重弹 → 用户点了书却没有任何提示，正是需求 2 要避免的。
     * 落盘记录正好补上这个窗口。</p>
     *
     * <p>命中落盘时会顺手回填内存镜像，避免同一进程内反复读盘。
     * 落盘判据内部已做失效检查（{@code PendingForceUpgradeStore.read()}），过期记录会被清除
     * 并按「无记录」返回。</p>
     *
     * @return 欠升级信息；无记录 / 已失效 / 落盘读取失败时返回 {@code null}
     */
    private static UpdateInfo forceUpgradeInfo() {
        UpdateInfo cached = sLastForceUpdateInfo;
        if (cached != null) {
            return cached;
        }
        UpdateInfo persisted = PendingForceUpgradeStore.read();
        if (persisted != null) {
            sLastForceUpdateInfo = persisted;
            EasyLog.print(TAG, "内存无欠升级记录，改用落盘记录（跨进程/离线仍可拦阅读）");
        }
        return persisted;
    }

    /**
     * 「阅读功能」入口的强制升级重弹（需求 2，例外项）。
     *
     * <p>在用户**发起阅读意图**（点开一本书/章节）时调用，与本次是否发网络请求、内容是否来自
     * 本地缓存**无关**：只要还记着「有待强制升级」（内存镜像或落盘记录，见
     * {@link #forceUpgradeInfo()}），就直接用那份信息弹窗，**不重新拉取、不依赖网络**。
     * 这样即便内容已缓存命中、根本不发内容请求（旧实现靠 426 触发，会漏弹），也能保证点阅读就重弹。</p>
     *
     * <p>该重弹**不受** {@link #sForceUpgradeDismissed} 抑制（取消强制升级后逛其他功能不弹，但点阅读仍弹）。</p>
     *
     * @param activity 当前可用宿主（阅读发起时的 Activity）
     * @return {@code true} 表示已弹出强制升级框，调用方**必须放弃本次阅读导航**——新的阅读页
     *         （全屏 Activity）会把弹窗盖住，用户根本看不到提示；{@code false} 表示无待升级项，可正常进入阅读。
     */
    public static boolean checkForceOnReading(Activity activity) {
        if (activity == null) {
            return false;
        }
        // 内存镜像空时回落读盘（2026-10-05）：这是「冷启动离线点书仍能拦住」的来源。
        UpdateInfo info = forceUpgradeInfo();
        if (info == null) {
            return false;
        }
        if (!ForegroundActivities.isUsableHost(activity)) {
            return false;
        }
        EasyLog.print(TAG, "阅读功能：基于已记录的强制升级信息（内存或落盘），主动重弹升级框（不依赖网络/缓存）");
        // 只有弹窗**确实在屏上**才阻断阅读：谎报成功会让用户既读不了、又看不到任何提示。
        // 弹不出来时（如 show 抛异常）放行，让内容请求自己撞 426 → 那条路径有兜底 toast。
        if (!showUpdateDialog(activity, info, true)) {
            EasyLog.print(TAG, "阅读入口的强制升级弹窗未能显示，放行本次阅读（由内容门兜底提示）");
            return false;
        }
        // 弹窗已弹出：调用方必须放弃本次阅读导航，否则阅读页会把弹窗盖住、用户看不到提示。
        return true;
    }

    /** 触发来源：比两个 boolean 更不容易传错顺序。 */
    private enum Trigger {
        /** 进入前台自动检查：静默，无更新或失败都不打扰用户。 */
        FOREGROUND,
        /** 用户手动点「检查更新」：无更新/失败都给 toast 反馈。 */
        MANUAL,
        /** 内容端点返回 426：本机已低于后端地板，必须升级。 */
        FORCE_426,
    }

    /**
     * 拉取 {@code /api/app/version} 并按结果弹窗。
     *
     * @param trigger 触发来源；比两个 boolean 更不容易传错顺序
     */
    private static void fetchAndShow(Activity activity, Trigger trigger) {
        if (activity == null) {
            return;
        }
        final boolean forceUpgrade = trigger == Trigger.FORCE_426;
        final boolean manual = trigger == Trigger.MANUAL;
        // 这里**不**做「已有弹窗就跳过本次检查」的去重：那是**拉取层**去重，会把 426 触发的
        // 强制检查挤掉（软弹窗在场时到达的 426 就被丢弃，用户被内容门拦死却看不到提示）。
        // 去重只放在弹窗层 showUpdateDialog；拉取是幂等只读 GET，重复几次没有代价（spec §7.1 坑 3）。
        // 本项目的 Activity 都继承 AppActivity → BaseActivity → AppCompatActivity，
        // 即都是 LifecycleOwner；仍显式判断一次，避免把非生命周期宿主交给 EasyHttp。
        if (!(activity instanceof LifecycleOwner)) {
            EasyLog.print(TAG, "宿主不是 LifecycleOwner，跳过版本检查");
            return;
        }
        EasyLog.print(TAG, "检查版本: forceUpgrade=" + forceUpgrade + ", manual=" + manual);
        EasyHttp.get((LifecycleOwner) activity)
                .server(new VersionRequestServer())
                .api(new UpdateApi())
                .request(new HttpCallback<UpdateInfo>(null) {

                    @Override
                    public void onSucceed(UpdateInfo info) {
                        int currentVersionCode = AppConfig.getVersionCode();
                        if (info == null) {
                            EasyLog.print(TAG, "版本接口返回空");
                            if (manual) {
                                Toaster.show(R.string.update_check_failed);
                            }
                            return;
                        }
                        // 三种情况都要弹，缺任一条都会让用户「被拦却看不到提示」：
                        //   ① forceUpgrade —— 426 触发，本机已低于后端地板；
                        //   ② info.isForce() —— 后端判本机低于硬地板（前台检查也能发现）；
                        //   ③ hasUpdate —— 有更新的版本可装。
                        boolean backendForce = info.isForce();
                        // 2026-10-05（ADR-0001 追加决议）：后端本次**没**判强制，就意味着
                        // 「本机已不在硬地板之下」——这是落盘欠升级记录的第二道失效保险，
                        // 必须在这里清。
                        //
                        // ⚠️ 判据刻意**不用**「三者都不成立」：那会让「后端 force=false 但有软更新」
                        // 这种最常见的情形**不清**记录 —— 后端明明已解除强制，App 却还记着欠升级，
                        // 于是点阅读继续弹框（初版就是这个问题）。
                        if (!backendForce) {
                            PendingForceUpgradeStore.clear();
                            sLastForceUpdateInfo = null;
                        }
                        if (!forceUpgrade && !backendForce
                                && !info.hasUpdate(currentVersionCode)) {
                            EasyLog.print(TAG, "当前已是最新版本: " + currentVersionCode);
                            // 已是最新：连「用户已取消」也一并复位（下次有真更新时他应能再看到）。
                            sForceUpgradeDismissed.set(false);
                            if (manual) {
                                Toaster.show(R.string.update_no_update);
                            }
                            return;
                        }
                        // force 直接采信后端：地板违规后端恒 true，地板之上的灰度规则可软可硬。
                        // 不能用 `current < minVersionCode` 自行推断，那会把软提示硬化成硬阻（ADR-0008 §5）。
                        boolean force = forceUpgrade || backendForce;
                        EasyLog.print(TAG, "升级提示: latest=" + info.getLatestVersionName()
                                + ", minVersionCode=" + info.getMinVersionCode()
                                + ", force=" + force + ", current=" + currentVersionCode);
                        if (force) {
                            // 记录「本进程欠升级」这件事（需求 2：阅读重弹不依赖网络）。
                            sLastForceUpdateInfo = info;
                            // 把「后端说本机欠升级」这个客观事实**落盘**（2026-10-05 决议）。
                            // 它与「用户已取消」是两条独立记忆 —— 后者仍只在内存。
                            // 落盘后，冷启动离线 / 版本检查失败 / 用户抢在点书前返回 这三种情况下
                            // 阅读入口也拦得住（原先标记只在成功回调里置位，会漏）。
                            //
                            // ⚠️ 落盘**只认后端的 `force`**：`write()` 内部有 `!info.isForce()` 守卫，
                            // 所以「426 触发但后端说不是强更」这种矛盾组合下**不会**落盘 ——
                            // 那时后端并未认定欠升级，不该拦。只留内存标记等本次进程用完。
                            PendingForceUpgradeStore.write(info);
                        }
                        // 需求 2：取消强制升级后，本次进程生命周期内**任何非阅读入口**都不再重弹，
                        // 426 也不例外。
                        //
                        // ⚠️ 曾经只抑制 FOREGROUND，理由是「FORCE_426 是内容门，让它弹才看得到原因」。
                        // 但需求原文是「点取消后，再选点其他功能，不允许重新弹窗升级」——426 由
                        // **所有**内容端点下发（middleware 的版本门覆盖整个 /api/AppBookRequest/*），
                        // 取消后点「发现」tab 拉 GetNav 就会 426 → 又弹，等于取消无效。
                        // 而「点了阅读要看到提示」这条已由 {@link #checkForceOnReading} 在阅读意图
                        // 入口独立把关（不依赖 426、不依赖网络），所以抑制 FORCE_426 不会让用户
                        // 看不到提示，反而消除了「逛别的功能反复弹」的噪音。
                        if (ForceUpgradeSuppression.shouldSuppressAfterCancel(
                                force, sForceUpgradeDismissed.get(), manual)) {
                            EasyLog.print(TAG, "强制升级已被用户取消，本次进程生命周期内不再重弹"
                                    + "（手动「检查更新」与阅读功能仍会弹）");
                            // 静默 return 会让这次内容请求**失败得毫无提示**：
                            // 426 的异常照旧抛给调用方，而 `HandledHttpFailure.shouldSilence`
                            // 又让通用错误 toast 静默（票据 21）→ 用户点书失败却不知原因。
                            // 故必须给一句「为什么不能看」，这是需求 2「不重新弹窗」的必要配套，
                            // 不是重新弹窗（只是一句 toast）。
                            if (trigger == Trigger.FORCE_426) {
                                Toaster.show(R.string.update_force_required_to_read);
                            }
                            return;
                        }
                        showUpdateDialog(activity, info, force);
                    }

                    @Override
                    public void onFail(Exception e) {
                        // 失败原因必须能从 logcat 一眼看出来。只打 `e.getMessage()` 时，
                        // 连接类异常往往只有一句通用文案（甚至空串），排查时分不清
                        // 「后端没有这个接口(404)」「地址不可达」「超时」——实测为此白查过一轮。
                        // 故连异常类型与**实际请求地址**一起打出：地址能立刻暴露「包指向了别的后端」
                        // 这类问题（构建时 serverType 选错就会这样，见项目技能）。
                        EasyLog.print(TAG, "检查版本失败: " + e.getClass().getSimpleName()
                                + " / " + e.getMessage()
                                + " / url=" + AppConfig.getHostUrl() + "/api/app/version");
                        if (manual) {
                            // 「连不上」与「服务器出错」给不同提示：前者让用户去查网络/后端地址，
                            // 后者只要等一会。共用一句笼统文案时用户只能靠猜（也无法从截图判断）。
                            Toaster.show(NetworkFailure.isNetworkFailure(e)
                                    ? R.string.update_check_failed_network
                                    : R.string.update_check_failed);
                        }
                    }
                });
    }

    /**
     * 真的把升级弹窗显示出来了吗？
     *
     * <p>调用方 {@link #checkForceOnReading} 依赖这个返回值决定是否阻断阅读导航：
     * 只有「弹窗确实在屏上」才配阻断。若这里谎报成功，调用方会放弃导航，用户就变成
     * **既读不了、也没看到任何提示**——比不阻断更糟。</p>
     */
    private static boolean showUpdateDialog(Activity activity, UpdateInfo info, boolean force) {
        if (!ForegroundActivities.isUsableHost(activity)) {
            // 强制升级错过 = 用户被内容门拦死却看不到原因，必须记下来补弹（票据 24）。
            // 软提示错过可以等下次检查，不记。需求 2：用户已取消则不再补弹。
            if (force && !sForceUpgradeDismissed.get()) {
                sPendingForceUpgrade.set(true);
            }
            EasyLog.print(TAG, "Activity 已不可用，跳过升级弹窗");
            return false;
        }
        // 真正的去重点：并发的多个 426 里只有一个能弹出来。
        // 走**全局**模态闸门而非本类的 sDialogShowing：公告框也在这条路上排队，
        // 两边各用各的标志会同时弹出来叠在一起（DESIGN §6.5，升级框优先）。
        if (!AppModalGate.tryAcquire()) {
            // ⚠️ 闸门被**公告框**占着时，强制升级不能就这么丢掉（2026-10-05 评审修复）。
            // 原实现直接 return，而公告框既不置 sPendingForceUpgrade、也不重排 →
            // 「公告先抢到闸门」时本次启动的强制升级**永久丢失**：用户被 426 内容门
            // 拦死，却只看到一个可关掉的公告框，关掉之后书也读不了 ——
            // 正是本 ADR 反复要消除的「既读不了又没提示」。
            //
            // 修法：强制升级**排队**等闸门空出来（升级框优先，DESIGN §6.5），并如实
            // 返回「提示已送达」——它马上就会弹，调用方据此阻断阅读导航是对的。
            // 软提示（force=false）不排队：错过可以等下次版本检查，不值得占着队列。
            if (force && !sForceUpgradeDismissed.get()) {
                EasyLog.print(TAG, "公告框在屏，强制升级排队等待（DESIGN §6.5 升级框优先）");
                AppModalGate.runWhenClear(() -> showUpdateDialog(activity, info, true));
                // 提示马上会送达（公告框一关就弹），所以如实报 true 让调用方阻断阅读导航。
                return true;
            }
            // 用户已取消过强制升级（需求 2），或这只是软提示（force=false）：
            // 本次进程内不排队、不阻断阅读 —— 软提示错过可以等下次版本检查。
            // ⚠️ 此刻若返回 true 就等于「拦住了却不给提示」，正是要消除的那个失败模式。
            EasyLog.print(TAG, force
                    ? "公告框在屏，且用户已取消强制升级：不排队、不阻断"
                    : "公告框在屏，软升级提示跳过（下次检查再弹）");
            // ⚠️ 只有「在屏的是**强制**弹窗」才算提示已送达（2026-10-05 评审修正）。
            // 软提示在场时也返回 true 会让 `checkForceOnReading` 谎报成功 → 调用方放弃
            // 阅读导航，可用户眼前只是一个可取消的软提示，关掉之后书也读不了 ——
            // 正是本 ADR 反复要消除的「既读不了又没提示」。
            return sDialogShowingIsForce.get();
        }
        sDialogShowing.set(true);
        try {
            UpdateDialog.Builder builder = new UpdateDialog.Builder(activity)
                    .setVersionName(info.getLatestVersionName())
                    .setForceUpdate(force)
                    .setUpdateLog(info.getUpdateLog())
                    .setDownloadUrl(info.getDownloadUrl())
                    .setFileMd5(info.getMd5());
            // 需求 2：强制升级框被「取消」时，记录 dismissed，抑制本次进程生命周期内的
            // 前台/普通导航/426 重弹（阅读功能仍会因 sLastForceUpdateInfo 重弹，不受此标志影响）。
            if (force) {
                builder.setForceCancelRunnable(() -> {
                    sForceUpgradeDismissed.set(true);
                    EasyLog.print(TAG, "用户取消强制升级，本次进程生命周期内抑制前台/普通导航/426 重弹"
                            + "（阅读功能仍会重弹）");
                });
            }
            // 弹窗消失后复位，下一次检查才能再弹。
            builder.addOnDismissListener(new BaseDialog.OnDismissListener() {
                @Override
                public void onDismiss(BaseDialog dialog) {
                    sDialogShowing.set(false);
                    sDialogShowingIsForce.set(false);
                    // 释放全局闸门，并让排队中的公告框补弹（DESIGN §6.5：升级框优先）。
                    AppModalGate.release();
                }
            });
            builder.show();
            // 记下在屏的是哪种弹窗：只有强制弹窗才能让阅读入口有理由阻断导航。
            sDialogShowingIsForce.set(force);
            // 弹窗真的展示出来了，待弹的强制升级提示才算送达。
            // 不在这里清的话：宿主随后失效导致 Dialog 跟着消失时，标志仍在，
            // 下次 resume 会**再弹一次**同一个提示（票据 24）。
            sPendingForceUpgrade.set(false);
            return true;
        } catch (RuntimeException error) {
            // show() 可能因宿主状态异常（如 onSaveInstanceState 之后再提交事务）抛出。
            // 必须释放标志，否则之后所有版本检查都会被静默跳过。
            sDialogShowing.set(false);
            sDialogShowingIsForce.set(false);
            // 闸门也必须还回去：否则公告框会永远排队等一个已经关闭的弹窗。
            AppModalGate.release();
            // 展示失败同样要保留「待补弹」，否则强制升级提示就此丢失。
            if (force) {
                sPendingForceUpgrade.set(true);
            }
            EasyLog.print(TAG, "升级弹窗展示失败: " + error);
            return false;
        }
    }
}
