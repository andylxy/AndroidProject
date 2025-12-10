/*
 * 项目名: AndroidProject
 * 类名: BinderFactory.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.binder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: Binder工厂 - 创建和管理各种Binder
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.binder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import run.yigou.gxzy.ui.tips.adapter.refactor.image.ImageLoader;
import run.yigou.gxzy.ui.tips.adapter.refactor.state.ExpandStateManager;
import run.yigou.gxzy.ui.tips.adapter.refactor.utils.SpannableStringCache;

/**
 * Binder工厂
 * 统一创建和管理各种类型的Binder
 */
public class BinderFactory {

    private final ExpandStateManager expandStateManager;
    private final SpannableStringCache spannableStringCache;
    private final ImageLoader imageLoader;

    /**
     * 构造函数
     *
     * @param expandStateManager   展开状态管理器
     * @param spannableStringCache SpannableString缓存
     * @param imageLoader          图片加载器
     */
    public BinderFactory(@NonNull ExpandStateManager expandStateManager,
                         @NonNull SpannableStringCache spannableStringCache,
                         @Nullable ImageLoader imageLoader) {
        this.expandStateManager = expandStateManager;
        this.spannableStringCache = spannableStringCache;
        this.imageLoader = imageLoader;
    }

    /**
     * 创建Header Binder
     *
     * @return HeaderBinder
     */
    @NonNull
    public HeaderBinder createHeaderBinder() {
        return new HeaderBinder(expandStateManager);
    }

    /**
     * 创建Child Text Binder
     *
     * @return ChildTextBinder
     */
    @NonNull
    public ChildTextBinder createChildTextBinder() {
        return new ChildTextBinder(spannableStringCache, imageLoader);
    }

    /**
     * 获取ExpandStateManager
     *
     * @return ExpandStateManager
     */
    @NonNull
    public ExpandStateManager getExpandStateManager() {
        return expandStateManager;
    }

    /**
     * 获取SpannableStringCache
     *
     * @return SpannableStringCache
     */
    @NonNull
    public SpannableStringCache getSpannableStringCache() {
        return spannableStringCache;
    }

    /**
     * 获取ImageLoader
     *
     * @return ImageLoader
     */
    @Nullable
    public ImageLoader getImageLoader() {
        return imageLoader;
    }
}
