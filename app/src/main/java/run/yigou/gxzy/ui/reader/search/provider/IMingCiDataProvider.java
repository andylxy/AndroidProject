/*
 * 项目名: AndroidProject
 * 类名: IMingCiDataProvider.java
 * 包名: run.yigou.gxzy.ui.reader.search.provider
 * 作者 : AI Assistant
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import run.yigou.gxzy.data.model.MingCiContent;

/**
 * 名词数据提供者接口
 * 
 * <p>职责：提供名词内容查询能力，解耦策略类与 GlobalDataHolder。
 * 
 * <p>设计意图：
 * <ul>
 *   <li>符合依赖倒置原则（DIP）：策略类依赖接口而非具体实现</li>
 *   <li>支持单元测试：可传入 Mock 实现</li>
 *   <li>支持任意数据源：内存、数据库、网络、缓存</li>
 * </ul>
 */
public interface IMingCiDataProvider {
    
    /**
     * 获取名词内容映射
     * 
     * @return 名词名称到内容的映射（不可为 null）
     */
    @NonNull
    Map<String, MingCiContent> getMingCiContentMap();
    
    /**
     * 查询指定名词的内容
     * 
     * @param name 名词名称
     * @return 名词内容，如果不存在返回 null
     */
    @Nullable
    MingCiContent getMingCiContent(@NonNull String name);
    
    /**
     * 名词数据是否已加载
     * 
     * @return true 表示数据已加载（即使为空），false 表示尚未加载
     */
    boolean isDataLoaded();
}
