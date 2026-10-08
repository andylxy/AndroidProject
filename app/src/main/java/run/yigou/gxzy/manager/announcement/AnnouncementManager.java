package run.yigou.gxzy.manager.announcement;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.base.BaseDialog;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.data.remote.api.announcement.AnnouncementApi;
import run.yigou.gxzy.data.remote.model.Announcement;
import run.yigou.gxzy.data.remote.model.AnnouncementResponse;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.network.server.VersionRequestServer;
import run.yigou.gxzy.dialog.AnnouncementDialog;
import run.yigou.gxzy.manager.concurrency.RetryScheduler;
import run.yigou.gxzy.manager.launch.LaunchOnceGate;
import run.yigou.gxzy.manager.modal.AppModalGate;
import run.yigou.gxzy.manager.update.UpdateManager;
import run.yigou.gxzy.manager.concurrency.PopDelay;
import run.yigou.gxzy.manager.lifecycle.ActivityManager;
import run.yigou.gxzy.manager.lifecycle.ForegroundActivities;

/**
 * 公告的拉取与展示（DESIGN §6.1）。严格镜像 {@code UpdateManager} 的结构。
 *
 * <p><b>需求</b>：App 启动就拉一次后端下发的公告写入本地缓存，启动完成后等
 * 15~25 秒，<b>从本地缓存</b>读出有效公告弹窗（标题 + 正文 + 关闭），没有就
 * <b>完全静默</b>。硬性不变量（DESIGN §2）：</p>
 * <ol>
 *   <li><b>静默失败</b>（INV-1）：网络错误 / 解析失败 → 只打 log，
 *       <b>不弹错误框、不 toast</b>。本类没有任何 toast 分支，这是与「检查更新」最大的区别
 *       （那边手动触发时会 toast 报「检查更新失败」，而公告是纯后台行为，弹个错误框
 *       只会让用户以为 App 坏了）。注意「静默」<b>不等于</b>「不给看旧公告」——
 *       拉取失败时本地缓存照弹。</li>
 *   <li><b>空即静默</b>（INV-2）：到点时本地缓存里没有有效公告就直接返回。</li>
 *   <li><b>一次启动一拉</b>（INV-3）：{@link LaunchOnceGate} 进程级闸门，
 *       回到前台不重复拉。</li>
 *   <li><b>不阻塞主流程</b>（INV-5）：关闭后正常进 App，不拦截任何导航。返回键与
 *       点外部同样算「用户主动关闭」，必须真的把对话框关掉。</li>
 * </ol>
 *
 * <p><b>与升级框的先后</b>（DESIGN §6.5）：升级框优先。它在屏时公告<b>排队等</b>，
 * 关掉后自动补弹，队列不丢——协调靠共享的 {@link AppModalGate}，不是各自的标志。</p>
 */
public final class AnnouncementManager {

    private static final String TAG = "AnnouncementManager";

    /**
     * 进程级「本次启动已拉过」闸门（INV-3）。
     *
     * <p>与 {@code UpdateManager} 的版本检查同理：{@code onApplicationForeground}
     * 在冷启动与每次回前台都会触发，而需求只要求「启动完成时获取一次」。</p>
     */
    private static final LaunchOnceGate sLaunchGate = new LaunchOnceGate();

    /**
     * 延迟弹窗用：{@code postDelayed} 必须挂在主线程 Handler 上（弹窗只能在主线程弹）。
     */
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /** 弹窗延迟的抖动来源。固定值会让每次启动都在同一刻弹，观感像闹钟。 */
    private static final Random sRandom = new Random();

    /**
     * 启动延迟是否已过（{@code volatile}：由 {@code postDelayed} 的主线程写、
     * 由 resume 回调读，虽同为主线程仍显式声明，避免以后挪到子线程时静默出错）。
     *
     * <p>延迟未过时，resume 回调<b>不</b>触发弹窗——否则 Splash→首页那一次 resume
     * 就会把「启动后等 15~25 秒」直接抵消掉。</p>
     */
    private static volatile boolean sPopAllowed = false;

    /**
     * 本次启动待弹的队列（按弹出顺序）。用并发容器以免回调线程与主线程竞争。
     *
     * <p>没有「已读」概念：需求是「每次启动都把有效公告弹一遍」，关闭只推进本次队列，
     * 不影响下次启动（队列只活在内存里，进程结束即消失）。</p>
     */
    private static final List<Announcement> sQueue = new CopyOnWriteArrayList<>();

    /**
     * 「已排了一次展示尝试」的单飞标志（adb 实测后加）。
     *
     * <p>没有它时，{@code onApplicationResume} 与闸门补弹会在闸门繁忙时**各排一个**任务，
     * 队列越堆越长；弹出时又逐条消费，最坏情况是同一批公告排着多次补弹。</p>
     */
    private static final AtomicBoolean sShowScheduled = new AtomicBoolean(false);

    /**
     * 连续「{@code show()} 抛异常」的次数（成功弹出一次即清零）。
     *
     * <p>用途是给延后重试封顶：{@code show()} 失败通常是宿主状态的**持续**问题
     * （同一个 Activity 不会因为再试一次就好），无限重试只会刷日志耗电。见
     * {@code showDialog} 的 catch 分支。</p>
     */
    private static final AtomicInteger sShowFailureCount = new AtomicInteger(0);

    /** 弹窗展示失败后最多再试几次（超过就放弃本次弹出，等下次启动）。 */
    private static final int MAX_SHOW_FAILURES = 1;

    /**
     * 退避重试的排程器（单飞 + 主线程定时）。
     *
     * <p>三条失败分支（响应缺字段、写盘失败、网络失败）各自都会排重试；单飞保证一次失败
     * 只排出<b>一个</b> 30 分钟后的任务 —— 否则每轮失败都会加倍，无限膨胀。</p>
     */
    private static final RetryScheduler sRetryScheduler = new RetryScheduler(
            AnnouncementStore.RETRY_INTERVAL_MS,
            () -> maybeRefreshCache(ForegroundActivities.topIfUsable()));

    private AnnouncementManager() {
    }

    /**
     * 把公告拉取挂到<b>应用级</b>前台回调（DESIGN §6.1）。
     *
     * <p>由 {@code AppApplication} 在启动时调用一次。</p>
     */
    public static void registerForegroundCheck() {
        EasyLog.print(TAG, "公告拉取已挂到应用级前台回调，本次启动只拉一次");
        ActivityManager.getInstance().registerApplicationLifecycleCallback(
                new ActivityManager.ApplicationLifecycleCallback() {
                    @Override
                    public void onApplicationCreate(Activity activity) {
                        // 冷启动也会走 onApplicationForeground，这里无需处理。
                    }

                    @Override
                    public void onApplicationDestroy(Activity activity) {
                        // 不在此做任何清理：公告队列留在内存里随进程一起消失即可。
                    }

                    @Override
                    public void onApplicationBackground(Activity activity) {
                        // 不在退到后台时弹公告：那会在用户切出去时强拉一个框。
                    }

                    @Override
                    public void onApplicationForeground(Activity activity) {
                        tryLaunchFetch(activity);
                    }
                });
        // 展示重试 + 首次拉取的兜底触发点（adb 实测后加）：
        // ① 展示重试：冷启动时首个前台回调拿到的宿主往往还不可用（实测「无可用宿主，
        //    公告留队等待下次机会」），而那次队列若只等「下次机会」就永远等不到——本次
        //    进程再没有别的触发点，公告直接丢失。
        // ② 首次拉取兜底：实测存在「整个进程只有一次 onApplicationForeground、且那次宿主
        //    不是 LifecycleOwner」的情况，于是靠上面那条闸门修法仍会一次都不拉。
        //    每个 Activity resume 必定带一个可用的 LifecycleOwner 宿主，由它兜底最可靠
        //    （与 UpdateManager 的 registerActivityResumeCallback 同一套路）。
        ActivityManager.getInstance().registerActivityResumeCallback(
                new ActivityManager.ActivityResumeCallback() {
                    @Override
                    public void onActivityResumed(Activity activity) {
                        tryLaunchFetch(activity);
                        // 只在**启动延迟已过**时才补弹。延迟未到就弹，等于绕过
                        // 「启动后等 15~25 秒」的需求（resume 在启动初期非常密集，
                        // Splash→首页 一次就会把延迟抵消掉）。
                        if (sPopAllowed) {
                            scheduleShow();
                        }
                    }
                });
    }

    /**
     * 「本次启动拉一次」的唯一入口：宿主可用**且**闸门未置位时才拉。
     *
     * <p>⚠️ 顺序不能反（adb 实测）：早期版本先 CAS 置位、再发现宿主不是
     * {@code LifecycleOwner}，于是「本次启动唯一一次」机会被白白烧掉，本次启动**永远
     * 不会再拉**——公告彻底不出现。先判宿主再置位，失败就等下一个触发点重来。
     *
     * <p>两个触发点共用本方法：应用级前台回调与每个 Activity resume。后者才是可靠的
     * 那个（resume 必定带可用宿主），前者只是更快。</p>
     */
    private static void tryLaunchFetch(Activity activity) {
        // 顺序与语义都封装在 LaunchOnceGate：先判宿主可用、再置位，失败不消耗机会。
        if (sLaunchGate.runOnceIfHostUsable(activity, () -> onLaunchReady(activity))) {
            EasyLog.print(TAG, "本次启动首次处理公告");
        }
    }

    /**
     * 启动完成后的统一入口：<b>先把延时弹窗排上，再按节流规则决定要不要拉远程</b>。
     *
     * <p>顺序是有意的：弹窗内容取自本地持久化缓存，它不依赖网络，也不该被远程请求的
     * 成败影响。这里只排<b>时钟</b>（何时去看有什么可弹），真正读缓存由
     * {@link #fillQueueFromCache()} 在到点那一刻执行；远程拉取在后面独立进行。</p>
     */
    private static void onLaunchReady(Activity activity) {
        schedulePopAfterLaunchDelay();
        maybeRefreshCache(activity);
    }

    /**
     * 启动后固定排一次延时动作：到点时<b>那一刻才读本地缓存</b>。
     *
     * <p>需求：App 启动立刻拉公告、拉到就写本地；启动完成后等 15~25 秒，<b>再从本地读
     * 最新内容</b>弹出 —— 让用户先看到主界面，而不是一进 App 就被框住。</p>
     *
     * <p>⚠️ <b>定时器必须无条件排</b>，不能「先预读缓存看看有没有」再决定排不排：排定的
     * 这一刻请求还没回来。<b>早期实现正是在这里预读缓存、为空就提前 return</b>，于是本次
     * 启动连定时器都没有、{@link #sPopAllowed} 也永远不会被置 true，而本次拉取写进本地的
     * 内容在全进程再没有第二处会去读 —— 结果是「本次拉到的公告本次启动永不弹」，只能等
     * 下一次冷启动。正确做法是<b>先上闹钟，闹钟响时才去看有什么可弹</b>。</p>
     */
    private static void schedulePopAfterLaunchDelay() {
        int delayMs = PopDelay.nextMs(sRandom);
        EasyLog.print(TAG, "启动后延迟 " + (delayMs / 1000) + "s，到点再读本地缓存决定弹窗");
        MAIN_HANDLER.postDelayed(() -> {
            // 先置「延迟已过」再读：让本次以及之后 resume 触发的补弹立刻生效，
            // 与 resume 回调里的语义保持一致。
            sPopAllowed = true;
            fillQueueFromCache();
        }, delayMs);
    }

    /**
     * 到点的动作：读本地缓存 → 挑出有效公告入队 → 尝试弹出。
     *
     * <p>读发生<b>在到点这一刻</b>，于是本次启动 {@link #maybeRefreshCache} 拉取成功并写入
     * 本地的内容（正常网络下远早于 15~25 秒）会被一并读到；拉取失败或超时则读到的仍是上次
     * 缓存 —— 回退路径与旧实现完全一致。若网络极慢导致写入晚于到点，新内容顺延到下次启动，
     * 不在中途半路补弹（避免同一条重复入队）。</p>
     */
    private static void fillQueueFromCache() {
        List<Announcement> cached = AnnouncementStore.readCache();
        if (cached.isEmpty()) {
            EasyLog.print(TAG, "本地无公告缓存，本次不弹（下次拉取成功后才会有）");
            return;
        }
        long now = System.currentTimeMillis();
        List<Announcement> queue = AnnouncementQueue.select(cached, now);
        if (queue.isEmpty()) {
            EasyLog.print(TAG, "缓存中没有有效公告，静默");
            return;
        }
        EasyLog.print(TAG, "本地缓存待弹 " + queue.size() + " 条: " + describe(queue));
        sQueue.addAll(queue);
        scheduleShow();
    }

    /**
     * 按节流规则决定是否拉远程：每天最多一次成功，失败后 30 分钟退避。
     *
     * <p>拉到的内容<b>只用于更新本地缓存</b>，本次启动的弹窗不依赖它（弹窗已按本地
     * 缓存排好）。这样远程失败或超时都不影响用户看到公告。</p>
     */
    private static void maybeRefreshCache(Activity activity) {
        if (!AnnouncementStore.shouldFetchNow()) {
            EasyLog.print(TAG, "今天已成功拉取过（或处于失败退避期），本次不请求");
            return;
        }
        if (activity == null || !(activity instanceof LifecycleOwner)) {
            // 节流已放行、只是没有可用宿主：必须排下一次，否则整条重试链到此为止。
            // 典型场景：退避 30 分钟到点时 App 还在后台（topIfUsable 返回 null），
            // 不续上就只能等明天那次冷启动。
            EasyLog.print(TAG, "宿主不可用，排一次退避后重试");
            scheduleRetry();
            return;
        }
        EasyLog.print(TAG, "拉取公告: url=" + AppConfig.getHostUrl() + "/api/app/announcements");
        EasyHttp.get((LifecycleOwner) activity)
                .server(new VersionRequestServer())
                .api(new AnnouncementApi())
                .request(new HttpCallback<AnnouncementResponse>(null) {

                    @Override
                    public void onSucceed(AnnouncementResponse response) {
                        long now = System.currentTimeMillis();
                        List<Announcement> list =
                                response == null ? null : response.getAnnouncements();
                        if (list == null) {
                            // 响应里没有 announcements 字段 = 响应对不上契约（网关空壳、
                            // 字段改名等），DESIGN §5.3 要求按 onFail 处理。绝不能当
                            // 「服务器说没有公告」——那会清空用户本地已有公告并锁住当日配额
                            //（节流是**本地自然日**判定，见 AnnouncementStore.shouldFetchNow，
                            //  不是「锁 24 小时」）。
                            AnnouncementStore.markFetchFailure(now);
                            EasyLog.print(TAG, "公告响应缺少 announcements 字段，"
                                    + "按失败处理（保留本地缓存）");
                            scheduleRetry();
                            return;
                        }
                        // 到这里才轮得到「空列表」，那是服务器的明确回答：运营撤回了全部公告。
                        // → writeCache 传空 = clearCache()，本地跟着清空。这是对的：既然已经
                        //   没有公告了，让用户继续看到运营早已撤下的内容就是陈旧信息。
                        //   与上面 list == null 的失败路径严格区分——失败**不清**缓存。
                        //
                        // ⚠️ 本次成功后**不重读、不入队、不补弹**（DESIGN §2.1 裁决 3，勿加）：
                        //   慢网下回包晚于弹窗时刻时，到点已按旧内容弹过了；再补弹会让同一批
                        //   内容在一次启动里弹两遍。取舍是「本次弹旧的、下次弹新的」。
                        //   本次拉到的内容已由 writeCache 全量覆盖写盘，下次启动即为最新。
                        if (AnnouncementStore.writeCache(list, now)) {
                            AnnouncementStore.markFetchSuccess(now);
                            EasyLog.print(TAG, "公告缓存已更新: " + list.size() + " 条");
                        } else {
                            // 写盘失败不能标记成功，否则**当日**不再重试，用户一直看旧内容。
                            AnnouncementStore.markFetchFailure(now);
                            EasyLog.print(TAG, "公告缓存写盘失败，不标记成功（稍后重试）");
                            scheduleRetry();
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        // INV-1 静默失败：只记退避时间点，不弹窗、不提示。
                        // 不清缓存——旧内容仍比没有内容好，下次启动照样能弹。
                        AnnouncementStore.markFetchFailure(System.currentTimeMillis());
                        EasyLog.print(TAG, "拉取公告失败(已静默，退避 "
                                + (AnnouncementStore.RETRY_INTERVAL_MS / 60000) + " 分钟): "
                                + e.getClass().getSimpleName()
                                + " / " + e.getMessage()
                                + " / url=" + AppConfig.getHostUrl() + "/api/app/announcements");
                        scheduleRetry();
                    }
                });
    }

    /**
     * 拉取失败后，安排一次「退避期到点再试」（需求：获取不成功则延后重新获取）。
     *
     * <p>只靠下次冷启动是不够的：进程可能被用户留在后台几小时甚至过夜，而 30 分钟
     * 退避期早就过了——那条公告要等到明天才有机会出现。重试本身仍走
     * {@link #maybeRefreshCache}，它开头的节流判定会拦住过于频繁的尝试，不会打转。</p>
     */
    private static void scheduleRetry() {
        sRetryScheduler.schedule();
    }

    /**
     * 排一次「尝试弹出队首」的动作。<b>单飞</b>：已有一次在途就不再排。
     *
     * <p>触发点都走这里：<b>到点读本地后</b>、每个 Activity resume、上一条关闭后、
     * 弹窗未能展示后的重试。
     * 闸门空闲时 {@code runWhenClear} 立即执行；闸门被升级框占着时排队等它关闭
     * （DESIGN §6.5 升级框优先），期间来的 resume 不会重复排队。</p>
     */
    private static void scheduleShow() {
        if (sQueue.isEmpty()) {
            return;
        }
        if (!sShowScheduled.compareAndSet(false, true)) {
            // 已有一次尝试在途（多半正排在闸门上），等它跑完即可。
            return;
        }
        AppModalGate.runWhenClear(() -> {
            // 先放开单飞标志：本次尝试结束时若队列还有货，后续触发点可以再排。
            sShowScheduled.set(false);
            showNext();
        });
    }

    /** 弹队列里的下一条；队列空或暂时弹不了就保留队列等下次机会。 */
    private static void showNext() {
        if (sQueue.isEmpty()) {
            return;
        }
        Activity activity = ForegroundActivities.topIfUsable();
        if (activity == null) {
            // 不弹出、也**不**丢弃队列：靠 resume 回调补试（冷启动首个宿主常不可用）。
            EasyLog.print(TAG, "无可用宿主，公告留队等待下次机会");
            return;
        }
        if (!AppModalGate.tryAcquire()) {
            // 闸门在别处被抢走了：重新排队等它空出来，不丢公告。
            AppModalGate.runWhenClear(() -> showNext());
            return;
        }
        Announcement announcement = sQueue.remove(0);
        showDialog(activity, announcement);
    }

    /**
     * 真正把公告弹窗显示出来。调用方必须已持有模态闸门。
     *
     * <p>三条收尾路径，<b>互斥</b>（由 {@code settled} 一次性闸门保证只走一条）：</p>
     * <ol>
     *   <li>用户主动关闭（点「我知道了」/ 返回键 / 点外部）→ 还闸门、弹下一条。</li>
     *   <li>宿主销毁导致对话框被拆掉（用户<b>根本没看到</b>）→ 放回队首等下次机会。</li>
     *   <li>{@code show()} 抛异常或静默 no-op → 交给 {@link #onShowFailed}。</li>
     * </ol>
     *
     * <p>⚠️ 判据不能只看「onClose 有没有跑」——两个回调的<b>先后顺序不由我们决定</b>
     * （{@code Builder.dismiss()} 里 super 会先发 onDismiss）。早期实现只在 onClose 里
     * CAS，onDismiss 抢在前面就把关闭当成「宿主销毁」，公告被放回队列立刻重弹，
     * 实测同一条公告连弹 4 次。现在让 onClose 自己置位，onDismiss 只认这个标志。</p>
     *
     * <p>⚠️ 用户主动关闭有<b>两条</b>路径（点按钮 / {@code Dialog.cancel()}），两条都必须
     * 回调 {@code mOnClose}，漏一条就会让用户「明明关掉了却看到它重弹」——实测漏
     * cancel 那条时，队列里后面的公告被永久堵住（第二条弹出计数为 0）。</p>
     */
    private static void showDialog(Activity activity, Announcement announcement) {
        // ⚠️ 两条关闭路径（点「我知道了」/ 返回键与点外部）都会回调 mOnClose，
        // Builder 内部已处理 Dialog.cancel() 绕过 dismiss 覆写的问题，
        // 所以这里不需要再判「是哪种关闭」。
        final AtomicBoolean userClosed = new AtomicBoolean(false);
        final AtomicBoolean settled = new AtomicBoolean(false);
        try {
            AnnouncementDialog.Builder builder = new AnnouncementDialog.Builder(activity)
                    .setAnnouncement(announcement)
                    .setOnCloseListener(new Runnable() {
                        @Override
                        public void run() {
                            userClosed.set(true);
                            if (!settled.compareAndSet(false, true)) {
                                return;
                            }
                            // 不记已读：需求是「每次启动都弹全部有效公告」，
                            // 关闭只推进队列，不影响下次启动。
                            // ⚠️ 顺序：先入队/推进，**再**还闸门。AppModalGate.release()
                            // 会同步执行 PENDING 里的任务（闸门空闲时当场执行），
                            // 若先还闸门，PENDING 会先弹出下一条、这条才排进队列 →
                            // priority DESC 顺序倒置。
                            AppModalGate.release();
                            showNext();
                        }
                    })
                    .addOnDismissListener(new BaseDialog.OnDismissListener() {
                        @Override
                        public void onDismiss(BaseDialog dialog) {
                            if (!settled.compareAndSet(false, true)) {
                                // 路径 ① 已经处理过（闸门已还、队列已推进）。
                                return;
                            }
                            if (userClosed.get()) {
                                // 路径 ① 的 onDismiss 尾巴：onClose 已做过收尾，这里什么都不用做。
                                return;
                            }
                            // 路径 ②：用户没看到。放回队首，等下一个可用宿主再弹。
                            EasyLog.print(TAG, "公告弹窗被宿主销毁（非用户关闭），放回队列: id="
                                    + announcement.getId());
                            if (!sQueue.contains(announcement)) {
                                sQueue.add(0, announcement);
                            }
                            AppModalGate.release();
                            scheduleShow();
                        }
                    });
            builder.show();

            // ⚠️ 必须查 isShowing()，**不能**只看 show() 有没有抛异常：
            // BaseDialog.Builder.show() 在宿主 isFinishing/isDestroyed 时是**静默 return**
            // （BaseDialog.java:926），既不抛异常也不显示。此时若当成成功：
            //   ① 计数器被错误清零（失败统计失真）；
            //   ② 对话框根本不存在 → 无 onClose/onDismiss → 闸门**永不还**，
            //      全 App（连强制升级框）再也弹不出任何模态框；
            //   ③ 该条公告已被 showNext 摘出队列，再没人记得它 → 永久丢失。
            // 这是冷启动的常见时序（首帧常是正在 finish 的 Splash），必须显式判活。
            if (!builder.isShowing()) {
                EasyLog.print(TAG, "公告弹窗未真正显示（宿主正在销毁），按展示失败处理: id="
                        + announcement.getId());
                // 模拟 showDialog 的 catch 分支：走同一套「还闸门 + 回队首 + 有限重试」，
                // 但不重复打「展示失败」的异常日志（这里不是异常）。
                if (settled.compareAndSet(false, true)) {
                    userClosed.set(true);
                    onShowFailed(announcement);
                }
                return;
            }
            // 成功弹出一条即清零失败计数：后续再失败又从「可重试」开始（封顶是针对
            // 连续失败，不是整个进程累计）。
            sShowFailureCount.set(0);
            EasyLog.print(TAG, "已弹公告: id=" + announcement.getId()
                    + ", version=" + announcement.getVersion());
        } catch (RuntimeException error) {
            if (userClosed.compareAndSet(false, true)) {
                onShowFailed(announcement);
            }
            EasyLog.print(TAG, "公告弹窗展示失败: " + error);
            EasyLog.print(error);
        }
    }

    /**
     * 「展示了但没成功」的统一收尾：还闸门 + 放回队首 + <b>有限</b>重试。
     *
     * <p>被两处调用：{@code show()} 抛异常，以及 {@code show()} 静默 no-op
     * （宿主在销毁，{@code BaseDialog} 直接 return 不抛异常）。</p>
     *
     * <p>⚠️ <b>顺序</b>：先入队、最后还闸门。{@code AppModalGate.release()} 会同步执行
     * PENDING 里的任务（闸门空闲时当场执行），若先还闸门，PENDING 会先弹出队列里
     * 下一条、再轮到这条重试 → {@code priority DESC} 顺序被倒置。</p>
     *
     * <p>⚠️ <b>绝不能同步重排</b>：{@code AppModalGate.runWhenClear} 在闸门空闲时当场执行，
     * 而 {@code show()} 失败通常是宿主状态的<b>持续</b>问题（同一个 Activity 不会因为
     * 再试一次就好），同步重排会无限递归 → {@code StackOverflowError}。必须
     * {@code post} 到下一个消息循环。</p>
     */
    private static void onShowFailed(Announcement announcement) {
        if (!sQueue.contains(announcement)) {
            sQueue.add(0, announcement);
        }
        // 只重试一次：第二次仍失败说明这个宿主根本弹不了（Activity 正在销毁、任务栈
        // 异常等），继续重试只会刷日志耗电。放弃后公告会在下次启动重弹 —— 本类不记
        // 已读、缓存内容也没动，「用户这次没看到」不会永久丢失。
        if (sShowFailureCount.incrementAndGet() <= MAX_SHOW_FAILURES) {
            EasyLog.print(TAG, "公告弹窗未能展示，安排一次延后重试: id="
                    + announcement.getId());
            MAIN_HANDLER.post(() -> scheduleShow());
        } else {
            // 放弃这条**不能连坐后面的**：队列里剩下的公告是各自独立的条目，
            // 它们还没试过。直接推进到下一条，否则「本次不弹」会变成「剩下的全不弹」。
            EasyLog.print(TAG, "公告弹窗连续未能展示 " + sShowFailureCount.get()
                    + " 次，放弃这一条（下次启动会再弹）: id=" + announcement.getId());
        }
        AppModalGate.release();
        // 让队列里剩下的条目也有机会弹（可能剩下的是能正常显示的）。
        scheduleShow();
    }

    /** 队列内容的日志摘要（只到 id/version，够定位，不刷屏）。 */
    private static String describe(List<Announcement> queue) {
        StringBuilder text = new StringBuilder();
        for (Announcement announcement : queue) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(announcement.getId()).append("@v").append(announcement.getVersion());
        }
        return text.toString();
    }
}
