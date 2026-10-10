package run.yigou.gxzy.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieAnimationView;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.permissions.XXPermissions;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.manager.announcement.AnnouncementManager;
import run.yigou.gxzy.manager.privacy.PrivacyAgreement;
import run.yigou.gxzy.manager.search.SearchPermissionManager;
import run.yigou.gxzy.manager.mingci.MingCiPermissionManager;
import run.yigou.gxzy.manager.update.UpdateManager;
import run.yigou.gxzy.ui.home.HomeActivity;

import com.hjq.widget.view.SlantedTextView;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : 闪屏界面
 */
public final class SplashActivity extends AppActivity {

    /** 权限引导页请求码 */
    private static final int REQUEST_CODE_PERMISSION_GUIDE = 1001;

    private LottieAnimationView mLottieView;
    private SlantedTextView mDebugView;

    /** 是否已经完成权限检查 */
    private boolean mPermissionChecked = false;

    public static void start(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.splash_activity;
    }

    @Override
    protected void initView() {
        mLottieView = findViewById(R.id.lav_splash_lottie);
        mDebugView = findViewById(R.id.iv_splash_debug);
    }

    @Override
    protected void initData() {
        mDebugView.setText(AppConfig.getBuildType().toUpperCase());
        if (AppConfig.isDebug()) {
            mDebugView.setVisibility(View.VISIBLE);
        } else {
            mDebugView.setVisibility(View.INVISIBLE);
        }

        // 权限已授予，开始播放动画
        if (mPermissionChecked) {
            startLottieAnimation();
        }
    }

    /**
     * 开始播放 Lottie 动画
     */
    private void startLottieAnimation() {
        mLottieView.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLottieView.removeAnimatorListener(this);
                HomeActivity.start(getContext());
                finish();
            }
        });
        mLottieView.playAnimation();
    }

    @NonNull
    @Override
    protected ImmersionBar createStatusBarConfig() {
        return super.createStatusBarConfig()
                // 隐藏状态栏和导航栏
                .hideBar(BarHide.FLAG_HIDE_BAR);
    }

    @Override
    public void onBackPressed() {
        //禁用返回键
        //super.onBackPressed();
    }

    @Override
    protected void initActivity() {
        // 问题及方案：https://www.cnblogs.com/net168/p/5722752.html
        // 如果当前 Activity 不是任务栈中的第一个 Activity
        if (!isTaskRoot()) {
            Intent intent = getIntent();
            // 如果当前 Activity 是通过桌面图标启动进入的
            if (intent != null && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                    && Intent.ACTION_MAIN.equals(intent.getAction())) {
                // 对当前 Activity 执行销毁操作，避免重复实例化入口
                finish();
                return;
            }
        }

        // 检查存储权限
        checkStoragePermission();
    }

    /**
     * 检查协议与权限，未全部通过则拉起引导页。
     *
     * <p><b>这是 App 的启动硬门，两个条件缺一不可</b>：</p>
     * <ol>
     *   <li>隐私协议已同意（{@link PrivacyAgreement#isAgreed()}）；</li>
     *   <li>{@link StartupPermissions#REQUIRED} 全部授予。</li>
     * </ol>
     *
     * <p>两者都满足才 {@link #onConsentAndPermissionGranted()}（注册会发请求的
     * 回调 + 进入首页）；任一不满足则拉起引导页，由它决定继续还是结束进程。这正是需求
     * 「用户没有同意之前不发网络请求」的落点 —— 注册动作在这里，而不在
     * {@code AppApplication.onCreate}。</p>
     */
    private void checkStoragePermission() {
        if (PrivacyAgreement.isAgreed() && XXPermissions.isGranted(this, StartupPermissions.REQUIRED)) {
            onConsentAndPermissionGranted();
            return;
        }
        // 任一不满足 → 拉起引导页。用户在其中「同意」会触发系统权限申请；
        // 拒绝、部分授予或点「退出」都由它结束进程。
        PermissionGuideActivity.start(this, REQUEST_CODE_PERMISSION_GUIDE);
    }

    /**
     * 协议与权限都通过：注册需要网络请求的回调，然后进入首页。
     *
     * <p><b>注册为什么放在这里</b>：这三个 Manager 靠「应用级前台回调」触发，
     * 而 {@code Application.onCreate} 早于任何 Activity —— 放在那里等于进程一启动
     * 回调就挂上，用户还没同意协议也可能发出请求。挪到这里就成了硬门的最后一环。</p>
     *
     * <p><b>每次启动都要执行</b>：注册是<b>进程级</b>的（各 Manager 内部有静态
     * {@code LaunchOnceGate} 闸门、状态落 MMKV），进程被杀后重来必须重新注册，
     * 否则「从后台被回收再回来」将永远不再拉取。</p>
     *
     * <p><b>顺序要求</b>：必须在 {@code MMKV.initialize} 与 {@code ActivityManager.init}
     * 之后（它们在 {@code AppApplication.onCreate} 里已完成），且版本检查先注册 ——
     * 两者都靠前台事件触发，注册顺序即弹窗优先级（升级框先），真出现竞争时由
     * {@code AppModalGate} 兜底。</p>
     *
     * <p><b>重复调用是安全的</b>：本方法可能在同一次进程里被调多次（Activity 重建后
     * {@code onActivityResult} 又给回 OK；引导页那边也会 {@code finish()} 后回传 OK）。
     * {@code ActivityManager} 存回调的列表<b>没有去重</b>，重复注册会让回调越挂越多；
     * 但各 Manager 内部都有进程级 {@code LaunchOnceGate} 闸门，第二次起不会再发请求，
     * 实际后果只是日志重复与列表增长。修法见 {@code ActivityManager}（尚未做）。</p>
     */
    private void onConsentAndPermissionGranted() {
        UpdateManager.registerForegroundCheck();
        AnnouncementManager.registerForegroundCheck();
        SearchPermissionManager.registerForegroundCheck();
        MingCiPermissionManager.registerForegroundCheck();

        mPermissionChecked = true;
        super.initActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_PERMISSION_GUIDE) {
            return;
        }
        if (resultCode == RESULT_OK) {
            onConsentAndPermissionGranted();
        }
        // 非 OK 不做任何处理：引导页所有「没通过」的场景（点退出、拒绝权限、拒绝去设置
        // 对话框）都已在它自己那侧结束进程（System.exit(0)），结果根本投递不到这里。
        // 无需兜底 —— 加一个 exitApp() 兜底反而是死代码，且会让人误以为这里存在一条
        // 「引导页没退出但回来了」的路径。
    }

    @Deprecated
    @Override
    protected void onDestroy() {
        // 因为修复了一个启动页被重复启动的问题，所以有可能 Activity 还没有初始化完成就已经销毁了
        // 所以如果需要在此处释放对象资源需要先对这个对象进行判空，否则可能会导致空指针异常
        super.onDestroy();
    }
}