package run.yigou.gxzy.Security.Cryptogram;

import android.util.Base64;
import android.util.Log;

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import run.yigou.gxzy.Security.Cryptogram.Sm.SM4CryptoUtil;
import run.yigou.gxzy.Security.Cryptogram.Sm.Sm4Context;

/**
 * 迁移自 SimpleEasy.Base.Cryptogram.SM4Util 的 Java 实现，保留默认密钥、补位模式等行为。
 */
public final class SM4Util {

    private static final String TAG = "SM4Util";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final String DEFAULT_KEY = "1514546261939465";
    private static final String DEFAULT_IV = "a000f00c0000e00d";
    private static final boolean DEFAULT_HEX_STRING = false;
    private static final Sm4CryptoEnum DEFAULT_MODE = Sm4CryptoEnum.ECB;

    private static volatile String configuredKey = DEFAULT_KEY;
    private static volatile String configuredIv = DEFAULT_IV;
    private static volatile boolean configuredHex = DEFAULT_HEX_STRING;
    private static volatile Sm4CryptoEnum configuredMode = DEFAULT_MODE;

    private SM4Util() {
        // 工具类不允许实例化
    }

    /**
     * 运行时注入新的默认参数（例如来自配置中心），便于与 .NET 端保持统一。
     */
    public static void configureDefaults(String key, String iv, boolean hexString, Sm4CryptoEnum mode) {
        if (key != null && !key.isEmpty()) {
            configuredKey = key;
        }
        if (iv != null && !iv.isEmpty()) {
            configuredIv = iv;
        }
        configuredHex = hexString;
        if (mode != null) {
            configuredMode = mode;
        }
    }

    public static String encrypt(String data) {
        return encrypt(buildRequest(data, configuredKey, configuredIv, configuredHex, configuredMode));
    }

    public static String decrypt(String cipherText) {
        return decrypt(buildRequest(cipherText, configuredKey, configuredIv, configuredHex, configuredMode));
    }

    public static String encrypt(String data, String key, String iv, boolean hexString, Sm4CryptoEnum mode) {
        return encrypt(buildRequest(data, key, iv, hexString, mode));
    }

    public static String decrypt(String cipherText, String key, String iv, boolean hexString, Sm4CryptoEnum mode) {
        return decrypt(buildRequest(cipherText, key, iv, hexString, mode));
    }

    private static Request buildRequest(String data, String key, String iv, boolean hexString, Sm4CryptoEnum mode) {
        Request request = new Request();
        request.data = data;
        request.key = key == null ? DEFAULT_KEY : key;
        request.iv = iv == null ? DEFAULT_IV : iv;
        request.hexString = hexString;
        request.cryptoMode = mode == null ? DEFAULT_MODE : mode;
        return request;
    }

    public static String encrypt(Request entity) {
        if (entity == null || isBlank(entity.data)) {
            return "";
        }
        return entity.cryptoMode == Sm4CryptoEnum.CBC ? encryptCBC(entity) : encryptECB(entity);
    }

    public static String decrypt(Request entity) {
        if (entity == null || isBlank(entity.data)) {
            return "";
        }
        return entity.cryptoMode == Sm4CryptoEnum.CBC ? decryptCBC(entity) : decryptECB(entity);
    }

    private static String encryptECB(Request entity) {
        try {
            Sm4Context ctx = new Sm4Context();
            byte[] keyBytes = entity.hexString ? Hex.decode(entity.key) : entity.key.getBytes(DEFAULT_CHARSET);
            SM4CryptoUtil.setKeyEnc(ctx, keyBytes);
            byte[] encrypted = SM4CryptoUtil.sm4CryptEcb(ctx, entity.data.getBytes(DEFAULT_CHARSET));
            return Hex.toHexString(encrypted).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            Log.e(TAG, "SM4 ECB encrypt failed", e);
            return "";
        }
    }

    private static String encryptCBC(Request entity) {
        try {
            Sm4Context ctx = new Sm4Context();
            byte[] keyBytes = entity.hexString ? Hex.decode(entity.key) : entity.key.getBytes(DEFAULT_CHARSET);
            byte[] ivBytes = entity.hexString ? Hex.decode(entity.iv) : entity.iv.getBytes(DEFAULT_CHARSET);
            SM4CryptoUtil.setKeyEnc(ctx, keyBytes);
            byte[] encrypted = SM4CryptoUtil.sm4CryptCbc(ctx, ivBytes, entity.data.getBytes(DEFAULT_CHARSET));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "SM4 CBC encrypt failed", e);
            return "";
        }
    }

    private static String decryptECB(Request entity) {
        try {
            Sm4Context ctx = new Sm4Context();
            ctx.mode = 0;
            byte[] keyBytes = entity.hexString ? Hex.decode(entity.key) : entity.key.getBytes(DEFAULT_CHARSET);
            SM4CryptoUtil.setKeyDec(ctx, keyBytes);
            byte[] decrypted = SM4CryptoUtil.sm4CryptEcb(ctx, Hex.decode(entity.data));
            return new String(decrypted, DEFAULT_CHARSET);
        } catch (Exception e) {
            Log.e(TAG, "SM4 ECB decrypt failed", e);
            return "";
        }
    }

    private static String decryptCBC(Request entity) {
        try {
            Sm4Context ctx = new Sm4Context();
            ctx.mode = 0;
            byte[] keyBytes = entity.hexString ? Hex.decode(entity.key) : entity.key.getBytes(DEFAULT_CHARSET);
            byte[] ivBytes = entity.hexString ? Hex.decode(entity.iv) : entity.iv.getBytes(DEFAULT_CHARSET);
            SM4CryptoUtil.setKeyDec(ctx, keyBytes);
            byte[] decrypted = SM4CryptoUtil.sm4CryptCbc(ctx, ivBytes, Base64.decode(entity.data, Base64.NO_WRAP));
            return new String(decrypted, DEFAULT_CHARSET);
        } catch (Exception e) {
            Log.e(TAG, "SM4 CBC decrypt failed", e);
            return "";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 供外部复用的请求载体，与 C# 类的字段保持一致。
     */
    public static final class Request {
        public String data;
        public String key;
        public String iv;
        public boolean hexString;
        public Sm4CryptoEnum cryptoMode;
    }

    /**
     * 工作模式描述枚举。
     */
    public enum Sm4CryptoEnum {
        ECB,
        CBC
    }
}
