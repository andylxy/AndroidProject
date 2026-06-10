/*
 * 项目名: AndroidProject
 * 类名: AppApplication.java
 * 包名: com.intellij.copyright.JavaCopyrightVariablesProvider$1@a05c99d,qualifiedClassName
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2023年07月05日 16:53:30
 * 上次修改时间: 2023年07月05日 16:52:24
 * Copyright (c) 2023 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.multidex.MultiDex;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonToken;
import com.hjq.bar.TitleBar;

import run.yigou.gxzy.R;
import run.yigou.gxzy.crypto.SecurityUtils;
import run.yigou.gxzy.common.FragmentSetting;
import run.yigou.gxzy.common.ManagerSetting;
import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.greendao.service.UserInfoService;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.greendao.util.MigrationOrchestrator;
import com.bumptech.glide.Glide;
import run.yigou.gxzy.http.server.RequestHandler;
import run.yigou.gxzy.http.server.RequestServer;
import run.yigou.gxzy.http.security.InterceptorHelper;
import run.yigou.gxzy.manager.ActivityManager;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.app.CrashHandler;
import run.yigou.gxzy.app.DebugLoggerTree;
import run.yigou.gxzy.app.TitleBarStyle;
import run.yigou.gxzy.app.ToastLogInterceptor;
import run.yigou.gxzy.app.ToastStyle;
import run.yigou.gxzy.widget.MaterialHeader;
import run.yigou.gxzy.widget.SmartBallPulseFooter;

import com.hjq.gson.factory.GsonFactory;
import com.hjq.gson.factory.ParseExceptionCallback;
import com.hjq.http.request.HttpRequest;
import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestInterceptor;
import com.hjq.http.model.HttpParams;
import com.hjq.http.model.HttpHeaders;
import run.yigou.gxzy.log.EasyLog;
import com.hjq.toast.Toaster;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;
import com.lucas.xbus.XEventBus;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mmkv.MMKV;

import okhttp3.OkHttpClient;

import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.SerialUtil;
import timber.log.Timber;

import java.util.concurrent.TimeUnit;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 应用入口
 */
/**
 * 应用程序入口类
 * 负责整个应用的生命周期管理和全局初始化
 * 
 * 主要功能：
 * 1. 应用生命周期管理
 * 2. 全局配置初始化
 * 3. 第三方SDK初始化
 * 4. 用户系统管理
 * 5. 网络配置和监控
 * 
 * @author Android 轮子哥
 * @author Zhs (xiaoyang_02@qq.com)
 * @since 2018/10/18
 */
public final class AppApplication extends Application {

    /**
     * 应用单例实例
     */
    public static AppApplication application;
    
    /**
     * 是否打开全局搜索页面功能
     */
    public boolean global_openness = true;

    /**
     * 登录状态
     */
    public boolean isLogin = false;
    
    /**
     * 用户信息服务
     */
    private UserInfoService mUserInfoService;
    
    /**
     * 当前登录用户信息
     */
    public UserInfo mUserInfoToken;
    
    /**
     * 应用配置管理器
     */
    public FragmentSetting fragmentSetting;
    
    /**
     * 网络连接超时时间（秒）
     */
    private static final int NETWORK_CONNECT_TIMEOUT = 60;
    
    /**
     * 网络读取超时时间（秒）
     */
    private static final int NETWORK_READ_TIMEOUT = 120;
    
    /**
     * 网络写入超时时间（秒）
     */
    private static final int NETWORK_WRITE_TIMEOUT = 120;
    
    /**
     * HTTP请求重试次数
     */
    private static final int HTTP_RETRY_COUNT = 2;

    /**
     * 获取应用实例
     * 
     * @return 应用单例实例
     */
    public static AppApplication getApplication() {
        return application;
    }

    /**
     * 获取应用上下文
     * 
     * @return 应用上下文
     */
    public static Context getContext() {
        return application;
    }

    // @Log("启动耗时")
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 基础初始化
        initBasicConfig();
        
        // 用户系统初始化
        initUserSystem();
        
        // 第三方SDK初始化
        initThirdPartySDKs();
        
        // 网络配置初始化
        initNetworkConfig();
        
        // 异步数据初始化
        initDataAsync();
    }
    
    /**
     * 基础配置初始化
     */
    private void initBasicConfig() {
        application = this;
        
        // 数据库迁移检查
        MigrationOrchestrator.ensureUpToDate(this);
        
        // 获取用户信息服务
        mUserInfoService = DbService.getInstance().mUserInfoService;
        
        // 应用配置
        fragmentSetting = ManagerSetting.getFragmentSetting();
        
        // 事件总线注册
        registryByReflect();
    }
    
    /**
     * 用户系统初始化
     */
    private void initUserSystem() {
        initUserLogin();
    }
    
    /**
     * 第三方SDK初始化
     */
    private void initThirdPartySDKs() {
        // 初始化工具类
        Utils.init(this);
        
        // Gson配置
        GsonUtils.setGsonDelegate(new Gson());
        
        // 安全管理器初始化
        SecurityUtils.initSecurityManager();
        
        // 初始化SDK
        initSdk(this);
    }
    
    /**
     * 网络配置初始化
     */
    private void initNetworkConfig() {
        // 网络配置在initSdk中处理
    }
    
    /**
     * 异步数据初始化
     */
    private void initDataAsync() {
        // 异步初始化应用数据（必须在 initSdk 之后，因为依赖 EasyLog）
        run.yigou.gxzy.utils.ThreadUtil.runInBackground(() -> AppDataInitializer.initializeIfNeeded(this));
    }
    
    /**
     * 检查是否启用日志
     * 统一日志开关检查，避免重复调用
     */
    private static boolean isLogEnabled() {
        return AppConfig.isLogEnable();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 启用 MultiDex 支持
        MultiDex.install(this);
    }

    /**
     * 初始化用户登录状态
     * 从数据库获取当前登录用户信息，并设置HTTP请求Token
     * 
     * 处理流程：
     * 1. 检查用户信息服务是否可用
     * 2. 获取当前登录用户信息
     * 3. 验证用户凭证完整性
     * 4. 设置登录状态
     * 
     * 异常处理：
     * - 记录异常日志
     * - 清除可能损坏的登录数据
     * - 设置登录状态为false
     */
    private void initUserLogin() {
        try {
            // 1. 检查用户信息服务是否可用
            if (mUserInfoService == null) {
                EasyLog.print("InitUserLogin", "用户信息服务未初始化");
                isLogin = false;
                return;
            }
            
            // 2. 获取当前登录用户信息
            UserInfo userInfo = mUserInfoService.getLoginUserInfo();
            if (userInfo == null) {
                EasyLog.print("InitUserLogin", "当前无登录用户");
                isLogin = false;
                return;
            }
            
            // 3. 验证用户凭证完整性
            if (isValidUserCredentials(userInfo)) {
                // 用户凭证完整，设置登录状态
                mUserInfoToken = userInfo; // 直接赋值，避免重复调用
                isLogin = true;
                EasyLog.print("InitUserLogin", "用户登录状态初始化成功");
            } else {
                // 用户凭证不完整，需要重新登录
                EasyLog.print("InitUserLogin", "用户凭证不完整，需要重新登录");
                isLogin = false;
                mUserInfoToken = null;
            }
            
        } catch (Exception e) {
            // 异常处理：记录日志，清除数据，重置状态
            EasyLog.print("InitUserLogin", "初始化用户登录状态失败: " + e.getMessage());
            isLogin = false;
            mUserInfoToken = null;
            clearLoginDataIfNeeded();
        }
    }
    
    /**
     * 验证用户凭证是否完整有效
     */
    private boolean isValidUserCredentials(UserInfo userInfo) {
        return userInfo != null 
            && userInfo.getAccessKeyId() != null 
            && userInfo.getAccessKeySecret() != null
            && !userInfo.getAccessKeyId().trim().isEmpty()
            && !userInfo.getAccessKeySecret().trim().isEmpty();
    }
    
    /**
     * 在需要时清除登录数据
     */
    private void clearLoginDataIfNeeded() {
        try {
            if (mUserInfoService != null) {
                // 可以选择清除损坏的登录数据
                // mUserInfoService.clearLoginUserInfo();
            }
        } catch (Exception e) {
            EasyLog.print("InitUserLogin", "清除登录数据失败: " + e.getMessage());
        }
    }

    /**
     * 方法一：以反射调用
     */
    public void registryByReflect() {
        XEventBus.getDefault().register(this);
    }

    @Subscribe
    public void onDummyEvent(Object event) {
        // 空实现，仅为满足XEventBus注册要求
    }

    /**
     * 方法二：APT方式调用
     */
//    public void registryByApt(){
//      AptMethodFinder aptMethodFinder = new AptMethodFinder();
//        //注解处理器代码的模板类
////        AptMethodFinderTemplate aptMethodFinder = new AptMethodFinderTemplate();
//        //注解处理调用方式
//        XEventBus.builder().setMethodHandle(aptMethodFinder).build().register(this);
//    }
    /**
     * 应用终止时调用
     * 释放资源，注销事件总线
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        try {
            // 注销事件总线
            XEventBus.getDefault().unregister(this);
            
            // 可以在这里添加其他资源释放逻辑
            // TipsSingleData.getInstance().onDestroy();
            
        } catch (Exception e) {
            EasyLog.print("AppApplication", "应用终止时出错: " + e.getMessage());
        }
    }

    /**
     * 系统内存不足时调用
     * 清理所有图片内存缓存
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            // 清理所有图片内存缓存
            Glide.get(this).onLowMemory();
        } catch (Exception e) {
            EasyLog.print("AppApplication", "内存不足处理失败: " + e.getMessage());
        }
    }

    /**
     * 根据内存级别调整缓存
     * 根据手机内存剩余情况清理图片内存缓存
     * 
     * @param level 内存级别
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        try {
            // 根据手机内存剩余情况清理图片内存缓存
            Glide.get(this).onTrimMemory(level);
        } catch (Exception e) {
            EasyLog.print("AppApplication", "内存调整失败: " + e.getMessage());
        }
    }

    /**
     * 初始化第三方SDK框架
     * 按照依赖关系顺序初始化各个组件
     * 
     * @param application 应用上下文
     */
    public void initSdk(Application application) {
        try {
            // 1. 初始化基础UI组件
            initUIComponents(application);
            
            // 2. 初始化工具类和服务
            initUtilsAndServices(application);
            
            // 3. 初始化网络框架
            initNetworkFramework(application);
            
            // 4. 初始化数据解析和日志
            initDataParsingAndLogging(application);
            
            // 5. 初始化网络状态监听
            initNetworkMonitoring(application);
            
        } catch (Exception e) {
            EasyLog.print("AppApplication", "第三方SDK初始化失败: " + e.getMessage());
            // 可以选择记录到崩溃报告系统
            // if (AppConfig.isDebug()) {
            //     throw new RuntimeException("SDK初始化失败", e);
            // }
        }
    }
    
    /**
     * 初始化UI组件
     */
    private void initUIComponents(Application application) {
        // 设置标题栏初始化器
        TitleBar.setDefaultStyle(new TitleBarStyle());

        // 设置全局的 Header 构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreator((cx, layout) ->
                new MaterialHeader(application).setColorSchemeColors(ContextCompat.getColor(application, R.color.common_accent_color)));
        
        // 设置全局的 Footer 构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator((cx, layout) -> new SmartBallPulseFooter(application));
        
        // 设置全局初始化器
        SmartRefreshLayout.setDefaultRefreshInitializer((cx, layout) -> {
            layout.setEnableHeaderTranslationContent(true)           // 刷新头部是否跟随内容偏移
                   .setEnableFooterTranslationContent(true)          // 刷新尾部是否跟随内容偏移
                   .setEnableFooterFollowWhenNoMoreData(true)         // 加载更多是否跟随内容偏移
                   .setEnableLoadMoreWhenContentNotFull(false)        // 内容不满一页时是否可以上拉加载更多
                   .setEnableOverScrollDrag(false);                   // 仿苹果越界效果开关
        });
    }
    
    /**
     * 初始化工具类和服务
     */
    private void initUtilsAndServices(Application application) {
        // 初始化吐司
        Toaster.init(application);
        Toaster.setStyle(new ToastStyle());
        Toaster.setInterceptor(new ToastLogInterceptor());
        
        // 初始化序列化器
        SerialUtil.getInstance(application);
        
        // 本地异常捕捉
        CrashHandler.register(application);
        
        // Activity 栈管理初始化
        ActivityManager.getInstance().init(application);
        
        // MMKV 初始化
        MMKV.initialize(application);
    }
    
    /**
     * 初始化网络框架
     */
    private void initNetworkFramework(Application application) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(NETWORK_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(NETWORK_READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(NETWORK_WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();
                    
            EasyConfig.with(okHttpClient)
                    .setLogEnabled(isLogEnabled())
                    .setServer(new RequestServer())
                    .setHandler(new RequestHandler(application))
                    .setRetryCount(HTTP_RETRY_COUNT)
                    .setInterceptor(new IRequestInterceptor() {
                        @Override
                        public void interceptArguments(HttpRequest<?> httpRequest, HttpParams params, HttpHeaders headers) {
                            InterceptorHelper.handleIntercept(httpRequest.getRequestApi(), params, headers, AppApplication.this);
                        }
                    })
                    .into();
        } catch (Exception e) {
            EasyLog.print("AppApplication", "网络框架初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 初始化数据解析和日志
     */
    private void initDataParsingAndLogging(Application application) {
        // 设置 Json 解析容错监听
        GsonFactory.setParseExceptionCallback(new ParseExceptionCallback() {
            @Override
            public void onParseObjectException(TypeToken<?> typeToken, String fieldName, JsonToken jsonToken) {
                handlerGsonParseException("解析对象析异常：" + typeToken + "#" + fieldName + "，后台返回的类型为：" + jsonToken);
            }

            @Override
            public void onParseListItemException(TypeToken<?> typeToken, String fieldName, JsonToken listItemJsonToken) {
                handlerGsonParseException("解析 List 异常：" + typeToken + "#" + fieldName + "，后台返回的条目类型为：" + listItemJsonToken);
            }

            @Override
            public void onParseMapItemException(TypeToken<?> typeToken, String fieldName, String mapItemKey, JsonToken mapItemJsonToken) {
                handlerGsonParseException("解析 Map 异常：" + typeToken + "#" + fieldName + "，mapItemKey = " + mapItemKey + "，后台返回的条目类型为：" + mapItemJsonToken);
            }

            private void handlerGsonParseException(String message) {
                if (isLogEnabled()) {
                    throw new IllegalArgumentException(message);
                }
                // 可以选择上报到 Bugly 错误列表中
                // CrashReport.postCatchedException(new IllegalArgumentException(message));
            }
        });

        // 初始化日志打印
        if (isLogEnabled()) {
            Timber.plant(new DebugLoggerTree());
        }
    }
    
    /**
     * 初始化网络状态监听
     */
    private void initNetworkMonitoring(Application application) {
        ConnectivityManager connectivityManager = ContextCompat.getSystemService(application, ConnectivityManager.class);
        if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(@NonNull Network network) {
                    handleNetworkLost();
                }
            });
        }
    }
    
    /**
     * 处理网络连接丢失
     */
    private void handleNetworkLost() {
        try {
            Activity topActivity = ActivityManager.getInstance().getTopActivity();
            if (!(topActivity instanceof LifecycleOwner)) {
                return;
            }

            LifecycleOwner lifecycleOwner = ((LifecycleOwner) topActivity);
            if (lifecycleOwner.getLifecycle().getCurrentState() != Lifecycle.State.RESUMED) {
                return;
            }

            Toaster.show(R.string.common_network_error);
        } catch (Exception e) {
            EasyLog.print("AppApplication", "网络连接丢失处理失败: " + e.getMessage());
        }
    }
}