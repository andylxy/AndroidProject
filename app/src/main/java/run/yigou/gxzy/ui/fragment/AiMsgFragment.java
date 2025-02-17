package run.yigou.gxzy.ui.fragment;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.ui.activity.CopyActivity;
import run.yigou.gxzy.ui.tips.adapter.ChatAdapter;
import run.yigou.gxzy.ui.tips.aimsg.ChatMessage;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : 可进行拷贝的副本
 */
public final class AiMsgFragment extends AppFragment<CopyActivity> {
    private List<ChatMessage> msgList = new ArrayList<ChatMessage>();

    private EditText inputText;

    private Button send;

    private RecyclerView msgRecyclerView;

    private ChatAdapter adapter;
    public static AiMsgFragment newInstance() {
        return new AiMsgFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tips_ai_msg_fragment;
    }

    @Override
    protected void initView() {

        initMsgs(); // 初始化消息数据
        inputText = (EditText) findViewById(R.id.input_text);
        send = (Button) findViewById(R.id.send);
        msgRecyclerView = (RecyclerView) findViewById(R.id.msg_recycle_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new ChatAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = inputText.getText().toString();
                if (!"".equals(content)) {
                    ChatMessage msg = new ChatMessage(content, ChatMessage.TYPE_SENT);
                    msgList.add(msg);
                    // 当有新消息时，刷新ListView中的显示
                    adapter.notifyItemInserted(msgList.size() - 1);
                    // 将ListView定位到最后一行
                    msgRecyclerView.scrollToPosition(msgList.size() - 1);
                    // 清空输入框中的内容
                    inputText.setText("");
                }
            }
        });
    }

    /**
     * 初始化聊天消息
     */
    private void initMsgs() {
        ChatMessage msg1 = new ChatMessage("Hello guy.", ChatMessage.TYPE_RECEIVED);
        msgList.add(msg1);
        ChatMessage msg2 = new ChatMessage("Hello. Who is that?", ChatMessage.TYPE_SENT);
        msgList.add(msg2);
        ChatMessage msg3 = new ChatMessage("This is Tom. Nice talking to you. ", ChatMessage.TYPE_RECEIVED);
        msgList.add(msg3);
    }

    @Override
    protected void initData() {
        toast("AI页面信息!!!!");
    }
}


