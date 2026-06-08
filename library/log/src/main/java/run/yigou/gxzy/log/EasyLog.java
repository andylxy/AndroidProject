package run.yigou.gxzy.log;

import android.util.Log;

/**
 * 本地 EasyLog 替代类，解决 EasyHttp 升级后 EasyLog 方法缺失问题
 */
public final class EasyLog {
    private static final String TAG = "EasyLog";

    public static void print(String msg) {
        Log.i(TAG, msg != null ? msg : "null");
    }

    public static void print(String tag, String msg) {
        Log.i(tag, msg != null ? msg : "null");
    }

    public static void print(Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
    }

    public static void json(String json) {
        Log.i(TAG, "JSON: " + (json != null ? json : "null"));
    }
    
    public static void printJson(Object request, String json) {
        Log.i(TAG, "JSON: " + (json != null ? json : "null"));
    }
}
