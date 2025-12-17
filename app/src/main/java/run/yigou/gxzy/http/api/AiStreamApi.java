package run.yigou.gxzy.http.api;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.hjq.http.EasyConfig;

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
 * 使用 SSE (Server-Sent Events) 实现实时流式接收 AI 回复
 * 
 * @author Zhs
 * @date 2025-12-17
 */
public class AiStreamApi {
    
    private static final String TAG = "AiStreamApi";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    // API 配置
    private String host = "http://aime.881019.xyz:8443/";
    private String query;           // 用户问题
    private String conversationId;  // 会话 ID
    private String endUserId;       // 用户 ID
    
    // Getters and Setters (链式调用)
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
    
    /**
     * 获取 Host 地址
     */
    private String getHost() {
        if (AppApplication.application.global_openness) {
            return host;
        }
        return AppConfig.getHostUrl();
    }
    
    /**
     * 获取完整的 API URL
     */
    private String getFullUrl() {
        return getHost() + "api/AppBookRequest/streamConversation";
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
    
    /**
     * 执行 SSE 流式请求
     * 
     * @param callback 流式数据回调
     */
    public void execute(@NonNull SseStreamCallback callback) {
        LogUtils.d(TAG, "开始执行 SSE 流式请求");
        LogUtils.d(TAG, "URL: " + getFullUrl());
        LogUtils.d(TAG, "Query: " + query);
        LogUtils.d(TAG, "ConversationId: " + conversationId);
        
        try {
            // 构建 OkHttpClient（设置超时）
            OkHttpClient client = EasyConfig.getInstance().getClient().newBuilder()
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            // 构建请求体
            String jsonBody = buildRequestBody();
            LogUtils.d(TAG, "请求体: " + jsonBody);
            
            RequestBody body = RequestBody.create(JSON, jsonBody);
            
            // 构建 Request
            Request request = new Request.Builder()
                    .url(getFullUrl())
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .build();
            
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
                                eventSource.cancel();
                                callback.onComplete();
                            } else if ("error".equals(chunk.getType())) {
                                LogUtils.e(TAG, "收到错误信号: " + chunk.getError());
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
                    LogUtils.e(TAG, "SSE 连接失败: " + t.getMessage());
                    if (response != null) {
                        LogUtils.e(TAG, "响应码: " + response.code());
                        try {
                            String errorBody = response.body() != null ? response.body().string() : "";
                            LogUtils.e(TAG, "错误响应: " + errorBody);
                        } catch (IOException e) {
                            LogUtils.e(TAG, "读取错误响应失败: " + e.getMessage());
                        }
                    }
                    callback.onError(new Exception(t));
                }
            });
            
            LogUtils.d(TAG, "EventSource 已创建并启动");
            
        } catch (Exception e) {
            LogUtils.e(TAG, "创建 SSE 请求失败: " + e.getMessage());
            callback.onError(e);
        }
    }
    
    /**
     * 请求数据实体（内部使用）
     */
    private static class RequestData {
        String query;
        String conversationId;
        String endUserId;
    }
}
