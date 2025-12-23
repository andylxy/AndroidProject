package run.yigou.gxzy.ui.tips.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;

public class ChatHistoryAdapter extends AppAdapter<ChatHistoryAdapter.ChatHistoryItem> {

    private int selectedPosition = -1;
    private OnChatHistoryItemClickListener mListener;
    private OnChatHistoryItemDeleteListener mDeleteListener;
    private OnChatHistoryItemEditTitleListener mEditTitleListener;
    private OnChatHistoryItemSummaryListener mSummaryListener;

    public ChatHistoryAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(R.layout.chat_history_item);
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public void setOnChatHistoryItemClickListener(OnChatHistoryItemClickListener listener) {
        mListener = listener;
    }

    public void setOnChatHistoryItemDeleteListener(OnChatHistoryItemDeleteListener listener) {
        mDeleteListener = listener;
    }
    
    public void setOnChatHistoryItemEditTitleListener(OnChatHistoryItemEditTitleListener listener) {
        mEditTitleListener = listener;
    }

    public void setOnChatHistoryItemSummaryListener(OnChatHistoryItemSummaryListener listener) {
        mSummaryListener = listener;
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {
        private TextView tvTitle;
        private TextView tvPreview;
        private TextView tvTime;
        private TextView tvMessageCount;
        private ImageButton btnDelete;
        private ImageButton btnEditTitle;
        private ImageButton btnSummaryManage;

        private ViewHolder(int viewLayout) {
            super(viewLayout);
            tvTitle = findViewById(R.id.tv_chat_title);
            tvPreview = findViewById(R.id.tv_chat_preview);
            tvTime = findViewById(R.id.tv_chat_time);
            tvMessageCount = findViewById(R.id.tv_message_count);
            btnDelete = findViewById(R.id.btn_delete);
            btnEditTitle = findViewById(R.id.btn_edit_title);
            btnSummaryManage = findViewById(R.id.btn_summary_manage);
        }

        @Override
        public void onBindView(int position) {
            ChatHistoryItem item = getItem(position);

            tvTitle.setText(item.getTitle());
            tvPreview.setText(item.getPreview());
            tvTime.setText(item.getTime());
            tvMessageCount.setText(item.getMessageCount());

            // 设置斑马纹背景
            if (position % 2 == 0) {
                // 偶数行使用较浅的背景色
                getItemView().setBackgroundResource(R.drawable.chat_history_item_bg_even);
            } else {
                // 奇数行使用稍深的背景色
                getItemView().setBackgroundResource(R.drawable.chat_history_item_bg_odd);
            }

            // 设置选中状态
            if (position == selectedPosition) {
                getItemView().setBackgroundColor(getContext().getResources().getColor(R.color.common_primary_color));
            }
            
            // 设置删除按钮点击事件
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 显示确认对话框
                    showDeleteConfirmationDialog(position, item);
                }
            });
            
            // 设置编辑标题按钮点击事件
            btnEditTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mEditTitleListener != null) {
                        mEditTitleListener.onChatHistoryItemEditTitle(position, item);
                    }
                }
            });
            
            // 设置总结管理按钮点击事件
            btnSummaryManage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSummaryListener != null) {
                        mSummaryListener.onChatHistoryItemSummary(position, item);
                    }
                }
            });

            getItemView().setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onChatHistoryItemClick(position, item);
                }
            });
        }

        private void showDeleteConfirmationDialog(int position, ChatHistoryItem item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("确认删除");
            builder.setMessage("确定要删除该会话记录吗？删除后将无法恢复。");
            builder.setPositiveButton("确定", (dialog, which) -> {
                if (mDeleteListener != null) {
                    mDeleteListener.onChatHistoryItemDelete(position, item);
                }
            });
            builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public interface OnChatHistoryItemClickListener {
        void onChatHistoryItemClick(int position, ChatHistoryItem item);
    }

    public interface OnChatHistoryItemDeleteListener {
        void onChatHistoryItemDelete(int position, ChatHistoryItem item);
    }
    
    public interface OnChatHistoryItemEditTitleListener {
        void onChatHistoryItemEditTitle(int position, ChatHistoryItem item);
    }
    
    public interface OnChatHistoryItemSummaryListener {
        void onChatHistoryItemSummary(int position, ChatHistoryItem item);
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