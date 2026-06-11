package run.yigou.gxzy.network.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import run.yigou.gxzy.R;
import com.hjq.http.EasyConfig;

import java.io.File;
import java.io.InputStream;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/15
 *    desc   : Glide ??????
 */
@GlideModule
public final class GlideConfig extends AppGlideModule {

    /** ??????????????????*/
    private static final int IMAGE_DISK_CACHE_MAX_SIZE = 500 * 1024 * 1024;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // ??????????????????????????
        File diskCacheFile = new File(context.getCacheDir(), "glide");
        // ?????????????????
        if (diskCacheFile.exists() && diskCacheFile.isFile()) {
            // ?????????
            diskCacheFile.delete();
        }
        // ??????????????
        if (!diskCacheFile.exists()) {
            // ?????????
            diskCacheFile.mkdirs();
        }
        builder.setDiskCache(() -> DiskLruCacheWrapper.create(diskCacheFile, IMAGE_DISK_CACHE_MAX_SIZE));

        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context).build();
        int defaultMemoryCacheSize = calculator.getMemoryCacheSize();
        int defaultBitmapPoolSize = calculator.getBitmapPoolSize();

        int customMemoryCacheSize = (int) (1.2 * defaultMemoryCacheSize);
        int customBitmapPoolSize = (int) (1.2 * defaultBitmapPoolSize);

        builder.setMemoryCache(new LruResourceCache(customMemoryCacheSize));
        builder.setBitmapPool(new LruBitmapPool(customBitmapPoolSize));

        builder.setDefaultRequestOptions(new RequestOptions()
                // ???????????????
                .placeholder(R.drawable.image_loading_ic)
                // ?????????????????
                .error(R.drawable.image_error_ic));
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // Glide ????????? HttpURLConnection ???????????????????????? OkHttp
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpLoader.Factory(EasyConfig.getInstance().getClient()));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
