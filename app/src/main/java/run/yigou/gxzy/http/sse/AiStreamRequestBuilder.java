package run.yigou.gxzy.http.sse;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.http.api.AiStreamApi;
import run.yigou.gxzy.utils.EasyLog;
import run.yigou.gxzy.utils.SerialUtil;

/**
 * AI 流式请求构建器
 * 负责构建 Request 对象，包括请求体生成、Header 添加和签名逻辑
 */
public class AiStreamRequestBuilder {

    private static final String TAG = "AiStreamRequestBuilder";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final AiStreamApi api;

    public AiStreamRequestBuilder(AiStreamApi api) {
        this.api = api;
    }

    public Request build() {
        String fullUrl = getFullUrl();
        EasyLog.print(TAG, "构建请求 URL: " + fullUrl);

        // 构建请求体
        String jsonBody = buildRequestBody();
        EasyLog.print(TAG, "请求体: " + jsonBody);
        
        RequestBody body = RequestBody.create(JSON, jsonBody);
        
        // 构建请求头
        Request.Builder requestBuilder = new Request.Builder()
                .url(fullUrl)
                .post(body);
        
        // 添加基础头部
        requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
        requestBuilder.addHeader("Accept", "text/event-stream");
        requestBuilder.addHeader("Cache-Control", "no-cache");
        requestBuilder.addHeader("Connection", "keep-alive");
        
        // ✅ 添加 EasyHttp 拦截器的公共头部
        requestBuilder.addHeader("app", "2");
        requestBuilder.addHeader("SessionId", SerialUtil.getSerial());
        
        // ✅ 添加安全签名
        addSignatureHeaders(requestBuilder);
        
        return requestBuilder.build();
    }

    private String getFullUrl() {
        String host = api.getHost();
        if (!host.endsWith("/")) {
            host += "/";
        }
        return host + "api/AppBookRequest/" + api.getApi();
    }

    private String buildRequestBody() {
        RequestData data = new RequestData();
        data.query = api.getQuery();
        data.conversationId = api.getConversationId();
        data.endUserId = api.getEndUserId();
        
        Gson gson = new Gson();
        return gson.toJson(data);
    }

    private void addSignatureHeaders(Request.Builder requestBuilder) {
        if (!run.yigou.gxzy.http.security.SecurityConfig.isAntiReplayAttackEnabled()) {
            return;
        }

        String accessKeyId = run.yigou.gxzy.http.security.SecurityConfig.getAccessKeyId();
        String accessKeySecret = run.yigou.gxzy.http.security.SecurityConfig.getAccessKeySecret();
        
        // 使用登录用户的密钥
        if (AppApplication.application.mUserInfoToken != null) {
            accessKeyId = AppApplication.application.mUserInfoToken.getAccessKeyId();
            accessKeySecret = AppApplication.application.mUserInfoToken.getAccessKeySecret();
        }
        
        if (accessKeyId != null && !accessKeyId.isEmpty() && 
            accessKeySecret != null && !accessKeySecret.isEmpty()) {
            
            // 生成签名参数
            String method = "POST";
            
            // ⚠️ 签名用的 Host 需要去除协议头和末尾斜杠
            String rawHost = api.getHost();
            String hostForSign = rawHost;
            if (hostForSign.startsWith("http://")) {
                hostForSign = hostForSign.substring(7);
            } else if (hostForSign.startsWith("https://")) {
                hostForSign = hostForSign.substring(8);
            }
            if (hostForSign.endsWith("/")) {
                hostForSign = hostForSign.substring(0, hostForSign.length() - 1);
            }
            
            String path = "/api/AppBookRequest/" + api.getApi();
            String timestamp = run.yigou.gxzy.http.security.SecurityConfig.getCurrentTimestamp();
            String nonce = run.yigou.gxzy.http.security.SecurityConfig.generateNonce();
            
            EasyLog.print(TAG, "签名参数: method=" + method + ", host=" + hostForSign + ", path=" + path);
            
            // ⚠️ 重要：在生成签名前，确保 SecurityConfig 使用正确的密钥
            run.yigou.gxzy.http.security.SecurityConfig.setAccessKeyId(accessKeyId);
            run.yigou.gxzy.http.security.SecurityConfig.setAccessKeySecret(accessKeySecret);
            
            // 生成签名
            String signature = run.yigou.gxzy.http.security.SecurityConfig.generateSignature(
                    api, method, hostForSign, path, timestamp, nonce);
            
            // 添加安全头部
            requestBuilder.addHeader("Signature", "Signature " + signature);
            requestBuilder.addHeader("X-AccessKeyId", accessKeyId);
            requestBuilder.addHeader("X-Timestamp", timestamp);
            requestBuilder.addHeader("X-Nonce", nonce);
            
            EasyLog.print(TAG, "✅ 已添加安全签名头部");
        }
    }
    
    /**
     * 请求数据模型
     */
    private static class RequestData {
        @com.google.gson.annotations.SerializedName("conversationId")
        String conversationId;
        @com.google.gson.annotations.SerializedName("endUserId")
        String endUserId;
        @com.google.gson.annotations.SerializedName("query")
        String query;
    }
}
