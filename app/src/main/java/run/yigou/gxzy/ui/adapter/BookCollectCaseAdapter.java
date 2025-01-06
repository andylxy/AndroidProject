package run.yigou.gxzy.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.other.AppConfig;

/**
 * 作者:  zhs
 * 时间:  2023-07-13 14:40:41
 * 包名:  run.yigou.gxzy.ui.adapter
 * 类名:  BookCollectCaseAdapter
 * 版本:  1.0
 * 描述:
 */
public final class BookCollectCaseAdapter extends AppAdapter<Book> {

    public BookCollectCaseAdapter(Context context) {
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

        // private final ImageView mDelete;
        private final TextView tvBookName;
        private final TextView tvBookAuthor;
        //private final TextView tvBookDesc;
        //private final ImageView ivBookImg;

        private ViewHolder() {
            super(R.layout.book_tab_label_book_item);
            // mTextView = findViewById(R.id.tv_status_text);
            tvBookName = findViewById(R.id.tv_book_name);
            tvBookAuthor = findViewById(R.id.tv_book_author);
            // tvBookDesc = findViewById(R.id.tv_book_desc);
            //  ivBookImg = findViewById(R.id.iv_book_img);
            // mDelete = findViewById(R.id.iv_book_del);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindView(int position) {
            //mTextView.setText(getItem(position));
            if (getData() != null) {
                Book item = getData().get(position);
                tvBookName.setText(item.getBookName());
                tvBookAuthor.setText(item.getAuthor());
//                tvBookDesc.setText("     " + item.getDesc());
//                GlideApp.with(this.getItemView())
//                        .load(AppConfig.getHostUrl() + item.getImgUrl())
//                        .into(ivBookImg);
            }
        }
    }
}