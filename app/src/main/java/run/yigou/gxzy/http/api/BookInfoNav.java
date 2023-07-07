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

import java.io.Serializable;
import java.util.List;


/**
 *  作者:  zhs
 *  时间:  2023/7/6 9:22
 *  包名:  run.yigou.gxzy.http.api
 *  文件:  BookInfoNav.java
 *  类名:  BookInfoNav
 *  版本:  1.0
 *  描述:  dkii
 *
 */

public final class BookInfoNav implements IRequestApi {

    @Override
    public String getApi() {
        return "BookInfo/getNav";
    }

    public final static class Bean {
       private int Id ;
       private String Name ;
       private List<NavItem> NavList ;

        public int getId() {
            return Id;
        }

        public String getName() {
            return Name;
        }

        public List<NavItem> getNavList() {
            return NavList;
        }

        public final static class NavItem implements Serializable {
        private int   Id ;
        private String   ImageUrl ;
        private String BookName;
        private String ChenɡShu;
        private String Author;
        private String Desc;

           public int getId() {
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

           public String getDesc() {
               return Desc;
           }
       }

    }
}