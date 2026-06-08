package run.yigou.gxzy.crypto;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * AES 对称加密算法，加解密工具类
 */
public class AESUtil {

    private static final String TAG = AESUtil.class.getSimpleName() + " --> ";

    /**
     * 加密算法
     */
    private static final String KEY_ALGORITHM = "AES";

    /**
     * AES 的 密钥长度，32 字节，范围：16 - 32 字节
     */
    public static final int SECRET_KEY_LENGTH = 16;

    /**
     * 字符编码
     */
    private static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;

    /**
     * 秘钥长度不足 16 个字节时，默认填充位数
     */
    private static final String DEFAULT_VALUE = "0";
    /**
     * 加解密算法/工作模式/填充方式
     */
    //private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    /**
     * 加密密码，长度：16 或 32 个字符(随便定义)
     */
    private static final String secretKey = "1qaz2wsx3edc4rfv";
    /**
     * AES 加密
     *
     * @param data      待加密内容
     * @return 返回Base64转码后的加密数据
     */
    public static String encrypt(String data) {
        try {
            //创建密码器
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            //初始化为加密密码器
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(secretKey));
            byte[] encryptByte = cipher.doFinal(data.getBytes(CHARSET_UTF8));
            // 将加密以后的数据进行 Base64 编码
            return base64Encode(encryptByte);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    /**
     * AES 解密
     *
     * @param base64Data 加密的密文 Base64 字符串
     */
    public static String decrypt(String base64Data) {
        try {
            byte[] data = base64Decode(base64Data);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            //设置为解密模式
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(secretKey));
            //执行解密操作
            byte[] result = cipher.doFinal(data);
            return new String(result, CHARSET_UTF8);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    /**
     * 使用密码获取 AES 秘钥
     */
    public static SecretKeySpec getSecretKey(String secretKey) {
        secretKey = toMakeKey(secretKey, SECRET_KEY_LENGTH, DEFAULT_VALUE);
        return new SecretKeySpec(secretKey.getBytes(CHARSET_UTF8), KEY_ALGORITHM);
    }

    /**
     * 如果 AES 的密钥小于 {@code length} 的长度，就对秘钥进行补位，保证秘钥安全。
     *
     * @param secretKey 密钥 key
     * @param length    密钥应有的长度
     * @param text      默认补的文本
     * @return 密钥
     */
    private static String toMakeKey(String secretKey, int length, String text) {
        // 获取密钥长度
        int strLen = secretKey.length();
        // 判断长度是否小于应有的长度
        if (strLen < length) {
            // 补全位数
            StringBuilder builder = new StringBuilder();
            // 将key添加至builder中
            builder.append(secretKey);
            // 遍历添加默认文本
            for (int i = 0; i < length - strLen; i++) {
                builder.append(text);
            }
            // 赋值
            secretKey = builder.toString();
        }
        return secretKey;
    }

    /**
     * 将 Base64 字符串 解码成 字节数组
     */
    public static byte[] base64Decode(String data) {
        return Base64.decode(data, Base64.NO_WRAP);
    }

    /**
     * 将 字节数组 转换成 Base64 编码
     */
    public static String base64Encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    /**
     * 处理异常
     */
    private static void handleException(Exception e) {
        Log.e(TAG, "AES exception", e);
    }

    /**
     * 关闭流
     */
    public static void closeSilent(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(TAG, "close exception", e);
            }
        }
    }

    // AES相关方法

    /**
     * AES 的 密钥长度，32 字节，范围：16 - 32 字节
     */
    public static final int AES_SECRET_KEY_LENGTH = 16;

    /**
     * 字符编码
     */
    private static final Charset AES_CHARSET_UTF8 = StandardCharsets.UTF_8;

    /**
     * 秘钥长度不足 16 个字节时，默认填充位数
     */
    private static final String AES_DEFAULT_VALUE = "0";
    /**
     * 加解密算法/工作模式/填充方式
     */
    private static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    /**
     * 加密密码，长度：16 或 32 个字符(随便定义)
     */
    private static final String AES_SECRET_KEY = "1qaz2wsx3edc4rfv";

    public static String encryptByAES(String data) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(AES_SECRET_KEY));
            byte[] encryptByte = cipher.doFinal(data.getBytes(AES_CHARSET_UTF8));
            return base64Encode(encryptByte);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public static String decryptByAES(String base64Data) {
        try {
            byte[] data = base64Decode(base64Data);
            Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(AES_SECRET_KEY));
            byte[] result = cipher.doFinal(data);
            return new String(result, AES_CHARSET_UTF8);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public static String encryptByAesAndECB(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_SECRET_KEY.getBytes(AES_CHARSET_UTF8), KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptByte = cipher.doFinal(data.getBytes(AES_CHARSET_UTF8));
            return base64Encode(encryptByte);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    public static String decryptByAesAndECB(String base64Data) {
        try {
            byte[] data = base64Decode(base64Data);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_SECRET_KEY.getBytes(AES_CHARSET_UTF8), KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] result = cipher.doFinal(data);
            return new String(result, AES_CHARSET_UTF8);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    /**
     * AES 加密文件
     *
     * @param inputFile  加密文件路径
     * @param outputFile 加密文件输出路径
     */
    public static void encryptFileByAES(String inputFile, String outputFile) {
        try {
            File srcFile = new File(inputFile);
            if (!srcFile.exists()) {
                throw new FileNotFoundException("file not found: " + inputFile);
            }
            Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(AES_SECRET_KEY));
            CipherOutputStream cos = null;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(srcFile);
                cos = new CipherOutputStream(new FileOutputStream(outputFile), cipher);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, len);
                }
            } finally {
                closeSilent(cos);
                closeSilent(fis);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IOException e) {
            handleException(e);
        }
    }

    /**
     * AES 解密文件
     *
     * @param inputFile  解密文件路径
     * @param outputFile 解密文件输出路径
     */
    public static void decryptFileByAES(String inputFile, String outputFile) {
        try {
            File srcFile = new File(inputFile);
            if (!srcFile.exists()) {
                throw new FileNotFoundException("file not found: " + inputFile);
            }
            Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(AES_SECRET_KEY));
            CipherInputStream cis = null;
            FileOutputStream fos = null;
            try {
                cis = new CipherInputStream(new FileInputStream(srcFile), cipher);
                fos = new FileOutputStream(outputFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            } finally {
                closeSilent(fos);
                closeSilent(cis);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IOException e) {
            handleException(e);
        }
    }

    /**
     * 加密文件
     *
     * @param inputFile  加密文件路径
     * @param outputFile 加密文件输出路径
     */
    public static void encryptFile(String inputFile, String outputFile) {
        try {
            File srcFile = new File(inputFile);
            if (!srcFile.exists()) {
                throw new FileNotFoundException("file not found: " + inputFile);
            }
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_SECRET_KEY.getBytes(AES_CHARSET_UTF8), KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            CipherOutputStream cos = null;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(srcFile);
                cos = new CipherOutputStream(new FileOutputStream(outputFile), cipher);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, len);
                }
            } finally {
                closeSilent(cos);
                closeSilent(fis);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IOException e) {
            handleException(e);
        }
    }

    /**
     * 解密文件
     *
     * @param inputFile  解密文件路径
     * @param outputFile 解密文件输出路径
     */
    public static void decryptFile(String inputFile, String outputFile) {
        try {
            File srcFile = new File(inputFile);
            if (!srcFile.exists()) {
                throw new FileNotFoundException("file not found: " + inputFile);
            }
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_SECRET_KEY.getBytes(AES_CHARSET_UTF8), KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            CipherInputStream cis = null;
            FileOutputStream fos = null;
            try {
                cis = new CipherInputStream(new FileInputStream(srcFile), cipher);
                fos = new FileOutputStream(outputFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            } finally {
                closeSilent(fos);
                closeSilent(cis);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IOException e) {
            handleException(e);
        }
    }
}
