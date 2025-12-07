package run.yigou.gxzy.utils;

import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.Security;

/**
 * Android平台下类似于sm-crypto库的SM2工具类
 * 提供便捷的SM2加密、解密、签名、验签功能
 * 支持两种密钥初始化方式（分离坐标形式和完整公钥形式）
 */
public class SM2Util {
    private static final String ALGORITHM_NAME = "SM2";
    private static final String CURVE_NAME = "sm2p256v1";
    
    // 默认的国密曲线参数
    private static final BigInteger P = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger A = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC", 16);
    private static final BigInteger B = new BigInteger("28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93", 16);
    private static final BigInteger N = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123", 16);
    private static final BigInteger GX = new BigInteger("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
    private static final BigInteger GY = new BigInteger("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);
    
    private static ECCurve curve;
    private static ECDomainParameters domainParams;
    private static SM2Util instance;
    
    private ECPublicKeyParameters publicKey;
    private ECPrivateKeyParameters privateKey;
    
    static {
        Security.addProvider(new BouncyCastleProvider());
        curve = new ECCurve.Fp(P, A, B);
        ECPoint G = curve.createPoint(GX, GY);
        domainParams = new ECDomainParameters(curve, G, N);
    }
    
    private SM2Util() {}
    
    /**
     * 获取单例实例
     * @return SM2Util实例
     */
    public static SM2Util getInstance() {
        if (instance == null) {
            synchronized (SM2Util.class) {
                if (instance == null) {
                    instance = new SM2Util();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化密钥对（使用分离的坐标形式）
     * @param publicKeyX 公钥X坐标（十六进制字符串）
     * @param publicKeyY 公钥Y坐标（十六进制字符串）
     * @param privateKeyD 私钥D值（十六进制字符串）
     */
    public void initKeys(String publicKeyX, String publicKeyY, String privateKeyD) {
        // 初始化公钥
        BigInteger x = new BigInteger(publicKeyX, 16);
        BigInteger y = new BigInteger(publicKeyY, 16);
        ECPoint pubPoint = curve.createPoint(x, y);
        this.publicKey = new ECPublicKeyParameters(pubPoint, domainParams);
        
        // 初始化私钥
        BigInteger d = new BigInteger(privateKeyD, 16);
        this.privateKey = new ECPrivateKeyParameters(d, domainParams);
    }
    
    /**
     * 初始化密钥对（使用组合公钥形式，格式为"x,y"）
     * @param publicKey 公钥字符串，格式为"x,y"
     * @param privateKeyD 私钥D值（十六进制字符串）
     */
    public void initKeysWithCombinedPublicKey(String publicKey, String privateKeyD) {
        if (publicKey == null || !publicKey.contains(",")) {
            throw new IllegalArgumentException("Invalid public key format. Expected format: \"x,y\"");
        }
        
        String[] parts = publicKey.split(",", 2);
        String publicKeyX = parts[0];
        String publicKeyY = parts[1];
        initKeys(publicKeyX, publicKeyY, privateKeyD);
    }
    
    /**
     * 初始化密钥对（使用完整公钥形式，类似sm-crypto）
     * @param publicKey 完整公钥（十六进制字符串，以04开头）
     * @param privateKeyD 私钥D值（十六进制字符串）
     */
    public void initKeysWithFullPublicKey(String publicKey, String privateKeyD) {
        // 解析完整公钥（去掉04前缀）
        String pubKeyData = publicKey.startsWith("04") ? publicKey.substring(2) : publicKey;
        String publicKeyX = pubKeyData.substring(0, 64);
        String publicKeyY = pubKeyData.substring(64, 128);
        initKeys(publicKeyX, publicKeyY, privateKeyD);
    }
    
    /**
     * SM2加密
     * @param data 待加密数据
     * @return 加密后的数据（十六进制字符串）
     */
    public String doEncrypt(String data) {
        if (publicKey == null) {
            throw new IllegalStateException("请先初始化公钥");
        }
        
        try {
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(true, new ParametersWithRandom(publicKey));
            byte[] encrypted = engine.processBlock(data.getBytes("UTF-8"), 0, data.length());
            return Hex.toHexString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM2加密失败", e);
        }
    }
    
    /**
     * SM2解密
     * @param encryptedData 加密数据（十六进制字符串）
     * @return 解密后的原始数据
     */
    public String doDecrypt(String encryptedData) {
        if (privateKey == null) {
            throw new IllegalStateException("请先初始化私钥");
        }
        
        try {
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(false, privateKey);
            byte[] decrypted = engine.processBlock(Hex.decode(encryptedData), 0, encryptedData.length()/2);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("SM2解密失败", e);
        }
    }
    
    /**
     * SM2签名
     * @param data 待签名数据
     * @return 签名值（十六进制字符串）
     */
    public String doSignature(String data) {
        if (privateKey == null) {
            throw new IllegalStateException("请先初始化私钥");
        }
        
        try {
            org.bouncycastle.crypto.signers.SM2Signer signer = new org.bouncycastle.crypto.signers.SM2Signer();
            signer.init(true, new ParametersWithRandom(privateKey));
            byte[] dataBytes = data.getBytes("UTF-8");
            signer.update(dataBytes, 0, dataBytes.length);
            byte[] signature = signer.generateSignature();
            return Hex.toHexString(signature);
        } catch (Exception e) {
            throw new RuntimeException("SM2签名失败", e);
        }
    }
    
    /**
     * SM2验签
     * @param data 原始数据
     * @param signature 签名值（十六进制字符串）
     * @return 验签结果
     */
    public boolean doVerifySignature(String data, String signature) {
        if (publicKey == null) {
            throw new IllegalStateException("请先初始化公钥");
        }
        
        try {
            org.bouncycastle.crypto.signers.SM2Signer signer = new org.bouncycastle.crypto.signers.SM2Signer();
            signer.init(false, publicKey);
            byte[] dataBytes = data.getBytes("UTF-8");
            signer.update(dataBytes, 0, dataBytes.length);
            return signer.verifySignature(Hex.decode(signature));
        } catch (Exception e) {
            throw new RuntimeException("SM2验签失败", e);
        }
    }
    
    /**
     * 获取公钥字符串（x,y格式）
     * @return 公钥字符串
     */
    public String getPublicKeyString() {
        if (publicKey == null) {
            return null;
        }
        
        ECPoint q = publicKey.getQ();
        BigInteger x = q.getAffineXCoord().toBigInteger();
        BigInteger y = q.getAffineYCoord().toBigInteger();
        
        return x.toString(16) + "," + y.toString(16);
    }
    
    /**
     * 获取完整公钥字符串（以04开头的格式）
     * @return 完整公钥字符串
     */
    public String getFullPublicKeyString() {
        if (publicKey == null) {
            return null;
        }
        
        ECPoint q = publicKey.getQ();
        BigInteger x = q.getAffineXCoord().toBigInteger();
        BigInteger y = q.getAffineYCoord().toBigInteger();
        
        // 补齐64位长度
        String xHex = String.format("%064x", x);
        String yHex = String.format("%064x", y);
        
        return "04" + xHex + yHex;
    }
    
    // Getter methods for public and private keys
    public ECPublicKeyParameters getPublicKey() {
        return publicKey;
    }
    
    public ECPrivateKeyParameters getPrivateKey() {
        return privateKey;
    }
}