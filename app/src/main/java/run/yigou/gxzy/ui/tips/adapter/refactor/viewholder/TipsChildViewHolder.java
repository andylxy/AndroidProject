/*
 * 项目名: AndroidProject
 * 类名: TipsChildViewHolder.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.viewholder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 子项ViewHolder - 封装Child绑定逻辑,支持三段式TextView
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.viewholder;

import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.adapter.refactor.utils.TextViewHelper;
import run.yigou.gxzy.ui.tips.entity.ChildEntity;

/**
 * Tips子项ViewHolder
 * 封装Child数据绑定逻辑,管理三段式TextView(text/note/video)
 */
public class TipsChildViewHolder {

    private final TextView tvText;
    private final TextView tvNote;
    private final TextView tvVideo;

    /**
     * 构造函数
     *
     * @param holder BaseViewHolder
     */
    public TipsChildViewHolder(@NonNull BaseViewHolder holder) {
        this.tvText = holder.get(R.id.tv_sectiontext);
        this.tvNote = holder.get(R.id.tv_sectionnote);
        this.tvVideo = holder.get(R.id.tv_sectionvideo);
    }

    /**
     * 绑定Child数据 - 完整版本
     *
     * @param entity 数据实体
     * @param mode   显示模式
     */
    public void bind(@NonNull ChildEntity entity, @NonNull TextViewHelper.DisplayMode mode) {
        // 绑定文本内容
        bindText(entity);
        bindNote(entity);
        bindVideo(entity);

        // 切换显示模式
        TextViewHelper.switchDisplayMode(tvText, tvNote, tvVideo, mode);
    }

    /**
     * 绑定正文内容
     *
     * @param entity 数据实体
     */
    private void bindText(@NonNull ChildEntity entity) {
        SpannableStringBuilder spannableText = entity.getAttributed_child_section_text();
        if (spannableText != null && spannableText.length() > 0) {
            tvText.setText(spannableText);
        } else if (entity.getChild_section_text() != null) {
            tvText.setText(entity.getChild_section_text());
        } else {
            tvText.setText("");
        }
    }

    /**
     * 绑定注释内容
     *
     * @param entity 数据实体
     */
    private void bindNote(@NonNull ChildEntity entity) {
        SpannableStringBuilder spannableNote = entity.getAttributed_child_section_note();
        if (spannableNote != null && spannableNote.length() > 0) {
            tvNote.setText(spannableNote);
        } else if (entity.getChild_section_note() != null) {
            tvNote.setText(entity.getChild_section_note());
        } else {
            tvNote.setText("");
        }
    }

    /**
     * 绑定视频内容
     *
     * @param entity 数据实体
     */
    private void bindVideo(@NonNull ChildEntity entity) {
        SpannableStringBuilder spannableVideo = entity.getAttributed_child_section_video();
        if (spannableVideo != null && spannableVideo.length() > 0) {
            tvVideo.setText(spannableVideo);
        } else if (entity.getChild_section_video() != null) {
            tvVideo.setText(entity.getChild_section_video());
        } else {
            tvVideo.setText("");
        }
    }

    /**
     * 绑定数据 - 简化版本(使用默认TEXT模式)
     *
     * @param entity 数据实体
     */
    public void bind(@NonNull ChildEntity entity) {
        bind(entity, TextViewHelper.DisplayMode.TEXT);
    }

    /**
     * 切换显示模式
     *
     * @return 切换后的模式
     */
    public TextViewHelper.DisplayMode toggleVisibility() {
        return TextViewHelper.toggleVisibility(tvText, tvNote, tvVideo);
    }

    /**
     * 获取当前显示模式
     *
     * @return 当前模式
     */
    public TextViewHelper.DisplayMode getCurrentMode() {
        return TextViewHelper.getCurrentMode(tvText, tvNote, tvVideo);
    }

    /**
     * 获取当前可见的TextView
     *
     * @return 当前显示的TextView
     */
    @Nullable
    public TextView getVisibleTextView() {
        if (tvText.getVisibility() == View.VISIBLE) {
            return tvText;
        } else if (tvNote.getVisibility() == View.VISIBLE) {
            return tvNote;
        } else if (tvVideo.getVisibility() == View.VISIBLE) {
            return tvVideo;
        }
        return null;
    }

    /**
     * 获取当前可见TextView的文本
     *
     * @return 当前显示的文本
     */
    @Nullable
    public CharSequence getVisibleText() {
        TextView visibleView = getVisibleTextView();
        return visibleView != null ? visibleView.getText() : null;
    }

    /**
     * 设置点击事件
     *
     * @param holder          BaseViewHolder
     * @param clickListener   点击监听器
     */
    public static void setClickListener(@NonNull BaseViewHolder holder,
                                         @NonNull View.OnClickListener clickListener) {
        holder.itemView.setOnClickListener(clickListener);
    }

    /**
     * 设置长按事件
     *
     * @param holder              BaseViewHolder
     * @param longClickListener   长按监听器
     */
    public static void setLongClickListener(@NonNull BaseViewHolder holder,
                                             @NonNull View.OnLongClickListener longClickListener) {
        holder.itemView.setOnLongClickListener(longClickListener);
    }

    /**
     * 获取正文TextView
     *
     * @return TextView
     */
    public TextView getTextView() {
        return tvText;
    }

    /**
     * 获取注释TextView
     *
     * @return TextView
     */
    public TextView getNoteView() {
        return tvNote;
    }

    /**
     * 获取视频TextView
     *
     * @return TextView
     */
    public TextView getVideoView() {
        return tvVideo;
    }
    
    /**
     * 切换正文可见性
     */
    public void toggleTextVisibility(SpannableStringBuilder content) {
        if (content == null || content.length() == 0) {
            return;
        }
        if (tvText.getVisibility() == View.VISIBLE) {
            tvText.setVisibility(View.GONE);
        } else {
            tvText.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 切换笺注可见性
     */
    public void toggleNoteVisibility(SpannableStringBuilder content) {
        if (content == null || content.length() == 0) {
            return;
        }
        if (tvNote.getVisibility() == View.VISIBLE) {
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 切换视频可见性
     */
    public void toggleVideoVisibility(SpannableStringBuilder content) {
        if (content == null || content.length() == 0) {
            return;
        }
        if (tvVideo.getVisibility() == View.VISIBLE) {
            tvVideo.setVisibility(View.GONE);
        } else {
            tvVideo.setVisibility(View.VISIBLE);
        }
    }
}
