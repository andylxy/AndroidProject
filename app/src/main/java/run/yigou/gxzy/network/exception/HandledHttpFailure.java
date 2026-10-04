package run.yigou.gxzy.network.exception;

import com.hjq.http.exception.ResponseException;

import okhttp3.Response;

/**
 * 「这条失败是否已经由专用 UI 提示过」的**唯一判定处**。
 *
 * 集中在这里有两个原因：
 * 1. 响应语义（426 = 强制升级框；401 + {@code X-Device-Revoked: 1} = 设备已被禁用）
 *    属于网络层知识，异常类不该反过来依赖抛它的 {@code RequestHandler}（包循环）；
 * 2. 这里是纯 JVM 可测的（不碰 Android 类），所以判定逻辑能被单测覆盖，
 *    而不必每次都靠真机 logcat 验证。
 *
 * 为什么要两条判定（ADR-0008 / 票据 21）：
 * - EasyHttp 的 {@code ResponseException} 是 final，且实测会把我们抛出的异常重包成它
 *   （消息不变、类型被换），所以只靠 {@code instanceof} 判不出来；
 * - 但也不能只靠响应码：若某天库换成不包装，标记类型这条仍然有效。两条互补。
 */
public final class HandledHttpFailure {

    /** 版本门：低于地板 → 426（spec §6.4）。 */
    public static final int HTTP_UPGRADE_REQUIRED = 426;

    /** 未授权。 */
    public static final int HTTP_UNAUTHORIZED = 401;

    /** 吊销区分信号：与「会话过期」的普通 401 区分（ADR-0003）。 */
    public static final String HEADER_DEVICE_REVOKED = "X-Device-Revoked";

    /** 该响应头为 1 时表示「设备已被管理员禁用」。 */
    public static final String DEVICE_REVOKED_FLAG = "1";

    private HandledHttpFailure() {
    }

    /** 该响应是否属于「已由专用提示处理过」的失败。 */
    public static boolean isHandled(Response response) {
        if (response.code() == HTTP_UPGRADE_REQUIRED) {
            return true;
        }
        return response.code() == HTTP_UNAUTHORIZED
                && DEVICE_REVOKED_FLAG.equals(response.header(HEADER_DEVICE_REVOKED));
    }

    /**
     * 该失败是否已有专用提示 —— 是则通用错误 toast 必须静默（票据 21）。
     * 其它失败（网络、超时、500、普通 401）照旧弹 toast。
     */
    public static boolean shouldSilence(Throwable e) {
        if (e instanceof HandledHttpException) {
            return true;
        }
        if (!(e instanceof ResponseException)) {
            return false;
        }
        final Response response = ((ResponseException) e).getResponse();
        return response != null && isHandled(response);
    }
}
