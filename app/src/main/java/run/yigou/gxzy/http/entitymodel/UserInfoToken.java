/*
 * 项目名: AndroidProject
 * 类名: UserInfoToken.java
 * 包名: run.yigou.gxzy.http.entitymodel.UserInfoToken
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月11日 08:17:34
 * 上次修改时间: 2024年01月16日 21:42:31
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.entitymodel;

import java.io.Serializable;

/**
 * author :
 * github :
 * time   :
 * desc   : 登陆用户信息
 */
public final   class UserInfoToken implements Serializable {
    //jwt
    private String token;
    //用户名
    private String userName;

    public UserInfoToken(String token, String userName, String img, String userLoginAccount) {
        this.token = token;
        this.userName = userName;
        this.img = img;
        this.userLoginAccount = userLoginAccount;
    }

    //头像
    private String img;

    public String getUserLoginAccount() {
        return userLoginAccount;
    }

    private String userLoginAccount;
    public UserInfoToken setToken(String token) {
        this.token = token;
        return this;
    }

    public UserInfoToken setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public UserInfoToken setImg(String img) {
        this.img = img;
        return this;
    }

    public String getToken() {
        return token;
    }

    public String getUserName() {
        return userName;
    }

    public String getImg() {
        return img;
    }

}