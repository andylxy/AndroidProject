/*
 * 项目名: AndroidProject
 * 类名: UserInfo.java
 * 包名: run.yigou.gxzy.greendao.entity.UserInfo
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月11日 14:07:06
 * 上次修改时间: 2024年03月11日 14:07:06
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.NotNull;

/**
 * 版本:  1.0
 * 描述: 用户数据
 */
@Entity
public class UserInfo  {
    @Id
    private  String  id;

    //jwt
    private String token;
    //用户名
    private String userName;
    //头像
    private String img;
    @NotNull
    private String userLoginAccount;
    @Generated(hash = 569043903)
    public UserInfo(String id, String token, String userName, String img,
            @NotNull String userLoginAccount) {
        this.id = id;
        this.token = token;
        this.userName = userName;
        this.img = img;
        this.userLoginAccount = userLoginAccount;
    }
    @Generated(hash = 1279772520)
    public UserInfo() {
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getToken() {
        return this.token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public String getUserName() {
        return this.userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getImg() {
        return this.img;
    }
    public void setImg(String img) {
        this.img = img;
    }
    public String getUserLoginAccount() {
        return this.userLoginAccount;
    }
    public void setUserLoginAccount(String userLoginAccount) {
        this.userLoginAccount = userLoginAccount;
    }
    
}
