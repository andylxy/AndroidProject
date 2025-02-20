package run.yigou.gxzy.ui.fragment;

import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;

import run.yigou.gxzy.EventBus.ChatMessageBeanEvent;
import run.yigou.gxzy.EventBus.TipsFragmentSettingEventNotification;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.activity.AiConfigActivity;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.tips.adapter.TipsAiChatAdapter;
import run.yigou.gxzy.ui.tips.aimsg.AiConfig;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.ui.tips.aimsg.HttpUtil;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.ThreadUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public final class AiMsgFragment extends TitleBarFragment<HomeActivity> {

    private RecyclerView rv_chat;
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
        if (AiConfig.getAssistantName() != null)
            ((TextView) findViewById(R.id.tv_title)).setText(AiConfig.getAssistantName());
        rv_chat = findViewById(R.id.rv_chat);
        // 获取 Activity
        Activity activity = getActivity();
        if (activity == null) return;
        // 创建并设置适配器
        mChatAdapter = new TipsAiChatAdapter(activity);
        mChatAdapter.setHasStableIds(true);
        // 设置 RecyclerView 的动画时长
        rv_chat.getItemAnimator().setChangeDuration(0);
        // 设置 RecyclerView 的布局管理器和适配器
        rv_chat.setLayoutManager(new LinearLayoutManager(getContext()));
        rv_chat.setAdapter(mChatAdapter);
        // 注册事件
        XEventBus.getDefault().register(AiMsgFragment.this);
        // 初始化消息数据
        initMsgs();
    }


    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(priority = 1)
    public void ChatMessageEvent(ChatMessageBeanEvent event) {
        ThreadUtil.runOnUiThread(() -> {
            if (event.isClear()) {
                initData();
                mChatAdapter.notifyDataSetChanged();
            }
            if (event.isAssistantName())
                ((TextView) findViewById(R.id.tv_title)).setText(AiConfig.getAssistantName());
        });
    }

    /**
     * 初始化聊天消息
     */
    private void initMsgs() {

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

                // 检查 API Key 是否为空
                if (AiConfig.getApiKey() == null || AiConfig.getApiKey().isEmpty()) {
                    startActivity(new Intent(getActivity(), AiConfigActivity.class));
                }

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
                        final ChatMessageBean receivedMessage = new ChatMessageBean(ChatMessageBean.TYPE_RECEIVED, AiConfig.getAssistantName(), "", "请稍等...");
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

                        final int[] index = {0};
                        HttpUtil.chat(result, new HttpUtil.CallBack() {
                            @Override
                            public void onCallBack(final String result, final boolean isLast) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        receivedMessage.setContent(result);

                                        // 根据滚动状态决定是否更新数据
                                        if ((scrollState == 0 && index[0] % 3 == 0) || isLast) {
                                            mChatAdapter.updateData();
                                        }

                                        // 滚动到最后一条消息
                                        if ((scrollState == 0 && ++index[0] % 20 == 0) || isLast) {
                                            if (rv_chat != null) {
                                                rv_chat.scrollToPosition(mChatAdapter.getData().size() - 1);
                                            }
                                        }

                                        if (isLast) {
                                            // 将回复消息插入数据库
                                            receivedMessage.setCreateDate(DateHelper.getSeconds1());
                                            receivedMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
                                            DbService.getInstance().mChatMessageBeanService.addEntity(receivedMessage);
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
        findViewById(R.id.ll_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), AiConfigActivity.class));
            }
        });

    }

    private ArrayList<ChatMessageBean> chatMessageBeans;

    @Override
    protected void initData() {
        chatMessageBeans = DbService.getInstance().mChatMessageBeanService.findAll();
        if (chatMessageBeans != null) {
            mChatAdapter.setData(chatMessageBeans);
            return;
        }
        mChatAdapter.setData(new ArrayList<ChatMessageBean>());
    }

    @Override
    public void onDestroy() {
        // 注销事件
        XEventBus.getDefault().unregister(AiMsgFragment.this);
        super.onDestroy();
    }
}


