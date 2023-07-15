/*
 * 项目名: AndroidProject
 * 类名: RequestServer.java
 * 包名: com.intellij.copyright.JavaCopyrightVariablesProvider$1@41232cd1,qualifiedClassName
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 19:20:00
 * 上次修改时间: 2023年07月05日 17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.model;

import run.yigou.gxzy.other.AppConfig;

import com.hjq.http.config.IRequestServer;
import com.hjq.http.model.BodyType;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2020/10/02
 * desc   : 服务器配置
 */
public class RequestServer implements IRequestServer {

    @Override
    public String getHost() {
        return AppConfig.getHostUrl();
    }

    @Override
    public String getPath() {
        return "api/";
    }

    //    @Override
//    public BodyType getType() {
//        // 以表单的形式提交参数
//        return BodyType.FORM;
//    }
    @Override
    public BodyType getType() {
        return BodyType.JSON;
    }
}