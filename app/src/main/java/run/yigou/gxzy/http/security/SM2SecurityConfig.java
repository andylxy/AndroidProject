package run.yigou.gxzy.http.security;

import android.util.Log;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.Security;

/**
 * author : Android 轮子哥
 * desc   : SM2安全配置类，用于管理SM2算法相关的配置信息
 */
public class SM2SecurityConfig {
    private static final String TAG = "SM2SecurityConfig";
    
    // 默认的国密曲线参数
    private static final BigInteger P = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger A = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC", 16);
    private static final BigInteger B = new BigInteger("28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93", 16);
    private static final BigInteger N = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123", 16);
    private static final BigInteger GX = new BigInteger("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
    private static final BigInteger GY = new BigInteger("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);
    
    private static ECCurve curve;
    private static ECDomainParameters domainParams;
    
    private static ECPublicKeyParameters sSm2PublicKey;
    private static ECPrivateKeyParameters sSm2PrivateKey;
    
    static {
        curve = new ECCurve.Fp(P, A, B);
        ECPoint G = curve.createPoint(GX, GY);
        domainParams = new ECDomainParameters(curve, G, N);
    }
    
    /**
     * 初始化SM2密钥对
     * 在实际应用中，应该从安全的地方加载密钥，而不是硬编码
     * 
     * @param publicKeyX 公钥X坐标 (十六进制字符串)
     * @param publicKeyY 公钥Y坐标 (十六进制字符串)
     * @param privateKeyD 私钥D值 (十六进制字符串)
     */
    public static void initSM2Keys(String publicKeyX, String publicKeyY, String privateKeyD) {
        try {
            // 启用SM2算法
            SecurityConfig.enableSM2();
            
            // 初始化公钥
            BigInteger x = new BigInteger(publicKeyX, 16);
            BigInteger y = new BigInteger(publicKeyY, 16);
            ECPoint pubPoint = curve.createPoint(x, y);
            sSm2PublicKey = new ECPublicKeyParameters(pubPoint, domainParams);
            
            // 初始化私钥
            BigInteger d = new BigInteger(privateKeyD, 16);
            sSm2PrivateKey = new ECPrivateKeyParameters(d, domainParams);
            
            Log.i(TAG, "SM2 keys initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SM2 keys", e);
        }
    }
    
    /**
     * 初始化SM2密钥对（使用组合公钥格式）
     * 公钥格式为 "x,y"，其中x和y都是十六进制字符串
     * 
     * @param publicKey 公钥字符串，格式为"x,y"
     * @param privateKeyD 私钥D值 (十六进制字符串)
     */
    public static void initSM2Keys(String publicKey, String privateKeyD) {
        try {
            if (publicKey == null || !publicKey.contains(",")) {
                throw new IllegalArgumentException("Invalid public key format. Expected format: \"x,y\"");
            }
            
            String[] parts = publicKey.split(",", 2);
            String publicKeyX = parts[0];
            String publicKeyY = parts[1];
            
            initSM2Keys(publicKeyX, publicKeyY, privateKeyD);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SM2 keys from combined public key", e);
        }
    }
    
    /**
     * 初始化SM2密钥对（使用完整公钥格式）
     * 公钥格式为以"04"开头的完整公钥十六进制字符串
     * 
     * @param fullPublicKey 完整公钥（十六进制字符串，以04开头）
     * @param privateKeyD 私钥D值 (十六进制字符串)
     */
    public static void initSM2KeysWithFullPublicKey(String fullPublicKey, String privateKeyD) {
        try {
            // 解析完整公钥（去掉04前缀）
            String pubKeyData = fullPublicKey.startsWith("04") ? fullPublicKey.substring(2) : fullPublicKey;
            String publicKeyX = pubKeyData.substring(0, 64);
            String publicKeyY = pubKeyData.substring(64, 128);
            
            initSM2Keys(publicKeyX, publicKeyY, privateKeyD);
            
            Log.i(TAG, "SM2 keys initialized successfully with full public key");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SM2 keys from full public key", e);
        }
    }
    
    /**
     * 使用SM2公钥加密数据
     * 
     * @param data 待加密数据
     * @return 加密后的数据
     */
    public static byte[] encryptWithSM2(byte[] data) {
        if (!SecurityConfig.isSM2Enabled()) {
            throw new IllegalStateException("SM2 is not enabled");
        }
        
        if (sSm2PublicKey == null) {
            throw new IllegalStateException("SM2 public key is not initialized");
        }
        
        try {
            org.bouncycastle.crypto.engines.SM2Engine engine = new org.bouncycastle.crypto.engines.SM2Engine();
            engine.init(true, new org.bouncycastle.crypto.params.ParametersWithRandom(sSm2PublicKey));
            byte[] encrypted = engine.processBlock(data, 0, data.length);
            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException("SM2 encryption failed", e);
        }
    }
    
    /**
     * 使用SM2私钥解密数据
     * 
     * @param encryptedData 待解密数据
     * @return 解密后的数据
     */
    public static byte[] decryptWithSM2(byte[] encryptedData) {
        if (!SecurityConfig.isSM2Enabled()) {
            throw new IllegalStateException("SM2 is not enabled");
        }
        
        if (sSm2PrivateKey == null) {
            throw new IllegalStateException("SM2 private key is not initialized");
        }
        
        try {
            org.bouncycastle.crypto.engines.SM2Engine engine = new org.bouncycastle.crypto.engines.SM2Engine();
            engine.init(false, sSm2PrivateKey);
            byte[] decrypted = engine.processBlock(encryptedData, 0, encryptedData.length);
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException("SM2 decryption failed", e);
        }
    }
    
    /**
     * 使用SM2私钥签名数据
     * 
     * @param data 待签名数据
     * @return 签名值
     */
    public static byte[] signWithSM2(byte[] data) {
        if (!SecurityConfig.isSM2Enabled()) {
            throw new IllegalStateException("SM2 is not enabled");
        }
        
        if (sSm2PrivateKey == null) {
            throw new IllegalStateException("SM2 private key is not initialized");
        }
        
        try {
            org.bouncycastle.crypto.signers.SM2Signer signer = new org.bouncycastle.crypto.signers.SM2Signer();
            signer.init(true, new org.bouncycastle.crypto.params.ParametersWithRandom(sSm2PrivateKey));
            signer.update(data, 0, data.length);
            byte[] signature = signer.generateSignature();
            return signature;
        } catch (Exception e) {
            throw new RuntimeException("SM2 signing failed", e);
        }
    }
    
    /**
     * 使用SM2公钥验签数据
     * 
     * @param data 数据
     * @param signature 签名值
     * @return 验签结果
     */
    public static boolean verifyWithSM2(byte[] data, byte[] signature) {
        if (!SecurityConfig.isSM2Enabled()) {
            throw new IllegalStateException("SM2 is not enabled");
        }
        
        if (sSm2PublicKey == null) {
            throw new IllegalStateException("SM2 public key is not initialized");
        }
        
        try {
            org.bouncycastle.crypto.signers.SM2Signer signer = new org.bouncycastle.crypto.signers.SM2Signer();
            signer.init(false, sSm2PublicKey);
            signer.update(data, 0, data.length);
            return signer.verifySignature(signature);
        } catch (Exception e) {
            throw new RuntimeException("SM2 verification failed", e);
        }
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
     * SM4 ECB模式加密
     * 
     * @param data 待加密数据
     * @param key 密钥
     * @return 加密结果
     */
    public static String sm4EncryptECB(String data, String key) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            
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
            throw new RuntimeException("SM4 ECB encryption failed", e);
        }
    }
    
    /**
     * SM4 ECB模式解密
     * 
     * @param encryptedData 加密数据
     * @param key 密钥
     * @return 解密结果
     */
    public static String sm4DecryptECB(String encryptedData, String key) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            
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
            throw new RuntimeException("SM4 ECB decryption failed", e);
        }
    }
    
    /**
     * SM4 CBC模式加密
     * 
     * @param data 待加密数据
     * @param key 密钥
     * @param iv 初始向量
     * @return 加密结果
     */
    public static String sm4EncryptCBC(String data, String key, String iv) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            
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
            throw new RuntimeException("SM4 CBC encryption failed", e);
        }
    }
    
    /**
     * SM4 CBC模式解密
     * 
     * @param encryptedData 加密数据
     * @param key 密钥
     * @param iv 初始向量
     * @return 解密结果
     */
    public static String sm4DecryptCBC(String encryptedData, String key, String iv) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            
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
            throw new RuntimeException("SM4 CBC decryption failed", e);
        }
    }
    
    /**
     * 获取公钥字符串（x,y格式）
     * 
     * @return 公钥字符串
     */
    public static String getPublicKeyString() {
        if (sSm2PublicKey == null) {
            return null;
        }
        
        ECPoint q = sSm2PublicKey.getQ();
        BigInteger x = q.getAffineXCoord().toBigInteger();
        BigInteger y = q.getAffineYCoord().toBigInteger();
        
        return x.toString(16) + "," + y.toString(16);
    }
    
    /**
     * 获取完整公钥字符串（以04开头的格式）
     * 
     * @return 完整公钥字符串
     */
    public static String getFullPublicKeyString() {
        if (sSm2PublicKey == null) {
            return null;
        }
        
        ECPoint q = sSm2PublicKey.getQ();
        BigInteger x = q.getAffineXCoord().toBigInteger();
        BigInteger y = q.getAffineYCoord().toBigInteger();
        
        // 补齐64位长度
        String xHex = String.format("%064x", x);
        String yHex = String.format("%064x", y);
        
        return "04" + xHex + yHex;
    }
}