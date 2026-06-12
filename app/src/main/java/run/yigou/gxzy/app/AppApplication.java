/*
 * ???: AndroidProject
 * ??: AppApplication.java
 * ??: com.intellij.copyright.JavaCopyrightVariablesProvider$1@a05c99d,qualifiedClassName
 * ?? : Zhs (xiaoyang_02@qq.com)
 * ?????? : 2023?07?05? 16:53:30
 * ??????: 2023?07?05? 16:52:24
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
 * author : Android ???
 * github : https://github.com/getActivity/AndroidProject
 * time   : 2018/10/18
 * desc   : ????
 */
/**
 * ???????
 * ???????????????????
 * 
 * ?????
 * 1. ????????
 * 2. ???????
 * 3. ???SDK???
 * 4. ??????
 * 5. ???????
 * 
 * @author Android ???
 * @author Zhs (xiaoyang_02@qq.com)
 * @since 2018/10/18
 */
public final class AppApplication extends Application {

    /**
     * ??????
     */
    public static AppApplication application;
    
    /**
     * ????????????
     */
    public boolean global_openness = true;

    /**
     * ????
     */
    public boolean isLogin = false;
    
    /**
     * ??????
     */
    private UserInfoService mUserInfoService;
    
    /**
     * ????????
     */
    public UserInfo mUserInfoToken;
    
    /**
     * ???????
     */
    public FragmentSetting fragmentSetting;
    
    /**
     * ???????????
     */
    private static final int NETWORK_CONNECT_TIMEOUT = 60;
    
    /**
     * ???????????
     */
    private static final int NETWORK_READ_TIMEOUT = 120;
    
    /**
     * ???????????
     */
    private static final int NETWORK_WRITE_TIMEOUT = 120;
    
    /**
     * HTTP??????
     */
    private static final int HTTP_RETRY_COUNT = 2;

    /**
     * ??????
     * 
     * @return ??????
     */
    public static AppApplication getApplication() {
        return application;
    }

    /**
     * ???????
     * 
     * @return ?????
     */
    public static Context getContext() {
        return application;
    }

    // @Log("????")
    @Override
    public void onCreate() {
        super.onCreate();
        
        // ?????
        initBasicConfig();
        
        // ???????
        initUserSystem();
        
        // ???SDK???
        initThirdPartySDKs();
        
        // ???????
        initNetworkConfig();
        
        // ???????
        initDataAsync();
    }
    
    /**
     * ???????
     */
    private void initBasicConfig() {
        application = this;
        
        // ???????
        MigrationOrchestrator.ensureUpToDate(this);
        
        // ????????
        mUserInfoService = DbService.getInstance().mUserInfoService;
        
        // ????
        fragmentSetting = ManagerSetting.getFragmentSetting();
        
        // ??????
        registryByReflect();
    }
    
    /**
     * ???????
     */
    private void initUserSystem() {
        initUserLogin();
    }
    
    /**
     * ???SDK???
     */
    private void initThirdPartySDKs() {
        // ??????
        Utils.init(this);
        
        // Gson??
        GsonUtils.setGsonDelegate(new Gson());
        
        // ????????
        SecurityUtils.initSecurityManager();
        
        // ???SDK
        initSdk(this);
    }
    
    /**
     * ???????
     */
    private void initNetworkConfig() {
        // ?????initSdk???
    }
    
    /**
     * ???????
     */
    private void initDataAsync() {
        // ????????????? initSdk ??????? EasyLog?
        run.yigou.gxzy.utils.ThreadUtil.runInBackground(() -> AppDataInitializer.initializeIfNeeded(this));
    }
    
    /**
     * ????????
     * ???????????????
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
     * ?????????
     * ??????????????????HTTP??Token
     * 
     * ?????
     * 1. ????????????
     * 2. ??????????
     * 3. ?????????
     * 4. ??????
     * 
     * ?????
     * - ??????
     * - ???????????
     * - ???????false
     */
    private void initUserLogin() {
        try {
            // 1. ????????????
            if (mUserInfoService == null) {
                EasyLog.print("InitUserLogin", "??????????");
                isLogin = false;
                return;
            }
            
            // 2. ??????????
            UserInfo userInfo = mUserInfoService.getLoginUserInfo();
            if (userInfo == null) {
                EasyLog.print("InitUserLogin", "???????");
                isLogin = false;
                return;
            }
            
            // 3. ?????????
            if (isValidUserCredentials(userInfo)) {
                // ?????????????
                mUserInfoToken = userInfo; // ???????????
                isLogin = true;
                EasyLog.print("InitUserLogin", "???????????");
            } else {
                // ??????????????
                EasyLog.print("InitUserLogin", "??????????????");
                isLogin = false;
                mUserInfoToken = null;
            }
            
        } catch (Exception e) {
            // ???????????????????
            EasyLog.print("InitUserLogin", "???????????: " + e.getMessage());
            isLogin = false;
            mUserInfoToken = null;
            clearLoginDataIfNeeded();
        }
    }
    
    /**
     * ????????????
     */
    private boolean isValidUserCredentials(UserInfo userInfo) {
        return userInfo != null 
            && userInfo.getAccessKeyId() != null 
            && userInfo.getAccessKeySecret() != null
            && !userInfo.getAccessKeyId().trim().isEmpty()
            && !userInfo.getAccessKeySecret().trim().isEmpty();
    }
    
    /**
     * ??????????
     */
    private void clearLoginDataIfNeeded() {
        try {
            if (mUserInfoService != null) {
                // ?????????????
                // mUserInfoService.clearLoginUserInfo();
            }
        } catch (Exception e) {
            EasyLog.print("InitUserLogin", "????????: " + e.getMessage());
        }
    }

    /**
     * ?????????
     */
    public void registryByReflect() {
        XEventBus.getDefault().register(this);
    }

    @Subscribe
    public void onDummyEvent(Object event) {
        // ????????XEventBus????
    }

    /**
     * ????APT????
     */
//    public void registryByApt(){
//      AptMethodFinder aptMethodFinder = new AptMethodFinder();
//        //???????????
////        AptMethodFinderTemplate aptMethodFinder = new AptMethodFinderTemplate();
//        //????????
//        XEventBus.builder().setMethodHandle(aptMethodFinder).build().register(this);
//    }
    /**
     * ???????
     * ???????????
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        try {
            // ??????
            XEventBus.getDefault().unregister(this);
            
            // ???????????????
            // TipsSingleData.getInstance().onDestroy();
            
        } catch (Exception e) {
            EasyLog.print("AppApplication", "???????: " + e.getMessage());
        }
    }

    /**
     * ?????????
     * ??????????
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            // ??????????
            Glide.get(this).onLowMemory();
        } catch (Exception e) {
            EasyLog.print("AppApplication", "????????: " + e.getMessage());
        }
    }

    /**
     * ??????????
     * ??????????????????
     * 
     * @param level ????
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        try {
            // ??????????????????
            Glide.get(this).onTrimMemory(level);
        } catch (Exception e) {
            EasyLog.print("AppApplication", "??????: " + e.getMessage());
        }
    }

    /**
     * ??????SDK??
     * ???????????????
     * 
     * @param application ?????
     */
    public void initSdk(Application application) {
        try {
            // 1. ?????UI??
            initUIComponents(application);
            
            // 2. ?????????
            initUtilsAndServices(application);
            
            // 3. ???????
            initNetworkFramework(application);
            
            // 4. ??????????
            initDataParsingAndLogging(application);
            
            // 5. ?????????
            initNetworkMonitoring(application);
            
        } catch (Exception e) {
            EasyLog.print("AppApplication", "???SDK?????: " + e.getMessage());
            // ?????????????
            // if (AppConfig.isDebug()) {
            //     throw new RuntimeException("SDK?????", e);
            // }
        }
    }
    
    /**
     * ???UI??
     */
    private void initUIComponents(Application application) {
        // ?????????
        TitleBar.setDefaultStyle(new TitleBarStyle());

        // ????? Header ???
        SmartRefreshLayout.setDefaultRefreshHeaderCreator((cx, layout) ->
                new MaterialHeader(application).setColorSchemeColors(ContextCompat.getColor(application, R.color.common_accent_color)));
        
        // ????? Footer ???
        SmartRefreshLayout.setDefaultRefreshFooterCreator((cx, layout) -> new SmartBallPulseFooter(application));
        
        // ????????
        SmartRefreshLayout.setDefaultRefreshInitializer((cx, layout) -> {
            layout.setEnableHeaderTranslationContent(true)           // ????????????
                   .setEnableFooterTranslationContent(true)          // ????????????
                   .setEnableFooterFollowWhenNoMoreData(true)         // ????????????
                   .setEnableLoadMoreWhenContentNotFull(false)        // ?????????????????
                   .setEnableOverScrollDrag(false);                   // ?????????
        });
    }
    
    /**
     * ?????????
     */
    private void initUtilsAndServices(Application application) {
        // ?????
        Toaster.init(application);
        Toaster.setStyle(new ToastStyle());
        Toaster.setInterceptor(new ToastLogInterceptor());
        
        // ???????
        SerialUtil.getInstance(application);
        
        // ??????
        CrashHandler.register(application);
        
        // Activity ??????
        ActivityManager.getInstance().init(application);
        
        // MMKV ???
        MMKV.initialize(application);
    }
    
    /**
     * ???????
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
            EasyLog.print("AppApplication", "?????????: " + e.getMessage());
        }
    }
    
    /**
     * ??????????
     */
    private void initDataParsingAndLogging(Application application) {
        // ?? Json ??????
        GsonFactory.setParseExceptionCallback(new ParseExceptionCallback() {
            @Override
            public void onParseObjectException(TypeToken<?> typeToken, String fieldName, JsonToken jsonToken) {
                handlerGsonParseException("????????" + typeToken + "#" + fieldName + "??????????" + jsonToken);
            }

            @Override
            public void onParseListItemException(TypeToken<?> typeToken, String fieldName, JsonToken listItemJsonToken) {
                handlerGsonParseException("?? List ???" + typeToken + "#" + fieldName + "????????????" + listItemJsonToken);
            }

            @Override
            public void onParseMapItemException(TypeToken<?> typeToken, String fieldName, String mapItemKey, JsonToken mapItemJsonToken) {
                handlerGsonParseException("?? Map ???" + typeToken + "#" + fieldName + "?mapItemKey = " + mapItemKey + "????????????" + mapItemJsonToken);
            }

            private void handlerGsonParseException(String message) {
                if (isLogEnabled()) {
                    throw new IllegalArgumentException(message);
                }
                // ??????? Bugly ?????
                // CrashReport.postCatchedException(new IllegalArgumentException(message));
            }
        });

        // ???????
        if (isLogEnabled()) {
            Timber.plant(new DebugLoggerTree());
        }
    }
    
    /**
     * ?????????
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
     * ????????
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
            EasyLog.print("AppApplication", "??????????: " + e.getMessage());
        }
    }
}