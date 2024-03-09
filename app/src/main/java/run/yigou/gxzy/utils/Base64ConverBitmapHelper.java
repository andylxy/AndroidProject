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

    // 假设你已经在布局文件中定义了一个ImageView，并且有一个Base64编码的字符串
    public static Bitmap displayBase64Image(String base64String) {
        // 首先检查可能存在的Data URI前缀
        if (!containsImageDataUri(base64String)) return null;
        // 移除可能存在的Data URI前缀
        String baseString = removeDataUriScheme(base64String);
        // 接下来，转换为字节数组
        byte[] imageBytes = base64ToByteArray(baseString);
        // 最后，转换为Bitmap并返回
        return byteArrayToBitmap(imageBytes);
    }

    public static boolean containsImageDataUri(String text) {
        return text != null && text.startsWith("data:image");
    }

    public static String removeDataUriScheme(String base64String) {
        if (base64String == null) {
            return null;
        }

        // 检查并移除image/png前缀
        if (base64String.startsWith("data:image/png;base64,")) {
            return base64String.substring("data:image/png;base64,".length());
        }

        // 以下为处理其他图像格式的示例，可以根据需要添加更多
        if (base64String.startsWith("data:image/jpeg;base64,")) {
            return base64String.substring("data:image/jpeg;base64,".length());
        }
        if (base64String.startsWith("data:image/gif;base64,")) {
            return base64String.substring("data:image/gif;base64,".length());
        }
        if (base64String.startsWith("data:image/webp;base64,")) {
            return base64String.substring("data:image/webp;base64,".length());
        }

        // 如果没有匹配的前缀，返回原字符串
        return base64String;
    }

    public static byte[] base64ToByteArray(String base64String) {
        return Base64.decode(base64String, Base64.DEFAULT);
    }

    public static Bitmap byteArrayToBitmap(byte[] byteArray) {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }
}
