package run.yigou.gxzy.ui.reader.helper;

import android.content.Context;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.ui.dialog.MenuDialog;

/**
 * Tips 模块菜单对话框辅助类
 * 负责根据类型构建长按菜单对话框（拷贝/跳转/重新下载）
 */
public class TipsDialogHelper {

    /** 对话框类型：仅拷贝内容 */
    public static final int DIALOG_TYPE_COPY = 1;
    /** 对话框类型：拷贝内容 + 跳转到本章内容 */
    public static final int DIALOG_TYPE_COPY_AND_JUMP = 2;
    /** 对话框类型：重新下载本章节 */
    public static final int DIALOG_TYPE_REDOWNLOAD = 3;

    @IntDef({DIALOG_TYPE_COPY, DIALOG_TYPE_COPY_AND_JUMP, DIALOG_TYPE_REDOWNLOAD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogType {}

    private static final List<String> RE_DOWNLOAD_DATA = Arrays.asList("重新下本章节");
    private static final List<String> COPY_ONLY_DATA = Arrays.asList("拷贝内容");
    private static final List<String> COPY_AND_JUMP_DATA = Arrays.asList("拷贝内容", "跳转到本章内容");

    /**
     * 根据类型构建菜单对话框
     *
     * @param context 上下文
     * @param type    对话框类型，使用 {@link #DIALOG_TYPE_COPY} / {@link #DIALOG_TYPE_COPY_AND_JUMP} / {@link #DIALOG_TYPE_REDOWNLOAD}
     * @return MenuDialog.Builder 实例
     */
    public static MenuDialog.Builder showListDialog(Context context, @DialogType int type) {
        MenuDialog.Builder builder;
        switch (type) {
            case DIALOG_TYPE_REDOWNLOAD:
                builder = new MenuDialog.Builder(context).setList(RE_DOWNLOAD_DATA);
                break;
            case DIALOG_TYPE_COPY_AND_JUMP:
                builder = new MenuDialog.Builder(context).setList(COPY_AND_JUMP_DATA);
                break;
            case DIALOG_TYPE_COPY:
            default:
                builder = new MenuDialog.Builder(context).setList(COPY_ONLY_DATA);
        }

        return builder;
    }
}
