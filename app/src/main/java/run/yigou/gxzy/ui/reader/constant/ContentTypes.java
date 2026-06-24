/*
 * 项目名: AndroidProject
 * 类名: ContentTypes.java
 * 包名: run.yigou.gxzy.ui.reader.constant
 * 创建时间 : 2026年01月
 * Copyright (c) 2026 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.constant;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 内容类型常量类
 * 
 * 统一用于：
 * - 配置中心 linkType（StyleConfig.linkType）
 * - 点击处理路由（TipsClickHandler.handleClick）
 * - 列表展示类型（TipsFangYaoFragment.contentType）
 * 
 * <p>设计原则：
 * <ul>
 *   <li>值从 1 开始（0 保留给"未知类型"）</li>
 *   <li>使用 @IntDef 而非枚举（Android 性能最佳实践）</li>
 *   <li>预留扩展空间（视频/音频/图片等）</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * // 1. 方法参数类型校验
 * public void handleClick(@ContentTypes.ContentType int contentType) {
 *     switch (contentType) {
 *         case ContentTypes.YAO:
 *             // 处理药物
 *             break;
 *         case ContentTypes.FANG:
 *             // 处理方剂
 *             break;
 *     }
 * }
 * 
 * // 2. 字段类型声明
 * &#64;ContentTypes.ContentType
 * private int contentType;
 * 
 * // 3. Fragment 创建
 * TipsFangYaoFragment fragment = TipsFangYaoFragment.newInstance(
 *     ContentTypes.FANG, bookId
 * );
 * </pre>
 */
public final class ContentTypes {
    
    private ContentTypes() {
        // 工具类禁止实例化
    }
    
    // ==================== 内容类型常量 ====================
    
    /** 药物（对应配置中心 linkType=1，文本标记 $u{}） */
    public static final int YAO = 1;
    
    /** 方剂（对应配置中心 linkType=2，文本标记 $f{}） */
    public static final int FANG = 2;
    
    /** 名词（对应配置中心 linkType=3，文本标记 $m{}） */
    public static final int MING_CI = 3;
    
    /** 汉制单位（仅用于列表展示，无链接点击） */
    public static final int HAN_ZHI_UNIT = 4;
    
    // ==================== 预留扩展类型 ====================
    
    /** 视频（预留） */
    public static final int VIDEO = 5;
    
    /** 音频（预留） */
    public static final int AUDIO = 6;
    
    /** 图片（预留） */
    public static final int IMAGE = 7;
    
    // ==================== 类型注解 ====================
    
    /**
     * 内容类型注解（用于方法参数校验）
     * 
     * 编译期检查：如果传入未定义的值，IDE 会显示警告
     */
    @IntDef({YAO, FANG, MING_CI, HAN_ZHI_UNIT, VIDEO, AUDIO, IMAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentType {
    }
}
