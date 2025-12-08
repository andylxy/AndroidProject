/*
 * 项目名: AndroidProject
 * 类名: UserInfoService.java
 * 包名: run.yigou.gxzy.greendao.service.UserInfoService
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月11日 14:17:34
 * 上次修改时间: 2024年03月11日 14:17:34
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.greendao.service;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.greendao.gen.UserInfoDao;

/**
 * 版本:  1.0
 * 描述:
 */
public class UserInfoService extends BaseService<UserInfo,UserInfoDao> {

    private UserInfoService() {
        if (UserInfoService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final UserInfoService INSTANCE = new UserInfoService();
    }

    public static UserInfoService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    protected Class<UserInfo> getEntityClass() {
        return UserInfo.class;
    }

    @Override
    protected UserInfoDao  getDao() {
        tableName= UserInfoDao.TABLENAME;
        return  daoSession.getUserInfoDao();
    }

    @Override
    protected void createTable() {
        //UserInfoDao.dropTable(mDatabase,true);
        UserInfoDao.createTable(mDatabase,true);
    }

    public UserInfo getLoginUserInfo() {
         if (!getQueryBuilder().list().isEmpty()){
             return getQueryBuilder().list().get(0);
         }else return null;
    }

    public UserInfo findUserInfoByLoginAccount(String account){
        UserInfo userInfo = null;
        try {
            QueryBuilder<UserInfo> userInfoQueryBuilder = getQueryBuilder().where(UserInfoDao.Properties.UserLoginAccount.eq(account));
            List<UserInfo> userInfoList = userInfoQueryBuilder.list(); //查出当前对应的数据
            if (userInfoList.size() == 1) {
                userInfo = userInfoList.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userInfo;
    }

    @Override
    public long addEntity(UserInfo entity) {
        entity.setId(getUUID());
        return super.addEntity(entity);
    }
}
