package run.yigou.gxzy.ui.tips.aimsg;

public class Message {

  public String id;

  public String role;

  public String content;
  /**
   * stop结束,其他未结束,
   */
  public String type;
  /**
   * 消息类型 heartBeat 心跳   本地发送 message 消息 normal  接收到的消息  resend  有消息需要重新发送
   */
  public String messageType;
  /**
   * 消息顺序下标
   */
  public Integer index;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }
}
