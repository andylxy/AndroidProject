package run.yigou.gxzy.ui.feature.ai.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.data.local.entity.ChatMessageBean;
import run.yigou.gxzy.data.local.entity.ChatSessionBean;
import run.yigou.gxzy.manager.ai.ChatSessionManager;
import run.yigou.gxzy.ui.feature.ai.adapter.ChatHistoryAdapter;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.log.EasyLog;

/**
 * 侧边栏助手：管理聊天历史记录列表
 */
public class ChatSidebarHelper {
    private static final String TAG = "ChatSidebarHelper";
    
    private final Context context;
    private final View rootView;
    private DrawerLayout drawerLayout;
    private RecyclerView chatHistoryList;
    private ChatHistoryAdapter chatHistoryAdapter;
    
    private OnSidebarActionListener actionListener;

    public interface OnSidebarActionListener {
        void onSessionSelected(ChatSessionBean session);
        void onSessionDeleted(ChatSessionBean session);
        void onSessionTitleEdited(ChatSessionBean session);
        void onSessionSummaryRequested(ChatSessionBean session);
        void onClearAllSessions();
    }

    public ChatSidebarHelper(Context context, View rootView, OnSidebarActionListener listener) {
        this.context = context;
        this.rootView = rootView;
        this.actionListener = listener;
        initView();
    }

    private void initView() {
        drawerLayout = rootView.findViewById(R.id.drawer_layout);
        chatHistoryList = rootView.findViewById(R.id.chat_history_list);

        // 初始化聊天历史记录列表
        chatHistoryAdapter = new ChatHistoryAdapter(context);
        chatHistoryList.setAdapter(chatHistoryAdapter);
        chatHistoryList.setLayoutManager(new LinearLayoutManager(context));

        // 设置聊天历史记录项监听器
        chatHistoryAdapter.setOnChatHistoryItemClickListener(this::onHistoryItemClick);
        chatHistoryAdapter.setOnChatHistoryItemDeleteListener(this::onHistoryItemDelete);
        chatHistoryAdapter.setOnChatHistoryItemEditTitleListener(this::onHistoryItemEditTitle);
        chatHistoryAdapter.setOnChatHistoryItemSummaryListener(this::onHistoryItemSummary);

        // 禁止通过边缘滑动手势打开侧边栏
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }

        // 设置侧边栏清空所有会话按钮点击事件
        ImageButton clearAllButton = rootView.findViewById(R.id.btn_edit_title);
        if (clearAllButton != null) {
            clearAllButton.setOnClickListener(v -> showClearAllSessionsDialog());
        }
    }

    /**
     * 打开侧边栏
     */
    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(rootView.findViewById(R.id.side_panel));
        }
    }

    /**
     * 关闭侧边栏
     */
    public void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
    }

    /**
     * 刷新侧边栏数据
     * @param currentSession 当前选中的会话（用于高亮）
     */
    public void refreshChatHistorySidebar(ChatSessionBean currentSession) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();

        List<ChatHistoryAdapter.ChatHistoryItem> historyItems = new ArrayList<>();
        int currentSessionPosition = 0;
        
        for (int i = 0; i < sessions.size(); i++) {
            ChatSessionBean session = sessions.get(i);
            List<ChatMessageBean> messages = session.getMessages();
            String messageCount = messages.size() + " 条消息";

            historyItems.add(new ChatHistoryAdapter.ChatHistoryItem(
                    session.getTitle(),
                    session.getPreview(),
                    session.getUpdateTime().substring(0, 10),
                    messageCount));
            
            // 找到当前会话的位置
            if (currentSession != null && session.getId().equals(currentSession.getId())) {
                currentSessionPosition = i;
            }
        }

        // 更新适配器数据
        chatHistoryAdapter.setData(historyItems);
        chatHistoryAdapter.setSelectedPosition(currentSessionPosition);
        EasyLog.print(TAG, "Refreshed sidebar, current session at position: " + currentSessionPosition);
    }
    
    /**
     * 清空侧边栏
     */
    public void clearHistory() {
        chatHistoryAdapter.setData(new ArrayList<>());
    }

    private void onHistoryItemClick(int position, ChatHistoryAdapter.ChatHistoryItem item) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (position >= 0 && position < sessions.size()) {
            ChatSessionBean selectedSession = sessions.get(position);
            ChatSessionManager.getInstance().saveLastSessionId(selectedSession.getId());
            
            if (actionListener != null) {
                actionListener.onSessionSelected(selectedSession);
            }
            closeDrawer();
        }
    }

    private void onHistoryItemDelete(int position, ChatHistoryAdapter.ChatHistoryItem item) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (position >= 0 && position < sessions.size()) {
            ChatSessionBean sessionToDelete = sessions.get(position);
            ChatSessionManager.getInstance().deleteSession(sessionToDelete);

            if (actionListener != null) {
                actionListener.onSessionDeleted(sessionToDelete);
            }
            
            // 重新加载列表（注意：这里不传入 currentSession，由外部控制刷新）
            // 或者我们可以要求外部调用 refreshChatHistorySidebar
        }
    }

    private void onHistoryItemEditTitle(int position, ChatHistoryAdapter.ChatHistoryItem item) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (position >= 0 && position < sessions.size()) {
            ChatSessionBean sessionToEdit = sessions.get(position);
            showEditSessionTitleDialog(sessionToEdit);
        }
    }

    private void onHistoryItemSummary(int position, ChatHistoryAdapter.ChatHistoryItem item) {
        List<ChatSessionBean> sessions = ChatSessionManager.getInstance().getAllSessionsSorted();
        if (position >= 0 && position < sessions.size()) {
            ChatSessionBean session = sessions.get(position);
            if (actionListener != null) {
                actionListener.onSessionSummaryRequested(session);
            }
        }
    }

    /**
     * 显示编辑会话标题对话框
     */
    private void showEditSessionTitleDialog(ChatSessionBean session) {
        if (session == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("编辑会话标题");

        final EditText input = new EditText(context);
        input.setText(session.getTitle());
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty() && !newTitle.equals(session.getTitle())) {
                session.setTitle(newTitle);
                session.setUpdateTime(DateHelper.getSeconds1());
                ChatSessionManager.getInstance().updateSession(session);

                if (actionListener != null) {
                    actionListener.onSessionTitleEdited(session);
                }
                
                Toast.makeText(context, "标题已更新", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        input.requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }
    
    private void showClearAllSessionsDialog() {
        new AlertDialog.Builder(context)
                .setTitle("清空所有会话")
                .setMessage("确定要删除所有聊天会话吗？此操作不可恢复！")
                .setPositiveButton("确定", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onClearAllSessions();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
