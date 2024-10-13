package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;

import java.io.Serializable;
import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import run.yigou.gxzy.greendao.gen.DaoSession;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;
import run.yigou.gxzy.greendao.gen.YaoFangDao;
@Entity
public class YaoFang implements Serializable {
    private static final long serialVersionUID = 3301L;

    @Id
    private String yaoFangID;
    private int bookId;
    private int yaoCount;
    private int height;
    private String name;
    private int ID;
    private float drinkNum;
    private String text;
    private String fangList;
    private String yaoList;
    private long signatureId;
    private String signature;

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
    @ToMany(referencedJoinProperty = "yaoFangID")
    private List<YaoFangBody> standardYaoList;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 800854105)
    private transient YaoFangDao myDao;

    @Generated(hash = 1295549483)
    public YaoFang(String yaoFangID, int bookId, int yaoCount, int height, String name, int ID,
            float drinkNum, String text, String fangList, String yaoList, long signatureId,
            String signature) {
        this.yaoFangID = yaoFangID;
        this.bookId = bookId;
        this.yaoCount = yaoCount;
        this.height = height;
        this.name = name;
        this.ID = ID;
        this.drinkNum = drinkNum;
        this.text = text;
        this.fangList = fangList;
        this.yaoList = yaoList;
        this.signatureId = signatureId;
        this.signature = signature;
    }

    @Generated(hash = 1325300536)
    public YaoFang() {
    }



    public String getYaoFangID() {
        return this.yaoFangID;
    }
    public void setYaoFangID(String yaoFangID) {
        this.yaoFangID = yaoFangID;
    }
    public int getYaoCount() {
        return this.yaoCount;
    }
    public void setYaoCount(int yaoCount) {
        this.yaoCount = yaoCount;
    }
    public int getHeight() {
        return this.height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getID() {
        return this.ID;
    }
    public void setID(int ID) {
        this.ID = ID;
    }

    public String getText() {
        return this.text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getFangList() {
        return this.fangList;
    }
    public void setFangList(String fangList) {
        this.fangList = fangList;
    }
    public String getYaoList() {
        return this.yaoList;
    }
    public void setYaoList(String yaoList) {
        this.yaoList = yaoList;
    }
    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 493536584)
    public List<YaoFangBody> getStandardYaoList() {
        if (standardYaoList == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            YaoFangBodyDao targetDao = daoSession.getYaoFangBodyDao();
            List<YaoFangBody> standardYaoListNew = targetDao
                    ._queryYaoFang_StandardYaoList(yaoFangID);
            synchronized (this) {
                if (standardYaoList == null) {
                    standardYaoList = standardYaoListNew;
                }
            }
        }
        return standardYaoList;
    }
    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 2023334037)
    public synchronized void resetStandardYaoList() {
        standardYaoList = null;
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
    @Generated(hash = 662340023)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getYaoFangDao() : null;
    }
    public int getBookId() {
        return this.bookId;
    }
    public void setBookId(int bookId) {
        this.bookId = bookId;
    }
    public float getDrinkNum() {
        return this.drinkNum;
    }
    public void setDrinkNum(float drinkNum) {
        this.drinkNum = drinkNum;
    }
}
