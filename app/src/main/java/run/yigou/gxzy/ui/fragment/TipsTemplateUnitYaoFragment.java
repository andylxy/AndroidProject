package run.yigou.gxzy.ui.fragment;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.hjq.base.FragmentPagerAdapter;
import com.hjq.widget.layout.WrapRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.ui.adapter.TabAdapter;
import run.yigou.gxzy.ui.adapter.TipsUnitFragmentAdapter;


public final class TipsTemplateUnitYaoFragment extends AppFragment<AppActivity>   implements  TabAdapter.OnTabListener, ViewPager.OnPageChangeListener{

//    private WrapRecyclerView rvTipsUnitList;
//    private TipsUnitFragmentAdapter tipsUnitFragmentAdapter;
    private ViewPager mViewPager;
    private FragmentPagerAdapter<AppFragment<?>> mPagerAdapter;
    private TabAdapter mTabAdapter;
    private RecyclerView mTabView;
    public static TipsTemplateUnitYaoFragment newInstance() {
        return new TipsTemplateUnitYaoFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.template_unit_yao_fragment;
    }

    @Override
    protected void initView() {
//        rvTipsUnitList =  findViewById(R.id.rv_tips_unit_list);
//        tipsUnitFragmentAdapter = new TipsUnitFragmentAdapter(getAttachActivity());
//        rvTipsUnitList.setAdapter(tipsUnitFragmentAdapter);
        mTabView = findViewById(R.id.rv_home_tab);
        mViewPager = findViewById(R.id.vp_home_pager);
        mViewPager.addOnPageChangeListener(this);

        mTabAdapter = new TabAdapter(getAttachActivity());
        mPagerAdapter = new FragmentPagerAdapter<>(this);

        mTabView.setAdapter(mTabAdapter);
        mViewPager.setAdapter(mPagerAdapter);
    }
    @Override
    public boolean onTabSelected(RecyclerView recyclerView, int position) {
        mViewPager.setCurrentItem(position);
        return true;
    }

    /**
     * {@link ViewPager.OnPageChangeListener}
     */

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        if (mTabAdapter == null) {
            return;
        }
        mTabAdapter.setSelectedPosition(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    protected void initData() {
        mPagerAdapter.addFragment(TipsUnitShowFragment.newInstance());
        mTabAdapter.addItem("汉制单位");
        mTabAdapter.setOnTabListener(this);
    }
}