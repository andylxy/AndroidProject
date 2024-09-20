

package run.yigou.gxzy.ui.fragment;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.http.api.BookContentApi;
import run.yigou.gxzy.http.api.BookFangApi;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.api.YaoContentApi;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.manager.CacheDataManager;
import run.yigou.gxzy.manager.ThreadPoolManager;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.adapter.BookInfoAdapter;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.Singleton_Net_Data;
import run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data;


public final class TipsWindowNetFragment extends TitleBarFragment<AppActivity>
        implements OnRefreshLoadMoreListener,
        BaseAdapter.OnItemClickListener {

    public static TipsWindowNetFragment newInstance() {
        return new TipsWindowNetFragment();
    }

    public static TipsWindowNetFragment newInstance(List<BookInfoNav.Bean.NavItem> navList) {
        TipsWindowNetFragment bookInfoFragment = new TipsWindowNetFragment();
        bookInfoFragment.mNavList = navList;
        return bookInfoFragment;
    }

    private List<BookInfoNav.Bean.NavItem> mNavList;
    private SmartRefreshLayout mRefreshLayout;
    private WrapRecyclerView mRecyclerView;

    private BookInfoAdapter mAdapter;
    /**
     * singleData 所有书籍 数据单例
     */
    Tips_Single_Data singleData ;
    /**
     * 当前点击书本数据
     */
    Singleton_Net_Data singletonNetData;

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
        singleData = Tips_Single_Data.getInstance();
        // List<BookInfoNav.Bean.NavList> navList =mNavList;
        mAdapter.setData(analogData());


    }

    private List<BookInfoNav.Bean.NavItem> analogData() {
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

        singletonNetData = Tips_Single_Data.getInstance().getBookIdContent(mAdapter.getItem(position).getBookNo());
        singletonNetData.setYaoAliasDict(singleData.getYaoAliasDict());
        singletonNetData.setFangAliasDict(singleData.getFangAliasDict());

        getBookData(mAdapter.getItem(position).getBookNo());
        //等待后台数据获取成功
        ThreadPoolManager.getInstance().execute(() -> {
            int count =0;
            try {
                while (singletonNetData.getContent().isEmpty() && count < 20) {
                    Thread.sleep(500); // 延迟数据获取成功
//                    if (mAdapter.getItem(position).getBookNo() == 10001) {
//                        //如果宋版的伤寒.则同获取宋版金匮
//                        getBookData(10002);
//                    }
                    count++;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (singletonNetData.getContent().isEmpty()){
                toast("获取数据失败：" );
                return;
            }
            post(() -> {
                // 启动跳转 到阅读窗口
                TipsFragmentActivity.start(getAttachActivity(), true, mAdapter.getItem(position).getBookNo());
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
                        if (data != null && data.getData().size() > 0) {
                            List<HH2SectionData> detailList = data.getData();
                            //加载书本内容
                            singletonNetData.setContent(detailList);
                        }

                    }
                });
        EasyHttp.get(this)
                .api(new BookFangApi().setBookId(bookId))
                .request(new HttpCallback<HttpData<List<Fang>>>(this) {
                    @Override
                    public void onSucceed(HttpData<List<Fang>> data) {
                        if (data != null && data.getData().size() > 0) {
                            List<Fang> detailList = data.getData();
                            //加载书本相关的药方
                            singletonNetData.setFang(new HH2SectionData(detailList, 0, "方"));
                        }
                    }
                });
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