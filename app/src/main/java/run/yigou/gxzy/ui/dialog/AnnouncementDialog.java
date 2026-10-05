package run.yigou.gxzy.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.hjq.base.BaseDialog;
import com.hjq.base.action.SingleClick;

import run.yigou.gxzy.R;
import run.yigou.gxzy.data.remote.model.Announcement;

/**
 * 公告弹窗：标题 + 多行正文 + 关闭（DESIGN §6.3）。
 *
 * <p>骨架复用 {@code BaseDialog.Builder}（与 {@code UpdateDialog} 同族），但刻意
 * <b>没有</b>操作按钮：公告是「看完关闭」的被动通知，不该引导任何动作。</p>
 *
 * <p><b>可取消（setCancelable(true)）</b>：与强制升级框相反。公告不阻断任何功能，
 * 点外部/返回键关掉与点「关闭」等价，都会走 {@code onClose} 记已读
 * （需求 INV-5「关闭后正常进入 App，不拦截导航」）。</p>
 *
 * <p><b>正文按纯文本渲染</b>（{@code TextView.setText}）：<b>绝不</b>用
 * {@code Html.fromHtml} 或 WebView。后端内容是运营在后台输入的，一旦按 HTML 渲染，
 * 任何能写公告的人都能往客户端注入可执行内容；纯文本没有这个攻击面。</p>
 */
public final class AnnouncementDialog {

    public static final class Builder
            extends BaseDialog.Builder<Builder> {

        private final TextView mTitleView;
        private final TextView mContentView;
        private final TextView mCloseView;

        /** 用户**主动**关闭时的回调（用于记已读并弹下一条）。宿主销毁不会触发它。 */
        private Runnable mOnClose;

        /** 本次关闭是否由用户主动发起（点关闭 / 返回键 / 点外部）。 */
        private boolean mUserClosed;

        public Builder(Context context) {
            super(context);

            setContentView(R.layout.announcement_dialog);
            setAnimStyle(BaseDialog.ANIM_BOTTOM);
            // 公告不阻断使用：点外部/返回键等价于「关闭」，都会记已读。
            setCancelable(true);

            mTitleView = findViewById(R.id.tv_announcement_title);
            mContentView = findViewById(R.id.tv_announcement_content);
            mCloseView = findViewById(R.id.tv_announcement_close);
            setOnClickListener(mCloseView);

            // 返回键 / 点外部是**用户主动关闭**，与点「我知道了」同义。
            // Android 把这条路径走 OnCancel（不是 dismiss），所以必须单独挂：
            // 漏了它，用户明明关掉了公告却不会被记已读，下次又弹一遍。
            addOnCancelListener(new BaseDialog.OnCancelListener() {
                @Override
                public void onCancel(BaseDialog dialog) {
                    mUserClosed = true;
                }
            });

            // 让较长的正文可滚动（maxLines=8 之外的溢出仍要看得到）。
            mContentView.setMovementMethod(new ScrollingMovementMethod());
        }

        /**
         * 设置公告内容。
         */
        public Builder setAnnouncement(Announcement announcement) {
            if (announcement == null) {
                return this;
            }
            mTitleView.setText(announcement.getTitle());
            // 正文缺失时隐藏该区域，避免留一块空白。
            String body = announcement.getBody();
            boolean hasBody = body != null && !body.trim().isEmpty();
            mContentView.setText(hasBody ? body : "");
            mContentView.setVisibility(hasBody ? View.VISIBLE : View.GONE);
            return this;
        }

        /**
         * 设置「用户关闭」回调。
         */
        public Builder setOnCloseListener(Runnable runnable) {
            mOnClose = runnable;
            return this;
        }

        @SingleClick
        @Override
        public void onClick(View view) {
            if (view == mCloseView) {
                // 明确标记为「用户主动关闭」：只有这种关闭才算「已读」。
                mUserClosed = true;
                dismiss();
            }
        }

        @Override
        public void dismiss() {
            super.dismiss();
            // ⚠️ **只**在用户主动关闭时才回调。宿主被销毁（Activity 切换/被回收）也会走到
            // dismiss()，而那时用户**根本没看到**这条公告；若照样回调，调用方会把它记成
            // 「已读」，于是这条公告再也不会弹——用户从未读过的消息被当成读过了
            // （adb 实测踩到：三条公告在 Activity 切换瞬间全部被记已读）。
            if (mUserClosed && mOnClose != null) {
                mOnClose.run();
            }
        }
    }
}
