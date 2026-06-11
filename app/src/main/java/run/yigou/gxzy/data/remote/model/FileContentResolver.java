package run.yigou.gxzy.data.remote.model;

import android.content.Context;
import android.net.Uri;
import java.io.File;

/**
 * FileContentResolver Stub for compilation
 */
public class FileContentResolver extends File {
    private Uri contentUri;

    public FileContentResolver(Context context, Uri uri, String fileName) {
        super(context.getCacheDir(), fileName);
        this.contentUri = uri;
    }

    public Uri getContentUri() {
        return contentUri;
    }
}
