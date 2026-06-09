package run.yigou.gxzy.ui.feature.reader.utils;

import android.os.Build;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import run.yigou.gxzy.log.EasyLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.model.MingCiContent;
import run.yigou.gxzy.model.Yao;
import run.yigou.gxzy.ui.feature.reader.data.BookData;
import run.yigou.gxzy.ui.feature.reader.data.ChapterData;
import run.yigou.gxzy.ui.feature.reader.entity.GroupData;
import run.yigou.gxzy.ui.feature.reader.entity.ItemData;
import run.yigou.gxzy.ui.feature.reader.repository.BookRepository;
import run.yigou.gxzy.model.DataItem;
import run.yigou.gxzy.model.HH2SectionData;
import run.yigou.gxzy.ui.feature.reader.helper.TipsNetHelper;
import run.yigou.gxzy.data.GlobalDataHolder;
import run.yigou.gxzy.utils.DebugLog;

/**
 * 搜索数据适配器 - 统一处理方剂/药物/名词搜索
 * 【新架构版本】使用BookRepository直接获取数据
 */
public class SearchDataAdapter {
    
    private final BookRepository bookRepository;
    private final int bookId;
    private final BookData bookData;
    private final List<Chapter> chapters;
    
    /**
     * 构造函数
     * @param bookRepository 书籍仓库
     * @param bookId 当前书籍ID
     */
    public SearchDataAdapter(BookRepository bookRepository, int bookId) {
        this.bookRepository = bookRepository;
        this.bookId = bookId;
        this.bookData = bookRepository.getBookData(bookId);
        this.chapters = bookRepository.getChapters(bookId);
        
        EasyLog.print("=== SearchDataAdapter 初始化 ===");
        EasyLog.print("BookId: " + bookId);
        EasyLog.print("BookData: " + (bookData != null ? "已加载" : "未加载"));
        EasyLog.print("章节数: " + (chapters != null ? chapters.size() : 0));
    }
    
    /**
     * 搜索方剂相关内容
     */
    public Pair<List<GroupData>, List<List<ItemData>>> searchFangContent(String keyword) {
        EasyLog.print("=== SearchDataAdapter.searchFangContent() [新架构] ===");
        EasyLog.print("方剂关键字: " + keyword);
        
        List<GroupData> groupDataList = new ArrayList<>();
        List<List<ItemData>> itemDataList = new ArrayList<>();
        
        if (bookData == null || chapters == null || chapters.isEmpty()) {
            EasyLog.print("❌ 数据源不可用");
            addNotFoundFangResult(groupDataList, itemDataList);
            return new Pair<>(groupDataList, itemDataList);
        }
        
        // 获取别名字典
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        Map<String, String> fangAliasDict = globalData.getFangAliasDict();
        
        // 增强日志：显示别名字典状态
        EasyLog.print("方剂别名字典: " + (fangAliasDict != null ? fangAliasDict.size() : 0) + " 个别名");
        if (fangAliasDict != null && fangAliasDict.size() > 0) {
            EasyLog.print("别名示例: " + fangAliasDict.keySet().stream().limit(5).collect(java.util.stream.Collectors.toList()));
        }
        
        String aliasName = getAliasName(fangAliasDict, keyword);
        
        EasyLog.print("搜索关键词: " + keyword);
        EasyLog.print("解析别名: " + aliasName);
        EasyLog.print("别名匹配: " + (!keyword.equals(aliasName) ? "是" : "否"));
        
        // 【方剂详细信息】优先添加方剂配方（从BookData.getFangData()）
        ChapterData fangChapterData = bookData.getFangData();
        EasyLog.print("方剂数据: " + (fangChapterData != null && fangChapterData.isContentLoaded() ? "已加载" : "未加载"));
        
        if (fangChapterData != null && fangChapterData.isContentLoaded()) {
            List<DataItem> fangItems = fangChapterData.getContent();
            EasyLog.print("方剂条目数: " + (fangItems != null ? fangItems.size() : 0));
            
            if (fangItems != null) {
                boolean foundMatch = false;
                
                for (DataItem fangItem : fangItems) {
                    // 检查方剂名称是否匹配
                    String itemName = fangItem.getName();
                    
                    // 增强匹配逻辑：支持多种匹配方式
                    boolean isMatch = isFangNameMatch(itemName, aliasName, keyword, fangAliasDict);
                    
                    if (isMatch) {
                        foundMatch = true;
                        EasyLog.print("找到匹配方剂: " + itemName);
                        
                        // 添加方剂配方信息
                        GroupData groupData = new GroupData();
                        groupData.setTitle(fangChapterData.getTitle());
                        groupData.setExpanded(true);
                        groupDataList.add(groupData);
                        
                        List<ItemData> items = new ArrayList<>();
                        items.add(convertDataItemToItemData(fangItem, true));
                        itemDataList.add(items);
                        
                        EasyLog.print("✅ 找到方剂配方: " + itemName);
                        break;
                    }
                }
                
                if (!foundMatch) {
                    EasyLog.print("未在方剂数据中找到匹配: " + aliasName);
                }
            }
        } else {
            // 方剂数据未加载，尝试从章节内容中搜索
            EasyLog.print("方剂数据未加载，尝试从章节内容中搜索方剂: " + keyword);
            searchFangInChapters(keyword, aliasName, fangAliasDict, groupDataList, itemDataList);
        }
        
        // 在所有已下载章节中搜索
        boolean foundFang = false;
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
            
            // 【关键修复】如果内容未加载，主动触发加载
            if (!chapterData.isContentLoaded()) {
                EasyLog.print("🔄 章节 " + chapter.getChapterHeader() + " 内容未加载，触发加载...");
                bookRepository.loadChapterContent(bookData, chapter);
                
                // 加载后再次检查
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
                
                // 检查是否包含目标方剂
                for (String fangName : fangList) {
                    String actualName = getAliasName(fangAliasDict, fangName);
                    if (actualName != null && actualName.equals(aliasName)) {
                        matchedItems.add(dataItem);
                        foundFang = true;
                        break;
                    }
                }
            }
            
            // 如果找到匹配项，添加到结果
            if (!matchedItems.isEmpty()) {
                GroupData groupData = new GroupData();
                groupData.setTitle(chapter.getChapterHeader());
                groupData.setExpanded(true);
                groupDataList.add(groupData);
                
                List<ItemData> items = new ArrayList<>();
                for (DataItem dataItem : matchedItems) {
                    items.add(convertDataItemToItemData(dataItem, false));
                }
                itemDataList.add(items);
                
                matchedSections++;
                EasyLog.print("✅ 章节: " + chapter.getChapterHeader() + ", 条目: " + matchedItems.size());
            }
        }
        
        // 如果未找到，添加"未见方。"
        if (!foundFang) {
            EasyLog.print("❌ 未找到匹配的方剂: " + keyword + " (别名: " + aliasName + ")");
            addNotFoundFangResult(groupDataList, itemDataList);
        } else {
            EasyLog.print("✅ 找到 " + groupDataList.size() + " 个匹配的方剂组");
        }
        
        EasyLog.print("=== 搜索完成 ===");
        EasyLog.print("已下载章节: " + downloadedChapters + ", 已加载内容: " + loadedChapters);
        EasyLog.print("检查条目数: " + totalItems + ", 匹配章节: " + matchedSections);
        
        return new Pair<>(groupDataList, itemDataList);
    }

    /**
     * 搜索药物相关内容
     */
    public Pair<List<GroupData>, List<List<ItemData>>> searchYaoContent(String keyword) {
        EasyLog.print("=== SearchDataAdapter.searchYaoContent() [新架构] ===");
        EasyLog.print("药物关键字: " + keyword);
        
        List<GroupData> groupDataList = new ArrayList<>();
        List<List<ItemData>> itemDataList = new ArrayList<>();
        
        if (bookData == null || chapters == null) {
            EasyLog.print("❌ 数据源不可用");
            addNotFoundYaoResult(groupDataList, itemDataList);
            return new Pair<>(groupDataList, itemDataList);
        }
        
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        Map<String, String> yaoAliasDict = globalData.getYaoAliasDict();
        String aliasName = getAliasName(yaoAliasDict, keyword);
        
        // 添加药物信息
        Map<String, Yao> yaoMap = globalData.getYaoMap();
        Yao yao = yaoMap != null ? yaoMap.get(aliasName) : null;
        
        if (yao != null) {
            GroupData groupData = new GroupData();
            groupData.setTitle("药物信息");
            groupData.setExpanded(true);
            groupDataList.add(groupData);
            
            List<ItemData> items = new ArrayList<>();
            ItemData itemData = new ItemData();
            
            SpannableStringBuilder yaoText = TipsNetHelper.renderText("$x{" + yao.getName() + "}\n");
            if (yao.getAttributedText() != null) {
                yaoText.append(yao.getAttributedText());
            }
            itemData.setAttributedText(yaoText);
            
            items.add(itemData);
            itemDataList.add(items);
            
            EasyLog.print("✅ 找到药物: " + yao.getName());
        } else {
            addNotFoundYaoResult(groupDataList, itemDataList);
        }
        
        // 在章节中搜索包含该药的内容
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
                    String actualName = getAliasName(yaoAliasDict, yaoName);
                    if (actualName != null && actualName.equals(aliasName)) {
                        matchedItems.add(dataItem);
                        break;
                    }
                }
            }
            
            if (!matchedItems.isEmpty()) {
                GroupData groupData = new GroupData();
                groupData.setTitle(chapter.getChapterHeader());
                groupData.setExpanded(true);
                groupDataList.add(groupData);
                
                List<ItemData> items = new ArrayList<>();
                for (DataItem dataItem : matchedItems) {
                    items.add(convertDataItemToItemData(dataItem, false));
                }
                itemDataList.add(items);
                
                matchedSections++;
            }
        }
        
        EasyLog.print("匹配章节: " + matchedSections);
        return new Pair<>(groupDataList, itemDataList);
    }

    /**
     * 搜索名词相关内容
     */
    public Pair<List<GroupData>, List<List<ItemData>>> searchMingCiContent(String keyword) {
        EasyLog.print("=== SearchDataAdapter.searchMingCiContent() [新架构] ===");
        EasyLog.print("名词关键字: " + keyword);
        
        List<GroupData> groupDataList = new ArrayList<>();
        List<List<ItemData>> itemDataList = new ArrayList<>();
        
        // 获取名词定义
        Map<String, MingCiContent> mingCiContentMap = GlobalDataHolder.getInstance().getMingCiContentMap();
        MingCiContent mingCiContent = mingCiContentMap != null ? mingCiContentMap.get(keyword) : null;
        
        if (mingCiContent != null) {
            GroupData groupData = new GroupData();
            groupData.setTitle("名词解释");
            groupData.setExpanded(true);
            groupDataList.add(groupData);
            
            List<ItemData> items = new ArrayList<>();
            ItemData itemData = new ItemData();
            
            SpannableStringBuilder mingCiText = TipsNetHelper.renderText("$x{" + mingCiContent.getName() + "}\n");
            if (mingCiContent.getAttributedText() != null) {
                mingCiText.append(mingCiContent.getAttributedText());
            }
            itemData.setAttributedText(mingCiText);
            
            items.add(itemData);
            itemDataList.add(items);
            
            EasyLog.print("✅ 找到名词: " + mingCiContent.getName());
        } else {
            GroupData groupData = new GroupData();
            groupData.setTitle("名词解释");
            groupData.setExpanded(true);
            groupDataList.add(groupData);
            
            List<ItemData> items = new ArrayList<>();
            ItemData itemData = new ItemData();
            itemData.setAttributedText(TipsNetHelper.renderText("$m{未见此名词。}"));
            items.add(itemData);
            itemDataList.add(items);
            
            EasyLog.print("⚠️ 未找到名词");
        }
        
        return new Pair<>(groupDataList, itemDataList);
    }
    
    // ========== 辅助方法 ==========
    
    private void addNotFoundFangResult(List<GroupData> groupDataList, List<List<ItemData>> itemDataList) {
        EasyLog.print("添加未找到方剂结果");
        
        GroupData groupData = new GroupData();
        groupData.setTitle("伤寒金匮方");
        groupData.setExpanded(true);
        groupDataList.add(groupData);
        
        List<ItemData> items = new ArrayList<>();
        ItemData itemData = new ItemData();
        itemData.setAttributedText(TipsNetHelper.renderText("$m{未见方。}"));
        items.add(itemData);
        itemDataList.add(items);
    }
    
    private void addNotFoundYaoResult(List<GroupData> groupDataList, List<List<ItemData>> itemDataList) {
        GroupData groupData = new GroupData();
        groupData.setTitle("药物信息");
        groupData.setExpanded(true);
        groupDataList.add(groupData);
        
        List<ItemData> items = new ArrayList<>();
        ItemData itemData = new ItemData();
        itemData.setAttributedText(TipsNetHelper.renderText("$m{未见此药。}"));
        items.add(itemData);
        itemDataList.add(items);
    }
    
    private String getAliasName(Map<String, String> aliasDict, String key) {
        if (aliasDict == null || key == null) {
            return key;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return aliasDict.getOrDefault(key, key);
        } else {
            return aliasDict.containsKey(key) ? aliasDict.get(key) : key;
        }
    }
    
    /**
     * 检查方剂名称是否匹配
     * 支持多种匹配方式：
     * 1. 直接名称匹配
     * 2. 别名匹配
     * 3. 基础名称匹配（去掉剂型后缀）
     * 4. 包含匹配
     */
    private boolean isFangNameMatch(String itemName, String aliasName, String originalKeyword, Map<String, String> aliasDict) {
        if (itemName == null || originalKeyword == null) {
            return false;
        }
        
        // 1. 直接匹配
        if (itemName.equals(aliasName)) {
            return true;
        }
        
        // 2. 原始关键词直接匹配
        if (itemName.equals(originalKeyword)) {
            return true;
        }
        
        // 3. 包含匹配（如果关键词较短）
        if (originalKeyword.length() >= 2 && itemName.contains(originalKeyword)) {
            return true;
        }
        
        // 4. 基础名称匹配（去掉剂型后缀）
        String itemBaseName = extractFangBaseName(itemName);
        String keywordBaseName = extractFangBaseName(originalKeyword);
        
        if (itemBaseName.equals(keywordBaseName) && !itemBaseName.isEmpty()) {
            return true;
        }
        
        // 5. 反向别名检查（检查关键词是否是itemName的别名）
        for (Map.Entry<String, String> entry : aliasDict.entrySet()) {
            if (entry.getValue().equals(itemName) && entry.getKey().equals(originalKeyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 提取方剂基础名称（去掉剂型后缀）
     */
    private String extractFangBaseName(String fangName) {
        if (fangName == null) return "";
        
        String baseName = fangName.trim();
        
        // 去掉常见的剂型后缀
        String[] suffixes = {"汤", "散", "丸", "膏", "丹", "片", "胶囊", "颗粒", "口服液", "注射液"};
        for (String suffix : suffixes) {
            if (baseName.endsWith(suffix) && baseName.length() > suffix.length()) {
                baseName = baseName.substring(0, baseName.length() - suffix.length());
                break;
            }
        }
        
        return baseName.trim();
    }
    
    /**
     * 在章节内容中搜索方剂（当方剂数据未加载时的备选方案）
     * 这是为了确保阅读器功能不受影响，同时提供基本的方剂搜索能力
     */
    private void searchFangInChapters(String keyword, String aliasName, Map<String, String> fangAliasDict,
                                    List<GroupData> groupDataList, List<List<ItemData>> itemDataList) {
        try {
            if (bookData == null) {
                EasyLog.print("书籍数据为空，无法搜索章节中的方剂");
                return;
            }
            
            // 从BookData获取所有章节
            List<ChapterData> allChapters = bookData.getAllChapters();
            if (allChapters == null || allChapters.isEmpty()) {
                EasyLog.print("章节数据为空，无法搜索方剂");
                return;
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
                
                // 在当前章节中搜索方剂
                boolean foundInChapter = false;
                List<ItemData> matchedItems = new ArrayList<>();
                
                for (DataItem item : contents) {
                    if (item == null || item.getName() == null) {
                        continue;
                    }
                    
                    // 检查是否是方剂相关的内容
                    String itemName = item.getName();
                    
                    // 使用宽松的匹配策略
                    if (isPotentialFangMatch(itemName, keyword, aliasName)) {
                        foundInChapter = true;
                        foundAnyFang = true;
                        
                        ItemData itemData = convertDataItemToItemData(item, false);
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
                
                // 如果在本章节中找到匹配的方剂，添加到结果
                if (foundInChapter && !matchedItems.isEmpty()) {
                    GroupData groupData = new GroupData();
                    groupData.setTitle(chapter.getTitle() + " (章节引用)");
                    groupData.setExpanded(true);
                    groupDataList.add(groupData);
                    
                    itemDataList.add(matchedItems);
                    
                    EasyLog.print("在章节 '" + chapter.getTitle() + "' 中找到 " + matchedItems.size() + " 个匹配的方剂引用");
                }
            }
            
            if (!foundAnyFang) {
                EasyLog.print("在所有章节中均未找到匹配的方剂引用: " + keyword);
            } else {
                EasyLog.print("✅ 在章节中找到方剂引用，共 " + groupDataList.size() + " 个章节组");
            }
            
        } catch (Exception e) {
            EasyLog.print("在章节中搜索方剂时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 判断是否是潜在的方剂匹配（宽松的匹配策略）
     */
    private boolean isPotentialFangMatch(String itemName, String keyword, String aliasName) {
        if (itemName == null || keyword == null) {
            return false;
        }
        
        // 1. 直接包含匹配
        if (itemName.contains(keyword)) {
            return true;
        }
        
        // 2. 别名包含匹配
        if (!keyword.equals(aliasName) && itemName.contains(aliasName)) {
            return true;
        }
        
        // 3. 基础名称匹配（去掉剂型后缀）
        String itemBaseName = extractFangBaseName(itemName);
        String keywordBaseName = extractFangBaseName(keyword);
        
        if (itemBaseName.equals(keywordBaseName) && !itemBaseName.isEmpty()) {
            return true;
        }
        
        // 4. 如果关键词很短，检查是否可能是方剂名的一部分
        if (keyword.length() >= 2 && itemName.length() > keyword.length()) {
            // 检查是否包含常见的方剂关键词
            String[] commonFangKeywords = {"汤", "散", "丸", "膏", "丹"};
            for (String commonKeyword : commonFangKeywords) {
                if (itemName.contains(keyword) && itemName.contains(commonKeyword)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private ItemData convertDataItemToItemData(DataItem dataItem, boolean isFangRecipe) {
        ItemData itemData = new ItemData();
        
        // 只在方剂配方时添加橙色标签【方剂配方】
        if (isFangRecipe && dataItem.getAttributedText() != null) {
            SpannableStringBuilder enhancedText = new SpannableStringBuilder();
            
            // 创建橙色标签
            String  hint = "【" +  dataItem.getName() + "】";
            SpannableString hintSpan = new SpannableString(hint);
            
            // 设置淡橙色 #FFB74D (ARGB: 0xFFFFB74D)
            hintSpan.setSpan(new ForegroundColorSpan(0xFFFFB74D), 0, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 设置粗体
            hintSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 设置点击事件
//            hintSpan.setSpan(new ClickableSpan() {
//                @Override
//                public void onClick(@NonNull View widget) {
//                    Toast.makeText(widget.getContext(), "方剂配方详情", Toast.LENGTH_SHORT).show();
//                }
//
//                @Override
//                public void updateDrawState(@NonNull TextPaint ds) {
//                    super.updateDrawState(ds);
//                    ds.setUnderlineText(false); // 不显示下划线
//                }
//            }, 0, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            enhancedText.append(hintSpan);
            enhancedText.append("\n\n");
            enhancedText.append(dataItem.getAttributedText());

            itemData.setAttributedText(enhancedText);
        } else if (dataItem.getAttributedText() != null) {
            // 其他情况直接设置,不添加橙色标签
            itemData.setAttributedText(dataItem.getAttributedText());
        }

        if (dataItem.getAttributedNote() != null) {
            itemData.setAttributedNote(dataItem.getAttributedNote());
        }
        
        if (dataItem.getAttributedSectionVideo() != null) {
            itemData.setAttributedVideo(dataItem.getAttributedSectionVideo());
        }
        
        if (dataItem.getImageUrl() != null) {
            itemData.setImageUrl(dataItem.getImageUrl());
        }
        
        itemData.setGroupPosition(dataItem.getGroupPosition());
        
        return itemData;
    }
}
