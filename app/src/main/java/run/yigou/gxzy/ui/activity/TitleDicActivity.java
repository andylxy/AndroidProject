package run.yigou.gxzy.ui.activity;

import android.content.Intent;

import com.hjq.widget.layout.WrapRecyclerView;

import java.util.ArrayList;
import java.util.Objects;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.http.api.BookDetailList;
import run.yigou.gxzy.http.entitymodel.ChapterList;
import run.yigou.gxzy.http.entitymodel.TitelInfo;
import run.yigou.gxzy.ui.adapter.ChapterDicAdapter;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 可进行拷贝的副本
 */
public final class TitleDicActivity extends AppActivity {
    private WrapRecyclerView mLlTitleListDic;
    private ChapterDicAdapter mChapterDicAdapter;
    private ArrayList<TitelInfo> mTitelInfos = new ArrayList<>();
    private BookDetailList.Bean bean;

    @Override
    protected int getLayoutId() {
        return R.layout.title_dic_collect_case;
    }

    @Override
    protected void initView() {
        mLlTitleListDic = findViewById(R.id.ll_title_list_dic);
    }

    @Override
    protected void initData() {
        initViewData();
    }

    protected void initViewData() {
        bean = getSerializable(AppConst.CHAPTER);
        if (bean != null) {
            setTitle(bean.getTitle());
            try {

//                    Chapter chapter = new Chapter();
//                    chapter.setNumber(i++);
//                    chapter.setTitle(" - - " + bean.getTitle());
//                    chapter.setBookId(chapterList.getBookId()+"");
//                    chapter.setId(bean.getId() + "");
//                    chapters.add(chapter);

                    for (ChapterList chapter:  bean.getChapterLists()){
                        TitelInfo titelInfo = new TitelInfo();
                        titelInfo.setId(chapter.getId() + "");
                        titelInfo.setParentId(bean.getParentId());
                        titelInfo.setTitleColor(bean.getTitleColor());
                        if (Objects.equals(bean.getParentId(), "0"))
                            titelInfo.setTitle(chapter.getTitle());
                        else
                            titelInfo.setTitle(bean.getTitle());
                        titelInfo.setComment(chapter.getTitle());
                        titelInfo.setBookId(bean.getBookId()+"");
                        titelInfo.setNo(chapter.getNo());
                        mTitelInfos.add(titelInfo);
                    }


            } catch (Exception e) {

                e.printStackTrace();
            }
        }
//        //处理序言,自序页面
//        if (bean != null && bean.getChapterLists().size() ==1  && Objects.equals( bean.getChapterLists().get(0).getNo(), "0")) {
//
//                Intent intent = new Intent(getActivity(), BookReadActivity.class);
//                Book book = new Book();
//                book.setId(mTitelInfos.get(0).getBookId() + "");
//                book.setBookId(mTitelInfos.get(0).getBookId());
//                book.setChapterUrl(mTitelInfos.get(0).getId() + "");
//                book.setSource("Search");
//                intent.putExtra(APPCONST.BOOK, book);
//                startActivity(intent);
//
//        }
        //标准点击处理方式
        mChapterDicAdapter = new ChapterDicAdapter(getContext(),bean.getParentId());
        mChapterDicAdapter.setOnItemClickListener((adapterView, view, i) -> {
            Intent intent = new Intent(getActivity(), BookReadActivity.class);
            Book book = new Book();
            book.setId(mTitelInfos.get(i).getBookId() + "");
            book.setBookId(mTitelInfos.get(i).getBookId());
            book.setChapterUrl(mTitelInfos.get(i).getId() + "");
            book.setSource("Search");
            intent.putExtra(AppConst.BOOK, book);
            startActivity(intent);
        });
        mLlTitleListDic.setAdapter(mChapterDicAdapter);
        mChapterDicAdapter.setData(mTitelInfos);
    }
}
