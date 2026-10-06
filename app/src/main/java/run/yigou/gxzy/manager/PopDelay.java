package run.yigou.gxzy.manager;

import java.util.Random;

/**
 * 启动后到弹窗之间的等待时长（纯逻辑，可 JVM 单测）。
 *
 * <p>与存储、节流都无关，故独立成类而不是挂在 {@code AnnouncementStore} 上。</p>
 */
final class PopDelay {

    /** 下限：启动完成后至少等这么久。 */
    static final int MIN_MS = 15_000;

    /** 上限：最多等这么久。 */
    static final int MAX_MS = 25_000;

    private PopDelay() {
    }

    /**
     * 在 `[MIN_MS, MAX_MS]` 内随机取一个时长。
     *
     * <p>随机而非固定：固定值让每次启动都在同一刻弹，观感像闹钟。
     * 延迟的用意是让用户先看到主界面，而不是一进 App 就被框住。</p>
     *
     * @param random 取值来源（生产传 `new Random()`，单测传固定实现以获得确定值）
     */
    static int nextMs(Random random) {
        // +1 是为了含上区间上界；nextInt 要求上界为正。
        return MIN_MS + random.nextInt(MAX_MS - MIN_MS + 1);
    }
}
