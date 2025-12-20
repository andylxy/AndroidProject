/*
 * 项目名: AndroidProject
 * 类名: ChildTextBinder.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.binder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: Child文本绑定器 - 绑定Child文本数据到ViewHolder
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.binder;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.http.EasyLog;

import run.yigou.gxzy.ui.tips.adapter.refactor.image.ImageLoader;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.ItemData;
import run.yigou.gxzy.ui.tips.adapter.refactor.utils.SpannableStringCache;
import run.yigou.gxzy.ui.tips.adapter.refactor.utils.TextViewHelper;
import run.yigou.gxzy.ui.tips.adapter.refactor.viewholder.TipsChildViewHolder;
import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.widget.LocalLinkMovementMethod;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.other.AppConfig;
import run.yigou.gxzy.utils.DebugLog;

/**
 * Child文本绑定器
 * 负责将ChildEntity的文本数据绑定到TipsChildViewHolder
 */
public class ChildTextBinder implements DataBinder<ChildEntity, TipsChildViewHolder> {

    private final SpannableStringCache spannableStringCache;
    private final ImageLoader imageLoader;

    /**
     * 构造函数
     *
     * @param spannableStringCache SpannableString缓存
     * @param imageLoader          图片加载器
     */
    public ChildTextBinder(@NonNull SpannableStringCache spannableStringCache,
                           @Nullable ImageLoader imageLoader) {
        this.spannableStringCache = spannableStringCache;
        this.imageLoader = imageLoader;
    }

    /**
     * 绑定Child文本数据
     *
     * @param data       数据实体
     * @param viewHolder ViewHolder对象
     * @param position   位置(childPosition)
     */
    @Override
    public void bind(@NonNull ChildEntity data,
                     @NonNull TipsChildViewHolder viewHolder,
                     int position) {
        // 绑定基础数据(text/note/video)
        viewHolder.bind(data, TextViewHelper.DisplayMode.TEXT);

        // 加载图片(如果有)
        if (imageLoader != null && data.getChild_section_image() != null) {
            loadImage(data, viewHolder, position);
        }
    }

    /**
     * 加载图片到TextView
     *
     * @param data       数据实体
     * @param viewHolder ViewHolder对象
     * @param position   位置
     */
    private void loadImage(@NonNull ChildEntity data,
                           @NonNull TipsChildViewHolder viewHolder,
                           int position) {
        // 构建图片URL
        String imageUrl = buildImageUrl(data.getChild_section_image());

        // 加载图片
        imageLoader.loadIntoTextView(
                imageUrl,
                viewHolder.getTextView(),
                new ImageLoader.ImageLoadCallback() {
                    @Override
                    public void onSuccess(@NonNull String url, int imageWidth, int imageHeight) {
                        // 图片加载成功
                    }

                    @Override
                    public void onFailure(@NonNull String url, @Nullable String error) {
                        // 图片加载失败
                    }
                }
        );
    }

    /**
     * 构建图片URL
     *
     * @param imagePath 图片路径
     * @return 完整URL
     */
    @NonNull
    private String buildImageUrl(@NonNull String imagePath) {
        String host;
        if (AppConfig.isLogEnable()) {
            host = AppConfig.getHostUrl();
        } else {
            host = AppConst.ImageHost;
        }
        return host + imagePath;
    }

    /**
     * 解绑ViewHolder
     *
     * @param viewHolder ViewHolder对象
     */
    @Override
    public void unbind(@NonNull TipsChildViewHolder viewHolder) {
        // 取消图片加载
        if (imageLoader != null) {
            imageLoader.cancel(viewHolder.getTextView());
        }
    }
    
    /**
     * 绑定Child文本数据 - 使用新数据结构ItemData
     *
     * @param data       数据实体(ItemData)
     * @param viewHolder ViewHolder对象
     * @param position   位置(childPosition)
     */
    public void bind(@NonNull ItemData data,
                     @NonNull TipsChildViewHolder viewHolder,
                     int position) {
        DebugLog.print("=== ChildTextBinder.bind(ItemData) 诊断开始 ===");
        DebugLog.print("position: " + position);
        
        // 设置LocalLinkMovementMethod以支持ClickableSpan点击
        viewHolder.getTextView().setMovementMethod(LocalLinkMovementMethod.getInstance());
        viewHolder.getNoteView().setMovementMethod(LocalLinkMovementMethod.getInstance());
        viewHolder.getVideoView().setMovementMethod(LocalLinkMovementMethod.getInstance());
        
        DebugLog.print("✅ MovementMethod已设置");
        
        // 设置初始可见性: text可见, note和video默认隐藏
        viewHolder.getTextView().setVisibility(View.VISIBLE);
        viewHolder.getNoteView().setVisibility(View.GONE);
        viewHolder.getVideoView().setVisibility(View.GONE);
        
        // 诊断: 检查textSpan状态
        DebugLog.print("hasTextSpan: " + data.hasTextSpan());
        
        // 绑定正文
        if (data.hasTextSpan()) {
            SpannableStringBuilder textSpan = data.getTextSpan();
            DebugLog.print("✅ textSpan不为空, length: " + textSpan.length());
            
            // 检查ClickableSpan
            ClickableSpan[] spans = textSpan.getSpans(0, textSpan.length(), ClickableSpan.class);
            DebugLog.print("ClickableSpan count: " + spans.length);
            
            if (spans.length > 0) {
                DebugLog.print("✅ ClickableSpan存在！");
                for (int i = 0; i < Math.min(spans.length, 5); i++) {
                    int start = textSpan.getSpanStart(spans[i]);
                    int end = textSpan.getSpanEnd(spans[i]);
                    String clickText = textSpan.subSequence(start, end).toString();
                    DebugLog.print("  Span[" + i + "]: \"" + clickText + "\" (" + start + "-" + end + ")");
                }
            } else {
                DebugLog.print("❌ 没有ClickableSpan！");
            }
            
            viewHolder.getTextView().setText(textSpan);
            DebugLog.print("setText(textSpan) 完成");
            
            // 验证MovementMethod是否还在
            DebugLog.print("MovementMethod after setText: " + viewHolder.getTextView().getMovementMethod());
        } else {
            DebugLog.print("⚠️ textSpan为null，使用普通文本");
            viewHolder.getTextView().setText(data.getText());
        }
        
        DebugLog.print("=== ChildTextBinder.bind(ItemData) 诊断结束 ===\n");
        
        // 绑定笺注
        if (data.hasNoteSpan()) {
            viewHolder.getNoteView().setText(data.getNoteSpan());
            // Fix: 如果有高亮关键字，强制显示
            if (hasHighlight(data.getNoteSpan())) {
                viewHolder.getNoteView().setVisibility(View.VISIBLE);
            }
        } else if (data.hasNote()) {
            viewHolder.getNoteView().setText(data.getNote());
        }
        
        // 绑定视频
        if (data.hasVideoSpan()) {
            viewHolder.getVideoView().setText(data.getVideoSpan());
            // Fix: 如果有高亮关键字，强制显示
            if (hasHighlight(data.getVideoSpan())) {
                viewHolder.getVideoView().setVisibility(View.VISIBLE);
            }
        } else if (data.hasVideo()) {
            viewHolder.getVideoView().setText(data.getVideoUrl());
        }
        
        // 加载图片(如果有)
        if (imageLoader != null && data.hasImage()) {
            String imageUrl = buildImageUrl(data.getImageUrl());
            imageLoader.loadIntoTextView(
                imageUrl,
                viewHolder.getTextView(),
                new ImageLoader.ImageLoadCallback() {
                    @Override
                    public void onSuccess(@NonNull String url, int imageWidth, int imageHeight) {
                        // 图片加载成功
                    }

                    @Override
                    public void onFailure(@NonNull String url, @Nullable String error) {
                        // 图片加载失败
                    }
                }
            );
        }
    }


    /**
     * 检查是否包含高亮Span
     * 用于在搜索模式下自动展开包含关键字的隐藏字段
     */
    private boolean hasHighlight(CharSequence text) {
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            Object[] spans = spanned.getSpans(0, spanned.length(), BackgroundColorSpan.class);
            return spans != null && spans.length > 0;
        }
        return false;
    }
}
