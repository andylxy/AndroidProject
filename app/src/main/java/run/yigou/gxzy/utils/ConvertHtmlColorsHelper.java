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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML颜色转换工具类
 * 作者: zhs
 * 时间: 2023-07-16 16:04:37
 * 包名: run.yigou.gxzy.utils
 * 类名: ConvertHtmlColorsHelper
 * 版本: 1.0
 * 描述: 将HTML中的RGB颜色格式转换为十六进制颜色值
 */
public class ConvertHtmlColorsHelper {

    /**
     * 将HTML文本中的RGB颜色值转换为十六进制颜色值
     *
     * @param htmlText 包含RGB颜色值的HTML文本
     * @return 转换后的HTML文本，其中RGB颜色值已被替换为十六进制颜色值
     */
    public static String convertHtmlColors(String htmlText) {
        if (StringHelper.isEmpty(htmlText)) {
            return null;
        }
        return convertColors(htmlText);
    }

    /**
     * 转换HTML文本中的RGB颜色值为十六进制颜色值
     *
     * @param htmlText 包含RGB颜色值的HTML文本
     * @return 转换后的HTML文本
     */
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

    /**
     * 将RGB颜色值转换为十六进制颜色值
     *
     * @param red   红色分量(0-255)
     * @param green 绿色分量(0-255)
     * @param blue  蓝色分量(0-255)
     * @return 十六进制颜色值(如:"FF0000")
     */
    private static String convertToHex(int red, int green, int blue) {
        String hexRed = Integer.toHexString(red);
        String hexGreen = Integer.toHexString(green);
        String hexBlue = Integer.toHexString(blue);

        return padZero(hexRed) + padZero(hexGreen) + padZero(hexBlue);
    }

    /**
     * 为单个十六进制字符补零
     *
     * @param value 十六进制字符
     * @return 补零后的十六进制字符
     */
    private static String padZero(String value) {
        if (value == null) {
            return "00";
        }
        return value.length() == 1 ? "0" + value : value;
    }
}