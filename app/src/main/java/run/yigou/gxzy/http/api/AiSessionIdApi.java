/*
 * 项目名: AndroidProject
 * 类名: CopyApi.java
 * 包名: com.intellij.copyright.JavaCopyrightVariablesProvider$1@516caa2d,qualifiedClassName
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 18:41:20
 * 上次修改时间: 2023年07月05日 17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.hjq.http.config.IRequestApi;

import java.io.Serializable;

/**
 *  版本:  1.0
 *  描述: 在和AI对话前要获取sessionId才可以获取AI的答案
 *
 */
public final class AiSessionIdApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetConversationId";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "GET"; // 或 "POST", "PUT" 等
    }
    public final static class Bean implements Serializable {
        @SerializedName("ConversationId")
        private String conversationIdJson;
        @SerializedName("EndUserId")
        private String endUserId;
        
        // 解析后的对象
        private transient ConversationData conversationData;
        
        // 添加transient关键字，防止该字段被序列化
        private transient boolean parsed = false;
        
        public void setConversationId(String conversationIdJson) {
            this.conversationIdJson = conversationIdJson;
            parseConversationData();
        }
        
        public String getConversationId() {
            return conversationIdJson;
        }
        
        // 获取真正的conversation_id
        public String getRealConversationId() {
            ensureParsed();
            if (conversationData != null) {
                return conversationData.getConversationId();
            }
            return null;
        }
        
        // 获取request_id
        public String getRequestId() {
            ensureParsed();
            if (conversationData != null) {
                return conversationData.getRequestId();
            }
            return null;
        }
        
        // 确保数据已被解析
        private void ensureParsed() {
            if (!parsed) {
                parseConversationData();
            }
        }
        
        // 解析嵌套的JSON字符串
        private void parseConversationData() {
            if (conversationIdJson != null && !conversationIdJson.isEmpty()) {
                try {
                    // 检查是否是有效的 JSON 对象格式（以 { 开头）
                    String trimmed = conversationIdJson.trim();
                    if (!trimmed.startsWith("{")) {
                        // 如果不是 JSON 对象，可能直接是 conversation_id 字符串
                        // 创建一个包含该值的 ConversationData 对象
                        this.conversationData = new ConversationData();
                        this.conversationData.setConversationId(conversationIdJson);
                        this.parsed = true;
                        return;
                    }
                    
                    Gson gson = new Gson();
                    this.conversationData = gson.fromJson(conversationIdJson, ConversationData.class);
                    this.parsed = true;
                } catch (JsonSyntaxException e) {
                    // 解析失败时，尝试将原始字符串作为 conversation_id 使用
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
    
    // 嵌套JSON对象的实体类
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