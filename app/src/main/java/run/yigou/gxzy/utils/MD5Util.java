package run.yigou.gxzy.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密工具类
 */
public class MD5Util {
    private static final String[] HEX_ARRAY = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    /**
     * 计算指定字符串的MD5值
     *
     * @param originString 原始字符串
     * @return MD5加密后的字符串，如果加密失败则返回null
     */
    public static String CalcMD5(String originString) {
        if (originString == null) {
            return null;
        }

        try {
            // 创建具有MD5算法的信息摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 使用指定的字节数组对摘要进行最后更新，然后完成摘要计算
            byte[] bytes = md.digest(originString.getBytes());
            // 将得到的字节数组变成字符串返回
            String result = byteArrayToHex(bytes);
            return result.toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将字节数组转换成十六进制，并以字符串的形式返回
     * 128位是指二进制位。二进制太长，所以一般都改写成16进制，
     * 每一位16进制数可以代替4位二进制数，所以128位二进制数写成16进制就变成了128/4=32位。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String byteArrayToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(byteToHex(b));
        }
        return sb.toString();
    }

    /**
     * 将一个字节转换成十六进制，并以字符串的形式返回
     *
     * @param b 字节值
     * @return 十六进制字符串
     */
    private static String byteToHex(byte b) {
        int n = b;
        if (n < 0) {
            n = n + 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return HEX_ARRAY[d1] + HEX_ARRAY[d2];
    }
}