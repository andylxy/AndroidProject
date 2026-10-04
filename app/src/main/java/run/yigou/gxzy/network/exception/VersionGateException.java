package run.yigou.gxzy.network.exception;

/**
 * 内容端点返回 HTTP 426（版本门）：本机低于后端地板（ADR-0005 / ADR-0008）。
 *
 * 该失败已由 {@code UpdateManager#onVersionTooLow()} 拉取 {@code /api/app/version}
 * 并弹出**强制**升级框处理完毕，故通用错误 toast 必须静默——用户该看到的是升级框，
 * 而不是「服务器响应异常，请稍后再试」（票据 21）。
 */
public final class VersionGateException extends HandledHttpException {

    public VersionGateException(String message) {
        super(message);
    }
}
