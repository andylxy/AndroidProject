package run.yigou.gxzy.ui.adapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import com.hjq.base.AppAdapter;

/**
 *  作者:  zhs
 *  时间:  2023-07-15 08:32:15
 *  包名:  run.yigou.gxzy.ui.adapter
 *  类名:  CopyAdapter
 *  版本:  1.0
 *  描述:
 *
*/
public final class CopyAdapter extends AppAdapter<String> {

    public CopyAdapter(Context context) {
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

        private ViewHolder() {
            super(R.layout.copy_item);
        }

        @Override
        public void onBindView(int position) {

        }
    }
}