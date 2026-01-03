package run.yigou.gxzy.ui.activity;

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
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.Toaster;

import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.manager.ActivityManager;
import run.yigou.gxzy.ui.dialog.AgreementDialog;

/**
 * 权限引导页面（对话框样式）
 * 显示为闪屏页的 80% 大小，包含隐私协议和免责声明
 */
public final class PermissionGuideActivity extends AppActivity {

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

        // 同意并继续按钮
        mBtnAgree.setOnClickListener(v -> requestStoragePermission());

        // 退出应用按钮
        mBtnExit.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            exitApp();
        });

        // 隐私协议点击
        mTvPrivacy.setOnClickListener(v -> showPrivacyPolicyDialog());

        // 免责声明点击
        mTvDisclaimer.setOnClickListener(v -> showDisclaimerDialog());
    }

    @Override
    protected void initData() {
        // 设置窗口大小为屏幕的 80%
        setWindowSize();
    }

    /**
     * 设置窗口大小为屏幕的 80%
     */
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

    /**
     * 显示隐私协议对话框
     */
    private void showPrivacyPolicyDialog() {
        new AgreementDialog.Builder(this)
                .setTitle(getString(R.string.permission_guide_privacy))
                .setContent(getString(R.string.privacy_policy_content))
                .show();
    }

    /**
     * 显示免责声明对话框
     */
    private void showDisclaimerDialog() {
        new AgreementDialog.Builder(this)
                .setTitle(getString(R.string.permission_guide_disclaimer))
                .setContent(getString(R.string.disclaimer_content))
                .show();
    }

    /**
     * 请求存储权限
     */
    private void requestStoragePermission() {
        XXPermissions.with(this)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (allGranted) {
                            // 权限已授予，返回成功结果
                            setResult(RESULT_OK);
                            finish();
                        }
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                        if (doNotAskAgain) {
                            // 用户勾选了"不再询问"，引导去设置页
                            showGoToSettingsDialog();
                        } else {
                            // 用户拒绝授权，提示
                            Toaster.show(R.string.permission_denied_exit_message);
                        }
                    }
                });
    }

    /**
     * 显示前往设置页面的对话框
     */
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
                                Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE);
                    }

                    @Override
                    public void onCancel(com.hjq.base.BaseDialog dialog) {
                        setResult(RESULT_CANCELED);
                        exitApp();
                    }
                })
                .show();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // 从设置页返回后，检查权限是否已授予
        if (XXPermissions.isGranted(this, Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)) {
            setResult(RESULT_OK);
            finish();
        }
    }

    /**
     * 退出应用
     */
    private void exitApp() {
        ActivityManager.getInstance().finishAllActivities();
    }
}
