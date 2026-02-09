package run.yigou.gxzy.manager;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LifecycleOwner;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.http.listener.OnHttpListener;

import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.entity.ChatSessionBean;
import run.yigou.gxzy.http.api.AiSessionIdApi;
import run.yigou.gxzy.http.api.AiStreamApi;
import run.yigou.gxzy.http.callback.SseStreamCallback;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.http.model.SseChunk;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.EasyLog;

import java.util.List;

/**
 * AI 聊天管理器
 * 负责处理网络请求、SSE 流式通信及消息状态流转
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
     * 聊天流式响应监听器
     */
    public interface ChatStreamListener {
        void onThinking(String content);
        void onAnswerStart(ChatMessageBean answerMessage);
        void onAnswerChunk(String content);
        void onError(String error);
        void onComplete();
    }

    /**
     * 申请会话ID回调
     */
    public interface SessionIdCallback {
        void onSuccess(String conversationId, String endUserId);
        void onFailure(String error);
    }

    /**
     * 会话检查回调
     */
    public interface SessionCheckCallback {
        void onSessionValid(ChatSessionBean session);
        void onFailure(String error);
    }

    /**
     * 检查会话有效性并执行（如果无效会自动尝试修复/刷新）
     * 
     * @param lifecycleOwner 生命周期所有者
     * @param session 当前会话
     * @param callback 检查结果回调
     */
    public void checkSessionAndExecute(LifecycleOwner lifecycleOwner, final ChatSessionBean session, final SessionCheckCallback callback) {
        if (session == null) {
            callback.onFailure("会话为空");
            return;
        }

        boolean needRequest = false;

        // 1. 检查会话ID是否存在
        if (session.getConversationId() == null || 
            session.getConversationId().isEmpty() ||
            session.getEndUserId() == null ||
            session.getEndUserId().isEmpty()) {
            needRequest = true;
            EasyLog.print(TAG, "Session missing conversationId or endUserId, need request");
        } 
        // 2. 检查会话是否过期（6天）
        else if (session.getCreateTime() != null) {
            long createTime = DateHelper.strDateToLong(session.getCreateTime());
            long currentTime = System.currentTimeMillis();
            // 6天 = 6 * 24 * 60 * 60 * 1000 毫秒
            if (currentTime - createTime > 6 * 24 * 60 * 60 * 1000L) {
                needRequest = true;
                EasyLog.print(TAG, "Session expired, need request");
            }
        } else {
            // createTime 为空，视为无效，需要重新申请
            needRequest = true;
            EasyLog.print(TAG, "Session createTime is null, need request");
        }

        if (needRequest) {
            requestSessionId(lifecycleOwner, new SessionIdCallback() {
                @Override
                public void onSuccess(String conversationId, String endUserId) {
                    // 更新会话信息
                    session.setConversationId(conversationId);
                    session.setEndUserId(endUserId);
                    session.setCreateTime(DateHelper.getSeconds1());
                    
                    // 保存更新到数据库
                    ChatSessionManager.getInstance().updateSession(session);
                    EasyLog.print(TAG, "Session ID refreshed: " + conversationId);
                    
                    // 回调成功
                    callback.onSessionValid(session);
                }

                @Override
                public void onFailure(String error) {
                    EasyLog.print(TAG, "Failed to refresh session ID: " + error);
                    callback.onFailure(error);
                }
            });
        } else {
            // 会话有效，直接回调
            callback.onSessionValid(session);
        }
    }

    /**
     * 申请或刷新会话ID
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
                                callback.onFailure("返回数据为空");
                            }
                        } else {
                            callback.onFailure(data != null ? data.getMessage() : "请求失败");
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
     * 开始新会话（创建本地对象并申请ID）
     */
    public void startNewSession(LifecycleOwner lifecycleOwner, final SessionCheckCallback callback) {
        // 1. 创建本地会话
        final ChatSessionBean newSession = ChatSessionManager.getInstance().createLocalSession("新对话", "开始新的对话");
        
        // 2. 申请ID
        requestSessionId(lifecycleOwner, new SessionIdCallback() {
            @Override
            public void onSuccess(String conversationId, String endUserId) {
                // 3. 更新会话信息
                newSession.setConversationId(conversationId);
                newSession.setEndUserId(endUserId);
                newSession.setCreateTime(DateHelper.getSeconds1());
                
                ChatSessionManager.getInstance().updateSession(newSession);
                EasyLog.print(TAG, "New Session started: " + conversationId);
                
                // 4. 回调
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
     * 发送消息（SSE流式）
     *
     * @param session 当前会话
     * @param query 用户问题（包含可能的总结上下文）
     * @param thinkingMessage 思考消息对象（用于更新思考状态）
     * @param listener 回调监听
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
                        EasyLog.print(TAG, "SSE 连接已建立");
                    }

                    @Override
                    public void onChunk(SseChunk chunk) {
                        if (chunk == null) return;

                        // 判断是否是思考过程
                        boolean isThinkingChunk = "thinking".equals(chunk.getType()) || chunk.isThinking();

                        if (isThinkingChunk) {
                            // === 处理思考过程 ===
                            String content = chunk.getContent() != null ? chunk.getContent() : "";
                            // 更新内存对象
                            String current = thinkingMessage.getContent();
                            if ("正在思考...".equals(current)) {
                                current = "";
                            }
                            thinkingMessage.setContent(current + content);
                            
                            // 回调 UI
                            listener.onThinking(content);

                        } else if ("chunk".equals(chunk.getType()) || "answer".equals(chunk.getType())) {
                            // === 处理正式回答 ===
                            
                            // 如果是第一次从思考切换到回答
                            if (isThinkingPhase[0]) {
                                isThinkingPhase[0] = false;

                                // 1. 结束思考阶段
                                thinkingMessage.setThinkingCollapsed(true);
                                ChatSessionManager.getInstance().updateMessage(thinkingMessage);

                                // 2. 创建正式回答消息
                                ChatMessageBean answerMsg = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED, "Ai", "", "");
                                answerMsg.setSessionId(session.getId());
                                answerMsg.setCreateDate(DateHelper.getSeconds1());
                                answerMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                answerMsg.setStreaming(true);

                                long answerId = ChatSessionManager.getInstance().saveMessage(answerMsg);
                                answerMsg.setId(answerId);
                                answerMessageRef[0] = answerMsg;

                                // 通知 UI 开始回答
                                listener.onAnswerStart(answerMsg);
                            }

                            // 追加回答内容
                            if (answerMessageRef[0] != null && chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                                String content = chunk.getContent();
                                String current = answerMessageRef[0].getContent();
                                answerMessageRef[0].setContent(current + content);
                                
                                listener.onAnswerChunk(content);
                            }

                        } else if ("error".equals(chunk.getType())) {
                            String error = chunk.getError();
                            EasyLog.print(TAG, "SSE 错误: " + error);
                            
                            // 记录错误到消息中
                            if (answerMessageRef[0] != null) {
                                answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n[错误: " + error + "]");
                            } else {
                                thinkingMessage.setContent(thinkingMessage.getContent() + "\n[错误: " + error + "]");
                            }
                            
                            listener.onError(error);
                        }
                    }

                    @Override
                    public void onComplete() {
                        EasyLog.print(TAG, "SSE 流式对话完成");
                        
                        // 最终保存
                        if (answerMessageRef[0] != null) {
                            answerMessageRef[0].setStreaming(false);
                            ChatSessionManager.getInstance().updateMessage(answerMessageRef[0]);
                            
                            // 更新会话预览
                            String preview = answerMessageRef[0].getContent();
                            if (preview.length() > 30) {
                                preview = preview.substring(0, 30) + "...";
                            }
                            session.setPreview("AI: " + preview);
                            session.setUpdateTime(DateHelper.getSeconds1());
                            ChatSessionManager.getInstance().updateSession(session);
                        } else {
                            // 如果没有生成回答（只有思考或出错），保存思考消息
                            ChatSessionManager.getInstance().updateMessage(thinkingMessage);
                        }
                        
                        listener.onComplete();
                    }

                    @Override
                    public void onError(Exception e) {
                        EasyLog.print(TAG, "SSE 请求失败: " + e.getMessage());
                        
                        String errStr = "网络请求失败: " + e.getMessage();
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
     * 生成总结
     */
    public void generateSummary(ChatSessionBean session, String prompt, final ChatStreamListener listener) {
        new AiStreamApi()
                .setQuery(prompt)
                .setConversationId(session.getConversationId())
                .setEndUserId(session.getEndUserId())
                .execute(new SseStreamCallback() {
                    @Override
                    public void onOpen() {
                        EasyLog.print(TAG, "总结 SSE 连接已建立");
                    }

                    @Override
                    public void onChunk(SseChunk chunk) {
                        if (chunk != null && chunk.getContent() != null) {
                            if (!"thinking".equals(chunk.getType()) && !chunk.isThinking()) {
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
     * 构建总结 Prompt
     */
    public String generateSummaryPrompt(List<ChatMessageBean> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请对以下对话内容进行总结，提取关键要点：\n\n");
        
        for (ChatMessageBean message : messages) {
            if (message.getType() == ChatMessageBean.TYPE_SEND) {
                promptBuilder.append("用户：").append(message.getContent()).append("\n");
            } else if (message.getType() == ChatMessageBean.TYPE_RECEIVED) {
                promptBuilder.append("AI：").append(message.getContent()).append("\n");
            }
        }
        
        promptBuilder.append("\n请用简洁的语言总结上述对话的主要内容和关键信息。");
        return promptBuilder.toString();
    }
}
