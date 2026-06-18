package run.yigou.gxzy.ui.main;

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
import com.hjq.base.DoubleClickHelper;

import run.yigou.gxzy.ui.reader.ai.AiMsgFragment;
import run.yigou.gxzy.ui.account.MyFragmentPersonal;
import run.yigou.gxzy.ui.reader.BookCollectCaseFragment;
import run.yigou.gxzy.ui.main.HomeFragment;
import run.yigou.gxzy.ui.main.NavigationAdapter;
import run.yigou.gxzy.log.EasyLog;

/**
 * 首页Activity
 * 应用程序的主界面，采用底部导航+ViewPager架构
 * 包含四个主要功能模块：书架、发现、AI聊天（登录用户）、我的
 * 
 * 功能特点：
 * 1. 底部导航栏动态显示（根据登录状态显示AI聊天）
 * 2. ViewPager页面切换与导航栏同步
 * 3. 双击返回键退出应用
 * 4. 登录状态变化时动态更新界面
 * 
 * 导航结构：
 * - 书架（BookCollectCaseFragment）：收藏的书籍
 * - 发现（HomeFragment）：推荐内容和搜索
 * - AI聊天（AiMsgFragment）：仅登录用户显示，AI对话功能
 * - 我的（MyFragmentPersonal）：个人中心和设置
 * 
 * @author Android 轮子哥
 * @github https://github.com/getActivity/AndroidProject
 * @since 2018/10/18
 */
public final class HomeActivity extends AppActivity
        implements NavigationAdapter.OnNavigationListener {

    private static final String TAG = "HomeActivity";

    /**
     * Intent参数：Fragment索引
     */
    private static final String INTENT_KEY_IN_FRAGMENT_INDEX = "fragmentIndex";
    /**
     * Intent参数：Fragment类名
     */
    private static final String INTENT_KEY_IN_FRAGMENT_CLASS = "fragmentClass";
    
    // Fragment索引常量
    /**
     * Fragment索引：发现
     */
    private static final int FRAGMENT_INDEX_FOUND = 1;
    /**
     * Fragment索引：AI聊天
     */
    private static final int FRAGMENT_INDEX_AI_CHAT = 2;
    /**
     * Fragment索引：我的
     */
    private static final int FRAGMENT_INDEX_PROFILE = 3;
    

    // Fragment数量常量
    /**
     * 登录后的Fragment数量
     */
    private static final int FRAGMENT_COUNT_WITH_AI = 4;
    

    // 延迟时间常量（毫秒）
    /**
     * 退出延迟时间
     */
    private static final int EXIT_DELAY_MS = 300;
    
    /**
     * ViewPager，用于管理Fragment页面切换
     */
    private ViewPager mViewPager;
    /**
     * 底部导航RecyclerView
     */
    private RecyclerView mNavigationView;

    /**
     * 导航适配器
     */
    private NavigationAdapter mNavigationAdapter;
    /**
     * Fragment页面适配器
     */
    private FragmentPagerAdapter<AppFragment<?>> mPagerAdapter;

    /**
     * 启动首页Activity，默认显示发现页面
     * 
     * @param context 上下文
     */
    public static void start(Context context) {
        start(context, HomeFragment.class);
    }

    /**
     * 启动首页Activity，并指定初始显示的Fragment
     * 
     * @param context 上下文
     * @param fragmentClass 要显示的Fragment类
     */
    @Log
    public static void start(Context context, Class<? extends AppFragment<?>> fragmentClass) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(INTENT_KEY_IN_FRAGMENT_CLASS, fragmentClass);
        
        // 如果context不是Activity，需要添加NEW_TASK标志
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
        setupViews();
        setupNavigation();
    }

    /**
     * 初始化视图组件
     */
    private void setupViews() {
        mViewPager = findViewById(R.id.vp_home_pager);
        mNavigationView = findViewById(R.id.rv_home_navigation);
    }

    /**
     * 设置导航菜单
     */
    private void setupNavigation() {
        mNavigationAdapter = new NavigationAdapter(this);
        
        // 添加基础导航项
        addBaseNavigationItems();
        
        // 添加AI聊天导航项（仅登录用户）
        addAiChatNavigationItemIfNeeded();
        
        // 添加我的导航项
        addProfileNavigationItem();
        
        // 设置导航监听器
        mNavigationAdapter.setOnNavigationListener(this);
        mNavigationView.setAdapter(mNavigationAdapter);
    }

    /**
     * 添加基础导航项
     */
    private void addBaseNavigationItems() {
        // 导航顺序：0-书架, 1-发现
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.home_nav_index),
                ContextCompat.getDrawable(this, R.drawable.home_home_selector)
        ));
        
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.home_nav_found),
                ContextCompat.getDrawable(this, R.drawable.home_found_selector)
        ));
    }

    /**
     * 添加AI聊天导航项（仅登录用户）
     */
    private void addAiChatNavigationItemIfNeeded() {
        if (AppApplication.application.isLogin) {
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                    getString(R.string.tips_nav_aimsg),
                    ContextCompat.getDrawable(this, R.drawable.ruler_selector_msg)
            ));
        }
    }

    /**
     * 添加我的导航项
     */
    private void addProfileNavigationItem() {
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.home_nav_me),
                ContextCompat.getDrawable(this, R.drawable.home_me_selector)
        ));
    }
    /**
     * 发现页面Fragment
     */
    HomeFragment homeFragment;
    /**
     * Fragment是否已经初始化
     */
    private boolean isFragmentsInitialized = false;

    @Override
    protected void initData() {
        if (isFragmentsInitialized) {
            // 如果已经初始化过Fragment，则只刷新数据
            refreshFragments();
            return;
        }

        setupFragments();
        setupPageChangeListener();
        registerEventBus();
        handleInitialIntent();
        isFragmentsInitialized = true;
    }

    /**
     * 设置Fragment适配器
     */
    private void setupFragments() {
        homeFragment = HomeFragment.newInstance();
        mPagerAdapter = new FragmentPagerAdapter<>(this);
        
        // 添加基础Fragment
        addBaseFragments();
        
        // 添加AI聊天Fragment（仅登录用户）
        addAiChatFragmentIfNeeded();
        
        // 添加个人中心Fragment
        addProfileFragment();
        
        mViewPager.setAdapter(mPagerAdapter);
    }

    /**
     * 添加基础Fragment
     */
    private void addBaseFragments() {
        // Fragment顺序：0-书架, 1-发现
        mPagerAdapter.addFragment(BookCollectCaseFragment.newInstance());
        mPagerAdapter.addFragment(homeFragment);
    }

    /**
     * 添加AI聊天Fragment（仅登录用户）
     */
    private void addAiChatFragmentIfNeeded() {
        if (AppApplication.application.isLogin) {
            mPagerAdapter.addFragment(AiMsgFragment.newInstance());
        }
    }

    /**
     * 添加个人中心Fragment
     */
    private void addProfileFragment() {
        mPagerAdapter.addFragment(MyFragmentPersonal.newInstance());
    }

    /**
     * 设置页面切换监听
     */
    private void setupPageChangeListener() {
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
    }

    /**
     * 注册事件总线
     */
    private void registerEventBus() {
        XEventBus.getDefault().register(HomeActivity.this);
    }

    /**
     * 处理初始意图
     */
    private void handleInitialIntent() {
        onNewIntent(getIntent());
    }

    /**
     * 刷新Fragment列表
     * 当登录状态变化时，动态添加或移除AI聊天Fragment
     * 优化了性能，减少了不必要的adapter通知
     */
    private void refreshFragments() {
        if (mPagerAdapter == null || mViewPager == null) {
            return;
        }

        int currentFragmentCount = mPagerAdapter.getCount();
        boolean shouldShowAiChat = AppApplication.application.isLogin;
        boolean hasAiChat = currentFragmentCount == FRAGMENT_COUNT_WITH_AI;

        // 只有在状态真正需要改变时才执行操作
        if (shouldShowAiChat && !hasAiChat) {
            addAiChatFragment();
        } else if (!shouldShowAiChat && hasAiChat) {
            removeAiChatFragment();
        }
        // 如果状态没有变化，不需要任何操作，避免不必要的性能开销
    }

    /**
     * 添加AI聊天Fragment
     */
    private void addAiChatFragment() {
        // 保存当前页面位置
        int currentPosition = mViewPager.getCurrentItem();
        
        // 登录后添加AI聊天Fragment（在第3个位置，索引为2）
        mPagerAdapter.addFragment(AiMsgFragment.newInstance(), null, FRAGMENT_INDEX_AI_CHAT);
        
        // 恢复页面位置
        restorePositionAfterAddingAiChat(currentPosition);
    }

    /**
     * 移除AI聊天Fragment
     */
    private void removeAiChatFragment() {
        // 保存当前页面位置
        int currentPosition = mViewPager.getCurrentItem();
        
        // 登出后移除AI聊天Fragment（索引为2）
        mPagerAdapter.removeFragment(FRAGMENT_INDEX_AI_CHAT);
        
        // 恢复页面位置
        restorePositionAfterRemovingAiChat(currentPosition);
    }

    /**
     * 添加AI聊天Fragment后恢复位置
     */
    private void restorePositionAfterAddingAiChat(int currentPosition) {
        int targetPosition = calculateTargetPositionAfterAdding(currentPosition);
        if (targetPosition >= 0) {
            mViewPager.setCurrentItem(targetPosition, false);
        }
    }

    /**
     * 移除AI聊天Fragment后恢复位置
     */
    private void restorePositionAfterRemovingAiChat(int currentPosition) {
        int targetPosition = calculateTargetPositionAfterRemoving(currentPosition);
        if (targetPosition >= 0) {
            mViewPager.setCurrentItem(targetPosition, false);
        }
    }

    /**
     * 计算添加AI聊天Fragment后的目标位置
     */
    private int calculateTargetPositionAfterAdding(int currentPosition) {
        if (currentPosition == FRAGMENT_INDEX_AI_CHAT) {
            // 原来的"我的"现在在索引3
            return FRAGMENT_INDEX_PROFILE;
        } else if (currentPosition < FRAGMENT_INDEX_AI_CHAT) {
            // 书架和发现位置不变
            return currentPosition;
        }
        return -1; // 无效位置
    }

    /**
     * 计算移除AI聊天Fragment后的目标位置
     */
    private int calculateTargetPositionAfterRemoving(int currentPosition) {
        if (currentPosition == FRAGMENT_INDEX_AI_CHAT) {
            // 如果在AI聊天页面，跳转到发现
            return FRAGMENT_INDEX_FOUND;
        } else if (currentPosition == FRAGMENT_INDEX_PROFILE) {
            // 如果在"我的"页面，调整到AI聊天索引
            return FRAGMENT_INDEX_AI_CHAT;
        } else if (currentPosition < FRAGMENT_INDEX_AI_CHAT) {
            // 书架和发现位置不变
            return currentPosition;
        }
        return -1; // 无效位置
    }

    /**
     * 处理新的Intent
     * 主要用于处理从其他页面跳转过来的请求，切换到指定的Fragment
     * 
     * @param intent 新的Intent对象
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 默认切换到发现页面（索引1），如果指定了其他页面则切换到对应页面
        int targetIndex = intent.getIntExtra(INTENT_KEY_IN_FRAGMENT_INDEX, FRAGMENT_INDEX_FOUND);
        switchFragment(targetIndex);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        try {
            super.onSaveInstanceState(outState);
            // 保存当前 Fragment 索引位置
            if (mViewPager != null) {
                outState.putInt(INTENT_KEY_IN_FRAGMENT_INDEX, mViewPager.getCurrentItem());
            }
        } catch (Exception e) {
            EasyLog.print(TAG, "Error saving instance state");
            EasyLog.print(e);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
            // 恢复当前 Fragment 索引位置
            if (savedInstanceState != null) {
                switchFragment(savedInstanceState.getInt(INTENT_KEY_IN_FRAGMENT_INDEX, FRAGMENT_INDEX_FOUND));
            }
        } catch (Exception e) {
            EasyLog.print(TAG, "Error restoring instance state");
            EasyLog.print(e);
        }
    }

    /**
     * 切换Fragment页面
     * @param fragmentIndex Fragment索引
     */
    public void switchFragment(int fragmentIndex) {
        try {
            if (fragmentIndex == -1) {
                return;
            }

            if (fragmentIndex >= 0 && fragmentIndex < mPagerAdapter.getCount()) {
                mViewPager.setCurrentItem(fragmentIndex);
                // 同步更新导航选中状态
                int navigationPosition = fragmentIndexToNavigationIndex(fragmentIndex);
                if (navigationPosition >= 0 && mNavigationAdapter != null) {
                    mNavigationAdapter.setSelectedPosition(navigationPosition);
                }
            }
        } catch (Exception e) {
            EasyLog.print(TAG, "Error switching fragment to index: " + fragmentIndex);
            EasyLog.print(e);
        }
    }

    /**
     * Fragment索引转换为导航索引
     * Fragment顺序：0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
     * 导航顺序：  0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
     * 两者顺序完全一致，直接1:1映射
     * 
     * @param fragmentIndex Fragment索引
     * @return 导航索引，如果无效则返回-1
     */
    private int fragmentIndexToNavigationIndex(int fragmentIndex) {
        // Fragment顺序和导航顺序完全一致，直接返回
        int maxIndex = getMaxFragmentIndex();
        return isValidIndex(fragmentIndex, maxIndex) ? fragmentIndex : -1;
    }

    /**
     * 获取最大有效的Fragment索引
     */
    private int getMaxFragmentIndex() {
        return AppApplication.application.isLogin ? FRAGMENT_INDEX_PROFILE : FRAGMENT_INDEX_AI_CHAT;
    }

    /**
     * 验证索引是否有效
     */
    private boolean isValidIndex(int index, int maxIndex) {
        return index >= 0 && index <= maxIndex;
    }

    /**
     * {@link NavigationAdapter.OnNavigationListener}
     */

    @Override
    public boolean onNavigationItemSelected(int position) {
        try {
            // 导航位置和Fragment索引完全一致，直接使用position
            // Fragment顺序：0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
            // 导航顺序：  0-书架, 1-发现, 2-AI聊天(仅登录), 3-我的
            
            if (position >= 0 && position < mPagerAdapter.getCount()) {
                mViewPager.setCurrentItem(position);
                return true;
            }
        } catch (Exception e) {
            EasyLog.print(TAG, "Error selecting navigation position: " + position);
            EasyLog.print(e);
        }
        return false;
    }

    /**
     * 创建状态栏配置
     * 设置导航栏背景颜色为白色
     * 
     * @return ImmersionBar配置对象
     */
    @NonNull
    @Override
    protected ImmersionBar createStatusBarConfig() {
        return super.createStatusBarConfig()
                // 指定导航栏背景颜色
                .navigationBarColor(R.color.white);
    }

    @Override
    public void onBackPressed() {
        try {
            if (!DoubleClickHelper.isOnDoubleClick()) {
                if (homeFragment != null) {
                    homeFragment.clearSearchTextFocus();
                }
                toast(R.string.home_exit_hint);
                return;
            }

            // 移动到上一个任务栈，避免侧滑引起的不良反应
            moveTaskToBack(false);
            postDelayed(() -> {
                try {
                    // 进行内存优化，销毁掉所有的界面
                    ActivityManager.getInstance().finishAllActivities();
                    // 销毁进程（注意：调用此 API 可能导致当前 Activity onDestroy 方法无法正常回调）
                    // System.exit(0);
                } catch (Exception e) {
                    EasyLog.print(TAG, "Error during exit cleanup");
                    EasyLog.print(e);
                }
            }, EXIT_DELAY_MS);
        } catch (Exception e) {
            EasyLog.print(TAG, "Error in onBackPressed");
            EasyLog.print(e);
            // 降级处理：直接调用父类方法
            super.onBackPressed();
        }
    }
    /**
     * 重新激活时调用
     */
    @Override
    public void onResume() {
        super.onResume();
        // 刷新导航菜单，根据登录状态动态显示AI聊天
        refreshNavigationMenu();
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
        addNavigationMenuItems();

        // 更新布局管理器的列数（未登录3列，登录后4列）
        mNavigationAdapter.updateLayoutManager(mNavigationView);

        // 通知数据变化
        mNavigationAdapter.notifyDataSetChanged();

        // 同步刷新Fragment
        refreshFragments();
    }

    /**
     * 添加导航菜单项
     */
    private void addNavigationMenuItems() {
        // 基础菜单项
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.home_nav_index),
                ContextCompat.getDrawable(this, R.drawable.home_home_selector)
        ));
        
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.home_nav_found),
                ContextCompat.getDrawable(this, R.drawable.home_found_selector)
        ));

        // 仅登录用户显示AI聊天
        if (AppApplication.application.isLogin) {
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                    getString(R.string.tips_nav_aimsg),
                    ContextCompat.getDrawable(this, R.drawable.ruler_selector_msg)
            ));
        }

        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.home_nav_me),
                ContextCompat.getDrawable(this, R.drawable.home_me_selector)
        ));
    }

    @Override
    protected void onDestroy() {
        try {
            // 第一步：注销事件总线，防止内存泄漏
            safelyUnregisterEventBus();
            
            // 第二步：清理适配器和监听器
            safelyCleanupAdapters();
            
            // 第三步：清理ViewPager
            safelyCleanupViewPager();
            
            // 第四步：清理导航视图
            safelyCleanupNavigationView();
            
        } catch (Exception e) {
            EasyLog.print(TAG, "Error during onDestroy cleanup");
            EasyLog.print(e);
        } finally {
            // 最后：调用父类销毁方法
            super.onDestroy();
        }
    }

    /**
     * 安全地注销事件总线
     */
    private void safelyUnregisterEventBus() {
        try {
            XEventBus.getDefault().unregister(HomeActivity.this);
        } catch (Exception e) {
            EasyLog.print(TAG, "Error unregistering event bus");
            EasyLog.print(e);
        }
    }

    /**
     * 安全地清理适配器和监听器
     */
    private void safelyCleanupAdapters() {
        if (mNavigationAdapter != null) {
            try {
                mNavigationAdapter.setOnNavigationListener(null);
            } catch (Exception e) {
                EasyLog.print(TAG, "Error cleaning up navigation adapter");
                EasyLog.print(e);
            }
        }
    }

    /**
     * 安全地清理ViewPager
     */
    private void safelyCleanupViewPager() {
        if (mViewPager != null) {
            try {
                mViewPager.setAdapter(null);
                mViewPager.clearOnPageChangeListeners();
            } catch (Exception e) {
                EasyLog.print(TAG, "Error cleaning up view pager");
                EasyLog.print(e);
            }
        }
    }

    /**
     * 安全地清理导航视图
     */
    private void safelyCleanupNavigationView() {
        if (mNavigationView != null) {
            try {
                mNavigationView.setAdapter(null);
            } catch (Exception e) {
                EasyLog.print(TAG, "Error cleaning up navigation view");
                EasyLog.print(e);
            }
        }
    }


}