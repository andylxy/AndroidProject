package run.yigou.gxzy.greendao.service;


import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.ArrayList;

import run.yigou.gxzy.Security.SecurityUtils;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.gen.ChatMessageBeanDao;

public class ChatMessageBeanService extends BaseService<ChatMessageBean, ChatMessageBeanDao> {
    private ChatMessageBeanService() {
        if (ChatMessageBeanService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final ChatMessageBeanService INSTANCE = new ChatMessageBeanService();
    }

    public static ChatMessageBeanService getInstance() {
        return ChatMessageBeanService.SingletonHolder.INSTANCE;
    }
    
    /**
     * 添加消息（自动加密 content，存储后恢复原始内容）
     */
    @Override
    public long addEntity(ChatMessageBean entity) {
        if (entity == null) return 0;
        // 保存原始内容
        String originalContent = entity.getContent();
        // 加密
        encryptMessage(entity);
        // 存入数据库
        long id = super.addEntity(entity);
        // 恢复原始内容（避免影响 UI 显示）
        entity.setContent(originalContent);
        return id;
    }
    
    /**
     * 更新消息（自动加密 content，存储后恢复原始内容）
     */
    @Override
    public void updateEntity(ChatMessageBean entity) {
        if (entity == null) return;
        // 保存原始内容
        String originalContent = entity.getContent();
        // 加密
        encryptMessage(entity);
        // 存入数据库
        super.updateEntity(entity);
        // 恢复原始内容（避免影响 UI 显示）
        entity.setContent(originalContent);
    }
    
    @Override
    public ArrayList<ChatMessageBean> findAll() {
        QueryBuilder<ChatMessageBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatMessageBeanDao.Properties.IsDelete.eq(1));
        queryBuilder.orderAsc(ChatMessageBeanDao.Properties.CreateDate);
        ArrayList<ChatMessageBean> list = (ArrayList<ChatMessageBean>) queryBuilder.list();
        // 解密所有消息
        for (ChatMessageBean msg : list) {
            decryptMessage(msg);
        }
        return list;
    }
    
    /**
     * 按条件查找消息（自动解密 content）
     */
    @Override
    public ArrayList<ChatMessageBean> find(WhereCondition cond, WhereCondition... condMore) {
        ArrayList<ChatMessageBean> list = super.find(cond, condMore);
        // 解密所有消息
        for (ChatMessageBean msg : list) {
            decryptMessage(msg);
        }
        return list;
    }
    
    /**
     * 加密消息内容
     */
    private void encryptMessage(ChatMessageBean entity) {
        String content = entity.getContent();
        if (content != null && !content.isEmpty()) {
            String encrypted = SecurityUtils.rc4Encrypt(content);
            if (encrypted != null) {
                entity.setContent(encrypted);
            }
        }
    }
    
    /**
     * 解密消息内容
     */
    private void decryptMessage(ChatMessageBean entity) {
        String content = entity.getContent();
        if (content != null && !content.isEmpty()) {
            String decrypted = SecurityUtils.rc4Decrypt(content);
            if (decrypted != null) {
                entity.setContent(decrypted);
            }
        }
    }
    
    /**
     * @return
     */
    @Override
    protected Class<ChatMessageBean> getEntityClass() {
        return ChatMessageBean.class ;
    }

    /**
     * @return
     */
    @Override
    protected ChatMessageBeanDao getDao() {
        tableName = ChatMessageBeanDao.TABLENAME;
        return  daoSession.getChatMessageBeanDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        ChatMessageBeanDao.createTable(mDatabase, true);
    }
}
