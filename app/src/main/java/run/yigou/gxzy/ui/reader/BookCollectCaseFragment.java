package run.yigou.gxzy.ui.reader;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.TitleBarFragment;
import run.yigou.gxzy.data.local.entity.Book;
import run.yigou.gxzy.data.local.gen.BookDao;
import run.yigou.gxzy.data.local.service.BookService;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.local.helper.LocalServices;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.data.Callback;
import run.yigou.gxzy.ui.home.HomeActivity;
import run.yigou.gxzy.utils.ThreadUtil;
import run.yigou.gxzy.manager.update.UpdateManager;
import run.yigou.gxzy.ui.home.TipsFragmentActivity;
import run.yigou.gxzy.ui.reader.adapter.BookCollectCaseAdapter;
import run.yigou.gxzy.ui.dialog.MessageDialog;


import com.hjq.base.BaseAdapter;
import com.hjq.base.BaseDialog;
import com.hjq.widget.layout.WrapRecyclerView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.List;

/**
 * 作者:  zhs
 * 时间:  2023-07-13 11:04:50
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

    // 单例模式移除，确保 Fragment 正确生命周期管理
    // private static volatile BookCollectCaseFragment instance;

    // 公共构造函数，允许系统重建
    public BookCollectCaseFragment() {
    }

    public static BookCollectCaseFragment newInstance() {
        return new BookCollectCaseFragment();
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

//        String data = "你好";
//        String password = "1qaz2wsx3edc4rfv";
//        EasyLog.print("明文密码：" + password);
//        String encryptedData = RC4Utils.encryptToBase64(data.getBytes(CHARSET), password);
//        EasyLog.print("RC4加密：" + encryptedData);

        int index = 1;
        // 使用新的方式切换Fragment
        if (getActivity() instanceof HomeActivity) {
            HomeActivity homeActivity = (HomeActivity) getActivity();
            homeActivity.switchFragment(index);
            homeActivity.onNavigationItemSelected(index);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // instance = null;
    }

    @Override
    protected void initData() {
        setTitle("书架");
        mBookService = LocalServices.getInstance().mBookService;
        loadData();
    }

    private void loadData() {
        // 异步加载数据
        run.yigou.gxzy.utils.ThreadUtil.runInBackground(() -> {
            List<Book> gvList = mBookService.getAllBooks();
            run.yigou.gxzy.utils.ThreadUtil.runOnUiThread(() -> {
                if (gvList != null && !gvList.isEmpty()) {
                    mLlNoDataTips.setVisibility(View.GONE);
                    mRefreshLayout.setVisibility(View.VISIBLE);
                    mBookCollectCaseAdapter.setData(gvList);
                } else {
                    mLlNoDataTips.setVisibility(View.VISIBLE);
                    mRefreshLayout.setVisibility(View.GONE);
                    mBookCollectCaseAdapter.setData(new ArrayList<>());
                }
            });
        });
    }

    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }


    @Override
    public void onItemClick(RecyclerView recyclerView, View itemView, int position) {
        // toast(mBookCollectCaseAdapter.getItem(position));

        // 需求 2：进入阅读前，若有待强制升级则重弹升级框并阻断本次导航（不依赖网络/缓存）。
        if (getContext() instanceof Activity
                && UpdateManager.checkForceOnReading((Activity) getContext())) {
            return;
        }

        // 按书号查出这本书再决定跳不跳转（读发生在后台，结果回主线程后再 startActivity）
        final String bookNo = mBookCollectCaseAdapter.getItem(position).getBookNo();
        DbService.getInstance().readInBackground(
                new Callable<ArrayList<Book>>() {
                    @Override
                    public ArrayList<Book> call() {
                        return mBookService.find(BookDao.Properties.BookNo.eq(bookNo));
                    }
                },
                new Callback<ArrayList<Book>>() {
                    @Override
                    public void onSuccess(ArrayList<Book> books) {
                        openBookOrWarn(books);
                    }

                    @Override
                    public void onError(Exception e) {
                        // 读失败与"查不到"同义：同样提示书本异常
                        openBookOrWarn(null);
                    }
                });
    }

    /** 拿到书架记录后：有就进阅读页，没有就提示重加。 */
    private void openBookOrWarn(ArrayList<Book> books) {
        // 读在后台：回调回来时 Fragment 可能已被销毁，此时 startActivity/toast 会出问题
        if (!isAdded()) {
            return;
        }
        if (books == null || books.isEmpty()) {
            toast("书本异常.请删除后,重新加入书架");
        } else {
            String bookId = books.get(0).getBookNo();
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

    public void refreshLayout() {
        postDelayed(() -> {
            mBookCollectCaseAdapter.clearData();
            loadData();
            mRefreshLayout.finishRefresh();
        }, 1000);
    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        refreshLayout();
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
                            // 读与删必须在同一个后台任务里：拆成两次提交会让"删除前的状态"被其他任务看到。
                            final String bookNo = mBookCollectCaseAdapter.getItem(position).getBookNo();
                            DbService.getInstance().runInBackgroundSerial(new Runnable() {
                                @Override
                                public void run() {
                                    boolean removed = false;
                                    try {
                                        ArrayList<Book> books = mBookService.find(
                                                BookDao.Properties.BookNo.eq(bookNo));
                                        if (books != null && !books.isEmpty()) {
                                            mBookService.deleteEntity(books.get(0));
                                            removed = true;
                                        }
                                    } catch (Throwable t) {
                                        EasyLog.print(t);
                                    }
                                    final boolean didRemove = removed;
                                    ThreadUtil.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (didRemove) {
                                                // 刷新书架（UI 操作必须在主线程）
                                                refreshLayout();
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    })
                    .show();


        }

        return false;
    }
}