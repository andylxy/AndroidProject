package run.yigou.gxzy.ui.helper;

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
import run.yigou.gxzy.manager.AiChatManager;
import run.yigou.gxzy.manager.ChatSessionManager;
import run.yigou.gxzy.ui.dialog.ChatSummaryListDialog;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.EasyLog;
import run.yigou.gxzy.utils.MarkdownUtils;

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

    /**
     * 生成当前会话的总结
     */
    private void generateSessionSummary() {
        ChatSessionBean currentSession = actionListener.getCurrentSession();
        if (currentSession == null) {
            Toast.makeText(context, "请先选择一个会话", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前会话的所有消息
        currentSession.resetMessages();
        List<ChatMessageBean> messages = currentSession.getMessages();
        if (messages == null || messages.isEmpty()) {
            Toast.makeText(context, "当前会话没有消息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建总结请求的 prompt
        String prompt = AiChatManager.getInstance().generateSummaryPrompt(messages);

        // 显示进度提示
        Toast.makeText(context, "正在生成总结...", Toast.LENGTH_SHORT).show();

        // 创建总结消息
        final ChatMessageBean summaryMessage = new ChatMessageBean();
        summaryMessage.setType(ChatMessageBean.TYPE_SUMMARY);
        summaryMessage.setContent("");
        summaryMessage.setNick("会话总结");
        summaryMessage.setCreateDate(DateHelper.getSeconds1());
        summaryMessage.setSessionId(currentSession.getId());
        summaryMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
        summaryMessage.setStreaming(true);

        // 通知 UI 添加消息
        actionListener.onSummaryGenerated(summaryMessage);

        final StringBuilder summaryContent = new StringBuilder();

        AiChatManager.getInstance().generateSummary(currentSession, prompt, new AiChatManager.ChatStreamListener() {
            @Override
            public void onThinking(String content) {}

            @Override
            public void onAnswerStart(ChatMessageBean answerMessage) {}

            @Override
            public void onAnswerChunk(String content) {
                summaryContent.append(content);
                summaryMessage.setContent(summaryContent.toString());
                actionListener.onSummaryStreamUpdate(summaryMessage);
            }

            @Override
            public void onComplete() {
                summaryMessage.setStreaming(false);
                if (summaryContent.length() > 0) {
                    summaryMessage.setContent(summaryContent.toString());
                    long msgId = ChatSessionManager.getInstance().saveMessage(summaryMessage);
                    summaryMessage.setId(msgId);
                    actionListener.onSummaryStreamComplete(summaryMessage, true);
                    Toast.makeText(context, "总结已生成，长按可选择采用", Toast.LENGTH_SHORT).show();
                } else {
                    actionListener.onSummaryStreamComplete(summaryMessage, false);
                    Toast.makeText(context, "生成总结失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                actionListener.onSummaryStreamError(summaryMessage, error);
                Toast.makeText(context, "生成总结失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 采用总结：将总结消息保存到总结表（ChatSummaryBean）
     */
    public void adoptSummary(ChatMessageBean summaryMessage) {
        ChatSessionBean currentSession = actionListener.getCurrentSession();
        if (summaryMessage == null || currentSession == null) return;

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
