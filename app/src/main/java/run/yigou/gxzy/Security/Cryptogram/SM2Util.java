package run.yigou.gxzy.Security.Cryptogram;

import android.util.Log;

import run.yigou.gxzy.Security.Cryptogram.Sm.SM2CryptoUtil;

/**
 * Java 版 SM2 门面类，对应 SimpleEasy.Base.Cryptogram.SM2Util。
 * 该类只保存静态公钥/私钥配置，并委派给 {@link SM2CryptoUtil} 完成实际计算。
 */
public final class SM2Util {

    private static final String TAG = "SM2Util";

    private static volatile String publicKey = "";
    private static volatile String privateKey = "";

    private SM2Util() {
        // 工具类不允许实例化
    }

    /**
     * 供外部注入新的公私钥，通常在启动阶段通过配置文件完成。
     */
    public static void configureKeys(String publicKeyHex, String privateKeyHex) {
        if (publicKeyHex != null) {
            publicKey = publicKeyHex;
        }
        if (privateKeyHex != null) {
            privateKey = privateKeyHex;
        }
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return "";
        }
        if (publicKey == null || publicKey.isEmpty()) {
            Log.e(TAG, "SM2 encrypt skipped: public key missing");
            return "";
        }
        try {
            return SM2CryptoUtil.encrypt(publicKey, plainText);
        } catch (Exception e) {
            Log.e(TAG, "SM2 encrypt failed", e);
            return "";
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.trim().isEmpty()) {
            return "";
        }
        if (privateKey == null || privateKey.isEmpty()) {
            Log.e(TAG, "SM2 decrypt skipped: private key missing");
            return "";
        }
        try {
            String normalizedCipher = cipherText.startsWith("04") ? cipherText : "04" + cipherText;
            return SM2CryptoUtil.decrypt(privateKey, normalizedCipher);
        } catch (Exception e) {
            Log.e(TAG, "SM2 decrypt failed", e);
            return "";
        }
    }
}
