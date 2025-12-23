package run.yigou.gxzy.http.api;

import androidx.annotation.NonNull;

import run.yigou.gxzy.utils.DebugLog;
import com.google.gson.Gson;
import com.hjq.http.EasyConfig;
import com.hjq.http.EasyLog;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestClient;
import com.hjq.http.config.IRequestHost;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.http.callback.SseStreamCallback;
import run.yigou.gxzy.http.model.SseChunk;
import run.yigou.gxzy.other.AppConfig;

/**
 * AI 流式对话 API
 * 
 * ✅ 实现 EasyHttp 接口，利用拦截器、签名等基础设施
 * ✅ 保持 SSE 流式响应特性
 * 
 * @author Zhs
 * @date 2025-12-17
 */
public final class AiStreamApi implements IRequestApi, IRequestClient, IRequestHost {
    
    private static final String TAG = "AiStreamApi";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
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
        if (run.yigou.gxzy.BuildConfig.DEBUG) {
            // 开发环境：使用 HTTP
            return "http://192.168.2.158:9991";
        } else {
            // 正式/预览环境：使用 HTTPS
            return "https://aime.881019.xyz:8443";
        }
    }
    
    /**
     * 实现 IRequestClient - 返回配置好的 OkHttpClient
     * 
     * ✅ 从 EasyConfig 获取基础 client，自动应用拦截器
     * ✅ 添加 SSE 特定配置（超时时间）
     * ✅ HTTPS 使用 Let's Encrypt 证书，Android 默认信任，无需特殊配置
     */
    @NonNull
    @Override
    public OkHttpClient getClient() {
        EasyLog.print(TAG, "========== 构建 SSE OkHttpClient ==========");
        EasyLog.print(TAG, "当前环境: " + (run.yigou.gxzy.BuildConfig.DEBUG ? "开发(HTTP)" : "正式(HTTPS-Let's Encrypt)"));
        EasyLog.print(TAG, "目标地址: " + getHost());
        
        // ✅ 从 EasyConfig 获取基础 client（自动应用拦截器、签名等）
        OkHttpClient.Builder clientBuilder = EasyConfig.getInstance()
                .getClient()
                .newBuilder()
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS);
        
        EasyLog.print(TAG, "已设置超时：读300秒，写300秒，连接30秒");
        
        // ✅ HTTPS 使用 Let's Encrypt 证书
        // Let's Encrypt 是受信任的 CA，Android 系统默认信任，无需自定义 SSL 配置
        // 系统会自动验证：
        // 1. 证书是否由受信任的 CA 签发 ✓
        // 2. 证书是否在有效期内 ✓
        // 3. 证书域名是否与请求域名匹配 ✓
        
        return clientBuilder.build();
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 构建完整 URL
     */
    private String getFullUrl() {
        String host = getHost();
        if (!host.endsWith("/")) {
            host += "/";
        }
        return host + "api/AppBookRequest/" + getApi();
    }
    
    /**
     * 构建请求 JSON 体
     */
    private String buildRequestBody() {
        RequestData data = new RequestData();
        data.query = this.query;
        data.conversationId = this.conversationId;
        data.endUserId = this.endUserId;
        
        Gson gson = new Gson();
        return gson.toJson(data);
    }
    
    // ========== SSE 流式请求方法 ==========
    
    // 标记是否已主动取消（完成）
    private volatile boolean isCanceled = false;

    /**
     * 执行 SSE 流式请求
     * 
     * ⚠️ 注意：虽然实现了 EasyHttp 接口，但不使用 EasyHttp.post().request() 调用
     * 而是保持自定义的 execute() 方法以支持 SSE 流式回调
     * 
     * @param callback 流式数据回调
     */
    public void execute(@NonNull SseStreamCallback callback) {
        EasyLog.print(TAG, "开始执行 SSE 流式请求");
        EasyLog.print(TAG, "URL: " + getFullUrl());
        EasyLog.print(TAG, "Query: " + query);
        EasyLog.print(TAG, "ConversationId: " + conversationId);
        
        // 重置取消标记
        isCanceled = false;
        
        try {
            // ✅ 使用 getClient() 获取配置好的 OkHttpClient
            //    自动应用 EasyHttp 的拦截器、签名等
            OkHttpClient client = getClient();
            
            // 构建请求体
            String jsonBody = buildRequestBody();
            EasyLog.print(TAG, "请求体: " + jsonBody);
            
            RequestBody body = RequestBody.create(JSON, jsonBody);
            
            // ✅ 手动应用 EasyHttp 拦截器逻辑
            // 构建请求头，模拟 InterceptorHelper.handleIntercept()
            Request.Builder requestBuilder = new Request.Builder()
                    .url(getFullUrl())
                    .post(body);
            
            // 添加基础头部
            requestBuilder.addHeader("Content-Type", "application/json; charset=utf-8");
            requestBuilder.addHeader("Accept", "text/event-stream");
            requestBuilder.addHeader("Cache-Control", "no-cache");
            requestBuilder.addHeader("Connection", "keep-alive");
            
            // ✅ 添加 EasyHttp 拦截器的公共头部
            requestBuilder.addHeader("app", "2");
            requestBuilder.addHeader("SessionId", run.yigou.gxzy.utils.SerialUtil.getSerial());
            
            // ✅ 添加安全签名（如果启用）
            if (run.yigou.gxzy.http.security.SecurityConfig.isAntiReplayAttackEnabled()) {
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
                    
                    // ⚠️ 签名用的 Host 需要去除协议头和末尾斜杠，与 RequestHelper.getHost() 逻辑保持一致
                    String rawHost = getHost();
                    String hostForSign = rawHost;
                    if (hostForSign.startsWith("http://")) {
                        hostForSign = hostForSign.substring(7);
                    } else if (hostForSign.startsWith("https://")) {
                        hostForSign = hostForSign.substring(8);
                    }
                    if (hostForSign.endsWith("/")) {
                        hostForSign = hostForSign.substring(0, hostForSign.length() - 1);
                    }
                    
                    String path = "/api/AppBookRequest/" + getApi();
                    String timestamp = run.yigou.gxzy.http.security.SecurityConfig.getCurrentTimestamp();
                    String nonce = run.yigou.gxzy.http.security.SecurityConfig.generateNonce();
                    
                    EasyLog.print(TAG, "签名参数: method=" + method + ", host=" + hostForSign + ", path=" + path);
                    
                    // ⚠️ 重要：在生成签名前，确保 SecurityConfig 使用正确的密钥
                    run.yigou.gxzy.http.security.SecurityConfig.setAccessKeyId(accessKeyId);
                    run.yigou.gxzy.http.security.SecurityConfig.setAccessKeySecret(accessKeySecret);
                    
                    // 生成签名
                    String signature = run.yigou.gxzy.http.security.SecurityConfig.generateSignature(
                            this, method, hostForSign, path, timestamp, nonce);
                    
                    // 添加安全头部
                    requestBuilder.addHeader("Signature", "Signature " + signature);
                    requestBuilder.addHeader("X-AccessKeyId", accessKeyId);
                    requestBuilder.addHeader("X-Timestamp", timestamp);
                    requestBuilder.addHeader("X-Nonce", nonce);
                    
//                    if (run.yigou.gxzy.http.security.SecurityConfig.isSM2Enabled()) {
//                        requestBuilder.addHeader("X-Encryption-Algorithm", "SM2");
//                    }
                    
                    EasyLog.print(TAG, "✅ 已添加安全签名头部");
                }
            }
            
            Request request = requestBuilder.build();
            
            EasyLog.print(TAG, "请求 URL: " + getFullUrl());
            EasyLog.print(TAG, "请求方法: POST");
            
            // 创建 EventSource
            EventSource.Factory factory = EventSources.createFactory(client);
            EventSource eventSource = factory.newEventSource(request, new EventSourceListener() {
                
                @Override
                public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
                    EasyLog.print(TAG, "SSE 连接已建立");
                    callback.onOpen();
                }
                
                @Override
                public void onEvent(@NonNull EventSource eventSource, String id, String type, @NonNull String data) {
                   // EasyLog.print(TAG, "接收到 SSE 事件: " + data.substring(0, Math.min(data.length(), 100)));
                    
                    try {
                        // 解析 JSON 数据
                        Gson gson = new Gson();
                        SseChunk chunk = gson.fromJson(data, SseChunk.class);
                        
                        if (chunk != null) {
                        //    EasyLog.print(TAG, "解析数据块: type=" + chunk.getType() + ", content length=" +
                        //            (chunk.getContent() != null ? chunk.getContent().length() : 0));
                            callback.onChunk(chunk);
                            
                            // 如果是 done 或 error，关闭连接
                            if ("done".equals(chunk.getType())) {
                                EasyLog.print(TAG, "收到完成信号，关闭连接");
                                isCanceled = true; // ⚠️ 标记为主动取消，防止 onFailure 误报
                                eventSource.cancel();
                                callback.onComplete();
                            } else if ("error".equals(chunk.getType())) {
                                EasyLog.print(TAG, "收到错误信号: " + chunk.getError());
                                isCanceled = true; // 错误也需要取消连接
                                eventSource.cancel();
                                callback.onError(new Exception(chunk.getError()));
                            }
                        } else {
                            EasyLog.print(TAG, "无法解析数据块");
                        }
                    } catch (Exception e) {
                        EasyLog.print(TAG, "解析 SSE 数据失败: " + e.getMessage());
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onClosed(@NonNull EventSource eventSource) {
                    EasyLog.print(TAG, "SSE 连接已关闭");
                }
                
                @Override
                public void onFailure(@NonNull EventSource eventSource, Throwable t, Response response) {
                    // ⚠️ 如果是主动取消导致的 Socket closed，则忽略
                    if (isCanceled && t instanceof java.io.IOException && "Socket closed".equals(t.getMessage())) {
                        EasyLog.print(TAG, "忽略主动取消连接后的 Socket closed");
                        return;
                    }

                    String errorMsg = t != null ? t.getMessage() : "未知错误";
                    EasyLog.print(TAG, "====== SSE 连接失败详情 ======");
                    EasyLog.print(TAG, "错误消息: " + errorMsg);
                    if (t != null) {
                        EasyLog.print(TAG, "异常类型: " + t.getClass().getName());
                        EasyLog.print(TAG, "堆栈跟踪: ");
                        t.printStackTrace();
                    }
                    if (response != null) {
                        EasyLog.print(TAG, "响应码: " + response.code());
                        EasyLog.print(TAG, "响应消息: " + response.message());
                        try {
                            String errorBody = response.body() != null ? response.body().string() : "无响应体";
                            EasyLog.print(TAG, "错误响应体: " + errorBody);
                        } catch (IOException e) {
                            EasyLog.print(TAG, "读取错误响应失败: " + e.getMessage());
                        }
                    } else {
                        EasyLog.print(TAG, "响应为 null");
                    }
                    EasyLog.print(TAG, "================================");
                    callback.onError(new Exception(errorMsg, t));
                }
            });
            
            EasyLog.print(TAG, "EventSource 已创建并启动");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "SSE 请求创建失败: " + e.getMessage());
            e.printStackTrace();
            callback.onError(e);
        }
    }
    
    /**
     * 请求数据模型
     */
    private static class RequestData {
        String conversationId;
        String endUserId;
        String query;
    }
}
