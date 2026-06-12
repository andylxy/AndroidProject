package run.yigou.gxzy.ui.feature.search.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import com.hjq.base.AppAdapter;
import run.yigou.gxzy.data.local.entity.SearchHistory;

/**
 *  作者:  zhs
 *  时间:  2023-07-14 14:25:35
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType
) {
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