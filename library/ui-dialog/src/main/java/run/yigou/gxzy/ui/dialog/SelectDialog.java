package run.yigou.gxzy.ui.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.base.BaseDialog;
import run.yigou.gxzy.ui.dialog.R;
import com.hjq.base.action.SingleClick;
import com.hjq.base.AppAdapter;
import com.hjq.toast.Toaster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *    author : Android 杞瓙鍝?
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/10/09
 *    desc   : 鍗曢€夋垨鑰呭閫夊璇濇
 */
public final class SelectDialog {

    public static final class Builder
            extends CommonDialog.Builder<Builder>
            implements View.OnLayoutChangeListener, Runnable {

        @SuppressWarnings("rawtypes")
        @Nullable
        private OnListener mListener;

        private final RecyclerView mRecyclerView;
        private final SelectAdapter mAdapter;

        public Builder(Context context) {
            super(context);

            setCustomView(R.layout.select_dialog);
            mRecyclerView = findViewById(R.id.rv_select_list);
            mRecyclerView.setItemAnimator(null);

            mAdapter = new SelectAdapter(getContext());
            mRecyclerView.setAdapter(mAdapter);
        }

        public Builder setList(int... ids) {
            List<String> data = new ArrayList<>(ids.length);
            for (int id : ids) {
                data.add(getString(id));
            }
            return setList(data);
        }

        public Builder setList(String... data) {
            return setList(Arrays.asList(data));
        }

        @SuppressWarnings("all")
        public Builder setList(List data) {
            mAdapter.setData(data);
            mRecyclerView.addOnLayoutChangeListener(this);
            return this;
        }

        /**
         * 璁剧疆榛樿閫変腑鐨勪綅缃?
         */
        public Builder setSelect(int... positions) {
            mAdapter.setSelect(positions);
            return this;
        }

        /**
         * 璁剧疆鏈€澶ч€夋嫨鏁伴噺
         */
        public Builder setMaxSelect(int count) {
            mAdapter.setMaxSelect(count);
            return this;
        }

        /**
         * 璁剧疆鏈€灏忛€夋嫨鏁伴噺
         */
        public Builder setMinSelect(int count) {
            mAdapter.setMinSelect(count);
            return this;
        }

        /**
         * 璁剧疆鍗曢€夋ā寮?
         */
        public Builder setSingleSelect() {
            mAdapter.setSingleSelect();
            return this;
        }

        @SuppressWarnings("rawtypes")
        public Builder setListener(OnListener listener) {
            mListener = listener;
            return this;
        }

        @SingleClick
        @SuppressWarnings("all")
        @Override
        public void onClick(View view) {
            int viewId = view.getId();
            if (viewId == R.id.tv_ui_confirm) {
                HashMap<Integer, Object> data = mAdapter.getSelectSet();
                if (data.size() >= mAdapter.getMinSelect()) {
                    autoDismiss();
                    if (mListener == null) {
                        return;
                    }
                    mListener.onSelected(getDialog(), data);
                } else {
                    Toaster.show(String.format(getString(R.string.select_min_hint), mAdapter.getMinSelect()));
                }
            } else if (viewId == R.id.tv_ui_cancel) {
                autoDismiss();
                if (mListener == null) {
                    return;
                }
                mListener.onCancel(getDialog());
            }
        }

        /**
         * {@link View.OnLayoutChangeListener}
         */
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            mRecyclerView.removeOnLayoutChangeListener(this);
            // 杩欓噷涓€瀹氳鍔犲欢杩燂紝濡傛灉涓嶅姞鍦?Android 9.0 涓婇潰浼氬鑷?setLayoutParams 鏃犳晥
            post(this);
        }

        @Override
        public void run() {
            final ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
            final int maxHeight = getScreenHeight() / 4 * 3;
            if (mRecyclerView.getHeight() > maxHeight) {
                if (params.height != maxHeight) {
                    params.height = maxHeight;
                    mRecyclerView.setLayoutParams(params);
                }
            } else {
                if (params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    mRecyclerView.setLayoutParams(params);
                }
            }
        }

        /**
         *  鑾峰彇灞忓箷鐨勯珮搴?
         */
        private int getScreenHeight() {
            Resources resources = getResources();
            DisplayMetrics outMetrics = resources.getDisplayMetrics();
            return outMetrics.heightPixels;
        }
    }

    private static final class SelectAdapter extends AppAdapter<Object>
            implements BaseAdapter.OnItemClickListener {

        /** 鏈€灏忛€夋嫨鏁伴噺 */
        private int mMinSelect = 1;
        /** 鏈€澶ч€夋嫨鏁伴噺 */
        private int mMaxSelect = Integer.MAX_VALUE;

        /** 閫夋嫨瀵硅薄闆嗗悎 */
        @SuppressLint("UseSparseArrays")
        private final HashMap<Integer, Object> mSelectSet = new HashMap<>();

        private SelectAdapter(Context context) {
            super(context);
            setOnItemClickListener(this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder();
        }

        private void setSelect(int... positions) {
            for (int position : positions) {
                mSelectSet.put(position, getItem(position));
            }
            notifyDataSetChanged();
        }

        private void setMaxSelect(int count) {
            mMaxSelect = count;
        }

        private void setMinSelect(int count) {
            mMinSelect = count;
        }

        private int getMinSelect() {
            return mMinSelect;
        }

        private void setSingleSelect() {
            setMaxSelect(1);
            setMinSelect(1);
        }

        private boolean isSingleSelect() {
            return mMaxSelect == 1 && mMinSelect == 1;
        }

        private HashMap<Integer, Object> getSelectSet() {
            return mSelectSet;
        }

        /**
         * {@link BaseAdapter.OnItemClickListener}
         */

        @Override
        public void onItemClick(RecyclerView recyclerView, View itemView, int position) {
            if (mSelectSet.containsKey(position)) {
                // 褰撳墠蹇呴』涓嶆槸鍗曢€夋ā寮忔墠鑳藉彇娑堥€変腑
                if (!isSingleSelect()) {
                    mSelectSet.remove(position);
                    notifyItemChanged(position);
                }
            } else {
                if (mMaxSelect == 1) {
                    mSelectSet.clear();
                    notifyDataSetChanged();
                }

                if (mSelectSet.size() < mMaxSelect) {
                    mSelectSet.put(position, getItem(position));
                    notifyItemChanged(position);
                } else {
                    Toaster.show(String.format(getString(R.string.select_max_hint), mMaxSelect));
                }
            }
        }

        private final class ViewHolder extends AppAdapter<?>.ViewHolder {

            private final TextView mTextView;
            private final CheckBox mCheckBox;

            ViewHolder() {
                super(R.layout.select_item);
                mTextView = findViewById(R.id.tv_select_text);
                mCheckBox = findViewById(R.id.tv_select_checkbox);
            }

            @Override
            public void onBindView(int position) {
                mTextView.setText(getItem(position).toString());
                mCheckBox.setChecked(mSelectSet.containsKey(position));
                if (mMaxSelect == 1) {
                    mCheckBox.setClickable(false);
                } else {
                    mCheckBox.setEnabled(false);
                }
            }
        }
    }

    public interface OnListener<T> {

        /**
         * 閫夋嫨鍥炶皟
         *
         * @param data              閫夋嫨鐨勪綅缃拰鏁版嵁
         */
        void onSelected(BaseDialog dialog, HashMap<Integer, T> data);

        /**
         * 鍙栨秷鍥炶皟
         */
        default void onCancel(BaseDialog dialog) {}
    }
}