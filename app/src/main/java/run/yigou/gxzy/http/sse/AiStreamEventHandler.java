package run.yigou.gxzy.http.sse;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import run.yigou.gxzy.http.callback.SseStreamCallback;
import run.yigou.gxzy.http.model.SseChunk;
import run.yigou.gxzy.utils.EasyLog;

/**
 * AI 流式事件处理器
 * 负责处理 SSE 连接的生命周期事件和数据解析
 */
public class AiStreamEventHandler extends EventSourceListener {

    private static final String TAG = "AiStreamEventHandler";
    
    private final SseStreamCallback callback;
    private volatile boolean isCanceled = false;

    public AiStreamEventHandler(@NonNull SseStreamCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
        EasyLog.print(TAG, "SSE 连接已建立");
        callback.onOpen();
    }

    @Override
    public void onEvent(@NonNull EventSource eventSource, String id, String type, @NonNull String data) {
        try {
            // 解析 JSON 数据
            Gson gson = new Gson();
            SseChunk chunk = gson.fromJson(data, SseChunk.class);
            
            if (chunk != null) {
                EasyLog.print(TAG, "解析数据块: type=" + chunk.getType() + ", content length=" +
                        (chunk.getContent() != null ? chunk.getContent().length() : 0));
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
                    // 错误回调可能会触发 UI 操作，不要在 onFailure 中重复触发
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
        // ⚠️ 如果是主动取消导致的错误，则忽略
        // 增加对 "canceled" 消息的检查
        if (isCanceled || (t != null && "canceled".equals(t.getMessage()))) {
            EasyLog.print(TAG, "忽略主动取消连接后的错误 (isCanceled=" + isCanceled + ", msg=" + (t != null ? t.getMessage() : "null") + ")");
            return;
        }
        
        String errMsg = t != null ? t.getMessage() : "";
        if (t instanceof java.io.IOException && "Socket closed".equals(errMsg)) {
            EasyLog.print(TAG, "忽略主动取消连接后的 Socket closed");
            return;
        }
        // 检查 StreamResetException: CANCEL
        if (errMsg != null && errMsg.contains("CANCEL")) {
            EasyLog.print(TAG, "忽略主动取消连接后的 stream was reset: CANCEL");
            return;
        }
        // 检查 cause 是否是 StreamResetException
        if (t != null && t.getCause() != null) {
            String causeMsg = t.getCause().getMessage();
            if (causeMsg != null && causeMsg.contains("CANCEL")) {
                EasyLog.print(TAG, "忽略主动取消连接后的 StreamResetException: CANCEL");
                return;
            }
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
        } else {
            EasyLog.print(TAG, "响应为 null");
        }
        EasyLog.print(TAG, "================================");
        callback.onError(new Exception(errorMsg, t));
    }
    
    /**
     * 标记为主动取消
     */
    public void cancel() {
        isCanceled = true;
    }
}
