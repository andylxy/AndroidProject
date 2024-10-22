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

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.entity.SearchHistory;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.gen.BeiMingCiDao;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;
import run.yigou.gxzy.greendao.gen.TabNavDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoDao;
import run.yigou.gxzy.greendao.service.BeiMingCiService;
import run.yigou.gxzy.greendao.service.SearchHistoryService;
import run.yigou.gxzy.greendao.service.TabNavBodyService;
import run.yigou.gxzy.greendao.service.TabNavService;
import run.yigou.gxzy.greendao.service.YaoService;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.api.MingCiContentApi;
import run.yigou.gxzy.http.api.YaoContentApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.activity.BookContentSearchActivity;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.adapter.SearchHistoryAdapter;
import run.yigou.gxzy.ui.adapter.TabAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
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
    private List<TabNav> bookNavList;
    private RecyclerView mTabView;
    private ViewPager mViewPager;
    private boolean isGetYaoData = true;
    private boolean isGetMingCiData = true;
    private TabAdapter mTabAdapter;
    private FragmentPagerAdapter<AppFragment<?>> mPagerAdapter;
    private TabNavService mTabNavService;
    private TabNavBodyService mTabNavBodyService;
    //药物信息
    private YaoService mYaoService;
    //别名信息
    private BeiMingCiService mBeiMingCiService;

    private WrapRecyclerView lvHistoryList;
    private LinearLayout llHistoryView;


    public static HomeFragment newInstance() {
        return new HomeFragment();
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

        mTabView = findViewById(R.id.rv_home_tab);
        mViewPager = findViewById(R.id.vp_home_pager);
        llHistoryView = findViewById(R.id.include_book_content_search_ll_history_view).findViewById(R.id.ll_history_view);
        lvHistoryList = findViewById(R.id.include_book_content_search_ll_history_view).findViewById(R.id.lv_history_list);
        llClearHistory = findViewById(R.id.include_book_content_search_ll_history_view).findViewById(R.id.ll_clear_history);
        mPagerAdapter = new FragmentPagerAdapter<>(this);

        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mTabAdapter = new TabAdapter(getAttachActivity());
        mTabView.setAdapter(mTabAdapter);
        // 给这个 ToolBar 设置顶部内边距，才能和 TitleBar 进行对齐
        ImmersionBar.setTitleBar(getAttachActivity(), mToolbar);
        //设置渐变监听
        mCollapsingToolbarLayout.setOnScrimsListener(this);
        setOnClickListener(R.id.tv_home_search_text, R.id.iv_home_search);
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

    private void clearSearchTextFocus() {
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
        mYaoService = DbService.getInstance().mYaoService;
        mBeiMingCiService = DbService.getInstance().mBeiMingCiService;
        mTabNavService = DbService.getInstance().mTabNavService;
        mTabNavBodyService = DbService.getInstance().mTabNavBodyService;
        getBookInfoList();
        mTabAdapter.setOnTabListener(this);
        if (isGetYaoData)
            ThreadUtil.runInBackground((this::getAllYaoData));
        if (isGetMingCiData)
            ThreadUtil.runInBackground((this::getAllMingCiData));
        mSearchHistoryService = DbService.getInstance().mSearchHistoryService;
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

    private SearchHistoryService mSearchHistoryService;

    private SearchHistoryAdapter mSearchHistoryAdapter;
    private List<SearchHistory> mSearchHistories;
    private LinearLayout llClearHistory;
    private String searchKey;//搜索关键字

    /**
     * 初始化历史列表
     */
    private void initHistoryList() {
        mSearchHistories = mSearchHistoryService.findAllSearchHistory();
        mSearchHistoryAdapter = new SearchHistoryAdapter(getActivity());
        mSearchHistoryAdapter.setData(mSearchHistories);
        mSearchHistoryAdapter.setOnItemClickListener(this);
        lvHistoryList.setAdapter(mSearchHistoryAdapter);
        lvHistoryList.addItemDecoration(new CustomDividerItemDecoration());
        llHistoryView.setVisibility(View.GONE);
        llClearHistory.setVisibility(View.GONE);
    }

    /**
     * 搜索
     */
    private void search() {
        //保存搜索关键字
        if (!StringHelper.isEmpty(searchKey)) {
            mSearchHistoryService.addOrUpadteHistory(searchKey);
            Intent intent = new Intent(getActivity(), BookContentSearchActivity.class);
            // 添加一个参数（Extra）到 Intent
            intent.putExtra("searchQuery", searchKey);

            startActivityForResult(intent, (resultCode, data) -> {
//                if (resultCode == RESULT_OK) {
//                    // 获取返回的数据（Extra）
//                    String result = data.getStringExtra("result");
//                    // 处理返回的数据
//                    toast(result);
//                }
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
        int viewId = view.getId();

        switch (viewId) {
            case R.id.tv_home_search_text:
                mTvHomeSearchText.setFocusable(true);
                mTvHomeSearchText.setFocusableInTouchMode(true);
                mTvHomeSearchText.requestFocus();
                break;
            case R.id.iv_home_search:
                searchKey = Objects.requireNonNull(mTvHomeSearchText.getText()).toString();
                search();
                break;
            default:
                EasyLog.print("onClick value: " + viewId);
        }
    }

    private void getBookInfoList() {

        EasyHttp.get(this)
                .api(new BookInfoNav())
                .request(new HttpCallback<HttpData<List<TabNav>>>(this) {

                    @Override
                    public void onSucceed(HttpData<List<TabNav>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            bookNavList = data.getData();
                            for (TabNav nav : bookNavList) {
                                //内容列表存在才添加
                                if (nav.getNavList() != null && !nav.getNavList().isEmpty()) {
                                    String tabNavId = mTabNavService.getUUID();
                                    mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(nav.getNavList()));
                                    mTabAdapter.addItem(nav.getName());
                                    ThreadUtil.runInBackground(() -> {
                                        //保存到数据库
                                        tabNvaInDb(nav, tabNavId);
                                    });
                                }
                            }
                        } else
                            bookNavList = new ArrayList<>();

                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        Map<Integer, TabNav> tabNavMap = TipsSingleData.getInstance().getNavTabMap();
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
                        }
                    }
                });
    }

    private void tabNvaInDb(TabNav nav, String tabNavId) {
        // 当前数据不存则,添加到数据库
        ArrayList<TabNav> navList = mTabNavService.find(TabNavDao.Properties.CaseId.eq(nav.getCaseId()));
        if (navList == null || navList.isEmpty()) {
            nav.setTabNavId(tabNavId);
            try {
                mTabNavService.addEntity(nav);
            } catch (Exception e) {
                // 处理异常，比如记录日志、通知管理员等
                EasyLog.print("Failed to addEntity: " + e.getMessage());
                // 根据具体情况决定是否需要重新抛出异常
                //throw e;
            }

        } else {
            tabNavId = navList.get(0).getTabNavId();
        }
        for (TabNavBody item : nav.getNavList()) {

            if (item.getBookNo() > 0)
                TipsSingleData.getInstance().getNavTabBodyMap().put(item.getBookNo(), item);
            // 当前数据不存则,添加到数据库
            ArrayList<TabNavBody> list = mTabNavBodyService.find(TabNavBodyDao.Properties.BookNo.eq(item.getBookNo()));
            if (list == null || list.isEmpty()) {
                item.setTabNavId(tabNavId);
                item.setTabNavBodyId(mTabNavBodyService.getUUID());
                try {
                    mTabNavBodyService.addEntity(item);
                } catch (Exception e) {
                    // 处理异常，比如记录日志、通知管理员等
                    EasyLog.print("Failed to addEntity: " + e.getMessage());
                    // 根据具体情况决定是否需要重新抛出异常
                    //throw e;
                }

            }
        }
    }

    public void getAllYaoData() {

        EasyHttp.get(this)
                .api(new YaoContentApi())
                .request(new HttpCallback<HttpData<List<Yao>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<Yao>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            List<Yao> detailList = data.getData();
                            //加载所有药物的数据
                            TipsSingleData.getInstance().setYaoData(new HH2SectionData(detailList, 0, "伤寒金匮所有药物"));
                            isGetYaoData = false;
                            //保存内容
                            ThreadUtil.runInBackground(() -> {
                                for (Yao yao : detailList) {
                                    List<ZhongYao> zhongYaoList = mYaoService.getQueryBuilder().where(ZhongYaoDao.Properties.SignatureId.eq(yao.getSignatureId())).list();
                                    if (zhongYaoList != null && !zhongYaoList.isEmpty()) {
                                        ZhongYao zhongYao = zhongYaoList.get(0);
                                        //有更新,与本地数据对比
                                        if (!Objects.equals(zhongYao.getSignature(), yao.getSignature())) {
                                            zhongYao.setText(yao.getText());
                                            zhongYao.setYaoList(String.join(",", yao.getYaoList()));
                                            zhongYao.setName(yao.getName());
                                            zhongYao.setSignature(yao.getSignature());
                                            zhongYao.setID(yao.getID());
                                            try {
                                                mYaoService.updateEntity(zhongYao);
                                            } catch (Exception e) {
                                                // 处理异常，比如记录日志、通知管理员等
                                                EasyLog.print("Failed to updateEntity: " + e.getMessage());
                                                // 根据具体情况决定是否需要重新抛出异常
                                                //throw e;
                                            }
                                        }
                                    } else {

                                        ZhongYao yao1 = new ZhongYao();
                                        yao1.setText(yao.getText());
                                        yao1.setName(yao.getName());
                                        yao1.setYaoList(String.join(",", yao.getYaoList()));
                                        yao1.setID(yao.getID());
                                        yao1.setSignature(yao.getSignature());
                                        yao1.setSignatureId(yao.getSignatureId());
                                        try {
                                            mYaoService.addEntity(yao1);
                                        } catch (Exception e) {
                                            // 处理异常，比如记录日志、通知管理员等
                                            EasyLog.print("Failed to add entity: " + e.getMessage());
                                            // 根据具体情况决定是否需要重新抛出异常
                                            //throw e;
                                        }
                                    }
                                }
                            });

                        }

                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
//                        List<Yao> detailList = new ArrayList<>();
//                        ArrayList<ZhongYao> yaoList = mYaoService.findAll();
//                        for (ZhongYao yao : yaoList) {
//                            Yao yao1 = new Yao();
//                            yao1.setText(yao.getText());
//                            yao1.setName(yao.getName());
//                            //yao1.setYaoList(String.join(",", yao.getYaoList()));
//                            yao1.setYaoList(Arrays.asList(yao.getYaoList()));
//                            yao1.setID(yao.getID());
//                            //yao1.setHeight(yao.getHeight());
//                            detailList.add(yao1);
//                        }
//                        Tips_Single_Data.getInstance().setYaoData(new HH2SectionData(detailList, 0, "伤寒金匮所有药物"));
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
                                     //加载所有药物的数据
                                     TipsSingleData.getInstance().setMingCiData(new HH2SectionData(detailList, 0, "医书相关的名词说明"));
                                     isGetMingCiData = false;
                                     //保存内容
                                     ThreadUtil.runInBackground(() -> {
                                         for (MingCiContent mingCiContent : detailList) {

                                             List<BeiMingCi> beiMingCiList = mBeiMingCiService.getQueryBuilder().where(BeiMingCiDao.Properties.SignatureId.eq(mingCiContent.getSignatureId())).list();
                                             if (beiMingCiList != null && !beiMingCiList.isEmpty()) {
                                                 BeiMingCi locBeiMingCi = beiMingCiList.get(0);
                                                 //有更新,与本地数据对比
                                                 if (!Objects.equals(locBeiMingCi.getSignature(), mingCiContent.getSignature())) {
                                                     locBeiMingCi.setText(mingCiContent.getText());
                                                     locBeiMingCi.setMingCiList(String.join(",", mingCiContent.getYaoList()));
                                                     locBeiMingCi.setName(mingCiContent.getName());
                                                     locBeiMingCi.setSignature(mingCiContent.getSignature());
                                                     locBeiMingCi.setID(mingCiContent.getID());
                                                     try {
                                                         mBeiMingCiService.updateEntity(locBeiMingCi);
                                                     } catch (Exception e) {
                                                         // 处理异常，比如记录日志、通知管理员等
                                                         EasyLog.print("Failed to updateEntity: " + e.getMessage());
                                                         // 根据具体情况决定是否需要重新抛出异常
                                                         //throw e;
                                                     }

                                                 }
                                             } else {
                                                 BeiMingCi beiMingCi = new BeiMingCi();
                                                 beiMingCi.setText(mingCiContent.getText());
                                                 beiMingCi.setName(mingCiContent.getName());
                                                 beiMingCi.setMingCiList(String.join(",", mingCiContent.getYaoList()));
                                                 beiMingCi.setSignature(mingCiContent.getSignature());
                                                 beiMingCi.setSignatureId(mingCiContent.getSignatureId());
                                                 beiMingCi.setID(mingCiContent.getID());
                                                 //yao1.setHeight(yao.getHeight());
                                                 try {
                                                     mBeiMingCiService.addEntity(beiMingCi);
                                                 } catch (Exception e) {
                                                     // 处理异常，比如记录日志、通知管理员等
                                                     EasyLog.print("Failed to add entity: " + e.getMessage());
                                                     // 根据具体情况决定是否需要重新抛出异常
                                                     //throw e;
                                                 }
                                             }
                                         }
                                     });
                                 }

                             }

                             @Override
                             public void onFail(Exception e) {
                                 super.onFail(e);
//                                 List<MingCiContent> detailList = new ArrayList<>();
//                                 ArrayList<BeiMingCi> beiMingCiList = mBeiMingCiService.findAll();
//                                 for (BeiMingCi beiMingCi : beiMingCiList) {
//                                     MingCiContent birdContent = new MingCiContent();
//                                     birdContent.setText(beiMingCi.getText());
//                                     birdContent.setName(beiMingCi.getName());
//                                     //birdContent.setMingCiList(String.join(",", beiMingCi.getMingCiList()));
//                                     birdContent.setYaoList(Arrays.asList(beiMingCi.getMingCiList()));
//                                     birdContent.setID(beiMingCi.getID());
//                                     //yao1.setHeight(yao.getHeight());
//                                     detailList.add(birdContent);
//                                 }
//                                 Tips_Single_Data.getInstance().setMingCiData(new HH2SectionData(detailList, 0, "医书相关的名词说明"));
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewPager.setAdapter(null);
        mViewPager.removeOnPageChangeListener(this);
        mTabAdapter.setOnTabListener(null);
    }
}