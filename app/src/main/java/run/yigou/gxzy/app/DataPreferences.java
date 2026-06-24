/*
 * 项目名: AndroidProject
 * 类名: DataPreferences.java
 * 包名: run.yigou.gxzy.app
 * 作者 : AI Assistant
 * 当前修改时间 : 2026年01月22日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.app;

import com.tencent.mmkv.MMKV;

import run.yigou.gxzy.log.EasyLog;

/**
 * 数据更新配置管理类
 * 
 * <p>使用 MMKV 存储数据更新相关配置
 * <p>管理数据更新频率和上次更新时间
 * 
 * <p>设计原则：
 * <ul>
 *   <li>所有方法为 static，无需实例化</li>
 *   <li>使用独立的 MMKV 空间（data_update_config）</li>
 *   <li>提供默认值保护（默认每周更新）</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * // 获取当前更新频率
 * DataUpdateFrequency frequency = DataPreferences.getUpdateFrequency();
 * 
 * // 设置新的更新频率
 * DataPreferences.setUpdateFrequency(DataUpdateFrequency.DAILY);
 * 
 * // 记录更新时间
 * DataPreferences.setLastUpdateTime(System.currentTimeMillis());
 * </pre>
 */
public class DataPreferences {
    
    private static final String TAG = "DataPreferences";
    
    // MMKV 配置
    private static final String MMKV_ID = "data_update_config";
    private static final String KEY_UPDATE_FREQUENCY = "update_frequency";
    private static final String KEY_LAST_UPDATE_TIME = "last_update_time";
    
    // MMKV 实例
    private static final MMKV mmkv = MMKV.mmkvWithID(MMKV_ID);
    
    /**
     * 私有构造方法，防止实例化
     */
    private DataPreferences() {
    }
    
    /**
     * 获取数据更新频率
     * 
     * @return 更新频率（默认每周一次）
     */
    public static DataUpdateFrequency getUpdateFrequency() {
        int code = mmkv.decodeInt(KEY_UPDATE_FREQUENCY, 
            DataUpdateFrequency.WEEKLY.getCode());
        DataUpdateFrequency frequency = DataUpdateFrequency.fromCode(code);
        
        EasyLog.print(TAG, "📊 读取更新频率配置: " + frequency.getDisplayName());
        return frequency;
    }
    
    /**
     * 设置数据更新频率
     * 
     * @param frequency 更新频率
     */
    public static void setUpdateFrequency(DataUpdateFrequency frequency) {
        mmkv.encode(KEY_UPDATE_FREQUENCY, frequency.getCode());
        EasyLog.print(TAG, "💾 保存更新频率配置: " + frequency.getDisplayName());
    }
    
    /**
     * 获取上次更新时间
     * 
     * @return 时间戳（毫秒），0 表示从未更新
     */
    public static long getLastUpdateTime() {
        long time = mmkv.decodeLong(KEY_LAST_UPDATE_TIME, 0L);
        EasyLog.print(TAG, "📊 读取上次更新时间: " + time);
        return time;
    }
    
    /**
     * 设置上次更新时间
     * 
     * @param time 时间戳（毫秒）
     */
    public static void setLastUpdateTime(long time) {
        mmkv.encode(KEY_LAST_UPDATE_TIME, time);
        EasyLog.print(TAG, "💾 保存更新时间: " + time);
    }
}
