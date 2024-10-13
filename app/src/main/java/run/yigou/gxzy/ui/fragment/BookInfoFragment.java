/*
 * 项目名: AndroidProject
 * 类名: BookInfoFragment.java
 * 包名: run.yigou.gxzy.ui.fragment.BookInfoFragment
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月06日 09:57:26
 * 上次修改时间: 2023年07月06日 09:49:47
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.fragment;

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
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.ui.activity.BookInfoActivity;
import run.yigou.gxzy.ui.adapter.BookInfoAdapter;

/**
 * 作者:  zhs
 * 时间:  2023-07-06 09:57:46
 * 包名:  run.yigou.gxzy.ui.fragment
 * 类名:  BookInfoFragment
 * 版本:  1.0
 * 描述:
 **/
public final class BookInfoFragment extends TitleBarFragment<AppActivity>
        implements OnRefreshLoadMoreListener,
        BaseAdapter.OnItemClickListener {

    public static BookInfoFragment newInstance() {
        return new BookInfoFragment();
    }

    public static BookInfoFragment newInstance(List<TabNavBody> navList) {
        BookInfoFragment bookInfoFragment = new BookInfoFragment();
        bookInfoFragment.mNavList = navList;
        return bookInfoFragment;
    }

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
        mRefreshLayout.setOnRefreshLoadMoreListener(this);
        //表示禁用上拉加载更多功能。
        mRefreshLayout.setEnableLoadMore(false);
        // 禁用下拉刷新
        mRefreshLayout.setEnableRefresh(false);
    }

    @Override
    protected void initData() {
        // List<BookInfoNav.Bean.NavList> navList =mNavList;
        mAdapter.setData(analogData());
    }

    /**
     * 模拟数据
     */
    private List<TabNavBody> analogData() {
//        List<String> data = new ArrayList<>();
//        for (int i = mAdapter.getCount(); i < mAdapter.getCount() + 20; i++) {
//            data.add("我是第" + i + "条目");
//        }
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

        //启动跳转
        //toast(mAdapter.getItem(position));
        BookInfoActivity.start(getAttachActivity(),mAdapter.getItem(position) );

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
}