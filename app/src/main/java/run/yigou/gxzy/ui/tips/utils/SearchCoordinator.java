package run.yigou.gxzy.ui.tips.utils;

import android.text.SpannableStringBuilder;
import android.util.Pair;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.ui.tips.entity.GroupData;
import run.yigou.gxzy.ui.tips.entity.ItemData;
import run.yigou.gxzy.ui.tips.repository.BookRepository;
import run.yigou.gxzy.ui.tips.data.BookData;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.ui.tips.data.ChapterData;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;

/**
 * 搜索协调器
 * 负责协调全局搜索流程，整合搜索结果
 */
public class SearchCoordinator {
    
    private BookRepository bookRepository;
    private int bookId;
    
    /**
     * 构造函数
     * 
     * @param bookId 书籍ID
     */
    public SearchCoordinator(int bookId) {
        this.bookId = bookId;
        this.bookRepository = new BookRepository();
    }
    
    /**
     * 全局搜索
     * 在整本书的所有章节中搜索关键字
     * 
     * @param keyword 关键字
     * @return GroupData 和 ItemData 的配对列表
     */
    public Pair<List<GroupData>, List<List<ItemData>>> searchGlobal(String keyword) {
        EasyLog.print("=== SearchCoordinator.searchGlobal() ===");
        EasyLog.print("书籍ID: " + bookId + ", 关键字: " + keyword);
        
        List<GroupData> groupDataList = new ArrayList<>();
        List<List<ItemData>> itemDataList = new ArrayList<>();
        
        // 验证输入
        if (keyword == null || keyword.trim().isEmpty()) {
            EasyLog.print("❌ 关键字为空");
            return new Pair<>(groupDataList, itemDataList);
        }
        
        String trimmedKeyword = keyword.trim();
        
        // 获取所有章节
        List<Chapter> chapters = bookRepository.getChapters(bookId);
        if (chapters == null || chapters.isEmpty()) {
            EasyLog.print("❌ 无章节数据");
            return new Pair<>(groupDataList, itemDataList);
        }
        
        BookData bookData = bookRepository.getBookData(bookId);
        if (bookData == null) {
            EasyLog.print("❌ 无书籍数据");
            return new Pair<>(groupDataList, itemDataList);
        }
        
        int matchedChapters = 0;
        int totalMatches = 0;
        
        EasyLog.print("开始搜索，总章节数: " + chapters.size());
        
        // 遍历每个章节
        for (Chapter chapter : chapters) {
            ChapterData chapterData = bookData.findChapterBySignature(
                chapter.getSignatureId()
            );
            
            // 跳过未加载的章节
            if (chapterData == null || !chapterData.isContentLoaded()) {
                continue;
            }
            
            // 在章节内容中搜索
            List<DataItem> matchedItems = new ArrayList<>();
            List<DataItem> content = chapterData.getContent();
            
            if (content != null) {
                for (DataItem item : content) {
                    if (matchesKeyword(item, trimmedKeyword)) {
                        matchedItems.add(item);
                    }
                }
            }
            
            // 如果找到匹配，创建 GroupData
            if (!matchedItems.isEmpty()) {
                GroupData groupData = new GroupData();
                groupData.setTitle(chapter.getChapterHeader());
                groupData.setExpanded(false); // 默认折叠
                groupDataList.add(groupData);
                
                // 创建 ItemData 列表（带高亮）
                List<ItemData> items = new ArrayList<>();
                for (DataItem dataItem : matchedItems) {
                    ItemData itemData = convertToItemDataWithHighlight(dataItem, trimmedKeyword);
                    items.add(itemData);
                }
                itemDataList.add(items);
                
                matchedChapters++;
                totalMatches += matchedItems.size();
                
                EasyLog.print("✅ 章节: " + chapter.getChapterHeader() + 
                            ", 匹配数: " + matchedItems.size());
            }
        }
        
        EasyLog.print("=== 搜索完成 ===");
        EasyLog.print("匹配章节: " + matchedChapters + ", 总匹配数: " + totalMatches);
        
        return new Pair<>(groupDataList, itemDataList);
    }
    
    /**
     * 判断 DataItem 是否包含关键字
     * 
     * @param item 数据项
     * @param keyword 关键字
     * @return 是否匹配
     */
    private boolean matchesKeyword(DataItem item, String keyword) {
        // 检查文本内容
        if (item.getAttributedText() != null) {
            String text = item.getAttributedText().toString();
            if (SearchMatcher.contains(text, keyword)) {
                return true;
            }
        }
        
        // 检查注释
        if (item.getAttributedNote() != null) {
            String note = item.getAttributedNote().toString();
            if (SearchMatcher.contains(note, keyword)) {
                return true;
            }
        }
        
        // 检查视频标注
        if (item.getAttributedSectionVideo() != null) {
            String video = item.getAttributedSectionVideo().toString();
            if (SearchMatcher.contains(video, keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 转换为 ItemData 并高亮关键字
     * 
     * @param dataItem 原始数据项
     * @param keyword 关键字
     * @return 高亮后的 ItemData
     */
    private ItemData convertToItemDataWithHighlight(DataItem dataItem, String keyword) {
        ItemData itemData = new ItemData();
        
        // 高亮文本内容
        if (dataItem.getAttributedText() != null) {
            SpannableStringBuilder highlightedText = 
                TextHighlighter.createHighlighted(dataItem.getAttributedText(), keyword);
            itemData.setAttributedText(highlightedText);
        }
        
        // 高亮注释
        if (dataItem.getAttributedNote() != null) {
            SpannableStringBuilder highlightedNote = 
                TextHighlighter.createHighlighted(dataItem.getAttributedNote(), keyword);
            itemData.setAttributedNote(highlightedNote);
        }
        
        // 高亮视频标注
        if (dataItem.getAttributedSectionVideo() != null) {
            SpannableStringBuilder highlightedVideo = 
                TextHighlighter.createHighlighted(dataItem.getAttributedSectionVideo(), keyword);
            itemData.setAttributedVideo(highlightedVideo);
        }
        
        // 复制其他字段
        if (dataItem.getImageUrl() != null) {
            itemData.setImageUrl(dataItem.getImageUrl());
        }
        
        itemData.setGroupPosition(dataItem.getGroupPosition());
        
        return itemData;
    }
}
