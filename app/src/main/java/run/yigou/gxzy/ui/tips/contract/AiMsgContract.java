package run.yigou.gxzy.ui.tips.contract;

import java.util.List;

import androidx.lifecycle.LifecycleOwner;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.entity.ChatSessionBean;

public interface AiMsgContract {

    interface View {
        /**
         * 显示会话列表
         */
        void showSessionList(List<ChatSessionBean> sessions);

        /**
         * 显示当前会话的消息列表
         */
        void showMessages(List<ChatMessageBean> messages);

        /**
         * 追加一条消息
         */
        void appendMessage(ChatMessageBean message);

        /**
         * 更新一条消息（内容或状态）
         */
        void updateMessage(ChatMessageBean message);

        /**
         * 移除一条消息
         */
        void removeMessage(ChatMessageBean message);

        /**
         * 清空消息列表
         */
        void clearMessages();

        /**
         * 更新标题栏
         */
        void updateTitle(String title);

        /**
         * 滚动到底部
         */
        void scrollToBottom();

        /**
         * 显示加载状态
         */
        void showLoading(boolean isShow);

        /**
         * 显示错误提示
         */
        void showError(String msg);
        
        /**
         * 更新当前会话状态（供 View 层 Helper 使用）
         */
        void updateCurrentSession(ChatSessionBean session);
        
        /**
         * 获取当前会话ID (用于 Presenter 校验)
         */
        Long getCurrentSessionId();

        /**
         * 获取生命周期所有者
         */
        LifecycleOwner getLifecycleOwner();

        // Summary Helper 状态获取
        boolean isLatestSummaryChecked();
        boolean isAllSummaryChecked();
    }

    interface Presenter {
        /**
         * 初始化数据（加载会话列表，恢复上次会话）
         */
        void start();

        /**
         * 发送消息
         */
        void sendMessage(String content);

        /**
         * 创建新会话
         */
        void createNewSession();

        /**
         * 切换会话
         */
        void switchSession(ChatSessionBean session);

        /**
         * 删除会话
         */
        void deleteSession(ChatSessionBean session);

        /**
         * 重命名会话
         */
        void renameSession(ChatSessionBean session, String newTitle);

        /**
         * 清空所有会话
         */
        void clearAllSessions();

        /**
         * 删除单条消息
         */
        void deleteMessage(ChatMessageBean message);

        /**
         * 生成总结
         */
        void generateSummary();

        /**
         * 采用总结
         */
        void adoptSummary(ChatMessageBean summary);

        /**
         * 销毁资源
         */
        void onDestroy();
    }
}
