package run.yigou.gxzy.ui.splash;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.viewpager2.widget.ViewPager2;

import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.manager.lifecycle.ActivityManager;
import run.yigou.gxzy.manager.privacy.PrivacyAgreement;
import run.yigou.gxzy.ui.home.HomeActivity;

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
    protected void initActivity() {
        // ⚠️ 本页最后一屏会 startActivity(HomeActivity.class)，是一条**绕过启动硬门**的路径：
        // 它不检查协议同意、也不检查系统权限，一旦被接上，用户就能在未同意时直达首页，
        // 首页随即触发书本 tab 加载与三个 Manager 的前台回调（它们在
        // SplashActivity 放行时才注册，但 HomeFragment 自身的加载不受那道闸门管）。
        //
        // 当前它在 Manifest 里没有 intent-filter、exported 默认为 false、也没有任何地方
        // start 它，所以实际进不来，这里加校验是**为了让不变量无条件成立**：将来若有人
        // 把它接回启动流程，闸门已经在，而不是靠「谁都没接」这种约定。
        if (!PrivacyAgreement.isAgreed()) {
            // 不弹提示、不引导：协议都没同意，引导页本身就是不该出现的内容。
            finish();
            ActivityManager.getInstance().finishAllActivities();
            System.exit(0);
            return;
        }
        super.initActivity();
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