package run.yigou.gxzy.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import java.util.Objects;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.SearchHistory;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;
import run.yigou.gxzy.greendao.service.SearchHistoryService;
import run.yigou.gxzy.greendao.service.TabNavBodyService;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.adapter.SearchBookAdapter;
import run.yigou.gxzy.ui.adapter.SearchHistoryAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.Search.SearchKey;
import run.yigou.gxzy.ui.tips.adapter.ExpandableAdapter;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.utils.StringHelper;

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
    private AppCompatButton tvSearchConform;
    private WrapRecyclerView lvSearchBooksList;
    private WrapRecyclerView lvHistoryList;
    private LinearLayout llClearHistory;
    private LinearLayout llHistoryView;
    private SearchHistoryService mSearchHistoryService;
    private TabNavBodyService mTabNavBodyService;
    private SearchHistoryAdapter mSearchHistoryAdapter;
    private ExpandableAdapter mSearchBookDetailAdapter;
    private SearchBookAdapter mSearchBookAdapter;

    public String getSearchKey() {
        return searchKey;
    }

    private String searchKey;//搜索关键字
    private List<SearchHistory> mSearchHistories;
    private WrapRecyclerView mLvSearchBooks;
    private ArrayList<SearchKey> searchKeyTextList = new ArrayList<>();
    private Map<Integer, SingletonNetData> singleDataMap;
    private LinearLayoutManager layoutManager;

    @Override
    protected int getLayoutId() {
        return R.layout.book_content_search;
    }

    @Override
    protected void initView() {
        init();
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
                    search();
                } else {
                    llClearHistory.setVisibility(View.VISIBLE);
                    llHistoryView.setVisibility(View.GONE);
                    searchKeyTextList.clear();
                    lvSearchBooksList.setVisibility(View.GONE);
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
        etSearchKey = findViewById(R.id.et_search_key);
        // 给这个 View 设置沉浸式，避免状态栏遮挡
        ImmersionBar.setTitleBar(this, findViewById(R.id.ll_search_key));
        tvSearchConform = findViewById(R.id.tv_search_conform);
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
            getData();
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
    private int showShanghan;
    private int showJinkui;
    /**
     * 初始化搜索列表
     */
    private void initSearchList() {
        mSearchBookDetailAdapter = new ExpandableAdapter(getActivity());
        lvSearchBooksList.setAdapter(mSearchBookDetailAdapter);
        layoutManager = new LinearLayoutManager(getContext());
        lvSearchBooksList.setLayoutManager(layoutManager);
        lvSearchBooksList.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_BookList_RecyclerView_Color, AppConst.CustomDivider_Height));
        lvSearchBooksList.setVisibility(View.GONE);
        mSearchBookDetailAdapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
//                Toast.makeText(getContext(), "组头：groupPosition = " + groupPosition,
//                        Toast.LENGTH_LONG).show();
                ExpandableAdapter expandableAdapter = (ExpandableAdapter) adapter;
                if (expandableAdapter.isExpand(groupPosition)) {
                    expandableAdapter.collapseGroup(groupPosition);
                } else {
                    expandableAdapter.expandGroup(groupPosition);
                }
            }
        });
        //跳转指定章节
        mSearchBookDetailAdapter.setOnJumpSpecifiedItemListener(new ExpandableAdapter.OnJumpSpecifiedItemListener() {
            @Override
            public void onJumpSpecifiedItem(int groupPosition, int childPosition) {
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
    }

    private void extHandleShangHanData(int bookId) {

        // 默认初始化设置  宋版伤寒,金匮显示
        // 从 SharedPreferences 中读取设置值
        SharedPreferences sharedPreferences = TipsSingleData.getInstance().getSharedPreferences();
        showShanghan = sharedPreferences.getInt(AppConst.Key_Shanghan, 0);
        showJinkui = sharedPreferences.getInt(AppConst.Key_Jinkui, 1);
        // 加载数据处理监听
        singleDataMap.get(bookId).setOnContentUpdateListener(new SingletonNetData.OnContentUpdateListener() {
            @Override
            public ArrayList<HH2SectionData> contentDateUpdate(ArrayList<HH2SectionData> contentList) {
                if (contentList == null || contentList.isEmpty()) {
                    return new ArrayList<>();
                }
                int size = contentList.size();

                int start = 0;
                int end = size;

                if (showJinkui == AppConst.Show_Jinkui_None) {
                    if (showShanghan == AppConst.Show_Shanghan_398) {
                        start = 8;
                        end = Math.min(18, size);
                    } else if (showShanghan == AppConst.Show_Shanghan_AllSongBan) {
                        end = Math.min(26, size);
                    }
                } else if (showJinkui == AppConst.Show_Jinkui_Default) {
                    if (showShanghan == AppConst.Show_Shanghan_398) {
                        start = 8;
                    }
                }

                if (start < size) {
                    return new ArrayList<>(contentList.subList(start, end));
                } else {
                    return contentList;
                }
            }
        });

        //宋版显示修改通知
        singleDataMap.get(bookId).setOnContentShowStatusNotification(new SingletonNetData.OnContentShowStatusNotification() {
            @Override
            public void contentShowStatusNotification(int status) {
                //刷新数据显示
                reListAdapter();
            }
        });
    }
    /**
     * 获取搜索数据
     */
    private void getData() {
        searchKeyTextList.clear();
        singleDataMap = TipsSingleData.getInstance().getMapBookContent();
        // 遍历 Map
        for (Map.Entry<Integer, SingletonNetData> entry : singleDataMap.entrySet()) {
            Integer key = entry.getKey();
            SingletonNetData value = entry.getValue();
            //兼容处理宋版伤寒
            if (key == AppConst.ShangHanNo)
                extHandleShangHanData(key);
            ArrayList<HH2SectionData> filteredData = new ArrayList<>();
            // 处理键值对
            // 检查搜索文本是否有效（不为 null、不为空且不是数字）
            if (searchKey != null && !searchKey.isEmpty() && value != null) {
                try {
                    SearchKeyEntity searchKeyEntity = new SearchKeyEntity(new StringBuilder(searchKey));
                    List<HH2SectionData> searchResults = TipsNetHelper.getSearchHh2SectionData(searchKeyEntity, value);
                    if (!searchResults.isEmpty()) {
                        filteredData.addAll(searchResults);
                    }
                } catch (Exception e) {
                    // 处理异常，例如记录日志或返回默认值
                    EasyLog.print("Error occurred while fetching search results", e.getMessage());
                }
            }
            if (!filteredData.isEmpty()) {
                ArrayList<TabNavBody> keys = mTabNavBodyService.find(TabNavBodyDao.Properties.BookNo.eq(key));
                if (!keys.isEmpty()) {
                    TabNavBody tabNavBody = keys.get(0);
                    if (tabNavBody != null)
                        searchKeyTextList.add(new SearchKey(searchKey, filteredData.size(), tabNavBody.getBookName(), key, filteredData));
                }
            }
        }

        initSearchList();
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
        ArrayList<HH2SectionData> singletonNetData = Objects.requireNonNull(singleDataMap.get(currentSearchKey.getBookNo()), "当前搜索对象:" + currentSearchKey.getBookName()).getContent();
        mSearchBookDetailAdapter.setmGroups(GroupModel.getExpandableGroups(singletonNetData, false));

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
            ArrayList<ExpandableGroupEntity> sectionData = GroupModel.getExpandableGroups(searchKeyTextList.get(position).getFilteredData(), true);
            mSearchBookDetailAdapter.setmGroups(sectionData);
            mSearchBookDetailAdapter.notifyDataChanged();

        }
    }


    @Override
public void onDestroy() {
    super.onDestroy();

    // 空指针检查
    if (mSearchBookDetailAdapter != null) {
        mSearchBookDetailAdapter.setOnJumpSpecifiedItemListener(null);
    }

    if (singleDataMap != null && singleDataMap.containsKey(AppConst.ShangHanNo)) {
         SingletonNetData  singleData = singleDataMap.get(AppConst.ShangHanNo);
        if (singleData != null) {
            singleData.setOnContentShowStatusNotification(null);
            singleData.setOnContentUpdateListener(null);
        }
    }

    // 日志记录
    EasyLog.print("YourTag", "onDestroy: Released all listeners and resources");
}
}