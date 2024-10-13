package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;

public class YaoFangBodyService extends BaseService<YaoFangBody, YaoFangBodyDao>{
    /**
     * @return
     */
    @Override
    protected Class<YaoFangBody> getEntityClass() {
        return YaoFangBody.class;
    }

    /**
     * @return
     */
    @Override
    protected YaoFangBodyDao getDao() {
        tableName = YaoFangBodyDao.TABLENAME;
        return daoSession.getYaoFangBodyDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
         YaoFangBodyDao.createTable(mDatabase,true);
    }
}
