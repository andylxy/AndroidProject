package run.yigou.gxzy.EventBus;

public class ChatMessageBeanEvent {

    private  boolean isClear =false;
    private boolean assistantName=false;

    public boolean isAssistantName() {
        return assistantName;
    }

    /**
     *   通知小助手名称改变
     * @param assistantName false 未变更,true 已变更
     */
    public ChatMessageBeanEvent setAssistantName(boolean assistantName) {
        this.assistantName = assistantName;
        return this;
    }

    public boolean isClear() {
        return isClear;
    }

    /**
     * 设置是否清空消息
     * @param clear false:不清空 true:清空
     * @return ChatMessageBeanEvent实体
     */
    public ChatMessageBeanEvent setClear(boolean clear) {
        isClear = clear;
        return this;
    }
}
