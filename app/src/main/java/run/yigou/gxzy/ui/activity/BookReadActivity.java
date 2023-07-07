package run.yigou.gxzy.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.Setting;
import run.yigou.gxzy.common.SysManager;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.ui.adapter.BookReadContenAdapter;

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

    //0正序  1倒序
    private int curSortflag = 0;
    private boolean settingChange;//是否是设置改变
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
        initViewData();
    }

    private DrawerLayout mDlReadActivity;
    private SmartRefreshLayout mSrlContent;
    private WrapRecyclerView mRvContent;
    private ProgressBar mPbLoading;
    private WrapRecyclerView mLvChapterList;
    private TextView mTvBookList;
    private TextView mTvChapterSort;
    private LinearLayout mLlChapterListView;
    private BookReadContenAdapter mBookReadContenAdapter;
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

    }

    private void initViewData() {

        if (mSetting.isDayStyle()) {
            mDlReadActivity.setBackgroundResource(mSetting.getReadBgColor());
        } else {
            mDlReadActivity.setBackgroundResource(R.color.sys_night_bg);
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
        mSrlContent.finishLoadMore();
        if (mSetting.isDayStyle()) {
            mTvBookList.setTextColor(getContext().getResources().getColor(mSetting.getReadWordColor()));
            mTvChapterSort.setTextColor(getContext().getResources().getColor(mSetting.getReadWordColor()));
        } else {
            mTvBookList.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
            mTvChapterSort.setTextColor(getContext().getResources().getColor(R.color.sys_night_word));
        }
        if (mSetting.isDayStyle()) {
            mLlChapterListView.setBackgroundResource(mSetting.getReadBgColor());
        } else {
            mLlChapterListView.setBackgroundResource(R.color.sys_night_bg);
        }
        int selectedPostion, curChapterPosition;

        //设置布局管理器
//        if (curSortflag == 0) {
//            mChapterTitleAdapter = new ChapterTitleAdapter(mReadActivity, R.layout.listview_chapter_title_item, mChapters);
////            curChapterPosition = mReadActivity.getRvContent().getLastVisiblePosition();
//            curChapterPosition = mLinearLayoutManager.findLastVisibleItemPosition();
//            selectedPostion = curChapterPosition - 5;
//            if (selectedPostion < 0) selectedPostion = 0;
//            if (mChapters.size() - 1 - curChapterPosition < 5) selectedPostion = mChapters.size();
//            mChapterTitleAdapter.setCurChapterPosition(curChapterPosition);
//        } else {
//            mChapterTitleAdapter = new ChapterTitleAdapter(mReadActivity, R.layout.listview_chapter_title_item, mInvertedOrderChapters);
////            curChapterPosition = mChapters.size() - 1 - mReadActivity.getLvContent().getLastVisiblePosition();
//            curChapterPosition = mChapters.size() - 1 - mLinearLayoutManager.findLastVisibleItemPosition();
//            selectedPostion = curChapterPosition - 5;
//            if (selectedPostion < 0) selectedPostion = 0;
//            if (mChapters.size() - 1 - curChapterPosition < 5) selectedPostion = mChapters.size();
//            mChapterTitleAdapter.setCurChapterPosition(curChapterPosition);
//        }
//        mReadActivity.getLvChapterList().setAdapter(mChapterTitleAdapter);
//        mReadActivity.getLvChapterList().setSelection(selectedPostion);
    }
}