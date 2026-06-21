package run.yigou.gxzy.ui.system;

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

        mBtnAgree.setOnClickListener(v -> requestStoragePermission());
        mBtnExit.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            exitApp();
        });
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
        com.tencent.mmkv.MMKV.defaultMMKV().encode("is_privacy_agreed", true);

        XXPermissions.with(this)
                .permission(Permission.READ_MEDIA_IMAGES)
                .permission(Permission.READ_MEDIA_VIDEO)
                .permission(Permission.READ_MEDIA_AUDIO)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (allGranted) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                        if (doNotAskAgain) {
                            showGoToSettingsDialog();
                        } else {
                            Toaster.show(R.string.permission_denied_exit_message);
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
                                Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO, Permission.READ_MEDIA_AUDIO);
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
        if (XXPermissions.isGranted(this, Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO, Permission.READ_MEDIA_AUDIO)) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void exitApp() {
        ActivityManager.getInstance().finishAllActivities();
    }
}
