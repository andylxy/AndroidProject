package run.yigou.gxzy.http.security;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.hjq.http.EasyLog;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.model.BodyType;
import com.hjq.http.model.HttpParams;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;

/**
 * author : Android 轮子哥
 * desc   : 安全配置类，用于管理防重放攻击相关的配置信息
 */
public class SecurityConfig {
    
    private static final String TAG = "SecurityConfig";
    
    /**
     * AccessKey ID
     */
    private static String sAccessKeyId = "3xl81vfcZMFoFWks14d1iMXzCNmOxyyX";
    
    /**
     * AccessKey 密钥
     */
    private static String sAccessKeySecret = "KZbbYBtUeMXbIimx";
    
    /**
     * 是否启用防重放攻击功能
     */
    private static boolean sEnableAntiReplayAttack = true;
    
    /**
     * SM2公钥参数
     */
    private static ECPublicKeyParameters sSm2PublicKey;
    
    /**
     * SM2私钥参数
     */
    private static ECPrivateKeyParameters sSm2PrivateKey;
    
    /**
     * 是否启用SM2国密算法
     */
    private static boolean sEnableSM2 = false;

    static {
        // 添加BouncyCastleProvider提供者
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(new BouncyCastleProvider());
        Log.i(TAG, "BouncyCastleProvider registered in SecurityConfig");
    }

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
     * 启用SM2国密算法
     */
    public static void enableSM2() {
        sEnableSM2 = true;
    }

    /**
     * 禁用SM2国密算法
     */
    public static void disableSM2() {
        sEnableSM2 = false;
    }

    /**
     * 设置SM2公钥
     * 
     * @param publicKey SM2公钥参数
     */
    public static void setSM2PublicKey(ECPublicKeyParameters publicKey) {
        sSm2PublicKey = publicKey;
    }

    /**
     * 设置SM2私钥
     * 
     * @param privateKey SM2私钥参数
     */
    public static void setSM2PrivateKey(ECPrivateKeyParameters privateKey) {
        sSm2PrivateKey = privateKey;
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
     * 是否启用了SM2国密算法
     * 
     * @return true表示已启用，false表示未启用
     */
    public static boolean isSM2Enabled() {
        return sEnableSM2;
    }
    
    /**
     * 获取SM2公钥
     * 
     * @return SM2公钥参数
     */
    public static ECPublicKeyParameters getSM2PublicKey() {
        return sSm2PublicKey;
    }
    
    /**
     * 获取SM2私钥
     * 
     * @return SM2私钥参数
     */
    public static ECPrivateKeyParameters getSM2PrivateKey() {
        return sSm2PrivateKey;
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
        // 获取当前时间戳,它是什么格式的。utc时间戳还是now？
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 为请求生成签名
     * 
     * @param api IRequestApi对象
     * @param method HTTP方法 (GET, POST, etc.)
     * @param host 请求主机
     * @param path 请求路径
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @return 签名字符串
     */
    public static String generateSignature(IRequestApi api, String method, String host, String path, 
                                         String timestamp, String nonce) {
        if (!sEnableAntiReplayAttack || sAccessKeyId.isEmpty() || sAccessKeySecret.isEmpty()) {
            return "";
        }
        
        // 构造签名字符串 (根据2025-12变更，仅包含Method/Host/Path/Timestamp/Nonce)
        String stringToSign = method + "\n" +
                host + "\n" +
                path + "\n" +
                timestamp + "\n" +
                nonce;
        
        // 使用默认的HmacSHA256签名
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