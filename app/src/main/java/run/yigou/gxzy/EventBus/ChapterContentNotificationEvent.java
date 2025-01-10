package run.yigou.gxzy.EventBus;

import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;

public class ChapterContentNotificationEvent {

    private int groupPosition;
    // 书籍id
    private int bookId;
    // 章节序号
    private int chapterSection;
    // 章节标题
    private String chapterHeader;
    // 书签id
    private Long signatureId;

    public HH2SectionData getData() {
        return data;
    }

    public void setData(HH2SectionData data) {
        this.data = data;
    }

    private HH2SectionData data;
    public int getGroupPosition() {
        return groupPosition;
    }

    public void setGroupPosition(int groupPosition) {
        this.groupPosition = groupPosition;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getChapterSection() {
        return chapterSection;
    }

    public void setChapterSection(int chapterSection) {
        this.chapterSection = chapterSection;
    }

    public String getChapterHeader() {
        return chapterHeader;
    }

    public void setChapterHeader(String chapterHeader) {
        this.chapterHeader = chapterHeader;
    }

    public Long getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(Long signatureId) {
        this.signatureId = signatureId;
    }
}
