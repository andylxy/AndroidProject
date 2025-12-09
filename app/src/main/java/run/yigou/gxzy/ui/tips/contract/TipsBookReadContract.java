/*
 * 项目名: AndroidProject
 * 类名: TipsBookReadContract.java
 * 包名: run.yigou.gxzy.ui.tips.contract
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.contract;

import android.content.Context;

import java.util.List;

import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;

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
         * 显示搜索结果
         * @param results 搜索结果
         * @param totalCount 结果总数
         */
        void showSearchResults(List<ExpandableGroupEntity> results, int totalCount);
        
        /**
         * 显示加载状态
         * @param isLoading true-显示加载, false-隐藏加载
         */
        void showLoading(boolean isLoading);
        
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
        void loadBookContent(int bookId, int lastReadPosition, boolean isShowBookCollect);
        
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
         * 清除搜索
         */
        void clearSearch();
        
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
    }
}
