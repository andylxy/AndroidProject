package run.yigou.gxzy.manager.ai;

import androidx.lifecycle.LifecycleOwner;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.http.listener.OnHttpListener;

import run.yigou.gxzy.data.local.entity.ChatMessageBean;
import run.yigou.gxzy.data.local.entity.ChatSessionBean;
import run.yigou.gxzy.data.remote.api.ai.AiSessionIdApi;
import run.yigou.gxzy.data.remote.api.ai.AiStreamApi;
import run.yigou.gxzy.sse.SseStreamCallback;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.sse.SseChunk;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.data.Callback;

import java.util.List;

/**
 * AI 聊天管理器
 * <p>
 * 基于 SSE 流式响应的聊天管理，负责：
 * <ul>
 *   <li>会话创建与会话 ID 管理</li>
 *   <li>消息发送与流式响应处理</li>
 *   <li>会话总结生成</li>
 * </ul>
 * </p>
 * <p>
 * 回调分层设计：
 * <ul>
 *   <li>简单操作（会话检查/创建/ID请求）：使用标准 {@link Callback}</li>
 *   <li>流式操作（消息发送/总结生成）：使用 {@link ChatStreamListener}</li>
 * </ul>
 * </p>
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
     * 聊天流式监听器（SSE 流式响应专用）
     */
    public interface ChatStreamListener {
        void onThinking(String content);
        void onAnswerStart(ChatMessageBean answerMessage);
        void onAnswerChunk(String content);
        void onError(Exception e);
        void onComplete();
    }

    /**
     * 检查会话有效性并执行回调
     * <p>
     * 检查逻辑：
     * <ol>
     *   <li>会话 ID 缺失（conversationId 或 endUserId 为空）</li>
     *   <li>会话过期（创建时间超过6天）</li>
     *   <li>创建时间为 null</li>
     * </ol>
     * </p>
     *
     * @param lifecycleOwner 生命周期持有者，用于绑定网络请求
     * @param session 当前会话
     * @param callback 回调接口，会话有效时返回 session
     */
    public void checkSessionAndExecute(LifecycleOwner lifecycleOwner, final ChatSessionBean session, final Callback<ChatSessionBean> callback) {
        if (session == null) {
            callback.onError(new IllegalArgumentException("会话为空"));
            return;
        }

        boolean needRequest = false;

        // 1. 检查会话 ID 是否有效
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
            // createTime 为 null，需要重新获取
            needRequest = true;
            EasyLog.print(TAG, "Session createTime is null, need request");
        }

        if (needRequest) {
            requestSessionId(lifecycleOwner, new Callback<AiSessionIdApi.Bean>() {
                @Override
                public void onSuccess(AiSessionIdApi.Bean bean) {
                    // 更新会话信息
                    session.setConversationId(bean.getRealConversationId());
                    session.setEndUserId(bean.getEndUserId());
                    session.setCreateTime(DateHelper.getSeconds1());
                    
                    // 保存到数据库
                    ChatSessionManager.getInstance().updateSession(session);
                    EasyLog.print(TAG, "Session ID refreshed: " + bean.getRealConversationId());
                    
                    // 回调通知
                    callback.onSuccess(session);
                }

                @Override
                public void onError(Exception e) {
                    EasyLog.print(TAG, "Failed to refresh session ID: " + e.getMessage());
                    callback.onError(e);
                }
            });
        } else {
            // 会话有效，直接回调
            callback.onSuccess(session);
        }
    }

    /**
     * 请求会话 ID（内部方法）
     * 
     * <p>仅在 {@link #checkSessionAndExecute} 和 {@link #startNewSession} 中调用，
     * 不对外暴露。
     * 
     * @param lifecycleOwner 生命周期持有者，用于绑定网络请求
     * @param callback 回调接口，成功时返回 AiSessionIdApi.Bean
     */
    void requestSessionId(LifecycleOwner lifecycleOwner, final Callback<AiSessionIdApi.Bean> callback) {
        EasyHttp.get(lifecycleOwner)
                .api(new AiSessionIdApi())
                .request(new HttpCallback<HttpData<AiSessionIdApi.Bean>>((OnHttpListener) lifecycleOwner) {
                    @Override
                    public void onSucceed(HttpData<AiSessionIdApi.Bean> data) {
                        if (data != null && data.isRequestSucceed() && data.getData() != null) {
                            callback.onSuccess(data.getData());
                        } else {
                            callback.onError(new IllegalStateException("响应数据为空"));
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        callback.onError(e);
                    }
                });
    }

    /**
     * 创建新会话
     * <p>
     * 流程：
     * <ol>
     *   <li>创建本地会话对象</li>
     *   <li>请求服务器获取会话 ID</li>
     *   <li>更新会话信息到数据库</li>
     *   <li>回调通知调用方</li>
     * </ol>
     * </p>
     *
     * @param lifecycleOwner 生命周期持有者
     * @param callback 回调接口，创建成功时返回新会话
     */
    public void startNewSession(LifecycleOwner lifecycleOwner, final Callback<ChatSessionBean> callback) {
        // 1. 创建本地会话
        final ChatSessionBean newSession = ChatSessionManager.getInstance().createLocalSession("用户: ", "新对话");
        
        // 2. 请求会话 ID
        requestSessionId(lifecycleOwner, new Callback<AiSessionIdApi.Bean>() {
            @Override
            public void onSuccess(AiSessionIdApi.Bean bean) {
                // 3. 更新会话信息
                newSession.setConversationId(bean.getRealConversationId());
                newSession.setEndUserId(bean.getEndUserId());
                newSession.setCreateTime(DateHelper.getSeconds1());
                
                ChatSessionManager.getInstance().updateSession(newSession);
                EasyLog.print(TAG, "New Session started: " + bean.getRealConversationId());
                
                // 4. 回调通知
                callback.onSuccess(newSession);
            }

            @Override
            public void onError(Exception e) {
                EasyLog.print(TAG, "Failed to start new session: " + e.getMessage());
                callback.onError(e);
            }
        });
    }

    /**
     * 发送消息（SSE 流式）
     * <p>
     * 流式响应阶段：
     * <ol>
     *   <li>思考阶段：onThinking() - AI 正在思考</li>
     *   <li>回答阶段：onAnswerStart() → onAnswerChunk() - AI 开始回答并持续输出</li>
     *   <li>完成阶段：onComplete() - 流式传输完成</li>
     *   <li>错误处理：onError() - 网络或服务端错误</li>
     * </ol>
     * </p>
     *
     * @param session 当前会话
     * @param query 用户发送的消息内容
     * @param thinkingMessage 思考中的消息对象（用于更新思考内容）
     * @param listener 流式响应监听器
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
                        EasyLog.print(TAG, "SSE 连接已打开");
                    }

                    @Override
                    public void onChunk(SseChunk chunk) {
                        if (chunk == null) return;

                        // 判断是否为思考阶段的数据块
                        boolean isThinkingChunk = "thinking".equals(chunk.getType()) || chunk.isThinking();

                        if (isThinkingChunk) {
                            // === 处理思考阶段数据 ===
                            String content = chunk.getContent() != null ? chunk.getContent() : "";
                            String current = thinkingMessage.getContent();
                            if ("思考中...".equals(current)) {
                                current = "";
                            }
                            thinkingMessage.setContent(current + content);
                            
                            // 通知 UI 更新思考内容
                            listener.onThinking(content);

                        } else if ("chunk".equals(chunk.getType()) || "answer".equals(chunk.getType())) {
                            // === 处理回答阶段数据 ===
                            
                            // 从思考阶段切换到回答阶段
                            if (isThinkingPhase[0]) {
                                isThinkingPhase[0] = false;

                                // 1. 折叠思考消息
                                thinkingMessage.setThinkingCollapsed(true);
                                ChatSessionManager.getInstance().updateMessage(thinkingMessage);

                                // 2. 创建回答消息
                                ChatMessageBean answerMsg = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED, "Ai", "", "");
                                answerMsg.setSessionId(session.getId());
                                answerMsg.setCreateDate(DateHelper.getSeconds1());
                                answerMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                answerMsg.setStreaming(true);

                                long answerId = ChatSessionManager.getInstance().saveMessage(answerMsg);
                                answerMsg.setId(answerId);
                                answerMessageRef[0] = answerMsg;

                                // 通知 UI 开始接收回答
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
                            
                            // 更新消息内容显示错误
                            if (answerMessageRef[0] != null) {
                                answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n[错误: " + error + "]");
                            } else {
                                thinkingMessage.setContent(thinkingMessage.getContent() + "\n[错误: " + error + "]");
                            }
                            
                            listener.onError(new RuntimeException(error));
                        }
                    }

                    @Override
                    public void onComplete() {
                        EasyLog.print(TAG, "SSE 流式传输完成");
                        
                        // 回调通知
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
                            // 只有思考阶段，更新思考消息
                            ChatSessionManager.getInstance().updateMessage(thinkingMessage);
                        }
                        
                        listener.onComplete();
                    }

                    @Override
                    public void onError(Exception e) {
                        EasyLog.print(TAG, "SSE 请求异常: " + e.getMessage());
                        
                        String errStr = "请求失败: " + e.getMessage();
                        if (answerMessageRef[0] != null) {
                            answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n" + errStr);
                            answerMessageRef[0].setStreaming(false);
                            ChatSessionManager.getInstance().updateMessage(answerMessageRef[0]);
                        } else {
                            thinkingMessage.setContent(thinkingMessage.getContent() + "\n" + errStr);
                            thinkingMessage.setStreaming(false);
                            ChatSessionManager.getInstance().updateMessage(thinkingMessage);
                        }
                        
                        listener.onError(e);
                    }
                });
    }

    /**
     * 生成会话总结（SSE 流式）
     * <p>
     * 与 sendMessage() 类似，但专注于生成会话摘要。
     * 流式响应会直接输出总结内容，不区分思考和回答阶段。
     * </p>
     *
     * @param session 当前会话
     * @param prompt 总结提示词
     * @param listener 流式响应监听器
     */
    public void generateSummary(ChatSessionBean session, String prompt, final ChatStreamListener listener) {
        new AiStreamApi()
                .setQuery(prompt)
                .setConversationId(session.getConversationId())
                .setEndUserId(session.getEndUserId())
                .execute(new SseStreamCallback() {
                    @Override
                    public void onOpen() {
                        EasyLog.print(TAG, "摘要生成 SSE 连接已打开");
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
                        listener.onError(e);
                    }
                });
    }

    /**
     * 生成摘要 Prompt
     */
    public String generateSummaryPrompt(List<ChatMessageBean> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请总结以下对话内容：\n\n");
        
        for (ChatMessageBean message : messages) {
            if (message.getType() == ChatMessageBean.TYPE_SEND) {
                promptBuilder.append("用户: ").append(message.getContent()).append("\n");
            } else if (message.getType() == ChatMessageBean.TYPE_RECEIVED) {
                promptBuilder.append("AI: ").append(message.getContent()).append("\n");
            }
        }
        
        promptBuilder.append("\n请生成简洁的摘要。");
        return promptBuilder.toString();
    }
}
