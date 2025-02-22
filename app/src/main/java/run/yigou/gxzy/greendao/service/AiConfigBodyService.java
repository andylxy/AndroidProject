package run.yigou.gxzy.greendao.service;


import run.yigou.gxzy.greendao.entity.AiConfigBody;
import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.gen.AiConfigBodyDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoDao;

public class AiConfigBodyService extends BaseService<AiConfigBody, AiConfigBodyDao> {
    private AiConfigBodyService() {
        if (AiConfigBodyService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final AiConfigBodyService INSTANCE = new AiConfigBodyService();
    }

    public static AiConfigBodyService getInstance() {
        return AiConfigBodyService.SingletonHolder.INSTANCE;
    }

    /**
     * @return
     */
    @Override
    protected Class<AiConfigBody> getEntityClass() {
        return AiConfigBody.class ;
    }

    /**
     * @return
     */
    @Override
    protected AiConfigBodyDao getDao() {
        tableName = AiConfigBodyDao.TABLENAME;
        return  daoSession.getAiConfigBodyDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        AiConfigBodyDao.createTable(mDatabase, true);
    }
}
