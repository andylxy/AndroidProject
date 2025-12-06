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

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonToken;
import com.hjq.bar.TitleBar;

import run.yigou.gxzy.R;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.common.FragmentSetting;
import run.yigou.gxzy.common.ManagerSetting;
import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.greendao.service.UserInfoService;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.greendao.util.MigrationOrchestrator;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.http.Server.RequestHandler;
import run.yigou.gxzy.http.Server.RequestServer;
import run.yigou.gxzy.http.security.InterceptorHelper;
import run.yigou.gxzy.http.security.SecurityConfig;
import run.yigou.gxzy.http.security.RequestHelper;
import run.yigou.gxzy.manager.ActivityManager;
import run.yigou.gxzy.other.AppConfig;
import run.yigou.gxzy.other.CrashHandler;
import run.yigou.gxzy.other.DebugLoggerTree;
import run.yigou.gxzy.other.MaterialHeader;
import run.yigou.gxzy.other.SmartBallPulseFooter;
import run.yigou.gxzy.other.TitleBarStyle;
import run.yigou.gxzy.other.ToastLogInterceptor;
import run.yigou.gxzy.other.ToastStyle;

import com.hjq.gson.factory.GsonFactory;
import com.hjq.gson.factory.ParseExceptionCallback;
import com.hjq.http.EasyConfig;
import com.hjq.http.EasyLog;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.model.BodyType;
import com.hjq.toast.ToastUtils;
import com.lucas.annotations.Subscribe;
import com.lucas.xbus.XEventBus;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mmkv.MMKV;

import okhttp3.OkHttpClient;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.utils.SerialUtil;
import timber.log.Timber;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : 应用入口
 */
public final class AppApplication extends Application {

    public static AppApplication application;
    /**
     * 是否打开所有页面功能
     */
    //public boolean global_openness = false;
    public boolean global_openness = true;


    //登陆信息
    public boolean isLogin = false;
    private UserInfoService mUserInfoService;
    public UserInfo mUserInfoToken;

    public static AppApplication getApplication() {
        return application;
    }

    public static Context getContext() {
        return application;
    }

    // @Log("启动耗时")
    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        MigrationOrchestrator.ensureUpToDate(this);
        mUserInfoService = DbService.getInstance().mUserInfoService;
        initUserLogin();
        fragmentSetting = ManagerSetting.getFragmentSetting();
        //构造书籍数据/实现本地数据搜索
        ///ThreadUtil.runInBackground(ConvertEntity::tipsSingleDataInit);
        registryByReflect();
        initSdk(this);
        //初始化工具类
        Utils.init(this);
        GsonUtils.setGsonDelegate(new Gson());
    }

    public FragmentSetting fragmentSetting;


    private void initUserLogin() {
        try {
            UserInfo userInfo = mUserInfoService.getLoginUserInfo();
            if (userInfo != null) {
                mUserInfoToken = userInfo; // 直接赋值，避免重复调用
                if (mUserInfoToken.getToken() != null) {
                    // 添加 http 请求 Token
                    isLogin = true;
                    EasyConfig.getInstance().addHeader("Authorization", mUserInfoToken.getToken());
                }
            }
        } catch (Exception e) {
            // 记录异常日志，或者进行其他错误处理
            EasyLog.print("InitUserLogin", "Error initializing user login: " + e.getMessage());
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
    @Override
    public void onTerminate() {
        super.onTerminate();
        // 释放资源，例如关闭数据库、清理缓存等
        TipsSingleData.getInstance().onDestroy();
        // 注销事件总线
        XEventBus.getDefault().unregister(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // 清理所有图片内存缓存
        GlideApp.get(this).onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // 根据手机内存剩余情况清理图片内存缓存
        GlideApp.get(this).onTrimMemory(level);
    }

    /**
     * 初始化一些第三方框架
     */
    public void initSdk(Application application) {
        // 设置标题栏初始化器
        TitleBar.setDefaultStyle(new TitleBarStyle());

        // 设置全局的 Header 构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreator((cx, layout) ->
                new MaterialHeader(application).setColorSchemeColors(ContextCompat.getColor(application, R.color.common_accent_color)));
        // 设置全局的 Footer 构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator((cx, layout) -> new SmartBallPulseFooter(application));
        // 设置全局初始化器
        SmartRefreshLayout.setDefaultRefreshInitializer((cx, layout) -> {
            // 刷新头部是否跟随内容偏移
            layout.setEnableHeaderTranslationContent(true)
                    // 刷新尾部是否跟随内容偏移
                    .setEnableFooterTranslationContent(true)
                    // 加载更多是否跟随内容偏移
                    .setEnableFooterFollowWhenNoMoreData(true)
                    // 内容不满一页时是否可以上拉加载更多
                    .setEnableLoadMoreWhenContentNotFull(false)
                    // 仿苹果越界效果开关
                    .setEnableOverScrollDrag(false);
        });

        // 初始化吐司
        ToastUtils.init(application, new ToastStyle());
        // 设置调试模式
        ToastUtils.setDebugMode(AppConfig.isDebug());
        // 设置 Toast 拦截器
        ToastUtils.setInterceptor(new ToastLogInterceptor());
        // 初始化序列化器
        SerialUtil.getInstance(application);
        // 本地异常捕捉
        CrashHandler.register(application);

        // 友盟统计、登录、分享 SDK
        // UmengClient.init(application, AppConfig.isLogEnable());

        // Bugly 异常捕捉
        // CrashReport.initCrashReport(application, AppConfig.getBuglyId(), AppConfig.isDebug());

        // Activity 栈管理初始化
        ActivityManager.getInstance().init(application);

        // MMKV 初始化
        MMKV.initialize(application);

        // 网络请求框架初始化
        // OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)  // 连接超时时间
                .readTimeout(120, TimeUnit.SECONDS)     // 读取超时时间
                .writeTimeout(120, TimeUnit.SECONDS)    // 写入超时时间
                .build();
        EasyConfig.with(okHttpClient)
                // 是否打印日志
                .setLogEnabled(AppConfig.isLogEnable())
                //.setLogEnabled(true)
                // 设置服务器配置
                .setServer(new RequestServer())
                // 设置请求处理策略
                .setHandler(new RequestHandler(application))
                // 设置请求重试次数
                .setRetryCount(2)
                .setInterceptor((api, params, headers) -> {
                    InterceptorHelper.handleIntercept(api, params, headers, this);
                })
                .into();

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
                if (AppConfig.isLogEnable()) {
                    throw new IllegalArgumentException(message);
                } else {
                    // 上报到 Bugly 错误列表中
                    //  CrashReport.postCatchedException(new IllegalArgumentException(message));
                }
            }
        });

        // 初始化日志打印
        if (AppConfig.isLogEnable()) {
            Timber.plant(new DebugLoggerTree());
        }

        // 注册网络状态变化监听
        ConnectivityManager connectivityManager = ContextCompat.getSystemService(application, ConnectivityManager.class);
        if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(@NonNull Network network) {
                    Activity topActivity = ActivityManager.getInstance().getTopActivity();
                    if (!(topActivity instanceof LifecycleOwner)) {
                        return;
                    }

                    LifecycleOwner lifecycleOwner = ((LifecycleOwner) topActivity);
                    if (lifecycleOwner.getLifecycle().getCurrentState() != Lifecycle.State.RESUMED) {
                        return;
                    }

                    ToastUtils.show(R.string.common_network_error);
                }
            });
        }
    }
}