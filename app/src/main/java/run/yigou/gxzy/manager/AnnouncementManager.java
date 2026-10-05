package run.yigou.gxzy.manager;

import android.app.Activity;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.base.BaseDialog;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import run.yigou.gxzy.app.AppConfig;
import run.yigou.gxzy.data.remote.api.AnnouncementApi;
import run.yigou.gxzy.data.remote.model.Announcement;
import run.yigou.gxzy.data.remote.model.AnnouncementResponse;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.network.server.VersionRequestServer;
import run.yigou.gxzy.ui.dialog.AnnouncementDialog;

/**
 * 公告的拉取与展示（DESIGN §6.1）。严格镜像 {@code UpdateManager} 的结构。
 *
 * <p><b>需求</b>：App 启动完成后拉一次后端下发的公告，有就弹窗（标题 + 正文 + 关闭），
 * 没有就<b>完全静默</b>。四条硬性不变量（DESIGN §2）：</p>
 * <ol>
 *   <li><b>静默失败</b>（INV-1）：网络错误 / 解析失败 / 无内容 → 只打 log，
 *       <b>不弹窗、不 toast</b>。本类没有任何 toast 分支，这是与「检查更新」最大的区别
 *       （那边手动触发时会 toast 报「检查更新失败」，而公告是纯后台行为，弹个错误框
 *       只会让用户以为 App 坏了）。</li>
 *   <li><b>空即静默</b>（INV-2）：{@code announcements} 为空直接返回。</li>
 *   <li><b>一次启动一拉</b>（INV-3）：{@link LaunchOnceGate} 进程级闸门，
 *       回到前台不重复拉。</li>
 *   <li><b>不阻塞主流程</b>（INV-5）：关闭后正常进 App，不拦截任何导航。</li>
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

    /** 本次启动待弹的队列（不含已读，按弹出顺序）。用并发容器以免回调线程与主线程竞争。 */
    private static final List<Announcement> sQueue = new CopyOnWriteArrayList<>();

    /**
     * 「已排了一次展示尝试」的单飞标志（adb 实测后加）。
     *
     * <p>没有它时，{@code onApplicationResume} 与闸门补弹会在闸门繁忙时**各排一个**任务，
     * 队列越堆越长；弹出时又逐条消费，最坏情况是同一批公告排着多次补弹。</p>
     */
    private static final AtomicBoolean sShowScheduled = new AtomicBoolean(false);

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
                        scheduleShow();
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
        if (sLaunchGate.runOnceIfHostUsable(activity, () -> fetchAndShow(activity))) {
            EasyLog.print(TAG, "本次启动首次拉取公告");
        }
    }

    /**
     * 拉取 {@code /api/app/announcements} 并按结果弹窗。
     *
     * <p>不在拉取层做「已有弹窗就跳过」的去重：那是拉取层去重，会把本次拉到的公告整批
     * 丢掉。去重在弹窗层（{@link #showNext} + {@link AppModalGate}），而拉取是幂等只读
     * GET，多拉一次没有代价。</p>
     */
    private static void fetchAndShow(Activity activity) {
        if (activity == null) {
            return;
        }
        // 本项目的 Activity 都继承 AppActivity → BaseActivity → AppCompatActivity，
        // 即都是 LifecycleOwner；仍显式判断一次，避免把非生命周期宿主交给 EasyHttp。
        if (!(activity instanceof LifecycleOwner)) {
            EasyLog.print(TAG, "宿主不是 LifecycleOwner，跳过公告拉取");
            return;
        }
        EasyLog.print(TAG, "拉取公告: url=" + AppConfig.getHostUrl() + "/api/app/announcements");
        EasyHttp.get((LifecycleOwner) activity)
                .server(new VersionRequestServer())
                .api(new AnnouncementApi())
                .request(new HttpCallback<AnnouncementResponse>(null) {

                    @Override
                    public void onSucceed(AnnouncementResponse response) {
                        List<Announcement> list = response == null ? null : response.getAnnouncements();
                        // INV-2：空列表（或 null）一律静默返回，一个字都不打给用户。
                        if (list == null || list.isEmpty()) {
                            EasyLog.print(TAG, "没有生效公告，静默返回");
                            return;
                        }
                        // 滤已读 + 排序 + 截断到 K 条（未弹的不丢，下次启动补弹）。
                        List<Announcement> queue = AnnouncementQueue.select(
                                list, AnnouncementQueue.MAX_POP_PER_LAUNCH);
                        if (queue.isEmpty()) {
                            EasyLog.print(TAG, "公告均已读过或无效，静默返回");
                            return;
                        }
                        EasyLog.print(TAG, "待弹公告 " + queue.size() + " 条: " + describe(queue));
                        sQueue.addAll(queue);
                        scheduleShow();
                    }

                    @Override
                    public void onFail(Exception e) {
                        // INV-1：静默失败。日志必须能一眼看出原因（连同实际请求地址），
                        // 否则排查时分不清「后端没这个接口(404)」「地址不可达」「超时」。
                        EasyLog.print(TAG, "拉取公告失败(已静默): " + e.getClass().getSimpleName()
                                + " / " + e.getMessage()
                                + " / url=" + AppConfig.getHostUrl() + "/api/app/announcements");
                    }
                });
    }

    /**
     * 排一次「尝试弹出队首」的动作。<b>单飞</b>：已有一次在途就不再排。
     *
     * <p>三个触发点都走这里：拉取成功后、每个 Activity resume、上一条关闭后。
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

    /** 真正把公告弹窗显示出来。调用方必须已持有模态闸门。 */
    private static void showDialog(Activity activity, Announcement announcement) {
        // 两条收尾路径，**互斥**（由 userClosed 一次性闸门保证只走一条）：
        //
        // ① 用户主动关闭（点「我知道了」/ 返回键 / 点外部）——这是「已读」：
        //    记已读、还闸门、弹下一条。
        // ② 宿主销毁导致对话框被拆掉（Activity 切换/被回收，用户**根本没看到**）——
        //    **绝不记已读**，把公告**放回队首**等下次机会。
        //
        // ② 若误当成 ①，消息就永久丢失：用户没读过的公告被记成已读，下次不再弹
        // （adb 实测踩到：Activity 切换瞬间三条公告全被记已读，一条都没让人看到）。
        final AtomicBoolean userClosed = new AtomicBoolean(false);
        try {
            new AnnouncementDialog.Builder(activity)
                    .setAnnouncement(announcement)
                    .setOnCloseListener(new Runnable() {
                        @Override
                        public void run() {
                            if (!userClosed.compareAndSet(false, true)) {
                                return;
                            }
                            AnnouncementStore.markSeen(
                                    announcement.getId(), announcement.getVersion());
                            AppModalGate.release();
                            showNext();
                        }
                    })
                    .addOnDismissListener(new BaseDialog.OnDismissListener() {
                        @Override
                        public void onDismiss(BaseDialog dialog) {
                            if (!userClosed.compareAndSet(false, true)) {
                                // 路径 ① 已经处理过（闸门已还、队列已推进）。
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
                    })
                    .show();
            EasyLog.print(TAG, "已弹公告: id=" + announcement.getId()
                    + ", version=" + announcement.getVersion());
        } catch (RuntimeException error) {
            // show() 可能因宿主状态异常（如 onSaveInstanceState 之后提交事务）抛出。
            // 同样按「用户没看到」处理：闸门必须还回去（否则全 App 弹不出模态框），
            // 且不能记已读。
            if (userClosed.compareAndSet(false, true)) {
                if (!sQueue.contains(announcement)) {
                    sQueue.add(0, announcement);
                }
                AppModalGate.release();
            }
            EasyLog.print(TAG, "公告弹窗展示失败: " + error);
            EasyLog.print(error);
        }
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
