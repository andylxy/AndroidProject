package run.yigou.gxzy.ui.fragment;

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
import com.hjq.http.model.FileContentResolver;
import com.hjq.widget.layout.SettingBar;
import com.lxj.xpopup.XPopup;


import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.http.api.UpdateImageApi;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.activity.ImageCropActivity;
import run.yigou.gxzy.ui.activity.ImagePreviewActivity;
import run.yigou.gxzy.ui.activity.ImageSelectActivity;
import run.yigou.gxzy.ui.activity.LoginActivity;
import run.yigou.gxzy.ui.activity.SettingActivity;
import run.yigou.gxzy.ui.dialog.AddressDialog;
import run.yigou.gxzy.ui.dialog.InputDialog;
import run.yigou.gxzy.ui.tips.CustomBubbleAttachPopup;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/04/20
 *    desc   : 个人资料
 */
public final class MyFragmentPersonal extends TitleBarFragment<HomeActivity> {

    public static MyFragmentPersonal newInstance() {
        return new MyFragmentPersonal();
    }
    private ViewGroup mAvatarLayout;
    private ImageView mAvatarView;
    private SettingBar mIdView;
    private SettingBar mNameView;
    private SettingBar mAddressView;
    private SettingBar mPersonDataSetting;

    private TextView mTest_btn;


    /** 省 */
    private String mProvince = "...";
    /** 市 */
    private String mCity = "...";
    /** 区 */
    private String mArea = "...";

    /** 头像地址 */
    private Uri mAvatarUrl;
    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
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
        mAddressView = findViewById(R.id.sb_person_data_address);
        mPersonDataSetting = findViewById(R.id.sb_person_data_setting);
        mTest_btn = findViewById(R.id.test_btn);
        setOnClickListener(mAvatarLayout, mAvatarView, mNameView, mAddressView,mPersonDataSetting,mTest_btn);
    }

    @Override
    protected void initData() {
        GlideApp.with(getAttachActivity())
                .load(R.drawable.avatar_placeholder_ic)
                .placeholder(R.drawable.avatar_placeholder_ic)
                .error(R.drawable.avatar_placeholder_ic)
                .transform(new MultiTransformation<>(new CenterCrop(), new CircleCrop()))
                .into(mAvatarView);
        String address = mProvince + mCity + mArea;
        mIdView.setRightText("...");
        mNameView.setRightText("...");
        mAddressView.setRightText(address);
        mPersonDataSetting.setVisibility(View.GONE);
        if (AppApplication.application.mUserInfoToken !=null){
            mIdView.setRightText(AppApplication.application.mUserInfoToken.getUserLoginAccount());
            mNameView.setRightText(AppApplication.application.mUserInfoToken.getUserName());
            //mAddressView.setRightText(address);
            mPersonDataSetting.setVisibility(View.VISIBLE);
        }

    }

    /**
     * 重新激活时调用
     */
    @Override
    public void onResume() {
        super.onResume();
        initData();
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        //if (isLogin()) return;
        if (view == mAvatarLayout) {
            ImageSelectActivity.start(getAttachActivity(), data -> {
                // 裁剪头像
                cropImageFile(new File(data.get(0)));
            });
        } else if (view == mAvatarView) {
            if (mAvatarUrl != null) {
                // 查看头像
                ImagePreviewActivity.start(getActivity(), mAvatarUrl.toString());
            } else {
                // 选择头像
                onClick(mAvatarLayout);
            }
        } else if (view == mNameView) {
            new InputDialog.Builder(getAttachActivity())
                    // 标题可以不用填写
                    .setTitle(getString(R.string.personal_data_name_hint))
                    .setContent(mNameView.getRightText())
                    //.setHint(getString(R.string.personal_data_name_hint))
                    //.setConfirm("确定")
                    // 设置 null 表示不显示取消按钮
                    //.setCancel("取消")
                    // 设置点击按钮后不关闭对话框
                    //.setAutoDismiss(false)
                    .setListener((dialog, content) -> {
                        if (!mNameView.getRightText().equals(content)) {
                            mNameView.setRightText(content);
                        }
                    })
                    .show();
        } else if (view == mAddressView) {
            new AddressDialog.Builder(getAttachActivity())
                    //.setTitle("选择地区")
                    // 设置默认省份
                    .setProvince(mProvince)
                    // 设置默认城市（必须要先设置默认省份）
                    .setCity(mCity)
                    // 不选择县级区域
                    //.setIgnoreArea()
                    .setListener((dialog, province, city, area) -> {
                        String address = province + city + area;
                        if (!mAddressView.getRightText().equals(address)) {
                            mProvince = province;
                            mCity = city;
                            mArea = area;
                            mAddressView.setRightText(address);
                        }
                    })
                    .show();
        }
        else if (view == mPersonDataSetting) {
            startActivity(SettingActivity.class);
        }

        else if (view == mTest_btn) {
            new XPopup.Builder(getContext())
                    //.isTouchThrough(true)
                    .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
                    .atView(mTest_btn)
                    .hasShadowBg(false) // 去掉半透明背景
                    .asCustom(new CustomBubbleAttachPopup(getContext()))
                    .show();
        }

    }
    /**
     *     登陆
     */
    private boolean isLogin() {
        if (AppApplication.application.mUserInfoToken ==null){
            startActivity(LoginActivity.class);
            return true;
        }
        return false;
    }

    /**
     * 裁剪图片
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
                // 没有的话就不裁剪，直接上传原图片
                // 但是这种情况极其少见，可以忽略不计
                updateCropImage(sourceFile, false);
            }
        });
    }

    /**
     * 上传裁剪后的图片
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