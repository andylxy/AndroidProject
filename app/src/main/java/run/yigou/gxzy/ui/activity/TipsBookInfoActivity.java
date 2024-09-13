package run.yigou.gxzy.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hjq.widget.layout.WrapRecyclerView;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.APPCONST;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.service.BookService;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.BookDetailList;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.entitymodel.TitelInfo;
import run.yigou.gxzy.ui.tips.tipsutils.TipsHelper;
import run.yigou.gxzy.ui.adapter.ChapterDicAdapter;
import run.yigou.gxzy.ui.fragment.BookCollectCaseFragment;
import run.yigou.gxzy.utils.StringHelper;

/**
 * 作者:  zhs
 * 时间:  2023-07-07 10:12:44
 * 包名:  run.yigou.gxzy.ui.activity
 * 类名:  BookInfoActivity
 * 版本:  1.0
 * 描述:
 */
public final class TipsBookInfoActivity extends AppActivity {
    private static final String Book_KEY_IN = "book";
    private BookInfoNav.Bean.NavItem mNavItem;
    private Book mBook;
    private BookService mBookService;
    private TextView tvBookAuthor;
    private TextView tvBookDesc;
    // private TextView tvBookType;
    private TextView tvTvBookName;
    private ImageView ivBookImg;
    private TextView btnReadBook;
    private TextView btnAddBookcase;
    private WrapRecyclerView mLvChapterDic;
    private ChapterDicAdapter mChapterDicAdapter;
    private ArrayList<TitelInfo> mTitelInfos = new ArrayList<>();
    private List<BookDetailList.Bean> detailList;


    public static void start(Context context, BookInfoNav.Bean.NavItem item) {
        Intent intent = new Intent(context, TipsBookInfoActivity.class);
        intent.putExtra(APPCONST.BOOK, item);
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

    private void init() {
        tvBookAuthor = findViewById(R.id.tv_book_author);
        tvBookDesc = findViewById(R.id.tv_book_desc);
//        tvBookType = findViewById(R.id.tv_book_type);
        tvTvBookName = findViewById(R.id.tv_book_name);
        btnReadBook = findViewById(R.id.btn_read_book);
        btnAddBookcase = findViewById(R.id.btn_add_bookcase);
        ivBookImg = findViewById(R.id.iv_book_img);
        mLvChapterDic = findViewById(R.id.lv_chapter_dic);

    }

    @Override
    protected void initData() {
        mBookService = DbService.getInstance().mBookService;// new BookService();
        if (BookCollected(true)) {
            btnAddBookcase.setText("弃书不读了");
        } else {
            btnAddBookcase.setText("加入书架");
        }
        tvBookAuthor.setText(mBook.getAuthor());
        tvTvBookName.setText(mBook.getName());
        tvBookAuthor.setText(mBook.getAuthor());
        tvBookDesc.setText(TipsHelper.renderText(mBook.getDesc()));
        setTitle(mBook.getName());
        if (Objects.equals(mBook.getName(), "黄帝内经")){
            btnAddBookcase.setVisibility(View.GONE);
            btnReadBook.setVisibility(View.GONE);

        }
        //图片
//        GlideApp.with(this.getContext())
//                .load(AppConfig.getHostUrl() + mNavItem.getImageUrl())
//                .into(ivBookImg);
//        EasyHttp.get(this)
//                .api(new BookDetailList().setId(mBook.getBookId()))
//                .request(new HttpCallback<HttpData<List<BookDetailList.Bean>>>(this) {
//                    @Override
//                    public void onSucceed(HttpData<List<BookDetailList.Bean>> data) {
//                        if (data != null && data.getData().size() > 0) {
//                            detailList = data.getData();
//                            try {
//                                for (BookDetailList.Bean bean : detailList) {
//                                    TitelInfo titelInfo = new TitelInfo();
//                                    titelInfo.setId(bean.getId() + "");
//                                    titelInfo.setParentId(bean.getParentId());
//                                    titelInfo.setTitleColor(bean.getTitleColor());
//                                    titelInfo.setTitle(bean.getTitle());
//                                    titelInfo.setBookId(bean.getBookId() + "");
//                                    mTitelInfos.add(titelInfo);
//                                }
//
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            } finally {
//                                initViewData();
//                            }
//                        }
//                    }
//                });

    }

    private void initViewData() {


        mChapterDicAdapter = new ChapterDicAdapter(getContext());
        mChapterDicAdapter.setOnItemClickListener((adapterView, view, i) -> {
            BookDetailList.Bean chapter = detailList.get(i);
            //处理序言,自序单页面
            if (chapter.getChapterLists().size() == 1 && Objects.equals(chapter.getChapterLists().get(0).getNo(), "0")) {
                    Intent intent = new Intent(getActivity(), BookReadActivity.class);
                    Book book = new Book();
                    book.setId(chapter.getChapterLists().get(0).getId() + "");
                    book.setBookId(chapter.getBookId()+ "");
                    book.setChapterUrl(chapter.getChapterLists().get(0).getId() + "");
                    book.setSource("Search");
                    intent.putExtra(APPCONST.BOOK, book);
                    startActivity(intent);
                    return;
            }
            //标准点击处理方式
            Intent intent = new Intent(getActivity(), TitleDicActivity.class);
            intent.putExtra(APPCONST.CHAPTER, chapter);
            startActivity(intent);
        });
        mLvChapterDic.setAdapter(mChapterDicAdapter);
        mChapterDicAdapter.setData(mTitelInfos);

    }

    @SingleClick
    @Override
    public void onClick(View view) {
        int viewId = view.getId();

        switch (viewId) {
            case R.id.btn_read_book:
                BookCollected(false);
                TipsBookReadActivity.start(getActivity());
                break;
            case R.id.btn_add_bookcase:
                if (StringHelper.isEmpty(mBook.getId())) {
                    mBookService.addBook(mBook);
                    BookCollectCaseFragment.mBookCollectCaseFragment.RefreshLayout();
                    toast("成功加入书架");
                    btnAddBookcase.setText("弃书不读了");
                } else {
                    mBookService.deleteBookById(mBook.getId());
                    mBook.setId(null);
                    toast("成功移除书籍");
                    btnAddBookcase.setText("加入书架");
                }
                break;
            default:
              //  EasyLog.print("onClick value: " + viewId);
        }
    }


    private boolean BookCollected(boolean start) {

        if (start) {
            mNavItem = getSerializable(APPCONST.BOOK);
            mBook = new Book();
            mBook.setAuthor(mNavItem.getAuthor());
            mBook.setDesc( mNavItem.getDesc());
            mBook.setImgUrl(mNavItem.getImageUrl());
            mBook.setName(mNavItem.getBookName());
            mBook.setBookId(mNavItem.getId());
        }

        Book book = mBookService.findBookByAuthorAndName(mBook.getName(), mBook.getAuthor());
        if (book == null) {
            return false;
        } else {
            mBook = book;
            return true;
        }
    }
}