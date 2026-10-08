/*
 * 项目名: AndroidProject
 * 类名: TipsBookReadPresenter.java
 * 包名: run.yigou.gxzy.ui.reader.bookread.presenter
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.bookread.presenter;

import run.yigou.gxzy.log.EasyLog;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentCallbacks2;

import run.yigou.gxzy.ui.reader.helper.TipsNetHelper;
import run.yigou.gxzy.ui.reader.data.DataConverter; // Kept existing

import run.yigou.gxzy.data.local.entity.Book;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.helper.DataRepository;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.ui.reader.bookread.contract.TipsBookReadContract;
import run.yigou.gxzy.ui.reader.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.reader.entity.GroupModel;
import run.yigou.gxzy.ui.reader.data.BookData;
import run.yigou.gxzy.ui.reader.data.BookDataManager;
import run.yigou.gxzy.ui.reader.data.ChapterData;
import run.yigou.gxzy.ui.reader.data.ChapterIndexBuilder;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.ui.reader.repository.BookRepository;
import run.yigou.gxzy.manager.chapter.ChapterContentManager;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;
import run.yigou.gxzy.manager.Callback;
import run.yigou.gxzy.ui.reader.adapter.model.GroupData;
import run.yigou.gxzy.ui.reader.adapter.model.ItemData;
import run.yigou.gxzy.ui.reader.search.SearchCoordinator;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * TipsBookRead Presenter 实现
 * 负责业务逻辑处理
 */
public class TipsBookReadPresenter implements TipsBookReadContract.Presenter {

    private TipsBookReadContract.View view;
    private final BookRepository repository;
    private final ChapterContentManager contentManager;
    private final BookDataManager dataManager;
    private final GlobalDataHolder globalData;

    // 状态管理
    private String currentBookId;
    private TabNavBody currentBookInfo; // 当前书籍信息
    private int currentChapterIndex = -1;
    private boolean isShowBookCollect = false;

    // 数据管理
    private List<Chapter> allChapters;
    private BookData currentBookData;  // 新数据模型
    private ChapterIndexBuilder indexBuilder;  // 搜索索引
    private java.util.Set<String> loadedBookFangs = new java.util.HashSet<>();  // 已加载药方的书籍集合

    /** D8：搜索在途序号，只认最后一次搜索结果，避免过期结果覆盖新结果 */
    private int searchSeq = 0;

    /**
     * 作废当前在途的搜索（D8）。
     *
     * <p>用户清空搜索框时，Fragment 已把列表恢复成全量章节，此时若在途搜索的结果
     * 仍回填搜索结果，会覆盖已恢复的全量列表；更麻烦的是清空后 isSearchActive()
     * 为 false，updateChapterContent/updateDownloadStatus 的搜索态守卫会全部失效，
     * 重新打开 D2/D2.1 想关闭的污染路径。故清空时必须递增序号作废在途结果。
     */
    @Override
    public void cancelSearch() {
        if (searchSeq != 0) {
            searchSeq++;
            EasyLog.print("TipsBookReadPresenter", "cancelSearch() 作废在途搜索");
        }
    }

    public TipsBookReadPresenter(TipsBookReadContract.View view) {
        this.view = view;
        this.repository = new BookRepository();
        this.contentManager = new ChapterContentManager();
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
        
        // 取消所有获取
        if (contentManager != null) {
            contentManager.cancelAll();
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
    public void loadBookContent(String bookId, int lastReadPosition, boolean isShowBookCollect) {
        if (!isViewActive()) {
            return;
        }

        this.currentBookId = bookId;
        this.currentChapterIndex = lastReadPosition;
        this.isShowBookCollect = isShowBookCollect;

        view.showLoading(true);

        try {
            // 获取书籍信息（使用全局数据）
            TabNavBody book = repository.getBookInfo(bookId);
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
    public void loadBookContent(TabNavBody book, String bookId, int lastReadPosition, boolean isShowBookCollect) {
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
    private void loadBookContentInternal(TabNavBody book, String bookId, int lastReadPosition, boolean isShowBookCollect) {
        this.currentBookInfo = book;
        EasyLog.print("TipsBookReadPresenter", "开始加载书籍内容: " + book.getBookName());

        // 加载书籍数据（新数据模型，使用 LRU 缓存）
        // 改走 BookRepository.getBookDataAsync：读库发生在 DB 串行后台线程，结果回主线程。
        // 原来这里在主线程直接同步查库（repository.getBookData），会触发 StrictMode DiskReadViolation。
        // BookData 到手之前，下面依赖它的工作（setBookContext、章节列表异步加载）都搬进 onSuccess，
        // 与既有 getChaptersAsync 串成两段式后台加载；getBookData 内部已填充章节缓存，
        // 后续 getChaptersAsync 会直接命中缓存，不会再进一次后台。
        final int lastPosition = lastReadPosition;
        final String finalBookId = bookId;
        final TabNavBody finalBook = book;
        repository.getBookDataAsync(bookId, new Callback<BookData>() {
            @Override
            public void onSuccess(BookData bookData) {
                // 读到结果前阅读页可能已退出，先判活再落字段、再继续，避免向失效页面发指令。
                if (!isViewActive()) {
                    return;
                }
                currentBookData = bookData;
                if (currentBookData == null) {
                    view.showLoading(false);
                    view.showError("书籍数据加载失败");
                    return;
                }
                EasyLog.print("TipsBookReadPresenter", "BookData 加载成功，章节数=" + currentBookData.getChapterCount());

                // 【新架构】设置TipsNetHelper的BookRepository上下文，用于点击链接时搜索
                TipsNetHelper.setBookContext(repository, bookId);

                // 获取章节列表
                // 读发生在 DB 串行后台线程（命中 getBookData 已填的缓存则直接回主线程），
                // 下面依赖这份列表的工作搬进 onChaptersLoaded，回调回主线程接上。
                repository.getChaptersAsync(finalBookId, new Callback<List<Chapter>>() {
                    @Override
                    public void onSuccess(List<Chapter> chapters) {
                        try {
                            onChaptersLoaded(chapters, lastPosition, finalBookId, finalBook);
                        } catch (Exception e) {
                            EasyLog.print("TipsBookReadPresenter", "加载章节列表后处理失败: " + e.getMessage());
                            view.showLoading(false);
                            view.showError("加载失败: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        EasyLog.print("TipsBookReadPresenter", "章节列表加载失败: " + e.getMessage());
                        view.showLoading(false);
                        view.showError("加载失败: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                EasyLog.print("TipsBookReadPresenter", "BookData 加载失败: " + e.getMessage());
                view.showLoading(false);
                view.showError("书籍数据加载失败");
            }
        });
    }

    /**
     * 章节列表到手之后的那一段工作（主线程）。
     *
     * <p>原来这段代码是 {@code loadBookContentInternal} 的下半段。因为现在章节要异步取，
     * 只能切成两步——这是这次线程迁移唯一必需的流程改动，其余逻辑逐行照搬。
     *
     * @param chapters         章节列表（可能为 null / 空）
     * @param lastReadPosition 上次阅读位置
     * @param bookId           书籍 ID
     * @param book             书籍信息
     */
    private void onChaptersLoaded(List<Chapter> chapters, int lastReadPosition, String bookId, TabNavBody book) {
        // 章节是异步读的：回调回来时阅读页可能已经退出（用户返回、切换书籍），
        // 此时继续 showLoading / 操作 View 就是在向一个已失效的页面发指令。
        if (!isViewActive()) {
            return;
        }
        allChapters = chapters;
        if (allChapters == null || allChapters.isEmpty()) {
            view.showLoading(false);
            view.showError("章节列表为空");
            return;
        }
        EasyLog.print("TipsBookReadPresenter", "章节列表加载成功，共 " + allChapters.size() + " 个章节");

        // 初始化内容管理器缓存
        contentManager.initContentCache(allChapters);

        // 构建搜索索引（新功能）
        indexBuilder = new ChapterIndexBuilder();
        indexBuilder.buildIndex(allChapters);
        EasyLog.print("TipsBookReadPresenter", "搜索索引构建完成");

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
        if (currentBookId != null && !currentBookId.isEmpty()) {
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
            String signatureId = chapter.getSignatureId();
            
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
                contentManager.preloadNearbyChapters(allChapters, position, lifecycleOwner);
                return;
            }
            
            EasyLog.print("TipsBookReadPresenter", "未下载章节，开始下载流程");

            // 生命周期检查（下载前）
            if (!isViewActive()) {
                EasyLog.print("TipsBookReadPresenter", "下载前检查：View 已销毁，取消下载");
                return;
            }

            // 检查是否正在获取
            if (contentManager.isContentFetching(chapter)) {
                view.showToast("章节正在获取中...");
                return;
            }

            // 高优先级下载
            view.showDownloadProgress(position, "正在下载...");
            
            // 将 view (Fragment) 作为生命周期对象传递给下载管理器
            // TipsBookNetReadFragment extends AppFragment extends BaseFragment extends Fragment (LifecycleOwner)
            androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
            
            contentManager.fetchChapterContent(lifecycleOwner, chapter, new ChapterContentManager.ContentCallback() {
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
                    contentManager.preloadNearbyChapters(allChapters, position, lifecycleOwner);
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
                    new Callback<ChapterData>() {
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
                        public void onError(Exception e) {
                            if (isViewActive()) {
                                view.showError("重新下载失败: " + e.getMessage());
                            }
                        }
                    });
            } else {
                // 降级：使用旧 API（带生命周期绑定）
                androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
                repository.downloadChapter(chapter, lifecycleOwner, new Callback<HH2SectionData>() {
                    @Override
                    public void onSuccess(HH2SectionData data) {
                        if (isViewActive()) {
                            view.updateChapterContent(position, data);
                            view.showToast("重新下载完成");
                        }
                    }

                    @Override
                    public void onError(Exception e) {
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


    // ==================== 搜索（T3/D3 统一入口 + D8 异步化） ====================

    /**
     * 书内全局搜索：整本书章节检索。
     *
     * <p>T3/D3：此前本方法是空实现，Fragment 侧自行直调 SearchCoordinator，形成两条搜索路径；
     * 现统一由本入口负责，Fragment 只发起不再自己检索。
     *
     * <p>D8：检索是「整本书 DB 读 + 过滤 + 高亮」，原先在主线程同步执行会卡 UI，
     * 现放到线程池执行，结果回主线程后再交给 View 渲染。
     */
    @Override
    public void search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        if (!isViewActive()) {
            return;
        }
        final String bookId = currentBookId;
        if (bookId == null || bookId.isEmpty()) {
            EasyLog.print("TipsBookReadPresenter", "search() 缺少 bookId，忽略本次搜索");
            view.showError("搜索失败：未加载书籍信息");
            return;
        }

        // 在途序号：用户连续输入时只认最后一次结果，避免旧结果覆盖新结果
        final int mySeq = ++searchSeq;
        final String trimmed = keyword.trim();
        EasyLog.print("TipsBookReadPresenter", "search() 开始: bookId=" + bookId + ", keyword=" + trimmed);
        view.showSearching(true);

        ThreadUtil.runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    android.util.Pair<List<GroupData>, List<List<ItemData>>> result =
                            // T6：传入「显示列表」而非 allChapters —— 两表填充顺序不保证一致（见 SearchCoordinator 类注释），
                            // 只有与适配器当前列表同坐标系，后续滚动 / 展开才不会越界。
                            new SearchCoordinator(bookId, getChapterContentList())
                                    .searchGlobal(trimmed);

                    ThreadUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 过期结果直接丢弃（期间用户又发起了新搜索，或已清空搜索框）
                            if (mySeq != searchSeq) {
                                EasyLog.print("TipsBookReadPresenter", "search() 丢弃过期结果: " + trimmed);
                                return;
                            }
                            if (!isViewActive() || view == null) {
                                return;
                            }
                            if (result == null) {
                                EasyLog.print("TipsBookReadPresenter", "search() 结果为 null: " + trimmed);
                                view.showSearching(false);
                                view.showError("搜索失败，请稍后重试");
                                return;
                            }
                            int total = countMatches(result.second);
                            EasyLog.print("TipsBookReadPresenter",
                                    "search() 完成: keyword=" + trimmed + ", 命中章节=" + result.first.size()
                                            + ", 匹配数=" + total);
                            view.showSearchResults(result.first, result.second, total);
                        }
                    });
                } catch (Exception e) {
                    // 检索在子线程执行，异常不会自动冒泡，必须就地捕获并回主线程提示，
                    // 否则会静默吞掉（DB 读失败、章节结构异常等）
                    EasyLog.print("TipsBookReadPresenter",
                            "search() 检索异常: keyword=" + trimmed + ", " + e.getMessage());
                    EasyLog.print(e);
                    ThreadUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mySeq != searchSeq) {
                                return;
                            }
                            if (isViewActive() && view != null) {
                                view.showSearching(false);
                                view.showError("搜索失败：" + e.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    /** 统计搜索结果里命中的子项总数（跨所有分组求和） */
    private int countMatches(List<List<ItemData>> itemDataList) {
        int total = 0;
        if (itemDataList != null) {
            for (List<ItemData> items : itemDataList) {
                if (items != null) {
                    total += items.size();
                }
            }
        }
        return total;
    }

    // ==================== 以下接口方法未被 Fragment 调用，保留空实现 ====================

    @Override
    public void onBackPressed(boolean shouldSave) {
        // 保留接口兼容性
    }

    @Override
    public void checkBookStatusForExit() {
        if (!isViewActive() || currentBookInfo == null) {
            // 如果信息不足，直接退出
            if (isViewActive()) view.closeView();
            return;
        }

        // 查询书架：原来在主线程直查。后台读完之后按结果决定"弹加入书架"还是"更新进度后关闭"。
        final TabNavBody bookInfo = currentBookInfo;
        repository.queryBookshelfAsync(bookInfo.getBookNo(), new Callback<ArrayList<Book>>() {
            @Override
            public void onSuccess(ArrayList<Book> books) {
                if (!isViewActive()) {
                    return;
                }
                if (books == null || books.isEmpty()) {
                    // 不在书架中，提示添加
                    view.showAddToBookshelfConfirmDialog(bookInfo);
                } else {
                    // 在书架中，更新进度并退出
                    if (currentChapterIndex != -1) {
                        Book bookEntity = books.get(0);
                        bookEntity.setLastReadPosition(currentChapterIndex);
                        bookEntity.setHistoriographerNumb(currentChapterIndex);
                        repository.updateReadingProgressAsync(bookEntity);
                    }
                    view.closeView();
                }
            }

            @Override
            public void onError(Exception e) {
                EasyLog.print("TipsBookReadPresenter", "检查书籍状态失败: " + e.getMessage());
                if (isViewActive()) {
                    view.closeView();
                }
            }
        });
    }

    @Override
    public void addToBookshelfAndExit(TabNavBody navTabBody) {
        if (!isViewActive() || navTabBody == null) {
            if (isViewActive()) view.closeView();
            return;
        }

        try {
            Book book = new Book();
            book.setBookId(repository.generateBookId());
            book.setBookNo(navTabBody.getBookNo());
            book.setBookName(navTabBody.getBookName());
            book.setAuthor(navTabBody.getAuthor());
            book.setHistoriographerNumb(currentChapterIndex == -1 ? 0 : currentChapterIndex);
            book.setLastReadPosition(currentChapterIndex == -1 ? 0 : currentChapterIndex);

            // 加入书架的写入交给串行后台线程，界面不用等它完成
            repository.addToBookshelfAsync(book);
            
            // 通知刷新书架 (通过 EventBus 或回调，这里假设 View 关闭后 Activity 会刷新，或者需要显式刷新)
            // Fragment 原逻辑调用了 BookCollectCaseFragment.newInstance().RefreshLayout();
            // 这是一种不太好的耦合。Presenter 不应该知道 BookCollectCaseFragment。
            // 可以在 View.closeView() 中处理，或者发送 EventBus。
            // 原逻辑: BookCollectCaseFragment.newInstance().RefreshLayout();
            // 这里我们保持原样，在 View 实现中处理刷新，或者 Presenter 发送事件。
            // 简单起见，Presenter 只负责数据操作，View 负责界面跳转和刷新。
            
            view.closeView();
            
        } catch (Exception e) {
            EasyLog.print("TipsBookReadPresenter", "添加书架失败: " + e.getMessage());
            view.closeView();
        }
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
        // 改走后台线程读库：原来这里在主线程直接调 DataRepository.getFangDetailList 同步查库，
        // 会触发 StrictMode DiskReadViolation（与 getBookData / getChapters 同一类问题，见 logcat 栈
        // loadBookFang:816 → getFangDetailList:843 → BaseService.find:210 → SQLite 主线程读）。
        // 读库发生在 DB 串行后台线程，结果回主线程后继续下面"已加载/网络下载"的分支。
        final TabNavBody finalBook = book;
        DbService.getInstance().runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                final ArrayList<run.yigou.gxzy.data.model.Fang> cachedFangList =
                    DataRepository.getFangDetailList(currentBookId);
                ThreadUtil.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 读库期间阅读页可能已退出，撤销预标记避免之后无法重试
                        if (!isViewActive()) {
                            loadedBookFangs.remove(currentBookId);
                            return;
                        }
                        if (cachedFangList != null && !cachedFangList.isEmpty()) {
                            // 数据库已有方剂数据，直接加载到BookData
                            EasyLog.print("TipsBookReadPresenter", "从数据库加载方剂: " + cachedFangList.size() + " 个");

                            List<DataItem> fangItemList = new ArrayList<>(cachedFangList);
                            ChapterData fangChapterData = new ChapterData("", finalBook.getBookName() + "方", 0, fangItemList);

                            if (currentBookData != null) {
                                currentBookData.setFangData(fangChapterData);
                                EasyLog.print("TipsBookReadPresenter", "✅ 方剂数据已从数据库加载到BookData: " + cachedFangList.size() + " 个");
                            }
                            return;
                        }

                        // 数据库无数据，从网络下载
                        EasyLog.print("TipsBookReadPresenter", "数据库无方剂数据，开始从网络下载");
                        androidx.lifecycle.LifecycleOwner lifecycleOwner = (androidx.lifecycle.LifecycleOwner) view;
                        repository.downloadBookFang(currentBookId, lifecycleOwner, new Callback<List<run.yigou.gxzy.data.model.Fang>>() {
                            @Override
                            public void onSuccess(List<run.yigou.gxzy.data.model.Fang> data) {
                                EasyLog.print("TipsBookReadPresenter", "药方数据网络下载完成: " + data.size() + " 个");

                                // 【新架构】将方剂数据设置到BookData
                                if (data != null && !data.isEmpty() && currentBookData != null) {
                                    List<DataItem> fangItemList = new ArrayList<>(data);
                                    ChapterData fangChapterData = new ChapterData("", finalBook.getBookName() + "方", 0, fangItemList);

                                    currentBookData.setFangData(fangChapterData);
                                    EasyLog.print("TipsBookReadPresenter", "✅ 方剂数据已设置到BookData: " + data.size() + " 个");
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                EasyLog.print("TipsBookReadPresenter", "药方数据加载失败: " + e.getMessage());
                                // 失败时移除标记，允许重试
                                loadedBookFangs.remove(currentBookId);
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * 获取当前章节内容列表（转换为 HH2SectionData）
     * 用于 Fragment UI 展示
     */
    public List<HH2SectionData> getChapterContentList() {
        if (currentBookData == null || allChapters == null) {
            return new ArrayList<>();
        }
        
        // 转换为 HH2SectionData
        ArrayList<HH2SectionData> contentList = convertChaptersToSectionData(allChapters);

        return contentList;
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
            new Callback<ChapterData>() {
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
                public void onError(Exception e) {
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
    private Chapter findChapterBySignatureId(String signatureId) {
        // 优先使用 O(1) 查找
        if (currentBookData != null) {
            ChapterData chapterData = currentBookData.findChapterBySignature(signatureId);
            if (chapterData != null) {
                // 从 allChapters 中找到对应的 Chapter 实体
                for (Chapter chapter : allChapters) {
                    if (chapter != null && signatureId.equals(chapter.getSignatureId())) {
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
