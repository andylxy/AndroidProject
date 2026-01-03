/*
 * 项目名: AndroidProject
 * 类名: LoginActivity.java
 * 包名: com.intellij.copyright.JavaCopyrightVariablesProvider$1@a563d04,qualifiedClassName
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 19:07:17
 * 上次修改时间: 2023年07月05日 17:23:50
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.activity;

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

import run.yigou.gxzy.EventBus.LoginEventNotification;
import run.yigou.gxzy.R;
import run.yigou.gxzy.Security.SecurityUtils;
import run.yigou.gxzy.aop.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.common.LoginType;
import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.LoginApi;
import run.yigou.gxzy.http.api.VierCode;
import run.yigou.gxzy.http.entitymodel.WebResponseContent;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.manager.InputTextManager;
import run.yigou.gxzy.other.KeyboardWatcher;
import run.yigou.gxzy.ui.fragment.HomeFragment;
import run.yigou.gxzy.utils.Base64ConverBitmapHelper;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.wxapi.WXEntryActivity;

import com.hjq.http.EasyConfig;
import com.hjq.http.EasyHttp;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.listener.OnHttpListener;
import com.hjq.umeng.Platform;
import com.hjq.umeng.UmengClient;
import com.hjq.umeng.UmengLogin;
import com.hjq.widget.view.CountdownView;
import com.hjq.widget.view.SubmitButton;
import com.lucas.xbus.XEventBus;

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

    private ImageView mLogoView;

    private ViewGroup mBodyLayout;
    private EditText mPhoneView;
    private EditText mEt_login_sms_code;
    private EditText mPasswordView;

    private View mForgetView;
    private SubmitButton mCommitView;

    private View mOtherView;
    private View mQQView;
    private View mWeChatView;

    private View mIvLoginAccount;
    private View mIvLoginPhone;
    private View mLlLoginSmsCodeLinear;
    private View mEtLoginVcodeLinear;

    /**
     * logo 缩放比例
     */
    private final float mLogoScale = 0.8f;
    /**
     * 动画时间
     */
    private final int mAnimTime = 300;
    private CountdownView mCountdownView;
    private ImageView mEtLoginVcode;
    private EditText mEtLoginTextCode;
    /**
     * 验证码
     */
    private VierCode.Bean mVierificationCode;
    /**
     * 默认为 账号登陆
     */
    private int mLongInType = LoginType.mLoginAccount;

    @Override
    protected int getLayoutId() {
        return R.layout.login_activity;
    }

    @Override
    protected void initView() {
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
        mEt_login_sms_code = findViewById(R.id.et_login_sms_code);
        mEtLoginVcode = findViewById(R.id.et_login_vcode);
        mEtLoginTextCode = findViewById(R.id.et_login_text_code);
        if (inLoginOrNoLogin()) return;
        setOnClickListener(mForgetView, mCommitView, mQQView, mWeChatView, mIvLoginAccount, mIvLoginPhone, mCountdownView, mEtLoginVcode);
        mPasswordView.setOnEditorActionListener(this);
        //setViewShow(mIvLoginAccount);
        getLoginVcode();
    }


    @Override
    protected void initData() {
        // if (inLoginOrNoLogin()) return;
        postDelayed(() -> {
            KeyboardWatcher.with(LoginActivity.this).setListener(LoginActivity.this);
        }, 500);
        // 默认使用账号密码登陆
        mIvLoginAccount.setVisibility(View.GONE);
        mLlLoginSmsCodeLinear.setVisibility(View.GONE);
        // mPasswordView.setHint("请输入您的姓名");
        // 判断用户当前有没有安装 QQ
        if (!UmengClient.isAppInstalled(this, Platform.QQ)) {
            mQQView.setVisibility(View.GONE);
        }

        // 判断用户当前有没有安装微信
        if (!UmengClient.isAppInstalled(this, Platform.WECHAT)) {
            mWeChatView.setVisibility(View.GONE);
        }

        // 如果这两个都没有安装就隐藏提示
        if (mQQView.getVisibility() == View.GONE && mWeChatView.getVisibility() == View.GONE) {
            mOtherView.setVisibility(View.GONE);
        }

        // 自动填充手机号和密码
        mPhoneView.setText(getString(INTENT_KEY_IN_PHONE));
        mPasswordView.setText(getString(INTENT_KEY_IN_PASSWORD));

        //    XEventBus.getDefault().register(LoginActivity.this);
    }

    /**
     * 检查是否已经登陆
     *
     * @return true 已登陆,false 未登陆
     */
    private boolean inLoginOrNoLogin() {
        //如果已登陆,侧跳转到我的
        if (AppApplication.application.isLogin) {
            HomeActivityStart();
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
            startActivity(PasswordForgetActivity.class);
            return;
        }
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

            // 隐藏软键盘
            hideKeyboard(getCurrentFocus());

//            // 获取验证码
//            EasyHttp.post(this)
//                    .api(new GetCodeApi()
//                            .setPhone(mPhoneView.getText().toString()))
//                    .request(new OnHttpListener<HttpData<LoginBean>>() {
//
//                        @Override
//                        public void onSucceed(HttpData<Void> data) {
//                            toast(R.string.common_code_send_hint);
//                            mCountdownView.start();
//                        }
//                    });
        }
        if (view == mIvLoginAccount) {
            setViewShow(mIvLoginAccount);
            mLongInType = LoginType.mLoginAccount;
            return;
        }
        if (view == mIvLoginPhone) {
            setViewShow(mIvLoginPhone);
            mLongInType = LoginType.mLoginPhone;
            return;
        }
        if (view == mEtLoginVcode) {
            getLoginVcode();
            return;
        }
        if (view == mCommitView) {
            // 判断账号，手机都可以登陆，注释掉以下手机号判断
            // 判断手机号格式是否正确
//            if (mPhoneView.getText().toString().length() != 11) {
//                mPhoneView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
//                mCommitView.showError(3000);
//                toast(R.string.common_phone_input_error);
//                return;
//            }

            // 隐藏软键盘
            hideKeyboard(getCurrentFocus());

            //登陆逻辑处理
            LoginApi requestApi = null;
            if (mLongInType == LoginType.mLoginAccount) {

                //String pwd = mPasswordView.getText().toString();
               // String user = mPhoneView.getText().toString();
                if (mPhoneView.getText().toString() == null || mPasswordView.getText().toString() == null) {
                    toast("请输入账号和密码");
                }
                String passwd = SecurityUtils.doSm2Encrypt(mPasswordView.getText().toString());
                requestApi = new LoginApi()
                        .setUserName(mPhoneView.getText().toString())
                        .setPassword(passwd);

                if (mVierificationCode.isCode()) {
                    requestApi.setVerificationCode(mEtLoginTextCode.getText().toString())
                            .setUUID(mVierificationCode.getUuid());
                } else {
                    requestApi.setVerificationCode("E6Y4D6")
                            .setUUID(mVierificationCode.getUuid());
                }
            } else if (mLongInType == LoginType.mLoginPhone) {
                requestApi = new LoginApi()
                        .setUserName(mPhoneView.getText().toString())
                        .setPassword(mPasswordView.getText().toString());
            }

            if (requestApi != null)
                login(requestApi);
            return;
        }

        if (view == mQQView || view == mWeChatView) {
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
    }

    private void login(IRequestApi requestApi) {
        EasyHttp.post(this)
                .api(requestApi)
                .request(new OnHttpListener<HttpData<LoginApi.Bean>>() {



                    @Override
                    public void onHttpSuccess(HttpData<LoginApi.Bean> data) {
                        if (data == null || data.getData() == null || data.getData() == null) {
                            // System.out.println("data is empty or null");
                            toast("登陆失败，请检查账号密码是否正确");
                            return;
                        }
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
                                // System.out.println("onSucceed" + "Database operation failed: " + e.getMessage());
                                toast("登陆信息,保存失败");
                            }
                        }
                        //  XEventBus.getDefault().post(new LoginEventNotification(true));
                        HomeActivityStart();

                    }


                    @Override
                    public void onHttpFail(Throwable e) {
                        // super.onFail(e);
                        postDelayed(() -> {
                            mCommitView.showError(3000);
                        }, 1000);
                    }
                });
    }

    private void HomeActivityStart() {
        // 登录成功后 , 跳转到首页
        // 跳转到首页
        HomeActivity.start(getContext(), HomeFragment.class);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //  XEventBus.getDefault().unregister(LoginActivity.this);
    }

    private void getLoginVcode() {

        EasyHttp.get(this)
                .api(new VierCode())
                .request(new OnHttpListener<HttpData<VierCode.Bean>>() {
                    @Override
                    public void onHttpSuccess(HttpData<VierCode.Bean> data) {
                        if (data.getData() != null && data.getData().isCode()) {
                            String img = data.getData().getImg();
                            if (!StringHelper.isEmpty(img)) {
                                Bitmap bitmap = Base64ConverBitmapHelper.getBase64ToImage(img);
                                setLoginVcode(bitmap);
                            }
                        }
                        mVierificationCode = data.getData();
                        setViewShow(mIvLoginAccount);
                    }

                    @Override
                    public void onHttpFail(Throwable e) {
                        toast(e.getMessage());
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
            mEt_login_sms_code.setText("");
            //是否开启验证码登陆
            if (mVierificationCode.isCode()) {
                mEtLoginVcodeLinear.setVisibility(View.VISIBLE);
                InputTextManager.with(this)
                        .addView(mPhoneView)
                        .addView(mPasswordView)
                        .addView(mEtLoginTextCode)
                        .setMain(mCommitView).build();
            } else {
                mEtLoginVcodeLinear.setVisibility(View.GONE);
                InputTextManager.with(this)
                        .addView(mPhoneView)
                        .addView(mPasswordView)
                        .setMain(mCommitView).build();
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
            InputTextManager.with(this)
                    .addView(mPhoneView)
                    .addView(mEt_login_sms_code)
                    .setMain(mCommitView).build();
        }

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
        // 执行位移动画
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mBodyLayout, "translationY", 0, -mCommitView.getHeight());
        objectAnimator.setDuration(mAnimTime);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.start();

        // 执行缩小动画
        mLogoView.setPivotX(mLogoView.getWidth() / 2f);
        mLogoView.setPivotY(mLogoView.getHeight());
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mLogoView, "scaleX", 1f, mLogoScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mLogoView, "scaleY", 1f, mLogoScale);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(mLogoView, "translationY", 0f, -mCommitView.getHeight());
        animatorSet.play(translationY).with(scaleX).with(scaleY);
        animatorSet.setDuration(mAnimTime);
        animatorSet.start();
    }

    @Override
    public void onSoftKeyboardClosed() {
        // 执行位移动画
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mBodyLayout, "translationY", mBodyLayout.getTranslationY(), 0f);
        objectAnimator.setDuration(mAnimTime);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.start();

        if (mLogoView.getTranslationY() == 0) {
            return;
        }

        // 执行放大动画
        mLogoView.setPivotX(mLogoView.getWidth() / 2f);
        mLogoView.setPivotY(mLogoView.getHeight());
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mLogoView, "scaleX", mLogoScale, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mLogoView, "scaleY", mLogoScale, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(mLogoView, "translationY", mLogoView.getTranslationY(), 0f);
        animatorSet.play(translationY).with(scaleX).with(scaleY);
        animatorSet.setDuration(mAnimTime);
        animatorSet.start();
    }

    /**
     * {@link TextView.OnEditorActionListener}
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE && mCommitView.isEnabled()) {
            // 模拟点击登录按钮
            onClick(mCommitView);
            return true;
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