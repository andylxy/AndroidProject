package run.yigou.gxzy.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.provider.Settings;
import android.view.WindowManager;

/**
 * 屏幕亮度工具类
 * 作者: zhs
 * 时间: 2023-07-08 10:52:30
 * 包名: run.yigou.gxzy.utils
 * 类名: BrightUtil
 * 版本: 1.0
 * 描述: 提供屏幕亮度相关的操作方法
 */
public class BrightUtil {
    
    /**
     * 获取屏幕的亮度
     *
     * @param activity Activity实例
     * @return 当前屏幕亮度值(0-255)，获取失败时返回0
     */
    public static int getScreenBrightness(Activity activity) {
        if (activity == null) {
            return 0;
        }
        
        int nowBrightnessValue = 0;
        ContentResolver resolver = activity.getContentResolver();
        try {
            nowBrightnessValue = Settings.System.getInt(
                    resolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nowBrightnessValue;
    }
    
    /**
     * 设置屏幕亮度
     *
     * @param activity   Activity实例
     * @param brightness 亮度值(0-255)
     */
    public static void setBrightness(Activity activity, int brightness) {
        if (activity == null) {
            return;
        }
        
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = Float.valueOf(brightness) * (1f / 255f);
        activity.getWindow().setAttributes(lp);
    }

    /**
     * 将亮度值转换为进度条进度
     *
     * @param brightness 亮度值(0-255)
     * @return 进度值(0-100)
     */
    public static int brightToProgress(int brightness) {
        return (int) (Float.valueOf(brightness) * (1f / 255f) * 100);
    }

    /**
     * 将进度条进度转换为亮度值
     *
     * @param progress 进度值(0-100)
     * @return 亮度值(0-255)
     */
    public static int progressToBright(int progress) {
        return progress * 255 / 100;
    }

    /**
     * 设置屏幕亮度跟随系统
     *
     * @param activity Activity实例
     */
    public static void followSystemBright(Activity activity) {
        if (activity == null) {
            return;
        }
        
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        activity.getWindow().setAttributes(lp);
    }
}