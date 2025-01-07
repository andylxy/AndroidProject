/*
 * 项目名: AndroidProject
 * 类名: TipsBookReadActivity.java
 * 包名: run.yigou.gxzy.ui.activity.TipsBookReadActivity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月08日 10:50:43
 * 上次修改时间: 2024年09月08日 10:50:43
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import com.hjq.base.BaseDialog;
import com.hjq.http.EasyLog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;


import java.util.ArrayList;

import run.yigou.gxzy.EventBus.ShowUpdateNotificationEvent;
import run.yigou.gxzy.EventBus.TipsFragmentSettingEventNotification;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.common.BookArgs;
import run.yigou.gxzy.common.FragmentSetting;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.gen.BookDao;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.dialog.MessageDialog;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.adapter.ExpandableAdapter;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.utils.ThreadUtil;


public class TipsBookNetReadFragment extends AppFragment<AppActivity> {


    private WrapRecyclerView rvList;
    private ClearEditText clearEditText;
    private ExpandableAdapter adapter;
    private BookArgs bookArgs;

    /**
     *
     */
    private TextView numTips;
    /**
     *
     */
    private int bookId = 0;
    private int bookLastReadPosition;
    private String searchText = null;
    private Button tipsBtnSearch;


    private LinearLayoutManager layoutManager;
    /**
     * 当前选中的章节索引
     */
    private int currentIndex = -1;
    private boolean isShowBookCollect = false;
    /**
     * 数据传递
     */

    private SingletonNetData singletonNetData;


// 在 onDestroy 中释放资源

    private OnBackPressedCallback onBackPressedCallback;


    // 单例模式，确保实例的唯一性
    private static volatile TipsBookNetReadFragment instance;

    // 私有构造函数，防止外部直接实例化
    private TipsBookNetReadFragment() {
        try {
            // 构造函数中的初始化逻辑
            // 可以在这里添加一些基本的校验逻辑
        } catch (Exception e) {
            // 异常处理
            throw new RuntimeException("Failed to create TipsBookNetReadFragment instance", e);
        }
    }

    public static synchronized TipsBookNetReadFragment newInstance(BookArgs bookArgs) {
        if (instance == null) {
            instance = new TipsBookNetReadFragment();
        }
        if (bookArgs != null){
            instance.bookArgs = bookArgs;
        }
        return instance;
    }

    /**
     * @return
     */
    @Override
    protected int getLayoutId() {
        return R.layout.tips_book_read_activity_group_list;
    }

    /**
     *
     */
    @SuppressLint("CutPasteId")
    @Override
    protected void initView() {
        // 初始化视图
        rvList = findViewById(R.id.tips_book_read_activity_group_list);
        if (rvList == null) {
            throw new IllegalStateException("rvList not found");
        }
        clearEditText =findViewById(R.id.searchEditText);
        if (clearEditText == null) {
            throw new IllegalStateException("clearEditText not found");
        }
        tipsBtnSearch =findViewById(R.id.tips_btn_search);
        if (tipsBtnSearch == null) {
            throw new IllegalStateException("tipsBtnSearch not found");
        }
        numTips =findViewById(R.id.numTips);
        if (numTips == null) {
            throw new IllegalStateException("numTips not found");
        }

        // 设置 RecyclerView 布局管理器和装饰
        layoutManager = new LinearLayoutManager(getContext());
        rvList.setLayoutManager(layoutManager);
        rvList.addItemDecoration(new CustomDividerItemDecoration());

        // 设置按钮点击监听
        tipsBtnSearch.setOnClickListener(this);

        // 设置文本变化监听
        clearEditText.addTextChangedListener(new TextWatcher() {

            private final Runnable runnable = () -> {
                String text = clearEditText.getText().toString();
                if (charSequenceIsEmpty(text)) {
                    reListAdapter(true, false);
                    numTips.setText("");
                    adapter.setSearch(false);
                } else {
                    adapter.setSearch(true);
                    setSearchText(text);
                }
            };

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                EasyLog.print("clearEditText", "onTextChanged: " + s);
                removeCallbacks(runnable);
                postDelayed(runnable, 300); // 延迟 300 毫秒执行
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        // 注册事件
        XEventBus.getDefault().register(TipsBookNetReadFragment.this);
    }


    private void fragmentOnBackPressed() {
        // 显示返回键
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 处理自定义逻辑
                TabNavBody navTabBody = TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
                if (navTabBody == null) {
                    return;
                }

                try {
                    handleBackPress(navTabBody);
                } catch (Exception e) {
                    EasyLog.print("HandleBackPress", "Error handling back press: " + e.getMessage());
                }
            }

            private void handleBackPress(TabNavBody navTabBody) {
                ArrayList<Book> books = queryBooks(navTabBody);
                if (books == null || books.isEmpty()) {
                    showAddToBookshelfDialog(navTabBody);
                } else {
                    updateBookInfo(books);
                }
            }

            private ArrayList<Book> queryBooks(TabNavBody navTabBody) {
                try {
                    return DbService.getInstance().mBookService.find(BookDao.Properties.BookNo.eq(navTabBody.getBookNo()));
                } catch (Exception e) {
                    EasyLog.print("QueryBooks", "Error querying books: " + e.getMessage());
                    return null;
                }
            }

            private void showAddToBookshelfDialog(TabNavBody navTabBody) {
                new MessageDialog.Builder(getContext())
                        .setTitle("加入书架")
                        .setMessage(navTabBody.getBookName())
                        .setConfirm(getString(R.string.common_confirm))
                        .setCancel(getString(R.string.common_cancel))
                        .setListener(new MessageDialog.OnListener() {
                            @Override
                            public void onConfirm(BaseDialog dialog) {
                                addBookToBookshelf(navTabBody);
                            }

                            @Override
                            public void onCancel(BaseDialog dialog) {
                                allowSystemBackPress();
                            }
                        })
                        .show();
            }

            private void addBookToBookshelf(TabNavBody navTabBody) {
                Book book = new Book();
                book.setBookId(DbService.getInstance().mBookService.getUUID());
                book.setBookNo(navTabBody.getBookNo());
                book.setBookName(navTabBody.getBookName());
                book.setAuthor(navTabBody.getAuthor());
                book.setHistoriographerNumb(currentIndex == -1 ? 0 : currentIndex);
                book.setLastReadPosition(currentIndex == -1 ? 0 : currentIndex);

                try {
                    DbService.getInstance().mBookService.addEntity(book);
                    refreshBookshelf();
                    allowSystemBackPress();
                } catch (Exception e) {
                    EasyLog.print("AddBook", "Error adding book to bookshelf: " + e.getMessage());
                }
            }

            private void updateBookInfo(ArrayList<Book> books) {
                if (currentIndex != -1) {
                    Book book = books.get(0);
                    book.setLastReadPosition(currentIndex);
                    book.setHistoriographerNumb(currentIndex);

                    try {
                        DbService.getInstance().mBookService.updateEntity(book);
                    } catch (Exception e) {
                        EasyLog.print("UpdateBook", "Error updating book info: " + e.getMessage());
                    }
                }
                allowSystemBackPress();
            }

            private void refreshBookshelf() {
                BookCollectCaseFragment.newInstance().RefreshLayout();
            }

            private void allowSystemBackPress() {
                postDelayed(() -> {
                    setEnabled(false); // 禁用当前回调
                    requireActivity().onBackPressed(); // 调用系统返回
                }, AppConst.postDelayMillis);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

    }


    /**
     * 初始化数据
     */
    @Override
    protected void initData() {
        try {
            // 获取传递的书本编号
            retrieveBookArguments(bookArgs);

            //fragmentSetting = AppApplication.getApplication().getFragmentSetting();
            // 获取指定书籍数据
            singletonNetData = TipsSingleData.getInstance().getMapBookContent(bookId);
            // 兼容处理宋版伤寒
            if (bookId == AppConst.ShangHanNo) {
                // initializeShanghanSettings();
                setShanghanContentUpdateListener();
            }
            // Fragment 处理返回键动作,是否保存阅读

            if (AppApplication.getApplication().fragmentSetting.isShuJie())
                fragmentOnBackPressed();

            //setContentShowStatusNotification();
            // 加载到UI显示
            initializeAdapter();
            setHeaderClickListener();
            setJumpSpecifiedItemListener();
            //setHttpUpdateStatusNotification();
            rvList.setAdapter(adapter);
            refreshData();
        } catch (Exception e) {
            e.printStackTrace();
            EasyLog.print("TipsBookNetReadFragment initData", e.getMessage());
            // 处理异常，例如显示错误提示
        }
    }

    private void retrieveBookArguments(BookArgs bookArgs) {

        if (clearEditText != null) {
            clearEditText.setText("");
        }
        if (bookArgs != null) {
            bookId = bookArgs.getBookNo();
            bookLastReadPosition = bookArgs.getBookLastReadPosition();
            isShowBookCollect = bookArgs.isShowBookCollect();
        } else {
            bookId = 0;
            bookLastReadPosition = 0;
            isShowBookCollect = false;
            searchText = null;

        }
    }

    SingletonNetData.OnContentUpdateListener contentUpdateListener;

    private void setShanghanContentUpdateListener() {

        if (contentUpdateListener == null) {
            contentUpdateListener = new SingletonNetData.OnContentUpdateListener() {
                @Override
                public ArrayList<HH2SectionData> contentDateUpdate(ArrayList<HH2SectionData> contentList) {
                    if (contentList == null || contentList.isEmpty()) {
                        return new ArrayList<>();
                    }

                    int size = contentList.size();

                    int start = 0;
                    int end = size;


                    if (!AppApplication.getApplication().fragmentSetting.isSong_JinKui()) {
                        if (!AppApplication.getApplication().fragmentSetting.isSong_ShangHan()) {
                            start = 8;
                            end = Math.min(18, size);
                        } else {
                            end = Math.min(26, size);
                        }
                    } else {
                        if (!AppApplication.getApplication().fragmentSetting.isSong_ShangHan()) {
                            start = 8;
                        }
                    }

                    if (start < size) {
                        return new ArrayList<>(contentList.subList(start, end));
                    } else {
                        return contentList;
                    }
                }
            };
            singletonNetData.setOnContentUpdateListener(contentUpdateListener);
        }
    }

    @Subscribe(priority = 1)
    public void onEvent(TipsFragmentSettingEventNotification event) {
        ThreadUtil.runOnUiThread(() -> {

            // 兼容处理宋版伤寒
            if (bookId == AppConst.ShangHanNo) {
                setShanghanContentUpdateListener();
            }
            refreshData();
            // Fragment 处理返回键动作,是否保存阅读
            setBackPressedCallback();
            // EasyLog.print("TipsBookNetReadFragment onEvent", "onEvent");

        });
    }

    private void setBackPressedCallback() {
        if (AppApplication.getApplication().fragmentSetting.isShuJie()) {
            if (onBackPressedCallback == null)
                fragmentOnBackPressed();
        } else {
            if (onBackPressedCallback != null) {
                onBackPressedCallback.remove();
                onBackPressedCallback = null;
            }
        }
    }


    private void initializeAdapter() {
        adapter = new ExpandableAdapter(getContext());
    }

    private void setHeaderClickListener() {
        adapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
                ExpandableAdapter expandableAdapter = (ExpandableAdapter) adapter;
                if (expandableAdapter.isExpand(groupPosition)) {
                    expandableAdapter.collapseGroup(groupPosition);
                } else {
                    expandableAdapter.expandGroup(groupPosition);
                }
                // 记录当前点击位置,0则表示没有点击,或者点击了第一章.
                if (isShowBookCollect)
                    currentIndex = groupPosition;
            }


        });
        adapter.setOnHeaderLongClickListener(new GroupedRecyclerViewAdapter.OnHeaderLongClickListener() {

            /**
             * @param adapter2
             * @param holder
             * @param groupPosition
             * @return
             */
            @Override
            public boolean onHeaderLongClick(GroupedRecyclerViewAdapter adapter2, BaseViewHolder holder, int groupPosition) {
                // 搜索状态不响应长按
                if(adapter.getSearch()) return true;
                TipsNetHelper.showListDialog(getContext(), AppConst.reData_Type)
                        .setListener((dialog, position, string) -> {
                            if (string.equals("重新下载全部数据")) {
                                //通知显示已经变更
                                ShowUpdateNotificationEvent showUpdateNotification = singletonNetData.getShowUpdateNotification();
                                if (!showUpdateNotification.isUpdateNotification()) {
                                    // 标记正在重新下载数据
                                    showUpdateNotification.setUpdateNotification(true);
                                    XEventBus.getDefault().post(showUpdateNotification);
                                } else {
                                    toast("正在重新下载数据!!!!");
                                }
                            }

                        })
                        .show();

                return true;
            }
        });
    }

    ExpandableAdapter.OnJumpSpecifiedItemListener onJumpSpecifiedItemListener;

    private void setJumpSpecifiedItemListener() {
        if (onJumpSpecifiedItemListener == null) {
            onJumpSpecifiedItemListener = new ExpandableAdapter.OnJumpSpecifiedItemListener() {
                @Override
                public void onJumpSpecifiedItem(int groupPosition, int childPosition) {
                    clearEditText.setText("");
                    numTips.setText("");
                    postDelayed(()->{
                        layoutManager.scrollToPositionWithOffset(groupPosition, 0);
                        adapter.expandGroup(groupPosition, true);
                    },300);

                }
            };
            adapter.setOnJumpSpecifiedItemListener(onJumpSpecifiedItemListener);
        }
    }


    private void refreshData() {
        reListAdapter(true, false);
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        refreshData();
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        adapter.setOnHeaderClickListener(null);
        singletonNetData.setOnContentUpdateListener(null);
        //singletonNetData.setOnContentShowStatusNotification(null);
        singletonNetData.setOnContentUpdateListener(null);
        adapter.setOnJumpSpecifiedItemListener(null);
        //singletonNetData.setOnContentUpdateHttpDataNotification(null);
        if (rvList != null) {
            rvList.setAdapter(null);
            rvList.setLayoutManager(null);
            rvList.removeItemDecorationAt(0);
        }
        contentUpdateListener = null;
        onJumpSpecifiedItemListener = null;
        singletonNetData = null;
        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
        }
        instance =null;
        // 注销事件
        XEventBus.getDefault().unregister(TipsBookNetReadFragment.this);
    }


    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.tips_btn_search) {

            if (this.searchText == null) {
                reListAdapter(true, false);
                adapter.setSearch(false);
            } else {
                setSearchText(this.searchText);
                adapter.setSearch(true);
            }
        }
    }

    /**
     * @param init     true  初始化显示 ,false 搜索结果 显示
     * @param isExpand false 表头不展开, true 展开
     */
    private void reListAdapter(boolean init, boolean isExpand) {
        if (bookId != 0) {

            if (init) {
                adapter.setmGroups(GroupModel.getExpandableGroups(singletonNetData.getContent(), isExpand));
                //如果有上次阅读记录，则定位到上次阅读位置
                if (isShowBookCollect) {
                    layoutManager.scrollToPositionWithOffset(bookLastReadPosition, 0);
                    adapter.expandGroup(bookLastReadPosition, true);
                }
            } else {
                //搜索结果
               // if (!singletonNetData.getSearchResList().isEmpty())
                    adapter.setmGroups(GroupModel.getExpandableGroups(singletonNetData.getSearchResList(), isExpand));
            }
            adapter.notifyDataChanged();
        }
    }

    /**
     * 判断 CharSequence 是否为空。
     *
     * @param charSequence 需要判断的 CharSequence
     * @return 如果为 null 或长度为 0，则返回 true；否则返回 false
     */
    public boolean charSequenceIsEmpty(CharSequence charSequence) {
        boolean bl = false;
        if (charSequence == null || charSequence.length() == 0) {
            this.searchText = null;
            bl = true;
        }
        return bl;
    }

    @SuppressLint("DefaultLocale")
    public void setSearchText(String searchText) {
        this.searchText = searchText;
        // 重置匹配结果数量
        // 检查搜索文本是否有效（不为 null、不为空且不是数字）
        if (searchText != null && !searchText.isEmpty() /*&& !TipsHelper.isNumeric(searchText)*/) {
            SearchKeyEntity searchKeyEntity = new SearchKeyEntity(new StringBuilder(searchText));
            ArrayList<HH2SectionData> filteredData = TipsNetHelper.getSearchHh2SectionData(searchKeyEntity, singletonNetData);
            // 更新展示的数据和结果
            singletonNetData.setSearchResList(filteredData);
            if (this.numTips != null) {
                this.numTips.setText(String.format("%d个结果", searchKeyEntity.getSearchResTotalNum()));
            }
            if (this.adapter != null) {
                reListAdapter(false, true);
            }
        } else {
            if (this.adapter != null) {
                reListAdapter(true, false);
            }
        }

    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TipsBookNetReadFragment.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

}
