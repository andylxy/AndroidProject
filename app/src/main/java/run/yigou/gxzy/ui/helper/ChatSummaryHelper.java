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

        // 创建思考消息
        final ChatMessageBean thinkingMessage = new ChatMessageBean();
        thinkingMessage.setType(ChatMessageBean.TYPE_THINKING);
        thinkingMessage.setContent("正在思考...");
        thinkingMessage.setNick("Ai");
        thinkingMessage.setCreateDate(DateHelper.getSeconds1());
        thinkingMessage.setSessionId(currentSession.getId());
        thinkingMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
        
        // 立即保存思考消息到数据库以获取 ID
        long thinkingId = ChatSessionManager.getInstance().saveMessage(thinkingMessage);
        thinkingMessage.setId(thinkingId);
        
        // 通知 UI 添加思考消息
        actionListener.onSummaryGenerated(thinkingMessage);

        // 创建总结消息（初始不显示）
        final ChatMessageBean summaryMessage = new ChatMessageBean();
        summaryMessage.setType(ChatMessageBean.TYPE_SUMMARY);
        summaryMessage.setContent("");
        summaryMessage.setNick("会话总结");
        summaryMessage.setCreateDate(DateHelper.getSeconds1());
        summaryMessage.setSessionId(currentSession.getId());
        summaryMessage.setIsDelete(ChatMessageBean.IS_Delete_NO);
        summaryMessage.setStreaming(true);
        
        // 立即保存总结消息到数据库以获取 ID
        long summaryId = ChatSessionManager.getInstance().saveMessage(summaryMessage);
        summaryMessage.setId(summaryId);

        final StringBuilder summaryContent = new StringBuilder();
        
        // 标记是否已经开始接收回答
        final boolean[] hasStartedAnswer = {false};

        AiChatManager.getInstance().generateSummary(currentSession, prompt, new AiChatManager.ChatStreamListener() {
            @Override
            public void onThinking(String content) {
                ThreadUtil.runOnUiThread(() -> {
                    String current = thinkingMessage.getContent();
                    if ("正在思考...".equals(current)) {
                        current = "";
                    }
                    thinkingMessage.setContent(current + content);
                    actionListener.onSummaryStreamUpdate(thinkingMessage);
                });
            }

            @Override
            public void onAnswerStart(ChatMessageBean answerMessage) {
                // 忽略传入的 answerMessage，使用我们自己创建的 summaryMessage
            }

            @Override
            public void onAnswerChunk(String content) {
                // 1. 在当前线程（后台）更新数据，避免频繁切换主线程
                summaryContent.append(content);
                
                // 如果是第一次收到回答，先通知 UI 添加总结消息
                if (!hasStartedAnswer[0]) {
                    hasStartedAnswer[0] = true;
                    ThreadUtil.runOnUiThread(() -> {
                        thinkingMessage.setThinkingCollapsed(true);
                        actionListener.onSummaryStreamUpdate(thinkingMessage);
                        actionListener.onSummaryGenerated(summaryMessage);
                    });
                }
                
                // 2. 节流判断
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime > UI_UPDATE_INTERVAL) {
                    lastUpdateTime = currentTime;
                    
                    // 获取当前完整内容
                    final String currentFullText = summaryContent.toString();
                    
                    // 3. 仅在需要刷新 UI 时切换到主线程
                    ThreadUtil.runOnUiThread(() -> {
                        summaryMessage.setContent(currentFullText);
                        actionListener.onSummaryStreamUpdate(summaryMessage);
                    });
                }
            }

            @Override
            public void onComplete() {
                ThreadUtil.runOnUiThread(() -> {
                    // 确保思考消息被折叠
                    thinkingMessage.setThinkingCollapsed(true);
                    actionListener.onSummaryStreamUpdate(thinkingMessage);

                    // ⚠️ 先切断流式状态，确保后续操作针对的是完整消息
                    summaryMessage.setStreaming(false);
                    
                    if (summaryContent.length() > 0) {
                        summaryMessage.setContent(summaryContent.toString());
                        
                        // 更新数据库中的消息（而不是再次保存）
                        ChatSessionManager.getInstance().updateMessage(thinkingMessage); 
                        ChatSessionManager.getInstance().updateMessage(summaryMessage);
                        
                        // 通知 UI 成功完成（这里会触发最后一次刷新，确保内容完整并渲染 Markdown）
                        actionListener.onSummaryStreamComplete(summaryMessage, true);
                        
                        // ⚠️ 使用 Application Context 显示 Toast，避免 Activity 销毁导致的问题
                        Toast.makeText(context.getApplicationContext(), "总结已生成，长按可选择采用", Toast.LENGTH_SHORT).show();
                    } else {
                        // 如果没有生成总结，移除思考消息和总结消息
                        actionListener.onSummaryStreamError(thinkingMessage, "生成失败");
                        actionListener.onSummaryStreamComplete(summaryMessage, false);
                        Toast.makeText(context.getApplicationContext(), "生成总结失败: 内容为空", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                ThreadUtil.runOnUiThread(() -> {
                    // 记录错误到消息中
                    String errStr = "\n[错误: " + error + "]";
                    if (summaryContent.length() > 0) {
                        summaryMessage.setContent(summaryMessage.getContent() + errStr);
                        summaryMessage.setStreaming(false);
                        actionListener.onSummaryStreamUpdate(summaryMessage);
                    } else {
                        thinkingMessage.setContent(thinkingMessage.getContent() + errStr);
                        actionListener.onSummaryStreamUpdate(thinkingMessage);
                    }
                    
                    // 忽略 "canceled" 错误，因为这是正常的流结束或中断
                    if (!"canceled".equals(error)) {
                        Toast.makeText(context.getApplicationContext(), "生成总结失败: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
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
