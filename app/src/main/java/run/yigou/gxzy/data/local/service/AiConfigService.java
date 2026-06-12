package run.yigou.gxzy.data.local.service;


import run.yigou.gxzy.data.local.entity.AiConfig;
import run.yigou.gxzy.data.local.entity.ZhongYao;
import run.yigou.gxzy.data.local.gen.AiConfigDao;
import run.yigou.gxzy.data.local.gen.ZhongYaoDao;

public class AiConfigService extends BaseService<AiConfig, AiConfigDao> {
    private AiConfigService() {
        if (AiConfigService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final AiConfigService INSTANCE = new AiConfigService();
    }

    public static AiConfigService getInstance() {
        return AiConfigService.SingletonHolder.INSTANCE;
    }

    /**
     * @return
     */
    @Override
    protected Class<AiConfig> getEntityClass() {
        return AiConfig.class ;
    }

    /**
     * @return
     */
    @Override
    protected AiConfigDao getDao() {
        tableName = AiConfigDao.TABLENAME;
        return  daoSession.getAiConfigDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        AiConfigDao.createTable(mDatabase, true);
    }
}
