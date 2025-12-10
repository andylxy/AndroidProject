/*
 * 项目名: AndroidProject
 * 类名: BaseRefactoredAdapter.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 重构基类Adapter - 提供公共功能
 */

package run.yigou.gxzy.ui.tips.adapter.refactor;

import android.content.Context;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;

import java.util.ArrayList;

import run.yigou.gxzy.ui.tips.adapter.refactor.binder.BinderFactory;
import run.yigou.gxzy.ui.tips.adapter.refactor.image.GlideImageLoader;
import run.yigou.gxzy.ui.tips.adapter.refactor.image.ImageLoader;
import run.yigou.gxzy.ui.tips.adapter.refactor.state.ExpandStateManager;
import run.yigou.gxzy.ui.tips.adapter.refactor.state.SearchStateManager;
import run.yigou.gxzy.ui.tips.adapter.refactor.utils.SpannableStringCache;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * 重构基类Adapter
 * 提供所有重构Adapter的公共功能
 */
public abstract class BaseRefactoredAdapter extends GroupedRecyclerViewAdapter {

    protected final Context context;
    protected ArrayList<ExpandableGroupEntity> groups;

    // 状态管理器
    protected final ExpandStateManager expandStateManager;
    protected final SearchStateManager searchStateManager;

    // 工具类
    protected final SpannableStringCache spannableStringCache;
    protected final ImageLoader imageLoader;

    // Binder工厂
    protected final BinderFactory binderFactory;

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public BaseRefactoredAdapter(@NonNull Context context) {
        super(context);
        this.context = context;

        // 初始化状态管理器
        this.expandStateManager = new ExpandStateManager();
        this.searchStateManager = new SearchStateManager();

        // 初始化工具类
        this.spannableStringCache = new SpannableStringCache();
        this.imageLoader = new GlideImageLoader();

        // 初始化Binder工厂
        this.binderFactory = new BinderFactory(
                expandStateManager,
                spannableStringCache,
                imageLoader
        );
    }

    /**
     * 设置数据
     *
     * @param groups 数据列表
     */
    public void setGroups(@NonNull ArrayList<ExpandableGroupEntity> groups) {
        this.groups = groups;
        // 同步展开状态
        expandStateManager.syncFromData(groups);
        notifyDataSetChanged();
    }

    /**
     * 获取数据
     *
     * @return 数据列表
     */
    public ArrayList<ExpandableGroupEntity> getGroups() {
        return groups;
    }

    @Override
    public int getGroupCount() {
        return groups == null ? 0 : groups.size();
    }

    /**
     * 获取展开状态管理器
     *
     * @return ExpandStateManager
     */
    public ExpandStateManager getExpandStateManager() {
        return expandStateManager;
    }

    /**
     * 获取搜索状态管理器
     *
     * @return SearchStateManager
     */
    public SearchStateManager getSearchStateManager() {
        return searchStateManager;
    }

    /**
     * 获取SpannableString缓存
     *
     * @return SpannableStringCache
     */
    public SpannableStringCache getSpannableStringCache() {
        return spannableStringCache;
    }

    /**
     * 获取图片加载器
     *
     * @return ImageLoader
     */
    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        spannableStringCache.clear();
        imageLoader.clearCache();
    }
}
