package run.yigou.gxzy.ui.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Looper;
import android.text.Html;

import androidx.appcompat.widget.AppCompatTextView;

import com.hjq.http.EasyHttp;
import run.yigou.gxzy.other.EasyLog;
import com.hjq.http.listener.OnHttpListener;

import java.util.Calendar;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.greendao.entity.About;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.http.api.AboutApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 关于界面
 */
public final class AboutActivity extends AppActivity {

    private AppCompatTextView tvThks;
    private AppCompatTextView tvDeveloper;
    private AppCompatTextView tvAuthorBook;
    private AppCompatTextView tvUpdataLog;
    private AppCompatTextView tvDisclaimer;

    @Override
    protected int getLayoutId() {
        return R.layout.about_activity;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        AppCompatTextView tvCopyright = findViewById(R.id.tv_copyright);
        tvThks = findViewById(R.id.tv_thks);
        tvDeveloper = findViewById(R.id.tv_developer);
        tvAuthorBook = findViewById(R.id.tv_author_book);
        tvUpdataLog = findViewById(R.id.tv_updata_log);
        tvDisclaimer = findViewById(R.id.tv_disclaimer);

        // 设置版权年份
        tvCopyright.setText("  Copyright © 2023 - " + Calendar.getInstance().get(Calendar.YEAR));

        // 加载免责声明内容
        loadDisclaimerContent();
    }

    // 缓存相关常量
    private static final String PREF_NAME = "about_cache";
    private static final String KEY_CACHE_TIME = "cache_time";
    private static final long CACHE_EXPIRY_MS = 90L * 24 * 60 * 60 * 1000; // 3个月（90天）

    @Override
    protected void initData() {
        // 优先加载本地数据
        List<About> localData = ConvertEntity.getAbout();
        if (!localData.isEmpty()) {
            setAbout(localData);
            EasyLog.print("AboutActivity", "Loaded data from local cache");
        }

        // 检查是否需要刷新数据（缓存超过3个月或本地无数据）
        if (shouldRefreshCache() || localData.isEmpty()) {
            requestAboutData();
        } else {
            EasyLog.print("AboutActivity", "Cache is still valid, skip network request");
        }
    }

    /**
     * 检查缓存是否过期（超过3个月）
     */
    private boolean shouldRefreshCache() {
        long lastCacheTime = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .getLong(KEY_CACHE_TIME, 0);
        
        if (lastCacheTime == 0) {
            // 从未缓存过
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastCacheTime;
        
        boolean expired = timeDiff > CACHE_EXPIRY_MS;
        if (expired) {
            EasyLog.print("AboutActivity", "Cache expired, last update: " + 
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(new java.util.Date(lastCacheTime)));
        }
        return expired;
    }

    /**
     * 更新缓存时间戳
     */
    private void updateCacheTime() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
                .apply();
    }

    /**
     * 请求网络数据
     */
    private void requestAboutData() {
        EasyLog.print("AboutActivity", "Requesting about data from network...");
        
        EasyHttp.get(this)
                .api(new AboutApi())
                .request(new OnHttpListener<HttpData<List<About>>>() {
                    @Override
                    public void onHttpSuccess(HttpData<List<About>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<About> aboutList = data.getData();
                            
                            // 更新UI
                            setAbout(aboutList);
                            
                            // 后台保存数据并更新缓存时间
                            ThreadUtil.runInBackground(() -> {
                                ConvertEntity.saveAbout(aboutList);
                                updateCacheTime();
                                EasyLog.print("AboutActivity", "Data saved and cache time updated");
                            });
                        }
                    }

                    @Override
                    public void onHttpFail(Throwable e) {
                        // super.onFail(e);
                        EasyLog.print("AboutActivity", "Network request failed: " + e.getMessage());
                        // 网络失败时，如果本地没有数据，再次尝试加载
                        List<About> localData = ConvertEntity.getAbout();
                        if (!localData.isEmpty()) {
                            setAbout(localData);
                        }
                    }
                });
    }

    private static final String ABOUT_THKS = "about_thks";
    private static final String ABOUT_MANUAL = "about_manual";
    private static final String ABOUT_BOOKNAME = "about_bookname";
    private static final String ABOUT_UPDATELOG = "app_updata_log";


    private void setAbout(List<About> detailList) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }

        if (detailList == null || detailList.isEmpty()) {
            return;
        }

        if (!isMainThread()) {
            EasyLog.print("setAbout", "Method called from non-UI thread, posting to UI thread.");
            post(() -> setAbout(detailList));
            return;
        }

        for (About about : detailList) {
            String name = about.getName();
            String text = about.getText();

            if (name == null || text == null) {
                continue;
            }

            switch (name) {
                case ABOUT_THKS:
                    setText(tvThks, TipsNetHelper.renderText(text));
                    break;
                case ABOUT_MANUAL:
                    setSpannableText(tvDeveloper, text);
                    break;
                case ABOUT_UPDATELOG:
                    setSpannableText(tvUpdataLog, text);
                    break;
                case ABOUT_BOOKNAME:
                    setText(tvAuthorBook, TipsNetHelper.renderText(text));
                    break;
                default:
                    EasyLog.print("setAbout", "Unknown about name: " + name);
                    break;
            }
        }
    }

    private boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private void setText(AppCompatTextView textView, CharSequence text) {
        if (textView != null) {
            textView.setText(text);
        } else {
            EasyLog.print("setText", "TextView is null for text: " + text);
        }
    }

    private final StringBuilder spannableStringBuilder = new StringBuilder();

    private void setSpannableText(AppCompatTextView textView, String text) {
        if (textView != null) {
            spannableStringBuilder.setLength(0);
            String[] lines = text.split("\\|");
            for (String line : lines) {
                spannableStringBuilder.append(line).append("\n");
            }
            textView.setText(TipsNetHelper.renderText(spannableStringBuilder.toString()));
        } else {
            EasyLog.print("setSpannableText", "TextView is null for text: " + text);
        }
    }

    /**
     * 加载免责声明内容
     * 从 strings.xml 读取 HTML 格式的免责声明并显示
     */
    private void loadDisclaimerContent() {
        if (tvDisclaimer == null) {
            return;
        }

        String disclaimerHtml = getString(R.string.disclaimer_content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvDisclaimer.setText(Html.fromHtml(disclaimerHtml, Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvDisclaimer.setText(Html.fromHtml(disclaimerHtml));
        }
    }
}