package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.other.AppConfig;


public final class TipsUnitFragmentAdapter extends AppAdapter<String> {

    public TipsUnitFragmentAdapter(Context context) {
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
        private final TextView tvTipsUnitFragmentItem;
        private ViewHolder() {
            super(R.layout.tips_unit_fragment_item);
            tvTipsUnitFragmentItem =findViewById(R.id. tv_tips_unit_fragment_item);
        }

        @Override
        public void onBindView(int position) {
            if (getData() != null && !getData().isEmpty()) {
                tvTipsUnitFragmentItem.setText(getData().get(position));
            }
        }
    }
}