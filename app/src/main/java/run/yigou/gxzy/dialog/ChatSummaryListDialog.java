package run.yigou.gxzy.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hjq.toast.Toaster;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.hjq.base.BaseDialog;
import com.hjq.widget.layout.WrapRecyclerView;

import java.util.List;
import java.util.concurrent.Callable;

import io.noties.markwon.Markwon;
import run.yigou.gxzy.R;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.Callback;
import run.yigou.gxzy.utils.ThreadUtil;
import run.yigou.gxzy.data.local.entity.ChatSummaryBean;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.local.helper.LocalServices;
import run.yigou.gxzy.ui.reader.ai.adapter.ChatSummaryAdapter;

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

            // 摘要读取挪到串行后台线程（统一入口见 DbService.readInBackground）；
            // findBySessionId 查出后会就地解密（RC4），这个语义由 Service 保证，换线程不影响。
            final Long sessionId = mSessionId;
            DbService.getInstance().readInBackground(
                    new Callable<List<ChatSummaryBean>>() {
                        @Override
                        public List<ChatSummaryBean> call() {
                            return LocalServices.getInstance()
                                    .mChatSummaryBeanService.findBySessionId(sessionId);
                        }
                    },
                    new Callback<List<ChatSummaryBean>>() {
                        @Override
                        public void onSuccess(List<ChatSummaryBean> loaded) {
                            applySummaries(loaded);
                        }

                        @Override
                        public void onError(Exception e) {
                            // 读失败按"没有摘要"渲染，与迁移前一致
                            applySummaries(null);
                        }
                    });
        }

        /** 把后台读到的摘要交给 UI（主线程）。 */
        private void applySummaries(List<ChatSummaryBean> summaries) {
            // 读在后台，回调回来时弹窗可能已经关掉：此时再动 View 就是操作一个已回收的窗口。
            if (!isShowing()) {
                return;
            }
            if (summaries == null || summaries.isEmpty()) {
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
            // 软删除：写库进串行后台线程（ChatSummaryBeanService 会先加密再写库、
            // 写完把明文改回实体，这个语义与在哪条线程上执行无关）
            final ChatSummaryBean removing = item;
            DbService.getInstance().runInBackgroundSerial(new Runnable() {
                @Override
                public void run() {
                    try {
                        removing.setIsDelete(ChatSummaryBean.IS_Delete_YES);
                        LocalServices.getInstance().mChatSummaryBeanService.updateEntity(removing);
                    } catch (Throwable t) {
                        EasyLog.print(t);
                    }
                }
            });

            // 从列表中移除
            mAdapter.removeItem(position);

            // 检查是否为空
            if (mAdapter.getItemCount() == 0) {
                mEmptyHintView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            }

            Toaster.show("总结已删除");

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
                        Toaster.show("已复制到剪贴板");
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
