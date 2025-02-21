package run.yigou.gxzy.ui.tips.aimsg;


import com.blankj.utilcode.util.SPUtils;

public class AiConfig {

    /**
     * input your ChatGPT ApiKey
     */
    public static String getApiKey() {
        return SPUtils.getInstance().getString("apiKey", "");
    }

    public static void setApiKey(String value) {
        SPUtils.getInstance().put("apiKey", value);
    }

    /**
     * 小助手昵称
     */
    public static String getAssistantName() {
        return SPUtils.getInstance().getString("assistantName", "AI智能对话");
    }

    public static void setAssistantName(String value) {
        SPUtils.getInstance().put("assistantName", value);
    }

    /**
     * 开启上下文
     */
    public static boolean getUseContext() {
        return SPUtils.getInstance().getBoolean("useContext", true);
    }

    public static void setUseContext(boolean value) {
        SPUtils.getInstance().put("useContext", value);
    }

    /**
     * 模型代理地址
     */
    public static String getProxyAddress() {
        return SPUtils.getInstance().getString("proxyAddress", "https://api.siliconflow.cn");
    }

    public static void setProxyAddress(String value) {
        SPUtils.getInstance().put("proxyAddress", value);
    }

    /**
     * GPT模型
     */
    public static String getGptModel() {
        return SPUtils.getInstance().getString("gptModel", "gpt-4o-mini");
    }

    public static void setGptModel(String value) {
        SPUtils.getInstance().put("gptModel", value);
    }
}
