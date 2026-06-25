/*
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.data.remote.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.hjq.http.config.IRequestApi;

import java.io.Serializable;

/**
 * AI 会话 ID 请求 API
 * 
 * <p>接口路径：{@code GetConversationId}
 * <p>请求方法：GET
 * 
 * <h3>响应数据结构：</h3>
 * <pre>
 * {
 *   "ConversationId": "{ \"ConversationId\": \"xxx\", \"RequestId\": \"yyy\" }",
 *   "EndUserId": "user123"
 * }
 * </pre>
 * 
 * <p>注意：{@code ConversationId} 字段可能是纯字符串或嵌套 JSON，
 * {@link Bean#getRealConversationId()} 已处理两种格式的解析。
 * 
 * @see Bean#getRealConversationId()
 * @see Bean#getRequestId()
 */
public final class AiSessionIdApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetConversationId";
    }
    
    /**
     * 获取请求方法
     * 
     * @return HTTP 方法（当前固定为 GET，如需切换请求方法可修改此返回值）
     */
    public String getMethod() {
        return "GET";
    }
    
    /**
     * 会话 ID 响应数据模型
     * 
     * <p>包含会话标识和用户标识，支持延迟解析嵌套 JSON。
     * 
     * <h3>字段说明：</h3>
     * <ul>
     *   <li>{@code conversationIdJson}：原始 JSON 字符串（可能是纯字符串或嵌套 JSON）</li>
     *   <li>{@code endUserId}：用户标识</li>
     *   <li>{@code conversationData}：解析后的会话数据对象（transient，不参与序列化）</li>
     * </ul>
     * 
     * <h3>使用示例：</h3>
     * <pre>
     * AiSessionIdApi.Bean bean = ...;
     * String conversationId = bean.getRealConversationId();  // 自动处理解析
     * String endUserId = bean.getEndUserId();
     * </pre>
     */
    public final static class Bean implements Serializable {
        @SerializedName("ConversationId")
        private String conversationIdJson;
        @SerializedName("EndUserId")
        private String endUserId;
        
        // 延迟解析的会话数据对象
        private transient ConversationData conversationData;
        
        // transient 标记：不参与序列化，每次反序列化后需重新解析
        private transient boolean parsed = false;
        
        public void setConversationId(String conversationIdJson) {
            this.conversationIdJson = conversationIdJson;
            parseConversationData();
        }
        
        public String getConversationId() {
            return conversationIdJson;
        }
        
        /**
         * 获取真实的会话 ID（从嵌套 JSON 中解析）
         * 
         * <p>支持两种格式：
         * <ol>
         *   <li>纯字符串：直接返回</li>
         *   <li>JSON 对象：解析后返回 conversation_id 字段</li>
         * </ol>
         * </p>
         */
        public String getRealConversationId() {
            ensureParsed();
            if (conversationData != null) {
                return conversationData.getConversationId();
            }
            return null;
        }
        
        /**
         * 获取请求 ID（从嵌套 JSON 中解析）
         */
        public String getRequestId() {
            ensureParsed();
            if (conversationData != null) {
                return conversationData.getRequestId();
            }
            return null;
        }
        
        // 确保已解析（懒加载）
        private void ensureParsed() {
            if (!parsed) {
                parseConversationData();
            }
        }
        
        /**
         * 解析 ConversationId 字段
         * 
         * <p>容错处理：
         * <ul>
         *   <li>非 JSON 字符串：直接包装为 ConversationData</li>
         *   <li>JSON 格式：使用 Gson 解析</li>
         *   <li>解析失败：降级为纯字符串处理</li>
         * </ul>
         * </p>
         */
        private void parseConversationData() {
            if (conversationIdJson != null && !conversationIdJson.isEmpty()) {
                try {
                    // 判断是否为 JSON 格式
                    String trimmed = conversationIdJson.trim();
                    if (!trimmed.startsWith("{")) {
                        // 非 JSON 格式：直接包装为 ConversationData
                        this.conversationData = new ConversationData();
                        this.conversationData.setConversationId(conversationIdJson);
                        this.parsed = true;
                        return;
                    }
                    
                    Gson gson = new Gson();
                    this.conversationData = gson.fromJson(conversationIdJson, ConversationData.class);
                    this.parsed = true;
                } catch (JsonSyntaxException e) {
                    // 解析失败：降级为纯字符串处理
                    e.printStackTrace();
                    this.conversationData = new ConversationData();
                    this.conversationData.setConversationId(conversationIdJson);
                    this.parsed = true;
                }
            }
        }

        public void setEndUserId(String endUserId) {
            this.endUserId = endUserId;
        }
        
        public String getEndUserId() {
            return endUserId;
        }
    }
    
    /**
     * 嵌套的会话数据模型
     * 
     * <p>从 {@code ConversationId} 字段的 JSON 中解析得到。
     * 
     * <h3>字段说明：</h3>
     * <ul>
     *   <li>{@code requestId}：请求标识（用于追踪）</li>
     *   <li>{@code conversationId}：真实会话标识</li>
     * </ul>
     */
    public static class ConversationData  implements Serializable{
        private static final long serialVersionUID = 189044334L;
        @SerializedName("RequestId")
        private String requestId;
        
        @SerializedName("ConversationId")
        private String conversationId;

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }
    }
}
