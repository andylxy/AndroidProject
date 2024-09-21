package run.yigou.gxzy.ui.activity;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.ui.fragment.TipsBookNetReadFragment;
import run.yigou.gxzy.ui.fragment.TipsSettingFragment;
import run.yigou.gxzy.ui.fragment.TipsUnitFragment;
import run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data;

public final class TipsFragmentActivity extends AppActivity {

    // 定义常量用于替换硬编码的资源 ID
    private static final int FIRST_CONTENT_TAB_ID = R.id.firstContentTab;
    private static final int UNIT_TAB_ID = R.id.unitTab;
    private static final int SETTINGS_TAB_ID = R.id.settingsTab;
    private static final int FRAGMENT_CONTAINER_ID = R.id.fragment_tips_container;

    private RadioGroup radioGroup; // 用于切换不同 Fragment 的 RadioGroup
    private FragmentManager fragmentManager; // Fragment 管理器
    private RadioGroup.OnCheckedChangeListener tabCheckedChangeListener; // RadioGroup 的监听器
    private RadioButton currentCheckedRadioButton;
    public TipsFragmentActivity() {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_tab_net_list;
    }

    @Override
    protected void initView() {
        try {
            // 从意图中获取书籍ID
            int bookId = getIntent().getIntExtra("bookId", 0);
            if (bookId == 0) {
                // 如果书籍ID为0，则显示提示信息并返回
                toast("获取书籍信息错误");
                return;
            }

            // 获取 FragmentManager
            fragmentManager = getSupportFragmentManager();

            // 创建主要显示页实例
            TipsBookNetReadFragment fragment = new TipsBookNetReadFragment();
            TipsUnitFragment tipsUnitFragment = new TipsUnitFragment();
            TipsSettingFragment tipsSettingFragment = new TipsSettingFragment();

            // 初始化 RadioGroup
            radioGroup = findViewById(R.id.rg_tab);
            radioGroup.check(FIRST_CONTENT_TAB_ID); // 设置默认选中的 RadioButton
            currentCheckedRadioButton = radioGroup.findViewById(FIRST_CONTENT_TAB_ID);
            // 设置 RadioGroup 的监听器
            tabCheckedChangeListener = (group, checkedId) -> {
                switch (checkedId) {
                    case FIRST_CONTENT_TAB_ID:
                        replaceFragment(fragment);
                        break;
                    case UNIT_TAB_ID:
                        replaceFragment(tipsUnitFragment);
                        break;
                    case SETTINGS_TAB_ID:
                        replaceFragment(tipsSettingFragment);
                        break;
                    default:
                        // 默认处理
                        break;
                }
            };

            radioGroup.setOnCheckedChangeListener(tabCheckedChangeListener);

            // 设置主要显示页的参数
            Bundle args = new Bundle();
            args.putInt("bookNo", bookId);  // 替换为实际参数
            fragment.setArguments(args);

            BookInfoNav.Bean.TabNav tabNav = Tips_Single_Data.getInstance().getNavTabMap().get(bookId);
            StringBuilder stringBuilder = new StringBuilder("阅读器");
            if (tabNav != null) {
                // 清空内容
                stringBuilder.setLength(0);
                stringBuilder.append(tabNav.getBookName());
            }
            currentCheckedRadioButton.setText(stringBuilder);

            // 显示默认页面
            replaceFragment(fragment);
        } catch (Exception e) {
            e.printStackTrace();
            toast("初始化视图时发生错误");
        }
    }

    /**
     * 替换容器中的 Fragment
     * @param fragment 要添加到容器的 Fragment 实例
     */
    private void replaceFragment(androidx.fragment.app.Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(FRAGMENT_CONTAINER_ID, fragment);
        transaction.commit();
    }

    @Override
    protected void initData() {}

    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (radioGroup != null && tabCheckedChangeListener != null) {
            // 在 Activity 销毁时注销 RadioGroup 的监听器，避免内存泄漏
            radioGroup.setOnCheckedChangeListener(null);
        }
    }
}
