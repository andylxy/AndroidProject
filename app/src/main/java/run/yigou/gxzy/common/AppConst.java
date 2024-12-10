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

    public static String dbName = "myzhongyi.db";

//    public static String publicKey;//服务端公钥
//    public static String privateKey;//app私钥
    /**
     * 自定义分隔线高度
     */
    public static final int CustomDivider_Height = 5;
    public static final int CustomDivider_Content_RecyclerView_Color = Color.argb(230,  97,185,248);
    public static final int CustomDivider_BookList_RecyclerView_Color = Color.parseColor("#F4F4F4");//Color.argb(230,  97,185,248);
    /**
     * 加入书架延迟时间
     */
    public static final long  postDelayMillis = 100;
    public static final String FILE_NAME_SETTING = "setting";

     /**
     * 宋版伤寒书籍编号
     */
    public static final int ShangHanNo = 10001;

    /**
     * 图片主机地址
     */
    public static final String ImageHost = "https://948526c.webp.li/";


    public static final String Key_Window_Tips = "功能暂时未实现,敬请等待";
}
