package run.yigou.gxzy.common;

import android.graphics.Color;

import run.yigou.gxzy.R;


/**
 * 作者:  zhs
 * 时间:  2023-07-07 22:20:14
 * 包名:  run.yigou.gxzy.common
 * 类名:  APPCONST
 * 版本:  1.0
 * 描述:
 */

public class AppConst {

    public static String dbName = "myzhongyi.db";//服务端公钥

//    public static String publicKey;//服务端公钥
//    public static String privateKey;//app私钥
    /**
     * 自定义分隔线高度
     */
    public static final int CustomDivider_Height = 5;
    public static final int CustomDivider_Content_RecyclerView_Color = Color.argb(230,  97,185,248);
    public static final int CustomDivider_BookList_RecyclerView_Color = Color.parseColor("#F4F4F4");//Color.argb(230,  97,185,248);

    public static final String FILE_NAME_SETTING = "setting";
    /**
     * SharedPreferences 伤寒保存的的Key字符串
     */
    public static final String Key_Shanghan = "showShanghan";
    /**
     * 伤寒书籍编号
     */
    public static final int ShangHanNo = 10001;
    /**
     * 显示398条辨
     */
    public static final int Show_Shanghan_398 = 0;
    /**
     * 显示所有条辨
     */
    public static final int Show_Shanghan_AllSongBan = 1;
    /**
     * 金匮不显示
     */
    public static final int Show_Jinkui_None = 0;
    /**
     * 显示完整金匮
     */
    public static final int Show_Jinkui_Default = 1;
    /**
     * SharedPreferences 金匮保存的的Key字符串
     */
    public static final String Key_Jinkui = "showJinkui";


}
