package run.yigou.gxzy.sse;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import run.yigou.gxzy.log.EasyLog;

/**
 * AI 流式事件处理器
 * 负责处理 SSE 连接的生命周期事件和数据解析
 */
public class SseEventHandler extends EventSourceListener {

    private static final String TAG = "SseEventHandler";

    private final SseStreamCallback callback;
    private volatile boolean isCanceled = false;
    private final SseLogger logger;

    public SseEventHandler(@NonNull SseStreamCallback callback) {
        this(callback, null);
    }

    public SseEventHandler(@NonNull SseStreamCallback callback, SseLogger logger) {
        this.callback = callback;
        this.logger = logger;
    }

    @Override
    public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
        log("SSE 连接已建立");
        callback.onOpen();
    }

    @Override
    public void onEvent(@NonNull EventSource eventSource, String id, String type, @NonNull String data) {
        try {
            // 解析 JSON 数据
            Gson gson = new Gson();
            SseChunk chunk = gson.fromJson(data, SseChunk.class);

            if (chunk != null) {
                log("解析数据块: type=" + chunk.getType() + ", content length=" +
                        (chunk.getContent() != null ? chunk.getContent().length() : 0));

                // 打印具体内容，用于调试
                if (chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                    log("Content: " + chunk.getContent());
                }

                callback.onChunk(chunk);

                // 如果是 done 或 error，关闭连接
                if ("done".equals(chunk.getType())) {
                    log("收到完成信号，关闭连接");
                    isCanceled = true; // ⚠️ 标记为主动取消，防止 onFailure 误报
                    eventSource.cancel();
                    callback.onComplete();
                } else if ("error".equals(chunk.getType())) {
                    log("收到错误信号: " + chunk.getError());
                    isCanceled = true; // 错误也需要取消连接
                    eventSource.cancel();
                    // 错误回调可能会触发 UI 操作，不要在 onFailure 中重复触发
                    callback.onError(new Exception(chunk.getError()));
                }
            } else {
                log("无法解析数据块");
            }
        } catch (Exception e) {
            log("解析 SSE 数据失败: " + e.getMessage());
            callback.onError(e);
        }
    }

    @Override
    public void onClosed(@NonNull EventSource eventSource) {
        log("SSE 连接已关闭");
    }

    @Override
    public void onFailure(@NonNull EventSource eventSource, Throwable t, Response response) {
        // ⚠️ 如果是主动取消导致的错误，则忽略
        // 增加对 "canceled" 消息的检查
        if (isCanceled || (t != null && "canceled".equals(t.getMessage()))) {
            log("忽略主动取消连接后的错误 (isCanceled=" + isCanceled + ", msg=" + (t != null ? t.getMessage() : "null") + ")");
            return;
        }

        String errMsg = t != null ? t.getMessage() : "";
        if (t instanceof java.io.IOException && "Socket closed".equals(errMsg)) {
            log("忽略主动取消连接后的 Socket closed");
            return;
        }
        // 检查 StreamResetException: CANCEL
        if (errMsg != null && errMsg.contains("CANCEL")) {
            log("忽略主动取消连接后的 stream was reset: CANCEL");
            return;
        }
        // 检查 cause 是否是 StreamResetException
        if (t != null && t.getCause() != null) {
            String causeMsg = t.getCause().getMessage();
            if (causeMsg != null && causeMsg.contains("CANCEL")) {
                log("忽略主动取消连接后的 StreamResetException: CANCEL");
                return;
            }
        }

        String errorMsg = t != null ? t.getMessage() : "未知错误";
        log("====== SSE 连接失败详情 ======");
        log("错误消息: " + errorMsg);
        if (t != null) {
            log("异常类型: " + t.getClass().getName());
            log("堆栈跟踪: ");
            t.printStackTrace();
        }
        if (response != null) {
            log("响应码: " + response.code());
            log("响应消息: " + response.message());
        } else {
            log("响应为 null");
        }
        log("================================");
        callback.onError(new Exception(errorMsg, t));
    }

    /**
     * 标记为主动取消
     */
    public void cancel() {
        isCanceled = true;
    }

    /**
     * 日志输出（优先使用注入的 logger，否则使用 EasyLog）
     */
    private void log(String message) {
        if (logger != null) {
            logger.log(TAG, message);
        } else {
            EasyLog.print(TAG, message);
        }
    }
}
