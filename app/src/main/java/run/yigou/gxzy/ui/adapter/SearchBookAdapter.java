package run.yigou.gxzy.ui.adapter;

import android.content.Context;

import android.text.Spanned;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.http.entitymodel.ChapterSearchRes;
import run.yigou.gxzy.http.entitymodel.SearchKeyText;
import run.yigou.gxzy.ui.activity.BookContentSearchActivity;
import run.yigou.gxzy.utils.SpannableStringHelper;

/**
 *  作者:  zhs
 *  时间:  2023-07-14 14:28:00
 *  包名:  run.yigou.gxzy.ui.adapter
 *  类名:  SearchBookAdapter
 *  版本:  1.0
 *  描述:
 *
*/
public final class SearchBookAdapter extends AppAdapter<SearchKeyText> {

    private BookContentSearchActivity mBookContentSearchActivity;
    public SearchBookAdapter(Context context) {
        super(context);
        mBookContentSearchActivity = (BookContentSearchActivity) context;
    }

//    @Override
//    public int getItemCount() {
//        return 10;
//    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {


        private AppCompatImageView mIvSearchAvatar;
        private TextView mTvSearchBookName;
        private TextView mTvSearchKeyCount;



        private ViewHolder() {
            super(R.layout.book_search_book);
            mIvSearchAvatar = findViewById(R.id.iv_search_avatar);
            mTvSearchBookName = findViewById(R.id.tv_search_book_name);
            mTvSearchKeyCount = findViewById(R.id.tv_search_key_count);

        }

        @Override
        public void onBindView(int position) {
          SearchKeyText keyText =  getItem(position);
            mTvSearchBookName.setText( getItem(position).getBookCaseName());
            mTvSearchKeyCount.setText( getItem(position).getSearcTextResCount()+"");
            if ( getItem(position).getSearcTextResCount()>0)
            {
                // 加载 drawable 中的图片资源
                mIvSearchAvatar.setImageResource(R.drawable.success);
            }else {
                mIvSearchAvatar.setImageResource(R.drawable.failed);
            }
        }
    }
}