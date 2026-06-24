/*
 * 项目名: AndroidProject
 * 类名: ContentSearchStrategy.java
 * 包名: run.yigou.gxzy.ui.reader.search
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search;

import android.util.Pair;

import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;

import java.util.List;

/**
 * 内容搜索策略接口
 * 
 * <p>设计意图：
 * <ul>
 *   <li>定义统一的搜索策略接口，支持方剂/药物/名词三种内容类型</li>
 *   <li>实现策略模式，便于未来新增搜索类型无需修改现有代码（OCP）</li>
 *   <li>每个策略可独立单元测试</li>
 * </ul>
 * 
 * <p>使用方式：
 * <pre>{@code
 *   ContentSearchStrategy strategy = new FangSearchStrategy(repository, bookId);
 *   Pair<List<GroupData>, List<List<ItemData>>> result = strategy.search("关键词");
 * }</pre>
 */
public interface ContentSearchStrategy {
    
    /**
     * 搜索内容
     * 
     * @param keyword 搜索关键词
     * @return Pair<groups, items> 搜索结果，groups 为分组列表，items 为每个分组的数据项列表
     */
    Pair<List<GroupData>, List<List<ItemData>>> search(String keyword);
}
