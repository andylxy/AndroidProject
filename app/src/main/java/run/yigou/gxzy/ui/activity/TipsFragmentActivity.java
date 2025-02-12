package run.yigou.gxzy.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.base.FragmentPagerAdapter;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.common.BookArgs;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.ui.adapter.NavigationAdapter;
import run.yigou.gxzy.ui.fragment.TipsBookNetReadFragment;
import run.yigou.gxzy.ui.fragment.TipsFangYaoFragment;
import run.yigou.gxzy.ui.fragment.TipsSettingFragment;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;

public final class TipsFragmentActivity extends AppActivity implements NavigationAdapter.OnNavigationListener {
    private static final String INTENT_KEY_IN_FRAGMENT_INDEX = "fragmentIndex";
    private ViewPager mViewPager;
    private RecyclerView mNavigationView;
    private NavigationAdapter mNavigationAdapter;
    private FragmentPagerAdapter<AppFragment<?>> mPagerAdapter;
    private int bookId = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.tips_fragment_tab_net_list;
    }


    @Override
    protected void initView() {
        // 给这个 View 设置沉浸式，避免状态栏遮挡
        ImmersionBar.setTitleBar(this, findViewById(R.id.tips_fragment_pager));
        // 注册事件
        // XEventBus.getDefault().register(this);
        // 从意图中获取书籍ID
        bookId = getIntent().getIntExtra("bookId", 0);
        if (bookId == 0) {
            // 如果书籍ID为0，则显示提示信息并返回
            toast("获取书籍信息错误");
            return;
        }

        mViewPager = findViewById(R.id.tips_fragment_pager);
        mNavigationView = findViewById(R.id.tips_fragment_navigation);

        mNavigationAdapter = new NavigationAdapter(this);

        TabNavBody tabNav = TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
        if (tabNav == null) {
            toast("获取书籍信息错误");
            return;
        }
        String bookName = tabNav.getBookName().split("[.,・]").length == 0 ? tabNav.getBookName() : tabNav.getBookName().split("[.,・]")[0];

        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(bookName,
                ContextCompat.getDrawable(this, R.drawable.list_selector)));

        /*
            如果是黄帝内经和本草类型，则不显示方药和药单位
         */
        {

            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(bookName +getString(R.string.tips_nav_fang),
                    ContextCompat.getDrawable(this, R.drawable.list_selector)));
            mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.tips_nav_yao),
                    ContextCompat.getDrawable(this, R.drawable.ruler_selector)));
        }

//        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.tips_nav_unit),
//                ContextCompat.getDrawable(this, R.drawable.ruler_selector)));
        mNavigationAdapter.addItem(new NavigationAdapter.MenuItem(getString(R.string.tips_nav_set),
                ContextCompat.getDrawable(this, R.drawable.settings_selector)));
        mNavigationAdapter.setOnNavigationListener(this);
        mNavigationView.setAdapter(mNavigationAdapter);

    }


    @Override
    protected void initData() {

        int bookLastReadPosition = getIntent().getIntExtra("bookLastReadPosition", 0);
        boolean isShow = getIntent().getBooleanExtra("bookCollect", false);
        BookArgs bookArgs = BookArgs.newInstance(bookId, bookLastReadPosition, isShow);
        mPagerAdapter = new FragmentPagerAdapter<>(this);
        mPagerAdapter.addFragment(TipsBookNetReadFragment.newInstance(bookArgs));


         /*
            如果是黄帝内经和本草类型，则不显示方药和药单位
        */
        {   // true 为方，false为药
            mPagerAdapter.addFragment(TipsFangYaoFragment.newInstance(true));
            mPagerAdapter.addFragment(TipsFangYaoFragment.newInstance(false));
        }
       // mPagerAdapter.addFragment(TipsYaoUnitShowFragment.newInstance());
        mPagerAdapter.addFragment(TipsSettingFragment.newInstance(bookArgs));
        mViewPager.setAdapter(mPagerAdapter);
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

        switch (fragmentIndex) {
            case 0:
            case 1:
            case 2:
            case 3:
                mViewPager.setCurrentItem(fragmentIndex);
                mNavigationAdapter.setSelectedPosition(fragmentIndex);
                break;
            default:
                break;
        }
    }

    /**
     * @param position
     * @return
     */
    @Override
    public boolean onNavigationItemSelected(int position) {
        switch (position) {
            case 0:
            case 1:
            case 2:
            case 3:
                mViewPager.setCurrentItem(position);
                return true;
            default:
                return false;
        }
    }


    @Override
    protected void onDestroy() {
        mViewPager.setAdapter(null);
        mViewPager = null;
        mNavigationView.setAdapter(null);
        mNavigationView = null;
        mNavigationAdapter.setOnNavigationListener(null);
        mNavigationAdapter = null;
        super.onDestroy();
    }

}
