package run.yigou.gxzy.http.exception;

import com.hjq.http.exception.HttpException;

public final class ResultException extends HttpException {

    public ResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultException(String message, Object data) {
        super(message);
    }
}
