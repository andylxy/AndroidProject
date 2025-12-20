package run.yigou.gxzy.ui.fragment;

import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;

import android.app.AlertDialog;
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
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.AiSessionApi;
import run.yigou.gxzy.http.api.AiSessionIdApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.tips.adapter.TipsAiChatAdapter;

import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.entity.ChatSessionBean;


import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.ThreadUtil;
import run.yigou.gxzy.ui.tips.adapter.ChatHistoryAdapter;

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

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.hjq.bar.OnTitleBarListener;
import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import run.yigou.gxzy.http.api.AiStreamApi;
import run.yigou.gxzy.http.callback.SseStreamCallback;
import run.yigou.gxzy.http.model.SseChunk;

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
    private static final String PREFS_NAME = "AiChatPrefs";
    private static final String KEY_LAST_SESSION_ID = "last_session_id";
    
    private RecyclerView rv_chat;
    private DrawerLayout drawerLayout;
    private RecyclerView chatHistoryList;
    private run.yigou.gxzy.ui.tips.adapter.ChatHistoryAdapter chatHistoryAdapter; // 修改导入路径
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private int scrollState = 0;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    // UI 更新节流相关
    private Handler uiUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable answerUpdateRunnable = null;
    private static final long UI_UPDATE_INTERVAL = 200; // 200ms 批量更新一次

    // 当前会话
    private ChatSessionBean currentSession;

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
        mMarkwon = Markwon.builder(getContext())
                .usePlugin(CorePlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(getContext()))
                .usePlugin(TaskListPlugin.create(getContext()))
                .usePlugin(ImagesPlugin.create())
                .build();
        // 初始化状态栏,设置为沉浸式
        getStatusBarConfig().setTitleBar(this, findViewById(R.id.tv_title));
        getStatusBarConfig().setTitleBar(this, findViewById(R.id.side_panel));
        rv_chat = findViewById(R.id.rv_chat);
        drawerLayout = findViewById(R.id.drawer_layout);
        chatHistoryList = findViewById(R.id.chat_history_list);

        // 初始化聊天历史记录列表
        chatHistoryAdapter = new ChatHistoryAdapter(getActivity());
        chatHistoryList.setAdapter(chatHistoryAdapter);
        chatHistoryList.setLayoutManager(new LinearLayoutManager(getContext()));

        // 设置聊天历史记录项点击监听
        chatHistoryAdapter.setOnChatHistoryItemClickListener(new ChatHistoryAdapter.OnChatHistoryItemClickListener() {
            @Override
            public void onChatHistoryItemClick(int position, ChatHistoryAdapter.ChatHistoryItem item) {
                // 获取会话列表
                List<ChatSessionBean> sessions = DbService.getInstance().mChatSessionBeanService.findAll();
                // 确保位置有效
                if (position >= 0 && position < sessions.size()) {
                    ChatSessionBean selectedSession = sessions.get(position);
                    // 保存最后选中的会话ID
                    saveLastSessionId(selectedSession.getId());
                    // 加载选中会话的所有聊天数据
                    loadChatDataForSession(selectedSession.getId());

                    // 关闭侧边栏
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawers();
                    }
                }
            }
        });

        // 设置聊天历史记录项删除监听
        chatHistoryAdapter.setOnChatHistoryItemDeleteListener(new ChatHistoryAdapter.OnChatHistoryItemDeleteListener() {
            @Override
            public void onChatHistoryItemDelete(int position, ChatHistoryAdapter.ChatHistoryItem item) {
                // 获取会话列表
                List<ChatSessionBean> sessions = DbService.getInstance().mChatSessionBeanService.findAll();
                // 确保位置有效
                if (position >= 0 && position < sessions.size()) {
                    // 从数据库中删除会话
                    ChatSessionBean sessionToDelete = sessions.get(position);
                    DbService.getInstance().mChatSessionBeanService.deleteEntity(sessionToDelete);

                    // 如果删除的是当前会话，清空聊天界面
                    if (currentSession != null && currentSession.getId().equals(sessionToDelete.getId())) {
                        currentSession = null;
                        if (mChatAdapter != null) {
                            mChatAdapter.setData(new ArrayList<ChatMessageBean>());
                        }
                    }

                    // 重新加载聊天历史记录
                    populateChatHistoryWithTestData();
                }
            }
        });

        // 设置聊天历史记录项编辑标题监听
        chatHistoryAdapter.setOnChatHistoryItemEditTitleListener(new ChatHistoryAdapter.OnChatHistoryItemEditTitleListener() {
            @Override
            public void onChatHistoryItemEditTitle(int position, ChatHistoryAdapter.ChatHistoryItem item) {
                // 获取会话列表
                List<ChatSessionBean> sessions = DbService.getInstance().mChatSessionBeanService.findAll();
                // 确保位置有效
                if (position >= 0 && position < sessions.size()) {
                    ChatSessionBean sessionToEdit = sessions.get(position);
                    showEditSessionTitleDialog(sessionToEdit);
                }
            }
        });

        // 设置标题栏点击监听
        if (getTitleBar() != null) {
            getTitleBar().setOnTitleBarListener(this);
        }

        // 禁止通过边缘滑动手势打开侧边栏
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }

        // 获取 Activity
        Activity activity = getActivity();
        if (activity == null) return;
        // 创建并设置适配器
        mChatAdapter = new TipsAiChatAdapter(activity);
        mChatAdapter.setHasStableIds(true);
        // 设置 RecyclerView 的动画时长
        //rv_chat.getItemAnimator().setChangeDuration(0);
        rv_chat.setItemAnimator(null); // 禁用动画
        rv_chat.setNestedScrollingEnabled(false); // 禁用嵌套滚动，让内部 ScrollView 可以滚动
        // 设置 RecyclerView 的布局管理器和适配器
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rv_chat.setLayoutManager(layoutManager);
        rv_chat.setAdapter(mChatAdapter);
        
        // 设置打字机渲染回调，实现打字时同步滚动
        TipsAiChatAdapter.setScrollCallback(() -> scrollToAbsoluteBottom());

        // 注册事件
        XEventBus.getDefault().register(AiMsgFragment.this);
        // 初始化消息数据
        initMsgs();

        // 添加调试日志，检查组件是否正确初始化
        EasyLog.print(TAG, "initView: rv_chat=" + rv_chat);
        EasyLog.print(TAG, "initView: mChatAdapter=" + mChatAdapter);
        EasyLog.print(TAG, "initView: layoutManager=" + layoutManager);

        // 设置EditText的输入监听器，确保光标始终在最左侧
        EditText chatContent = findViewById(R.id.chat_content);
        ImageView clearButton = findViewById(R.id.iv_clear);

        // 设置清除按钮的点击事件
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatContent.setText("");
            }
        });

        // 合并所有的文本监听功能到一个监听器中
        chatContent.addTextChangedListener(new TextWatcher() {
            int lines = 1;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 根据文本内容控制清除按钮的显示和隐藏
                if (s.length() > 0) {
                    clearButton.setVisibility(View.VISIBLE);
                } else {
                    clearButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 获取当前行数
                int currentLines = chatContent.getLineCount();

                // 如果行数增加，滚动到顶部以实现向上扩展的效果
                if (currentLines > lines) {
                    chatContent.scrollTo(0, 0);
                }

                lines = currentLines;
            }
        });

        chatContent.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // 光标始终在最左侧
                    chatContent.setSelection(0, 0);
                    // 根据文本内容控制清除按钮的显示和隐藏
                    if (chatContent.getText().length() > 0) {
                        clearButton.setVisibility(View.VISIBLE);
                    } else {
                        clearButton.setVisibility(View.GONE);
                    }
                }
            }
        });
        
        // 设置键盘回车/发送键监听，按下时发送消息
        chatContent.setOnKeyListener((v, keyCode, event) -> {
            // 是否是回车键
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                String msg = chatContent.getText().toString().trim();
                if (!msg.isEmpty()) {
                    // 隐藏键盘
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                            requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null && requireActivity().getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 
                                android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    // 触发发送消息（调用发送按钮的点击逻辑）
                    Button sendButton = findViewById(R.id.chat_send);
                    if (sendButton != null) {
                        sendButton.performClick();
                    }
                }
                return true; // 消费事件，防止换行
            }
            return false;
        });

        // 设置侧边栏清空所有会话按钮点击事件
        ImageButton clearAllButton = findViewById(R.id.btn_edit_title);
        clearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearAllSessionsDialog();
            }
        });
    }


    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(priority = 1)
    public void ChatMessageEvent(ChatMessageBeanEvent event) {
        ThreadUtil.runOnUiThread(() -> {
            if (event.isClear()) {
                initData();
                mChatAdapter.notifyDataSetChanged();

                // 重新填充聊天历史记录测试数据
                populateChatHistoryWithTestData();
            }
        });
    }


    /**
     * 填充聊天历史记录列表测试数据
     */
    private void populateChatHistoryWithTestData() {
        List<ChatSessionBean> sessions = DbService.getInstance().mChatSessionBeanService.findAll();
        
        // 按更新时间倒序排序（最新的在最前面）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sessions.sort((s1, s2) -> {
                String time1 = s1.getUpdateTime() != null ? s1.getUpdateTime() : "";
                String time2 = s2.getUpdateTime() != null ? s2.getUpdateTime() : "";
                return time2.compareTo(time1); // 倒序：time2 - time1
            });
        }

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
        ChatSessionBean newSession = new ChatSessionBean();
        newSession.setTitle("新对话");
        newSession.setPreview("开始新的对话");
        String currentTime = DateHelper.getSeconds1();
        newSession.setUpdateTime(currentTime);
        newSession.setCreateTime(currentTime); // 不能为null，因为是@NotNull字段
        newSession.setIsDelete(ChatSessionBean.IS_Delete_NO);
        
        // 保存到数据库
        long sessionId = DbService.getInstance().mChatSessionBeanService.addEntity(newSession);
        newSession.setId(sessionId);
        currentSession = newSession;
        
        EasyLog.print(TAG, "Created new session with ID: " + sessionId);
        
        // 申请会话ID
        EasyHttp.get(AiMsgFragment.this)
                .api(new AiSessionIdApi())
                .request(new HttpCallback<HttpData<AiSessionIdApi.Bean>>(AiMsgFragment.this) {
                    
                    @Override
                    public void onSucceed(HttpData<AiSessionIdApi.Bean> data) {
                        if (data != null && data.isRequestSucceed()) {
                            AiSessionIdApi.Bean bean = data.getData();
                            if (bean != null && currentSession != null) {
                                // 设置会话Id
                                currentSession.setConversationId(bean.getRealConversationId());
                                currentSession.setEndUserId(bean.getEndUserId());
                                currentSession.setCreateTime(DateHelper.getSeconds1());
                                
                                // 更新数据库
                                DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);
                                
                                EasyLog.print(TAG, "Session ID obtained: " + bean.getRealConversationId());
                                
                                // 刷新会话列表UI
                                populateChatHistoryWithTestData();
                            }
                        } else {
                            EasyLog.print(TAG, "Failed to obtain session ID: " + (data != null ? data.getMessage() : "null response"));
                            Toast.makeText(getContext(), "会话创建失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        EasyLog.print(TAG, "Failed to request session ID: " + e.getMessage());
                        Toast.makeText(getContext(), "网络请求失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    /**
     * 保存最后选中的会话ID
     */
    private void saveLastSessionId(Long sessionId) {
        if (getContext() == null) return;
        
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_SESSION_ID, sessionId).apply();
        EasyLog.print(TAG, "Saved last session ID: " + sessionId);
    }
    
    /**
     * 读取最后选中的会话ID
     */
    private Long getLastSessionId() {
        if (getContext() == null) {
            EasyLog.print(TAG, "getLastSessionId: Context is null");
            return null;
        }
        
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long sessionId = prefs.getLong(KEY_LAST_SESSION_ID, -1L);
        EasyLog.print(TAG, "getLastSessionId: Read session ID from SharedPreferences: " + sessionId);
        return sessionId > 0 ? sessionId : null;
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
        ChatSessionBean session = DbService.getInstance().mChatSessionBeanService.findById(sessionId);
        if (session == null) {
            EasyLog.print(TAG, "Session not found for ID: " + sessionId);
            loadDefaultSessionData();
            return;
        }

        // 设置当前会话
        currentSession = session;

        // 获取会话中的所有消息
        List<ChatMessageBean> messages = session.getMessages();

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
        populateChatHistoryWithTestData();

        Button chat_send = findViewById(R.id.chat_send);
        chat_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String result = ((EditText) findViewById(R.id.chat_content)).getText().toString();
                if (!result.isEmpty()) {
                    if (mChatAdapter != null) {
                        String time = sdf.format(new Date());
                        // 如果当前没有会话，创建一个新的会话
                        if (currentSession == null) {
                            createNewSession("新对话");
                        } else {

                            // 检查会话ID是否存在（conversationId 或 endUserId 为空需要重新申请）
                            if (currentSession.getConversationId() == null || 
                                currentSession.getConversationId().isEmpty() ||
                                currentSession.getEndUserId() == null ||
                                currentSession.getEndUserId().isEmpty()) {
                                // 会话ID缺失，重新申请
                                EasyLog.print(TAG, "Session missing conversationId or endUserId, requesting new ID");
                                requestNewSessionIdAndContinue(result, time);
                                return;
                            }
                            
                            // 检查会话创建时间是否为空或者会话是否过期
                            if (currentSession.getCreateTime() == null) {
                                // 如果会话创建时间为空，重新申请会话Id后再继续执行
                                requestNewSessionIdAndContinue(result, time);
                                return; // 暂停当前执行，等待会话ID申请完成后再继续
                            } else {
                                // 检查会话是否过期（会话有效期为6天）
                                long createTime = DateHelper.strDateToLong(currentSession.getCreateTime());
                                long currentTime = System.currentTimeMillis();
                                // 6天 = 6 * 24 * 60 * 60 * 1000 毫秒
                                if (currentTime - createTime > 6 * 24 * 60 * 60 * 1000L) {
                                    // 会话已过期，重新申请会话Id后再继续执行
                                    requestNewSessionIdAndContinue(result, time);
                                    return; // 暂停当前执行，等待会话ID申请完成后再继续
                                }
                            }
                        }

                        // 确保会话已保存到数据库
                        ensureSessionSaved();

                        // 处理系统消息
                        handleSystemMessage(time);

                        // 添加发送消息
                        ChatMessageBean chatMessageBeanSend = new ChatMessageBean(ChatMessageBean.TYPE_SEND, "", "", result);
                        chatMessageBeanSend.setSessionId(currentSession.getId());
                        chatMessageBeanSend.setCreateDate(DateHelper.getSeconds1());
                        chatMessageBeanSend.setIsDelete(ChatMessageBean.IS_Delete_NO);
                        // 保存发送消息到数据库
                        long sendMsgId = DbService.getInstance().mChatMessageBeanService.addEntity(chatMessageBeanSend);
                        chatMessageBeanSend.setId(sendMsgId);
                        mChatAdapter.addItem(chatMessageBeanSend);
                        EasyLog.print(TAG, "Saved sent message to database with ID: " + sendMsgId + " and session ID: " + currentSession.getId());

                        // 更新会话预览和时间
                        currentSession.setPreview("我: " + result);
                        currentSession.setUpdateTime(DateHelper.getSeconds1());
                        DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);

                        // 1. 先创建并添加 "思考中" 消息
                        final ChatMessageBean thinkingMessage = new ChatMessageBean(ChatMessageBean.TYPE_THINKING, "Ai", "", "正在思考...");
                        thinkingMessage.setSessionId(currentSession.getId());
                        thinkingMessage.setCreateDate(DateHelper.getSeconds1());
                        thinkingMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                        
                        // 保存思考消息到数据库
                        long thinkingMsgId = DbService.getInstance().mChatMessageBeanService.addEntity(thinkingMessage);
                        thinkingMessage.setId(thinkingMsgId);
                        mChatAdapter.addItem(thinkingMessage);
                        EasyLog.print(TAG, "Added thinking message with ID: " + thinkingMsgId);

                        // 准备 "回答" 消息的引用（初始为空，等到有回答时再创建）
                        final ChatMessageBean[] answerMessageRef = {null};
                        final boolean[] isThinkingPhase = {true}; // 标记当前是否处于思考阶段

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

                        // 发送消息给GPT - 使用 SSE 流式请求
                        new AiStreamApi()
                                .setQuery(result)
                                .setConversationId(currentSession.getConversationId())
                                .setEndUserId(currentSession.getEndUserId())
                                .execute(new SseStreamCallback() {
                                    
                                    @Override
                                    public void onOpen() {
                                        EasyLog.print(TAG, "SSE 连接已建立");
                                    }
                                    
                                    @Override
                                    public void onChunk(SseChunk chunk) {
                                        if (chunk == null) {
                                            EasyLog.print(TAG, "接收到空数据块");
                                            return;
                                        }
                                        
                                        // EasyLog.print(TAG, "SSE 数据块: type=" + chunk.getType());
                                        
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 判断是否是思考过程
                                                boolean isThinkingChunk = "thinking".equals(chunk.getType()) || chunk.isThinking();
                                                
                                                if (isThinkingChunk) {
                                                    // === 处理思考过程 ===
                                                    String currentContent = thinkingMessage.getContent();
                                                    if ("正在思考...".equals(currentContent)) {
                                                        currentContent = "";
                                                    }
                                                    thinkingMessage.setContent(currentContent + (chunk.getContent() != null ? chunk.getContent() : ""));
                                                    
                                                    // 刷新思考消息 UI
                                                    // 找到 adapter 中的位置并刷新
                                                    int pos = mChatAdapter.getData().indexOf(thinkingMessage);
                                                    if (pos != -1) {
                                                        mChatAdapter.notifyItemChanged(pos); // 局部刷新
                                                        
                                                        // 自动滚动 (思考阶段也需要保持在底部，以便用户看到正在生成的内容)
                                                        if (scrollState == 0) {
                                                            // 使用 smoothScrollToPosition 可能更流畅，但在快速更新时 scrollToPosition 更稳定
                                                            rv_chat.scrollToPosition(mChatAdapter.getData().size() - 1);
                                                        }
                                                    }
                                                    
                                                } else if ("chunk".equals(chunk.getType()) || "answer".equals(chunk.getType())) {
                                                    // === 处理正式回答 ===
                                                    
                                                    // 如果是第一次从思考切换到回答
                                                    if (isThinkingPhase[0]) {
                                                        isThinkingPhase[0] = false;
                                                        
                                                        // 1. 折叠思考消息
                                                        thinkingMessage.setThinkingCollapsed(true);
                                                        int thinkPos = mChatAdapter.getData().indexOf(thinkingMessage);
                                                        if (thinkPos != -1) {
                                                            mChatAdapter.notifyItemChanged(thinkPos);
                                                        }
                                                        // 更新数据库中的思考消息（保存完整思考过程）
                                                        DbService.getInstance().mChatMessageBeanService.updateEntity(thinkingMessage);
                                                        
                                                        // 2. 创建正式回答消息
                                                        ChatMessageBean answerMsg = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED, "Ai", "", "");
                                                        answerMsg.setSessionId(currentSession.getId());
                                                        answerMsg.setCreateDate(DateHelper.getSeconds1());
                                                        answerMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                                        answerMsg.setStreaming(true); // 初始设为流式
                                                        
                                                        long answerId = DbService.getInstance().mChatMessageBeanService.addEntity(answerMsg);
                                                        answerMsg.setId(answerId);
                                                        answerMessageRef[0] = answerMsg;
                                                        
                                                        mChatAdapter.addItem(answerMsg);
                                                    }
                                                    
                                                    // 追加回答内容
                                                    if (answerMessageRef[0] != null && chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                                                        String current = answerMessageRef[0].getContent();
                                                        answerMessageRef[0].setContent(current + chunk.getContent());
                                                        
                                                        // 使用节流更新 UI，减少重绘次数
                                                        scheduleAnswerUIUpdate(answerMessageRef[0]);
                                                    }
                                                    
                                                } else if ("error".equals(chunk.getType())) {
                                                    // 错误处理
                                                    EasyLog.print(TAG, "SSE 错误: " + chunk.getError());
                                                    if (answerMessageRef[0] != null) {
                                                        answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n[错误: " + chunk.getError() + "]");
                                                        mChatAdapter.updateData();
                                                    } else {
                                                        thinkingMessage.setContent(thinkingMessage.getContent() + "\n[错误: " + chunk.getError() + "]");
                                                        mChatAdapter.notifyDataSetChanged();
                                                    }
                                                }
                                            }
                                        });
                                    }
                                    
                                    @Override
                                    public void onComplete() {
                                        EasyLog.print(TAG, "SSE 流式对话完成");
                                        
                                        // 移除待处理的 UI 更新任务
                                        if (answerUpdateRunnable != null) {
                                            uiUpdateHandler.removeCallbacks(answerUpdateRunnable);
                                            answerUpdateRunnable = null;
                                        }
                                        
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 保存最终回答到数据库
                                                if (answerMessageRef[0] != null) {
                                                    answerMessageRef[0].setCreateDate(DateHelper.getSeconds1());
                                                    answerMessageRef[0].setStreaming(false); // 结束流式状态
                                                    DbService.getInstance().mChatMessageBeanService.updateEntity(answerMessageRef[0]);
                                                    
                                                    // 更新会话预览
                                                    String preview = answerMessageRef[0].getContent();
                                                    if (preview.length() > 30) {
                                                        preview = preview.substring(0, 30) + "...";
                                                    }
                                                    currentSession.setPreview("AI: " + preview);
                                                    currentSession.setUpdateTime(DateHelper.getSeconds1());
                                                    DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);
                                                } else {
                                                    // 如果没有回答（可能只有思考或出错了），更新思考消息
                                                    DbService.getInstance().mChatMessageBeanService.updateEntity(thinkingMessage);
                                                }
                                                
                                                // 强制折叠思考
                                                thinkingMessage.setThinkingCollapsed(true);
                                                int thinkPos = mChatAdapter.getData().indexOf(thinkingMessage);
                                                if (thinkPos != -1) {
                                                    mChatAdapter.notifyItemChanged(thinkPos);
                                                }
                                                
                                                // 最终刷新并滚动
                                                mChatAdapter.updateData();
                                                scrollToAbsoluteBottom();
                                                
                                                // 恢复焦点能力，允许用户选择文本
                                                if (rv_chat != null) {
                                                    rv_chat.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                                                }
                                            }
                                        });
                                    }
                                    
                                    @Override
                                    public void onError(Exception e) {
                                        EasyLog.print(TAG, "SSE 请求失败: " + e.getMessage());
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                String errStr = "网络请求失败: " + e.getMessage();
                                                if (answerMessageRef[0] != null) {
                                                    answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n" + errStr);
                                                    DbService.getInstance().mChatMessageBeanService.updateEntity(answerMessageRef[0]);
                                                } else {
                                                    thinkingMessage.setContent(thinkingMessage.getContent() + "\n" + errStr);
                                                    DbService.getInstance().mChatMessageBeanService.updateEntity(thinkingMessage);
                                                }
                                                mChatAdapter.updateData();
                                                
                                                // 恢复焦点能力
                                                if (rv_chat != null) {
                                                    rv_chat.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                                                }
                                            }
                                        });
                                    }
                                });
                    }
                    ((EditText) findViewById(R.id.chat_content)).getText().clear();
                }
            }
        });

        //setRightTitle("...");
        //getTitleBar().setRightTitleSize(26);
        //getTitleBar().setRightTitleColor(Color.BLACK);

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
                        // 标记流式状态
                        if (!answerMessage.isStreaming()) {
                            answerMessage.setStreaming(true);
                        }
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
     * 创建新会话
     *
     * @param title 会话标题
     */
    private void createNewSession(String title) {
        String currentTime = DateHelper.getSeconds1();
        ChatSessionBean newSession = new ChatSessionBean();
        newSession.setTitle(title);
        newSession.setPreview("新对话");
        newSession.setCreateTime(currentTime);
        newSession.setUpdateTime(currentTime);
        newSession.setIsDelete(ChatSessionBean.IS_Delete_NO);
        // 注意：这里不立即保存到数据库，等到用户第一次发送消息时再保存
        currentSession = newSession;

        EasyLog.print(TAG, "Created new session in memory only");
    }

    /**
     * 确保会话已保存到数据库
     */
    private void ensureSessionSaved() {
        if (currentSession.getId() == null) {
            String currentTime = DateHelper.getSeconds1();
            currentSession.setCreateTime(currentTime);
            currentSession.setUpdateTime(currentTime);
            long sessionId = DbService.getInstance().mChatSessionBeanService.addEntity(currentSession);
            currentSession.setId(sessionId);
            EasyLog.print(TAG, "Saved new session to database with ID: " + sessionId);

            // 保存当前会话ID到 SharedPreferences（确保下次不会被覆盖）
            saveLastSessionId(sessionId);

            // 更新标题栏标题
            if (getTitleBar() != null) {
                getTitleBar().setTitle(currentSession.getTitle());
            }

            // 会话保存到数据库后，只刷新侧边栏UI（不重新加载会话数据）
            refreshChatHistorySidebar();
        } else {
            EasyLog.print(TAG, "Session already exists in database with ID: " + currentSession.getId());
        }
    }
    
    /**
     * 只刷新聊天历史侧边栏UI，不重新加载会话数据
     */
    private void refreshChatHistorySidebar() {
        List<ChatSessionBean> sessions = DbService.getInstance().mChatSessionBeanService.findAll();
        
        // 按更新时间倒序排序
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sessions.sort((s1, s2) -> {
                String time1 = s1.getUpdateTime() != null ? s1.getUpdateTime() : "";
                String time2 = s2.getUpdateTime() != null ? s2.getUpdateTime() : "";
                return time2.compareTo(time1);
            });
        }

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
     * 处理系统消息
     *
     * @param time 时间戳
     */
    private void handleSystemMessage(String time) {
        // 检查消息列表中是否只有系统消息
        boolean onlySystemMessages = true;
        List<ChatMessageBean> currentMessages = mChatAdapter.getData();
        if (currentMessages != null && !currentMessages.isEmpty()) {
            //currentMessages只有一条消息,是不是系统消息,后续消息全部不再检查
            if (currentMessages.size() == 1) {
                ChatMessageBean message = currentMessages.get(0);
                if (message.getType() != ChatMessageBean.TYPE_SYSTEM) {
                    onlySystemMessages = false;
                }
            }

        } else {
            // 如果消息列表为空，视为只有系统消息的情况
            onlySystemMessages = true;
        }

        EasyLog.print(TAG, "Checking if should save system message, onlySystemMessages: " + onlySystemMessages);

        // 只有当消息列表中包含非系统消息时，才保存系统消息到数据库
        if (!onlySystemMessages) {
            // 检查并添加系统消息
            boolean messageExists = false;
            for (ChatMessageBean message : mChatAdapter.getData()) {
                if (message.getType() == ChatMessageBean.TYPE_SYSTEM && time.equals(message.getContent())) {
                    messageExists = true;
                    break;
                }
            }

            if (!messageExists) {
                ChatMessageBean chatMessageBeanSystem = new ChatMessageBean(ChatMessageBean.TYPE_SYSTEM, null, null, time);
                chatMessageBeanSystem.setSessionId(currentSession.getId());
                chatMessageBeanSystem.setCreateDate(DateHelper.getSeconds1());
                chatMessageBeanSystem.setIsDelete(ChatMessageBean.IS_Delete_NO);
                // 保存系统消息到数据库
                long systemMsgId = DbService.getInstance().mChatMessageBeanService.addEntity(chatMessageBeanSystem);
                chatMessageBeanSystem.setId(systemMsgId);
                mChatAdapter.addItem(chatMessageBeanSystem);
                EasyLog.print(TAG, "Saved system message to database with ID: " + systemMsgId + " and session ID: " + currentSession.getId());
            }
        } else {
            // 如果只有系统消息，添加系统消息但不保存到数据库
            boolean messageExists = false;
            for (ChatMessageBean message : mChatAdapter.getData()) {
                if (message.getType() == ChatMessageBean.TYPE_SYSTEM && time.equals(message.getContent())) {
                    messageExists = true;
                    break;
                }
            }

            if (!messageExists) {
                ChatMessageBean chatMessageBeanSystem = new ChatMessageBean(ChatMessageBean.TYPE_SYSTEM, null, null, time);
                chatMessageBeanSystem.setSessionId(currentSession.getId());
                chatMessageBeanSystem.setCreateDate(DateHelper.getSeconds1());
                chatMessageBeanSystem.setIsDelete(ChatMessageBean.IS_Delete_NO);
                // 不保存系统消息到数据库，只添加到界面
                mChatAdapter.addItem(chatMessageBeanSystem);
                EasyLog.print(TAG, "Added system message to UI only (not saved to database)");
            }
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
        //先后端请求会话Id
        EasyHttp.get(AiMsgFragment.this)
                .api(new AiSessionIdApi())
                .request(new HttpCallback<HttpData<AiSessionIdApi.Bean>>(AiMsgFragment.this) {

                    @Override
                    public void onSucceed(HttpData<AiSessionIdApi.Bean> data) {

                        if (data != null && data.isRequestSucceed()) {
                            AiSessionIdApi.Bean bean = data.getData();
                            if (currentSession != null) {
                                // 设置会话Id
                                currentSession.setConversationId(bean.getRealConversationId());
                                currentSession.setEndUserId(bean.getEndUserId());
                                EasyLog.print(TAG, "Session ID obtained: " + bean.getRealConversationId());
                            }
                        } else {
                            EasyLog.print("会话Id申请失败：" + data.getMessage());
                        }

                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);

                        EasyLog.print("会话Id申请失败：" + e.getMessage());
                    }
                });

        // 创建新会话
        createNewSession("新对话");

        // 清空输入框
        EditText chatContent = findViewById(R.id.chat_content);
        chatContent.setText("");

        // 注意：新创建的会话还没有保存到数据库，所以不能通过loadChatDataForSession加载
        // 直接清空聊天界面并显示系统消息
        if (mChatAdapter != null) {
            mChatAdapter.setData(new ArrayList<ChatMessageBean>());
            // 添加系统消息到界面（但不保存到数据库）
            ChatMessageBean systemMessage = new ChatMessageBean(
                    ChatMessageBean.TYPE_SYSTEM,
                    null,
                    null,
                    "开始新的对话 " + sdf.format(new Date()));
            systemMessage.setCreateDate(DateHelper.getSeconds1());
            systemMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
            mChatAdapter.addItem(systemMessage);
            EasyLog.print(TAG, "Added system message to UI only (not saved to database yet)");
        }

        // 更新标题栏标题
        if (getTitleBar() != null) {
            getTitleBar().setTitle("新对话");
        }

        // 关闭侧边栏（如果打开的话）
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
    }

    private ArrayList<ChatMessageBean> chatMessageBeans;

    @Override
    protected void initData() {
        EasyLog.print(TAG, "initData: Starting to initialize data");

        // 只有在数据库中有会话数据时才初始化
        List<ChatSessionBean> sessions = DbService.getInstance().mChatSessionBeanService.findAll();
        if (!sessions.isEmpty()) {
            //初始化聊天会话数据
            // 加载历史会话历史记录
            populateChatHistoryWithTestData();
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
                    DbService.getInstance().mChatSessionBeanService.updateEntity(session);

                    // 如果是当前会话，更新标题栏显示
                    if (currentSession != null && currentSession.getId().equals(session.getId())) {
                        if (getTitleBar() != null) {
                            getTitleBar().setTitle(newTitle);
                        }
                    }

                    // 重新加载侧边栏数据
                    populateChatHistoryWithTestData();
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

    /**
     * 显示编辑当前会话标题对话框
     */
    private void showEditTitleDialog() {
        if (currentSession == null) {
            Toast.makeText(getContext(), "当前没有选中的会话", Toast.LENGTH_SHORT).show();
            return;
        }

        showEditSessionTitleDialog(currentSession);
    }

    /**
     * 请求新的会话ID
     */
    private void requestNewSessionId() {
        //检测会话Id是否已过期
        EasyHttp.get(AiMsgFragment.this)
                .api(new AiSessionIdApi())
                .request(new HttpCallback<HttpData<AiSessionIdApi.Bean>>(AiMsgFragment.this) {

                    @Override
                    public void onSucceed(HttpData<AiSessionIdApi.Bean> data) {

                        if (data != null && data.isRequestSucceed()) {
                            AiSessionIdApi.Bean bean = data.getData();
                            if (currentSession != null) {
                                // 设置会话Id
                                currentSession.setConversationId(bean.getRealConversationId());
                                currentSession.setEndUserId(bean.getEndUserId());
                                currentSession.setCreateTime(DateHelper.getSeconds1());
                                EasyLog.print(TAG, "Session ID obtained: " + bean.getRealConversationId());
                            }
                        } else {
                            EasyLog.print("过期会话Id申请失败：" + data.getMessage());
                        }

                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);

                        EasyLog.print("过期会话Id申请失败：" + e.getMessage());
                    }
                });

    }
    
    // 保存用户输入的内容和时间，用于重新申请会话ID后继续执行
    private String pendingMessageContent;
    private String pendingMessageTime;

    /**
     * 请求新的会话ID并在完成后继续执行发送消息逻辑
     */
    private void requestNewSessionIdAndContinue(String messageContent, String messageTime) {
        // 保存待发送的消息内容和时间
        pendingMessageContent = messageContent;
        pendingMessageTime = messageTime;
        
        //检测会话Id是否已过期
        EasyHttp.get(AiMsgFragment.this)
                .api(new AiSessionIdApi())
                .request(new HttpCallback<HttpData<AiSessionIdApi.Bean>>(AiMsgFragment.this) {

                    @Override
                    public void onSucceed(HttpData<AiSessionIdApi.Bean> data) {

                        if (data != null && data.isRequestSucceed()) {
                            AiSessionIdApi.Bean bean = data.getData();
                            if (currentSession != null && bean != null) {
                                // 设置会话Id
                                currentSession.setConversationId(bean.getRealConversationId());
                                currentSession.setEndUserId(bean.getEndUserId());
                                currentSession.setCreateTime(DateHelper.getSeconds1());
                                EasyLog.print(TAG, "Session ID obtained: " + bean.getRealConversationId());
                                
                                // 会话ID申请成功后，继续执行发送消息的逻辑
                                continueSendingMessage();
                            }
                        } else {
                            EasyLog.print("过期会话Id申请失败：" + data.getMessage());
                            // 清除待发送消息
                            pendingMessageContent = null;
                            pendingMessageTime = null;
                        }

                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);

                        EasyLog.print("过期会话Id申请失败：" + e.getMessage());
                        // 清除待发送消息
                        pendingMessageContent = null;
                        pendingMessageTime = null;
                    }
                });
    }
    
    /**
     * 继续执行发送消息的逻辑
     */
    private void continueSendingMessage() {
        // 确保会话已保存到数据库
        ensureSessionSaved();

        // 处理系统消息
        handleSystemMessage(pendingMessageTime);

        // 添加发送消息
        ChatMessageBean chatMessageBeanSend = new ChatMessageBean(ChatMessageBean.TYPE_SEND, "", "", pendingMessageContent);
        chatMessageBeanSend.setSessionId(currentSession.getId());
        chatMessageBeanSend.setCreateDate(DateHelper.getSeconds1());
        chatMessageBeanSend.setIsDelete(ChatMessageBean.IS_Delete_NO);
        // 保存发送消息到数据库
        long sendMsgId = DbService.getInstance().mChatMessageBeanService.addEntity(chatMessageBeanSend);
        chatMessageBeanSend.setId(sendMsgId);
        mChatAdapter.addItem(chatMessageBeanSend);
        EasyLog.print(TAG, "Saved sent message to database with ID: " + sendMsgId + " and session ID: " + currentSession.getId());

        // 更新会话预览和时间
        currentSession.setPreview("我: " + pendingMessageContent);
        currentSession.setUpdateTime(DateHelper.getSeconds1());
        DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);

        // 1. 先创建并添加 "思考中" 消息
        final ChatMessageBean thinkingMessage = new ChatMessageBean(ChatMessageBean.TYPE_THINKING, "Ai", "", "正在思考...");
        thinkingMessage.setSessionId(currentSession.getId());
        thinkingMessage.setCreateDate(DateHelper.getSeconds1());
        thinkingMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
        
        // 保存思考消息到数据库
        long thinkingMsgId = DbService.getInstance().mChatMessageBeanService.addEntity(thinkingMessage);
        thinkingMessage.setId(thinkingMsgId);
        mChatAdapter.addItem(thinkingMessage);
        EasyLog.print(TAG, "Added thinking message with ID: " + thinkingMsgId);

        // 准备 "回答" 消息的引用（初始为空，等到有回答时再创建）
        final ChatMessageBean[] answerMessageRef = {null};
        final boolean[] isThinkingPhase = {true}; // 标记当前是否处于思考阶段

        // 滚动到最后一条消息
        if (rv_chat != null) {
            rv_chat.scrollToPosition(mChatAdapter.getData().size() - 1);
            rv_chat.clearOnScrollListeners();
            rv_chat.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    scrollState = newState;
                }
            });
        }

        // 发送消息给GPT - 使用 SSE 流式请求
        new AiStreamApi()
                .setQuery(pendingMessageContent)
                .setConversationId(currentSession.getConversationId())
                .setEndUserId(currentSession.getEndUserId())
                .execute(new SseStreamCallback() {
                    
                    @Override
                    public void onOpen() {
                        EasyLog.print(TAG, "SSE 连接已建立（继续发送）");
                    }
                    
                    @Override
                    public void onChunk(SseChunk chunk) {
                        if (chunk == null) {
                            EasyLog.print(TAG, "接收到空数据块");
                            return;
                        }
                        
                        EasyLog.print(TAG, "SSE 数据块: type=" + chunk.getType() + 
                                ", content length=" + (chunk.getContent() != null ? chunk.getContent().length() : 0));
                        
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 判断是否是思考过程
                                boolean isThinkingChunk = "thinking".equals(chunk.getType()) || chunk.isThinking();
                                
                                if (isThinkingChunk) {
                                    // === 处理思考过程 ===
                                    String currentContent = thinkingMessage.getContent();
                                    if ("正在思考...".equals(currentContent)) {
                                        currentContent = "";
                                    }
                                    thinkingMessage.setContent(currentContent + (chunk.getContent() != null ? chunk.getContent() : ""));
                                    
                                    // 刷新思考消息 UI
                                    int pos = mChatAdapter.getData().indexOf(thinkingMessage);
                                    if (pos != -1) {
                                        mChatAdapter.notifyItemChanged(pos); // 局部刷新
                                        
                                        // 自动滚动
                                        if (scrollState == 0) {
                                            rv_chat.scrollToPosition(mChatAdapter.getData().size() - 1);
                                        }
                                    }
                                    
                                } else if ("chunk".equals(chunk.getType()) || "answer".equals(chunk.getType())) {
                                    // === 处理正式回答 ===
                                    
                                    // 如果是第一次从思考切换到回答
                                    if (isThinkingPhase[0]) {
                                        isThinkingPhase[0] = false;
                                        
                                        // 1. 折叠思考消息
                                        thinkingMessage.setThinkingCollapsed(true);
                                        int thinkPos = mChatAdapter.getData().indexOf(thinkingMessage);
                                        if (thinkPos != -1) {
                                            mChatAdapter.notifyItemChanged(thinkPos);
                                        }
                                        // 更新数据库中的思考消息
                                        DbService.getInstance().mChatMessageBeanService.updateEntity(thinkingMessage);
                                        
                                        // 2. 创建正式回答消息
                                        ChatMessageBean answerMsg = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED, "Ai", "", "");
                                        answerMsg.setSessionId(currentSession.getId());
                                        answerMsg.setCreateDate(DateHelper.getSeconds1());
                                        answerMsg.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                        answerMsg.setStreaming(true); // 设置为流式状态，启用打字机效果
                                        
                                        long answerId = DbService.getInstance().mChatMessageBeanService.addEntity(answerMsg);
                                        answerMsg.setId(answerId);
                                        answerMessageRef[0] = answerMsg;
                                        
                                        mChatAdapter.addItem(answerMsg);
                                    }
                                    
                                    // 追加回答内容
                                    if (answerMessageRef[0] != null && chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                                        String current = answerMessageRef[0].getContent();
                                        answerMessageRef[0].setContent(current + chunk.getContent());
                                        
                                        // 刷新回答消息 UI（不执行滚动，避免闪烁跳动）
                                        int answerPos = mChatAdapter.getData().indexOf(answerMessageRef[0]);
                                        if (answerPos != -1) {
                                            mChatAdapter.notifyItemChanged(answerPos); // 局部刷新
                                        }
                                    }
                                    
                                } else if ("error".equals(chunk.getType())) {
                                    // 错误处理
                                    EasyLog.print(TAG, "SSE 错误: " + chunk.getError());
                                    if (answerMessageRef[0] != null) {
                                        answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n[错误: " + chunk.getError() + "]");
                                        mChatAdapter.updateData();
                                    } else {
                                        thinkingMessage.setContent(thinkingMessage.getContent() + "\n[错误: " + chunk.getError() + "]");
                                        mChatAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        });
                    }
                    
                    @Override
                    public void onComplete() {
                        EasyLog.print(TAG, "SSE 流式对话完成（继续发送）");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 最终更新 UI
                                mChatAdapter.updateData();
                                
                                // 保存最终回答到数据库
                                if (answerMessageRef[0] != null) {
                                    answerMessageRef[0].setCreateDate(DateHelper.getSeconds1());
                                    DbService.getInstance().mChatMessageBeanService.updateEntity(answerMessageRef[0]);
                                    
                                    // 更新会话预览
                                    String preview = answerMessageRef[0].getContent();
                                    if (preview.length() > 30) {
                                        preview = preview.substring(0, 30) + "...";
                                    }
                                    currentSession.setPreview("AI: " + preview);
                                    currentSession.setUpdateTime(DateHelper.getSeconds1());
                                    DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);
                                } else {
                                    // 更新思考消息
                                    DbService.getInstance().mChatMessageBeanService.updateEntity(thinkingMessage);
                                }
                                // 强制折叠思考
                                thinkingMessage.setThinkingCollapsed(true);
                                int thinkPos = mChatAdapter.getData().indexOf(thinkingMessage);
                                if (thinkPos != -1) mChatAdapter.notifyItemChanged(thinkPos);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        EasyLog.print(TAG, "SSE 请求失败: " + e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String errStr = "网络请求失败: " + e.getMessage();
                                if (answerMessageRef[0] != null) {
                                    answerMessageRef[0].setContent(answerMessageRef[0].getContent() + "\n" + errStr);
                                    DbService.getInstance().mChatMessageBeanService.updateEntity(answerMessageRef[0]);
                                } else {
                                    thinkingMessage.setContent(thinkingMessage.getContent() + "\n" + errStr);
                                    DbService.getInstance().mChatMessageBeanService.updateEntity(thinkingMessage);
                                }
                                mChatAdapter.updateData();
                            }
                        });
                    }
                });
                
        // 清除待发送消息
        pendingMessageContent = null;
        pendingMessageTime = null;
        
        // 清空输入框
        ((EditText) findViewById(R.id.chat_content)).getText().clear();
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
        // 删除所有消息
        DbService.getInstance().mChatMessageBeanService.deleteAll();
        // 删除所有会话
        DbService.getInstance().mChatSessionBeanService.deleteAll();
        
        // 清空当前会话
        currentSession = null;
        
        // 清空聊天列表
        if (mChatAdapter != null) {
            mChatAdapter.setData(new ArrayList<>());
        }
        
        // 清空 SharedPreferences 中的最后会话ID
        if (getContext() != null) {
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY_LAST_SESSION_ID)
                    .apply();
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
}