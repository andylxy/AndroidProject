package run.yigou.gxzy.ui.account;

import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import run.yigou.gxzy.R;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.manager.InputTextManager;
import run.yigou.gxzy.manager.account.AccountDataManager;
import run.yigou.gxzy.manager.Callback;
import com.hjq.widget.view.CountdownView;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/02/27
 *    desc   : ????
 */
public final class PasswordForgetActivity extends AppActivity
        implements TextView.OnEditorActionListener {

    /** 账户数据管理器 */
    private final AccountDataManager mAccountDataManager = AccountDataManager.getInstance();

    private EditText mPhoneView;
    private EditText mCodeView;
    private CountdownView mCountdownView;
    private Button mCommitView;

    @Override
    protected int getLayoutId() {
        return R.layout.password_forget_activity;
    }

    @Override
    protected void initView() {
        mPhoneView = findViewById(R.id.et_password_forget_phone);
        mCodeView = findViewById(R.id.et_password_forget_code);
        mCountdownView = findViewById(R.id.cv_password_forget_countdown);
        mCommitView = findViewById(R.id.btn_password_forget_commit);

        setOnClickListener(mCountdownView, mCommitView);

        mCodeView.setOnEditorActionListener(this);

        InputTextManager.with(this)
                .addView(mPhoneView)
                .addView(mCodeView)
                .setMain(mCommitView)
                .build();
    }

    @Override
    protected void initData() {

    }

    @SingleClick
    @Override
    public void onClick(View view) {
        if (view == mCountdownView) {
            if (mPhoneView.getText().toString().length() != 11) {
                mPhoneView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                toast(R.string.common_phone_input_error);
                return;
            }

            if (true) {
                toast(R.string.common_code_send_hint);
                mCountdownView.start();
                return;
            }

            // 发送验证码
            hideKeyboard(getCurrentFocus());

            // 调用 AccountDataManager
            mAccountDataManager.sendForgetPasswordSmsCode(this,
                    mPhoneView.getText().toString(),
                    new Callback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            toast(R.string.common_code_send_hint);
                            mCountdownView.start();
                        }

                        @Override
                        public void onError(Exception e) {
                            toast("发送验证码失败：" + e.getMessage());
                        }
                    });
        } else if (view == mCommitView) {

            if (mPhoneView.getText().toString().length() != 11) {
                mPhoneView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                toast(R.string.common_phone_input_error);
                return;
            }

            if (mCodeView.getText().toString().length() != getResources().getInteger(R.integer.sms_code_length)) {
                mCodeView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                toast(R.string.common_code_error_hint);
                return;
            }

            if (true) {
                PasswordResetActivity.start(getActivity(), mPhoneView.getText().toString(), mCodeView.getText().toString());
                finish();
                return;
            }

            // 调用 AccountDataManager
            mAccountDataManager.verifySmsCodeForForgetPassword(this,
                    mPhoneView.getText().toString(),
                    mCodeView.getText().toString(),
                    new Callback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            PasswordResetActivity.start(getActivity(), mPhoneView.getText().toString(), mCodeView.getText().toString());
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            toast("验证码校验失败：" + e.getMessage());
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
            // ?????????
            onClick(mCommitView);
            return true;
        }
        return false;
    }
}