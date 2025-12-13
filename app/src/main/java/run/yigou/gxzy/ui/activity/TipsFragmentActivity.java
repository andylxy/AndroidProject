package run.yigou.gxzy.ui.activity;

import android.content.Intent;
import android.os.Bundle;

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
import run.yigou.gxzy.common.BookArgs;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.ui.adapter.NavigationAdapter;
import run.yigou.gxzy.ui.fragment.TipsBookNetReadFragment;
import run.yigou.gxzy.ui.fragment.TipsFangYaoFragment;
import run.yigou.gxzy.ui.fragment.TipsSettingFragment;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;

public final class TipsFragmentActivity extends AppActivity implements NavigationAdapter.OnNavigationListener {
    private static final String INTENT_KEY_IN_FRAGMENT_INDEX = "fragmentIndex";
    private ViewPager2 mViewPager;
    private RecyclerView mNavigationView;
    private NavigationAdapter mNavigationAdapter;
    private TipsFragmentStateAdapter mPagerAdapter;
    private ViewPager2.OnPageChangeCallback mPageChangeCallback;
    private int bookId = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_tab_net_list;
    }

    TabNavBody bookInfo = null;

    @Override
    protected void initView() {
        // 给这个 View 设置沉浸式，避免状态栏遮挡
        ImmersionBar.setTitleBar(this, findViewById(R.id.tips_fragment_pager));
        
        // 防止重复初始化
        if (mViewPager != null) {
            return;
        }
        
        // 从意图中获取书籍ID
        bookId = getIntent().getIntExtra("bookId", 0);
        if (bookId == 0) {
            toast("获取书籍信息错误");
            finish();
            return;
        }

        mViewPager = findViewById(R.id.tips_fragment_pager);
        mNavigationView = findViewById(R.id.tips_fragment_navigation);
        
        // 禁用 ViewPager2 的用户滑动输入
        mViewPager.setUserInputEnabled(false);

        bookInfo = GlobalDataHolder.getInstance().getNavTabBodyMap().get(bookId);
        if (bookInfo == null) {
            toast("获取书籍信息错误");
            finish();
            return;
        }
        
        // 初始化导航适配器
        setupNavigationAdapter();
    }


    @Override
    protected void initData() {
        int bookLastReadPosition = getIntent().getIntExtra("bookLastReadPosition", 0);
        boolean isShow = getIntent().getBooleanExtra("bookCollect", false);
        BookArgs bookArgs = BookArgs.newInstance(bookId, bookLastReadPosition, isShow);
        
        // 根据书籍类型动态创建 Fragment 列表
        setupFragments(bookArgs);
        
        mViewPager.setAdapter(mPagerAdapter);
        
        // ViewPager2 页面切换监听
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
        
        onNewIntent(getIntent());
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
        if (fragmentIndex == -1 || mPagerAdapter == null || mNavigationAdapter == null) {
            return;
        }
        
        // 动态边界检查（适应 1-5 个 Fragment）
        if (fragmentIndex >= 0 && fragmentIndex < mPagerAdapter.getItemCount()) {
            mViewPager.setCurrentItem(fragmentIndex, false); // false = 无动画切换
            mNavigationAdapter.setSelectedPosition(fragmentIndex);
        }
    }

    /**
     * {@link NavigationAdapter.OnNavigationListener}
     */
    @Override
    public boolean onNavigationItemSelected(int position) {
        if (mPagerAdapter == null) {
            return false;
        }
        
        // 动态边界检查
        if (position >= 0 && position < mPagerAdapter.getItemCount()) {
            mViewPager.setCurrentItem(position, true); // true = 平滑动画
            return true;
        }
        return false;
    }


    @Override
    protected void onDestroy() {
        // 第一步：注销所有监听器（防止回调触发）
        if (mNavigationAdapter != null) {
            mNavigationAdapter.setOnNavigationListener(null);
        }
        
        // 第二步：清理 ViewPager2（按正确顺序）
        if (mViewPager != null) {
            // 2.1 先注销回调
            if (mPageChangeCallback != null) {
                mViewPager.unregisterOnPageChangeCallback(mPageChangeCallback);
            }
            // 2.2 设置 adapter 为 null，让 ViewPager2 释放 Fragment 引用
            // 这对于防止内存泄漏是必要的
            mViewPager.setAdapter(null);
        }
        
        // 第三步：清理 Adapter 内部引用
        if (mPagerAdapter != null) {
            mPagerAdapter.clearFragments();
        }
        
        // 第四步：清理 RecyclerView
        if (mNavigationView != null) {
            mNavigationView.setAdapter(null);
        }
        
        // 第五步：释放所有成员变量引用
        mPagerAdapter = null;
        mNavigationAdapter = null;
        mPageChangeCallback = null;
        mViewPager = null;
        mNavigationView = null;
        bookInfo = null;
        
        // 最后：调用父类
        super.onDestroy();
    }

    /**
     * ViewPager2 的 FragmentStateAdapter 实现
     */
    private class TipsFragmentStateAdapter extends FragmentStateAdapter {
        private final List<AppFragment<?>> fragmentList = new ArrayList<>();
        
        public TipsFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        public void addFragment(AppFragment<?> fragment) {
            fragmentList.add(fragment);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragmentList.get(position);
        }
        
        @Override
        public int getItemCount() {
            return fragmentList.size();
        }
        
        // ViewPager2 需要实现此方法以支持动态更新
        @Override
        public long getItemId(int position) {
            return fragmentList.get(position).hashCode();
        }
        
        @Override
        public boolean containsItem(long itemId) {
            for (AppFragment<?> fragment : fragmentList) {
                if (fragment.hashCode() == itemId) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * 清理 Fragment 引用，帮助 GC 回收
         * 在 Activity onDestroy 时调用
         */
        public void clearFragments() {
            fragmentList.clear();
        }
    }

    /**
     * 根据书籍类型动态创建 Fragment 列表
     */
    private void setupFragments(BookArgs bookArgs) {
        mPagerAdapter = new TipsFragmentStateAdapter(this);
        
        // 1. 书籍阅读（必有）
        mPagerAdapter.addFragment(TipsBookNetReadFragment.newInstance(bookArgs));
        
        // 2. 方药相关（根据书籍类型）
        if (shouldShowFangYaoTabs()) {
            mPagerAdapter.addFragment(TipsFangYaoFragment.newInstance(1, bookId)); // 方
            mPagerAdapter.addFragment(TipsFangYaoFragment.newInstance(2, bookId)); // 药
        }
        
        // 3. 单位（仅伤寒类书籍）
        if (bookInfo.getCaseTag() == 5) {
            mPagerAdapter.addFragment(TipsFangYaoFragment.newInstance(3, bookId)); // 单位
        }
        
        // 4. 设置（必有）
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
        mNavigationAdapter = new NavigationAdapter(this);
        
        String bookName = extractBookName(bookInfo.getBookName());
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(bookName,
                ContextCompat.getDrawable(this, R.drawable.list_selector)));

        if (shouldShowFangYaoTabs()) {
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                    bookName + getString(R.string.tips_nav_fang),
                    ContextCompat.getDrawable(this, R.drawable.list_fang_selector)));
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                    getString(R.string.tips_nav_yao),
                    ContextCompat.getDrawable(this, R.drawable.ruler_yao_selector)));
        }
        
        if (bookInfo.getCaseTag() == 5) {
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                    getString(R.string.tips_nav_unit),
                    ContextCompat.getDrawable(this, R.drawable.ruler_selector)));
        }
        
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(
                getString(R.string.tips_nav_set),
                ContextCompat.getDrawable(this, R.drawable.settings_selector)));
        
        mNavigationAdapter.setOnNavigationListener(this);
        mNavigationView.setAdapter(mNavigationAdapter);
    }

    /**
     * 提取书名（去除标点）
     */
    private String extractBookName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }
        String[] parts = fullName.split("[.,・]");
        return parts.length > 0 ? parts[0] : fullName;
    }

}
