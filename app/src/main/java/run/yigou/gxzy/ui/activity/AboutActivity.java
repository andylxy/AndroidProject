package run.yigou.gxzy.ui.activity;

import android.annotation.SuppressLint;

import androidx.appcompat.widget.AppCompatTextView;

import java.util.Calendar;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;

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

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        AppCompatTextView textView = findViewById(R.id.tv_copyright);
       // AppCompatTextView tv_version = findViewById(R.id.tv_version);
       // String about_version = "<font color=\"#FF252C\">\"致谢(不分顺序)2\n    </font>1、感谢“风若吹”老师,一直以来无私的帮助校对论寒论・桂林古本。\"";
       // tv_version.setText(about_version);
        textView.setText("  Copyright © 2023 - " + Calendar.getInstance().get(Calendar.YEAR));
    }

    @Override
    protected void initData() {
    }
}