package run.yigou.gxzy.ui.reader.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import com.hjq.base.AppAdapter;
import run.yigou.gxzy.ui.reader.constant.ContentTypes;
import run.yigou.gxzy.ui.reader.helper.TipsClickHandler;
import run.yigou.gxzy.tips.widget.LocalLinkMovementMethod;


public final class TipsFangYaoAdapter extends AppAdapter<String> {
    
    @ContentTypes.ContentType
    private final int contentType;

    public TipsFangYaoAdapter(Context context, @ContentTypes.ContentType int contentType) {
        super(context);
        this.contentType = contentType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    private final class ViewHolder extends AppAdapter<?>.ViewHolder {
        private final TextView mFang_Yao;

        private ViewHolder() {
            super(R.layout.tips_fragment_fang_yao_item);
            mFang_Yao = findViewById(R.id.tv_fang_yao);
            mFang_Yao.setMovementMethod(LocalLinkMovementMethod.getInstance());
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindView(int position) {
            switch (contentType) {
                case ContentTypes.FANG:
                    if (getData() != null) {
                        String f = (position + 1) + "、" + "$f{" + getData().get(position) + "}";
                        mFang_Yao.setText(TipsClickHandler.renderText(f));

                    }
                    break;
                case ContentTypes.YAO:
                    if (getData() != null) {
                        String u = (position + 1) + "、" + "$u{" + getData().get(position) + "}";
                        mFang_Yao.setText(TipsClickHandler.renderText(u));
                    }
                    break;
                case ContentTypes.HAN_ZHI_UNIT:
                    if (getData() != null && !getData().isEmpty()) {
                        mFang_Yao.setText((position + 1) + "、" + getData().get(position));
                    }
                    break;
                default:
                    break;
            }

        }
    }
}