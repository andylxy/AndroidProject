package run.yigou.gxzy.ui.dialog;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hjq.base.BaseDialog;

import run.yigou.gxzy.R;

/**
 * 协议对话框
 * 用于显示隐私协议和免责声明，窗口大小为屏幕的 95%
 */
public final class AgreementDialog {

    public static final class Builder extends BaseDialog.Builder<Builder> {

        private final TextView mTitleView;
        private final TextView mContentView;
        private final ScrollView mScrollView;
        private final ImageButton mCloseButton;

        public Builder(Context context) {
            super(context);
            setContentView(R.layout.agreement_dialog);
            setAnimStyle(BaseDialog.ANIM_IOS);
            setGravity(Gravity.CENTER);

            mTitleView = findViewById(R.id.tv_agreement_title);
            mContentView = findViewById(R.id.tv_agreement_content);
            mScrollView = findViewById(R.id.sv_agreement_content);
            mCloseButton = findViewById(R.id.btn_close);

            // 设置关闭按钮点击事件
            mCloseButton.setOnClickListener(v -> dismiss());
        }

        public Builder setTitle(String title) {
            mTitleView.setText(title);
            return this;
        }

        public Builder setContent(String content) {
            // 使用 Html.fromHtml 渲染 HTML 内容
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mContentView.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
            } else {
                mContentView.setText(Html.fromHtml(content));
            }
            return this;
        }

        @Override
        public BaseDialog create() {
            BaseDialog dialog = super.create();
            // 设置对话框大小为屏幕的 95%
            Window window = dialog.getWindow();
            if (window != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                window.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                
                WindowManager.LayoutParams params = window.getAttributes();
                params.width = (int) (displayMetrics.widthPixels * 0.95);
                params.height = (int) (displayMetrics.heightPixels * 0.95);
                params.gravity = Gravity.CENTER;
                window.setAttributes(params);
            }
            return dialog;
        }
    }
}
