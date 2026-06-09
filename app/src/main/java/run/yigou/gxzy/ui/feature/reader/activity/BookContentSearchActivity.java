package run.yigou.gxzy.ui.feature.reader.activity;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import run.yigou.gxzy.log.EasyLog;
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
import run.yigou.gxzy.data.GlobalDataHolder;
import run.yigou.gxzy.ui.feature.reader.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.feature.reader.entity.GroupModel;
import run.yigou.gxzy.ui.feature.reader.entity.SearchKeyEntity;
import run.yigou.gxzy.model.HH2SectionData;
import run.yigou.gxzy.ui.feature.reader.helper.TipsNetHelper;
import run.yigou.gxzy.ui.feature.reader.repository.BookRepository;
import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * 书籍内容搜索Activity
 * 提供书籍内容的搜索功能，支持关键词搜索、历史记录、结果展示等
 * 
 * 功能特点：
 * 1. 实时搜索：支持500ms防抖的实时搜索
 * 2. 历史记录：保存和显示搜索历史
 * 3. 结果展示：支持书籍列表和详细内容两种展示方式
 * 4. 智能过滤：针对伤寒论等特殊书籍提供过滤功能
 * 
 * 搜索流程：
 * 1. 用户输入搜索关键词
 * 2. 系统在所有书籍内容中进行匹配
 * 3. 返回包含关键词的书籍列表
 * 4. 用户点击书籍查看详细匹配结果
 * 
 * @author zhs
 * @since 2023-07-13
 */
/**
 * 书籍内容搜索Activity
 * 提供书籍内容的搜索功能，支持关键词搜索、历史记录、结果展示等
 * 
 * 功能特点：
 * 1. 实时搜索：支持500ms防抖的实时搜索
 * 2. 历史记录：保存和显示搜索历史
 * 3. 结果展示：支持书籍列表和详细内容两种展示方式
 * 4. 智能过滤：针对伤寒论等特殊书籍提供过滤功能
 * 
 * 搜索流程：
 * 1. 用户输入搜索关键词
 * 2. 系统在所有书籍内容中进行匹配
 * 3. 返回包含关键词的书籍列表
 * 4. 用户点击书籍查看详细匹配结果
 * 
 * @author zhs
 * @since 2023-07-13
 */
public final class BookContentSearchActivity extends AppActivity implements BaseAdapter.OnItemClickListener {

    /**
     * 搜索防抖延迟时间（毫秒）
     */
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 500L;
    
    /**
     * 伤寒论书籍ID
     */
    private static final int SHANGHAN_BOOK_ID = AppConst.ShangHanNo;
    
    /**
     * 伤寒论金匮要略开始索引（从0开始，第9章）
     */
    private static final int SHANGHAN_JINKUI_START_INDEX = 8;
    
    /**
     * 伤寒论金匮要略结束索引（第18章）
     */
    private static final int SHANGHAN_JINKUI_END_INDEX = 18;
    
    /**
     * 伤寒论主体部分结束索引（第26章）
     */
    private static final int SHANGHAN_MAIN_END_INDEX = 26;
    /**
     * 搜索输入框
     */
    private ClearEditText etSearchKey;
    /**
     * 搜索确认按钮
     */
    private Button tvSearchConform;
    /**
     * 搜索结果数量提示
     */
    private android.widget.TextView numTips;
    /**
     * 搜索书籍详情列表
     */
    private WrapRecyclerView lvSearchBooksList;
    /**
     * 搜索历史列表
     */
    private WrapRecyclerView lvHistoryList;
    /**
     * 清空历史记录布局
     */
    private LinearLayout llClearHistory;
    /**
     * 历史记录视图布局
     */
    private LinearLayout llHistoryView;
    /**
     * 搜索历史服务
     */
    private SearchHistoryService mSearchHistoryService;
    /**
     * 书籍导航服务
     */
    private TabNavBodyService mTabNavBodyService;
    /**
     * 搜索历史适配器
     */
    private SearchHistoryAdapter mSearchHistoryAdapter;
    /**
     * 搜索书籍详情适配器
     */
    private RefactoredSearchAdapter mSearchBookDetailAdapter;
    /**
     * 搜索书籍适配器
     */
    private SearchBookAdapter mSearchBookAdapter;

    /**
     * 获取当前搜索关键词
     * @return 搜索关键词
     */
    public String getSearchKey() {
        return searchKey;
    }

    /**
     * 搜索关键字
     */
    private String searchKey;
    /**
     * 搜索历史列表
     */
    private List<SearchHistory> mSearchHistories;
    /**
     * 搜索书籍列表视图
     */
    private WrapRecyclerView mLvSearchBooks;
    /**
     * 搜索结果列表
     */
    private ArrayList<SearchKey> searchKeyTextList = new ArrayList<>();
    /**
     * 布局管理器
     */
    private LinearLayoutManager layoutManager;
    /**
     * 主线程Handler，用于防抖搜索
     */
    private final android.os.Handler mHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    /**
     * 搜索任务Runnable
     */
    private Runnable mSearchRunnable;
    /**
     * 书籍数据仓库
     */
    private BookRepository mBookRepository;

    @Override
    protected int getLayoutId() {
        return R.layout.book_content_search;
    }

    @Override
    protected void initView() {
        setupViews();
        setupListeners();
        setupServices();
    }

    @Override
    protected void initData() {
        setupInitialData();
        setupAdapters();
        setupInitialSearch();
    }

    /**
     * 设置服务
     */
    private void setupServices() {
        mTabNavBodyService = DbService.getInstance().mTabNavBodyService;
        mSearchHistoryService = DbService.getInstance().mSearchHistoryService;
        mBookRepository = new BookRepository();
    }

    /**
     * 设置初始数据
     */
    private void setupInitialData() {
        initHistoryList();
        // 默认初始化 TipsNetHelper 上下文，防止空指针（使用伤寒论作为默认值）
        TipsNetHelper.setBookContext(mBookRepository, AppConst.ShangHanNo);
    }

    /**
     * 设置适配器
     */
    private void setupAdapters() {
        initAdapters();
    }

    /**
     * 设置初始搜索
     */
    private void setupInitialSearch() {
        // 获取传递过来的 Intent
        Intent intent = getIntent();
        // 从 Intent 中提取参数
        searchKey = intent.getStringExtra("searchQuery");
        if (!StringHelper.isEmpty(searchKey)) {
            etSearchKey.setText(searchKey);
            etSearchKey.setSelection(searchKey.length());
            // 如果有初始搜索词，直接开始搜索，不需要防抖
            search();
        }
    }

    /**
     * 初始化适配器
     */
    private void initAdapters() {
        // 初始化搜索书籍适配器
        setupSearchBookAdapter();
        
        // 初始化搜索详情适配器
        setupSearchDetailAdapter();
        
        // 初始化历史记录适配器（如果需要）
        setupHistoryAdapter();
    }

    /**
     * 设置搜索书籍适配器
     */
    private void setupSearchBookAdapter() {
        mSearchBookAdapter = new SearchBookAdapter(getActivity());
        mSearchBookAdapter.setOnItemClickListener(this);
        
        setupRecyclerView(mLvSearchBooks, mSearchBookAdapter, 
                         AppConst.CustomDivider_BookList_RecyclerView_Color);
    }

    /**
     * 设置搜索详情适配器
     */
    private void setupSearchDetailAdapter() {
        mSearchBookDetailAdapter = new RefactoredSearchAdapter(getActivity());
        
        // 设置头部点击监听器
        mSearchBookDetailAdapter.setOnHeaderClickListener((adapter, holder, groupPosition) -> {
            RefactoredSearchAdapter expandableAdapter = (RefactoredSearchAdapter) adapter;
            if (expandableAdapter.isExpand(groupPosition)) {
                expandableAdapter.collapseGroup(groupPosition);
            } else {
                expandableAdapter.expandGroup(groupPosition);
            }
        });
        
        // 设置跳转指定项监听器
        mSearchBookDetailAdapter.setOnJumpSpecifiedItemListener((groupPosition, childPosition) -> {
            reListAdapter();
            layoutManager.scrollToPositionWithOffset(groupPosition, 0);
            mSearchBookDetailAdapter.expandGroup(groupPosition, true);
        });

        layoutManager = new LinearLayoutManager(getContext());
        setupRecyclerView(lvSearchBooksList, mSearchBookDetailAdapter, 
                         AppConst.CustomDivider_Content_RecyclerView_Color);
        lvSearchBooksList.setVisibility(View.GONE);
    }

    /**
     * 通用的RecyclerView设置方法
     */
    private void setupRecyclerView(RecyclerView recyclerView, RecyclerView.Adapter<?> adapter, int dividerColor) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new CustomDividerItemDecoration(dividerColor, AppConst.CustomDivider_Height));
    }

    /**
     * 处理搜索文本变化
     * @param editable 文本内容
     */
    private void handleSearchTextChanged(final Editable editable) {
        //保存搜索关键字
        searchKey = editable.toString();
        
        // 移除之前的搜索任务
        cancelPendingSearch();

        if (!StringHelper.isEmpty(searchKey)) {
            // 500ms 防抖
            scheduleSearch();
        } else {
            handleEmptySearch();
        }
    }

    /**
     * 从键盘执行搜索
     */
    private void performSearchFromKeyboard() {
        //隐藏键盘
        hideKeyboard();
        //搜索
        search();
    }

    /**
     * 隐藏软键盘
     */
    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    /**
     * 取消待执行的搜索任务
     */
    private void cancelPendingSearch() {
        if (mSearchRunnable != null) {
            mHandler.removeCallbacks(mSearchRunnable);
            mSearchRunnable = null;
        }
    }

    /**
     * 调度搜索任务
     */
    private void scheduleSearch() {
        mSearchRunnable = this::search;
        mHandler.postDelayed(mSearchRunnable, SEARCH_DEBOUNCE_DELAY_MS);
    }

    /**
     * 清空搜索历史
     */
    private void clearSearchHistory() {
        mSearchHistoryService.clearHistory();
        mSearchHistories.clear();
        toast("清空历史记录成功");
        llClearHistory.setVisibility(View.GONE);
    }

    /**
     * 处理空搜索
     */
    private void handleEmptySearch() {
        // 显示历史记录
        showHistoryViews();
        
        // 清空搜索结果
        clearSearchResults();
        
        // 重置适配器状态
        resetAdaptersState();
        
        // 清空结果数量提示
        clearResultCount();
    }

    /**
     * 清空搜索结果
     */
    private void clearSearchResults() {
        searchKeyTextList.clear();
        setSearchResultVisibility(false);
    }

    /**
     * 重置适配器状态
     */
    private void resetAdaptersState() {
        if (mSearchBookDetailAdapter != null) {
            mSearchBookDetailAdapter.setSearch(false);
        }
    }

    /**
     * 清空结果数量提示
     */
    private void clearResultCount() {
        if (numTips != null) {
            numTips.setText("");
        }
    }

    /**
     * 初始化视图组件
     */
    private void setupViews() {
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
     * 设置监听器
     */
    private void setupListeners() {
        setupSearchListeners();
        setupHistoryListeners();
    }

    /**
     * 设置搜索相关监听器
     */
    private void setupSearchListeners() {
        etSearchKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // 不需要实现
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // 不需要实现
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                handleSearchTextChanged(editable);
            }
        });

        etSearchKey.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                performSearchFromKeyboard();
                return true;
            }
            return false;
        });

        tvSearchConform.setOnClickListener(view -> search());
    }

    /**
     * 设置历史记录监听器
     */
    private void setupHistoryListeners() {
        llClearHistory.setOnClickListener(v -> clearSearchHistory());
    }

    /**
     * 搜索
     */
    private void search() {
        if (!StringHelper.isEmpty(searchKey)) {
            llHistoryView.setVisibility(View.GONE);
            // 放到后台执行
            startSearch(searchKey);
        }
    }

    /**
     * 初始化历史列表
     */
    private void initHistoryList() {
        mSearchHistories = mSearchHistoryService.findAllSearchHistory();
        
        if (isHistoryEmpty()) {
            hideHistoryViews();
        } else {
            setupHistoryAdapter();
            showHistoryViews();
        }
    }

    /**
     * 检查历史记录是否为空
     */
    private boolean isHistoryEmpty() {
        return mSearchHistories == null || mSearchHistories.isEmpty();
    }

    /**
     * 隐藏历史记录视图
     */
    private void hideHistoryViews() {
        llHistoryView.setVisibility(View.GONE);
        llClearHistory.setVisibility(View.GONE);
    }

    /**
     * 设置历史记录适配器
     */
    private void setupHistoryAdapter() {
        mSearchHistoryAdapter = new SearchHistoryAdapter(getActivity());
        mSearchHistoryAdapter.setData(mSearchHistories);
        mSearchHistoryAdapter.setOnItemClickListener(this);
        
        setupRecyclerView(lvHistoryList, mSearchHistoryAdapter, 
                         AppConst.CustomDivider_BookList_RecyclerView_Color);
    }

    /**
     * 显示历史记录视图
     */
    private void showHistoryViews() {
        llClearHistory.setVisibility(View.VISIBLE);
        llHistoryView.setVisibility(View.VISIBLE);
    }


    /**
     * 片段设置
     */
    private FragmentSetting fragmentSetting;

    /**
     * 初始化搜索列表
     */
    private void initSearchList() {
        if (isFinishing() || isDestroyed()) return;

        // 更新搜索书籍适配器数据
        updateSearchBookAdapter();
        
        // 设置搜索结果视图可见性
        setSearchResultVisibility(true);
        
        // 更新搜索结果数量提示
        updateSearchResultCount();
    }

    /**
     * 更新搜索书籍适配器数据
     */
    private void updateSearchBookAdapter() {
        if (mSearchBookAdapter != null) {
            mSearchBookAdapter.setData(searchKeyTextList);
        }
    }

    /**
     * 设置搜索结果视图可见性
     * @param showBookList 是否显示书籍列表
     */
    private void setSearchResultVisibility(boolean showBookList) {
        mLvSearchBooks.setVisibility(showBookList ? View.VISIBLE : View.GONE);
        lvSearchBooksList.setVisibility(showBookList ? View.GONE : View.VISIBLE);
    }

    /**
     * 更新搜索结果数量提示
     */
    private void updateSearchResultCount() {
        if (numTips == null) {
            return;
        }
        
        int totalResults = calculateTotalResults();
        String resultText = totalResults > 0 ? String.format("%d个结果", totalResults) : "无结果";
        numTips.setText(resultText);
    }

    /**
     * 计算总结果数
     */
    private int calculateTotalResults() {
        int totalResults = 0;
        for (SearchKey key : searchKeyTextList) {
            totalResults += key.getSearchResTotalNum();
        }
        return totalResults;
    }

    // 移除 extHandleShangHanData: 逻辑移至搜索内部

    /**
     * 开始搜索 (异步)
     * 优化了异常处理和性能
     * 
     * @param keyword 搜索关键词
     */
    private void startSearch(String keyword) {
        if (StringHelper.isEmpty(keyword)) {
            return;
        }

        // 保存搜索历史
        saveSearchHistory(keyword);

        ThreadUtil.runInBackground(() -> {
            try {
                ArrayList<SearchKey> tempResults = performSearchInBackground(keyword);
                
                // 更新UI
                ThreadUtil.runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    // 确认关键词未变（处理并发结果乱序）
                    if (!keyword.equals(etSearchKey.getText().toString())) return;
                    
                    updateSearchResults(tempResults);
                });
            } catch (Exception e) {
                android.util.Log.e("BCSearchActivity", "Search failed: " + e.getMessage(), e);
                ThreadUtil.runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        toast("搜索失败，请重试");
                        handleEmptySearch();
                    }
                });
            }
        });
    }

    /**
     * 在后台执行搜索
     */
    private ArrayList<SearchKey> performSearchInBackground(String keyword) {
        ArrayList<SearchKey> tempResults = new ArrayList<>();
        
        // 获取所有书籍信息
        Map<Integer, TabNavBody> bookMap = GlobalDataHolder.getInstance().getNavTabBodyMap();
        if (bookMap == null || bookMap.isEmpty()) {
            return tempResults;
        }
        
        // 获取全局别名字典
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        Map<String, String> yaoAliasDict = globalData.getYaoAliasDict();
        Map<String, String> fangAliasDict = globalData.getFangAliasDict();
        
        // 遍历书籍进行搜索
        for (Map.Entry<Integer, TabNavBody> entry : bookMap.entrySet()) {
            int bookId = entry.getKey();
            TabNavBody bookInfo = entry.getValue();
            
            if (bookInfo == null) {
                continue;
            }
            
            try {
                SearchKey searchResult = searchInBook(bookId, bookInfo, keyword, yaoAliasDict, fangAliasDict);
                if (searchResult != null) {
                    tempResults.add(searchResult);
                }
            } catch (Exception e) {
                android.util.Log.e("BCSearchActivity", "Search error for book " + bookId + ": " + e.getMessage(), e);
            }
        }
        
        return tempResults;
    }

    /**
     * 在单本书籍中搜索
     */
    private SearchKey searchInBook(int bookId, TabNavBody bookInfo, String keyword, 
                                  Map<String, String> yaoAliasDict, Map<String, String> fangAliasDict) {
        // 获取书籍内容 (直接从数据库读取，不污染BookDataManager缓存)
        List<HH2SectionData> contentList = ConvertEntity.getBookChapterDetailList(bookId);
        
        if (contentList == null || contentList.isEmpty()) {
            return null;
        }
        
        // 伤寒论特殊过滤逻辑
        if (bookId == SHANGHAN_BOOK_ID) {
             contentList = filterShangHanData(contentList);
        }
        
        // 执行搜索
        SearchKeyEntity searchKeyEntity = new SearchKeyEntity(new StringBuilder(keyword));
        ArrayList<HH2SectionData> searchResults = TipsNetHelper.getSearchHh2SectionData(
                searchKeyEntity, 
                contentList, 
                yaoAliasDict, 
                fangAliasDict
        );
        
        if (searchResults.isEmpty()) {
            return null;
        }
        
        return new SearchKey(
            keyword, 
            searchResults.size(), 
            bookInfo.getBookName(), 
            bookId, 
            searchResults
        );
    }

    /**
     * 更新搜索结果
     */
    private void updateSearchResults(ArrayList<SearchKey> results) {
        searchKeyTextList.clear();
        searchKeyTextList.addAll(results);
        initSearchList();
    }

    /**
     * 保存搜索历史
     */
    private void saveSearchHistory(String keyword) {
        try {
            if (mSearchHistoryService != null) {
                mSearchHistoryService.addOrUpadteHistory(keyword);
            }
        } catch (Exception e) {
            android.util.Log.e("BCSearchActivity", "Failed to save search history: " + e.getMessage(), e);
        }
    }
    
    /**
     * 伤寒论特殊过滤逻辑
     * 根据设置过滤伤寒论的不同部分
     */
    private ArrayList<HH2SectionData> filterShangHanData(List<HH2SectionData> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return new ArrayList<>();
        }
        
        int size = contentList.size();
        int start = 0;
        int end = size;

        // 根据设置确定过滤范围
        if (!fragmentSetting.isSong_JinKui()) {
            if (!fragmentSetting.isSong_ShangHan()) {
                // 只显示金匮要略部分（第9-18章）
                start = SHANGHAN_JINKUI_START_INDEX;
                end = Math.min(SHANGHAN_JINKUI_END_INDEX, size);
            } else {
                // 只显示伤寒论部分（第1-26章）
                end = Math.min(SHANGHAN_MAIN_END_INDEX, size);
            }
        } else {
            if (!fragmentSetting.isSong_ShangHan()) {
                // 从金匮要略开始显示
                start = SHANGHAN_JINKUI_START_INDEX;
            }
            // 否则显示全部内容
        }

        if (start < size) {
            return new ArrayList<>(contentList.subList(start, end));
        } else {
            return new ArrayList<>(contentList);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (!StringHelper.isEmpty(searchKey)) {
                // 如果有搜索结果，先返回搜索结果列表
                if (lvSearchBooksList.getVisibility() == View.GONE) {
                    super.onBackPressed();
                } else {
                    // 返回书籍列表
                    mLvSearchBooks.setVisibility(View.VISIBLE);
                    lvSearchBooksList.setVisibility(View.GONE);
                }
                return;
            }
            super.onBackPressed();
        } catch (Exception e) {
            android.util.Log.e("BCSearchActivity", "Error in onBackPressed", e);
            // 降级处理：直接调用父类方法
            super.onBackPressed();
        }
    }

    /**
     * 当前搜索的Key对象
     */
    private SearchKey currentSearchKey;

    /**
     * 重新设置适配器数据
     * 用于刷新搜索结果详情列表
     */
    private void reListAdapter() {
        if (currentSearchKey == null) {
            return;
        }
        
        // 直接从 SearchKey 中获取 filteredData
        List<HH2SectionData> resultList = currentSearchKey.getFilteredData();
        if (resultList == null || resultList.isEmpty()) {
            return;
        }
        
        ArrayList<ExpandableGroupEntity> groups = GroupModel.getExpandableGroups(
            (ArrayList<HH2SectionData>) resultList, false);
        mSearchBookDetailAdapter.setmGroups(groups);
        mSearchBookDetailAdapter.notifyDataChanged();
    }

    @Override
    public void onItemClick(RecyclerView recyclerView, View itemView, int position) {
        try {
            if (recyclerView.getId() == R.id.lv_history_list) {
                handleHistoryItemClick(position);
            } else if (recyclerView.getId() == R.id.lv_search_books) {
                handleSearchBookItemClick(position);
            }
        } catch (Exception e) {
            android.util.Log.e("BCSearchActivity", "Error in onItemClick", e);
            toast("操作失败，请重试");
        }
    }

    /**
     * 处理历史记录项点击
     */
    private void handleHistoryItemClick(int position) {
        if (position >= 0 && position < mSearchHistories.size()) {
            etSearchKey.setText(mSearchHistories.get(position).getContent());
            search();
        }
    }

    /**
     * 处理搜索书籍项点击
     */
    private void handleSearchBookItemClick(int position) {
        if (position >= 0 && position < searchKeyTextList.size()) {
            lvSearchBooksList.setVisibility(View.VISIBLE);
            mLvSearchBooks.setVisibility(View.GONE);
            currentSearchKey = searchKeyTextList.get(position);

            // 设置 TipsNetHelper 上下文，以便详情页链接点击能获取正确数据
            if (mBookRepository == null) {
                 mBookRepository = new BookRepository();
            }
            TipsNetHelper.setBookContext(mBookRepository, currentSearchKey.getBookNo());

            ArrayList<ExpandableGroupEntity> sectionData = GroupModel.getExpandableGroups(
                searchKeyTextList.get(position).getFilteredData(), false);
            mSearchBookDetailAdapter.setmGroups(sectionData);
            mSearchBookDetailAdapter.notifyDataChanged();
        }
    }


    @Override
    public void onDestroy() {
        try {
            // 第一步：取消待执行的搜索任务
            cancelPendingSearch();
            
            // 第二步：清理适配器监听器
            cleanupAdapterListeners();
            
            // 第三步：清理搜索相关资源
            cleanupSearchResources();
            
        } catch (Exception e) {
            android.util.Log.e("BCSearchActivity", "Error during onDestroy cleanup", e);
        } finally {
            // 最后：调用父类销毁方法
            super.onDestroy();
        }
    }

    /**
     * 清理适配器监听器
     */
    private void cleanupAdapterListeners() {
        if (mSearchBookDetailAdapter != null) {
            mSearchBookDetailAdapter.setSearch(false);
            mSearchBookDetailAdapter.setOnJumpSpecifiedItemListener(null);
        }
        
        if (mSearchBookAdapter != null) {
            mSearchBookAdapter.setOnItemClickListener(null);
        }
        
        if (mSearchHistoryAdapter != null) {
            mSearchHistoryAdapter.setOnItemClickListener(null);
        }
    }

    /**
     * 清理搜索相关资源
     */
    private void cleanupSearchResources() {
        // 清理搜索结果
        if (searchKeyTextList != null) {
            searchKeyTextList.clear();
        }
        
        if (mSearchHistories != null) {
            mSearchHistories.clear();
        }
        
        // 清理当前搜索关键字
        searchKey = null;
        currentSearchKey = null;
    }
}