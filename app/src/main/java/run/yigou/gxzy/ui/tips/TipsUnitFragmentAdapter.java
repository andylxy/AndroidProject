package run.yigou.gxzy.ui.tips;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import com.hjq.base.AppAdapter;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.app.AppConfig;


public final class TipsUnitFragmentAdapter extends AppAdapter<String> {

    public TipsUnitFragmentAdapter(Context context) {
        super(context);
    }


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

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindView(int position) {
            if (getData() != null && !getData().isEmpty()) {
                tvTipsUnitFragmentItem.setText((position+1)+"、"+getData().get(position));
            }
        }
    }
}