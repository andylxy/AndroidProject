package run.yigou.gxzy.ui.reader.window;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.manager.update.UpdateManager;
import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.ui.home.HomeActivity;
import run.yigou.gxzy.ui.home.TipsFragmentActivity;
import run.yigou.gxzy.ui.reader.adapter.BookInfoAdapter;
import run.yigou.gxzy.widget.CustomDividerItemDecoration;
import run.yigou.gxzy.base.GlobalDataHolder;

/**
 * HomeFragment页面下
 * Tab标签分类下的书籍信息列表
 **/
public final class TipsWindowNetFragment extends TitleBarFragment<HomeActivity>
        implements OnRefreshLoadMoreListener,
        BaseAdapter.OnItemClickListener {


    public static TipsWindowNetFragment newInstance(List<TabNavBody> navList) {
        TipsWindowNetFragment bookInfoFragment = new TipsWindowNetFragment();
        // mNavList 状态保存不安全，暂时保留，建议通过 setArguments/getArguments 传递
        bookInfoFragment.mNavList = navList;
        return bookInfoFragment;
    }
    private String bookId;
    private List<TabNavBody> mNavList;
    private SmartRefreshLayout mRefreshLayout;
    private WrapRecyclerView mRecyclerView;

    private BookInfoAdapter mAdapter;


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
        mAdapter.setData(mNavList);
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

        // 需求 2：发起阅读意图时，若内存里记着有待强制升级，主动重弹升级框
        // （不依赖网络/缓存，即使内容已本地缓存命中也会弹）。
        // 弹窗已弹出则放弃本次导航，否则阅读页会把弹窗盖住、用户根本看不到提示。
        if (UpdateManager.checkForceOnReading(getAttachActivity())) {
            return;
        }

        // 验证书籍信息是否存在
        TabNavBody book = GlobalDataHolder.getInstance().getNavTabBodyMap().get(bookId);
        if (book != null) {
            startFragmentActivity();
        } else {
            toast("书籍信息错误,退出后重新打开!!!!");
        }
    }
    private void startFragmentActivity() {
        // 直接获取 Activity 上下文，TitleBarFragment 提供了 getAttachActivity()
        Activity activity = getAttachActivity();
        if (activity == null) {
            // 如果 Fragment 当前不附加到 Activity，尝试使用 getActivity
            activity = getActivity();
        }
        if (activity == null) {
            return;
        }

        // 启动跳转到阅读窗口
        Intent intent = new Intent(activity, TipsFragmentActivity.class);
        intent.putExtra("bookId", bookId);

        startActivityForResult(intent, (resultCode, data) -> {

        });
    }


    /**
     * {@link OnRefreshLoadMoreListener}
     */

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        postDelayed(() -> {
            mAdapter.clearData();
            mAdapter.setData(mNavList);
            mRefreshLayout.finishRefresh();
        }, 1000);
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        postDelayed(() -> {
            mAdapter.addData(mNavList);
            mRefreshLayout.finishLoadMore();

            mAdapter.setLastPage(mAdapter.getCount() >= 100);
            mRefreshLayout.setNoMoreData(mAdapter.isLastPage());
        }, 1000);
    }
}