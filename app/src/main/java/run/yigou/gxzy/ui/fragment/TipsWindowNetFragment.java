

package run.yigou.gxzy.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import run.yigou.gxzy.EventBus.ChapterContentNotificationEvent;
import run.yigou.gxzy.EventBus.ShowUpdateNotificationEvent;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.gen.ChapterDao;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.BookContentApi;
import run.yigou.gxzy.http.api.BookFangApi;
import run.yigou.gxzy.http.api.ChapterContentApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.manager.ThreadPoolManager;
import run.yigou.gxzy.ui.activity.AboutActivity;
import run.yigou.gxzy.ui.activity.BookContentSearchActivity;
import run.yigou.gxzy.ui.activity.CopyActivity;
import run.yigou.gxzy.ui.activity.CopyActivityTest;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.adapter.BookInfoAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.utils.ThreadUtil;


public final class TipsWindowNetFragment extends TitleBarFragment<HomeActivity>
        implements OnRefreshLoadMoreListener,
        BaseAdapter.OnItemClickListener {

    public static TipsWindowNetFragment newInstance() {
        return new TipsWindowNetFragment();
    }

    public static TipsWindowNetFragment newInstance(List<TabNavBody> navList) {
        TipsWindowNetFragment bookInfoFragment = new TipsWindowNetFragment();
        bookInfoFragment.mNavList = navList;
        return bookInfoFragment;
    }

    private int bookId;
    private List<TabNavBody> mNavList;
    private SmartRefreshLayout mRefreshLayout;
    private WrapRecyclerView mRecyclerView;

    private BookInfoAdapter mAdapter;
    private boolean mLoading;
    /**
     * singleData 所有书籍 数据单例
     */
    TipsSingleData singleData;
    /**
     * 当前点击书本数据
     */
    SingletonNetData singletonNetData;

    @Override
    protected int getLayoutId() {
        return R.layout.book_info_fragment;
    }

    @Override
    protected void initView() {
        mRefreshLayout = findViewById(R.id.rl_status_refresh);
        mRecyclerView = findViewById(R.id.rv_status_list);
        mAdapter = new BookInfoAdapter(getAttachActivity());
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new CustomDividerItemDecoration(AppConst.CustomDivider_BookList_RecyclerView_Color, AppConst.CustomDivider_Height));
        mRefreshLayout.setOnRefreshLoadMoreListener(this);
        //表示禁用上拉加载更多功能。
        mRefreshLayout.setEnableLoadMore(false);
        // 禁用下拉刷新
        mRefreshLayout.setEnableRefresh(false);
    }

    @Override
    protected void initData() {
        singleData = TipsSingleData.getInstance();
        // List<BookInfoNav.Bean.NavList> navList =mNavList;
        mAdapter.setData(analogData());
        XEventBus.getDefault().register(TipsWindowNetFragment.this);
    }

    // 在 Fragment 中使用 WeakReference 来避免内存泄漏
    private WeakReference<Activity> weakActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        weakActivity = new WeakReference<>((Activity) context);
    }

    private List<TabNavBody> analogData() {
        return mNavList;
    }

    /**
     * {@link BaseAdapter.OnItemClickListener}
     *
     * @param recyclerView RecyclerView对象
     * @param itemView     被点击的条目对象
     * @param position     被点击的条目位置
     */
    @Override
    public void onItemClick(RecyclerView recyclerView, View itemView, int position) {

        bookId = mAdapter.getItem(position).getBookNo();
        singletonNetData = TipsSingleData.getInstance().getMapBookContent(bookId);
        if (singletonNetData.getYaoAliasDict() == null)
            singletonNetData.setYaoAliasDict(singleData.getYaoAliasDict());
        if (singletonNetData.getFangAliasDict() == null)
            singletonNetData.setFangAliasDict(singleData.getFangAliasDict());

        getBookData(bookId);
        //等待后台数据获取成功
        ThreadUtil.runInBackground(() -> {
            int count = 0;
            try {
                while (singletonNetData.getContent().isEmpty() && count < 60) {
                    Thread.sleep(1000); // 延迟数据获取成功
                    count++;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (singletonNetData.getContent().isEmpty()) {
                TabNavBody book = TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
                if (book != null) {
                    toast("网络异常 : <<" + book.getBookName() + ">> 获取数据失败,");
                } else {
                    toast("网络异常 :获取数据失败,");
                }
                return;
            }
            post(() -> {
                // 获取 Activity 上下文，避免使用 getContext() 引发潜在问题
                Activity activity = weakActivity.get();

                if (activity == null) {
                    // 如果 Fragment 当前不附加到 Activity，直接返回，不进行启动操作
                    return;
                }
                // 启动跳转到阅读窗口
                Intent intent = new Intent(activity, TipsFragmentActivity.class);
                //  Intent intent = new Intent(activity, CopyActivity.class);
                intent.putExtra("bookId", bookId);

                // 如果当前上下文不是 Activity，需要添加 FLAG_ACTIVITY_NEW_TASK
                if (!(activity instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }

                startActivityForResult(intent, (resultCode, data) -> {

                });
            });

        });

    }

    private boolean showUpdateNotificationEvent=false;
    private ShowUpdateNotificationEvent eventEntity;
    // 标记正在重新下载数据
    @Subscribe(priority = 1)
    public void onUpdateEvent(ShowUpdateNotificationEvent event) {
        ThreadUtil.runOnUiThread(() -> {
            showUpdateNotificationEvent = true;
            eventEntity = event;
            if (event.isUpdateNotification() && event.isAllChapterNotification()) {
                getBookData(bookId);
                toast("正在重新下载全部数据!!!!");
            } else {
                getBookData(bookId);
                toast("正在重新下载本章节数据!!!!");
            }

        });
    }


    /**
     * 获取点击项的数据
     *
     * @param bookId 获取指定的编号的信息
     */
    public void getBookData(int bookId) {

//        //加载书本相关的药方
        TabNavBody book = TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
//
        // todo 后续优化为设置开关
        if (true) {
            getBookChapter(book);
        } else {
            //获取书籍章节列表
            getBookContentAll(bookId);
        }

        StringBuilder fangName = new StringBuilder("\n");
        if (book != null) {
            fangName.append(book.getBookName());
        } else {
            fangName.append("药方");
        }
        getBookFang(bookId, fangName);
    }

    private void getBookContentAll(int bookId) {
        EasyHttp.get(this)
                .api(new BookContentApi().setBookId(bookId))
                .request(new HttpCallback<HttpData<List<HH2SectionData>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<HH2SectionData>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            List<HH2SectionData> detailList = data.getData();
                            //加载书本内容
                            singletonNetData.setContent(detailList);
                            //保存书籍内容
                            ThreadUtil.runInBackground(() -> {
                                //加载书本内容
                                mLoading = ConvertEntity.getBookDetailList(data.getData(), bookId);
                                //通知数据更新完成
                            });
                            chapterNotificationEvent();

                        }
                    }

                });
    }

    /**
     * 通知数据下载结束更新
     */
    private void chapterNotificationEvent() {

          // 通知数据下载结束更新

        toast("数据已经重新下载完成!!!!");
    }

    private final List<HH2SectionData> detailList = new ArrayList<>();

    private void getBookChapter(TabNavBody book) {
        if (book != null) {
            ArrayList<Chapter> list = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(book.getBookNo()));
            if (!showUpdateNotificationEvent) detailList.clear();
            for (Chapter chapter : list) {

                if (showUpdateNotificationEvent&& Objects.equals(eventEntity.getChapterId(), chapter.getSignatureId())) {
                    getChapterList(chapter, detailList);
                    break;

                } else if (!showUpdateNotificationEvent) {

                    HH2SectionData section = null;
                    if (!chapter.getIsDownload()) {
                        getChapterList(chapter, detailList);
                        section = new HH2SectionData(new ArrayList<>(), chapter.getChapterSection(), chapter.getChapterHeader());
                    } else {
                        // 从数据库中获取数据
                        List<DataItem> dataItem = ConvertEntity.getBookChapterDetailList(chapter);
                        // 创建HH2SectionData对象
                        section = new HH2SectionData(dataItem, chapter.getChapterSection(), chapter.getChapterHeader());
                    }

                    section.setSignatureId(chapter.getSignatureId());
                    detailList.add(section);
                }
            }
            //加载书本内容
            singletonNetData.setContent(detailList);
        } else {
            toast("书籍信息错误,退出后重新打开!!!!");
        }
    }

    private void getBookFang(int bookId, StringBuilder fangName) {
        EasyHttp.get(this)
                .api(new BookFangApi().setBookId(bookId))
                .request(new HttpCallback<HttpData<List<Fang>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<Fang>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            List<Fang> detailList = data.getData();
                            singletonNetData.setFang(new HH2SectionData(detailList, 0, fangName.toString()));
                            //保存药方数据
                            ThreadUtil.runInBackground(() -> {
                                ConvertEntity.getFangDetailList(data.getData(), bookId);
                            });

                        }
                    }
                });
    }

    public void getChapterList(Chapter chapter, List<HH2SectionData> detailList) {

        EasyHttp.get(this)
                .api(new ChapterContentApi()
                        .setContentId(chapter.getChapterSection())
                        .setSignatureId(chapter.getSignatureId())
                        .setBookId(chapter.getBookId())
                )

                .request(new HttpCallback<HttpData<List<HH2SectionData>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<HH2SectionData>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            //  ArrayList<Chapter> list = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(item.getBookNo()));
                            for (int i = 0; i < detailList.size(); i++) {
                                if (detailList.get(i).getSignatureId() == data.getData().get(0).getSignatureId()) {
                                    HH2SectionData hh2SectionData1 = new HH2SectionData(data.getData().get(0).getData(), chapter.getChapterSection(), chapter.getChapterHeader());
                                    ChapterContentNotificationEvent chapterContentNotificationEvent = getChapterContentNotificationEvent(chapter, i);
                                    chapterContentNotificationEvent.setData(hh2SectionData1);
                                    //通知数据更新完成
                                    XEventBus.getDefault().post(chapterContentNotificationEvent);
                                    // 通知数据下载结束更新
                                    if (showUpdateNotificationEvent)
                                        chapterNotificationEvent();
                                    try {
                                        // 更新数据库
                                        chapter.setIsDownload(true);
                                        DbService.getInstance().mChapterService.updateEntity(chapter);
                                        //保存内容
                                        ConvertEntity.saveBookChapterDetailList(chapter, data.getData());

                                    } catch (Exception e) {
                                        // 处理异常，比如记录日志、通知管理员等
                                        EasyLog.print("Failed to updateEntity: " + e.getMessage());
                                        return;
                                        // 根据具体情况决定是否需要重新抛出异常
                                        //throw e;
                                    }
                                }
                            }


                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);

                    }
                });

    }

    private @NonNull ChapterContentNotificationEvent getChapterContentNotificationEvent(Chapter chapter, int i) {
        ChapterContentNotificationEvent chapterContentNotificationEvent = new ChapterContentNotificationEvent();
        chapterContentNotificationEvent.setBookId(bookId);
        chapterContentNotificationEvent.setChapterSection(chapter.getChapterSection());
        chapterContentNotificationEvent.setChapterHeader(chapter.getChapterHeader());
        chapterContentNotificationEvent.setSignatureId(chapter.getSignatureId());
        chapterContentNotificationEvent.setGroupPosition(i);
        return chapterContentNotificationEvent;
    }

    /**
     * {@link OnRefreshLoadMoreListener}
     */

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {

        postDelayed(() -> {
            mAdapter.clearData();
            mAdapter.setData(analogData());
            mRefreshLayout.finishRefresh();
        }, 1000);
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        postDelayed(() -> {
            mAdapter.addData(analogData());
            mRefreshLayout.finishLoadMore();

            mAdapter.setLastPage(mAdapter.getCount() >= 100);
            mRefreshLayout.setNoMoreData(mAdapter.isLastPage());
        }, 1000);
    }

    @Override
    public void onDestroy() {
        // 清理 post 或异步任务，确保引用不再存在
        weakActivity.clear();
        XEventBus.getDefault().unregister(TipsWindowNetFragment.this);
        super.onDestroy();

    }
}