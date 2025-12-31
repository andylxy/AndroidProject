package run.yigou.gxzy.http.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * SSE 数据块实体
 * 表示从服务器接收到的单个 SSE 事件数据
 * 
 * @author Zhs
 * @date 2025-12-17
 */
public class SseChunk implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 数据块类型
     * 可能的值：
     * - "start": 开始流式传输
     * - "chunk": 普通内容块
     * - "thinking": AI 思考过程
     * - "answer": AI 正式回答
     * - "done": 传输完成
     * - "error": 发生错误
     */
    @SerializedName("type")
    private String type;
    
    /**
     * 数据块的文本内容
     * 对于 "chunk", "thinking", "answer" 类型，包含实际文本
     */
    @SerializedName("content")
    private String content;
    
    /**
     * 错误信息
     * 仅当 type 为 "error" 时有值
     */
    @SerializedName("error")
    private String error;
    
    /**
     * 是否为思考过程
     * true: 表示这是 AI 的思考过程，通常需要折叠显示
     * false: 表示这是正式回答内容
     */
    @SerializedName("isThinking")
    private boolean isThinking;
    
    // 构造函数
    public SseChunk() {
    }
    
    public SseChunk(String type, String content) {
        this.type = type;
        this.content = content;
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public boolean isThinking() {
        return isThinking;
    }
    
    public void setThinking(boolean thinking) {
        isThinking = thinking;
    }
    
    @Override
    public String toString() {
        return "SseChunk{" +
                "type='" + type + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) : "null") + "...'" +
                ", error='" + error + '\'' +
                ", isThinking=" + isThinking +
                '}';
    }
}
