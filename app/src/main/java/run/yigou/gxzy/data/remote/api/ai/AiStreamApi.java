package run.yigou.gxzy.data.remote.api.ai;

import androidx.annotation.NonNull;

import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestHost;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import run.yigou.gxzy.network.security.InterceptorHelper;
import run.yigou.gxzy.network.security.RequestHelper;
import run.yigou.gxzy.sse.SseClient;
import run.yigou.gxzy.sse.SseClientHelper;
import run.yigou.gxzy.sse.SseStreamCallback;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.utils.SerialUtil;

/**
 * AI 流式对话 API（声明式 API 模型）
 * <p>
 * 实现 {@link IRequestApi} / {@link IRequestHost} 接口以声明 API 路径和 Host 地址。
 * SSE 流式执行委派给 {@link SseClient}，安全签名委派给 {@link InterceptorHelper#addSseSecurityHeaders}，
 * 避免将请求构建、签名、SSE 执行等职责揉合在一个类中。
 * </p>
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
            // 适配 microfeed（http://192.168.2.158:4321/）
            return "http://192.168.2.158:4321";
        } else {
            // 正式/预览环境：使用 HTTPS
            return "https://aime.881019.xyz:8443";
        }
    }

    private String buildRequestBody() {
        RequestData data = new RequestData();
        data.conversationId = conversationId;
        data.endUserId = endUserId;
        data.query = query;
        return new Gson().toJson(data);
    }

    /**
     * 构建带有安全签名的 OkHttp Request
     * <p>
     * URL 路径复用 {@link RequestHelper#getPath}，安全签名委派给
     * {@link InterceptorHelper#addSseSecurityHeaders}，避免在此类内重复签名逻辑。
     * </p>
     */
    private Request buildRequest() {
        String jsonBody = buildRequestBody();
        RequestBody body = RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON);

        // RequestHelper.getPath() 返回的路径已包含前置 "/"，直接拼接即可
        String url = getHost() + RequestHelper.getPath(this);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", CONTENT_TYPE)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .addHeader("app", "2")
                .addHeader("SessionId", SerialUtil.getSerial());

        // 安全签名：委派给 InterceptorHelper（与 EasyHttp 拦截器共享同一签名逻辑）
        InterceptorHelper.addSseSecurityHeaders(requestBuilder, this,
                RequestHelper.getHost(), RequestHelper.getPath(this));

        return requestBuilder.build();
    }

    // ========== SSE 流式请求方法 ==========
    
    /**
     * 执行 SSE 流式请求
     * <p>
     * 使用 {@link SseClient} 执行 SSE 事件流监听，消除内联 {@code EventSources.createFactory} 样板代码。
     * </p>
     *
     * @param callback 流式数据回调
     */
    public void execute(@NonNull SseStreamCallback callback) {
        EasyLog.print(TAG, "开始执行 SSE 流式请求");
        
        try {
            // 1. 获取配置好的 OkHttpClient，设置长超时以适配流式响应
            OkHttpClient client = EasyConfig.getInstance()
                    .getClient()
                    .newBuilder()
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            // 2. 配置 TLS 1.2（Android 低版本兼容）
            client = SseClientHelper.configureTls12(client.newBuilder(), getHost());
            
            // 3. 构建请求 (Header, Body, 签名)
            Request request = buildRequest();
            
            // 4. 使用 SseClient 执行 SSE 流式请求（委派 EventSource 创建和管理）
            new SseClient(client, null).execute(request, callback);
            
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
