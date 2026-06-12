package run.yigou.gxzy.ui.feature.reader.ai.presenter;

import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import run.yigou.gxzy.data.local.entity.ChatMessageBean;
import run.yigou.gxzy.data.local.entity.ChatSessionBean;
import run.yigou.gxzy.data.local.entity.ChatSummaryBean;
import run.yigou.gxzy.manager.ai.AiChatManager;
import run.yigou.gxzy.manager.ai.ChatSessionManager;
import run.yigou.gxzy.ui.feature.reader.ai.contract.AiMsgContract;
import run.yigou.gxzy.utils.DateHelper;

public class AiMsgPresenter implements AiMsgContract.Presenter {

    private static final String TAG = "AiMsgPresenter";
    private final AiMsgContract.View mView;
    private ChatSessionBean currentSession;
    
    // UI 更新节流相关
    private final Handler uiUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable answerUpdateRunnable = null;
    private static final long UI_UPDATE_INTERVAL = 100; // 降低到 100ms，提高流畅度，避免一次性堆积太多
    
    @android.annotation.SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

    public AiMsgPresenter(AiMsgContract.View view) {
        this.mView = view;
    }

    @Override
    public void start() {
        loadAllSessions();
    }

    /**
     * 加载所有会话并恢复上次选中的会话
     */
    private void loadAllSessions() {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (sessions.isEmpty()) {
            createNewSession();
        } else {
            mView.showSessionList(sessions);
            loadLastSelectedSession(sessions);
        }
    }

    private void loadLastSelectedSession(List<ChatSessionBean> sessions) {
        Long lastSessionId = ChatSessionManager.getInstance().getLastSessionId();
        ChatSessionBean targetSession = sessions.get(0);
        
        if (lastSessionId != null && lastSessionId > 0) {
            for (ChatSessionBean session : sessions) {
                if (session.getId().equals(lastSessionId)) {
                    targetSession = session;
                    break;
                }
            }
        }
        switchSession(targetSession);
    }

    @Override
    public void switchSession(ChatSessionBean session) {
        if (session == null) return;
        
        // 保存最后选中的会话ID
        ChatSessionManager.getInstance().saveLastSessionId(session.getId());
        
        // 重新从数据库获取最新状态
        ChatSessionBean dbSession = ChatSessionManager.getInstance().getSessionById(session.getId());
        if (dbSession == null) {
            // 如果数据库中找不到了，可能被删除了，加载默认
            loadAllSessions();
            return;
        }
        
        currentSession = dbSession;
        mView.updateTitle(currentSession.getTitle());
        mView.updateCurrentSession(currentSession); // 通知 View 更新状态
        
        // 加载消息
        List<ChatMessageBean> messages = ChatSessionManager.getInstance().getMessagesForSession(currentSession);
        // 确保所有 Thinking 消息是折叠状态
        for (ChatMessageBean message : messages) {
            if (message.getType() == ChatMessageBean.TYPE_THINKING) {
                message.setThinkingCollapsed(true);
            }
        }
        mView.showMessages(messages);
        mView.scrollToBottom();
    }

    @Override
    public void createNewSession() {
        AiChatManager.getInstance().startNewSession(mView.getLifecycleOwner(), new AiChatManager.SessionCheckCallback() {
            @Override
            public void onSessionValid(ChatSessionBean session) {
                currentSession = session;
                ChatSessionManager.getInstance().saveLastSessionId(session.getId());
                
                mView.clearMessages();
                mView.updateTitle("新对话");
                mView.updateCurrentSession(currentSession); // 通知 View 更新状态
                
                // 添加系统消息
                String time = sdf.format(new Date());
                ChatMessageBean systemMessage = new ChatMessageBean(
                        ChatMessageBean.TYPE_SYSTEM,
                        null,
                        null,
                        "开始新的对话 " + time);
                systemMessage.setCreateDate(DateHelper.getSeconds1());
                systemMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                mView.appendMessage(systemMessage);
                
                // 刷新侧边栏
                mView.showSessionList(ChatSessionManager.getInstance().getAllSessionsSorted());
            }

            @Override
            public void onFailure(String error) {
                mView.showError("创建会话失败: " + error);
            }
        });
    }

    @Override
    public void sendMessage(String content) {
        if (content == null || content.trim().isEmpty()) return;
        
        String time = sdf.format(new Date());

        // 如果当前没有会话，创建一个新的会话
        if (currentSession == null) {
            // 这里应该是一个异步过程，但在 sendMsg 逻辑中通常是先创建临时的
            // 为了简化，我们假设 createNewSession 已经调用过或在这里同步创建
            // 但 AiChatManager.checkSessionAndExecute 会处理 ID 申请
        }

        // 确保会话已保存到数据库 (为了确保有 ID)
        ensureSessionSaved();

        // 使用 Manager 检查会话并执行
        AiChatManager.getInstance().checkSessionAndExecute(mView.getLifecycleOwner(), currentSession, new AiChatManager.SessionCheckCallback() {
            @Override
            public void onSessionValid(ChatSessionBean session) {
                currentSession = session;
                mView.updateCurrentSession(currentSession); // 通知 View 更新状态 (以防 ID 变化)
                executeSendMessage(content, time);
            }

            @Override
            public void onFailure(String error) {
                mView.showError("会话检查失败: " + error);
            }
        });
    }

    private void ensureSessionSaved() {
        if (currentSession == null) {
            // 创建临时的内存 Session
            currentSession = new ChatSessionBean();
            currentSession.setTitle("新对话");
            currentSession.setPreview("新对话");
            currentSession.setCreateTime(DateHelper.getSeconds1());
            currentSession.setUpdateTime(DateHelper.getSeconds1());
            currentSession.setIsDelete(ChatSessionBean.IS_Delete_NO);
        }
        
        if (currentSession.getId() == null) {
            long sessionId = ChatSessionManager.getInstance().saveSession(currentSession);
            currentSession.setId(sessionId);
            ChatSessionManager.getInstance().saveLastSessionId(sessionId);
            mView.updateTitle(currentSession.getTitle());
            mView.updateCurrentSession(currentSession); // 通知 View 更新状态
            mView.showSessionList(ChatSessionManager.getInstance().getAllSessionsSorted());
        }
    }

    private void executeSendMessage(String result, String time) {
        // 处理系统消息
        ChatMessageBean sysMsg = ChatSessionManager.getInstance().checkAndAddSystemMessage(
                currentSession.getId(), time, new ArrayList<>()); // 这里无法获取 Adapter 数据，传空列表或需要调整 Manager 接口
        // 注意：checkAndAddSystemMessage 依赖已有的消息列表来判断是否重复添加时间戳
        // 由于 Presenter 不直接持有 Adapter 数据，这里可能需要优化。
        // 暂时略过系统时间消息的去重逻辑，或者总是添加
        if (sysMsg != null) {
             mView.appendMessage(sysMsg);
        }

        // 检查是否选中了总结
        String messageToSend = result;
        String summaryTag = null;

        boolean useLatestSummary = mView.isLatestSummaryChecked();
        boolean useAllSummary = mView.isAllSummaryChecked();

        if (useLatestSummary || useAllSummary) {
            List<ChatSummaryBean> summaries = ChatSessionManager.getInstance().getSessionSummaries(currentSession.getId());
            if (summaries != null && !summaries.isEmpty()) {
                StringBuilder summaryContent = new StringBuilder();
                if (useLatestSummary) {
                    ChatSummaryBean latestSummary = summaries.get(0);
                    if (latestSummary.getContent() != null) {
                        summaryContent.append(latestSummary.getContent());
                    }
                    summaryTag = "[最近历史总结]";
                } else {
                    for (int i = summaries.size() - 1; i >= 0; i--) {
                        ChatSummaryBean summary = summaries.get(i);
                        if (summary.getContent() != null) {
                            if (summaryContent.length() > 0) summaryContent.append("\n\n---\n\n");
                            summaryContent.append(summary.getContent());
                        }
                    }
                    summaryTag = "[全部历史总结]";
                }
                if (summaryContent.length() > 0) {
                    messageToSend = result + "\n\n[历史总结]:\n" + summaryContent.toString();
                }
            }
        }

        // 确定显示内容
        String displayContent = result;
        if (summaryTag != null && !messageToSend.equals(result)) {
            displayContent = result + "\n" + summaryTag;
        }

        // 1. 保存发送消息
        ChatMessageBean sendMsg = new ChatMessageBean(ChatMessageBean.TYPE_SEND, "", "", displayContent);
        sendMsg.setSessionId(currentSession.getId());
        sendMsg.setCreateDate(DateHelper.getSeconds1());
        sendMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
        long sendId = ChatSessionManager.getInstance().saveMessage(sendMsg);
        sendMsg.setId(sendId);
        mView.appendMessage(sendMsg);

        // 更新会话
        currentSession.setPreview("我: " + result);
        currentSession.setUpdateTime(DateHelper.getSeconds1());
        ChatSessionManager.getInstance().updateSession(currentSession);
        mView.showSessionList(ChatSessionManager.getInstance().getAllSessionsSorted()); // 刷新侧边栏预览

        // 2. 创建思考中消息
        ChatMessageBean thinkingMsg = new ChatMessageBean(ChatMessageBean.TYPE_THINKING, "Ai", "", "正在思考...");
        thinkingMsg.setSessionId(currentSession.getId());
        thinkingMsg.setCreateDate(DateHelper.getSeconds1());
        thinkingMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
        long thinkId = ChatSessionManager.getInstance().saveMessage(thinkingMsg);
        thinkingMsg.setId(thinkId);
        mView.appendMessage(thinkingMsg);
        mView.scrollToBottom();

        // 3. 发送请求
        AiChatManager.getInstance().sendMessage(
                currentSession,
                messageToSend,
                thinkingMsg,
                new ChatUiStreamListener(thinkingMsg, false) // 禁用节流，交给 Adapter 的打字机处理
        );
    }

    @Override
    public void deleteSession(ChatSessionBean session) {
        if (session == null) return;
        ChatSessionManager.getInstance().deleteSession(session);
        
        // 如果删除的是当前会话
        if (currentSession != null && currentSession.getId().equals(session.getId())) {
            currentSession = null;
            mView.clearMessages();
            loadAllSessions(); // 重新加载，可能会创建新会话或选中下一个
        } else {
            mView.showSessionList(ChatSessionManager.getInstance().getAllSessionsSorted());
        }
    }

    @Override
    public void renameSession(ChatSessionBean session, String newTitle) {
        session.setTitle(newTitle);
        ChatSessionManager.getInstance().updateSession(session);
        mView.showSessionList(ChatSessionManager.getInstance().getAllSessionsSorted());
        if (currentSession != null && currentSession.getId().equals(session.getId())) {
            mView.updateTitle(newTitle);
        }
    }

    @Override
    public void clearAllSessions() {
        ChatSessionManager.getInstance().clearAllSessions();
        currentSession = null;
        mView.clearMessages();
        mView.updateTitle("AI助手");
        mView.showSessionList(new ArrayList<>());
        mView.showError("所有会话已清空");
    }

    @Override
    public void deleteMessage(ChatMessageBean message) {
        ChatSessionManager.getInstance().deleteMessage(message);
        mView.removeMessage(message);
    }

    @Override
    public void generateSummary() {
        if (currentSession == null) {
            mView.showError("请先选择一个会话");
            return;
        }
        
        List<ChatMessageBean> messages = ChatSessionManager.getInstance().getMessagesForSession(currentSession);
        if (messages == null || messages.isEmpty()) {
            mView.showError("当前会话没有消息");
            return;
        }

        String prompt = AiChatManager.getInstance().generateSummaryPrompt(messages);
        mView.showLoading(true); // Toast "正在生成总结..."

        // 创建思考消息
        final ChatMessageBean thinkingMsg = new ChatMessageBean();
        thinkingMsg.setType(ChatMessageBean.TYPE_THINKING);
        thinkingMsg.setContent("正在思考...");
        thinkingMsg.setNick("Ai");
        thinkingMsg.setCreateDate(DateHelper.getSeconds1());
        thinkingMsg.setSessionId(currentSession.getId());
        thinkingMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
        long thinkId = ChatSessionManager.getInstance().saveMessage(thinkingMsg);
        thinkingMsg.setId(thinkId);
        mView.appendMessage(thinkingMsg);
        mView.scrollToBottom();

        // 创建总结消息
        final ChatMessageBean summaryMsg = new ChatMessageBean();
        summaryMsg.setType(ChatMessageBean.TYPE_SUMMARY);
        summaryMsg.setContent("");
        summaryMsg.setNick("会话总结");
        summaryMsg.setCreateDate(DateHelper.getSeconds1());
        summaryMsg.setSessionId(currentSession.getId());
        summaryMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
        summaryMsg.setStreaming(true);
        long summaryId = ChatSessionManager.getInstance().saveMessage(summaryMsg);
        summaryMsg.setId(summaryId);

        final StringBuilder summaryContent = new StringBuilder();
        final boolean[] hasStartedAnswer = {false};

        AiChatManager.getInstance().generateSummary(currentSession, prompt, new AiChatManager.ChatStreamListener() {
            @Override
            public void onThinking(String content) {
                uiUpdateHandler.post(() -> {
                    String current = thinkingMsg.getContent();
                    if ("正在思考...".equals(current)) current = "";
                    thinkingMsg.setContent(current + content);
                    mView.updateMessage(thinkingMsg);
                });
            }

            @Override
            public void onAnswerStart(ChatMessageBean answerMessage) {
                // 忽略
            }

            @Override
            public void onAnswerChunk(String content) {
                summaryContent.append(content);
                
                if (!hasStartedAnswer[0]) {
                    hasStartedAnswer[0] = true;
                    uiUpdateHandler.post(() -> {
                        thinkingMsg.setThinkingCollapsed(true);
                        mView.updateMessage(thinkingMsg);
                        mView.appendMessage(summaryMsg);
                    });
                }
                
                // 节流更新
                // 这里为了简单，直接复用 scheduleAnswerUIUpdate 逻辑，或者直接更新
                // 原逻辑有节流，这里也应该有
                uiUpdateHandler.post(() -> {
                    summaryMsg.setContent(summaryContent.toString());
                    mView.updateMessage(summaryMsg);
                });
            }

            @Override
            public void onComplete() {
                uiUpdateHandler.post(() -> {
                    thinkingMsg.setThinkingCollapsed(true);
                    mView.updateMessage(thinkingMsg);
                    
                    summaryMsg.setStreaming(false);
                    summaryMsg.setContent(summaryContent.toString());
                    ChatSessionManager.getInstance().updateMessage(thinkingMsg);
                    ChatSessionManager.getInstance().updateMessage(summaryMsg);
                    
                    mView.updateMessage(summaryMsg);
                    mView.scrollToBottom();
                    mView.showLoading(false); // Toast "总结已生成..."
                });
            }

            @Override
            public void onError(String error) {
                uiUpdateHandler.post(() -> {
                     summaryMsg.setStreaming(false);
                     mView.updateMessage(summaryMsg);
                     mView.showError("生成总结失败: " + error);
                });
            }
        });
    }

    @Override
    public void adoptSummary(ChatMessageBean summaryMsg) {
        if (currentSession == null) return;
        
        ChatSummaryBean summary = new ChatSummaryBean();
        summary.setSessionId(currentSession.getId());
        summary.setTitle("总结 " + DateHelper.getSeconds1());
        summary.setContent(summaryMsg.getContent());
        summary.setCreateTime(DateHelper.getSeconds1());
        summary.setIsDelete(ChatSummaryBean.IS_Delete_NO);

        ChatSessionManager.getInstance().saveSummary(summary);
        mView.showError("总结已保存"); // Toast
        // 原逻辑会显示 Dialog，这个 Dialog 属于 View 层，可以在 View 实现 adoptSummary 成功后的回调
        // 这里 Presenter 只负责保存。
        // 或者 View 层直接处理？原逻辑是 ChatSummaryHelper 处理的。
        // 我们保留 ChatSummaryHelper 在 View 层，Presenter 负责数据保存。
        // 这里 Presenter 保存完后，其实不需要回调 View 显示 Dialog，因为原逻辑就是保存后显示。
        // 也许 adoptSummary 应该由 View 调用 Presenter 保存，然后 View 自己显示 Dialog。
    }

    @Override
    public void onDestroy() {
        if (uiUpdateHandler != null) {
            uiUpdateHandler.removeCallbacksAndMessages(null);
        }
    }
    
    // ================= Internal Helper Classes =================

    private class ChatUiStreamListener implements AiChatManager.ChatStreamListener {
        private final ChatMessageBean thinkingMessage;
        private final boolean useThrottle;
        private ChatMessageBean answerMessage;

        public ChatUiStreamListener(ChatMessageBean thinkingMessage, boolean useThrottle) {
            this.thinkingMessage = thinkingMessage;
            this.useThrottle = useThrottle;
        }

        @Override
        public void onThinking(String content) {
            uiUpdateHandler.post(() -> {
                mView.updateMessage(thinkingMessage);
            });
        }

        @Override
        public void onAnswerStart(ChatMessageBean answerMsg) {
            this.answerMessage = answerMsg;
            uiUpdateHandler.post(() -> {
                thinkingMessage.setThinkingCollapsed(true); // 收到回答时折叠思考
                mView.updateMessage(thinkingMessage);
                mView.appendMessage(answerMsg);
            });
        }

        @Override
        public void onAnswerChunk(String content) {
            if (answerMessage == null) return;

            if (useThrottle) {
                scheduleAnswerUIUpdate(answerMessage);
            } else {
                uiUpdateHandler.post(() -> {
                    mView.updateMessage(answerMessage);
                });
            }
        }

        @Override
        public void onComplete() {
            if (useThrottle && answerUpdateRunnable != null) {
                uiUpdateHandler.removeCallbacks(answerUpdateRunnable);
                answerUpdateRunnable = null;
            }

            uiUpdateHandler.post(() -> {
                thinkingMessage.setThinkingCollapsed(true);
                mView.updateMessage(thinkingMessage);
                
                // 确保 answerMessage 是最新的状态（如果用了节流，可能 content 还没更新进对象，但引用是同一个）
                // 这里的 content 是在 Manager 里 append 的，所以对象里的 content 是新的。
                // 主要是通知 View 刷新一下最终状态（比如 Markdown 渲染）
                mView.updateMessage(answerMessage);
                mView.scrollToBottom();
            });
        }

        @Override
        public void onError(String error) {
            uiUpdateHandler.post(() -> {
                mView.showError("请求出错: " + error);
                // 刷新界面以显示可能的错误状态
                // mView.showMessages(ChatSessionManager.getInstance().getMessagesForSession(currentSession));
            });
        }
    }

    private void scheduleAnswerUIUpdate(ChatMessageBean answerMessage) {
        if (answerUpdateRunnable != null) {
            uiUpdateHandler.removeCallbacks(answerUpdateRunnable);
        }
        answerUpdateRunnable = () -> {
            uiUpdateHandler.post(() -> {
                mView.updateMessage(answerMessage);
                mView.scrollToBottom(); // 流式过程中也需要滚动
                answerUpdateRunnable = null;
            });
        };
        uiUpdateHandler.postDelayed(answerUpdateRunnable, UI_UPDATE_INTERVAL);
    }
}
