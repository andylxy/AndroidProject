package run.yigou.gxzy.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hjq.http.EasyLog;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.other.AppConfig;

/**
 * 作者:  zhs
 * 时间:  2023-07-07 10:12:44
 * 包名:  run.yigou.gxzy.ui.activity
 * 类名:  BookInfoActivity
 * 版本:  1.0
 * 描述:
 */
public final class BookInfoActivity extends AppActivity {
    private static final String Book_KEY_IN = "book";
    private BookInfoNav.Bean.NavItem mNavItem;

    public static void start(Context context, BookInfoNav.Bean.NavItem item) {
        Intent intent = new Intent(context, BookInfoActivity.class);
        intent.putExtra(Book_KEY_IN, item);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.book_info_activity;
    }

    @Override
    protected void initView() {

        init();
        setOnClickListener(R.id.btn_read_book, R.id.btn_add_bookcase);
    }

    @Override
    protected void initData() {
       mNavItem= getSerializable(Book_KEY_IN);
        tvBookAuthor.setText(mNavItem.getAuthor());

        tvTvBookName.setText(mNavItem.getBookName());
        tvBookAuthor.setText(mNavItem.getAuthor());
        tvBookDesc.setText("        " + mNavItem.getDesc());
        //  tvBookType.setText(mNavItem.get());

        if (isBookCollected()) {
            btnAddBookcase.setText("不追了");
        } else {
            btnAddBookcase.setText("加入书架");
        }
        setTitle(mNavItem.getBookName());
        //图片
        GlideApp.with(this.getContext())
                .load(AppConfig.getHostUrl() + mNavItem.getImageUrl())
                .into(ivBookImg);

        //加入书架按钮
//        mBookInfoActivity.getBtnAddBookcase().setOnClickListener(view -> {
//            if (StringHelper.isEmpty(mBook.getId())) {
//                mBookService.addBook(mBook);
//                TextHelper.showText("成功加入书架");
//                mBookInfoActivity.getBtnAddBookcase().setText("不追了");
//            } else {
//                mBookService.deleteBookById(mBook.getId());
//                mBook.setId(null);
//                TextHelper.showText("成功移除书籍");
//                mBookInfoActivity.getBtnAddBookcase().setText("加入书架");
//            }
//
//        });
//        //todo 立即阅读
//        mBookInfoActivity.getBtnReadBook().setOnClickListener(view -> {
//            Intent intent = new Intent(mBookInfoActivity, ReadActivity.class);
//            intent.putExtra(APPCONST.BOOK, mBook);
//            mBookInfoActivity.startActivity(intent);
//
//        });
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        int viewId = view.getId();

        switch (viewId) {
            case R.id.btn_read_book:
                BookReadActivity.start(getActivity(),mNavItem);
                break;
            case R.id.btn_add_bookcase:
                toast( btnAddBookcase.getText());
                btnAddBookcase.setText("弃书不读了");
                break;
            default:
                EasyLog.print("onClick value: " + viewId);
        }
    }

    private TextView tvBookAuthor;
    private TextView tvBookDesc;
    // private TextView tvBookType;
    private TextView tvTvBookName;
    private ImageView ivBookImg;
    private TextView btnReadBook;
    private TextView btnAddBookcase;

    private void init() {
        tvBookAuthor = findViewById(R.id.tv_book_author);
        tvBookDesc = findViewById(R.id.tv_book_desc);
//        tvBookType = findViewById(R.id.tv_book_type);
        tvTvBookName = findViewById(R.id.tv_book_name);
        btnReadBook = findViewById(R.id.btn_read_book);
        btnAddBookcase = findViewById(R.id.btn_add_bookcase);
        ivBookImg = findViewById(R.id.iv_book_img);


    }

    private boolean isBookCollected() {
//        Book book = mBookService.findBookByAuthorAndName(mBook.getName(), mBook.getAuthor(), mBook.getSource());
//        if (book == null) {
//            return false;
//        } else {
//            mBook.setId(book.getId());
//            return true;
//        }
        return false;
    }
}