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

public class ShareUtil {

    /**
     * Share Text
     */
    public static final String TEXT = "text/plain";

    /**
     * Share Image
     */
    public static final String IMAGE = "image/*";

    /**
     * Share Audio
     */
    public static final String AUDIO = "audio/*";

    /**
     * Share Video
     */
    public static final String VIDEO = "video/*";

    /**
     * Share File
     */
    public static final String FILE = "*/*";

    /**
     * 文件分享 需要先授权显示悬浮窗
     */
    public static boolean share(String type, String path) {
        return share(type, new File(path));
    }

    /**
     * 文件分享 需要先授权显示悬浮窗
     */
    public static boolean share(String type, File file) {
        // 获取应用上下文
        final Context app = Utils.getApp();

        // 检查是否有显示悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(app)) {
                Log.e("ShareUtil", "文件分享失败 没有悬浮窗权限~");
                return false;
            }
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
        app.startActivity(chooserIntent);

        // 输出日志
        Log.e("ShareUtil", "分享了 " + type + " " + file.getAbsolutePath());

        return true;
    }
}
