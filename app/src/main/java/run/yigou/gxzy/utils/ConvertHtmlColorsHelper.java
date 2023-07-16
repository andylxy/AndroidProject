/*
 * 项目名: AndroidProject
 * 类名: convertHtmlColors.java
 * 包名: run.yigou.gxzy.utils.convertHtmlColors
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月16日 15:54:20
 * 上次修改时间: 2023年07月16日 15:54:20
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.utils;

import android.text.Spanned;

import androidx.core.text.HtmlCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  作者:  zhs
 *  时间:  2023-07-16 16:04:37
 *  包名:  run.yigou.gxzy.utils
 *  类名:  ConvertHtmlColorsHelper
 *  版本:  1.0
 *  描述: 颜色color: rgb(R, G, B) ,转为颜色整数值的十六进制字符串形式表现
 *
*/
public class ConvertHtmlColorsHelper {

    public static String convertHtmlColors(String HtmlText) {
        if (StringHelper.isEmpty(HtmlText)) return null;
      return convertColors(HtmlText);

    }

    private static String convertColors(String htmlText) {
        Pattern colorPattern = Pattern.compile("color:\\s*rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\);");
        Matcher colorMatcher = colorPattern.matcher(htmlText);

        StringBuffer sb = new StringBuffer();
        while (colorMatcher.find()) {
            int red = Integer.parseInt(colorMatcher.group(1).trim());
            int green = Integer.parseInt(colorMatcher.group(2).trim());
            int blue = Integer.parseInt(colorMatcher.group(3).trim());

            String hexColor = convertToHex(red, green, blue);
            String replacement = "color: #" + hexColor + ";";
            colorMatcher.appendReplacement(sb, replacement);
        }
        colorMatcher.appendTail(sb);

        return sb.toString();
    }

    private static String convertToHex(int red, int green, int blue) {
        String hexRed = Integer.toHexString(red);
        String hexGreen = Integer.toHexString(green);
        String hexBlue = Integer.toHexString(blue);

        return padZero(hexRed) + padZero(hexGreen) + padZero(hexBlue);
    }

    private static String padZero(String value) {
        return value.length() == 1 ? "0" + value : value;
    }

}
