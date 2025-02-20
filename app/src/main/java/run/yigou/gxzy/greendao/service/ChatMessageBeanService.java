package run.yigou.gxzy.greendao.service;


import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;

import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.gen.ChapterDao;
import run.yigou.gxzy.greendao.gen.ChatMessageBeanDao;
import run.yigou.gxzy.greendao.gen.TabNavDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoDao;
import run.yigou.gxzy.greendao.util.DbService;

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
    @Override
    public ArrayList<ChatMessageBean> findAll() {


        QueryBuilder<ChatMessageBean> queryBuilder = getQueryBuilder();
        queryBuilder.where(ChatMessageBeanDao.Properties.IsDelete.eq(1));
        // 按照 'Order' 字段升序排序
        queryBuilder.orderAsc(ChatMessageBeanDao.Properties.CreateDate);
        return (ArrayList<ChatMessageBean>) queryBuilder.list();
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
