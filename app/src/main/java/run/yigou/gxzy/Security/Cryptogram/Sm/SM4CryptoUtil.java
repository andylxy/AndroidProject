package run.yigou.gxzy.Security.Cryptogram.Sm;

import java.util.ArrayList;
import java.util.List;

/**
 * Java 版本的 SM4 算法实现，严格按照 SimpleEasy.Base.Cryptogram.Sm.SM4CryptoUtil 的 C# 逻辑迁移。
 * <p>
 * 所有位运算都以 32 位无符号整数的效果执行，因此在 Java 中使用 long 保存中间结果以避免符号位干扰。
 */
public final class SM4CryptoUtil {

    private SM4CryptoUtil() {
        // 工具类不允许实例化
    }

    private static long getULongByBe(byte[] b, int i) {
        return ((long) (b[i] & 0xff) << 24)
                | ((long) (b[i + 1] & 0xff) << 16)
                | ((long) (b[i + 2] & 0xff) << 8)
                | (long) (b[i + 3] & 0xff);
    }

    private static void putULongToBe(long n, byte[] b, int i) {
        b[i] = (byte) ((n >> 24) & 0xFF);
        b[i + 1] = (byte) ((n >> 16) & 0xFF);
        b[i + 2] = (byte) ((n >> 8) & 0xFF);
        b[i + 3] = (byte) (n & 0xFF);
    }

    private static long rotl(long x, int n) {
        long value = (x & 0xFFFFFFFFL);
        return ((value << n) | (value >>> (32 - n))) & 0xFFFFFFFFL;
    }

    private static void swap(long[] sk, int i) {
        long t = sk[i];
        sk[i] = sk[31 - i];
        sk[31 - i] = t;
    }

    private static final byte[] SBOX_TABLE = new byte[]{
            (byte) 0xd6, (byte) 0x90, (byte) 0xe9, (byte) 0xfe, (byte) 0xcc, (byte) 0xe1, 0x3d, (byte) 0xb7,
            0x16, (byte) 0xb6, 0x14, (byte) 0xc2, 0x28, (byte) 0xfb, 0x2c, 0x05,
            0x2b, 0x67, (byte) 0x9a, 0x76, 0x2a, (byte) 0xbe, 0x04, (byte) 0xc3,
            (byte) 0xaa, 0x44, 0x13, 0x26, 0x49, (byte) 0x86, 0x06, (byte) 0x99,
            (byte) 0x9c, 0x42, 0x50, (byte) 0xf4, (byte) 0x91, (byte) 0xef, (byte) 0x98, 0x7a,
            0x33, 0x54, 0x0b, 0x43, (byte) 0xed, (byte) 0xcf, (byte) 0xac, 0x62,
            (byte) 0xe4, (byte) 0xb3, 0x1c, (byte) 0xa9, (byte) 0xc9, 0x08, (byte) 0xe8, (byte) 0x95,
            (byte) 0x80, (byte) 0xdf, (byte) 0x94, (byte) 0xfa, 0x75, (byte) 0x8f, 0x3f, (byte) 0xa6,
            0x47, 0x07, (byte) 0xa7, (byte) 0xfc, (byte) 0xf3, 0x73, 0x17, (byte) 0xba,
            (byte) 0x83, 0x59, 0x3c, 0x19, (byte) 0xe6, (byte) 0x85, 0x4f, (byte) 0xa8,
            0x68, 0x6b, (byte) 0x81, (byte) 0xb2, 0x71, 0x64, (byte) 0xda, (byte) 0x8b,
            (byte) 0xf8, (byte) 0xeb, 0x0f, 0x4b, 0x70, 0x56, (byte) 0x9d, 0x35,
            0x1e, 0x24, 0x0e, 0x5e, 0x63, 0x58, (byte) 0xd1, (byte) 0xa2,
            0x25, 0x22, 0x7c, 0x3b, 0x01, 0x21, 0x78, (byte) 0x87,
            (byte) 0xd4, 0x00, 0x46, 0x57, (byte) 0x9f, (byte) 0xd3, 0x27, 0x52,
            0x4c, 0x36, 0x02, (byte) 0xe7, (byte) 0xa0, (byte) 0xc4, (byte) 0xc8, (byte) 0x9e,
            (byte) 0xea, (byte) 0xbf, (byte) 0x8a, (byte) 0xd2, 0x40, (byte) 0xc7, 0x38, (byte) 0xb5,
            (byte) 0xa3, (byte) 0xf7, (byte) 0xf2, (byte) 0xce, (byte) 0xf9, 0x61, 0x15, (byte) 0xa1,
            (byte) 0xe0, (byte) 0xae, 0x5d, (byte) 0xa4, (byte) 0x9b, 0x34, 0x1a, 0x55,
            (byte) 0xad, (byte) 0x93, 0x32, 0x30, (byte) 0xf5, (byte) 0x8c, (byte) 0xb1, (byte) 0xe3,
            0x1d, (byte) 0xf6, (byte) 0xe2, 0x2e, (byte) 0x82, 0x66, (byte) 0xca, 0x60,
            (byte) 0xc0, 0x29, 0x23, (byte) 0xab, 0x0d, 0x53, 0x4e, 0x6f,
            (byte) 0xd5, (byte) 0xdb, 0x37, 0x45, (byte) 0xde, (byte) 0xfd, (byte) 0x8e, 0x2f,
            0x03, (byte) 0xff, 0x6a, 0x72, 0x6d, 0x6c, 0x5b, 0x51,
            (byte) 0x8d, 0x1b, (byte) 0xaf, (byte) 0x92, (byte) 0xbb, (byte) 0xdd, (byte) 0xbc, 0x7f,
            0x11, (byte) 0xd9, 0x5c, 0x41, 0x1f, 0x10, 0x5a, (byte) 0xd8,
            0x0a, (byte) 0xc1, 0x31, (byte) 0x88, (byte) 0xa5, (byte) 0xcd, 0x7b, (byte) 0xbd,
            0x2d, 0x74, (byte) 0xd0, 0x12, (byte) 0xb8, (byte) 0xe5, (byte) 0xb4, (byte) 0xb0,
            (byte) 0x89, 0x69, (byte) 0x97, 0x4a, 0x0c, (byte) 0x96, 0x77, 0x7e,
            0x65, (byte) 0xb9, (byte) 0xf1, 0x09, (byte) 0xc5, 0x6e, (byte) 0xc6, (byte) 0x84,
            0x18, (byte) 0xf0, 0x7d, (byte) 0xec, 0x3a, (byte) 0xdc, 0x4d, 0x20,
            0x79, (byte) 0xee, 0x5f, 0x3e, (byte) 0xd7, (byte) 0xcb, 0x39, 0x48
    };

    private static final long[] FK = new long[]{
            0xa3b1bac6L, 0x56aa3350L, 0x677d9197L, 0xb27022dcL
    };

    private static final long[] CK = new long[]{
            0x00070e15L, 0x1c232a31L, 0x383f464dL, 0x545b6269L,
            0x70777e85L, 0x8c939aa1L, 0xa8afb6bdL, 0xc4cbd2d9L,
            0xe0e7eef5L, 0xfc030a11L, 0x181f262dL, 0x343b4249L,
            0x50575e65L, 0x6c737a81L, 0x888f969dL, 0xa4abb2b9L,
            0xc0c7ced5L, 0xdce3eaf1L, 0xf8ff060dL, 0x141b2229L,
            0x30373e45L, 0x4c535a61L, 0x686f767dL, 0x848b9299L,
            0xa0a7aeb5L, 0xbcc3cad1L, 0xd8dfe6edL, 0xf4fb0209L,
            0x10171e25L, 0x2c333a41L, 0x484f565dL, 0x646b7279L
    };

    private static byte sm4Sbox(int in) {
        int index = in & 0xFF;
        return SBOX_TABLE[index];
    }

    private static long sm4Lt(long ka) {
        byte[] a = new byte[4];
        byte[] b = new byte[4];
        putULongToBe(ka, a, 0);
        b[0] = sm4Sbox(a[0]);
        b[1] = sm4Sbox(a[1]);
        b[2] = sm4Sbox(a[2]);
        b[3] = sm4Sbox(a[3]);
        long bb = getULongByBe(b, 0);
        return bb ^ rotl(bb, 2) ^ rotl(bb, 10) ^ rotl(bb, 18) ^ rotl(bb, 24);
    }

    private static long sm4F(long x0, long x1, long x2, long x3, long rk) {
        return x0 ^ sm4Lt(x1 ^ x2 ^ x3 ^ rk);
    }

    private static long sm4CalciRk(long ka) {
        byte[] a = new byte[4];
        byte[] b = new byte[4];
        putULongToBe(ka, a, 0);
        b[0] = sm4Sbox(a[0]);
        b[1] = sm4Sbox(a[1]);
        b[2] = sm4Sbox(a[2]);
        b[3] = sm4Sbox(a[3]);
        long bb = getULongByBe(b, 0);
        return bb ^ rotl(bb, 13) ^ rotl(bb, 23);
    }

    private static void setKey(long[] sk, byte[] key) {
        long[] mk = new long[4];
        mk[0] = getULongByBe(key, 0);
        mk[1] = getULongByBe(key, 4);
        mk[2] = getULongByBe(key, 8);
        mk[3] = getULongByBe(key, 12);
        long[] k = new long[36];
        k[0] = mk[0] ^ FK[0];
        k[1] = mk[1] ^ FK[1];
        k[2] = mk[2] ^ FK[2];
        k[3] = mk[3] ^ FK[3];
        for (int i = 0; i < 32; i++) {
            k[i + 4] = k[i] ^ sm4CalciRk(k[i + 1] ^ k[i + 2] ^ k[i + 3] ^ CK[i]);
            sk[i] = k[i + 4] & 0xFFFFFFFFL;
        }
    }

    private static void sm4OneRound(long[] sk, byte[] input, byte[] output) {
        long[] ulbuf = new long[36];
        ulbuf[0] = getULongByBe(input, 0);
        ulbuf[1] = getULongByBe(input, 4);
        ulbuf[2] = getULongByBe(input, 8);
        ulbuf[3] = getULongByBe(input, 12);
        for (int i = 0; i < 32; i++) {
            ulbuf[i + 4] = sm4F(ulbuf[i], ulbuf[i + 1], ulbuf[i + 2], ulbuf[i + 3], sk[i]);
        }
        putULongToBe(ulbuf[35], output, 0);
        putULongToBe(ulbuf[34], output, 4);
        putULongToBe(ulbuf[33], output, 8);
        putULongToBe(ulbuf[32], output, 12);
    }

    private static byte[] padding(byte[] input, int mode) {
        if (input == null) {
            return null;
        }
        if (mode == 1) {
            int p = 16 - input.length % 16;
            byte[] ret = new byte[input.length + p];
            System.arraycopy(input, 0, ret, 0, input.length);
            for (int i = 0; i < p; i++) {
                ret[input.length + i] = (byte) p;
            }
            return ret;
        } else {
            int p = input[input.length - 1];
            byte[] ret = new byte[input.length - p];
            System.arraycopy(input, 0, ret, 0, input.length - p);
            return ret;
        }
    }

    public static void setKeyEnc(Sm4Context ctx, byte[] key) {
        ctx.mode = 1;
        setKey(ctx.key, key);
    }

    public static void setKeyDec(Sm4Context ctx, byte[] key) {
        ctx.mode = 0;
        setKey(ctx.key, key);
        for (int i = 0; i < 16; i++) {
            swap(ctx.key, i);
        }
    }

    public static byte[] sm4CryptEcb(Sm4Context ctx, byte[] input) {
        byte[] data = input;
        if (ctx.isPadding && ctx.mode == 1) {
            data = padding(input, 1);
        }
        int length = data.length;
        byte[] output = new byte[length];
        for (int i = 0; length > 0; length -= 16, i++) {
            byte[] inBytes = new byte[16];
            byte[] outBytes = new byte[16];
            int copyLen = Math.min(16, length);
            System.arraycopy(data, i * 16, inBytes, 0, copyLen);
            sm4OneRound(ctx.key, inBytes, outBytes);
            System.arraycopy(outBytes, 0, output, i * 16, copyLen);
        }
        if (ctx.isPadding && ctx.mode == 0) {
            return padding(output, 0);
        }
        return output;
    }

    public static byte[] sm4CryptCbc(Sm4Context ctx, byte[] iv, byte[] input) {
        byte[] data = input;
        if (ctx.isPadding && ctx.mode == 1) {
            data = padding(input, 1);
        }
        int length = data.length;
        byte[] ivCopy = new byte[16];
        System.arraycopy(iv, 0, ivCopy, 0, 16);
        List<Byte> output = new ArrayList<>();
        if (ctx.mode == 1) {
            for (int i = 0; length > 0; length -= 16, i++) {
                byte[] inBytes = new byte[16];
                byte[] outBytes = new byte[16];
                byte[] xorBytes = new byte[16];
                int copyLen = Math.min(16, length);
                System.arraycopy(data, i * 16, inBytes, 0, copyLen);
                for (int j = 0; j < 16; j++) {
                    xorBytes[j] = (byte) (inBytes[j] ^ ivCopy[j]);
                }
                sm4OneRound(ctx.key, xorBytes, outBytes);
                System.arraycopy(outBytes, 0, ivCopy, 0, 16);
                for (byte outByte : outBytes) {
                    output.add(outByte);
                }
            }
        } else {
            byte[] temp = new byte[16];
            for (int i = 0; length > 0; length -= 16, i++) {
                byte[] inBytes = new byte[16];
                byte[] outBytes = new byte[16];
                byte[] plainBytes = new byte[16];
                int copyLen = Math.min(16, length);
                System.arraycopy(data, i * 16, inBytes, 0, copyLen);
                System.arraycopy(inBytes, 0, temp, 0, 16);
                sm4OneRound(ctx.key, inBytes, outBytes);
                for (int j = 0; j < 16; j++) {
                    plainBytes[j] = (byte) (outBytes[j] ^ ivCopy[j]);
                }
                System.arraycopy(temp, 0, ivCopy, 0, 16);
                for (byte plainByte : plainBytes) {
                    output.add(plainByte);
                }
            }
        }
        byte[] result = new byte[output.size()];
        for (int i = 0; i < output.size(); i++) {
            result[i] = output.get(i);
        }
        if (ctx.isPadding && ctx.mode == 0) {
            return padding(result, 0);
        }
        return result;
    }
}
