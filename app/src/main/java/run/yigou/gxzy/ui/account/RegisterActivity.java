package run.yigou.gxzy.ui.account;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.base.BaseActivity;
import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.Log;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.data.remote.api.RegisterApi;
import run.yigou.gxzy.manager.InputTextManager;
import run.yigou.gxzy.manager.account.AccountDataManager;
import com.hjq.widget.view.CountdownView;
import com.hjq.widget.view.SubmitButton;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : ????
 */
public final class RegisterActivity extends AppActivity
        implements TextView.OnEditorActionListener {

    private static final String INTENT_KEY_PHONE = "phone";
    private static final String INTENT_KEY_PASSWORD = "password";

    /** 账户数据管理器 */
    private final AccountDataManager mAccountDataManager = AccountDataManager.getInstance();

    @Log
    public static void start(BaseActivity activity, String phone, String password, OnRegisterListener listener) {
        Intent intent = new Intent(activity, RegisterActivity.class);
        intent.putExtra(INTENT_KEY_PHONE, phone);
        intent.putExtra(INTENT_KEY_PASSWORD, password);
        activity.startActivityForResult(intent, (resultCode, data) -> {

            if (listener == null || data == null) {
                return;
            }

            if (resultCode == RESULT_OK) {
                listener.onSucceed(data.getStringExtra(INTENT_KEY_PHONE), data.getStringExtra(INTENT_KEY_PASSWORD));
            } else {
                listener.onCancel();
            }
        });
    }

    private EditText mPhoneView;
    private CountdownView mCountdownView;

    private EditText mCodeView;

    private EditText mFirstPassword;
    private EditText mSecondPassword;

    private SubmitButton mCommitView;

    @Override
    protected int getLayoutId() {
        return R.layout.register_activity;
    }

    @Override
    protected void initView() {
        mPhoneView = findViewById(R.id.et_register_phone);
        mCountdownView = findViewById(R.id.cv_register_countdown);
        mCodeView = findViewById(R.id.et_register_code);
        mFirstPassword = findViewById(R.id.et_register_password1);
        mSecondPassword = findViewById(R.id.et_register_password2);
        mCommitView = findViewById(R.id.btn_register_commit);

        setOnClickListener(mCountdownView, mCommitView);

        mSecondPassword.setOnEditorActionListener(this);

        // ??? View ?????????????
        ImmersionBar.setTitleBar(this, findViewById(R.id.tv_register_title));

        InputTextManager.with(this)
                .addView(mPhoneView)
                .addView(mCodeView)
                .addView(mFirstPassword)
                .addView(mSecondPassword)
                .setMain(mCommitView)
                .build();
    }

    @Override
    protected void initData() {
        // ??????????
        mPhoneView.setText(getString(INTENT_KEY_PHONE));
        mFirstPassword.setText(getString(INTENT_KEY_PASSWORD));
        mSecondPassword.setText(getString(INTENT_KEY_PASSWORD));
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

            // 临时注释：TODO 待网络接口就绪后恢复
            /*
            if (true) {
                toast(R.string.common_code_send_hint);
                mCountdownView.start();
                return;
            }

            // 获取验证码
            EasyHttp.post(this)
                    .api(new GetCodeApi()
                            .setPhone(mPhoneView.getText().toString()))
                    .request(new HttpCallback<HttpData<Void>>(this) {

                        @Override
                        public void onSucceed(HttpData<Void> data) {
                            toast(R.string.common_code_send_hint);
                            mCountdownView.start();
                        }

                        @Override
                        public void onFail(Exception e) {
                            super.onFail(e);
                            mCountdownView.start();
                        }
                    });
            */
            
            // 使用 AccountDataManager 封装的网络请求
            mAccountDataManager.sendRegisterSmsCode(this,
                    mPhoneView.getText().toString(),
                    new AccountDataManager.Callback<Void>() {
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
                mCommitView.showError(3000);
                toast(R.string.common_phone_input_error);
                return;
            }

            if (mCodeView.getText().toString().length() != getResources().getInteger(R.integer.sms_code_length)) {
                mCodeView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                mCommitView.showError(3000);
                toast(R.string.common_code_error_hint);
                return;
            }

            if (!mFirstPassword.getText().toString().equals(mSecondPassword.getText().toString())) {
                mFirstPassword.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                mSecondPassword.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                mCommitView.showError(3000);
                toast(R.string.common_password_input_unlike);
                return;
            }

            // ?????
            hideKeyboard(getCurrentFocus());

            // 临时注释：TODO 待网络接口就绪后恢复
            /*
            if (true) {
                mCommitView.showProgress();
                postDelayed(() -> {
                    mCommitView.showSucceed();
                    postDelayed(() -> {
                        setResult(RESULT_OK, new Intent()
                                .putExtra(INTENT_KEY_PHONE, mPhoneView.getText().toString())
                                .putExtra(INTENT_KEY_PASSWORD, mFirstPassword.getText().toString()));
                        finish();
                    }, 1000);
                }, 2000);
                return;
            }

            // 注册
            EasyHttp.post(this)
                    .api(new RegisterApi()
                            .setPhone(mPhoneView.getText().toString())
                            .setCode(mCodeView.getText().toString())
                            .setPassword(mFirstPassword.getText().toString()))
                    .request(new HttpCallback<HttpData<RegisterApi.Bean>>(this) {

                        @Override
                        public void onStart(Call call) {
                            mCommitView.showProgress();
                        }

                        @Override
                        public void onEnd(Call call) {}

                        @Override
                        public void onSucceed(HttpData<RegisterApi.Bean> data) {
                            postDelayed(() -> {
                                mCommitView.showSucceed();
                                postDelayed(() -> {
                                    setResult(RESULT_OK, new Intent()
                                            .putExtra(INTENT_KEY_PHONE, mPhoneView.getText().toString())
                                            .putExtra(INTENT_KEY_PASSWORD, mFirstPassword.getText().toString()));
                                    finish();
                                }, 1000);
                            }, 1000);
                        }

                        @Override
                        public void onFail(Exception e) {
                            super.onFail(e);
                            postDelayed(() -> {
                                mCommitView.showError(3000);
                            }, 1000);
                        }
                    });
            */
            
            // 使用 AccountDataManager 封装的网络请求
            mAccountDataManager.register(this,
                    mPhoneView.getText().toString(),
                    mCodeView.getText().toString(),
                    mFirstPassword.getText().toString(),
                    new AccountDataManager.Callback<RegisterApi.Bean>() {
                        @Override
                        public void onSuccess(RegisterApi.Bean data) {
                            mCommitView.showProgress();
                            postDelayed(() -> {
                                mCommitView.showSucceed();
                                postDelayed(() -> {
                                    setResult(RESULT_OK, new Intent()
                                            .putExtra(INTENT_KEY_PHONE, mPhoneView.getText().toString())
                                            .putExtra(INTENT_KEY_PASSWORD, mFirstPassword.getText().toString()));
                                    finish();
                                }, 1000);
                            }, 1000);
                        }

                        @Override
                        public void onError(Exception e) {
                            postDelayed(() -> {
                                mCommitView.showError(3000);
                            }, 1000);
                            toast("注册失败：" + e.getMessage());
                        }
                    });
        }
    }

    @NonNull
    @Override
    protected ImmersionBar createStatusBarConfig() {
        return super.createStatusBarConfig()
                // ?????????
                .navigationBarColor(R.color.white)
                // ??????????
                .keyboardEnable(true);
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

    /**
     * ????
     */
    public interface OnRegisterListener {

        /**
         * ????
         *
         * @param phone             ???
         * @param password          ??
         */
        void onSucceed(String phone, String password);

        /**
         * ????
         */
        default void onCancel() {}
    }
}