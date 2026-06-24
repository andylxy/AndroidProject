/*
 * 项目名: AndroidProject
 * 类名: GlobalDataFangProvider.java
 * 包名: run.yigou.gxzy.ui.reader.search.provider
 * 作者 : AI Assistant
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search.provider;

import androidx.annotation.NonNull;

import java.util.Map;

import run.yigou.gxzy.base.GlobalDataHolder;

/**
 * GlobalDataHolder 方剂数据提供者实现
 * 
 * <p>职责：将 GlobalDataHolder 适配为 IFangDataProvider 接口。
 */
public class GlobalDataFangProvider implements IFangDataProvider {
    
    private final GlobalDataHolder holder;
    
    /**
     * 构造函数
     * 
     * @param holder GlobalDataHolder 实例
     */
    public GlobalDataFangProvider(@NonNull GlobalDataHolder holder) {
        this.holder = holder;
    }
    
    @NonNull
    @Override
    public Map<String, String> getFangAliasDict() {
        return holder.getFangAliasDict();
    }
    
    @Override
    public boolean isDataLoaded() {
        return holder.isFangAliasLoaded();
    }
}
