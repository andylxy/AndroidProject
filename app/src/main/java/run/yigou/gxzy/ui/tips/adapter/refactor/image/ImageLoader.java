/*
 * 项目名: AndroidProject
 * 类名: ImageLoader.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.image
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 图片加载器接口 - 定义图片加载规范
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.image;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 图片加载器接口
 * 定义图片加载到TextView的标准行为
 */
public interface ImageLoader {

    /**
     * 将图片加载到TextView中(作为ImageSpan)
     *
     * @param url      图片URL
     * @param target   目标TextView
     * @param callback 加载回调
     */
    void loadIntoTextView(@NonNull String url,
                          @NonNull TextView target,
                          @Nullable ImageLoadCallback callback);

    /**
     * 取消加载
     *
     * @param target 目标TextView
     */
    void cancel(@NonNull TextView target);

    /**
     * 清理缓存
     */
    void clearCache();

    /**
     * 图片加载回调接口
     */
    interface ImageLoadCallback {
        /**
         * 加载成功
         *
         * @param url        图片URL
         * @param imageWidth 图片宽度
         * @param imageHeight 图片高度
         */
        void onSuccess(@NonNull String url, int imageWidth, int imageHeight);

        /**
         * 加载失败
         *
         * @param url   图片URL
         * @param error 错误信息
         */
        void onFailure(@NonNull String url, @Nullable String error);
    }
}
