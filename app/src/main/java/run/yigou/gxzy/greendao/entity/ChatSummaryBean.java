package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

/**
 * 会话总结实体类
 * 用于存储 AI 聊天会话的总结内容
 */
@Entity
public class ChatSummaryBean {
    
    @Id(autoincrement = true)
    private Long id;
    
    /**
     * 关联的会话ID
     */
    @NotNull
    private Long sessionId;
    
    /**
     * 总结标题（自动生成或手动命名）
     */
    private String title;
    
    /**
     * 总结内容
     */
    @NotNull
    private String content;
    
    /**
     * 创建时间
     */
    @NotNull
    private String createTime;
    
    /**
     * 是否已删除
     */
    private int isDelete = 1; // 1未删除，0已删除
    
    /**
     * 已删除
     */
    public static final int IS_Delete_YES = 0;
    /**
     * 未删除
     */
    public static final int IS_Delete_NO = 1;
    @Generated(hash = 812550334)
    public ChatSummaryBean(Long id, @NotNull Long sessionId, String title,
            @NotNull String content, @NotNull String createTime, int isDelete) {
        this.id = id;
        this.sessionId = sessionId;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
        this.isDelete = isDelete;
    }
    @Generated(hash = 699931905)
    public ChatSummaryBean() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getSessionId() {
        return this.sessionId;
    }
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return this.content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getCreateTime() {
        return this.createTime;
    }
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    public int getIsDelete() {
        return this.isDelete;
    }
    public void setIsDelete(int isDelete) {
        this.isDelete = isDelete;
    }
}
