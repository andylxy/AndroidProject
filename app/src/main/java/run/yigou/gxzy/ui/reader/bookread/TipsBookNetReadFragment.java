/*
 * 项目名: AndroidProject
 * 类名: TipsBookNetReadFragment.java
 * 包名: run.yigou.gxzy.ui.reader.bookread
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月08日 10:50:43
 * 上次修改时间: 2024年09月08日 10:50:43
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.bookread;

import android.annotation.SuppressLint;
import android.content.ComponentCallbacks2;
import android.content.Context;
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

import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.log.EasyLog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;


import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.event.TipsFragmentSettingEventNotification;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.base.args.BookArgs;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.gen.ChapterDao;
import run.yigou.gxzy.ui.dialog.MessageDialog;
import run.yigou.gxzy.ui.reader.BookCollectCaseFragment;
import run.yigou.gxzy.widget.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.reader.adapter.RefactoredExpandableAdapter;
import run.yigou.gxzy.ui.reader.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.reader.entity.GroupModel;
import run.yigou.gxzy.data.model.HH2SectionData;
import run.yigou.gxzy.ui.reader.helper.TipsNetHelper;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.ui.reader.manager.ChapterDownloadManager;
import run.yigou.gxzy.ui.reader.bookread.contract.TipsBookReadContract;
import run.yigou.gxzy.ui.reader.bookread.presenter.TipsBookReadPresenter;
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
    private run.yigou.gxzy.ui.reader.search.SearchCoordinator searchCoordinator;

    private OnBackPressedCallback onBackPressedCallback;

    public static TipsBookNetReadFragment newInstance(BookArgs bookArgs) {
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
                    if (adapter != null) {
                        adapter.setSearch(false);
                    }
                } else {
                    if (adapter != null) {
                        adapter.setSearch(true);
                    }
                    setSearchText(text);
                }
            };

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
                // 使用中间函数桥接，将逻辑下沉到 Presenter
                bridgeHandleBackPress();
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
            searchCoordinator = new run.yigou.gxzy.ui.reader.search.SearchCoordinator(bookId);

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
    }

    private boolean isShowUpdateNotification = true;

    private void setHeaderClickListener() {
        adapter.setOnHeaderClickListener(new GroupedRecyclerViewAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(GroupedRecyclerViewAdapter adapter, BaseViewHolder holder,
                                      int groupPosition) {
                // UI 操作：始终允许折叠/展开
                RefactoredExpandableAdapter expandableAdapter = (RefactoredExpandableAdapter) adapter;
                if (expandableAdapter.isExpand(groupPosition)) {
                    expandableAdapter.collapseGroup(groupPosition);
                } else {
                    expandableAdapter.expandGroup(groupPosition);
                }

                // 核心逻辑：获取真实索引并触发下载
                // Fix: 搜索模式下 groupPosition 是过滤后的索引，需映射回 chapterList 的真实索引
                int realIndex = groupPosition;
                boolean isSearchMode = searchText != null && searchText.length() > 0;

                if (isSearchMode) {
                    try {
                        // 获取当前显示的 Group Entity
                        // 注意：getmGroups() 返回的是当前 Adapter 持有的数据源（可能是过滤后的）
                        if (groupPosition >= 0 && groupPosition < expandableAdapter.getmGroups().size()) {
                            ExpandableGroupEntity group = expandableAdapter.getmGroups().get(groupPosition);
                            if (group != null) {
                                String headerTitle = group.getHeader();
                                int foundIndex = findChapterIndexByTitle(headerTitle);
                                if (foundIndex != -1) {
                                    realIndex = foundIndex;
                                } else {
                                    // 没找到对应章节，不触发后续逻辑以防错乱
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        EasyLog.print("TipsBookNetReadFragment", "Search index mapping error: " + e.getMessage());
                    }
                }
                
                // 记录当前点击位置 (使用真实索引)
                if (isShowBookCollect)
                    currentIndex = realIndex;

                // ✅ 搜索模式下：只允许展开/收起，不触发下载逻辑（防止数据被覆盖）
                if (isSearchMode) {
                    return;
                }

                // ✅ 非搜索模式：智能下载 + 预加载 (使用真实索引)
                triggerChapterDownload(realIndex);

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
     * 根据章节标题查找真实索引
     * 用于搜索模式下将 UI 索引映射回原始数据索引
     *
     * @param title 章节标题
     * @return 真实索引，未找到返回 -1
     */
    private int findChapterIndexByTitle(String title) {
        if (chapterList == null || title == null) {
            return -1;
        }
        for (int i = 0; i < chapterList.size(); i++) {
            Chapter chapter = chapterList.get(i);
            // 比对标题，注意处理 null
            if (chapter != null && title.equals(chapter.getChapterHeader())) {
                return i;
            }
        }
        return -1;
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
                } else {
                    // 如果是收起状态，只刷新组数据即可
                    adapter.notifyGroupChanged(groupPosition);
                }
            }
        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "更新章节内容失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ArrayList<Chapter> chapterList;

    private void bookInitData() {
        // 加载书本相关的药方
        TabNavBody book = GlobalDataHolder.getInstance().getNavTabBodyMap().get(bookId);
        
        if (book != null) {
            chapterList = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(book.getBookNo()));
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
        if (book != null) {
            if (presenter != null && chapterList != null) {
                // 调用 Presenter 初始化书籍数据（传递 TabNavBody 避免全局数据获取失败）
                // Presenter 会自动加载药方数据
                presenter.loadBookContent(book, bookId, bookLastReadPosition, isShowBookCollect);
                
                // 初始化章节下载管理器并启动后台下载
                initChapterDownloadManager();
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


    @Override
    public void onDestroy() {
        super.onDestroy();
        
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
        if (charSequence == null || charSequence.length() == 0) {
            this.searchText = null;
            return true;
        }
        return false;
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
        if (searchCoordinator == null) {
            return;
        }
        
        // 使用 SearchCoordinator 进行全局搜索
        android.util.Pair<List<run.yigou.gxzy.ui.reader.entity.GroupData>, 
                          List<List<run.yigou.gxzy.ui.reader.entity.ItemData>>> result = 
            searchCoordinator.searchGlobal(keyword);
        
        if (result == null) {
            return;
        }
        
        List<run.yigou.gxzy.ui.reader.entity.GroupData> groupDataList = result.first;
        List<List<run.yigou.gxzy.ui.reader.entity.ItemData>> itemDataList = result.second;
        
        // 更新适配器显示搜索结果
        if (adapter != null) {
            adapter.setSearchData(groupDataList, itemDataList);
            
            // 统计匹配数量
            int totalMatches = 0;
            if (itemDataList != null) {
                for (List<run.yigou.gxzy.ui.reader.entity.ItemData> items : itemDataList) {
                    if (items != null) {
                        totalMatches += items.size();
                    }
                }
            }
            
            // 显示匹配数量
            if (numTips != null) {
                numTips.setText(String.format("%d个结果", totalMatches));
            }
        }
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
        if (isLoading) {
        } else {
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
        super.onLowMemory();
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
                // 可以在这里添加适配器缓存清理逻辑
            }
        }
    }

    // ==================== 中间函数与接口实现 ====================

    /**
     * 桥接函数：处理返回键逻辑
     */
    private void bridgeHandleBackPress() {
        if (presenter != null) {
            presenter.checkBookStatusForExit();
        } else {
            closeView();
        }
    }

    /**
     * 桥接函数：加入书架
     */
    private void bridgeAddToBookshelf(TabNavBody navTabBody) {
        if (presenter != null) {
            presenter.addToBookshelfAndExit(navTabBody);
        }
    }

    @Override
    public void showAddToBookshelfConfirmDialog(TabNavBody book) {
        new MessageDialog.Builder(getContext())
                .setTitle("加入书架")
                .setMessage(book.getBookName())
                .setConfirm(getString(R.string.common_confirm))
                .setCancel(getString(R.string.common_cancel))
                .setListener(new MessageDialog.OnListener() {
                    @Override
                    public void onConfirm(BaseDialog dialog) {
                        bridgeAddToBookshelf(book);
                    }

                    @Override
                    public void onCancel(BaseDialog dialog) {
                        closeView();
                    }
                })
                .show();
    }

    @Override
    public void closeView() {
        // 刷新书架（保留原逻辑中的副作用）
        try {
            BookCollectCaseFragment.newInstance().refreshLayout();
        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "刷新书架失败: " + e.getMessage());
        }

        // 调用系统返回
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(false);
        }
        
        // 延迟调用以确保 UI 动画流畅（保持原有 delay）
        if (rvList != null) {
            rvList.postDelayed(() -> {
                if (getActivity() != null) {
                    requireActivity().onBackPressed();
                }
            }, AppConst.postDelayMillis);
        }
    }

}
