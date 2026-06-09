package run.yigou.gxzy.ui.feature.ai.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import io.noties.markwon.Markwon;
import run.yigou.gxzy.R;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.entity.ChatSessionBean;
import run.yigou.gxzy.greendao.entity.ChatSummaryBean;
import run.yigou.gxzy.manager.ai.AiChatManager;
import run.yigou.gxzy.manager.ai.ChatSessionManager;
import run.yigou.gxzy.ui.dialog.ChatSummaryListDialog;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.utils.MarkdownUtils;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * 总结助手：管理总结生成、显示和采用逻辑
 */
public class ChatSummaryHelper {
    private static final String TAG = "ChatSummaryHelper";
    
    private final Context context;
    private final View rootView;
    private final Markwon markwon;
    private final OnSummaryActionListener actionListener;
    
    private CheckBox cbLatestSummary;
    private CheckBox cbAllSummary;
    private Button btnSummarize;

    public interface OnSummaryActionListener {
        ChatSessionBean getCurrentSession();
        void onSummaryGenerated(ChatMessageBean summaryMessage);
        void onSummaryStreamUpdate(ChatMessageBean summaryMessage);
        void onSummaryStreamComplete(ChatMessageBean summaryMessage, boolean success);
        void onSummaryStreamError(ChatMessageBean summaryMessage, String error);
    }

    public ChatSummaryHelper(Context context, View rootView, Markwon markwon, OnSummaryActionListener listener) {
        this.context = context;
        this.rootView = rootView;
        this.markwon = markwon;
        this.actionListener = listener;
        initView();
    }

    private void initView() {
        cbLatestSummary = rootView.findViewById(R.id.cb_latest_summary);
        cbAllSummary = rootView.findViewById(R.id.cb_all_summary);
        btnSummarize = rootView.findViewById(R.id.btn_summarize);

        if (cbLatestSummary != null) {
            cbLatestSummary.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && cbAllSummary != null) {
                    cbAllSummary.setChecked(false);
                }
            });
        }
        if (cbAllSummary != null) {
            cbAllSummary.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && cbLatestSummary != null) {
                    cbLatestSummary.setChecked(false);
                }
            });
        }

        if (btnSummarize != null) {
            btnSummarize.setOnClickListener(v -> generateSessionSummary());
        }
    }
    
    public void resetCheckboxes() {
        if (cbLatestSummary != null && cbLatestSummary.isChecked()) {
            cbLatestSummary.setChecked(false);
        }
        if (cbAllSummary != null && cbAllSummary.isChecked()) {
            cbAllSummary.setChecked(false);
        }
    }
    
    public boolean isLatestSummaryChecked() {
        return cbLatestSummary != null && cbLatestSummary.isChecked();
    }
    
    public boolean isAllSummaryChecked() {
        return cbAllSummary != null && cbAllSummary.isChecked();
    }

    private static final long UI_UPDATE_INTERVAL = 200; // 200ms 批量更新一次
    private long lastUpdateTime = 0;

    /**
     * 生成当前会话的总结
     */
    private void generateSessionSummary() {
        actionListener.onSummaryGenerated(null);
    }

    /**
     * 采用总结：将总结消息保存到总结表（ChatSummaryBean）
     */
    public void adoptSummary(ChatMessageBean summaryMessage) {
        ChatSessionBean currentSession = actionListener.getCurrentSession();
        if (summaryMessage == null) {
            Toast.makeText(context, "总结内容为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentSession == null) {
            Toast.makeText(context, "会话状态异常，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatSummaryBean summary = new ChatSummaryBean();
        summary.setSessionId(currentSession.getId());
        summary.setTitle("总结 " + DateHelper.getSeconds1()); // 简化标题
        summary.setContent(summaryMessage.getContent());
        summary.setCreateTime(DateHelper.getSeconds1());
        summary.setIsDelete(ChatSummaryBean.IS_Delete_NO);

        long id = ChatSessionManager.getInstance().saveSummary(summary);
        summary.setId(id);

        Toast.makeText(context, "总结已保存", Toast.LENGTH_SHORT).show();
        
        // 显示总结内容
        showSummaryContentDialog(summary);
    }
    
    /**
     * 显示会话的总结列表对话框
     */
    public void showSummaryListDialog(ChatSessionBean session) {
        if (session == null) return;
        
        new ChatSummaryListDialog.Builder(context)
                .setSession(session.getId(), session.getTitle())
                .setMarkwon(markwon)
                .show();
    }
    
    /**
     * 显示总结内容对话框（支持复制）
     */
    public void showSummaryContentDialog(ChatSummaryBean summary) {
        if (summary == null) return;
        
        TextView textView = new TextView(context);
        textView.setText(summary.getContent());
        textView.setTextIsSelectable(true);
        textView.setPadding(48, 32, 48, 32);
        textView.setTextSize(15);
        textView.setLineSpacing(0, 1.3f);
        
        if (markwon != null) {
            markwon.setMarkdown(textView, summary.getContent());
        }
        
        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        scrollView.addView(textView);
        
        android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int maxHeight = (int) (displayMetrics.heightPixels * 0.7);
        scrollView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        
        final TextView finalTextView = textView;
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(summary.getTitle())
                .setView(scrollView)
                .setPositiveButton("复制全部", (d, which) -> {
                    String renderedText = finalTextView.getText().toString();
                    MarkdownUtils.copyToClipboard(context, renderedText);
                })
                .setNeutralButton("删除", (d, which) -> {
                    summary.setIsDelete(ChatSummaryBean.IS_Delete_YES);
                    ChatSessionManager.getInstance().updateSummary(summary);
                    Toast.makeText(context, "总结已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("关闭", null)
                .create();
        
        dialog.show();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.min(maxHeight, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }
}
