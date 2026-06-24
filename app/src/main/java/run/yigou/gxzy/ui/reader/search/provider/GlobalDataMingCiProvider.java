/*
 * 项目名: AndroidProject
 * 类名: GlobalDataMingCiProvider.java
 * 包名: run.yigou.gxzy.ui.reader.search.provider
 * 作者 : AI Assistant
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.data.model.MingCiContent;

/**
 * GlobalDataHolder 名词数据提供者实现
 * 
 * <p>职责：将 GlobalDataHolder 适配为 IMingCiDataProvider 接口。
 * 
 * <p>设计意图：
 * <ul>
 *   <li>包装单例调用，对外暴露接口</li>
 *   <li>保持向后兼容，不修改 GlobalDataHolder</li>
 *   <li>为未来替换数据源做准备</li>
 * </ul>
 */
public class GlobalDataMingCiProvider implements IMingCiDataProvider {
    
    private final GlobalDataHolder holder;
    
    /**
     * 构造函数
     * 
     * @param holder GlobalDataHolder 实例
     */
    public GlobalDataMingCiProvider(@NonNull GlobalDataHolder holder) {
        this.holder = holder;
    }
    
    @NonNull
    @Override
    public Map<String, MingCiContent> getMingCiContentMap() {
        return holder.getMingCiContentMap();
    }
    
    @Nullable
    @Override
    public MingCiContent getMingCiContent(@NonNull String name) {
        return holder.getMingCiContent(name);
    }
    
    @Override
    public boolean isDataLoaded() {
        return holder.isMingCiDataLoaded();
    }
}
