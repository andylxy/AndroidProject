/*
 * 项目名: AndroidProject
 * 类名: FangSearchStrategy.java
 * 包名: run.yigou.gxzy.ui.reader.search
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search;

import android.text.SpannableStringBuilder;
import android.util.Pair;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.ui.reader.data.BookData;
import run.yigou.gxzy.ui.reader.data.ChapterData;
import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;
import run.yigou.gxzy.ui.reader.repository.BookRepository;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.ui.reader.search.provider.IFangDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 方剂搜索策略
 * 
 * <p>职责：实现方剂内容搜索逻辑
 * <ul>
 *   <li>优先从方剂数据（BookData.getFangData()）中搜索</li>
 *   <li>备选从章节内容中搜索（当方剂数据未加载时）</li>
 *   <li>支持别名解析、多种匹配策略</li>
 * </ul>
 * 
 * <p>搜索流程：
 * <ol>
 *   <li>检查数据加载状态</li>
 *   <li>解析别名</li>
 *   <li>优先查方剂数据（精确匹配）</li>
 *   <li>备选查章节内容（宽松匹配）</li>
 *   <li>返回未找到结果（如均未匹配）</li>
 * </ol>
 */
public class FangSearchStrategy implements ContentSearchStrategy {
    
    private final BookRepository bookRepository;
    private final int bookId;
    private final BookData bookData;
    private final List<Chapter> chapters;
    private final IFangDataProvider fangProvider;
    
    private final FangNameMatcher matcher;
    private final SearchResultBuilder builder;
    
    /**
     * 构造函数
     * 
     * @param bookRepository 书籍仓库
     * @param bookId 当前书籍ID
     * @param fangProvider 方剂数据提供者（依赖注入）
     */
    public FangSearchStrategy(BookRepository bookRepository, int bookId, IFangDataProvider fangProvider) {
        this.bookRepository = bookRepository;
        this.bookId = bookId;
        this.bookData = bookRepository.getBookData(bookId);
        this.chapters = bookRepository.getChapters(bookId);
        this.fangProvider = fangProvider;
        
        this.matcher = new FangNameMatcher();
        this.builder = new SearchResultBuilder();
    }
    
    @Override
    public Pair<List<GroupData>, List<List<ItemData>>> search(String keyword) {
        EasyLog.print("=== FangSearchStrategy.search() ===");
        EasyLog.print("方剂关键字: " + keyword);
        
        if (bookData == null || chapters == null || chapters.isEmpty()) {
            EasyLog.print("❌ 数据源不可用");
            return builder.notFoundFang();
        }
        
        // 1. 检查数据加载状态
        if (!fangProvider.isDataLoaded()) {
            EasyLog.print("⚠️ 方剂数据未加载");
            return builder.notLoaded();
        }
        
        // 2. 获取别名字典
        Map<String, String> aliasDict = fangProvider.getFangAliasDict();
        String aliasName = matcher.resolveAlias(aliasDict, keyword);
        
        EasyLog.print("搜索关键词: " + keyword + ", 别名: " + aliasName);
        
        // 2. 搜索方剂数据（配方信息）
        Pair<List<GroupData>, List<List<ItemData>>> fangDataResult = searchFangData(keyword, aliasName, aliasDict);
        
        // 3. 搜索章节内容（条文信息）- 无论是否找到方剂数据，都要继续搜索
        Pair<List<GroupData>, List<List<ItemData>>> chapterResult = searchInChapters(keyword, aliasName, aliasDict);
        
        // 4. 合并结果
        List<GroupData> mergedGroups = new ArrayList<>();
        List<List<ItemData>> mergedItems = new ArrayList<>();
        
        // 添加方剂数据结果
        if (!fangDataResult.first.isEmpty()) {
            EasyLog.print("✅ 从方剂数据中找到结果");
            mergedGroups.addAll(fangDataResult.first);
            mergedItems.addAll(fangDataResult.second);
        }
        
        // 添加章节内容结果
        if (!chapterResult.first.isEmpty()) {
            EasyLog.print("✅ 从章节内容中找到结果");
            mergedGroups.addAll(chapterResult.first);
            mergedItems.addAll(chapterResult.second);
        }
        
        // 5. 如果均未找到，返回未找到结果
        if (mergedGroups.isEmpty()) {
            EasyLog.print("❌ 未找到匹配的方剂: " + keyword);
            return builder.notFoundFang();
        }
        
        EasyLog.print("=== 搜索完成 ===");
        EasyLog.print("总计: " + mergedGroups.size() + " 个分组");
        
        return new Pair<>(mergedGroups, mergedItems);
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 从方剂数据中搜索（精确匹配）
     */
    private Pair<List<GroupData>, List<List<ItemData>>> searchFangData(
            String keyword, String aliasName, Map<String, String> aliasDict) {
        
        List<GroupData> groups = new ArrayList<>();
        List<List<ItemData>> items = new ArrayList<>();
        
        ChapterData fangChapterData = bookData.getFangData();
        EasyLog.print("方剂数据: " + (fangChapterData != null && fangChapterData.isContentLoaded() ? "已加载" : "未加载"));
        
        if (fangChapterData == null || !fangChapterData.isContentLoaded()) {
            EasyLog.print("方剂数据未加载，尝试从章节内容中搜索方剂: " + keyword);
            // 方剂数据未加载，尝试备选方案
            return searchFangInChaptersFallback(keyword, aliasName, aliasDict);
        }
        
        List<DataItem> fangItems = fangChapterData.getContent();
        EasyLog.print("方剂条目数: " + (fangItems != null ? fangItems.size() : 0));
        
        if (fangItems == null) {
            return new Pair<>(groups, items);
        }
        
        for (DataItem fangItem : fangItems) {
            if (matcher.isMatch(fangItem.getName(), keyword, aliasDict)) {
                EasyLog.print("找到匹配方剂: " + fangItem.getName());
                
                GroupData group = builder.buildGroup(fangChapterData.getTitle(), true);
                groups.add(group);
                
                List<ItemData> itemList = new ArrayList<>();
                itemList.add(builder.convertDataItem(fangItem, true));
                items.add(itemList);
                
                EasyLog.print("✅ 找到方剂配方: " + fangItem.getName());
                return new Pair<>(groups, items);
            }
        }
        
        EasyLog.print("未在方剂数据中找到匹配: " + aliasName);
        return new Pair<>(groups, items);
    }
    
    /**
     * 从章节内容中搜索（已下载章节）
     */
    private Pair<List<GroupData>, List<List<ItemData>>> searchInChapters(
            String keyword, String aliasName, Map<String, String> aliasDict) {
        
        List<GroupData> groups = new ArrayList<>();
        List<List<ItemData>> items = new ArrayList<>();
        boolean foundAny = false;
        int totalItems = 0;
        int matchedSections = 0;
        int downloadedChapters = 0;
        int loadedChapters = 0;
        
        for (Chapter chapter : chapters) {
            if (!chapter.getIsDownload()) {
                continue;
            }
            downloadedChapters++;
            
            ChapterData chapterData = bookData.findChapterBySignature(chapter.getSignatureId());
            if (chapterData == null) {
                EasyLog.print("⚠️ 章节 " + chapter.getChapterHeader() + " ChapterData为null");
                continue;
            }
            
            // 如果内容未加载，主动触发加载
            if (!chapterData.isContentLoaded()) {
                EasyLog.print("🔄 章节 " + chapter.getChapterHeader() + " 内容未加载，触发加载...");
                bookRepository.loadChapterContent(bookData, chapter);
                
                if (!chapterData.isContentLoaded()) {
                    EasyLog.print("⚠️ 章节 " + chapter.getChapterHeader() + " 加载失败");
                    continue;
                }
            }
            loadedChapters++;
            
            List<DataItem> content = chapterData.getContent();
            if (content == null || content.isEmpty()) {
                EasyLog.print("⚠️ 章节 " + chapter.getChapterHeader() + " 内容为空");
                continue;
            }
            
            List<DataItem> matchedItems = new ArrayList<>();
            for (DataItem dataItem : content) {
                totalItems++;
                
                List<String> fangList = dataItem.getFangList();
                if (fangList == null || fangList.isEmpty()) {
                    continue;
                }
                
                for (String fangName : fangList) {
                    String actualName = matcher.resolveAlias(aliasDict, fangName);
                    if (actualName != null && actualName.equals(aliasName)) {
                        matchedItems.add(dataItem);
                        foundAny = true;
                        break;
                    }
                }
            }
            
            if (!matchedItems.isEmpty()) {
                GroupData group = builder.buildGroup(chapter.getChapterHeader(), true);
                groups.add(group);
                
                List<ItemData> itemList = new ArrayList<>();
                for (DataItem item : matchedItems) {
                    itemList.add(builder.convertDataItem(item, false));
                }
                items.add(itemList);
                
                matchedSections++;
                EasyLog.print("✅ 章节: " + chapter.getChapterHeader() + ", 条目: " + matchedItems.size());
            }
        }
        
        EasyLog.print("已下载章节: " + downloadedChapters + ", 已加载内容: " + loadedChapters);
        EasyLog.print("检查条目数: " + totalItems + ", 匹配章节: " + matchedSections);
        
        return foundAny ? new Pair<>(groups, items) : new Pair<>(new ArrayList<>(), new ArrayList<>());
    }
    
    /**
     * 备选方案：方剂数据未加载时，从所有章节中搜索方剂引用
     */
    private Pair<List<GroupData>, List<List<ItemData>>> searchFangInChaptersFallback(
            String keyword, String aliasName, Map<String, String> aliasDict) {
        
        List<GroupData> groups = new ArrayList<>();
        List<List<ItemData>> items = new ArrayList<>();
        
        try {
            if (bookData == null) {
                EasyLog.print("书籍数据为空，无法搜索章节中的方剂");
                return new Pair<>(groups, items);
            }
            
            List<ChapterData> allChapters = bookData.getAllChapters();
            if (allChapters == null || allChapters.isEmpty()) {
                EasyLog.print("章节数据为空，无法搜索方剂");
                return new Pair<>(groups, items);
            }
            
            EasyLog.print("开始在 " + allChapters.size() + " 个章节中搜索方剂: " + keyword);
            
            boolean foundAnyFang = false;
            
            for (ChapterData chapter : allChapters) {
                if (chapter == null || !chapter.isContentLoaded()) {
                    continue;
                }
                
                List<DataItem> contents = chapter.getContent();
                if (contents == null || contents.isEmpty()) {
                    continue;
                }
                
                boolean foundInChapter = false;
                List<ItemData> matchedItems = new ArrayList<>();
                
                for (DataItem item : contents) {
                    if (item == null || item.getName() == null) {
                        continue;
                    }
                    
                    String itemName = item.getName();
                    
                    if (matcher.isPotentialMatch(itemName, keyword, aliasName)) {
                        foundInChapter = true;
                        foundAnyFang = true;
                        
                        ItemData itemData = builder.convertDataItem(item, false);
                        // 添加标记表示这是从章节中找到的方剂
                        if (itemData.getAttributedText() != null) {
                            SpannableStringBuilder enhancedText = new SpannableStringBuilder();
                            enhancedText.append("【章节引用】 ");
                            enhancedText.append(itemData.getAttributedText());
                            itemData.setAttributedText(enhancedText);
                        }
                        
                        matchedItems.add(itemData);
                    }
                }
                
                if (foundInChapter && !matchedItems.isEmpty()) {
                    GroupData group = builder.buildGroup(chapter.getTitle() + " (章节引用)", true);
                    groups.add(group);
                    items.add(matchedItems);
                    
                    EasyLog.print("在章节 '" + chapter.getTitle() + "' 中找到 " + matchedItems.size() + " 个匹配的方剂引用");
                }
            }
            
            if (!foundAnyFang) {
                EasyLog.print("在所有章节中均未找到匹配的方剂引用: " + keyword);
            } else {
                EasyLog.print("✅ 在章节中找到方剂引用，共 " + groups.size() + " 个章节组");
            }
            
        } catch (Exception e) {
            EasyLog.print("在章节中搜索方剂时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new Pair<>(groups, items);
    }
}
