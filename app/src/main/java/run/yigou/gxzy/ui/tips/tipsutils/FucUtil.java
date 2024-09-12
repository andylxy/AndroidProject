/*
 * 项目名: AndroidProject
 * 类名: FucUtil.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.FucUtil
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:39
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class FucUtil {
    public static String readFile(Context context, String str) {
        try {
            InputStream open = context.getAssets().open(str);
            int available = open.available();
            byte[] bArr = new byte[available];
            open.read(bArr, 0, available);
            return new String(bArr);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static byte[] readAudioFile(Context context, String str) {
        try {
            InputStream open = context.getAssets().open(str);
            byte[] bArr = new byte[open.available()];
            open.read(bArr);
            open.close();
            return bArr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
