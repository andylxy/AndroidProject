package run.yigou.gxzy.http.callback;

import run.yigou.gxzy.http.model.SseChunk;

/**
 * SSE 流式请求回调接口
 * 用于处理 Server-Sent Events 流式数据传输
 * 
 * @author Zhs
 * @date 2025-12-17
 */
public interface SseStreamCallback {
    
    /**
     * 连接成功建立时回调
     * 在 SSE 连接成功打开时触发
     */
    void onOpen();
    
    /**
     * 接收到流式数据块时回调
     * 每次接收到来自服务器的数据块时触发
     * 
     * @param chunk SSE 数据块，包含类型和内容
     */
    void onChunk(SseChunk chunk);
    
    /**
     * 流式传输完成时回调
     * 当服务器发送完所有数据并关闭连接时触发
     */
    void onComplete();
    
    /**
     * 流式传输发生错误时回调
     * 包括网络错误、解析错误、服务器错误等
     * 
     * @param e 异常对象
     */
    void onError(Exception e);
}
