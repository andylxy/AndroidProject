/*
 * 项目名: AndroidProject
 * 类名: LoginActivity.java
 * 包名: com.intellij.copyright.JavaCopyrightVariablesProvider$1@a563d04,qualifiedClassName
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 19:07:17
 * 上次修改时间: 2023年07月05日 17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.feature.account;

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

import run.yigou.gxzy.eventbus.LoginEventNotification;
import run.yigou.gxzy.R;
import run.yigou.gxzy.crypto.SecurityUtils;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.common.LoginType;
import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.LoginApi;
import run.yigou.gxzy.http.api.VierCode;
import run.yigou.gxzy.http.api.GetCodeApi;

import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.manager.InputTextManager;
import com.hjq.base.KeyboardWatcher;
import run.yigou.gxzy.ui.home.HomeFragment;
import run.yigou.gxzy.ui.home.HomeActivity;
import run.yigou.gxzy.utils.Base64ConverBitmapHelper;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.wxapi.WXEntryActivity;

import com.hjq.http.EasyConfig;
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
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 登录界面
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
     * Logo视图
     */
    private ImageView mLogoView;

    /**
     * 主体布局
     */
    private ViewGroup mBodyLayout;
    /**
     * 手机号输入框
     */
    private EditText mPhoneView;
    /**
     * 短信验证码输入框
     */
    private EditText mEtLoginSmsCode;
    /**
     * 密码输入框
     */
    private EditText mPasswordView;

    /**
     * 忘记密码按钮
     */
    private View mForgetView;
    /**
     * 登录按钮
     */
    private SubmitButton mCommitView;

    /**
     * 其他登录方式布局
     */
    private View mOtherView;
    /**
     * QQ登录按钮
     */
    private View mQQView;
    /**
     * 微信登录按钮
     */
    private View mWeChatView;

    /**
     * 账号登录切换按钮
     */
    private View mIvLoginAccount;
    /**
     * 手机登录切换按钮
     */
    private View mIvLoginPhone;
    /**
     * 短信验证码布局
     */
    private View mLlLoginSmsCodeLinear;
    /**
     * 图形验证码布局
     */
    private View mEtLoginVcodeLinear;

    /**
     * logo 缩放比例
     */
    private final float mLogoScale = 0.8f;
    /**
     * 动画时间
     */
    private final int mAnimTime = 300;
    /**
     * 倒计时视图
     */
    private CountdownView mCountdownView;
    /**
     * 图形验证码视图
     */
    private ImageView mEtLoginVcode;
    /**
     * 验证码文本输入框
     */
    private EditText mEtLoginTextCode;
    /**
     * 验证码信息
     */
    private VierCode.Bean mVierificationCode;
    /**
     * 登录类型，默认为账号登录
     */
    private int mLongInType = LoginType.mLoginAccount;
    /**
     * 键盘监听器
     */
    private KeyboardWatcher mKeyboardWatcher;
    /**
     * 当前正在执行的动画集
     */
    private AnimatorSet mCurrentAnimatorSet;

    @Override
    protected int getLayoutId() {
        return R.layout.login_activity;
    }

    @Override
    protected void initView() {
        // 初始化视图
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
        
        // 检查是否已经登录
        if (inLoginOrNoLogin()) return;
        
        // 设置点击监听器
        setOnClickListener(mForgetView, mCommitView, mQQView, mWeChatView, mIvLoginAccount, mIvLoginPhone, mCountdownView, mEtLoginVcode);
        // 设置编辑器动作监听器
        mPasswordView.setOnEditorActionListener(this);
        mEtLoginTextCode.setOnEditorActionListener(this);
        mEtLoginSmsCode.setOnEditorActionListener(this);
        
        // 获取验证码
        getLoginVcode();
    }


    @Override
    protected void initData() {
        // 延迟初始化键盘监听器
        postDelayed(() -> {
            mKeyboardWatcher = KeyboardWatcher.with(LoginActivity.this);
            mKeyboardWatcher.setListener(LoginActivity.this);
        }, 500);
        
        // 默认使用账号密码登录
        mIvLoginAccount.setVisibility(View.GONE);
        mLlLoginSmsCodeLinear.setVisibility(View.GONE);
        
        // 判断用户当前有没有安装 QQ
        if (!UmengClient.isAppInstalled(this, Platform.QQ)) {
            mQQView.setVisibility(View.GONE);
        }

        // 判断用户当前有没有安装微信
        if (!UmengClient.isAppInstalled(this, Platform.WECHAT)) {
            mWeChatView.setVisibility(View.GONE);
        }

        // 如果这两个都没有安装就隐藏其他登录方式提示
        if (mQQView.getVisibility() == View.GONE && mWeChatView.getVisibility() == View.GONE) {
            mOtherView.setVisibility(View.GONE);
        }

        // 自动填充手机号和密码
        mPhoneView.setText(getString(INTENT_KEY_IN_PHONE));
        mPasswordView.setText(getString(INTENT_KEY_IN_PASSWORD));
    }

    /**
     * 检查是否已经登陆
     *
     * @return true 已登陆,false 未登陆
     */
    private boolean inLoginOrNoLogin() {
        //如果已登陆,侧跳转到我的
        if (AppApplication.application.isLogin) {
            homeActivityStart();
            return true;
        }
        return false;
    }

    @Override
    public void onRightClick(View view) {
        // 跳转到注册界面
        RegisterActivity.start(this, mPhoneView.getText().toString(), mPasswordView.getText().toString(), (phone, password) -> {
            // 如果已经注册成功，就执行登录操作
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
     * 处理忘记密码
     */
    private void handleForgetPassword() {
        startActivity(PasswordForgetActivity.class);
    }

    /**
     * 处理获取验证码
     */
    private void handleGetVerificationCode() {
        String phone = mPhoneView.getText().toString();
        if (phone == null || phone.length() != 11) {
            mPhoneView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
            toast(R.string.common_phone_input_error);
            return;
        }

        // 隐藏软键盘
        hideKeyboard(getCurrentFocus());

        // 获取验证码
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
                        toast("获取验证码失败，请检查网络连接");
                    }
                });
    }

    /**
     * 切换到账号登录
     */
    private void handleSwitchToAccountLogin() {
        setViewShow(mIvLoginAccount);
        mLongInType = LoginType.mLoginAccount;
    }

    /**
     * 切换到手机登录
     */
    private void handleSwitchToPhoneLogin() {
        setViewShow(mIvLoginPhone);
        mLongInType = LoginType.mLoginPhone;
    }

    /**
     * 刷新验证码
     */
    private void handleRefreshVerificationCode() {
        getLoginVcode();
    }

    /**
     * 处理登录
     */
    private void handleLogin() {
        // 隐藏软键盘
        hideKeyboard(getCurrentFocus());

        // 登录逻辑处理
        LoginApi requestApi = buildLoginRequest();
        if (requestApi != null) {
            login(requestApi);
        }
    }

    /**
     * 构建登录请求
     */
    private LoginApi buildLoginRequest() {
        if (mLongInType == LoginType.mLoginAccount) {
            String username = mPhoneView.getText().toString();
            String password = mPasswordView.getText().toString();
            
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                toast("请输入账号和密码");
                return null;
            }
            
            String passwd = SecurityUtils.doSm2Encrypt(password);
            if (passwd == null || passwd.isEmpty()) {
                toast("密码加密失败，请重试");
                return null;
            }
            
            LoginApi requestApi = new LoginApi()
                    .setUserName(username)
                    .setPassword(passwd);

            if (mVierificationCode != null) {
                if (mVierificationCode.isCode()) {
                    String verificationCode = mEtLoginTextCode.getText().toString();
                    if (verificationCode == null || verificationCode.isEmpty()) {
                        toast("请输入验证码");
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
                toast("请输入验证码");
                return null;
            }
            
            return new LoginApi()
                    .setUserName(phone)
                    .setPassword(smsCode);
        }
        return null;
    }

    /**
     * 处理第三方登录
     */
    private void handleThirdPartyLogin(View view) {
        toast("记得改好第三方 AppID 和 Secret，否则会调不起来哦");
        Platform platform;
        if (view == mQQView) {
            platform = Platform.QQ;
        } else if (view == mWeChatView) {
            platform = Platform.WECHAT;
            toast("也别忘了改微信 " + WXEntryActivity.class.getSimpleName() + " 类所在的包名哦");
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
                            toast("登陆失败，请检查账号密码是否正确");
                            mCommitView.showError(3000);
                            return;
                        }
                        
                        try {
                            // 保存登陆信息
                            AppApplication.getApplication().mUserInfoToken = data.getData();
                            // 保存登陆信息到数据库
                            String userLoginAccount = data.getData().getAccessKeyId();
                            if (userLoginAccount != null && !userLoginAccount.isEmpty()) {
                                UserInfo userInfo = DbService.getInstance().mUserInfoService.findUserInfoByLoginAccount(userLoginAccount);
                                AppApplication.application.isLogin = true;
                                
                                try {
                                    if (userInfo == null) {
                                        // 插入数据库前删除所有数据
                                        DbService.getInstance().mUserInfoService.deleteAll();
                                        // 插入数据库
                                        DbService.getInstance().mUserInfoService.addEntity(data.getData());
                                    } else {
                                        // 更新数据库
                                        DbService.getInstance().mUserInfoService.deleteEntity(data.getData());
                                    }
                                } catch (Exception e) {
                                    // 记录异常日志
                                    Log.e("LoginActivity", "Database operation failed: " + e.getMessage(), e);
                                    toast("登陆信息保存失败");
                                }
                            }
                            
                            homeActivityStart();
                        } catch (Exception e) {
                            Log.e("LoginActivity", "Login success handler failed: " + e.getMessage(), e);
                            toast("登陆成功但处理失败，请重试");
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
        // 登录成功后 , 跳转到首页
        // 跳转到首页
        HomeActivity.start(getContext(), HomeFragment.class);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 键盘监听器会在Activity销毁时自动清理，无需手动移除
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
                                toast("获取验证码失败，请重试");
                            }
                        } catch (Exception e) {
                            Log.e("LoginActivity", "Get verification code failed: " + e.getMessage(), e);
                            toast("获取验证码失败，请重试");
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        Log.e("LoginActivity", "Get verification code request failed: " + e.getMessage(), e);
                        toast("获取验证码失败，请检查网络连接");
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
            //是否开启验证码登陆
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
     * 创建账号登录的输入文本管理器
     * @param needVerificationCode 是否需要验证码
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
     * 创建手机登录的输入文本管理器
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
        // 友盟回调
        UmengClient.onActivityResult(this, requestCode, resultCode, data);
    }

    /**
     * {@link UmengLogin.OnLoginListener}
     */

    /**
     * 授权成功的回调
     *
     * @param platform 平台名称
     * @param data     用户资料返回
     */
    @Override
    public void onSucceed(Platform platform, UmengLogin.LoginData data) {
        if (isFinishing() || isDestroyed()) {
            // Glide：You cannot start a load for a destroyed activity
            return;
        }

        // 判断第三方登录的平台
        switch (platform) {
            case QQ:
                break;
            case WECHAT:
                break;
            default:
                break;
        }

        GlideApp.with(this).load(data.getAvatar()).circleCrop().into(mLogoView);

        toast("昵称：" + data.getName() + "\n" + "性别：" + data.getSex() + "\n" + "id：" + data.getId() + "\n" + "token：" + data.getToken());
    }

    /**
     * 授权失败的回调
     *
     * @param platform 平台名称
     * @param t        错误原因
     */
    @Override
    public void onError(Platform platform, Throwable t) {
        toast("第三方登录出错：" + t.getMessage());
    }

    /**
     * {@link KeyboardWatcher.SoftKeyboardStateListener}
     */

    @Override
    public void onSoftKeyboardOpened(int keyboardHeight) {
        // 取消之前的动画
        cancelCurrentAnimator();
        
        // 执行位移动画
        ObjectAnimator bodyAnimator = ObjectAnimator.ofFloat(mBodyLayout, "translationY", 0, -mCommitView.getHeight());
        bodyAnimator.setDuration(mAnimTime);
        bodyAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bodyAnimator.start();

        // 执行缩小动画
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
        // 取消之前的动画
        cancelCurrentAnimator();
        
        // 执行位移动画
        ObjectAnimator bodyAnimator = ObjectAnimator.ofFloat(mBodyLayout, "translationY", mBodyLayout.getTranslationY(), 0f);
        bodyAnimator.setDuration(mAnimTime);
        bodyAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bodyAnimator.start();

        if (mLogoView.getTranslationY() == 0) {
            return;
        }

        // 执行放大动画
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
     * 取消当前正在执行的动画
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
        // 处理输入法完成动作或回车键
        if (actionId == EditorInfo.IME_ACTION_DONE || 
            (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
            
            // 检查当前焦点视图和登录按钮状态
            if (mCommitView.isEnabled()) {
                // 验证输入是否完整
                if (isInputValidForLogin()) {
                    // 隐藏软键盘
                    hideKeyboard(v);
                    // 延迟执行登录，给用户视觉反馈
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
     * 验证输入是否完整，适合自动登录
     */
    private boolean isInputValidForLogin() {
        if (mLongInType == LoginType.mLoginAccount) {
            // 账号登录模式：检查用户名、密码和验证码（如果需要）
            String username = mPhoneView.getText().toString();
            String password = mPasswordView.getText().toString();
            
            if (username.isEmpty() || password.isEmpty()) {
                return false;
            }
            
            // 如果需要验证码
            if (mVierificationCode != null && mVierificationCode.isCode() && 
                mEtLoginVcodeLinear.getVisibility() == View.VISIBLE) {
                String verificationCode = mEtLoginTextCode.getText().toString();
                return !verificationCode.isEmpty();
            }
            
            return true;
        } else if (mLongInType == LoginType.mLoginPhone) {
            // 手机登录模式：检查手机号和验证码
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
                // 指定导航栏背景颜色
                .navigationBarColor(R.color.white);
    }
}