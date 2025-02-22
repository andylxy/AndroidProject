package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToMany;

import java.io.Serializable;
import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import run.yigou.gxzy.greendao.gen.DaoSession;
import run.yigou.gxzy.greendao.gen.AiConfigBodyDao;
import run.yigou.gxzy.greendao.gen.AiConfigDao;
@Entity
public class AiConfig implements Serializable {
    private static final long serialVersionUID = 1017L;
    @Id
    private  String AiConfigId;
    private String ProvideAi;
    private String ApiKey;
    private String AiUrl;
    @ToMany(referencedJoinProperty = "AiConfigId")
    private List<AiConfigBody> ModelList;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1138359028)
    private transient AiConfigDao myDao;
    @Generated(hash = 1199439553)
    public AiConfig(String AiConfigId, String ProvideAi, String ApiKey,
            String AiUrl) {
        this.AiConfigId = AiConfigId;
        this.ProvideAi = ProvideAi;
        this.ApiKey = ApiKey;
        this.AiUrl = AiUrl;
    }
    @Generated(hash = 1368893629)
    public AiConfig() {
    }
    public String getAiConfigId() {
        return this.AiConfigId;
    }
    public void setAiConfigId(String AiConfigId) {
        this.AiConfigId = AiConfigId;
    }
    public String getProvideAi() {
        return this.ProvideAi;
    }
    public void setProvideAi(String ProvideAi) {
        this.ProvideAi = ProvideAi;
    }
    public String getApiKey() {
        return this.ApiKey;
    }
    public void setApiKey(String ApiKey) {
        this.ApiKey = ApiKey;
    }
    public String getAiUrl() {
        return this.AiUrl;
    }
    public void setAiUrl(String AiUrl) {
        this.AiUrl = AiUrl;
    }
    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1059453975)
    public List<AiConfigBody> getModelList() {
        if (ModelList == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            AiConfigBodyDao targetDao = daoSession.getAiConfigBodyDao();
            List<AiConfigBody> ModelListNew = targetDao
                    ._queryAiConfig_ModelList(AiConfigId);
            synchronized (this) {
                if (ModelList == null) {
                    ModelList = ModelListNew;
                }
            }
        }
        return ModelList;
    }
    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1244427668)
    public synchronized void resetModelList() {
        ModelList = null;
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
    @Generated(hash = 1083863299)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getAiConfigDao() : null;
    }

}
