/*
 * 项目名: AndroidProject
 * 类名: MingCiSearchStrategy.java
 * 包名: run.yigou.gxzy.ui.reader.search
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search;

import android.text.SpannableStringBuilder;
import android.util.Pair;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;
import run.yigou.gxzy.ui.reader.helper.TipsClickHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 名词搜索策略
 * 
 * <p>职责：实现名词内容搜索逻辑
 * <ul>
 *   <li>查询名词定义（从 GlobalDataHolder.getMingCiContentMap()）</li>
 *   <li>显示名词解释或"未见此名词"提示</li>
 * </ul>
 * 
 * <p>搜索流程：
 * <ol>
 *   <li>查询名词定义</li>
 *   <li>返回名词解释或未找到提示</li>
 * </ol>
 */
public class MingCiSearchStrategy implements ContentSearchStrategy {
    
    private final SearchResultBuilder builder;
    
    /**
     * 构造函数
     */
    public MingCiSearchStrategy() {
        this.builder = new SearchResultBuilder();
    }
    
    @Override
    public Pair<List<GroupData>, List<List<ItemData>>> search(String keyword) {
        EasyLog.print("=== MingCiSearchStrategy.search() ===");
        EasyLog.print("名词关键字: " + keyword);
        
        // 获取名词定义
        Map<String, MingCiContent> mingCiMap = GlobalDataHolder.getInstance().getMingCiContentMap();
        MingCiContent mingCi = mingCiMap != null ? mingCiMap.get(keyword) : null;
        
        List<GroupData> groups = new ArrayList<>();
        List<List<ItemData>> items = new ArrayList<>();
        
        GroupData group = builder.buildGroup("名词解释", true);
        groups.add(group);
        
        List<ItemData> itemList = new ArrayList<>();
        ItemData item = new ItemData();
        
        if (mingCi != null) {
            SpannableStringBuilder text = TipsClickHandler.renderText("$x{" + mingCi.getName() + "}\n");
            if (mingCi.getAttributedText() != null) {
                text.append(mingCi.getAttributedText());
            }
            item.setAttributedText(text);
            EasyLog.print("✅ 找到名词: " + mingCi.getName());
        } else {
            item.setAttributedText(TipsClickHandler.renderText("$m{未见此名词。}"));
            EasyLog.print("⚠️ 未找到名词");
        }
        
        itemList.add(item);
        items.add(itemList);
        
        return new Pair<>(groups, items);
    }
}
