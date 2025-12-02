package run.yigou.gxzy.ui.fragment;

import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;

import run.yigou.gxzy.EventBus.ChatMessageBeanEvent;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.activity.AiConfigActivity;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.tips.adapter.TipsAiChatAdapter;
import run.yigou.gxzy.ui.tips.aimsg.AiConfigHelper;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.ui.tips.aimsg.AiHelper;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.ThreadUtil;
import run.yigou.gxzy.ui.tips.adapter.ChatHistoryAdapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.hjq.bar.OnTitleBarListener;
import com.hjq.bar.TitleBar;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
    
    // 模拟会话数据
    private List<ChatSession> chatSessions = new ArrayList<>();

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
        if (AiConfigHelper.getAssistantName() != null)
          setTitle(AiConfigHelper.getAssistantName());
        getStatusBarConfig().setTitleBar(this, findViewById(R.id.tv_title));
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
                // 加载选中会话的所有聊天数据
                loadChatDataForSession(position);
                
                // 关闭侧边栏
                if (drawerLayout != null) {
                    drawerLayout.closeDrawers();
                }
            }
        });
        
        // 设置标题栏点击监听
        if (getTitleBar() != null) {
            getTitleBar().setOnTitleBarListener(this);
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

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
    }


    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(priority = 1)
    public void ChatMessageEvent(ChatMessageBeanEvent event) {
        ThreadUtil.runOnUiThread(() -> {
            if (event.isClear()) {
                initData();
                mChatAdapter.notifyDataSetChanged();
                AiHelper.clearHistory();
                // 重新填充聊天历史记录测试数据
                populateChatHistoryWithTestData();
            }
            if (event.isAssistantName())
                ((TextView) findViewById(R.id.tv_title)).setText(AiConfigHelper.getAssistantName());
        });
    }

    /**
     * 填充聊天界面测试数据
     */
    private void populateChatWithTestData() {
        Log.d(TAG, "populateChatWithTestData: Starting to populate test data");
        
        // 如果还没有初始化会话数据，则先初始化
        if (chatSessions.isEmpty()) {
            initChatSessions();
        }
        
        // 加载第一个会话的数据
        if (!chatSessions.isEmpty()) {
            loadChatDataForSession(0);
        }
    }
    
    /**
     * 初始化聊天会话数据
     */
    private void initChatSessions() {
        chatSessions.clear();
        
        // 创建会话1: 中医基本理论
        List<ChatMessageBean> session1Messages = new ArrayList<>();
        
        // 添加系统消息
        ChatMessageBean systemMessage1 = new ChatMessageBean(
                ChatMessageBean.TYPE_SYSTEM, 
                null, 
                null, 
                "欢迎使用AI助手！今天是 " + new SimpleDateFormat("MM月dd日").format(new Date()));
        systemMessage1.setCreateDate(DateHelper.getSeconds1());
        session1Messages.add(systemMessage1);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage1 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "您好！我是您的AI助手，我可以帮您解答关于中医、健康等方面的问题。请问有什么我可以帮您的吗？");
        receiveMessage1.setCreateDate(DateHelper.getSeconds1());
        session1Messages.add(receiveMessage1);
        
        // 添加发送消息（用户）
        ChatMessageBean sendMessage1 = new ChatMessageBean(
                ChatMessageBean.TYPE_SEND, 
                "", 
                "", 
                "你好，我想了解一下中医的基本理论");
        sendMessage1.setCreateDate(DateHelper.getSeconds1());
        session1Messages.add(sendMessage1);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage2 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "中医的基本理论主要包括阴阳五行学说、脏腑经络学说、气血津液学说等。其中阴阳五行学说是中医理论的基础，用来解释人体的生理病理现象和指导临床诊断治疗。\n\n" +
                "阴阳学说认为，宇宙间一切事物都是由相互对立又相互关联的阴阳两方面组成。在人体中，阴阳平衡是健康的基础，失衡则会导致疾病。\n\n" +
                "五行学说将自然界的事物分为木、火、土、金、水五大类，它们之间存在着相生相克的关系。在人体中，五脏（肝、心、脾、肺、肾）分别对应五行，通过五行关系来解释脏腑之间的相互关系。");
        receiveMessage2.setCreateDate(DateHelper.getSeconds1());
        session1Messages.add(receiveMessage2);
        
        // 添加发送消息（用户）
        ChatMessageBean sendMessage2 = new ChatMessageBean(
                ChatMessageBean.TYPE_SEND, 
                "", 
                "", 
                "那中医是如何诊断疾病的呢？");
        sendMessage2.setCreateDate(DateHelper.getSeconds1());
        session1Messages.add(sendMessage2);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage3 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "中医诊断疾病主要通过四种方法，即望、闻、问、切，也称为\"四诊\"：\n\n" +
                "1. **望诊**：观察患者的神色、形态、舌象等外在表现\n" +
                "2. **闻诊**：听患者的声音、呼吸、咳嗽等，嗅其气味\n" +
                "3. **问诊**：询问患者的症状、病史、生活习惯等\n" +
                "4. **切诊**：通过脉诊和触诊了解患者的身体状况\n\n" +
                "通过四诊收集的信息，医生会进行综合分析，判断疾病的性质、部位、原因等，进而制定治疗方案。");
        receiveMessage3.setCreateDate(DateHelper.getSeconds1());
        session1Messages.add(receiveMessage3);
        
        ChatSession session1 = new ChatSession(
                "与AI助手对话 - 中医基本理论", 
                "我: 你好，我想了解一下中医的基本理论\nAI: 您好！我是您的AI助手，我可以帮您解答关于中医、健康等方面的问题...",
                "2025-12-01",
                session1Messages.size() + " 条消息",
                session1Messages);
        chatSessions.add(session1);
        
        // 创建会话2: 脾胃调理咨询
        List<ChatMessageBean> session2Messages = new ArrayList<>();
        
        // 添加系统消息
        ChatMessageBean systemMessage2 = new ChatMessageBean(
                ChatMessageBean.TYPE_SYSTEM, 
                null, 
                null, 
                "会话记录：脾胃调理咨询");
        systemMessage2.setCreateDate(DateHelper.getSeconds1());
        session2Messages.add(systemMessage2);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage4 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "您好！关于脾胃调理，我可以为您提供一些建议。");
        receiveMessage4.setCreateDate(DateHelper.getSeconds1());
        session2Messages.add(receiveMessage4);
        
        // 添加发送消息（用户）
        ChatMessageBean sendMessage3 = new ChatMessageBean(
                ChatMessageBean.TYPE_SEND, 
                "", 
                "", 
                "请问如何调理脾胃?");
        sendMessage3.setCreateDate(DateHelper.getSeconds1());
        session2Messages.add(sendMessage3);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage5 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "脾胃调理需要从饮食、作息等多方面入手:\n\n" +
                "1. 饮食规律：定时定量，避免暴饮暴食\n" +
                "2. 食物选择：多吃易消化的食物，如小米粥、山药等\n" +
                "3. 生活习惯：保持充足睡眠，适量运动\n" +
                "4. 情绪调节：避免过度焦虑和压力");
        receiveMessage5.setCreateDate(DateHelper.getSeconds1());
        session2Messages.add(receiveMessage5);
        
        ChatSession session2 = new ChatSession(
                "脾胃调理咨询", 
                "我: 请问如何调理脾胃?\nAI: 脾胃调理需要从饮食、作息等多方面入手...",
                "2025-11-30", 
                session2Messages.size() + " 条消息",
                session2Messages);
        chatSessions.add(session2);
        
        // 创建会话3: 失眠问题咨询
        List<ChatMessageBean> session3Messages = new ArrayList<>();
        
        // 添加系统消息
        ChatMessageBean systemMessage3 = new ChatMessageBean(
                ChatMessageBean.TYPE_SYSTEM, 
                null, 
                null, 
                "会话记录：失眠问题咨询");
        systemMessage3.setCreateDate(DateHelper.getSeconds1());
        session3Messages.add(systemMessage3);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage6 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "您好！关于失眠问题，我可以为您提供一些建议。");
        receiveMessage6.setCreateDate(DateHelper.getSeconds1());
        session3Messages.add(receiveMessage6);
        
        // 添加发送消息（用户）
        ChatMessageBean sendMessage4 = new ChatMessageBean(
                ChatMessageBean.TYPE_SEND, 
                "", 
                "", 
                "最近总是失眠怎么办?");
        sendMessage4.setCreateDate(DateHelper.getSeconds1());
        session3Messages.add(sendMessage4);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage7 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "失眠可能与心脾两虚有关，建议您可以尝试以下方法:\n\n" +
                "1. 睡前放松：避免刺激性活动，可以听轻音乐或冥想\n" +
                "2. 饮食调节：晚餐不宜过饱，避免浓茶、咖啡\n" +
                "3. 规律作息：尽量每天同一时间上床和起床\n" +
                "4. 适度运动：白天进行适量运动，但睡前3小时内避免剧烈运动");
        receiveMessage7.setCreateDate(DateHelper.getSeconds1());
        session3Messages.add(receiveMessage7);
        
        ChatSession session3 = new ChatSession(
                "失眠问题咨询", 
                "我: 最近总是失眠怎么办?\nAI: 失眠可能与心脾两虚有关，建议您可以尝试以下方法...",
                "2025-11-28", 
                session3Messages.size() + " 条消息",
                session3Messages);
        chatSessions.add(session3);
        
        // 创建会话4: 感冒用药咨询
        List<ChatMessageBean> session4Messages = new ArrayList<>();
        
        // 添加系统消息
        ChatMessageBean systemMessage4 = new ChatMessageBean(
                ChatMessageBean.TYPE_SYSTEM, 
                null, 
                null, 
                "会话记录：感冒用药咨询");
        systemMessage4.setCreateDate(DateHelper.getSeconds1());
        session4Messages.add(systemMessage4);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage8 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "您好！关于感冒用药，我可以为您提供一些建议。");
        receiveMessage8.setCreateDate(DateHelper.getSeconds1());
        session4Messages.add(receiveMessage8);
        
        // 添加发送消息（用户）
        ChatMessageBean sendMessage5 = new ChatMessageBean(
                ChatMessageBean.TYPE_SEND, 
                "", 
                "", 
                "感冒了可以用哪些中药?");
        sendMessage5.setCreateDate(DateHelper.getSeconds1());
        session4Messages.add(sendMessage5);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage9 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "感冒通常分为风寒感冒和风热感冒:\n\n" +
                "1. **风寒感冒**：症状为恶寒重、发热轻、无汗、头痛、鼻塞流清涕等，可用麻黄汤、桂枝汤等方剂\n" +
                "2. **风热感冒**：症状为发热重、恶寒轻、有汗、咽喉肿痛、鼻塞流黄涕等，可用银翘散、桑菊饮等方剂\n\n" +
                "建议在专业中医师指导下使用中药，以确保用药安全和疗效。");
        receiveMessage9.setCreateDate(DateHelper.getSeconds1());
        session4Messages.add(receiveMessage9);
        
        ChatSession session4 = new ChatSession(
                "感冒用药咨询", 
                "我: 感冒了可以用哪些中药?\nAI: 感冒通常分为风寒感冒和风热感冒...",
                "2025-11-25", 
                session4Messages.size() + " 条消息",
                session4Messages);
        chatSessions.add(session4);
        
        // 创建会话5: 养生茶推荐
        List<ChatMessageBean> session5Messages = new ArrayList<>();
        
        // 添加系统消息
        ChatMessageBean systemMessage5 = new ChatMessageBean(
                ChatMessageBean.TYPE_SYSTEM, 
                null, 
                null, 
                "会话记录：养生茶推荐");
        systemMessage5.setCreateDate(DateHelper.getSeconds1());
        session5Messages.add(systemMessage5);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage10 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "您好！关于养生茶，我可以为您推荐几种。");
        receiveMessage10.setCreateDate(DateHelper.getSeconds1());
        session5Messages.add(receiveMessage10);
        
        // 添加发送消息（用户）
        ChatMessageBean sendMessage6 = new ChatMessageBean(
                ChatMessageBean.TYPE_SEND, 
                "", 
                "", 
                "有什么推荐的养生茶吗?");
        sendMessage6.setCreateDate(DateHelper.getSeconds1());
        session5Messages.add(sendMessage6);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage11 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "根据常见的养生需求，我推荐以下几种养生茶:\n\n" +
                "1. **枸杞菊花茶**：养肝明目，适合长期用眼的人群\n" +
                "2. **玫瑰花茶**：疏肝解郁，美容养颜，适合女性饮用\n" +
                "3. **山楂荷叶茶**：降脂减肥，适合肥胖人群\n" +
                "4. **红枣桂圆茶**：补气养血，适合气血不足的人群\n\n" +
                "建议根据个人体质选择合适的养生茶，并注意适量饮用。");
        receiveMessage11.setCreateDate(DateHelper.getSeconds1());
        session5Messages.add(receiveMessage11);
        
        ChatSession session5 = new ChatSession(
                "养生茶推荐", 
                "我: 有什么推荐的养生茶吗?\nAI: 根据您的体质，我推荐以下几种养生茶...",
                "2025-11-20", 
                session5Messages.size() + " 条消息",
                session5Messages);
        chatSessions.add(session5);
    }

    /**
     * 填充聊天历史记录列表测试数据
     */
    private void populateChatHistoryWithTestData() {
        // 如果还没有初始化会话数据，则先初始化
        if (chatSessions.isEmpty()) {
            initChatSessions();
        }
        
        List<ChatHistoryAdapter.ChatHistoryItem> historyItems = new ArrayList<>();
        
        // 根据会话数据创建历史记录项
        for (ChatSession session : chatSessions) {
            historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                    session.getTitle(), 
                    session.getPreview(),
                    session.getTime(),
                    session.getMessageCount()));
        }

        // 更新适配器数据
        chatHistoryAdapter.setData(historyItems);
        
        // 默认选中第一个会话并加载其数据
        if (!historyItems.isEmpty()) {
            chatHistoryAdapter.setSelectedPosition(0);
            loadChatDataForSession(0);
        }
    }

    /**
     * 填充聊天历史记录列表 (真实数据)
     */
    private void populateChatHistory() {
        List<ChatHistoryAdapter.ChatHistoryItem> historyItems = new ArrayList<>();
        
        // 从数据库获取聊天记录
        ArrayList<ChatMessageBean> chatMessages = DbService.getInstance().mChatMessageBeanService.findAll();
        
        if (chatMessages != null && !chatMessages.isEmpty()) {
            // 按时间分组，创建聊天历史记录项
            String currentDate = "";
            StringBuilder preview = new StringBuilder();
            int messageCount = 0;
            
            for (int i = chatMessages.size() - 1; i >= 0; i--) {
                ChatMessageBean message = chatMessages.get(i);
                
                // 获取消息日期
                String messageDate = message.getCreateDate();
                if (messageDate != null && messageDate.length() >= 10) {
                    messageDate = messageDate.substring(0, 10); // 只取日期部分
                } else {
                    messageDate = "未知日期";
                }
                
                // 如果是新的一天，或者达到一定消息数量，创建一个新的历史记录项
                if (!messageDate.equals(currentDate) || messageCount >= 10) {
                    if (messageCount > 0) {
                        // 添加上一个历史记录项
                        String title = currentDate.isEmpty() ? "聊天记录" : "与AI助手对话";
                        historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                                title, 
                                preview.toString(),
                                currentDate,
                                messageCount + " 条消息"));
                    }
                    
                    // 重置计数器和预览文本
                    currentDate = messageDate;
                    preview = new StringBuilder();
                    messageCount = 0;
                }
                
                // 添加消息到预览中
                if (preview.length() > 0) {
                    preview.append("\n");
                }
                
                // 根据消息类型添加前缀
                String prefix = "";
                switch (message.getType()) {
                    case ChatMessageBean.TYPE_SEND:
                        prefix = "我: ";
                        break;
                    case ChatMessageBean.TYPE_RECEIVED:
                        prefix = "AI: ";
                        break;
                }
                
                preview.append(prefix).append(message.getContent());
                messageCount++;
            }
            
            // 添加最后一个历史记录项
            if (messageCount > 0) {
                String title = currentDate.isEmpty() ? "聊天记录" : "与AI助手对话";
                historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                        title, 
                        preview.toString(),
                        currentDate,
                        messageCount + " 条消息"));
            }
        } else {
            // 如果没有聊天记录，添加一个默认项
            historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                    "暂无聊天记录", 
                    "开始与AI助手对话吧",
                    "",
                    ""));
        }
        
        // 更新适配器数据
        chatHistoryAdapter.setData(historyItems);
        
        // 默认选中第一个会话并加载其数据
        if (!historyItems.isEmpty()) {
            chatHistoryAdapter.setSelectedPosition(0);
            loadChatDataForSession(0);
        }
    }

    /**
     * 加载指定会话的聊天数据
     *
     * @param sessionPosition 会话位置
     */
    private void loadChatDataForSession(int sessionPosition) {
        // 在实际应用中，这里应该根据会话ID从数据库加载对应的聊天记录
        // 现在我们使用统一的会话数据来演示功能
        
        Log.d(TAG, "loadChatDataForSession: Loading data for session " + sessionPosition);
        
        // 确保会话数据已初始化
        if (chatSessions.isEmpty()) {
            initChatSessions();
        }
        
        // 清空当前聊天数据
        if (mChatAdapter != null) {
            mChatAdapter.setData(new ArrayList<ChatMessageBean>());
        }
        
        // 根据会话位置加载对应的聊天数据
        if (sessionPosition >= 0 && sessionPosition < chatSessions.size()) {
            ChatSession session = chatSessions.get(sessionPosition);
            if (mChatAdapter != null) {
                mChatAdapter.setData(new ArrayList<>(session.getMessages()));
            }
            
            // 将会话标题同步设置到TitleBar标题显示
            if (getTitleBar() != null) {
                getTitleBar().setTitle(session.getTitle());
            }
        } else {
            loadDefaultSessionData();
            // 设置默认标题
            if (getTitleBar() != null) {
                getTitleBar().setTitle("Ai对话");
            }
        }
        
        // 滚动到聊天记录底部
        if (rv_chat != null && mChatAdapter != null) {
            rv_chat.post(new Runnable() {
                @Override
                public void run() {
                    if (mChatAdapter.getItemCount() > 0) {
                        rv_chat.scrollToPosition(mChatAdapter.getItemCount() - 1);
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

        // 按住说话按钮
        //       Button bt_asr = findViewById(R.id.bt_asr);
//        bt_asr.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//            }
//        });

//        ImageView bt_switch = findViewById(R.id.bt_switch);
//        bt_switch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                bt_asr.setVisibility(View.GONE);
//                findViewById(R.id.chat_bottom).setVisibility(View.VISIBLE);
//            }
//        });
//
//        ImageView chat_voice = findViewById(R.id.chat_voice);
//        chat_voice.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ;
//                bt_asr.setVisibility(View.VISIBLE);
//                findViewById(R.id.chat_bottom).setVisibility(View.GONE);
//            }
//        });

        Button chat_send = findViewById(R.id.chat_send);
        chat_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 检查 配置
//                if (AiConfigHelper.getApiKey() == null || AiConfigHelper.getApiKey().isEmpty()
//                        || AiConfigHelper.getGptModel() == null || AiConfigHelper.getGptModel().isEmpty()
//                        || AiConfigHelper.getProxyAddress() == null || AiConfigHelper.getProxyAddress().isEmpty()) {
//                    toast("请先配置好AI参数");
//                    startActivity(new Intent(getActivity(), AiConfigActivity.class));
//                    return;
//                }

                String result = ((EditText) findViewById(R.id.chat_content)).getText().toString();
                if (!result.isEmpty()) {
                    if (mChatAdapter != null) {
                        String time = sdf.format(new Date());

                        // 检查并添加系统消息
                        boolean messageExists = false;
                        for (ChatMessageBean message : mChatAdapter.getData()) {
                            if (message.getType() == ChatMessageBean.TYPE_SYSTEM && time.equals(message.getContent())) {
                                messageExists = true;
                                break;
                            }
                        }

                        if (!messageExists) {
                            ChatMessageBean chatMessageBeanSystem = new ChatMessageBean(ChatMessageBean.TYPE_SYSTEM, null, null, sdf.format(new Date()));
                            chatMessageBeanSystem.setCreateDate(DateHelper.getSeconds1());
                            chatMessageBeanSystem.setIsDelete(ChatMessageBean.IS_Delete_NO);
                            DbService.getInstance().mChatMessageBeanService.addEntity(chatMessageBeanSystem);
                            mChatAdapter.addItem(chatMessageBeanSystem);
                        }

                        // 添加发送消息
                        ChatMessageBean chatMessageBeanSend = new ChatMessageBean(ChatMessageBean.TYPE_SEND, "", "", result);
                        chatMessageBeanSend.setCreateDate(DateHelper.getSeconds1());
                        chatMessageBeanSend.setIsDelete(ChatMessageBean.IS_Delete_NO);
                        DbService.getInstance().mChatMessageBeanService.addEntity(chatMessageBeanSend);
                        mChatAdapter.addItem(chatMessageBeanSend);

                        // 添加收到的消息
                        final ChatMessageBean receivedMessage = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED, AiConfigHelper.getAssistantName(), "", "请稍等...");
                        mChatAdapter.addItem(receivedMessage);

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


                        AiHelper.chat(result, new AiHelper.CallBack() {
                            @Override
                            public void onCallBack(final String result, final boolean isLast) {
                                // 打印调试信息
                                LogUtils.d("gptResponse", result);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        receivedMessage.setContent(result);

                                        // 根据滚动状态决定是否更新数据
                                        if ((scrollState == 0 && index % 3 == 0) || isLast) {
                                            mChatAdapter.updateData();
                                            rv_chat.scrollBy(0, 15);
                                        }

                                        // 滚动到最后一条消息
//                                        if ((scrollState == 0 && ++index % 20 == 0) || isLast) {
//                                            EasyLog.print("scrollState: "+scrollState);
//                                            if (rv_chat != null) {
//                                                rv_chat.scrollToPosition(mChatAdapter.getData().size() - 1);
//
//                                            }
//
//                                        }

                                        if (isLast) {
                                            // 将回复消息插入数据库
                                            //rv_chat.smoothScrollToPosition(mChatAdapter.getData().size() - 1);
                                            // rv_chat .scrollBy(0, 20);
                                            receivedMessage.setCreateDate(DateHelper.getSeconds1());
                                            receivedMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                            DbService.getInstance().mChatMessageBeanService.addEntity(receivedMessage);
                                            
                                            // 更新聊天历史记录
                                            populateChatHistoryWithTestData();
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
                ((EditText) findViewById(R.id.chat_content)).getText().clear();
            }
        });

        //   设置点击事件
//        findViewById(R.id.tv_title).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(getActivity(), AiConfigActivity.class));
//            }
//        });
        //setRightTitle("...");
        //getTitleBar().setRightTitleSize(26);
        //getTitleBar().setRightTitleColor(Color.BLACK);

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
        startActivity(new Intent(getActivity(), AiConfigActivity.class));
    }

    private ArrayList<ChatMessageBean> chatMessageBeans;

    @Override
    protected void initData() {
        Log.d(TAG, "initData: Starting to initialize data");
        chatMessageBeans = DbService.getInstance().mChatMessageBeanService.findAll();
        if (chatMessageBeans != null) {
            Log.d(TAG, "initData: Found " + chatMessageBeans.size() + " chat messages in database");
            if (mChatAdapter != null) {
                mChatAdapter.setData(chatMessageBeans);
                Log.d(TAG, "initData: Data set to adapter from database");
            } else {
                Log.e(TAG, "initData: mChatAdapter is null!");
            }
            int lastPosition = mChatAdapter.getData().size() - 1;
            if (lastPosition > 0) {
                if (rv_chat != null) {
                    rv_chat.smoothScrollToPosition(lastPosition);
                    Log.d(TAG, "initData: Scrolled to last position: " + lastPosition);
                } else {
                    Log.e(TAG, "initData: rv_chat is null!");
                }
            }

        } else {
            Log.d(TAG, "initData: No chat messages found in database");
            if (mChatAdapter != null) {
                mChatAdapter.setData(new ArrayList<ChatMessageBean>());
                Log.d(TAG, "initData: Set empty data to adapter");
            } else {
                Log.e(TAG, "initData: mChatAdapter is null!");
            }

        }
        // 填充聊天历史记录列表测试数据
        populateChatHistoryWithTestData();
        // 填充聊天界面测试数据
        populateChatWithTestData();
        Log.d(TAG, "initView: Completed initialization");

        // 添加调试代码，确保数据正确显示
        rv_chat.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "RecyclerView post: child count = " + rv_chat.getChildCount());
                Log.d(TAG, "RecyclerView post: adapter count = " + mChatAdapter.getItemCount());
            }
        });

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
     * 聊天会话数据类
     */
    private static class ChatSession {
        private String title;
        private String preview;
        private String time;
        private String messageCount;
        private List<ChatMessageBean> messages;

        public ChatSession(String title, String preview, String time, String messageCount, List<ChatMessageBean> messages) {
            this.title = title;
            this.preview = preview;
            this.time = time;
            this.messageCount = messageCount;
            this.messages = messages;
        }

        public String getTitle() {
            return title;
        }

        public String getPreview() {
            return preview;
        }

        public String getTime() {
            return time;
        }

        public String getMessageCount() {
            return messageCount;
        }

        public List<ChatMessageBean> getMessages() {
            return messages;
        }
    }
}