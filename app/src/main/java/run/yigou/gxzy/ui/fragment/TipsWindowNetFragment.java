

package run.yigou.gxzy.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.List;

import run.yigou.gxzy.EventBus.ShowUpdateNotificationEvent;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.http.api.BookContentApi;
import run.yigou.gxzy.http.api.BookFangApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.manager.ThreadPoolManager;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.adapter.BookInfoAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.utils.ThreadUtil;


public final class TipsWindowNetFragment extends TitleBarFragment<AppActivity>
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
        ThreadPoolManager.getInstance().execute(() -> {
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
                    toast( "网络异常 : <<"+book.getBookName()+">> 获取数据失败,");
                }else {
                    toast("网络异常 :获取数据失败,");
                }
                return;
            }
            post(() -> {
                // 启动跳转 到阅读窗口
                Intent intent = new Intent(getContext(), TipsFragmentActivity.class);
                intent.putExtra("bookId", bookId);
                if (!(getContext() instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                startActivity(intent);
            });
        });

    }


    @Subscribe(priority = 1)
    public void onUpdateEvent(ShowUpdateNotificationEvent event) {
        ThreadUtil.runOnUiThread(() -> {
            getBookData(bookId);
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
        // 获取书本章节列表,后续再实现
//        if (book != null) {
//            ArrayList<Chapter> list = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(book.getBookNo()));
//            List<HH2SectionData> detailList = new ArrayList<>();
//            for (Chapter chapter : list) {
//
//                if (!chapter.getIsDownload()) {
//                    getChapterList(chapter,detailList);
//                }
//                detailList.add(new HH2SectionData(new ArrayList<>(), chapter.getChapterSection(), chapter.getChapterHeader()));
//
////                try {
////                    DbService.getInstance().mChapterService.updateEntity(chapter);
////                } catch (Exception e) {
////                    // 处理异常，比如记录日志、通知管理员等
////                    EasyLog.print("Failed to addEntity: " + e.getMessage());
////                    return;
////                    // 根据具体情况决定是否需要重新抛出异常
////                    //throw e;
////                }
//
//
//            }
//            //加载书本内容
//            singletonNetData.setContent(detailList);
//        } else {
//            toast("书籍信息错误,退出后重新打开!!!!");
//        }


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
                            ShowUpdateNotificationEvent showUpdateNotification = singletonNetData.getShowUpdateNotification();
                            if (showUpdateNotification.isUpdateNotification()) {
                                // 通知数据下载结束更新
                                showUpdateNotification.setUpdateNotification(false);
                                toast("数据已经重新下载完成,退出App重新打开!!!!");
                            }

                        }
                    }

                });

        StringBuilder fangName = new StringBuilder("\n");
        if (book != null) {
            fangName.append(book.getBookName());
        } else {
            fangName.append("药方");
        }
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
                                ConvertEntity.getFangDetailList(singletonNetData.getFang().get(0), data.getData(), bookId);
                            });

                        }
                    }
                });
    }
//    public  void getChapterList(Chapter chapter, List<HH2SectionData> detailList ) {
//
//        EasyHttp.get(this)
//                .api(new ChapterContentApi().setContentId(chapter.getChapterSection()).setSgnatureId(chapter.getSgnatureId()))
//
//                .request(new HttpCallback<HttpData<List<HH2SectionData>>>(this) {
//                    @Override
//                    public void onSucceed(HttpData<List<HH2SectionData>> data) {
//                        if (data != null && !data.getData().isEmpty()) {
//                          //  ArrayList<Chapter> list = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(item.getBookNo()));
//
//                            for (HH2SectionData hh2SectionData  : detailList) {
//                               if (hh2SectionData.getSignatureId() ==data.getData().get(0).getSignatureId()){
//                                 ArrayList<DataItem> list = (ArrayList<DataItem>) data.getData().get(0).getData();
//                                  // hh2SectionData.getData().addAll(list);
//                                   for (DataItem  dataItem :   data.getData().get(0).getData()) {
//
//                                       hh2SectionData.getData().add(dataItem);
//                                    }
//                               }
//                            }
//
//
//                       }
//                    }
//
//                    @Override
//                    public void onFail(Exception e) {
//                        super.onFail(e);
//
//                    }
//                });
//
//    }

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
        super.onDestroy();
        XEventBus.getDefault().unregister(TipsWindowNetFragment.this);
    }
}