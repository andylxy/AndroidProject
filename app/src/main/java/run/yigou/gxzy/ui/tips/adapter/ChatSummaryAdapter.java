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
import run.yigou.gxzy.greendao.entity.ChatSummaryBean;

/**
 * 会话总结列表适配器
 */
public class ChatSummaryAdapter extends AppAdapter<ChatSummaryBean> {

    private OnSummaryItemClickListener mClickListener;
    private OnSummaryItemDeleteListener mDeleteListener;

    public ChatSummaryAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(R.layout.item_summary);
    }

    public void setOnSummaryItemClickListener(OnSummaryItemClickListener listener) {
        mClickListener = listener;
    }

    public void setOnSummaryItemDeleteListener(OnSummaryItemDeleteListener listener) {
        mDeleteListener = listener;
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {
        private TextView tvTitle;
        private TextView tvPreview;
        private TextView tvTime;
        private ImageButton btnDelete;

        private ViewHolder(int viewLayout) {
            super(viewLayout);
            tvTitle = findViewById(R.id.tv_summary_title);
            tvPreview = findViewById(R.id.tv_summary_preview);
            tvTime = findViewById(R.id.tv_summary_time);
            btnDelete = findViewById(R.id.btn_delete_summary);
        }

        @Override
        public void onBindView(int position) {
            ChatSummaryBean item = getItem(position);
            if (item == null) return;

            tvTitle.setText(item.getTitle());
            
            // 预览内容，限制长度
            String preview = item.getContent();
            if (preview != null && preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            tvPreview.setText(preview);
            
            tvTime.setText(item.getCreateTime());

            // 设置斑马纹背景
            if (position % 2 == 0) {
                getItemView().setBackgroundResource(R.drawable.chat_history_item_bg_even);
            } else {
                getItemView().setBackgroundResource(R.drawable.chat_history_item_bg_odd);
            }

            // 点击事件 - 查看详情
            getItemView().setOnClickListener(v -> {
                if (mClickListener != null) {
                    mClickListener.onSummaryItemClick(position, item);
                }
            });

            // 删除按钮点击事件
            btnDelete.setOnClickListener(v -> {
                showDeleteConfirmDialog(position, item);
            });
        }

        private void showDeleteConfirmDialog(int position, ChatSummaryBean item) {
            new AlertDialog.Builder(getContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除这条总结吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        if (mDeleteListener != null) {
                            mDeleteListener.onSummaryItemDelete(position, item);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    /**
     * 总结项点击监听
     */
    public interface OnSummaryItemClickListener {
        void onSummaryItemClick(int position, ChatSummaryBean item);
    }

    /**
     * 总结项删除监听
     */
    public interface OnSummaryItemDeleteListener {
        void onSummaryItemDelete(int position, ChatSummaryBean item);
    }
}
