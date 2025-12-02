package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToMany;

import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import run.yigou.gxzy.greendao.gen.DaoSession;
import run.yigou.gxzy.greendao.gen.ChatMessageBeanDao;
import run.yigou.gxzy.greendao.gen.ChatSessionBeanDao;

@Entity
public class ChatSessionBean {
    @Id(autoincrement = true)
    private Long id;

    /**
     * 会话标题
     */
    @NotNull
    private String title;

    /**
     * 会话预览内容
     */
    private String preview;

    /**
     * 会话创建时间
     */
    @NotNull
    private String createTime;

    /**
     * 最后更新时间
     */
    @NotNull
    private String updateTime;

    /**
     * 是否已删除
     */
    private int isDelete = 1; // 1未删除，0已删除
    
    /**
     * 已删除
     */
    public static final int IS_Delete_YES = 0;
    /**
     * 未删除
     */
    public static final int IS_Delete_NO = 1;

    /**
     * 与该会话关联的聊天消息列表
     */
    @ToMany(referencedJoinProperty = "sessionId")
    private List<ChatMessageBean> messages;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1492390832)
    private transient ChatSessionBeanDao myDao;

    @Generated(hash = 992195672)
    public ChatSessionBean(Long id, @NotNull String title, String preview,
            @NotNull String createTime, @NotNull String updateTime, int isDelete) {
        this.id = id;
        this.title = title;
        this.preview = preview;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.isDelete = isDelete;
    }

    @Generated(hash = 83447120)
    public ChatSessionBean() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPreview() {
        return this.preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public int getIsDelete() {
        return this.isDelete;
    }

    public void setIsDelete(int isDelete) {
        this.isDelete = isDelete;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1381369774)
    public List<ChatMessageBean> getMessages() {
        if (messages == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            ChatMessageBeanDao targetDao = daoSession.getChatMessageBeanDao();
            List<ChatMessageBean> messagesNew = targetDao
                    ._queryChatSessionBean_Messages(id);
            synchronized (this) {
                if (messages == null) {
                    messages = messagesNew;
                }
            }
        }
        return messages;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1942469556)
    public synchronized void resetMessages() {
        messages = null;
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
    @Generated(hash = 1287442734)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getChatSessionBeanDao() : null;
    }

}