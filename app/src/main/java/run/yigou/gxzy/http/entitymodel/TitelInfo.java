/*
 * 项目名: AndroidProject
 * 类名: TitelInfo.java
 * 包名: run.yigou.gxzy.http.entitymodel.TitelInfo
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月27日 16:38:32
 * 上次修改时间: 2024年03月27日 16:38:32
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.entitymodel;

/**
 * 版本:  1.0
 * 描述:
 */
public class TitelInfo {

   private String   Id;
    private String  mTitle;
    private String  mTitleColor;
    private String mBookId ;
    private String mParentId;
    private String mComment;

    public String getId() {
        return Id;
    }

    public TitelInfo setId(String id) {
        Id = id;
        return this;
    }

    public String getTitle() {
        return mTitle;
    }

    public TitelInfo setTitle(String title) {
        mTitle = title;
        return this;
    }

    public String getTitleColor() {
        return mTitleColor;
    }

    public TitelInfo setTitleColor(String titleColor) {
        mTitleColor = titleColor;
        return this;
    }

    public String getBookId() {
        return mBookId;
    }

    public TitelInfo setBookId(String bookId) {
        mBookId = bookId;
        return this;
    }

    public String getParentId() {
        return mParentId;
    }

    public TitelInfo setParentId(String parentId) {
        mParentId = parentId;
        return this;
    }

    public String getComment() {
        return mComment;
    }

    public TitelInfo setComment(String comment) {
        mComment = comment;
        return this;
    }
}
