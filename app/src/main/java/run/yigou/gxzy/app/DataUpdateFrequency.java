/*
 * 项目名: AndroidProject
 * 类名: DataUpdateFrequency.java
 * 包名: run.yigou.gxzy.app
 * 作者 : AI Assistant
 * 当前修改时间 : 2026年01月22日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.app;

/**
 * 数据更新频率枚举
 * 
 * <p>定义用户可配置的数据更新周期
 * <p>用于控制名词、药物、方剂等核心业务数据的自动更新频率
 * 
 * <p>使用示例：
 * <pre>
 * // 获取当前配置
 * DataUpdateFrequency frequency = DataPreferences.getUpdateFrequency();
 * 
 * // 设置新频率
 * DataPreferences.setUpdateFrequency(DataUpdateFrequency.DAILY);
 * </pre>
 */
public enum DataUpdateFrequency {
    
    /**
     * 每次启动时更新
     */
    EVERY_START(0, "每次启动"),
    
    /**
     * 每天更新一次
     */
    DAILY(1, "每天一次", 86400000L),
    
    /**
     * 每周更新一次（默认）
     */
    WEEKLY(2, "每周一次", 604800000L),
    
    /**
     * 每月更新一次
     */
    MONTHLY(3, "每月一次", 2592000000L),
    
    /**
     * 仅手动更新
     */
    MANUAL(4, "手动更新");
    
    /**
     * 序号（用于 MMKV 存储）
     */
    private final int code;
    
    /**
     * 显示文本
     */
    private final String displayName;
    
    /**
     * 间隔时间（毫秒）
     * <p>-1 表示不需要自动更新（EVERY_START、MANUAL）
     */
    private final long intervalMillis;
    
    /**
     * 构造函数（无时间间隔）
     */
    DataUpdateFrequency(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
        this.intervalMillis = -1;
    }
    
    /**
     * 构造函数（有时间间隔）
     */
    DataUpdateFrequency(int code, String displayName, long intervalMillis) {
        this.code = code;
        this.displayName = displayName;
        this.intervalMillis = intervalMillis;
    }
    
    /**
     * 获取序号
     * 
     * @return 序号
     */
    public int getCode() {
        return code;
    }
    
    /**
     * 获取显示文本
     * 
     * @return 显示文本
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取间隔时间
     * 
     * @return 间隔时间（毫秒），-1 表示不需要自动更新
     */
    public long getIntervalMillis() {
        return intervalMillis;
    }
    
    /**
     * 根据 code 获取枚举
     * 
     * @param code 序号
     * @return 对应的枚举值，未匹配时返回默认 WEEKLY
     */
    public static DataUpdateFrequency fromCode(int code) {
        for (DataUpdateFrequency frequency : values()) {
            if (frequency.code == code) {
                return frequency;
            }
        }
        return WEEKLY; // 默认每周
    }
}
