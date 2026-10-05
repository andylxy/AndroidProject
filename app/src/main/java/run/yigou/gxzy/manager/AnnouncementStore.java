package run.yigou.gxzy.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.mmkv.MMKV;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import run.yigou.gxzy.log.EasyLog;

/**
 * 公告「已读」记忆的落盘存储（DESIGN §6.2 / D2）。
 *
 * <p>记的是「用户已关闭过哪条公告的哪个内容版本」，形如 {@code id → seenVersion}。
 * 为什么不是单纯的 id 集合：后端改了标题/正文会把 {@code version} +1，用户<b>应该</b>
 * 再看一次新的内容；只记 id 会让改过的公告永远不再出现。</p>
 *
 * <p>判定口径：{@code seenVersion < version} 即「未读」→ 需要弹。
 * 等号（已读且看过同一版内容）才算已读。</p>
 *
 * <p>存储用 MMKV（与 {@code PendingForceUpgradeStore} 同一惯例：进程级单例 + 方法内获取，
 * 不持 Context），Gson 默认字段名映射。</p>
 *
 * <p><b>失效与清理</b>：本类不主动清理历史条目。公告条目数量级极小（运营位而非流水账），
 * 全量保留的代价可忽略，而「清理错了导致停服通知不再弹」的代价极高。
 * 若将来条目数增长到需要清理，应按 {@code seenVersion} 远小于当前 {@code version}
 * 且已过期的条目清理，<b>不可</b>按固定条数截断。</p>
 */
public final class AnnouncementStore {

    private static final String TAG = "AnnouncementStore";

    private static final String MMKV_ID = "announcement_state";
    private static final String KEY_SEEN_MAP = "seen_map";

    private static final Gson GSON = new Gson();
    private static final Type SEEN_MAP_TYPE = new TypeToken<Map<Integer, Integer>>() {
    }.getType();

    private AnnouncementStore() {
    }

    /**
     * 取本类的 MMKV 实例。
     *
     * <p>方法内获取（与 {@code PendingForceUpgradeStore} 同一写法）而不是静态字段或
     * Holder：静态初始化会先于任何调用执行，而 MMKV 需要 native 库，JVM 单测里一触发就
     * {@code UnsatisfiedLinkError}。方法内获取天然把加载推迟到真正读写时，使
     * {@link #isSeen} 可被纯 JVM 单测直接调用。</p>
     */
    private static MMKV kv() {
        return MMKV.mmkvWithID(MMKV_ID);
    }

    /**
     * 这条公告是否已按<b>当前内容版本</b>读过（纯逻辑，<b>不碰 MMKV / Android</b>，可 JVM 单测）。
     *
     * <p>从未见过 → 未读。见过但 {@code seenVersion < version}（后端改过内容）→ 仍算未读，
     * 于是重弹一次（DESIGN D2）。</p>
     *
     * <p><b>方法名刻意不叫 {@code isSeen}</b>：本类还有一个走 MMKV 的
     * {@link #isSeen(int, int)}。若两者同名重载，调用方写 {@code isSeen(1, 1)} 这种
     * <b>int 字面量</b>实参时会静态绑定到 {@code (int, int)} 那个 → 单元测试里
     * 直接 {@code RuntimeException: android.util.Log not mocked}（MMKV 要 native 库）。
     * 改名后两个方法的语义在名字上就分得开，也不可能再绑错。</p>
     *
     * @param seenVersion 已记录的版本；{@code null} 表示从未读过
     * @param version     后端本次下发的内容版本
     */
    public static boolean isSeenVersion(Integer seenVersion, int version) {
        if (seenVersion == null) {
            return false;
        }
        return seenVersion >= version;
    }

    /**
     * 读出整张已读表。
     *
     * <p>读盘失败/脏数据一律降级为<b>空表</b>：那意味着「全都当没读过」，
     * 最坏结果是公告多弹一次；而解析失败若抛出去，会连累整个拉取流程。</p>
     */
    private static Map<Integer, Integer> readAll() {
        try {
            String raw = kv().decodeString(KEY_SEEN_MAP);
            if (raw == null || raw.isEmpty()) {
                return new HashMap<>();
            }
            Map<Integer, Integer> parsed = GSON.fromJson(raw, SEEN_MAP_TYPE);
            return parsed == null ? new HashMap<Integer, Integer>() : parsed;
        } catch (Throwable error) {
            EasyLog.print(TAG, "读取公告已读表失败，降级为空表（公告会多弹一次）");
            EasyLog.print(error);
            return new HashMap<>();
        }
    }

    /**
     * 这条公告是否已按当前内容版本读过（走 MMKV 已读表；<b>不能在纯 JVM 单测里调用</b>）。
     *
     * <p>{@code announcementId <= 0} 视为「没有有效 id」，返回 false（当未读过）——
     * 用一个假的 id 去记忆已读，会让所有 id=0 的脏数据共用同一条记忆。</p>
     */
    public static boolean isSeen(int announcementId, int version) {
        if (announcementId <= 0) {
            return false;
        }
        return isSeenVersion(readAll().get(announcementId), version);
    }

    /**
     * 记下「用户已关闭过这条公告的这个内容版本」。
     *
     * <p>只升不降：若已记的版本更高（理论上不会，因为后端 version 单调增），不覆盖，
     * 避免异常顺序导致已读记忆回退、公告反复重弹。</p>
     *
     * <p>写失败只记日志，不阻断关闭流程 —— 记忆是增强，最坏后果是下次多弹一次。</p>
     */
    public static void markSeen(int announcementId, int version) {
        if (announcementId <= 0) {
            return;
        }
        try {
            Map<Integer, Integer> seen = readAll();
            Integer previous = seen.get(announcementId);
            if (previous != null && previous >= version) {
                return;
            }
            seen.put(announcementId, version);
            kv().encode(KEY_SEEN_MAP, GSON.toJson(seen));
            EasyLog.print(TAG, "已记公告已读: id=" + announcementId + ", version=" + version);
        } catch (Throwable error) {
            EasyLog.print(TAG, "写入公告已读表失败（不阻断关闭）");
            EasyLog.print(error);
        }
    }
}
