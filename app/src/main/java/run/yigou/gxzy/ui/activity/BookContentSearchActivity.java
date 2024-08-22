package run.yigou.gxzy.ui.activity;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;

import java.util.ArrayList;
import java.util.List;

import me.gujun.android.taggroup.TagGroup;
import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.Log;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.APPCONST;
import run.yigou.gxzy.common.URLCONST;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.SearchHistory;
import run.yigou.gxzy.greendao.service.BookService;
import run.yigou.gxzy.greendao.service.SearchHistoryService;
import run.yigou.gxzy.http.api.HotBook;
import run.yigou.gxzy.http.api.PageDataOptions;
import run.yigou.gxzy.http.entitymodel.ChapterSearchRes;
import run.yigou.gxzy.http.entitymodel.SearchKeyText;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.adapter.SearchBookAdapter;
import run.yigou.gxzy.ui.adapter.SearchBookDetailAdapter;
import run.yigou.gxzy.ui.adapter.SearchHistoryAdapter;
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

    private BookService mBookService;
    private ClearEditText etSearchKey;
    private AppCompatButton tvSearchConform;
    private WrapRecyclerView lvSearchBooksList;
    private WrapRecyclerView lvHistoryList;
    private LinearLayout llClearHistory;
    private LinearLayout llHistoryView;
    private TagGroup tgSuggestBook;
    private SearchHistoryService mSearchHistoryService;

    private SearchHistoryAdapter mSearchHistoryAdapter;
    private List<String> mSuggestions;
    private SearchBookDetailAdapter mSearchBookDetailAdapter;
    private SearchBookAdapter mSearchBookAdapter;
    public String getSearchKey() {
        return searchKey;
    }

    private String searchKey;//搜索关键字
    //搜索结果
    private List<ChapterSearchRes> mSearchRes;
    //搜索结果分类
    private List<ChapterSearchRes> mSearchResorc;
    private List<SearchKeyText> mSearchKeyTextList;
    private List<SearchHistory> mSearchHistories;
    private WrapRecyclerView mLvSearchBooks;

    private LinearLayout mLlSuggestBooksView;




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
        mSearchHistoryService = new SearchHistoryService();
        mSuggestions =  new ArrayList<>();
        mSearchRes =  new ArrayList<>();
        mSearchResorc =  new ArrayList<>();
        mSearchKeyTextList = new ArrayList<>();
        setTitle("内容搜索");
        // getData();
        initHistoryList();

        viewDataInit();
        getHotBooksData();
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
                if (StringHelper.isEmpty(searchKey)) {
                    search();
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
        tgSuggestBook.setOnTagClickListener(tag -> {
            etSearchKey.setText(tag);
            search();
        });

        llClearHistory.setOnClickListener(v -> {
            mSearchHistoryService.clearHistory();
            mSearchHistories.clear();
            toast("清空历史记录成功");
            llClearHistory.setVisibility(View.GONE);
        });
    }

    private void init() {
        etSearchKey = findViewById(R.id.et_search_key);
        tvSearchConform = findViewById(R.id.tv_search_conform);
        lvSearchBooksList = findViewById(R.id.lv_search_books_list);
        lvHistoryList = findViewById(R.id.lv_history_list);
        llClearHistory = findViewById(R.id.ll_clear_history);
        llHistoryView = findViewById(R.id.ll_history_view);
        tgSuggestBook = findViewById(R.id.tg_suggest_book);
        mLvSearchBooks = findViewById(R.id.lv_search_books);
        mLlSuggestBooksView = findViewById(R.id.ll_suggest_books_view);
    }

    /**
     * 搜索
     */
    private void search() {

        if (StringHelper.isEmpty(searchKey)) {
            //  lvSearchBooksList.setVisibility(View.GONE);
        } else {
            // lvSearchBooksList.setVisibility(View.VISIBLE);
            llHistoryView.setVisibility(View.GONE);
            setTitle("搜索内容: "+ searchKey);
            getData();


        }
    }

    private void getHotBooksData() {
        EasyHttp.get(this)
                .api(new HotBook())
                .request(new HttpCallback<HttpData<List<HotBook.Bean>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<HotBook.Bean>> data) {
                        if (data != null) {
                            List<HotBook.Bean> beanList = data.getData();
                            postDelayed(() -> {
                                mSuggestions.clear();
                                for (HotBook.Bean bean : beanList) {
                                    mSuggestions.add(bean.getBookName());
                                }
                                tgSuggestBook.setTags(mSuggestions);
                            }, 1000);

                        }
                    }
                });

    }

    /**
     * 初始化历史列表
     */
    private void initHistoryList() {
        mSearchHistories = mSearchHistoryService.findAllSearchHistory();
        if (mSearchHistories == null || mSearchHistories.size() == 0) {
            llHistoryView.setVisibility(View.GONE);
            llClearHistory.setVisibility(View.GONE);
        } else {
            mSearchHistoryAdapter = new SearchHistoryAdapter(getActivity());
            mSearchHistoryAdapter.setData(mSearchHistories);
            mSearchHistoryAdapter.setOnItemClickListener(this);
            lvHistoryList.setAdapter(mSearchHistoryAdapter);
            llClearHistory.setVisibility(View.VISIBLE);
            llHistoryView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * 初始化搜索列表
     */
    private void initSearchList() {

        mSearchBookDetailAdapter = new SearchBookDetailAdapter(getActivity());
      //  mSearchBookDetailAdapter.setData(mSearchRes);
        mSearchBookDetailAdapter.setOnItemClickListener((adapterView, view, i) -> {
                Intent intent = new Intent(getActivity(), BookReadActivity.class);
                Book book = new Book();
                book.setDesc(getSearchKey());
                book.setId(mSearchResorc.get(i).getId() + "");
                book.setName(mSearchResorc.get(i).getBookName());
                book.setType(mSearchResorc.get(i).getType());
                book.setChapterUrl(mSearchResorc.get(i).getId() + "");
                book.setBookId(mSearchResorc.get(i).getId() + "");
                book.setSource("Search");
                intent.putExtra(APPCONST.BOOK, book);
                startActivity(intent);
        });
        lvSearchBooksList.setAdapter(mSearchBookDetailAdapter);
        lvSearchBooksList.setVisibility(View.GONE);
        mSearchBookAdapter = new SearchBookAdapter(getActivity());
        mSearchBookAdapter.setData(mSearchKeyTextList);
        mSearchBookAdapter.setOnItemClickListener(this);
        mLvSearchBooks.setAdapter(mSearchBookAdapter);
        mLvSearchBooks.setVisibility(View.VISIBLE);
        mLlSuggestBooksView.setVisibility(View.GONE);

    }

    /**
     * 获取搜索数据
     */
    private void getData() {

        EasyHttp.post(this)
                .api(new PageDataOptions().setUrl(URLCONST.SearchUrl)
                        .setFilter(new PageDataOptions.SearchParameters("Section", searchKey)))
                .request(new HttpCallback<HttpData<List<SearchKeyText>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<SearchKeyText>> data) {
                        if (data.getData().size() > 0) {
                                mSearchKeyTextList.clear();
                                mSearchKeyTextList = data.getData();
                                mSearchRes .clear();
                                for ( SearchKeyText search: mSearchKeyTextList){
                                   mSearchRes.addAll(search.getChapterList());
                                }
                                mSearchHistoryService.addOrUpadteHistory(searchKey);
                                initSearchList();
                        }

                    }
                });
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

    @Override
    public void onItemClick(RecyclerView recyclerView, View itemView, int position) {

        if (recyclerView.getId() == R.id.lv_history_list) {
            etSearchKey.setText(mSearchHistories.get(position).getContent());
            search();
        }
        if (recyclerView.getId() == R.id.lv_search_books) {
           // SearchKeyText searchKeyText = mSearchKeyTextList.get(position);
            mSearchResorc = mSearchKeyTextList.get(position).getChapterList();
            mSearchBookDetailAdapter.setData(mSearchResorc);
            mLvSearchBooks.setVisibility(View.GONE);
            lvSearchBooksList.setVisibility(View.VISIBLE);

        }
    }
}