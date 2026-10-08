package run.yigou.gxzy.ui.splash;

import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;

import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.lifecycle.ActivityManager;
import run.yigou.gxzy.manager.privacy.PrivacyAgreement;
import run.yigou.gxzy.dialog.AgreementDialog;

/**
 * 权限引导页面（对话框样式）
 * 显示为闪屏页的 80% 大小，包含隐私协议和免责声明
 *
 * <p><b>本页是 App 的启动硬门</b>：协议已同意（{@link PrivacyAgreement#isAgreed()}）
 * <b>且</b> {@link StartupPermissions#REQUIRED} 全部授予才放行，否则一律结束进程
 * （见 {@link #exitApp()}）。在此之前 {@code AppApplication} <b>不会注册版本检查 / 公告 /
 * 搜索权限这三个会发请求的前台回调</b>（注册在 {@code SplashActivity} 的放行入口里），
 * 因此「用户不知情前不碰网络」成立。</p>
 *
 * <p><b>放行路径共两条，都必须同时满足两个条件</b>，改动任一处都要复查另一处：
 * 权限回调 {@code onGranted}、从系统设置返回 {@code onRestart}。反向的「不放行」归宿统一是
 * {@link #exitApp()}：权限被拒、被标记不再询问后取消、<b>仅部分授予</b>、点「退出」四种情形
 * 都要能结束进程，任何一种漏掉都会让用户卡在这一页。</p>
 */
public final class PermissionGuideActivity extends AppActivity {

    private static final String TAG = "PermissionGuideActivity";

    private Button mBtnAgree;
    private Button mBtnExit;
    private TextView mTvPrivacy;
    private TextView mTvDisclaimer;

    public static void start(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, PermissionGuideActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.permission_guide_activity;
    }

    @Override
    protected void initView() {
        mBtnAgree = findViewById(R.id.btn_agree);
        mBtnExit = findViewById(R.id.btn_exit);
        mTvPrivacy = findViewById(R.id.tv_privacy_policy);
        mTvDisclaimer = findViewById(R.id.tv_disclaimer);

        mBtnAgree.setOnClickListener(v -> requestStoragePermission());
        mBtnExit.setOnClickListener(v -> exitApp());
        mTvPrivacy.setOnClickListener(v -> showPrivacyPolicyDialog());
        mTvDisclaimer.setOnClickListener(v -> showDisclaimerDialog());
    }

    @Override
    protected void initData() {
        setWindowSize();
    }

    private void setWindowSize() {
        Window window = getWindow();
        if (window != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (displayMetrics.widthPixels * 0.85);
            params.height = (int) (displayMetrics.heightPixels * 0.80);
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
        }
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键，必须选择同意或退出
    }

    private void showPrivacyPolicyDialog() {
        new AgreementDialog.Builder(this)
                .setTitle(getString(R.string.permission_guide_privacy))
                .setContent(getString(R.string.privacy_policy_content))
                .show();
    }

    private void showDisclaimerDialog() {
        new AgreementDialog.Builder(this)
                .setTitle(getString(R.string.permission_guide_disclaimer))
                .setContent(getString(R.string.disclaimer_content))
                .show();
    }

    private void requestStoragePermission() {
        PrivacyAgreement.markAgreed();

        XXPermissions.with(this)
                .permission(Arrays.asList(StartupPermissions.REQUIRED))
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (allGranted) {
                            setResult(RESULT_OK);
                            finish();
                            return;
                        }
                        // ⚠️ 部分授权（allGranted==false）**必须**有归宿：XXPermissions 在只拿到
                        // 部分权限时会回调这里，若什么都不做，用户就永久卡在这一页 ——
                        // 不 setResult（闪屏收不到结果）、不退出、也没有任何提示。
                        // 这与 onDenied 未处理是同一类死锁，两者都归到「权限没全给 → 退出」。
                        EasyLog.print(TAG, "权限仅部分授予（" + permissions.size() + "/"
                                + StartupPermissions.REQUIRED.length + "），退出应用");
                        exitApp();
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                        // 协议已同意但权限被拒 → 就地退出，这是本应用的硬门条件
                        // （协议 **且** 权限都通过才允许继续后续流程）。
                        //
                        // ⚠️ 这里必须走和「退出」按钮完全相同的归宿。只弹 toast 会把用户
                        // 永久卡在这一页：SplashActivity 的 onActivityResult 收不到结果，
                        // 既不继续也不退出 —— 而文案却写着「即将退出」。真机实测过。
                        if (doNotAskAgain) {
                            // 已被系统标记「不再询问」，再点系统弹窗也没用，只能去设置页开。
                            showGoToSettingsDialog();
                        } else {
                            EasyLog.print(TAG, "用户拒绝必要权限，退出应用");
                            exitApp();
                        }
                    }
                });
    }

    private void showGoToSettingsDialog() {
        new run.yigou.gxzy.ui.dialog.MessageDialog.Builder(this)
                .setTitle(R.string.common_permission_alert)
                .setMessage(R.string.permission_denied_goto_settings)
                .setConfirm(R.string.common_permission_goto)
                .setCancel(R.string.permission_btn_exit)
                .setCancelable(false)
                .setListener(new run.yigou.gxzy.ui.dialog.MessageDialog.OnListener() {
                    @Override
                    public void onConfirm(com.hjq.base.BaseDialog dialog) {
                        XXPermissions.startPermissionActivity(PermissionGuideActivity.this,
                                StartupPermissions.REQUIRED);
                    }

                    @Override
                    public void onCancel(com.hjq.base.BaseDialog dialog) {
                        exitApp();
                    }
                })
                .show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // 从系统设置页返回：用户可能刚在那里开了权限。
        //
        // ⚠️ 这里**必须同时判协议同意**，不能只看权限。原来只判 XXPermissions.isGranted 就
        // setResult(RESULT_OK)，于是存在一条绕过：全新安装 → 引导页 → 用户**不点同意**、
        // 按 Home 去系统设置手动授完媒体权限 → 返回本页 → 判 granted → 放行 → 三个会发请求的
        // Manager 被武装起来，而协议从未被同意。评审实测确认过。
        // 硬门是「协议 **且** 权限」两条件，缺一即不得放行。协议状态见 PrivacyAgreement。
        //
        // 何时可移除：等权限申请改用「拒绝即不再询问、用户只能去设置」的单向流程，
        // 且系统保证回设置页必被拒（本方法存在的前提是系统可能补授）。届时删掉本方法，
        // 只留 onGranted 一条放行路径。
        if (!PrivacyAgreement.isAgreed()) {
            return;
        }
        if (XXPermissions.isGranted(this, StartupPermissions.REQUIRED)) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void exitApp() {
        // 直接结束进程，**不** setResult：System.exit(0) 同步杀死进程，
        // onActivityResult 永远不会投递（实测），写了也只是死代码。
        ActivityManager.getInstance().finishAllActivities();
        // finishAllActivities() 只 finish 掉 Activity，**进程仍然活着**：MMKV、
        // 静态状态都还在，用户从桌面再点图标会复用同一个进程。把进程一并结束，
        // 才是需求所说的「退出程序」，也保证下次冷启动从协议页重新走一遍。
        System.exit(0);
    }
}
