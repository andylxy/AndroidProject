package run.yigou.gxzy.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.base.FragmentPagerAdapter;
import com.lucas.xbus.XEventBus;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.Log;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.manager.ActivityManager;
import run.yigou.gxzy.other.DoubleClickHelper;
import run.yigou.gxzy.ui.adapter.NavigationAdapter;
import run.yigou.gxzy.ui.fragment.AiMsgFragment;
import run.yigou.gxzy.ui.fragment.BookCollectCaseFragment;
import run.yigou.gxzy.ui.fragment.HomeFragment;
import run.yigou.gxzy.ui.fragment.MyFragmentPersonal;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 首页界面
 */
public final class HomeActivity extends AppActivity
        implements NavigationAdapter.OnNavigationListener {

    private static final String INTENT_KEY_IN_FRAGMENT_INDEX = "fragmentIndex";
    private static final String INTENT_KEY_IN_FRAGMENT_CLASS = "fragmentClass";
    public static HomeActivity mHomeActivity;
    private ViewPager mViewPager;
    private RecyclerView mNavigationView;

    private NavigationAdapter mNavigationAdapter;
    private FragmentPagerAdapter<AppFragment<?>> mPagerAdapter;

    public static void start(Context context) {
        start(context, HomeFragment.class);
    }

    @Log
    public static void start(Context context, Class<? extends AppFragment<?>> fragmentClass) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(INTENT_KEY_IN_FRAGMENT_CLASS, fragmentClass);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.home_activity;
    }


    @Override
    protected void initView() {
        mViewPager = findViewById(R.id.vp_home_pager);
        mNavigationView = findViewById(R.id.rv_home_navigation);

        mNavigationAdapter = new NavigationAdapter(this);
        // 导航顺序：0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.home_nav_index),
                ContextCompat.getDrawable(this, R.drawable.home_home_selector)));

        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.home_nav_found),
                ContextCompat.getDrawable(this, R.drawable.home_found_selector)));

        // 仅登录用户显示AI聊天
        if (AppApplication.application.isLogin) {
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.tips_nav_aimsg),
                    ContextCompat.getDrawable(this, R.drawable.ruler_selector_msg)));
        }

        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.home_nav_me),
                ContextCompat.getDrawable(this, R.drawable.home_me_selector)));
        mNavigationAdapter.setOnNavigationListener(this);
        mNavigationView.setAdapter(mNavigationAdapter);
    }
    HomeFragment homeFragment;
    private boolean isFragmentsInitialized = false;

    @Override
    protected void initData() {
        if (isFragmentsInitialized) {
            // 如果已经初始化过Fragment，则只刷新数据
            refreshFragments();
            return;
        }

        homeFragment = HomeFragment.newInstance();
        mPagerAdapter = new FragmentPagerAdapter<>(this);
        // Fragment顺序：0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
        mPagerAdapter.addFragment(BookCollectCaseFragment.newInstance()); // 0-书架
        mPagerAdapter.addFragment(homeFragment);                          // 1-发现
        // 仅登录用户显示AI聊天
        if (AppApplication.application.isLogin) {
            mPagerAdapter.addFragment(AiMsgFragment.newInstance());       // 2-AI聊天
        }
        mPagerAdapter.addFragment(MyFragmentPersonal.newInstance());      // 3-我的(登录) / 2-我的(未登录)
        mViewPager.setAdapter(mPagerAdapter);
        
        // 添加页面改变监听，同步更新导航选中状态
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // Fragment索引转换为导航索引
                int navigationPosition = fragmentIndexToNavigationIndex(position);
                if (navigationPosition >= 0) {
                    mNavigationAdapter.setSelectedPosition(navigationPosition);
                }
            }
        });
        
        mHomeActivity = this;

        XEventBus.getDefault().register(HomeActivity.this);
        onNewIntent(getIntent());
        isFragmentsInitialized = true;
    }

    /**
     * 刷新Fragment列表
     * 当登录状态变化时，动态添加或移除AI聊天Fragment
     */
    private void refreshFragments() {
        if (mPagerAdapter == null || mViewPager == null) {
            return;
        }

        int currentFragmentCount = mPagerAdapter.getCount();
        boolean shouldShowAiChat = AppApplication.application.isLogin;
        boolean hasAiChat = currentFragmentCount == 4; // 4个Fragment表示包含AI聊天

        if (shouldShowAiChat && !hasAiChat) {
            // 保存当前页面位置
            int currentPosition = mViewPager.getCurrentItem();
            
            // 登录后添加AI聊天Fragment（在第3个位置，索引为2）
            mPagerAdapter.addFragment(AiMsgFragment.newInstance(), null, 2);
            mPagerAdapter.notifyDataSetChanged();
            
            // 恢复页面位置（如果当前在"我的"页面，需要调整到新索引）
            if (currentPosition == 2) {
                // 原来的"我的"现在在索引3
                mViewPager.setCurrentItem(3, false);
            } else if (currentPosition < 2) {
                // 书架和发现位置不变
                mViewPager.setCurrentItem(currentPosition, false);
            }
        } else if (!shouldShowAiChat && hasAiChat) {
            // 保存当前页面位置
            int currentPosition = mViewPager.getCurrentItem();
            
            // 登出后移除AI聊天Fragment（索引为2）
            mPagerAdapter.removeFragment(2);
            mPagerAdapter.notifyDataSetChanged();
            
            // 恢复页面位置（如果当前在AI聊天或我的页面，需要调整）
            if (currentPosition == 2) {
                // 如果在AI聊天页面，跳转到发现
                mViewPager.setCurrentItem(1, false);
            } else if (currentPosition == 3) {
                // 如果在"我的"页面，调整到新索引2
                mViewPager.setCurrentItem(2, false);
            } else if (currentPosition < 2) {
                // 书架和发现位置不变
                mViewPager.setCurrentItem(currentPosition, false);
            }
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//            initView();
//
//           initData();
//    }

    //    @Subscribe(priority = 1)
//    public void onEvent( LoginEventNotification event) {
//        ThreadUtil.runOnUiThread(()->{
//            if (event.getLoginNotification()) {
//                //如果已登陆,
//                if(AppApplication.application.isLogin){
//                    mNavigationAdapter.addItem(2,msgMenuItem);
//                    mNavigationAdapter.notifyDataSetChanged();
//                    mPagerAdapter.addFragment(MyMsgFragment.newInstance(),null,2);
//                }
//
//            } else {
//                //如果退出登陆,
//                if(!AppApplication.application.isLogin){
//                    mNavigationAdapter.removeItem(2);
//                    mPagerAdapter.removeFragment(2);
//                }
//
//            }
//            mNavigationAdapter.notifyDataSetChanged();
//            mPagerAdapter.notifyDataSetChanged();
//        });
//    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        switchFragment(intent.getIntExtra(INTENT_KEY_IN_FRAGMENT_INDEX, 1));
        //switchFragment(0); // 默认显示书架
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存当前 Fragment 索引位置
        outState.putInt(INTENT_KEY_IN_FRAGMENT_INDEX, mViewPager.getCurrentItem());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 恢复当前 Fragment 索引位置
        switchFragment(savedInstanceState.getInt(INTENT_KEY_IN_FRAGMENT_INDEX));
    }

    public void switchFragment(int fragmentIndex) {
        if (fragmentIndex == -1) {
            return;
        }

        if (fragmentIndex >= 0 && fragmentIndex < mPagerAdapter.getCount()) {
            mViewPager.setCurrentItem(fragmentIndex);
            // 同步更新导航选中状态
            int navigationPosition = fragmentIndexToNavigationIndex(fragmentIndex);
            if (navigationPosition >= 0) {
                mNavigationAdapter.setSelectedPosition(navigationPosition);
            }
        }
    }

    /**
     * Fragment索引转换为导航索引
     * Fragment顺序：0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
     * 导航顺序：  0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
     * 两者顺序完全一致，直接1:1映射
     */
    private int fragmentIndexToNavigationIndex(int fragmentIndex) {
        // Fragment顺序和导航顺序完全一致，直接返回
        int maxIndex = AppApplication.application.isLogin ? 3 : 2;
        if (fragmentIndex >= 0 && fragmentIndex <= maxIndex) {
            return fragmentIndex;
        }
        return -1;
    }

    /**
     * {@link NavigationAdapter.OnNavigationListener}
     */

    @Override
    public boolean onNavigationItemSelected(int position) {
        // 导航位置和Fragment索引完全一致，直接使用position
        // Fragment顺序：0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
        // 导航顺序：  0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
        
        if (position >= 0 && position < mPagerAdapter.getCount()) {
            mViewPager.setCurrentItem(position);
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    protected ImmersionBar createStatusBarConfig() {
        return super.createStatusBarConfig()
                // 指定导航栏背景颜色
                .navigationBarColor(R.color.white);
    }

    @Override
    public void onBackPressed() {
        if (!DoubleClickHelper.isOnDoubleClick()) {
            homeFragment. clearSearchTextFocus();
            toast(R.string.home_exit_hint);
            return;
        }

        // 移动到上一个任务栈，避免侧滑引起的不良反应
        moveTaskToBack(false);
        postDelayed(() -> {
            // 进行内存优化，销毁掉所有的界面
            ActivityManager.getInstance().finishAllActivities();
            // 销毁进程（注意：调用此 API 可能导致当前 Activity onDestroy 方法无法正常回调）
            // System.exit(0);
        }, 300);
    }
    /**
     * 重新激活时调用
     */
    @Override
    public void onResume() {
        super.onResume();
        // 刷新导航菜单，根据登录状态动态显示AI聊天
        refreshNavigationMenu();
        //     toast("onResume");
    }

    /**
     * 刷新导航菜单
     * 根据登录状态动态添加或移除AI聊天菜单项
     */
    private void refreshNavigationMenu() {
        if (mNavigationAdapter == null) {
            return;
        }

        // 清空现有菜单项
        mNavigationAdapter.clearData();

        // 重新添加菜单项（顺序：0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的）
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.home_nav_index),
                ContextCompat.getDrawable(this, R.drawable.home_home_selector)));
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.home_nav_found),
                ContextCompat.getDrawable(this, R.drawable.home_found_selector)));

        // 仅登录用户显示AI聊天
        if (AppApplication.application.isLogin) {
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.tips_nav_aimsg),
                    ContextCompat.getDrawable(this, R.drawable.ruler_selector_msg)));
        }

        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.home_nav_me),
                ContextCompat.getDrawable(this, R.drawable.home_me_selector)));

        // 更新布局管理器的列数（未登录3列，登录后4列）
        mNavigationAdapter.updateLayoutManager(mNavigationView);

        // 通知数据变化
        mNavigationAdapter.notifyDataSetChanged();

        // 同步刷新Fragment
        refreshFragments();
    }
    @Override
    protected boolean isStatusBarEnabled() {
        return super.isStatusBarEnabled();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewPager.setAdapter(null);
        mNavigationView.setAdapter(null);
        mNavigationAdapter.setOnNavigationListener(null);
        mHomeActivity = null;
        XEventBus.getDefault().unregister(HomeActivity.this);
    }


}