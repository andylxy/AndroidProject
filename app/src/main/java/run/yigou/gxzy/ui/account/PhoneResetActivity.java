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
import com.hjq.toast.Toaster;
import com.hjq.widget.view.CountdownView;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/04/20
 *    desc   : ?????
 */
public final class PhoneResetActivity extends AppActivity
        implements TextView.OnEditorActionListener {

    private static final String INTENT_KEY_IN_CODE = "code";

    /** 账户数据管理器 */
    private final AccountDataManager mAccountDataManager = AccountDataManager.getInstance();

    @Log
    public static void start(Context context, String code) {
        Intent intent = new Intent(context, PhoneResetActivity.class);
        intent.putExtra(INTENT_KEY_IN_CODE, code);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private EditText mPhoneView;
    private EditText mCodeView;
    private CountdownView mCountdownView;
    private Button mCommitView;

    /** ??? */
    private String mVerifyCode;

    @Override
    protected int getLayoutId() {
        return R.layout.phone_reset_activity;
    }

    @Override
    protected void initView() {
        mPhoneView = findViewById(R.id.et_phone_reset_phone);
        mCodeView = findViewById(R.id.et_phone_reset_code);
        mCountdownView = findViewById(R.id.cv_phone_reset_countdown);
        mCommitView = findViewById(R.id.btn_phone_reset_commit);

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
        mVerifyCode = getString(INTENT_KEY_IN_CODE);
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
                    });
            */
            
            // 使用 AccountDataManager 封装的网络请求
            mAccountDataManager.sendResetPhoneSmsCode(this,
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
                Toaster.show(R.string.common_code_error_hint);
                return;
            }

            // ?????
            hideKeyboard(getCurrentFocus());

            // 临时注释：TODO 待网络接口就绪后恢复
            /*
            if (true) {
                new TipsDialog.Builder(this)
                        .setIcon(TipsDialog.ICON_FINISH)
                        .setMessage(R.string.phone_reset_commit_succeed)
                        .setDuration(2000)
                        .addOnDismissListener(dialog -> finish())
                        .show();
                return;
            }

            // ?????
            EasyHttp.post(this)
                    .api(new PhoneApi()
                            .setPreCode(mVerifyCode)
                            .setPhone(mPhoneView.getText().toString())
                            .setCode(mCodeView.getText().toString()))
                    .request(new HttpCallback<HttpData<Void>>(this) {

                        @Override
                        public void onSucceed(HttpData<Void> data) {
                            new TipsDialog.Builder(getActivity())
                                    .setIcon(TipsDialog.ICON_FINISH)
                                    .setMessage(R.string.phone_reset_commit_succeed)
                                    .setDuration(2000)
                                    .addOnDismissListener(dialog -> finish())
                                    .show();
                        }
                    });
            */
            
            // 使用 AccountDataManager 封装的网络请求
            mAccountDataManager.resetPhone(this,
                    mVerifyCode,
                    mPhoneView.getText().toString(),
                    mCodeView.getText().toString(),
                    new Callback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            new TipsDialog.Builder(getActivity())
                                    .setIcon(TipsDialog.ICON_FINISH)
                                    .setMessage(R.string.phone_reset_commit_succeed)
                                    .setDuration(2000)
                                    .addOnDismissListener(dialog -> finish())
                                    .show();
                        }

                        @Override
                        public void onError(Exception e) {
                            toast("重置手机号失败：" + e.getMessage());
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