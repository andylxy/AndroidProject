package run.yigou.gxzy.network.security;

import com.tencent.mmkv.MMKV;

import java.util.UUID;

import run.yigou.gxzy.log.EasyLog;

/**
 * 设备标识存储（spec §7「设备 ID 决策」）。
 *
 * <p>首次调用生成一个 UUID 并持久化到 MMKV，此后恒定返回同一个值——后端
 * {@code ext_user_devices} 以 {@code (user_id, device_id)} 为键做设备登记与吊销，
 * 标识必须跨进程重启稳定。卸载重装会清空私有存储、生成新标识（这是既定语义，
 * 也正因如此「设备吊销」不是安防边界，见 ADR-0001）。</p>
 *
 * <p>格式：去掉连字符的 32 位十六进制，满足后端
 * {@code ^[A-Za-z0-9_-]+$} 且长度 ≤64 的校验（{@code src/server/rbac/resolve.ts}）。
 * 若把带连字符的 UUID 原样上报也合法，但统一格式更便于排查。</p>
 *
 * <p><b>前置条件</b>：MMKV 必须已初始化（{@code AppApplication.initUtilsAndServices}
 * 在 {@code onCreate} 里完成，早于任何网络请求）。因此本类不需要 Context 参数——
 * {@code MMKV.mmkvWithID(id)} 取的是进程级单例。</p>
 */
public final class DeviceIdStore {

    /** 日志 tag（EasyLog 走 android.util.Log.i）。 */
    private static final String TAG = "DeviceIdStore";

    /** 独立的 MMKV 实例，避免与 http 缓存等实例的键空间混淆。 */
    private static final String MMKV_ID = "device_identity";

    private static final String KEY_DEVICE_ID = "key_device_id";

    /** 进程内缓存，避免每次请求都走一次 MMKV 读取。 */
    private static volatile String sCachedDeviceId;

    private DeviceIdStore() {
    }

    /** 返回本机稳定设备标识；不存在时生成并持久化。 */
    public static String get() {
        String cached = sCachedDeviceId;
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        synchronized (DeviceIdStore.class) {
            if (sCachedDeviceId != null && !sCachedDeviceId.isEmpty()) {
                return sCachedDeviceId;
            }
            String deviceId = loadOrCreate();
            sCachedDeviceId = deviceId;
            return deviceId;
        }
    }

    private static String loadOrCreate() {
        try {
            MMKV mmkv = MMKV.mmkvWithID(MMKV_ID);
            String stored = mmkv.decodeString(KEY_DEVICE_ID, null);
            if (stored != null && !stored.isEmpty()) {
                return stored;
            }
            String created = newDeviceId();
            mmkv.encode(KEY_DEVICE_ID, created);
            return created;
        } catch (Throwable error) {
            // MMKV 不可用（极早期调用 / 初始化失败）时退化为进程内标识：仍然合法，
            // 只是重启后会变——不能因为取标识失败而让整个请求链断掉。
            // 必须留日志：静默降级会让「设备标识不稳定」这种问题无法从 logcat 发现。
            EasyLog.print(TAG, "MMKV 不可用，退化为进程内临时设备标识（重启会变）: " + error);
            return newDeviceId();
        }
    }

    /** 32 位十六进制随机标识（UUID 去掉连字符）。 */
    private static String newDeviceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
