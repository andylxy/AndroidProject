package run.yigou.gxzy.Security;

import android.util.Base64;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import run.yigou.gxzy.Security.Cryptogram.CryptogramUtil;
import run.yigou.gxzy.Security.Cryptogram.SM2Util;
import run.yigou.gxzy.Security.Cryptogram.SM4Util;
import run.yigou.gxzy.Security.Cryptogram.SM4Util.Sm4CryptoEnum;
import run.yigou.gxzy.Security.Cryptogram.Sm.SM2CryptoUtil;
import run.yigou.gxzy.common.AppConst;

import com.github.gzuliyujiang.rsautils.RC4Utils;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

/**
 * 安全管理器类，用于全局初始化和管理加密功能
 * 提供统一的加密解密、签名验签接口
 */
public final class SecurityUtils {
    private static final String TAG = "SecurityUtils";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static SecurityUtils instance;

    /**
     * 缓存一次配置好的 SM2 公私钥，便于签名、验签直接复用。
     */
    private String sm2PublicKeyHex = "";
    private String sm2PrivateKeyHex = "";

    // 方式3：使用完整公钥形式初始化SM2公钥
    // 这种方式最符合Android客户端的使用场景
    private static final String DEFAULT_PUBLIC_KEY = "04CF658C65FB80CB5C7B91D3BD881521C2BD421202D29812785322F6366B8856B62D38D3AB5B5C299B03DD2EC0033370875A2787C6222E801AF87DDA53093322E2";

    static {
        // 添加BouncyCastleProvider提供者
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(new BouncyCastleProvider());
        Log.i(TAG, "BouncyCastleProvider registered in SecurityUtils");
    }

    private SecurityUtils() {
        // 初始化时同步一次默认密钥配置，避免首次调用找不到公钥
        // 注意：此时实例字段还是空字符串，真正的密钥配置在 initSecurityManager() 中完成
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
            //SecurityUtils securityManager = SecurityUtils.getInstance();
            SecurityUtils.initSM2PublicKeyWithFullFormat(DEFAULT_PUBLIC_KEY);

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
        getInstance().configureSm2Keys(buildFullPublicKey(publicKeyX, publicKeyY), null);
    }

    /**
     * 初始化SM2公钥（使用组合公钥格式）
     *
     * @param publicKey 公钥字符串，格式为"x,y"
     */
    public static void initSM2PublicKeyWithCombinedFormat(String publicKey) {
        if (publicKey == null || !publicKey.contains(",")) {
            Log.e(TAG, "initSM2PublicKeyWithCombinedFormat failed: format must be x,y");
            return;
        }
        String[] parts = publicKey.split(",", 2);
        initSM2PublicKey(parts[0], parts[1]);
    }

    /**
     * 初始化SM2公钥（使用完整公钥格式，类似sm-crypto）
     *
     * @param fullPublicKey 完整公钥（十六进制字符串，以04开头）
     */
    public static void initSM2PublicKeyWithFullFormat(String fullPublicKey) {
        getInstance().configureSm2Keys(normalizeFullPublicKey(fullPublicKey), null);
    }

    /**
     * 初始化SM2密钥对（仅在需要完整密钥对时使用）
     *
     * @param publicKeyX  公钥X坐标
     * @param publicKeyY  公钥Y坐标
     * @param privateKeyD 私钥D值
     */
    public static void initSM2Keys(String publicKeyX, String publicKeyY, String privateKeyD) {
        getInstance().configureSm2Keys(buildFullPublicKey(publicKeyX, publicKeyY), privateKeyD);
    }

    /**
     * 初始化SM2密钥对（使用组合公钥格式）
     *
     * @param publicKey   公钥字符串，格式为"x,y"
     * @param privateKeyD 私钥D值
     */
    public static void initSM2KeysWithCombinedPublicKey(String publicKey, String privateKeyD) {
        if (publicKey == null || !publicKey.contains(",")) {
            Log.e(TAG, "initSM2KeysWithCombinedPublicKey failed: format must be x,y");
            return;
        }
        String[] parts = publicKey.split(",", 2);
        initSM2Keys(parts[0], parts[1], privateKeyD);
    }

    /**
     * 初始化SM2密钥对（使用完整公钥格式，类似sm-crypto）
     *
     * @param fullPublicKey 完整公钥（十六进制字符串，以04开头）
     * @param privateKeyD   私钥D值
     */
    public static void initSM2KeysWithFullPublicKey(String fullPublicKey, String privateKeyD) {
        getInstance().configureSm2Keys(normalizeFullPublicKey(fullPublicKey), privateKeyD);
    }

    /**
     * SM2加密（使用公钥加密，适用于Android客户端场景）
     *
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm2Encrypt(String msgString) {
        return CryptogramUtil.sm2Encrypt(msgString);
    }

    /**
     * SM2解密（需要私钥，Android客户端通常无法使用）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm2Decrypt(String encryptedData) {
        return CryptogramUtil.sm2Decrypt(encryptedData);
    }

    /**
     * SM2签名（需要私钥，Android客户端通常无法使用）
     *
     * @param data 待签名数据
     * @return 签名值（十六进制字符串）
     */
    public static String doSignature(String data) {
        return getInstance().signWithSm2PrivateKey(data);
    }

    /**
     * SM2验签（只需要公钥，适用于Android客户端场景）
     *
     * @param data      原始数据
     * @param signature 签名值（十六进制字符串）
     * @return 验签结果
     */
    public static boolean doVerifySignature(String data, String signature) {
        return getInstance().verifyWithSm2PublicKey(data, signature);
    }

    // SM4相关静态方法（兼容CryptoUtil使用方式）

    /**
     * SM4 ECB加密（使用默认密钥）
     *
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4Encrypt(String msgString) {
        return CryptogramUtil.sm4Encrypt(msgString);
    }

    /**
     * SM4 ECB解密（使用默认密钥）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm4Decrypt(String encryptedData) {
        return CryptogramUtil.sm4Decrypt(encryptedData);
    }

    /**
     * SM4 CBC加密（使用默认密钥和IV）
     *
     * @param msgString 待加密消息
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4CbcEncrypt(String msgString) {
        return SM4Util.encrypt(msgString, null, null, false, Sm4CryptoEnum.CBC);
    }

    /**
     * SM4 CBC解密（使用默认密钥和IV）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密结果
     */
    public static String doSm4CbcDecrypt(String encryptedData) {
        return SM4Util.decrypt(encryptedData, null, null, false, Sm4CryptoEnum.CBC);
    }

    /**
     * SM4 ECB加密（自定义密钥）
     *
     * @param msgString 待加密消息
     * @param key       密钥
     * @return 加密结果（十六进制字符串）
     */
    public static String doSm4Encrypt(String msgString, String key) {
        return SM4Util.encrypt(msgString, key, null, false, Sm4CryptoEnum.ECB);
    }

    /**
     * SM4 ECB解密（自定义密钥）
     *
     * @param encryptedData 加密数据（十六进制字符串）
     * @param key           密钥
     * @return 解密结果
     */
    public static String doSm4Decrypt(String encryptedData, String key) {
        return SM4Util.decrypt(encryptedData, key, null, false, Sm4CryptoEnum.ECB);
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
        return SM4Util.encrypt(msgString, key, iv, false, Sm4CryptoEnum.CBC);
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
        return SM4Util.decrypt(encryptedData, key, iv, false, Sm4CryptoEnum.CBC);
    }

    // MD5相关方法
    
    private static final String[] HEX_ARRAY = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    /**
     * 计算指定字符串的MD5值
     *
     * @param originString 原始字符串
     * @return MD5加密后的字符串，如果加密失败则返回null
     */
    public static String calcMd5(String originString) {
        if (originString == null) {
            return null;
        }

        try {
            // 创建具有MD5算法的信息摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 使用指定的字节数组对摘要进行最后更新，然后完成摘要计算
            byte[] bytes = md.digest(originString.getBytes());
            // 将得到的字节数组变成字符串返回
            String result = byteArrayToHex(bytes);
            return result.toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将字节数组转换成十六进制，并以字符串的形式返回
     * 128位是指二进制位。二进制太长，所以一般都改写成16进制，
     * 每一位16进制数可以代替4位二进制数，所以128位二进制数写成16进制就变成了128/4=32位。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String byteArrayToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(byteToHex(b));
        }
        return sb.toString();
    }

    /**
     * 将一个字节转换成十六进制，并以字符串的形式返回
     *
     * @param b 字节值
     * @return 十六进制字符串
     */
    private static String byteToHex(byte b) {
        int n = b;
        if (n < 0) {
            n = n + 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return HEX_ARRAY[d1] + HEX_ARRAY[d2];
    }

    // AES相关方法

    /**
     * AES 的 密钥长度，32 字节，范围：16 - 32 字节
     */
    public static final int AES_SECRET_KEY_LENGTH = 16;

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
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    /**
     * 默认加密密码，长度：16 或 32 个字符
     */
    private static final String DEFAULT_AES_SECRET_KEY = "1qaz2wsx3edc4rfv";

    /**
     * AES 加密
     *
     * @param data 待加密内容
     * @return 返回Base64转码后的加密数据
     */
    public static String aesEncrypt(String data) {
        return aesEncrypt(data, DEFAULT_AES_SECRET_KEY);
    }

    /**
     * AES 加密
     *
     * @param data      待加密内容
     * @param secretKey 密钥
     * @return 返回Base64转码后的加密数据
     */
    public static String aesEncrypt(String data, String secretKey) {
        try {
            //创建密码器
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            //初始化为加密密码器
            cipher.init(Cipher.ENCRYPT_MODE, getAesSecretKey(secretKey));
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
    public static String aesDecrypt(String base64Data) {
        return aesDecrypt(base64Data, DEFAULT_AES_SECRET_KEY);
    }

    /**
     * AES 解密
     *
     * @param base64Data 加密的密文 Base64 字符串
     * @param secretKey  密钥
     */
    public static String aesDecrypt(String base64Data, String secretKey) {
        try {
            byte[] data = base64Decode(base64Data);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            //设置为解密模式
            cipher.init(Cipher.DECRYPT_MODE, getAesSecretKey(secretKey));
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
    public static SecretKeySpec getAesSecretKey(String secretKey) {
        secretKey = toMakeKey(secretKey, AES_SECRET_KEY_LENGTH, DEFAULT_VALUE);
        return new SecretKeySpec(secretKey.getBytes(CHARSET_UTF8), "AES");
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
        e.printStackTrace();
        Log.e(TAG, TAG + e);
    }

    /**
     * 对文件进行AES加密
     *
     * @param sourceFile 待加密文件
     * @param dir        加密后的文件存储路径
     * @param toFileName 加密后的文件名称
     * @param secretKey  密钥
     * @return 加密后的文件
     */
    public static File aesEncryptFile(File sourceFile, String dir, String toFileName, String secretKey) {
        try {
            // 创建加密后的文件
            File encryptFile = new File(dir, toFileName);
            // 根据文件创建输出流
            FileOutputStream outputStream = new FileOutputStream(encryptFile);
            // 初始化 Cipher
            Cipher cipher = initFileAESCipher(secretKey, Cipher.ENCRYPT_MODE);
            // 以加密流写入文件
            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(sourceFile), cipher);
            // 创建缓存字节数组
            byte[] buffer = new byte[1024 * 2];
            // 读取
            int len;
            // 读取加密并写入文件
            while ((len = cipherInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                outputStream.flush();
            }
            // 关闭加密输入流
            cipherInputStream.close();
            closeStream(outputStream);
            return encryptFile;
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    /**
     * AES解密文件
     *
     * @param sourceFile 源加密文件
     * @param dir        解密后的文件存储路径
     * @param toFileName 解密后的文件名称
     * @param secretKey  密钥
     */
    public static File aesDecryptFile(File sourceFile, String dir, String toFileName, String secretKey) {
        try {
            // 创建解密文件
            File decryptFile = new File(dir, toFileName);
            // 初始化Cipher
            Cipher cipher = initFileAESCipher(secretKey, Cipher.DECRYPT_MODE);
            // 根据源文件创建输入流
            FileInputStream inputStream = new FileInputStream(sourceFile);
            // 获取解密输出流
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(decryptFile), cipher);
            // 创建缓冲字节数组
            byte[] buffer = new byte[1024 * 2];
            int len;
            // 读取解密并写入
            while ((len = inputStream.read(buffer)) >= 0) {
                cipherOutputStream.write(buffer, 0, len);
                cipherOutputStream.flush();
            }
            // 关闭流
            cipherOutputStream.close();
            closeStream(inputStream);
            return decryptFile;
        } catch (IOException e) {
            handleException(e);
        }
        return null;
    }

    /**
     * 初始化 AES Cipher
     *
     * @param secretKey  密钥
     * @param cipherMode 加密模式
     * @return 密钥
     */
    private static Cipher initFileAESCipher(String secretKey, int cipherMode) {
        try {
            // 创建密钥规格
            SecretKeySpec secretKeySpec = getAesSecretKey(secretKey);
            // 获取密钥
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            // 初始化
            cipher.init(cipherMode, secretKeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
            return cipher;
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    /**
     * 关闭流
     *
     * @param closeable 实现Closeable接口
     */
    private static void closeStream(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     * 检查是否已设置SM2公钥
     */
    public boolean hasSM2PublicKey() {
        return !isBlank(sm2PublicKeyHex);
    }

    /**
     * 检查是否已设置SM2私钥
     */
    public boolean hasSM2PrivateKey() {
        return !isBlank(sm2PrivateKeyHex);
    }

    private void configureSm2Keys(String publicKeyHex, String privateKeyHex) {
        if (!isBlank(publicKeyHex)) {
            sm2PublicKeyHex = publicKeyHex.toUpperCase(Locale.ROOT);
        }
        if (!isBlank(privateKeyHex)) {
            sm2PrivateKeyHex = privateKeyHex.toUpperCase(Locale.ROOT);
        }
        SM2Util.configureKeys(sm2PublicKeyHex, sm2PrivateKeyHex);
    }

    private static String buildFullPublicKey(String publicKeyX, String publicKeyY) {
        return "04" + padCoordinate(publicKeyX) + padCoordinate(publicKeyY);
    }

    private static String normalizeFullPublicKey(String fullPublicKey) {
        if (isBlank(fullPublicKey)) {
            return "";
        }
        String sanitized = fullPublicKey.trim();
        return sanitized.startsWith("04") ? sanitized.toUpperCase(Locale.ROOT)
                : ("04" + sanitized).toUpperCase(Locale.ROOT);
    }

    private static String padCoordinate(String coordinate) {
        if (coordinate == null) {
            return "";
        }
        String sanitized = coordinate.trim();
        if (sanitized.startsWith("0x") || sanitized.startsWith("0X")) {
            sanitized = sanitized.substring(2);
        }
        sanitized = sanitized.replaceAll("[^0-9a-fA-F]", "");
        if (sanitized.length() > 64) {
            sanitized = sanitized.substring(sanitized.length() - 64);
        }
        return String.format(Locale.ROOT, "%64s", sanitized.toUpperCase(Locale.ROOT)).replace(' ', '0');
    }

    private String signWithSm2PrivateKey(String data) {
        if (isBlank(data)) {
            return "";
        }
        ECPrivateKeyParameters privateKeyParameters = buildPrivateKeyParameters();
        if (privateKeyParameters == null) {
            Log.e(TAG, "SM2 signature skipped: private key missing");
            return "";
        }
        try {
            SM2Signer signer = new SM2Signer();
            signer.init(true, new ParametersWithRandom(privateKeyParameters, SECURE_RANDOM));
            byte[] message = data.getBytes(StandardCharsets.UTF_8);
            signer.update(message, 0, message.length);
            byte[] signature = signer.generateSignature();
            return Hex.toHexString(signature).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            Log.e(TAG, "SM2 signature failed", e);
            return "";
        }
    }

    private boolean verifyWithSm2PublicKey(String data, String signature) {
        if (isBlank(data) || isBlank(signature)) {
            return false;
        }
        ECPublicKeyParameters publicKeyParameters = buildPublicKeyParameters();
        if (publicKeyParameters == null) {
            Log.e(TAG, "SM2 verify skipped: public key missing");
            return false;
        }
        try {
            SM2Signer signer = new SM2Signer();
            signer.init(false, publicKeyParameters);
            byte[] message = data.getBytes(StandardCharsets.UTF_8);
            signer.update(message, 0, message.length);
            byte[] signatureBytes = Hex.decode(signature);
            return signer.verifySignature(signatureBytes);
        } catch (Exception e) {
            Log.e(TAG, "SM2 verify failed", e);
            return false;
        }
    }

    private ECPrivateKeyParameters buildPrivateKeyParameters() {
        if (isBlank(sm2PrivateKeyHex)) {
            return null;
        }
        BigInteger d = new BigInteger(sm2PrivateKeyHex, 16);
        ECDomainParameters domainParameters = SM2CryptoUtil.getDomainParameters();
        return new ECPrivateKeyParameters(d, domainParameters);
    }

    private ECPublicKeyParameters buildPublicKeyParameters() {
        if (isBlank(sm2PublicKeyHex)) {
            return null;
        }
        String normalized = normalizeFullPublicKey(sm2PublicKeyHex);
        ECPoint point = SM2CryptoUtil.getCurve().decodePoint(Hex.decode(normalized));
        ECDomainParameters domainParameters = SM2CryptoUtil.getDomainParameters();
        return new ECPublicKeyParameters(point, domainParameters);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // RC4相关方法

    /**
     * RC4解密Base64字符串
     *
     * @param encryptedData 解密Base64字符串
     * @return 返回解密数据，解密失败则返回null
     */
    public static String rc4Decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return null;
        }

        try {
            // 简单检查是否可能是 Base64 (避免对普通文本进行解密尝试导致异常)
            // Base64 包含 A-Z, a-z, 0-9, +, /, =，且通常没有空格
            if (!isBase64Like(encryptedData)) {
                return null;
            }

            byte[] decryptedData = RC4Utils.decryptFromBase64(encryptedData, AppConst.rc4_SecretKey);
            if (decryptedData == null) {
                return null;
            }
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败（可能是普通文本或密钥不匹配），返回 null
            // Log.e(TAG, "RC4 Decrypt failed: " + e.getMessage());
            return null;
        }
    }

    /**
     *简单检查字符串是否像 Base64
     */
    private static boolean isBase64Like(String str) {
        if (str == null) return false;
        // 检查长度是否为4的倍数（严格 Base64）
        // if (str.length() % 4 != 0) return false; 
        // 放宽限制，只检查字符集
        for (char c : str.toCharArray()) {
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || 
                  (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=')) {
                return false;
            }
        }
        return true;
    }

    /**
     * RC4加密数据
     *
     * @param encryptData 加密数据
     * @return 返回加密后Base64字符串，加密失败则返回null
     */
    public static String rc4Encrypt(String encryptData) {
        if (encryptData == null) {
            return null;
        }

        return RC4Utils.encryptToBase64(encryptData.getBytes(StandardCharsets.UTF_8), AppConst.rc4_SecretKey);
    }

    /**
     * RC4解密Base64字符串（使用自定义密钥）
     *
     * @param encryptedData 解密Base64字符串
     * @param secretKey     RC4密钥
     * @return 返回解密数据，解密失败则返回null
     */
    public static String rc4Decrypt(String encryptedData, String secretKey) {
        if (encryptedData == null) {
            return null;
        }

        byte[] decryptedData = RC4Utils.decryptFromBase64(encryptedData, secretKey);
        if (decryptedData == null) {
            return null;
        }
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * RC4加密数据（使用自定义密钥）
     *
     * @param encryptData 加密数据
     * @param secretKey   RC4密钥
     * @return 返回加密后Base64字符串，加密失败则返回null
     */
    public static String rc4Encrypt(String encryptData, String secretKey) {
        if (encryptData == null) {
            return null;
        }

        return RC4Utils.encryptToBase64(encryptData.getBytes(StandardCharsets.UTF_8), secretKey);
    }
}