/*
 * 项目名: AndroidProject
 * 类名: SearchDataAdapter.java
 * 包名: run.yigou.gxzy.ui.reader.search
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search;

import android.util.Pair;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.ui.reader.constant.ContentTypes;
import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;
import run.yigou.gxzy.ui.reader.repository.BookRepository;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.ui.reader.search.provider.IMingCiDataProvider;
import run.yigou.gxzy.ui.reader.search.provider.IYaoDataProvider;
import run.yigou.gxzy.ui.reader.search.provider.IFangDataProvider;
import run.yigou.gxzy.ui.reader.search.provider.GlobalDataMingCiProvider;
import run.yigou.gxzy.ui.reader.search.provider.GlobalDataYaoProvider;
import run.yigou.gxzy.ui.reader.search.provider.GlobalDataFangProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索数据适配器 - 统一处理方剂/药物/名词搜索
 * 
 * <p>【重构后】仅作为协调器，委托给具体策略实现
 * <ul>
 *   <li>方剂搜索 → FangSearchStrategy</li>
 *   <li>药物搜索 → YaoSearchStrategy</li>
 *   <li>名词搜索 → MingCiSearchStrategy</li>
 * </ul>
 * 
 * <p>设计意图：
 * <ul>
 *   <li>应用策略模式，消除 690 行单一类的职责混乱</li>
 *   <li>每个搜索策略独立职责，可单独测试</li>
 *   <li>新增搜索类型无需修改现有代码（OCP）</li>
 * </ul>
 * 
 * <p>使用方式：
 * <pre>{@code
 *   SearchDataAdapter adapter = new SearchDataAdapter(repository, bookId);
 *   
 *   // 方剂搜索
 *   Pair<List<GroupData>, List<List<ItemData>>> fangResult = adapter.searchFangContent("桂枝汤");
 *   
 *   // 药物搜索
 *   Pair<List<GroupData>, List<List<ItemData>>> yaoResult = adapter.searchYaoContent("桂枝");
 *   
 *   // 名词搜索
 *   Pair<List<GroupData>, List<List<ItemData>>> mingCiResult = adapter.searchMingCiContent("伤寒");
 * }</pre>
 */
public class SearchDataAdapter {
    
    /**
     * 搜索策略映射表
     */
    private final Map<Integer, ContentSearchStrategy> strategies;
    
    /**
     * 构造函数
     * 
     * @param bookRepository 书籍仓库
     * @param bookId 当前书籍ID
     */
    public SearchDataAdapter(BookRepository bookRepository, int bookId) {
        EasyLog.print("=== SearchDataAdapter 初始化（策略模式） ===");
        EasyLog.print("BookId: " + bookId);
        
        // 创建数据提供者
        GlobalDataHolder holder = GlobalDataHolder.getInstance();
        IMingCiDataProvider mingCiProvider = new GlobalDataMingCiProvider(holder);
        IYaoDataProvider yaoProvider = new GlobalDataYaoProvider(holder);
        IFangDataProvider fangProvider = new GlobalDataFangProvider(holder);
        
        // 注册策略（注入数据提供者）
        strategies = new HashMap<>();
        strategies.put(ContentTypes.FANG, new FangSearchStrategy(bookRepository, bookId, fangProvider));
        strategies.put(ContentTypes.YAO, new YaoSearchStrategy(bookRepository, bookId, yaoProvider));
        strategies.put(ContentTypes.MING_CI, new MingCiSearchStrategy(mingCiProvider));
        
        EasyLog.print("已注册策略: 方剂(FANG), 药物(YAO), 名词(MING_CI)");
    }
    
    /**
     * 搜索方剂相关内容
     * 
     * @param keyword 搜索关键词
     * @return Pair<groups, items> 搜索结果
     */
    public Pair<List<GroupData>, List<List<ItemData>>> searchFangContent(String keyword) {
        return strategies.get(ContentTypes.FANG).search(keyword);
    }
    
    /**
     * 搜索药物相关内容
     * 
     * @param keyword 搜索关键词
     * @return Pair<groups, items> 搜索结果
     */
    public Pair<List<GroupData>, List<List<ItemData>>> searchYaoContent(String keyword) {
        return strategies.get(ContentTypes.YAO).search(keyword);
    }
    
    /**
     * 搜索名词相关内容
     * 
     * @param keyword 搜索关键词
     * @return Pair<groups, items> 搜索结果
     */
    public Pair<List<GroupData>, List<List<ItemData>>> searchMingCiContent(String keyword) {
        return strategies.get(ContentTypes.MING_CI).search(keyword);
    }
}
