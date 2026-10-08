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
import run.yigou.gxzy.data.local.helper.LocalServices;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.Callback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.ClearEditText;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import run.yigou.gxzy.event.TipsSettingChangedEvent;
import run.yigou.gxzy.manager.SearchEntry;
import run.yigou.gxzy.manager.SearchPermissionManager;
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
import run.yigou.gxzy.ui.reader.adapter.model.GroupData;
import run.yigou.gxzy.ui.reader.adapter.model.ItemData;
import run.yigou.gxzy.ui.reader.entity.GroupModel;
import run.yigou.gxzy.data.model.HH2SectionData;
import run.yigou.gxzy.ui.reader.helper.TipsDialogHelper;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.manager.chapter.ChapterContentManager;
import run.yigou.gxzy.ui.reader.bookread.contract.TipsBookReadContract;
import run.yigou.gxzy.ui.reader.bookread.presenter.TipsBookReadPresenter;
import run.yigou.gxzy.utils.ThreadUtil;


public class TipsBookNetReadFragment extends AppFragment<AppActivity> 
        implements TipsBookReadContract.View, ComponentCallbacks2 {


    private WrapRecyclerView rvList;
    private ClearEditText clearEditText;
    private RefactoredExpandableAdapter adapter;
    private BookArgs bookArgs;

    private TextView numTips;
    private String bookId = null;
    private int bookLastReadPosition;
    private String searchText = null;
    private Button tipsBtnSearch;


    private LinearLayoutManager layoutManager;
    /**
     * 是否保存到书架
     */
    private boolean isShowBookCollect = false;
    
    /**
     * MVP 架构组件
     */
    private TipsBookReadPresenter presenter;
    
    /**
     * 章节内容管理器
     */
    private ChapterContentManager chapterContentManager;

    private OnBackPressedCallback onBackPressedCallback;

    public static TipsBookNetReadFragment newInstance(BookArgs bookArgs) {
        TipsBookNetReadFragment instance = new TipsBookNetReadFragment();
        instance.bookArgs = bookArgs;
        return instance;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tips_book_read_activity_group_list;
    }

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
                // 统一入口：无论内容为空、非空还是全空白，都交给 setSearchText 判定，
                // 避免「退格清空」与「点搜索按钮」走两条路径导致 cancelSearch() 被绕过
                // （D8：清空时必须作废在途搜索，否则结果回填会覆盖已恢复的全量列表）
                if (charSequenceIsEmpty(text)) {
                    setSearchText(null);
                } else {
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
        // 事件注册移至 onStart，与 onStop 注销配对（D7 治理：view 销毁即退订，避免 onEvent 操作已释放成员）
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
            refreshData();
} catch (Exception e) {
            EasyLog.print(e);
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
            bookId = null;
            bookLastReadPosition = 0;
            isShowBookCollect = false;
            searchText = null;

        }
    }

    // 阅读设置（术解等）变更后由 XEventBus 通知本 Fragment 刷新列表

    @Subscribe(priority = 1)
    public void onEvent(TipsSettingChangedEvent event) {
        ThreadUtil.runOnUiThread(() -> {
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

                // ✅ 搜索模式下：只允许展开/收起，不触发下载逻辑（防止数据被覆盖）
                if (isSearchActive()) {
                    return;
                }

                // ✅ 非搜索模式：智能下载 + 预加载（groupPosition 即真实章节索引）
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
                // 搜索状态不响应长按（用 Fragment 自身维护的搜索态，而非适配器的空桩）
                if (isSearchActive()) return true;
                TipsDialogHelper.showListDialog(getContext(), TipsDialogHelper.DIALOG_TYPE_REDOWNLOAD)
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
                public void onJumpSpecifiedItem(int chapterIndex, int childPosition) {
                    // 入参是「显示列表下标」（T6：Handler 已把 groupPosition 换算成
                    // chapterIndex）。显示列表即 presenter.getChapterContentList()，
                    // 现在恒等于该书全量章节（宋版伤寒截取已移除），坐标系一致，可直接按显示列表理解。
                    // 若当前在搜索态，列表是搜索结果，必须先退出搜索态恢复成显示列表，
                    // 否则 chapterIndex 会作用在错误的列表上。
                    // 清空输入框：列表要恢复成全量，若搜索框仍显示关键字，用户接着
                    // 输入会拼成「旧关键字+新字符」，与列表状态错位。
                    // 注意 setText 会触发 TextWatcher 的 300ms 防抖，那次防抖会再
                    // reListAdapter 一次并重置展开态，故定位必须排在它之后（350 > 300），
                    // 否则刚展开的分组会被收回。
                    clearEditText.setText("");
                    // 空守卫：numTips 在 initView 才赋值，长按回调理论上可能先于
                    // view 绑定触发。与 showLoading/showError 的空守卫同口径。
                    if (numTips != null) {
                        numTips.setText("");
                    }
                    // 走 setSearchText(null) 以统一「作废在途搜索 + 恢复全量」的口径；
                    // 非搜索态下它只是把列表按当前状态重建一次，无副作用。
                    setSearchText(null);
                    postDelayed(() -> {
                        layoutManager.scrollToPositionWithOffset(chapterIndex, 0);
                        adapter.expandGroup(chapterIndex, true);
                    }, 350);
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
            EasyLog.print(e);
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

        // ✅ D2 修复：搜索态下列表展示的是搜索结果（groupDataList 已是过滤集），
        //    而 Presenter 回调的 position 来自全量章节侧，两侧索引不对齐；
        //    若继续写入会覆盖搜索结果同位置的项，造成数据污染。
        //    与 onHeaderClick 的搜索态守卫（"防止数据被覆盖"）保持同一口径：
        //    搜索态下不把章节下载结果应用到当前列表。非搜索态行为与修复前完全一致。
        if (isSearchActive()) {
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
            EasyLog.print(e);
        }
    }

    private ArrayList<Chapter> chapterList;

    /** D7：数据加载序号，用于丢弃过期的异步加载结果，避免并发覆盖 chapterList */
    private int bookDataLoadSeq = 0;

    private void bookInitData() {
        // 加载书本相关的药方
        TabNavBody book = GlobalDataHolder.getInstance().getNavTabBodyMap().get(bookId);
        
        if (book != null) {
            // 章节列表读取挪到后台（统一入口见 DbService.readInBackground）；
            // 读完之后回主线程再走 getBookData（它后面全是 UI 与 Presenter 调用）
            final TabNavBody target = book;
            // D7：递增序号，仅当本次结果仍为最新时才应用，防止连发事件/配置变更导致的多趟覆盖
            final int mySeq = ++bookDataLoadSeq;
            DbService.getInstance().readInBackground(
                    new Callable<ArrayList<Chapter>>() {
                        @Override
                        public ArrayList<Chapter> call() {
                            return LocalServices.getInstance().mChapterService.find(
                                    ChapterDao.Properties.BookId.eq(target.getBookNo()));
                        }
                    },
                    new Callback<ArrayList<Chapter>>() {
                        @Override
                        public void onSuccess(ArrayList<Chapter> loaded) {
                            if (mySeq != bookDataLoadSeq) return; // 丢弃过期结果
                            chapterList = loaded;
                            // 加载书本相关的章节
                            getBookData(target);
                        }

                        @Override
                        public void onError(Exception e) {
                            if (mySeq != bookDataLoadSeq) return; // 丢弃过期结果
                            // 读失败时 chapterList 保持 null，getBookData 内部对 null 有兜底
                            chapterList = null;
                            getBookData(target);
                        }
                    });
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
                
                // 初始化章节内容管理器并启动后台预加载
                initChapterContentManager();
            }
        }
    }

    /**
     * 初始化章节内容管理器，启动后台低优先级预加载
     */
    private void initChapterContentManager() {
        if (chapterContentManager == null && chapterList != null) {
            chapterContentManager = new ChapterContentManager();
            chapterContentManager.initContentCache(chapterList);
            
            // 启动后台批量预加载所有章节（低优先级）
            chapterContentManager.preloadAllChapters(chapterList, this);
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
        
        // 清理章节内容管理器
        if (chapterContentManager != null) {
            chapterContentManager.cancelAll();
            chapterContentManager = null;
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

        // 释放引用
        onJumpSpecifiedItemListener = null;
        adapter = null;
        rvList = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        // D7：与 onStop 配对注册，view 进入前台才订阅事件
        try {
            XEventBus.getDefault().register(this);
        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "⚠️ EventBus 注册异常: " + e.getMessage());
        }
    }

    @Override
    public void onStop() {
        // D7：view 离开前台即退订，避免 onDestroyView 之后仍被 onEvent 操作已释放成员
        try {
            if (XEventBus.getDefault() != null) {
                XEventBus.getDefault().unregister(this);
            }
        } catch (Exception e) {
            EasyLog.print("TipsBookNetReadFragment", "⚠️ EventBus 注销异常: " + e.getMessage());
        }
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.tips_btn_search) {

            if (this.searchText == null) {
                reListAdapter(true, false);
            } else {
                setSearchText(this.searchText);
            }
        }
    }

    /**
     * @param init     true  初始化显示 ,false 搜索结果 显示
     * @param isExpand false 表头不展开, true 展开
     */

    private void reListAdapter(boolean init, boolean isExpand) {
        if (bookId != null && !bookId.isEmpty() && presenter != null) {
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
            // 搜索结果不走这里：搜索由 Presenter.search() 完成后回调
            // showSearchResults → adapter.setSearchData 写入
            adapter.notifyDataChanged();
        }
    }

    /**
     * 判断搜索框内容是否为空（只看长度，不做 trim）。
     *
     * 注意：本方法只用于「长度非零但内容可能全为空白」的前置分流，
     * 真正的搜索态判定见 {@link #isSearchActive()}，清空后的统一处理见
     * {@link #setSearchText(String)}。不要在这里做状态恢复，否则会与
     * setSearchText 形成双入口，导致 {@code cancelSearch()} 在用户退格路径上被绕过。
     *
     * @param charSequence 需要判断的 CharSequence
     * @return 如果为 null 或长度为 0，则返回 true；否则返回 false
     */
    public boolean charSequenceIsEmpty(CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }

    @SuppressLint("DefaultLocale")
    public void setSearchText(String searchText) {
        this.searchText = searchText;
        
        if (searchText == null || searchText.trim().isEmpty()) {
            // 先作废在途搜索（D8）：否则它回填结果会覆盖下面恢复的全量列表，
            // 且清空后 isSearchActive() 为 false，章节更新类守卫会一并失效
            if (presenter != null) {
                presenter.cancelSearch();
            }
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
     * 是否处于搜索态。
     *
     * <p>判据是「trim 后非空」而非「非空」：用户输入纯空格时，setSearchText 会因
     * trim().isEmpty() 走清空分支并把列表恢复成全量章节；若这里仍判为搜索态，
     * D2/D2.1 的搜索态守卫（updateChapterContent / updateDownloadStatus /
     * onHeaderClick）会全部持续失效，重新打开它们想关闭的数据污染路径。
     */
    private boolean isSearchActive() {
        return searchText != null && !searchText.trim().isEmpty();
    }
    
    /**
     * 发起全局搜索。
     *
     * T3/D3：搜索执行统一交给 Presenter（后台线程 + 在途序号，见 TipsBookReadPresenter.search），
     * 本Fragment 只做「权限判定 +转发 + 结果展示」三件事，不再自行检索。
     */
    private void performGlobalSearch(String keyword) {
        if (presenter == null) {
            return;
        }

        // 搜索权限判定（microfeed 仓 .scratch/search-permission/DESIGN.md §5.2）：
        // 书内搜索需当前账号有 book 权限；未拉到 / 无权限则不发起搜索。
        if (!SearchPermissionManager.isSearchAllowed(SearchEntry.BOOK)) {
            toast(getString(R.string.search_permission_denied));
            return;
        }

        presenter.search(keyword);
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

    /**
     * 显示搜索结果（T3/D3：入参改为新结构 GroupData/ItemData，与适配器绑定用的 groupDataList 同构）
     */
    @Override
    public void showSearchResults(List<GroupData> groups, List<List<ItemData>> items, int totalCount) {
        // 双保险：非搜索态收到搜索结果一律丢弃。
        // Presenter 侧已用 cancelSearch() 作废在途序号，这里再挡一层——
        // 万一某条路径漏了 cancelSearch，也不会把搜索结果写进已恢复的全量列表。
        if (!isSearchActive()) {
            EasyLog.print("TipsBookNetReadFragment", "非搜索态收到搜索结果，已丢弃");
            return;
        }
        // Presenter 已在主线程回调，这里只需渲染
        if (adapter != null && groups != null) {
            adapter.setSearchData(groups, items);
        }
        if (numTips != null) {
            numTips.setText(String.format("%d个结果", totalCount));
        }
    }

    /**
     * 书籍加载状态。本页布局无 loading 控件、书籍加载进度也一直未做展示，保持空实现。
     *
     * <p>搜索的进行中提示走 {@link #showSearching(boolean)}，两者不可混用 ——
     * 本方法被 loadBookContent / onChaptersLoaded 复用，若在此写「搜索中…」，
     * 打开任意书籍都会闪搜索提示。
     */
    @Override
    public void showLoading(boolean isLoading) {
        // 保持空实现，见上方说明
    }

    /**
     * 显示/结束「搜索中」提示（D8/§4.8）。
     *
     * <p>本页布局无 loading 控件，且项目内无可复用的 loading 组件，故复用既有的结果提示位
     * numTips 承载搜索反馈，不新增控件、不改布局。真正的阻塞风险已由「检索移出主线程」消除，
     * 此处只是给用户一个进行中提示。
     *
     * <p>结束态：成功由 showSearchResults 写「N个结果」覆盖；失败路径不写结果数，故必须
     * 在这里清空，否则「搜索中…」会永久残留（失败没有 showSearchResults 来覆盖它）。
     * 清空不会误伤成功态 —— 那两条路径互斥。
     */
    @Override
    public void showSearching(boolean searching) {
        if (numTips == null) {
            return;
        }
        if (searching) {
            numTips.setText(R.string.search_in_progress);
        } else {
            numTips.setText("");
        }
    }

    @Override
    public void showError(String message) {
        // 失败路径可能正显示着「搜索中…」，先复位再 toast，否则提示会卡住不消失
        if (numTips != null) {
            numTips.setText("");
        }
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
        // ✅ D2.1 守卫：搜索态下列表为过滤结果，章节下载完成回调不应应用到当前列表
        if (isSearchActive()) {
            return;
        }
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
