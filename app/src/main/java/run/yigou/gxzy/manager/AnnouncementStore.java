package run.yigou.gxzy.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.mmkv.MMKV;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import run.yigou.gxzy.data.remote.model.Announcement;
import run.yigou.gxzy.log.EasyLog;

/**
 * 公告的落盘存储：<b>弹窗内容缓存</b>与<b>拉取节流时钟</b>。
 *
 * <p>弹窗延迟区间在 {@link PopDelay}，与存储无关，故不在本类。</p>
 *
 * <p>需求是「每次 App 启动都弹出全部有效公告」，所以本类<b>没有</b>「已读记忆」——
 * 关掉即关掉，下次启动照弹。存的是「最近一次成功拉到的公告列表」，
 * 弹窗显示的就是它；远程只负责每天刷新这份缓存。</p>
 *
 * <p>存储用 MMKV（与 {@code PendingForceUpgradeStore} 同一惯例：进程级单例 + 方法内获取，
 * 不持 Context），Gson 默认字段名映射。</p>
 */
public final class AnnouncementStore {

    private static final String TAG = "AnnouncementStore";

    /**
     * 拉取失败后的退避间隔：同一自然日只成功拉取一次（需求「每天从远程获取一次」），
     * 失败则等这么久再试（需求「获取不成功则延后重新获取」）。
     *
     * <p><b>注意这是「退避间隔」，不是「每天一次」的配额</b>。后者由
     * {@link #shouldFetchNow(long, FetchClock)} 里的 {@link #isSameLocalDay} 按
     * <b>本地自然日</b>判定（跨过 23:59:59 即算第二天），与本常量无关。
     * 本常量的实际定义在 {@link FetchClockStore}（两个功能共用）。</p>
     */
    public static final long RETRY_INTERVAL_MS = FetchClockStore.RETRY_INTERVAL_MS;

    private static final String MMKV_ID = "announcement_state";
    /** 拉取到的公告正文（只保存最近一次成功拉到的），弹窗显示的就是它。 */
    private static final String KEY_CACHE = "cache";

    private static final Gson GSON = new Gson();
    private static final Type CACHE_TYPE = new TypeToken<List<Announcement>>() {
    }.getType();

    private AnnouncementStore() {
    }

    /**
     * 取本类的 MMKV 实例。
     *
     * <p>方法内获取（与 {@code PendingForceUpgradeStore} 同一写法）而不是静态字段或
     * Holder：静态初始化会先于任何调用执行，而 MMKV 需要 native 库，JVM 单测里一触发就
     * {@code UnsatisfiedLinkError}。方法内获取天然把加载推迟到真正读写时，使
     * {@link #readCache} 可被纯 JVM 单测间接验证。</p>
     */
    private static MMKV kv() {
        return MMKV.mmkvWithID(MMKV_ID);
    }

    // ==================== 纯逻辑（不碰 MMKV，可 JVM 单测） ====================

    /**
     * 某条公告在 {@code now} 时刻是否已过期（纯逻辑，可 JVM 单测）。
     *
     * <p>{@code validTo} 为空表示无结束时间，<b>永不过期</b>。等于 {@code now} 视为
     * 仍有效（与后端 SQL 的 {@code valid_to >= ?} 口径一致）。
     */
    public static boolean isExpired(Announcement announcement, long now) {
        if (announcement == null || announcement.getValidTo() == null) {
            return false;
        }
        return announcement.getValidTo() < now;
    }

    /**
     * 滤掉已过期的公告（纯逻辑，可 JVM 单测）。
     *
     * <p>用户要求「本地只保存最新得到的内容，过期的删除掉」：写入前用它裁一遍，读取时
     * 再用它裁一遍（缓存可能已经放了很久，读出来时才发现过期）。</p>
     */
    public static List<Announcement> pruneExpired(List<Announcement> list, long now) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        List<Announcement> alive = new ArrayList<>();
        for (Announcement announcement : list) {
            if (announcement != null && !isExpired(announcement, now)) {
                alive.add(announcement);
            }
        }
        return alive;
    }

    /**
     * 两个时刻是否落在**同一个本地自然日**（纯逻辑，可 JVM 单测）。
     *
     * <p>需求：「每天从远程获取一次，超过 23:59:59 就算第二天」。用本地时区的
     * 年 + 年内第几天比较：同一日返回 true，跨过午夜返回 false。</p>
     *
     * <p>时区用设备默认时区，跟着用户走 —— 「今天」的含义对用户就是设备上的今天。</p>
     */
    public static boolean isSameLocalDay(long first, long second) {
        return localDayNumber(first) == localDayNumber(second);
    }

    /** 本地自然日的序号（同年同日同值）。 */
    private static long localDayNumber(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar.get(Calendar.YEAR) * 1000L + calendar.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 现在是否该去远程拉一次（纯逻辑，可 JVM 单测）。
     *
     * <p>三条规则（需求：「每天从远程获取一次，获取不成功则延后重新获取」）：</p>
     * <ol>
     *   <li><b>今天已成功过 → 当天不再拉</b>：上次成功与当前落在同一本地自然日就否决。
     *       按本地时区判定，跟着用户设备上的「今天」走（超过 23:59:59 算次日）。</li>
     *   <li><b>今天已尝试过（多半失败）→ 走 30 分钟退避</b>：距上次**尝试**不足
     *       {@link #RETRY_INTERVAL_MS} 就跳过，避免离线用户每次启动都打注定失败的网络请求；
     *       退避期过则再试一次（需求「延后重新获取」）。</li>
     *   <li><b>今天既没成功也没尝试过 → 放行</b>：跨过午夜（昨天成功、今天首次）或从未成功都算。
     *       跨日优先于退避：23:55 成功、00:20 才打开时距上次尝试仅 25 分钟，但若先判退避
     *       会被拦下、要再等 5 分钟，与「超过 23:59:59 就算第二天」冲突，故先问「是不是新的一天」。</li>
     * </ol>
     *
     * <p><b>关键修复</b>：此前把「跨日」写成无条件的首条放行，导致「昨天成功、今天持续失败」
     * 时每次调用都直接 return true、完全绕过退避（一天可拉数十次）。正确语义是
     * <b>跨日只放行一次</b>——今天的首次尝试放行后，若失败则 {@link #markFetchFailure}
     * 把 lastAttemptAt 推进到今天，之后就落入第 2 条退避，而非无限次直接放行。</p>
     *
     * <p>时钟回拨（用户把系统时间往前调）会让「距上次 &lt; 间隔」恒为真 → 永久饿死拉取。
     * 退避判断里用 {@code now &lt; lastAttemptAt} 放行回拨；而「今天已成功 / 已尝试」两道
     * 自然日判定在回拨后都变为「非今天」，同样自然放行，不会饿死。</p>
     */
    public static boolean shouldFetchNow(long now, FetchClock clock) {
        // 1. 今天已经成功拉过 → 当天不再拉（需求「每天一次」的自然日配额）。
        if (clock.lastSuccessAt > 0 && isSameLocalDay(clock.lastSuccessAt, now)) {
            return false;
        }
        // 2. 今天已经尝试过（多半失败）→ 走 30 分钟退避，退避期过再试一次。
        if (clock.lastAttemptAt > 0 && isSameLocalDay(clock.lastAttemptAt, now)) {
            // 退避期内不打网络（差值为负表示时钟回拨，放行避免永久退避）。
            if (now - clock.lastAttemptAt < RETRY_INTERVAL_MS) {
                return now < clock.lastAttemptAt;
            }
            return true; // 退避期已过 → 再试一次
        }
        // 3. 今天既没成功也没尝试过（跨过午夜，或从未成功）→ 放行。
        return true;
    }

    // ==================== 弹窗内容缓存（弹窗显示的就是它） ====================

    /**
     * 读出本地缓存的公告列表（弹窗数据源）。
     *
     * <p>用户要求：<b>弹窗显示的内容来自本地</b>，远程只负责每天更新一次这份缓存。
     * 这样断网、请求慢、后端临时不可用都不影响用户看到公告。</p>
     *
     * <p>读出时会再裁一遍过期项：缓存可能是上次启动写的，现在已经过期。</p>
     */
    public static List<Announcement> readCache() {
        long now = System.currentTimeMillis();
        try {
            String raw = kv().decodeString(KEY_CACHE);
            if (raw == null || raw.isEmpty()) {
                return new ArrayList<>();
            }
            List<Announcement> parsed = GSON.fromJson(raw, CACHE_TYPE);
            List<Announcement> alive = pruneExpired(parsed, now);
            if (parsed != null && alive.size() != parsed.size()) {
                // 回写一次，把过期项真正从存储层删掉（需求「过期的删除掉」）。
                // 只在内存里裁剪的话，远程长期失败时过期行会永久留在 MMKV 里。
                writeCache(alive, now);
            }
            return alive;
        } catch (Throwable error) {
            // 缓存坏了就当没有：最坏是这次不弹公告，绝不能让读盘异常连累 App 启动。
            EasyLog.print(TAG, "读取公告缓存失败，降级为空（本次不弹公告）");
            EasyLog.print(error);
            return new ArrayList<>();
        }
    }

    /**
     * 覆盖写入本地缓存：<b>只保存最近一次拉到的内容</b>，过期的当场丢掉（用户要求）。
     *
     * <p>「覆盖」而非「追加」：远程返回什么就是什么，运营撤下公告后不会留在用户手机上。
     * 传空列表等价于清空（远程说「现在没有公告」时必须这么处理）。</p>
     *
     * @return 是否真的落到盘上。false 表示写失败——调用方据此**不**标记「拉取成功」，
     *         否则用户会在<b>当日</b>一直看旧内容却以为已经更新过（节流按本地自然日
     *         判定，见 {@link #shouldFetchNow(long, FetchClock)}）。
     */
    public static boolean writeCache(List<Announcement> list, long now) {
        try {
            List<Announcement> alive = pruneExpired(list, now);
            if (alive.isEmpty()) {
                clearCache();
                return true;
            }
            boolean ok = kv().encode(KEY_CACHE, GSON.toJson(alive));
            EasyLog.print(TAG, "已写入公告缓存 " + alive.size() + " 条");
            return ok;
        } catch (Throwable error) {
            EasyLog.print(TAG, "写入公告缓存失败（不阻断本次弹窗）");
            EasyLog.print(error);
            return false;
        }
    }

    /**
     * 清空本地缓存。仅在「拉取成功但服务器明确返回空列表」时调用
     * （见 {@code AnnouncementManager.onSucceed}）——没有公告了，旧内容就是陈旧信息。
     *
     * <p>返回 void 而非 boolean：清空失败不改变任何后续判断（弹窗那一侧到点读盘读到的仍是
     * 旧内容，最坏是多弹一次本该消失的公告；标记成功与否无关，故不需要向上传递成败）。</p>
     */
    public static void clearCache() {
        try {
            kv().removeValueForKey(KEY_CACHE);
        } catch (Throwable error) {
            EasyLog.print(TAG, "清空公告缓存失败（下次读盘仍会读到旧内容）");
            EasyLog.print(error);
        }
    }

    // ==================== 拉取节流 ====================

    /** 现在该不该拉远程（读盘 + 判定；{@link #shouldFetchNow} 是其中的纯逻辑）。 */
    public static boolean shouldFetchNow() {
        return shouldFetchNow(System.currentTimeMillis(), readClock());
    }

    /** 拉取成功：同时推进「上次尝试」与「上次成功」。 */
    public static void markFetchSuccess(long now) {
        writeClock(FetchClock.afterSuccess(now));
    }

    /**
     * 拉取失败：只推进「上次尝试」，让 {@link #RETRY_INTERVAL_MS} 的退避生效，
     * 而「上次成功」保持不变——否则退避期一过，若那天已成功过就再也不会拉了。
     */
    public static void markFetchFailure(long now) {
        writeClock(readClock().afterFailure(now));
    }

    private static FetchClock readClock() {
        return FetchClockStore.read(MMKV_ID, TAG);
    }

    private static void writeClock(FetchClock clock) {
        FetchClockStore.write(MMKV_ID, TAG, clock);
    }
}
