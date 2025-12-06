package run.yigou.gxzy.greendao.util;

/**
 * 数据库版本管理器
 * 用于跟踪和管理数据库模式版本
 * 
 * 版本历史:
 * V1 - 初始版本
 * V2 - 为USER_INFO表添加ACCESS_KEY_ID等字段
 * ...
 * 
 * 注意: 每次对GreenDao实体进行结构性更改(添加/删除字段、添加新实体等)时，
 * 都应该增加此版本号，并相应地实现升级逻辑
 */
public class DatabaseVersionManager {
    // 当前数据库版本号
    // 每次对GreenDao实体进行结构性更改时都应该增加此数字
    public static final int CURRENT_VERSION = 2; // 更新版本号以触发升级
    
    /**
     * 获取当前数据库版本号
     * @return 当前数据库版本号
     */
    public static int getCurrentVersion() {
        return CURRENT_VERSION;
    }
    
    /**
     * 检查是否需要升级数据库
     * @param currentVersion 当前数据库版本
     * @return 如果需要升级则返回true，否则返回false
     */
    public static boolean isUpgradeNeeded(int currentVersion) {
        return currentVersion < getCurrentVersion();
    }
}