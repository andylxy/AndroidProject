package run.yigou.gxzy.ui.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Looper;

import androidx.appcompat.widget.AppCompatTextView;

import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.Calendar;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.greendao.entity.About;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.http.api.AboutApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 关于界面
 */
public final class AboutActivity extends AppActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.about_activity;
    }

    AppCompatTextView tv_thks;
    AppCompatTextView tv_developer;
    AppCompatTextView tv_author_book;
    AppCompatTextView tv_updata_log;

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        AppCompatTextView textView = findViewById(R.id.tv_copyright);
        tv_thks = findViewById(R.id.tv_thks);
        tv_developer = findViewById(R.id.tv_developer);
        tv_author_book = findViewById(R.id.tv_author_book);
        tv_updata_log = findViewById(R.id.tv_updata_log);
        textView.setText("  Copyright © 2023 - " + Calendar.getInstance().get(Calendar.YEAR));

    }

    @Override
    protected void initData() {

        List<About> detailList = ConvertEntity.getAbout();
        if (!detailList.isEmpty()) {
            setAbout(ConvertEntity.getAbout());
        }
        EasyHttp.get(this)
                .api(new AboutApi())
                .request(new HttpCallback<HttpData<List<About>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<About>> data) {
                        if (data != null && !data.getData().isEmpty()) {

                            //保存内容
                            ThreadUtil.runInBackground(() -> {
                                setAbout(data.getData());
                                ConvertEntity.saveAbout(data.getData());
                            });
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        ThreadUtil.runInBackground(() -> {
                            setAbout(ConvertEntity.getAbout());
                        });
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
                    setText(tv_thks, TipsNetHelper.renderText(text));
                    break;
                case ABOUT_MANUAL:
                    setSpannableText(tv_developer, text);
                    break;
                case ABOUT_UPDATELOG:
                    setSpannableText(tv_updata_log, text);
                    break;
                case ABOUT_BOOKNAME:
                    setText(tv_author_book, TipsNetHelper.renderText(text));
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

    StringBuilder spannableStringBuilder = new StringBuilder();

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


}