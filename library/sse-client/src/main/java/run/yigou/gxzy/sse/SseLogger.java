package run.yigou.gxzy.sse;

/**
 * SSE 日志接口
 * 允许调用方注入自定义日志实现
 */
public interface SseLogger {
    /**
     * 输出日志
     *
     * @param tag    日志标签
     * @param message 日志消息
     */
    void log(String tag, String message);
}
