package run.yigou.gxzy.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.ResultCallback;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.APPCONST;
import run.yigou.gxzy.common.Language;
import run.yigou.gxzy.common.ReadStyle;
import run.yigou.gxzy.common.Setting;
import run.yigou.gxzy.common.SysManager;
import run.yigou.gxzy.creator.DialogCreator;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.service.BookService;
import run.yigou.gxzy.greendao.service.ChapterService;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.BookDetailList;
import run.yigou.gxzy.http.api.GetChapterDetail;
import run.yigou.gxzy.http.entitymodel.ChapterInfoBody;
import run.yigou.gxzy.http.entitymodel.ChapterList;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.adapter.BookReadContenAdapter;
import run.yigou.gxzy.ui.adapter.ChapterTitleAdapter;
import run.yigou.gxzy.utils.BrightUtil;
import run.yigou.gxzy.utils.ConvertHtmlColorsHelper;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.StringHelper;


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
    private boolean settingChange = false;//是否是设置改变

    // private BookInfoNav.Bean.NavItem mNavItem;

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
    private LinearLayoutManager mLinearLayoutManager;

    public float width = 0;
    public float height = 0;
    /**
     * 屏幕上部  = 屏幕高度 / 3
     */
    private float settingOnClickValidFrom;
    /**
     * 屏幕下部  = 屏幕高度 / 3  * 2
     */
    private float settingOnClickValidTo;
    private float pointX;
    private float pointY;
    private float scrolledX;
    private float scrolledY;
    /**
     * 是否开启自动滑动
     */
    private boolean autoScrollOpening = false;
    /**
     * 上次点击时间
     */
    private long lastOnClickTime;
    /**
     * 双击确认时间
     */
    private long doubleOnClickConfirmTime = 200;
    /**
     * 设置视图
     */
    private Dialog mSettingDialog;
    /**
     * 详细设置视图
     */
    private Dialog mSettingDetailDialog;
    public Book mBook;
    /**
     * 是否已收藏
     */
    private boolean isStoreBook;
    private boolean isSearch;
    private boolean isFirstInit = true;
    private BookService mBookService;
    private ChapterService mChapterService;
    private ArrayList<Chapter> mChapters = new ArrayList<>();
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //设置背景
                    SetDayStyle();
                    break;
                case 1:
                    //SetDayStyle();
                    initViewData();
                    break;
                case 2:
                    mPbLoading.setVisibility(View.GONE);
                    mSrlContent.finishLoadMore();
                    break;
                case 3:
                    int position = msg.arg1;
                    mRvContent.scrollToPosition(position);
//                    if (position >= mChapters.size() - 1) {
//                        delayTurnToChapter(position);
//                    }
                    mPbLoading.setVisibility(View.GONE);
                    break;
                case 4:
                    position = msg.arg1;
                    mRvContent.scrollToPosition(position);
                    if (mBook.getHisttoryChapterNum() < position) {
                        delayTurnToChapter(position);
                    }
                    mPbLoading.setVisibility(View.GONE);
                    break;
                case 5:
                    saveLastChapterReadPosition(msg.arg1);
                    break;
                case 6:
                    mRvContent.scrollBy(0, mBook.getLastReadPosition());
                    mBook.setLastReadPosition(0);
                    if (!StringHelper.isEmpty(mBook.getId())) {
                        mBookService.updateEntity(mBook);
                    }
                    break;
                case 7:
                    if (mLinearLayoutManager != null) {
                        mRvContent.scrollBy(0, 2);
                    }
                    break;
                case 8:
                    showSettingView();
                    break;
                case 9:
                    //updateDownloadProgress((TextView) msg.obj);
                    break;
            }
        }
    };

    public static void start(Context context, Book item) {
        Intent intent = new Intent(context, BookReadActivity.class);
        intent.putExtra(APPCONST.BOOK, item);
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

        //initViewData();
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


        mRvContent.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //页面初始化的时候不要执行
                if (!isFirstInit) {

                    postDelayed(() -> {
                        saveLastChapterReadPosition(dy);
                    }, 200);

                } else {
                    isFirstInit = false;
                }
            }
        });

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
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                //打开手势滑动
                mDlReadActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                //关闭手势滑动
                mDlReadActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });


    }

    /**
     * 显示设置视图
     */
    private void showSettingView() {
        autoScrollOpening = false;
        if (mSettingDialog != null) {
            mSettingDialog.show();
        } else {
            int progress = mChapters.size();
//            if (mChapters.size() != 1) {
//                int lastPosition = mLinearLayoutManager.findLastVisibleItemPosition();
//                int mChapterNum = mChapters.size();
//                progress = lastPosition * 100 / (mChapterNum - 1);
//            }
            //缓存整本
            mSettingDialog = DialogCreator.createReadSetting(getActivity(), mSetting.isDayStyle(), progress, view -> {//返回
                        getActivity().finish();
                    }, view -> {//上一章
                        int curPosition = mLinearLayoutManager.findFirstVisibleItemPosition();
                        if (curPosition > 0) {
                            mRvContent.scrollToPosition(curPosition - 1);
                            selectedChapterPosition(curPosition - 1);
                        }
                    }, view -> {//下一章
                        int curPosition = mLinearLayoutManager.findLastVisibleItemPosition();
                        if (curPosition < mChapters.size() - 1) {
                            mRvContent.scrollToPosition(curPosition + 1);
                            delayTurnToChapter(curPosition + 1);
                            selectedChapterPosition(curPosition + 1);
                        }
                    }, view -> {//目录

                        mDlReadActivity.openDrawer(GravityCompat.START);
                        mSettingDialog.dismiss();

                    }, (dialog, view, isDayStyle) -> {//日夜切换

                        changeNightAndDaySetting(isDayStyle);
                    }, view -> {//设置
                        showSettingDetailView();
                    }, new SeekBar.OnSeekBarChangeListener() {
                        // todo 阅读进度待完善处理
                        // 当进度值改变时触发
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            mPbLoading.setVisibility(View.VISIBLE);
                            seekBar.setProgress(70);
                            final int chapterNum = (mChapters.size() - 1) * i / 100;
//                            getChapterContent(mChapters.get(chapterNum), new ResultCallback() {
//                                @Override
//                                public void onFinish(Object o, int code) {
//                                    mChapters.get(chapterNum).setContent((String) o);
//                                    mChapterService.saveOrUpdateChapter(mChapters.get(chapterNum));
//                                    mHandler.sendMessage(mHandler.obtainMessage(4, chapterNum, 0));
//                                }
//
//                                @Override
//                                public void onError(Exception e) {
//                                    mHandler.sendMessage(mHandler.obtainMessage(1));
//                                }
//                            });

                        }

                        // 当用户开始拖动 SeekBar 时触发
                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        // 当用户停止拖动 SeekBar 时触发
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    }
                    , null, (dialog, view, tvDownloadProgress) -> {
                        //下载章节缓存-不启用
//                        if (StringHelper.isEmpty(mBook.getId())) {
//                            addBookToCaseAndDownload(tvDownloadProgress);
//                        } else {
//                            getAllChapterData(tvDownloadProgress);
//                        }

                    });
        }

    }

    /**
     * 显示详细设置视图
     */
    private void showSettingDetailView() {
        mSettingDialog.dismiss();
        if (mSettingDetailDialog != null) {
            mSettingDetailDialog.show();
        } else {
            mSettingDetailDialog = DialogCreator.createReadDetailSetting(getActivity(), mSetting,
                    readStyle -> changeStyle(readStyle), v -> reduceTextSize(), v -> increaseTextSize(), v -> {
                        if (mSetting.getLanguage() == Language.simplified) {
                            mSetting.setLanguage(Language.traditional);
                        } else {
                            mSetting.setLanguage(Language.simplified);
                        }
                        SysManager.saveSetting(mSetting);
                        settingChange = true;
                        SetDayStyle();
                    }, v -> {
                        //字体页面
                        Intent intent = new Intent(getActivity(), FontsActivity.class);
                        startActivityForResult(intent, APPCONST.REQUEST_FONT);
                    }, v -> {
                        autoScroll();
                        mSettingDetailDialog.dismiss();
                    });
        }
    }

    /**
     * 白天夜间改变
     *
     * @param isCurDayStyle
     */
    private void changeNightAndDaySetting(boolean isCurDayStyle) {
        mSetting.setDayStyle(!isCurDayStyle);
        SysManager.saveSetting(mSetting);
        settingChange = true;
        //设置阅读窗口的背景色
        SetDayStyle();
    }

    /**
     * 缩小字体
     */
    private void reduceTextSize() {
        if (mSetting.getReadWordSize() > 1) {
            mSetting.setReadWordSize(mSetting.getReadWordSize() - 1);
            SysManager.saveSetting(mSetting);
            settingChange = true;
            //设置阅读窗口的背景色
            SetDayStyle();
        }
    }

    /**
     * 增大字体
     */
    private void increaseTextSize() {
        if (mSetting.getReadWordSize() < 40) {
            mSetting.setReadWordSize(mSetting.getReadWordSize() + 1);
            SysManager.saveSetting(mSetting);
            settingChange = true;
            //设置阅读窗口的背景色
            SetDayStyle();
        }
    }

    /**
     * 改变阅读风格
     *
     * @param readStyle
     */
    private void changeStyle(ReadStyle readStyle) {
        settingChange = true;
        if (!mSetting.isDayStyle()) mSetting.setDayStyle(true);
        mSetting.setReadStyle(readStyle);
        switch (readStyle) {
            case common:
                mSetting.setReadBgColor(R.color.sys_common_bg);
                mSetting.setReadWordColor(R.color.sys_common_word);
                break;
            case leather:
                mSetting.setReadBgColor(R.mipmap.theme_leather_bg);
                mSetting.setReadWordColor(R.color.sys_leather_word);
                break;
            case protectedEye:
                mSetting.setReadBgColor(R.color.sys_protect_eye_bg);
                mSetting.setReadWordColor(R.color.sys_protect_eye_word);
                break;
            case breen:
                mSetting.setReadBgColor(R.color.sys_breen_bg);
                mSetting.setReadWordColor(R.color.sys_breen_word);
                break;
            case blueDeep:
                mSetting.setReadBgColor(R.color.sys_blue_deep_bg);
                mSetting.setReadWordColor(R.color.sys_blue_deep_word);
                break;
        }
        SysManager.saveSetting(mSetting);
        //设置阅读窗口的背景色
        SetDayStyle();
    }

    /**
     * 自动滚动
     */
    private void autoScroll() {
        autoScrollOpening = true;
//        new Thread(() -> {
//            while (autoScrollOpening) {
//                try {
//                    Thread.sleep(mSetting.getAutoScrollSpeed() + 1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                mHandler.sendMessage(mHandler.obtainMessage(7));
//
//            }
//        }).start();

        postDelayed(() -> {
            while (autoScrollOpening) {
                try {
                    Thread.sleep(mSetting.getAutoScrollSpeed() + 1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // mHandler.sendMessage(mHandler.obtainMessage(7));
                if (mLinearLayoutManager != null) {
                    mRvContent.scrollBy(0, 2);
                }
            }
        }, 50);
    }

    /**
     * 初始化阅读界面点击事件
     */
    private void initReadViewOnClick() {
        mBookReadContenAdapter.setmOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //获取点击坐标(点击屏幕的位置)
                pointY = event.getRawY();
                return false;

            }
        });

        mBookReadContenAdapter.setmOnClickItemListener(new BookReadContenAdapter.OnClickItemListener() {
            @Override
            public void onClick(View view, final int positon) {
                //屏幕可见位置
                int firstVisibleItemPosition = mLinearLayoutManager.findFirstVisibleItemPosition();
                int lastVisibleItemPosition = mLinearLayoutManager.findLastVisibleItemPosition();
                //点击位置为屏幕中间,显示菜单窗口
                if (pointY > settingOnClickValidFrom && pointY < settingOnClickValidTo) {
                    autoScrollOpening = false;
                    //首次点击时间
                    long curOnClickTime = DateHelper.getLongDate();
                    //两次点击间隔时间少于200,侧自动滚屏
                    if (curOnClickTime - lastOnClickTime < doubleOnClickConfirmTime) {
                        autoScroll();
                    } else {
                        //显示菜单窗口
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    Thread.sleep(doubleOnClickConfirmTime);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                if (!autoScrollOpening) {
//                                    mHandler.sendMessage(mHandler.obtainMessage(8));
//                                }
//
//                            }
//                        }).start();

                        postDelayed(() -> {
                            try {
                                Thread.sleep(doubleOnClickConfirmTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (!autoScrollOpening) {
                                //mHandler.sendMessage(mHandler.obtainMessage(8));
                                if (!isSearch)
                                    showSettingView();
                            }
                        }, 300);


                    }
                    //记录首次击点时间
                    lastOnClickTime = curOnClickTime;
                } else if (pointY > settingOnClickValidTo) {
                    //下一章
                    if (firstVisibleItemPosition < lastVisibleItemPosition) {
                        selectedChapterPosition(lastVisibleItemPosition);
                    }

                    mRvContent.scrollBy(0, (int) height);
                } else if (pointY < settingOnClickValidFrom) {
                    //上一章
                    if (firstVisibleItemPosition < lastVisibleItemPosition) {

                        selectedChapterPosition(firstVisibleItemPosition);
                    }
                    mRvContent.scrollBy(0, (int) -height);
                }
            }
        });
    }


    private void initViewData() {

        mSetting = SysManager.getSetting();

        //// 禁用上拉加载更多功能
        mSrlContent.setEnableLoadMore(false);
        //// 禁用下拉刷新功能
        mSrlContent.setEnableRefresh(false);
        mPbLoading.setVisibility(View.GONE);
        //内容显示
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        //mLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        mRvContent.setLayoutManager(mLinearLayoutManager);

        mBookReadContenAdapter = new BookReadContenAdapter(getActivity());
        mBookReadContenAdapter.setData(mChapters);
        mRvContent.setAdapter(mBookReadContenAdapter);
        if (!settingChange) {
            mRvContent.scrollToPosition(mBook.getHisttoryChapterNum());
            delayTurnToLastChapterReadPosion();

        } else {
            settingChange = false;
        }
        //mSrlContent.finishLoadMore();
        //设置目录
        mChapterTitleAdapter = new ChapterTitleAdapter(getContext());
        //侧页列表项的点击事件监听器
        mChapterTitleAdapter.setOnItemClickListener((adapterView, view, i) -> {

            //选中指定Item关闭侧滑菜单
            mDlReadActivity.closeDrawer(GravityCompat.START);
            final int position;
            if (curSortflag == 0) {
                //正序
                position = i;
            } else {
                //倒序
                position = mChapters.size() - 1 - i;
            }
            //打开的章节是否为空
            if (StringHelper.isEmpty(mChapters.get(position).getMSection())) {
                mPbLoading.setVisibility(View.VISIBLE);
                //请求当前章节内容
                getChapterContent(position, new ResultCallback() {
                    @Override
                    public void onFinish(Object o, int code) {
                        //更新章节内容
                        if (position == (int) o) {
                            //通知UI刷新画布
                            // mHandler.sendMessage(mHandler.obtainMessage(4, position, 0));
                            postDelayed(() -> {
                                mRvContent.scrollToPosition(position);
                                if (mBook.getHisttoryChapterNum() < position) {
                                    delayTurnToChapter(position);
                                }
                                mPbLoading.setVisibility(View.GONE);
                            }, 50);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                    }
                });
            } else {
                //显示章节内容
//                    mReadActivity.getLvContent().setSelection(position);
                mRvContent.scrollToPosition(position);
                if (position > mBook.getHisttoryChapterNum()) {
                    delayTurnToChapter(position);
                    postDelayed(() -> {
                        mRvContent.scrollToPosition(position);
                        if (mBook.getHisttoryChapterNum() < position) {
                            delayTurnToChapter(position);
                        }
                        mPbLoading.setVisibility(View.GONE);
                    }, 50);
                }
            }
            selectedChapterPosition(position);
        });


        //初始化目录数据
        mChapterTitleAdapter.setData(mChapters);
        //跳到指定目录位置
        // selectedChapterPosition(0);
        mLvChapterList.setAdapter(mChapterTitleAdapter);
        //设置阅读窗口的背景色
        SetDayStyle();
        //设置ReadContenAdapter请求网络数据
        setGetChapterContent();
        //初始化内容点击事件
        DisplayMetrics dm = new DisplayMetrics();
        //获取屏幕宽高
        if (width == 0 || height == 0) {
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            width = dm.widthPixels;
            height = dm.heightPixels;
        }
        //设置点击屏幕范围
        settingOnClickValidFrom = height / 3 + 200;
        settingOnClickValidTo = height / 3 * 2 - 100;
        initReadViewOnClick();
    }

    /**
     * 当前显示在目录标记
     */
    private void selectedChapterPosition(int position) {

        mChapterTitleAdapter.setCurChapterPosition(position);
        //指定位置的项到可见区域
        mLvChapterList.scrollToPosition(position);
        //刷新目录
        mChapterTitleAdapter.notifyDataSetChanged();
    }

    private void setGetChapterContent() {
        mBookReadContenAdapter.setOnChapterContentListener(new BookReadContenAdapter.OnChapterContentListener() {
            @Override
            public void getChapter(int postion, ResultCallback callback) {
                // getChapterContent( postion,callback);
                getChapterContent(postion, callback);
            }
        });

    }

    /**
     * 延迟跳转章节位置
     */
    private void delayTurnToLastChapterReadPosion() {
//        new Thread(() -> {
//            try {
//                Thread.sleep(100);
//                mHandler.sendMessage(mHandler.obtainMessage(6));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();

        postDelayed(() -> {
            mRvContent.scrollBy(0, mBook.getLastReadPosition());
            mBook.setLastReadPosition(0);
            if (!StringHelper.isEmpty(mBook.getId())) {
                mBookService.updateEntity(mBook);
            }
        }, 50);

    }

    /**
     * 延迟跳转章节(防止跳到章节尾部)
     */
    private void delayTurnToChapter(final int position) {
//        new Thread(() -> {
//            try {
//                Thread.sleep(50);
//                mHandler.sendMessage(mHandler.obtainMessage(4, position, 0));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();

        postDelayed(() -> {
            mRvContent.scrollToPosition(position);
            if (mBook.getHisttoryChapterNum() < position) {
                delayTurnToChapter(position);
            }
            mPbLoading.setVisibility(View.GONE);
        }, 50);

    }

    /**
     * 保存最后阅读章节的进度
     */
    private void saveLastChapterReadPosition(int dy) {
        if (mLinearLayoutManager == null || isSearch) return;
        if (mLinearLayoutManager.findFirstVisibleItemPosition() != mLinearLayoutManager.findLastVisibleItemPosition()
                || dy == 0) {
            mBook.setLastReadPosition(0);
        } else {
            mBook.setLastReadPosition(mBook.getLastReadPosition() + dy);
        }
        mBook.setHisttoryChapterNum(mLinearLayoutManager.findLastVisibleItemPosition());
        if (!StringHelper.isEmpty(mBook.getId())) {
            mBookService.updateEntity(mBook);
        }
    }

    private void getChapterContent(int position, ResultCallback callback) {
        EasyHttp.get(this)
                .api(new GetChapterDetail().setId(mChapters.get(position).getUrl()))
                .request(new HttpCallback<HttpData<GetChapterDetail.Bean>>(this) {
                    @Override
                    public void onSucceed(HttpData<GetChapterDetail.Bean> data) {
                        if (data.getData() != null) {
                            GetChapterDetail.Bean bean = data.getData();
                            Chapter chapter = mChapters.get(position);
                                chapter.setId(UUID.randomUUID().toString());
                                chapter.setBookId(bean.getId() + "");
                                chapter.setTitle(bean.getTitle());
                                chapter.setParentId(bean.getParentId());
                                chapter.setMTitleColor(bean.getTitleColor());
                                chapter.setParentId(bean.getParentId());
                                //  mChapters.get(position).;
                                for (ChapterInfoBody body : bean.getChapterInfoBody()) {
                                    chapter.setMSection(ConvertHtmlColorsHelper.convertHtmlColors(body.getSection()));
                                    chapter.setMSectionNote(ConvertHtmlColorsHelper.convertHtmlColors(body.getSectionNote()));
                                    chapter.setMSectionVideoMemo(ConvertHtmlColorsHelper.convertHtmlColors(body.getSectionVideoMemo()));
                                    chapter.setMFangJi(ConvertHtmlColorsHelper.convertHtmlColors(body.getFangJi()));
                                    chapter.setMFangJiZhujie(ConvertHtmlColorsHelper.convertHtmlColors(body.getFangJiZhujie()));
                                    chapter.setAiJiu(ConvertHtmlColorsHelper.convertHtmlColors(body.getAiJiu()));
                                    chapter.setBieMing(ConvertHtmlColorsHelper.convertHtmlColors(body.getBieMing()));
                                    chapter.setShiCi(ConvertHtmlColorsHelper.convertHtmlColors(body.getShiCi()));
                                    chapter.setTeDian(ConvertHtmlColorsHelper.convertHtmlColors(body.getTeDian()));
                                    chapter.setShiYi(ConvertHtmlColorsHelper.convertHtmlColors(body.getShiYi()));
                                    chapter.setNotes(ConvertHtmlColorsHelper.convertHtmlColors(body.getNotes()));
                                    chapter.setZhuZhi(ConvertHtmlColorsHelper.convertHtmlColors(body.getZhuZhi()));
                                    chapter.setZhenJiu(ConvertHtmlColorsHelper.convertHtmlColors(body.getZhenJiu()));
                                    chapter.setPeiWu(ConvertHtmlColorsHelper.convertHtmlColors(body.getPeiWu()));
                                }
                            //更新章节内容
                            if (!isStoreBook)
                                mChapterService.updateChapter(mChapters.get(position));
                            if (chapter.getMSection() ==null)chapter.setMSection("内容走失,稍后再试");
                            callback.onFinish(position, 200);
                        }

                    }


                });
    }


    private void SetDayStyle() {

        //亮度
        if (!mSetting.isBrightFollowSystem()) {
            BrightUtil.setBrightness(getActivity(), mSetting.getBrightProgress());
        }

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
        //刷新控件
        if (settingChange)
            mBookReadContenAdapter.notifyDataSetChanged();
    }

    private void dataSetting() {

        //接收传入书的信息
        mBook = getSerializable(APPCONST.BOOK);
        isStoreBook = StringHelper.isEmpty(mBook.getId());
        isSearch = mBook.getSource() != null && mBook.getSource().equals("Search");
        //显示进度条(不显示使用)
        // mPbLoading.setVisibility(View.VISIBLE);
        //初始化富文本
        //在第一次调用RichText之前先调用RichText.initCacheDir()方法设置缓存目录，不设置会报错
        //RichText.initCacheDir(this.getContext());
        mBookService = DbService.getInstance().mBookService;//new BookService();
        mChapterService = DbService.getInstance().mChapterService;// new ChapterService();
        getBookDetailList();

    }

//    @Override
//    public void onBackPressed() {
//        // 执行返回操作
//        // ...
//        saveLastChapterReadPosition(0);
//        // 最后调用 super.onBackPressed() 执行默认的返回行为
//        super.onBackPressed();
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case APPCONST.REQUEST_FONT:
                if (resultCode == RESULT_OK) {
                    settingChange = true;
                    SetDayStyle();
                }
                break;
        }
    }


    private void getBookDetailList() {
        if (!isSearch) {
            mChapters = (ArrayList<Chapter>) mChapterService.findBookAllChapterByBookId(mBook.getId());
            ArrayList<Chapter> chapters = new ArrayList<>();
            EasyHttp.get(this)
                    .api(new BookDetailList().setId(mBook.getBookId()))
                    .request(new HttpCallback<HttpData<List<BookDetailList.Bean>>>(this) {
                        @Override
                        public void onSucceed(HttpData<List<BookDetailList.Bean>> data) {
                            if (data != null && data.getData().size() > 0) {
                                List<BookDetailList.Bean> detailList = data.getData();
                                int i = 0;
                                try {
                                    for (BookDetailList.Bean bean : detailList) {
                                        for (ChapterList chapt : bean.getChapterLists()) {
                                            Chapter chapter = new Chapter();
                                            chapter.setNumber(i++);
                                            chapter.setParentId(bean.getParentId());
                                            chapter.setTitle("" + chapt.getTitle());
                                            chapter.setUrl(chapt.getId() + "");
                                            chapters.add(chapter);
                                        }
                                    }
                                } catch (Exception e) {

                                    e.printStackTrace();
                                }
                                //是否存储到书架
                                if (isStoreBook) {
                                    mChapters.addAll(chapters);
                                } else {
                                    updateAllOldChapterData(chapters);
                                }
                                //   mHandler.sendMessage(mHandler.obtainMessage(1));
                                postDelayed(() -> {
                                    initViewData();
                                }, 50);
                            }

                        }

                        @Override
                        public void onFail(Exception e) {
                            initViewData();
                        }
                    });
        } else {
            EasyHttp.get(this)
                    .api(new GetChapterDetail().setId(mBook.getChapterUrl()).setKeywords(mBook.getDesc()))
                    .request(new HttpCallback<HttpData<GetChapterDetail.Bean>>(this) {
                        @Override
                        public void onSucceed(HttpData<GetChapterDetail.Bean> data) {
                            if (data != null) {
                                GetChapterDetail.Bean bean = data.getData();
                                for (ChapterInfoBody body : bean.getChapterInfoBody()) {
                                    Chapter chapter = new Chapter();
                                    chapter.setId(UUID.randomUUID().toString());
                                    chapter.setBookId(bean.getId() + "");
                                    chapter.setTitle(bean.getTitle());
                                    chapter.setMTitleColor(bean.getTitleColor());
                                    chapter.setParentId(bean.getParentId());
                                    chapter.setMNo(body.getNo()+"");
                                    chapter.setMSection(ConvertHtmlColorsHelper.convertHtmlColors(body.getSection()));
                                    chapter.setMSectionNote(ConvertHtmlColorsHelper.convertHtmlColors(body.getSectionNote()));
                                    chapter.setMSectionVideoMemo(ConvertHtmlColorsHelper.convertHtmlColors(body.getSectionVideoMemo()));
                                    chapter.setMFangJi(ConvertHtmlColorsHelper.convertHtmlColors(body.getFangJi()));
                                    chapter.setMFangJiZhujie(ConvertHtmlColorsHelper.convertHtmlColors(body.getFangJiZhujie()));
                                    chapter.setAiJiu(ConvertHtmlColorsHelper.convertHtmlColors(body.getAiJiu()));
                                    chapter.setBieMing(ConvertHtmlColorsHelper.convertHtmlColors(body.getBieMing()));
                                    chapter.setShiCi(ConvertHtmlColorsHelper.convertHtmlColors(body.getShiCi()));
                                    chapter.setTeDian(ConvertHtmlColorsHelper.convertHtmlColors(body.getTeDian()));
                                    chapter.setShiYi(ConvertHtmlColorsHelper.convertHtmlColors(body.getShiYi()));
                                    chapter.setNotes(ConvertHtmlColorsHelper.convertHtmlColors(body.getNotes()));
                                    chapter.setZhuZhi(ConvertHtmlColorsHelper.convertHtmlColors(body.getZhuZhi()));
                                    chapter.setZhenJiu(ConvertHtmlColorsHelper.convertHtmlColors(body.getZhenJiu()));
                                    chapter.setPeiWu(ConvertHtmlColorsHelper.convertHtmlColors(body.getPeiWu()));
                                    mChapters.add(chapter);
                                }
                                initViewData();

                            }

                        }
                    });
        }
    }

    /**
     * 更新所有章节
     *
     * @param newChapters
     */
    private void updateAllOldChapterData(ArrayList<Chapter> newChapters) {

        int i;
        for (i = 0; i < mChapters.size() && i < newChapters.size(); i++) {
            Chapter oldChapter = mChapters.get(i);
            Chapter newChapter = newChapters.get(i);
            if (!oldChapter.getTitle().equals(newChapter.getTitle())) {
                oldChapter.setTitle(newChapter.getTitle());
                oldChapter.setUrl(newChapter.getUrl());
                oldChapter.setMSection(null);
                mChapterService.updateEntity(oldChapter);
            }
        }
        if (mChapters.size() < newChapters.size()) {
            int start = mChapters.size();
            for (int j = mChapters.size(); j < newChapters.size(); j++) {
                newChapters.get(j).setId(UUID.randomUUID().toString());
                newChapters.get(j).setBookId(mBook.getId());
                mChapters.add(newChapters.get(j));
//                mChapterService.addChapter(newChapters.get(j));
            }
            mChapterService.addChapters(mChapters.subList(start, mChapters.size()));
        } else if (mChapters.size() > newChapters.size()) {
            for (int j = newChapters.size(); j < mChapters.size(); j++) {
                mChapterService.deleteEntity(mChapters.get(j));
            }
            mChapters.subList(0, newChapters.size());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //结束时清空内容
        //RichText.clear(this.getContext());
        // AppApplication.getApplication().shutdownThreadPool();
    }

    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }
}