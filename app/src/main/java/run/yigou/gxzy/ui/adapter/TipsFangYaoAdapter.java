

package run.yigou.gxzy.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppAdapter;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.widget.LocalLinkMovementMethod;


public final class TipsFangYaoAdapter extends AppAdapter<String> {
    private final String TAG = "TipsFangYaoAdapter";
    private final int typeFangYao;

    public TipsFangYaoAdapter(Context context, int typeFangYao) {
        super(context);
        this.typeFangYao = typeFangYao;
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
            switch (typeFangYao) {
                case 1:
                    if (getData() != null) {
                        String f = (position + 1) + "、" + "$f{" + getData().get(position) + "}";
                        mFang_Yao.setText(TipsNetHelper.renderText(f));

                    }
                    break;
                case 2:
                    if (getData() != null) {
                        String u = (position + 1) + "、" + "$u{" + getData().get(position) + "}";
                        mFang_Yao.setText(TipsNetHelper.renderText(u));
                    }
                    break;
                case 3:
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