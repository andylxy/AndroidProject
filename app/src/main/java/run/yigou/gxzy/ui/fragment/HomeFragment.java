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

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.AiConfig;
import run.yigou.gxzy.greendao.entity.SearchHistory;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.service.SearchHistoryService;
import run.yigou.gxzy.greendao.service.TabNavService;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.AiConfigApi;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.api.MingCiContentApi;
import run.yigou.gxzy.http.api.YaoAliaApi;
import run.yigou.gxzy.http.api.YaoContentApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.activity.BookContentSearchActivity;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.adapter.NavigationAdapter;
import run.yigou.gxzy.ui.adapter.SearchHistoryAdapter;
import run.yigou.gxzy.ui.adapter.TabAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.DataBeans.YaoAlia;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
import run.yigou.gxzy.app.AppDataInitializer;
import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;
import run.yigou.gxzy.widget.XCollapsingToolbarLayout;

/**
 * author : Android 轮子哥
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
    private List<TabNav> bookNavList;
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
    private String searchKey;//搜索关键字

    // 单例模式，确保实例的唯一性
    private static volatile HomeFragment instance;

    // 私有构造函数，防止外部直接实例化
    private HomeFragment() {
        try {
            // 构造函数中的初始化逻辑
            // 可以在这里添加一些基本的校验逻辑
        } catch (Exception e) {
            // 异常处理
            throw new RuntimeException("Failed to create HomeFragment instance", e);
        }
    }

    /**
     * 创建 HomeFragment 实例的方法。
     * 使用 synchronized 关键字确保线程安全。
     *
     * @return HomeFragment 实例
     */
    public static synchronized HomeFragment newInstance() {
        if (instance == null) {
            instance = new HomeFragment();
        }
        return instance;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.home_fragment;
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // 文本变化之前的操作
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // 文本变化时的操作
        }

        @Override
        public void afterTextChanged(Editable s) {
            // 文本变化之后的操作
        }
    };

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

        // 确保使用新的 adapter（避免单例模式下状态不一致）
        mPagerAdapter = new FragmentPagerAdapter<>(this);
        mTabAdapter = new TabAdapter(getAttachActivity());

        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mTabView.setAdapter(mTabAdapter);
        // 给这个 ToolBar 设置顶部内边距，才能和 TitleBar 进行对齐
        ImmersionBar.setTitleBar(getAttachActivity(), mToolbar);
        //设置渐变监听
        mCollapsingToolbarLayout.setOnScrimsListener(this);
        setOnClickListener(R.id.tv_home_search_text, R.id.iv_home_search, R.id.iv_home_refresh);
        //搜索处理
        mTvHomeSearchText.setMaxLines(1);  // 设置最大行数为 1，限制为单行输入
        mTvHomeSearchText.setSingleLine(true);  // 确保只显示一行
        mTvHomeSearchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // 当 EditText 获取焦点时，添加 TextWatcher
                    mTvHomeSearchText.addTextChangedListener(textWatcher);
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
                    // 当 EditText 失去焦点时，移除 TextWatcher
                    mTvHomeSearchText.removeTextChangedListener(textWatcher);
                    mTabView.setVisibility(View.VISIBLE);
                    mViewPager.setVisibility(View.VISIBLE);
                    llHistoryView.setVisibility(View.GONE);
                    llClearHistory.setVisibility(View.GONE);
                }

            }
        });
        mTvHomeSearchText.setOnKeyListener((v, keyCode, event) -> {

            //是否是回车键
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                //隐藏键盘
                ((InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(Objects.requireNonNull(requireActivity().getCurrentFocus())
                                .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                //搜索
                searchKey = Objects.requireNonNull(mTvHomeSearchText.getText()).toString();
                search();
            }
            return false;
        });
        //EditText 以外的区域时，EditText 失去焦点
        // 为整个布局设置触摸事件监听器
        findViewById(R.id.root_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 如果触摸的是 EditText 以外的地方
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
        // 使 EditText 失去焦点
        mTvHomeSearchText.clearFocus();
        mTvHomeSearchText.setFocusable(false);
        mTvHomeSearchText.setFocusableInTouchMode(false);
        mTvHomeSearchText.setText("");
        //    隐藏软键盘
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mTvHomeSearchText.getWindowToken(), 0);
        }
    }


    @Override
    protected void initData() {
        // 初始化服务（不再调用 tipsSingleDataInit，由 AppDataInitializer 统一处理）
        mTabNavService = DbService.getInstance().mTabNavService;
        // 优先加载本地数据
        loadBookNavigation();
        mTabAdapter.setOnTabListener(this);

        // 只在本地无药物数据时才从网络获取
        if (isGetYaoData && GlobalDataHolder.getInstance().getYaoMap().isEmpty()) {
            ThreadUtil.runInBackground(this::getAllYaoData);
        }
        // 只在本地无名词数据时才从网络获取
        if (isGetMingCiData && GlobalDataHolder.getInstance().getMingCiContentMap().isEmpty()) {
            ThreadUtil.runInBackground(this::getAllMingCiData);
        }
        mSearchHistoryService = DbService.getInstance().mSearchHistoryService;
        // 搜索框默认失去焦点
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
            toast("清空历史记录成功");
        });

    }


    /**
     * 初始化历史列表
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
     * 搜索
     */
    private void search() {

        //   toast(AppConst.Key_Window_Tips);
        //开放全部功能
        if (!AppApplication.application.global_openness) {
            toast(AppConst.Key_Window_Tips);
            return;
        }
        //有限功能不开放全部功能
//        //保存搜索关键字
        if (!StringHelper.isEmpty(searchKey)) {
            mSearchHistoryService.addOrUpadteHistory(searchKey);
            Intent intent = new Intent(getActivity(), BookContentSearchActivity.class);
            // 添加一个参数（Extra）到 Intent
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
            //  toast("点击历史搜索关健字");
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
     * 加载导航数据（优先本地）
     */
    private void loadBookNavigation() {
        // 检查本地数据
        ArrayList<TabNav> localNavList = mTabNavService.findAll();
        if (localNavList != null && !localNavList.isEmpty()) {
            // 本地有数据，使用 post 延迟加载（确保 ViewPager 初始化完成，与网络回调行为一致）
            mViewPager.post(() -> {
                loadNavFromLocal(localNavList);
                EasyLog.print("Loaded navigation from local database");
            });
        } else {
            // 本地无数据，从网络获取（这是异步的）
            getBookInfoList();
        }
    }

    /**
     * 从本地加载导航数据
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
     * 手动刷新数据（从网络重新获取）
     */
    private void refreshDataFromNetwork() {
        toast("正在刷新数据...");
        // 清空现有数据
        while (mPagerAdapter.getCount() > 0) {
            mPagerAdapter.removeFragment(0);
        }
        mTabAdapter.clearData();
        // 强制从网络获取
        getBookInfoList();
        // 同时刷新药物和名词数据
        ThreadUtil.runInBackground(this::getAllYaoData);
        ThreadUtil.runInBackground(this::getAllMingCiData);
    }

    private void getBookInfoList() {

        EasyHttp.get(this)
                .api(new BookInfoNav())
                .request(new HttpCallback<HttpData<List<TabNav>>>(this) {

                    @Override
                    public void onSucceed(HttpData<List<TabNav>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            if (mTabNavService == null || mPagerAdapter == null || mTabAdapter == null) {
                                throw new IllegalStateException("mTabNavService, mPagerAdapter, or mTabAdapter is not initialized");
                            }

                            bookNavList = new CopyOnWriteArrayList<>(data.getData());

                            // 【修复】同时保存到 GlobalDataHolder
                            Map<Integer, TabNav> navTabMap = GlobalDataHolder.getInstance().getNavTabMap();
                            Map<Integer, TabNavBody> navTabBodyMap = GlobalDataHolder.getInstance().getNavTabBodyMap();
                            int order = 0;

                            for (TabNav nav : bookNavList) {
                                // 内容列表存在才添加
                                if (nav.getNavList() != null && !nav.getNavList().isEmpty()) {

                                    // 保存到 GlobalDataHolder
                                    navTabMap.put(order, nav);
                                    for (TabNavBody item : nav.getNavList()) {
                                        if (item.getBookNo() > 0) {
                                            navTabBodyMap.put(item.getBookNo(), item);
                                        }
                                    }
                                    order++;

                                    // 添加到适配器
                                    mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(nav.getNavList()));
                                    mTabAdapter.addItem(nav.getName());

                                }
                            }
                            // 保存到数据库
                            ThreadUtil.runInBackground(() -> {
                                ConvertEntity.saveTabNvaInDb(bookNavList, newInstance());
                            });
                        } else {
                            bookNavList = new ArrayList<>();
                        }
                    }


                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        Map<Integer, TabNav> tabNavMap = GlobalDataHolder.getInstance().getNavTabMap();
                        if (tabNavMap != null && !tabNavMap.isEmpty()) {
                            // 遍历 Map
                            for (Map.Entry<Integer, TabNav> entry : tabNavMap.entrySet()) {
                                // Integer key = entry.getKey();
                                TabNav value = entry.getValue();
                                mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(value.getNavList()));
                                mTabAdapter.addItem(value.getName());
                            }
                        } else {
                            toast("获取数据失败：" + e.getMessage());
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
                            List<Yao> detailList = data.getData();
                            //加载所有药物的数据到 GlobalDataHolder
                            for (Yao yao : detailList) {
                                GlobalDataHolder.getInstance().putYao(yao.getName(), yao);
                                if (yao.getYaoList() != null) {
                                    for (String alias : yao.getYaoList()) {
                                        GlobalDataHolder.getInstance().putYao(alias, yao);
                                    }
                                }
                            }
                            isGetYaoData = false;
                            //保存内容
                            ThreadUtil.runInBackground(() -> {
                                ConvertEntity.saveYaoData(detailList);
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

                            //加载额外的别名的数据
                            // TipsSingleData.getInstance().setYaoData(new HH2SectionData(detailList, 0, "常用本草药物"));
                            //加载额外的别名的数据
                            Map<String, String> yaoAliasDict = GlobalDataHolder.getInstance().getYaoAliasDict();
                            for (YaoAlia yaoAlia : data.getData()) {
                                yaoAliasDict.put(yaoAlia.getBieming(), yaoAlia.getName());
                            }

                            //保存内容
                            ThreadUtil.runInBackground(() -> {
                                //保存数据
                                ConvertEntity.saveYaoAlia(data.getData());
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
                                     List<MingCiContent> detailList = data.getData();
                                     //加载所有名词数据到 GlobalDataHolder
                                     for (MingCiContent mingCi : detailList) {
                                         GlobalDataHolder.getInstance().putMingCiContent(mingCi.getName(), mingCi);
                                     }
                                     isGetMingCiData = false;
                                     //保存内容
                                     ThreadUtil.runInBackground(() -> {
                                         ConvertEntity.saveMingCiContent(detailList);
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
     * CollapsingToolbarLayout 渐变回调
     * <p>
     * {@link XCollapsingToolbarLayout.OnScrimsListener}
     */
    @SuppressLint("RestrictedApi")
    @Override
    public void onScrimsStateChange(XCollapsingToolbarLayout layout, boolean shown) {
        getStatusBarConfig().statusBarDarkFont(shown).init();
        mHomeSearchView.setTextColor(ContextCompat.getColor(getAttachActivity(), shown ? R.color.black : R.color.white));
        mTvHomeSearchText.setBackgroundResource(shown ? R.drawable.home_search_bar_gray_bg : R.drawable.home_search_bar_transparent_bg);
        mTvHomeSearchText.setTextColor(ContextCompat.getColor(getAttachActivity(), shown ? R.color.black60 : R.color.white60));
        mSearchView.setSupportImageTintList(ColorStateList.valueOf(getColor(shown ? R.color.common_icon_color : R.color.white)));
        mRefreshView.setSupportImageTintList(ColorStateList.valueOf(getColor(shown ? R.color.common_icon_color : R.color.white)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewPager.setAdapter(null);
        mViewPager.removeOnPageChangeListener(this);
        mTabAdapter.setOnTabListener(null);
        instance = null;
    }
}