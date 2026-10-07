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

import run.yigou.gxzy.config.AppStyleConfigProvider;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.Callback;
import run.yigou.gxzy.manager.SearchEntry;
import run.yigou.gxzy.manager.SearchPermissionManager;

import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.data.local.entity.SearchHistory;
import run.yigou.gxzy.data.local.entity.TabNav;
import run.yigou.gxzy.data.local.service.SearchHistoryService;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.ui.activity.BookContentSearchActivity;
import run.yigou.gxzy.ui.reader.search.SearchHistoryAdapter;
import run.yigou.gxzy.ui.reader.fragment.TipsWindowNetFragment;
import run.yigou.gxzy.widget.CustomDividerItemDecoration;
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

    /** 应用数据管理器 */
    private final run.yigou.gxzy.manager.AppDataManager mAppDataManager = run.yigou.gxzy.manager.AppDataManager.getInstance();

    private XCollapsingToolbarLayout mCollapsingToolbarLayout;
    private Toolbar mToolbar;

    private TextView mHomeSearchView;
    private ClearEditText mTvHomeSearchText;
    private AppCompatImageView mSearchView;
    private AppCompatImageView mRefreshView;
    private RecyclerView mTabView;
    private ViewPager mViewPager;
    private TabAdapter mTabAdapter;
    private FragmentPagerAdapter<AppFragment<?>> mPagerAdapter;


    private WrapRecyclerView lvHistoryList;
    private LinearLayout llHistoryView;
    private SearchHistoryService mSearchHistoryService;

    private SearchHistoryAdapter mSearchHistoryAdapter;
    // 初始化在这里：搜索历史改为后台读取后，列表可能在 UI 首屏渲染时还空着，
    // 留 null 会让"还没读到数据就点一条历史"变成空指针。
    private final List<SearchHistory> mSearchHistories = new ArrayList<>();
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
                    // 搜索历史改为后台读取，读到之后回主线程填列表
                    loadSearchHistories();

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
        // 1. 加载样式配置（优先使用缓存）
        loadStyleConfig();
        
        // 2. 初始化搜索历史服务
        mSearchHistoryService = DbService.getInstance().mSearchHistoryService;
        
        // 3. 检查数据加载状态
        if (mAppDataManager.isAllDataLoaded()) {
            // 数据已加载，直接从 GlobalDataHolder 恢复 UI（屏幕翻转、Fragment 重建场景）
            EasyLog.print("HomeFragment", "✅ 数据已加载，从 GlobalDataHolder 恢复 UI");
            loadNavFromGlobalDataHolder();
            mTabAdapter.setOnTabListener(this);
        } else {
            // 首次加载（或进程重启）
            EasyLog.print("HomeFragment", "🚀 首次加载，调用 DataManager");
            
            // 安全检查 LifecycleOwner 是否有效
            if (isAdded() && getContext() != null) {
                loadDataWithLifecycle();
            } else {
                // Fragment 未完全初始化，延迟到安全时机
                EasyLog.print("HomeFragment", "⚠️ Fragment 未完全初始化，延迟加载");
                if (getView() != null) {
                    getView().post(this::loadDataWithLifecycle);
                }
            }
        }
        
        // 4. 清除搜索框焦点
        clearSearchTextFocus();
        
        // 5. 初始化搜索历史列表
        initHistoryList();
        
        llClearHistory.setOnClickListener(v -> {
            clearSearchHistoryInBackground();
            mSearchHistories.clear();
            mSearchHistoryAdapter.notifyDataSetChanged();
            llClearHistory.setVisibility(View.GONE);
            llHistoryView.setVisibility(View.GONE);
            lvHistoryList.setVisibility(View.GONE);
            toast("已清除搜索历史");
        });
    }

    /**
     * 后台读搜索历史，回主线程应用。
     *
     * <p>为什么读要进后台：这是列表页的主线程 IO，历史越长越慢。
     * 串行入口内部用 {@code Executor.execute} 提交，异常不进 Future，
     * 所以任务内必须自己记录，否则读失败永远看不到。
     */
    private void loadSearchHistories() {
        DbService.getInstance().readInBackground(
                new Callable<List<SearchHistory>>() {
                    @Override
                    public List<SearchHistory> call() {
                        return mSearchHistoryService.findAllSearchHistory();
                    }
                },
                new Callback<List<SearchHistory>>() {
                    @Override
                    public void onSuccess(List<SearchHistory> loaded) {
                        applySearchHistories(loaded);
                    }

                    @Override
                    public void onError(Exception e) {
                        // 读失败按"没有历史"处理：与迁移前一致（原来 catch 之后就是传空结果）
                        applySearchHistories(null);
                    }
                });
    }

    /** 把后台读到的历史填进列表（主线程）。 */
    private void applySearchHistories(List<SearchHistory> loaded) {
        // 读在后台：回调回来时 Fragment 可能已被销毁，此时再碰 View 就是操作已回收的视图树
        if (!isAdded()) {
            return;
        }
        mSearchHistories.clear();
        if (loaded != null) {
            mSearchHistories.addAll(loaded);
        }
        if (mSearchHistoryAdapter != null) {
            mSearchHistoryAdapter.notifyDataSetChanged();
        }
        if (!mSearchHistories.isEmpty()) {
            lvHistoryList.setVisibility(View.VISIBLE);
            llClearHistory.setVisibility(View.VISIBLE);
        } else {
            llClearHistory.setVisibility(View.GONE);
        }
    }

    /** 清空搜索历史（写操作进后台，UI 反馈留在主线程）。 */
    private void clearSearchHistoryInBackground() {
        DbService.getInstance().runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                try {
                    mSearchHistoryService.clearHistory();
                } catch (Throwable t) {
                    EasyLog.print(t);
                }
            }
        });
    }

    /** 保存本次搜索关键词（写操作进后台，跳转留在主线程）。 */
    private void saveSearchHistoryInBackground(final String keyword) {
        DbService.getInstance().runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                try {
                    mSearchHistoryService.addOrUpadteHistory(keyword);
                } catch (Throwable t) {
                    EasyLog.print(t);
                }
            }
        });
    }


    /**
     * 初始化搜索历史列表
     */
    private void initHistoryList() {
        mSearchHistoryAdapter = new SearchHistoryAdapter(getActivity());
        mSearchHistoryAdapter.setData(mSearchHistories);
        mSearchHistoryAdapter.setOnItemClickListener(this);
        lvHistoryList.setAdapter(mSearchHistoryAdapter);
        lvHistoryList.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_BookList_RecyclerView_Color, AppConst.CustomDivider_Height));
        llHistoryView.setVisibility(View.GONE);
        llClearHistory.setVisibility(View.GONE);
        loadSearchHistories();
    }

    /**
     * 执行搜索
     */
    private void search() {

        // 搜索权限判定（microfeed 仓 .scratch/search-permission/DESIGN.md §5.2）：
        // 进程内未成功拉到权限 / 当前账号无首页搜索权限 → 禁用。
        // 拉取失败保持静默（INV-1），此处只负责按判定结果拦截并提示。
        if (!SearchPermissionManager.isSearchAllowed(SearchEntry.GLOBAL)) {
            toast(R.string.search_permission_denied);
            return;
        }
        //搜索关键词不为空时保存历史并跳转
        if (!StringHelper.isEmpty(searchKey)) {
            saveSearchHistoryInBackground(searchKey);
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
     * 使用 LifecycleOwner 加载数据
     * 
     * <p>通过 DataManager 统一加载所有业务数据。
     */
    private void loadDataWithLifecycle() {
        mAppDataManager.loadAllDataIfNeeded(this, 
            new Callback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    EasyLog.print("HomeFragment", "✅ 所有数据加载完成，更新 UI");
                    
                    // 数据加载完成，更新 UI
                    ThreadUtil.runOnUiThread(() -> {
                        loadNavFromGlobalDataHolder();
                        mTabAdapter.setOnTabListener(HomeFragment.this);
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    EasyLog.print("HomeFragment", "❌ 数据加载失败: " + e.getMessage());
                    toast("数据加载失败，请检查网络");
                }
            });
    }
    
    /**
     * 从 GlobalDataHolder 加载导航到 UI
     */
    private void loadNavFromGlobalDataHolder() {
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        List<TabNav> navTabs = globalData.getAllNavTabs();
        
        EasyLog.print("HomeFragment", "📊 从 GlobalDataHolder 加载导航：" + navTabs.size() + " 个分类");
        
        for (TabNav nav : navTabs) {
            if (nav.getNavList() != null && !nav.getNavList().isEmpty()) {
                mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(nav.getNavList()));
                mTabAdapter.addItem(nav.getName());
            }
        }
        
        // 通知 ViewPager 数据已改变（避免 IllegalStateException）
        mPagerAdapter.notifyDataSetChanged();
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
        final run.yigou.gxzy.config.AppStyleConfigProvider provider =
                new run.yigou.gxzy.config.AppStyleConfigProvider();

        // 1. 先检查缓存（读文件改到后台，严格模式实测这里在主线程读盘，见 greendao 加固票 11 A 项）
        provider.loadCacheConfigAsync(new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean cacheLoaded) {
                if (cacheLoaded != null && cacheLoaded) {
                    // 2. 有缓存 → 使用缓存配置，不再请求后端
                    EasyLog.print("HomeFragment", "样式配置已从缓存加载（不请求后端）");
                } else {
                    // 3. 无缓存 → 请求后端配置（loadConfig 绑定生命周期，必须在主线程）
                    boolean triggered = provider.loadConfig(HomeFragment.this);
                    if (triggered) {
                        EasyLog.print("HomeFragment", "已触发样式配置加载（从后端）");
                    } else {
                        EasyLog.print("HomeFragment", "样式配置加载失败");
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                EasyLog.print("HomeFragment", "样式配置缓存读取失败");
            }
        });
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
        
        // 重置 DataManager 状态
        mAppDataManager.reset();
        
        // 重新加载所有数据
        loadDataWithLifecycle();
        
        // 刷新样式配置（从服务端获取最新配置）
        AppStyleConfigProvider provider = new AppStyleConfigProvider();
        provider.loadConfig(this);
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