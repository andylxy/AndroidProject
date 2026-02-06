package run.yigou.gxzy.ui.tips.utils;

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

import run.yigou.gxzy.utils.EasyLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.data.BookData;
import run.yigou.gxzy.ui.tips.data.ChapterData;
import run.yigou.gxzy.ui.tips.entity.GroupData;
import run.yigou.gxzy.ui.tips.entity.ItemData;
import run.yigou.gxzy.ui.tips.repository.BookRepository;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
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
        String aliasName = getAliasName(fangAliasDict, keyword);
        
        EasyLog.print("实际别名: " + aliasName);
        
        // 【方剂详细信息】优先添加方剂配方（从BookData.getFangData()）
        ChapterData fangChapterData = bookData.getFangData();
        EasyLog.print("方剂数据: " + (fangChapterData != null && fangChapterData.isContentLoaded() ? "已加载" : "未加载"));
        
        if (fangChapterData != null && fangChapterData.isContentLoaded()) {
            List<DataItem> fangItems = fangChapterData.getContent();
            EasyLog.print("方剂条目数: " + (fangItems != null ? fangItems.size() : 0));
            
            if (fangItems != null) {
                for (DataItem fangItem : fangItems) {
                    // 检查方剂名称是否匹配
                    String itemName = fangItem.getName();
                    
                    if (aliasName.equals(itemName)) {
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
            }
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
            addNotFoundFangResult(groupDataList, itemDataList);
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
