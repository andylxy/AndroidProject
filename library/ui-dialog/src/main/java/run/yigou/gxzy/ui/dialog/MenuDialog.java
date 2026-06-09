package run.yigou.gxzy.ui.dialog;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.base.BaseAdapter;
import com.hjq.base.BaseDialog;
import run.yigou.gxzy.ui.dialog.R;
import com.hjq.base.action.SingleClick;
import com.hjq.base.AppAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *    author : Android 杞瓙鍝?
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/12/2
 *    desc   : 鑿滃崟閫夋嫨妗?
 */
public final class MenuDialog {

    public static final class Builder
            extends BaseDialog.Builder<Builder>
            implements BaseAdapter.OnItemClickListener,
            View.OnLayoutChangeListener, Runnable {

        @SuppressWarnings("rawtypes")
        @Nullable
        private OnListener mListener;
        private boolean mAutoDismiss = true;

        private final RecyclerView mRecyclerView;
        private final TextView mCancelView;

        private final MenuAdapter mAdapter;

        public Builder(Context context) {
            super(context);
            setContentView(R.layout.menu_dialog);
            setAnimStyle(BaseDialog.ANIM_BOTTOM);

            mRecyclerView = findViewById(R.id.rv_menu_list);
            mCancelView  = findViewById(R.id.tv_menu_cancel);
            setOnClickListener(mCancelView);

            mAdapter = new MenuAdapter(getContext());
            mAdapter.setOnItemClickListener(this);
            mRecyclerView.setAdapter(mAdapter);
        }

        @Override
        public Builder setGravity(int gravity) {
            switch (gravity) {
                // 濡傛灉杩欎釜鏄湪涓棿鏄剧ず鐨?
                case Gravity.CENTER:
                case Gravity.CENTER_VERTICAL:
                    // 涓嶆樉绀哄彇娑堟寜閽?
                    setCancel(null);
                    // 閲嶆柊璁剧疆鍔ㄧ敾
                    setAnimStyle(BaseDialog.ANIM_SCALE);
                    break;
                default:
                    break;
            }
            return super.setGravity(gravity);
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

        public Builder setCancel(@StringRes int id) {
            return setCancel(getString(id));
        }

        public Builder setCancel(CharSequence text) {
            mCancelView.setText(text);
            return this;
        }

        public Builder setAutoDismiss(boolean dismiss) {
            mAutoDismiss = dismiss;
            return this;
        }

        @SuppressWarnings("rawtypes")
        public Builder setListener(OnListener listener) {
            mListener = listener;
            return this;
        }

        @SingleClick
        @Override
        public void onClick(View view) {
            if (mAutoDismiss) {
                dismiss();
            }

            if (view == mCancelView) {
                if (mListener == null) {
                    return;
                }
                mListener.onCancel(getDialog());
            }
        }

        /**
         * {@link BaseAdapter.OnItemClickListener}
         */
        @SuppressWarnings("all")
        @Override
        public void onItemClick(RecyclerView recyclerView, View itemView, int position) {
            if (mAutoDismiss) {
                dismiss();
            }

            if (mListener == null) {
                return;
            }
            mListener.onSelected(getDialog(), position, mAdapter.getItem(position));
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
                return;
            }

            if (params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mRecyclerView.setLayoutParams(params);
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

    private static final class MenuAdapter extends AppAdapter<Object> {

        private MenuAdapter(Context context) {
            super(context);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder();
        }

        private final class ViewHolder extends AppAdapter<?>.ViewHolder {

            private final TextView mTextView;
            private final View mLineView;

            ViewHolder() {
                super(R.layout.menu_item);
                mTextView = findViewById(R.id.tv_menu_text);
                mLineView = findViewById(R.id.v_menu_line);
            }

            @Override
            public void onBindView(int position) {
                mTextView.setText(getItem(position).toString());

                if (position == 0) {
                    // 褰撳墠鏄惁鍙湁涓€涓潯鐩?
                    if (getCount() == 1) {
                        mLineView.setVisibility(View.GONE);
                    } else {
                        mLineView.setVisibility(View.VISIBLE);
                    }
                } else if (position == getCount() - 1) {
                    mLineView.setVisibility(View.GONE);
                } else {
                    mLineView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public interface OnListener<T> {

        /**
         * 閫夋嫨鏉＄洰鏃跺洖璋?
         */
        void onSelected(BaseDialog dialog, int position, T t);

        /**
         * 鐐瑰嚮鍙栨秷鏃跺洖璋?
         */
        default void onCancel(BaseDialog dialog) {}
    }
}