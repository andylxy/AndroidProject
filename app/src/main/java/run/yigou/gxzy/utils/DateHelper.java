package run.yigou.gxzy.utils;

import android.annotation.SuppressLint;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zhao on 2016/09/09.
 * 日期处理工具类
 */
public class DateHelper {

    /**
     * 日期格式化器 yyyy-MM-dd HH:mm
     */
    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat DATE_FORMAT_YMD_HM = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * 获取当前时间戳
     *
     * @return 当前时间戳
     */
    public static long getLongDate() {
        return System.currentTimeMillis();
    }

    /**
     * 截取日期字符串末尾指定数量的字符
     *
     * @param date 日期字符串
     * @param num  要截取的字符数
     * @return 截取后的字符串
     */
    public static String formatDateByTailNum(String date, int num) {
        if (date == null) {
            return null;
        }
        if (num <= 0 || num > date.length()) {
            return date;
        }
        return date.substring(0, date.length() - num);
    }

    /**
     * 格式化日期字符串，去除秒部分
     *
     * @param date 日期字符串，格式为 yyyy/MM/dd HH:mm:ss
     * @return 格式化后的字符串，格式为 yyyy/MM/dd HH:mm
     */
    public static String formatDate_3(String date) {
        return formatDateByTailNum(date, 3);
    }

    /**
     * 格式化日期字符串，只保留年月日部分
     *
     * @param date 日期字符串，格式为 yyyy/MM/dd HH:mm:ss
     * @return 格式化后的字符串，格式为 yyyy/MM/dd
     */
    public static String formatDate3(String date) {
        return formatDateByTailNum(date, 9);
    }

    /**
     * 获取当前时间戳字符串
     *
     * @return 时间戳字符串
     */
    public static String getStrLongDate() {
        return String.valueOf(getLongDate());
    }

    /**
     * 将时间戳转换为日期对象
     *
     * @param longDate 时间戳
     * @return 日期对象
     */
    public static Date longToDate(long longDate) {
        return new Date(longDate);
    }

    /**
     * 将时间戳字符串转换为日期对象
     *
     * @param strLongDate 时间戳字符串
     * @return 日期对象，如果转换失败则返回null
     */
    public static Date strLongToDate(String strLongDate) {
        try {
            return new Date(Long.parseLong(strLongDate));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将时间戳转换为指定格式的日期字符串 (yyyy-MM-dd HH:mm:ss)
     *
     * @param longDate 时间戳
     * @return 日期字符串
     */
    public static String longToTime(long longDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(longToDate(longDate));
    }

    /**
     * 将时间戳转换为指定格式的日期字符串 (yyyy-MM-dd HH:mm)
     *
     * @param longDate 时间戳
     * @return 日期字符串
     */
    public static String longToTime2(long longDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return df.format(longToDate(longDate));
    }

    /**
     * 将时间戳转换为指定格式的日期字符串 (yyyy-MM-dd)
     *
     * @param longDate 时间戳
     * @return 日期字符串
     */
    public static String longToTime3(long longDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(longToDate(longDate));
    }

    /**
     * 将时间戳转换为指定格式的时间字符串 (HH:mm)
     *
     * @param longDate 时间戳
     * @return 时间字符串
     */
    public static String longToDayTime(long longDate) {
        DateFormat df = new SimpleDateFormat("HH:mm");
        return df.format(longToDate(longDate));
    }

    /**
     * 将时间戳字符串转换为指定格式的日期字符串 (yyyy-MM-dd HH:mm:ss)
     *
     * @param strLongDate 时间戳字符串
     * @return 日期字符串
     */
    public static String strLongToTime(String strLongDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(strLongToDate(strLongDate));
    }

    /**
     * 将时间戳转换为指定格式的日期字符串 (yyyy年MM月dd日 HH:mm)
     *
     * @param longDate 时间戳
     * @return 日期字符串
     */
    public static String strLongToScheduleTime(long longDate) {
        DateFormat df = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        return df.format(longToDate(longDate));
    }

    /**
     * 获取当前日期时间字符串
     *
     * @return 日期时间字符串，格式为 EEE MMM dd HH:mm:ss zzz yyyy
     */
    public static String getTime1() {
        return new Date().toString();
    }

    /**
     * 获取当前日期字符串
     *
     * @return 日期字符串，格式为 yyyy-MM-dd
     */
    public static String getYearMonthDay1() {
        return new java.sql.Date(System.currentTimeMillis()).toString();
    }

    /**
     * 获取当前日期字符串
     *
     * @return 日期字符串，格式为 yyyy-MM-dd
     */
    public static String getYearMonthDay2() {
        Date date = new Date();
        return new java.sql.Date(date.getTime()).toString();
    }

    /**
     * 获取当前时间戳字符串
     *
     * @return 时间戳字符串，格式为 yyyy-MM-dd HH:mm:ss.SSS
     */
    public static String getSeconds1() {
        return new Timestamp(System.currentTimeMillis()).toString();
    }

    /**
     * 获取当前时间戳字符串
     *
     * @return 时间戳字符串，格式为 yyyy-MM-dd HH:mm:ss.SSS
     */
    public static String getSeconds2() {
        Date date = new Date();
        return new Timestamp(date.getTime()).toString();
    }

    /**
     * 获取当前日期时间字符串
     *
     * @return 日期时间字符串，格式为 yyyy-MM-dd HH:mm:ss
     */
    public static String getYear_Second1() {
        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(stamp);
    }

    /**
     * 获取当前日期时间字符串
     *
     * @return 日期时间字符串，格式为 yyyy-MM-dd HH:mm:ss
     */
    public static String getYear_Second2() {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(date);
    }

    /**
     * 将日期字符串转换为日期对象
     *
     * @param str 日期字符串，格式为 yyyy-MM-dd HH:mm:ss
     * @return 日期对象，如果转换失败则返回null
     */
    public static Date changeStringToDate1(String str) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将日期字符串转换为日期字符串
     *
     * @param str 日期字符串，格式为 yyyy-MM-dd HH:mm:ss
     * @return 日期字符串，格式为 yyyy-MM-dd
     */
    public static String changeStringToDate2(String str) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = sdf.parse(str);
            return new java.sql.Date(date.getTime()).toString();
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }


    /**
     * 将日期字符串转换为时间戳字符串
     *
     * @param str 日期字符串，格式为 yyyy-MM-dd HH:mm:ss
     * @return 时间戳字符串，格式为 yyyy-MM-dd HH:mm:ss.S
     */
    public static String changeStringToDate3(String str) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = sdf.parse(str);
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            return new Timestamp(sqlDate.getTime()).toString();
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }
    /**
     * 将日期字符串转换为时间戳
     *
     * @param str 日期字符串，格式为 yyyy-MM-dd HH:mm:ss
     * @return 时间戳，如果转换失败则返回0
     */
    public static long strDateToLong(String str) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = sdf.parse(str);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }
}