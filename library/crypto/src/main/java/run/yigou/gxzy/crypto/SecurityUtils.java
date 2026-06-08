package run.yigou.gxzy.crypto;

import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * 安全管理器单例，对应 SimpleAdmin.Core.Utils.SecurityUtils 的 Android 版本。
 * <p>
 * 该类主要封装 SM2 公钥初始化和加解密门面调用，避免业务代码直接依赖底层实现。
 * 硬编码公钥已替换为外部可配置的默认值，启动阶段建议通过 {@link #initSecurityManager(String)}
 * 以配置中心下发的公钥覆盖。
 */
public final class SecurityUtils {

    private static final String TAG = "SecurityUtils";

    /**
     * 默认 SM2 完整公钥（16 进制，以 04 开头），用于快速启动。
     * 生产环境建议通过 initSecurityManager(publicKey) 替换。
     */
    private static final String DEFAULT_PUBLIC_KEY =
            "04CF658C65FB80CB5C7B91D3BD881521C2BD421202D29812785322F6366B8856B62D38D3AB5B5C299B03DD2EC0033370875A2787C6222E801AF87DDA53093322E2";

    private static volatile SecurityUtils instance;
    private volatile String currentPublicKey = "";
    private volatile String currentPrivateKey = "";

    static {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(new BouncyCastleProvider());
        Log.i(TAG, "BouncyCastleProvider registered in SecurityUtils");
    }

    private SecurityUtils() {
        // 工具类不允许实例化
    }

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
     * 初始化安全管理器，使用默认公钥。
     */
    public static void initSecurityManager() {
        try {
            getInstance().configureSm2Keys(normalizeFullPublicKey(DEFAULT_PUBLIC_KEY), null);
            Log.d(TAG, "安全管理器初始化成功（默认公钥）");
        } catch (Exception e) {
            Log.e(TAG, "安全管理器初始化失败", e);
        }
    }

    /**
     * 初始化安全管理器，使用指定公钥覆盖默认值。
     */
    public static void initSecurityManager(String publicKey) {
        try {
            if (publicKey != null && !publicKey.isEmpty()) {
                getInstance().configureSm2Keys(normalizeFullPublicKey(publicKey), null);
                Log.d(TAG, "安全管理器初始化成功（自定义公钥）");
            } else {
                initSecurityManager();
            }
        } catch (Exception e) {
            Log.e(TAG, "安全管理器初始化失败", e);
        }
    }

    /**
     * 使用 X, Y 坐标拼接完整公钥并初始化。
     */
    public static void initSM2PublicKey(String publicKeyX, String publicKeyY) {
        getInstance().configureSm2Keys(buildFullPublicKey(publicKeyX, publicKeyY), null);
    }

    /**
     * 使用 x,y 组合格式公钥初始化。
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
     * 使用完整公钥格式初始化。
     */
    public static void initSM2PublicKeyWithFullFormat(String fullPublicKey) {
        getInstance().configureSm2Keys(normalizeFullPublicKey(fullPublicKey), null);
    }

    /**
     * 初始化完整密钥对。
     */
    public static void initSM2Keys(String publicKeyX, String publicKeyY, String privateKeyD) {
        getInstance().configureSm2Keys(buildFullPublicKey(publicKeyX, publicKeyY), privateKeyD);
    }

    /**
     * 使用组合公钥格式初始化完整密钥对。
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
     * 使用完整公钥格式初始化完整密钥对。
     */
    public static void initSM2KeysWithFullPublicKey(String fullPublicKey, String privateKeyD) {
        getInstance().configureSm2Keys(normalizeFullPublicKey(fullPublicKey), privateKeyD);
    }

    /**
     * SM2 加密门面调用。
     */
    public static String doSm2Encrypt(String msgString) {
        return CryptogramUtil.sm2Encrypt(msgString);
    }

    /**
     * SM2 解密门面调用。
     */
    public static String doSm2Decrypt(String encryptedData) {
        return CryptogramUtil.sm2Decrypt(encryptedData);
    }

    /**
     * SM2 签名门面调用（需要私钥）。
     */
    public static String doSignature(String data) {
        return getInstance().signWithSm2PrivateKey(data);
    }

    /**
     * SM2 验签门面调用（只需要公钥）。
     */
    public static boolean doVerifySignature(String data, String signature) {
        return getInstance().verifyWithSm2PublicKey(data, signature);
    }

    /**
     * SM4 ECB 加密门面调用。
     */
    public static String doSm4Encrypt(String msgString) {
        return CryptogramUtil.sm4Encrypt(msgString);
    }

    /**
     * SM4 ECB 解密门面调用。
     */
    public static String doSm4Decrypt(String encryptedData) {
        return CryptogramUtil.sm4Decrypt(encryptedData);
    }

    /**
     * SM4 CBC 加密门面调用。
     */
    public static String doSm4CbcEncrypt(String msgString) {
        return SM4Util.encrypt(msgString, null, null, false, SM4Util.Sm4CryptoEnum.CBC);
    }

    /**
     * SM4 CBC 解密门面调用。
     */
    public static String doSm4CbcDecrypt(String encryptedData) {
        return SM4Util.decrypt(encryptedData, null, null, false, SM4Util.Sm4CryptoEnum.CBC);
    }

    /**
     * SM4 ECB 加密（自定义密钥）。
     */
    public static String doSm4Encrypt(String msgString, String key) {
        return SM4Util.encrypt(msgString, key, null, false, SM4Util.Sm4CryptoEnum.ECB);
    }

    /**
     * SM4 ECB 解密（自定义密钥）。
     */
    public static String doSm4Decrypt(String encryptedData, String key) {
        return SM4Util.decrypt(encryptedData, key, null, false, SM4Util.Sm4CryptoEnum.ECB);
    }

    /**
     * SM4 CBC 加密（自定义密钥和 IV）。
     */
    public static String doSm4CbcEncrypt(String msgString, String key, String iv) {
        return SM4Util.encrypt(msgString, key, iv, false, SM4Util.Sm4CryptoEnum.CBC);
    }

    /**
     * SM4 CBC 解密（自定义密钥和 IV）。
     */
    public static String doSm4CbcDecrypt(String encryptedData, String key, String iv) {
        return SM4Util.decrypt(encryptedData, key, iv, false, SM4Util.Sm4CryptoEnum.CBC);
    }

    /**
     * 使用私钥执行 SM2 签名（内部方法）。
     */
    private String signWithSm2PrivateKey(String data) {
        if (currentPrivateKey == null || currentPrivateKey.isEmpty()) {
            Log.e(TAG, "SM2 sign skipped: private key missing");
            return "";
        }
        Log.w(TAG, "SM2 sign not yet implemented in Android migration");
        return "";
    }

    /**
     * 使用公钥执行 SM2 验签（内部方法）。
     */
    private boolean verifyWithSm2PublicKey(String data, String signature) {
        if (currentPublicKey == null || currentPublicKey.isEmpty()) {
            Log.e(TAG, "SM2 verify skipped: public key missing");
            return false;
        }
        Log.w(TAG, "SM2 verify not yet implemented in Android migration");
        return false;
    }

    /**
     * 配置 SM2 公私钥，由各类 initSM2 方法统一调用。
     */
    void configureSm2Keys(String publicKeyHex, String privateKeyHex) {
        if (publicKeyHex != null && !publicKeyHex.isEmpty()) {
            currentPublicKey = publicKeyHex;
        }
        if (privateKeyHex != null && !privateKeyHex.isEmpty()) {
            currentPrivateKey = privateKeyHex;
        }
        // 同时注入到 SM2Util，保证 CryptogramUtil.sm2Encrypt/Decrypt 能够直接使用
        SM2Util.configureKeys(currentPublicKey, currentPrivateKey);
    }

    private static String buildFullPublicKey(String x, String y) {
        if (x == null || y == null) {
            return "";
        }
        return "04" + x + y;
    }

    private static String normalizeFullPublicKey(String fullPublicKey) {
        if (fullPublicKey == null) {
            return "";
        }
        String normalized = fullPublicKey.trim();
        return normalized.startsWith("04") ? normalized : "04" + normalized;
    }

    /**
     * RC4 加密（静态快捷方法，委托给 RC4Helper）。
     */
    public static String rc4Encrypt(String plaintext) {
        return RC4Helper.encrypt(plaintext);
    }

    /**
     * RC4 解密（静态快捷方法，委托给 RC4Helper）。
     */
    public static String rc4Decrypt(String encrypted) {
        return RC4Helper.decrypt(encrypted);
    }

    /**
     * 设置 RC4 密钥。
     */
    public static void setRc4Key(String key) {
        RC4Helper.setKey(key);
    }
}
