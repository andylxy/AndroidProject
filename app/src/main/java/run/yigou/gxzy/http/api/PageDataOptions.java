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

import com.hjq.http.annotation.HttpIgnore;
import com.hjq.http.config.IRequestApi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  作者:  zhs
 *  时间:  2023-07-14 15:23:14
 *  包名:  run.yigou.gxzy.http.api
 *  类名:  PageDataOptions
 *  版本:  1.0
 *  描述:
 *
*/
public final class PageDataOptions implements IRequestApi {

    @Override
    public String getApi() {
        return mUrl;
    }

    @HttpIgnore
    private  String mUrl;
    private  int Page;
    private  int Rows;
    private  int Total;
    private  String TableName;
    private  String Sort;
    /// <summary>
    /// 排序方式
    /// </summary>
    private  String Order;
    private  String Wheres;
    private  boolean Export;
    private  Object Value;
    /// <summary>
    /// 查询条件
    /// </summary>
    private  List<SearchParameters>   Filter = new ArrayList<>();;

    public String getUrl() {
        return mUrl;
    }

    public PageDataOptions setUrl(String url) {
        mUrl = url;
        return this;
    }

    public int getPage() {
        return Page;
    }

    public PageDataOptions setPage(int page) {
        Page = page;
        return this;
    }

    public int getRows() {
        return Rows;
    }

    public PageDataOptions setRows(int rows) {
        Rows = rows;
        return this;
    }

    public int getTotal() {
        return Total;
    }

    public PageDataOptions setTotal(int total) {
        Total = total;
        return this;
    }

    public String getTableName() {
        return TableName;
    }

    public PageDataOptions setTableName(String tableName) {
        TableName = tableName;
        return this;
    }

    public String getSort() {
        return Sort;
    }

    public PageDataOptions setSort(String sort) {
        Sort = sort;
        return this;
    }

    public String getOrder() {
        return Order;
    }

    public PageDataOptions setOrder(String order) {
        Order = order;
        return this;
    }

    public String getWheres() {
        return Wheres;
    }

    public PageDataOptions setWheres(String wheres) {
        Wheres = wheres;
        return this;
    }

    public boolean isExport() {
        return Export;
    }

    public PageDataOptions setExport(boolean export) {
        Export = export;
        return this;
    }

    public Object getValue() {
        return Value;
    }

    public PageDataOptions setValue(Object value) {
        Value = value;
        return this;
    }

    public List<SearchParameters> getFilter() {
        return Filter;
    }

    public PageDataOptions setFilter(List<SearchParameters> filter) {
        Filter = filter;
        return this;
    }
    public PageDataOptions setFilter(SearchParameters filter) {

        Filter .add(filter) ;
        return this;
    }

    public static class SearchParameters implements Serializable {
        private String Name;
        private String Value;

        public SearchParameters(String name, String value) {
            Name = name;
            Value = value;
        }

        public String getName() {
            return Name;
        }

        public void setName(String name) {
            Name = name;
        }

        public String getValue() {
            return Value;
        }

        public void setValue(String value) {
            Value = value;
        }

        public String getDisplayType() {
            return DisplayType;
        }

        public void setDisplayType(String displayType) {
            DisplayType = displayType;
        }

        @Override
        public String toString() {
            return "{" +
                    "Name='" + Name + '\'' +
                    ", Value='" + Value + '\'' +
                    ", DisplayType='" + DisplayType + '\'' +
                    '}';
        }

        //查询类型：LinqExpressionType
        private String DisplayType="like";
    }

}