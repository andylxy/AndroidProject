package run.yigou.gxzy.event;

/**
 * 阅读设置（如术解开关）变更后发出的刷新信号，触发阅读页重新加载列表。
 * 零载荷标记事件：仅用于通知「设置已变」，接收方直接重读 fragmentSetting 后刷新。
 */
public class TipsSettingChangedEvent {


    public TipsSettingChangedEvent() {
    }


}
