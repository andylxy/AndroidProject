/*
 * ???: AndroidProject
 * ??: HomeFragment.java
 * ??: run.yigou.gxzy.ui.fragment.HomeFragment
 * ?? : Zhs (xiaoyang_02@qq.com)
 * ?????? : 2023?07?05? 23:27:44
 * ??????: 2023?07?05? 17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.home;

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
import run.yigou.gxzy.greendao.entity.AiConfig;
import run.yigou.gxzy.greendao.entity.SearchHistory;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.service.SearchHistoryService;
import run.yigou.gxzy.greendao.service.TabNavService;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.data.remote.api.AiConfigApi;
import run.yigou.gxzy.data.remote.api.BookInfoNav;
import run.yigou.gxzy.data.remote.api.MingCiContentApi;
import run.yigou.gxzy.data.remote.api.StyleConfigApi;
import run.yigou.gxzy.ui.feature.reader.renderer.TipsTextRendererBridge;
import run.yigou.gxzy.data.remote.api.YaoAliaApi;
import run.yigou.gxzy.data.remote.api.YaoContentApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.ui.feature.reader.activity.BookContentSearchActivity;
import run.yigou.gxzy.ui.home.HomeActivity;
import run.yigou.gxzy.ui.home.NavigationAdapter;
import run.yigou.gxzy.ui.feature.search.SearchHistoryAdapter;
import run.yigou.gxzy.ui.home.TabAdapter;
import run.yigou.gxzy.ui.feature.reader.TipsWindowNetFragment;
import run.yigou.gxzy.model.MingCiContent;
import run.yigou.gxzy.widget.CustomDividerItemDecoration;
import run.yigou.gxzy.model.Yao;
import run.yigou.gxzy.model.YaoAlia;
import run.yigou.gxzy.manager.GlobalDataHolder;
import run.yigou.gxzy.app.AppDataInitializer;
import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;
import com.hjq.widget.layout.XCollapsingToolbarLayout;

/**
 * author : Android ???
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : ?? Fragment
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
    private String searchKey;//?????

    // ????????? Fragment ????????
    // private static volatile HomeFragment instance;

    // ?????????????
    public HomeFragment() {
    }

    /**
     * ?? HomeFragment ??????
     *
     * @return HomeFragment ??
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

        // ?????? adapter??????????????
        mPagerAdapter = new FragmentPagerAdapter<>(this);
        mTabAdapter = new TabAdapter(getAttachActivity());

        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mTabView.setAdapter(mTabAdapter);
        // ??? ToolBar ??????????? TitleBar ????
        ImmersionBar.setTitleBar(getAttachActivity(), mToolbar);
        //??????
        mCollapsingToolbarLayout.setOnScrimsListener(this);
        setOnClickListener(R.id.tv_home_search_text, R.id.iv_home_search, R.id.iv_home_refresh);
        //????
        mTvHomeSearchText.setMaxLines(1);  // ??????? 1????????
        mTvHomeSearchText.setSingleLine(true);  // ???????
        mTvHomeSearchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // ? EditText ?????
                    // mTvHomeSearchText.addTextChangedListener(textWatcher); // ???????
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
                    // ? EditText ?????
                    // mTvHomeSearchText.removeTextChangedListener(textWatcher); // ???????
                    mTabView.setVisibility(View.VISIBLE);
                    mViewPager.setVisibility(View.VISIBLE);
                    llHistoryView.setVisibility(View.GONE);
                    llClearHistory.setVisibility(View.GONE);
                }

            }
        });
        mTvHomeSearchText.setOnKeyListener((v, keyCode, event) -> {

            //??????
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                //????
                ((InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(Objects.requireNonNull(requireActivity().getCurrentFocus())
                                .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                //??
                searchKey = Objects.requireNonNull(mTvHomeSearchText.getText()).toString();
                search();
            }
            return false;
        });
        //EditText ???????EditText ????
        // ??????????????
        findViewById(R.id.root_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // ?????? EditText ?????
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
        // ? EditText ????
        mTvHomeSearchText.clearFocus();
        mTvHomeSearchText.setFocusable(false);
        mTvHomeSearchText.setFocusableInTouchMode(false);
        mTvHomeSearchText.setText("");
        //    ?????
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mTvHomeSearchText.getWindowToken(), 0);
        }
    }


    @Override
    protected void initData() {
        // ?????????? tipsSingleDataInit?? AppDataInitializer ?????
        mTabNavService = DbService.getInstance().mTabNavService;
        // ????????
        loadBookNavigation();
        mTabAdapter.setOnTabListener(this);

        // ????????????????
        if (isGetYaoData && GlobalDataHolder.getInstance().getYaoMap().isEmpty()) {
            ThreadUtil.runInBackground(this::getAllYaoData);
        }
        // ????????????????
        if (isGetMingCiData && GlobalDataHolder.getInstance().getMingCiContentMap().isEmpty()) {
            ThreadUtil.runInBackground(this::getAllMingCiData);
        }
        mSearchHistoryService = DbService.getInstance().mSearchHistoryService;
        // ?????????
        clearSearchTextFocus();
        //?????????
        initHistoryList();
        llClearHistory.setOnClickListener(v -> {
            mSearchHistoryService.clearHistory();
            mSearchHistories.clear();
            mSearchHistoryAdapter.notifyDataSetChanged();
            llClearHistory.setVisibility(View.GONE);
            llHistoryView.setVisibility(View.GONE);
            lvHistoryList.setVisibility(View.GONE);
            toast("????????");
        });

         //todo ???
        // ??????? Tips ???????????????
        //getStyleConfig();
    }


    /**
     * ???????
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
     * ????????????
     * ?? Tips ??????????? $r{}, $u{} ?????????????????
     */
    private void getStyleConfig() {
        // ?? EasyHttp ?? GET ??
        EasyHttp.get(this)
                .api(new StyleConfigApi())
                .request(new HttpCallback<HttpData<StyleConfigApi.StyleConfigApiBean>>(this) {
                    
                    @Override
                    public void onSucceed(HttpData<StyleConfigApi.StyleConfigApiBean> result) {
                        // ??????????
                        if (result != null && result.getData() != null && result.getData().getStyles() != null) {
                            // ??????????????????????????????
                            // ?????????????? updateConfigFromApi ????? Map ??????
                            // ??????????????????????
                            TipsTextRendererBridge.updateConfigFromApi(result.getData().getStyles());
                            // EasyLog.print("??? Tips ???????: " + result.getData().getStyles().size());
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        // ?????????????????????
                        // ???????????
                        // EasyLog.print("????????: " + e.getMessage());
                        super.onFail(e);
                    }
                });
    }

    /**
     * ??
     */
    private void search() {

        //   toast(AppConst.Key_Window_Tips);
        //??????
        if (!AppApplication.application.global_openness) {
            toast(AppConst.Key_Window_Tips);
            return;
        }
        //???????????
//        //???????
        if (!StringHelper.isEmpty(searchKey)) {
            mSearchHistoryService.addOrUpadteHistory(searchKey);
            Intent intent = new Intent(getActivity(), BookContentSearchActivity.class);
            // ???????Extra?? Intent
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
            //  toast("?????????");
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
     * ????????????
     */
    private void loadBookNavigation() {
        // ??????
        ArrayList<TabNav> localNavList = mTabNavService.findAll();
        if (localNavList != null && !localNavList.isEmpty()) {
            // ???????? post ??????? ViewPager ????????????????
            mViewPager.post(() -> {
                loadNavFromLocal(localNavList);
                EasyLog.print("Loaded navigation from local database");
            });
        } else {
            // ??????????????????
            getBookInfoList();
        }
    }

    /**
     * ?????????
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
     * ???????????????
     */
    private void refreshDataFromNetwork() {
        toast("??????...");
        // ??????
        while (mPagerAdapter.getCount() > 0) {
            mPagerAdapter.removeFragment(0);
        }
        mTabAdapter.clearData();
        // ???????
        getBookInfoList();
        // ???????????
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
                                // ??? Fragment ???????
                                return;
                            }

                            // ??????????????
                            ThreadUtil.runInBackground(() -> {
                                List<TabNav> navList = new CopyOnWriteArrayList<>(data.getData());
                                
                                // ???????? GlobalDataHolder
                                Map<Integer, TabNav> navTabMap = GlobalDataHolder.getInstance().getNavTabMap();
                                Map<Integer, TabNavBody> navTabBodyMap = GlobalDataHolder.getInstance().getNavTabBodyMap();
                                int order = 0;

                                // ?? UI ???????
                                final List<TabNav> validNavList = new ArrayList<>();
                                
                                for (TabNav nav : navList) {
                                    if (nav.getNavList() != null && !nav.getNavList().isEmpty()) {
                                        // ????
                                        navTabMap.put(order, nav);
                                        for (TabNavBody item : nav.getNavList()) {
                                            if (item.getBookNo() > 0) {
                                                navTabBodyMap.put(item.getBookNo(), item);
                                            }
                                        }
                                        order++;
                                        validNavList.add(nav);
                                    }
                                }
                                
                                // ??????
                                ConvertEntity.saveTabNvaInDb(navList, HomeFragment.this);
                                
                                // ???????? UI
                                ThreadUtil.runOnUiThread(() -> {
                                    if (mPagerAdapter == null || mTabAdapter == null) return;
                                    
                                    bookNavList = navList;
                                    for (TabNav nav : validNavList) {
                                        mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(nav.getNavList()));
                                        mTabAdapter.addItem(nav.getName());
                                    }
                                });
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
                            // ?? Map
                            for (Map.Entry<Integer, TabNav> entry : tabNavMap.entrySet()) {
                                // Integer key = entry.getKey();
                                TabNav value = entry.getValue();
                                mPagerAdapter.addFragment(TipsWindowNetFragment.newInstance(value.getNavList()));
                                mTabAdapter.addItem(value.getName());
                            }
                        } else {
                            toast("???????" + e.getMessage());
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
                            // ??????????????
                            ThreadUtil.runInBackground(() -> {
                                List<Yao> detailList = data.getData();
                                //?????????? GlobalDataHolder
                                for (Yao yao : detailList) {
                                    GlobalDataHolder.getInstance().putYao(yao.getName(), yao);
                                    if (yao.getYaoList() != null) {
                                        for (String alias : yao.getYaoList()) {
                                            GlobalDataHolder.getInstance().putYao(alias, yao);
                                        }
                                    }
                                }
                                //????????
                                ConvertEntity.saveYaoData(detailList);
                                
                                // ?????? UI ???????????????????
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
                            // ??????
                            ThreadUtil.runInBackground(() -> {
                                //??????????
                                Map<String, String> yaoAliasDict = GlobalDataHolder.getInstance().getYaoAliasDict();
                                for (YaoAlia yaoAlia : data.getData()) {
                                    yaoAliasDict.put(yaoAlia.getBieming(), yaoAlia.getName());
                                }

                                //????
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
                                     // ??????
                                     ThreadUtil.runInBackground(() -> {
                                         List<MingCiContent> detailList = data.getData();
                                         //????????? GlobalDataHolder
                                         for (MingCiContent mingCi : detailList) {
                                             GlobalDataHolder.getInstance().putMingCiContent(mingCi.getName(), mingCi);
                                         }
                                         
                                         //????
                                         ConvertEntity.saveMingCiContent(detailList);
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
     * CollapsingToolbarLayout ????
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
        // instance = null; // ????
    }
}