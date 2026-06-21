package run.yigou.gxzy.ui.reader.search;

import android.annotation.SuppressLint;
import android.content.Context;

import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import run.yigou.gxzy.R;
import com.hjq.base.AppAdapter;

import run.yigou.gxzy.ui.reader.BookContentSearchActivity;

/**
 *  作者:  zhs
 *  时间:  2023-07-14 14:28:00
 *  类名:  SearchBookAdapter
 *  版本:  1.0
 *  描述:  搜索书籍列表适配器
 *
*/
public final class SearchBookAdapter extends AppAdapter<SearchKey> {

    private BookContentSearchActivity mBookContentSearchActivity;
    public SearchBookAdapter(Context context) {
        super(context);
        mBookContentSearchActivity = (BookContentSearchActivity) context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType
) {
        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {


        private AppCompatImageView mIvSearchAvatar;
        private TextView mTvSearchBookName;
        private TextView mTvSearchKeyCount;



        private ViewHolder() {
            super(R.layout.book_search_book);
            mIvSearchAvatar = findViewById(R.id.iv_search_flag);
            mTvSearchBookName = findViewById(R.id.tv_search_book_name);
            mTvSearchKeyCount = findViewById(R.id.tv_search_key_count);

        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindView(int position) {
          SearchKey keyText =  getItem(position);
            mTvSearchBookName.setText( keyText.getBookName());
            mTvSearchKeyCount.setText( keyText.getFilteredData().size()+"");
            if (!keyText.getFilteredData().isEmpty())
            {
                mIvSearchAvatar.setImageResource(R.drawable.success);
            }else {
                mIvSearchAvatar.setImageResource(R.drawable.failed);
            }
        }
    }
}
