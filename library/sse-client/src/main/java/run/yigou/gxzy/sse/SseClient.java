package run.yigou.gxzy.sse;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;

/**
 * SSE 客户端门面类
 * 提供简化的 SSE 连接入口
 * 
 * 使用示例：
 * <pre>
 * OkHttpClient client = EasyConfig.getInstance().getClient().newBuilder()
 *         .readTimeout(300, TimeUnit.SECONDS)
 *         .build();
 * 
 * client = SseClientHelper.configureTls12(client.newBuilder(), host);
 * 
 * SseClient sseClient = new SseClient(client, null);
 * sseClient.execute(request, callback);
 * </pre>
 */
public final class SseClient {

    private final OkHttpClient okHttpClient;
    private final SseLogger logger;

    public SseClient(OkHttpClient okHttpClient, SseLogger logger) {
        this.okHttpClient = okHttpClient;
        this.logger = logger;
    }

    /**
     * 执行 SSE 流式请求
     *
     * @param request  已构建好的 OkHttp Request
     * @param callback SSE 数据回调
     */
    public void execute(@NonNull Request request, @NonNull SseStreamCallback callback) {
        if (logger != null) {
            logger.log("SseClient", "开始执行 SSE 流式请求: " + request.url());
        }

        try {
            // 创建 EventSource 工厂
            EventSource.Factory factory = EventSources.createFactory(okHttpClient);
            
            // 创建事件处理器
            SseEventHandler eventHandler = new SseEventHandler(callback, logger);
            
            // 启动 EventSource
            factory.newEventSource(request, eventHandler);
            
            if (logger != null) {
                logger.log("SseClient", "EventSource 已创建并启动");
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.log("SseClient", "EventSource 创建失败: " + e.getMessage());
            }
            callback.onError(new Exception("EventSource 创建失败", e));
        }
    }
}
