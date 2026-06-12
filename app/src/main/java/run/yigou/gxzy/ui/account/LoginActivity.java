/*
 * ???: AndroidProject
 * ??: LoginActivity.java
 * ??: com.intellij.copyright.JavaCopyrightVariablesProvider$1@a563d04,qualifiedClassName
 * ?? : Zhs (xiaoyang_02@qq.com)
 * ?????? : 2023?07?05? 19:07:17
 * ??????: 2023?07?05? 17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.account;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gyf.immersionbar.ImmersionBar;

import run.yigou.gxzy.R;
import run.yigou.gxzy.crypto.SecurityUtils;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.base.constant.LoginType;
import run.yigou.gxzy.data.local.entity.UserInfo;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.remote.api.LoginApi;
import run.yigou.gxzy.data.remote.api.VierCode;
import run.yigou.gxzy.data.remote.api.GetCodeApi;

import run.yigou.gxzy.network.glide.GlideApp;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.manager.InputTextManager;
import com.hjq.base.KeyboardWatcher;
import run.yigou.gxzy.ui.home.HomeFragment;
import run.yigou.gxzy.ui.activity.HomeActivity;
import run.yigou.gxzy.utils.Base64ConverBitmapHelper;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.wxapi.WXEntryActivity;

import com.hjq.http.EasyHttp;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.listener.HttpCallback;
import com.hjq.umeng.Platform;
import com.hjq.umeng.UmengClient;
import com.hjq.umeng.UmengLogin;
import com.hjq.widget.view.CountdownView;
import com.hjq.widget.view.SubmitButton;


import okhttp3.Call;


/**
 * author : Android ???
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : ????
 */
public final class LoginActivity extends AppActivity implements UmengLogin.OnLoginListener, KeyboardWatcher.SoftKeyboardStateListener, TextView.OnEditorActionListener {

    private static final String INTENT_KEY_IN_PHONE = "phone";
    private static final String INTENT_KEY_IN_PASSWORD = "password";


    public static void start(Context context, String phone, String password) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(INTENT_KEY_IN_PHONE, phone);
        intent.putExtra(INTENT_KEY_IN_PASSWORD, password);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * Logo??
     */
    private ImageView mLogoView;

    /**
     * ????
     */
    private ViewGroup mBodyLayout;
    /**
     * ??????
     */
    private EditText mPhoneView;
    /**
     * ????????
     */
    private EditText mEtLoginSmsCode;
    /**
     * ?????
     */
    private EditText mPasswordView;

    /**
     * ??????
     */
    private View mForgetView;
    /**
     * ????
     */
    private SubmitButton mCommitView;

    /**
     * ????????
     */
    private View mOtherView;
    /**
     * QQ????
     */
    private View mQQView;
    /**
     * ??????
     */
    private View mWeChatView;

    /**
     * ????????
     */
    private View mIvLoginAccount;
    /**
     * ????????
     */
    private View mIvLoginPhone;
    /**
     * ???????
     */
    private View mLlLoginSmsCodeLinear;
    /**
     * ???????
     */
    private View mEtLoginVcodeLinear;

    /**
     * logo ????
     */
    private final float mLogoScale = 0.8f;
    /**
     * ????
     */
    private final int mAnimTime = 300;
    /**
     * ?????
     */
    private CountdownView mCountdownView;
    /**
     * ???????
     */
    private ImageView mEtLoginVcode;
    /**
     * ????????
     */
    private EditText mEtLoginTextCode;
    /**
     * ?????
     */
    private VierCode.Bean mVierificationCode;
    /**
     * ????????????
     */
    private int mLongInType = LoginType.mLoginAccount;
    /**
     * ?????
     */
    private KeyboardWatcher mKeyboardWatcher;
    /**
     * ??????????
     */
    private AnimatorSet mCurrentAnimatorSet;

    @Override
    protected int getLayoutId() {
        return R.layout.login_activity;
    }

    @Override
    protected void initView() {
        // ?????
        mLogoView = findViewById(R.id.iv_login_logo);
        mBodyLayout = findViewById(R.id.ll_login_body);
        mPhoneView = findViewById(R.id.et_login_phone);
        mPasswordView = findViewById(R.id.et_login_password);
        mForgetView = findViewById(R.id.tv_login_forget);
        mCommitView = findViewById(R.id.btn_login_commit);
        mOtherView = findViewById(R.id.ll_login_other);
        mQQView = findViewById(R.id.iv_login_qq);
        mWeChatView = findViewById(R.id.iv_login_wechat);
        mIvLoginAccount = findViewById(R.id.iv_login_account);
        mIvLoginPhone = findViewById(R.id.iv_login_phone);
        mLlLoginSmsCodeLinear = findViewById(R.id.ll_login_sms_code_linear);
        mEtLoginVcodeLinear = findViewById(R.id.et_login_vcode_linear);
        mCountdownView = findViewById(R.id.cv_login_sms_countdown);
        mEtLoginSmsCode = findViewById(R.id.et_login_sms_code);
        mEtLoginVcode = findViewById(R.id.et_login_vcode);
        mEtLoginTextCode = findViewById(R.id.et_login_text_code);
        
        // ????????
        if (inLoginOrNoLogin()) return;
        
        // ???????
        setOnClickListener(mForgetView, mCommitView, mQQView, mWeChatView, mIvLoginAccount, mIvLoginPhone, mCountdownView, mEtLoginVcode);
        // ??????????
        mPasswordView.setOnEditorActionListener(this);
        mEtLoginTextCode.setOnEditorActionListener(this);
        mEtLoginSmsCode.setOnEditorActionListener(this);
        
        // ?????
        getLoginVcode();
    }


    @Override
    protected void initData() {
        // ??????????
        postDelayed(() -> {
            mKeyboardWatcher = KeyboardWatcher.with(LoginActivity.this);
            mKeyboardWatcher.setListener(LoginActivity.this);
        }, 500);
        
        // ??????????
        mIvLoginAccount.setVisibility(View.GONE);
        mLlLoginSmsCodeLinear.setVisibility(View.GONE);
        
        // ??????????? QQ
        if (!UmengClient.isAppInstalled(this, Platform.QQ)) {
            mQQView.setVisibility(View.GONE);
        }

        // ?????????????
        if (!UmengClient.isAppInstalled(this, Platform.WECHAT)) {
            mWeChatView.setVisibility(View.GONE);
        }

        // ?????????????????????
        if (mQQView.getVisibility() == View.GONE && mWeChatView.getVisibility() == View.GONE) {
            mOtherView.setVisibility(View.GONE);
        }

        // ??????????
        mPhoneView.setText(getString(INTENT_KEY_IN_PHONE));
        mPasswordView.setText(getString(INTENT_KEY_IN_PASSWORD));
    }

    /**
     * ????????
     *
     * @return true ???,false ???
     */
    private boolean inLoginOrNoLogin() {
        //?????,??????
        if (AppApplication.application.isLogin) {
            homeActivityStart();
            return true;
        }
        return false;
    }

    @Override
    public void onRightClick(View view) {
        // ???????
        RegisterActivity.start(this, mPhoneView.getText().toString(), mPasswordView.getText().toString(), (phone, password) -> {
            // ????????????????
            mPhoneView.setText(phone);
            mPasswordView.setText(password);
            mPasswordView.requestFocus();
            mPasswordView.setSelection(mPasswordView.getText().length());
            onClick(mCommitView);
        });
    }

    @SingleClick
    @Override
    public void onClick(View view) {
        if (view == mForgetView) {
            handleForgetPassword();
        } else if (view == mCountdownView) {
            handleGetVerificationCode();
        } else if (view == mIvLoginAccount) {
            handleSwitchToAccountLogin();
        } else if (view == mIvLoginPhone) {
            handleSwitchToPhoneLogin();
        } else if (view == mEtLoginVcode) {
            handleRefreshVerificationCode();
        } else if (view == mCommitView) {
            handleLogin();
        } else if (view == mQQView || view == mWeChatView) {
            handleThirdPartyLogin(view);
        }
    }

    /**
     * ??????
     */
    private void handleForgetPassword() {
        startActivity(PasswordForgetActivity.class);
    }

    /**
     * ???????
     */
    private void handleGetVerificationCode() {
        String phone = mPhoneView.getText().toString();
        if (phone == null || phone.length() != 11) {
            mPhoneView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
            toast(R.string.common_phone_input_error);
            return;
        }

        // ?????
        hideKeyboard(getCurrentFocus());

        // ?????
        EasyHttp.post(this)
                .api(new GetCodeApi()
                        .setPhone(phone))
                .request(new HttpCallback<HttpData<Void>>(this) {

                    @Override
                    public void onSucceed(HttpData<Void> data) {
                        toast(R.string.common_code_send_hint);
                        mCountdownView.start();
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        Log.e("LoginActivity", "Get SMS code failed: " + e.getMessage(), e);
                        toast("???????????????");
                    }
                });
    }

    /**
     * ???????
     */
    private void handleSwitchToAccountLogin() {
        setViewShow(mIvLoginAccount);
        mLongInType = LoginType.mLoginAccount;
    }

    /**
     * ???????
     */
    private void handleSwitchToPhoneLogin() {
        setViewShow(mIvLoginPhone);
        mLongInType = LoginType.mLoginPhone;
    }

    /**
     * ?????
     */
    private void handleRefreshVerificationCode() {
        getLoginVcode();
    }

    /**
     * ????
     */
    private void handleLogin() {
        // ?????
        hideKeyboard(getCurrentFocus());

        // ??????
        LoginApi requestApi = buildLoginRequest();
        if (requestApi != null) {
            login(requestApi);
        }
    }

    /**
     * ??????
     */
    private LoginApi buildLoginRequest() {
        if (mLongInType == LoginType.mLoginAccount) {
            String username = mPhoneView.getText().toString();
            String password = mPasswordView.getText().toString();
            
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                toast("????????");
                return null;
            }
            
            String passwd = SecurityUtils.doSm2Encrypt(password);
            if (passwd == null || passwd.isEmpty()) {
                toast("??????????");
                return null;
            }
            
            LoginApi requestApi = new LoginApi()
                    .setUserName(username)
                    .setPassword(passwd);

            if (mVierificationCode != null) {
                if (mVierificationCode.isCode()) {
                    String verificationCode = mEtLoginTextCode.getText().toString();
                    if (verificationCode == null || verificationCode.isEmpty()) {
                        toast("??????");
                        return null;
                    }
                    requestApi.setVerificationCode(verificationCode)
                            .setUUID(mVierificationCode.getUuid());
                } else if (mVierificationCode.getUuid() != null) {
                    requestApi.setUUID(mVierificationCode.getUuid());
                }
            }
            return requestApi;
        } else if (mLongInType == LoginType.mLoginPhone) {
            String phone = mPhoneView.getText().toString();
            String smsCode = mPasswordView.getText().toString();
            
            if (phone == null || phone.isEmpty() || phone.length() != 11) {
                mPhoneView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
                toast(R.string.common_phone_input_error);
                return null;
            }
            
            if (smsCode == null || smsCode.isEmpty()) {
                toast("??????");
                return null;
            }
            
            return new LoginApi()
                    .setUserName(phone)
                    .setPassword(smsCode);
        }
        return null;
    }

    /**
     * ???????
     */
    private void handleThirdPartyLogin(View view) {
        toast("??????? AppID ? Secret?????????");
        Platform platform;
        if (view == mQQView) {
            platform = Platform.QQ;
        } else if (view == mWeChatView) {
            platform = Platform.WECHAT;
            toast("??????? " + WXEntryActivity.class.getSimpleName() + " ???????");
        } else {
            throw new IllegalStateException("are you ok?");
        }
        UmengClient.login(this, platform, this);
    }

    private void login(IRequestApi requestApi) {
        EasyHttp.post(this)
                .api(requestApi)
                .request(new HttpCallback<HttpData<LoginApi.Bean>>(this) {

                    @Override
                    public void onStart(Call call) {
                        mCommitView.showProgress();
                    }

                    @Override
                    public void onSucceed(HttpData<LoginApi.Bean> data) {
                        if (data == null || data.getData() == null) {
                            Log.e("LoginActivity", "Login failed: data is empty or null");
                            toast("????????????????");
                            mCommitView.showError(3000);
                            return;
                        }
                        
                        try {
                            // ??????
                            AppApplication.getApplication().mUserInfoToken = data.getData();
                            // ??????????
                            String userLoginAccount = data.getData().getAccessKeyId();
                            if (userLoginAccount != null && !userLoginAccount.isEmpty()) {
                                UserInfo userInfo = DbService.getInstance().mUserInfoService.findUserInfoByLoginAccount(userLoginAccount);
                                AppApplication.application.isLogin = true;
                                
                                try {
                                    if (userInfo == null) {
                                        // ????????????
                                        DbService.getInstance().mUserInfoService.deleteAll();
                                        // ?????
                                        DbService.getInstance().mUserInfoService.addEntity(data.getData());
                                    } else {
                                        // ?????
                                        DbService.getInstance().mUserInfoService.deleteEntity(data.getData());
                                    }
                                } catch (Exception e) {
                                    // ??????
                                    Log.e("LoginActivity", "Database operation failed: " + e.getMessage(), e);
                                    toast("????????");
                                }
                            }
                            
                            homeActivityStart();
                        } catch (Exception e) {
                            Log.e("LoginActivity", "Login success handler failed: " + e.getMessage(), e);
                            toast("?????????????");
                            mCommitView.showError(3000);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        Log.e("LoginActivity", "Login request failed: " + e.getMessage(), e);
                        postDelayed(() -> {
                            mCommitView.showError(3000);
                        }, 1000);
                    }
                });
    }

    private void homeActivityStart() {
        // ????? , ?????
        // ?????
        HomeActivity.start(getContext(), HomeFragment.class);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ???????Activity??????????????
        mKeyboardWatcher = null;
    }

    private void getLoginVcode() {
        EasyHttp.get(this)
                .api(new VierCode())
                .request(new HttpCallback<HttpData<VierCode.Bean>>(this) {
                    @Override
                    public void onSucceed(HttpData<VierCode.Bean> data) {
                        try {
                            if (data != null && data.getData() != null) {
                                if (data.getData().isCode()) {
                                    String img = data.getData().getImg();
                                    if (!StringHelper.isEmpty(img)) {
                                        Bitmap bitmap = Base64ConverBitmapHelper.getBase64ToImage(img);
                                        setLoginVcode(bitmap);
                                    }
                                }
                                mVierificationCode = data.getData();
                                setViewShow(mIvLoginAccount);
                            } else {
                                Log.e("LoginActivity", "Get verification code failed: data is null");
                                toast("???????????");
                            }
                        } catch (Exception e) {
                            Log.e("LoginActivity", "Get verification code failed: " + e.getMessage(), e);
                            toast("???????????");
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        Log.e("LoginActivity", "Get verification code request failed: " + e.getMessage(), e);
                        toast("???????????????");
                    }
                });
    }

    private void setLoginVcode(Bitmap bitmap) {
        GlideApp.with(this)
                .load(bitmap)
                .into(mEtLoginVcode);
    }

    private void setViewShow(View view) {
        if (view == mIvLoginAccount) {
            mIvLoginAccount.setVisibility(View.GONE);
            mIvLoginPhone.setVisibility(View.VISIBLE);
            mLlLoginSmsCodeLinear.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.VISIBLE);
            mForgetView.setVisibility(View.VISIBLE);
            mEtLoginTextCode.setVisibility(View.VISIBLE);
            mEtLoginSmsCode.setText("");
            //?????????
            if (mVierificationCode != null && mVierificationCode.isCode()) {
                mEtLoginVcodeLinear.setVisibility(View.VISIBLE);
                createInputTextManager(true);
            } else {
                mEtLoginVcodeLinear.setVisibility(View.GONE);
                createInputTextManager(false);
            }

        } else if (view == mIvLoginPhone) {
            mIvLoginPhone.setVisibility(View.GONE);
            mIvLoginAccount.setVisibility(View.VISIBLE);
            mLlLoginSmsCodeLinear.setVisibility(View.VISIBLE);
            mPasswordView.setVisibility(View.GONE);
            mForgetView.setVisibility(View.GONE);
            mPasswordView.setText("");
            mEtLoginVcodeLinear.setVisibility(View.GONE);
            mEtLoginTextCode.setVisibility(View.GONE);
            createInputTextManagerForPhoneLogin();
        }

    }

    /**
     * ??????????????
     * @param needVerificationCode ???????
     */
    private void createInputTextManager(boolean needVerificationCode) {
        InputTextManager.Builder builder = InputTextManager.with(this)
                .addView(mPhoneView)
                .addView(mPasswordView);
        if (needVerificationCode) {
            builder.addView(mEtLoginTextCode);
        }
        builder.setMain(mCommitView).build();
    }

    /**
     * ??????????????
     */
    private void createInputTextManagerForPhoneLogin() {
        InputTextManager.with(this)
                .addView(mPhoneView)
                .addView(mEtLoginSmsCode)
                .setMain(mCommitView).build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // ????
        UmengClient.onActivityResult(this, requestCode, resultCode, data);
    }

    /**
     * {@link UmengLogin.OnLoginListener}
     */

    /**
     * ???????
     *
     * @param platform ????
     * @param data     ??????
     */
    @Override
    public void onSucceed(Platform platform, UmengLogin.LoginData data) {
        if (isFinishing() || isDestroyed()) {
            // Glide?You cannot start a load for a destroyed activity
            return;
        }

        // ??????????
        switch (platform) {
            case QQ:
                break;
            case WECHAT:
                break;
            default:
                break;
        }

        GlideApp.with(this).load(data.getAvatar()).circleCrop().into(mLogoView);

        toast("???" + data.getName() + "\n" + "???" + data.getSex() + "\n" + "id?" + data.getId() + "\n" + "token?" + data.getToken());
    }

    /**
     * ???????
     *
     * @param platform ????
     * @param t        ????
     */
    @Override
    public void onError(Platform platform, Throwable t) {
        toast("????????" + t.getMessage());
    }

    /**
     * {@link KeyboardWatcher.SoftKeyboardStateListener}
     */

    @Override
    public void onSoftKeyboardOpened(int keyboardHeight) {
        // ???????
        cancelCurrentAnimator();
        
        // ??????
        ObjectAnimator bodyAnimator = ObjectAnimator.ofFloat(mBodyLayout, "translationY", 0, -mCommitView.getHeight());
        bodyAnimator.setDuration(mAnimTime);
        bodyAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bodyAnimator.start();

        // ??????
        mLogoView.setPivotX(mLogoView.getWidth() / 2f);
        mLogoView.setPivotY(mLogoView.getHeight());
        mCurrentAnimatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mLogoView, "scaleX", 1f, mLogoScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mLogoView, "scaleY", 1f, mLogoScale);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(mLogoView, "translationY", 0f, -mCommitView.getHeight());
        mCurrentAnimatorSet.play(translationY).with(scaleX).with(scaleY);
        mCurrentAnimatorSet.setDuration(mAnimTime);
        mCurrentAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mCurrentAnimatorSet.start();
    }

    @Override
    public void onSoftKeyboardClosed() {
        // ???????
        cancelCurrentAnimator();
        
        // ??????
        ObjectAnimator bodyAnimator = ObjectAnimator.ofFloat(mBodyLayout, "translationY", mBodyLayout.getTranslationY(), 0f);
        bodyAnimator.setDuration(mAnimTime);
        bodyAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bodyAnimator.start();

        if (mLogoView.getTranslationY() == 0) {
            return;
        }

        // ??????
        mLogoView.setPivotX(mLogoView.getWidth() / 2f);
        mLogoView.setPivotY(mLogoView.getHeight());
        mCurrentAnimatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mLogoView, "scaleX", mLogoScale, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mLogoView, "scaleY", mLogoScale, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(mLogoView, "translationY", mLogoView.getTranslationY(), 0f);
        mCurrentAnimatorSet.play(translationY).with(scaleX).with(scaleY);
        mCurrentAnimatorSet.setDuration(mAnimTime);
        mCurrentAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mCurrentAnimatorSet.start();
    }

    /**
     * ???????????
     */
    private void cancelCurrentAnimator() {
        if (mCurrentAnimatorSet != null && mCurrentAnimatorSet.isRunning()) {
            mCurrentAnimatorSet.cancel();
        }
    }

    /**
     * {@link TextView.OnEditorActionListener}
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // ?????????????
        if (actionId == EditorInfo.IME_ACTION_DONE || 
            (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
            
            // ???????????????
            if (mCommitView.isEnabled()) {
                // ????????
                if (isInputValidForLogin()) {
                    // ?????
                    hideKeyboard(v);
                    // ??????????????
                    v.postDelayed(() -> {
                        if (mCommitView.isEnabled()) {
                            onClick(mCommitView);
                        }
                    }, 100);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * ???????????????
     */
    private boolean isInputValidForLogin() {
        if (mLongInType == LoginType.mLoginAccount) {
            // ?????????????????????????
            String username = mPhoneView.getText().toString();
            String password = mPasswordView.getText().toString();
            
            if (username.isEmpty() || password.isEmpty()) {
                return false;
            }
            
            // ???????
            if (mVierificationCode != null && mVierificationCode.isCode() && 
                mEtLoginVcodeLinear.getVisibility() == View.VISIBLE) {
                String verificationCode = mEtLoginTextCode.getText().toString();
                return !verificationCode.isEmpty();
            }
            
            return true;
        } else if (mLongInType == LoginType.mLoginPhone) {
            // ????????????????
            String phone = mPhoneView.getText().toString();
            String smsCode = mEtLoginSmsCode.getText().toString();
            
            return !phone.isEmpty() && !smsCode.isEmpty();
        }
        
        return false;
    }

    @NonNull
    @Override
    protected ImmersionBar createStatusBarConfig() {
        return super.createStatusBarConfig()
                // ?????????
                .navigationBarColor(R.color.white);
    }
}