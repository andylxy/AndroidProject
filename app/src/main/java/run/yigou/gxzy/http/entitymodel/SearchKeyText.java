/*
 * 项目名: AndroidProject
 * 类名: SearchKeyText.java
 * 包名: run.yigou.gxzy.http.entitymodel.SearchKeyText
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月07日 23:00:37
 * 上次修改时间: 2024年03月07日 23:00:37
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.entitymodel;

/**
 * 版本:  1.0
 * 描述:
 */
public class SearchKeyText {

    private int Id;

    public int getId() {
        return Id;
    }

    public String getChapterId() {
        return ChapterId;
    }

    public String getTitle() {
        return Title;
    }

    public String getAuthor() {
        return Author;
    }

    public String getBookName() {
        return BookName;
    }

    public String getType() {
        return Type;
    }

    public String getData() {
        return Data;
    }

    private String ChapterId;
    private String Title;
    private String Author;
    private String BookName;
    private String Type ;
    private String Data ;
}
