package run.yigou.gxzy.http.api;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.hjq.http.EasyConfig;
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
     */
    @Override
    public String getHost() {
        // 开发环境使用测试地址，生产环境使用配置地址
        if (AppApplication.application.global_openness) {
            return "http://192.168.2.158:9991";
        }
        return AppConfig.getHostUrl();
    }
    
    /**
     * 实现 IRequestClient - 返回配置好的 OkHttpClient
     * 
     * ✅ 从 EasyConfig 获取基础 client，自动应用拦截器
     * ✅ 添加 SSE 特定配置（SSL、超时）
     */
    @NonNull
    @Override
    public OkHttpClient getClient() {
        LogUtils.d(TAG, "========== 构建 SSE OkHttpClient ==========");
        
        // ✅ 从 EasyConfig 获取基础 client（自动应用拦截器、签名等）
        OkHttpClient.Builder clientBuilder = EasyConfig.getInstance()
                .getClient()
                .newBuilder()
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS);
        
        LogUtils.d(TAG, "已设置超时：读300秒，写300秒，连接30秒");
        
        // ⚠️ 开发环境：强制信任所有 SSL 证书（仅用于测试）
        try {
            LogUtils.d(TAG, "========== 配置 SSL 信任 ==========");
            
            // 创建信任所有证书的 TrustManager
            final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        LogUtils.d(TAG, "✅ checkClientTrusted - 跳过验证");
                    }
                    
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        LogUtils.d(TAG, "✅ checkServerTrusted - 跳过验证");
                    }
                    
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };
            
            // 安装信任所有证书的 SSLContext
            final javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            
            LogUtils.d(TAG, "SSLContext 协议: " + sslContext.getProtocol());
            
            clientBuilder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            clientBuilder.hostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                @Override
                public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                    LogUtils.d(TAG, "✅ hostnameVerifier - 跳过验证: " + hostname);
                    return true;
                }
            });
            
            LogUtils.d(TAG, "========== ✅ SSL 信任配置完成 ==========");
        } catch (Exception e) {
            LogUtils.e(TAG, "❌ SSL 配置失败: " + e.getMessage());
            e.printStackTrace();
        }
        
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
        LogUtils.d(TAG, "开始执行 SSE 流式请求");
        LogUtils.d(TAG, "URL: " + getFullUrl());
        LogUtils.d(TAG, "Query: " + query);
        LogUtils.d(TAG, "ConversationId: " + conversationId);
        
        // 重置取消标记
        isCanceled = false;
        
        try {
            // ✅ 使用 getClient() 获取配置好的 OkHttpClient
            //    自动应用 EasyHttp 的拦截器、签名等
            OkHttpClient client = getClient();
            
            // 构建请求体
            String jsonBody = buildRequestBody();
            LogUtils.d(TAG, "请求体: " + jsonBody);
            
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
                    
                    LogUtils.d(TAG, "签名参数: method=" + method + ", host=" + hostForSign + ", path=" + path);
                    
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
                    
                    LogUtils.d(TAG, "✅ 已添加安全签名头部");
                }
            }
            
            Request request = requestBuilder.build();
            
            LogUtils.d(TAG, "请求 URL: " + getFullUrl());
            LogUtils.d(TAG, "请求方法: POST");
            
            // 创建 EventSource
            EventSource.Factory factory = EventSources.createFactory(client);
            EventSource eventSource = factory.newEventSource(request, new EventSourceListener() {
                
                @Override
                public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
                    LogUtils.d(TAG, "SSE 连接已建立");
                    callback.onOpen();
                }
                
                @Override
                public void onEvent(@NonNull EventSource eventSource, String id, String type, @NonNull String data) {
                    LogUtils.d(TAG, "接收到 SSE 事件: " + data.substring(0, Math.min(data.length(), 100)));
                    
                    try {
                        // 解析 JSON 数据
                        Gson gson = new Gson();
                        SseChunk chunk = gson.fromJson(data, SseChunk.class);
                        
                        if (chunk != null) {
                            LogUtils.d(TAG, "解析数据块: type=" + chunk.getType() + ", content length=" + 
                                    (chunk.getContent() != null ? chunk.getContent().length() : 0));
                            callback.onChunk(chunk);
                            
                            // 如果是 done 或 error，关闭连接
                            if ("done".equals(chunk.getType())) {
                                LogUtils.d(TAG, "收到完成信号，关闭连接");
                                isCanceled = true; // ⚠️ 标记为主动取消，防止 onFailure 误报
                                eventSource.cancel();
                                callback.onComplete();
                            } else if ("error".equals(chunk.getType())) {
                                LogUtils.e(TAG, "收到错误信号: " + chunk.getError());
                                isCanceled = true; // 错误也需要取消连接
                                eventSource.cancel();
                                callback.onError(new Exception(chunk.getError()));
                            }
                        } else {
                            LogUtils.w(TAG, "无法解析数据块");
                        }
                    } catch (Exception e) {
                        LogUtils.e(TAG, "解析 SSE 数据失败: " + e.getMessage());
                        callback.onError(e);
                    }
                }
                
                @Override
                public void onClosed(@NonNull EventSource eventSource) {
                    LogUtils.d(TAG, "SSE 连接已关闭");
                }
                
                @Override
                public void onFailure(@NonNull EventSource eventSource, Throwable t, Response response) {
                    // ⚠️ 如果是主动取消导致的 Socket closed，则忽略
                    if (isCanceled && t instanceof java.io.IOException && "Socket closed".equals(t.getMessage())) {
                        LogUtils.d(TAG, "忽略主动取消连接后的 Socket closed");
                        return;
                    }

                    String errorMsg = t != null ? t.getMessage() : "未知错误";
                    LogUtils.e(TAG, "====== SSE 连接失败详情 ======");
                    LogUtils.e(TAG, "错误消息: " + errorMsg);
                    if (t != null) {
                        LogUtils.e(TAG, "异常类型: " + t.getClass().getName());
                        LogUtils.e(TAG, "堆栈跟踪: ");
                        t.printStackTrace();
                    }
                    if (response != null) {
                        LogUtils.e(TAG, "响应码: " + response.code());
                        LogUtils.e(TAG, "响应消息: " + response.message());
                        try {
                            String errorBody = response.body() != null ? response.body().string() : "无响应体";
                            LogUtils.e(TAG, "错误响应体: " + errorBody);
                        } catch (IOException e) {
                            LogUtils.e(TAG, "读取错误响应失败: " + e.getMessage());
                        }
                    } else {
                        LogUtils.e(TAG, "响应为 null");
                    }
                    LogUtils.e(TAG, "================================");
                    callback.onError(new Exception(errorMsg, t));
                }
            });
            
            LogUtils.d(TAG, "EventSource 已创建并启动");
            
        } catch (Exception e) {
            LogUtils.e(TAG, "SSE 请求创建失败: " + e.getMessage());
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
