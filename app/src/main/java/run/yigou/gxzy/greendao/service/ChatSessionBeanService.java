package run.yigou.gxzy.greendao.service;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;

import run.yigou.gxzy.greendao.entity.ChatSessionBean;
import run.yigou.gxzy.greendao.gen.ChatSessionBeanDao;

public class ChatSessionBeanService extends BaseService<ChatSessionBean, ChatSessionBeanDao> {
    private ChatSessionBeanService() {
        if (ChatSessionBeanService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final ChatSessionBeanService INSTANCE = new ChatSessionBeanService();
    }

    public static ChatSessionBeanService getInstance() {
        return ChatSessionBeanService.SingletonHolder.INSTANCE;
    }

    @Override
    public ArrayList<ChatSessionBean> findAll() {
        QueryBuilder<ChatSessionBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSessionBeanDao.Properties.IsDelete.eq(1));
        // 按照更新时间降序排序
        queryBuilder.orderDesc(ChatSessionBeanDao.Properties.UpdateTime);
        return (ArrayList<ChatSessionBean>) queryBuilder.list();
    }

    /**
     * 查找所有会话（包括已删除的）
     */
    public ArrayList<ChatSessionBean> findAllWithDeleted() {
        QueryBuilder<ChatSessionBean> queryBuilder = getQueryBuilder();
        // 按照更新时间降序排序
        queryBuilder.orderDesc(ChatSessionBeanDao.Properties.UpdateTime);
        return (ArrayList<ChatSessionBean>) queryBuilder.list();
    }

    /**
     * 根据ID查找会话
     */
    public ChatSessionBean findById(Long id) {
        if (id == null) return null;
        QueryBuilder<ChatSessionBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSessionBeanDao.Properties.Id.eq(id));
        queryBuilder.where(ChatSessionBeanDao.Properties.IsDelete.eq(1));
        return queryBuilder.unique();
    }

    /**
     * @return
     */
    @Override
    protected Class<ChatSessionBean> getEntityClass() {
        return ChatSessionBean.class;
    }

    /**
     * @return
     */
    @Override
    protected ChatSessionBeanDao getDao() {
        tableName = ChatSessionBeanDao.TABLENAME;
        return daoSession.getChatSessionBeanDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        ChatSessionBeanDao.createTable(mDatabase, true);
    }
}