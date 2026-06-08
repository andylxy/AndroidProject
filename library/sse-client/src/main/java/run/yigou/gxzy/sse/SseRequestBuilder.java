package run.yigou.gxzy.sse;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import run.yigou.gxzy.log.EasyLog;

/**
 * AI 流式请求构建器（SSE 专用）
 * 用于构建符合服务器规范的可复现签名请求体
 * 
 * 注意：签名参数由调用方（app 模块）提供，此模块不依赖具体配置源
 */
public class SseRequestBuilder {

    private static final String TAG = "SseRequestBuilder";

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String CONTENT_TYPE = "application/json";
    private static final String APP_ID = "app_id";
    private static final String TIMESTAMP = "timestamp";
    private static final String NONCE_STR = "nonce_str";
    private static final String APP_SIGN = "app_sign";

    private static final Gson gson = new Gson();

    // 签名参数
    private final String appKey;
    private final String sk;
    private final String appSecret;
    private final String appId;
    
    // 请求参数
    private final String url;
    private final JsonObject body;

    /**
     * 构造函数
     *
     * @param url       SSE 接口 URL
     * @param appKey    应用 Key
     * @param sk        安全 Key
     * @param appSecret 应用密钥（用于签名）
     * @param appId     应用 ID（系统标识）
     * @param body      自定义请求体参数
     */
    public SseRequestBuilder(String url, String appKey, String sk, String appSecret, String appId, JsonObject body) {
        this.url = url;
        this.appKey = appKey;
        this.sk = sk;
        this.appSecret = appSecret;
        this.appId = appId;
        this.body = body;
    }

    /**
     * 从 AiStreamApi 构建请求（便捷方法）
     *
     * @param host   主机地址
     * @param api    API 路径名
     * @param aiApi  AiStreamApi 实例
     * @return Request 对象
     */
    @NonNull
    public static Request buildRequest(String host, String api, Object aiApi) {
        // 调用方需要提供签名参数
        // TODO: 签名参数应从配置或安全模块获取
        String appKey = "your-app-key";
        String sk = "your-sk";
        String appSecret = "your-secret";
        String appId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        String fullUrl = host.endsWith("/") ? host + "api/AppBookRequest/" + api : 
                                  host + "/api/AppBookRequest/" + api;

        // 构建请求体
        JsonObject body = new JsonObject();
        if (aiApi != null) {
            // 通过反射获取参数（避免直接依赖 AiStreamApi）
            try {
                java.lang.reflect.Method getQuery = aiApi.getClass().getMethod("getQuery");
                java.lang.reflect.Method getConversationId = aiApi.getClass().getMethod("getConversationId");
                java.lang.reflect.Method getEndUserId = aiApi.getClass().getMethod("getEndUserId");
                
                body.addProperty("query", String.valueOf(getQuery.invoke(aiApi)));
                body.addProperty("conversationId", String.valueOf(getConversationId.invoke(aiApi)));
                body.addProperty("endUserId", String.valueOf(getEndUserId.invoke(aiApi)));
            } catch (Exception e) {
                EasyLog.print(TAG, "获取 AiStreamApi 参数失败: " + e.getMessage());
            }
        }

        SseRequestBuilder builder = new SseRequestBuilder(fullUrl, appKey, sk, appSecret, appId, body);
        return builder.build();
    }

    /**
     * 构建 OkHttp Request 对象
     *
     * @return Request 对象
     */
    @NonNull
    public Request build() {
        EasyLog.print(TAG, "========== 构建 SSE 请求 ==========");
        EasyLog.print(TAG, "AK: " + appKey);
        EasyLog.print(TAG, "SK: " + sk);
        EasyLog.print(TAG, "URL: " + url);

        // 1. 生成签名参数
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonceStr = UUID.randomUUID().toString().replace("-", "");

        // 2. 组装请求体 JSON
        JsonObject requestBodyJson = body != null ? body : new JsonObject();
        requestBodyJson.addProperty(APP_ID, appId);
        requestBodyJson.addProperty(TIMESTAMP, timestamp);
        requestBodyJson.addProperty(NONCE_STR, nonceStr);

        String requestBodyStr = gson.toJson(requestBodyJson);

        // 3. 计算 app_sign: HMAC-SHA256(requestBody, appSecret)
        String appSign = signRequestBody(requestBodyStr, appSecret);

        EasyLog.print(TAG, "body: " + requestBodyStr);
        EasyLog.print(TAG, "sign: " + appSign);

        // 4. 构建 OkHttp Request
        return new Request.Builder()
                .url(url)
                .header("Content-Type", CONTENT_TYPE)
                .header("Accept", "text/event-stream")
                .header("app_key", appKey)
                .header("app_sign", appSign)
                .header("sk", sk)
                .post(RequestBody.create(requestBodyStr.getBytes(StandardCharsets.UTF_8), MediaType.parse(CONTENT_TYPE)))
                .build();
    }

    /**
     * 计算请求体的 HMAC-SHA256 签名
     *
     * @param body   请求体 JSON 字符串
     * @param secret 应用密钥
     * @return 签名字符串
     */
    private static String signRequestBody(String body, String secret) {
        try {
            Mac hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            hmac.init(keySpec);
            byte[] hash = hmac.doFinal(body.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexStr = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexStr.append('0');
                }
                hexStr.append(hex);
            }
            return hexStr.toString();
        } catch (Exception e) {
            EasyLog.print(TAG, "计算签名失败: " + e.getMessage());
            return "";
        }
    }
}
