package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.greendao.entity.SearchHistory;

/**
 *  作者:  zhs
 *  时间:  2023-07-14 14:25:35
 *  包名:  run.yigou.gxzy.ui.adapter
 *  类名:  SearchHistoryAdapter
 *  版本:  1.0
 *  描述:
 *
*/
public final class SearchHistoryAdapter extends AppAdapter<SearchHistory> {

    public SearchHistoryAdapter(Context context) {
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
      private   TextView tvContent;
        private ViewHolder() {
            super(R.layout.book_search_history_item);
             tvContent = findViewById(R.id.tv_history_content);
        }

        @Override
        public void onBindView(int position) {
            tvContent.setText(getItem(position).getContent());
        }
    }
}