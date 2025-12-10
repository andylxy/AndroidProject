/*
 * 项目名: AndroidProject
 * 类名: ImageSpanBuilder.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.image
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: ImageSpan构建器 - 创建和插入ImageSpan到SpannableString
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.image;

import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ImageSpan构建器
 * 封装ImageSpan的创建和插入逻辑
 */
public class ImageSpanBuilder {

    /**
     * 创建ImageSpan
     *
     * @param drawable 图片Drawable
     * @param width    宽度
     * @param height   高度
     * @return ImageSpan对象
     */
    @NonNull
    public static ImageSpan createImageSpan(@NonNull Drawable drawable,
                                             int width,
                                             int height) {
        // 设置图片边界
        drawable.setBounds(0, 0, width, height);
        // 创建ImageSpan(基线对齐)
        return new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
    }

    /**
     * 创建ImageSpan(使用原始尺寸)
     *
     * @param drawable 图片Drawable
     * @return ImageSpan对象
     */
    @NonNull
    public static ImageSpan createImageSpan(@NonNull Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        return createImageSpan(drawable, width, height);
    }

    /**
     * 在SpannableString指定位置插入ImageSpan
     *
     * @param spannableString SpannableString对象
     * @param imageSpan       ImageSpan对象
     * @param start           起始位置
     * @param end             结束位置
     */
    public static void insertImageSpan(@NonNull SpannableStringBuilder spannableString,
                                        @NonNull ImageSpan imageSpan,
                                        int start,
                                        int end) {
        spannableString.setSpan(imageSpan, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * 在SpannableString开头插入图片
     *
     * @param spannableString SpannableString对象
     * @param drawable        图片Drawable
     * @param width           图片宽度
     * @param height          图片高度
     * @param placeholder     占位符文本(会被替换为图片)
     */
    public static void prependImage(@NonNull SpannableStringBuilder spannableString,
                                     @NonNull Drawable drawable,
                                     int width,
                                     int height,
                                     @NonNull String placeholder) {
        // 创建ImageSpan
        ImageSpan imageSpan = createImageSpan(drawable, width, height);

        // 在开头插入占位符
        spannableString.insert(0, placeholder);

        // 将占位符替换为ImageSpan
        insertImageSpan(spannableString, imageSpan, 0, placeholder.length());
    }

    /**
     * 在SpannableString开头插入图片(默认占位符 "图")
     *
     * @param spannableString SpannableString对象
     * @param drawable        图片Drawable
     * @param width           图片宽度
     * @param height          图片高度
     */
    public static void prependImage(@NonNull SpannableStringBuilder spannableString,
                                     @NonNull Drawable drawable,
                                     int width,
                                     int height) {
        prependImage(spannableString, drawable, width, height, "图");
    }

    /**
     * 在SpannableString末尾追加图片
     *
     * @param spannableString SpannableString对象
     * @param drawable        图片Drawable
     * @param width           图片宽度
     * @param height          图片高度
     * @param placeholder     占位符文本
     */
    public static void appendImage(@NonNull SpannableStringBuilder spannableString,
                                    @NonNull Drawable drawable,
                                    int width,
                                    int height,
                                    @NonNull String placeholder) {
        int start = spannableString.length();
        spannableString.append(placeholder);
        int end = spannableString.length();

        ImageSpan imageSpan = createImageSpan(drawable, width, height);
        insertImageSpan(spannableString, imageSpan, start, end);
    }

    /**
     * 在SpannableString末尾追加图片(默认占位符 "图")
     *
     * @param spannableString SpannableString对象
     * @param drawable        图片Drawable
     * @param width           图片宽度
     * @param height          图片高度
     */
    public static void appendImage(@NonNull SpannableStringBuilder spannableString,
                                    @NonNull Drawable drawable,
                                    int width,
                                    int height) {
        appendImage(spannableString, drawable, width, height, "图");
    }

    /**
     * 替换SpannableString中的文本为图片
     *
     * @param spannableString SpannableString对象
     * @param drawable        图片Drawable
     * @param width           图片宽度
     * @param height          图片高度
     * @param start           起始位置
     * @param end             结束位置
     */
    public static void replaceWithImage(@NonNull SpannableStringBuilder spannableString,
                                         @NonNull Drawable drawable,
                                         int width,
                                         int height,
                                         int start,
                                         int end) {
        ImageSpan imageSpan = createImageSpan(drawable, width, height);
        insertImageSpan(spannableString, imageSpan, start, end);
    }

    /**
     * 移除SpannableString中的所有ImageSpan
     *
     * @param spannableString SpannableString对象
     */
    public static void removeAllImageSpans(@NonNull SpannableStringBuilder spannableString) {
        ImageSpan[] imageSpans = spannableString.getSpans(0, spannableString.length(), ImageSpan.class);
        for (ImageSpan span : imageSpans) {
            spannableString.removeSpan(span);
        }
    }

    /**
     * 获取SpannableString中的所有ImageSpan
     *
     * @param spannableString SpannableString对象
     * @return ImageSpan数组
     */
    @Nullable
    public static ImageSpan[] getAllImageSpans(@NonNull SpannableStringBuilder spannableString) {
        return spannableString.getSpans(0, spannableString.length(), ImageSpan.class);
    }

    /**
     * 判断SpannableString是否包含ImageSpan
     *
     * @param spannableString SpannableString对象
     * @return true表示包含
     */
    public static boolean hasImageSpan(@NonNull SpannableStringBuilder spannableString) {
        ImageSpan[] imageSpans = getAllImageSpans(spannableString);
        return imageSpans != null && imageSpans.length > 0;
    }
}
