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

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.ui.activity.BookInfoActivity;
import run.yigou.gxzy.ui.activity.TipsBookInfoActivity;
import run.yigou.gxzy.ui.adapter.BookInfoAdapter;

/**
 * 作者:  zhs
 * 时间:  2023-07-06 09:57:46
 * 包名:  run.yigou.gxzy.ui.fragment
 * 类名:  BookInfoFragment
 * 版本:  1.0
 * 描述:
 **/
public final class TipsWindowFragment extends TitleBarFragment<AppActivity>
        implements OnRefreshLoadMoreListener,
        BaseAdapter.OnItemClickListener {

    public static TipsWindowFragment newInstance() {
        return new TipsWindowFragment();
    }

    public static TipsWindowFragment newInstance(List<BookInfoNav.Bean.NavItem> navList) {
        TipsWindowFragment bookInfoFragment = new TipsWindowFragment();
        bookInfoFragment.mNavList = navList;
        return bookInfoFragment;
    }

    private List<BookInfoNav.Bean.NavItem> mNavList;
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
    private List<BookInfoNav.Bean.NavItem> analogData() {
//        List<String> data = new ArrayList<>();
//        for (int i = mAdapter.getCount(); i < mAdapter.getCount() + 20; i++) {
//            data.add("我是第" + i + "条目");
//        }

        mNavList = new ArrayList<>();
        mNavList.add( new BookInfoNav.Bean.NavItem(
               "赵开美翻刻宋板《伤寒论》\\r$m{明万历二十七年赵开美据北宋元祐三年小字本伤寒论翻刻}。伤寒论世无善本。余所藏治平官刊大字景写本。而外惟此赵清常本耳。亡友宗室伯兮祭酒曾悬重金购此本不可得。仅得日本安政覆刻本。$a{（今蜀中又有刻本亦从日本本出）}今夏从厂贾魏子敏得此本。完好无缺。惜伯兮不及见矣。\\r坊记   时戊申中秋日戊辰\\r\\r        北宋人官刻经注皆大字。单疏皆小字。所以别尊卑也。治平官本伤寒论乃大字经也。千金方外台秘要皆小字疏也。林亿诸人深于医矣。南宋已后乌足如此。\\r矩庵又记\",",
                "张仲景" ,
                "汉",
                "伤寒论",
                "",
                ""));
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
        TipsBookInfoActivity.start(getAttachActivity(),mAdapter.getItem(position) );

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