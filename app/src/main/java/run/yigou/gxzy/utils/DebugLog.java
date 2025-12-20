package run.yigou.gxzy.utils;
import com.blankj.utilcode.util.LogUtils;

import run.yigou.gxzy.BuildConfig;

/**
 * 调试日志工具类
 * 只在 debug 构建时输出日志，release 构建时不输出
 */
public final class DebugLog {
    
    // 调试开关：只在 debug 构建时输出日志
    private static final boolean DEBUG = BuildConfig.DEBUG;
    
    private DebugLog() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * Debug 级别日志
     */
    public static void d(String tag, String message) {
        if (DEBUG) {
            LogUtils.d(tag, message);
        }
    }
    
    /**
     * Debug 级别日志（带异常）
     */
    public static void d(String tag, String message, Throwable tr) {
        if (DEBUG) {
            LogUtils.d(tag, message, tr);
        }
    }
    
    /**
     * Info 级别日志
     */
    public static void i(String tag, String message) {
        if (DEBUG) {
            LogUtils.i(tag, message);
        }
    }
    
    /**
     * Verbose 级别日志
     */
    public static void v(String tag, String message) {
        if (DEBUG) {
            LogUtils.v(tag, message);
        }
    }
    
    /**
     * Warning 级别日志（通常也希望在 release 中看到，但这里按需求也做调试控制）
     */
    public static void w(String tag, String message) {
        if (DEBUG) {
            LogUtils.w(tag, message);
        }
    }
    
    /**
     * Error 级别日志（错误日志通常在 release 中也需要）
     * 如果需要在 release 中也输出错误日志，可以去掉 DEBUG 判断
     */
    public static void e(String tag, String message) {
        if (DEBUG) {
            LogUtils.e(tag, message);
        }
    }
    
    /**
     * Error 级别日志（带异常）
     */
    public static void e(String tag, String message, Throwable tr) {
        if (DEBUG) {
            LogUtils.e(tag, message, tr);
        }
    }
    
    /**
     * 打印日志（单参数版本，兼容 EasyLog.print）
     * 使用 Debug 级别输出
     */
    public static void print(String message) {
        if (DEBUG) {
            LogUtils.d(message);
        }
    }
    
    /**
     * 打印日志（双参数版本，兼容 EasyLog.print）
     * 使用 Debug 级别输出
     */
    public static void print(String tag, String message) {
        if (DEBUG) {
            LogUtils.d(tag, message);
        }
    }
    
    /**
     * 打印异常（兼容 EasyLog.print）
     */
    public static void print(Throwable tr) {
        if (DEBUG) {
            LogUtils.e(tr);
        }
    }
}
