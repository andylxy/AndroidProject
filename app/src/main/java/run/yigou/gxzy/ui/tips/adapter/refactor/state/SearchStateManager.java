/*
 * 项目名: AndroidProject
 * 类名: SearchStateManager.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.state
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 搜索状态管理器 - 管理搜索模式状态
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.state;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索状态管理器
 * 管理搜索模式的开启/关闭和搜索关键词
 */
public class SearchStateManager {

    // 是否为搜索模式
    private boolean isSearchMode = false;

    // 搜索关键词
    private String searchKeyword = "";

    // 状态观察者列表
    private final List<StateObserver> observers = new ArrayList<>();

    /**
     * 判断是否为搜索模式
     *
     * @return true表示搜索模式
     */
    public boolean isSearchMode() {
        return isSearchMode;
    }

    /**
     * 进入搜索模式
     */
    public void enterSearchMode() {
        enterSearchMode("");
    }

    /**
     * 进入搜索模式
     *
     * @param keyword 搜索关键词
     */
    public void enterSearchMode(@Nullable String keyword) {
        if (!isSearchMode) {
            isSearchMode = true;
            searchKeyword = keyword != null ? keyword : "";
            notifySearchModeChanged(true);
        } else if (keyword != null && !keyword.equals(searchKeyword)) {
            // 如果已经在搜索模式,但关键词变化了,更新关键词
            searchKeyword = keyword;
        }
    }

    /**
     * 退出搜索模式
     */
    public void exitSearchMode() {
        if (isSearchMode) {
            isSearchMode = false;
            searchKeyword = "";
            notifySearchModeChanged(false);
        }
    }

    /**
     * 切换搜索模式
     *
     * @return 切换后的模式(true表示搜索模式)
     */
    public boolean toggleSearchMode() {
        if (isSearchMode) {
            exitSearchMode();
            return false;
        } else {
            enterSearchMode();
            return true;
        }
    }

    /**
     * 获取搜索关键词
     *
     * @return 搜索关键词
     */
    @NonNull
    public String getSearchKeyword() {
        return searchKeyword;
    }

    /**
     * 设置搜索关键词
     *
     * @param keyword 搜索关键词
     */
    public void setSearchKeyword(@Nullable String keyword) {
        this.searchKeyword = keyword != null ? keyword : "";
    }

    /**
     * 判断是否有搜索关键词
     *
     * @return true表示有关键词
     */
    public boolean hasSearchKeyword() {
        return searchKeyword != null && !searchKeyword.isEmpty();
    }

    /**
     * 清空搜索关键词
     */
    public void clearSearchKeyword() {
        searchKeyword = "";
    }

    /**
     * 重置状态(退出搜索模式并清空关键词)
     */
    public void reset() {
        exitSearchMode();
        clearSearchKeyword();
    }

    /**
     * 添加状态观察者
     *
     * @param observer 观察者
     */
    public void addObserver(@NonNull StateObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * 移除状态观察者
     *
     * @param observer 观察者
     */
    public void removeObserver(@NonNull StateObserver observer) {
        observers.remove(observer);
    }

    /**
     * 通知搜索模式变化
     *
     * @param isSearchMode 是否为搜索模式
     */
    private void notifySearchModeChanged(boolean isSearchMode) {
        for (StateObserver observer : observers) {
            observer.onSearchModeChanged(isSearchMode);
        }
    }
}
