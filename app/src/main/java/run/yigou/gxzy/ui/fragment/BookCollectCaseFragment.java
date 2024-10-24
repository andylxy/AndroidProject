package run.yigou.gxzy.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.gen.BookDao;
import run.yigou.gxzy.greendao.service.BookService;
import run.yigou.gxzy.greendao.util.DbService;

import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;
import run.yigou.gxzy.ui.adapter.BookCollectCaseAdapter;
import run.yigou.gxzy.ui.dialog.MessageDialog;

import com.hjq.base.BaseAdapter;
import com.hjq.base.BaseDialog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者:  zhs
 * 时间:  2023-07-13 11:04:50
 * 包名:  run.yigou.gxzy.ui.fragment
 * 类名:  BookCollectCaseFragment
 * 版本:  1.0
 * 描述:
 */
public final class BookCollectCaseFragment extends TitleBarFragment<HomeActivity>
        implements OnRefreshLoadMoreListener, BaseAdapter.OnItemClickListener, BaseAdapter.OnItemLongClickListener, View.OnClickListener {

    private LinearLayout mLlNoDataTips;
    private WrapRecyclerView mGvBook;
    private BookCollectCaseAdapter mBookCollectCaseAdapter;
    private BookService mBookService;
    private SmartRefreshLayout mRefreshLayout;

    private BookCollectCaseFragment() {
        // 私有构造函数，防止外部实例化
        if (BookCollectCaseHolder.INSTANCE != null) {
            throw new IllegalStateException("已经初始化，不允许再次创建实例");
        }
    }

    private static class BookCollectCaseHolder {
        private static BookCollectCaseFragment INSTANCE = new BookCollectCaseFragment();
    }

    public static BookCollectCaseFragment newInstance() {
        return BookCollectCaseHolder.INSTANCE;
    }


    @Override
    protected int getLayoutId() {
        return R.layout.book_collect_case;
    }

    @Override
    protected void initView() {
        mRefreshLayout = findViewById(R.id.rl_status_refresh);
        // 给这个 View 设置沉浸式，避免状态栏遮挡
        // ImmersionBar.setTitleBar(this, findViewById(R.id.rl_status_refresh));
        mLlNoDataTips = findViewById(R.id.ll_no_data_tips);
        mGvBook = findViewById(R.id.gv_book);
        mBookCollectCaseAdapter = new BookCollectCaseAdapter(getAttachActivity());
        mBookCollectCaseAdapter.setOnItemClickListener(this);
        mBookCollectCaseAdapter.setOnItemLongClickListener(this);
        mGvBook.setAdapter(mBookCollectCaseAdapter);
        setOnClickListener(R.id.ll_no_data_tips, R.id.ll_no_book, R.id.ll_no_btn);

    }

    @SingleClick
    @Override
    public void onClick(View view) {
        int index = 1;
        // HomeActivity.start(getContext());
        HomeActivity.mHomeActivity.switchFragment(index);
        HomeActivity.mHomeActivity.onNavigationItemSelected(index);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BookCollectCaseHolder.INSTANCE = null;
    }

    @Override
    protected void initData() {
        setTitle("书架");
        mBookService = DbService.getInstance().mBookService;
        mBookCollectCaseAdapter.setData(loadData());
    }

    private List<Book> loadData() {
        List<Book> gvList = mBookService.getAllBooks();
        if (!gvList.isEmpty()) {
            mLlNoDataTips.setVisibility(View.GONE);
            mRefreshLayout.setVisibility(View.VISIBLE);
            return gvList;
        } else {
            mLlNoDataTips.setVisibility(View.VISIBLE);
            mRefreshLayout.setVisibility(View.GONE);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }


    @Override
    public void onItemClick(RecyclerView recyclerView, View itemView, int position) {
        // toast(mBookCollectCaseAdapter.getItem(position));

        ArrayList<Book> books = mBookService.find(BookDao.Properties.BookNo.eq(mBookCollectCaseAdapter.getItem(position).getBookNo()));
        if (books == null || books.isEmpty()) {
            toast("书本异常.请删除后,重新加入书架");
        } else {
            int bookId = books.get(0).getBookNo();
            int bookLastReadPosition = books.get(0).getLastReadPosition();
            // 启动跳转 到阅读窗口
            Intent intent = new Intent(getContext(), TipsFragmentActivity.class);
            intent.putExtra("bookId", bookId);
            intent.putExtra("bookLastReadPosition", bookLastReadPosition);
            intent.putExtra("bookCollect", true);
            if (!(getContext() instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            startActivity(intent);
        }
    }

    public void RefreshLayout() {
        postDelayed(() -> {
            mBookCollectCaseAdapter.clearData();
            mBookCollectCaseAdapter.setData(loadData());
            mRefreshLayout.finishRefresh();
        }, 1000);
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        RefreshLayout();
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {

    }

    @Override
    public boolean onItemLongClick(RecyclerView recyclerView, View itemView, int position) {


        Book book = mBookCollectCaseAdapter.getItem(position);
        if (book != null) {
            // 消息对话框
            new MessageDialog.Builder(getActivity())
                    // 标题可以不用填写
                    .setTitle("删除")
                    // 内容必须要填写
                    .setMessage(book.getBookName())
                    // 确定按钮文本
                    .setConfirm(getString(R.string.common_confirm))
                    // 设置 null 表示不显示取消按钮
                    .setCancel(getString(R.string.common_cancel))
                    // 设置点击按钮后不关闭对话框
                    //.setAutoDismiss(false)
                    .setListener(new MessageDialog.OnListener() {
                        @Override
                        public void onConfirm(BaseDialog dialog) {
                            ArrayList<Book> books = mBookService.find(BookDao.Properties.BookNo.eq(mBookCollectCaseAdapter.getItem(position).getBookNo()));
                            if (books != null && !books.isEmpty()) {
                                mBookService.deleteEntity(books.get(0));
                                //刷新书架
                                RefreshLayout();
                            }
                        }
                    })
                    .show();


        }

        return false;
    }
}