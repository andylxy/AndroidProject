package run.yigou.gxzy.event;

import run.yigou.gxzy.data.model.HH2SectionData;

public class ChapterContentNotificationEvent {

    private int groupPosition;
    // 书籍 ID
    private String bookId;
    // 章节段落号
    private int chapterSection;
    // 章节标题
    private String chapterHeader;
    // 签名 ID
    private String signatureId;

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

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
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

    public String getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(String signatureId) {
        this.signatureId = signatureId;
    }
}
