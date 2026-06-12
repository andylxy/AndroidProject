package run.yigou.gxzy.ui.account;

import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import run.yigou.gxzy.data.remote.model.FileContentResolver;
import com.hjq.permissions.XXPermissions;
import com.hjq.widget.layout.SettingBar;


import java.util.List;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.ui.home.HomeFragment;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.remote.api.UpdateImageApi;
import run.yigou.gxzy.network.glide.GlideApp;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.ui.feature.settings.AboutActivity;
import run.yigou.gxzy.ui.media.activity.ImageCropActivity;
import run.yigou.gxzy.ui.media.activity.ImagePreviewActivity;
import run.yigou.gxzy.ui.media.activity.ImageSelectActivity;
import run.yigou.gxzy.ui.feature.settings.SettingActivity;
import run.yigou.gxzy.ui.home.HomeActivity;
import run.yigou.gxzy.ui.dialog.InputDialog;

/**
 * author : Android ???
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2019/04/20
 * desc   : ????
 */
public final class MyFragmentPersonal extends TitleBarFragment<HomeActivity> {

    public static MyFragmentPersonal newInstance() {
        return new MyFragmentPersonal();
    }

    private ViewGroup mAvatarLayout;
    private ImageView mAvatarView;
    private SettingBar mIdView;
    private SettingBar mNameView;
    private TextView mMyLogin;
    private SettingBar mPersonDataSetting;
    private TextView mLoginExit;
    private SettingBar mAboutView;
    private SettingBar mPermissionView;
//
//    /** ? */
//    private String mProvince = "...";
//    /** ? */
//    private String mCity = "...";
//    /** ? */
//    private String mArea = "...";

    /**
     * ????
     */
    private Uri mAvatarUrl;

    @Override
    public boolean isStatusBarEnabled() {
        // ????????
        return !super.isStatusBarEnabled();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.personal_data_activity;
    }

    @Override
    protected void initView() {
        mAvatarLayout = findViewById(R.id.fl_person_data_avatar);
        mAvatarView = findViewById(R.id.iv_person_data_avatar);
        mIdView = findViewById(R.id.sb_person_data_id);
        mNameView = findViewById(R.id.sb_person_data_name);
        //  mAddressView = findViewById(R.id.sb_person_data_address);
        mPersonDataSetting = findViewById(R.id.sb_person_data_setting);
        mMyLogin = findViewById(R.id.my_setting_login);
        mLoginExit = findViewById(R.id.my_Login_exit);
        mAboutView = findViewById(R.id.my_setting_about);
        mPermissionView = findViewById(R.id.my_permission_setting);
        setOnClickListener(mAvatarLayout, mAvatarView, mNameView, mPersonDataSetting, mMyLogin, mLoginExit, mAboutView, mPermissionView);
    }

    @Override
    protected void initData() {
        GlideApp.with(getAttachActivity())
                .load(R.drawable.avatar_placeholder_ic)
                .placeholder(R.drawable.avatar_placeholder_ic)
                .error(R.drawable.avatar_placeholder_ic)
                .transform(new MultiTransformation<>(new CenterCrop(), new CircleCrop()))
                .into(mAvatarView);
        //String address = mProvince + mCity + mArea;
        mIdView.setRightText("...");
        mNameView.setRightText("...");
        // mAddressView.setRightText(address);
        mPersonDataSetting.setVisibility(View.GONE);
        mLoginExit.setVisibility(View.VISIBLE);
        mMyLogin.setVisibility(View.VISIBLE);
        if (AppApplication.application.isLogin) {
            mIdView.setRightText(AppApplication.application.mUserInfoToken.getUserLoginAccount());
            mNameView.setRightText(AppApplication.application.mUserInfoToken.getUserName());
            //mAddressView.setRightText(address);
            mPersonDataSetting.setVisibility(View.VISIBLE);
            mMyLogin.setVisibility(View.GONE);
            mLoginExit.setVisibility(View.VISIBLE);
        } else {
            mLoginExit.setVisibility(View.GONE);
        }

    }

    /**
     * ???????
     */
    @Override
    public void onResume() {
        super.onResume();
        //initView();
        initData();

        //     toast("onResume");
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        // ???????????
        if (handleNonLoginRequiredActions(view)) {
            return;
        }

//        // ????????
//        if (!AppApplication.application.global_openness) {
//            toast(AppConst.Key_Window_Tips);
//            return;
//        }

        // ????????????
        if (!AppApplication.application.isLogin && isLoginRequired(view)) {
            startActivity(LoginActivity.class);
            return;
        }

        // ???????????
        handleLoginRequiredActions(view);
    }

    /**
     * ????????????
     *
     * @param view ??????
     * @return ??????????
     */
    private boolean handleNonLoginRequiredActions(View view) {
        if (view == mAboutView) {
            startActivity(AboutActivity.class);
            return true;
        } else if (view == mPermissionView) {
            XXPermissions.startPermissionActivity(this);
            return true;
        }
        return false;
    }

    /**
     * ????????????
     *
     * @param view ??????
     * @return ??????
     */
    private boolean isLoginRequired(View view) {
        return view == mAvatarLayout || view == mAvatarView || view == mNameView || view == mMyLogin ||
               view == mPersonDataSetting || view == mLoginExit;
    }

    /**
     * ???????????
     *
     * @param view ??????
     */
    private void handleLoginRequiredActions(View view) {
        if (view == mAvatarLayout) {
            ImageSelectActivity.start(getAttachActivity(), new ImageSelectActivity.OnPhotoSelectListener() {
                @Override
                public void onSelected(List<String> data) {
                    // ????
                    cropImageFile(new File(data.get(0)));
                }

                @Override
                public void onCancel() {
                }
            });
        } else if (view == mAvatarView) {
            if (mAvatarUrl != null) {
                // ????
                ImagePreviewActivity.start(getActivity(), mAvatarUrl.toString());
            } else {
                // ????
                onClick(mAvatarLayout);
            }
        } else if (view == mNameView) {
            new InputDialog.Builder(getAttachActivity())
                    .setTitle(getString(R.string.personal_data_name_hint))
                    .setContent(mNameView.getRightText())
                    .setListener((dialog, content) -> {
                        if (!mNameView.getRightText().equals(content)) {
                            mNameView.setRightText(content);
                        }
                    })
                    .show();
        } else if (view == mPersonDataSetting) {
            startActivity(SettingActivity.class);
        } else if (view == mLoginExit) {
            handleLogout();
        }
    }

    /**
     * ????????
     */
    private void handleLogout() {
        DbService.getInstance().mUserInfoService.deleteEntity(AppApplication.application.mUserInfoToken);
        AppApplication.application.mUserInfoToken = null;
        AppApplication.application.isLogin = false;
        HomeActivity.start(getContext(), HomeFragment.class);
    }

    /**
     * ??
     */
    private boolean isLogin() {
        if (!AppApplication.application.isLogin) {
            startActivity(LoginActivity.class);
            return true;
        }
        return false;
    }

    /**
     * ????
     */
    private void cropImageFile(File sourceFile) {
        ImageCropActivity.start(getAttachActivity(), sourceFile, 1, 1, new ImageCropActivity.OnCropListener() {

            @Override
            public void onSucceed(Uri fileUri, String fileName) {
                File outputFile;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    outputFile = new FileContentResolver(getActivity(), fileUri, fileName);
                } else {
                    try {
                        outputFile = new File(new URI(fileUri.toString()));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        outputFile = new File(fileUri.toString());
                    }
                }
                updateCropImage(outputFile, true);
            }

            @Override
            public void onError(String details) {
                // ????????????????
                // ?????????????????
                updateCropImage(sourceFile, false);
            }
        });
    }

    /**
     * ????????
     */
    private void updateCropImage(File file, boolean deleteFile) {
        if (true) {
            if (file instanceof FileContentResolver) {
                mAvatarUrl = ((FileContentResolver) file).getContentUri();
            } else {
                mAvatarUrl = Uri.fromFile(file);
            }
            GlideApp.with(getActivity())
                    .load(mAvatarUrl)
                    .transform(new MultiTransformation<>(new CenterCrop(), new CircleCrop()))
                    .into(mAvatarView);
            return;
        }

        EasyHttp.post(this)
                .api(new UpdateImageApi()
                        .setImage(file))
                .request(new HttpCallback<HttpData<String>>(this) {

                    @Override
                    public void onSucceed(HttpData<String> data) {
                        mAvatarUrl = Uri.parse(data.getData());
                        GlideApp.with(getActivity())
                                .load(mAvatarUrl)
                                .transform(new MultiTransformation<>(new CenterCrop(), new CircleCrop()))
                                .into(mAvatarView);
                        if (deleteFile) {
                            file.delete();
                        }
                    }
                });
    }
}