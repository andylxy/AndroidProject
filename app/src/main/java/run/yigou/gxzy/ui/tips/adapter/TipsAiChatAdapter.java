package run.yigou.gxzy.ui.tips.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import timber.log.Timber;


public final class TipsAiChatAdapter extends AppAdapter<ChatMessageBean> {


    // 定义布局资源ID常量
    private static final int LAYOUT_RECEIVED = R.layout.tips_ai_msg_chat_receive;
    private static final int LAYOUT_SEND = R.layout.tips_ai_msg_chat_send;
    private static final int LAYOUT_SYSTEM = R.layout.tips_ai_msg_chat_system;
    private static final int LAYOUT_DEFAULT = 0; // 默认布局资源ID

    public TipsAiChatAdapter(Context context) {
        super(context);
    }

    @Override
    public int getItemViewType(int position) {

       // ChatMessageBean chatMessageBean =   getItem(position);

      //  int type =   chatMessageBean.getType();
       // EasyLog.print("getItemViewType: " + type);
        return getItem(position).getType();
    }
    public void updateData() {
        if (getData() != null) {
            notifyItemChanged(getData().size() - 1);
        }
    }
    private int viewTypeLayout;

    @NonNull
    @Override
    public TipsAiChatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        switch (viewType) {
            case ChatMessageBean.TYPE_RECEIVED:
                viewTypeLayout = LAYOUT_RECEIVED;
                break;
            case ChatMessageBean.TYPE_SEND:
                viewTypeLayout = LAYOUT_SEND;
                break;
            case ChatMessageBean.TYPE_SYSTEM:
                viewTypeLayout = LAYOUT_SYSTEM;
                break;
            default:
                // 记录日志并使用默认布局
                Timber.tag("TipsAiChatAdapter").w("Unknown view type: %s", viewType);
                viewTypeLayout = LAYOUT_DEFAULT;
                break;
        }
        // 确保 viewTypeLayout 是有效的布局资源ID
        if (viewTypeLayout == 0) {
            throw new IllegalArgumentException("Invalid layout resource ID for viewType: " + viewType);
        }

        return new TipsAiChatAdapter.ViewHolder(viewTypeLayout);
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {

        private ViewHolder(int viewlayout) {
            super(viewlayout);
        }

        @Override
        public void onBindView(int position) {
            ChatMessageBean bean = getItem(position);
            switch (bean.getType()) {
                case ChatMessageBean.TYPE_RECEIVED:
                    TextView tv_receive_content = findViewById(R.id.tv_receive_content);
                    ImageView iv_receive_picture = findViewById(R.id.iv_receive_picture);
                    TextView tv_receive_nick = findViewById(R.id.tv_receive_nick);
                    tv_receive_content.setText(bean.getContent());
                    tv_receive_nick.setText(bean.getNick());
                    iv_receive_picture.setImageResource(R.drawable.icon);
                    break;

                case ChatMessageBean.TYPE_SEND:
                    TextView tv_send_content = findViewById(R.id.tv_send_content);
                    ImageView iv_send_picture = findViewById(R.id.iv_send_picture);
                    tv_send_content.setText(bean.getContent());
                    iv_send_picture.setImageResource(R.drawable.icon);
                    break;
                case ChatMessageBean.TYPE_SYSTEM:
                    TextView tv_system_content = findViewById(R.id.tv_system_content);
                    tv_system_content.setText(bean.getContent());
                    break;
            }
        }


    }
}