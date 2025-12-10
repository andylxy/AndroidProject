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
}
