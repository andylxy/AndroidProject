

package run.yigou.gxzy.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.greendao.entity.BookChapter;
import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;
import run.yigou.gxzy.greendao.gen.BookChapterDao;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;
import run.yigou.gxzy.greendao.gen.YaoFangDao;
import run.yigou.gxzy.greendao.service.BookChapterBodyService;
import run.yigou.gxzy.greendao.service.BookChapterService;
import run.yigou.gxzy.greendao.service.YaoFangBodyService;
import run.yigou.gxzy.greendao.service.YaoFangService;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.BookContentApi;
import run.yigou.gxzy.http.api.BookFangApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.manager.ThreadPoolManager;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.adapter.BookInfoAdapter;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.DataBeans.YaoUse;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.utils.StringHelper;
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
    /**
     * singleData 所有书籍 数据单例
     */
    TipsSingleData singleData;
    /**
     * 当前点击书本数据
     */
    SingletonNetData singletonNetData;
    private BookChapterService mBookChapterService;
    private BookChapterBodyService mBookChapterBodyService;
    private YaoFangService mYaoFangService;
    private YaoFangBodyService mYaoFangBodyService;

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
        // 获取数据表服务
        mBookChapterService = DbService.getInstance().mBookChapterService;
        mBookChapterBodyService = DbService.getInstance().mBookChapterBodyService;
        mYaoFangService = DbService.getInstance().mYaoFangService;
        mYaoFangBodyService = DbService.getInstance().mYaoFangBodyService;
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
                while (singletonNetData.getContent().isEmpty() && count < 20) {
                    Thread.sleep(500); // 延迟数据获取成功
                    count++;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (singletonNetData.getContent().isEmpty()) {
                toast("获取数据失败：");
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

    /**
     * 获取点击项的数据
     *
     * @param bookId 获取指定的编号的信息
     */
    public void getBookData(int bookId) {

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
                                ConvertEntity.getBookDetailList(singletonNetData.getContent(), data.getData(), bookId);
                            });

                        }
                    }

                });


        //加载书本相关的药方
        TabNavBody tabNav = TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
        StringBuilder fangName = new StringBuilder("\n");
        if (tabNav != null) {
            fangName.append(tabNav.getBookName());
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
                                ConvertEntity.getFangDetailList(singletonNetData.getFang().get(0),data.getData(),bookId);
                            });

                        }
                    }
                });
    }


//    private void procesFangDetailList(List<Fang> detailList) {
//        if (detailList == null || detailList.isEmpty()) {
//            EasyLog.print("detailList is empty or null.");
//            return;
//        }
//
//        try {
//            StringBuilder chapterId = new StringBuilder();
//            for (Fang fang : detailList) {
//                chapterId.setLength(0);
//                chapterId.append(StringHelper.getUuid());
//                ArrayList<YaoFang> yaoFangList = mYaoFangService.find(YaoFangDao.Properties.SignatureId.eq(fang.getSignatureId()));
//                if (yaoFangList != null && !yaoFangList.isEmpty()) {
//                    YaoFang locYaoFang = yaoFangList.get(0);
//                    locYaoFang.setText(fang.getText());
//                    locYaoFang.setFangList(String.join(",", fang.getFangList()));
//                    locYaoFang.setYaoList(String.join(",", fang.getYaoList()));
//                    locYaoFang.setSignature(fang.getSignature());
//                    locYaoFang.setName(fang.getName());
//                    mYaoFangService.updateEntity(locYaoFang);
//
//                } else {
//                    YaoFang yaoFang = new YaoFang();
//                    yaoFang.setYaoCount(fang.getYaoCount());
//                    yaoFang.setName(fang.getName());
//                    yaoFang.setBookId(bookId);
//                    yaoFang.setID(fang.getID());
//                    yaoFang.setDrinkNum(fang.getDrinkNum());
//                    yaoFang.setText(fang.getText());
//                    yaoFang.setFangList(String.join(",", fang.getFangList()));
//                    yaoFang.setYaoList(String.join(",", fang.getYaoList()));
//                    yaoFang.setYaoFangID(chapterId.toString());
//                    yaoFang.setSignature(fang.getSignature());
//                    yaoFang.setSignatureId(fang.getSignatureId());
//                    mYaoFangService.addEntity(yaoFang);
//                }
//
//                for (YaoUse content : fang.getStandardYaoList()) {
//                    ArrayList<YaoFangBody> yaoUseList = mYaoFangBodyService.find(YaoFangBodyDao.Properties.SignatureId.eq(content.getSignatureId()));
//                    if (yaoUseList != null && !yaoUseList.isEmpty()) {
//                        YaoFangBody locYaoFangBody = yaoUseList.get(0);
//                        locYaoFangBody.setAmount(content.getAmount());
//                        locYaoFangBody.setExtraProcess(content.getExtraProcess());
//                        locYaoFangBody.setShowName(content.getShowName());
//                        locYaoFangBody.setSignature(content.getSignature());
//                        locYaoFangBody.setSuffix(content.getSuffix());
//                        locYaoFangBody.setWeight(content.getWeight());
//                        mYaoFangBodyService.updateEntity(locYaoFangBody);
//                    } else {
//                        YaoFangBody yaoFangBody = getYaoFangBody(content, chapterId);
//                        mYaoFangBodyService.addEntity(yaoFangBody);
//                    }
//
//                }
//            }
//        } catch (Exception e) {
//            EasyLog.print("Error processing detail list: " + e.getMessage());
//            throw e;
//        }
//    }
//
//    private static @NonNull YaoFangBody getYaoFangBody(YaoUse content, StringBuilder chapterId) {
//        YaoFangBody yaoFangBody = new YaoFangBody();
//        yaoFangBody.setYaoFangBodyId(StringHelper.getUuid());
//        yaoFangBody.setYaoFangID(chapterId.toString());
//        yaoFangBody.setSuffix(content.getSuffix());
//        yaoFangBody.setAmount(content.getAmount());
//        yaoFangBody.setYaoID(content.getYaoID());
//        yaoFangBody.setWeight(content.getWeight());
//        yaoFangBody.setShowName(content.getShowName());
//        yaoFangBody.setExtraProcess(content.getExtraProcess());
//        yaoFangBody.setSignatureId(content.getSignatureId());
//        yaoFangBody.setSignature(content.getSignature());
//        return yaoFangBody;
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
}