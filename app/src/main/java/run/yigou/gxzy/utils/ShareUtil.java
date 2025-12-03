package run.yigou.gxzy.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.blankj.utilcode.util.Utils;

import java.io.File;

/**
 * 文件分享工具类
 */
public class ShareUtil {

    /**
     * 分享文本类型
     */
    public static final String TYPE_TEXT = "text/plain";

    /**
     * 分享图片类型
     */
    public static final String TYPE_IMAGE = "image/*";

    /**
     * 分享音频类型
     */
    public static final String TYPE_AUDIO = "audio/*";

    /**
     * 分享视频类型
     */
    public static final String TYPE_VIDEO = "video/*";

    /**
     * 分享文件类型
     */
    public static final String TYPE_FILE = "*/*";

    /**
     * 文件分享 需要先授权显示悬浮窗
     *
     * @param type 分享类型
     * @param path 文件路径
     * @return 是否分享成功
     */
    public static boolean share(String type, String path) {
        if (path == null) {
            return false;
        }
        return share(type, new File(path));
    }

    /**
     * 文件分享 需要先授权显示悬浮窗
     *
     * @param type 分享类型
     * @param file 文件对象
     * @return 是否分享成功
     */
    public static boolean share(String type, File file) {
        if (type == null || file == null) {
            return false;
        }

        // 获取应用上下文
        final Context app = Utils.getApp();
        if (app == null) {
            Log.e("ShareUtil", "无法获取应用上下文");
            return false;
        }

        // 检查是否有显示悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(app)) {
                Log.e("ShareUtil", "文件分享失败 没有悬浮窗权限~");
                return false;
            }
        }

        // 检查文件是否存在
        if (!file.exists()) {
            Log.e("ShareUtil", "文件不存在: " + file.getAbsolutePath());
            return false;
        }

        // 创建分享Intent
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType(type);

        // 获取文件URI
        Uri fileURI = FileProvider.getUriForFile(app, app.getPackageName() + ".fileprovider", file);
        intent.putExtra(Intent.EXTRA_STREAM, fileURI);

        // 创建选择器
        Intent chooserIntent = Intent.createChooser(intent, "WorkTool文件分享");
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 启动分享活动
        try {
            app.startActivity(chooserIntent);
        } catch (Exception e) {
            Log.e("ShareUtil", "启动分享失败", e);
            return false;
        }

        // 输出日志
        Log.i("ShareUtil", "分享了 " + type + " " + file.getAbsolutePath());

        return true;
    }
}