package run.yigou.gxzy.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.hjq.base.BaseDialog;

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

        /**
         * 用户**主动**关闭时的回调（推进队列 + 弹下一条）。宿主销毁不会触发它。
         *
         * <p>⚠️ 必须同时挂在 {@link #dismiss()} 与 {@link #onCancel} 两条路径上：
         * Android 的返回键/点外部走 {@code Dialog.cancel()}，它内部调的是
         * <b>Dialog 自己的</b> {@code dismiss()}，<b>绕过 Builder 的覆写</b>。
         * 早期实现只挂在 {@code dismiss()} 上，注释还声称已处理返回键 —— 真机实测
         * 用户按返回键后同一条公告被当成「宿主销毁」放回队列立刻重弹，且把队列里
         * 后面的公告<b>永久堵住</b>（实测第二条弹出计数为 0）。</p>
         */
        private Runnable mOnClose;

        /** 本次关闭是否由用户主动发起（点「我知道了」/ 返回键 / 点外部）。 */
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
            // 必须用带 listener 的重载：`setOnClickListener(View...)` 只把 View 记进
            // 点击数组，事件仍派发到 ClickAction 的空实现，点击进不到下面的 listener。
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCloseViewClicked();
                }
            }, mCloseView);

            // 返回键 / 点外部是**用户主动关闭**，与点「我知道了」同义。
            // Android 把这条路径走 OnCancel（不是 dismiss），所以必须单独挂；
            // 且这里要**直接回调 mOnClose**（不能只置 mUserClosed）——因为
            // Dialog.cancel() 不会经过 Builder.dismiss()，那个标志没人读。
            addOnCancelListener(new BaseDialog.OnCancelListener() {
                @Override
                public void onCancel(BaseDialog dialog) {
                    mUserClosed = true;
                    if (mOnClose != null) {
                        mOnClose.run();
                    }
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

        /** 「我知道了」被点：标记为用户主动关闭并关掉弹窗。 */
        private void onCloseViewClicked() {
            // 只有这种关闭才算「用户主动关闭」；宿主销毁不走这里。
            mUserClosed = true;
            dismiss();
        }

        @Override
        public void dismiss() {
            // ⚠️ 顺序要紧：先回调 onClose，再 super.dismiss()。
            // super.dismiss() 内部会通知 BaseDialog 的 onDismiss 监听器；若先调它，
            // 接收方会看到「用户没主动关闭」（onClose 还没跑）而判定为宿主销毁，
            // 把这条公告放回队列 → 立刻又弹一次，死循环（实测 id=6 连弹 4 次）。
            if (mUserClosed && mOnClose != null) {
                mOnClose.run();
            }
            super.dismiss();
        }
    }
}
