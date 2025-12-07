package run.yigou.gxzy.utils;

import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.security.Security;

/**
 * Android平台下类似于sm-crypto库的SM4工具类
 * 提供便捷的SM4加密、解密功能
 * 支持ECB和CBC模式，支持自定义密钥和初始向量
 */
public class SM4Util {
    private static final String ALGORITHM_NAME = "SM4";
    
    // 默认密钥和IV
    private static final String DEFAULT_KEY = "0123456789abcdeffedcba9876543210";
    private static final String DEFAULT_IV = "fedcba98765432100123456789abcdef";
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * SM4 ECB模式加密
     * @param data 待加密数据
     * @param key 密钥（十六进制字符串）
     * @return 加密后的数据（十六进制字符串）
     */
    public static String encryptECB(String data, String key) {
        try {
            SM4Engine engine = new SM4Engine();
            KeyParameter keyParam = new KeyParameter(Hex.decode(key));
            
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(engine, new PKCS7Padding());
            cipher.init(true, keyParam);
            
            byte[] dataBytes = data.getBytes("UTF-8");
            byte[] encrypted = new byte[cipher.getOutputSize(dataBytes.length)];
            int len = cipher.processBytes(dataBytes, 0, dataBytes.length, encrypted, 0);
            cipher.doFinal(encrypted, len);
            
            return Hex.toHexString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM4 ECB加密失败", e);
        }
    }
    
    /**
     * SM4 ECB模式解密
     * @param encryptedData 加密数据（十六进制字符串）
     * @param key 密钥（十六进制字符串）
     * @return 解密后的原始数据
     */
    public static String decryptECB(String encryptedData, String key) {
        try {
            SM4Engine engine = new SM4Engine();
            KeyParameter keyParam = new KeyParameter(Hex.decode(key));
            
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(engine, new PKCS7Padding());
            cipher.init(false, keyParam);
            
            byte[] encryptedBytes = Hex.decode(encryptedData);
            byte[] decrypted = new byte[cipher.getOutputSize(encryptedBytes.length)];
            int len = cipher.processBytes(encryptedBytes, 0, encryptedBytes.length, decrypted, 0);
            len += cipher.doFinal(decrypted, len);
            
            return new String(decrypted, 0, len, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("SM4 ECB解密失败", e);
        }
    }
    
    /**
     * SM4 CBC模式加密
     * @param data 待加密数据
     * @param key 密钥（十六进制字符串）
     * @param iv 初始向量（十六进制字符串）
     * @return 加密后的数据（十六进制字符串）
     */
    public static String encryptCBC(String data, String key, String iv) {
        try {
            CBCBlockCipher cbcCipher = new CBCBlockCipher(new SM4Engine());
            KeyParameter keyParam = new KeyParameter(Hex.decode(key));
            ParametersWithIV paramsWithIV = new ParametersWithIV(keyParam, Hex.decode(iv));
            
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(cbcCipher, new PKCS7Padding());
            cipher.init(true, paramsWithIV);
            
            byte[] dataBytes = data.getBytes("UTF-8");
            byte[] encrypted = new byte[cipher.getOutputSize(dataBytes.length)];
            int len = cipher.processBytes(dataBytes, 0, dataBytes.length, encrypted, 0);
            cipher.doFinal(encrypted, len);
            
            return Hex.toHexString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM4 CBC加密失败", e);
        }
    }
    
    /**
     * SM4 CBC模式解密
     * @param encryptedData 加密数据（十六进制字符串）
     * @param key 密钥（十六进制字符串）
     * @param iv 初始向量（十六进制字符串）
     * @return 解密后的原始数据
     */
    public static String decryptCBC(String encryptedData, String key, String iv) {
        try {
            CBCBlockCipher cbcCipher = new CBCBlockCipher(new SM4Engine());
            KeyParameter keyParam = new KeyParameter(Hex.decode(key));
            ParametersWithIV paramsWithIV = new ParametersWithIV(keyParam, Hex.decode(iv));
            
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(cbcCipher, new PKCS7Padding());
            cipher.init(false, paramsWithIV);
            
            byte[] encryptedBytes = Hex.decode(encryptedData);
            byte[] decrypted = new byte[cipher.getOutputSize(encryptedBytes.length)];
            int len = cipher.processBytes(encryptedBytes, 0, encryptedBytes.length, decrypted, 0);
            len += cipher.doFinal(decrypted, len);
            
            return new String(decrypted, 0, len, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("SM4 CBC解密失败", e);
        }
    }
    
    /**
     * 使用默认密钥进行SM4 ECB加密
     * @param data 待加密数据
     * @return 加密后的数据（十六进制字符串）
     */
    public static String encryptECB(String data) {
        return encryptECB(data, DEFAULT_KEY);
    }
    
    /**
     * 使用默认密钥进行SM4 ECB解密
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密后的原始数据
     */
    public static String decryptECB(String encryptedData) {
        return decryptECB(encryptedData, DEFAULT_KEY);
    }
    
    /**
     * 使用默认密钥和IV进行SM4 CBC加密
     * @param data 待加密数据
     * @return 加密后的数据（十六进制字符串）
     */
    public static String encryptCBC(String data) {
        return encryptCBC(data, DEFAULT_KEY, DEFAULT_IV);
    }
    
    /**
     * 使用默认密钥和IV进行SM4 CBC解密
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密后的原始数据
     */
    public static String decryptCBC(String encryptedData) {
        return decryptCBC(encryptedData, DEFAULT_KEY, DEFAULT_IV);
    }
}