package run.yigou.gxzy.ui.feature.media.adapter;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.github.chrisbanes.photoview.PhotoView;
import run.yigou.gxzy.R;
import com.hjq.base.AppAdapter;
import run.yigou.gxzy.network.glide.GlideApp;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2020/08/28
 *    desc   : ???????
 */
public final class ImagePreviewAdapter extends AppAdapter<String> {

    public ImagePreviewAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {

        private final PhotoView mPhotoView;

        private ViewHolder() {
            super(R.layout.image_preview_item);
            mPhotoView = (PhotoView) getItemView();
        }

        @Override
        public void onBindView(int position) {
            GlideApp.with(getContext())
                    .load(getItem(position))
                    .into(mPhotoView);
        }
    }
}