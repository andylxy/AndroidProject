package run.yigou.gxzy.ui.tips.tipsutils;

import android.os.Build;
import android.text.SpannableStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 数据项类，用于存储和管理数据项的各种属性。
 */
public class DataItem implements Serializable {
    /**
     * 记录序号
     */
    private int ID;
    /**
     * 带有富文本格式的内容
     */
    private SpannableStringBuilder attributedText;
    /**
     * 方列表
     */
    private List<String> fangList;
    /**
     * 内容文本
     */
    private String text;
    /**
     * 药列表
     */
    private List<String> yaoList;
    /**
     * 内容注解
     */
    private String note;
    /**
     * 内容笔记
     */
    private String sectionvideo;
    /**
     * 带有富文本格式的注解
     */
    private SpannableStringBuilder attributedNote;
    /**
     * 带有富文本格式的笔记
     */
    private SpannableStringBuilder attributedSectionVideo;
    /**
     * 组位置
     */
    private int groupPosition;
    /**
     * 签名ID
     */
    private long signatureId;
    /**
     * 签名
     */
    private String signature;
    /**
     * 图片URL
     */
    private String imageUrl;
    /**
     * 名称
     */
    private String name;

    /**
     * 获取名称
     * @return 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置名称
     * @param name 名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 设置图片URL
     * @param imageUrl 图片URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 获取图片URL
     * @return 图片URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 获取签名ID
     * @return 签名ID
     */
    public long getSignatureId() {
        return signatureId;
    }

    /**
     * 设置签名ID
     * @param signatureId 签名ID
     */
    public void setSignatureId(long signatureId) {
        this.signatureId = signatureId;
    }

    /**
     * 获取签名
     * @return 签名
     */
    public String getSignature() {
        return signature;
    }

    /**
     * 设置签名
     * @param signature 签名
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * 获取组位置
     * @return 组位置
     */
    public int getGroupPosition() {
        return groupPosition;
    }

    /**
     * 设置组位置
     * @param groupPosition 组位置
     */
    public void setGroupPosition(int groupPosition) {
        this.groupPosition = groupPosition;
    }

    /**
     * 获取带有富文本格式的笔记
     * @return 带有富文本格式的笔记
     */
    public SpannableStringBuilder getAttributedSectionVideo() {
        if (this.attributedSectionVideo == null && this.sectionvideo != null) {
            this.attributedSectionVideo = TipsNetHelper.renderText(this.sectionvideo);
        }
        return attributedSectionVideo;
    }

    /**
     * 设置带有富文本格式的笔记
     * @param attributedSectionVideo 带有富文本格式的笔记
     */
    public void setAttributedSectionVideo(SpannableStringBuilder attributedSectionVideo) {
        this.attributedSectionVideo = attributedSectionVideo;
    }

    /**
     * 获取带有富文本格式的注解
     * @return 带有富文本格式的注解
     */
    public SpannableStringBuilder getAttributedNote() {
        if (this.attributedNote == null && this.note != null) {
            this.attributedNote = TipsNetHelper.renderText(this.note);
        }
        return attributedNote;
    }

    /**
     * 设置带有富文本格式的注解
     * @param attributedNote 带有富文本格式的注解
     */
    public void setAttributedNote(SpannableStringBuilder attributedNote) {
        this.attributedNote = attributedNote;
    }

    /**
     * 获取注解
     * @return 注解
     */
    public String getNote() {
        return note;
    }

    /**
     * 设置注解，并生成带有富文本格式的注解
     * @param note 注解
     */
    public void setNote(String note) {
        this.note = note;
        this.attributedNote = TipsNetHelper.renderText(this.note);
    }

    /**
     * 获取笔记
     * @return 笔记
     */
    public String getSectionvideo() {
        return sectionvideo;
    }

    /**
     * 设置笔记，并生成带有富文本格式的笔记
     * @param sectionvideo 笔记
     */
    public void setSectionvideo(String sectionvideo) {
        this.sectionvideo = sectionvideo;
        this.attributedSectionVideo = TipsNetHelper.renderText(this.sectionvideo);
    }

    /**
     * 获取记录序号
     * @return 记录序号
     */
    public int getID() {
        return ID;
    }

    /**
     * 设置记录序号
     * @param ID 记录序号
     */
    public void setID(int ID) {
        this.ID = ID;
    }

    /**
     * 设置内容文本，并生成带有富文本格式的内容
     * @param str 内容文本
     */
    public void setText(String str) {
        this.text = str;
        this.attributedText = TipsNetHelper.renderText(this.text);
    }


    public DataItem getCopy() {
    DataItem dataItem = new DataItem();

    try {
        copyIfNotNull(this.text, dataItem::setPureText);
        copyIfNotNull(this.imageUrl, dataItem::setImageUrl);
        copyIfNotNull(this.attributedText, attributedText -> dataItem.setAttributedText(new SpannableStringBuilder(attributedText)));
        copyIfNotNull(this.fangList, fangList -> dataItem.setFangList(new ArrayList<>(fangList)));
        copyIfNotNull(this.yaoList, yaoList -> dataItem.setYaoList(new ArrayList<>(yaoList)));
        copyIfNotNull(this.note, dataItem::setNote);
        copyIfNotNull(this.sectionvideo, dataItem::setSectionvideo);
        copyIfNotNull(this.attributedNote, attributedNote -> dataItem.setAttributedNote(new SpannableStringBuilder(attributedNote)));
        copyIfNotNull(this.attributedSectionVideo, attributedSectionVideo -> dataItem.setAttributedSectionVideo(new SpannableStringBuilder(attributedSectionVideo)));
    } catch (Exception e) {
        // 记录日志或处理异常
        System.err.println("Error occurred during deep copy: " + e.getMessage());
    }

    return dataItem;
}

private <T> void copyIfNotNull(T value, Consumer<T> setter) {
    if (value != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setter.accept(value);
        }
    }
}



//    /**
//     * 创建当前对象的深拷贝
//     * @return 深拷贝的对象
//     */
//    public DataItem getCopy() {
//        DataItem dataItem = new DataItem();
//
//        // 设置纯文本内容
//        if (this.text != null) {
//            dataItem.setPureText(this.text);
//        }
//        // 设置图片URL
//        if (this.imageUrl != null) {
//            dataItem.setImageUrl(this.imageUrl);
//        }
//        // 设置带有富文本格式的内容
//        if (this.attributedText != null) {
//            dataItem.setAttributedText(new SpannableStringBuilder(this.attributedText));
//        }
//
//        // 深拷贝方列表
//        if (this.fangList != null) {
//            List<String> fangListCopy = new ArrayList<>(this.fangList);
//            dataItem.setFangList(fangListCopy);
//        }
//
//        // 深拷贝药列表
//        if (this.yaoList != null) {
//            List<String> yaoListCopy = new ArrayList<>(this.yaoList);
//            dataItem.setYaoList(yaoListCopy);
//        }
//
//        // 设置注解
//        if (this.note != null) {
//            dataItem.setNote(this.note);
//        }
//
//        // 设置笔记
//        if (this.sectionvideo != null) {
//            dataItem.setSectionvideo(this.sectionvideo);
//        }
//
//        // 设置带有富文本格式的注解
//        if (this.attributedNote != null) {
//            dataItem.setAttributedNote(new SpannableStringBuilder(this.attributedNote));
//        }
//
//        // 设置带有富文本格式的笔记
//        if (this.attributedSectionVideo != null) {
//            dataItem.setAttributedSectionVideo(new SpannableStringBuilder(this.attributedSectionVideo));
//        }
//
//        return dataItem;
//    }

    /**
     * 设置纯文本内容
     * @param str 纯文本内容
     */
    private void setPureText(String str) {
        this.text = str;
    }

    /**
     * 设置方列表
     * @param list 方列表
     */
    public void setFangList(List<String> list) {
        this.fangList = list;
    }

    /**
     * 设置药列表
     * @param list 药列表
     */
    public void setYaoList(List<String> list) {
        this.yaoList = list;
    }

    /**
     * 获取内容文本
     * @return 内容文本
     */
    public String getText() {
        return this.text;
    }

    /**
     * 获取带有富文本格式的内容
     * @return 带有富文本格式的内容
     */
    public SpannableStringBuilder getAttributedText() {
        if (this.attributedText == null && this.text != null) {
            this.attributedText = TipsNetHelper.renderText(this.text);
        }
        return this.attributedText;
    }

    /**
     * 设置带有富文本格式的内容
     * @param spannableStringBuilder 带有富文本格式的内容
     */
    public void setAttributedText(SpannableStringBuilder spannableStringBuilder) {
        this.attributedText = spannableStringBuilder;
    }

    /**
     * 获取方列表
     * @return 方列表
     */
    public List<String> getFangList() {
        return this.fangList == null ? new ArrayList<>() : this.fangList;
    }

    /**
     * 获取药列表
     * @return 药列表
     */
    public List<String> getYaoList() {
        return this.yaoList == null ? new ArrayList<>() : this.yaoList;
    }

    /**
     * 获取项目索引
     * @param str 输入字符串
     * @return 项目索引
     */
    protected int getItemIndex(String str) {
        return Integer.parseInt(str.substring(0, str.indexOf("、"))) - 1;
    }
}
