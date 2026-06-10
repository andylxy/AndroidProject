package run.yigou.gxzy.http.api;

import androidx.annotation.NonNull;

import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestHost;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.http.security.SecurityConfig;
import run.yigou.gxzy.sse.SseStreamCallback;
import run.yigou.gxzy.sse.SseEventHandler;
import run.yigou.gxzy.sse.SseClientHelper;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.utils.SerialUtil;

/**
 * AI 流式对话 API
 * 
 * ✅ 实现 EasyHttp 接口，利用拦截器、签名等基础设施
 * ✅ 保持 SSE 流式响应特性
 * 
 * @author Zhs
 * @date 2025-12-17
 */
public final class AiStreamApi implements IRequestApi, IRequestHost {
    
    private static final String TAG = "AiStreamApi";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final MediaType JSON = MediaType.parse(CONTENT_TYPE);
    
    // 参数字段
    private String query;           // 用户问题
    private String conversationId;  // 会话 ID
    private String endUserId;       // 用户 ID
    
    //  ========== 参数设置方法（链式调用）==========
    
    public AiStreamApi setQuery(String query) {
        this.query = query;
        return this;
    }
    
    public AiStreamApi setConversationId(String conversationId) {
        this.conversationId = conversationId;
        return this;
    }
    
    public AiStreamApi setEndUserId(String endUserId) {
        this.endUserId = endUserId;
        return this;
    }

    // ========== Getter 方法 (供 Builder 使用) ==========

    public String getQuery() {
        return query;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getEndUserId() {
        return endUserId;
    }
    
    // ========== EasyHttp 接口实现 ==========
    
    /**
     * 实现 IRequestApi - 返回 API 路径
     */
    @Override
    public String getApi() {
        return "streamConversation";
    }
    
    /**
     * 实现 IRequestHost - 返回 Host 地址
     * 
     * 开发环境(DEBUG): http://192.168.2.158:9991 (无 SSL)
     * 正式/预览环境: https://aime.881019.xyz:8443 (需要 SSL)
     */
    @Override
    public String getHost() {
        if (Objects.equals(AppConfig.getBuildType(), "debug")) {
            // 开发环境：使用 HTTP
            return "http://192.168.2.158:9991";
        } else {
            // 正式/预览环境：使用 HTTPS
            return "https://aime.881019.xyz:8443";
        }
    }
    
    private String getFullUrl() {
        String host = getHost();
        if (!host.endsWith("/")) {
            host += "/";
        }
        return host + "api/AppBookRequest/" + getApi();
    }

    private String buildRequestBody() {
        RequestData data = new RequestData();
        data.conversationId = conversationId;
        data.endUserId = endUserId;
        data.query = query;
        return new Gson().toJson(data);
    }

    private Request buildSignedRequest() {
        String jsonBody = buildRequestBody();
        RequestBody body = RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(getFullUrl())
                .post(body)
                .addHeader("Content-Type", CONTENT_TYPE)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .addHeader("app", "2")
                .addHeader("SessionId", getSessionIdHeader());

        addSecurityHeaders(requestBuilder);
        return requestBuilder.build();
    }

    private String getSessionIdHeader() {
        String sessionId = SerialUtil.getSerial();
        return sessionId != null ? sessionId : "";
    }

    private void addSecurityHeaders(Request.Builder requestBuilder) {
        if (!SecurityConfig.isAntiReplayAttackEnabled()) {
            return;
        }

        String accessKeyId = SecurityConfig.getAccessKeyId();
        String accessKeySecret = SecurityConfig.getAccessKeySecret();
        if (AppApplication.application != null && AppApplication.application.mUserInfoToken != null) {
            accessKeyId = AppApplication.application.mUserInfoToken.getAccessKeyId();
            accessKeySecret = AppApplication.application.mUserInfoToken.getAccessKeySecret();
        }

        if (accessKeyId == null || accessKeyId.isEmpty() || accessKeySecret == null || accessKeySecret.isEmpty()) {
            EasyLog.print(TAG, "SSE 请求缺少移动端登录签名凭证");
            return;
        }

        String method = "POST";
        String path = "/api/AppBookRequest/" + getApi();
        String timestamp = SecurityConfig.getCurrentTimestamp();
        String nonce = SecurityConfig.generateNonce();
        String hostForSign = getHostForSign();

        SecurityConfig.setAccessKeyId(accessKeyId);
        SecurityConfig.setAccessKeySecret(accessKeySecret);
        String signature = SecurityConfig.generateSignature(this, method, hostForSign, path, timestamp, nonce);

        requestBuilder.addHeader("Signature", "Signature " + signature);
        requestBuilder.addHeader("X-AccessKeyId", accessKeyId);
        requestBuilder.addHeader("X-Timestamp", timestamp);
        requestBuilder.addHeader("X-Nonce", nonce);
    }

    private String getHostForSign() {
        String host = getHost();
        if (host.startsWith("http://")) {
            host = host.substring(7);
        } else if (host.startsWith("https://")) {
            host = host.substring(8);
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }

    // ========== SSE 流式请求方法 ==========
    
    /**
     * 执行 SSE 流式请求
     * 
     * @param callback 流式数据回调
     */
    public void execute(@NonNull SseStreamCallback callback) {
        EasyLog.print(TAG, "开始执行 SSE 流式请求");
        
        try {
            // 1. 获取配置好的 OkHttpClient (TLS 1.2 等)
            OkHttpClient client = EasyConfig.getInstance()
                    .getClient()
                    .newBuilder()
                    .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            
            // 2. 配置 TLS 1.2
            client = SseClientHelper.configureTls12(client.newBuilder(), getHost());
            
            // 3. 构建请求 (Header, Body, 签名)
            Request request = buildSignedRequest();
            
            // 4. 创建并启动 EventSource
            EventSource.Factory factory = EventSources.createFactory(client);
            SseEventHandler eventHandler = new SseEventHandler(callback);
            factory.newEventSource(request, eventHandler);
            
            EasyLog.print(TAG, "EventSource 已创建并启动");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "SSE 请求创建失败: " + e.getMessage());
            e.printStackTrace();
            callback.onError(e);
        }
    }

    private static class RequestData {
        @com.google.gson.annotations.SerializedName("conversationId")
        String conversationId;
        @com.google.gson.annotations.SerializedName("endUserId")
        String endUserId;
        @com.google.gson.annotations.SerializedName("query")
        String query;
    }
}
