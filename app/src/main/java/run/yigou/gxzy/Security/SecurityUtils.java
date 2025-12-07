package run.yigou.gxzy.Security;

import android.util.Log;

import run.yigou.gxzy.utils.SM2Util;
import run.yigou.gxzy.utils.SM4Util;

/**
 * 安全管理器类，用于全局初始化和管理加密功能
 * 提供统一的加密解密、签名验签接口
 */
public final class SecurityUtils {
    private static final String TAG = "SecurityUtils";
    private static SecurityUtils instance;
    private SM2Util sm2Util;

    // 方式3：使用完整公钥形式初始化SM2公钥
    // 这种方式最符合Android客户端的使用场景
    private static final String DEFAULT_PUBLIC_KEY = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" +
            "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";

    private SecurityUtils() {
        sm2Util = SM2Util.getInstance();
    }

    /**
     * 获取安全管理器单例实例
     *
     * @return SecurityUtils实例
     */
    public static SecurityUtils getInstance() {
        if (instance == null) {
            synchronized (SecurityUtils.class) {
                if (instance == null) {
                    instance = new SecurityUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化安全管理器
     * 在此处配置SM2公钥等安全参数
     */
    public static void initSecurityManager() {
        try {
            // 获取安全管理器实例
            SecurityUtils securityManager = SecurityUtils.getInstance();
            securityManager.initSM2PublicKeyWithFullFormat(DEFAULT_PUBLIC_KEY);

            Log.d(TAG, "安全管理器初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "安全管理器初始化失败", e);
        }
    }

    // SM2相关静态方法（兼容CryptoUtil使用方式）

    /**
     * 初始化SM2公钥（适用于Android客户端场景）
     *
     * @param publicKeyX 公钥X坐标
     * @param publicKeyY 公钥Y坐标
     */
    public static void initSM2PublicKey(String publicKeyX, String publicKeyY) {
        getInstance().sm2Util.initPublicKey(publicKeyX, publicKeyY);
    }

    /**
     * 初始化SM2公钥（使用组合公钥格式）
     *
     * @param publicKey 公钥字符串，格式为"x,y"
     */
    public static void initSM2PublicKeyWithCombinedFormat(String publicKey) {
        getInstance().sm2Util.initPublicKeyWithCombinedFormat(publicKey);
    }

    /**
     * 初始化SM2公钥（使用完整公钥格式，类似sm-crypto）
     *
     * @param fullPublicKey 完整公钥（十六进制字符串，以04开头）
     */
    public static void initSM2PublicKeyWithFullFormat(String fullPublicKey) {
        getInstance().sm2Util.initPublicKeyWithFullFormat(fullPublicKey);
    }

    /**
     * 初始化SM2密钥对（仅在需要完整密钥对时使用）
     *
     * @param publicKeyX  公钥X坐标
     * @param publicKeyY  公钥Y坐标
     * @param privateKeyD 私钥D值
     */
    public static void initSM2Keys(String publicKeyX, String publicKeyY, String privateKeyD) {
        getInstance().sm2Util.initKeys(publicKeyX, publicKeyY, privateKeyD);
    }

    /**
     * 初始化SM2密钥对（使用组合公钥格式）
     *
     * @param publicKey   公钥字符串，格式为"x,y"
     * @param privateKeyD 私钥D值
     */
    public static void initSM2KeysWithCombinedPublicKey(String publicKey, String privateKeyD) {
        getInstance().sm2Util.initKeysWithCombinedPublicKey(publicKey, privateKeyD);
    }

    /**
     * 初始化SM2密钥对（使用完整公钥格式，类似sm-crypto）
     *
     * @param fullPublicKey 完整公钥（十六进制字符串，以04开头）
     * @param privateKeyD   私钥D值
     */
    public static void initSM2KeysWithFullPublicKey(String fullPublicKey, String privateKeyD) {
        getInstance().sm2Util.initKeysWithFullPublicKey(fullPublicKey, privateKeyD);
    }

    /**
     * SM2加密（使用公钥加密，适用于Android客户端场景）
     *
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm2Encrypt(String msgString) {
        return getInstance().sm2Util.doEncrypt(msgString);
    }

    /**
     * SM2解密（需要私钥，Android客户端通常无法使用）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm2Decrypt(String encryptedData) {
        return getInstance().sm2Util.doDecrypt(encryptedData);
    }

    /**
     * SM2签名（需要私钥，Android客户端通常无法使用）
     *
     * @param data 待签名数据
     * @return 签名值（十六进制字符串）
     */
    public static String doSignature(String data) {
        return getInstance().sm2Util.doSignature(data);
    }

    /**
     * SM2验签（只需要公钥，适用于Android客户端场景）
     *
     * @param data      原始数据
     * @param signature 签名值（十六进制字符串）
     * @return 验签结果
     */
    public static boolean doVerifySignature(String data, String signature) {
        return getInstance().sm2Util.doVerifySignature(data, signature);
    }

    // SM4相关静态方法（兼容CryptoUtil使用方式）

    /**
     * SM4 ECB加密（使用默认密钥）
     *
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4Encrypt(String msgString) {
        return SM4Util.encryptECB(msgString);
    }

    /**
     * SM4 ECB解密（使用默认密钥）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm4Decrypt(String encryptedData) {
        return SM4Util.decryptECB(encryptedData);
    }

    /**
     * SM4 CBC加密（使用默认密钥和IV）
     *
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4CbcEncrypt(String msgString) {
        return SM4Util.encryptCBC(msgString);
    }

    /**
     * SM4 CBC解密（使用默认密钥和IV）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm4CbcDecrypt(String encryptedData) {
        return SM4Util.decryptCBC(encryptedData);
    }

    /**
     * SM4 ECB加密（自定义密钥）
     *
     * @param msgString 待加密消息
     * @param key       密钥
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4Encrypt(String msgString, String key) {
        return SM4Util.encryptECB(msgString, key);
    }

    /**
     * SM4 ECB解密（自定义密钥）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @param key           密钥
     * @return 解密结果
     */
    public static String doSm4Decrypt(String encryptedData, String key) {
        return SM4Util.decryptECB(encryptedData, key);
    }

    /**
     * SM4 CBC加密（自定义密钥和IV）
     *
     * @param msgString 待加密消息
     * @param key       密钥
     * @param iv        初始向量
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4CbcEncrypt(String msgString, String key, String iv) {
        return SM4Util.encryptCBC(msgString, key, iv);
    }

    /**
     * SM4 CBC解密（自定义密钥和IV）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @param key           密钥
     * @param iv            初始向量
     * @return 解密结果
     */
    public static String doSm4CbcDecrypt(String encryptedData, String key, String iv) {
        return SM4Util.decryptCBC(encryptedData, key, iv);
    }

    /**
     * 获取SM2工具类实例
     *
     * @return SM2Util实例
     */
    public SM2Util getSM2Util() {
        return sm2Util;
    }

    /**
     * 检查是否已设置SM2公钥
     *
     * @return true表示已设置公钥，false表示未设置
     */
    public boolean hasSM2PublicKey() {
        return sm2Util.hasPublicKey();
    }

    /**
     * 检查是否已设置SM2私钥
     *
     * @return true表示已设置私钥，false表示未设置
     */
    public boolean hasSM2PrivateKey() {
        return sm2Util.hasPrivateKey();
    }
}