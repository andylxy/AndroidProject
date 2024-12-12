package run.yigou.gxzy.utils;

import com.github.gzuliyujiang.rsautils.RC4Utils;
import com.hjq.http.EasyLog;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import run.yigou.gxzy.common.AppConst;

public class RC4Helper {
    /**
     * 字符编码
     */
    private static final Charset CHARSET = StandardCharsets.UTF_8;


    /**
     *  解密
     * @param encryptedData 解密Base64字符串
     * @return 返回解密数据
     */
    public static String decrypt(String encryptedData) {

        byte[] decryptedData = RC4Utils.decryptFromBase64(encryptedData,  AppConst.rc4_SecretKey);
        if (decryptedData == null) {

            return null;
        }
        return  new String(decryptedData, CHARSET);
    }

    /**
     * @param encryptData 加密数据
     * @return 返回加密后 Base64字符串
     */
    public static String encrypt(String encryptData) {

        if (encryptData == null) return null;

        return RC4Utils.encryptToBase64(encryptData.getBytes(CHARSET), AppConst.rc4_SecretKey);

    }
}
