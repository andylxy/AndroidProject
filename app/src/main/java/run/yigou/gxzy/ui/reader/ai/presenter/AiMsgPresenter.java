package run.yigou.gxzy.ui.reader.ai.presenter;

import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import run.yigou.gxzy.data.local.entity.ChatMessageBean;
import run.yigou.gxzy.data.local.entity.ChatSessionBean;
import run.yigou.gxzy.data.local.entity.ChatSummaryBean;
import run.yigou.gxzy.manager.ai.AiChatManager;
import run.yigou.gxzy.manager.ai.ChatSessionManager;
import run.yigou.gxzy.manager.Callback;
import run.yigou.gxzy.ui.reader.ai.contract.AiMsgContract;
import run.yigou.gxzy.utils.DateHelper;

/**
 * AI 消息 Presenter
 * 负责：会话管理、消息收发、总结生成
 */
public class AiMsgPresenter implements AiMsgContract.Presenter {

    private static final String TAG = "AiMsgPresenter";
    private final AiMsgContract.View mView;
    private ChatSessionBean currentSession;
    
    // UI 更新节流相关
    private final Handler uiUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable answerUpdateRunnable = null;
    private static final long UI_UPDATE_INTERVAL_MS = 100;
    
    // SimpleDateFormat 线程安全：使用 ThreadLocal 确保每个线程独立实例
    private final ThreadLocal<SimpleDateFormat> sdf = ThreadLocal.withInitial(
        () -> new SimpleDateFormat("HH:mm", Locale.getDefault())
    );
    
    // 会话相关常量
    private static final String SESSION_NEW_TITLE = "新对话";
    private static final String SESSION_PREVIEW_PREFIX = "我: ";
    private static final String SESSION_DEFAULT_TITLE = "AI助手";
    private static final String SYSTEM_MESSAGE_PREFIX = "开始新的对话 ";
    
    // 消息相关常量
    private static final String THINKING_CONTENT = "正在思考...";
    private static final String SUMMARY_NICK = "会话总结";
    
    // 总结相关常量
    private static final String SUMMARY_TAG_LATEST = "[最近历史总结]";
    private static final String SUMMARY_TAG_ALL = "[全部历史总结]";
    private static final String SUMMARY_CONTENT_PREFIX = "\n\n[历史总结]:\n";
    private static final String SUMMARY_SEPARATOR = "\n\n---\n\n";
    
    // 错误提示常量
    private static final String ERROR_SESSION_CHECK_FAIL = "会话检查失败: ";
    private static final String ERROR_CREATE_SESSION_FAIL = "创建会话失败: ";
    private static final String ERROR_NO_SESSION = "请先选择一个会话";
    private static final String ERROR_NO_MESSAGES = "当前会话没有消息";
    private static final String ERROR_SUMMARY_FAIL = "生成总结失败: ";
    private static final String ERROR_REQUEST_FAIL = "请求出错: ";
    private static final String TOAST_SESSION_CLEARED = "所有会话已清空";
    private static final String TOAST_SUMMARY_SAVED = "总结已保存";
    
    // 集合索引常量
    private static final int FIRST_INDEX = 0;
    private static final int LAST_INDEX_OFFSET = 1;

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
        AiChatManager.getInstance().startNewSession(mView.getLifecycleOwner(), new Callback<ChatSessionBean>() {
            @Override
            public void onSuccess(ChatSessionBean session) {
                currentSession = session;
                ChatSessionManager.getInstance().saveLastSessionId(session.getId());
                
                mView.clearMessages();
                mView.updateTitle(SESSION_NEW_TITLE);
                mView.updateCurrentSession(currentSession); // 通知 View 更新状态
                
                // 添加系统消息
                String time = sdf.get().format(new Date());
                ChatMessageBean systemMessage = new ChatMessageBean(
                        ChatMessageBean.TYPE_SYSTEM,
                        null,
                        null,
                        SYSTEM_MESSAGE_PREFIX + time);
                systemMessage.setCreateDate(DateHelper.getSeconds1());
                systemMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                mView.appendMessage(systemMessage);
                
                // 刷新侧边栏
                refreshSessionList();
            }

            @Override
            public void onError(Exception e) {
                mView.showError(ERROR_CREATE_SESSION_FAIL + e.getMessage());
            }
        });
    }

    @Override
    public void sendMessage(String content) {
        if (content == null || content.trim().isEmpty()) return;
        
        String time = sdf.get().format(new Date());

        // 确保会话已保存到数据库 (为了确保有 ID)
        ensureSessionSaved();

        // 使用 Manager 检查会话并执行
        AiChatManager.getInstance().checkSessionAndExecute(mView.getLifecycleOwner(), currentSession, new Callback<ChatSessionBean>() {
            @Override
            public void onSuccess(ChatSessionBean session) {
                currentSession = session;
                mView.updateCurrentSession(currentSession); // 通知 View 更新状态 (以防 ID 变化)
                executeSendMessage(content, time);
            }

            @Override
            public void onError(Exception e) {
                mView.showError(ERROR_SESSION_CHECK_FAIL + e.getMessage());
            }
        });
    }

    private void ensureSessionSaved() {
        if (currentSession == null) {
            // 创建临时的内存 Session
            currentSession = new ChatSessionBean();
            currentSession.setTitle(SESSION_NEW_TITLE);
            currentSession.setPreview(SESSION_NEW_TITLE);
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
            refreshSessionList();
        }
    }

    private void executeSendMessage(String result, String time) {
        // 处理系统消息
        ChatMessageBean sysMsg = ChatSessionManager.getInstance().checkAndAddSystemMessage(
                currentSession.getId(), time, Collections.emptyList());
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
                    ChatSummaryBean latestSummary = summaries.get(FIRST_INDEX);
                    if (latestSummary.getContent() != null) {
                        summaryContent.append(latestSummary.getContent());
                    }
                    summaryTag = SUMMARY_TAG_LATEST;
                } else {
                    for (int i = summaries.size() - LAST_INDEX_OFFSET; i >= FIRST_INDEX; i--) {
                        ChatSummaryBean summary = summaries.get(i);
                        if (summary.getContent() != null) {
                            if (summaryContent.length() > 0) summaryContent.append(SUMMARY_SEPARATOR);
                            summaryContent.append(summary.getContent());
                        }
                    }
                    summaryTag = SUMMARY_TAG_ALL;
                }
                if (summaryContent.length() > 0) {
                    messageToSend = result + SUMMARY_CONTENT_PREFIX + summaryContent.toString();
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
        currentSession.setPreview(SESSION_PREVIEW_PREFIX + result);
        currentSession.setUpdateTime(DateHelper.getSeconds1());
        ChatSessionManager.getInstance().updateSession(currentSession);
        refreshSessionList(); // 刷新侧边栏预览

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
            refreshSessionList();
        }
    }

    @Override
    public void renameSession(ChatSessionBean session, String newTitle) {
        session.setTitle(newTitle);
        ChatSessionManager.getInstance().updateSession(session);
        refreshSessionList();
        if (currentSession != null && currentSession.getId().equals(session.getId())) {
            mView.updateTitle(newTitle);
        }
    }

    @Override
    public void clearAllSessions() {
        ChatSessionManager.getInstance().clearAllSessions();
        currentSession = null;
        mView.clearMessages();
        mView.updateTitle(SESSION_DEFAULT_TITLE);
        mView.showSessionList(Collections.emptyList());
        mView.showError(TOAST_SESSION_CLEARED);
    }

    @Override
    public void deleteMessage(ChatMessageBean message) {
        ChatSessionManager.getInstance().deleteMessage(message);
        mView.removeMessage(message);
    }

    @Override
    public void generateSummary() {
        if (currentSession == null) {
            mView.showError(ERROR_NO_SESSION);
            return;
        }
        
        List<ChatMessageBean> messages = ChatSessionManager.getInstance().getMessagesForSession(currentSession);
        if (messages == null || messages.isEmpty()) {
            mView.showError(ERROR_NO_MESSAGES);
            return;
        }

        String prompt = AiChatManager.getInstance().generateSummaryPrompt(messages);
        mView.showLoading(true); // Toast "正在生成总结..."

        // 创建思考消息
        final ChatMessageBean thinkingMsg = new ChatMessageBean();
        thinkingMsg.setType(ChatMessageBean.TYPE_THINKING);
        thinkingMsg.setContent(THINKING_CONTENT);
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
        summaryMsg.setNick(SUMMARY_NICK);
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
                    if (THINKING_CONTENT.equals(current)) current = "";
                    thinkingMsg.setContent(current + content);
                    mView.updateMessage(thinkingMsg);
                });
            }

            @Override
            public void onAnswerStart(ChatMessageBean answerMessage) {
                // 总结生成不需要处理 answerStart
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
                     mView.showError(ERROR_SUMMARY_FAIL + error);
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
        mView.showError(TOAST_SUMMARY_SAVED);
    }

    @Override
    public void onDestroy() {
        uiUpdateHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * 刷新侧边栏会话列表
     */
    private void refreshSessionList() {
        mView.showSessionList(ChatSessionManager.getInstance().getAllSessionsSorted());
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
                scheduleAnswerUIUpdate(answerMessage, UI_UPDATE_INTERVAL_MS);
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
                
                mView.updateMessage(answerMessage);
                mView.scrollToBottom();
            });
        }

        @Override
        public void onError(String error) {
            uiUpdateHandler.post(() -> {
                mView.showError(ERROR_REQUEST_FAIL + error);
            });
        }
    }

    private void scheduleAnswerUIUpdate(ChatMessageBean answerMessage, long interval) {
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
        uiUpdateHandler.postDelayed(answerUpdateRunnable, interval);
    }
}
