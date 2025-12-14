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

import androidx.annotation.NonNull;

import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestClient;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * 版本:  1.0
 * 描述: 获取sessionId后通过GetConversation获取AI的答案
 *
 */
public final class AiSessionApi implements IRequestApi, IRequestClient {

    @Override
    public String getApi() {
        return "GetConversation";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "POST"; // 或 "POST", "PUT" 等
    }
    private String appId;
    private String query;
    private String conversationId;
    private boolean stream;
    private String endUserId;

    @NonNull
    @Override
    public OkHttpClient getClient() {
        OkHttpClient.Builder builder = EasyConfig.getInstance().getClient().newBuilder();
        builder.readTimeout(300, TimeUnit.SECONDS);
        builder.writeTimeout(300, TimeUnit.SECONDS);
        builder.connectTimeout(300, TimeUnit.SECONDS);
        return builder.build();
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public AiSessionApi setQuery(String query) {
        this.query = query;
        return this;
    }

    public AiSessionApi setConversationId(String conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    public AiSessionApi setStream(boolean stream) {
        this.stream = stream;
        return this;
    }

    public AiSessionApi setEndUserId(String endUserId) {
        this.endUserId = endUserId;
        return this;
    }

    public final static class Bean implements Serializable {
                //answer
                // quer
                //status
        private String answer;
        private String query;
        private boolean status;

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public boolean isStatus() {
            return status;
        }

        public void setStatus(boolean status) {
            this.status = status;
        }
    }
}