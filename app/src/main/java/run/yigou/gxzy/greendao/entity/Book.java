package run.yigou.gxzy.greendao.entity;



import androidx.annotation.Nullable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

/**
 * 书
 * Created by zhao on 2017/7/24.
 */

@Entity
public class Book implements Serializable {
    @Transient
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String bookId;
    private String name;//书名
    private String chapterUrl;//书目Url
    private String imgUrl;//封面图片url
    private String desc;//简介
    private String author;//作者

    private String type;//类型

    private String updateDate;//更新时间

    private String newestChapterId;//最新章节id

    private String newestChapterTitle;//最新章节标题

    private String newestChapterUrl;//最新章节url

    private String historyChapterId;//上次关闭时的章节ID

    private int histtoryChapterNum;//上次关闭时的章节数

    private int sortCode;//排序编码

    private int noReadNum;//未读章数量

    private int chapterTotalNum;//总章节数

    private int lastReadPosition;//上次阅读到的章节的位置


    private String source;




    @Generated(hash = 2009405574)
    public Book(String id, String bookId, String name, String chapterUrl,
            String imgUrl, String desc, String author, String type,
            String updateDate, String newestChapterId, String newestChapterTitle,
            String newestChapterUrl, String historyChapterId,
            int histtoryChapterNum, int sortCode, int noReadNum,
            int chapterTotalNum, int lastReadPosition, String source) {
        this.id = id;
        this.bookId = bookId;
        this.name = name;
        this.chapterUrl = chapterUrl;
        this.imgUrl = imgUrl;
        this.desc = desc;
        this.author = author;
        this.type = type;
        this.updateDate = updateDate;
        this.newestChapterId = newestChapterId;
        this.newestChapterTitle = newestChapterTitle;
        this.newestChapterUrl = newestChapterUrl;
        this.historyChapterId = historyChapterId;
        this.histtoryChapterNum = histtoryChapterNum;
        this.sortCode = sortCode;
        this.noReadNum = noReadNum;
        this.chapterTotalNum = chapterTotalNum;
        this.lastReadPosition = lastReadPosition;
        this.source = source;
    }
    @Generated(hash = 1839243756)
    public Book() {
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getChapterUrl() {
        return this.chapterUrl;
    }
    public void setChapterUrl(String chapterUrl) {
        this.chapterUrl = chapterUrl;
    }
    public String getImgUrl() {
        return this.imgUrl;
    }
    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
    public String getDesc() {
        return this.desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    public String getAuthor() {
        return this.author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getType() {
        return this.type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getUpdateDate() {
        return this.updateDate;
    }
    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }
    public String getNewestChapterId() {
        return this.newestChapterId;
    }
    public void setNewestChapterId(String newestChapterId) {
        this.newestChapterId = newestChapterId;
    }
    public String getNewestChapterTitle() {
        return this.newestChapterTitle;
    }
    public void setNewestChapterTitle(String newestChapterTitle) {
        this.newestChapterTitle = newestChapterTitle;
    }
    public String getNewestChapterUrl() {
        return this.newestChapterUrl;
    }
    public void setNewestChapterUrl(String newestChapterUrl) {
        this.newestChapterUrl = newestChapterUrl;
    }
    public String getHistoryChapterId() {
        return this.historyChapterId;
    }
    public void setHistoryChapterId(String historyChapterId) {
        this.historyChapterId = historyChapterId;
    }
    public int getHisttoryChapterNum() {
        return this.histtoryChapterNum;
    }
    public void setHisttoryChapterNum(int histtoryChapterNum) {
        this.histtoryChapterNum = histtoryChapterNum;
    }
    public int getSortCode() {
        return this.sortCode;
    }
    public void setSortCode(int sortCode) {
        this.sortCode = sortCode;
    }
    public int getNoReadNum() {
        return this.noReadNum;
    }
    public void setNoReadNum(int noReadNum) {
        this.noReadNum = noReadNum;
    }
    public int getChapterTotalNum() {
        return this.chapterTotalNum;
    }
    public void setChapterTotalNum(int chapterTotalNum) {
        this.chapterTotalNum = chapterTotalNum;
    }
    public int getLastReadPosition() {
        return this.lastReadPosition;
    }
    public void setLastReadPosition(int lastReadPosition) {
        this.lastReadPosition = lastReadPosition;
    }
    public String getSource() {
        return this.source;
    }
    public void setSource(String source) {
        this.source = source;
    }
    public String getBookId() {
        return this.bookId;
    }
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }


}
