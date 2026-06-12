package run.yigou.gxzy.ui.activity;

import android.view.Gravity;
import android.view.View;

import com.hjq.base.BaseDialog;

import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.network.glide.GlideApp;
import run.yigou.gxzy.manager.CacheDataManager;
import run.yigou.gxzy.manager.ThreadPoolManager;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.ui.browser.BrowserActivity;
import run.yigou.gxzy.ui.dialog.MenuDialog;
import run.yigou.gxzy.ui.dialog.SafeDialog;
import run.yigou.gxzy.ui.dialog.UpdateDialog;
import run.yigou.gxzy.ui.home.HomeFragment;
import run.yigou.gxzy.ui.account.PhoneResetActivity;
import run.yigou.gxzy.ui.account.PasswordResetActivity;

import com.hjq.permissions.XXPermissions;
import com.hjq.widget.layout.SettingBar;
import com.hjq.widget.view.SwitchButton;

/**
 * author : Android ???
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2019/03/01
 * desc   : ????
 */
public final class SettingActivity extends AppActivity
        implements SwitchButton.OnCheckedChangeListener {

    private SettingBar mLanguageView;
    private SettingBar mPhoneView;
    private SettingBar mPasswordView;
    private SettingBar mCleanCacheView;
    private SwitchButton mAutoSwitchView;

    @Override
    protected int getLayoutId() {
        return R.layout.setting_activity;
    }

    @Override
    protected void initView() {
        mLanguageView = findViewById(R.id.sb_setting_language);
        mPhoneView = findViewById(R.id.sb_setting_phone);
        mPasswordView = findViewById(R.id.sb_setting_password);
        mCleanCacheView = findViewById(R.id.sb_setting_cache);
        mAutoSwitchView = findViewById(R.id.sb_setting_switch);

        // ?????????
        mAutoSwitchView.setOnCheckedChangeListener(this);

        setOnClickListener(R.id.sb_setting_language, R.id.sb_setting_update, R.id.sb_setting_phone,
                R.id.sb_setting_password, R.id.sb_setting_agreement, R.id.sb_setting_about,
                R.id.sb_setting_cache, R.id.sb_setting_auto, R.id.sb_setting_exit, R.id.sb_permission_setting);
    }

    @Override
    protected void initData() {
        // ????????
        mCleanCacheView.setRightText(CacheDataManager.getTotalCacheSize(this));
        //??????
        // mAutoSwitchView.setVisibility(View.GONE);
        mLanguageView.setRightText("????");
        mPhoneView.setRightText("181****1413");
        mPasswordView.setRightText("??????");
       // XEventBus.getDefault().register(SettingActivity.this);
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.sb_setting_language) {

            // ?????
            new MenuDialog.Builder(this)
                    // ?????????????
                    //.setAutoDismiss(false)
                    .setList(R.string.setting_language_simple, R.string.setting_language_complex)
                    .setListener((MenuDialog.OnListener<String>) (dialog, position, string) -> {
                        mLanguageView.setRightText(string);
                        BrowserActivity.start(getActivity(), "https://github.com/getActivity/MultiLanguages");
                    })
                    .setGravity(Gravity.BOTTOM)
                    .setAnimStyle(BaseDialog.ANIM_BOTTOM)
                    .show();

        } else if (viewId == R.id.sb_setting_update) {

            // ???????????????
            if (20 > AppConfig.getVersionCode()) {
                new UpdateDialog.Builder(this)
                        .setVersionName("2.0")
                        .setForceUpdate(false)
                        .setUpdateLog("??Bug\n??????")
                        .setDownloadUrl("https://down.qq.com/qqweb/QQ_1/android_apk/Android_8.5.0.5025_537066738.apk")
                        .setFileMd5("560017dc94e8f9b65f4ca997c7feb326")
                        .show();
            } else {
                toast(R.string.update_no_update);
            }

        } else if (viewId == R.id.sb_setting_phone) {

            new SafeDialog.Builder(this)
                    .setListener((dialog, phone, code) -> PhoneResetActivity.start(getActivity(), code))
                    .show();

        } else if (viewId == R.id.sb_setting_password) {

            new SafeDialog.Builder(this)
                    .setListener((dialog, phone, code) -> PasswordResetActivity.start(getActivity(), phone, code))
                    .show();

        } else if (viewId == R.id.sb_setting_agreement) {

            BrowserActivity.start(this, "https://github.com/getActivity/Donate");

        } else if (viewId == R.id.sb_setting_about) {

            startActivity(AboutActivity.class);

        } else if (viewId == R.id.sb_permission_setting) {

            XXPermissions.startPermissionActivity(this);

        } else if (viewId == R.id.sb_setting_auto) {

            // ????
            mAutoSwitchView.setChecked(!mAutoSwitchView.isChecked());

        } else if (viewId == R.id.sb_setting_cache) {

            // ??????????????
            GlideApp.get(getActivity()).clearMemory();
            ThreadPoolManager.getInstance().execute(() -> {
                CacheDataManager.clearAllCache(this);
                // ??????????????
                GlideApp.get(getActivity()).clearDiskCache();
                post(() -> {
                    // ??????????
                    mCleanCacheView.setRightText(CacheDataManager.getTotalCacheSize(getActivity()));
                });
            });

        } else if (viewId == R.id.sb_setting_exit) {

            if (true) {

                DbService.getInstance().mUserInfoService.deleteEntity(AppApplication.application.mUserInfoToken);
                AppApplication.application.mUserInfoToken = null;
                // startActivity(LoginActivity.class);
                AppApplication.application.isLogin=false;
               // XEventBus.getDefault().post(new LoginEvent(false));
                HomeActivity.start(getContext(), HomeFragment.class);
                finish();


            }

//            // ????
//            EasyHttp.post(this)
//                    .api(new LogoutApi())
//                    .request(new HttpCallback<HttpData<Void>>(this) {
//
//                        @Override
//                        public void onSucceed(HttpData<Void> data) {
//                            startActivity(LoginActivity.class);
//                            // ????????????????????
//                            ActivityManager.getInstance().finishAllActivities(LoginActivity.class);
//                        }
//                    });

        }
    }

    /**
     * {@link SwitchButton.OnCheckedChangeListener}
     */

    @Override
    public void onCheckedChanged(SwitchButton button, boolean checked) {
        toast(checked);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // XEventBus.getDefault().unregister(SettingActivity.this);
    }
}