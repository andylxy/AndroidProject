package run.yigou.gxzy.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.entity.ChatSessionBean;
import run.yigou.gxzy.greendao.entity.ChatSummaryBean;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.EasyLog;
import run.yigou.gxzy.Security.SecurityUtils;

/**
 * 会话管理器
 * 负责管理会话的数据库操作、状态维护和本地存储
 */
public class ChatSessionManager {
    private static final String TAG = "ChatSessionManager";
    private static final String PREFS_NAME = "AiChatPrefs";
    private static final String KEY_LAST_SESSION_ID = "last_session_id";

    private static volatile ChatSessionManager instance;
    private Context context;

    private ChatSessionManager() {
    }

    public static ChatSessionManager getInstance() {
        if (instance == null) {
            synchronized (ChatSessionManager.class) {
                if (instance == null) {
                    instance = new ChatSessionManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 获取所有会话并按时间倒序排序
     */
    public List<ChatSessionBean> getAllSessionsSorted() {
        List<ChatSessionBean> sessions = DbService.getInstance().mChatSessionBeanService.findAll();
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        
        // 按更新时间倒序排序（最新的在最前面）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sessions.sort((s1, s2) -> {
                String time1 = s1.getUpdateTime() != null ? s1.getUpdateTime() : "";
                String time2 = s2.getUpdateTime() != null ? s2.getUpdateTime() : "";
                return time2.compareTo(time1); // 倒序：time2 - time1
            });
        }
        return sessions;
    }

    /**
     * 根据ID查找会话
     */
    public ChatSessionBean getSessionById(Long sessionId) {
        if (sessionId == null) return null;
        return DbService.getInstance().mChatSessionBeanService.findById(sessionId);
    }

    /**
     * 创建新的本地会话（不含网络请求）
     */
    public ChatSessionBean createLocalSession(String title, String preview) {
        ChatSessionBean newSession = new ChatSessionBean();
        newSession.setTitle(title);
        newSession.setPreview(preview);
        String currentTime = DateHelper.getSeconds1();
        newSession.setUpdateTime(currentTime);
        newSession.setCreateTime(currentTime);
        newSession.setIsDelete(ChatSessionBean.IS_Delete_NO);
        
        // 保存到数据库
        long sessionId = DbService.getInstance().mChatSessionBeanService.addEntity(newSession);
        newSession.setId(sessionId);
        
        EasyLog.print(TAG, "Created new local session with ID: " + sessionId);
        return newSession;
    }

    /**
     * 保存新会话到数据库
     */
    public long saveSession(ChatSessionBean session) {
        if (session == null) return -1;
        return DbService.getInstance().mChatSessionBeanService.addEntity(session);
    }

    /**
     * 更新会话实体
     */
    public void updateSession(ChatSessionBean session) {
        if (session != null) {
            DbService.getInstance().mChatSessionBeanService.updateEntity(session);
        }
    }

    /**
     * 删除会话及相关数据
     */
    public void deleteSession(ChatSessionBean session) {
        if (session == null) return;
        
        // 1. 删除相关的消息
        List<ChatMessageBean> messages = getMessagesForSession(session);
        if (messages != null && !messages.isEmpty()) {
            for (ChatMessageBean msg : messages) {
                DbService.getInstance().mChatMessageBeanService.deleteEntity(msg);
            }
        }

        // 2. 删除相关的总结
        DbService.getInstance().mChatSummaryBeanService.deleteBySessionId(session.getId());
        
        // 3. 删除会话本身
        DbService.getInstance().mChatSessionBeanService.deleteEntity(session);
    }

    /**
     * 清空所有会话数据
     */
    public void clearAllSessions() {
        DbService.getInstance().mChatMessageBeanService.deleteAll();
        DbService.getInstance().mChatSummaryBeanService.deleteAll();
        DbService.getInstance().mChatSessionBeanService.deleteAll();
        
        // 清除本地存储的ID
        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY_LAST_SESSION_ID)
                    .apply();
        }
    }

    /**
     * 加载会话消息并解密
     */
    public List<ChatMessageBean> getMessagesForSession(ChatSessionBean session) {
        if (session == null) return new ArrayList<>();

        // 强制重置缓存
        session.resetMessages();
        List<ChatMessageBean> messages = session.getMessages();
        if (messages == null) return new ArrayList<>();

        // 解密消息
        for (ChatMessageBean msg : messages) {
            String content = msg.getContent();
            if (!TextUtils.isEmpty(content)) {
                String decrypted = SecurityUtils.rc4Decrypt(content);
                if (decrypted != null) {
                    msg.setContent(decrypted);
                }
            }
        }
        return messages;
    }

    /**
     * 保存最后选中的会话ID
     */
    public void saveLastSessionId(Long sessionId) {
        if (context == null || sessionId == null) return;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_SESSION_ID, sessionId).apply();
    }

    /**
     * 获取最后选中的会话ID
     */
    public Long getLastSessionId() {
        if (context == null) return null;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long sessionId = prefs.getLong(KEY_LAST_SESSION_ID, -1L);
        return sessionId > 0 ? sessionId : null;
    }
    
    /**
     * 获取指定会话的总结
     */
    public List<ChatSummaryBean> getSessionSummaries(Long sessionId) {
        if (sessionId == null) return new ArrayList<>();
        return DbService.getInstance().mChatSummaryBeanService.findBySessionId(sessionId);
    }

    /**
     * 保存消息到数据库
     */
    public long saveMessage(ChatMessageBean message) {
        return DbService.getInstance().mChatMessageBeanService.addEntity(message);
    }
    
    /**
     * 更新消息到数据库
     */
    public void updateMessage(ChatMessageBean message) {
        DbService.getInstance().mChatMessageBeanService.updateEntity(message);
    }
    
    /**
     * 删除消息
     */
    public void deleteMessage(ChatMessageBean message) {
        DbService.getInstance().mChatMessageBeanService.deleteEntity(message);
    }

    /**
     * 保存总结
     */
    public long saveSummary(ChatSummaryBean summary) {
        return DbService.getInstance().mChatSummaryBeanService.addEntity(summary);
    }

    /**
     * 更新总结
     */
    public void updateSummary(ChatSummaryBean summary) {
        DbService.getInstance().mChatSummaryBeanService.updateEntity(summary);
    }
}
