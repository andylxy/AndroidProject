package run.yigou.gxzy.Security.Cryptogram;

import android.util.Log;

/**
 * 对应 SimpleAdmin.Core.Utils.CryptogramUtil 的 Java 版本，统一对外暴露 SM2/SM4 常用方法。
 */
public final class CryptogramUtil {

    private static final String TAG = "CryptogramUtil";

    private CryptogramUtil() {
        // 工具类不允许实例化
    }

    public static String sm2Encrypt(String data) {
        try {
            if (!isBlank(data)) {
                return SM2Util.encrypt(data);
            }
        } catch (Exception e) {
            Log.e(TAG, "SM2 encrypt error", e);
        }
        return "";
    }

    public static String sm2Decrypt(String data) {
        try {
            if (!isBlank(data)) {
                return SM2Util.decrypt(data);
            }
        } catch (Exception e) {
            Log.e(TAG, "SM2 decrypt error", e);
        }
        return "";
    }

    public static String sm4Encrypt(String data) {
        if (!isBlank(data)) {
            return SM4Util.encrypt(data);
        }
        return "";
    }

    public static String sm4Decrypt(String data) {
        if (!isBlank(data)) {
            return SM4Util.decrypt(data);
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
