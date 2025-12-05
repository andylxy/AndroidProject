package run.yigou.gxzy.http.security;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.hjq.http.EasyLog;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.model.BodyType;
import com.hjq.http.model.HttpParams;

/**
 * author : Android 轮子哥
 * desc   : 安全配置类，用于管理防重放攻击相关的配置信息
 */
public class SecurityConfig {
    
    /**
     * AccessKey ID
     */
    private static String sAccessKeyId = "12345";
    
    /**
     * AccessKey 密钥
     */
    private static String sAccessKeySecret = "1234";
    
    /**
     * 是否启用防重放攻击功能
     */
    private static boolean sEnableAntiReplayAttack = true;

    /**
     * 设置 AccessKey ID
     * 
     * @param accessKeyId AccessKey ID
     */
    public static void setAccessKeyId(String accessKeyId) {
        sAccessKeyId = accessKeyId;
    }

    /**
     * 设置 AccessKey 密钥
     * 
     * @param accessKeySecret AccessKey 密钥
     */
    public static void setAccessKeySecret(String accessKeySecret) {
        sAccessKeySecret = accessKeySecret;
    }

    /**
     * 启用防重放攻击功能
     */
    public static void enableAntiReplayAttack() {
        sEnableAntiReplayAttack = true;
    }

    /**
     * 禁用防重放攻击功能
     */
    public static void disableAntiReplayAttack() {
        sEnableAntiReplayAttack = false;
    }

    /**
     * 获取 AccessKey ID
     * 
     * @return AccessKey ID
     */
    public static String getAccessKeyId() {
        return sAccessKeyId;
    }

    /**
     * 获取 AccessKey 密钥
     * 
     * @return AccessKey 密钥
     */
    public static String getAccessKeySecret() {
        return sAccessKeySecret;
    }

    /**
     * 是否启用了防重放攻击功能
     * 
     * @return true表示已启用，false表示未启用
     */
    public static boolean isAntiReplayAttackEnabled() {
        return sEnableAntiReplayAttack;
    }
    
    /**
     * 生成 Nonce 参数（防止重放攻击）
     *
     * @return 随机字符串
     */
    public static String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }
    
    /**
     * 获取当前时间戳
     * 
     * @return 当前时间戳（毫秒）
     */
    public static String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
    
    /**
     * 为请求生成签名
     * 
     * @param api IRequestApi对象
     * @param method HTTP方法 (GET, POST, etc.)
     * @param host 请求主机
     * @param path 请求路径
     * @param queryString 查询字符串
     * @param bodyString 请求体内容
     * @return 签名字符串
     */
    public static String generateSignature(IRequestApi api, String method, String host, String path, 
                                         String queryString, String bodyString) {
        if (!sEnableAntiReplayAttack || sAccessKeyId.isEmpty() || sAccessKeySecret.isEmpty()) {
            return "";
        }
        
        // 生成时间戳和Nonce
        String timestamp = getCurrentTimestamp();
        String nonce = generateNonce();
        
        // 构造签名字符串
        String stringToSign = method + "\n" +
                host + "\n" +
                path + "\n" +
                queryString + "\n" +
                bodyString + "\n" +
                timestamp + "\n" +
                nonce;
        
        // 生成签名
        String signature = hmacSha256(stringToSign, sAccessKeySecret);
        
        EasyLog.print("签名字符串：\n" + stringToSign);
        EasyLog.print("签名结果：" + signature);
        
        return signature;
    }
    
    /**
     * 使用 HmacSHA256 算法对字符串进行签名
     *
     * @param content    待签名的字符串
     * @param secretKey  密钥
     * @return           签名结果
     */
    private static String hmacSha256(String content, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] result = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(result, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            EasyLog.print("签名过程出现异常：" + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * 构建查询字符串
     * 
     * @param api IRequestApi对象
     * @param params 请求参数
     * @return 查询字符串
     */
    public static String buildQueryString(IRequestApi api, HttpParams params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        StringBuilder queryString = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : params.getParams().entrySet()) {
            if (!first) {
                queryString.append("&");
            }
            queryString.append(entry.getKey())
                      .append("=")
                      .append(entry.getValue() != null ? entry.getValue().toString() : "");
            first = false;
        }
        
        return queryString.toString();
    }
    
    /**
     * 构建请求体字符串
     * 
     * @param api IRequestApi对象
     * @param params 请求参数
     * @param bodyType 请求体类型
     * @return 请求体字符串
     */
    public static String buildBodyString(IRequestApi api, HttpParams params, BodyType bodyType) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        if (bodyType == BodyType.JSON) {
            // 构建JSON请求体
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : params.getParams().entrySet()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append("\"")
                          .append(entry.getKey())
                          .append("\":\"");
                if (entry.getValue() != null) {
                    jsonBuilder.append(entry.getValue().toString());
                }
                jsonBuilder.append("\"");
                first = false;
            }
            jsonBuilder.append("}");
            return jsonBuilder.toString();
        } else if (bodyType == BodyType.FORM) {
            // 构建表单请求体
            StringBuilder formBuilder = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Object> entry : params.getParams().entrySet()) {
                if (!first) {
                    formBuilder.append("&");
                }
                formBuilder.append(entry.getKey())
                          .append("=")
                          .append(entry.getValue() != null ? entry.getValue().toString() : "");
                first = false;
            }
            return formBuilder.toString();
        }
        
        return "";
    }
}