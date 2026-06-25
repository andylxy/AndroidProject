package run.yigou.gxzy.ui.account;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.Log;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.manager.InputTextManager;
import run.yigou.gxzy.manager.account.AccountDataManager;
import run.yigou.gxzy.manager.Callback;
import run.yigou.gxzy.ui.dialog.TipsDialog;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/02/27
 *    desc   : ????
 */
public final class PasswordResetActivity extends AppActivity
        implements TextView.OnEditorActionListener {

    private static final String INTENT_KEY_IN_PHONE = "phone";
    private static final String INTENT_KEY_IN_CODE = "code";

    /** 账户数据管理器 */
    private final AccountDataManager mAccountDataManager = AccountDataManager.getInstance();

    @Log
    public static void start(Context context, String phone, String code) {
        Intent intent = new Intent(context, PasswordResetActivity.class);
        intent.putExtra(INTENT_KEY_IN_PHONE, phone);
        intent.putExtra(INTENT_KEY_IN_CODE, code);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private EditText mFirstPassword;
    private EditText mSecondPassword;
    private Button mCommitView;

    /** ??? */
    private String mPhoneNumber;
    /** ??? */
    private String mVerifyCode;

    @Override
    protected int getLayoutId() {
        return R.layout.password_reset_activity;
    }

    @Override
    protected void initView() {
        mFirstPassword = findViewById(R.id.et_password_reset_password1);
        mSecondPassword = findViewById(R.id.et_password_reset_password2);
        mCommitView = findViewById(R.id.btn_password_reset_commit);

        setOnClickListener(mCommitView);

        mSecondPassword.setOnEditorActionListener(this);

        InputTextManager.with(this)
                .addView(mFirstPassword)
                .addView(mSecondPassword)
                .setMain(mCommitView)
                .build();
    }

    @Override
    protected void initData() {
        mPhoneNumber = getString(INTENT_KEY_IN_PHONE);
        mVerifyCode = getString(INTENT_KEY_IN_CODE);
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        if (view == mCommitView) {

            if (!mFirstPassword.getText().toString().equals(mSecondPassword.getText().toString())) {
                mFirstPassword.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                mSecondPassword.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                toast(R.string.common_password_input_unlike);
                return;
            }

            // ?????
            hideKeyboard(getCurrentFocus());

            // 临时注释：TODO 待网络接口就绪后恢复
            /*
            if (true) {
                new TipsDialog.Builder(this)
                        .setIcon(TipsDialog.ICON_FINISH)
                        .setMessage(R.string.password_reset_success)
                        .setDuration(2000)
                        .addOnDismissListener(dialog -> finish())
                        .show();
                return;
            }
            */

            // 使用 AccountDataManager 封装的网络请求
            mAccountDataManager.resetPassword(this,
                    mPhoneNumber,
                    mVerifyCode,
                    mFirstPassword.getText().toString(),
                    new Callback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            new TipsDialog.Builder(getActivity())
                                    .setIcon(TipsDialog.ICON_FINISH)
                                    .setMessage(R.string.password_reset_success)
                                    .setDuration(2000)
                                    .addOnDismissListener(dialog -> finish())
                                    .show();
                        }

                        @Override
                        public void onError(Exception e) {
                            toast("重置密码失败：" + e.getMessage());
                        }
                    });
        }
    }

    /**
     * {@link TextView.OnEditorActionListener}
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE && mCommitView.isEnabled()) {
            // ????????
            onClick(mCommitView);
            return true;
        }
        return false;
    }
}