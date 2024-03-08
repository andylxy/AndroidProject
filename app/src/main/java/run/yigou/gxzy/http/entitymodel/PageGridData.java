/*
 * 项目名: AndroidProject
 * 类名: PageGridData.java
 * 包名: run.yigou.gxzy.http.entitymodel.PageGridData
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月07日 17:27:56
 * 上次修改时间: 2024年03月07日 17:27:56
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.entitymodel;

import java.io.Serializable;
import java.util.List;

/**
 * 版本:  1.0
 * 描述:
 */
public class PageGridData <T> implements Serializable {
    private int status ;
    private String msg  ;
    private int total  ;
    private List<T> rows  ;
    private Object summary  ;

    public int getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public int getTotal() {
        return total;
    }

    public List<T> getRows() {
        return rows;
    }

    public Object getSummary() {
        return summary;
    }

    public Object getExtra() {
        return extra;
    }

    /// <summary>
    /// 可以在返回前，再返回一些额外的数据，比如返回其他表的信息，前台找到查询后的方法取出来
    /// </summary>
    private Object extra  ;
}
