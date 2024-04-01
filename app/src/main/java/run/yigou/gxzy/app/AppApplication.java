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
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.hjq.bar.TitleBar;
import run.yigou.gxzy.R;
import run.yigou.gxzy.aop.Log;
import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.greendao.service.UserInfoService;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.glide.GlideApp;
import run.yigou.gxzy.http.model.RequestHandler;
import run.yigou.gxzy.http.model.RequestServer;
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
import com.hjq.http.EasyConfig;
import com.hjq.toast.ToastUtils;
import com.hjq.umeng.UmengClient;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mmkv.MMKV;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : 应用入口
 */
public final class AppApplication extends Application {

    public static AppApplication application;
    /**
     * 主线程执行
     *
     * @param runnable
     */
    private static Handler handler = new Handler();

    private ScheduledExecutorService mFixedThreadPool;

    public static void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public void newThread(Runnable runnable) {
        try {
            mFixedThreadPool.schedule(runnable,1, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            mFixedThreadPool = Executors.newScheduledThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), 3));//初始化线程池
            mFixedThreadPool.execute(runnable);
        }
    }

    public void shutdownThreadPool(){
        mFixedThreadPool.shutdownNow();
    }
    //登陆信息

    private UserInfoService mUserInfoService;
    public  UserInfo mUserInfoToken ;
    public static AppApplication getApplication() {
        return application;
    }
    public static Context getmContext() {
        return application;
    }
    @Log("启动耗时")
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 TitleBar 默认样式
       // TitleBar.setDefaultStyle(new ITitleBarStyle());
        application=this;
        mUserInfoService = DbService.getInstance().mUserInfoService;
        initSdk(this);
        initUserLogin();
    }

    private void initUserLogin() {
        UserInfo userInfo =mUserInfoService.getLoginUserInfo();
        if (userInfo!=null){
            mUserInfoToken = mUserInfoService.getLoginUserInfo();
            //添加http请求Token
            if (mUserInfoToken !=null)
                EasyConfig.getInstance().addHeader("Authorization", mUserInfoToken.getToken());

        }
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
    public static void initSdk(Application application) {
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

        // 本地异常捕捉
       // CrashHandler.register(application);

        // 友盟统计、登录、分享 SDK
       // UmengClient.init(application, AppConfig.isLogEnable());

        // Bugly 异常捕捉
      //  CrashReport.initCrashReport(application, AppConfig.getBuglyId(), AppConfig.isDebug());

        // Activity 栈管理初始化
        ActivityManager.getInstance().init(application);

        // MMKV 初始化
        MMKV.initialize(application);

        // 网络请求框架初始化
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();

        EasyConfig.with(okHttpClient)
                // 是否打印日志
                .setLogEnabled(AppConfig.isLogEnable())
                // 设置服务器配置
                .setServer(new RequestServer())
                // 设置请求处理策略
                .setHandler(new RequestHandler(application))
                // 设置请求重试次数
                .setRetryCount(1)
                .setInterceptor((api, params, headers) -> {
                    // 添加全局请求头
                   // headers.put("Authorization", mUserInfoToken.getToken());
                    headers.put("app", "2");
                   // headers.put("ClientId", DeviceIdentifier.getAndroidID(this));
                    headers.put("versionName", AppConfig.getVersionName());
                    headers.put("versionCode", String.valueOf(AppConfig.getVersionCode()));
                    headers.put("Content-Type", "application/json;charset=UTF-8");
                    headers.put("Accept", "application/json, text/plain, */*");
                    // 添加全局请求参数
                    // params.put("6666666", "6666666");
                })
                .into();

        // 设置 Json 解析容错监听
        GsonFactory.setJsonCallback((typeToken, fieldName, jsonToken) -> {
            // 上报到 Bugly 错误列表
//            CrashReport.postCatchedException(new IllegalArgumentException(
//                    "类型解析异常：" + typeToken + "#" + fieldName + "，后台返回的类型为：" + jsonToken));
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