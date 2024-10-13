/*
 * 项目名: AndroidProject
 * 类名: NavItem.java
 * 包名: run.yigou.gxzy.http.entitymodel.NavItem
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月21日 16:56:28
 * 上次修改时间: 2024年09月21日 16:56:28
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.entitymodel;

import java.io.Serializable;

public class NavItem implements Serializable {

    private long Id;
    private String ImageUrl;
    private String BookName;
    private String ChenɡShu;
    private String Author;
    private String Desc;

    public int getBookNo() {
        return BookNo;
    }

    public void setBookNo(int bookNo) {
        BookNo = bookNo;
    }

    private int BookNo;


    public long getId() {
        return Id;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public String getBookName() {
        return BookName;
    }

    public String getChenɡShu() {
        return ChenɡShu;
    }

    public String getAuthor() {
        return Author;
    }

    public void setId(long id) {
        Id = id;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }

    public void setBookName(String bookName) {
        BookName = bookName;
    }

    public void setChenɡShu(String chenɡShu) {
        ChenɡShu = chenɡShu;
    }

    public void setAuthor(String author) {
        Author = author;
    }

    public void setDesc(String desc) {
        Desc = desc;
    }

    public String getDesc() {
        return Desc;
    }

}
