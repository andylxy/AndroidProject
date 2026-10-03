package run.yigou.gxzy.network.security;

import com.hjq.http.config.IRequestApi;
import com.hjq.http.model.RequestBodyType;
import com.hjq.http.model.HttpHeaders;
import com.hjq.http.model.HttpParams;

import java.util.Map;

import okhttp3.Request;
import run.yigou.gxzy.network.server.RequestServer;
import run.yigou.gxzy.utils.SerialUtil;
import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.log.EasyLog;

/**
 * 请求拦截辅助类
 * <p>
 * 为 EasyHttp 请求提供统一的 Header 注入（{@link #handleIntercept}），
 * 同时为绕过 EasyHttp 拦截器链的 SSE 等请求提供 Header 辅助（{@link #addSseSecurityHeaders}）。
 * 逻辑集中于此处，避免分散到各 API 类中重复实现。
 * </p>
 * <p>
 * 鉴权模型（microfeed 适配）：Bearer = 当前登录用户的 microfeed 登录凭证（{@code mflc_}）；
 * 服务端据此解析「用户 → 角色 → 权限」——AppBookRequest 内容端点需 {@code app:mobile:access}。
 * 不再使用固定设备密钥（HMAC / AccessKey / mf_ API Key 均已废弃）。
 * </p>
 * <p>
 * 防重放：始终注入 {@code X-Timestamp} / {@code X-Nonce}——microfeed 的 {@code checkReplay}
 * 靠这两个头做时间窗校验 + nonce 去重，**不需要共享密钥**。HMAC 签名
 * （{@code Signature} / {@code X-AccessKeyId}）是 netcore 需要的，仅在 AccessKey 凭证有效时注入；
 * microfeed 不校验签名。
 * </p>
 */
public class InterceptorHelper {

    /**
     * EasyHttp 请求拦截器核心方法：注入公共 Header、Bearer 鉴权、防重放头。
     */
    public static void handleIntercept(IRequestApi api, HttpParams params, HttpHeaders headers, AppApplication appApplication) {

        // 设置公共 Header
        //if (appApplication.global_openness && appApplication.mUserInfoToken == null)
        //    headers.put("Authorization", AppConst.AllowAnonymous_Token);
        headers.put("app", "2");
        headers.put("SessionId", SerialUtil.getSerial());
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/plain, */*");

        // 设备标识 + 版本码（spec §7）。后端据此：
        //   - `X-Device-Id` → ext_user_devices 设备登记 / 吊销判定；
        //   - `app-version`（整数 versionCode）→ 版本门判 426。
        // app-version 必须**始终**发送：后端对缺头/非整数一律按「低于地板」返回 426，
        // 不发头等于自己把自己挡在门外。
        putDeviceHeaders(headers);

        // 鉴权适配：Bearer = 当前登录用户的 mflc_ 登录凭证（由 login 接口签发）。
        String bearer = loginBearer(appApplication);
        if (bearer != null && !bearer.isEmpty()) {
            headers.put("Authorization", "Bearer " + bearer);
        }

        // 防重放
        if (SecurityConfig.isAntiReplayAttackEnabled()) {
            String timestamp = SecurityConfig.getCurrentTimestamp();
            String nonce = SecurityConfig.generateNonce();
            headers.put("X-Timestamp", timestamp);
            headers.put("X-Nonce", nonce);

            String accessKeyId = SecurityConfig.getAccessKeyId();
            String accessKeySecret = SecurityConfig.getAccessKeySecret();
            if (appApplication != null && appApplication.mUserInfoToken != null) {
                String tokenKeyId = appApplication.mUserInfoToken.getAccessKeyId();
                String tokenKeySecret = appApplication.mUserInfoToken.getAccessKeySecret();
                // 仅当用户 token 携带有效 AccessKey 时才覆盖设备默认值（microfeed 的登录响应不含
                // AccessKey，此时保留设备默认值，不影响 timestamp/nonce 的防重放）。
                if (tokenKeyId != null && !tokenKeyId.isEmpty()
                        && tokenKeySecret != null && !tokenKeySecret.isEmpty()) {
                    accessKeyId = tokenKeyId;
                    accessKeySecret = tokenKeySecret;
                    SecurityConfig.setAccessKeyId(accessKeyId);
                    SecurityConfig.setAccessKeySecret(accessKeySecret);
                }
            }
            if (accessKeyId != null && !accessKeyId.isEmpty()
                    && accessKeySecret != null && !accessKeySecret.isEmpty()) {
                String method = RequestHelper.getRequestMethod(api, params);
                String host = RequestHelper.getHost();
                String path = RequestHelper.getPath(api);
                String signature = SecurityConfig.generateSignature(api, method, host, path, timestamp, nonce);
                headers.put("Signature", "Signature " + signature);
                headers.put("X-AccessKeyId", accessKeyId);
            }
        }
    }

    /**
     * 为绕过 EasyHttp 的请求（如 SSE）添加 Bearer + 防重放 Header
     * <p>
     * 当请求无法使用 EasyHttp 拦截器链时（例如 SSE 流式请求直接操作 OkHttp {@link Request.Builder}），
     * 调用此方法手动注入，逻辑与 {@link #handleIntercept} 保持一致。
     * </p>
     */
    public static void addSseSecurityHeaders(Request.Builder builder, IRequestApi api,
                                              String host, String path) {
        // SSE 绕过 EasyHttp 拦截器链，设备/版本头需手动补上，否则该请求缺头 → 426。
        putDeviceHeaders(builder);

        String bearer = loginBearer(AppApplication.application);
        if (bearer != null && !bearer.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + bearer);
        }

        if (SecurityConfig.isAntiReplayAttackEnabled()) {
            String timestamp = SecurityConfig.getCurrentTimestamp();
            String nonce = SecurityConfig.generateNonce();
            builder.addHeader("X-Timestamp", timestamp);
            builder.addHeader("X-Nonce", nonce);

            String accessKeyId = SecurityConfig.getAccessKeyId();
            String accessKeySecret = SecurityConfig.getAccessKeySecret();
            if (AppApplication.application != null && AppApplication.application.mUserInfoToken != null) {
                String tokenKeyId = AppApplication.application.mUserInfoToken.getAccessKeyId();
                String tokenKeySecret = AppApplication.application.mUserInfoToken.getAccessKeySecret();
                if (tokenKeyId != null && !tokenKeyId.isEmpty()
                        && tokenKeySecret != null && !tokenKeySecret.isEmpty()) {
                    accessKeyId = tokenKeyId;
                    accessKeySecret = tokenKeySecret;
                    SecurityConfig.setAccessKeyId(accessKeyId);
                    SecurityConfig.setAccessKeySecret(accessKeySecret);
                }
            }
            if (accessKeyId != null && !accessKeyId.isEmpty()
                    && accessKeySecret != null && !accessKeySecret.isEmpty()) {
                String method = "POST";
                String signature = SecurityConfig.generateSignature(api, method, host, path, timestamp, nonce);
                builder.addHeader("Signature", "Signature " + signature);
                builder.addHeader("X-AccessKeyId", accessKeyId);
            }
        }
    }

    /**
     * 设备/版本头的**唯一取值来源**（EasyHttp 与 SSE 两条路径共用，避免取值漂移）。
     *
     * <p>头名与取值都只在这里定义一次；两个 {@code putDeviceHeaders} 重载只是把它们
     * 分别写进 {@link HttpHeaders} / {@link Request.Builder}。</p>
     */
    private static final String HEADER_DEVICE_ID = "X-Device-Id";

    private static final String HEADER_APP_VERSION = "app-version";

    /** 稳定设备标识（首次启动生成并持久化，重装才变）。 */
    private static String deviceIdValue() {
        return DeviceIdStore.get();
    }

    /** 整数 versionCode；后端整数比较，缺头一律 426。 */
    private static String appVersionValue() {
        return String.valueOf(AppConfig.getVersionCode());
    }

    private static void putDeviceHeaders(HttpHeaders headers) {
        headers.put(HEADER_DEVICE_ID, deviceIdValue());
        headers.put(HEADER_APP_VERSION, appVersionValue());
    }

    private static void putDeviceHeaders(Request.Builder builder) {
        builder.addHeader(HEADER_DEVICE_ID, deviceIdValue());
        builder.addHeader(HEADER_APP_VERSION, appVersionValue());
    }

    /**
     * 当前登录用户携带的 microfeed 登录凭证（{@code mflc_}）；未登录时返回 null。
     * <p>
     * 不再回落到固定设备密钥——AppBookRequest 内容端点需 {@code app:mobile:access}，
     * 该权限必须由登录用户承担（用户 → 角色 → 权限）。
     * </p>
     */
    private static String loginBearer(AppApplication appApplication) {
        // 登录后优先用用户凭证（`mflc_…`，按用户维度签发）。
        if (appApplication != null && appApplication.mUserInfoToken != null
                && appApplication.mUserInfoToken.getToken() != null
                && !appApplication.mUserInfoToken.getToken().isEmpty()) {
            return appApplication.mUserInfoToken.getToken();
        }
        // 未登录时回落到默认凭证（对应 App 原有的「固定设备密钥」兜底模型）。
        return SecurityConfig.getApiKey();
    }
}
