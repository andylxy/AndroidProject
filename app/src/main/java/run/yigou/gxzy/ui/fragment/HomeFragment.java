/*
 * 项目名: AndroidProject
 * 类名: HomeFragment.java
 * 包名: run.yigou.gxzy.ui.fragment.HomeFragment
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 23:27:44
 * 上次修改时间: 2023年07月05日 17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.base.FragmentPagerAdapter;
import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.activity.BookContentSearchActivity;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.adapter.TabAdapter;
import run.yigou.gxzy.widget.XCollapsingToolbarLayout;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 首页 Fragment
 */
public final class HomeFragment extends TitleBarFragment<HomeActivity>
        implements TabAdapter.OnTabListener, ViewPager.OnPageChangeListener,
        XCollapsingToolbarLayout.OnScrimsListener {

    private XCollapsingToolbarLayout mCollapsingToolbarLayout;
    private Toolbar mToolbar;

    private TextView mAddressView;
    private TextView mHintView;
    private AppCompatImageView mSearchView;
    private List<BookInfoNav.Bean> bookNavList;
    private RecyclerView mTabView;
    private ViewPager mViewPager;

    private TabAdapter mTabAdapter;
    private FragmentPagerAdapter<AppFragment<?>> mPagerAdapter;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.home_fragment;
    }

    @Override
    protected void initView() {
        mCollapsingToolbarLayout = findViewById(R.id.ctl_home_bar);
        mToolbar = findViewById(R.id.tb_home_title);

        mAddressView = findViewById(R.id.tv_home_address);
        mHintView = findViewById(R.id.tv_home_hint);
        mSearchView = findViewById(R.id.iv_home_search);

        mTabView = findViewById(R.id.rv_home_tab);
        mViewPager = findViewById(R.id.vp_home_pager);
        mPagerAdapter = new FragmentPagerAdapter<>(this);
        //mPagerAdapter.addFragment(BrowserFragment.newInstance("https://github.com/getActivity"), "网页演示");
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mTabAdapter = new TabAdapter(getAttachActivity());
        mTabView.setAdapter(mTabAdapter);
        // 给这个 ToolBar 设置顶部内边距，才能和 TitleBar 进行对齐
        ImmersionBar.setTitleBar(getAttachActivity(), mToolbar);

        //设置渐变监听
        mCollapsingToolbarLayout.setOnScrimsListener(this);
        setOnClickListener(R.id.tv_home_hint, R.id.iv_home_search);

    }

    @Override
    protected void initData() {
        getBookInfoList();
        // mTabAdapter.addItem("网页演示");
        mTabAdapter.setOnTabListener(this);

    }

    @SingleClick
    @Override
    public void onClick(View view) {
        int viewId = view.getId();

        switch (viewId) {
            case R.id.tv_home_hint:
            case R.id.iv_home_search:
                //todo 搜索跳转
               // Intent intent = new Intent(getActivity(), BookContentSearchActivity.class);
               // startActivity(intent);
                break;
            default:
                EasyLog.print("onClick value: " + viewId);
        }
    }

    private void getBookInfoList() {
        EasyHttp.get(this)
                .api(new BookInfoNav())
                .request(new HttpCallback<HttpData<List<BookInfoNav.Bean>>>(this) {

                    @Override
                    public void onSucceed(HttpData<List<BookInfoNav.Bean>> data) {
                        if (data.getData().size() > 0) {
                            bookNavList = data.getData();
                            for (BookInfoNav.Bean nav : bookNavList) {
                                if (Objects.equals(nav.getName(), "伤寒"))
                                    mPagerAdapter.addFragment(TipsWindowFragment.newInstance());
                                else
                                    mPagerAdapter.addFragment(BookInfoFragment.newInstance(nav.getNavList()), nav.getName());
                                mTabAdapter.addItem(nav.getName());
                            }
                        } else
                            bookNavList = new ArrayList<>();

                    }
                });
    }


    @Override
    public boolean isStatusBarDarkFont() {
        return mCollapsingToolbarLayout.isScrimsShown();
    }

    /**
     * {@link TabAdapter.OnTabListener}
     */

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

    /**
     * CollapsingToolbarLayout 渐变回调
     * <p>
     * {@link XCollapsingToolbarLayout.OnScrimsListener}
     */
    @SuppressLint("RestrictedApi")
    @Override
    public void onScrimsStateChange(XCollapsingToolbarLayout layout, boolean shown) {
        getStatusBarConfig().statusBarDarkFont(shown).init();
        mAddressView.setTextColor(ContextCompat.getColor(getAttachActivity(), shown ? R.color.black : R.color.white));
        mHintView.setBackgroundResource(shown ? R.drawable.home_search_bar_gray_bg : R.drawable.home_search_bar_transparent_bg);
        mHintView.setTextColor(ContextCompat.getColor(getAttachActivity(), shown ? R.color.black60 : R.color.white60));
        mSearchView.setSupportImageTintList(ColorStateList.valueOf(getColor(shown ? R.color.common_icon_color : R.color.white)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewPager.setAdapter(null);
        mViewPager.removeOnPageChangeListener(this);
        mTabAdapter.setOnTabListener(null);
    }
}