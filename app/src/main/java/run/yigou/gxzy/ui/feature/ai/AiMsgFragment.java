package run.yigou.gxzy.ui.feature.ai;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.bar.OnTitleBarListener;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import run.yigou.gxzy.eventbus.ChatMessageBeanEvent;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.entity.ChatSessionBean;
import run.yigou.gxzy.ui.home.HomeActivity;
import run.yigou.gxzy.ui.feature.ai.helper.ChatInputHelper;
import run.yigou.gxzy.ui.feature.ai.helper.ChatSidebarHelper;
import run.yigou.gxzy.ui.feature.ai.helper.ChatSummaryHelper;
import run.yigou.gxzy.ui.feature.ai.adapter.TipsAiChatAdapter;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.utils.MarkdownUtils;
import run.yigou.gxzy.utils.ThreadUtil;

public final class AiMsgFragment extends TitleBarFragment<HomeActivity> 
        implements OnTitleBarListener, AiMsgContract.View {

    private static final String TAG = "AiMsgFragment";
    
    private RecyclerView rv_chat;
    
    // Helpers
    private ChatSidebarHelper sidebarHelper;
    private ChatSummaryHelper summaryHelper;
    private ChatInputHelper inputHelper;
    
    private AiMsgContract.Presenter mPresenter;
    private TipsAiChatAdapter mChatAdapter;
    private Markwon mMarkwon;
    private ChatSessionBean currentSession;
    
    // UI 线程 Handler
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public static AiMsgFragment newInstance() {
        return new AiMsgFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tips_ai_msg_activity_chat;
    }

    @Override
    protected void initView() {
        EasyLog.print(TAG, "initView: Starting initialization (MVP Version)");
        
        initMarkwon();
        initTitleBar();
        initChatRecyclerView();
        initHelpers();

        // 注册事件
        XEventBus.getDefault().register(this);
    }
    
    @Override
    protected void initData() {
        mPresenter = new AiMsgPresenter(this);
        mPresenter.start();
    }

    private void initHelpers() {
        View rootView = rv_chat.getRootView();

        // Sidebar Helper
        sidebarHelper = new ChatSidebarHelper(getContext(), rootView, new ChatSidebarHelper.OnSidebarActionListener() {
            @Override
            public void onSessionSelected(ChatSessionBean session) {
                mPresenter.switchSession(session);
            }

            @Override
            public void onSessionDeleted(ChatSessionBean session) {
                mPresenter.deleteSession(session);
            }

            @Override
            public void onSessionTitleEdited(ChatSessionBean session) {
                // 原逻辑是在 Helper 里弹窗，确定后回调这里
                // 这里我们假设 Helper 传递回了 session 对象（已修改标题）或者在这里弹窗？
                // 查看 ChatSidebarHelper 源码（未提供），通常回调意味着“用户完成了编辑”
                // 我们假设 session 对象已经包含了新标题，或者我们需要弹窗让用户输入。
                // 根据原代码逻辑：sidebarHelper.refreshChatHistorySidebar(currentSession);
                // 应该是 Helper 内部处理了编辑 UI，回调通知 Fragment 更新数据。
                // 我们这里调用 Presenter 更新数据库。
                mPresenter.renameSession(session, session.getTitle());
            }

            @Override
            public void onSessionSummaryRequested(ChatSessionBean session) {
                if (summaryHelper != null) {
                    summaryHelper.showSummaryListDialog(session);
                }
            }

            @Override
            public void onClearAllSessions() {
                mPresenter.clearAllSessions();
            }
        });
        
        // Summary Helper
        summaryHelper = new ChatSummaryHelper(getContext(), rootView, mMarkwon, new ChatSummaryHelper.OnSummaryActionListener() {
            @Override
            public ChatSessionBean getCurrentSession() {
                return currentSession;
            }

            @Override
            public void onSummaryGenerated(ChatMessageBean summaryMessage) {
                // 用户点击生成总结
                mPresenter.generateSummary();
            }

            // 以下回调在 MVP 中不再需要，因为 Presenter 会直接调用 View 的 updateMessage
            @Override public void onSummaryStreamUpdate(ChatMessageBean summaryMessage) {}
            @Override public void onSummaryStreamComplete(ChatMessageBean summaryMessage, boolean success) {}
            @Override public void onSummaryStreamError(ChatMessageBean summaryMessage, String error) {}
        });
        
        // Input Helper
        inputHelper = new ChatInputHelper(getActivity(), rootView, message -> mPresenter.sendMessage(message));
    }

    private void initMarkwon() {
        mMarkwon = Markwon.builder(getContext())
                .usePlugin(CorePlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(getContext()))
                .usePlugin(TaskListPlugin.create(getContext()))
                .usePlugin(ImagesPlugin.create())
                .build();
    }

    private void initTitleBar() {
        getStatusBarConfig().setTitleBar(this, findViewById(R.id.tv_title));
        getStatusBarConfig().setTitleBar(this, findViewById(R.id.side_panel));
        if (getTitleBar() != null) {
            getTitleBar().setOnTitleBarListener(this);
        }
    }

    private void initChatRecyclerView() {
        rv_chat = findViewById(R.id.rv_chat);
        Activity activity = getActivity();
        if (activity == null) return;
        
        mChatAdapter = new TipsAiChatAdapter(activity);
        mChatAdapter.setHasStableIds(true);
        rv_chat.setItemAnimator(null);
        rv_chat.setNestedScrollingEnabled(false);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rv_chat.setLayoutManager(layoutManager);
        rv_chat.setAdapter(mChatAdapter);
        rv_chat.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        
        TipsAiChatAdapter.setScrollCallback(new TipsAiChatAdapter.OnTypewriterRenderCallback() {
            @Override
            public void onRender() {
                onTypewriterScroll();
            }

            @Override
            public void onRenderComplete() {
                scrollToBottom();
            }
        });
        
        mChatAdapter.setOnMessageActionListener(new TipsAiChatAdapter.OnMessageActionListener() {
            @Override
            public void onMessageClick(View view, ChatMessageBean message, float x, float y) {
                showMessageActionMenu(view, message, x, y);
            }

            @Override
            public void onAdoptSummary(ChatMessageBean summaryMessage) {
                mPresenter.adoptSummary(summaryMessage);
            }
        });
    }

    // ================= MVP View Implementation =================

    @Override
    public void showSessionList(List<ChatSessionBean> sessions) {
        if (sidebarHelper != null) {
            // 刷新侧边栏并高亮当前会话
            sidebarHelper.refreshChatHistorySidebar(currentSession); 
        }
    }
    
    // 为了支持 Sidebar 高亮，我们需要让 Presenter 告诉 View 当前 Session
    @Override
    public void updateCurrentSession(ChatSessionBean session) {
        this.currentSession = session;
        if (sidebarHelper != null) {
            sidebarHelper.refreshChatHistorySidebar(session);
        }
    }

    @Override
    public void showMessages(List<ChatMessageBean> messages) {
        if (mChatAdapter != null) {
            mChatAdapter.setData(new ArrayList<>(messages));
            scrollToBottom();
        }
    }

    @Override
    public void appendMessage(ChatMessageBean message) {
        if (mChatAdapter != null) {
            mChatAdapter.addItem(message);
            scrollToBottom();
        }
    }

    @Override
    public void updateMessage(ChatMessageBean message) {
        if (mChatAdapter != null) {
            int index = mChatAdapter.getData().indexOf(message);
            if (index != -1) {
                mChatAdapter.notifyItemChanged(index, TipsAiChatAdapter.PAYLOAD_UPDATE_CONTENT);
            }
        }
    }

    @Override
    public void removeMessage(ChatMessageBean message) {
        if (mChatAdapter != null) {
            mChatAdapter.removeItem(message);
        }
    }

    @Override
    public void clearMessages() {
        if (mChatAdapter != null) {
            mChatAdapter.clearData();
        }
    }

    @Override
    public void updateTitle(String title) {
        if (getTitleBar() != null) {
            getTitleBar().setTitle(title);
        }
    }

    @Override
    public void scrollToBottom() {
        if (rv_chat == null || mChatAdapter == null) return;
        rv_chat.post(() -> {
            int count = mChatAdapter.getItemCount();
            if (count > 0) {
                rv_chat.smoothScrollToPosition(count - 1);
                rv_chat.postDelayed(() -> rv_chat.smoothScrollBy(0, 10000), 100);
            }
        });
    }

    private void onTypewriterScroll() {
        if (rv_chat == null) return;
        rv_chat.post(() -> rv_chat.smoothScrollBy(0, 200));
    }

    @Override
    public void showLoading(boolean isShow) {
        if (isShow) {
            // showDialog(); // 如果有加载框
            Toast.makeText(getContext(), "处理中...", Toast.LENGTH_SHORT).show();
        } else {
            // hideDialog();
        }
    }

    @Override
    public void showError(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public Long getCurrentSessionId() {
        // 暂时不需要，Presenter 自己维护
        return null;
    }

    @Override
    public LifecycleOwner getLifecycleOwner() {
        return this;
    }

    @Override
    public boolean isLatestSummaryChecked() {
        return summaryHelper != null && summaryHelper.isLatestSummaryChecked();
    }

    @Override
    public boolean isAllSummaryChecked() {
        return summaryHelper != null && summaryHelper.isAllSummaryChecked();
    }

    // ================= TitleBar Listener =================

    @Override
    public void onLeftClick(View view) {
        if (sidebarHelper != null) {
            sidebarHelper.openDrawer();
        }
    }

    @Override
    public void onRightClick(View view) {
        mPresenter.createNewSession();
    }
    
    @Override
    public void onTitleClick(View view) {}

    // ================= Other UI Logic =================

    private void showMessageActionMenu(View view, ChatMessageBean message, float x, float y) {
        // ... (保持原有的 PopupWindow 逻辑，但点击事件调用 Presenter)
        // 为了节省篇幅，这里简化，实际应该保留原有的完整代码
        // ...
        // 这里需要把原代码的 showMessageActionMenu 复制过来，
        // 并将 resendMessage -> mPresenter.sendMessage
        // deleteMessage -> mPresenter.deleteMessage
        // copyToClipboard -> local method
        // adoptSummary -> mPresenter.adoptSummary
        
        if (message == null || view == null || getContext() == null) return;
        
        String[] items;
        switch (message.getType()) {
            case ChatMessageBean.TYPE_SEND: items = new String[]{"重发", "删除", "复制"}; break;
            case ChatMessageBean.TYPE_RECEIVED:
            case ChatMessageBean.TYPE_THINKING: items = new String[]{"删除", "复制"}; break;
            case ChatMessageBean.TYPE_SUMMARY: items = new String[]{"复制", "删除", "采用"}; break;
            default: return;
        }

        // ... (创建 View 和 Popup 逻辑同原代码) ...
        android.widget.LinearLayout menuLayout = new android.widget.LinearLayout(getContext());
        menuLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(android.graphics.Color.WHITE);
        background.setCornerRadius(8);
        background.setStroke(1, android.graphics.Color.LTGRAY);
        menuLayout.setBackground(background);
        menuLayout.setPadding(4, 4, 4, 4);
        
        final String[] menuItems = items;
        for (int i = 0; i < items.length; i++) {
            android.widget.TextView menuItem = new android.widget.TextView(getContext());
            menuItem.setText(items[i]);
            menuItem.setPadding(24, 16, 24, 16);
            menuItem.setTextSize(14);
            menuItem.setTextColor(android.graphics.Color.BLACK);
            menuItem.setBackgroundResource(android.R.drawable.list_selector_background);
            menuLayout.addView(menuItem);
        }
        
        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
            menuLayout,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(4);
        
        for (int i = 0; i < menuLayout.getChildCount(); i++) {
            final int index = i;
            menuLayout.getChildAt(i).setOnClickListener(v -> {
                popupWindow.dismiss();
                String clickedItem = menuItems[index];
                
                if ("重发".equals(clickedItem)) {
                    mPresenter.sendMessage(message.getContent());
                } else if ("删除".equals(clickedItem)) {
                    mPresenter.deleteMessage(message);
                } else if ("复制".equals(clickedItem)) {
                    copyToClipboard(message.getContent());
                } else if ("采用".equals(clickedItem)) {
                    mPresenter.adoptSummary(message);
                }
            });
        }
        
        View decorView = getActivity().getWindow().getDecorView();
        int[] location = new int[2];
        decorView.getLocationOnScreen(location);
        int popupX = (int) x - location[0];
        int popupY = (int) y - location[1];
        popupWindow.showAtLocation(decorView, android.view.Gravity.NO_GRAVITY, popupX, popupY);
    }

    private void copyToClipboard(String content) {
        String plainText = MarkdownUtils.convertMarkdownToPlainText(content);
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("聊天内容", plainText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    @Subscribe(priority = 1)
    public void ChatMessageEvent(ChatMessageBeanEvent event) {
        ThreadUtil.runOnUiThread(() -> {
            if (event.isClear()) {
                mPresenter.start(); // 重新加载
            }
        });
    }

    @Override
    public void onDestroy() {
        if (mPresenter != null) {
            mPresenter.onDestroy();
        }
        XEventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
