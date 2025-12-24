package run.yigou.gxzy.greendao.service;

import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.Security.SecurityUtils;
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
    
    /**
     * 添加总结（自动加密 title 和 content，存储后恢复原始内容）
     */
    @Override
    public long addEntity(ChatSummaryBean entity) {
        if (entity == null) return 0;
        // 保存原始内容
        String originalTitle = entity.getTitle();
        String originalContent = entity.getContent();
        // 加密
        encryptSummary(entity);
        // 存入数据库
        long id = super.addEntity(entity);
        // 恢复原始内容（避免影响 UI 显示）
        entity.setTitle(originalTitle);
        entity.setContent(originalContent);
        return id;
    }
    
    /**
     * 更新总结（自动加密 title 和 content，存储后恢复原始内容）
     */
    @Override
    public void updateEntity(ChatSummaryBean entity) {
        if (entity == null) return;
        // 保存原始内容
        String originalTitle = entity.getTitle();
        String originalContent = entity.getContent();
        // 加密
        encryptSummary(entity);
        // 存入数据库
        super.updateEntity(entity);
        // 恢复原始内容（避免影响 UI 显示）
        entity.setTitle(originalTitle);
        entity.setContent(originalContent);
    }

    @Override
    public ArrayList<ChatSummaryBean> findAll() {
        QueryBuilder<ChatSummaryBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSummaryBeanDao.Properties.IsDelete.eq(ChatSummaryBean.IS_Delete_NO));
        // 按照创建时间降序排序
        queryBuilder.orderDesc(ChatSummaryBeanDao.Properties.CreateTime);
        ArrayList<ChatSummaryBean> list = (ArrayList<ChatSummaryBean>) queryBuilder.list();
        // 解密所有总结
        for (ChatSummaryBean summary : list) {
            decryptSummary(summary);
        }
        return list;
    }
    
    /**
     * 按条件查找总结（自动解密）
     */
    @Override
    public ArrayList<ChatSummaryBean> find(WhereCondition cond, WhereCondition... condMore) {
        ArrayList<ChatSummaryBean> list = super.find(cond, condMore);
        // 解密所有总结
        for (ChatSummaryBean summary : list) {
            decryptSummary(summary);
        }
        return list;
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
        List<ChatSummaryBean> list = queryBuilder.list();
        // 解密所有总结
        for (ChatSummaryBean summary : list) {
            decryptSummary(summary);
        }
        return list;
    }

    /**
     * 根据ID查找总结
     */
    public ChatSummaryBean findById(Long id) {
        if (id == null) return null;
        QueryBuilder<ChatSummaryBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSummaryBeanDao.Properties.Id.eq(id));
        queryBuilder.where(ChatSummaryBeanDao.Properties.IsDelete.eq(ChatSummaryBean.IS_Delete_NO));
        ChatSummaryBean summary = queryBuilder.unique();
        if (summary != null) {
            decryptSummary(summary);
        }
        return summary;
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
            // 注意：这里需要先加密再更新，因为 findBySessionId 已经解密了
            encryptSummary(summary);
            super.updateEntity(summary);
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
    public void deleteAllSummaries() {
        List<ChatSummaryBean> summaries = findAll();
        for (ChatSummaryBean summary : summaries) {
            summary.setIsDelete(ChatSummaryBean.IS_Delete_YES);
            // 注意：这里需要先加密再更新，因为 findAll 已经解密了
            encryptSummary(summary);
            super.updateEntity(summary);
        }
    }
    
    /**
     * 加密总结内容
     */
    private void encryptSummary(ChatSummaryBean entity) {
        String title = entity.getTitle();
        if (title != null && !title.isEmpty()) {
            String encrypted = SecurityUtils.rc4Encrypt(title);
            if (encrypted != null) {
                entity.setTitle(encrypted);
            }
        }
        String content = entity.getContent();
        if (content != null && !content.isEmpty()) {
            String encrypted = SecurityUtils.rc4Encrypt(content);
            if (encrypted != null) {
                entity.setContent(encrypted);
            }
        }
    }
    
    /**
     * 解密总结内容
     */
    private void decryptSummary(ChatSummaryBean entity) {
        String title = entity.getTitle();
        if (title != null && !title.isEmpty()) {
            String decrypted = SecurityUtils.rc4Decrypt(title);
            if (decrypted != null) {
                entity.setTitle(decrypted);
            }
        }
        String content = entity.getContent();
        if (content != null && !content.isEmpty()) {
            String decrypted = SecurityUtils.rc4Decrypt(content);
            if (decrypted != null) {
                entity.setContent(decrypted);
            }
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
