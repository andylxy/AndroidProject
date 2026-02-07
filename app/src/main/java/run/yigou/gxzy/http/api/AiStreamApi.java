package run.yigou.gxzy.http.api;

import androidx.annotation.NonNull;

import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestHost;

import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;
import run.yigou.gxzy.http.callback.SseStreamCallback;
import run.yigou.gxzy.http.sse.AiStreamEventHandler;
import run.yigou.gxzy.http.sse.AiStreamRequestBuilder;
import run.yigou.gxzy.http.sse.SseClientHelper;
import run.yigou.gxzy.other.AppConfig;
import run.yigou.gxzy.utils.EasyLog;

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
            OkHttpClient client = SseClientHelper.createSseClient(getHost());
            
            // 2. 构建请求 (Header, Body, 签名)
            Request request = new AiStreamRequestBuilder(this).build();
            
            // 3. 创建并启动 EventSource
            EventSource.Factory factory = EventSources.createFactory(client);
            AiStreamEventHandler eventHandler = new AiStreamEventHandler(callback);
            
            factory.newEventSource(request, eventHandler);
            
            EasyLog.print(TAG, "EventSource 已创建并启动");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "SSE 请求创建失败: " + e.getMessage());
            e.printStackTrace();
            callback.onError(e);
        }
    }
}
