package run.yigou.gxzy.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.APPCONST;
import run.yigou.gxzy.ui.fragment.TipsBookNetReadFragment;
import run.yigou.gxzy.ui.fragment.TipsBookReadFragment;



public final class TipsFragmentActivity extends AppActivity {

    public static void start(Context context, boolean isLocalNet, int bookNo) {
        Intent intent = new Intent(context, TipsFragmentActivity.class);
        isLocal = isLocalNet;
        BookId = bookNo;
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }




        context.startActivity(intent);
    }

    public TipsFragmentActivity() {
    }

    /**
     * isLocal 加载数据方式. false为本地,true 为网络
     */
    private static boolean isLocal = false;
    private static int BookId = 0;

    @Override
    protected int getLayoutId() {
        if (!isLocal)
            return R.layout.tips_fragment_tab_list;
        else return R.layout.tips_fragment_tab_net_list;
    }

    @Override
    protected void initView() {

        if (isLocal) {
            // 创建 Fragment 实例
            TipsBookNetReadFragment fragment = new TipsBookNetReadFragment();

            // 创建 Bundle 并传递参数
            Bundle args = new Bundle();
            args.putInt("bookNo", BookId);  // 替换为实际参数
            fragment.setArguments(args);

            // 获取 FragmentManager
            FragmentManager fragmentManager = getSupportFragmentManager();

            // 使用 FragmentTransaction 添加 Fragment
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_tips_container, fragment);
            transaction.commit();
        }
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