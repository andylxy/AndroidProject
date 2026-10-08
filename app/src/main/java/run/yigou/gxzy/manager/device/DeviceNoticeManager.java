package run.yigou.gxzy.manager.device;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

import java.util.concurrent.atomic.AtomicBoolean;

import run.yigou.gxzy.R;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.update.UpdateManager;
import run.yigou.gxzy.manager.lifecycle.ActivityManager;
import run.yigou.gxzy.manager.lifecycle.ForegroundActivities;

/**
 * 「此设备已被管理员禁用」的提示（ADR-0003）。
 *
 * <p>从 {@code UpdateManager} 拆出来：那里管的是**版本升级**，而设备吊销是另一件事
 * （吊销的语义边界见 CONTEXT.md「设备吊销」——它按账号生效，不是安防边界）。
 * 混在一个类里会让"Update"名不副实。</p>
 *
 * <p>与「会话过期」的区别：这里**不跳登录页**。否则会陷入「登录成功 → 又被 401」
 * 的死循环（ADR-0003）。</p>
 */
public final class DeviceNoticeManager {

    private static final String TAG = "DeviceNotice";

    /** 只弹一次：被吊销后该设备的每个请求都会 401，不能每次都弹。 */
    private static final AtomicBoolean sShown = new AtomicBoolean(false);

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private DeviceNoticeManager() {
    }

    /**
     * 收到 {@code 401 + X-Device-Revoked: 1} 时调用（可能在 OkHttp 线程，统一切主线程做 UI）。
     */
    public static void onDeviceRevoked() {
        MAIN_HANDLER.post(() -> {
            if (sShown.get()) {
                return;
            }
            Activity activity = ForegroundActivities.topIfUsable();
            if (activity == null) {
                // 没有可用的前台 Activity：**不消费**标志，等下一次请求再提示，
                // 否则这一台设备的禁用提示就永远不出现（spec §7.1 坑 4：先确认能做，再消费标志）。
                EasyLog.print(TAG, "设备已禁用，但当前没有可用的 Activity，稍后重试提示");
                return;
            }
            if (!sShown.compareAndSet(false, true)) {
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
}
