/*
 * 项目名: AndroidProject
 * 类名: TipsBookReadPresenter.java
 * 包名: run.yigou.gxzy.ui.tips.presenter
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.presenter;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentCallbacks2;
import android.text.SpannableStringBuilder;

import run.yigou.gxzy.ui.tips.utils.SearchMatcher;
import run.yigou.gxzy.ui.tips.utils.TextHighlighter;

import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.data.DataConverter; // Kept existing

import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.common.FragmentSetting;

import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.ui.tips.contract.TipsBookReadContract;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.data.BookData;
import run.yigou.gxzy.ui.tips.data.BookDataManager;
import run.yigou.gxzy.ui.tips.data.ChapterData;
import run.yigou.gxzy.ui.tips.data.ChapterIndexBuilder;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
import run.yigou.gxzy.ui.tips.repository.BookRepository;
import run.yigou.gxzy.ui.tips.tipsutils.ChapterDownloadManager;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.utils.DebugLog;

/**
 * TipsBookRead Presenter 实现
 * 负责业务逻辑处理
 */
public class TipsBookReadPresenter implements TipsBookReadContract.Presenter {

    private TipsBookReadContract.View view;
    private final BookRepository repository;
    private final ChapterDownloadManager downloadManager;
    private final BookDataManager dataManager;
    private final GlobalDataHolder globalData;

    // 状态管理
    private int currentBookId;
    private int currentChapterIndex = -1;
    private boolean isShowBookCollect = false;
    private boolean isSearchMode = false;

    // 数据管理
    private List<Chapter> allChapters;
    private BookData currentBookData;  // 新数据模型
    private ChapterIndexBuilder indexBuilder;  // 搜索索引
    private java.util.Set<Integer> loadedBookFangs = new java.util.HashSet<>();  // 已加载药方的书籍集合
    private boolean isShanghanBook = false;  // 是否为宋版伤寒书籍

    public TipsBookReadPresenter(TipsBookReadContract.View view) {
        this.view = view;
        this.repository = new BookRepository();
        this.downloadManager = new ChapterDownloadManager();
        this.dataManager = BookDataManager.getInstance();
        this.globalData = GlobalDataHolder.getInstance();
    }

    @Override
    public void onViewCreated() {
        EasyLog.print("TipsBookReadPresenter", "View 创建完成");
    }

    @Override
    public void onViewDestroy() {
        EasyLog.print("TipsBookReadPresenter", "View 销毁");
        
        // 取消所有下载
        if (downloadManager != null) {
            downloadManager.cancelAll();
        }
        
        // 释放引用
        view = null;
        allChapters = null;
        currentBookData = null;
    }

    @Override
    public void onViewResume() {
        // 可选实现
    }

    @Override
    public void onViewPause() {
        // 可选实现
    }
    
    /**
     * 内存压力回调
     * 当系统内存紧张时自动释放缓存
     * 
     * @param level 内存压力级别
     */
    public void onTrimMemory(int level) {
        EasyLog.print("TipsBookReadPresenter", "内存压力回调: level=" + level);
        
        if (dataManager != null) {
            dataManager.trimMemory(level);
        }
        
        // 极端内存压力时释放当前数据
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            if (currentBookData != null) {
                // 清除所有章节缓存
                for (ChapterData chapter : currentBookData.getAllChapters()) {
                    if (chapter != null) {
                        chapter.clearCache();
                    }
                }
                EasyLog.print("TipsBookReadPresenter", "释放当前书籍缓存");
            }
        }
    }

    @Override
    public void loadBookContent(int bookId, int lastReadPosition, boolean isShowBookCollect) {
        if (!isViewActive()) {
            return;
        }

        this.currentBookId = bookId;
        this.currentChapterIndex = lastReadPosition;
        this.isShowBookCollect = isShowBookCollect;

        view.showLoading(true);

        try {
            // 获取书籍信息（使用全局数据）
            TabNavBody book = repository.getBookInfoFromGlobal(bookId);
            if (book == null) {
                view.showLoading(false);
                view.showError("书籍信息不存在");
                EasyLog.print("TipsBookReadPresenter", "书籍信息获取失败: bookId=" + bookId);
                return;
            }

            // 调用重载方法
            loadBookContentInternal(book, bookId, lastReadPosition, isShowBookCollect);
            
        } catch (Exception e) {
            view.showLoading(false);
            view.showError("加载失败: " + e.getMessage());
            EasyLog.print("TipsBookReadPresenter", "加载书籍内容失败: " + e.getMessage());
        }
    }

    /**
     * 加载书籍内容（重载方法，接受 TabNavBody）
     */
    public void loadBookContent(TabNavBody book, int bookId, int lastReadPosition, boolean isShowBookCollect) {
        if (!isViewActive()) {
            return;
        }

        this.currentBookId = bookId;
        this.currentChapterIndex = lastReadPosition;
        this.isShowBookCollect = isShowBookCollect;

        view.showLoading(true);

        try {
            if (book == null) {
                view.showLoading(false);
                view.showError("书籍信息不存在");
                EasyLog.print("TipsBookReadPresenter", "TabNavBody 为 null");
                return;
            }

            // 调用内部实现
            loadBookContentInternal(book, bookId, lastReadPosition, isShowBookCollect);
            
        } catch (Exception e) {
            view.showLoading(false);
            view.showError("加载失败: " + e.getMessage());
            EasyLog.print("TipsBookReadPresenter", "加载书籍内容失败: " + e.getMessage());
        }
    }

    /**
     * 内部实现：加载书籍内容
     */
    private void loadBookContentInternal(TabNavBody book, int bookId, int lastReadPosition, boolean isShowBookCollect) {
        EasyLog.print("TipsBookReadPresenter", "开始加载书籍内容: " + book.getBookName());

        // 加载书籍数据（新数据模型，使用 LRU 缓存）
        currentBookData = repository.getBookData(bookId);
        if (currentBookData == null) {
            view.showLoading(false);
            view.showError("书籍数据加载失败");
            return;
        }
        EasyLog.print("TipsBookReadPresenter", "BookData 加载成功，章节数=" + currentBookData.getChapterCount());
        
        // 【新架构】设置TipsNetHelper的BookRepository上下文，用于点击链接时搜索
        TipsNetHelper.setBookContext(repository, bookId);

        // 获取章节列表

        // 获取章节列表
        allChapters = repository.getChapters(bookId);
        if (allChapters == null || allChapters.isEmpty()) {
            view.showLoading(false);
            view.showError("章节列表为空");
            return;
        }
        EasyLog.print("TipsBookReadPresenter", "章节列表加载成功，共 " + allChapters.size() + " 个章节");

        // 初始化下载管理器缓存
        downloadManager.initDownloadedCache(allChapters);

        // 构建搜索索引（新功能）
        indexBuilder = new ChapterIndexBuilder();
        indexBuilder.buildIndex(allChapters);
        EasyLog.print("TipsBookReadPresenter", "搜索索引构建完成");

        // 兼容处理宋版伤寒
        if (bookId == AppConst.ShangHanNo) {
            setupShanghanContentListener();
        }

        // 加载药方数据
        loadBookFang(book);

        // 显示章节列表
        displayChapterList();

        // 滚动到上次阅读位置
        if (lastReadPosition > 0 && lastReadPosition < allChapters.size()) {
            view.scrollToPosition(lastReadPosition);
            view.expandChapter(lastReadPosition);
        }

        view.showLoading(false);
        EasyLog.print("TipsBookReadPresenter", "书籍内容加载完成");
    }

    @Override
    public void refreshData() {
        if (currentBookId > 0) {
            // 清除缓存并重新加载
            repository.clearCacheForBook(currentBookId);
            loadBookContent(currentBookId, currentChapterIndex, isShowBookCollect);
        }
    }

    @Override
    public void onChapterClick(int position) {
        EasyLog.print("TipsBookReadPresenter", "========== onChapterClick 开始 ==========");
        EasyLog.print("TipsBookReadPresenter", "点击章节位置: position=" + position);
        
        if (!isViewActive()) {
            EasyLog.print("TipsBookReadPresenter", "前置检查失败: View 未激活");
            return;
        }
        
        // 检查必要数据是否已加载
        if (currentBookData == null || allChapters == null) {
            EasyLog.print("TipsBookReadPresenter", "数据未加载: currentBookData=" + (currentBookData != null) + 
                ", allChapters=" + (allChapters != null));
            return;
        }
        
        // 边界检查
        if (position < 0 || position >= allChapters.size()) {
            EasyLog.print("TipsBookReadPresenter", "position 越界: " + position);
            return;
        }

        try {
            // 记录当前位置
            if (isShowBookCollect) {
                currentChapterIndex = position;
            }
            
            // 直接从 allChapters 获取章节实体
            Chapter chapter = allChapters.get(position);
            Long signatureId = chapter.getSignatureId();
            
            EasyLog.print("TipsBookReadPresenter", "章节信息: signatureId=" + signatureId + 
                ", header=" + chapter.getChapterHeader());
            
            // 查找 ChapterData
            ChapterData chapterData = currentBookData.findChapterBySignature(signatureId);
            EasyLog.print("TipsBookReadPresenter", "ChapterData 查找结果: " + (chapterData != null ? "找到" : "未找到"));
            
            // 检查章节下载状态
            boolean isDownloaded = chapter.getIsDownload();
            boolean isContentLoaded = chapterData != null && chapterData.isContentLoaded();
            
            EasyLog.print("TipsBookReadPresenter", "章节状态检查: isDownloaded=" + isDownloaded + 
                ", chapterData!=null=" + (chapterData != null) + 
                ", isContentLoaded=" + isContentLoaded);
            
            // 懒加载逻辑：仅对已下载但内容未加载的章节生效
            if (isDownloaded && chapterData != null && !chapterData.isContentLoaded()) {
                // 已下载但内容未加载，使用懒加载机制
                EasyLog.print("TipsBookReadPresenter", "触发懒加载: position=" + position);
                
                // 生命周期检查（懒加载前）
                if (!isViewActive()) {
                    EasyLog.print("TipsBookReadPresenter", "懒加载前检查：View 已销毁，取消懒加载");
                    return;
                }
                
                loadChapterLazy(position, chapter);
                return;
            }
            
            // 已下载的章节处理
            if (isDownloaded) {
                EasyLog.print("TipsBookReadPresenter", "已下载章节，检查内容加载状态");
                
                // 确保内容已加载到内存
                if (chapterData != null && !chapterData.isContentLoaded()) {
                    // 从数据库加载内容
                    EasyLog.print("TipsBookReadPresenter", "从数据库加载章节内容");
                    repository.loadChapterContent(currentBookData, chapter);
                }
                
                // 转换为 UI 格式
                HH2SectionData sectionData = DataConverter.toHH2SectionData(chapterData, chapter);
                
                EasyLog.print("TipsBookReadPresenter", "更新章节内容: position=" + position + 
                    ", contentSize=" + (sectionData != null && sectionData.getData() != null ? sectionData.getData().size() : 0));
                
                // 更新 UI（不强制展开，由 Fragment 控制展开/收缩）
                view.updateChapterContent(position, sectionData);
                
                // 生命周期检查（预加载前）
                if (!isViewActive()) {
                    EasyLog.print("TipsBookReadPresenter", "预加载前检查：View 已销毁，取消预加载");
                    return;
                }
                
                // 触发预加载
                EasyLog.print("TipsBookReadPresenter", "触发预加载");
                androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
                downloadManager.preloadChapters(allChapters, position, lifecycleOwner);
                return;
            }
            
            EasyLog.print("TipsBookReadPresenter", "未下载章节，开始下载流程");

            // 生命周期检查（下载前）
            if (!isViewActive()) {
                EasyLog.print("TipsBookReadPresenter", "下载前检查：View 已销毁，取消下载");
                return;
            }

            // 检查是否正在下载
            if (downloadManager.isChapterDownloading(chapter)) {
                view.showToast("章节正在下载中...");
                return;
            }

            // 高优先级下载
            view.showDownloadProgress(position, "正在下载...");
            
            // 将 view (Fragment) 作为生命周期对象传递给下载管理器
            // TipsBookNetReadFragment extends AppFragment extends BaseFragment extends Fragment (LifecycleOwner)
            androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
            
            downloadManager.downloadChapter(lifecycleOwner, chapter, new ChapterDownloadManager.DownloadCallback() {
                @Override
                public void onSuccess(Chapter chapter, HH2SectionData sectionData) {
                    // 生命周期检查（下载后）
                    if (!isViewActive()) {
                        EasyLog.print("TipsBookReadPresenter", "下载后检查：View 已销毁，丢弃结果");
                        return;
                    }
                    
                    view.updateChapterContent(position, sectionData);
                    view.updateDownloadStatus(position, true);
                    view.showToast("章节下载完成");
                    
                    // 触发预加载(传递生命周期对象,Fragment 销毁时自动取消)
                    downloadManager.preloadChapters(allChapters, position, lifecycleOwner);
                }

                @Override
                public void onFailure(Chapter chapter, Exception e) {
                    // 生命周期检查（失败回调）
                    if (!isViewActive()) {
                        EasyLog.print("TipsBookReadPresenter", "下载失败回调：View 已销毁，忽略错误");
                        return;
                    }
                    view.showError("下载失败: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            view.showError("处理点击异常: " + e.getMessage());
            EasyLog.print("TipsBookReadPresenter", "章节点击异常: " + e.getMessage());
        }
    }

    @Override
    public void reloadChapter(int position) {
        if (!isViewActive() || allChapters == null) {
            return;
        }

        try {
            if (position < 0 || position >= allChapters.size()) {
                view.showError("章节索引越界");
                return;
            }

            Chapter chapter = allChapters.get(position);

            if (chapter == null) {
                view.showError("未找到章节信息");
                return;
            }

            view.showToast("开始重新下载: " + chapter.getChapterHeader());

            // 使用新 API 异步下载
            if (currentBookData != null) {
                androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
                repository.downloadChapterAsync(chapter, currentBookData, lifecycleOwner, 
                    new BookRepository.DataCallback<ChapterData>() {
                        @Override
                        public void onSuccess(ChapterData data) {
                            if (isViewActive()) {
                                // 转换为旧格式供 View 使用（兼容）
                                HH2SectionData sectionData = DataConverter.toHH2SectionData(data, chapter);
                                view.updateChapterContent(position, sectionData);
                                view.showToast("重新下载完成");
                            }
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            if (isViewActive()) {
                                view.showError("重新下载失败: " + e.getMessage());
                            }
                        }
                    });
            } else {
                // 降级：使用旧 API（带生命周期绑定）
                androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
                repository.downloadChapter(chapter, lifecycleOwner, new BookRepository.DataCallback<HH2SectionData>() {
                    @Override
                    public void onSuccess(HH2SectionData data) {
                        if (isViewActive()) {
                            view.updateChapterContent(position, data);
                            view.showToast("重新下载完成");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isViewActive()) {
                            view.showError("重新下载失败: " + e.getMessage());
                        }
                    }
                });
            }

        } catch (Exception e) {
            view.showError("重新下载异常: " + e.getMessage());
        }
    }

    // ==================== 注释掉未使用的搜索方法 ====================
    // 说明: TipsBookNetReadFragment 实际使用 SearchCoordinator.searchGlobal
    // 此方法从未被调用，保留代码以备参考
    /*
    @Override
    public void search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            clearSearch();
            return;
        }

        isSearchMode = true;

        try {
            long startTime = System.currentTimeMillis();
            
            EasyLog.print("TipsBookReadPresenter", "开始严格搜索(TipsNetHelper): " + keyword);
            
            // 1. 准备搜索关键字
            SearchKeyEntity searchKeyEntity = new SearchKeyEntity(new StringBuilder(keyword));
            
            // 2. 从数据库获取整本书所有章节内容 (与 BookContentSearchActivity 一致)
            // 使用 ConvertEntity 而非 DataConverter，确保搜索范围覆盖整本书
            List<HH2SectionData> allContent = ConvertEntity.getBookChapterDetailList(currentBookId);
            
            // 2.1 针对伤寒论进行特殊过滤 (与 BookContentSearchActivity 逻辑保持一致)
            if (currentBookId == AppConst.ShangHanNo) {
                 allContent = filterShangHanData(allContent);
            }
            
            // 3. 获取全书别名映射（用于增强搜索）
            run.yigou.gxzy.ui.tips.data.GlobalDataHolder globalData = run.yigou.gxzy.ui.tips.data.GlobalDataHolder.getInstance();
            java.util.Map<String, String> yaoAliasDict = globalData.getYaoAliasDict();
            java.util.Map<String, String> fangAliasDict = globalData.getFangAliasDict();
            
            // 4. 调用核心搜索方法 (Delegation to TipsNetHelper -> TipsSearchEngine)
            // 该方法支持正则、多关键字、别名匹配和高亮生成
            ArrayList<HH2SectionData> filteredData = TipsNetHelper.getSearchHh2SectionData(
                searchKeyEntity, 
                allContent, 
                yaoAliasDict, 
                fangAliasDict
            );

            // 5. 转换为显示格式
            // 注意：isExpand 参数设为 false (默认折叠，与用户要求一致)
            ArrayList<ExpandableGroupEntity> groups = GroupModel.getExpandableGroups(filteredData, false);

            int totalMatchCount = searchKeyEntity.getSearchResTotalNum();
            if (isViewActive()) {
                view.showSearchResults(groups, totalMatchCount);
            }
            
            long searchTime = System.currentTimeMillis() - startTime;
            EasyLog.print("TipsBookReadPresenter", "搜索完成: " + totalMatchCount + 
                " 个匹配项, 耗时 " + searchTime + "ms");

        } catch (Exception e) {
            view.showError("搜索失败: " + e.getMessage());
            EasyLog.print("TipsBookReadPresenter", "搜索异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<HH2SectionData> filterShangHanData(List<HH2SectionData> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return new ArrayList<>();
        }
        
        FragmentSetting fragmentSetting = AppApplication.getApplication().fragmentSetting;
        if (fragmentSetting == null) {
            return contentList;
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
    */
    
    // 空实现：搜索逻辑已移至 SearchCoordinator
    @Override
    public void search(String keyword) {
        // 搜索由 SearchCoordinator.searchGlobal 处理
        // 此方法保留以满足接口契约
        EasyLog.print("TipsBookReadPresenter", "search() 已弃用，使用 SearchCoordinator");
    }

    // ==================== 以下接口方法未被 Fragment 调用，保留空实现 ====================
    
    @Override
    public void clearSearch() {
        // 未使用：搜索由 SearchCoordinator 处理
        // isSearchMode = false;
        // displayChapterList();
    }

    @Override
    public void onBackPressed(boolean shouldSave) {
        // 未使用：返回键逻辑由 Fragment 直接处理
        /*
        if (!isViewActive()) {
            return;
        }

        if (!shouldSave) {
            // 直接返回
            return;
        }

        try {
            TabNavBody book = repository.getBookInfo(currentBookId);
            if (book == null) {
                return;
            }

            // 查询书架
            ArrayList<Book> books = repository.queryBookshelf(book.getBookNo());

            if (books == null || books.isEmpty()) {
                // 显示添加到书架对话框
                view.showToast("是否将书籍添加到书架？");
            } else {
                // 更新阅读进度
                if (currentChapterIndex != -1) {
                    Book bookEntity = books.get(0);
                    bookEntity.setLastReadPosition(currentChapterIndex);
                    bookEntity.setHistoriographerNumb(currentChapterIndex);
                    repository.updateReadingProgress(bookEntity);
                }
            }

        } catch (Exception e) {
            EasyLog.print("TipsBookReadPresenter", "保存阅读进度失败: " + e.getMessage());
        }
        */
    }

    @Override
    public void onSettingChanged() {
        // 未使用：设置变更由 Fragment 的 EventBus 处理
        // refreshData();
    }

    @Override
    public void onJumpToPosition(int groupPosition, int childPosition) {
        // 未使用：跳转逻辑由 Fragment 直接处理
        /*
        if (isViewActive()) {
            view.scrollToPosition(groupPosition);
            view.expandChapter(groupPosition);
        }
        */
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 显示章节列表
     */
    private void displayChapterList() {
        if (!isViewActive() || currentBookData == null || allChapters == null) {
            return;
        }

        try {
            // 使用 DataConverter 将数据转换为 UI 格式
            ArrayList<HH2SectionData> contentList = DataConverter.toHH2SectionDataList(
                currentBookData, allChapters);
            
            // 构建可展开的分组结构
            ArrayList<ExpandableGroupEntity> groups = GroupModel.getExpandableGroups(
                contentList, false);
            
            view.showChapterList(groups);
        } catch (Exception e) {
            view.showError("显示章节列表失败: " + e.getMessage());
            EasyLog.print("TipsBookReadPresenter", "显示章节列表异常: " + e.getMessage());
        }
    }

    /**
     * 加载药方数据（只在初始化时执行一次）
     * 流程：
     * 1. 检查是否已加载到内存
     * 2. 检查数据库是否已有方剂数据
     * 3. 如果数据库有数据，直接加载到BookData
     * 4. 如果数据库无数据，从网络下载
     */
    private void loadBookFang(TabNavBody book) {
        // 检查是否已加载到内存
        if (loadedBookFangs.contains(currentBookId)) {
            EasyLog.print("TipsBookReadPresenter", "药方已加载到内存，跳过: bookId=" + currentBookId);
            return;
        }
        
        // 生命周期检查
        if (!isViewActive()) {
            EasyLog.print("TipsBookReadPresenter", "View 未激活，取消药方加载");
            return;
        }
        
        // 标记为已加载（预先标记，避免重复请求）
        loadedBookFangs.add(currentBookId);
        
        // 【优化】先检查数据库是否已有方剂数据
        ArrayList<run.yigou.gxzy.ui.tips.DataBeans.Fang> cachedFangList = 
            ConvertEntity.getFangDetailList(currentBookId);
        
        if (cachedFangList != null && !cachedFangList.isEmpty()) {
            // 数据库已有方剂数据，直接加载到BookData
            EasyLog.print("TipsBookReadPresenter", "从数据库加载方剂: " + cachedFangList.size() + " 个");
            
            List<DataItem> fangItemList = new ArrayList<>(cachedFangList);
            ChapterData fangChapterData = new ChapterData(0L, book.getBookName() + "方", 0, fangItemList);
            
            if (currentBookData != null) {
                currentBookData.setFangData(fangChapterData);
                EasyLog.print("TipsBookReadPresenter", "✅ 方剂数据已从数据库加载到BookData: " + cachedFangList.size() + " 个");
            }
            return;
        }
        
        // 数据库无数据，从网络下载
        EasyLog.print("TipsBookReadPresenter", "数据库无方剂数据，开始从网络下载");
        
        androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
        repository.downloadBookFang(currentBookId, lifecycleOwner, new BookRepository.DataCallback<List<run.yigou.gxzy.ui.tips.DataBeans.Fang>>() {
            @Override
            public void onSuccess(List<run.yigou.gxzy.ui.tips.DataBeans.Fang> data) {
                EasyLog.print("TipsBookReadPresenter", "药方数据网络下载完成: " + data.size() + " 个");
                
                // 【新架构】将方剂数据设置到BookData
                if (data != null && !data.isEmpty() && currentBookData != null) {
                    // 创建ChapterData包装方剂列表
                    List<DataItem> fangItemList = new ArrayList<>(data);
                    ChapterData fangChapterData = new ChapterData(0L, book.getBookName() + "方", 0, fangItemList);
                    
                    currentBookData.setFangData(fangChapterData);
                    EasyLog.print("TipsBookReadPresenter", "✅ 方剂数据已设置到BookData: " + data.size() + " 个");
                }
            }

            @Override
            public void onFailure(Exception e) {
                EasyLog.print("TipsBookReadPresenter", "药方数据加载失败: " + e.getMessage());
                // 失败时移除标记，允许重试
                loadedBookFangs.remove(currentBookId);
            }
        });
    }

    /**
     * 设置宋版伤寒内容监听器
     */
    private void setupShanghanContentListener() {
        // 标记为宋版伤寒书籍
        isShanghanBook = true;
        EasyLog.print("TipsBookReadPresenter", "宋版伤寒内容监听器已设置");
    }

    /**
     * 获取当前章节内容列表（转换为 HH2SectionData）
     * 用于 Fragment UI 展示，支持宋版伤寒过滤
     */
    public List<HH2SectionData> getChapterContentList() {
        if (currentBookData == null || allChapters == null) {
            return new ArrayList<>();
        }
        
        // 转换为 HH2SectionData
        ArrayList<HH2SectionData> contentList = convertChaptersToSectionData(allChapters);
        
        // 如果是宋版伤寒，应用内容过滤
        if (isShanghanBook) {
            contentList = filterShanghanContent(contentList);
        }
        
        return contentList;
    }

    /**
     * 宋版伤寒内容过滤逻辑
     */
    private ArrayList<HH2SectionData> filterShanghanContent(ArrayList<HH2SectionData> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return new ArrayList<>();
        }

        int size = contentList.size();
        int start = 0;
        int end = size;

        // 读取设置
        run.yigou.gxzy.app.AppApplication app = run.yigou.gxzy.app.AppApplication.getApplication();
        if (app == null || app.fragmentSetting == null) {
            return contentList;
        }

        if (!app.fragmentSetting.isSong_JinKui()) {
            if (!app.fragmentSetting.isSong_ShangHan()) {
                start = 8;
                end = Math.min(18, size);
            } else {
                end = Math.min(26, size);
            }
        } else {
            if (!app.fragmentSetting.isSong_ShangHan()) {
                start = 8;
            }
        }

        if (start < size) {
            return new ArrayList<>(contentList.subList(start, end));
        } else {
            return contentList;
        }
    }

    /**
     * 将 Chapter 列表转换为 HH2SectionData 列表
     */
    private ArrayList<HH2SectionData> convertChaptersToSectionData(List<Chapter> chapters) {
        ArrayList<HH2SectionData> result = new ArrayList<>();
        
        if (chapters == null || chapters.isEmpty()) {
            return result;
        }
        
        for (Chapter chapter : chapters) {
            try {
                // 从 currentBookData 获取对应的 ChapterData
                ChapterData chapterData = currentBookData.findChapterBySignature(chapter.getSignatureId());
                
                // 使用 DataConverter 转换
                HH2SectionData sectionData = DataConverter.toHH2SectionData(chapterData, chapter);
                result.add(sectionData);
            } catch (Exception e) {
                EasyLog.print("TipsBookReadPresenter", "转换章节失败: " + e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 懒加载章节内容
     */
    private void loadChapterLazy(int position, Chapter chapter) {
        view.showDownloadProgress(position, "正在加载...");
        
        // 传递 Fragment 生命周期,确保 Fragment 销毁时自动取消网络请求
        androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
        repository.loadChapterLazy(currentBookId, position, lifecycleOwner, 
            new BookRepository.DataCallback<ChapterData>() {
                @Override
                public void onSuccess(ChapterData data) {
                    // 生命周期检查（懒加载成功回调）
                    if (!isViewActive()) {
                        EasyLog.print("TipsBookReadPresenter", "懒加载成功回调：View 已销毁，丢弃结果");
                        return;
                    }
                    
                    // 转换为 UI 格式
                    HH2SectionData sectionData = DataConverter.toHH2SectionData(data, chapter);
                    view.updateChapterContent(position, sectionData);
                    view.updateDownloadStatus(position, true);
                    
                    // 触发预加载相邻章节
                    preloadAdjacentChapters(position);
                }

                @Override
                public void onFailure(Exception e) {
                    // 生命周期检查（懒加载失败回调）
                    if (!isViewActive()) {
                        EasyLog.print("TipsBookReadPresenter", "懒加载失败回调：View 已销毁，忽略错误");
                        return;
                    }
                    view.showError("加载失败: " + e.getMessage());
                }
            });
    }
    
    /**
     * 预加载相邻章节
     */
    private void preloadAdjacentChapters(int currentPosition) {
        if (currentBookData == null || allChapters == null) {
            return;
        }
        
        // 预加载下一章
        if (currentPosition + 1 < allChapters.size()) {
            androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
            repository.loadChapterLazy(currentBookId, currentPosition + 1, lifecycleOwner, null);
        }
        
        // 预加载上一章
        if (currentPosition > 0) {
            androidx.lifecycle.LifecycleOwner lifecycleOwner2 = (androidx.lifecycle.LifecycleOwner) view;
            repository.loadChapterLazy(currentBookId, currentPosition - 1, lifecycleOwner2, null);
        }
    }

    /**
     * 根据 signatureId 查找章节（保留用于兼容）
     * 注意：新代码应优先使用 BookData.findChapterBySignature() O(1) 查找
     */
    private Chapter findChapterBySignatureId(long signatureId) {
        // 优先使用 O(1) 查找
        if (currentBookData != null) {
            ChapterData chapterData = currentBookData.findChapterBySignature(signatureId);
            if (chapterData != null) {
                // 从 allChapters 中找到对应的 Chapter 实体
                for (Chapter chapter : allChapters) {
                    if (chapter != null && chapter.getSignatureId() == signatureId) {
                        return chapter;
                    }
                }
            }
        }
        
        // 降级：O(n) 查找
        if (allChapters == null || allChapters.isEmpty()) {
            return null;
        }

        for (Chapter chapter : allChapters) {
            if (chapter != null && chapter.getSignatureId() == signatureId) {
                return chapter;
            }
        }
        return null;
    }

    /**
     * 检查 View 是否处于活动状态（增强版）
     */
    private boolean isViewActive() {
        if (view == null) {
            EasyLog.print("TipsBookReadPresenter", "生命周期检查失败: view == null");
            return false;
        }
        
        // 检查 View 本身的 isActive 状态
        if (!view.isActive()) {
            EasyLog.print("TipsBookReadPresenter", "生命周期检查失败: view.isActive() == false");
            return false;
        }
        
        return true;
    }
}
