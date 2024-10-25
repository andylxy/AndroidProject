/*
 * 项目名: AndroidProject
 * 类名: SearchKeyEntity.java
 * 包名: run.yigou.gxzy.ui.tips.entity.SearchKeyEntity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月13日 12:24:52
 * 上次修改时间: 2024年09月13日 12:24:52
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.entity;

public class SearchKeyEntity {
    //String searchText,  Integer totalNum
    private  StringBuilder searchKeyText;
    private int searchResTotalNum=0;

    public SearchKeyEntity(StringBuilder searchKeyText, int searchResTotalNum) {
        this.searchKeyText = searchKeyText;
        this.searchResTotalNum = searchResTotalNum;
    }

    public SearchKeyEntity() {
    }

    public SearchKeyEntity(StringBuilder searchKeyText) {
        this.searchKeyText = searchKeyText;
    }

    public StringBuilder getSearchKeyText() {
        return searchKeyText;
    }

    public void setSearchKeyText(StringBuilder searchKeyText) {
        this.searchKeyText = searchKeyText;
    }

    public int getSearchResTotalNum() {
        return searchResTotalNum;
    }

    public void setSearchResTotalNum(int searchResTotalNum) {
        this.searchResTotalNum = searchResTotalNum;
    }
}
