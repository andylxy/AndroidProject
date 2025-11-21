/*
 * 项目名: AndroidProject
 * 类名: CopyApi.java
 * 包名: com.intellij.copyright.JavaCopyrightVariablesProvider$1@516caa2d,qualifiedClassName
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 18:41:20
 * 上次修改时间: 2023年07月05日 17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.http.entitymodel.ChapterInfo;

/**
 *  版本:  1.0
 *  描述:
 *
 */
public final class GetChapterDetail implements IRequestApi {

    @Override
    public String getApi() {
        return "getChapterDetail";
    }

    public String getId() {
        return Id;
    }

    public GetChapterDetail setId(String id) {
        Id = id;
        return this;
    }

    private   String Id;

    public String getKeywords() {
        return Keywords;
    }

    public GetChapterDetail setKeywords(String keywords) {
        Keywords = keywords;
        return this;
    }

    private   String Keywords;

    public final static class Bean extends ChapterInfo {

    }
}