/*
 * 项目名: AndroidProject
 * 类名: AccountDataManager.java
 * 包名: run.yigou.gxzy.manager.account
 * 作者 : AI Assistant
 * 当前修改时间 : 2026年06月25日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.manager.account;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.listener.HttpCallback;
import com.hjq.http.listener.OnHttpListener;

import run.yigou.gxzy.data.remote.api.GetCodeApi;
import run.yigou.gxzy.data.remote.api.LoginApi;
import run.yigou.gxzy.data.remote.api.PasswordApi;
import run.yigou.gxzy.data.remote.api.PhoneApi;
import run.yigou.gxzy.data.remote.api.RegisterApi;
import run.yigou.gxzy.data.remote.api.UpdateImageApi;
import run.yigou.gxzy.data.remote.api.VerifyCodeApi;
import run.yigou.gxzy.data.remote.api.VierCode;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.log.EasyLog;

import java.io.File;

/**
 * 账户数据管理器
 * 
 * <p>职责：
 * <ul>
 *   <li>完整封装账户模块所有网络请求</li>
 *   <li>提供统一的回调接口</li>
 *   <li>生命周期绑定（LifecycleOwner）</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * AccountDataManager.getInstance().login(this, account, password, vcode,
 *     new AccountDataManager.Callback<LoginApi.Bean>() {
 *         @Override
 *         public void onSuccess(LoginApi.Bean data) {
 *             // 处理成功
 *         }
 *         
 *         @Override
 *         public void onError(Exception e) {
 *             // 处理失败
 *         }
 *     });
 * </pre>
 */
public class AccountDataManager {
    
    private static final String TAG = "AccountDataManager";
    
    // 单例
    private static volatile AccountDataManager instance;
    
    /**
     * 私有构造方法
     */
    private AccountDataManager() {
    }
    
    /**
     * 获取单例实例
     */
    public static AccountDataManager getInstance() {
        if (instance == null) {
            synchronized (AccountDataManager.class) {
                if (instance == null) {
                    instance = new AccountDataManager();
                }
            }
        }
        return instance;
    }
    
    // ========== 登录相关 ==========
    
    /**
     * 发送短信验证码（登录页）
     */
    public void sendLoginSmsCode(LifecycleOwner owner, String phone, Callback<Void> callback) {
        EasyHttp.post(owner)
                .api(new GetCodeApi().setPhone(phone))
                .request(new HttpCallback<HttpData<Void>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<Void> data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "发送登录短信验证码失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 登录（账号密码 / 短信登录）
     */
    public void login(LifecycleOwner owner, IRequestApi requestApi, Callback<LoginApi.Bean> callback) {
        EasyHttp.post(owner)
                .api(requestApi)
                .request(new HttpCallback<HttpData<LoginApi.Bean>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<LoginApi.Bean> data) {
                        if (data != null && data.getData() != null) {
                            callback.onSuccess(data.getData());
                        } else {
                            callback.onError(new Exception("登录返回数据为空"));
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "登录请求失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 获取登录验证码图片
     */
    public void getLoginVcode(LifecycleOwner owner, Callback<VierCode.Bean> callback) {
        EasyHttp.get(owner)
                .api(new VierCode())
                .request(new HttpCallback<HttpData<VierCode.Bean>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<VierCode.Bean> data) {
                        if (data != null && data.getData() != null) {
                            callback.onSuccess(data.getData());
                        } else {
                            callback.onError(new Exception("验证码返回数据为空"));
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "获取验证码图片失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    // ========== 注册相关 ==========
    
    /**
     * 发送短信验证码（注册页）
     */
    public void sendRegisterSmsCode(LifecycleOwner owner, String phone, Callback<Void> callback) {
        EasyHttp.post(owner)
                .api(new GetCodeApi().setPhone(phone))
                .request(new HttpCallback<HttpData<Void>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<Void> data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "发送注册短信验证码失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 注册
     */
    public void register(LifecycleOwner owner, String phone, String code, String password, 
                         Callback<RegisterApi.Bean> callback) {
        EasyHttp.post(owner)
                .api(new RegisterApi()
                        .setPhone(phone)
                        .setCode(code)
                        .setPassword(password))
                .request(new HttpCallback<HttpData<RegisterApi.Bean>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<RegisterApi.Bean> data) {
                        if (data != null && data.getData() != null) {
                            callback.onSuccess(data.getData());
                        } else {
                            callback.onError(new Exception("注册返回数据为空"));
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "注册请求失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    // ========== 密码管理相关 ==========
    
    /**
     * 发送短信验证码（忘记密码页）
     */
    public void sendForgetPasswordSmsCode(LifecycleOwner owner, String phone, Callback<Void> callback) {
        EasyHttp.post(owner)
                .api(new GetCodeApi().setPhone(phone))
                .request(new HttpCallback<HttpData<Void>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<Void> data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "发送忘记密码短信验证码失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 验证短信验证码（忘记密码页）
     */
    public void verifySmsCodeForForgetPassword(LifecycleOwner owner, String phone, String code, 
                                                Callback<Void> callback) {
        EasyHttp.post(owner)
                .api(new VerifyCodeApi()
                        .setPhone(phone)
                        .setCode(code))
                .request(new HttpCallback<HttpData<Void>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<Void> data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "验证短信验证码失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 重置密码
     */
    public void resetPassword(LifecycleOwner owner, String phone, String code, String password, 
                              Callback<Void> callback) {
        EasyHttp.post(owner)
                .api(new PasswordApi()
                        .setPhone(phone)
                        .setCode(code)
                        .setPassword(password))
                .request(new HttpCallback<HttpData<Void>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<Void> data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "重置密码失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    // ========== 手机号重置相关 ==========
    
    /**
     * 发送短信验证码（重置手机号页）
     */
    public void sendResetPhoneSmsCode(LifecycleOwner owner, String phone, Callback<Void> callback) {
        EasyHttp.post(owner)
                .api(new GetCodeApi().setPhone(phone))
                .request(new HttpCallback<HttpData<Void>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<Void> data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "发送重置手机号短信验证码失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 重置手机号
     */
    public void resetPhone(LifecycleOwner owner, String preCode, String phone, String code, 
                           Callback<Void> callback) {
        EasyHttp.post(owner)
                .api(new PhoneApi()
                        .setPreCode(preCode)
                        .setPhone(phone)
                        .setCode(code))
                .request(new HttpCallback<HttpData<Void>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<Void> data) {
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "重置手机号失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    // ========== 个人信息相关 ==========
    
    /**
     * 更新头像
     */
    public void updateAvatar(LifecycleOwner owner, File imageFile, Callback<String> callback) {
        EasyHttp.post(owner)
                .api(new UpdateImageApi().setImage(imageFile))
                .request(new HttpCallback<HttpData<String>>((OnHttpListener) owner) {
                    @Override
                    public void onSucceed(HttpData<String> data) {
                        if (data != null && data.getData() != null) {
                            callback.onSuccess(data.getData());
                        } else {
                            callback.onError(new Exception("更新头像返回数据为空"));
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "更新头像失败: " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    // ========== 回调接口 ==========
    
    /**
     * 统一回调接口
     * 
     * @param <T> 数据类型
     */
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }
}
