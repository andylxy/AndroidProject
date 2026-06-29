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
import run.yigou.gxzy.log.EasyLog;

/**
 * 请求拦截辅助类
 * <p>
 * 为 EasyHttp 请求提供统一的安全签名 Header 注入（{@link #handleIntercept}），
 * 同时为绕过 EasyHttp 拦截器链的 SSE 等请求提供签名辅助（{@link #addSseSecurityHeaders}）。
 * 签名逻辑集中于此处，避免分散到各 API 类中重复实现。
 * </p>
 */
public class InterceptorHelper {

    /**
     * EasyHttp 请求拦截器核心方法
     * <p>
     * 为所有 EasyHttp 请求统一注入公共 Header（app、SessionId、Content-Type、Accept），
     * 并在防重放攻击启用时计算并注入签名 Header（Signature、X-AccessKeyId、X-Timestamp、X-Nonce）。
     * 签名逻辑与 {@link #addSseSecurityHeaders} 保持一致，避免分散到各 API 类中重复实现。
     * </p>
     *
     * @param api            IRequestApi 实例，用于获取 API 路径和请求方法
     * @param params         请求参数
     * @param headers        请求 Header 集合，签名 Header 将注入此处
     * @param appApplication 应用上下文，用于获取用户 Token 中的 AccessKey 凭证
     */
    public static void handleIntercept(IRequestApi api, HttpParams params, HttpHeaders headers, AppApplication appApplication) {

        // 设置公共 Header
        //if (appApplication.global_openness && appApplication.mUserInfoToken == null)
        //    headers.put("Authorization", AppConst.AllowAnonymous_Token);
        headers.put("app", "2");
        headers.put("SessionId", SerialUtil.getSerial());
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/plain, */*");

        // 防重放攻击签名
        if (SecurityConfig.isAntiReplayAttackEnabled()) {
            String accessKeyId = SecurityConfig.getAccessKeyId();
            String accessKeySecret = SecurityConfig.getAccessKeySecret();
            // 用户登录后，使用用户 Token 中的 AccessKeyId 和 AccessKeySecret 覆盖默认凭证
            if (appApplication.mUserInfoToken != null){
                accessKeyId = appApplication.mUserInfoToken.getAccessKeyId();
                accessKeySecret = appApplication.mUserInfoToken.getAccessKeySecret();
                // 同步到 SecurityConfig，确保签名计算使用最新凭证
                SecurityConfig.setAccessKeyId(accessKeyId);
                SecurityConfig.setAccessKeySecret(accessKeySecret);
            }
            // 校验 AccessKey 凭证是否有效
            if (accessKeyId != null && !accessKeyId.isEmpty() &&
                    accessKeySecret != null && !accessKeySecret.isEmpty()) {

                // 获取请求方法
                String method = RequestHelper.getRequestMethod(api, params);
                String host = RequestHelper.getHost();
                String path = RequestHelper.getPath(api);
                
                // 生成时间戳和 Nonce（防止重放攻击）
                String timestamp = SecurityConfig.getCurrentTimestamp();
                String nonce = SecurityConfig.generateNonce();

                // 计算签名
                String signature = SecurityConfig.generateSignature(api, method, host, path, timestamp, nonce);

                // 注入签名 Header
                headers.put("Signature", "Signature " + signature);
                headers.put("X-AccessKeyId", accessKeyId);
                headers.put("X-Timestamp", timestamp);
                headers.put("X-Nonce", nonce);
                
                // 如启用 SM2 国密算法，注入算法标识
                if (SecurityConfig.isSM2Enabled()) {
                    headers.put("X-Encryption-Algorithm", "SM2");
                }
            }
        }
    }

    /**
     * 为绕过 EasyHttp 的请求（如 SSE）添加安全签名 Header
     * <p>
     * 当请求无法使用 EasyHttp 拦截器链时（例如 SSE 流式请求直接操作 OkHttp {@link Request.Builder}），
     * 调用此方法手动注入签名 Header，签名逻辑与 {@link #handleIntercept} 保持一致。
     * </p>
     *
     * @param builder OkHttp Request.Builder，签名 Header 将添加到该构建器
     * @param api     IRequestApi 实例（用于 {@link SecurityConfig#generateSignature} 签名计算）
     * @param host    请求主机（不含 scheme，例如 "aime.881019.xyz:8443"）
     * @param path    请求路径（例如 "/api/AppBookRequest/streamConversation"）
     */
    public static void addSseSecurityHeaders(Request.Builder builder, IRequestApi api,
                                              String host, String path) {
        if (!SecurityConfig.isAntiReplayAttackEnabled()) {
            return;
        }

        String accessKeyId = SecurityConfig.getAccessKeyId();
        String accessKeySecret = SecurityConfig.getAccessKeySecret();
        if (AppApplication.application != null && AppApplication.application.mUserInfoToken != null) {
            accessKeyId = AppApplication.application.mUserInfoToken.getAccessKeyId();
            accessKeySecret = AppApplication.application.mUserInfoToken.getAccessKeySecret();
        }

        if (accessKeyId == null || accessKeyId.isEmpty()
                || accessKeySecret == null || accessKeySecret.isEmpty()) {
            EasyLog.print("InterceptorHelper", "SSE 请求缺少移动端登录签名凭证");
            return;
        }

        String method = "POST";
        String timestamp = SecurityConfig.getCurrentTimestamp();
        String nonce = SecurityConfig.generateNonce();

        SecurityConfig.setAccessKeyId(accessKeyId);
        SecurityConfig.setAccessKeySecret(accessKeySecret);
        String signature = SecurityConfig.generateSignature(api, method, host, path, timestamp, nonce);

        builder.addHeader("Signature", "Signature " + signature);
        builder.addHeader("X-AccessKeyId", accessKeyId);
        builder.addHeader("X-Timestamp", timestamp);
        builder.addHeader("X-Nonce", nonce);
    }
}
