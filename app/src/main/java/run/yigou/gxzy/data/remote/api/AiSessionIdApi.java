/*
 * ????? AndroidProject
 * ???: CopyApi.java
 * ???: com.intellij.copyright.JavaCopyrightVariablesProvider$1@516caa2d,qualifiedClassName
 * ????: Zhs (xiaoyang_02@qq.com)
 * ????????? : 2023??7??5??18:41:20
 * ?????????: 2023??7??5??17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.data.remote.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.hjq.http.config.IRequestApi;

import java.io.Serializable;

/**
 *  版本:  1.0
 *  描述: 获取 AI 会话 sessionId 接口
 *
 */
public final class AiSessionIdApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetConversationId";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
    public final static class Bean implements Serializable {
        @SerializedName("ConversationId")
        private String conversationIdJson;
        @SerializedName("EndUserId")
        private String endUserId;
        
        // ?????????
        private transient ConversationData conversationData;
        
        // ???transient????????????????????
        private transient boolean parsed = false;
        
        public void setConversationId(String conversationIdJson) {
            this.conversationIdJson = conversationIdJson;
            parseConversationData();
        }
        
        public String getConversationId() {
            return conversationIdJson;
        }
        
        // ????????onversation_id
        public String getRealConversationId() {
            ensureParsed();
            if (conversationData != null) {
                return conversationData.getConversationId();
            }
            return null;
        }
        
        // ???request_id
        public String getRequestId() {
            ensureParsed();
            if (conversationData != null) {
                return conversationData.getRequestId();
            }
            return null;
        }
        
        // ????????????
        private void ensureParsed() {
            if (!parsed) {
                parseConversationData();
            }
        }
        
        // ????????SON?????
        private void parseConversationData() {
            if (conversationIdJson != null && !conversationIdJson.isEmpty()) {
                try {
                    // ?????????????JSON ????????? { ?????
                    String trimmed = conversationIdJson.trim();
                    if (!trimmed.startsWith("{")) {
                        // ?????? JSON ???????????? conversation_id ?????
                        // ?????????????? ConversationData ???
                        this.conversationData = new ConversationData();
                        this.conversationData.setConversationId(conversationIdJson);
                        this.parsed = true;
                        return;
                    }
                    
                    Gson gson = new Gson();
                    this.conversationData = gson.fromJson(conversationIdJson, ConversationData.class);
                    this.parsed = true;
                } catch (JsonSyntaxException e) {
                    // ???????????????????????? conversation_id ???
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
    
    // ???JSON?????????
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
