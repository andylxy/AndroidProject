/*
 * 项目名: AndroidProject
 * 类名: ImageSizeCalculator.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.image
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 图片尺寸计算器 - 计算图片在TextView中的显示尺寸
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.image;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * 图片尺寸计算器
 * 根据TextView宽度和图片原始尺寸计算最终显示尺寸
 */
public class ImageSizeCalculator {

    /**
     * 计算结果类
     */
    public static class SizeResult {
        public final int width;
        public final int height;

        public SizeResult(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * 根据TextView宽度计算图片显示尺寸
     * 保持图片宽高比,宽度不超过TextView宽度
     *
     * @param textView       目标TextView
     * @param drawable       图片Drawable
     * @param maxWidthRatio  最大宽度比例(相对于TextView宽度,0.0-1.0)
     * @return 计算后的尺寸
     */
    @NonNull
    public static SizeResult calculate(@NonNull TextView textView,
                                        @NonNull Drawable drawable,
                                        float maxWidthRatio) {
        // 获取TextView宽度
        int textViewWidth = textView.getWidth();
        int textViewPaddingLeft = textView.getPaddingLeft();
        int textViewPaddingRight = textView.getPaddingRight();
        int availableWidth = textViewWidth - textViewPaddingLeft - textViewPaddingRight;

        // 获取图片原始尺寸
        int originalWidth = drawable.getIntrinsicWidth();
        int originalHeight = drawable.getIntrinsicHeight();

        // 如果无法获取尺寸,返回原始尺寸
        if (availableWidth <= 0 || originalWidth <= 0 || originalHeight <= 0) {
            return new SizeResult(originalWidth, originalHeight);
        }

        // 计算最大宽度
        int maxWidth = (int) (availableWidth * maxWidthRatio);

        // 如果图片宽度小于最大宽度,保持原始尺寸
        if (originalWidth <= maxWidth) {
            return new SizeResult(originalWidth, originalHeight);
        }

        // 按比例缩放
        float scale = (float) maxWidth / originalWidth;
        int newWidth = maxWidth;
        int newHeight = (int) (originalHeight * scale);

        return new SizeResult(newWidth, newHeight);
    }

    /**
     * 根据TextView宽度计算图片显示尺寸(默认最大宽度比例0.9)
     *
     * @param textView TextView
     * @param drawable 图片Drawable
     * @return 计算后的尺寸
     */
    @NonNull
    public static SizeResult calculate(@NonNull TextView textView,
                                        @NonNull Drawable drawable) {
        return calculate(textView, drawable, 0.9f);
    }

    /**
     * 根据最大宽度计算图片显示尺寸
     *
     * @param drawable 图片Drawable
     * @param maxWidth 最大宽度(像素)
     * @return 计算后的尺寸
     */
    @NonNull
    public static SizeResult calculateByMaxWidth(@NonNull Drawable drawable, int maxWidth) {
        int originalWidth = drawable.getIntrinsicWidth();
        int originalHeight = drawable.getIntrinsicHeight();

        if (originalWidth <= 0 || originalHeight <= 0 || maxWidth <= 0) {
            return new SizeResult(originalWidth, originalHeight);
        }

        if (originalWidth <= maxWidth) {
            return new SizeResult(originalWidth, originalHeight);
        }

        float scale = (float) maxWidth / originalWidth;
        int newWidth = maxWidth;
        int newHeight = (int) (originalHeight * scale);

        return new SizeResult(newWidth, newHeight);
    }

    /**
     * 根据最大高度计算图片显示尺寸
     *
     * @param drawable  图片Drawable
     * @param maxHeight 最大高度(像素)
     * @return 计算后的尺寸
     */
    @NonNull
    public static SizeResult calculateByMaxHeight(@NonNull Drawable drawable, int maxHeight) {
        int originalWidth = drawable.getIntrinsicWidth();
        int originalHeight = drawable.getIntrinsicHeight();

        if (originalWidth <= 0 || originalHeight <= 0 || maxHeight <= 0) {
            return new SizeResult(originalWidth, originalHeight);
        }

        if (originalHeight <= maxHeight) {
            return new SizeResult(originalWidth, originalHeight);
        }

        float scale = (float) maxHeight / originalHeight;
        int newWidth = (int) (originalWidth * scale);
        int newHeight = maxHeight;

        return new SizeResult(newWidth, newHeight);
    }

    /**
     * 根据最大宽高计算图片显示尺寸(保持宽高比,不超过最大宽高)
     *
     * @param drawable  图片Drawable
     * @param maxWidth  最大宽度(像素)
     * @param maxHeight 最大高度(像素)
     * @return 计算后的尺寸
     */
    @NonNull
    public static SizeResult calculateByMaxSize(@NonNull Drawable drawable,
                                                 int maxWidth,
                                                 int maxHeight) {
        int originalWidth = drawable.getIntrinsicWidth();
        int originalHeight = drawable.getIntrinsicHeight();

        if (originalWidth <= 0 || originalHeight <= 0) {
            return new SizeResult(originalWidth, originalHeight);
        }

        // 如果图片尺寸小于最大尺寸,保持原始尺寸
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return new SizeResult(originalWidth, originalHeight);
        }

        // 计算宽度和高度的缩放比例
        float widthScale = (float) maxWidth / originalWidth;
        float heightScale = (float) maxHeight / originalHeight;

        // 选择较小的缩放比例,确保不超过最大尺寸
        float scale = Math.min(widthScale, heightScale);

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        return new SizeResult(newWidth, newHeight);
    }
}
