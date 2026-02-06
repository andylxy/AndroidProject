package com.hjq.http.exception;

/**
 * Token 失效异常
 */
public class TokenException extends HttpException {

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
