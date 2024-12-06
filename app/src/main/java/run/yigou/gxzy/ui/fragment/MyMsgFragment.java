package run.yigou.gxzy.ui.fragment;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.ui.activity.HomeActivity;


/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : 可进行拷贝的副本
 */
public final class MyMsgFragment extends TitleBarFragment<HomeActivity> {

    public static MyMsgFragment newInstance() {
        return new MyMsgFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.my_msg_fragment;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {
        setTitle("功能暂时没实现");
    }

    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }

}