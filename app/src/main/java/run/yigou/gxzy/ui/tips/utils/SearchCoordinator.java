package run.yigou.gxzy.ui.tips.utils;

import android.text.SpannableStringBuilder;
import android.util.Pair;

import run.yigou.gxzy.other.EasyLog;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.ui.tips.entity.GroupData;
import run.yigou.gxzy.ui.tips.entity.ItemData;
import run.yigou.gxzy.ui.tips.repository.BookRepository;
import run.yigou.gxzy.ui.tips.data.BookData;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.ui.tips.data.ChapterData;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.utils.DebugLog;

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
        
        // 1. 从数据库获取整本书所有章节内容 (与 BookContentSearchActivity 逻辑一致)
        java.util.List<run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData> allContent = 
            run.yigou.gxzy.greendao.util.ConvertEntity.getBookChapterDetailList(bookId);
        
        if (allContent == null || allContent.isEmpty()) {
            EasyLog.print("❌ 无书籍数据");
            return new Pair<>(groupDataList, itemDataList);
        }
        
        EasyLog.print("开始搜索，总章节数: " + allContent.size());
        
        // 2. 针对伤寒论进行特殊过滤
        if (bookId == run.yigou.gxzy.common.AppConst.ShangHanNo) {
            allContent = filterShangHanData(allContent);
        }
        
        // 3. 获取别名字典
        run.yigou.gxzy.ui.tips.data.GlobalDataHolder globalData = 
            run.yigou.gxzy.ui.tips.data.GlobalDataHolder.getInstance();
        java.util.Map<String, String> yaoAliasDict = globalData.getYaoAliasDict();
        java.util.Map<String, String> fangAliasDict = globalData.getFangAliasDict();
        
        // 4. 调用 TipsNetHelper 进行过滤和高亮
        run.yigou.gxzy.ui.tips.entity.SearchKeyEntity searchKeyEntity = 
            new run.yigou.gxzy.ui.tips.entity.SearchKeyEntity(new StringBuilder(trimmedKeyword));
        
        java.util.ArrayList<run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData> filteredData = 
            run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper.getSearchHh2SectionData(
                searchKeyEntity, 
                allContent, 
                yaoAliasDict, 
                fangAliasDict
            );
        
        // 5. 转换为 GroupData/ItemData 格式
        for (run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData section : filteredData) {
            GroupData groupData = new GroupData();
            groupData.setTitle(section.getHeader());
            groupData.setExpanded(false); // 默认折叠
            groupDataList.add(groupData);
            
            // 创建 ItemData 列表
            List<ItemData> items = new ArrayList<>();
            if (section.getData() != null) {
                for (run.yigou.gxzy.ui.tips.tipsutils.DataItem dataItem : section.getData()) {
                    ItemData itemData = convertDataItemToItemData(dataItem);
                    items.add(itemData);
                }
            }
            itemDataList.add(items);
        }
        
        int totalMatches = searchKeyEntity.getSearchResTotalNum();
        EasyLog.print("=== 搜索完成 ===");
        EasyLog.print("匹配章节: " + groupDataList.size() + ", 总匹配数: " + totalMatches);
        
        return new Pair<>(groupDataList, itemDataList);
    }
    
    /**
     * 伤寒论特殊过滤逻辑
     */
    private java.util.List<run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData> filterShangHanData(
            java.util.List<run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData> contentList) {
        if (contentList == null || contentList.isEmpty()) {
            return new ArrayList<>();
        }
        
        run.yigou.gxzy.common.FragmentSetting fragmentSetting = 
            run.yigou.gxzy.app.AppApplication.getApplication().fragmentSetting;
        if (fragmentSetting == null) {
            return contentList;
        }

        int size = contentList.size();
        int start = 0;
        int end = size;

        if (!fragmentSetting.isSong_JinKui()) {
            if (!fragmentSetting.isSong_ShangHan()) {
                start = 8;
                end = Math.min(18, size);
            } else {
                end = Math.min(26, size);
            }
        } else {
            if (!fragmentSetting.isSong_ShangHan()) {
                start = 8;
            }
        }

        if (start < size) {
            return new ArrayList<>(contentList.subList(start, end));
        } else {
            return new ArrayList<>(contentList);
        }
    }
    
    /**
     * 将 DataItem 转换为 ItemData (已高亮)
     * 
     * 注意：直接使用原始 CharSequence，不要创建新的 SpannableStringBuilder，
     * 否则会丢失 ClickableSpan！
     */
    private ItemData convertDataItemToItemData(run.yigou.gxzy.ui.tips.tipsutils.DataItem dataItem) {
        ItemData itemData = new ItemData();
        
        // ✅ 直接使用原始对象，保留 ClickableSpan
        // 不要使用 new SpannableStringBuilder()，那会丢失 Span！
        if (dataItem.getAttributedText() != null) {
            CharSequence text = dataItem.getAttributedText();
            if (text instanceof android.text.SpannableStringBuilder) {
                itemData.setAttributedText((android.text.SpannableStringBuilder) text);
            } else {
                // 如果不是 SpannableStringBuilder，需要复制并保留 Span
                android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder(text);
                android.text.TextUtils.copySpansFrom(
                    (android.text.Spanned) text, 0, text.length(),
                    null, builder, 0);
                itemData.setAttributedText(builder);
            }
        }
        
        if (dataItem.getAttributedNote() != null) {
            CharSequence note = dataItem.getAttributedNote();
            if (note instanceof android.text.SpannableStringBuilder) {
                itemData.setAttributedNote((android.text.SpannableStringBuilder) note);
            } else if (note instanceof android.text.Spanned) {
                android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder(note);
                android.text.TextUtils.copySpansFrom(
                    (android.text.Spanned) note, 0, note.length(),
                    null, builder, 0);
                itemData.setAttributedNote(builder);
            } else {
                itemData.setAttributedNote(new android.text.SpannableStringBuilder(note));
            }
        }
        
        if (dataItem.getAttributedSectionVideo() != null) {
            CharSequence video = dataItem.getAttributedSectionVideo();
            if (video instanceof android.text.SpannableStringBuilder) {
                itemData.setAttributedVideo((android.text.SpannableStringBuilder) video);
            } else if (video instanceof android.text.Spanned) {
                android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder(video);
                android.text.TextUtils.copySpansFrom(
                    (android.text.Spanned) video, 0, video.length(),
                    null, builder, 0);
                itemData.setAttributedVideo(builder);
            } else {
                itemData.setAttributedVideo(new android.text.SpannableStringBuilder(video));
            }
        }
        
        if (dataItem.getImageUrl() != null) {
            itemData.setImageUrl(dataItem.getImageUrl());
        }
        
        itemData.setGroupPosition(dataItem.getGroupPosition());
        
        return itemData;
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
