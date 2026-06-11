/*
 * ????? AndroidProject
 * ???: ChapterSearchRes.java
 * ???: run.yigou.gxzy.http.model.ChapterSearchRes
 * ????: Zhs (xiaoyang_02@qq.com)
 * ????????? : 2024??4??1??09:36:14
 * ?????????: 2024??4??1??09:36:14
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.data.remote.model;

import java.io.Serializable;

/**
 * ???:  1.0
 * ???:
 */
public class ChapterSearchRes implements Serializable {

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
