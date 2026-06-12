package run.yigou.gxzy.base.constant;

import android.graphics.Color;

import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.R;


/**
 * ????  zhs
 * ???:  2023-07-07 22:20:14
 * ???:  run.yigou.gxzy.common
 * ???:  APPCONST
 * ???:  1.0
 * ???:
 */

public class AppConst {

    public static String UrlPath = "/api/AppBookRequest/";
    public static String dbName = "myzhongyi.db";

//    public static String publicKey;//公开密钥
//    public static String privateKey;//app私钥
    /**
     * 自定义分隔线高度
     */
    public static final int CustomDivider_Height = 5;
    public static final int CustomDivider_Content_RecyclerView_Color = Color.argb(230, 97, 185, 248);
    public static final int CustomDivider_BookList_RecyclerView_Color = Color.parseColor("#F4F4F4");//Color.argb(230,  97,185,248);
    /**
     * 延迟提交时间（毫秒）
     */
    public static final long postDelayMillis = 100;
    public static final String FILE_NAME_SETTING = "setting";

    /**
     * 伤寒论书籍编号
     */
    public static final int ShangHanNo = 10001;

    /**
     * 图片服务器地址
     */
    public static final String ImageHost = "https://948526c.webp.li/";


    public static final String Key_Window_Tips = "Key_Window_Tips";

    /**
     * 匿名访问 Token
     */
    public static final String AllowAnonymous_Token = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ2b2wuY29yZS5vd25lciIsImp0aSI6IjE5NzEyNDYzOTk3NTIzNzYzMjAiLCJpYXQiOjE3NjM3NDAxNTcsIm5iZiI6MTc2Mzc0MDE1NywiZXhwIjoxNzYzNzQ3MzU3LCJhdWQiOiJ2b2wuY29yZSIsIlVTRVJfSUQiOjE5NzEyNDYzOTk3NTIzNzYzMjAsIkFDQ09VTlQiOiJhZG1pbjY2NiIsIk5BTUUiOiI2NjYiLCJJU19TVVBFUl9BRE1JTiI6ZmFsc2UsIk9SR19JRCI6MTk5MDY4NTg1MDU5NzQ2MjAxNiwiVEVOQU5UX0lEIjpudWxsfQ.ydOA3sKKiu70ICakee4VUHPUMvFP6Fmt91EybYl8TwY";

    /**
     * Rc4 加密密钥
     */
    public static String rc4_SecretKey = "C5ABA9E202D94C13A3CB66002BF77FAF";

    /**
     * 返回数据类型
     */
    /**
     * 无更多数据类型
     */
    public static int reData_Type = 3;
    /**
     * 无底部类型
     */
    public static int noFooter_Type = 2;
    /**
     * 数据类型
     */
    public static int data_Type = 1;

}
