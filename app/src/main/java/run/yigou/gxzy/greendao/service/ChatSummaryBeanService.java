package run.yigou.gxzy.greendao.service;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.greendao.entity.ChatSummaryBean;
import run.yigou.gxzy.greendao.gen.ChatSummaryBeanDao;

/**
 * 会话总结数据服务类
 */
public class ChatSummaryBeanService extends BaseService<ChatSummaryBean, ChatSummaryBeanDao> {
    
    private ChatSummaryBeanService() {
        if (ChatSummaryBeanService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final ChatSummaryBeanService INSTANCE = new ChatSummaryBeanService();
    }

    public static ChatSummaryBeanService getInstance() {
        return ChatSummaryBeanService.SingletonHolder.INSTANCE;
    }

    @Override
    public ArrayList<ChatSummaryBean> findAll() {
        QueryBuilder<ChatSummaryBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSummaryBeanDao.Properties.IsDelete.eq(ChatSummaryBean.IS_Delete_NO));
        // 按照创建时间降序排序
        queryBuilder.orderDesc(ChatSummaryBeanDao.Properties.CreateTime);
        return (ArrayList<ChatSummaryBean>) queryBuilder.list();
    }

    /**
     * 根据会话ID查找所有总结
     * @param sessionId 会话ID
     * @return 总结列表（按创建时间降序）
     */
    public List<ChatSummaryBean> findBySessionId(Long sessionId) {
        if (sessionId == null) return new ArrayList<>();
        QueryBuilder<ChatSummaryBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSummaryBeanDao.Properties.SessionId.eq(sessionId));
        queryBuilder.where(ChatSummaryBeanDao.Properties.IsDelete.eq(ChatSummaryBean.IS_Delete_NO));
        queryBuilder.orderDesc(ChatSummaryBeanDao.Properties.CreateTime);
        return queryBuilder.list();
    }

    /**
     * 根据ID查找总结
     */
    public ChatSummaryBean findById(Long id) {
        if (id == null) return null;
        QueryBuilder<ChatSummaryBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSummaryBeanDao.Properties.Id.eq(id));
        queryBuilder.where(ChatSummaryBeanDao.Properties.IsDelete.eq(ChatSummaryBean.IS_Delete_NO));
        return queryBuilder.unique();
    }

    /**
     * 删除会话的所有总结（软删除）
     * @param sessionId 会话ID
     */
    public void deleteBySessionId(Long sessionId) {
        if (sessionId == null) return;
        List<ChatSummaryBean> summaries = findBySessionId(sessionId);
        for (ChatSummaryBean summary : summaries) {
            summary.setIsDelete(ChatSummaryBean.IS_Delete_YES);
            updateEntity(summary);
        }
    }

    /**
     * 获取会话的最新总结
     * @param sessionId 会话ID
     * @return 最新的总结，如果没有则返回null
     */
    public ChatSummaryBean findLatestBySessionId(Long sessionId) {
        List<ChatSummaryBean> summaries = findBySessionId(sessionId);
        return summaries.isEmpty() ? null : summaries.get(0);
    }

    /**
     * 删除所有总结（软删除）
     */
    public void deleteAll() {
        List<ChatSummaryBean> summaries = findAll();
        for (ChatSummaryBean summary : summaries) {
            summary.setIsDelete(ChatSummaryBean.IS_Delete_YES);
            updateEntity(summary);
        }
    }

    @Override
    protected Class<ChatSummaryBean> getEntityClass() {
        return ChatSummaryBean.class;
    }

    @Override
    protected ChatSummaryBeanDao getDao() {
        tableName = ChatSummaryBeanDao.TABLENAME;
        return daoSession.getChatSummaryBeanDao();
    }

    @Override
    protected void createTable() {
        ChatSummaryBeanDao.createTable(mDatabase, true);
    }
}
