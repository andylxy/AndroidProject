package run.yigou.gxzy.ui.activity;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;

/**
 * 作者:  zhs
 * 时间:  2023-07-07 10:12:44
 * 包名:  run.yigou.gxzy.ui.activity
 * 类名:  BookInfoActivity
 * 版本:  1.0
 * 描述:
 */
public final class TipsFragmentActivity extends AppActivity {

    public TipsFragmentActivity(){}
    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_tab_list;
    }

    @Override
    protected void initView() {

    }
    @Override
    protected void initData() {

    }


    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }
}