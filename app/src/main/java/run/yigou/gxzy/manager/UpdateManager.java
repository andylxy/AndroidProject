package run.yigou.gxzy.manager;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
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
import run.yigou.gxzy.network.server.VersionRequestServer;
import run.yigou.gxzy.ui.dialog.UpdateDialog;

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
 *   <li>进入前台自动检查（{@link #checkOnForeground}）；</li>
 *   <li>{@code RequestHandler} 收到 HTTP 426（{@link #onVersionTooLow}）——
 *       版本门只作用于 App 内容端点，426 是「低于地板」的信号（ADR-0005/0008）。</li>
 * </ul>
 *
 * <p><b>去重放在「弹窗」这一层，不在「拉取」这一层</b>：App 启动会并发多个内容请求、
 * 可能同时收到多个 426，必须保证只有一个弹窗；但如果在拉取层就把后来的请求丢掉，
 * 426 触发的**强制**检查会被在飞的前台检查挤掉，结果一个弹窗都不弹。拉取是幂等的只读
 * GET，重复几次没有代价。</p>
 *
 * <p><b>不在此类做设备禁用提示之外的事</b>：{@link #onDeviceRevoked} 处理
 * {@code 401 + X-Device-Revoked: 1}（ADR-0003）。</p>
 */
public final class UpdateManager {

    private static final String TAG = "UpdateManager";

    /** 弹窗去重：同一时刻只允许一个升级弹窗。弹窗消失后复位。 */
    private static final AtomicBoolean sDialogShowing = new AtomicBoolean(false);

    /** 设备禁用提示只弹一次（该设备的每个请求都会 401，不能每次都弹）。 */
    private static final AtomicBoolean sDeviceRevokedShown = new AtomicBoolean(false);

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private UpdateManager() {
    }

    /** 设置页「检查更新」：拉取后无更新则 toast 提示已是最新。 */
    public static void checkManually(Activity activity) {
        fetchAndShow(activity, false, true);
    }

    /**
     * 进入前台自动检查版本（spec §7）。
     *
     * <p>静默：无更新或请求失败都不给用户任何打扰；只有确实要提示时才弹窗。</p>
     */
    public static void checkOnForeground(Activity activity) {
        fetchAndShow(activity, false, false);
    }

    /**
     * 收到 HTTP 426：拉版本信息并弹**强制**升级框。
     *
     * <p>可能在 OkHttp 线程被调用，统一切回主线程做 UI。</p>
     */
    public static void onVersionTooLow() {
        EasyLog.print(TAG, "收到 HTTP 426（版本门），拉取 /api/app/version 并弹强制升级");
        MAIN_HANDLER.post(() -> fetchAndShow(topActivity(), true, false));
    }

    /**
     * 收到 {@code 401 + X-Device-Revoked: 1}：提示「此设备已被禁用」，**不**跳登录。
     *
     * <p>若与「会话过期」一样去弹登录框，会陷入「登录成功 → 又被 401」的死循环
     * （ADR-0003）。</p>
     */
    public static void onDeviceRevoked() {
        MAIN_HANDLER.post(() -> {
            if (sDeviceRevokedShown.get()) {
                return;
            }
            Activity activity = topActivity();
            if (activity == null) {
                // 没有可用的前台 Activity：**不消费**标志，等下一次请求再提示，
                // 否则这一台设备的禁用提示就永远不会出现。
                EasyLog.print(TAG, "设备已禁用，但当前没有可用的 Activity，稍后重试提示");
                return;
            }
            if (!sDeviceRevokedShown.compareAndSet(false, true)) {
                return;
            }
            EasyLog.print(TAG, "收到 401 + X-Device-Revoked，提示设备已被禁用（不跳登录）");
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.device_revoked_title)
                    .setMessage(R.string.device_revoked_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.device_revoked_confirm, (dialog, which) ->
                            ActivityManager.getInstance().finishAllActivities())
                    .show();
        });
    }

    /** 当前可用的前台 Activity；没有或正在销毁时返回 null。 */
    private static Activity topActivity() {
        Activity activity = ActivityManager.getInstance().getTopActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        return activity;
    }

    /**
     * 拉取 {@code /api/app/version} 并按结果弹窗。
     *
     * @param forceUpgrade 调用方已知必须强制升级（426 触发时为 true）
     * @param manual       用户手动触发（无更新/失败时给出 toast 反馈）
     */
    private static void fetchAndShow(Activity activity, boolean forceUpgrade, boolean manual) {
        if (activity == null) {
            return;
        }
        if (sDialogShowing.get()) {
            EasyLog.print(TAG, "已有升级弹窗在显示，跳过本次检查");
            return;
        }
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
                        if (!forceUpgrade && !backendForce
                                && !info.hasUpdate(currentVersionCode)) {
                            EasyLog.print(TAG, "当前已是最新版本: " + currentVersionCode);
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
                        showUpdateDialog(activity, info, force);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "检查版本失败: " + e.getMessage());
                        if (manual) {
                            Toaster.show(R.string.update_check_failed);
                        }
                    }
                });
    }

    private static void showUpdateDialog(Activity activity, UpdateInfo info, boolean force) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            EasyLog.print(TAG, "Activity 已不可用，跳过升级弹窗");
            return;
        }
        // 真正的去重点：并发的多个 426 里只有一个能弹出来。
        if (!sDialogShowing.compareAndSet(false, true)) {
            EasyLog.print(TAG, "已有升级弹窗在显示，跳过本次");
            return;
        }
        try {
            UpdateDialog.Builder builder = new UpdateDialog.Builder(activity)
                    .setVersionName(info.getLatestVersionName())
                    .setForceUpdate(force)
                    .setUpdateLog(info.getUpdateLog())
                    .setDownloadUrl(info.getDownloadUrl())
                    .setFileMd5(info.getMd5());
            // 弹窗消失后复位，下一次检查才能再弹。
            builder.addOnDismissListener(new BaseDialog.OnDismissListener() {
                @Override
                public void onDismiss(BaseDialog dialog) {
                    sDialogShowing.set(false);
                }
            });
            builder.show();
        } catch (RuntimeException error) {
            // show() 可能因宿主状态异常（如 onSaveInstanceState 之后再提交事务）抛出。
            // 必须释放标志，否则之后所有版本检查都会被静默跳过。
            sDialogShowing.set(false);
            EasyLog.print(TAG, "升级弹窗展示失败: " + error);
        }
    }
}
