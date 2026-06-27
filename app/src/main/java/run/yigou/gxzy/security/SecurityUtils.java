package run.yigou.gxzy.security;

import android.util.Log;

import run.yigou.gxzy.base.util.RC4Helper;

/**
 * 安全工具门面
 *
 * <p>SM2 加密通过 NativeBridge 走 NDK 实现，RC4 加密委托给 RC4Helper。
 * 此类替换原 library:crypto 模块的 SecurityUtils，消除对 crypto 模块的依赖。
 *
 * <p>设计原则：
 * <ul>
 *   <li>只保留运行时实际有调用者的方法</li>
 *   <li>SM 操作统一走 NDK（NativeBridge），Java 侧不实现 SM 算法</li>
 *   <li>密钥管理由 native key_store 负责，Java 侧不接触明文密钥</li>
 * </ul>
 */
public final class SecurityUtils {

    private static final String TAG = "SecurityUtils";

    private SecurityUtils() {}

    /**
     * 初始化安全模块（native 层密钥已在编译期就绪）
     *
     * <p>保留空壳供 AppApplication.onCreate() 调用，不再执行实际初始化。
     */
    public static void initSecurityManager() {
        Log.d(TAG, "SecurityUtils initialized (NDK mode)");
    }

    /**
     * SM2 加密门面
     *
     * <p>委托 NativeBridge.sm2Encrypt 走 NDK 实现。
     *
     * @param msgString 明文字符串
     * @return Base64 密文字符串，失败返回 null
     */
    public static String doSm2Encrypt(String msgString) {
        Log.d(TAG, "[NDK] doSm2Encrypt 进入，明文长度=" + msgString.length());

        byte[] input = msgString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String result = run.yigou.gxzy.nativecore.NativeBridge.sm2Encrypt(input);

        if (result != null && !result.isEmpty()) {
            Log.d(TAG, "[NDK] doSm2Encrypt 成功，密文长度=" + result.length()
                    + "，前20字符=" + result.substring(0, Math.min(20, result.length())));
        } else {
            Log.e(TAG, "[NDK] doSm2Encrypt 失败：NativeBridge.sm2Encrypt 返回空");
        }
        return result;
    }

    /**
     * RC4 加密
     */
    public static String rc4Encrypt(String plaintext) {
        return RC4Helper.encrypt(plaintext);
    }

    /**
     * RC4 解密
     */
    public static String rc4Decrypt(String encrypted) {
        return RC4Helper.decrypt(encrypted);
    }

    /**
     * 设置 RC4 密钥
     */
    public static void setRc4Key(String key) {
        RC4Helper.setKey(key);
    }
}
