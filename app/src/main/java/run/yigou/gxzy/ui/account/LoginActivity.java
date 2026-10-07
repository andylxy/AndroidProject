/*
 * 项目名: AndroidProject
 * 类名: LoginActivity.java
 * 包名: run.yigou.gxzy.ui.account
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 19:07:17
 * 上次修改时间: 2023年07月05日 17:23:50
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
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.security.SecurityUtils;
import com.hjq.base.action.SingleClick;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.base.constant.LoginType;
import run.yigou.gxzy.data.local.entity.UserInfo;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.remote.api.LoginApi;
import run.yigou.gxzy.data.remote.api.VierCode;
import run.yigou.gxzy.data.remote.api.GetCodeApi;

import run.yigou.gxzy.network.glide.GlideApp;
import run.yigou.gxzy.manager.InputTextManager;
import run.yigou.gxzy.manager.account.AccountDataManager;
import run.yigou.gxzy.manager.Callback;
import com.hjq.base.KeyboardWatcher;
import run.yigou.gxzy.ui.main.HomeFragment;
import run.yigou.gxzy.ui.main.HomeActivity;
import run.yigou.gxzy.utils.Base64ConverBitmapHelper;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;
import run.yigou.gxzy.wxapi.WXEntryActivity;

import com.hjq.http.config.IRequestApi;
import com.hjq.umeng.Platform;
import com.hjq.umeng.UmengClient;
import com.hjq.umeng.UmengLogin;
import com.hjq.widget.view.CountdownView;
import com.hjq.widget.view.SubmitButton;


/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 登录页面
 */
public final class LoginActivity extends AppActivity implements UmengLogin.OnLoginListener, KeyboardWatcher.SoftKeyboardStateListener, TextView.OnEditorActionListener {

    private static final String INTENT_KEY_IN_PHONE = "phone";
    private static final String INTENT_KEY_IN_PASSWORD = "password";

    /** 账户数据管理器 */
    private final AccountDataManager mAccountDataManager = AccountDataManager.getInstance();


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
     * Logo 动画
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
     * 忘记密码
     */
    private View mForgetView;
    /**
     * 提交按钮
     */
    private SubmitButton mCommitView;

    /**
     * 第三方登录区域
     */
    private View mOtherView;
    /**
     * QQ 登录
     */
    private View mQQView;
    /**
     * 微信登录
     */
    private View mWeChatView;

    /**
     * 账号密码切换
     */
    private View mIvLoginAccount;
    /**
     * 手机号切换
     */
    private View mIvLoginPhone;
    /**
     * 短信验证码区域
     */
    private View mLlLoginSmsCodeLinear;
    /**
     * 图形验证码区域
     */
    private View mEtLoginVcodeLinear;

    /**
     * logo 缩放比例
     */
    private final float mLogoScale = 0.8f;
    /**
     * 动画时长
     */
    private final int mAnimTime = 300;
    /**
     * 短信倒计时
     */
    private CountdownView mCountdownView;
    /**
     * 图形验证码图片
     */
    private ImageView mEtLoginVcode;
    /**
     * 图形验证码输入框
     */
    private EditText mEtLoginTextCode;
    /**
     * 验证码数据
     */
    private VierCode.Bean mVierificationCode;
    /**
     * 当前登录类型
     */
    private int mLongInType = LoginType.mLoginAccount;
    /**
     * 键盘监听器
     */
    private KeyboardWatcher mKeyboardWatcher;
    /**
     * 当前正在执行的动画
     */
    private AnimatorSet mCurrentAnimatorSet;

    @Override
    protected int getLayoutId() {
        return R.layout.login_activity;
    }

    @Override
    protected void initView() {
        // 初始化控件
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
        
        // 检查登录状态
        if (inLoginOrNoLogin()) return;
        
        // 设置点击事件
        setOnClickListener(mForgetView, mCommitView, mQQView, mWeChatView, mIvLoginAccount, mIvLoginPhone, mCountdownView, mEtLoginVcode);
        // 设置编辑器动作监听
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
        
        // 默认显示账号密码登录
        mIvLoginAccount.setVisibility(View.GONE);
        mLlLoginSmsCodeLinear.setVisibility(View.GONE);
        
        // 未安装 QQ 时隐藏 QQ 登录
        if (!UmengClient.isAppInstalled(this, Platform.QQ)) {
            mQQView.setVisibility(View.GONE);
        }

        // 未安装微信时隐藏微信登录
        if (!UmengClient.isAppInstalled(this, Platform.WECHAT)) {
            mWeChatView.setVisibility(View.GONE);
        }

        // 所有第三方登录都隐藏时隐藏第三方区域
        if (mQQView.getVisibility() == View.GONE && mWeChatView.getVisibility() == View.GONE) {
            mOtherView.setVisibility(View.GONE);
        }

        // 填充传递过来的手机号和密码
        mPhoneView.setText(getString(INTENT_KEY_IN_PHONE));
        mPasswordView.setText(getString(INTENT_KEY_IN_PASSWORD));
    }

    /**
     * 检查是否已登录
     *
     * @return true 已登录, false 未登录
     */
    private boolean inLoginOrNoLogin() {
        //已登录，直接跳转主页
        if (AppApplication.application.isLogin) {
            homeActivityStart();
            return true;
        }
        return false;
    }

    @Override
    public void onRightClick(View view) {
        // 跳转注册页面
        RegisterActivity.start(this, mPhoneView.getText().toString(), mPasswordView.getText().toString(), (phone, password) -> {
            // 注册完成后回填手机号和密码
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
     * 忘记密码
     */
    private void handleForgetPassword() {
        startActivity(PasswordForgetActivity.class);
    }

    /**
     * 获取短信验证码
     */
    private void handleGetVerificationCode() {
        String phone = mPhoneView.getText().toString();
        if (phone == null || phone.length() != 11) {
            mPhoneView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.shake_anim));
            toast(R.string.common_phone_input_error);
            return;
        }

        // 隐藏键盘
        hideKeyboard(getCurrentFocus());

        // 临时注释：TODO 待网络接口就绪后恢复
        /*
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
                        toast("???????????????");
                    }
                });
        */
        
        // 使用 AccountDataManager 封装的网络请求
        mAccountDataManager.sendLoginSmsCode(this,
                phone,
                new Callback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        toast(R.string.common_code_send_hint);
                        mCountdownView.start();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("LoginActivity", "Get SMS code failed: " + e.getMessage(), e);
                        toast("发送验证码失败：" + e.getMessage());
                    }
                });
    }

    /**
     * 切换为账号密码登录
     */
    private void handleSwitchToAccountLogin() {
        setViewShow(mIvLoginAccount);
        mLongInType = LoginType.mLoginAccount;
    }

    /**
     * 切换为短信验证码登录
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
        // 隐藏键盘
        hideKeyboard(getCurrentFocus());

        // 验证码尚未加载完成时阻止提交（异步竞态条件防护）
        if (mVierificationCode == null) {
            toast("验证码加载中，请稍候...");
            return;
        }

        // 构建请求参数
        LoginApi requestApi = buildLoginRequest();
        if (requestApi != null) {
            login(requestApi);
        }
    }

    /**
     * 构建登录请求参数
     */
    private LoginApi buildLoginRequest() {
        if (mLongInType == LoginType.mLoginAccount) {
            String username = mPhoneView.getText().toString();
            String password = mPasswordView.getText().toString();
            
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                toast("账号或密码不能为空");
                return null;
            }
            
            // microfeed 适配：指向 microfeed 时发送**明文**口令（better-auth 校验明文，
            // 依赖 HTTPS 保护）；netcore（预发布/正式）仍走 SM2 加密。
            String passwd = password;
            if (!AppConfig.isMicrofeedAuth()) {
                passwd = SecurityUtils.doSm2Encrypt(password);
                if (passwd == null || passwd.isEmpty()) {
                    toast("密码加密失败");
                    return null;
                }
            }

            LoginApi requestApi = new LoginApi()
                    .setUserName(username)
                    .setPassword(passwd);

            // 服务端要求图形验证码时，校验验证码输入并附加参数
            if (mVierificationCode != null && mVierificationCode.isCode()) {
                String verificationCode = mEtLoginTextCode.getText().toString();
                if (verificationCode == null || verificationCode.isEmpty()) {
                    toast("验证码不能为空");
                    return null;
                }
                requestApi.setVerificationCode(verificationCode)
                        .setUUID(mVierificationCode.getUuid());
            } else if (mVierificationCode != null && mVierificationCode.getUuid() != null) {
                requestApi.setUUID(mVierificationCode.getUuid());
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
                toast("短信验证码不能为空");
                return null;
            }
            
            return new LoginApi()
                    .setUserName(phone)
                    .setPassword(smsCode);
        }
        return null;
    }

    /**
     * 第三方登录
     */
    private void handleThirdPartyLogin(View view) {
        toast("请先配置 QQ/微信 AppID 和 Secret 后才能使用");
        Platform platform;
        if (view == mQQView) {
            platform = Platform.QQ;
        } else if (view == mWeChatView) {
            platform = Platform.WECHAT;
            toast("请先配置 " + WXEntryActivity.class.getSimpleName() + " 相关参数");
        } else {
            throw new IllegalStateException("are you ok?");
        }
        UmengClient.login(this, platform, this);
    }

    private void login(IRequestApi requestApi) {
        // 临时注释：TODO 待网络接口就绪后恢复
        /*
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
                            toast("登录返回数据为空");
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
                                        // ????????
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
        */
            
        // 使用 AccountDataManager 封装的网络请求
        mAccountDataManager.login(this,
                requestApi,
                new Callback<LoginApi.Bean>() {
                    @Override
                    public void onSuccess(LoginApi.Bean data) {
                        if (data == null) {
                            Log.e("LoginActivity", "Login failed: data is empty or null");
                            toast("登录返回数据为空");
                            mCommitView.showError(3000);
                            return;
                        }
                            
                        try {
                            // 保存登录信息
                            AppApplication.getApplication().mUserInfoToken = data;
                            // 更新数据库
                            // 使用表单输入的账号作为 userLoginAccount（而非 accessKeyId）
                            String userLoginAccount = mPhoneView.getText().toString().trim();
                            if (userLoginAccount != null && !userLoginAccount.isEmpty()) {
                                // 将正确账号设置到实体中，确保持久化时保存
                                data.setUserLoginAccount(userLoginAccount);
                                AppApplication.application.isLogin = true;
                                // 读账号 + 分支写入整体交给 DB 串行后台线程（见 persistLoginUserInfo 的注释）
                                persistLoginUserInfo(data, userLoginAccount);
                            }
                                
                            homeActivityStart();
                        } catch (Exception e) {
                            Log.e("LoginActivity", "Login success handler failed: " + e.getMessage(), e);
                            toast("登录处理失败");
                            mCommitView.showError(3000);
                        }
                    }
    
                    @Override
                    public void onError(Exception e) {
                        Log.e("LoginActivity", "Login request failed: " + e.getMessage(), e);
                        postDelayed(() -> {
                            mCommitView.showError(3000);
                        }, 1000);
                        toast("登录失败：" + e.getMessage());
                    }
                });
    }

    private void homeActivityStart() {
        // 清空所有 Activity，跳转主页
        // 重置 Fragment
        HomeActivity.start(getContext(), HomeFragment.class);
        finish();
    }

    /**
     * 把登录信息写入本地库的 USER_INFO 表。
     *
     * <p>为什么整体放到后台：这条路径包含 {@code deleteAll()} 整表删除 + 新增，
     * 放在主线程正是历史上 ANR 的成因类别。改用 {@code DbService.runInBackgroundSerial}
     * 后执行顺序与原先一致（串行单线程），只是换了线程。
     *
     * <p>为什么任务内必须自己 try/catch：串行入口内部用 {@code Executor.execute} 提交，
     * 异常不会进 Future，只会杀掉 worker 线程——自己不记日志就等于什么都看不到。
     * 失败仍按原行为给一次吐司，所以要把提示切回主线程。
     *
     * <p>不改判定结果：到底走"首次登录"还是"已有账号"分支，与原先完全相同。
     *
     * @param data             登录返回的实体（账号字段已在外层设置好）
     * @param userLoginAccount 登录账号
     */
    private void persistLoginUserInfo(final UserInfo data, final String userLoginAccount) {
        DbService.getInstance().runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                try {
                    UserInfo userInfo = DbService.getInstance().mUserInfoService
                            .findUserInfoByLoginAccount(userLoginAccount);
                    if (userInfo == null) {
                        // 首次登录：删除所有旧数据后新增
                        DbService.getInstance().mUserInfoService.deleteAll();
                        DbService.getInstance().mUserInfoService.addEntity(data);
                    } else {
                        // 已有该账号：用服务端返回的最新凭证**更新已有的那一行**。
                        //
                        // 这里原来是 deleteEntity(data)，等于把刚拿到的登录信息从库里删掉：
                        // AppApplication.initUserLogin() 在下次启动时读不到行就判 isLogin=false，
                        // 结果是每次冷启动都要重新登录（见 greendao 加固票 10 的取证记录）。
                        //
                        // 也不能直接 updateEntity(data)：UserInfoService.addEntity 会给新增的行
                        // 生成一个随机 UUID 主键，而服务端返回的实体不带这个主键，
                        // update(data) 一条都匹配不上，是静默无更新。
                        // ⇒ 先沿用本地已有行的主键，再整体更新。
                        data.setId(userInfo.getId());
                        DbService.getInstance().mUserInfoService.updateEntity(data);
                    }
                } catch (Throwable t) {
                    EasyLog.print(t);
                    ThreadUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast("数据库操作失败");
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 避免 Activity 销毁后键盘监听器持有引用
        mKeyboardWatcher = null;
    }

    private void getLoginVcode() {
        // 临时注释：TODO 待网络接口就绪后恢复
        /*
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
                                toast("获取验证码失败");
                            }
                        } catch (Exception e) {
                            Log.e("LoginActivity", "Get verification code failed: " + e.getMessage(), e);
                            toast("获取验证码失败");
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);
                        Log.e("LoginActivity", "Get verification code request failed: " + e.getMessage(), e);
                        toast("获取验证码请求失败");
                    }
                });
        */
        
        // 使用 AccountDataManager 封装的网络请求
        mAccountDataManager.getLoginVcode(this,
                new Callback<VierCode.Bean>() {
                    @Override
                    public void onSuccess(VierCode.Bean data) {
                        try {
                            if (data != null) {
                                if (data.isCode()) {
                                    String img = data.getImg();
                                    if (!StringHelper.isEmpty(img)) {
                                        Bitmap bitmap = Base64ConverBitmapHelper.getBase64ToImage(img);
                                        setLoginVcode(bitmap);
                                    }
                                }
                                mVierificationCode = data;
                                setViewShow(mIvLoginAccount);
                            } else {
                                Log.e("LoginActivity", "Get verification code failed: data is null");
                                toast("获取验证码失败");
                            }
                        } catch (Exception e) {
                            Log.e("LoginActivity", "Get verification code failed: " + e.getMessage(), e);
                            toast("获取验证码失败：" + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("LoginActivity", "Get verification code request failed: " + e.getMessage(), e);
                        toast("获取验证码失败：" + e.getMessage());
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
            // 切换回账号密码时清空验证码输入，避免 UUID 刷新后旧验证码残留
            mEtLoginTextCode.setText("");
            // 根据验证码状态显示或隐藏验证码区域
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
     * 创建输入框文本管理器
     * @param needVerificationCode 是否需要验证码输入框
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