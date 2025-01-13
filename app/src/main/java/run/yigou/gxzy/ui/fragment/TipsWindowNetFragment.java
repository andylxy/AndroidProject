

package run.yigou.gxzy.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.gen.ChapterDao;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.adapter.BookInfoAdapter;
import run.yigou.gxzy.ui.dividerItemdecoration.CustomDividerItemDecoration;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;

/**
 * HomeFragment页面下
 * Tab标签分类下的书籍信息列表
 **/
public final class TipsWindowNetFragment extends TitleBarFragment<HomeActivity>
        implements OnRefreshLoadMoreListener,
        BaseAdapter.OnItemClickListener {


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

   // TipsSingleData singleData;
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
       // singleData = TipsSingleData.getInstance();
        // List<BookInfoNav.Bean.NavList> navList =mNavList;
        mAdapter.setData(analogData());
       // XEventBus.getDefault().register(TipsWindowNetFragment.this);
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

        getBookData(bookId);

    }
    private void startFragmentActivity() {
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
    }
    /**
     * 获取点击项的数据
     *
     * @param bookId 获取指定的编号的信息
     */
    public void getBookData(int bookId) {

//        //加载书本相关的药方
        TabNavBody book = TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
        if (book != null) {
            //加载书本相关的章节
            getBookChapter(book);
        } else {
            toast("书籍信息错误,退出后重新打开!!!!");
        }

    }



    private void getBookChapter(TabNavBody book) {

        List<HH2SectionData> detailList = new ArrayList<>();
        ArrayList<Chapter> list = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(book.getBookNo()));

        for (Chapter chapter : list) {

            HH2SectionData section = null;
            if (!chapter.getIsDownload()) {
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
        //加载书本内容
        singletonNetData.setContent(detailList);
        // 启动跳转到阅读窗口
        startFragmentActivity();
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
       // XEventBus.getDefault().unregister(TipsWindowNetFragment.this);
        super.onDestroy();

    }
}