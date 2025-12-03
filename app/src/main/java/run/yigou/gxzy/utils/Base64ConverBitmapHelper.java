/*
 * 项目名: AndroidProject
 * 类名: Base64ConverBitmapHelper.java
 * 包名: run.yigou.gxzy.utils.Base64ConverBitmapHelper
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月09日 09:28:27
 * 上次修改时间: 2024年03月09日 09:28:27
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;

/**
 * 版本:  1.0
 * 描述: Base64编码的字符串转换为Bitmap
 */
public class Base64ConverBitmapHelper {

    // 支持的图片类型前缀
    private static final String PNG_PREFIX = "data:image/png;base64,";
    private static final String JPEG_PREFIX = "data:image/jpeg;base64,";
    private static final String GIF_PREFIX = "data:image/gif;base64,";
    private static final String WEBP_PREFIX = "data:image/webp;base64,";

    /**
     * 将Base64编码的字符串转换为Bitmap
     *
     * @param base64String Base64编码的字符串
     * @return Bitmap对象，如果转换失败则返回null
     */
    public static Bitmap getBase64ToImage(String base64String) {
        // 检查输入是否为空
        if (base64String == null) {
            return null;
        }

        // 检查是否包含Data URI前缀
        if (containsImageDataUri(base64String)) {
            return null;
        }

        // 移除可能存在的Data URI前缀
        String baseString = removeDataUriScheme(base64String);

        // 将Base64字符串转换为字节数组
        byte[] imageBytes = base64ToByteArray(baseString);

        // 将字节数组转换为Bitmap并返回
        return byteArrayToBitmap(imageBytes);
    }

    /**
     * 检查字符串是否包含图片的Data URI前缀
     *
     * @param text 待检查的字符串
     * @return 如果包含图片Data URI前缀则返回true，否则返回false
     */
    private static boolean containsImageDataUri(String text) {
        return text.startsWith("data:image");
    }

    /**
     * 移除图片Data URI前缀
     *
     * @param base64String 包含Data URI前缀的Base64字符串
     * @return 移除前缀后的Base64字符串
     */
    private static String removeDataUriScheme(String base64String) {
        if (base64String == null) {
            return null;
        }

        // 检查并移除各种图像格式的前缀
        if (base64String.startsWith(PNG_PREFIX)) {
            return base64String.substring(PNG_PREFIX.length());
        }

        if (base64String.startsWith(JPEG_PREFIX)) {
            return base64String.substring(JPEG_PREFIX.length());
        }

        if (base64String.startsWith(GIF_PREFIX)) {
            return base64String.substring(GIF_PREFIX.length());
        }

        if (base64String.startsWith(WEBP_PREFIX)) {
            return base64String.substring(WEBP_PREFIX.length());
        }

        // 如果没有匹配的前缀，返回原字符串
        return base64String;
    }

    /**
     * 将Base64字符串转换为字节数组
     *
     * @param base64String Base64编码的字符串
     * @return 字节数组
     */
    private static byte[] base64ToByteArray(String base64String) {
        return Base64.decode(base64String, Base64.DEFAULT);
    }

    /**
     * 将字节数组转换为Bitmap
     *
     * @param byteArray 字节数组
     * @return Bitmap对象
     */
    private static Bitmap byteArrayToBitmap(byte[] byteArray) {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }
}