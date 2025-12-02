package run.yigou.gxzy.ui.tips.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;

public class ChatHistoryAdapter extends AppAdapter<ChatHistoryAdapter.ChatHistoryItem> {
    private Context context;
    private OnChatHistoryItemClickListener listener;
    private int selectedPosition = -1;

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
        private TextView tvChatTitle;
        private TextView tvChatPreview;
        private TextView tvChatTime;
        private TextView tvMessageCount;

        private ViewHolder(int viewlayout) {
            super(viewlayout);
            
            tvChatTitle = findViewById(R.id.tv_chat_title);
            tvChatPreview = findViewById(R.id.tv_chat_preview);
            tvChatTime = findViewById(R.id.tv_chat_time);
            tvMessageCount = findViewById(R.id.tv_message_count);
            
            // 设置点击事件
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // 更新选中状态
                        selectedPosition = position;
                        notifyDataSetChanged();
                        
                        // 回调接口通知外部
                        if (listener != null) {
                            listener.onChatHistoryItemClick(position, getItem(position));
                        }
                    }
                }
            });
        }

        @Override
        public void onBindView(int position) {
            ChatHistoryItem item = getItem(position);
            
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
            
            // 设置选中状态背景
            if (position == selectedPosition) {
                itemView.setBackgroundResource(R.drawable.chat_history_item_bg_selected);
            } else if (position % 2 == 0) {
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
    
    // 设置监听器
    public void setOnChatHistoryItemClickListener(OnChatHistoryItemClickListener listener) {
        this.listener = listener;
    }
    
    // 选中指定位置的项
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }
    
    // 获取选中位置
    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    // 定义点击事件接口
    public interface OnChatHistoryItemClickListener {
        void onChatHistoryItemClick(int position, ChatHistoryItem item);
    }
}