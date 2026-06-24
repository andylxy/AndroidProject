/*
 * 项目名: AndroidProject
 * 类名: GlobalDataYaoProvider.java
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
import run.yigou.gxzy.data.model.Yao;

/**
 * GlobalDataHolder 药物数据提供者实现
 * 
 * <p>职责：将 GlobalDataHolder 适配为 IYaoDataProvider 接口。
 */
public class GlobalDataYaoProvider implements IYaoDataProvider {
    
    private final GlobalDataHolder holder;
    
    /**
     * 构造函数
     * 
     * @param holder GlobalDataHolder 实例
     */
    public GlobalDataYaoProvider(@NonNull GlobalDataHolder holder) {
        this.holder = holder;
    }
    
    @NonNull
    @Override
    public Map<String, Yao> getYaoMap() {
        return holder.getYaoMap();
    }
    
    @NonNull
    @Override
    public Map<String, String> getYaoAliasDict() {
        return holder.getYaoAliasDict();
    }
    
    @Nullable
    @Override
    public Yao getYao(@NonNull String name) {
        return holder.getYao(name);
    }
    
    @Override
    public boolean isDataLoaded() {
        return holder.isYaoDataLoaded();
    }
}
