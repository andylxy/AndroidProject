package run.yigou.gxzy.network.exception;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.hjq.http.exception.ResponseException;

import org.junit.Test;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 「已由专用 UI 提示过」的判定单测（票据 21）。
 *
 * 纯 JVM，不碰 Android 运行时，也不引入 mock 库 —— {@code Response} 用
 * {@code Response.Builder} 真实构造。在此之前，这段只能靠真机 logcat 间接验证。
 */
public class HandledHttpFailureTest {

    private static Response response(int code, String revokedFlag) {
        final Request request = new Request.Builder().url("http://localhost/x").build();
        final Response.Builder builder = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("m");
        if (revokedFlag != null) {
            builder.header(HandledHttpFailure.HEADER_DEVICE_REVOKED, revokedFlag);
        }
        return builder.build();
    }

    @Test
    public void versionGateResponseCountsAsHandled() {
        assertTrue(HandledHttpFailure.isHandled(response(426, null)));
    }

    @Test
    public void revokedDeviceResponseCountsAsHandled() {
        assertTrue(HandledHttpFailure.isHandled(response(401, "1")));
    }

    @Test
    public void plainUnauthorizedDoesNotCountAsHandled() {
        // 会话过期的普通 401 不能被当成「设备被禁用」，否则会误吞掉重登录提示。
        assertFalse(HandledHttpFailure.isHandled(response(401, null)));
        assertFalse(HandledHttpFailure.isHandled(response(401, "0")));
    }

    @Test
    public void serverErrorDoesNotCountAsHandled() {
        assertFalse(HandledHttpFailure.isHandled(response(500, null)));
    }

    @Test
    public void markerExceptionsAreSilenced() {
        assertTrue(HandledHttpFailure.shouldSilence(new VersionGateException("426")));
        assertTrue(HandledHttpFailure.shouldSilence(new DeviceRevokedException("401")));
    }

    @Test
    public void rewrappedHandledResponseIsStillSilenced() {
        // EasyHttp 会把我们抛出的异常重包成 ResponseException（消息不变、类型被换），
        // 所以只靠 instanceof 判不出来 —— 必须能靠响应码认出这两种。
        assertTrue(HandledHttpFailure.shouldSilence(
                new ResponseException("426", response(426, null))));
        assertTrue(HandledHttpFailure.shouldSilence(
                new ResponseException("401", response(401, "1"))));
    }

    @Test
    public void ordinaryResponseStillShowsToast() {
        assertFalse(HandledHttpFailure.shouldSilence(
                new ResponseException("500", response(500, null))));
        assertFalse(HandledHttpFailure.shouldSilence(
                new ResponseException("401", response(401, null))));
    }

    @Test
    public void nonHttpFailureIsNotSilenced() {
        assertFalse(HandledHttpFailure.shouldSilence(new IllegalStateException("boom")));
    }
}
