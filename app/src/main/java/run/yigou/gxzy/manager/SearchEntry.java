package run.yigou.gxzy.manager;

/**
 * 搜索入口的种类。设计依据见 microfeed 仓 {@code .scratch/search-permission/DESIGN.md} §5.2。
 *
 * <p>两个受管入口各对应一个权限布尔：</p>
 * <ul>
 *   <li>{@link #GLOBAL} —— 首页全局搜索（{@code HomeFragment.search()}）；</li>
 *   <li>{@link #BOOK} —— 书内阅读页搜索（{@code TipsBookNetReadFragment}）。</li>
 * </ul>
 *
 * <p>{@code SearchPermissionManager.isSearchAllowed(entry)} 据此返回对应入口是否被允许。</p>
 */
public enum SearchEntry {
    GLOBAL,
    BOOK
}
