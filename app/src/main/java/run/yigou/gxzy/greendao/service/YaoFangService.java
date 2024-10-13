package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.gen.YaoFangDao;

public class YaoFangService extends BaseService<YaoFang, YaoFangDao>{
    /**
     * @return
     */
    @Override
    protected Class<YaoFang> getEntityClass() {
        return YaoFang.class;
    }

    /**
     * @return
     */
    @Override
    protected YaoFangDao getDao() {
        tableName = YaoFangDao.TABLENAME;
        return daoSession.getYaoFangDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {

        YaoFangDao.createTable(mDatabase,true);
    }
}
