package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;

import java.io.Serializable;
import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import run.yigou.gxzy.greendao.gen.DaoSession;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;
import run.yigou.gxzy.greendao.gen.BookChapterDao;

@Entity
public class BookChapter implements Serializable {
    private static final long serialVersionUID = 32014L;

    @Id
    private String bookChapterId;
    private int section;
    private int bookId;
    private String header;
    private long signatureId;
    private String signature;
    @ToMany(referencedJoinProperty = "bookChapterId")
    private List<BookChapterBody> data;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1400647315)
    private transient BookChapterDao myDao;


    @Generated(hash = 551523080)
    public BookChapter(String bookChapterId, int section, int bookId, String header, long signatureId,
            String signature) {
        this.bookChapterId = bookChapterId;
        this.section = section;
        this.bookId = bookId;
        this.header = header;
        this.signatureId = signatureId;
        this.signature = signature;
    }

    @Generated(hash = 1481387400)
    public BookChapter() {
    }

    public long getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(long signatureId) {
        this.signatureId = signatureId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setSection(int section) {
        this.section = section;
    }

    public int getSection() {
        return section;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }









    public int getBookId() {
        return this.bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getBookChapterId() {
        return this.bookChapterId;
    }

    public void setBookChapterId(String bookChapterId) {
        this.bookChapterId = bookChapterId;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1064605192)
    public List<BookChapterBody> getData() {
        if (data == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            BookChapterBodyDao targetDao = daoSession.getBookChapterBodyDao();
            List<BookChapterBody> dataNew = targetDao
                    ._queryBookChapter_Data(bookChapterId);
            synchronized (this) {
                if (data == null) {
                    data = dataNew;
                }
            }
        }
        return data;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1283600904)
    public synchronized void resetData() {
        data = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 980434935)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getBookChapterDao() : null;
    }

}
