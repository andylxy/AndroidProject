package run.yigou.gxzy.greendao.entity;



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
    private static final long serialVersionUID = 1525487L;

    @Id
    private String bookId;
    private int bookNo;
    private String bookName;//书名
    private String author;//作者
    private int historiographerNumb;//上次关闭时的章节数
    private int sortCode;//排序编码

    private int lastReadPosition;//上次阅读到的章节的位置



    @Generated(hash = 1839243756)
    public Book() {
    }



    @Generated(hash = 2101327507)
    public Book(String bookId, int bookNo, String bookName, String author,
            int historiographerNumb, int sortCode, int lastReadPosition) {
        this.bookId = bookId;
        this.bookNo = bookNo;
        this.bookName = bookName;
        this.author = author;
        this.historiographerNumb = historiographerNumb;
        this.sortCode = sortCode;
        this.lastReadPosition = lastReadPosition;
    }



    public int getBookNo() {
        return this.bookNo;
    }

    public void setBookNo(int bookNo) {
        this.bookNo = bookNo;
    }

    public String getBookName() {
        return this.bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getHistoriographerNumb() {
        return this.historiographerNumb;
    }

    public void setHistoriographerNumb(int historiographerNumb) {
        this.historiographerNumb = historiographerNumb;
    }

    public int getSortCode() {
        return this.sortCode;
    }

    public void setSortCode(int sortCode) {
        this.sortCode = sortCode;
    }

    public int getLastReadPosition() {
        return this.lastReadPosition;
    }

    public void setLastReadPosition(int lastReadPosition) {
        this.lastReadPosition = lastReadPosition;
    }



    public String getBookId() {
        return this.bookId;
    }



    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

}
