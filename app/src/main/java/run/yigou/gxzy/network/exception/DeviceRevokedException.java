package run.yigou.gxzy.network.exception;

/**
 * 请求收到 401 且响应头带 {@code X-Device-Revoked: 1}：该设备已被管理员吊销（ADR-0003）。
 *
 * 该失败已由 {@code UpdateManager#onDeviceRevoked()} 弹出「设备已被禁用」提示处理完毕，
 * 且**不会**跳转登录页（避免「反复登录仍 401」的死循环）。故通用错误 toast 必须静默（票据 21）。
 */
public final class DeviceRevokedException extends HandledHttpException {

    public DeviceRevokedException(String message) {
        super(message);
    }
}
