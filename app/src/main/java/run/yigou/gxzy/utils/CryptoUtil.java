package run.yigou.gxzy.utils;

/**
 * 统一的国密工具类，整合SM2和SM4功能
 * 类似于前端sm-crypto库的使用方式
 * 封装了SM2和SM4常用操作，简化了API调用
 * 
 * 注意：在Android客户端场景下，通常只有公钥，主要用于加密数据发送给服务端
 */
public class CryptoUtil {
    
    // SM2相关方法
    
    /**
     * 初始化SM2公钥（适用于Android客户端场景）
     * @param publicKeyX 公钥X坐标
     * @param publicKeyY 公钥Y坐标
     */
    public static void initSM2PublicKey(String publicKeyX, String publicKeyY) {
        SM2Util.getInstance().initPublicKey(publicKeyX, publicKeyY);
    }
    
    /**
     * 初始化SM2公钥（使用组合公钥格式）
     * @param publicKey 公钥字符串，格式为"x,y"
     */
    public static void initSM2PublicKey(String publicKey) {
        SM2Util.getInstance().initPublicKeyWithCombinedFormat(publicKey);
    }
    
    /**
     * 初始化SM2公钥（使用完整公钥格式，类似sm-crypto）
     * @param fullPublicKey 完整公钥（十六进制字符串，以04开头）
     */
    public static void initSM2PublicKeyWithFullFormat(String fullPublicKey) {
        SM2Util.getInstance().initPublicKeyWithFullFormat(fullPublicKey);
    }
    
    /**
     * 初始化SM2密钥对（仅在需要完整密钥对时使用）
     * @param publicKeyX 公钥X坐标
     * @param publicKeyY 公钥Y坐标
     * @param privateKeyD 私钥D值
     */
    public static void initSM2Keys(String publicKeyX, String publicKeyY, String privateKeyD) {
        SM2Util.getInstance().initKeys(publicKeyX, publicKeyY, privateKeyD);
    }
    
    /**
     * 初始化SM2密钥对（使用组合公钥格式）
     * @param publicKey 公钥字符串，格式为"x,y"
     * @param privateKeyD 私钥D值
     */
    public static void initSM2Keys(String publicKey, String privateKeyD) {
        SM2Util.getInstance().initKeysWithCombinedPublicKey(publicKey, privateKeyD);
    }
    
    /**
     * 初始化SM2密钥对（使用完整公钥格式，类似sm-crypto）
     * @param fullPublicKey 完整公钥（十六进制字符串，以04开头）
     * @param privateKeyD 私钥D值
     */
    public static void initSM2KeysWithFullPublicKey(String fullPublicKey, String privateKeyD) {
        SM2Util.getInstance().initKeysWithFullPublicKey(fullPublicKey, privateKeyD);
    }
    
    /**
     * SM2加密（使用公钥加密，适用于Android客户端场景）
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm2Encrypt(String msgString) {
        return SM2Util.getInstance().doEncrypt(msgString);
    }
    
    /**
     * SM2解密（需要私钥，Android客户端通常无法使用）
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm2Decrypt(String encryptedData) {
        return SM2Util.getInstance().doDecrypt(encryptedData);
    }
    
    /**
     * SM2签名（需要私钥，Android客户端通常无法使用）
     * @param data 待签名数据
     * @return 签名值（十六进制字符串）
     */
    public static String doSignature(String data) {
        return SM2Util.getInstance().doSignature(data);
    }
    
    /**
     * SM2验签（只需要公钥，适用于Android客户端场景）
     * @param data 原始数据
     * @param signature 签名值（十六进制字符串）
     * @return 验签结果
     */
    public static boolean doVerifySignature(String data, String signature) {
        return SM2Util.getInstance().doVerifySignature(data, signature);
    }
    
    // SM4相关方法
    
    /**
     * SM4 ECB加密（使用默认密钥）
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4Encrypt(String msgString) {
        return SM4Util.encryptECB(msgString);
    }
    
    /**
     * SM4 ECB解密（使用默认密钥）
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm4Decrypt(String encryptedData) {
        return SM4Util.decryptECB(encryptedData);
    }
    
    /**
     * SM4 CBC加密（使用默认密钥和IV）
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4CbcEncrypt(String msgString) {
        return SM4Util.encryptCBC(msgString);
    }
    
    /**
     * SM4 CBC解密（使用默认密钥和IV）
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm4CbcDecrypt(String encryptedData) {
        return SM4Util.decryptCBC(encryptedData);
    }
    
    /**
     * SM4 ECB加密（自定义密钥）
     * @param msgString 待加密消息
     * @param key 密钥
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4Encrypt(String msgString, String key) {
        return SM4Util.encryptECB(msgString, key);
    }
    
    /**
     * SM4 ECB解密（自定义密钥）
     * @param encryptedData 加密数据（十六进制字符串）
     * @param key 密钥
     * @return 解密结果
     */
    public static String doSm4Decrypt(String encryptedData, String key) {
        return SM4Util.decryptECB(encryptedData, key);
    }
    
    /**
     * SM4 CBC加密（自定义密钥和IV）
     * @param msgString 待加密消息
     * @param key 密钥
     * @param iv 初始向量
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4CbcEncrypt(String msgString, String key, String iv) {
        return SM4Util.encryptCBC(msgString, key, iv);
    }
    
    /**
     * SM4 CBC解密（自定义密钥和IV）
     * @param encryptedData 加密数据（十六进制字符串）
     * @param key 密钥
     * @param iv 初始向量
     * @return 解密结果
     */
    public static String doSm4CbcDecrypt(String encryptedData, String key, String iv) {
        return SM4Util.decryptCBC(encryptedData, key, iv);
    }
}