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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private ListView chatHistoryList;
    private ChatHistoryAdapter chatHistoryAdapter;
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private int scrollState = 0;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    public static AiMsgFragment newInstance() {
        return new AiMsgFragment();
    }


    @Override
    protected int getLayoutId() {
        //  return R.layout.tips_ai_msg_fragment;
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
        setTitle("Ai对话");
        // 注册事件
        XEventBus.getDefault().register(AiMsgFragment.this);
        // 初始化消息数据
        initMsgs();
        
        // 添加调试日志，检查组件是否正确初始化
        Log.d(TAG, "initView: rv_chat=" + rv_chat);
        Log.d(TAG, "initView: mChatAdapter=" + mChatAdapter);
        Log.d(TAG, "initView: layoutManager=" + layoutManager);
        
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
        // 创建一些测试消息
        ArrayList<ChatMessageBean> testData = new ArrayList<>();
        
        // 添加系统消息
        ChatMessageBean systemMessage = new ChatMessageBean(
                ChatMessageBean.TYPE_SYSTEM, 
                null, 
                null, 
                "欢迎使用AI助手！今天是 " + new SimpleDateFormat("MM月dd日").format(new Date()));
        systemMessage.setCreateDate(DateHelper.getSeconds1());
        testData.add(systemMessage);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage1 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "您好！我是您的AI助手，我可以帮您解答关于中医、健康等方面的问题。请问有什么我可以帮您的吗？");
        receiveMessage1.setCreateDate(DateHelper.getSeconds1());
        testData.add(receiveMessage1);
        
        // 添加发送消息（用户）
        ChatMessageBean sendMessage1 = new ChatMessageBean(
                ChatMessageBean.TYPE_SEND, 
                "", 
                "", 
                "你好，我想了解一下中医的基本理论");
        sendMessage1.setCreateDate(DateHelper.getSeconds1());
        testData.add(sendMessage1);
        
        // 添加接收消息（AI回复）
        ChatMessageBean receiveMessage2 = new ChatMessageBean(
                ChatMessageBean.TYPE_RECEIVED, 
                "AI助手", 
                "", 
                "中医的基本理论主要包括阴阳五行学说、脏腑经络学说、气血津液学说等。其中阴阳五行学说是中医理论的基础，用来解释人体的生理病理现象和指导临床诊断治疗。\n\n" +
                "阴阳学说认为，宇宙间一切事物都是由相互对立又相互关联的阴阳两方面组成。在人体中，阴阳平衡是健康的基础，失衡则会导致疾病。\n\n" +
                "五行学说将自然界的事物分为木、火、土、金、水五大类，它们之间存在着相生相克的关系。在人体中，五脏（肝、心、脾、肺、肾）分别对应五行，通过五行关系来解释脏腑之间的相互关系。");
        receiveMessage2.setCreateDate(DateHelper.getSeconds1());
        testData.add(receiveMessage2);
        
        // 添加发送消息（用户）
        ChatMessageBean sendMessage2 = new ChatMessageBean(
                ChatMessageBean.TYPE_SEND, 
                "", 
                "", 
                "那中医是如何诊断疾病的呢？");
        sendMessage2.setCreateDate(DateHelper.getSeconds1());
        testData.add(sendMessage2);
        
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
        testData.add(receiveMessage3);
        
        // 设置测试数据到适配器
        Log.d(TAG, "populateChatWithTestData: Setting data to adapter, size=" + testData.size());
        Log.d(TAG, "populateChatWithTestData: rv_chat=" + rv_chat);
        Log.d(TAG, "populateChatWithTestData: mChatAdapter=" + mChatAdapter);
        
        if (mChatAdapter != null) {
            mChatAdapter.setData(testData);
            Log.d(TAG, "populateChatWithTestData: Data set to adapter successfully");
        } else {
            Log.e(TAG, "populateChatWithTestData: mChatAdapter is null!");
        }
        
        // 强制刷新UI
        if (rv_chat != null) {
            rv_chat.post(new Runnable() {
                @Override
                public void run() {
                    if (mChatAdapter != null) {
                        mChatAdapter.notifyDataSetChanged();
                        Log.d(TAG, "populateChatWithTestData: notifyDataSetChanged called");
                        
                        // 检查 RecyclerView 是否正确设置
                        Log.d(TAG, "populateChatWithTestData: RecyclerView adapter=" + rv_chat.getAdapter());
                        Log.d(TAG, "populateChatWithTestData: Adapter item count=" + mChatAdapter.getItemCount());
                        Log.d(TAG, "populateChatWithTestData: RecyclerView child count=" + rv_chat.getChildCount());
                        
                        // 滚动到最新消息
                        if (testData.size() > 0) {
                            rv_chat.scrollToPosition(testData.size() - 1);
                            Log.d(TAG, "populateChatWithTestData: Scrolled to position " + (testData.size() - 1));
                        }
                    } else {
                        Log.e(TAG, "populateChatWithTestData: mChatAdapter is null in post!");
                    }
                }
            });
        } else {
            Log.e(TAG, "populateChatWithTestData: rv_chat is null!");
        }
        
        Log.d(TAG, "populateChatWithTestData: Completed populating test data");
    }

    /**
     * 填充聊天历史记录列表测试数据
     */
    private void populateChatHistoryWithTestData() {
        List<ChatHistoryAdapter.ChatHistoryItem> historyItems = new ArrayList<>();
        
        // 添加测试数据
        historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                "与AI助手对话", 
                "我: 你好，我想了解一下中医\nAI: 您好！我很乐意为您介绍中医的相关知识...",
                "2025-12-01",
                "12 条消息"));
        
        historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                "脾胃调理咨询", 
                "我: 请问如何调理脾胃?\nAI: 脾胃调理需要从饮食、作息等多方面入手...",
                "2025-11-30", 
                "8 条消息"));
        
        historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                "失眠问题咨询", 
                "我: 最近总是失眠怎么办?\nAI: 失眠可能与心脾两虚有关，建议您可以尝试以下方法...",
                "2025-11-28", 
                "15 条消息"));
        
        historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                "感冒用药咨询", 
                "我: 感冒了可以用哪些中药?\nAI: 感冒通常分为风寒感冒和风热感冒...",
                "2025-11-25", 
                "6 条消息"));
        
        historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                "养生茶推荐", 
                "我: 有什么推荐的养生茶吗?\nAI: 根据您的体质，我推荐以下几种养生茶...",
                "2025-11-20", 
                "10 条消息"));

        // 更新适配器数据
        chatHistoryAdapter.setData(historyItems);
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
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        LogUtils.e("按住结束");
//                        asrUtil.onClick(1);
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        LogUtils.e("按住结束");
//                        asrUtil.onClick(2);
//                        break;
//                }
//                return true;
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
}