package run.yigou.gxzy.ui.fragment;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.service.BookService;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.ui.activity.BookReadActivity;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.ui.adapter.BookCollectCaseAdapter;
import run.yigou.gxzy.ui.dialog.MessageDialog;

import com.hjq.base.BaseAdapter;
import com.hjq.base.BaseDialog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.hjq.widget.view.CountdownView;
import com.hjq.widget.view.SwitchButton;
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
    public static  BookCollectCaseFragment mBookCollectCaseFragment;
//    private ImageView mNobookImageView;
//    private TextView mNoBtnTextView;

    public static BookCollectCaseFragment newInstance() {
        return new BookCollectCaseFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.book_collect_case;
    }

    @Override
    protected void initView() {
//        mNobookImageView = findViewById(R.id.ll_no_book);
//        mNoBtnTextView = findViewById(R.id.ll_no_btn);
        mRefreshLayout = findViewById(R.id.rl_status_refresh);
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
        int index =1;
       // HomeActivity.start(getContext());
        HomeActivity.mHomeActivity. switchFragment(index);
        HomeActivity.mHomeActivity.onNavigationItemSelected(index);
    }

    @Override
    protected void initData() {
        setTitle("书架");
        mBookService = new BookService();
        mBookCollectCaseAdapter.setData(loadData());
        mBookCollectCaseFragment=this;

    }

    private List<Book> loadData() {
        List<Book> gvLisg = mBookService.getAllBooks();
        if (gvLisg.size() > 0) {
            mLlNoDataTips.setVisibility(View.GONE);
            mRefreshLayout.setVisibility(View.VISIBLE);
            return gvLisg;
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
        BookReadActivity.start(getActivity(), mBookCollectCaseAdapter.getItem(position));
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
        //        postDelayed(() -> {
        //            mBookCollectCaseAdapter.clearData();
        //            mBookCollectCaseAdapter.setData(loadData());
        //            mRefreshLayout.finishRefresh();
        //        }, 1000);
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
                    .setMessage(book.getName() + "")
                    // 确定按钮文本
                    .setConfirm(getString(R.string.common_confirm))
                    // 设置 null 表示不显示取消按钮
                    .setCancel(getString(R.string.common_cancel))
                    // 设置点击按钮后不关闭对话框
                    //.setAutoDismiss(false)
                    .setListener(new MessageDialog.OnListener() {

                        @Override
                        public void onConfirm(BaseDialog dialog) {
                            Book book2 = mBookService.getBookById(book.getId());
                            if (book2 != null) {
                                mBookService.deleteBookById(book2.getId());
                                //刷新书架
                                RefreshLayout();
                            }
                        }

                        @Override
                        public void onCancel(BaseDialog dialog) {

                        }
                    })
                    .show();


        }

        return false;
    }
}