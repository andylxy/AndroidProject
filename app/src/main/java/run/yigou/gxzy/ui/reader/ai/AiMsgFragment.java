package run.yigou.gxzy.ui.reader.ai;

import android.app.Activity;
import android.content.Context;
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
import run.yigou.gxzy.event.ChatMessageBeanEvent;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.data.local.entity.ChatMessageBean;
import run.yigou.gxzy.data.local.entity.ChatSessionBean;
import run.yigou.gxzy.ui.reader.ai.contract.AiMsgContract;
import run.yigou.gxzy.ui.reader.ai.presenter.AiMsgPresenter;
import run.yigou.gxzy.ui.main.HomeActivity;
import run.yigou.gxzy.ui.reader.ai.helper.ChatInputHelper;
import run.yigou.gxzy.ui.reader.ai.helper.ChatMessageMenuHelper;
import run.yigou.gxzy.ui.reader.ai.helper.ChatSidebarHelper;
import run.yigou.gxzy.ui.reader.ai.helper.ChatSummaryHelper;
import run.yigou.gxzy.ui.reader.ai.adapter.TipsAiChatAdapter;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.utils.MarkdownUtils;
import run.yigou.gxzy.utils.ThreadUtil;

public final class AiMsgFragment extends TitleBarFragment<HomeActivity> 
        implements OnTitleBarListener, AiMsgContract.View {

    private static final String TAG = "AiMsgFragment";
    
    // 滚动相关常量
    private static final int SCROLL_EXTRA_DISTANCE = 10000;
    private static final int SCROLL_DELAY_MS = 100;
    private static final int TYPEWRITER_SCROLL_DISTANCE = 200;
    
    // Toast 文本常量
    private static final String TOAST_LOADING = "处理中...";
    
    private RecyclerView rv_chat;
    
    // Helpers
    private ChatSidebarHelper sidebarHelper;
    private ChatSummaryHelper summaryHelper;
    private ChatInputHelper inputHelper;
    private ChatMessageMenuHelper menuHelper;
    
    private AiMsgContract.Presenter mPresenter;
    private TipsAiChatAdapter mChatAdapter;
    private Markwon mMarkwon;
    private ChatSessionBean currentSession;

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
        
        // 获取共享 Markwon 实例
        Context context = getContext();
        if (context == null) return;
        mMarkwon = MarkdownUtils.getMarkwon(context);

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
                // Helper 内部处理编辑 UI，回调时 session 已包含新标题
                // 调用 Presenter 更新数据库
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

            @Override public void onSummaryStreamUpdate(ChatMessageBean summaryMessage) {}
            @Override public void onSummaryStreamComplete(ChatMessageBean summaryMessage, boolean success) {}
            @Override public void onSummaryStreamError(ChatMessageBean summaryMessage, String error) {}
        });
        
        // Input Helper
        inputHelper = new ChatInputHelper(getActivity(), rootView, message -> mPresenter.sendMessage(message));
        
        // Message Menu Helper
        menuHelper = new ChatMessageMenuHelper(getActivity(), new ChatMessageMenuHelper.OnMessageMenuActionListener() {
            @Override
            public void onResendMessage(String content) {
                mPresenter.sendMessage(content);
            }

            @Override
            public void onDeleteMessage(ChatMessageBean message) {
                mPresenter.deleteMessage(message);
            }

            @Override
            public void onAdoptSummary(ChatMessageBean message) {
                mPresenter.adoptSummary(message);
            }
        });
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
        
        rv_chat.setLayoutManager(new LinearLayoutManager(getContext()));
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
                if (menuHelper != null) {
                    menuHelper.showMenu(view, message, x, y);
                }
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
                rv_chat.postDelayed(() -> rv_chat.smoothScrollBy(0, SCROLL_EXTRA_DISTANCE), SCROLL_DELAY_MS);
            }
        });
    }

    private void onTypewriterScroll() {
        if (rv_chat == null) return;
        rv_chat.post(() -> rv_chat.smoothScrollBy(0, TYPEWRITER_SCROLL_DISTANCE));
    }

    @Override
    public void showLoading(boolean isShow) {
        if (!isAdded() || getContext() == null) return;
        
        if (isShow) {
            Toast.makeText(getContext(), TOAST_LOADING, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showError(String msg) {
        if (!isAdded() || getContext() == null) return;
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public Long getCurrentSessionId() {
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

    // ================= Lifecycle =================

    @Subscribe(priority = 1)
    public void onChatMessageEvent(ChatMessageBeanEvent event) {
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
            mPresenter = null;
        }
        
        // 清理 Adapter 数据
        if (mChatAdapter != null) {
            mChatAdapter.clearData();
        }
        
        // 清理 Helper 引用，防止内存泄漏
        sidebarHelper = null;
        summaryHelper = null;
        inputHelper = null;
        menuHelper = null;
        
        // 清理 Markwon 引用
        mMarkwon = null;
        
        XEventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
