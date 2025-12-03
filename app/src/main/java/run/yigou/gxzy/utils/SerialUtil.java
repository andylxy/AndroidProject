package run.yigou.gxzy.utils;

import android.annotation.SuppressLint;
import android.provider.Settings;
import android.content.Context;

/**
 * 设备序列号工具类
 */
public class SerialUtil {
    private static SerialUtil instance;
    private static Context context;

    private SerialUtil() {
    }

    /**
     * 获取SerialUtil单例实例
     *
     * @param ctx 上下文对象
     * @return SerialUtil实例
     */
    public static synchronized SerialUtil getInstance(Context ctx) {
        if (instance == null) {
            instance = new SerialUtil();
        }
        context = ctx.getApplicationContext();
        return instance;
    }

    /**
     * 获取设备ANDROID_ID
     *
     * @return ANDROID_ID字符串
     */
    @SuppressLint("HardwareIds")
    public static String getSerialAndroidId() {
        if (context == null) {
            return null;
        }
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * 获取设备序列号
     *
     * @return 设备序列号
     */
    public static String getSerial() {
        String serial = getSerialAndroidId();
        if (serial == null || serial.isEmpty()) {
            serial = getSerialCalcMD5();
        }
        return serial;
    }

    /**
     * 通过设备信息计算MD5值作为序列号
     *
     * @return MD5字符串
     */
    private static String getSerialCalcMD5() {
        String hardwareInfo = android.os.Build.BOARD + android.os.Build.BRAND + android.os.Build.DEVICE + android.os.Build.DISPLAY
                + android.os.Build.HOST + android.os.Build.ID + android.os.Build.MANUFACTURER + android.os.Build.MODEL
                + android.os.Build.PRODUCT + android.os.Build.TAGS + android.os.Build.TYPE + android.os.Build.USER;
        return MD5Util.CalcMD5(hardwareInfo);
    }
}