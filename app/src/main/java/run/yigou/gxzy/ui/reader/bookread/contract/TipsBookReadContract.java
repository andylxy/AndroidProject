/*
 * 项目名: AndroidProject
 * 类名: TipsBookReadContract.java
 * 包名: run.yigou.gxzy.ui.reader.bookread.contract
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.bookread.contract;

import android.content.Context;

import java.util.List;

import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.ui.reader.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;
import run.yigou.gxzy.data.model.HH2SectionData;

/**
 * MVP 契约接口
 * 定义 View 和 Presenter 的职责边界
 */
public interface TipsBookReadContract {

    /**
     * View 接口
     * Fragment 实现此接口，负责 UI 显示和用户交互
     */
    interface View {
        
        // ==================== 显示相关 ====================
        
        /**
         * 显示章节列表
         * @param chapters 章节数据
         */
        void showChapterList(List<ExpandableGroupEntity> chapters);
        
        /**
         * 显示搜索结果（T3/D3：搜索结果走新结构 GroupData/ItemData，与适配器绑定用的
         * groupDataList 同构，不再经ExpandableGroupEntity 旧结构中转）
         *
         * @param groups     搜索命中的分组（章节）列表
         * @param items      与 groups 一一对应的子项列表
         * @param totalCount 匹配总数
         */
        void showSearchResults(List<GroupData> groups, List<List<ItemData>> items, int totalCount);
        
        /**
         * 显示书籍加载状态。
         *
         * <p>注意与 {@link #showSearching(boolean)} 区分：本书页布局没有 loading 控件，
         * 本方法一直是空实现（书籍加载进度由各环节自己的提示承担）；搜索的进行中提示
         * 走 showSearching，避免打开任意书籍时在结果提示位闪「搜索中…」。
         *
         * @param isLoading true-显示加载, false-隐藏加载
         */
        void showLoading(boolean isLoading);

        /**
         * 显示/结束「搜索中」提示（D8/§4.8）
         *
         * <p>与 {@link #showLoading(boolean)} 分开的原因：两者语义不同 —— 本方法只服务书内搜索，
         * 而 showLoading 被书籍加载链路（loadBookContent / onChaptersLoaded）复用。
         * 若共用一个方法，打开任意书籍都会在结果提示位显示「搜索中…」。
         *
         * @param searching true-显示搜索中, false-结束并清空提示
         */
        void showSearching(boolean searching);
        
        /**
         * 显示错误信息
         * @param message 错误消息
         */
        void showError(String message);
        
        /**
         * 显示提示消息
         * @param message 提示消息
         */
        void showToast(String message);
        
        // ==================== 下载相关 ====================
        
        /**
         * 更新章节内容
         * @param position 章节位置
         * @param sectionData 章节数据
         */
        void updateChapterContent(int position, HH2SectionData sectionData);
        
        /**
         * 显示下载进度
         * @param position 章节位置
         * @param message 进度消息
         */
        void showDownloadProgress(int position, String message);
        
        /**
         * 更新下载状态
         * @param position 章节位置
         * @param isDownloaded 是否已下载
         */
        void updateDownloadStatus(int position, boolean isDownloaded);
        
        // ==================== 导航相关 ====================
        
        /**
         * 滚动到指定位置
         * @param position 目标位置
         */
        void scrollToPosition(int position);
        
        /**
         * 展开章节
         * @param position 章节位置
         */
        void expandChapter(int position);
        
        /**
         * 收起章节
         * @param position 章节位置
         */
        void collapseChapter(int position);
        
        // ==================== 生命周期查询 ====================
        
        /**
         * 检查 View 是否处于活动状态
         * @return true-活动, false-非活动
         */
        boolean isActive();
        
        /**
         * 获取上下文
         * @return Context 对象
         */
        Context getContext();

        // ==================== 退出逻辑 ====================

        /**
         * 显示加入书架确认框
         * @param book 书籍信息
         */
        void showAddToBookshelfConfirmDialog(TabNavBody book);

        /**
         * 关闭当前页面
         */
        void closeView();
    }

    /**
     * Presenter 接口
     * 负责业务逻辑处理
     */
    interface Presenter {
        
        // ==================== 生命周期 ====================
        
        /**
         * View 创建完成回调
         */
        void onViewCreated();
        
        /**
         * View 销毁回调
         */
        void onViewDestroy();
        
        /**
         * View 恢复回调
         */
        void onViewResume();
        
        /**
         * View 暂停回调
         */
        void onViewPause();
        
        // ==================== 数据加载 ====================
        
        /**
         * 加载书籍内容
         * @param bookId 书籍 ID
         * @param lastReadPosition 上次阅读位置
         * @param isShowBookCollect 是否显示书架
         */
        void loadBookContent(String bookId, int lastReadPosition, boolean isShowBookCollect);
        
        /**
         * 刷新数据
         */
        void refreshData();
        
        // ==================== 下载相关 ====================
        
        /**
         * 章节点击事件
         * 触发下载和预加载
         * @param position 章节位置
         */
        void onChapterClick(int position);
        
        /**
         * 重新下载章节
         * @param position 章节位置
         */
        void reloadChapter(int position);
        
        // ==================== 搜索 ====================
        
        /**
         * 执行搜索
         * @param keyword 搜索关键字
         */
        void search(String keyword);

        /**
         * 作废当前在途的搜索（D8）
         *
         * 用户清空搜索框时必须调用：列表此时已恢复成全量章节，若在途结果仍回填
         * 会覆盖全量列表，且清空后搜索态守卫失效，会重新打开 D2/D2.1 的污染路径。
         */
        void cancelSearch();

        // ==================== 用户交互 ====================
        
        /**
         * 返回键处理
         * @param shouldSave 是否保存阅读进度
         */
        void onBackPressed(boolean shouldSave);
        
        /**
         * 设置变更回调
         */
        void onSettingChanged();
        
        /**
         * 跳转到指定章节
         * @param groupPosition 组位置
         * @param childPosition 子位置
         */
        void onJumpToPosition(int groupPosition, int childPosition);

        // ==================== 退出逻辑 ====================

        /**
         * 检查书籍状态并处理退出逻辑
         * (替代原 Fragment 中的 fragmentOnBackPressed)
         */
        void checkBookStatusForExit();

        /**
         * 将书籍加入书架并退出
         * @param navTabBody 书籍信息
         */
        void addToBookshelfAndExit(TabNavBody navTabBody);
    }
}
