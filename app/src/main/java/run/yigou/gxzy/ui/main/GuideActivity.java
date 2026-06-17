package run.yigou.gxzy.ui.main;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.viewpager2.widget.ViewPager2;

import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;

import me.relex.circleindicator.CircleIndicator3;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/09/21
 *    desc   : 应用引导页
 */
public final class GuideActivity extends AppActivity {

    private ViewPager2 mViewPager;
    private CircleIndicator3 mIndicatorView;
    private View mCompleteView;

    private GuideAdapter mAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.guide_activity;
    }

    @Override
    protected void initView() {
        mViewPager = findViewById(R.id.vp_guide_pager);
        mIndicatorView = findViewById(R.id.cv_guide_indicator);
        mCompleteView = findViewById(R.id.btn_guide_complete);
        setOnClickListener(mCompleteView);
    }

    @Override
    protected void initData() {
        mAdapter = new GuideAdapter(this);
        mViewPager.setAdapter(mAdapter);
        mViewPager.registerOnPageChangeCallback(mCallback);
        mIndicatorView.setViewPager(mViewPager);
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        if (mViewPager.getCurrentItem() != mAdapter.getCount() - 1) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        } else {
            // 跳转到首页
            startActivity(HomeActivity.class);
            // 销毁当前页面
            finish();
        }
    }

    private ViewPager2.OnPageChangeCallback mCallback = new ViewPager2.OnPageChangeCallback() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mAdapter.getCount() == 0) {
                return;
            }
            // 判断是否是最后一页
            if (position == mAdapter.getCount() - 1) {
                mCompleteView.setVisibility(View.VISIBLE);

                float endProgress = 1.0f - positionOffset;
                ScaleAnimation scaleAnimation = new ScaleAnimation(
                        endProgress, 1.0f, endProgress, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(0);
                scaleAnimation.setFillAfter(true);
                mCompleteView.startAnimation(scaleAnimation);
            } else {
                mCompleteView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}