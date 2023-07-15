package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.lang.reflect.Type;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.greendao.entity.Book;

/**
 *  作者:  zhs
 *  时间:  2023-07-14 14:28:00
 *  包名:  run.yigou.gxzy.ui.adapter
 *  类名:  SearchBookAdapter
 *  版本:  1.0
 *  描述:
 *
*/
public final class SearchBookAdapter extends AppAdapter<Book> {

    public SearchBookAdapter(Context context) {
        super(context);
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

        TextView tvBookName;
        TextView tvDesc;
        TextView tvAuthor;
        TextView tvType;

        private ViewHolder() {
            super(R.layout.book_search_book_item);
            tvBookName = findViewById(R.id.tv_book_name);
            tvDesc = findViewById(R.id.tv_book_desc);
            tvAuthor = findViewById(R.id.tv_book_author);
            tvType = findViewById(R.id.tv_book_type);

        }

        @Override
        public void onBindView(int position) {
            tvBookName.setText( getItem(position).getName());
            tvDesc.setText( getItem(position).getDesc());
            tvAuthor.setText( getItem(position).getAuthor());
            tvType.setText( getItem(position).getType());
        }
    }
}