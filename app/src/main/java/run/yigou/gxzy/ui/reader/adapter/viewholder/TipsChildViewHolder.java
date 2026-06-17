/*
 * 项目名: AndroidProject
 * 类名: TipsChildViewHolder.java
 * 包名: run.yigou.gxzy.ui.reader.adapter.viewholder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 子项ViewHolder - 封装Child绑定逻辑,支持三段式TextView
 */

package run.yigou.gxzy.ui.reader.adapter.viewholder;

import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import run.yigou.gxzy.R;
import run.yigou.gxzy.utils.TextViewHelper;
import run.yigou.gxzy.ui.reader.entity.ChildEntity;

/**
 * Tips子项ViewHolder
 * 封装Child数据绑定逻辑,管理三段式TextView(text/note/video)
 */
public class TipsChildViewHolder {

    private final TextView tvText;
    private final TextView tvNote;
    private final TextView tvVideo;
    
    // 状态标记: 用于管理note/video切换逻辑
    private boolean isSectionvideo = true;

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
     * @param noteContent 笺注内容,用于判断是否可切换
     */
    public void toggleTextVisibility(SpannableStringBuilder noteContent) {
        if (isLinkClick(tvText)) {
            return;
        }
        
        // 检查笺注内容是否可用
        if (noteContent == null || noteContent.length() == 0) {
            return;
        }
        
        // 切换笺注显示
        if (tvNote.getVisibility() == View.VISIBLE) {
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 切换笺注可见性 - 复杂逻辑
     * @param videoContent 视频内容,用于判断是否可切换
     */
    public void toggleNoteVisibility(SpannableStringBuilder videoContent) {
        if (isLinkClick(tvNote)) {
            return;
        }
        
        // 检查video是否可用
        int videoLength = videoContent != null ? videoContent.length() : 0;
        boolean isVideoAvailable = videoLength > 0;
        
        // 特殊逻辑: note显示且video隐藏时的处理
        if (tvNote.getVisibility() == View.VISIBLE && 
            tvVideo.getVisibility() == View.GONE && 
            !isSectionvideo) {
            tvNote.setVisibility(View.GONE);
            isSectionvideo = true;
            return;
        }
        
        // 状态管理
        if (isVideoAvailable) {
            isSectionvideo = false;
        } else {
            tvNote.setVisibility(View.GONE);
            isSectionvideo = true;
            return;
        }
        
        // 切换视频显示
        if (tvVideo.getVisibility() == View.VISIBLE) {
            tvVideo.setVisibility(View.GONE);
        } else {
            tvVideo.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 切换视频可见性
     */
    public void toggleVideoVisibility() {
        if (isLinkClick(tvVideo)) {
            return;
        }
        
        // 切换视频显示并更新状态
        if (tvVideo.getVisibility() == View.VISIBLE) {
            tvVideo.setVisibility(View.GONE);
            isSectionvideo = true;
        } else {
            tvVideo.setVisibility(View.VISIBLE);
            isSectionvideo = false;
        }
    }

    /**
     * 检查 TextView 是否正在处理链接点击（由 LocalLinkMovementMethod 标记）
     */
    private static boolean isLinkClick(TextView textView) {
        Boolean isClick = (Boolean) textView.getTag();
        return isClick != null && isClick;
    }
}
