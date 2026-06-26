package run.yigou.gxzy.nativecore;

import androidx.annotation.Nullable;

/**
 * Native 核心能力桥接
 *
 * <p>模块对外唯一入口。所有 native 能力通过此类暴露给 Java 层。
 * App 或其他模块只需 {@code implementation} 依赖后即可调用。
 *
 * <p>设计原则：
 * <ul>
 *   <li>只暴露 static native 方法，禁止实例化</li>
 *   <li>方法返回 null 或 false 表示 native 层不可用，调用方应回退</li>
 *   <li>密钥在 native 层管理，Java 侧不接触明文密钥</li>
 * </ul>
 */
public final class NativeBridge {

    static {
        System.loadLibrary("native_core");
    }

    /** SM2 加密（公钥由 native 层管理，返回 Base64 密文字符串） */
    @Nullable
    public static native String sm2Encrypt(byte[] data);

    /** SM4 加密 */
    @Nullable
    public static native byte[] sm4Encrypt(byte[] data, byte[] iv);

    /** HMAC-SHA256 签名（返回 Base64 编码结果） */
    @Nullable
    public static native String hmacSha256(String data, String key);

    /** 完整请求签名（构造原文 → HMAC-SHA256 → Base64） */
    @Nullable
    public static native String signRequest(String method, String host, String path,
        String timestamp, String nonce, String secret);

    /** 检测调试器是否附加 */
    public static native boolean isDebuggerAttached();

    /** 设置 API 签名密钥（立即存入 native 层，Java 堆中主动清除） */
    public static native void setSigningKey(@Nullable String key);

    /** 完整请求签名（密钥由 native 层管理，无需 Java 传入） */
    @Nullable
    public static native String signRequestInternal(String method, String host, String path,
        String timestamp, String nonce);

    private NativeBridge() {}
}