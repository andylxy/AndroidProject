package run.yigou.gxzy.tipsutils;

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
