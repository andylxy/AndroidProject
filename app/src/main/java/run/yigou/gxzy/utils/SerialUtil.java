package run.yigou.gxzy.utils;

import android.annotation.SuppressLint;
import android.provider.Settings;
import android.content.Context;

public class SerialUtil {
    private SerialUtil() {
    }

    private static SerialUtil instance = new SerialUtil();
    public static Context context;

    public static SerialUtil getInstance(Context context) {
        if (instance == null) {
            instance = new SerialUtil();
        }
        SerialUtil.context = context;
        return instance;
    }


    @SuppressLint("HardwareIds")
    public static String getSerialAndroidId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    public static String getSerial() {
        String serial = getSerialAndroidId();
        if (serial == null) {
            serial = getSerialCalcMD5() ;
        }
        return serial;
    }
    private static String getSerialCalcMD5() {
        String hardwareInfo = android.os.Build.BOARD + android.os.Build.BRAND + android.os.Build.DEVICE + android.os.Build.DISPLAY
                + android.os.Build.HOST + android.os.Build.ID + android.os.Build.MANUFACTURER + android.os.Build.MODEL
                + android.os.Build.PRODUCT + android.os.Build.TAGS + android.os.Build.TYPE +android.os. Build.USER;
        return MD5Util.CalcMD5(hardwareInfo);
    }

}
