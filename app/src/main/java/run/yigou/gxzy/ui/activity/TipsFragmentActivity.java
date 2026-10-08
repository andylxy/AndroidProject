package run.yigou.gxzy.ui.activity;

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
import run.yigou.gxzy.ui.reader.bookread.TipsBookNetReadFragment;
import run.yigou.gxzy.ui.reader.constant.ContentTypes;
import run.yigou.gxzy.ui.reader.fangyao.TipsFangYaoFragment;
import run.yigou.gxzy.ui.reader.settings.TipsSettingFragment;
import run.yigou.gxzy.ui.home.NavigationAdapter;
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
    private String bookId = null;

    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_tab_net_list;
    }

    TabNavBody bookInfo = null;

    @Override
    protected void initView() {
        setupViews();
        setupViewPager();
        // 注意：底部导航不能在 here 建。BaseActivity.initActivity() 的调用顺序是
        // initLayout() → initView() → initData()，而 bookInfo 要到 initData() 里的
        // validateAndGetBookArgs() 才赋值。若在 initView() 建导航，会因 bookInfo==null
        // 提前 return，导航项一个都不加，RecyclerView 高度塌成 0 → 底部整排标签消失。
        // 因此改到 initData() 中「Fragment 列表建好之后」再构建，见 buildNavigation()。
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
        // 验证书籍信息并获取参数（内部会给 bookInfo 赋值）
        BookArgs bookArgs = validateAndGetBookArgs();
        if (bookArgs == null) {
            return;
        }
        
        // 设置Fragment适配器（决定底部标签的数量与顺序）
        setupFragmentAdapter(bookArgs);
        
        // 建底部导航：必须排在 setupFragmentAdapter 之后，
        // 既需要 bookInfo，也需要 mPagerAdapter 已就位（标签数量要与 Fragment 数量一致）
        buildNavigation();
        
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
        bookId = getIntent().getStringExtra("bookId");
        if (bookId == null || bookId.isEmpty()) {
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
        // 先判空再取 size：原写法是`fragmentIndex < mPagerAdapter.getItemCount()` 在前、
        // `mPagerAdapter != null` 在后，mPagerAdapter 为 null 时会先解引用抛 NPE，
        // 后面的判空形同虚设。
        if (mPagerAdapter == null || mNavigationAdapter == null) {
            return false;
        }
        return fragmentIndex >= 0 &&
               fragmentIndex < mPagerAdapter.getItemCount();
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
     * Fragment类型常量（已迁移到 ContentTypes）
     * @deprecated 使用 ContentTypes.FANG/YAO/HAN_ZHI_UNIT 替代
     */
    @Deprecated
    private static final int FRAGMENT_TYPE_FANG = ContentTypes.FANG;
    @Deprecated
    private static final int FRAGMENT_TYPE_YAO = ContentTypes.YAO;
    @Deprecated
    private static final int FRAGMENT_TYPE_UNIT = ContentTypes.HAN_ZHI_UNIT;
    private static final int CASE_TAG_SHANGHAN = 5;  // 伤寒类书籍

    /**
     * caseTag（书籍分类，取自后端 WorkInfo.Case）具名常量。
     *
     * <p>注意：这些是「书籍分类」，与 {@link ContentTypes} 的「内容类型」是两套东西，
     * 数值并不通用，不要混用。
     *
     * <p>判读规则：黄帝内经(1)、本草(2、3) 不展示方药/单位标签；伤寒(5) 展示单位标签。
     */
    private static final int CASE_TAG_NEIJING = 1;
    private static final int CASE_TAG_BENCAO_A = 2;
    private static final int CASE_TAG_BENCAO_B = 3;

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
        if (shouldShowUnitTab()) {
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
        // 黄帝内经(1)、本草(2、3) 不显示方药，其余显示
        return !isBookCaseTag(CASE_TAG_NEIJING, CASE_TAG_BENCAO_A, CASE_TAG_BENCAO_B);
    }

    /**
     * 判断是否显示单位（汉制单位）标签
     */
    private boolean shouldShowUnitTab() {
        return isBookCaseTag(CASE_TAG_SHANGHAN);
    }

    /**
     * 统一的 caseTag 判读入口：bookInfo 为 null 时一律返回 false。
     *
     * <p>原先散落在 shouldShowFangYaoTabs() 与 addUnitFragmentIfNeeded() /
     * addUnitNavigationItemIfNeeded() 里各写一遍 `bookInfo != null && bookInfo.getCaseTag() == x`，
     * 容易漏判null。这里收口后，Fragment 列表与底部导航标签共用同一套判读，
     * 两者数量必然一致（不一致会导致标签点得动、内容不换页）。
     */
    private boolean isBookCaseTag(int... caseTags) {
        if (bookInfo == null) {
            return false;
        }
        int caseTag = bookInfo.getCaseTag();
        for (int tag : caseTags) {
            if (caseTag == tag) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建底部导航（FRM 标签栏）
     *
     * <p>分两步：先建adapter 并挂到 RecyclerView，再按 bookInfo 填充导航项。
     * 这样即使 bookInfo 为 null 也不会「一建就整体放弃」，而是得到一个空但可用的栏，
     * 不会静默塌成高度 0 而让人以为整排标签消失。
     *
     * <p>必须在 setupFragmentAdapter() 之后调用：标签项要与 Fragment 列表逐项对应。
     */
    private void buildNavigation() {
        if (mNavigationView == null) {
            return;
        }

        mNavigationAdapter = new NavigationAdapter(this);
        mNavigationAdapter.setOnNavigationListener(this);
        mNavigationView.setAdapter(mNavigationAdapter);

        if (bookInfo == null) {
            // 无书籍信息时不编造标签；validateAndGetBookArgs() 已 toast 并 finish()，
            // 这里只保证 RecyclerView 已 attach，不会是一片空白塌陷。
            return;
        }

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

        // NavigationAdapter 的列数是在 onAttachedToRecyclerView 时按当时的 getCount() 定的，
        // 而挂载发生在上面的 setAdapter()、此时还没加项 → 列数会按空数据的默认 4 列算。
        // 填充完必须重设一次列数，否则标签个数与列数不匹配会排版错乱。
        mNavigationAdapter.updateLayoutManager(mNavigationView);

        // 选中态跟随当前页，避免首屏无高亮
        mNavigationAdapter.setSelectedPosition(mViewPager != null
                ? mViewPager.getCurrentItem()
                : 0);
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
        if (shouldShowUnitTab()) {
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
