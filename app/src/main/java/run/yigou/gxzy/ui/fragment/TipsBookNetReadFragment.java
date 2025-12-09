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
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
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
import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;


import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.EventBus.TipsFragmentSettingEventNotification;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.common.BookArgs;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.gen.BookDao;
import run.yigou.gxzy.greendao.gen.ChapterDao;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.BookFangApi;
import run.yigou.gxzy.http.api.ChapterContentApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.dialog.MessageDialog;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.adapter.ExpandableAdapter;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.ui.tips.contract.TipsBookReadContract;
import run.yigou.gxzy.ui.tips.presenter.TipsBookReadPresenter;
import run.yigou.gxzy.ui.tips.repository.BookRepository;
import run.yigou.gxzy.utils.ThreadUtil;


public class TipsBookNetReadFragment extends AppFragment<AppActivity> 
        implements TipsBookReadContract.View, ComponentCallbacks2 {


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
    /**
     * 是否保存到书架
     */
    private boolean isShowBookCollect = false;
    /**
     * 数据传递
     */

    private SingletonNetData singletonNetData;

    /**
     * MVP 架构组件
     */
    private TipsBookReadPresenter presenter;

    // 在 onDestroy 中释放资源

    private OnBackPressedCallback onBackPressedCallback;


//    // 单例模式，确保实例的唯一性
//    private static volatile TipsBookNetReadFragment instance;
//
//    // 私有构造函数，防止外部直接实例化
//    private TipsBookNetReadFragment() {
//        try {
//            // 构造函数中的初始化逻辑
//            // 可以在这里添加一些基本的校验逻辑
//        } catch (Exception e) {
//            // 异常处理
//            throw new RuntimeException("Failed to create TipsBookNetReadFragment instance", e);
//        }
//    }

    public static synchronized TipsBookNetReadFragment newInstance(BookArgs bookArgs) {
//        if (instance == null) {
//            instance = new TipsBookNetReadFragment();
//        }
//        if (bookArgs != null) {
//            instance.bookArgs = bookArgs;
//        }
        TipsBookNetReadFragment instance = new TipsBookNetReadFragment();
        instance.bookArgs = bookArgs;
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
        clearEditText = findViewById(R.id.searchEditText);

        tipsBtnSearch = findViewById(R.id.tips_btn_search);

        numTips = findViewById(R.id.numTips);


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

            // 获取指定书籍数据
            singletonNetData = TipsSingleData.getInstance().getMapBookContent(bookId);
            // 兼容处理宋版伤寒
            if (bookId == AppConst.ShangHanNo) {
                setShanghanContentUpdateListener();
            }
            // Fragment 处理返回键动作,是否保存阅读

            if (AppApplication.getApplication().fragmentSetting.isShuJie())
                fragmentOnBackPressed();

            // 初始化 MVP 架构
            presenter = new TipsBookReadPresenter(this);
            presenter.onViewCreated();

            // 加载到UI显示
            initializeAdapter();
            setHeaderClickListener();
            setJumpSpecifiedItemListener();
            //setHttpUpdateStatusNotification();
            rvList.setAdapter(adapter);
            adapter.setSearch(false);
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

    private boolean isShowUpdateNotification = true;

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

                // ✅ 智能下载：按需下载 + 预加载
                triggerChapterDownload(groupPosition);

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
                if (adapter.getSearch()) return true;
                TipsNetHelper.showListDialog(getContext(), AppConst.reData_Type)
                        .setListener((dialog, position, string) -> {
                            if (string.equals("重新下本章节")) {
                                if (isShowUpdateNotification) {
                                    isShowUpdateNotification = false;
                                    
                                    // ✅ 使用新的下载管理器重新下载
                                    reloadChapter(groupPosition);
                                } else {
                                    toast("正在重新下本章节数据!!!!");
                                }
                            }
                        })
                        .show();

                return true;
            }
        });
    }

    /**
     * 重新下载章节
     * 用户长按选择"重新下本章节"时调用
     * 
     * @param groupPosition 章节索引
     */
    private void reloadChapter(int groupPosition) {
        if (singletonNetData == null || chapterList == null) {
            toast("数据未加载，无法重新下载");
            return;
        }

        try {
            ArrayList<HH2SectionData> contentList = singletonNetData.getContent();
            if (contentList == null || groupPosition >= contentList.size()) {
                toast("章节索引越界");
                return;
            }

            HH2SectionData section = contentList.get(groupPosition);
            Chapter chapter = findChapterBySignatureId(section.getSignatureId());
            
            if (chapter == null) {
                toast("未找到章节信息");
                return;
            }

            toast("开始重新下载: " + chapter.getChapterHeader());
            
            // 使用旧的 getChapterList 方法重新下载
            getChapterList(chapter, contentList);
            
            // 重新下载完成后，重置标志
            postDelayed(() -> {
                isShowUpdateNotification = true;
            }, 2000);

        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "重新下载章节异常: " + e.getMessage());
            toast("重新下载失败: " + e.getMessage());
            isShowUpdateNotification = true;
        }
    }

    private ExpandableAdapter.OnJumpSpecifiedItemListener onJumpSpecifiedItemListener;

    private void setJumpSpecifiedItemListener() {
        if (onJumpSpecifiedItemListener == null) {
            onJumpSpecifiedItemListener = new ExpandableAdapter.OnJumpSpecifiedItemListener() {
                @Override
                public void onJumpSpecifiedItem(int groupPosition, int childPosition) {
                    clearEditText.setText("");
                    numTips.setText("");
                    postDelayed(() -> {
                        layoutManager.scrollToPositionWithOffset(groupPosition, 0);
                        adapter.expandGroup(groupPosition, true);
                    }, 300);

                }
            };
            adapter.setOnJumpSpecifiedItemListener(onJumpSpecifiedItemListener);
        }
    }

    /**
     * 触发章节智能下载
     * 
     * 流程：
     * 1. 检查章节是否已下载
     * 2. 未下载则高优先级下载
     * 3. 下载完成后触发预加载
     * 
     * @param groupPosition 章节索引
     */
    private void triggerChapterDownload(int groupPosition) {
        if (singletonNetData == null || chapterList == null) {
            return;
        }

        try {
            // 获取当前章节数据
            ArrayList<HH2SectionData> contentList = singletonNetData.getContent();
            if (contentList == null || groupPosition >= contentList.size()) {
                return;
            }

            HH2SectionData section = contentList.get(groupPosition);
            if (section == null) {
                return;
            }

            // 查找对应的章节对象
            Chapter chapter = findChapterBySignatureId(section.getSignatureId());
            if (chapter == null) {
                EasyLog.print("TipsBookNetReadFragment", "未找到章节: signatureId=" + section.getSignatureId());
                return;
            }

            // 通过 Presenter 处理章节点击
            if (presenter != null) {
                presenter.onChapterClick(groupPosition);
            }

        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "触发下载异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据 signatureId 查找章节对象
     * 
     * @param signatureId 章节签名 ID
     * @return 章节对象，未找到返回 null
     */
    private Chapter findChapterBySignatureId(long signatureId) {
        if (chapterList == null || chapterList.isEmpty()) {
            return null;
        }

        for (Chapter chapter : chapterList) {
            if (chapter != null && chapter.getSignatureId() == signatureId) {
                return chapter;
            }
        }
        return null;
    }

    /**
     * 更新章节内容到 UI
     * 
     * @param groupPosition 章节索引
     * @param sectionData 章节数据
     */
    public void updateChapterContent(int groupPosition, HH2SectionData sectionData) {
        if (singletonNetData == null || adapter == null || sectionData == null) {
            return;
        }

        try {
            ArrayList<HH2SectionData> contentList = singletonNetData.getContent();
            if (contentList != null && groupPosition < contentList.size()) {
                // 更新数据
                contentList.set(groupPosition, sectionData);
                
                // 刷新 UI
                ExpandableGroupEntity groupEntity = GroupModel.getExpandableGroupEntity(false, sectionData);
                adapter.getmGroups().set(groupPosition, groupEntity);
                adapter.notifyGroupChanged(groupPosition);
                
                EasyLog.print("TipsBookNetReadFragment", "章节内容已更新: " + sectionData.getHeader());
            }
        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "更新章节内容失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ArrayList<Chapter> chapterList;

    private void bookInitData() {

        EasyLog.print("TipsBookNetReadFragment", "========== bookInitData 开始 ==========");
        
        singletonNetData = TipsSingleData.getInstance().getMapBookContent(bookId);
        EasyLog.print("TipsBookNetReadFragment", "singletonNetData=" + (singletonNetData != null ? "存在" : "null"));
        
        if (singletonNetData.getYaoAliasDict() == null)
            singletonNetData.setYaoAliasDict(singletonNetData.getYaoAliasDict());
        if (singletonNetData.getFangAliasDict() == null)
            singletonNetData.setFangAliasDict(singletonNetData.getFangAliasDict());

        //加载书本相关的药方

        TabNavBody book = TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
        EasyLog.print("TipsBookNetReadFragment", "book=" + (book != null ? book.getBookName() : "null"));
        
        if (book != null) {
            chapterList = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(book.getBookNo()));
            EasyLog.print("TipsBookNetReadFragment", "chapterList size=" + (chapterList != null ? chapterList.size() : "null"));
            //加载书本相关的章节
            getBookData(book);
        } else {
            toast("书籍信息错误,退出后重新打开!!!!");
        }

    }

    /**
     * 获取数据
     */
    public void getBookData(TabNavBody book) {

        EasyLog.print("TipsBookNetReadFragment", "========== getBookData 开始 ==========");
        EasyLog.print("TipsBookNetReadFragment", "book=" + (book != null ? book.getBookName() : "null"));
        EasyLog.print("TipsBookNetReadFragment", "presenter=" + (presenter != null ? "存在" : "null"));
        EasyLog.print("TipsBookNetReadFragment", "chapterList=" + (chapterList != null ? chapterList.size() : "null"));

        if (book != null) {
            //加载书本相关的章节（已移除旧的批量下载逻辑）
            // getBookChapter(); // ❌ 已移除：启动时不再批量下载
            
            // ✅ 通过 Presenter 加载书籍数据（新数据模型）
            if (presenter != null && chapterList != null) {
                EasyLog.print("TipsBookNetReadFragment", "开始加载书籍数据，共 " + chapterList.size() + " 个章节");
                // 调用 Presenter 初始化书籍数据（传递 TabNavBody 避免全局数据获取失败）
                presenter.loadBookContent(book, bookId, bookLastReadPosition, isShowBookCollect);
            } else {
                EasyLog.print("TipsBookNetReadFragment", "跳过加载: presenter=" + (presenter != null) + 
                    ", chapterList=" + (chapterList != null));
            }
            
            StringBuilder fangName = new StringBuilder("\n").append(book.getBookName());
            //书本相关的药方只加载一次
            if (!singletonNetData.getBookFang(bookId)){
                getBookFang(bookId, fangName);
                //标记书本是否已经下载过
                singletonNetData.setBookFang(bookId);
            }
        }
    }

    private void getBookChapter() {
        int setp = 150;
        for (Chapter chapter : chapterList) {
            // 从数据库中获取数据,是否下载过
            if (!chapter.getIsDownload()) {
                postDelayed(() -> {
                    getChapterList(chapter, singletonNetData.getContent());
                }, setp);
                setp += 500;
            }
        }
    }

    public void getChapterList(Chapter chapter, ArrayList<HH2SectionData> detailList) {

        EasyHttp.get(this)
                .api(new ChapterContentApi()
                        .setContentId(chapter.getChapterSection())
                        .setSignatureId(chapter.getSignatureId())
                        .setBookId(chapter.getBookId())
                )
                .request(new HttpCallback<HttpData<List<HH2SectionData>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<HH2SectionData>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            //  ArrayList<Chapter> list = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(item.getBookNo()));
                            for (int i = 0; i < detailList.size(); i++) {
                                if (detailList.get(i).getSignatureId() == data.getData().get(0).getSignatureId()) {
                                    HH2SectionData hh2SectionData = new HH2SectionData(data.getData().get(0).getData(), chapter.getChapterSection(), chapter.getChapterHeader());
                                    hh2SectionData.setSignatureId(data.getData().get(0).getSignatureId());
                                    ExpandableGroupEntity groupEntity = GroupModel.getExpandableGroupEntity(false, hh2SectionData);
                                    // 更新数据
                                    detailList.set(i, hh2SectionData);
                                    adapter.getmGroups().set(i, groupEntity);
                                    // 刷新列表
                                    adapter.notifyGroupChanged(i);
                                    //
                                    if (!isShowUpdateNotification) {
                                        isShowUpdateNotification = true;
                                        toast("重新下载完成!!!!");
                                    }
                                    try {
                                        // 更新数据库
                                        chapter.setIsDownload(true);
                                        DbService.getInstance().mChapterService.updateEntity(chapter);
                                        //保存内容
                                        ConvertEntity.saveBookChapterDetailList(chapter, data.getData());

                                    } catch (Exception e) {
                                        // 处理异常，比如记录日志、通知管理员等
                                        EasyLog.print("Failed to updateEntity: " + e.getMessage());
                                        return;
                                        // 根据具体情况决定是否需要重新抛出异常
                                        //throw e;
                                    }
                                }
                            }


                        }
                    }

//                    @Override
//                    public void onFail(Exception e) {
//                        super.onFail(e);
//                        toast("onFail  getChapterList!!!");
//                    }
                });

    }

    private void getBookFang(int bookId, StringBuilder fangName) {
        EasyHttp.get(this)
                .api(new BookFangApi().setBookId(bookId))
                .request(new HttpCallback<HttpData<List<Fang>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<Fang>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            List<Fang> detailList = data.getData();
                            singletonNetData.setFang(new HH2SectionData(detailList, 0, fangName.toString()));
                            //保存药方数据
                            ThreadUtil.runInBackground(() -> {
                                ConvertEntity.getFangDetailList(data.getData(), bookId);
                            });

                        }
                    }
                });
    }

    private void refreshData() {
        bookInitData();
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
        
        // Presenter 已在 onDestroyView 中清理
        
        // 清理适配器监听器
        if (adapter != null) {
            adapter.setOnHeaderClickListener(null);
            adapter.setOnJumpSpecifiedItemListener(null);
        }
        
        // 清理数据监听器
        if (singletonNetData != null) {
            singletonNetData.setOnContentUpdateListener(null);
        }

        // 清理 RecyclerView
        if (rvList != null) {
            rvList.setAdapter(null);
            rvList.setLayoutManager(null);
            if (rvList.getItemDecorationCount() > 0) {
                rvList.removeItemDecorationAt(0);
            }
        }
        
        // 清理回调
        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
            onBackPressedCallback = null;
        }
        
        // 注销 EventBus
        XEventBus.getDefault().unregister(this);

        
        // 释放引用
        contentUpdateListener = null;
        onJumpSpecifiedItemListener = null;
        singletonNetData = null;
        adapter = null;
        rvList = null;
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
        
        // 通过 Presenter 执行搜索
        if (presenter != null && searchText != null && !searchText.isEmpty()) {
            presenter.search(searchText);
        } else {
            // 清空搜索，恢复原始列表
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

    // ==================== MVP View 接口实现 ====================

    @Override
    public void showChapterList(List<ExpandableGroupEntity> chapters) {
        // 显示章节列表
        if (adapter != null && chapters != null) {
            post(() -> {
                adapter.setmGroups(new ArrayList<>(chapters));
                adapter.notifyDataChanged();
            });
        }
    }

    @Override
    public void showSearchResults(List<ExpandableGroupEntity> results, int totalCount) {
        // 显示搜索结果
        post(() -> {
            if (adapter != null && results != null) {
                adapter.setmGroups(new ArrayList<>(results));
                adapter.notifyDataChanged();
            }
            if (numTips != null) {
                numTips.setText(String.format("%d个结果", totalCount));
            }
        });
    }

    @Override
    public void showLoading(boolean isLoading) {
        // 显示/隐藏加载中
        if (isLoading) {
            // 可以添加 loading 对话框
        } else {
            // 隐藏 loading
        }
    }

    @Override
    public void showError(String message) {
        // 显示错误信息
        post(() -> toast(message));
    }

    @Override
    public void showToast(String message) {
        // 显示提示信息
        post(() -> toast(message));
    }

    @Override
    public void showDownloadProgress(int position, String message) {
        // 显示下载进度
        EasyLog.print("Download", "Position " + position + ": " + message);
    }

    @Override
    public void updateDownloadStatus(int position, boolean isDownloaded) {
        // 更新下载状态
        if (adapter != null) {
            post(() -> adapter.notifyGroupChanged(position));
        }
    }

    @Override
    public void scrollToPosition(int position) {
        // 滚动到指定位置
        if (layoutManager != null) {
            post(() -> layoutManager.scrollToPositionWithOffset(position, 0));
        }
    }

    @Override
    public void expandChapter(int position) {
        // 展开章节
        if (adapter != null) {
            post(() -> adapter.expandGroup(position, true));
        }
    }

    @Override
    public void collapseChapter(int position) {
        // 收起章节
        if (adapter != null) {
            post(() -> adapter.collapseGroup(position));
        }
    }

    @Override
    public boolean isActive() {
        // 检查 View 是否处于活动状态
        return isAdded() && !isDetached() && getActivity() != null;
    }

    @Override
    public void onDestroyView() {
        // 清理 Presenter
        if (presenter != null) {
            presenter.onViewDestroy();
            presenter = null;
        }
        super.onDestroyView();
    }

    // ==================== ComponentCallbacks2 实现 ====================

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 配置变更处理
    }

    @Override
    public void onLowMemory() {
        EasyLog.print("TipsBookNetReadFragment", "低内存警告");
        // 相当于 TRIM_MEMORY_COMPLETE
        onTrimMemory(TRIM_MEMORY_COMPLETE);
    }

    @Override
    public void onTrimMemory(int level) {
        EasyLog.print("TipsBookNetReadFragment", "内存压力回调: level=" + level);
        
        // 通知 Presenter 处理内存压力
        if (presenter != null) {
            presenter.onTrimMemory(level);
        }
        
        // 根据内存压力级别采取不同策略
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
            // 极端情况：清除适配器缓存
            if (adapter != null) {
                EasyLog.print("TipsBookNetReadFragment", "清除适配器缓存");
                // 可以在这里添加适配器缓存清理逻辑
            }
        } else if (level >= TRIM_MEMORY_RUNNING_MODERATE) {
            // 中等压力：减少缓存
            EasyLog.print("TipsBookNetReadFragment", "中等内存压力");
        } else if (level >= TRIM_MEMORY_RUNNING_LOW) {
            // 较低压力：预警
            EasyLog.print("TipsBookNetReadFragment", "较低内存压力");
        }
    }

}
