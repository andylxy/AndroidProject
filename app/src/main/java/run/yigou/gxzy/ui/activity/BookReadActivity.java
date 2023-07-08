package run.yigou.gxzy.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.APPCONST;
import run.yigou.gxzy.common.Setting;
import run.yigou.gxzy.common.SysManager;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.ui.adapter.BookReadContenAdapter;
import run.yigou.gxzy.ui.adapter.ChapterTitleAdapter;
import run.yigou.gxzy.utils.BrightUtil;

/**
 * 作者:  zhs
 * 时间:  2023-07-07 14:11:13
 * 包名:  run.yigou.gxzy.ui.activity
 * 类名:  ReadActivity
 * 版本:  1.0
 * 描述:
 */
public final class BookReadActivity extends AppActivity {
    private static final String Book_KEY_IN = "book";
    private Setting mSetting;

    /**
     * 0正序  1倒序
     */
    private int curSortflag = 0;
    private boolean settingChange;//是否是设置改变

    private BookInfoNav.Bean.NavItem mNavItem;

    private DrawerLayout mDlReadActivity;
    private SmartRefreshLayout mSrlContent;
    private WrapRecyclerView mRvContent;
    private ProgressBar mPbLoading;
    private WrapRecyclerView mLvChapterList;
    private TextView mTvBookList;
    private TextView mTvChapterSort;
    private LinearLayout mLlChapterListView;
    private BookReadContenAdapter mBookReadContenAdapter;
    private ChapterTitleAdapter mChapterTitleAdapter;
    public float width = 0;
    public float height = 0;
    private float settingOnClickValidFrom;
    private float settingOnClickValidTo;

    public static void start(Context context, BookInfoNav.Bean.NavItem item) {
        Intent intent = new Intent(context, BookReadActivity.class);
        intent.putExtra(Book_KEY_IN, item);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {

        return R.layout.book_read_activity;
    }

    @Override
    protected void initView() {
        init();
    }

    @Override
    protected void initData() {
        mSetting = SysManager.getSetting();
        mNavItem = getSerializable(Book_KEY_IN);
        initViewData();
        dataSetting();
    }


    private void init() {
        //设置阅读窗口的背景色
        mDlReadActivity = findViewById(R.id.dl_read_activity);
        mSrlContent = findViewById(R.id.srl_content);
        //显示进度条
        mPbLoading = findViewById(R.id.pb_loading);
        //章节列表
        mLvChapterList = findViewById(R.id.lv_chapter_list);
        //目录
        mTvBookList = findViewById(R.id.tv_book_list);
        //目录排序
        mTvChapterSort = findViewById(R.id.tv_chapter_sort);
        //侧页
        mLlChapterListView = findViewById(R.id.ll_chapter_list_view);
        //内容显示
        mRvContent = findViewById(R.id.rv_content);
        mBookReadContenAdapter = new BookReadContenAdapter(getActivity());
        mRvContent.setAdapter(mBookReadContenAdapter);

        //侧页列表项的点击事件监听器
//        mLvChapterList.setOnItemClickListener((adapterView, view, i, l) -> {
//            //选中指定Item关闭侧滑菜单
//            mDlReadActivity.closeDrawer(GravityCompat.START);
//            final int position;
//            if (curSortflag == 0) {
//                //正序
//                position = i;
//            } else {
//                //倒序
//                position = mChapters.size() - 1 - i;
//            }
//            //打开的章节是否为空
//            if (StringHelper.isEmpty(mChapters.get(position).getContent())) {
//                mReadActivity.getPbLoading().setVisibility(View.VISIBLE);
//                //请求当前章节内容
//                BookStoreApi.getChapterContent(mChapters.get(position).getUrl(), new ResultCallback() {
//                    @Override
//                    public void onFinish(Object o, int code) {
//                        //更新章节内容
//                        mChapters.get(position).setContent((String) o);
//                        mChapterService.saveOrUpdateChapter(mChapters.get(position));
//                        //通知UI刷新画布
//                        mHandler.sendMessage(mHandler.obtainMessage(4, position, 0));
//                    }
//
//                    @Override
//                    public void onError(Exception e) {
//
//                    }
//                });
//            } else {
//                //显示章节内容
////                    mReadActivity.getLvContent().setSelection(position);
//                mReadActivity.getRvContent().scrollToPosition(position);
//                if (position > mBook.getHisttoryChapterNum()) {
//                    delayTurnToChapter(position);
//                }
//            }
//
//        });
//        mRvContent.addOnScrollListener(new RecyclerView.OnScrollListener() {
//
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, final int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//                //页面初始化的时候不要执行
//                if (!isFirstInit) {
//                    MyApplication.getApplication().newThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            saveLastChapterReadPosition(dy);
//                        }
//                    });
//                } else {
//                    isFirstInit = false;
//                }
//            }
//        });

//        mTvChapterSort.setOnClickListener(view -> {
//            if (curSortflag == 0) {//当前正序
//                mTvChapterSort.setText(mReadActivity.getString(R.string.positive_sort));
//                curSortflag = 1;
//                changeChapterSort();
//            } else {//当前倒序
//                mTvChapterSort.setText(mReadActivity.getString(R.string.inverted_sort));
//                curSortflag = 0;
//                changeChapterSort();
//            }
//        });

        //关闭手势滑动
        mDlReadActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDlReadActivity.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                //打开手势滑动
                mDlReadActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                //关闭手势滑动
                mDlReadActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });


    }


    private void initViewData() {
        //设置阅读窗口的背景色
        if (mSetting.isDayStyle()) {
            //白天
            mDlReadActivity.setBackgroundResource(mSetting.getReadBgColor());
            mTvBookList.setTextColor(getContext().getResources().getColor(mSetting.getReadWordColor()));
            mTvChapterSort.setTextColor(getContext().getResources().getColor(mSetting.getReadWordColor()));
            mLlChapterListView.setBackgroundResource(mSetting.getReadBgColor());

        } else {
            //晚上
            mDlReadActivity.setBackgroundResource(R.color.sys_night_bg);
            mTvBookList.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
            mTvChapterSort.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
            mLlChapterListView.setBackgroundResource(R.color.sys_night_bg);
        }

        //加载文章内容
        //initReadViewOnClick();

//        if (!settingChange) {
//            mRvContent.scrollToPosition(mBook.getHisttoryChapterNum());
//            delayTurnToLastChapterReadPosion();
//
//        } else {
//            settingChange = false;
//        }
        mPbLoading.setVisibility(View.GONE);
        //mSrlContent.finishLoadMore();

        int selectedPostion, curChapterPosition;
        mChapterTitleAdapter = new ChapterTitleAdapter(getContext());
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getActivity());
        //设置目录
        if (curSortflag == 0) {
            //findLastVisibleItemPosition 返回 最后一个可见项的位置索引
            curChapterPosition = mLinearLayoutManager.findLastVisibleItemPosition();
            //默认显示多少项
            selectedPostion = curChapterPosition - 5;
            if (selectedPostion < 0) selectedPostion = 0;
            if (mChapterTitleAdapter.getCount() - 1 - curChapterPosition < 5)
                selectedPostion = mChapterTitleAdapter.getCount();
            mChapterTitleAdapter.setCurChapterPosition(curChapterPosition);
        } else {
            // mChapterTitleAdapter = new ChapterTitleAdapter(mReadActivity, R.layout.listview_chapter_title_item, mInvertedOrderChapters);
            curChapterPosition = mChapterTitleAdapter.getCount() - 1 - mLinearLayoutManager.findLastVisibleItemPosition();
            selectedPostion = curChapterPosition - 5;
            if (selectedPostion < 0) selectedPostion = 0;
            if (mChapterTitleAdapter.getCount() - 1 - curChapterPosition < 5)
                selectedPostion = mChapterTitleAdapter.getCount();
            mChapterTitleAdapter.setCurChapterPosition(curChapterPosition);
        }
        mLvChapterList.setAdapter(mChapterTitleAdapter);
        //指定位置的项到可见区域
        mLvChapterList.scrollToPosition(selectedPostion);
    }

    private void dataSetting() {
        DisplayMetrics dm = new DisplayMetrics();
        //获取屏幕宽高
        if (width == 0 || height == 0) {
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            width = dm.widthPixels;
            height = dm.heightPixels;
        }
        if (!mSetting.isBrightFollowSystem()) {
            BrightUtil.setBrightness(getActivity(), mSetting.getBrightProgress());
        }
//        //接收传入书的信息
//        mBook = (Book) mReadActivity.getIntent().getSerializableExtra(APPCONST.BOOK);
//        if (TextUtils.isEmpty(mBook.getSource())) {
//            //设置书本来源
//            mBook.setSource(BookSource.ctwh.toString());
//            //更来源信息
//            mBookService.updateEntity(mBook);
//        }

        //设置点击屏幕范围
        settingOnClickValidFrom = height / 4;
        settingOnClickValidTo = height / 4 * 3;
        //// 禁用上拉加载更多功能
        mSrlContent.setEnableLoadMore(false);
        //// 禁用下拉刷新功能
        mSrlContent.setEnableRefresh(false);

        //显示进度条(不显示使用)
        // mPbLoading.setVisibility(View.VISIBLE);
        //初始化富文本
        //在第一次调用RichText之前先调用RichText.initCacheDir()方法设置缓存目录，不设置会报错
        //RichText.initCacheDir(this.getContext());
        //getData();
    }

}