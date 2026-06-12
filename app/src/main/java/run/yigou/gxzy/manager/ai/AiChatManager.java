package run.yigou.gxzy.manager.ai;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LifecycleOwner;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.http.listener.OnHttpListener;

import run.yigou.gxzy.data.local.entity.ChatMessageBean;
import run.yigou.gxzy.data.local.entity.ChatSessionBean;
import run.yigou.gxzy.data.remote.api.AiSessionIdApi;
import run.yigou.gxzy.data.remote.api.AiStreamApi;
import run.yigou.gxzy.sse.SseStreamCallback;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.sse.SseChunk;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.log.EasyLog;

import java.util.List;

/**
 * AI ?????
 * ?????????SSE ???????????
 */
public class AiChatManager {
    private static final String TAG = "AiChatManager";
    private static volatile AiChatManager instance;

    private AiChatManager() {
    }

    public static AiChatManager getInstance() {
        if (instance == null) {
            synchronized (AiChatManager.class) {
                if (instance == null) {
                    instance = new AiChatManager();
                }
            }
        }
        return instance;
    }

    /**
     * ?????????
     */
    public interface ChatStreamListener {
        void onThinking(String content);
        void onAnswerStart(ChatMessageBean answerMessage);
        void onAnswerChunk(String content);
        void onError(String error);
        void onComplete();
    }

    /**
     * ????ID??
     */
    public interface SessionIdCallback {
        void onSuccess(String conversationId, String endUserId);
        void onFailure(String error);
    }

    /**
     * ??????
     */
    public interface SessionCheckCallback {
        void onSessionValid(ChatSessionBean session);
        void onFailure(String error);
    }

    /**
     * ??????????????????????/???
     * 
     * @param lifecycleOwner ???????
     * @param session ????
     * @param callback ??????
     */
    public void checkSessionAndExecute(LifecycleOwner lifecycleOwner, final ChatSessionBean session, final SessionCheckCallback callback) {
        if (session == null) {
            callback.onFailure("????");
            return;
        }

        boolean needRequest = false;

        // 1. ????ID????
        if (session.getConversationId() == null || 
            session.getConversationId().isEmpty() ||
            session.getEndUserId() == null ||
            session.getEndUserId().isEmpty()) {
            needRequest = true;
            EasyLog.print(TAG, "Session missing conversationId or endUserId, need request");
        } 
        // 2. ?????????6??
        else if (session.getCreateTime() != null) {
            long createTime = DateHelper.strDateToLong(session.getCreateTime());
            long currentTime = System.currentTimeMillis();
            // 6? = 6 * 24 * 60 * 60 * 1000 ??
            if (currentTime - createTime > 6 * 24 * 60 * 60 * 1000L) {
                needRequest = true;
                EasyLog.print(TAG, "Session expired, need request");
            }
        } else {
            // createTime ??????????????
            needRequest = true;
            EasyLog.print(TAG, "Session createTime is null, need request");
        }

        if (needRequest) {
            requestSessionId(lifecycleOwner, new SessionIdCallback() {
                @Override
                public void onSuccess(String conversationId, String endUserId) {
                    // ??????
                    session.setConversationId(conversationId);
                    session.setEndUserId(endUserId);
                    session.setCreateTime(DateHelper.getSeconds1());
                    
                    // ????????
                    ChatSessionManager.getInstance().updateSession(session);
                    EasyLog.print(TAG, "Session ID refreshed: " + conversationId);
                    
                    // ????
                    callback.onSessionValid(session);
                }

                @Override
                public void onFailure(String error) {
                    EasyLog.print(TAG, "Failed to refresh session ID: " + error);
                    callback.onFailure(error);
                }
            });
        } else {
            // ?????????
            callback.onSessionValid(session);
        }
    }

    /**
     * ???????ID
     */
    public void requestSessionId(LifecycleOwner lifecycleOwner, final SessionIdCallback callback) {
        EasyHttp.get(lifecycleOwner)
                .api(new AiSessionIdApi())
                .request(new HttpCallback<HttpData<AiSessionIdApi.Bean>>((OnHttpListener) lifecycleOwner) {
                    @Override
                    public void onSucceed(HttpData<AiSessionIdApi.Bean> data) {
                        if (data != null && data.isRequestSucceed()) {
                            AiSessionIdApi.Bean bean = data.getData();
                            if (bean != null) {
                                callback.onSuccess(bean.getRealConversationId(), bean.getEndUserId());
                            } else {
                                callback.onFailure("??????");
                            }
                        } else {
                            callback.onFailure(data != null ? data.getMessage() : "????");
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * ???????????????ID?
     */
    public void startNewSession(LifecycleOwner lifecycleOwner, final SessionCheckCallback callback) {
        // 1. ??????
        final ChatSessionBean newSession = ChatSessionManager.getInstance().createLocalSession("???", "??????");
        
        // 2. ??ID
        requestSessionId(lifecycleOwner, new SessionIdCallback() {
            @Override
            public void onSuccess(String conversationId, String endUserId) {
                // 3. ??????
                newSession.setConversationId(conversationId);
                newSession.setEndUserId(endUserId);
                newSession.setCreateTime(DateHelper.getSeconds1());
                
                ChatSessionManager.getInstance().updateSession(newSession);
                EasyLog.print(TAG, "New Session started: " + conversationId);
                
                // 4. ??
                callback.onSessionValid(newSession);
            }

            @Override
            public void onFailure(String error) {
                EasyLog.print(TAG, "Failed to start new session: " + error);
                callback.onFailure(error);
            }
        });
    }

    /**
     * ?????SSE???
     *
     * @param session ????
     * @param query ????????????????
     * @param thinkingMessage ????????????????
     * @param listener ????
     */
    public void sendMessage(final ChatSessionBean session, String query, 
                           final ChatMessageBean thinkingMessage, 
                           final ChatStreamListener listener) {
        
        final ChatMessageBean[] answerMessageRef = {null};
        final boolean[] isThinkingPhase = {true};

        new AiStreamApi()
                .setQuery(query)
                .setConversationId(session.getConversationId())
                .setEndUserId(session.getEndUserId())
                .execute(new SseStreamCallback() {
                    @Override
                    public void onOpen() {
                        EasyLog.print(TAG, "SSE ?????");
                    }

                    @Override
                    public void onChunk(SseChunk chunk) {
                        if (chunk == null) return;

                        // ?????????
                        boolean isThinkingChunk = "thinking".equals(chunk.getType()) || chunk.isThinking();

                        if (isThinkingChunk) {
                            // === ?????? ===
                            String content = chunk.getContent() != null ? chunk.getContent() : "";
                            // ??????
                            String current = thinkingMessage.getContent();
                            if ("????...".equals(current)) {
                                current = "";
                            }
                            thinkingMessage.setContent(current + content);
                            
                            // ?? UI
                            listener.onThinking(content);

                        } else if ("chunk".equals(chunk.getType()) || "answer".equals(chunk.getType())) {
                            // === ?????? ===
                            
                            // ??????????????
                            if (isThinkingPhase[0]) {
                                isThinkingPhase[0] = false;

                                // 1. ??????
                                thinkingMessage.setThinkingCollapsed(true);
                                ChatSessionManager.getInstance().updateMessage(thinkingMessage);

                                // 2. ????????
                                ChatMessageBean answerMsg = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED, "Ai", "", "");
                                answerMsg.setSessionId(session.getId());
                                answerMsg.setCreateDate(DateHelper.getSeconds1());
                                answerMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                answerMsg.setStreaming(true);

                                long answerId = ChatSessionManager.getInstance().saveMessage(answerMsg);
                                answerMsg.setId(answerId);
                                answerMessageRef[0] = answerMsg;

                                // ?? UI ????
                                listener.onAnswerStart(answerMsg);
                            }

                            // ??????
                            if (answerMessageRef[0] != null && chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                                String content = chunk.getContent();
                                String current = answerMessageRef[0].getContent();
                                answerMessageRef[0].setContent(current + content);
                                
                                listener.onAnswerChunk(content);
                            }

                        } else if ("error".equals(chunk.getType())) {
                            String error = chunk.getError();
                            EasyLog.print(TAG, "SSE ??: " + error);
                            
                            // ????????
                            if (answerMessageRef[0] != null) {
                                answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n[??: " + error + "]");
                            } else {
                                thinkingMessage.setContent(thinkingMessage.getContent() + "\n[??: " + error + "]");
                            }
                            
                            listener.onError(error);
                        }
                    }

                    @Override
                    public void onComplete() {
                        EasyLog.print(TAG, "SSE ??????");
                        
                        // ????
                        if (answerMessageRef[0] != null) {
                            answerMessageRef[0].setStreaming(false);
                            ChatSessionManager.getInstance().updateMessage(answerMessageRef[0]);
                            
                            // ??????
                            String preview = answerMessageRef[0].getContent();
                            if (preview.length() > 30) {
                                preview = preview.substring(0, 30) + "...";
                            }
                            session.setPreview("AI: " + preview);
                            session.setUpdateTime(DateHelper.getSeconds1());
                            ChatSessionManager.getInstance().updateSession(session);
                        } else {
                            // ????????????????????????
                            ChatSessionManager.getInstance().updateMessage(thinkingMessage);
                        }
                        
                        listener.onComplete();
                    }

                    @Override
                    public void onError(Exception e) {
                        EasyLog.print(TAG, "SSE ????: " + e.getMessage());
                        
                        String errStr = "??????: " + e.getMessage();
                        if (answerMessageRef[0] != null) {
                            answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n" + errStr);
                            answerMessageRef[0].setStreaming(false);
                            ChatSessionManager.getInstance().updateMessage(answerMessageRef[0]);
                        } else {
                            thinkingMessage.setContent(thinkingMessage.getContent() + "\n" + errStr);
                            thinkingMessage.setStreaming(false);
                            ChatSessionManager.getInstance().updateMessage(thinkingMessage);
                        }
                        
                        listener.onError(e.getMessage());
                    }
                });
    }

    /**
     * ????
     */
    public void generateSummary(ChatSessionBean session, String prompt, final ChatStreamListener listener) {
        new AiStreamApi()
                .setQuery(prompt)
                .setConversationId(session.getConversationId())
                .setEndUserId(session.getEndUserId())
                .execute(new SseStreamCallback() {
                    @Override
                    public void onOpen() {
                        EasyLog.print(TAG, "?? SSE ?????");
                    }

                    @Override
                    public void onChunk(SseChunk chunk) {
                        if (chunk != null && chunk.getContent() != null) {
                            if ("thinking".equals(chunk.getType()) || chunk.isThinking()) {
                                listener.onThinking(chunk.getContent());
                            } else {
                                listener.onAnswerChunk(chunk.getContent());
                            }
                        }
                    }

                    @Override
                    public void onComplete() {
                        listener.onComplete();
                    }

                    @Override
                    public void onError(Exception e) {
                        listener.onError(e.getMessage());
                    }
                });
    }

    /**
     * ???? Prompt
     */
    public String generateSummaryPrompt(List<ChatMessageBean> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("????????????????????\n\n");
        
        for (ChatMessageBean message : messages) {
            if (message.getType() == ChatMessageBean.TYPE_SEND) {
                promptBuilder.append("???").append(message.getContent()).append("\n");
            } else if (message.getType() == ChatMessageBean.TYPE_RECEIVED) {
                promptBuilder.append("AI?").append(message.getContent()).append("\n");
            }
        }
        
        promptBuilder.append("\n????????????????????????");
        return promptBuilder.toString();
    }
}
