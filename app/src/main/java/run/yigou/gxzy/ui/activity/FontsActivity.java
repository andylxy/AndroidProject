package run.yigou.gxzy.ui.activity;

import android.widget.ListView;
import android.widget.ProgressBar;

import com.hjq.widget.layout.WrapRecyclerView;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.Font;
import run.yigou.gxzy.ui.adapter.ChapterTitleAdapter;
import run.yigou.gxzy.ui.adapter.FontsAdapter;

/**
 *  作者:  zhs
 *  时间:  2023-07-08 21:43:48
 *  包名:  run.yigou.gxzy.ui.activity
 *  类名:  FontsActivity
 *  版本:  1.0
 *  描述:
 *
*/
public final class FontsActivity extends AppActivity {
    private ArrayList<Font> mFonts;
    private FontsAdapter mFontsAdapter;
    @Override
    protected int getLayoutId() {
        return R.layout.fonts_activity;
    }
    private WrapRecyclerView mLvFonts;
    private ProgressBar mPbLoading;
    @Override
    protected void initView() {
        //列表
        mLvFonts = findViewById(R.id.lv_fonts);
        //
        mPbLoading = findViewById(R.id.pb_loading);
    }

    @Override
    protected void initData() {
        initFonts();
        setTitle(getResources().getString(R.string.font));
        mFontsAdapter = new FontsAdapter(getContext());
        mLvFonts.setAdapter(mFontsAdapter);
        mFontsAdapter.setData(mFonts);
    }
    private void initFonts() {
        mFonts = new ArrayList<>();
        mFonts.add(Font.默认字体);
        mFonts.add(Font.方正楷体);
        mFonts.add(Font.经典宋体);
        mFonts.add(Font.方正行楷);
        mFonts.add(Font.迷你隶书);
        mFonts.add(Font.方正黄草);
        mFonts.add(Font.书体安景臣钢笔行书);
    }
}