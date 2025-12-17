package run.yigou.gxzy.ui.fragment;

import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
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


public final class AiMsgFragment extends TitleBarFragment<HomeActivity> implements OnTitleBarListener {

    private static final String TAG = "AiMsgFragment";
    private RecyclerView rv_chat;
    private DrawerLayout drawerLayout;
    private RecyclerView chatHistoryList;
    private run.yigou.gxzy.ui.tips.adapter.ChatHistoryAdapter chatHistoryAdapter; // 修改导入路径
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private int scrollState = 0;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

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

    @Override
    protected void initView() {
        Log.d(TAG, "initView: Starting initialization");
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
                    // 加载选中会话的所有聊天数据
                    loadChatDataForSession(sessions.get(position).getId());

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
        // 设置 RecyclerView 的布局管理器和适配器
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rv_chat.setLayoutManager(layoutManager);
        rv_chat.setAdapter(mChatAdapter);

        // 注册事件
        XEventBus.getDefault().register(AiMsgFragment.this);
        // 初始化消息数据
        initMsgs();

        // 添加调试日志，检查组件是否正确初始化
        Log.d(TAG, "initView: rv_chat=" + rv_chat);
        Log.d(TAG, "initView: mChatAdapter=" + mChatAdapter);
        Log.d(TAG, "initView: layoutManager=" + layoutManager);

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

        // 设置侧边栏标题编辑按钮点击事件
        ImageButton editTitleButton = findViewById(R.id.btn_edit_title);
        editTitleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTitleDialog();
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
            Log.d(TAG, "Added session to sidebar: " + session.getTitle() + ", message count: " + messageCount);
        }

        // 更新适配器数据
        chatHistoryAdapter.setData(historyItems);

        // 默认选中第一个会话并加载其数据
        if (!historyItems.isEmpty()) {
            chatHistoryAdapter.setSelectedPosition(0);
            if (!sessions.isEmpty()) {
                loadChatDataForSession(sessions.get(0).getId());
            }
        }
    }

    /**
     * 加载指定会话的聊天数据
     *
     * @param sessionId 会话ID
     */
    private void loadChatDataForSession(Long sessionId) {
        Log.d(TAG, "loadChatDataForSession: Loading data for session " + sessionId);

        // 获取会话信息
        ChatSessionBean session = DbService.getInstance().mChatSessionBeanService.findById(sessionId);
        if (session == null) {
            Log.w(TAG, "Session not found for ID: " + sessionId);
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
            mChatAdapter.setData(new ArrayList<>(messages));
            Log.d(TAG, "Loaded " + messages.size() + " messages for session " + sessionId);
        }

        // 将会话标题同步设置到TitleBar标题显示
        if (getTitleBar() != null) {
            getTitleBar().setTitle(session.getTitle());
        }

        // 滚动到聊天记录底部
        if (rv_chat != null && mChatAdapter != null) {
            rv_chat.post(new Runnable() {
                @Override
                public void run() {
                    if (mChatAdapter.getItemCount() > 0) {
                        rv_chat.scrollToPosition(mChatAdapter.getItemCount() - 1);
                        Log.d(TAG, "Scrolled to position " + (mChatAdapter.getItemCount() - 1));
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
        Log.d(TAG, "initMsgs: Initializing messages");

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
                        Log.d(TAG, "Saved sent message to database with ID: " + sendMsgId + " and session ID: " + currentSession.getId());

                        // 更新会话预览和时间
                        currentSession.setPreview("我: " + result);
                        currentSession.setUpdateTime(DateHelper.getSeconds1());
                        DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);

                        // 添加收到的消息占位符
                        final ChatMessageBean receivedMessage = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED, "Ai", "", "请稍等...");
                        receivedMessage.setSessionId(currentSession.getId());
                        receivedMessage.setCreateDate(DateHelper.getSeconds1());
                        receivedMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                        // 先保存占位符消息到数据库
                        long receivedMsgId = DbService.getInstance().mChatMessageBeanService.addEntity(receivedMessage);
                        receivedMessage.setId(receivedMsgId);
                        mChatAdapter.addItem(receivedMessage);
                        Log.d(TAG, "Saved placeholder received message to database with ID: " + receivedMsgId);

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
                                .setQuery(result)
                                .setConversationId(currentSession.getConversationId())
                                .setEndUserId(currentSession.getEndUserId())
                                .execute(new SseStreamCallback() {
                                    
                                    @Override
                                    public void onOpen() {
                                        LogUtils.d(TAG, "SSE 连接已建立");
                                    }
                                    
                                    @Override
                                    public void onChunk(SseChunk chunk) {
                                        if (chunk == null) {
                                            LogUtils.w(TAG, "接收到空数据块");
                                            return;
                                        }
                                        
                                        LogUtils.d(TAG, "SSE 数据块: type=" + chunk.getType() + 
                                                ", content length=" + (chunk.getContent() != null ? chunk.getContent().length() : 0));
                                        
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 根据类型处理不同的数据块
                                                if ("chunk".equals(chunk.getType()) || "answer".equals(chunk.getType())) {
                                                    // 累加内容到 receivedMessage
                                                    String currentContent = receivedMessage.getContent();
                                                    if ("请稍等...".equals(currentContent)) {
                                                        // 第一次接收数据，清空占位符
                                                        currentContent = "";
                                                    }
                                                    receivedMessage.setContent(currentContent + (chunk.getContent() != null ? chunk.getContent() : ""));
                                                    
                                                    // 更新 UI（不更新数据库）
                                                    if (scrollState == 0 && index % 3 == 0) {
                                                        mChatAdapter.updateData();
                                                        rv_chat.scrollBy(0, 15);
                                                    }
                                                    index++;
                                                    
                                                } else if ("error".equals(chunk.getType())) {
                                                    // 错误处理
                                                    LogUtils.e(TAG, "SSE 错误: " + chunk.getError());
                                                    receivedMessage.setContent("请求失败: " + chunk.getError());
                                                    mChatAdapter.updateData();
                                                }
                                            }
                                        });
                                    }
                                    
                                    @Override
                                    public void onComplete() {
                                        LogUtils.d(TAG, "SSE 流式对话完成");
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 最终更新 UI
                                                mChatAdapter.updateData();
                                                
                                                // 保存最终消息到数据库
                                                receivedMessage.setCreateDate(DateHelper.getSeconds1());
                                                receivedMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                                DbService.getInstance().mChatMessageBeanService.updateEntity(receivedMessage);
                                                LogUtils.d(TAG, "已保存完整的 AI 回复到数据库，ID: " + receivedMessage.getId());
                                                
                                                // 更新会话预览
                                                String preview = receivedMessage.getContent();
                                                if (preview.length() > 30) {
                                                    preview = preview.substring(0, 30) + "...";
                                                }
                                                currentSession.setPreview("AI: " + preview);
                                                currentSession.setUpdateTime(DateHelper.getSeconds1());
                                                DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);
                                            }
                                        });
                                    }
                                    
                                    @Override
                                    public void onError(Exception e) {
                                        LogUtils.e(TAG, "SSE 请求失败: " + e.getMessage());
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                receivedMessage.setContent("网络请求失败: " + e.getMessage());
                                                mChatAdapter.updateData();
                                                
                                                // 保存错误消息到数据库
                                                DbService.getInstance().mChatMessageBeanService.updateEntity(receivedMessage);
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

        Log.d(TAG, "Created new session in memory only");
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
            Log.d(TAG, "Saved new session to database with ID: " + sessionId);

            // 更新标题栏标题
            if (getTitleBar() != null) {
                getTitleBar().setTitle(currentSession.getTitle());
            }

            // 会话保存到数据库后，重新加载侧边栏数据
            populateChatHistoryWithTestData();
        } else {
            Log.d(TAG, "Session already exists in database with ID: " + currentSession.getId());
        }
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

        Log.d(TAG, "Checking if should save system message, onlySystemMessages: " + onlySystemMessages);

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
                Log.d(TAG, "Saved system message to database with ID: " + systemMsgId + " and session ID: " + currentSession.getId());
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
                Log.d(TAG, "Added system message to UI only (not saved to database)");
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
                                Log.d(TAG, "Session ID obtained: " + bean.getRealConversationId());
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
            Log.d(TAG, "Added system message to UI only (not saved to database yet)");
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
        Log.d(TAG, "initData: Starting to initialize data");

        // 只有在数据库中有会话数据时才初始化
        List<ChatSessionBean> sessions = DbService.getInstance().mChatSessionBeanService.findAll();
        if (!sessions.isEmpty()) {
            //初始化聊天会话数据
            // 加载历史会话历史记录
            populateChatHistoryWithTestData();
        }
        if (sessions.isEmpty()) {
            // 没有会话数据时不加载任何内容，保持界面空白
            Log.d(TAG, "No session data found in database, not loading any content");
            // 清空侧边栏
            chatHistoryAdapter.setData(new ArrayList<ChatHistoryAdapter.ChatHistoryItem>());
            // 显示默认空内容
        }
        handleAddNewSession();
        Log.d(TAG, "initData: Completed data initialization");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed");
        // 确保数据在恢复时也能正确显示
        if (mChatAdapter != null && rv_chat != null) {
            rv_chat.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onResume: Adapter item count = " + mChatAdapter.getItemCount());
                    Log.d(TAG, "onResume: RecyclerView child count = " + rv_chat.getChildCount());
                    if (mChatAdapter.getItemCount() > 0 && rv_chat.getChildCount() == 0) {
                        Log.w(TAG, "onResume: Data exists but not displayed, forcing refresh");
                        mChatAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
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
                                Log.d(TAG, "Session ID obtained: " + bean.getRealConversationId());
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
                                Log.d(TAG, "Session ID obtained: " + bean.getRealConversationId());
                                
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
        Log.d(TAG, "Saved sent message to database with ID: " + sendMsgId + " and session ID: " + currentSession.getId());

        // 更新会话预览和时间
        currentSession.setPreview("我: " + pendingMessageContent);
        currentSession.setUpdateTime(DateHelper.getSeconds1());
        DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);

        // 添加收到的消息占位符
        final ChatMessageBean receivedMessage = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED,"Ai", "", "请稍等...");
        receivedMessage.setSessionId(currentSession.getId());
        receivedMessage.setCreateDate(DateHelper.getSeconds1());
        receivedMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
        // 先保存占位符消息到数据库
        long receivedMsgId = DbService.getInstance().mChatMessageBeanService.addEntity(receivedMessage);
        receivedMessage.setId(receivedMsgId);
        mChatAdapter.addItem(receivedMessage);
        Log.d(TAG, "Saved placeholder received message to database with ID: " + receivedMsgId);

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
                        LogUtils.d(TAG, "SSE 连接已建立（继续发送）");
                    }
                    
                    @Override
                    public void onChunk(SseChunk chunk) {
                        if (chunk == null) {
                            LogUtils.w(TAG, "接收到空数据块");
                            return;
                        }
                        
                        LogUtils.d(TAG, "SSE 数据块: type=" + chunk.getType() + 
                                ", content length=" + (chunk.getContent() != null ? chunk.getContent().length() : 0));
                        
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 根据类型处理不同的数据块
                                if ("chunk".equals(chunk.getType()) || "answer".equals(chunk.getType())) {
                                    // 累加内容到 receivedMessage
                                    String currentContent = receivedMessage.getContent();
                                    if ("请稍等...".equals(currentContent)) {
                                        // 第一次接收数据，清空占位符
                                        currentContent = "";
                                    }
                                    receivedMessage.setContent(currentContent + (chunk.getContent() != null ? chunk.getContent() : ""));
                                    
                                    // 更新 UI（不更新数据库）
                                    if (scrollState == 0 && index % 3 == 0) {
                                        mChatAdapter.updateData();
                                        rv_chat.scrollBy(0, 15);
                                    }
                                    index++;
                                    
                                } else if ("error".equals(chunk.getType())) {
                                    // 错误处理
                                    LogUtils.e(TAG, "SSE 错误: " + chunk.getError());
                                    receivedMessage.setContent("请求失败: " + chunk.getError());
                                    mChatAdapter.updateData();
                                }
                            }
                        });
                    }
                    
                    @Override
                    public void onComplete() {
                        LogUtils.d(TAG, "SSE 流式对话完成（继续发送）");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 最终更新 UI
                                mChatAdapter.updateData();
                                
                                // 保存最终消息到数据库
                                receivedMessage.setCreateDate(DateHelper.getSeconds1());
                                receivedMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                DbService.getInstance().mChatMessageBeanService.updateEntity(receivedMessage);
                                LogUtils.d(TAG, "已保存完整的 AI 回复到数据库，ID: " + receivedMessage.getId());
                                
                                // 更新会话预览
                                String preview = receivedMessage.getContent();
                                if (preview.length() > 30) {
                                    preview = preview.substring(0, 30) + "...";
                                }
                                currentSession.setPreview("AI: " + preview);
                                currentSession.setUpdateTime(DateHelper.getSeconds1());
                                DbService.getInstance().mChatSessionBeanService.updateEntity(currentSession);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        LogUtils.e(TAG, "SSE 请求失败: " + e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                receivedMessage.setContent("网络请求失败: " + e.getMessage());
                                mChatAdapter.updateData();
                                
                                // 保存错误消息到数据库
                                DbService.getInstance().mChatMessageBeanService.updateEntity(receivedMessage);
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
}