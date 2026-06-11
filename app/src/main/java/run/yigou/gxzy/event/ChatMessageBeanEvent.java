package run.yigou.gxzy.event;

public class ChatMessageBeanEvent {

    private  boolean isClear =false;
    private boolean assistantName=false;

    public boolean isAssistantName() {
        return assistantName;
    }

    /**
     *   ??????????????
     * @param assistantName false ?????true ?????
     */
    public ChatMessageBeanEvent setAssistantName(boolean assistantName) {
        this.assistantName = assistantName;
        return this;
    }

    public boolean isClear() {
        return isClear;
    }

    /**
     * ????????????
     * @param clear false:?????true:???
     * @return ChatMessageBeanEvent???
     */
    public ChatMessageBeanEvent setClear(boolean clear) {
        isClear = clear;
        return this;
    }
}
