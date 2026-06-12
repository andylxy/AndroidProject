package run.yigou.gxzy.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.gyf.immersionbar.ImmersionBar;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.base.args.BookArgs;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.ui.feature.reader.fragment.TipsBookNetReadFragment;
import run.yigou.gxzy.ui.feature.reader.fragment.TipsFangYaoFragment;
import run.yigou.gxzy.ui.feature.reader.fragment.TipsSettingFragment;
import run.yigou.gxzy.base.GlobalDataHolder;

/**
 * 书籍详情页面Activity
 * 展示书籍的阅读内容、方药信息、单位设置等功能
 * 通过ViewPager2和底部导航实现多标签页切换
 */
public final class TipsFragmentActivity extends AppActivity implements NavigationAdapter.OnNavigationListener {
    private static final String INTENT_KEY_IN_FRAGMENT_INDEX = "fragmentIndex";
    /**
     * ViewPager2，用于管理Fragment页面切换
     */
    private ViewPager2 mViewPager;
    /**
     * 底部导航RecyclerView
     */
    private RecyclerView mNavigationView;
    /**
     * 导航适配器
     */
    private NavigationAdapter mNavigationAdapter;
    /**
     * Fragment状态适配器
     */
    private TipsFragmentStateAdapter mPagerAdapter;
    /**
     * 页面切换回调
     */
    private ViewPager2.OnPageChangeCallback mPageChangeCallback;
    /**
     * 当前书籍ID
     */
    private int bookId = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_tab_net_list;
    }

    TabNavBody bookInfo = null;

    @Override
    protected void initView() {
        setupViews();
        setupViewPager();
        setupNavigationAdapter();
    }

    /**
     * 初始化视图组件
     */
    private void setupViews() {
        // 给这个 View 设置沉浸式，避免状态栏遮挡
        ImmersionBar.setTitleBar(this, findViewById(R.id.tips_fragment_pager));
        
        // 初始化视图引用
        mViewPager = findViewById(R.id.tips_fragment_pager);
        mNavigationView = findViewById(R.id.tips_fragment_navigation);
    }

    /**
     * 设置ViewPager2配置
     */
    private void setupViewPager() {
        if (mViewPager == null) {
            return;
        }
        
        // 禁用 ViewPager2 的用户滑动输入，防止误触
        mViewPager.setUserInputEnabled(false);
    }


    @Override
    protected void initData() {
        // 验证书籍信息并获取参数
        BookArgs bookArgs = validateAndGetBookArgs();
        if (bookArgs == null) {
            return;
        }
        
        // 设置Fragment适配器
        setupFragmentAdapter(bookArgs);
        
        // 设置页面切换监听
        setupPageChangeCallback();
        
        // 处理新的意图
        onNewIntent(getIntent());
    }

    /**
     * 验证书籍信息并获取参数
     */
    private BookArgs validateAndGetBookArgs() {
        // 获取书籍ID
        bookId = getIntent().getIntExtra("bookId", 0);
        if (bookId == 0) {
            handleBookInfoError("书籍ID无效");
            return null;
        }
        
        // 获取书籍信息
        bookInfo = GlobalDataHolder.getInstance().getNavTabBodyMap().get(bookId);
        if (bookInfo == null) {
            handleBookInfoError("获取书籍信息失败");
            return null;
        }
        
        int bookLastReadPosition = getIntent().getIntExtra("bookLastReadPosition", 0);
        boolean isShow = getIntent().getBooleanExtra("bookCollect", false);
        return BookArgs.newInstance(bookId, bookLastReadPosition, isShow);
    }

    /**
     * 设置Fragment适配器
     */
    private void setupFragmentAdapter(BookArgs bookArgs) {
        // 根据书籍类型动态创建 Fragment 列表
        setupFragments(bookArgs);
        mViewPager.setAdapter(mPagerAdapter);
    }

    /**
     * 设置页面切换监听
     */
    private void setupPageChangeCallback() {
        mPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (mNavigationAdapter != null) {
                    mNavigationAdapter.setSelectedPosition(position);
                }
            }
        };
        mViewPager.registerOnPageChangeCallback(mPageChangeCallback);
    }

    /**
     * 处理书籍信息错误
     */
    private void handleBookInfoError(String errorMessage) {
        Log.e("TipsFragmentActivity", errorMessage);
        toast("获取书籍信息错误");
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        switchFragment(0);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存当前 Fragment 索引位置
        if (mViewPager != null) {
            outState.putInt(INTENT_KEY_IN_FRAGMENT_INDEX, mViewPager.getCurrentItem());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 恢复当前 Fragment 索引位置
        if (savedInstanceState != null && mViewPager != null) {
            int fragmentIndex = savedInstanceState.getInt(INTENT_KEY_IN_FRAGMENT_INDEX, 0);
            switchFragment(fragmentIndex);
        }
    }

    public void switchFragment(int fragmentIndex) {
        if (!isValidFragmentIndex(fragmentIndex)) {
            return;
        }
        
        // 无动画切换Fragment
        mViewPager.setCurrentItem(fragmentIndex, false);
        mNavigationAdapter.setSelectedPosition(fragmentIndex);
    }

    /**
     * 验证Fragment索引是否有效
     */
    private boolean isValidFragmentIndex(int fragmentIndex) {
        return fragmentIndex >= 0 && 
               fragmentIndex < mPagerAdapter.getItemCount() &&
               mPagerAdapter != null && 
               mNavigationAdapter != null;
    }

    /**
     * {@link NavigationAdapter.OnNavigationListener}
     */
    @Override
    public boolean onNavigationItemSelected(int position) {
        if (!isValidNavigationPosition(position)) {
            return false;
        }
        
        // 平滑动画切换Fragment
        mViewPager.setCurrentItem(position, true);
        return true;
    }

    /**
     * 验证导航位置是否有效
     */
    private boolean isValidNavigationPosition(int position) {
        return mPagerAdapter != null && 
               position >= 0 && 
               position < mPagerAdapter.getItemCount();
    }


    @Override
    protected void onDestroy() {
        try {
            // 第一步：注销所有监听器（防止回调触发）
            safelyUnregisterListeners();
            
            // 第二步：清理 ViewPager2（按正确顺序）
            safelyCleanupViewPager();
            
            // 第三步：清理 Adapter 内部引用
            safelyCleanupAdapters();
            
            // 第四步：清理 RecyclerView
            safelyCleanupRecyclerView();
            
            // 第五步：释放所有成员变量引用
            releaseReferences();
        } catch (Exception e) {
            Log.e("TipsFragmentActivity", "Error during onDestroy cleanup", e);
        } finally {
            // 最后：调用父类
            super.onDestroy();
        }
    }

    /**
     * 安全地注销监听器
     */
    private void safelyUnregisterListeners() {
        if (mNavigationAdapter != null) {
            try {
                mNavigationAdapter.setOnNavigationListener(null);
            } catch (Exception e) {
                Log.e("TipsFragmentActivity", "Error unregistering navigation listener", e);
            }
        }
    }

    /**
     * 安全地清理ViewPager2
     */
    private void safelyCleanupViewPager() {
        if (mViewPager != null) {
            try {
                // 先注销回调
                if (mPageChangeCallback != null) {
                    mViewPager.unregisterOnPageChangeCallback(mPageChangeCallback);
                }
                // 设置 adapter 为 null，释放 Fragment 引用
                mViewPager.setAdapter(null);
            } catch (Exception e) {
                Log.e("TipsFragmentActivity", "Error cleaning up ViewPager", e);
            }
        }
    }

    /**
     * 安全地清理适配器
     */
    private void safelyCleanupAdapters() {
        if (mPagerAdapter != null) {
            try {
                mPagerAdapter.clearFragments();
            } catch (Exception e) {
                Log.e("TipsFragmentActivity", "Error clearing pager adapter fragments", e);
            }
        }
    }

    /**
     * 安全地清理RecyclerView
     */
    private void safelyCleanupRecyclerView() {
        if (mNavigationView != null) {
            try {
                mNavigationView.setAdapter(null);
            } catch (Exception e) {
                Log.e("TipsFragmentActivity", "Error cleaning up navigation view", e);
            }
        }
    }

    /**
     * 释放所有引用
     */
    private void releaseReferences() {
        mPagerAdapter = null;
        mNavigationAdapter = null;
        mPageChangeCallback = null;
        mViewPager = null;
        mNavigationView = null;
        bookInfo = null;
    }

    /**
     * ViewPager2 的 FragmentStateAdapter 实现
     * 优化了Fragment的创建和缓存机制
     */
    private class TipsFragmentStateAdapter extends FragmentStateAdapter {
        private final List<AppFragment<?>> fragmentList = new ArrayList<>();
        private final List<Long> fragmentIds = new ArrayList<>();
        
        public TipsFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        public void addFragment(AppFragment<?> fragment) {
            fragmentList.add(fragment);
            fragmentIds.add((long) fragment.hashCode());
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // 添加边界检查，防止IndexOutOfBoundsException
            if (position < 0 || position >= fragmentList.size()) {
                Log.e("TipsFragmentStateAdapter", "Invalid position: " + position);
                return new Fragment(); // 返回空Fragment作为降级处理
            }
            return fragmentList.get(position);
        }
        
        @Override
        public int getItemCount() {
            return fragmentList.size();
        }
        
        @Override
        public long getItemId(int position) {
            // 添加边界检查
            if (position < 0 || position >= fragmentIds.size()) {
                return RecyclerView.NO_ID;
            }
            return fragmentIds.get(position);
        }
        
        @Override
        public boolean containsItem(long itemId) {
            return fragmentIds.contains(itemId);
        }
        
        /**
         * 清理 Fragment 引用，帮助 GC 回收
         * 在 Activity onDestroy 时调用
         */
        public void clearFragments() {
            try {
                fragmentList.clear();
                fragmentIds.clear();
            } catch (Exception e) {
                Log.e("TipsFragmentStateAdapter", "Error clearing fragments", e);
            }
        }
    }

    /**
     * Fragment类型常量
     */
    private static final int FRAGMENT_TYPE_FANG = 1;  // 方
    private static final int FRAGMENT_TYPE_YAO = 2;   // 药
    private static final int FRAGMENT_TYPE_UNIT = 3; // 单位
    private static final int CASE_TAG_SHANGHAN = 5;  // 伤寒类书籍

    /**
     * 根据书籍类型动态创建 Fragment 列表
     */
    private void setupFragments(BookArgs bookArgs) {
        mPagerAdapter = new TipsFragmentStateAdapter(this);
        
        // 1. 书籍阅读（必有）
        addBookReadingFragment(bookArgs);
        
        // 2. 方药相关（根据书籍类型）
        addFangYaoFragments();
        
        // 3. 单位（仅伤寒类书籍）
        addUnitFragmentIfNeeded();
        
        // 4. 设置（必有）
        addSettingsFragment(bookArgs);
    }

    /**
     * 添加书籍阅读Fragment
     */
    private void addBookReadingFragment(BookArgs bookArgs) {
        mPagerAdapter.addFragment(TipsBookNetReadFragment.newInstance(bookArgs));
    }

    /**
     * 添加方药相关Fragments
     */
    private void addFangYaoFragments() {
        if (shouldShowFangYaoTabs()) {
            mPagerAdapter.addFragment(TipsFangYaoFragment.newInstance(FRAGMENT_TYPE_FANG, bookId));
            mPagerAdapter.addFragment(TipsFangYaoFragment.newInstance(FRAGMENT_TYPE_YAO, bookId));
        }
    }

    /**
     * 添加单位Fragment（如果需要）
     */
    private void addUnitFragmentIfNeeded() {
        if (bookInfo != null && bookInfo.getCaseTag() == CASE_TAG_SHANGHAN) {
            mPagerAdapter.addFragment(TipsFangYaoFragment.newInstance(FRAGMENT_TYPE_UNIT, bookId));
        }
    }

    /**
     * 添加设置Fragment
     */
    private void addSettingsFragment(BookArgs bookArgs) {
        mPagerAdapter.addFragment(TipsSettingFragment.newInstance(bookArgs));
    }

    /**
     * 判断是否显示方药标签
     */
    private boolean shouldShowFangYaoTabs() {
        if (bookInfo == null) {
            return false;
        }
        int caseTag = bookInfo.getCaseTag();
        // 黄帝内经(1)、本草(2,3) 不显示方药
        return caseTag != 1 && caseTag != 2 && caseTag != 3;
    }

    /**
     * 设置导航适配器
     */
    private void setupNavigationAdapter() {
        if (mNavigationView == null || bookInfo == null) {
            return;
        }
        
        mNavigationAdapter = new NavigationAdapter(this);
        
        // 缓存处理后的书名，避免重复调用
        String processedBookName = extractBookName(bookInfo.getBookName());
        
        // 添加基础导航项
        addBaseNavigationItems(processedBookName);
        
        // 添加方药导航项
        addFangYaoNavigationItems(processedBookName);
        
        // 添加单位导航项（如果需要）
        addUnitNavigationItemIfNeeded();
        
        // 添加设置导航项
        addSettingsNavigationItem();
        
        // 设置监听器
        mNavigationAdapter.setOnNavigationListener(this);
        mNavigationView.setAdapter(mNavigationAdapter);
    }

    /**
     * 添加基础导航项
     */
    private void addBaseNavigationItems(String bookName) {
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                bookName,
                ContextCompat.getDrawable(this, R.drawable.list_selector)
        ));
    }

    /**
     * 添加方药导航项
     */
    private void addFangYaoNavigationItems(String bookName) {
        if (!shouldShowFangYaoTabs()) {
            return;
        }
        
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                bookName + getString(R.string.tips_nav_fang),
                ContextCompat.getDrawable(this, R.drawable.list_fang_selector)
        ));
        
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.tips_nav_yao),
                ContextCompat.getDrawable(this, R.drawable.ruler_yao_selector)
        ));
    }

    /**
     * 添加单位导航项（如果需要）
     */
    private void addUnitNavigationItemIfNeeded() {
        if (bookInfo != null && bookInfo.getCaseTag() == CASE_TAG_SHANGHAN) {
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                    getString(R.string.tips_nav_unit),
                    ContextCompat.getDrawable(this, R.drawable.ruler_selector)
            ));
        }
    }

    /**
     * 添加设置导航项
     */
    private void addSettingsNavigationItem() {
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.tips_nav_set),
                ContextCompat.getDrawable(this, R.drawable.settings_selector)
        ));
    }

    /**
     * 提取书名（去除标点符号）
     * 处理包含.,・等标点符号的书名，提取主要部分
     * 
     * @param fullName 完整书名
     * @return 处理后的书名，如果为空则返回空字符串
     */
    private String extractBookName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        
        // 使用正则表达式分割标点符号
        String[] parts = fullName.split("[.,・]");
        return parts.length > 0 ? parts[0].trim() : fullName.trim();
    }

}
