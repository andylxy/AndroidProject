package run.yigou.gxzy.ui.tips.entity;

import android.text.SpannableStringBuilder;

/**
 * 条目数据类
 * 封装Child级别的数据
 */
public class ItemData {
    private SpannableStringBuilder attributedText;
    private SpannableStringBuilder attributedNote;
    private SpannableStringBuilder attributedVideo;
    private String imageUrl;
    private int groupPosition;
    
    // 默认构造函数
    public ItemData() {
    }
    
    // DataAdapter.fromChildEntity 使用的构造函数
    public ItemData(String text, String note, String videoUrl, String imageUrl,
                    SpannableStringBuilder textSpan, SpannableStringBuilder noteSpan,
                    SpannableStringBuilder videoSpan) {
        // 优先使用富文本（带 ClickableSpan）
        this.attributedText = textSpan;
        this.attributedNote = noteSpan;
        this.attributedVideo = videoSpan;
        this.imageUrl = imageUrl;
    }

    public SpannableStringBuilder getAttributedText() {
        return attributedText;
    }

    public void setAttributedText(SpannableStringBuilder attributedText) {
        this.attributedText = attributedText;
    }

    public SpannableStringBuilder getAttributedNote() {
        return attributedNote;
    }

    public void setAttributedNote(SpannableStringBuilder attributedNote) {
        this.attributedNote = attributedNote;
    }

    public SpannableStringBuilder getAttributedVideo() {
        return attributedVideo;
    }

    public void setAttributedVideo(SpannableStringBuilder attributedVideo) {
        this.attributedVideo = attributedVideo;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getGroupPosition() {
        return groupPosition;
    }

    public void setGroupPosition(int groupPosition) {
        this.groupPosition = groupPosition;
    }
    
    // ==================== 便捷方法 (ChildTextBinder 使用) ====================
    
    public boolean hasTextSpan() {
        return attributedText != null && attributedText.length() > 0;
    }
    
    public SpannableStringBuilder getTextSpan() {
        return attributedText;
    }
    
    public String getText() {
        return attributedText != null ? attributedText.toString() : "";
    }
    
    public boolean hasNoteSpan() {
        return attributedNote != null && attributedNote.length() > 0;
    }
    
    public SpannableStringBuilder getNoteSpan() {
        return attributedNote;
    }
    
    public String getNote() {
        return attributedNote != null ? attributedNote.toString() : "";
    }
    
    public boolean hasVideoSpan() {
        return attributedVideo != null && attributedVideo.length() > 0;
    }
    
    public SpannableStringBuilder getVideoSpan() {
        return attributedVideo;
    }
    
    public String getVideo() {
        return attributedVideo != null ? attributedVideo.toString() : "";
    }
}
