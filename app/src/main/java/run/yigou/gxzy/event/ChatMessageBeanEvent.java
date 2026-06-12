package run.yigou.gxzy.event;

public class ChatMessageBeanEvent {

    private  boolean isClear =false;
    private boolean assistantName=false;

    public boolean isAssistantName() {
        return assistantName;
    }

    /**
     * 设置是否显示 AI 助手名称
     * @param assistantName false 不显示助手名称, true 显示助手名称
     */
    public ChatMessageBeanEvent setAssistantName(boolean assistantName) {
        this.assistantName = assistantName;
        return this;
    }

    public boolean isClear() {
        return isClear;
    }

    /**
     * 设置是否清除聊天消息
     * @param clear false: 不清除, true: 清除
     * @return ChatMessageBeanEvent 当前实例
     */
    public ChatMessageBeanEvent setClear(boolean clear) {
        isClear = clear;
        return this;
    }
}
