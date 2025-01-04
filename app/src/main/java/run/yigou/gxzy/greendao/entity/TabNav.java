package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;

import java.io.Serializable;
import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import run.yigou.gxzy.greendao.gen.DaoSession;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;
import run.yigou.gxzy.greendao.gen.TabNavDao;
@Entity
public class TabNav  implements Serializable {
    private static final long serialVersionUID = 55544L;
    @Id
    private String tabNavId;
    private int caseId;
    private String name;
    private  int order;
    @ToMany(referencedJoinProperty = "tabNavId")
    private List<TabNavBody> navList;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 353333401)
    private transient TabNavDao myDao;

    @Generated(hash = 1330423056)
    public TabNav(String tabNavId, int caseId, String name, int order) {
        this.tabNavId = tabNavId;
        this.caseId = caseId;
        this.name = name;
        this.order = order;
    }
    @Generated(hash = 1847113361)
    public TabNav() {
    }

    public String getTabNavId() {
        return this.tabNavId;
    }
    public void setTabNavId(String tabNavId) {
        this.tabNavId = tabNavId;
    }
    public int getCaseId() {
        return this.caseId;
    }
    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getOrder() {
        return this.order;
    }
    public void setOrder(int order) {
        this.order = order;
    }
    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1131160606)
    public List<TabNavBody> getNavList() {
        if (navList == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            TabNavBodyDao targetDao = daoSession.getTabNavBodyDao();
            List<TabNavBody> navListNew = targetDao._queryTabNav_NavList(tabNavId);
            synchronized (this) {
                if (navList == null) {
                    navList = navListNew;
                }
            }
        }
        return navList;
    }
    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 10087846)
    public synchronized void resetNavList() {
        navList = null;
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
    @Generated(hash = 1643148368)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getTabNavDao() : null;
    }

}
