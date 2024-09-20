package run.yigou.gxzy.ui.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.ui.fragment.TipsBookNetReadFragment;
import run.yigou.gxzy.ui.fragment.TipsSettingFragment;
import run.yigou.gxzy.ui.fragment.TipsUnitFragment;


public final class TipsFragmentActivity extends AppActivity {

    public static void start(Context context, /*boolean isLocalNet, */int bookNo) {
        Intent intent = new Intent(context, TipsFragmentActivity.class);
        //isLocal = isLocalNet;
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
   // private static boolean isLocal = false;
    private static int BookId = 0;
    private RadioGroup radioGroup;

    @Override
    protected int getLayoutId() {
        //if (!isLocal)
        //    return R.layout.tips_fragment_tab_list;
        //else
            return R.layout.tips_fragment_tab_net_list;
    }

    private FragmentManager fragmentManager;

    @Override
    protected void initView() {

        // 获取 FragmentManager
        fragmentManager = getSupportFragmentManager();
        // 创建 主要显示页 实例
        TipsBookNetReadFragment fragment = new TipsBookNetReadFragment();
        TipsUnitFragment tipsUnitFragment = new TipsUnitFragment();
        TipsSettingFragment tipsSettingFragment = new TipsSettingFragment();
        this.radioGroup = findViewById(R.id.rg_tab);
        // 设置默认选中的 RadioButton
        radioGroup.check(R.id.firstContentTab); // 选择 id 为 radioButton1 的按钮
        // 监听选项变化
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.firstContentTab) {
                ShouFragment(fragment);
            } else if (checkedId == R.id.unitTab) {
                ShouFragment(tipsUnitFragment);
            }
            else if (checkedId == R.id.settingsTab) {
                ShouFragment(tipsSettingFragment);
            }
        });
//        if (isLocal) {
//            // 创建 Bundle 并传递参数
//            Bundle args = new Bundle();
//            args.putInt("bookNo", BookId);  // 替换为实际参数
//            fragment.setArguments(args);
//            //ShouFragment( fragment);
//        }
        Bundle args = new Bundle();
        args.putInt("bookNo", BookId);  // 替换为实际参数
        fragment.setArguments(args);
        ShouFragment(fragment);
    }

    private void ShouFragment(androidx.fragment.app.Fragment fragment) {
        // 使用 FragmentTransaction 添加 Fragment
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_tips_container, fragment);
        transaction.commit();
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