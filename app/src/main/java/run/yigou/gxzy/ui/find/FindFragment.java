package run.yigou.gxzy.ui.find;

import android.widget.ImageView;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import run.yigou.gxzy.R;

import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.network.glide.GlideApp;
import run.yigou.gxzy.ui.home.HomeActivity;

import com.hjq.widget.view.CountdownView;
import com.hjq.widget.view.SwitchButton;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : ?? Fragment
 */
public final class FindFragment extends TitleBarFragment<HomeActivity>
        implements SwitchButton.OnCheckedChangeListener {

    private ImageView mCircleView;
    private ImageView mCornerView;
    private SwitchButton mSwitchButton;
    private CountdownView mCountdownView;

    public static FindFragment newInstance() {
        return new FindFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.find_fragment;
    }

    @Override
    protected void initView() {
        mCircleView = findViewById(R.id.iv_find_circle);
        mCornerView = findViewById(R.id.iv_find_corner);
        mSwitchButton = findViewById(R.id.sb_find_switch);
        mCountdownView = findViewById(R.id.cv_find_countdown);
        setOnClickListener(mCountdownView);

        mSwitchButton.setOnCheckedChangeListener(this);
    }

    @Override
    protected void initData() {
        // ????? ImageView
        GlideApp.with(this)
                .load(R.drawable.update_app_top_bg)
                .transform(new MultiTransformation<>(new CenterCrop(), new CircleCrop()))
                .into(mCircleView);
    }

    @Override
    public void onCheckedChanged(SwitchButton button, boolean checked) {
        toast(checked ? "??" : "??");
    }
}