/*
 * 项目名: AndroidProject
 * 类名: GlideImageLoader.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.image
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: Glide图片加载器实现 - 封装Glide加载图片到TextView的逻辑
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.image;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.HashMap;
import java.util.Map;

/**
 * Glide图片加载器实现
 * 封装Glide加载图片并插入到TextView的完整逻辑
 */
public class GlideImageLoader implements ImageLoader {

    // 保存每个TextView对应的CustomTarget,用于取消加载
    private final Map<TextView, CustomTarget<Drawable>> targetMap = new HashMap<>();

    /**
     * 将图片加载到TextView中(作为ImageSpan)
     *
     * @param url      图片URL
     * @param target   目标TextView
     * @param callback 加载回调
     */
    @Override
    public void loadIntoTextView(@NonNull String url,
                                  @NonNull TextView target,
                                  @Nullable ImageLoadCallback callback) {
        // 取消之前的加载任务
        cancel(target);

        // 获取TextView的当前文本
        CharSequence currentText = target.getText();
        SpannableStringBuilder spannableString;
        if (currentText instanceof SpannableStringBuilder) {
            spannableString = (SpannableStringBuilder) currentText;
        } else {
            spannableString = new SpannableStringBuilder(currentText);
        }

        // 如果SpannableString开头没有占位符,添加占位符
        if (spannableString.length() == 0 || !spannableString.subSequence(0, Math.min(2, spannableString.length())).toString().equals("12")) {
            spannableString.insert(0, "12");
        }

        // 创建CustomTarget
        CustomTarget<Drawable> customTarget = new CustomTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource,
                                         @Nullable Transition<? super Drawable> transition) {
                // 设置图片初始边界
                resource.setBounds(0, 0, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());

                // 添加PreDrawListener在布局完成后调整图片大小
                target.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        // 计算图片显示尺寸
                        ImageSizeCalculator.SizeResult size = ImageSizeCalculator.calculate(target, resource, 0.9f);

                        // 调整图片边界
                        resource.setBounds(0, 0, size.width, size.height);

                        // 创建ImageSpan并插入
                        ImageSpanBuilder.replaceWithImage(spannableString, resource, size.width, size.height, 0, 2);

                        // 添加换行符
                        if (spannableString.length() >= 2 && spannableString.charAt(2) != '\n') {
                            spannableString.insert(2, "\n");
                        }

                        // 设置到TextView
                        target.setText(spannableString);

                        // 强制重新布局
                        target.requestLayout();

                        // 移除监听器
                        target.getViewTreeObserver().removeOnPreDrawListener(this);

                        // 回调成功
                        if (callback != null) {
                            callback.onSuccess(url, size.width, size.height);
                        }

                        return true;
                    }
                });

                // 先设置临时文本
                target.setText(spannableString);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                // 清理时的处理
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                // 加载失败时的处理
                if (callback != null) {
                    callback.onFailure(url, "Image load failed");
                }
            }
        };

        // 保存Target引用
        targetMap.put(target, customTarget);

        // 开始加载
        Glide.with(target.getContext())
                .load(url)
                .into(customTarget);
    }

    /**
     * 取消加载
     *
     * @param target 目标TextView
     */
    @Override
    public void cancel(@NonNull TextView target) {
        CustomTarget<Drawable> customTarget = targetMap.remove(target);
        if (customTarget != null) {
            try {
                Glide.with(target.getContext()).clear(customTarget);
            } catch (Exception e) {
                // 忽略异常
            }
        }
    }

    /**
     * 清理缓存
     */
    @Override
    public void clearCache() {
        targetMap.clear();
    }

    /**
     * 获取当前活动的加载任务数量
     *
     * @return 任务数量
     */
    public int getActiveTaskCount() {
        return targetMap.size();
    }
}
