package run.yigou.gxzy.ui.account;

import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.Permissions;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.network.glide.GlideApp;
import run.yigou.gxzy.ui.home.HomeFragment;
import run.yigou.gxzy.ui.activity.HomeActivity;

import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : ?? Fragment
 */
public final class MessageFragment extends TitleBarFragment<HomeActivity> {

    private ImageView mImageView;

    public static MessageFragment newInstance() {
        return new MessageFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.message_fragment;
    }

    @Override
    protected void initView() {
        mImageView = findViewById(R.id.iv_message_image);
        setOnClickListener(R.id.btn_message_image1, R.id.btn_message_image2, R.id.btn_message_image3,
                R.id.btn_message_toast, R.id.btn_message_permission, R.id.btn_message_setting,
                R.id.btn_message_black, R.id.btn_message_white, R.id.btn_message_tab, R.id.btn_message_login);
    }

    @Override
    protected void initData() {

    }

    @Override
    public boolean isStatusBarEnabled() {
        // ????????
        return !super.isStatusBarEnabled();
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        switch (viewId) {
            case R.id.btn_message_image1:
                mImageView.setVisibility(View.VISIBLE);
                GlideApp.with(this)
                        .load("https://www.baidu.com/img/bd_logo.png")
                        .into(mImageView);
                break;
            case R.id.btn_message_image2:
                mImageView.setVisibility(View.VISIBLE);
                GlideApp.with(this)
                        .load("https://www.baidu.com/img/bd_logo.png")
                        .circleCrop()
                        .into(mImageView);
                break;
            case R.id.btn_message_image3:
                mImageView.setVisibility(View.VISIBLE);
                GlideApp.with(this)
                        .load("https://www.baidu.com/img/bd_logo.png")
                        .transform(new RoundedCorners((int) getResources().getDimension(R.dimen.dp_20)))
                        .into(mImageView);
                break;
            case R.id.btn_message_toast:
                toast("????");
                break;
            case R.id.btn_message_login:
                startActivity(LoginActivity.class);
                break;
            case R.id.btn_message_permission:
                requestPermission();
                break;
            case R.id.btn_message_setting:
                XXPermissions.startPermissionActivity(this);
                break;
            case R.id.btn_message_black:
                getAttachActivity()
                        .getStatusBarConfig()
                        .statusBarDarkFont(true)
                        .init();
                break;
            case R.id.btn_message_white:
                getAttachActivity()
                        .getStatusBarConfig()
                        .statusBarDarkFont(false)
                        .init();
                break;
            case R.id.btn_message_tab:
                HomeActivity.start(getActivity(), HomeFragment.class);
                break;
        }
    }

    @Permissions(Permission.CAMERA)
    private void requestPermission() {
        toast("?????????");
    }
}