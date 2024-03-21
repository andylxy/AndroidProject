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
public class WebResponseContent<T> implements Serializable {
    private boolean Status ;
    private String Code  ;
    private String Message  ;

    public boolean isStatus() {
        return Status;
    }

    public String getCode() {
        return Code;
    }

    public String getMessage() {
        return Message;
    }

    public T getData() {
        return Data;
    }

    /// <summary>
    /// 返回的数据
    /// </summary>
    private T Data  ;
}
