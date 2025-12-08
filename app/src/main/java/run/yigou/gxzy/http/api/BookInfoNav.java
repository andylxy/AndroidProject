/*
 * 项目名: AndroidProject
 * 类名: BookInfoNav.java
 * 包名: run.yigou.gxzy.http.api.BookInfoNav
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 23:20:45
 * 上次修改时间: 2023年07月05日 20:35:30
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

import java.util.List;

import run.yigou.gxzy.http.entitymodel.NavItem;


/**
 * 作者:  zhs
 * 时间:  2023/7/6 9:22
 * 包名:  run.yigou.gxzy.http.api
 * 文件:  BookInfoNav.java
 * 类名:  BookInfoNav
 * 版本:  1.0
 * 描述:  dkii
 */

public final class BookInfoNav implements IRequestApi {

    @Override
    public String getApi() {
        return "getNav";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "GET"; // 或 "POST", "PUT" 等
    }
}