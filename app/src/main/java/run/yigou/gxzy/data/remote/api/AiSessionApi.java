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

import androidx.annotation.NonNull;

import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestHost;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppConfig;

/**
 * ???:  1.0
 * ???: ???sessionId?????GetConversation???AI?????
 *
 */
public final class AiSessionApi implements IRequestApi, IRequestHost {

    private  String host = "http://aime.881019.xyz:8443/";
    @Override
    public String getApi() {
        return "GetConversation";
    }
    // ????????????
    public String getMethod() {
        return "POST"; // ??"POST", "PUT" ??
    }
    private String appId;
    private String query;
    private String conversationId;
    private boolean stream;
    private String endUserId;

    @NonNull
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

    @Override
    public String getHost() {
        if(AppApplication.application.global_openness){
            return host;
        }
       return AppConfig.getHostUrl() + "/";
    }

    public final static class Bean implements Serializable {
                //answer
                // quer
                //status
        private static final long serialVersionUID = 19085553L;
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
