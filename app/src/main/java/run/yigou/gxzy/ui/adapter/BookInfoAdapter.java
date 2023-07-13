/*
 * 项目名: AndroidProject
 * 类名: BookInfoAdapter.java
 * 包名: run.yigou.gxzy.ui.adapter.bookinfo.BookInfoAdapter
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月06日 11:07:57
 * 上次修改时间: 2023年07月06日 10:48:01
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.http.api.BookInfoNav;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.other.AppConfig;

/**
 *  作者:  zhs
 *  时间:  2023-07-13 09:13:14
 *  包名:  run.yigou.gxzy.ui.adapter
 *  类名:  BookInfoAdapter
 *  版本:  1.0
 *  描述:
 *
*/
public final class BookInfoAdapter extends AppAdapter<BookInfoNav.Bean.NavItem> {

    public BookInfoAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }
    private final class ViewHolder extends AppAdapter<?>.ViewHolder {

       // private final TextView mTextView;
        private final TextView tvBookName;
        private final TextView tvBookAuthor;
        private final TextView tvBookDesc;
        private final ImageView ivBookImg;

        private ViewHolder() {
            super(R.layout.book_tab_label_book_item);
           // mTextView = findViewById(R.id.tv_status_text);
            tvBookName = findViewById(R.id.tv_book_name);
            tvBookAuthor = findViewById(R.id.tv_book_author);
            tvBookDesc = findViewById(R.id.tv_book_desc);
            ivBookImg = findViewById(R.id.iv_book_img);
            // mTextView = findViewById(R.id.tv_status_text);

        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindView(int position) {
            //mTextView.setText(getItem(position));
            BookInfoNav.Bean.NavItem item = null;
            if (getData() != null) {
                item = getData().get(position);
            }
            if (item != null) {
                tvBookName.setText(item.getBookName());
                tvBookAuthor.setText(item.getAuthor());
                tvBookDesc.setText("     " + (item.getDesc() == null ?"":item.getDesc()));
                GlideApp.with(this.getItemView())
                        .load(AppConfig.getHostUrl()+item.getImageUrl())
                        .into(ivBookImg);
            }
        }
    }
}