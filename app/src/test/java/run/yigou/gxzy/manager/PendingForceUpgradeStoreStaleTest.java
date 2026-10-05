package run.yigou.gxzy.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import run.yigou.gxzy.data.remote.model.UpdateInfo;

import org.junit.Test;

/**
 * 欠升级记录失效判据的单元测试（ADR-0001 追加决议，2026-10-05）。
 *
 * <p>纯 JVM：被测的 {@link PendingForceUpgradeStore#isStale} 刻意不碰 MMKV / Android，
 * 且 MMKV 实例放在 holder 里延迟加载，所以调用它不会触发 native 库加载
 * （昨天的同类教训：把纯逻辑放进持有 Android 依赖的类里，单测直接
 * {@code ExceptionInInitializerError}）。</p>
 *
 * <p>这条判据错了的后果不对称：判成「未过期」会让<b>已升级的用户被永久拦着读不了书</b>，
 * 判成「已过期」只是少弹一次框。所以每个边界都要钉死。</p>
 *
 * <p>用 Gson 从 JSON 构造 {@link UpdateInfo}（它没有 setter，是只读响应模型），
 * 顺带验证字段名映射 —— 该模型刻意不加 {@code @SerializedName}，字段名一旦与后端
 * JSON 不符就会静默变成 0/false，而那正好会把记录判成「已失效」。</p>
 */
public class PendingForceUpgradeStoreStaleTest {

    private static final Gson GSON = new Gson();

    private static UpdateInfo force(int minVersionCode) {
        return GSON.fromJson(
                "{\"latestVersionCode\":20,\"latestVersionName\":\"2.0\","
                        + "\"minVersionCode\":" + minVersionCode + ",\"force\":true}",
                UpdateInfo.class);
    }

    @Test
    public void gsonMapsFieldsAsExpected() {
        // 先钉住映射本身：若这条挂了，下面所有断言都失去意义（会静默按 0/false 走）。
        UpdateInfo info = force(20);
        assertTrue(info.isForce());
        assertEquals(20, info.getMinVersionCode());
        assertEquals(20, info.getLatestVersionCode());
        assertEquals("2.0", info.getLatestVersionName());
    }

    @Test
    public void keepsRecordWhenStillBelowFloor() {
        // 本机 10，记录要求 >= 20：确实欠升级，必须继续拦。
        assertFalse(PendingForceUpgradeStore.isStale(force(20), 10));
    }

    @Test
    public void expiresWhenReachingFloorExactly() {
        // 相等即达标 —— 后端地板是「最低支持版本」，等于它就是被支持的。
        assertTrue(PendingForceUpgradeStore.isStale(force(20), 20));
    }

    @Test
    public void expiresWhenAboveFloor() {
        assertTrue(PendingForceUpgradeStore.isStale(force(20), 21));
    }

    @Test
    public void treatsNullAsExpired() {
        assertTrue(PendingForceUpgradeStore.isStale(null, 10));
    }

    @Test
    public void expiresWhenRecordIsNotAForceUpgrade() {
        // 只存了软更新信息（force=false）时不该拦任何阅读。
        UpdateInfo soft = GSON.fromJson(
                "{\"latestVersionCode\":20,\"minVersionCode\":20,\"force\":false}",
                UpdateInfo.class);
        assertTrue(PendingForceUpgradeStore.isStale(soft, 10));
    }

    @Test
    public void handlesZeroFloorWithoutBlocking() {
        // minVersionCode=0 且 force=true 是自相矛盾的组合（没有低于 0 的版本），
        // 按「已达标」处理，避免这种脏记录永久拦住阅读。
        assertTrue(PendingForceUpgradeStore.isStale(force(0), 10));
    }

    @Test
    public void treatsClientAboveNegativeFloorAsExpired() {
        // 地板 -1、本机 0：本机在地板**之上**，记录应失效。
        // 这条锁定比较方向是「本机 >= 地板 → 失效」而不是反过来（反过来会让
        // 负地板场景把所有人都判成欠升级）。
        assertTrue(PendingForceUpgradeStore.isStale(force(-1), 0));
    }

    @Test
    public void keepsRecordWhenClientBelowFloorIncludingNegative() {
        // 地板 5、本机 3（含负数客户端版本号也照样按同一规则比较）。
        assertFalse(PendingForceUpgradeStore.isStale(force(5), 3));
        assertFalse(PendingForceUpgradeStore.isStale(force(5), -2));
    }
}
