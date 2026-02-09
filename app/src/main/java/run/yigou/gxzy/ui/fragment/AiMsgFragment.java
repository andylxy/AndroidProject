package run.yigou.gxzy.ui.fragment;

import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Toast;

import run.yigou.gxzy.EventBus.ChatMessageBeanEvent;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.tips.adapter.TipsAiChatAdapter;

import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.entity.ChatSessionBean;
import run.yigou.gxzy.greendao.entity.ChatSummaryBean;


import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.ThreadUtil;
import run.yigou.gxzy.ui.tips.adapter.ChatHistoryAdapter;
import run.yigou.gxzy.ui.dialog.ChatSummaryListDialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import run.yigou.gxzy.utils.DebugLog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CheckBox;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.hjq.bar.OnTitleBarListener;
import run.yigou.gxzy.utils.EasyLog;
import run.yigou.gxzy.utils.MarkdownUtils;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import run.yigou.gxzy.manager.AiChatManager;
import run.yigou.gxzy.manager.ChatSessionManager;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;


public final class AiMsgFragment extends TitleBarFragment<HomeActivity> implements OnTitleBarListener {

    private static final String TAG = "AiMsgFragment";
    
    private RecyclerView rv_chat;
    private DrawerLayout drawerLayout;
    private RecyclerView chatHistoryList;
    private run.yigou.gxzy.ui.tips.adapter.ChatHistoryAdapter chatHistoryAdapter; // 修改导入路径
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private int scrollState = 0;

    // UI 更新节流相关
    private Handler uiUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable answerUpdateRunnable = null;
    private static final long UI_UPDATE_INTERVAL = 200; // 200ms 批量更新一次

    // 当前会话
    private ChatSessionBean currentSession;
    
    // 总结功能相关
    private CheckBox cbLatestSummary;  // 最近一次总结
    private CheckBox cbAllSummary;     // 全部总结
    private Button btnSummarize;

    public static AiMsgFragment newInstance() {
        return new AiMsgFragment();
    }


    @Override
    protected int getLayoutId() {
        // return R.layout.ai_msg_activity_chat;
        return R.layout.tips_ai_msg_activity_chat;
    }

    // 创建并设置适配器
    private TipsAiChatAdapter mChatAdapter;
    private Markwon mMarkwon;

    @Override
    protected void initView() {
        EasyLog.print(TAG, "initView: Starting initialization");
        
        // 初始化管理器
        ChatSessionManager.getInstance().init(getContext());
        
        initMarkwon();
        initTitleBar();
        initChatRecyclerView();
        initHistorySidebar();
        initInputArea();
        initSummaryFeatures();

        // 注册事件
        XEventBus.getDefault().register(AiMsgFragment.this);
        // 初始化消息数据
        initMsgs();

        // 添加调试日志，检查组件是否正确初始化
        EasyLog.print(TAG, "initView: rv_chat=" + rv_chat);
        EasyLog.print(TAG, "initView: mChatAdapter=" + mChatAdapter);
    }

    private void initMarkwon() {
        mMarkwon = Markwon.builder(getContext())
                .usePlugin(CorePlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(getContext()))
                .usePlugin(TaskListPlugin.create(getContext()))
                .usePlugin(ImagesPlugin.create())
                .build();
    }

    private void initTitleBar() {
        // 初始化状态栏,设置为沉浸式
        getStatusBarConfig().setTitleBar(this, findViewById(R.id.tv_title));
        getStatusBarConfig().setTitleBar(this, findViewById(R.id.side_panel));
        // 设置标题栏点击监听
        if (getTitleBar() != null) {
            getTitleBar().setOnTitleBarListener(this);
        }
    }

    private void initChatRecyclerView() {
        rv_chat = findViewById(R.id.rv_chat);
        
        // 获取 Activity
        Activity activity = getActivity();
        if (activity == null) return;
        
        // 创建并设置适配器
        mChatAdapter = new TipsAiChatAdapter(activity);
        mChatAdapter.setHasStableIds(true);
        // 设置 RecyclerView 的动画时长
        rv_chat.setItemAnimator(null); // 禁用动画
        rv_chat.setNestedScrollingEnabled(false); // 禁用嵌套滚动，让内部 ScrollView 可以滚动
        
        // 设置 RecyclerView 的布局管理器和适配器
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rv_chat.setLayoutManager(layoutManager);
        rv_chat.setAdapter(mChatAdapter);
        
        // 设置打字机渲染回调，实现打字时同步滚动
        TipsAiChatAdapter.setScrollCallback(this::scrollToAbsoluteBottom);
        
        // 设置消息即点击操作回调
        mChatAdapter.setOnMessageActionListener(new TipsAiChatAdapter.OnMessageActionListener() {
            @Override
            public void onMessageClick(android.view.View view, ChatMessageBean message, float x, float y) {
                showMessageActionMenu(view, message, x, y);
            }

            @Override
            public void onAdoptSummary(ChatMessageBean summaryMessage) {
                adoptSummary(summaryMessage);
            }
        });
        EasyLog.print(TAG, "initView: layoutManager=" + layoutManager);
    }

    private void initHistorySidebar() {
        drawerLayout = findViewById(R.id.drawer_layout);
        chatHistoryList = findViewById(R.id.chat_history_list);

        // 初始化聊天历史记录列表
        chatHistoryAdapter = new ChatHistoryAdapter(getActivity());
        chatHistoryList.setAdapter(chatHistoryAdapter);
        chatHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));

        // 设置聊天历史记录项监听器
        chatHistoryAdapter.setOnChatHistoryItemClickListener(this::onHistoryItemClick);
        chatHistoryAdapter.setOnChatHistoryItemDeleteListener(this::onHistoryItemDelete);
        chatHistoryAdapter.setOnChatHistoryItemEditTitleListener(this::onHistoryItemEditTitle);
        chatHistoryAdapter.setOnChatHistoryItemSummaryListener(this::onHistoryItemSummary);

        // 禁止通过边缘滑动手势打开侧边栏
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }

        // 设置侧边栏清空所有会话按钮点击事件
        ImageButton clearAllButton = findViewById(R.id.btn_edit_title);
        clearAllButton.setOnClickListener(v -> showClearAllSessionsDialog());
    }

    private void onHistoryItemClick(int position, ChatHistoryAdapter.ChatHistoryItem item) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (position >= 0 && position < sessions.size()) {
            ChatSessionBean selectedSession = sessions.get(position);
            ChatSessionManager.getInstance().saveLastSessionId(selectedSession.getId());
            loadChatDataForSession(selectedSession.getId());
            if (drawerLayout != null) {
                drawerLayout.closeDrawers();
            }
        }
    }

    private void onHistoryItemDelete(int position, ChatHistoryAdapter.ChatHistoryItem item) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (position >= 0 && position < sessions.size()) {
            ChatSessionBean sessionToDelete = sessions.get(position);
            ChatSessionManager.getInstance().deleteSession(sessionToDelete);

            if (currentSession != null && currentSession.getId().equals(sessionToDelete.getId())) {
                currentSession = null;
                if (mChatAdapter != null) {
                    mChatAdapter.setData(new ArrayList<ChatMessageBean>());
                }
            }
            loadChatSessionList();
        }
    }

    private void onHistoryItemEditTitle(int position, ChatHistoryAdapter.ChatHistoryItem item) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (position >= 0 && position < sessions.size()) {
            ChatSessionBean sessionToEdit = sessions.get(position);
            showEditSessionTitleDialog(sessionToEdit);
        }
    }

    private void onHistoryItemSummary(int position, ChatHistoryAdapter.ChatHistoryItem item) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (position >= 0 && position < sessions.size()) {
            ChatSessionBean session = sessions.get(position);
            showSummaryListDialog(session);
        }
    }
    
    private void initInputArea() {
        EditText chatContent = findViewById(R.id.chat_content);
        ImageView clearButton = findViewById(R.id.iv_clear);
        Button sendButton = findViewById(R.id.chat_send);

        // 设置清除按钮的点击事件
        clearButton.setOnClickListener(v -> chatContent.setText(""));

        // 合并所有的文本监听功能到一个监听器中
        chatContent.addTextChangedListener(new TextWatcher() {
            int lines = 1;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                int currentLines = chatContent.getLineCount();
                if (currentLines > lines) {
                    chatContent.scrollTo(0, 0);
                }
                lines = currentLines;
            }
        });

        chatContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                chatContent.setSelection(0, 0);
                clearButton.setVisibility(chatContent.getText().length() > 0 ? View.VISIBLE : View.GONE);
            }
        });
        
        // 设置键盘回车/发送键监听
        chatContent.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                String msg = chatContent.getText().toString().trim();
                if (!msg.isEmpty()) {
                    hideKeyboard();
                    if (sendButton != null) {
                        sendButton.performClick();
                    }
                }
                return true;
            }
            return false;
        });
        
        // 发送按钮逻辑
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> {
                String result = chatContent.getText().toString();
                if (!result.isEmpty()) {
                    chatContent.setText("");
                    sendMsg(result);
                }
            });
        }
    }
    
    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 
                    android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void initSummaryFeatures() {
        cbLatestSummary = findViewById(R.id.cb_latest_summary);
        cbAllSummary = findViewById(R.id.cb_all_summary);
        btnSummarize = findViewById(R.id.btn_summarize);
        
        if (cbLatestSummary != null) {
            cbLatestSummary.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && cbAllSummary != null) {
                    cbAllSummary.setChecked(false);
                }
            });
        }
        if (cbAllSummary != null) {
            cbAllSummary.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && cbLatestSummary != null) {
                    cbLatestSummary.setChecked(false);
                }
            });
        }
        
        if (btnSummarize != null) {
            btnSummarize.setOnClickListener(v -> generateSessionSummary());
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(priority = 1)
    public void ChatMessageEvent(ChatMessageBeanEvent event) {
        ThreadUtil.runOnUiThread(() -> {
            if (event.isClear()) {
                initData();
                mChatAdapter.notifyDataSetChanged();

                // 重新填充聊天历史记录测试数据
                loadChatSessionList();
            }
        });
    }

    /**
     * 只刷新聊天历史侧边栏UI，不重新加载会话数据
     */
    private void refreshChatHistorySidebar() {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();

        List<ChatHistoryAdapter.ChatHistoryItem> historyItems = new ArrayList<>();
        int currentSessionPosition = 0;
        
        for (int i = 0; i < sessions.size(); i++) {
            ChatSessionBean session = sessions.get(i);
            List<ChatMessageBean> messages = session.getMessages();
            String messageCount = messages.size() + " 条消息";

            historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                    session.getTitle(),
                    session.getPreview(),
                    session.getUpdateTime().substring(0, 10),
                    messageCount));
            
            // 找到当前会话的位置
            if (currentSession != null && session.getId().equals(currentSession.getId())) {
                currentSessionPosition = i;
            }
        }

        // 更新适配器数据
        chatHistoryAdapter.setData(historyItems);
        chatHistoryAdapter.setSelectedPosition(currentSessionPosition);
        EasyLog.print(TAG, "Refreshed sidebar, current session at position: " + currentSessionPosition);
    }

    /**
     * 加载聊天会话列表
     * Renamed from populateChatHistoryWithTestData
     */
    private void loadChatSessionList() {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();

        List<ChatHistoryAdapter.ChatHistoryItem> historyItems = new ArrayList<>();

        // 根据会话数据创建历史记录项
        for (ChatSessionBean session : sessions) {
            // 计算会话中的消息数量
            List<ChatMessageBean> messages = session.getMessages();
            String messageCount = messages.size() + " 条消息";

            historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                    session.getTitle(),
                    session.getPreview(),
                    session.getUpdateTime().substring(0, 10), // 提取日期部分
                    messageCount));
            EasyLog.print(TAG, "Added session to sidebar: " + session.getTitle() + ", message count: " + messageCount);
        }

        // 更新适配器数据
        chatHistoryAdapter.setData(historyItems);

        // 恢复上次选中的会话
        loadLastSelectedSession(sessions);
    }
    
    /**
     * 加载上次选中的会话
     */
    private void loadLastSelectedSession(List<ChatSessionBean> sessions) {
        // 如果会话列表为空，创建一个新会话
        if (sessions.isEmpty()) {
            EasyLog.print(TAG, "Session list is empty, creating new session");
            createNewSessionWithId();
            return;
        }
        
        // 读取保存的最后选中会话ID
        Long lastSessionId = getLastSessionId();
        
        // 查找对应的会话位置
        int selectedPosition = 0; // 默认选中第一个
        if (lastSessionId != null && lastSessionId > 0) {
            for (int i = 0; i < sessions.size(); i++) {
                if (sessions.get(i).getId().equals(lastSessionId)) {
                    selectedPosition = i;
                    EasyLog.print(TAG, "Found last selected session at position: " + i);
                    break;
                }
            }
        }
        
        // 选中并加载会话
        chatHistoryAdapter.setSelectedPosition(selectedPosition);
        loadChatDataForSession(sessions.get(selectedPosition).getId());
        EasyLog.print(TAG, "Auto-loaded session: " + sessions.get(selectedPosition).getTitle());
    }
    
    /**
     * 创建新会话并申请会话ID
     */
    private void createNewSessionWithId() {
        // 创建新会话
        ChatSessionBean newSession = ChatSessionManager.getInstance().createLocalSession("新对话", "开始新的对话");
        currentSession = newSession;
        
        // 申请会话ID
        AiChatManager.getInstance().requestSessionId(AiMsgFragment.this, new AiChatManager.SessionIdCallback() {
            @Override
            public void onSuccess(String conversationId, String endUserId) {
                if (currentSession != null) {
                    currentSession.setConversationId(conversationId);
                    currentSession.setEndUserId(endUserId);
                    currentSession.setCreateTime(DateHelper.getSeconds1());
                    
                    ChatSessionManager.getInstance().updateSession(currentSession);
                    
                    EasyLog.print(TAG, "Session ID obtained: " + conversationId);
                    
                    // 刷新会话列表UI
                    loadChatSessionList();
                }
            }

            @Override
            public void onFailure(String error) {
                EasyLog.print(TAG, "Failed to obtain session ID: " + error);
                Toast.makeText(getContext(), "会话创建失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 保存最后选中的会话ID
     */
    private void saveLastSessionId(Long sessionId) {
        ChatSessionManager.getInstance().saveLastSessionId(sessionId);
    }
    
    /**
     * 读取最后选中的会话ID
     */
    private Long getLastSessionId() {
        return ChatSessionManager.getInstance().getLastSessionId();
    }

    /**
     * 加载指定会话的聊天数据
     *
     * @param sessionId 会话ID
     */
    private void loadChatDataForSession(Long sessionId) {
        EasyLog.print(TAG, "loadChatDataForSession: Loading data for session " + sessionId);

        // 保存最后选中的会话ID
        saveLastSessionId(sessionId);
        
        // 获取会话信息
        ChatSessionBean session = ChatSessionManager.getInstance().getSessionById(sessionId);
        if (session == null) {
            EasyLog.print(TAG, "Session not found for ID: " + sessionId);
            loadDefaultSessionData();
            return;
        }

        // 设置当前会话
        currentSession = session;

        // 获取会话中的所有消息（已解密）
        List<ChatMessageBean> messages = ChatSessionManager.getInstance().getMessagesForSession(session);

        // 清空当前聊天数据
        if (mChatAdapter != null) {
            mChatAdapter.setData(new ArrayList<ChatMessageBean>());
        }

        // 加载会话消息
        if (mChatAdapter != null) {
            // 确保所有 Thinking 消息是折叠状态
            for (ChatMessageBean message : messages) {
                if (message.getType() == ChatMessageBean.TYPE_THINKING) {
                    message.setThinkingCollapsed(true);
                }
            }
            mChatAdapter.setData(new ArrayList<>(messages));
            EasyLog.print(TAG, "Loaded " + messages.size() + " messages for session " + sessionId);
        }

        // 将会话标题同步设置到TitleBar标题显示
        if (getTitleBar() != null) {
            getTitleBar().setTitle(session.getTitle());
        }

        // 滚动到聊天记录的绝对底部
        if (rv_chat != null && mChatAdapter != null) {
            rv_chat.post(new Runnable() {
                @Override
                public void run() {
                    if (mChatAdapter.getItemCount() > 0) {
                        // 先滚动到最后一条消息
                        rv_chat.scrollToPosition(mChatAdapter.getItemCount() - 1);
                        // 再滚动到 item 的绝对底部
                        rv_chat.post(() -> rv_chat.scrollBy(0, 10000));
                        EasyLog.print(TAG, "Scrolled to absolute bottom");
                    }
                }
            });
        }
    }

    /**
     * 加载默认会话数据
     */
    private void loadDefaultSessionData() {
        ArrayList<ChatMessageBean> sessionData = new ArrayList<>();

        // 添加系统消息
        ChatMessageBean systemMessage = new ChatMessageBean(
                ChatMessageBean.TYPE_SYSTEM,
                null,
                null,
                "请选择一个会话或开始新的对话");
        systemMessage.setCreateDate(DateHelper.getSeconds1());
        systemMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
        sessionData.add(systemMessage);

        // 设置数据到适配器
        if (mChatAdapter != null) {
            mChatAdapter.setData(sessionData);
        }
    }

    private int index = 0;

    /**
     * 初始化聊天消息
     */
    private void initMsgs() {
        EasyLog.print(TAG, "initMsgs: Initializing messages");
        
        // 加载会话列表并恢复上次选中的会话
        loadChatSessionList();
    }

    /**
     * 节流更新 AI 回答消息的 UI
     * 通过延迟执行减少 RecyclerView 重绘次数，避免闪烁
     * 
     * @param answerMessage AI 回答消息
     */
    private void scheduleAnswerUIUpdate(ChatMessageBean answerMessage) {
        // 移除之前待处理的更新任务
        if (answerUpdateRunnable != null) {
            uiUpdateHandler.removeCallbacks(answerUpdateRunnable);
        }
        
        // 创建新的更新任务
        answerUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    int answerPos = mChatAdapter.getData().indexOf(answerMessage);
                    if (answerPos != -1) {
                        // ⚠️ 移除重置 streaming=true 的逻辑，避免覆盖 onComplete 中的 false 值
                        // 刷新 UI
                        mChatAdapter.notifyItemChanged(answerPos, TipsAiChatAdapter.PAYLOAD_UPDATE_CONTENT);
                        
                        // 滚动到底部：使用 scrollBy 直接滚动像素，避免动画冲突
                        scrollToAbsoluteBottom();
                    }
                    answerUpdateRunnable = null;
                });
            }
        };
        
        // 延迟 200ms 执行，批量更新
        uiUpdateHandler.postDelayed(answerUpdateRunnable, UI_UPDATE_INTERVAL);
    }
    
    /**
     * UI 流式响应监听器 (提取出的内部类)
     */
    private class ChatUiStreamListener implements AiChatManager.ChatStreamListener {
        private final ChatMessageBean thinkingMessage;
        private final boolean useThrottle;
        private final boolean resetSummaryCheckbox;
        private ChatMessageBean answerMessage;

        public ChatUiStreamListener(ChatMessageBean thinkingMessage, boolean useThrottle, boolean resetSummaryCheckbox) {
            this.thinkingMessage = thinkingMessage;
            this.useThrottle = useThrottle;
            this.resetSummaryCheckbox = resetSummaryCheckbox;
        }

        @Override
        public void onThinking(String content) {
            runOnUiThread(() -> {
                int pos = mChatAdapter.getData().indexOf(thinkingMessage);
                if (pos != -1) {
                    mChatAdapter.notifyItemChanged(pos);
                    // 自动滚动
                    if (scrollState == 0 && rv_chat != null) {
                        rv_chat.scrollToPosition(mChatAdapter.getData().size() - 1);
                    }
                }
            });
        }

        @Override
        public void onAnswerStart(ChatMessageBean answerMsg) {
            this.answerMessage = answerMsg;
            runOnUiThread(() -> {
                // 折叠思考消息
                int thinkPos = mChatAdapter.getData().indexOf(thinkingMessage);
                if (thinkPos != -1) {
                    mChatAdapter.notifyItemChanged(thinkPos);
                }
                // 添加回答消息
                mChatAdapter.addItem(answerMsg);
            });
        }

        @Override
        public void onAnswerChunk(String content) {
            if (answerMessage == null) return;

            if (useThrottle) {
                scheduleAnswerUIUpdate(answerMessage);
            } else {
                runOnUiThread(() -> {
                    int answerPos = mChatAdapter.getData().indexOf(answerMessage);
                    if (answerPos != -1) {
                        mChatAdapter.notifyItemChanged(answerPos);
                    }
                });
            }
        }

        @Override
        public void onComplete() {
            // 移除待处理的 UI 更新任务
            if (useThrottle && answerUpdateRunnable != null) {
                uiUpdateHandler.removeCallbacks(answerUpdateRunnable);
                answerUpdateRunnable = null;
            }

            runOnUiThread(() -> {
                // 强制折叠思考
                thinkingMessage.setThinkingCollapsed(true);
                int thinkPos = mChatAdapter.getData().indexOf(thinkingMessage);
                if (thinkPos != -1) {
                    mChatAdapter.notifyItemChanged(thinkPos);
                }

                // 最终刷新并滚动
                mChatAdapter.updateData();
                scrollToAbsoluteBottom();

                // 恢复焦点能力
                if (rv_chat != null) {
                    rv_chat.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                }

                // 取消总结 CheckBox 选中
                if (resetSummaryCheckbox) {
                    if (cbLatestSummary != null && cbLatestSummary.isChecked()) {
                        cbLatestSummary.setChecked(false);
                    }
                    if (cbAllSummary != null && cbAllSummary.isChecked()) {
                        cbAllSummary.setChecked(false);
                    }
                }
            });
        }

        @Override
        public void onError(String error) {
            runOnUiThread(() -> {
                mChatAdapter.updateData();
                // 恢复焦点能力
                if (rv_chat != null) {
                    rv_chat.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                }
                Toast.makeText(getContext(), "请求出错: " + error, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    /**
     * 滚动到 RecyclerView 的绝对底部
     * 使用 smoothScrollBy 实现丝滑滚动
     */
    private void scrollToAbsoluteBottom() {
        if (rv_chat == null) return;
        
        rv_chat.post(() -> {
            // 使用 smoothScrollBy 丝滑滚动
            rv_chat.smoothScrollBy(0, 200);
        });
    }
    

    /**
     * 确保会话已保存到数据库 (Re-added as simple wrapper for legacy support if needed, or remove completely)
     * 目前仅保留 refreshChatHistorySidebar 逻辑供 startNewSession 回调使用，
     * 但 startNewSession 已经包含了 ID 申请，所以 ensureSessionSaved 的大部分逻辑已不再需要。
     * 为了兼容 sendMsg 中的调用，我们保留一个简化版或者直接移除并在 sendMsg 中处理。
     * sendMsg 现在使用 AiChatManager.checkSessionAndExecute，它会处理 ID 申请。
     * 唯一的问题是首次创建会话（没有 ID）的情况。
     * checkSessionAndExecute 依赖 session 对象。
     * startNewSession 创建并保存了 session。
     * 
     * 我们需要保留 ensureSessionSaved 供 sendMsg 调用吗？
     * sendMsg 中：
     * if (currentSession == null) createNewSession("新对话"); -> 这里 createNewSession 已经被我们删除了，需要修复。
     * ensureSessionSaved(); -> 这里也需要修复。
     * 
     * 让我们修复 sendMsg 逻辑。
     */
     
     // 重新添加简化版的辅助方法，适配重构后的逻辑
     
    private void createNewSession(String title) {
        // 仅在内存中创建，用于 sendMsg 的判空逻辑
        // 实际上 sendMsg 会调用 ensureSessionSaved -> saveSession
        // 但更好的方式是让 sendMsg 流程使用 startNewSession 的逻辑，但这涉及异步。
        // 目前 sendMsg 逻辑是：如果 currentSession 为空，先创建一个临时的。
        // 然后 ensureSessionSaved 保存它。
        // 然后 checkSessionAndExecute 检查它。
        
        // 恢复 createNewSession 以避免编译错误
        String currentTime = DateHelper.getSeconds1();
        ChatSessionBean newSession = new ChatSessionBean();
        newSession.setTitle(title);
        newSession.setPreview("新对话");
        newSession.setCreateTime(currentTime);
        newSession.setUpdateTime(currentTime);
        newSession.setIsDelete(ChatSessionBean.IS_Delete_NO);
        currentSession = newSession;
        EasyLog.print(TAG, "Created new session in memory only");
    }

    private void ensureSessionSaved() {
        if (currentSession.getId() == null) {
            String currentTime = DateHelper.getSeconds1();
            currentSession.setCreateTime(currentTime);
            currentSession.setUpdateTime(currentTime);
            
            long sessionId = ChatSessionManager.getInstance().saveSession(currentSession);
            currentSession.setId(sessionId);
            EasyLog.print(TAG, "Saved new session to database with ID: " + sessionId);

            saveLastSessionId(sessionId);
            if (getTitleBar() != null) {
                getTitleBar().setTitle(currentSession.getTitle());
            }
            loadChatSessionList(); // 使用 loadChatSessionList 替代 refreshChatHistorySidebar
        }
    }

    @Override
    public void onLeftClick(View view) {
        // 左侧按钮被点击，打开侧边栏
        if (drawerLayout != null) {
            drawerLayout.openDrawer(findViewById(R.id.side_panel));
        }
    }

    @Override
    public void onTitleClick(View view) {
        // 标题被点击

    }

    @Override
    public void onRightClick(View view) {
        // 右侧按钮（wx_add_chat_icon）被点击，添加新会话
        handleAddNewSession();
    }

    /**
     * 处理添加新会话的逻辑
     */
    private void handleAddNewSession() {
        // 使用 AiChatManager 开始新会话
        AiChatManager.getInstance().startNewSession(AiMsgFragment.this, new AiChatManager.SessionCheckCallback() {
            @Override
            public void onSessionValid(ChatSessionBean session) {
                // 更新当前会话
                currentSession = session;
                
                // 保存 ID 为最后选中状态，确保下次加载能选中它
                saveLastSessionId(session.getId());
                
                // 清空输入框
                EditText chatContent = findViewById(R.id.chat_content);
                if (chatContent != null) {
                    chatContent.setText("");
                }

                // 清空聊天界面并显示系统消息
                if (mChatAdapter != null) {
                    mChatAdapter.setData(new ArrayList<ChatMessageBean>());
                    // 添加系统消息到界面
                    String time = sdf.format(new Date());
                    ChatMessageBean systemMessage = new ChatMessageBean(
                            ChatMessageBean.TYPE_SYSTEM,
                            null,
                            null,
                            "开始新的对话 " + time);
                    systemMessage.setCreateDate(DateHelper.getSeconds1());
                    systemMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                    // 注意：新会话的第一条系统消息通常不保存到数据库，直到用户发送第一条消息
                    mChatAdapter.addItem(systemMessage);
                }

                // 更新标题栏标题
                if (getTitleBar() != null) {
                    getTitleBar().setTitle("新对话");
                }

                // 关闭侧边栏（如果打开的话）
                if (drawerLayout != null) {
                    drawerLayout.closeDrawers();
                }
                
                // 只刷新侧边栏 UI，不重新加载聊天数据
                refreshChatHistorySidebar();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "创建会话失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ArrayList<ChatMessageBean> chatMessageBeans;

    @Override
    protected void initData() {
        EasyLog.print(TAG, "initData: Starting to initialize data");

        // 只有在数据库中有会话数据时才初始化
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (!sessions.isEmpty()) {
            //初始化聊天会话数据
            // 加载历史会话历史记录
            loadChatSessionList();
        } else {
            // 没有会话数据时，创建新会话
            EasyLog.print(TAG, "No session data found in database, creating new session");
            // 清空侧边栏
            chatHistoryAdapter.setData(new ArrayList<ChatHistoryAdapter.ChatHistoryItem>());
            // 创建新会话
            handleAddNewSession();
        }
        EasyLog.print(TAG, "initData: Completed data initialization");
    }

    @Override
    public void onResume() {
        super.onResume();
        EasyLog.print(TAG, "onResume: Fragment resumed");
        // 确保数据在恢复时也能正确显示
        if (mChatAdapter != null && rv_chat != null) {
            rv_chat.post(new Runnable() {
                @Override
                public void run() {
                    EasyLog.print(TAG, "onResume: Adapter item count = " + mChatAdapter.getItemCount());
                    EasyLog.print(TAG, "onResume: RecyclerView child count = " + rv_chat.getChildCount());
                    if (mChatAdapter.getItemCount() > 0 && rv_chat.getChildCount() == 0) {
                        EasyLog.print(TAG, "onResume: Data exists but not displayed, forcing refresh");
                        mChatAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        // 清理 UI 更新 Handler，防止内存泄漏
        if (uiUpdateHandler != null) {
            if (answerUpdateRunnable != null) {
                uiUpdateHandler.removeCallbacks(answerUpdateRunnable);
                answerUpdateRunnable = null;
            }
            uiUpdateHandler.removeCallbacksAndMessages(null);
        }
        
        // 注销事件
        XEventBus.getDefault().unregister(AiMsgFragment.this);
        super.onDestroy();
    }

    /**
     * 显示编辑会话标题对话框
     *
     * @param session 要编辑的会话
     */
    private void showEditSessionTitleDialog(ChatSessionBean session) {
        if (session == null) {
            Toast.makeText(getContext(), "会话不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("编辑会话标题");

        final EditText input = new EditText(getContext());
        input.setText(session.getTitle());
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newTitle = input.getText().toString().trim();
                if (!newTitle.isEmpty() && !newTitle.equals(session.getTitle())) {
                    // 更新会话标题
                    session.setTitle(newTitle);
                    // 保存到数据库
                    session.setUpdateTime(DateHelper.getSeconds1());
                    ChatSessionManager.getInstance().updateSession(session);

                    // 如果是当前会话，更新标题栏显示
                    if (currentSession != null && currentSession.getId().equals(session.getId())) {
                        if (getTitleBar() != null) {
                            getTitleBar().setTitle(newTitle);
                        }
                    }

                    // 重新加载侧边栏数据
                    loadChatSessionList();
                    Toast.makeText(getContext(), "标题已更新", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // 自动弹出键盘
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    // 保存用户输入的内容和时间，用于重新申请会话ID后继续执行
    // private String pendingMessageContent;
    // private String pendingMessageTime;

    /**
     * 发送消息的实际执行逻辑 (提取出的中间函数)
     */
    private void executeSendMessage(String result, String time) {
        // 处理系统消息
        ChatSessionManager.getInstance().checkAndAddSystemMessage(currentSession.getId(), time, mChatAdapter.getData());
        
        // 如果是系统自动添加了消息，刷新界面
        // 注意：checkAndAddSystemMessage 返回的 message 已经被保存了，但我们需要手动添加到 adapter
        // 为了简单起见，我们重新检查一下 adapter 里有没有
        boolean hasSystemMsg = false;
        for(ChatMessageBean msg : mChatAdapter.getData()) {
            if(msg.getType() == ChatMessageBean.TYPE_SYSTEM && time.equals(msg.getContent())) {
                hasSystemMsg = true;
                break;
            }
        }
        if(!hasSystemMsg) {
             // 如果 adapter 里没有（说明是 checkAndAddSystemMessage 新加的，或者是纯 UI 显示），我们需要添加
             // 但由于 checkAndAddSystemMessage 逻辑较复杂（涉及 DB 操作），且原逻辑是直接操作 adapter
             // 这里我们简化：如果 checkAndAddSystemMessage 返回了新消息，我们添加到 adapter
             // 但由于我们无法直接获取刚才调用的返回值（因为没有修改方法签名），这里我们做一个简单的 UI 补丁：
             // 更好的做法是 checkAndAddSystemMessage 返回 ChatMessageBean，我们在 Fragment 里添加到 adapter
             // 这里为了保持 executeSendMessage 简洁，我们假设系统消息逻辑由 Manager 处理数据，Fragment 只负责刷新
             // 但 Adapter 是 UI 状态，必须同步。
             // 让我们回滚一点：executeSendMessage 里的 handleSystemMessage 调用改为：
             
             ChatMessageBean sysMsg = ChatSessionManager.getInstance().checkAndAddSystemMessage(
                 currentSession.getId(), time, mChatAdapter.getData());
             if (sysMsg != null) {
                 mChatAdapter.addItem(sysMsg);
             }
        }

        // 检查是否选中了总结 CheckBox，根据类型获取并追加总结
        String messageToSend = result;
        String summaryTag = null; // 用于显示的标记

        boolean useLatestSummary = cbLatestSummary != null && cbLatestSummary.isChecked();
        boolean useAllSummary = cbAllSummary != null && cbAllSummary.isChecked();

        if (useLatestSummary || useAllSummary) {
            EasyLog.print(TAG, "带总结发送：" + (useLatestSummary ? "最近" : "全部") + "，会话ID: " + currentSession.getId());
            // 获取当前会话的总结
            List<run.yigou.gxzy.greendao.entity.ChatSummaryBean> summaries =
                    ChatSessionManager.getInstance().getSessionSummaries(currentSession.getId());
            EasyLog.print(TAG, "带总结发送：找到 " + (summaries != null ? summaries.size() : 0) + " 条总结");

            if (summaries != null && !summaries.isEmpty()) {
                StringBuilder summaryContent = new StringBuilder();

                if (useLatestSummary) {
                    // 只发送最近一次总结（列表按创建时间降序，最新的在第一个）
                    run.yigou.gxzy.greendao.entity.ChatSummaryBean latestSummary = summaries.get(0);
                    if (latestSummary.getContent() != null && !latestSummary.getContent().isEmpty()) {
                        summaryContent.append(latestSummary.getContent());
                    }
                    summaryTag = "[最近历史总结]";
                } else {
                    // 发送全部总结（按时间顺序，从旧到新）
                    for (int i = summaries.size() - 1; i >= 0; i--) {
                        run.yigou.gxzy.greendao.entity.ChatSummaryBean summary = summaries.get(i);
                        if (summary.getContent() != null && !summary.getContent().isEmpty()) {
                            if (summaryContent.length() > 0) {
                                summaryContent.append("\n\n---\n\n");
                            }
                            summaryContent.append(summary.getContent());
                        }
                    }
                    summaryTag = "[全部历史总结]";
                }

                if (summaryContent.length() > 0) {
                    messageToSend = result + "\n\n[历史总结]:\n" + summaryContent.toString();
                    EasyLog.print(TAG, "带总结发送：已追加总结，总结内容长度: " + summaryContent.length());
                }
            } else {
                EasyLog.print(TAG, "带总结发送：当前会话没有总结数据");
            }
        }

        // 确定显示在聊天框中的内容（如果带总结，添加标记）
        String displayContent = result;
        if (summaryTag != null && !messageToSend.equals(result)) {
            displayContent = result + "\n" + summaryTag;
        }

        // 添加发送消息（显示带标记的内容）
        ChatMessageBean chatMessageBeanSend = new ChatMessageBean(ChatMessageBean.TYPE_SEND, "", "", displayContent);
        chatMessageBeanSend.setSessionId(currentSession.getId());
        chatMessageBeanSend.setCreateDate(DateHelper.getSeconds1());
        chatMessageBeanSend.setIsDelete(ChatMessageBean.IS_Delete_NO);
        // 保存发送消息到数据库
        long sendMsgId = ChatSessionManager.getInstance().saveMessage(chatMessageBeanSend);
        chatMessageBeanSend.setId(sendMsgId);
        mChatAdapter.addItem(chatMessageBeanSend);
        EasyLog.print(TAG, "Saved sent message to database with ID: " + sendMsgId + " and session ID: " + currentSession.getId());

        // 更新会话预览和时间
        currentSession.setPreview("我: " + result);
        currentSession.setUpdateTime(DateHelper.getSeconds1());
        ChatSessionManager.getInstance().updateSession(currentSession);

        // 1. 先创建并添加 "思考中" 消息
        final ChatMessageBean thinkingMessage = new ChatMessageBean(ChatMessageBean.TYPE_THINKING, "Ai", "", "正在思考...");
        thinkingMessage.setSessionId(currentSession.getId());
        thinkingMessage.setCreateDate(DateHelper.getSeconds1());
        thinkingMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);

        // 保存思考消息到数据库
        long thinkingMsgId = ChatSessionManager.getInstance().saveMessage(thinkingMessage);
        thinkingMessage.setId(thinkingMsgId);
        mChatAdapter.addItem(thinkingMessage);
        EasyLog.print(TAG, "Added thinking message with ID: " + thinkingMsgId);

        // 滚动到最后
        if (rv_chat != null) {
            rv_chat.scrollToPosition(mChatAdapter.getData().size() - 1);
            rv_chat.clearOnScrollListeners();
            rv_chat.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    scrollState = newState;
                }
            });
            // 阻止子 View 获取焦点，防止 notifyItemChanged 时 TextView 获取焦点导致滚动跳动
            rv_chat.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }

        // 发送消息给GPT - 使用 AiChatManager
        AiChatManager.getInstance().sendMessage(
                currentSession,
                messageToSend,
                thinkingMessage,
                new ChatUiStreamListener(thinkingMessage, true, true));
    }

    /**
     * 发送消息（核心逻辑）
     */
    private void sendMsg(String result) {
        if (mChatAdapter != null) {
            String time = sdf.format(new Date());

            // 如果当前没有会话，创建一个新的会话
            if (currentSession == null) {
                createNewSession("新对话");
            }

            // 确保会话已保存到数据库 (为了确保有 ID)
            ensureSessionSaved();

            // 使用 Manager 检查会话并执行
            AiChatManager.getInstance().checkSessionAndExecute(this, currentSession, new AiChatManager.SessionCheckCallback() {
                @Override
                public void onSessionValid(ChatSessionBean session) {
                    // 会话有效，执行发送逻辑
                    executeSendMessage(result, time);
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "会话检查失败: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    /**
     * 显示清空所有会话的确认对话框
     */
    private void showClearAllSessionsDialog() {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("清空所有会话")
                .setMessage("确定要删除所有聊天会话吗？此操作不可恢复！")
                .setPositiveButton("确定", (dialog, which) -> {
                    clearAllSessions();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 清空所有会话和消息
     */
    private void clearAllSessions() {
        ChatSessionManager.getInstance().clearAllSessions();
        
        // 清空当前会话
        currentSession = null;
        
        // 清空聊天列表
        if (mChatAdapter != null) {
            mChatAdapter.setData(new ArrayList<>());
        }
        
        // 刷新侧边栏历史列表
        if (chatHistoryAdapter != null) {
            chatHistoryAdapter.setData(new ArrayList<>());
        }
        
        // 关闭侧边栏
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        
        // 提示用户
        toast("所有会话已清空");
        
        // 重置标题
        if (getTitleBar() != null) {
            getTitleBar().setTitle(getString(R.string.app_name_ai));
        }
    }
    
    // ==========================================
    // Summary Feature Logic
    // ==========================================
    /**
     * 生成当前会话的总结
     */
    private void generateSessionSummary() {
        if (currentSession == null) {
            Toast.makeText(getContext(), "请先选择一个会话", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取当前会话的所有消息
        currentSession.resetMessages();
        List<ChatMessageBean> messages = currentSession.getMessages();
        if (messages == null || messages.isEmpty()) {
            Toast.makeText(getContext(), "当前会话没有消息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 构建总结请求的 prompt
        // 使用 Manager 生成 Prompt
        String prompt = AiChatManager.getInstance().generateSummaryPrompt(messages);
        
        // 显示进度提示
        Toast.makeText(getContext(), "正在生成总结...", Toast.LENGTH_SHORT).show();
        
        // 使用 AiChatManager 生成总结 - 打字机效果
        final StringBuilder summaryContent = new StringBuilder();
        
        // 先创建总结消息并添加到列表（流式显示）
        final ChatMessageBean summaryMessage = new ChatMessageBean();
        summaryMessage.setType(ChatMessageBean.TYPE_SUMMARY);
        summaryMessage.setContent("");
        summaryMessage.setNick("会话总结");
        summaryMessage.setCreateDate(DateHelper.getSeconds1());
        summaryMessage.setSessionId(currentSession.getId());
        summaryMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
        summaryMessage.setStreaming(true); // 标记为流式传输中
        
        // 添加到聊天列表
        runOnUiThread(() -> {
            if (mChatAdapter != null) {
                mChatAdapter.addItem(summaryMessage);
                // 滚动到底部
                if (rv_chat != null) {
                    rv_chat.scrollToPosition(mChatAdapter.getItemCount() - 1);
                }
            }
        });
        
        AiChatManager.getInstance().generateSummary(currentSession, prompt, new AiChatManager.ChatStreamListener() {
            @Override
            public void onThinking(String content) {
                // 总结模式通常没有思考过程，或者忽略
            }

            @Override
            public void onAnswerStart(ChatMessageBean answerMessage) {
                // 不使用，因为我们已经手动创建了 summaryMessage
            }

            @Override
            public void onAnswerChunk(String content) {
                summaryContent.append(content);
                
                // 实时更新消息内容（打字机效果）
                runOnUiThread(() -> {
                    summaryMessage.setContent(summaryContent.toString());
                    if (mChatAdapter != null) {
                        int position = mChatAdapter.getData().indexOf(summaryMessage);
                        if (position >= 0) {
                            mChatAdapter.notifyItemChanged(position, TipsAiChatAdapter.PAYLOAD_UPDATE_CONTENT);
                        }
                    }
                    // 滚动到底部
                    scrollToAbsoluteBottom();
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    // 标记流式传输结束
                    summaryMessage.setStreaming(false);
                    
                    if (summaryContent.length() > 0) {
                        summaryMessage.setContent(summaryContent.toString());
                        
                        // 保存到聊天消息数据库
                        long msgId = ChatSessionManager.getInstance().saveMessage(summaryMessage);
                        summaryMessage.setId(msgId);
                        
                        // 刷新显示（应用 Markdown 渲染）
                        if (mChatAdapter != null) {
                            int position = mChatAdapter.getData().indexOf(summaryMessage);
                            if (position >= 0) {
                                mChatAdapter.notifyItemChanged(position);
                            }
                        }

                        toast("总结已生成，长按可选择采用");
                    } else {
                        // 生成失败，移除消息
                        if (mChatAdapter != null) {
                            int position = mChatAdapter.getData().indexOf(summaryMessage);
                            if (position >= 0) {
                                mChatAdapter.removeItem(position);
                            }
                        }
                        toast("生成总结失败");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // 移除消息
                    if (mChatAdapter != null) {
                        int position = mChatAdapter.getData().indexOf(summaryMessage);
                        if (position >= 0) {
                            mChatAdapter.removeItem(position);
                        }
                    }
                    toast("生成总结失败: " + error);
                });
            }
        });
    }
    
    /**
     * 保存总结到数据库并显示
     */
    private void saveSummary(String content) {
        ChatSummaryBean summary = new ChatSummaryBean();
        summary.setSessionId(currentSession.getId());
        summary.setTitle("总结 " + sdf.format(new Date()));
        summary.setContent(content);
        summary.setCreateTime(DateHelper.getSeconds1());
        summary.setIsDelete(ChatSummaryBean.IS_Delete_NO);
        
        long id = ChatSessionManager.getInstance().saveSummary(summary);
        summary.setId(id);

        toast("总结已保存");
        
        // 显示总结内容
        showSummaryContentDialog(summary);
    }
    
    /**
     * 显示会话的总结列表对话框
     */
    private void showSummaryListDialog(ChatSessionBean session) {
        if (session == null) return;
        
        new ChatSummaryListDialog.Builder(getContext())
                .setSession(session.getId(), session.getTitle())
                .setMarkwon(mMarkwon)
                .show();
    }
    
    /**
     * 显示总结内容对话框（支持复制）
     */
    private void showSummaryContentDialog(ChatSummaryBean summary) {
        if (summary == null) return;
        
        // 创建可选择文字的 TextView
        TextView textView = new TextView(getContext());
        textView.setText(summary.getContent());
        textView.setTextIsSelectable(true);
        textView.setPadding(48, 32, 48, 32);
        textView.setTextSize(15);
        textView.setLineSpacing(0, 1.3f);
        
        // 使用 Markwon 渲染 Markdown
        if (mMarkwon != null) {
            mMarkwon.setMarkdown(textView, summary.getContent());
        }
        
        // 包装在 ScrollView 中以支持长文本
        android.widget.ScrollView scrollView = new android.widget.ScrollView(getContext());
        scrollView.addView(textView);
        
        // 设置最大高度为屏幕高度的 70%
        android.util.DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int maxHeight = (int) (displayMetrics.heightPixels * 0.7);
        scrollView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // 创建对话框 - 保存 textView 引用用于复制
        final TextView finalTextView = textView;
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(summary.getTitle())
                .setView(scrollView)
                .setPositiveButton("复制全部", (d, which) -> {
                    // 复制渲染后的纯文本，而非原始 Markdown
                    String renderedText = finalTextView.getText().toString();
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("会话总结", renderedText);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("删除", (d, which) -> {
                    // 删除总结
                    summary.setIsDelete(ChatSummaryBean.IS_Delete_YES);
                    ChatSessionManager.getInstance().updateSummary(summary);
                    Toast.makeText(getContext(), "总结已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("关闭", null)
                .create();
        
        dialog.show();
        
        // 限制对话框最大高度
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.min(maxHeight, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
    
    /**
     * 将总结显示在聊天框中并保存到聊天消息表
     * 不点采用：总结保存到聊天消息表
     * 点击采用：额外保存到总结表（ChatSummaryBean）
     */
    private void displaySummaryInChat(String content) {
        if (currentSession == null) {
            toast("当前没有会话");
            return;
        }
        
        // 创建一个总结类型的消息
        ChatMessageBean summaryMessage = new ChatMessageBean();
        summaryMessage.setType(ChatMessageBean.TYPE_SUMMARY);
        summaryMessage.setContent(content);
        summaryMessage.setNick("会话总结");
        summaryMessage.setCreateDate(DateHelper.getSeconds1());
        summaryMessage.setSessionId(currentSession.getId());
        summaryMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
        
        // 保存到聊天消息数据库
        long msgId = ChatSessionManager.getInstance().saveMessage(summaryMessage);
        summaryMessage.setId(msgId);
        
        // 添加到聊天列表显示
        if (mChatAdapter != null) {
            mChatAdapter.addItem(summaryMessage);
            
            // 滚动到底部
            if (rv_chat != null) {
                rv_chat.post(() -> {
                    rv_chat.scrollToPosition(mChatAdapter.getItemCount() - 1);
                    rv_chat.post(() -> rv_chat.scrollBy(0, 10000));
                });
            }
        }

        toast("总结已生成，长按可选择采用");
    }
    
    /**
     * 采用总结：将总结消息保存到数据库（消息保留在聊天列表中）
     */
    public void adoptSummary(ChatMessageBean summaryMessage) {
        if (summaryMessage == null || currentSession == null) return;
        
        ChatSummaryBean summary = new ChatSummaryBean();
        summary.setSessionId(currentSession.getId());
        summary.setTitle("总结 " + sdf.format(new Date()));
        summary.setContent(summaryMessage.getContent());
        summary.setCreateTime(DateHelper.getSeconds1());
        summary.setIsDelete(ChatSummaryBean.IS_Delete_NO);
        
        long id = ChatSessionManager.getInstance().saveSummary(summary);
        summary.setId(id);
        
        Toast.makeText(getContext(), "总结已保存", Toast.LENGTH_SHORT).show();
        
        // 消息保留在聊天列表中，不移除
    }
    
    // ==========================================
    // Summary Feature Logic
    // ==========================================

    
    // ==========================================
    // Message Action Logic
    // ==========================================
    /**
     * 显示消息操作菜单（在点击位置弹出）
     */
    private void showMessageActionMenu(android.view.View view, ChatMessageBean message, float x, float y) {
        if (message == null || view == null || getContext() == null) return;
        
        // 准备菜单项
        String[] items;
        switch (message.getType()) {
            case ChatMessageBean.TYPE_SEND: // 用户消息：重发、删除、复制
                items = new String[]{"重发", "删除", "复制"};
                break;
            case ChatMessageBean.TYPE_RECEIVED: // AI消息：删除、复制
            case ChatMessageBean.TYPE_THINKING:
                items = new String[]{"删除", "复制"};
                break;
            case ChatMessageBean.TYPE_SUMMARY: // 总结消息：复制、删除、采用
                items = new String[]{"复制", "删除", "采用"};
                break;
            default:
                return;
        }
        
        // 创建菜单列表视图（紧凑布局）
        android.widget.LinearLayout menuLayout = new android.widget.LinearLayout(getContext());
        menuLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        // 使用纯色背景代替 frame drawable 以避免额外空白
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(android.graphics.Color.WHITE);
        background.setCornerRadius(8);
        background.setStroke(1, android.graphics.Color.LTGRAY);
        menuLayout.setBackground(background);
        menuLayout.setPadding(4, 4, 4, 4);
        
        final int messageType = message.getType();
        final String[] menuItems = items;
        for (int i = 0; i < items.length; i++) {
            android.widget.TextView menuItem = new android.widget.TextView(getContext());
            menuItem.setText(items[i]);
            menuItem.setPadding(24, 16, 24, 16);
            menuItem.setTextSize(14);
            menuItem.setTextColor(android.graphics.Color.BLACK);
            menuItem.setBackgroundResource(android.R.drawable.list_selector_background);
            menuLayout.addView(menuItem);
        }
        
        // 创建 PopupWindow（紧凑尺寸）
        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
            menuLayout,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(4);
        
        // 设置菜单项点击事件
        for (int i = 0; i < menuLayout.getChildCount(); i++) {
            final int index = i;
            menuLayout.getChildAt(i).setOnClickListener(v -> {
                popupWindow.dismiss();
                String clickedItem = menuItems[index];
                
                if ("重发".equals(clickedItem)) {
                    resendMessage(message);
                } else if ("删除".equals(clickedItem)) {
                    deleteMessage(message);
                } else if ("复制".equals(clickedItem)) {
                    copyToClipboard(message.getContent());
                } else if ("采用".equals(clickedItem)) {
                    adoptSummary(message);
                }
            });
        }
        
        // 计算弹出位置
        android.view.View decorView = getActivity().getWindow().getDecorView();
        int[] location = new int[2];
        decorView.getLocationOnScreen(location);
        int popupX = (int) x - location[0];
        int popupY = (int) y - location[1];
        
        // 在点击位置显示
        popupWindow.showAtLocation(decorView, android.view.Gravity.NO_GRAVITY, popupX, popupY);
    }
    
    /**
     * 复制到剪贴板（转换 Markdown 为纯文本）
     */
    private void copyToClipboard(String content) {
        // 将 Markdown 转换为纯文本
        String plainText = MarkdownUtils.convertMarkdownToPlainText(content);
        
        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("聊天内容", plainText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 删除消息（从列表和数据库中彻底删除）
     */
    private void deleteMessage(ChatMessageBean message) {
        // 1. 从数据库彻底删除记录
        ChatSessionManager.getInstance().deleteMessage(message);
        
        // 2. 从列表中移除
        if (mChatAdapter != null) {
            int position = mChatAdapter.getData().indexOf(message);
            if (position >= 0) {
                mChatAdapter.removeItem(position);
            }
        }
        Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 问题重发（用户）
     */
    private void resendMessage(ChatMessageBean message) {
        String content = message.getContent();
        if (android.text.TextUtils.isEmpty(content)) return;
        
        // 调用发送逻辑
        sendMsg(content);
    }
}