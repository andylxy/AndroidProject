package run.yigou.gxzy.greendao.entity;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;



@Entity
public class ChatMessageBean implements Serializable {

    private static final long serialVersionUID = 1015L;

    @Id(autoincrement = true)
    private Long Id;
    
    /**
     * 关联的会话ID
     */
    private Long sessionId;
    
    /**
     * 发送的消息 msg
     */
    @Transient
    public static final int TYPE_SEND = 1;
    /**
     * 接收到的消息 只有答案
     */
    @Transient
    public static final int TYPE_RECEIVED = 2;
    /**
     * 系统消息
     */
    @Transient
    public static final int TYPE_SYSTEM = 3;

    /**
     * 已删除
     */
    public static final int     IS_Delete_YES= 0;
    /**
     * 未删除
     */
    public static final int     IS_Delete_NO= 1;


    /**
     * 是否已经展示了更多
     */
    @Transient
    private boolean isMore = false;
    /**
     * 是否是灰色模式
     */
    @Transient
    private boolean isGreyMode = false;// 主要用于在咨询界面下，双工时，产生的救场语以及提问的内容置为灰色

    /**
     * 昵称
     */
    private String nick = "小甲";
    /**
     * 头像
     */
    private String pic_url = "";
    /**
     * 内容
     */
    private String content = "";
    /**
     * 类型
     */
    private int type = 0;
    /**
     * 是否是简单问答
     */
    @Transient
    private boolean simpleQa = false;
    /**
     * 是否保存
     */
    @Transient
    private boolean save = false;
    /**
     * 是否是连续的
     */
    @Transient
    private boolean continuesListen = false;
    /**
     * 创建时间
     */
    @NotNull
    private String createDate;//创建时间

    /**
     *  是否删除
     */
    private  int isDelete;

    @Generated(hash = 1215263979)
    public ChatMessageBean(Long Id, Long sessionId, String nick, String pic_url,
            String content, int type, @NotNull String createDate, int isDelete) {
        this.Id = Id;
        this.sessionId = sessionId;
        this.nick = nick;
        this.pic_url = pic_url;
        this.content = content;
        this.type = type;
        this.createDate = createDate;
        this.isDelete = isDelete;
    }

    @Generated(hash = 1557449535)
    public ChatMessageBean() {
    }
    
    /**
     * 构造方法
     *
     * @param type    消息类型
     * @param nick    昵称
     * @param pic_url 头像url
     * @param content 内容
     */
    @Keep
    public ChatMessageBean(int type, String nick, String pic_url, String content) {
        this.type = type;
        this.content = content;
        this.nick = nick;
        this.pic_url = pic_url;
    }

    public Long getId() {
        return this.Id;
    }

    public void setId(Long Id) {
        this.Id = Id;
    }

    public Long getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getNick() {
        return this.nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getPic_url() {
        return this.pic_url;
    }

    public void setPic_url(String pic_url) {
        this.pic_url = pic_url;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public int getIsDelete() {
        return this.isDelete;
    }

    public void setIsDelete(int isDelete) {
        this.isDelete = isDelete;
    }


}