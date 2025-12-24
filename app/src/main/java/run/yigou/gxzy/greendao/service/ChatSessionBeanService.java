package run.yigou.gxzy.greendao.service;

import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.ArrayList;

import run.yigou.gxzy.Security.SecurityUtils;
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
    
    /**
     * 添加会话（自动加密 title 和 preview，存储后恢复原始内容）
     */
    @Override
    public long addEntity(ChatSessionBean entity) {
        if (entity == null) return 0;
        // 保存原始内容
        String originalTitle = entity.getTitle();
        String originalPreview = entity.getPreview();
        // 加密
        encryptSession(entity);
        // 存入数据库
        long id = super.addEntity(entity);
        // 恢复原始内容（避免影响 UI 显示）
        entity.setTitle(originalTitle);
        entity.setPreview(originalPreview);
        return id;
    }
    
    /**
     * 更新会话（自动加密 title 和 preview，存储后恢复原始内容）
     */
    @Override
    public void updateEntity(ChatSessionBean entity) {
        if (entity == null) return;
        // 保存原始内容
        String originalTitle = entity.getTitle();
        String originalPreview = entity.getPreview();
        // 加密
        encryptSession(entity);
        // 存入数据库
        super.updateEntity(entity);
        // 恢复原始内容（避免影响 UI 显示）
        entity.setTitle(originalTitle);
        entity.setPreview(originalPreview);
    }

    @Override
    public ArrayList<ChatSessionBean> findAll() {
        QueryBuilder<ChatSessionBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSessionBeanDao.Properties.IsDelete.eq(1));
        // 按照更新时间降序排序
        queryBuilder.orderDesc(ChatSessionBeanDao.Properties.UpdateTime);
        ArrayList<ChatSessionBean> list = (ArrayList<ChatSessionBean>) queryBuilder.list();
        // 解密所有会话
        for (ChatSessionBean session : list) {
            decryptSession(session);
        }
        return list;
    }
    
    /**
     * 按条件查找会话（自动解密）
     */
    @Override
    public ArrayList<ChatSessionBean> find(WhereCondition cond, WhereCondition... condMore) {
        ArrayList<ChatSessionBean> list = super.find(cond, condMore);
        // 解密所有会话
        for (ChatSessionBean session : list) {
            decryptSession(session);
        }
        return list;
    }

    /**
     * 查找所有会话（包括已删除的）
     */
    public ArrayList<ChatSessionBean> findAllWithDeleted() {
        QueryBuilder<ChatSessionBean> queryBuilder = getQueryBuilder();
        // 按照更新时间降序排序
        queryBuilder.orderDesc(ChatSessionBeanDao.Properties.UpdateTime);
        ArrayList<ChatSessionBean> list = (ArrayList<ChatSessionBean>) queryBuilder.list();
        // 解密所有会话
        for (ChatSessionBean session : list) {
            decryptSession(session);
        }
        return list;
    }

    /**
     * 根据ID查找会话
     */
    public ChatSessionBean findById(Long id) {
        if (id == null) return null;
        QueryBuilder<ChatSessionBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatSessionBeanDao.Properties.Id.eq(id));
        queryBuilder.where(ChatSessionBeanDao.Properties.IsDelete.eq(1));
        ChatSessionBean session = queryBuilder.unique();
        if (session != null) {
            decryptSession(session);
        }
        return session;
    }
    
    /**
     * 加密会话内容
     */
    private void encryptSession(ChatSessionBean entity) {
        String title = entity.getTitle();
        if (title != null && !title.isEmpty()) {
            String encrypted = SecurityUtils.rc4Encrypt(title);
            if (encrypted != null) {
                entity.setTitle(encrypted);
            }
        }
        String preview = entity.getPreview();
        if (preview != null && !preview.isEmpty()) {
            String encrypted = SecurityUtils.rc4Encrypt(preview);
            if (encrypted != null) {
                entity.setPreview(encrypted);
            }
        }
    }
    
    /**
     * 解密会话内容
     */
    private void decryptSession(ChatSessionBean entity) {
        String title = entity.getTitle();
        if (title != null && !title.isEmpty()) {
            String decrypted = SecurityUtils.rc4Decrypt(title);
            if (decrypted != null) {
                entity.setTitle(decrypted);
            }
        }
        String preview = entity.getPreview();
        if (preview != null && !preview.isEmpty()) {
            String decrypted = SecurityUtils.rc4Decrypt(preview);
            if (decrypted != null) {
                entity.setPreview(decrypted);
            }
        }
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