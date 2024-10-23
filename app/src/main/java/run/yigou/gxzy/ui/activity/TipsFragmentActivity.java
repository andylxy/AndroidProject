package run.yigou.gxzy.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gyf.immersionbar.ImmersionBar;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.ui.fragment.TipsBookNetReadFragment;
import run.yigou.gxzy.ui.fragment.TipsSettingFragment;
import run.yigou.gxzy.ui.fragment.TipsUnitShowFragment;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;

public final class TipsFragmentActivity extends AppActivity {

    // 定义常量用于替换硬编码的资源 ID
    private static final int FIRST_CONTENT_TAB_ID = R.id.firstContentTab;
    private static final int UNIT_TAB_ID = R.id.unitTab;
    private static final int SETTINGS_TAB_ID = R.id.settingsTab;
    private static final int FRAGMENT_CONTAINER_ID = R.id.fragment_tips_container;

    private RadioGroup radioGroup; // 用于切换不同 Fragment 的 RadioGroup
    private FragmentManager fragmentManager; // Fragment 管理器
    private RadioGroup.OnCheckedChangeListener tabCheckedChangeListener; // RadioGroup 的监听器

    public TipsFragmentActivity() {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_tab_net_list;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void initView() {
        // 给这个 View 设置沉浸式，避免状态栏遮挡
        ImmersionBar.setTitleBar(this, findViewById(R.id.fragment_tips_container));
        // 注册事件
        // XEventBus.getDefault().register(this);

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
        TipsUnitShowFragment tipsUnitShowFragment = new TipsUnitShowFragment();
        TipsSettingFragment tipsSettingFragment = new TipsSettingFragment();

        // 初始化 RadioGroup
        radioGroup = findViewById(R.id.rg_tab);
        radioGroup.check(FIRST_CONTENT_TAB_ID); // 设置默认选中的 RadioButton
        RadioButton currentCheckedRadioButton = radioGroup.findViewById(FIRST_CONTENT_TAB_ID);

        tabCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                try {
                    switch (checkedId) {
                        case FIRST_CONTENT_TAB_ID:
                            replaceFragment(fragment);
                            break;
                        case UNIT_TAB_ID:
                            replaceFragment(tipsUnitShowFragment);
                            break;
                        case SETTINGS_TAB_ID:
                            replaceFragment(tipsSettingFragment);
                            break;
                        default:
                            // 记录未处理的 checkedId
                            // Log.w("TabChangeListener", "Unknown checkedId: " + checkedId);
                            break;
                    }

                } catch (Exception e) {
                    // 异常处理
                    // Log.e("TabChangeListener", "Error replacing fragment", e);
                }
            }
        };
        radioGroup.setOnCheckedChangeListener(tabCheckedChangeListener);
        // 设置主要显示页的参数
        Bundle args = new Bundle();
        args.putInt("bookNo", bookId);  // 替换为实际参数
        args.putInt("bookLastReadPosition",  getIntent() .getIntExtra("bookLastReadPosition",0));
        fragment.setArguments(args);
        try {
            TabNavBody tabNav = TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
            StringBuilder stringBuilder = new StringBuilder("阅读器");
            if (tabNav != null) {
                // 清空内容
                stringBuilder.setLength(0);
                stringBuilder.append(tabNav.getBookName());
            }
            tabNav = null;
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
     *
     * @param fragment 要添加到容器的 Fragment 实例
     */
    private void replaceFragment(androidx.fragment.app.Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(FRAGMENT_CONTAINER_ID, fragment);
        transaction.commit();
    }

    @Override
    protected void initData() {
    }

//    @Override
//    public void onBackPressed() {
//        if (!StringHelper.isEmpty(searchKey)) {
//            if (lvSearchBooksList.getVisibility() == View.GONE)
//                super.onBackPressed();
//            mLvSearchBooks.setVisibility(View.VISIBLE);
//            lvSearchBooksList.setVisibility(View.GONE);
//            return;
//        }
//        super.onBackPressed();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 在 Activity 销毁时注销 RadioGroup 的监听器，避免内存泄漏
        radioGroup.setOnCheckedChangeListener(null);
        tabCheckedChangeListener = null;

        //  EasyLog.print(REFERENCE_KEY + " onDestroy");
        // Tips_Single_Data.getInstance().curActivity = null;
        // XEventBus.getDefault().unregister(this);
    }



}
