package run.yigou.gxzy.data.local.service;


import run.yigou.gxzy.data.local.entity.AiConfigBody;
import run.yigou.gxzy.data.local.entity.ZhongYao;
import run.yigou.gxzy.data.local.gen.AiConfigBodyDao;
import run.yigou.gxzy.data.local.gen.ZhongYaoDao;

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
