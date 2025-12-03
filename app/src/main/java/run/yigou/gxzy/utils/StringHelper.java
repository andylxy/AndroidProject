package run.yigou.gxzy.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串处理工具类
 */
public class StringHelper {

    /**
     * 获取UUID字符串
     *
     * @return UUID字符串
     */
    public static String getUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 截取指定字符串之后的部分
     *
     * @param str 原始字符串
     * @param sub 分割字符串
     * @return 截取后的字符串
     */
    public static String substring(String str, String sub) {
        if (str == null || sub == null) {
            return null;
        }

        int commaIndex = str.indexOf(sub);
        if (commaIndex != -1) { // 检查分割字符串是否存在
            return str.substring(commaIndex + sub.length()); // 从分割字符串后面的位置开始截取字符串
        }
        return null;
    }

    /**
     * 判断字符串是否为空
     *
     * @param str 待检查字符串
     * @return true 为空，false 不为空
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * 是否是Emoji表情符
     *
     * @param string 字符串
     * @return 是否包含Emoji
     */
    public static boolean isEmoji(String string) {
        if (string == null) {
            return false;
        }
        
        Pattern p = Pattern.compile("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
                Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(string);
        return m.find();
    }

    /**
     * UTF-8字符集编码
     *
     * @param encoded 待编码字符串
     * @return 编码后的字符串
     */
    public static String encode(String encoded) {
        return encode(encoded, "UTF-8");
    }

    /**
     * 指定字符集编码
     *
     * @param encoded     待编码字符串
     * @param charsetName 字符集名称
     * @return 编码后的字符串
     */
    public static String encode(String encoded, String charsetName) {
        if (encoded == null || charsetName == null) {
            return encoded;
        }
        
        try {
            return URLEncoder.encode(encoded, charsetName);
        } catch (Exception e) {
            e.printStackTrace();
            return encoded;
        }
    }

    /**
     * UTF-8字符集解码
     *
     * @param decoded 待解码字符串
     * @return 解码后的字符串
     */
    public static String decode(String decoded) {
        return decode(decoded, "UTF-8");
    }

    /**
     * 指定字符集解码
     *
     * @param decoded     待解码字符串
     * @param charsetName 字符集名称
     * @return 解码后的字符串
     */
    public static String decode(String decoded, String charsetName) {
        if (decoded == null || charsetName == null) {
            return decoded;
        }
        
        try {
            return URLDecoder.decode(decoded, charsetName);
        } catch (Exception e) {
            e.printStackTrace();
            return decoded;
        }
    }

    /**
     * 生成指定长度的随机数字和字母组合
     *
     * @param length 生成随机数的长度
     * @return 随机字符串
     */
    public static String getStringRandom(int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder val = new StringBuilder();
        Random random = new Random();

        // 参数length，表示生成几位随机数
        for (int i = 0; i < length; i++) {
            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
            // 输出字母还是数字
            if ("char".equalsIgnoreCase(charOrNum)) {
                // 输出是大写字母还是小写字母
                int temp = random.nextInt(2) % 2 == 0 ? 65 : 97;
                val.append((char) (random.nextInt(26) + temp));
            } else if ("num".equalsIgnoreCase(charOrNum)) {
                val.append(random.nextInt(10));
            }
        }
        return val.toString();
    }

    /**
     * 从JID中提取用户名
     *
     * @param jid JID字符串
     * @return 用户名
     */
    public static String jidToUsername(String jid) {
        if (jid == null) {
            return "";
        }
        
        if (jid.contains("@")) {
            return jid.substring(0, jid.indexOf("@"));
        } else {
            return jid;
        }
    }

    /**
     * 检查传入的字符串是否为空（包括只包含空格的情况）
     *
     * @param str 待检查字符串
     * @return true 为空，false 不为空
     */
    public static boolean isEmpty(String str) {
        if (str != null) {
            str = str.replace(" ", "");
        }
        return str == null || str.equals("");
    }

    /**
     * 检查字符串是否不等于"<p><br></p>"
     *
     * @param str 字符串
     * @return true 不等于，false 等于
     */
    public static boolean isNotEquals(String str) {
        if (str != null) {
            str = str.replace(" ", "");
        }
        return !Objects.equals(str, "<p><br></p>");
    }

    /**
     * 缩减字符串长度
     *
     * @param strLocation 原始字符串
     * @param maxLength   最大长度
     * @return 缩减后的字符串
     */
    public static String reduceString(String strLocation, int maxLength) {
        if (strLocation == null) {
            return null;
        }
        
        if (strLocation.length() > maxLength) {
            return strLocation.substring(0, maxLength) + "...";
        } else {
            return strLocation;
        }
    }

    /**
     * 比较两个字符串是否相等或者都为空
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return true 相等，false 不相等
     */
    public static boolean isNotEquals(String str1, String str2) {
        if (isEmpty(str1) && isEmpty(str2)) {
            return true;
        } else {
            return !isEmpty(str1) && !isEmpty(str2) && str1.equals(str2);
        }
    }
}