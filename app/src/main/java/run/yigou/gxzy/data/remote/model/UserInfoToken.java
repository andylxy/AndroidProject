/*
 * ????? AndroidProject
 * ???: UserInfoToken.java
 * ???: run.yigou.gxzy.http.model.UserInfoToken
 * ????: Zhs (xiaoyang_02@qq.com)
 * ????????? : 2024??3??1??08:17:34
 * ?????????: 2024??1??6??21:42:31
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.data.remote.model;

import java.io.Serializable;

/**
 * author :
 * github :
 * time   :
 * desc   : ?????????
 */
public final   class UserInfoToken implements Serializable {
    //jwt
    private String token;
    //?????
    private String userName;

    public UserInfoToken(String token, String userName, String img, String userLoginAccount) {
        this.token = token;
        this.userName = userName;
        this.img = img;
        this.userLoginAccount = userLoginAccount;
    }

    //???
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
