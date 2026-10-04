package run.yigou.gxzy.network.exception;

import com.hjq.http.exception.HttpException;

/**
 * 已由专用 UI 处理、通用错误 toast 必须静默的 HTTP 失败的**标记类型**。
 *
 * {@code RequestHandler} 抛异常前会先通知 {@code UpdateManager} 弹出专用提示（强制升级框 /
 * 设备已被禁用），但异常照旧抛给调用方，而 {@code AppActivity/AppFragment#onHttpFail} 一律
 * toast，于是多出一条「服务器响应异常，请稍后再试」，与「必须升级」自相矛盾（票据 21）。
 *
 * 约定：本类（含子类）只保留专用提示，不弹通用 toast；其它失败（网络、超时、500、普通 401）照旧。
 * 判定逻辑见 {@link HandledHttpFailure}。
 *
 * 为何不继承 {@code ResponseException}：它是 final，故继承其父类 HttpException。
 */
public abstract class HandledHttpException extends HttpException {

    protected HandledHttpException(String message) {
        super(message);
    }
}
