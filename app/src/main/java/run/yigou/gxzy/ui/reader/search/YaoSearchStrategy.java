/*
 * 项目名: AndroidProject
 * 类名: YaoSearchStrategy.java
 * 包名: run.yigou.gxzy.ui.reader.search
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search;

import android.text.SpannableStringBuilder;
import android.util.Pair;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.ui.reader.data.BookData;
import run.yigou.gxzy.ui.reader.data.ChapterData;
import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;
import run.yigou.gxzy.ui.reader.repository.BookRepository;
import run.yigou.gxzy.ui.reader.helper.TipsClickHandler;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.ui.reader.search.provider.IYaoDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 药物搜索策略
 * 
 * <p>职责：实现药物内容搜索逻辑
 * <ul>
 *   <li>查询药物详细信息（通过 IYaoDataProvider）</li>
 *   <li>在章节中搜索包含该药物的内容</li>
 *   <li>支持别名解析</li>
 * </ul>
 * 
 * <p>搜索流程：
 * <ol>
 *   <li>检查数据加载状态</li>
 *   <li>解析别名</li>
 *   <li>查询药物信息（显示药物详情）</li>
 *   <li>在章节中搜索包含该药物的内容</li>
 * </ol>
 */
public class YaoSearchStrategy implements ContentSearchStrategy {
    
    private final BookRepository bookRepository;
    private final int bookId;
    private final BookData bookData;
    private final List<Chapter> chapters;
    private final IYaoDataProvider yaoProvider;
    
    private final FangNameMatcher matcher;
    private final SearchResultBuilder builder;
    
    /**
     * 构造函数
     * 
     * @param bookRepository 书籍仓库
     * @param bookId 当前书籍ID
     * @param yaoProvider 药物数据提供者（依赖注入）
     */
    public YaoSearchStrategy(BookRepository bookRepository, int bookId, IYaoDataProvider yaoProvider) {
        this.bookRepository = bookRepository;
        this.bookId = bookId;
        this.bookData = bookRepository.getBookData(bookId);
        this.chapters = bookRepository.getChapters(bookId);
        this.yaoProvider = yaoProvider;
        
        this.matcher = new FangNameMatcher();
        this.builder = new SearchResultBuilder();
    }
    
    @Override
    public Pair<List<GroupData>, List<List<ItemData>>> search(String keyword) {
        EasyLog.print("=== YaoSearchStrategy.search() ===");
        EasyLog.print("药物关键字: " + keyword);
        
        if (bookData == null || chapters == null) {
            EasyLog.print("❌ 数据源不可用");
            return builder.notFoundYao();
        }
        
        // 1. 查询药物信息
        Pair<List<GroupData>, List<List<ItemData>>> result = searchYaoInfo(keyword);
        
        // 2. 在章节中搜索
        searchYaoInChapters(keyword, result);
        
        EasyLog.print("匹配章节: " + (result.second.size() - 1)); // -1 因为第一个是药物信息
        return result;
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 查询药物详细信息
     */
    private Pair<List<GroupData>, List<List<ItemData>>> searchYaoInfo(String keyword) {
        List<GroupData> groups = new ArrayList<>();
        List<List<ItemData>> items = new ArrayList<>();
        
        // 检查数据加载状态
        if (!yaoProvider.isDataLoaded()) {
            EasyLog.print("⚠️ 药物数据未加载");
            return builder.notLoaded();
        }
        
        // 通过接口查询数据
        Map<String, String> aliasDict = yaoProvider.getYaoAliasDict();
        String aliasName = matcher.resolveAlias(aliasDict, keyword);
        
        Yao yao = yaoProvider.getYao(aliasName);
        
        if (yao != null) {
            GroupData group = builder.buildGroup("药物信息", true);
            groups.add(group);
            
            List<ItemData> itemList = new ArrayList<>();
            ItemData item = new ItemData();
            
            SpannableStringBuilder yaoText = TipsClickHandler.renderText("$x{" + yao.getName() + "}\n");
            if (yao.getAttributedText() != null) {
                yaoText.append(yao.getAttributedText());
            }
            item.setAttributedText(yaoText);
            itemList.add(item);
            items.add(itemList);
            
            EasyLog.print("✅ 找到药物: " + yao.getName());
        } else {
            EasyLog.print("⚠️ 未找到药物信息");
            // 添加未找到结果
            Pair<List<GroupData>, List<List<ItemData>>> notFound = builder.notFoundYao();
            groups.addAll(notFound.first);
            items.addAll(notFound.second);
        }
        
        return new Pair<>(groups, items);
    }
    
    /**
     * 在章节中搜索包含该药物的内容
     */
    private void searchYaoInChapters(String keyword, Pair<List<GroupData>, List<List<ItemData>>> result) {
        // 通过接口查询数据
        Map<String, String> aliasDict = yaoProvider.getYaoAliasDict();
        String aliasName = matcher.resolveAlias(aliasDict, keyword);
        
        int matchedSections = 0;
        
        for (Chapter chapter : chapters) {
            if (!chapter.getIsDownload()) {
                continue;
            }
            
            ChapterData chapterData = bookData.findChapterBySignature(chapter.getSignatureId());
            if (chapterData == null || !chapterData.isContentLoaded()) {
                // 如果内容未加载，触发加载
                if (chapterData != null && !chapterData.isContentLoaded()) {
                    bookRepository.loadChapterContent(bookData, chapter);
                }
                
                // 如果仍未加载，跳过
                if (chapterData == null || !chapterData.isContentLoaded()) {
                    continue;
                }
            }
            
            List<DataItem> content = chapterData.getContent();
            if (content == null) {
                continue;
            }
            
            List<DataItem> matchedItems = new ArrayList<>();
            
            for (DataItem dataItem : content) {
                List<String> yaoList = dataItem.getYaoList();
                if (yaoList == null || yaoList.isEmpty()) {
                    continue;
                }
                
                for (String yaoName : yaoList) {
                    String actualName = matcher.resolveAlias(aliasDict, yaoName);
                    if (actualName != null && actualName.equals(aliasName)) {
                        matchedItems.add(dataItem);
                        break;
                    }
                }
            }
            
            if (!matchedItems.isEmpty()) {
                GroupData group = builder.buildGroup(chapter.getChapterHeader(), true);
                result.first.add(group);
                
                List<ItemData> itemList = new ArrayList<>();
                for (DataItem item : matchedItems) {
                    itemList.add(builder.convertDataItem(item, false));
                }
                result.second.add(itemList);
                
                matchedSections++;
            }
        }
        
        EasyLog.print("匹配章节: " + matchedSections);
    }
}
