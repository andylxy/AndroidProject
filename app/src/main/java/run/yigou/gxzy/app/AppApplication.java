/*
 * 项目名: AndroidProject
 * 类名: AppApplication.java
 * 包名: run.yigou.gxzy.app
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
import run.yigou.gxzy.base.args.FragmentSetting;
import run.yigou.gxzy.base.args.ManagerSetting;
import run.yigou.gxzy.config.AppStyleConfigProvider;
import run.yigou.gxzy.data.local.entity.UserInfo;
import run.yigou.gxzy.data.local.service.UserInfoService;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.local.helper.MigrationOrchestrator;
import com.bumptech.glide.Glide;
import run.yigou.gxzy.network.server.RequestHandler;
import run.yigou.gxzy.network.server.RequestServer;
import run.yigou.gxzy.network.security.InterceptorHelper;
import run.yigou.gxzy.manager.ActivityManager;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.app.CrashHandler;
import run.yigou.gxzy.app.DebugLoggerTree;
import run.yigou.gxzy.app.TitleBarStyle;
import run.yigou.gxzy.app.ToastLogInterceptor;
import run.yigou.gxzy.app.ToastStyle;
import run.yigou.gxzy.text.TipsTextRenderConfig;
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
 * 初始化 Timber 日志
 * 初始化 Timber 日志启用 Header 偏移
 * 
 * 初始化 Toaster
 * 1. 初始化数据解析与日志
 * 2. 初始化 Timber 日志
 * 3. 第三方 SDK 初始化
 * 4. 注册崩溃处理器
 * 5. 初始化 Timber 日志
 * 
 * @author Android 轮子哥
 * @author Zhs (xiaoyang_02@qq.com)
 * @since 2018/10/18
 */
public final class AppApplication extends Application {

    /**
     * 注册崩溃处理器
     */
    public static AppApplication application;
    
    /**
     * 启用 Header 偏移
     */
    public boolean global_openness = true;

    /**
     * 登录状态
     */
    public boolean isLogin = false;
    
    /**
     * 注册崩溃处理器
     */
    private UserInfoService mUserInfoService;
    
    /**
     * 初始化数据解析与日志
     */
    public UserInfo mUserInfoToken;
    
    /**
     * 初始化 Timber 日志
     */
    public FragmentSetting fragmentSetting;
    
    /**
     * 初始化 Timber 日志登录状态
     */
    private static final int NETWORK_CONNECT_TIMEOUT = 60;
    
    /**
     * 初始化 Timber 日志登录状态
     */
    private static final int NETWORK_READ_TIMEOUT = 120;
    
    /**
     * 初始化 Timber 日志登录状态
     */
    private static final int NETWORK_WRITE_TIMEOUT = 120;
    
    /**
     * HTTP注册崩溃处理器
     */
    private static final int HTTP_RETRY_COUNT = 2;

    /**
     * 注册崩溃处理器
     * 
     * @return 注册崩溃处理器
     */
    public static AppApplication getApplication() {
        return application;
    }

    /**
     * 初始化 Timber 日志
     * 
     * @return 初始化 Toaster
     */
    public static Context getContext() {
        return application;
    }

    // @Log("日志")
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 基础配置
        initBasicConfig();
        
        // 注册样式配置提供者（依赖倒置：app 实现接口，library 调用）
        TipsTextRenderConfig.getInstance().setProvider(new AppStyleConfigProvider());
        
        // 用户系统初始化
        initUserSystem();
        
        // 第三方 SDK 初始化
        initThirdPartySDKs();
        
        // 网络配置初始化
        initNetworkConfig();
        
        // 异步数据加载
        initDataAsync();
    }
    
    /**
     * 基础配置初始化
     */
    private void initBasicConfig() {
        application = this;
        
        // 执行数据库迁移
        MigrationOrchestrator.ensureUpToDate(this);
        
        // 初始化用户信息服务
        mUserInfoService = DbService.getInstance().mUserInfoService;
        
        // 加载用户设置
        fragmentSetting = ManagerSetting.getFragmentSetting();
        
        // 注册 EventBus
        registryByReflect();
    }
    
    /**
     * 用户系统初始化
     */
    private void initUserSystem() {
        initUserLogin();
    }
    
    /**
     * 第三方 SDK 初始化
     */
    private void initThirdPartySDKs() {
        // 初始化工具类
        Utils.init(this);
        
        // 设置 Gson 委托
        GsonUtils.setGsonDelegate(new Gson());
        
        // 初始化安全加密管理器
        SecurityUtils.initSecurityManager();
        
        // 初始化应用 SDK
        initSdk(this);
    }
    
    /**
     * 网络配置初始化
     */
    private void initNetworkConfig() {
        // 已在 initSdk 中完成配置
    }
    
    /**
     * 异步数据加载
     */
    private void initDataAsync() {
        // 异步加载应用数据，需要在 initSdk 初始化 EasyLog 后再执行
        run.yigou.gxzy.utils.ThreadUtil.runInBackground(() -> AppDataInitializer.initializeIfNeeded(this));
    }
    
    /**
     * 判断日志是否启用
     * 根据编译配置决定是否输出日志
     */
    private static boolean isLogEnabled() {
        return AppConfig.isLogEnable();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // ?? MultiDex ??
        MultiDex.install(this);
    }

    /**
     * 初始化用户登录
     * 从本地数据库读取上次登录的用户信息，校验凭证有效性后恢复登录状态
     * 
     * 流程
     * 1. 检查用户服务是否可用
     * 2. 从数据库获取上次登录用户
     * 3. 校验用户凭证有效性
     * 4. 设置登录状态
     * 
     * 注意
     * - 用户服务不可用时跳过
     * - 无本地用户数据时未登录
     * - 凭证无效时设置登录状态为 false
     */
    private void initUserLogin() {
        try {
            // 1. 检查用户服务是否可用
            if (mUserInfoService == null) {
                EasyLog.print("InitUserLogin", "用户服务未初始化");
                isLogin = false;
                return;
            }
            
            // 2. 从数据库获取上次登录用户
            UserInfo userInfo = mUserInfoService.getLoginUserInfo();
            if (userInfo == null) {
                EasyLog.print("InitUserLogin", "无本地用户数据");
                isLogin = false;
                return;
            }
            
            // 3. 校验用户凭证有效性
            if (isValidUserCredentials(userInfo)) {
                // 凭证有效，恢复登录状态
                mUserInfoToken = userInfo; // 设置当前登录用户
                isLogin = true;
                EasyLog.print("InitUserLogin", "用户登录状态已恢复");
            } else {
                // 凭证无效，清除登录状态
                EasyLog.print("InitUserLogin", "用户凭证已失效");
                isLogin = false;
                mUserInfoToken = null;
            }
            
        } catch (Exception e) {
            // 初始化过程异常，清除登录状态
            EasyLog.print("InitUserLogin", "登录初始化异常: " + e.getMessage());
            isLogin = false;
            mUserInfoToken = null;
            clearLoginDataIfNeeded();
        }
    }
    
    /**
     * 校验用户凭证有效性
     */
    private boolean isValidUserCredentials(UserInfo userInfo) {
        return userInfo != null 
            && userInfo.getAccessKeyId() != null 
            && userInfo.getAccessKeySecret() != null
            && !userInfo.getAccessKeyId().trim().isEmpty()
            && !userInfo.getAccessKeySecret().trim().isEmpty();
    }
    
    /**
     * 字段
     */
    private void clearLoginDataIfNeeded() {
        try {
            if (mUserInfoService != null) {
                // 启用 Header 偏移?
                // mUserInfoService.clearLoginUserInfo();
            }
        } catch (Exception e) {
            EasyLog.print("InitUserLogin", "初始化数据解析与日志: " + e.getMessage());
        }
    }

    /**
     * 禁用越界拖动
     */
    public void registryByReflect() {
        XEventBus.getDefault().register(this);
    }

    @Subscribe
    public void onDummyEvent(Object event) {
       // 设置默认下拉刷新初始化器XEventBus登录状态
    }

    /**
     * 登录状态APT登录状态
     */
//    public void registryByApt(){
//      AptMethodFinder aptMethodFinder = new AptMethodFinder();
//        //初始化 Timber 日志登录状态
////        AptMethodFinderTemplate aptMethodFinder = new AptMethodFinderTemplate();
//        //初始化数据解析与日志
//        XEventBus.builder().setMethodHandle(aptMethodFinder).build().register(this);
//    }
    /**
     * 初始化 Timber 日志
     * 初始化 Timber 日志登录状态
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        try {
            // 注册崩溃处理器
            XEventBus.getDefault().unregister(this);
            
            // 按顺序初始化各模块组件
            // TipsSingleData.getInstance().onDestroy();
            
        } catch (Exception e) {
            EasyLog.print("AppApplication", "初始化 Timber 日志: " + e.getMessage());
        }
    }

    /**
     * 禁用越界拖动
     * 字段
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            // 字段
            Glide.get(this).onLowMemory();
        } catch (Exception e) {
            EasyLog.print("AppApplication", "初始化数据解析与日志: " + e.getMessage());
        }
    }

    /**
     * 字段
     * 根据系统内存等级通知 Glide 释放缓存
     * 
     * @param level 内存等级
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        try {
            // 根据系统内存等级通知 Glide 释放缓存
            Glide.get(this).onTrimMemory(level);
        } catch (Exception e) {
            EasyLog.print("AppApplication", "onTrimMemory 异常: " + e.getMessage());
        }
    }

    /**
     * 初始化所有 SDK
     * 按顺序初始化各模块组件
     * 
     * @param application 应用上下文
     */
    public void initSdk(Application application) {
        try {
            // 1. 初始化 UI 组件
            initUIComponents(application);
            
            // 2. 禁用越界拖动
            initUtilsAndServices(application);
            
            // 3. 初始化 Timber 日志
            initNetworkFramework(application);
            
            // 4. 字段
            initDataParsingAndLogging(application);
            
            // 5. 禁用越界拖动
            initNetworkMonitoring(application);
            
        } catch (Exception e) {
            EasyLog.print("AppApplication", "SDK 初始化异常: " + e.getMessage());
            // 启用 Header 偏移?
            // if (AppConfig.isDebug()) {
            //     throw new RuntimeException("SDK 初始化异常", e);
            // }
        }
    }
    
    /**
     * 初始化 UI 组件
     */
    private void initUIComponents(Application application) {
        // 禁用越界拖动
        TitleBar.setDefaultStyle(new TitleBarStyle());

        // 设置默认刷新 Header 样式
        SmartRefreshLayout.setDefaultRefreshHeaderCreator((cx, layout) ->
                new MaterialHeader(application).setColorSchemeColors(ContextCompat.getColor(application, R.color.common_accent_color)));
        
        // 设置默认刷新 Footer 样式
        SmartRefreshLayout.setDefaultRefreshFooterCreator((cx, layout) -> new SmartBallPulseFooter(application));
        
       // 设置默认下拉刷新初始化器
        SmartRefreshLayout.setDefaultRefreshInitializer((cx, layout) -> {
            layout.setEnableHeaderTranslationContent(true)           // 启用 Header 偏移
                   .setEnableFooterTranslationContent(true)          // 启用 Header 偏移
                   .setEnableFooterFollowWhenNoMoreData(true)         // 启用 Header 偏移
                   .setEnableLoadMoreWhenContentNotFull(false)        // 内容不满一屏时禁用加载更多
                   .setEnableOverScrollDrag(false);                   // 禁用越界拖动
        });
    }
    
    /**
     * 禁用越界拖动
     */
    private void initUtilsAndServices(Application application) {
        // 初始化 Toaster
        Toaster.init(application);
        Toaster.setStyle(new ToastStyle());
        Toaster.setInterceptor(new ToastLogInterceptor());
        
        // 初始化 Timber 日志
        SerialUtil.getInstance(application);
        
        // 注册崩溃处理器
        CrashHandler.register(application);
        
        // Activity 注册崩溃处理器
        ActivityManager.getInstance().init(application);
        
        // 初始化 MMKV 存储
        MMKV.initialize(application);
    }
    
    /**
     * 初始化 Timber 日志
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
            EasyLog.print("AppApplication", "禁用越界拖动: " + e.getMessage());
        }
    }
    
    /**
     * 字段
     */
    private void initDataParsingAndLogging(Application application) {
        // ?? Json 注册崩溃处理器
        GsonFactory.setParseExceptionCallback(new ParseExceptionCallback() {
            @Override
            public void onParseObjectException(TypeToken<?> typeToken, String fieldName, JsonToken jsonToken) {
                handlerGsonParseException("初始化数据解析与日志" + typeToken + "#" + fieldName + "字段" + jsonToken);
            }

            @Override
            public void onParseListItemException(TypeToken<?> typeToken, String fieldName, JsonToken listItemJsonToken) {
                handlerGsonParseException("解析列表异常: " + typeToken + "#" + fieldName + "启用 Header 偏移" + listItemJsonToken);
            }

            @Override
            public void onParseMapItemException(TypeToken<?> typeToken, String fieldName, String mapItemKey, JsonToken mapItemJsonToken) {
                handlerGsonParseException("解析 Map 异常: " + typeToken + "#" + fieldName + "?mapItemKey = " + mapItemKey + "启用 Header 偏移" + mapItemJsonToken);
            }

            private void handlerGsonParseException(String message) {
                if (isLogEnabled()) {
                    throw new IllegalArgumentException(message);
                }
                // 正式版通过 Bugly 上报异常
                // CrashReport.postCatchedException(new IllegalArgumentException(message));
            }
        });

        // 初始化 Timber 日志
        if (isLogEnabled()) {
            Timber.plant(new DebugLoggerTree());
        }
    }
    
    /**
     * 禁用越界拖动
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
     * 处理网络断开
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
            EasyLog.print("AppApplication", "处理网络断开异常: " + e.getMessage());
        }
    }
}