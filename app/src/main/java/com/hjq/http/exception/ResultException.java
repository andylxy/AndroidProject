package com.hjq.http.exception;

/**
 * 返回结果异常
 */
public class ResultException extends HttpException {

    private final Object mData;

    public ResultException(String message, Object data) {
        super(message);
        mData = data;
    }

    public ResultException(String message, Throwable cause, Object data) {
        super(message, cause);
        mData = data;
    }

    public <T> T getData() {
        return (T) mData;
    }
}
