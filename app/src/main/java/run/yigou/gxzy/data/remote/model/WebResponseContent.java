/*
 * ????? AndroidProject
 * ???: PageGridData.java
 * ???: run.yigou.gxzy.http.model.PageGridData
 * ????: Zhs (xiaoyang_02@qq.com)
 * ????????? : 2024??3??7??17:27:56
 * ?????????: 2024??3??7??17:27:56
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.data.remote.model;

import java.io.Serializable;
import java.util.List;

/**
 * ???:  1.0
 * ???:
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
    /// ????????
    /// </summary>
    private T Data  ;
}
