package run.yigou.gxzy.ui.feature.media.dialog;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.base.BaseDialog;
import com.hjq.base.BottomSheetDialog;
import run.yigou.gxzy.R;
import com.hjq.base.AppAdapter;
import run.yigou.gxzy.network.glide.GlideApp;

import java.util.List;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/07/27
 *    desc   : ?????????
 */
public final class AlbumDialog {

    public static final class Builder
            extends BaseDialog.Builder<Builder>
            implements BaseAdapter.OnItemClickListener {

        @Nullable
        private OnListener mListener;

        private final RecyclerView mRecyclerView;
        private final AlbumAdapter mAdapter;

        public Builder(Context context) {
            super(context);

            setContentView(R.layout.album_dialog);

            mRecyclerView = findViewById(R.id.rv_album_list);
            mAdapter = new AlbumAdapter(context);
            mAdapter.setOnItemClickListener(this);
            mRecyclerView.setAdapter(mAdapter);
        }

        public Builder setData(List<AlbumInfo> data) {
            mAdapter.setData(data);
            // ????????
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).isSelect()) {
                    mRecyclerView.scrollToPosition(i);
                    break;
                }
            }
            return this;
        }

        public Builder setListener(OnListener listener) {
            mListener = listener;
            return this;
        }

        @Override
        public void onItemClick(RecyclerView recyclerView, View itemView, int position) {
            List<AlbumInfo> data = mAdapter.getData();
            if (data == null) {
                return;
            }

            for (AlbumInfo info : data) {
                if (info.isSelect()) {
                    info.setSelect(false);
                    break;
                }
            }
            mAdapter.getItem(position).setSelect(true);
            mAdapter.notifyDataSetChanged();

            // ????
            postDelayed(() -> {

                if (mListener != null) {
                    mListener.onSelected(getDialog(), position, mAdapter.getItem(position));
                }
                dismiss();

            }, 300);
        }

        @NonNull
        @Override
        protected BaseDialog createDialog(Context context, int themeId) {
            BottomSheetDialog dialog = new BottomSheetDialog(context, themeId);
            dialog.getBottomSheetBehavior().setPeekHeight(getResources().getDisplayMetrics().heightPixels / 2);
            return dialog;
        }
    }

    private static final class AlbumAdapter extends AppAdapter<AlbumInfo> {

        private AlbumAdapter(Context context) {
            super(context);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder();
        }

        private final class ViewHolder extends AppAdapter<?>.ViewHolder {

            private final ImageView mIconView;
            private final TextView mNameView;
            private final TextView mRemarkView;
            private final CheckBox mCheckBox;

            private ViewHolder() {
                super(R.layout.album_item);
                mIconView = findViewById(R.id.iv_album_icon);
                mNameView = findViewById(R.id.tv_album_name);
                mRemarkView = findViewById(R.id.tv_album_remark);
                mCheckBox = findViewById(R.id.rb_album_check);
            }

            @Override
            public void onBindView(int position) {
                AlbumInfo info = getItem(position);

                GlideApp.with(getContext())
                        .asBitmap()
                        .load(info.getIcon())
                        .into(mIconView);

                mNameView.setText(info.getName());
                mRemarkView.setText(info.getRemark());
                mCheckBox.setChecked(info.isSelect());
                mCheckBox.setVisibility(info.isSelect() ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    /**
     * ?????
     */
    public static class AlbumInfo {

        /** ?? */
        private String icon;
        /** ?? */
        private String name;
        /** ?? */
        private String remark;
        /** ?? */
        private boolean select;

        public AlbumInfo(String icon, String name, String remark, boolean select) {
            this.icon = icon;
            this.name = name;
            this.remark = remark;
            this.select = select;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSelect(boolean select) {
            this.select = select;
        }

        public String getIcon() {
            return icon;
        }

        public String getName() {
            return name;
        }

        public String getRemark() {
            return remark;
        }

        public boolean isSelect() {
            return select;
        }
    }

    public interface OnListener {

        /**
         * ???????
         */
        void onSelected(BaseDialog dialog, int position, AlbumInfo bean);
    }
}