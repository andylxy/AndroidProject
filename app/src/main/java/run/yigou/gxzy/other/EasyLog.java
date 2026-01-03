package run.yigou.gxzy.other;

import android.util.Log;

/**
 * desc   : Log replacement for EasyConfig
 */
public final class EasyLog {

    private static final String TAG = "EasyLog";
    private static boolean isDebug = true;

    public static void print(String log) {
        if (isDebug) {
            Log.d(TAG, log);
        }
    }

    public static void print(String tag, String log) {
        if (isDebug) {
            Log.d(tag, log);
        }
    }

    public static void print(Throwable e) {
        if (isDebug && e != null) {
            e.printStackTrace();
        }
    }

    public static void json(String json) {
        if (isDebug) {
            Log.d(TAG, json);
        }
    }
}
