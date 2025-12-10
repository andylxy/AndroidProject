/*
 * 项目名: AndroidProject
 * 类名: SpannableStringCache.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.utils
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: SpannableString缓存管理器 - 使用LruCache避免重复创建SpannableString
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.utils;

import android.text.SpannableStringBuilder;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SpannableString缓存管理器
 * 使用LruCache缓存已创建的SpannableString,避免重复创建和渲染
 */
public class SpannableStringCache {

    // 缓存大小: 最多缓存200个SpannableString对象
    private static final int DEFAULT_CACHE_SIZE = 200;

    // LruCache实例
    private final LruCache<String, SpannableStringBuilder> cache;

    /**
     * 构造函数 - 使用默认缓存大小
     */
    public SpannableStringCache() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * 构造函数 - 自定义缓存大小
     *
     * @param maxSize 最大缓存数量
     */
    public SpannableStringCache(int maxSize) {
        this.cache = new LruCache<String, SpannableStringBuilder>(maxSize) {
            @Override
            protected int sizeOf(String key, SpannableStringBuilder value) {
                // 每个条目占用1个单位
                return 1;
            }
        };
    }

    /**
     * 从缓存中获取SpannableString
     *
     * @param key 缓存键
     * @return SpannableString对象,如果不存在返回null
     */
    @Nullable
    public SpannableStringBuilder get(@NonNull String key) {
        return cache.get(key);
    }

    /**
     * 将SpannableString存入缓存
     *
     * @param key   缓存键
     * @param value SpannableString对象
     */
    public void put(@NonNull String key, @NonNull SpannableStringBuilder value) {
        cache.put(key, value);
    }

    /**
     * 生成缓存键
     * 格式: "type_groupPos_childPos"
     *
     * @param type          类型(text/note/video/header)
     * @param groupPosition 组位置
     * @param childPosition 子项位置(-1表示header)
     * @return 缓存键
     */
    @NonNull
    public static String generateKey(@NonNull String type, int groupPosition, int childPosition) {
        return type + "_" + groupPosition + "_" + childPosition;
    }

    /**
     * 生成缓存键 - 用于Header
     *
     * @param groupPosition 组位置
     * @return 缓存键
     */
    @NonNull
    public static String generateHeaderKey(int groupPosition) {
        return generateKey("header", groupPosition, -1);
    }

    /**
     * 生成缓存键 - 用于Child文本
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @return 缓存键
     */
    @NonNull
    public static String generateChildTextKey(int groupPosition, int childPosition) {
        return generateKey("text", groupPosition, childPosition);
    }

    /**
     * 生成缓存键 - 用于Child注释
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @return 缓存键
     */
    @NonNull
    public static String generateChildNoteKey(int groupPosition, int childPosition) {
        return generateKey("note", groupPosition, childPosition);
    }

    /**
     * 生成缓存键 - 用于Child视频
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @return 缓存键
     */
    @NonNull
    public static String generateChildVideoKey(int groupPosition, int childPosition) {
        return generateKey("video", groupPosition, childPosition);
    }

    /**
     * 移除指定缓存
     *
     * @param key 缓存键
     */
    public void remove(@NonNull String key) {
        cache.remove(key);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.evictAll();
    }

    /**
     * 获取缓存命中率
     *
     * @return 命中率(0.0-1.0)
     */
    public float getHitRate() {
        int hitCount = cache.hitCount();
        int missCount = cache.missCount();
        int total = hitCount + missCount;
        return total == 0 ? 0f : (float) hitCount / total;
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存中的条目数量
     */
    public int size() {
        return cache.size();
    }

    /**
     * 获取最大缓存大小
     *
     * @return 最大缓存数量
     */
    public int maxSize() {
        return cache.maxSize();
    }
}
