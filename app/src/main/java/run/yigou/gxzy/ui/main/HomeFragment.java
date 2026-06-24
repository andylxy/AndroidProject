/*
 * Project: AndroidProject
 * File: HomeFragment.java
 * Author: Zhs (xiaoyang_02@qq.com)
 * Created: 2023/07/05
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.main;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.base.BaseAdapter;
import com.hjq.base.FragmentPagerAdapter;
import com.hjq.http.EasyHttp;

import run.yigou.gxzy.config.AppStyleConfigProvider;
import run.yigou.gxzy.log.EasyLog;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.data.local.entity.SearchHistory;
import run.yigou.gxzy.data.local.entity.TabNav;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.service.SearchHistoryService;
import run.yigou.gxzy.data.local.service.TabNavService;
import run.yigou.gxzy.data.local.helper.DataRepository;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.remote.api.BookInfoNav;
import run.yigou.gxzy.data.remote.api.MingCiContentApi;
import run.yigou.gxzy.ui.main.HomeActivity;
import run.yigou.gxzy.ui.reader.BookContentSearchActivity;
import run.yigou.gxzy.data.remote.api.YaoAliaApi;
import run.yigou.gxzy.data.remote.api.YaoContentApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.ui.reader.search.SearchHistoryAdapter;
import run.yigou.gxzy.ui.reader.fragment.TipsWindowNetFragment;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.widget.CustomDividerItemDecoration;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.YaoAlia;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;
import com.hjq.widget.layout.XCollapsingToolbarLayout;

/**
 * author : Android 开源项目
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 首页 Fragment
 */
public final class HomeFragment extends TitleBarFragment<HomeActivity>
        implements TabAdapter.OnTabListener, ViewPager.OnPageChangeListener,
        XCollapsingToolbarLayout.OnScrimsListener, BaseAdapter.OnItemClickListener {

    private XCollapsingToolbarLayout mCollapsingToolbarLayout;
    private Toolbar mToolbar;

    private TextView mHomeSearchView;
    private ClearEditText mTvHomeSearchText;
    private AppCompatImageView mSearchView;
    private AppCompatImageView mRefreshView;
    private RecyclerView mTabView;
    private ViewPager mViewPager;
    private boolean isGetYaoData = true;
    private boolean isGetMingCiData = true;
    private TabAdapter mTabAdapter;
    private FragmentPagerAdapter<AppFragment<?>> mPagerAdapter;
    private TabNavService mTabNavService;


    private WrapRecyclerView lvHistoryList;
    private LinearLayout llHistoryView;
    private SearchHistoryService mSearchHistoryService;

    private SearchHistoryAdapter mSearchHistoryAdapter;
    private List<SearchHistory> mSearchHistories;
    private LinearLayout llClearHistory;
    private String searchKey;//搜索关键词

    // 必须保留空构造方法（Fragment 要求）
    public HomeFragment() {
    }

    /**
     * 创建 HomeFragment 实例
     *
     * @return HomeFragment 实例
     */
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.home_fragment;
    }

    @SuppressLint("CutPasteId")
    @Override
    protected void initView() {

        mCollapsingToolbarLayout = findViewById(R.id.ctl_home_bar);
        mToolbar = findViewById(R.id.tb_home_title);

        mHomeSearchView = findViewById(R.id.tv_home_search);
        mTvHomeSearchText = findViewById(R.id.tv_home_search_text);

        mSearchView = findViewById(R.id.iv_home_search);
        mRefreshView = findViewById(R.id.iv_home_refresh);

        mTabView = findViewById(R.id.rv_home_tab);
        mViewPager = findViewById(R.id.vp_home_pager);
        llHistoryView = findViewById(R.id.include_book_content_search_ll_history_view).findViewById(R.id.ll_history_view);
        lvHistoryList = findViewById(R.id.include_book_content_search_ll_history_view).findViewById(R.id.lv_history_list);
        llClearHistory = findViewById(R.id.include_book_content_search_ll_history_view).findViewById(R.id.ll_clear_history);

        // 创建 adapter，绑定 ViewPager 和 Tab 适配器
        mPagerAdapter = new FragmentPagerAdapter<>(this);
        mTabAdapter = new TabAdapter(getAttachActivity());

        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mTabView.setAdapter(mTabAdapter);
        // 将 ToolBar 设为沉浸式状态栏的 TitleBar
        ImmersionBar.setTitleBar(getAttachActivity(), mToolbar);
        //设置折叠监听
        mCollapsingToolbarLayout.setOnScrimsListener(this);
        setOnClickListener(R.id.tv_home_search_text, R.id.iv_home_search, R.id.iv_home_refresh);
        //搜索框设置
        mTvHomeSearchText.setMaxLines(1);  // 限制最多 1 行，防止换行
        mTvHomeSearchText.setSingleLine(true);  // 启用单行模式
        mTvHomeSearchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // 当 EditText 获得焦点时，显示搜索历史
                    mTabView.setVisibility(View.GONE);
                    mViewPager.setVisibility(View.GONE);
                    llHistoryView.setVisibility(View.VISIBLE);
                    mSearchHistories = mSearchHistoryService.findAllSearchHistory();
                    if (!mSearchHistories.isEmpty()) {
                        mSearchHistoryAdapter.notifyDataSetChanged();
                        lvHistoryList.setVisibility(View.VISIBLE);
                        llClearHistory.setVisibility(View.VISIBLE);
                    }

                } else {
                    // 当 EditText 失去焦点时，恢复主界面
                    mTabView.setVisibility(View.VISIBLE);
                    mViewPager.setVisibility(View.VISIBLE);
                    llHistoryView.setVisibility(View.GONE);
                    llClearHistory.setVisibility(View.GONE);
                }

            }
        });
        mTvHomeSearchText.setOnKeyListener((v, keyCode, event) -> {

            //处理回车键事件
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                //隐藏软键盘
                ((InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(Objects.requireNonNull(requireActivity().getCurrentFocus())
                                .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                //执行搜索
                searchKey = Objects.requireNonNull(mTvHomeSearchText.getText()).toString();
                search();
            }
            return false;
        });
        //EditText 失去焦点时收起搜索框
        // 点击根布局区域时关闭搜索
        findViewById(R.id.root_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 如果 EditText 当前有焦点，则清除
                    if (mTvHomeSearchText.isFocused()) {
                        clearSearchTextFocus();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public void clearSearchTextFocus() {
        // 清除 EditText 焦点并关闭键盘
        mTvHomeSearchText.clearFocus();
        mTvHomeSearchText.setFocusable(false);
        mTvHomeSearchText.setFocusableInTouchMode(false);
        mTvHomeSearchText.setText("");
        //    隐藏输入法
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mTvHomeSearchText.getWindowToken(), 0);
        }
    }


    @Override
    protected void initData() {
        // 导航数据初始化（原 tipsSingleDataInit 已移至 AppDataInitializer 中处理）
        mTabNavService = DbService.getInstance().mTabNavService;
        // 加载导航数据
        loadBookNavigation();
        
        // 加载样式配置（优先使用缓存）
        loadStyleConfig();
        
        mTabAdapter.setOnTabListener(this);

        // 首次加载时获取药方数据
        if (isGetYaoData && GlobalDataHolder.getInstance().getYaoMap().isEmpty()) {
            ThreadUtil.runInBackground(this::getAllYaoData);
        }
        // 首次加载时获取名词数据
        if (isGetMingCiData && GlobalDataHolder.getInstance().getMingCiContentMap().isEmpty()) {
            ThreadUtil.runInBackground(this::getAllMingCiData);
        }
        mSearchHistoryService = DbService.getInstance().mSearchHistoryService;
        // 清除搜索框焦点
        clearSearchTextFocus();
        //初始化搜索历史列表
        initHistoryList();
        llClearHistory.setOnClickListener(v -> {
            mSearchHistoryService.clearHistory();
            mSearchHistories.clear();
            mSearchHistoryAdapter.notifyDataSetChanged();
            llClearHistory.setVisibility(View.GONE);
            llHistoryView.setVisibility(View.GONE);
            lvHistoryList.setVisibility(View.GONE);
            toast("已清除搜索历史");
        });
    }


    /**
     * 初始化搜索历史列表
     */
    private void initHistoryList() {
        mSearchHistories = mSearchHistoryService.findAllSearchHistory();
        mSearchHistoryAdapter = new SearchHistoryAdapter(getActivity());
        mSearchHistoryAdapter.setData(mSearchHistories);
        mSearchHistoryAdapter.setOnItemClickListener(this);
        lvHistoryList.setAdapter(mSearchHistoryAdapter);
        lvHistoryList.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_BookList_RecyclerView_Color, AppConst.CustomDivider_Height));
        llHistoryView.setVisibility(View.GONE);
        llClearHistory.setVisibility(View.GONE);
    }

    /**
     * 执行搜索
     */
    private void search() {

        //全局开关判断
        if (!AppApplication.application.global_openness) {
            toast(AppConst.Key_Window_Tips);
            return;
        }
        //搜索关键词不为空时保存历史并跳转
        if (!StringHelper.isEmpty(searchKey)) {
            mSearchHistoryService.addOrUpadteHistory(searchKey);
            Intent intent = new Intent(getActivity(), BookContentSearchActivity.class);
            // 通过 Extra 传递搜索关键词到 Intent
            intent.putExtra("searchQuery", searchKey);
            startActivityForResult(intent, (resultCode, data) -> {
                clearSearchTextFocus();
            });
        }
    }

    @Override
    public void onItemClick(RecyclerView recyclerView, View itemView, int position) {

        if (recyclerView.getId() == R.id.lv_history_list) {
            searchKey = mSearchHistories.get(position).getContent();
            mTvHomeSearchText.setText(searchKey);
            search();
        }
    }

    @SingleClick
    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.tv_home_search_text:
                mTvHomeSearchText.setFocusable(true);
                mTvHomeSearchText.setFocusableInTouchMode(true);
                mTvHomeSearchText.requestFocus();
                break;
            case R.id.iv_home_search:
                searchKey = Objects.requireNonNull(mTvHomeSearchText.getText()).toString();
                search();
                break;
            case R.id.iv_home_refresh:
                refreshDataFromNetwork();
                break;
            default:
                EasyLog.print("onClick value: " + view.getId());
        }
    }

    /**
     * 加载书籍导航数据
     */
    private void loadBookNavigation() {
        // 从本地数据库查询导航数据
        ArrayList<TabNav> localNavList = mTabNavService.findAll();
        if (localNavList != null && !localNavList.isEmpty()) {
            // 使用 post 确保 ViewPager 完成布局后再加载
            mViewPager.post(() -> {
                loadNavFromLocal(localNavList);
                EasyLog.print("Loaded navigation from local database");
            });
        } else {
            // 本地无数据，从网络获取
            getBookInfoList();
        }
    }
    
    /**
     * 加载样式配置
     * 参考 loadBookNavigation() 模式：
     * - 优先检查缓存
     * - 有缓存 → 使用缓存配置，不再请求后端
     * - 无缓存 → 请求服务端最新配置
     * - 成功后自动保存到缓存
     * - 同步更新到当前配置列表
     */
    private void loadStyleConfig() {
        run.yigou.gxzy.config.AppStyleConfigProvider provider = new run.yigou.gxzy.config.AppStyleConfigProvider();
        
        // 1. 先检查缓存
        boolean cacheLoaded = provider.loadCacheConfig();
        if (cacheLoaded) {
            // 2. 有缓存 → 使用缓存配置，不再请求后端
            EasyLog.print("HomeFragment", "样式配置已从缓存加载（不请求后端）");
        } else {
            // 3. 无缓存 → 请求后端配置
            boolean triggered = provider.loadConfig(this);
            if (triggered) {
                EasyLog.print("HomeFragment", "已触发样式配置加载（从后端）");
            } else {
                EasyLog.print("HomeFragment", "样式配置加载失败");
            }
        }
    }

    /**
     * 从本地数据加载导航
     */
    private void loadNavFromLocal(List<TabNav> navList) {
        for (TabNav nav : navList) {
            if (nav.getNavList() != null && !nav.getNavList().isEmpty()) {
                mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(nav.getNavList()));
                mTabAdapter.addItem(nav.getName());
            }
        }
    }

    /**
     * 从网络刷新全部数据
     */
    private void refreshDataFromNetwork() {
        toast("加载中...");
        // 清空现有数据
        while (mPagerAdapter.getCount() > 0) {
            mPagerAdapter.removeFragment(0);
        }
        mTabAdapter.clearData();
        // 重新请求网络数据
        getBookInfoList();
        // 同时刷新药方和名词数据
        ThreadUtil.runInBackground(this::getAllYaoData);
        ThreadUtil.runInBackground(this::getAllMingCiData);
        // 刷新样式配置（从服务端获取最新配置）
        AppStyleConfigProvider provider = new AppStyleConfigProvider();
        provider.loadConfig(this);
    }

    private void getBookInfoList() {

        EasyHttp.get(this)
                .api(new BookInfoNav())
                .request(new HttpCallback<HttpData<List<TabNav>>>(this) {

                    @Override
                    public void onSucceed(HttpData<List<TabNav>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            if (mTabNavService == null || mPagerAdapter == null || mTabAdapter == null) {
                                // 如果 Fragment 已销毁，直接返回
                                return;
                            }

                            // 在后台线程中处理导航数据
                            ThreadUtil.runInBackground(() -> {
                                List<TabNav> navList = new CopyOnWriteArrayList<>(data.getData());
                                
                                GlobalDataHolder globalData = GlobalDataHolder.getInstance();
                                int order = 0;

                                // 收集有效导航数据用于 UI 更新
                                final List<TabNav> validNavList = new ArrayList<>();
                                
                                for (TabNav nav : navList) {
                                    if (nav.getNavList() != null && !nav.getNavList().isEmpty()) {
                                        // ✅ 使用 putNavTab 触发状态标记
                                        globalData.putNavTab(order, nav);
                                        for (TabNavBody item : nav.getNavList()) {
                                            if (item.getBookNo() > 0) {
                                                // ✅ 使用 putBookInfo 触发状态标记
                                                globalData.putBookInfo(item.getBookNo(), item);
                                            }
                                        }
                                        order++;
                                        validNavList.add(nav);
                                    }
                                }
                                
                                // 保存到数据库
                                DataRepository.saveTabNvaInDb(navList, HomeFragment.this);
                                
                                // ✅ 加载方剂别名数据（依赖导航数据）
                                try {
                                    java.util.List<run.yigou.gxzy.data.local.entity.TabNavBody> bookInfos = globalData.getAllBookInfos();
                                    java.util.Map<String, String> fangAliasDict = new java.util.HashMap<>();
                                    
                                    int aliasCount = 0;
                                    for (run.yigou.gxzy.data.local.entity.TabNavBody bookInfo : bookInfos) {
                                        int bookId = bookInfo.getBookNo();
                                        java.util.ArrayList<run.yigou.gxzy.data.model.Fang> fangList = 
                                            run.yigou.gxzy.data.local.helper.DataRepository.getFangDetailList(bookId);
                                        
                                        if (fangList != null && !fangList.isEmpty()) {
                                            for (run.yigou.gxzy.data.model.Fang fang : fangList) {
                                                String fangName = fang.getName();
                                                if (fangName != null && !fangName.trim().isEmpty()) {
                                                    fangAliasDict.put(fangName.trim(), fangName.trim());
                                                    aliasCount++;
                                                }
                                            }
                                        }
                                    }
                                    
                                    globalData.putAllFangAlias(fangAliasDict);
                                    android.util.Log.i("HomeFragment", "✅ 网络加载：方剂别名 " + aliasCount + " 条（来自 " + bookInfos.size() + " 本书）");
                                } catch (Exception e) {
                                    android.util.Log.e("HomeFragment", "❌ 网络加载：方剂别名加载失败", e);
                                }
                                
                                // 在主线程更新 UI
                                ThreadUtil.runOnUiThread(() -> {
                                    if (mPagerAdapter == null || mTabAdapter == null) return;
                                    for (TabNav nav : validNavList) {
                                        mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(nav.getNavList()));
                                        mTabAdapter.addItem(nav.getName());
                                    }
                                });
                            });

                        } else {
                        }
                    }


                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        Map<Integer, TabNav> tabNavMap = GlobalDataHolder.getInstance().getNavTabMap();
                        if (tabNavMap != null && !tabNavMap.isEmpty()) {
                            // 从缓存 Map 恢复导航数据
                            for (Map.Entry<Integer, TabNav> entry : tabNavMap.entrySet()) {
                                TabNav value = entry.getValue();
                                mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(value.getNavList()));
                                mTabAdapter.addItem(value.getName());
                            }
                        } else {
                            toast("加载失败: " + e.getMessage());
                            EasyLog.print(e);
                        }
                    }
                });
    }


    public void getAllYaoData() {

        EasyHttp.get(this)
                .api(new YaoContentApi())
                .request(new HttpCallback<HttpData<List<Yao>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<Yao>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            // 在后台线程处理药方数据
                            ThreadUtil.runInBackground(() -> {
                                List<Yao> detailList = data.getData();
                                //将药方数据及别名映射存入 GlobalDataHolder
                                for (Yao yao : detailList) {
                                    GlobalDataHolder.getInstance().putYao(yao.getName(), yao);
                                    if (yao.getYaoList() != null) {
                                        for (String alias : yao.getYaoList()) {
                                            GlobalDataHolder.getInstance().putYao(alias, yao);
                                        }
                                    }
                                }
                                //持久化到数据库
                                DataRepository.saveYaoData(detailList);
                                
                                // 标记已完成，UI 层不再触发重复请求
                                isGetYaoData = false;
                            });
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        isGetYaoData = false;
                    }
                });


        EasyHttp.get(this)
                .api(new YaoAliaApi())
                .request(new HttpCallback<HttpData<List<YaoAlia>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<YaoAlia>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            // 后台处理别名数据
                            ThreadUtil.runInBackground(() -> {
                                // ✅ 使用临时 Map 收集数据，最后调用 putAllYaoAlias 触发状态标记
                                java.util.Map<String, String> yaoAliasDict = new java.util.HashMap<>();
                                for (YaoAlia yaoAlia : data.getData()) {
                                    yaoAliasDict.put(yaoAlia.getBieming(), yaoAlia.getName());
                                }
                                
                                GlobalDataHolder.getInstance().putAllYaoAlias(yaoAliasDict);

                                //持久化别名数据
                                DataRepository.saveYaoAlia(data.getData());
                            });
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        isGetYaoData = false;
                    }
                });


    }

    public void getAllMingCiData() {


        EasyHttp.get(this)
                .api(new MingCiContentApi())
                .request(new HttpCallback<HttpData<List<MingCiContent>>>(this) {
                             @Override
                             public void onSucceed(HttpData<List<MingCiContent>> data) {
                                 if (data != null && !data.getData().isEmpty()) {
                                     // 后台处理名词数据
                                     ThreadUtil.runInBackground(() -> {
                                         List<MingCiContent> detailList = data.getData();
                                         //将名词数据存入 GlobalDataHolder
                                         for (MingCiContent mingCi : detailList) {
                                             GlobalDataHolder.getInstance().putMingCiContent(mingCi.getName(), mingCi);
                                         }
                                         
                                         //持久化名词数据
                                         DataRepository.saveMingCiContent(detailList);
                                         isGetMingCiData = false;
                                     });
                                 }
                             }

                             @Override
                             public void onFail(Exception e) {
                                 super.onFail(e);
                                 isGetMingCiData = false;
                             }
                         }
                );
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
     * CollapsingToolbarLayout 折叠状态变化回调
     * <p>
     * {@link XCollapsingToolbarLayout.OnScrimsListener}
     */
    @SuppressLint("RestrictedApi")
    @Override
    public void onScrimsStateChange(XCollapsingToolbarLayout layout, boolean shown) {
        getStatusBarConfig().statusBarDarkFont(shown).init();
        mHomeSearchView.setTextColor(ContextCompat.getColor(getAttachActivity(), shown ? R.color.black : R.color.white));
        mTvHomeSearchText.setBackgroundResource(shown ? R.drawable.home_search_bar_gray_bg : R.drawable.home_search_bar_transparent_bg);
        mTvHomeSearchText.setTextColor(ContextCompat.getColor(getAttachActivity(), shown ? R.color.black_66_percent : R.color.white_66_percent));
        mSearchView.setSupportImageTintList(ColorStateList.valueOf(getColor(shown ? R.color.common_icon_color : R.color.white)));
        mRefreshView.setSupportImageTintList(ColorStateList.valueOf(getColor(shown ? R.color.common_icon_color : R.color.white)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewPager.setAdapter(null);
        mViewPager.removeOnPageChangeListener(this);
        mTabAdapter.setOnTabListener(null);
    }
}