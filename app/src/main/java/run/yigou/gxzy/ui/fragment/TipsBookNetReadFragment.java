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
import com.hjq.http.EasyLog;
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
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.dialog.MessageDialog;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.adapter.refactor.RefactoredExpandableAdapter;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
import run.yigou.gxzy.ui.tips.tipsutils.ChapterDownloadManager;
import run.yigou.gxzy.ui.tips.contract.TipsBookReadContract;
import run.yigou.gxzy.ui.tips.presenter.TipsBookReadPresenter;
import run.yigou.gxzy.ui.tips.repository.BookRepository;
import run.yigou.gxzy.utils.ThreadUtil;


public class TipsBookNetReadFragment extends AppFragment<AppActivity> 
        implements TipsBookReadContract.View, ComponentCallbacks2 {


    private WrapRecyclerView rvList;
    private ClearEditText clearEditText;
    private RefactoredExpandableAdapter adapter;
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
     * MVP 架构组件
     */
    private TipsBookReadPresenter presenter;
    
    /**
     * 章节下载管理器
     */
    private ChapterDownloadManager chapterDownloadManager;
    
    /**
     * 全局搜索协调器
     */
    private run.yigou.gxzy.ui.tips.utils.SearchCoordinator searchCoordinator;

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
                TabNavBody navTabBody = GlobalDataHolder.getInstance().getNavTabBodyMap().get(bookId);
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
            // ✅ 不再需要初始化 singletonNetData
            // ✅ 宋版伤寒逻辑已移至 Presenter
            
            // Fragment 处理返回键动作,是否保存阅读

            if (AppApplication.getApplication().fragmentSetting.isShuJie())
                fragmentOnBackPressed();

            // 初始化 MVP 架构
            presenter = new TipsBookReadPresenter(this);
            presenter.onViewCreated();
            
            // 初始化全局搜索协调器
            searchCoordinator = new run.yigou.gxzy.ui.tips.utils.SearchCoordinator(bookId);

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

    // ✅ 宋版伤寒监听器已移至 Presenter 内部处理
    // ✅ 不再需要 Fragment 中的监听器

    @Subscribe(priority = 1)
    public void onEvent(TipsFragmentSettingEventNotification event) {
        ThreadUtil.runOnUiThread(() -> {
            // ✅ 宋版伤寒逻辑已移至 Presenter，不需要 Fragment 中设置
            refreshData();
            // Fragment 处理返回键动作,是否保存阅读
            setBackPressedCallback();
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
        adapter = new RefactoredExpandableAdapter(getContext());
        EasyLog.print("TipsBookNetReadFragment", "========== 初始化RefactoredExpandableAdapter ==========");
        EasyLog.print("TipsBookNetReadFragment", "adapter实例: " + adapter);
    }

    private boolean isShowUpdateNotification = true;

    private void setHeaderClickListener() {
        adapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
                RefactoredExpandableAdapter expandableAdapter = (RefactoredExpandableAdapter) adapter;
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
        if (chapterList == null || presenter == null) {
            toast("数据未加载，无法重新下载");
            return;
        }

        try {
            // 边界检查
            if (groupPosition < 0 || groupPosition >= chapterList.size()) {
                toast("章节索引越界");
                return;
            }

            Chapter chapter = chapterList.get(groupPosition);
            if (chapter == null) {
                toast("未找到章节信息");
                return;
            }

            // ✅ 通过 Presenter 重新下载章节 (Presenter 会显示提示)
            presenter.reloadChapter(groupPosition);
            
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

    private RefactoredExpandableAdapter.OnJumpSpecifiedItemListener onJumpSpecifiedItemListener;

    private void setJumpSpecifiedItemListener() {
        if (onJumpSpecifiedItemListener == null) {
            onJumpSpecifiedItemListener = new RefactoredExpandableAdapter.OnJumpSpecifiedItemListener() {
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
        if (chapterList == null || presenter == null) {
            return;
        }

        try {
            // ✅ 直接通过 Presenter 处理章节点击（不再依赖 singletonNetData）
            presenter.onChapterClick(groupPosition);

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
        if (adapter == null || sectionData == null) {
            return;
        }

        try {
            // ✅ 直接更新 UI（数据已由 Presenter 管理）
            if (groupPosition >= 0 && groupPosition < adapter.getmGroups().size()) {
                // ✅ 保留当前的展开状态
                boolean isCurrentlyExpanded = adapter.isExpand(groupPosition);
                
                // ✅ 使用当前展开状态创建新的 GroupEntity
                ExpandableGroupEntity groupEntity = GroupModel.getExpandableGroupEntity(isCurrentlyExpanded, sectionData);
                
                // ✅ 使用新的重构API更新数据（同步groups和groupDataList）
                adapter.updateGroupFromEntity(groupPosition, groupEntity);
                
                // ✅ 关键修复: 根据展开状态决定刷新策略
                if (isCurrentlyExpanded) {
                    // 数据更新后重新展开，确保子项显示
                    adapter.notifyDataChanged();  // 先刷新所有数据
                    adapter.expandGroup(groupPosition, false);  // 再展开该组(无动画，避免闪烁)
                    EasyLog.print("TipsBookNetReadFragment", "章节内容已更新并重新展开: " + sectionData.getHeader());
                } else {
                    // 如果是收起状态，只刷新组数据即可
                    adapter.notifyGroupChanged(groupPosition);
                    EasyLog.print("TipsBookNetReadFragment", "章节内容已更新(收起状态): " + sectionData.getHeader());
                }
            }
        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "更新章节内容失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ArrayList<Chapter> chapterList;

    private void bookInitData() {

        EasyLog.print("TipsBookNetReadFragment", "========== bookInitData 开始 ==========");
        
        // ✅ 不再需要初始化 singletonNetData
        // ✅ 不再需要初始化别名字典（由 Repository/Presenter 处理）

        //加载书本相关的药方
        TabNavBody book = GlobalDataHolder.getInstance().getNavTabBodyMap().get(bookId);
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
            // ✅ 通过 Presenter 加载书籍数据（新数据模型）
            if (presenter != null && chapterList != null) {
                EasyLog.print("TipsBookNetReadFragment", "开始加载书籍数据，共 " + chapterList.size() + " 个章节");
                // 调用 Presenter 初始化书籍数据（传递 TabNavBody 避免全局数据获取失败）
                // Presenter 会自动加载药方数据
                presenter.loadBookContent(book, bookId, bookLastReadPosition, isShowBookCollect);
                
                // 【新功能】初始化章节下载管理器并启动后台下载
                initChapterDownloadManager();
            } else {
                EasyLog.print("TipsBookNetReadFragment", "跳过加载: presenter=" + (presenter != null) + 
                    ", chapterList=" + (chapterList != null));
            }
        }
    }

    /**
     * 初始化章节下载管理器，启动后台低优先级下载
     */
    private void initChapterDownloadManager() {
        if (chapterDownloadManager == null && chapterList != null) {
            chapterDownloadManager = new ChapterDownloadManager();
            chapterDownloadManager.initDownloadedCache(chapterList);
            
            EasyLog.print("TipsBookNetReadFragment", "章节下载管理器初始化完成");
            
            // 启动后台批量下载所有未下载章节（低优先级）
            chapterDownloadManager.batchDownloadAllChapters(chapterList, this);
        }
    }

    // ✅ 废弃方法已移除：getBookChapter(), getChapterList(), getBookFang()
    // ✅ 这些功能已由 Presenter 和 Repository 接管

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
        
        // 清理章节下载管理器
        if (chapterDownloadManager != null) {
            chapterDownloadManager.cancelAll();
            chapterDownloadManager = null;
        }
        
        // 清理适配器监听器
        if (adapter != null) {
            adapter.setOnHeaderClickListener(null);
            adapter.setOnJumpSpecifiedItemListener(null);
        }
        
        // ✅ 不再需要清理 singletonNetData 监听器

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
        try {
            if (XEventBus.getDefault() != null) {
                XEventBus.getDefault().unregister(this);
                EasyLog.print("TipsBookNetReadFragment", "✅ EventBus 注销成功");
            }
        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "⚠️ EventBus 注销异常: " + e.getMessage());
        }

        // 释放引用
        onJumpSpecifiedItemListener = null;
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
        if (bookId != 0 && presenter != null) {
            if (init) {
                // ✅ 从 Presenter 获取章节内容列表
                List<HH2SectionData> contentList = presenter.getChapterContentList();
                adapter.setmGroups(GroupModel.getExpandableGroups(new ArrayList<>(contentList), isExpand));
                //如果有上次阅读记录，则定位到上次阅读位置
                if (isShowBookCollect) {
                    layoutManager.scrollToPositionWithOffset(bookLastReadPosition, 0);
                    adapter.expandGroup(bookLastReadPosition, true);
                }
            }
            // 搜索结果已通过 presenter.search() -> view.showSearchResults() 回调处理
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
        
        if (searchText == null || searchText.trim().isEmpty()) {
            // 清空搜索，恢复原始列表
            if (this.adapter != null) {
                reListAdapter(true, false);
            }
            if (numTips != null) {
                numTips.setText("");
            }
        } else {
            // 执行全局搜索
            performGlobalSearch(searchText.trim());
        }
    }
    
    /**
     * 执行全局搜索
     */
    private void performGlobalSearch(String keyword) {
        EasyLog.print("TipsBookNetReadFragment", "执行全局搜索: " + keyword);
        
        if (searchCoordinator == null) {
            EasyLog.print("TipsBookNetReadFragment", "❌ searchCoordinator 未初始化");
            return;
        }
        
        // 使用 SearchCoordinator 进行全局搜索
        android.util.Pair<List<run.yigou.gxzy.ui.tips.entity.GroupData>, 
                          List<List<run.yigou.gxzy.ui.tips.entity.ItemData>>> result = 
            searchCoordinator.searchGlobal(keyword);
        
        if (result == null) {
            EasyLog.print("TipsBookNetReadFragment", "❌ 搜索结果为 null");
            return;
        }
        
        List<run.yigou.gxzy.ui.tips.entity.GroupData> groupDataList = result.first;
        List<List<run.yigou.gxzy.ui.tips.entity.ItemData>> itemDataList = result.second;
        
        // 更新适配器显示搜索结果
        if (adapter != null) {
            adapter.setSearchData(groupDataList, itemDataList);
            
            // 统计匹配数量
            int totalMatches = 0;
            if (itemDataList != null) {
                for (List<run.yigou.gxzy.ui.tips.entity.ItemData> items : itemDataList) {
                    if (items != null) {
                        totalMatches += items.size();
                    }
                }
            }
            
            // 显示匹配数量
            if (numTips != null) {
                numTips.setText(String.format("%d个结果", totalMatches));
            }
            
            EasyLog.print("TipsBookNetReadFragment", 
                "✅ 搜索完成，匹配章节: " + (groupDataList != null ? groupDataList.size() : 0) + 
                ", 总匹配数: " + totalMatches);
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
