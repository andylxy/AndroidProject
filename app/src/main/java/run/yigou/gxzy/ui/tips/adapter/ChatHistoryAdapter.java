package run.yigou.gxzy.ui.tips.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;

public class ChatHistoryAdapter extends AppAdapter<ChatHistoryAdapter.ChatHistoryItem> {
    private Context context;

    public ChatHistoryAdapter(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(R.layout.chat_history_item);
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {

        private ViewHolder(int viewlayout) {
            super(viewlayout);
        }

        @Override
        public void onBindView(int position) {
            ChatHistoryItem item = getItem(position);
            
            TextView tvChatTitle = findViewById(R.id.tv_chat_title);
            TextView tvChatPreview = findViewById(R.id.tv_chat_preview);
            TextView tvChatTime = findViewById(R.id.tv_chat_time);
            TextView tvMessageCount = findViewById(R.id.tv_message_count);

            if (tvChatTitle != null) {
                tvChatTitle.setText(item.getTitle());
            }
            
            if (tvChatPreview != null) {
                tvChatPreview.setText(item.getPreview());
            }
            
            if (tvChatTime != null) {
                tvChatTime.setText(item.getTime());
            }
            
            if (tvMessageCount != null) {
                tvMessageCount.setText(item.getMessageCount());
            }
            
            // 设置斑马纹背景
            if (position % 2 == 0) {
                itemView.setBackgroundResource(R.drawable.chat_history_item_bg_even);
            } else {
                itemView.setBackgroundResource(R.drawable.chat_history_item_bg_odd);
            }
        }
    }

    public static class ChatHistoryItem {
        private String title;
        private String preview;
        private String time;
        private String messageCount;

        public ChatHistoryItem(String title, String preview, String time, String messageCount) {
            this.title = title;
            this.preview = preview;
            this.time = time;
            this.messageCount = messageCount;
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
    }
}