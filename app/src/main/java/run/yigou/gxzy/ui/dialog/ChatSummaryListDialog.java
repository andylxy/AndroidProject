package run.yigou.gxzy.ui.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.hjq.base.BaseDialog;
import com.hjq.widget.layout.WrapRecyclerView;

import java.util.List;

import io.noties.markwon.Markwon;
import run.yigou.gxzy.R;
import run.yigou.gxzy.greendao.entity.ChatSummaryBean;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.tips.adapter.ChatSummaryAdapter;

/**
 * 会话总结列表对话框
 */
public final class ChatSummaryListDialog {

    public static final class Builder extends BaseDialog.Builder<Builder> {

        private final TextView mTitleView;
        private final TextView mEmptyHintView;
        private final WrapRecyclerView mRecyclerView;
        private final ImageButton mCloseButton;

        private ChatSummaryAdapter mAdapter;
        private Long mSessionId;
        private String mSessionTitle;
        private Markwon mMarkwon;
        private OnSummaryChangedListener mOnSummaryChangedListener;

        public Builder(Context context) {
            super(context);

            setContentView(R.layout.dialog_summary_list);
            setAnimStyle(BaseDialog.ANIM_SCALE);
            setGravity(Gravity.CENTER);

            mTitleView = findViewById(R.id.tv_dialog_title);
            mEmptyHintView = findViewById(R.id.tv_empty_hint);
            mRecyclerView = findViewById(R.id.rv_summary_list);
            mCloseButton = findViewById(R.id.btn_close);

            // 设置关闭按钮
            mCloseButton.setOnClickListener(v -> dismiss());

            // 初始化 RecyclerView
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            mAdapter = new ChatSummaryAdapter(context);
            mRecyclerView.setAdapter(mAdapter);

            // 设置点击事件 - 查看总结详情
            mAdapter.setOnSummaryItemClickListener((position, item) -> {
                showSummaryContentDialog(item);
            });

            // 设置删除事件
            mAdapter.setOnSummaryItemDeleteListener((position, item) -> {
                deleteSummary(position, item);
            });
        }

        /**
         * 设置会话信息
         */
        public Builder setSession(Long sessionId, String sessionTitle) {
            mSessionId = sessionId;
            mSessionTitle = sessionTitle;
            mTitleView.setText("会话总结 - " + sessionTitle);
            loadSummaries();
            return this;
        }

        /**
         * 设置 Markwon 用于渲染 Markdown
         */
        public Builder setMarkwon(Markwon markwon) {
            mMarkwon = markwon;
            return this;
        }

        /**
         * 设置总结变化监听
         */
        public Builder setOnSummaryChangedListener(OnSummaryChangedListener listener) {
            mOnSummaryChangedListener = listener;
            return this;
        }

        /**
         * 加载总结列表
         */
        private void loadSummaries() {
            if (mSessionId == null) {
                mEmptyHintView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
                return;
            }

            List<ChatSummaryBean> summaries = DbService.getInstance()
                    .mChatSummaryBeanService.findBySessionId(mSessionId);

            if (summaries.isEmpty()) {
                mEmptyHintView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else {
                mEmptyHintView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
                mAdapter.setData(summaries);
            }
        }

        /**
         * 删除总结
         */
        private void deleteSummary(int position, ChatSummaryBean item) {
            // 软删除
            item.setIsDelete(ChatSummaryBean.IS_Delete_YES);
            DbService.getInstance().mChatSummaryBeanService.updateEntity(item);

            // 从列表中移除
            mAdapter.removeItem(position);

            // 检查是否为空
            if (mAdapter.getItemCount() == 0) {
                mEmptyHintView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            }

            Toast.makeText(getContext(), "总结已删除", Toast.LENGTH_SHORT).show();

            // 通知外部
            if (mOnSummaryChangedListener != null) {
                mOnSummaryChangedListener.onSummaryDeleted(item);
            }
        }

        /**
         * 显示总结内容详情对话框
         */
        private void showSummaryContentDialog(ChatSummaryBean summary) {
            if (summary == null) return;

            // 创建可选择文字的 TextView
            TextView textView = new TextView(getContext());
            textView.setText(summary.getContent());
            textView.setTextIsSelectable(true);
            textView.setPadding(48, 32, 48, 32);
            textView.setTextSize(15);
            textView.setLineSpacing(0, 1.3f);

            // 使用 Markwon 渲染 Markdown
            if (mMarkwon != null) {
                mMarkwon.setMarkdown(textView, summary.getContent());
            }

            // 包装在 ScrollView 中以支持长文本
            android.widget.ScrollView scrollView = new android.widget.ScrollView(getContext());
            scrollView.addView(textView);

            // 设置最大高度为屏幕高度的 70%
            android.util.DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            int maxHeight = (int) (displayMetrics.heightPixels * 0.7);
            scrollView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

            // 创建对话框 - 需要保存 textView 的引用用于复制
            final TextView finalTextView = textView;
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                    .setTitle(summary.getTitle())
                    .setView(scrollView)
                    .setPositiveButton("复制全部", (d, which) -> {
                        // 复制渲染后的纯文本，而非原始 Markdown
                        String renderedText = finalTextView.getText().toString();
                        android.content.ClipboardManager clipboard =
                                (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("会话总结", renderedText);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("关闭", null)
                    .create();

            dialog.show();

            // 限制对话框最大高度
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.min(maxHeight, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
    }

    /**
     * 总结变化监听接口
     */
    public interface OnSummaryChangedListener {
        void onSummaryDeleted(ChatSummaryBean summary);
    }
}
