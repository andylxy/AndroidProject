package run.yigou.gxzy.Security.Cryptogram.Sm;

/**
 * 复刻 C# 版本的 Sm4Context，用于在加解密过程中共享模式、密钥和是否补位等控制信息。
 */
public class Sm4Context {

    /**
     * 1 表示加密，0 表示解密。
     */
    public int mode = 1;

    /**
     * 是否需要按照 PKCS#7 规则补齐 16 字节块。
     */
    public boolean isPadding = true;

    /**
     * 32 轮子密钥数组，单位同 C# 版本使用的 long。
     */
    public long[] key = new long[32];
}
