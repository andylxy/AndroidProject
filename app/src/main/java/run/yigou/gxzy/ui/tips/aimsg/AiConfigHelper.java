package run.yigou.gxzy.ui.tips.aimsg;


import com.blankj.utilcode.util.SPUtils;

import run.yigou.gxzy.utils.DateHelper;

/**
 *  AI配置助手
 */
public class AiConfigHelper {

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
        return SPUtils.getInstance().getString("assistantName", "AI小助手");
    }

    public static void setAssistantName(String value) {
        SPUtils.getInstance().put("assistantName", value);
    }


    /**
     *  获取AI提供商
     * @return
     */
    public static String getProvideAi() {
        return SPUtils.getInstance().getString("ProvideAi", "");
    }

    /**
     *  配置AI提供商
     * @param value
     */
    public static void setProvideAi(String value) {
        SPUtils.getInstance().put("ProvideAi",value);
    }


    /**
     *  获取定时任务时间
     * @return 间隔时间
     */
    public static String getConfigCronJob() {
        return SPUtils.getInstance().getString("configCronJob", DateHelper.getYearMonthDay1());
    }

    /**
     *  配置定时任务
     * @param value 间隔时间
     */
    public static void setConfigCronJob(String value) {
         SPUtils.getInstance().put("configCronJob",value);
    }
    /**
     * 开启上下文
     */
    public static boolean getUseContext() {
        return SPUtils.getInstance().getBoolean("useContext", false);
    }

    /**
     * 设置开启上下文
     * @param value 开启上下文
     */
    public static void setUseContext(boolean value) {
        SPUtils.getInstance().put("useContext", value);
    }

    /**
     * 模型代理地址
     */
    public static String getProxyAddress() {
        return SPUtils.getInstance().getString("proxyAddress", "");
    }

    public static void setProxyAddress(String value) {
        SPUtils.getInstance().put("proxyAddress", value);
    }

    /**
     * GPT模型
     */
    public static String getGptModel() {
        return SPUtils.getInstance().getString("gptModel", "");
    }

    public static void setGptModel(String value) {
        SPUtils.getInstance().put("gptModel", value);
    }
}
