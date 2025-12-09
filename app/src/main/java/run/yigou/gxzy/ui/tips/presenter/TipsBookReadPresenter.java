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

import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.ui.tips.contract.TipsBookReadContract;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.repository.BookRepository;
import run.yigou.gxzy.ui.tips.tipsutils.ChapterDownloadManager;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;

/**
 * TipsBookRead Presenter 实现
 * 负责业务逻辑处理
 */
public class TipsBookReadPresenter implements TipsBookReadContract.Presenter {

    private TipsBookReadContract.View view;
    private final BookRepository repository;
    private final ChapterDownloadManager downloadManager;

    // 状态管理
    private int currentBookId;
    private int currentChapterIndex = -1;
    private boolean isShowBookCollect = false;
    private boolean isSearchMode = false;

    // 数据管理
    private List<Chapter> allChapters;
    private SingletonNetData singletonNetData;

    public TipsBookReadPresenter(TipsBookReadContract.View view) {
        this.view = view;
        this.repository = new BookRepository();
        this.downloadManager = new ChapterDownloadManager();
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
        singletonNetData = null;
    }

    @Override
    public void onViewResume() {
        // 可选实现
    }

    @Override
    public void onViewPause() {
        // 可选实现
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
            // 获取书籍信息
            TabNavBody book = repository.getBookInfo(bookId);
            if (book == null) {
                view.showLoading(false);
                view.showError("书籍信息不存在");
                return;
            }

            // 获取章节列表
            allChapters = repository.getChapters(bookId);
            if (allChapters == null || allChapters.isEmpty()) {
                view.showLoading(false);
                view.showError("章节列表为空");
                return;
            }

            // 初始化下载管理器缓存
            downloadManager.initDownloadedCache(allChapters);

            // 获取单例数据
            singletonNetData = TipsSingleData.getInstance().getMapBookContent(bookId);

            // 兼容处理宋版伤寒
            if (bookId == AppConst.ShangHanNo) {
                setupShanghanContentListener();
            }

            // 加载药方数据
            if (singletonNetData != null && !singletonNetData.getBookFang(bookId)) {
                loadBookFang(book);
            }

            // 显示章节列表
            displayChapterList();

            // 滚动到上次阅读位置
            if (lastReadPosition > 0 && lastReadPosition < allChapters.size()) {
                view.scrollToPosition(lastReadPosition);
                view.expandChapter(lastReadPosition);
            }

            view.showLoading(false);

        } catch (Exception e) {
            view.showLoading(false);
            view.showError("加载失败: " + e.getMessage());
            EasyLog.print("TipsBookReadPresenter", "加载书籍内容失败: " + e.getMessage());
        }
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
        if (!isViewActive() || allChapters == null || position >= allChapters.size()) {
            return;
        }

        try {
            // 记录当前位置
            if (isShowBookCollect) {
                currentChapterIndex = position;
            }

            // 获取章节数据
            ArrayList<HH2SectionData> contentList = singletonNetData.getContent();
            if (contentList == null || position >= contentList.size()) {
                return;
            }

            HH2SectionData section = contentList.get(position);
            Chapter chapter = findChapterBySignatureId(section.getSignatureId());

            if (chapter == null) {
                view.showError("未找到章节");
                return;
            }

            // 检查是否已下载
            if (downloadManager.isChapterDownloaded(chapter)) {
                // 已下载，直接触发预加载
                downloadManager.preloadChapters(allChapters, position);
                return;
            }

            // 检查是否正在下载
            if (downloadManager.isChapterDownloading(chapter)) {
                view.showToast("章节正在下载中...");
                return;
            }

            // 高优先级下载
            view.showDownloadProgress(position, "正在下载...");
            downloadManager.downloadChapter(chapter, new ChapterDownloadManager.DownloadCallback() {
                @Override
                public void onSuccess(Chapter chapter, HH2SectionData sectionData) {
                    if (isViewActive()) {
                        view.updateChapterContent(position, sectionData);
                        view.updateDownloadStatus(position, true);
                        view.showToast("章节下载完成");
                        
                        // 触发预加载
                        downloadManager.preloadChapters(allChapters, position);
                    }
                }

                @Override
                public void onFailure(Chapter chapter, Exception e) {
                    if (isViewActive()) {
                        view.showError("下载失败: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            view.showError("处理点击异常: " + e.getMessage());
            EasyLog.print("TipsBookReadPresenter", "章节点击异常: " + e.getMessage());
        }
    }

    @Override
    public void reloadChapter(int position) {
        if (!isViewActive()) {
            return;
        }

        try {
            ArrayList<HH2SectionData> contentList = singletonNetData.getContent();
            if (contentList == null || position >= contentList.size()) {
                view.showError("章节索引越界");
                return;
            }

            HH2SectionData section = contentList.get(position);
            Chapter chapter = findChapterBySignatureId(section.getSignatureId());

            if (chapter == null) {
                view.showError("未找到章节信息");
                return;
            }

            view.showToast("开始重新下载: " + chapter.getChapterHeader());

            // 使用 Repository 下载
            repository.downloadChapter(chapter, new BookRepository.DataCallback<HH2SectionData>() {
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

        } catch (Exception e) {
            view.showError("重新下载异常: " + e.getMessage());
        }
    }

    @Override
    public void search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            clearSearch();
            return;
        }

        isSearchMode = true;

        try {
            // 创建搜索实体
            SearchKeyEntity searchKeyEntity = new SearchKeyEntity(new StringBuilder(keyword));

            // 执行搜索
            ArrayList<HH2SectionData> filteredData = TipsNetHelper.getSearchHh2SectionData(
                searchKeyEntity, 
                singletonNetData
            );

            // 更新展示的数据
            singletonNetData.setSearchResList(filteredData);

            // 转换为显示格式
            ArrayList<ExpandableGroupEntity> groups = GroupModel.getExpandableGroups(filteredData, true);

            if (isViewActive()) {
                view.showSearchResults(groups, searchKeyEntity.getSearchResTotalNum());
            }

        } catch (Exception e) {
            view.showError("搜索失败: " + e.getMessage());
            EasyLog.print("TipsBookReadPresenter", "搜索异常: " + e.getMessage());
        }
    }

    @Override
    public void clearSearch() {
        isSearchMode = false;
        displayChapterList();
    }

    @Override
    public void onBackPressed(boolean shouldSave) {
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
    }

    @Override
    public void onSettingChanged() {
        // 设置变更时刷新数据
        refreshData();
    }

    @Override
    public void onJumpToPosition(int groupPosition, int childPosition) {
        if (isViewActive()) {
            view.scrollToPosition(groupPosition);
            view.expandChapter(groupPosition);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 显示章节列表
     */
    private void displayChapterList() {
        if (!isViewActive() || singletonNetData == null) {
            return;
        }

        try {
            ArrayList<HH2SectionData> contentList = singletonNetData.getContent();
            ArrayList<ExpandableGroupEntity> groups = GroupModel.getExpandableGroups(contentList, false);
            view.showChapterList(groups);
        } catch (Exception e) {
            view.showError("显示章节列表失败: " + e.getMessage());
        }
    }

    /**
     * 加载药方数据
     */
    private void loadBookFang(TabNavBody book) {
        String fangName = "\n" + book.getBookName();
        
        repository.downloadBookFang(currentBookId, new BookRepository.DataCallback<List<run.yigou.gxzy.ui.tips.DataBeans.Fang>>() {
            @Override
            public void onSuccess(List<run.yigou.gxzy.ui.tips.DataBeans.Fang> data) {
                if (singletonNetData != null) {
                    HH2SectionData fangData = new HH2SectionData(data, 0, fangName);
                    singletonNetData.setFang(fangData);
                    singletonNetData.setBookFang(currentBookId);
                    EasyLog.print("TipsBookReadPresenter", "药方数据加载完成: " + data.size() + " 个");
                }
            }

            @Override
            public void onFailure(Exception e) {
                EasyLog.print("TipsBookReadPresenter", "药方数据加载失败: " + e.getMessage());
            }
        });
    }

    /**
     * 设置宋版伤寒内容监听器
     */
    private void setupShanghanContentListener() {
        // 兼容处理宋版伤寒的特殊逻辑
        // 根据设置过滤内容
        SingletonNetData.OnContentUpdateListener listener = new SingletonNetData.OnContentUpdateListener() {
            @Override
            public ArrayList<HH2SectionData> contentDateUpdate(ArrayList<HH2SectionData> contentList) {
                if (contentList == null || contentList.isEmpty()) {
                    return new ArrayList<>();
                }

                int size = contentList.size();
                int start = 0;
                int end = size;

                // 根据设置过滤（需要从 AppApplication 获取设置）
                // 这里简化处理，实际应该注入设置对象
                
                if (start < size) {
                    return new ArrayList<>(contentList.subList(start, end));
                } else {
                    return contentList;
                }
            }
        };

        if (singletonNetData != null) {
            singletonNetData.setOnContentUpdateListener(listener);
        }
    }

    /**
     * 根据 signatureId 查找章节
     */
    private Chapter findChapterBySignatureId(long signatureId) {
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
     * 检查 View 是否处于活动状态
     */
    private boolean isViewActive() {
        return view != null && view.isActive();
    }
}
