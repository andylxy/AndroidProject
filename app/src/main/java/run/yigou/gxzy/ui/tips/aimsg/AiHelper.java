package run.yigou.gxzy.ui.tips.aimsg;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * AiHelper类是一个辅助类，兼容openAi接口, 用于与Ai数据模型 API进行交互，实现聊天功能。
 */
public class AiHelper {

    // 存储聊天记录的历史消息列表
    private static ArrayList<Message> history = new ArrayList<>();

    /**
     * 清空历史记录
     */
    public static void clearHistory() {
        history.clear();
    }

    // 构造请求的JSON数据
    private static HashMap<String, Object> gptRequestJson = new HashMap<String, Object>() {{
        put("model", AiConfigHelper.getGptModel()); // 使用配置中的GPT模型
        put("stream", true);           // 启用流式传输
        put("messages", history);      // 设置历史消息
    }};

    /**
     * 调用在线Ai模型 API接口进行聊天
     *
     * @param send     用户发送的消息
     * @param callback 回调接口，用于处理返回的结果
     */
    public static void chat(String send, final CallBack callback) {
        // Ai API的URL地址
        String url = AiConfigHelper.getProxyAddress() + "/v1/chat/completions";
        // 获取API Key，用于验证请求

        String  apiKey = "Bearer " + AiConfigHelper.getApiKey();

        // 如果不使用上下文（历史消息），则清空历史记录
        if (!AiConfigHelper.getUseContext()) {
            clearHistory();
        }

        // 将用户的消息添加到历史记录中
        Message message = new Message();
        message.setRole("user");
        message.setContent(send);
        history.add(message);

        // 更新请求中的模型配置
        gptRequestJson.put("model", AiConfigHelper.getGptModel());

        // 打印调试信息
         LogUtils.d("gptRequestJson", GsonUtils.toJson(gptRequestJson));

        // 创建请求体，使用JSON格式传递参数
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), GsonUtils.toJson(gptRequestJson));

        // 构建请求对象，设置URL、请求方法（POST）、请求头和请求体
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Authorization", apiKey) // 添加Authorization头
                .build();

        OkHttpUtil.okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败时打印异常并显示错误提示
                e.printStackTrace();
                ToastUtils.showLong("网络请求出错 请检查网络");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    // 获取响应体
                    ResponseBody responseBody = response.body();
                    if (responseBody != null && response.code() == 200) {
                        // 创建一个新的消息对象来保存助手的回复
                        Message message = new Message();
                        message.setRole("assistant");
                        message.setContent("");
                        history.add(message);

                        // 读取响应体中的数据
                        BufferedReader bufferedReader = new BufferedReader(responseBody.charStream());
                        String line = bufferedReader.readLine();
                        int index = 0;  // 索引，用于区分消息的顺序
                        StringBuilder sb = new StringBuilder(); // 用于拼接流式返回的消息内容

                        while (line != null) {
                            // 将每行数据转换成消息对象
                            Message msg = convert(line, "1", index++);
                            if (msg != null) {
                                // 拼接助手的内容
                                sb.append(msg.getContent());
                                message.setContent(sb.toString());
                                callback.onCallBack(sb.toString(), false); // 通过回调返回部分消息
                            }
                            line = bufferedReader.readLine(); // 读取下一行
                        }
                        callback.onCallBack(sb.toString(), true); // 返回完整消息
                    } else {
                        // 如果接口请求失败，返回错误信息
                        callback.onCallBack("接口请求失败，请检查模型配置！\n" + (response.body() != null ? response.body().string() : ""), true);
                    }
                } catch (Exception e) {
                    // 捕获异常并打印错误
                    e.printStackTrace();
                    ToastUtils.showLong("网络请求出错 请检查配置");
                }
            }
        });


    }

    /**
     * 处理流式返回的数据
     *
     * @param answer     GPT返回的单行数据
     * @param questionId 当前问题的ID
     * @param index      当前数据的索引
     * @return 处理后的消息对象
     */
    public static Message convert(String answer, String questionId, int index) {
        Message msg = new Message();
        msg.setContent("");
        msg.setMessageType("normal");
        msg.setId(questionId);

        // 如果返回的数据不是结束标志（data: [DONE]），继续处理
        if (!"data: [DONE]".equals(answer)) {
            // 去除前缀"data: "并解析JSON数据
            String beanStr = answer.replaceFirst("data: ", "");
            try {
                // 将JSON字符串转换为StreamAiAnswer对象
                StreamAiAnswer aiAnswer = GsonUtils.fromJson(beanStr, StreamAiAnswer.class);
                if (aiAnswer == null) return null;

                // 获取choices列表（可能有多个选择）
                ArrayList<StreamAiAnswer.Choices> choices = (ArrayList<StreamAiAnswer.Choices>) aiAnswer.getChoices();
                if (choices.isEmpty()) {
                    return null;
                }

                // 用于拼接每个choice的内容
                StringBuilder stringBuffer = new StringBuilder();
                for (StreamAiAnswer.Choices choice : choices) {
                    // 如果finish_reason不是"stop"且.content不为空，拼接内容getFinish_reason
                    if (!"stop".equals(choice.getFinish_reason())) {
                        if (choice.getDelta().getContent() != null) {
                            stringBuffer.append(choice.getDelta().getContent());
                        } else {
                            return null;
                        }
                    }
                }

                // 设置消息内容
                msg.setContent(stringBuffer.toString());

                // 如果是第一行并且内容是"\n\n"，去除开头的换行
                if (index == 0 && "\n\n".equals(msg.getContent())) {
                    LogUtils.e("发现开头有两次换行,移除两次换行");
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace(); // 捕获异常
            }
        } else {
            // 如果是结束标志，设置消息类型为"stop"
            msg.setType("stop");
        }

        // 设置消息的索引值
        msg.setIndex(index);
        return msg;
    }

    /**
     * 回调接口，用于处理消息返回结果
     */
    public interface CallBack {
        /**
         * 处理消息返回结果
         *
         * @param result 返回的消息结果
         * @param isLast false返回部分消息,不是完整消息; true返回完整消息
         */
        void onCallBack(String result, boolean isLast); // 回调方法
    }
}
