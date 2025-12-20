package run.yigou.gxzy.ui.activity;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.base.BaseAdapter;
import com.hjq.http.EasyLog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.common.FragmentSetting;
import run.yigou.gxzy.greendao.entity.SearchHistory;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.service.SearchHistoryService;
import run.yigou.gxzy.greendao.service.TabNavBodyService;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.adapter.SearchBookAdapter;
import run.yigou.gxzy.ui.adapter.SearchHistoryAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.Search.SearchKey;
import run.yigou.gxzy.ui.tips.adapter.refactor.RefactoredSearchAdapter;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * 作者:  zhs
 * 时间:  2023-07-13 22:15:51
 * 包名:  run.yigou.gxzy.ui.activity
 * 类名:  BookContentSearchActivity
 * 版本:  1.0
 * 描述:
 */
public final class BookContentSearchActivity extends AppActivity implements BaseAdapter.OnItemClickListener {
    private ClearEditText etSearchKey;
    private Button tvSearchConform;
    private android.widget.TextView numTips; // 搜索结果数量提示
    private WrapRecyclerView lvSearchBooksList;
    private WrapRecyclerView lvHistoryList;
    private LinearLayout llClearHistory;
    private LinearLayout llHistoryView;
    private SearchHistoryService mSearchHistoryService;
    private TabNavBodyService mTabNavBodyService;
    private SearchHistoryAdapter mSearchHistoryAdapter;
    private RefactoredSearchAdapter mSearchBookDetailAdapter;
    private SearchBookAdapter mSearchBookAdapter;

    public String getSearchKey() {
        return searchKey;
    }

    private String searchKey;//搜索关键字
    private List<SearchHistory> mSearchHistories;
    private WrapRecyclerView mLvSearchBooks;
    private ArrayList<SearchKey> searchKeyTextList = new ArrayList<>();
    // private Map<Integer, SingletonNetData> singleDataMap; // 移除
    private LinearLayoutManager layoutManager;

    @Override
    protected int getLayoutId() {
        return R.layout.book_content_search;
    }

    @Override
    protected void initView() {
        init();
        fragmentSetting = AppApplication.application.fragmentSetting;
    }

    @Override
    protected void initData() {
        mTabNavBodyService = DbService.getInstance().mTabNavBodyService;
        mSearchHistoryService = DbService.getInstance().mSearchHistoryService;
        initHistoryList();
        viewDataInit();

        // 获取传递过来的 Intent
        Intent intent = getIntent();
        // 从 Intent 中提取参数
        searchKey = intent.getStringExtra("searchQuery");
        etSearchKey.setText(searchKey);

    }

    private void viewDataInit() {
        etSearchKey.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(final Editable editable) {
                //保存搜索关键字
                searchKey = editable.toString();
                if (!StringHelper.isEmpty(searchKey)) {
                    // 如果内容不为空则重新光标定位
                   etSearchKey.setSelection(searchKey.length());
                    search();
                } else {
                    llClearHistory.setVisibility(View.VISIBLE);
                    llHistoryView.setVisibility(View.VISIBLE);
                    searchKeyTextList.clear();
                    lvSearchBooksList.setVisibility(View.GONE);
                    mLvSearchBooks.setVisibility(View.GONE);
                    mSearchBookDetailAdapter.setSearch(false);
                    // 清空搜索结果数量提示
                    if (numTips != null) {
                        numTips.setText("");
                    }
                }

            }

        });
        etSearchKey.setOnKeyListener((v, keyCode, event) -> {
            //是否是回车键
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                //隐藏键盘
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(getCurrentFocus()
                                .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                //搜索
                search();
            }
            return false;
        });

        tvSearchConform.setOnClickListener(view -> search());
        llClearHistory.setOnClickListener(v -> {
            mSearchHistoryService.clearHistory();
            mSearchHistories.clear();
            toast("清空历史记录成功");
            llClearHistory.setVisibility(View.GONE);
        });
    }

    private void init() {
        // 使用 include_search_bar 中的 ID
        etSearchKey = findViewById(R.id.searchEditText);
        tvSearchConform = findViewById(R.id.tips_btn_search);
        numTips = findViewById(R.id.numTips);
        // 给这个 View 设置沉浸式，避免状态栏遮挡
        ImmersionBar.setTitleBar(this, findViewById(R.id.titlebarwrapper));
        lvSearchBooksList = findViewById(R.id.lv_search_books_list);
        lvHistoryList = findViewById(R.id.lv_history_list);
        llClearHistory = findViewById(R.id.ll_clear_history);
        llHistoryView = findViewById(R.id.ll_history_view);
        mLvSearchBooks = findViewById(R.id.lv_search_books);
    }

    /**
     * 搜索
     */
    private void search() {

        if (!StringHelper.isEmpty(searchKey)) {
            llHistoryView.setVisibility(View.GONE);
            //  setTitle("关键词: " + searchKey);
            // 放到后台执行
            startSearch(searchKey);
        }
    }

    /**
     * 初始化历史列表
     */
    private void initHistoryList() {
        mSearchHistories = mSearchHistoryService.findAllSearchHistory();
        if (mSearchHistories == null || mSearchHistories.isEmpty()) {
            llHistoryView.setVisibility(View.GONE);
            llClearHistory.setVisibility(View.GONE);
        } else {
            mSearchHistoryAdapter = new SearchHistoryAdapter(getActivity());
            mSearchHistoryAdapter.setData(mSearchHistories);
            mSearchHistoryAdapter.setOnItemClickListener(this);
            lvHistoryList.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_BookList_RecyclerView_Color, AppConst.CustomDivider_Height));
            lvHistoryList.setAdapter(mSearchHistoryAdapter);
            llClearHistory.setVisibility(View.VISIBLE);
            llHistoryView.setVisibility(View.VISIBLE);
        }
    }


    private FragmentSetting fragmentSetting;

    /**
     * 初始化搜索列表
     */
    private void initSearchList() {
        mSearchBookDetailAdapter = new RefactoredSearchAdapter(getActivity());
        // 搜索模式默认已开启,不需要setSearch
        lvSearchBooksList.setAdapter(mSearchBookDetailAdapter);
        layoutManager = new LinearLayoutManager(getContext());
        lvSearchBooksList.setLayoutManager(layoutManager);
        lvSearchBooksList.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_Content_RecyclerView_Color, AppConst.CustomDivider_Height));
        lvSearchBooksList.setVisibility(View.GONE);
        mSearchBookDetailAdapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
//                Toast.makeText(getContext(), "组头：groupPosition = " + groupPosition,
//                        Toast.LENGTH_LONG).show();
                RefactoredSearchAdapter expandableAdapter = (RefactoredSearchAdapter) adapter;
                if (expandableAdapter.isExpand(groupPosition)) {
                    expandableAdapter.collapseGroup(groupPosition);
                } else {
                    expandableAdapter.expandGroup(groupPosition);
                }
            }
        });
        //跳转指定章节
        mSearchBookDetailAdapter.setOnJumpSpecifiedItemListener(new RefactoredSearchAdapter.OnJumpSpecifiedItemListener() {
            @Override
            public void onJumpSpecifiedItem(int groupPosition, int childPosition) {
                // 搜索模式已经默认开启,不需要setSearch
                reListAdapter();
                layoutManager.scrollToPositionWithOffset(groupPosition, 0);
                mSearchBookDetailAdapter.expandGroup(groupPosition, true);
            }
        });
        mSearchBookAdapter = new SearchBookAdapter(getActivity());
        mSearchBookAdapter.setData(searchKeyTextList);
        mSearchBookAdapter.setOnItemClickListener(this);
        mLvSearchBooks.setAdapter(mSearchBookAdapter);
        mLvSearchBooks.setVisibility(View.VISIBLE);
        mLvSearchBooks.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_BookList_RecyclerView_Color, AppConst.CustomDivider_Height));
        
        // 更新搜索结果数量提示
        if (numTips != null) {
            int totalResults = 0;
            for (SearchKey key : searchKeyTextList) {
                totalResults += key.getSearchResTotalNum();
            }
            if (totalResults > 0) {
                numTips.setText(String.format("%d个结果", totalResults));
            } else {
                numTips.setText("无结果");
            }
        }
    }

    // 移除 extHandleShangHanData: 逻辑移至搜索内部

    /**
     * 开始搜索 (异步)
     */
    private void startSearch(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return;
        }

        ThreadUtil.runInBackground(() -> {
            searchKeyTextList.clear();
            
            // 1. 获取所有书籍信息
            Map<Integer, TabNavBody> bookMap = GlobalDataHolder.getInstance().getNavTabBodyMap();
            
            // 2. 获取全局别名字典
            GlobalDataHolder globalData = GlobalDataHolder.getInstance();
            Map<String, String> yaoAliasDict = globalData.getYaoAliasDict();
            Map<String, String> fangAliasDict = globalData.getFangAliasDict();
            
            // 3. 遍历书籍
            for (Map.Entry<Integer, TabNavBody> entry : bookMap.entrySet()) {
                int bookId = entry.getKey();
                TabNavBody bookInfo = entry.getValue();
                
                try {
                    // 4. 获取书籍内容 (直接从数据库读取，不污染BookDataManager缓存)
                    List<HH2SectionData> contentList = ConvertEntity.getBookChapterDetailList(bookId);
                    
                    if (contentList == null || contentList.isEmpty()) {
                        continue;
                    }
                    
                    // 5. 伤寒论特殊过滤逻辑
                    if (bookId == AppConst.ShangHanNo) {
                         contentList = filterShangHanData(contentList);
                    }
                    
                    // 6. 执行搜索
                    SearchKeyEntity searchKeyEntity = new SearchKeyEntity(new StringBuilder(keyword));
                    ArrayList<HH2SectionData> searchResults = TipsNetHelper.getSearchHh2SectionData(
                            searchKeyEntity, 
                            contentList, 
                            yaoAliasDict, 
                            fangAliasDict
                    );
                    
                    if (!searchResults.isEmpty()) {
                        searchKeyTextList.add(new SearchKey(
                            keyword, 
                            searchResults.size(), 
                            bookInfo.getBookName(), 
                            bookId, 
                            searchResults
                        ));
                    }
                    
                } catch (Exception e) {
                    EasyLog.print("Search error for book " + bookId + ": " + e.getMessage());
                }
            }
            
            // 7. 更新UI
            ThreadUtil.runOnUiThread(() -> initSearchList());
        });
    }
    
    /**
     * 伤寒论特殊过滤逻辑
     */
    private ArrayList<HH2SectionData> filterShangHanData(List<HH2SectionData> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return new ArrayList<>();
        }
        int size = contentList.size();

        int start = 0;
        int end = size;

        if (!fragmentSetting.isSong_JinKui()) {
            if (!fragmentSetting.isSong_ShangHan()) {
                start = 8;
                end = Math.min(18, size);
            } else {
                end = Math.min(26, size);
            }
        } else {
            if (!fragmentSetting.isSong_ShangHan()) {
                start = 8;
            }
        }

        if (start < size) {
            return new ArrayList<>(contentList.subList(start, end));
        } else {
            return new ArrayList<>(contentList);
        }
    }

    @Override
    public void onBackPressed() {
        if (!StringHelper.isEmpty(searchKey)) {
            if (lvSearchBooksList.getVisibility() == View.GONE)
                super.onBackPressed();
            mLvSearchBooks.setVisibility(View.VISIBLE);
            lvSearchBooksList.setVisibility(View.GONE);
            return;
        }
        super.onBackPressed();
    }

    private SearchKey currentSearchKey;

    private void reListAdapter() {
        // 直接从 SearchKey 中获取 filteredData, 不再依赖 singleDataMap
        List<HH2SectionData> resultList = currentSearchKey.getFilteredData();
        // ArrayList<HH2SectionData> singletonNetData = Objects.requireNonNull(singleDataMap.get(currentSearchKey.getBookNo()), "当前搜索对象:" + currentSearchKey.getBookName()).getContent();
        mSearchBookDetailAdapter.setmGroups(GroupModel.getExpandableGroups((ArrayList<HH2SectionData>) resultList, false));

        mSearchBookDetailAdapter.notifyDataChanged();

    }

    @Override
    public void onItemClick(RecyclerView recyclerView, View itemView, int position) {

        if (recyclerView.getId() == R.id.lv_history_list) {
            etSearchKey.setText(mSearchHistories.get(position).getContent());
            search();
        }
        if (recyclerView.getId() == R.id.lv_search_books) {
            lvSearchBooksList.setVisibility(View.VISIBLE);
            mLvSearchBooks.setVisibility(View.GONE);
            currentSearchKey = searchKeyTextList.get(position);
            ArrayList<ExpandableGroupEntity> sectionData = GroupModel.getExpandableGroups(searchKeyTextList.get(position).getFilteredData(), false);
            mSearchBookDetailAdapter.setmGroups(sectionData);
            mSearchBookDetailAdapter.notifyDataChanged();

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // 空指针检查
        if (mSearchBookDetailAdapter != null) {
            mSearchBookDetailAdapter.setSearch(false);
            mSearchBookDetailAdapter.setOnJumpSpecifiedItemListener(null);
        }


        // 移除 singleDataMap 清理逻辑，因为已经没有该字段了
//        if (singleDataMap != null && singleDataMap.containsKey(AppConst.ShangHanNo)) {
//            SingletonNetData singleData = singleDataMap.get(AppConst.ShangHanNo);
//            if (singleData != null) {
//                singleData.setOnContentUpdateListener(null);
//            }
//        }

    }
}