package run.yigou.gxzy.Security.Cryptogram.Sm;

import android.util.Log;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Java 版本的 SM2 加解密实现，完全按照 SimpleEasy.Base.Cryptogram.Sm.SM2CryptoUtil 的 C# 逻辑移植。
 * 通过固定国密 SM2 曲线参数手动实现密钥生成、加解密流程，确保与 .NET 端结果保持一致。
 */
public final class SM2CryptoUtil {

    private static final String TAG = "SM2CryptoUtil";

    private SM2CryptoUtil() {
        // 工具类不允许被实例化
    }

    /**
     * 生成一对新的 SM2 公私钥，返回 16 进制字符串。
     */
    public static SM2Model getKey() {
        SM2 sm2 = SM2.getInstance();
        AsymmetricCipherKeyPair keyPair = sm2.getKeyPairGenerator().generateKeyPair();
        ECPrivateKeyParameters privateKeyParameters = (ECPrivateKeyParameters) keyPair.getPrivate();
        ECPublicKeyParameters publicKeyParameters = (ECPublicKeyParameters) keyPair.getPublic();
        SM2Model model = new SM2Model();
        model.setPrivateKey(Hex.toHexString(privateKeyParameters.getD().toByteArray()).toUpperCase(Locale.ROOT));
        model.setPublicKey(Hex.toHexString(publicKeyParameters.getQ().getEncoded(false)).toUpperCase(Locale.ROOT));
        return model;
    }

    /**
     * 使用 16 进制公钥对字符串进行加密，返回 C1C2C3 拼接的 16 进制大写字符串。
     */
    public static String encrypt(String publicKeyHex, String sourceData) {
        if (publicKeyHex == null || publicKeyHex.isEmpty() || sourceData == null || sourceData.isEmpty()) {
            return "";
        }
        try {
            byte[] publicKeyBytes = Hex.decode(publicKeyHex);
            byte[] data = sourceData.getBytes(StandardCharsets.UTF_8);
            return encrypt(publicKeyBytes, data);
        } catch (Exception e) {
            Log.e(TAG, "SM2 encrypt failed", e);
            return "";
        }
    }

    /**
     * C# 版本中的核心加密实现，直接返回 C1C2C3 拼接后的 16 进制字符串。
     */
    public static String encrypt(byte[] publicKey, byte[] data) {
        if (publicKey == null || publicKey.length == 0 || data == null || data.length == 0) {
            return "";
        }
        byte[] source = new byte[data.length];
        System.arraycopy(data, 0, source, 0, data.length);
        Cipher cipher = new Cipher();
        SM2 sm2 = SM2.getInstance();
        ECPoint userKey = sm2.getCurve().decodePoint(publicKey);
        ECPoint c1 = cipher.initEnc(sm2, userKey);
        cipher.encrypt(source);
        byte[] c3 = new byte[32];
        cipher.doFinal(c3);
        String sc1 = Hex.toHexString(c1.getEncoded(false));
        String sc2 = Hex.toHexString(source);
        String sc3 = Hex.toHexString(c3);
        return (sc1 + sc2 + sc3).toUpperCase(Locale.ROOT);
    }

    /**
     * 使用 16 进制私钥与密文执行解密，返回 UTF-8 明文。
     */
    public static String decrypt(String privateKeyHex, String encryptedDataHex) {
        if (privateKeyHex == null || privateKeyHex.isEmpty() || encryptedDataHex == null || encryptedDataHex.isEmpty()) {
            return "";
        }
        try {
            byte[] result = decrypt(Hex.decode(privateKeyHex), Hex.decode(encryptedDataHex));
            return result == null ? "" : new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "SM2 decrypt failed", e);
            return "";
        }
    }

    /**
     * C# 版本中的核心解密流程，按照 C1|C2|C3 结构拆分密文并执行 SM2 解密。
     */
    public static byte[] decrypt(byte[] privateKey, byte[] encryptedData) {
        if (privateKey == null || privateKey.length == 0 || encryptedData == null || encryptedData.length == 0) {
            return null;
        }
        String cipherHex = Hex.toHexString(encryptedData);
        if (cipherHex.length() < 130) {
            Log.e(TAG, "SM2 decrypt failed: invalid cipher length");
            return null;
        }
        byte[] c1Bytes = Hex.decode(cipherHex.substring(0, 130));
        int c2Len = encryptedData.length - 97;
        if (c2Len < 0) {
            Log.e(TAG, "SM2 decrypt failed: negative c2 length");
            return null;
        }
        byte[] c2 = Hex.decode(cipherHex.substring(130, 130 + (c2Len * 2)));
        byte[] c3 = Hex.decode(cipherHex.substring(130 + (c2Len * 2)));
        SM2 sm2 = SM2.getInstance();
        BigInteger userD = new BigInteger(1, privateKey);
        ECPoint c1 = sm2.getCurve().decodePoint(c1Bytes);
        Cipher cipher = new Cipher();
        cipher.initDec(userD, c1);
        cipher.decrypt(c2);
        cipher.doFinal(c3);
        return c2;
    }

    /**
     * Cipher 算法体，负责 SM3 KDF、异或加解密，与 C# 版本保持一致。
     */
    private static final class Cipher {
        private int ct = 1;
        private ECPoint p2;
        private SM3Digest sm3keybase;
        private SM3Digest sm3c3;
        private final byte[] key = new byte[32];
        private byte keyOff;

        private void reset() {
            sm3keybase = new SM3Digest();
            sm3c3 = new SM3Digest();
            byte[] x = convert32Bytes(p2.normalize().getXCoord().toBigInteger());
            sm3keybase.update(x, 0, x.length);
            sm3c3.update(x, 0, x.length);
            byte[] y = convert32Bytes(p2.normalize().getYCoord().toBigInteger());
            sm3keybase.update(y, 0, y.length);
            ct = 1;
            nextKey();
        }

        private void nextKey() {
            SM3Digest sm3keycur = new SM3Digest(sm3keybase);
            sm3keycur.update((byte) (ct >> 24 & 0xff));
            sm3keycur.update((byte) (ct >> 16 & 0xff));
            sm3keycur.update((byte) (ct >> 8 & 0xff));
            sm3keycur.update((byte) (ct & 0xff));
            sm3keycur.doFinal(key, 0);
            keyOff = 0;
            ct++;
        }

        private ECPoint initEnc(SM2 sm2, ECPoint userKey) {
            AsymmetricCipherKeyPair keyPair = sm2.getKeyPairGenerator().generateKeyPair();
            ECPrivateKeyParameters ecpriv = (ECPrivateKeyParameters) keyPair.getPrivate();
            BigInteger k = ecpriv.getD();
            ECPublicKeyParameters ecpub = (ECPublicKeyParameters) keyPair.getPublic();
            ECPoint c1 = ecpub.getQ();
            p2 = userKey.multiply(k);
            reset();
            return c1;
        }

        private void encrypt(byte[] data) {
            sm3c3.update(data, 0, data.length);
            for (int i = 0; i < data.length; i++) {
                if (keyOff == key.length) {
                    nextKey();
                }
                data[i] ^= key[keyOff++];
            }
        }

        private void initDec(BigInteger userD, ECPoint c1) {
            p2 = c1.multiply(userD);
            reset();
        }

        private void decrypt(byte[] data) {
            for (int i = 0; i < data.length; i++) {
                if (keyOff == key.length) {
                    nextKey();
                }
                data[i] ^= key[keyOff++];
            }
            sm3c3.update(data, 0, data.length);
        }

        private void doFinal(byte[] c3) {
            byte[] y = convert32Bytes(p2.normalize().getYCoord().toBigInteger());
            sm3c3.update(y, 0, y.length);
            sm3c3.doFinal(c3, 0);
            reset();
        }

        private static byte[] convert32Bytes(BigInteger bigInteger) {
            byte[] tmp = bigInteger.toByteArray();
            if (tmp.length == 32) {
                return tmp;
            }
            byte[] result = new byte[32];
            if (tmp.length > 32) {
                System.arraycopy(tmp, tmp.length - 32, result, 0, 32);
            } else {
                System.arraycopy(tmp, 0, result, 32 - tmp.length, tmp.length);
            }
            return result;
        }
    }

    /**
     * SM2 曲线参数定义，与 C# 版本保持一致。
     */
    private static final class SM2 {
        private static final SM2 INSTANCE = new SM2();

        private final BigInteger eccP = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF", 16);
        private final BigInteger eccA = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC", 16);
        private final BigInteger eccB = new BigInteger("28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93", 16);
        private final BigInteger eccN = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123", 16);
        private final BigInteger eccGx = new BigInteger("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
        private final BigInteger eccGy = new BigInteger("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);

        private final ECCurve curve;
        private final ECPoint g;
        private final ECDomainParameters domainParameters;
        private final ECKeyPairGenerator keyPairGenerator;

        private SM2() {
            curve = new ECCurve.Fp(eccP, eccA, eccB);
            g = curve.createPoint(eccGx, eccGy);
            domainParameters = new ECDomainParameters(curve, g, eccN);
            keyPairGenerator = new ECKeyPairGenerator();
            keyPairGenerator.init(new ECKeyGenerationParameters(domainParameters, new SecureRandom()));
        }

        public static SM2 getInstance() {
            return INSTANCE;
        }

        public ECCurve getCurve() {
            return curve;
        }

        public ECKeyPairGenerator getKeyPairGenerator() {
            return keyPairGenerator;
        }

        public ECDomainParameters getDomainParameters() {
            return domainParameters;
        }
    }

    /**
     * 暴露曲线域参数，便于外部执行签名/验签等操作。
     */
    public static ECDomainParameters getDomainParameters() {
        return SM2.getInstance().getDomainParameters();
    }

    /**
     * 暴露曲线实例，方便根据公钥坐标创建 ECPoint。
     */
    public static ECCurve getCurve() {
        return SM2.getInstance().getCurve();
    }

    /**
     * 与 C# 版本一致的数据结构，封装公私钥字符串。
     */
    public static final class SM2Model {
        private String publicKey;
        private String privateKey;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
    }
}
