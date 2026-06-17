/*
 * ???: AndroidProject
 * ??: BookInfoAdapter.java
 * ??: run.yigou.gxzy.ui.feature.bookshelf
 * ?? : Zhs (xiaoyang_02@qq.com)
 * ?????? : 2023?07?06? 11:07:57
 * ??????: 2023?07?06? 10:48:01
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import com.hjq.base.AppAdapter;
import run.yigou.gxzy.data.local.entity.TabNavBody;

/**
 *  ??:  zhs
 *  ??:  2023-07-13 09:13:14
 *  ??:  BookInfoAdapter
 *  ??:  1.0
 *  ??:
 *
*/
public final class BookInfoAdapter extends AppAdapter<TabNavBody> {

    public BookInfoAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }
    private final class ViewHolder extends AppAdapter<?>.ViewHolder {


        private final TextView tvBookName;
        private final TextView tvBookAuthor;


        private ViewHolder() {
            super(R.layout.book_tab_label_book_item);

            tvBookName = findViewById(R.id.tv_book_name);
            tvBookAuthor = findViewById(R.id.tv_book_author);


        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindView(int position) {
            //mTextView.setText(getItem(position));
           TabNavBody item = null;
            if (getData() != null) {
                item = getData().get(position);
            }
            if (item != null) {
                tvBookName.setText(item.getBookName());
                tvBookAuthor.setText(item.getAuthor());
//                tvBookDesc.setText("     " + (item.getDesc() == null ?"":item.getDesc()));
//                GlideApp.with(this.getItemView())
//                        .load(AppConfig.getHostUrl()+item.getImageUrl())
//                        .into(ivBookImg);
            }
        }
    }
}