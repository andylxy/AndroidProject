package run.yigou.gxzy.ui.reader.search;

import android.text.SpannableStringBuilder;
import android.util.Pair;

import run.yigou.gxzy.log.EasyLog;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.ui.reader.adapter.model.GroupData;
import run.yigou.gxzy.ui.reader.adapter.model.ItemData;
import run.yigou.gxzy.ui.reader.data.BookData;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.ui.reader.data.ChapterData;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.text.SearchMatcher;
import run.yigou.gxzy.text.TextHighlighter;
import run.yigou.gxzy.utils.DebugLog;

/**
 * 搜索协调器
 * 负责协调全局搜索流程，整合搜索结果
 */
public class SearchCoordinator {
    
    private String bookId;

    /**
     * 当前「显示列表」（{@code TipsBookReadPresenter#getChapterContentList()} 的返回值），
     * 用于把搜索结果的 signatureId 映射成列表下标。
     *
     * <p>T6：搜索结果来自 {@code BOOK_CHAPTER} 表，书籍列表来自 {@code CHAPTER} 表，
     * 两表查询都没有 orderBy、顺序不保证一致，故搜索结果的过滤后下标不能当章节下标用。
     * 两者都带 {@code signatureId} 且逐章对应，故以它为跨表键反查位置。
     *
     * <p><b>为什么基准必须是「显示列表」而不是「全量章节列表」</b>：{@code chapterIndex}
     * 的用途是滚动 / 展开 / 重新下载，这些动作都作用在适配器当前的列表上。显示列表即
     * {@code getChapterContentList()} 的返回值，现在恒等于该书全量章节（宋版伤寒截取已移除），
     * 故显示列表与全量章节是同一坐标系。以显示列表为基准，{@code chapterIndex} 才正确。
     * 查不到的章节反查会得到 {@link GroupData#NO_CHAPTER_INDEX}，由 UI 层拦截。
     */
    private final List<run.yigou.gxzy.data.model.HH2SectionData> displayedSections;

    /**
     * 构造函数
     *
     * @param bookId 书籍ID
     * @param displayedSections 当前显示列表（含 signatureId），用于反查列表下标；
     *                          可为 null（此时搜索结果不绑定下标）
     */
    public SearchCoordinator(String bookId,
                             List<run.yigou.gxzy.data.model.HH2SectionData> displayedSections) {
        this.bookId = bookId;
        this.displayedSections = displayedSections;
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
        java.util.List<run.yigou.gxzy.data.model.HH2SectionData> allContent = 
            run.yigou.gxzy.data.local.helper.DataRepository.getBookChapterDetailList(bookId);
        
        if (allContent == null || allContent.isEmpty()) {
            EasyLog.print("❌ 无书籍数据");
            return new Pair<>(groupDataList, itemDataList);
        }
        
        EasyLog.print("开始搜索，总章节数: " + allContent.size());
        
        // 2. 获取别名字典
        run.yigou.gxzy.base.GlobalDataHolder globalData = 
            run.yigou.gxzy.base.GlobalDataHolder.getInstance();
        java.util.Map<String, String> yaoAliasDict = globalData.getYaoAliasDict();
        java.util.Map<String, String> fangAliasDict = globalData.getFangAliasDict();
        
        // 3. 调用 TipsNetHelper 进行过滤和高亮
        run.yigou.gxzy.ui.reader.entity.SearchKeyEntity searchKeyEntity = 
            new run.yigou.gxzy.ui.reader.entity.SearchKeyEntity(new StringBuilder(trimmedKeyword));
        
        java.util.ArrayList<run.yigou.gxzy.data.model.HH2SectionData> filteredData = 
            run.yigou.gxzy.ui.reader.helper.TipsNetHelper.getSearchHh2SectionData(
                searchKeyEntity, 
                allContent, 
                yaoAliasDict, 
                fangAliasDict
            );
        
        // 4. 转换为 GroupData/ItemData 格式
        int unboundCount = 0;
        for (run.yigou.gxzy.data.model.HH2SectionData section : filteredData) {
            GroupData groupData = new GroupData(section.getHeader());
            groupData.setExpanded(false); // 默认折叠

            // T6：绑定章节下标（显示列表坐标系）。查不到时保持 NO_CHAPTER_INDEX，
            // 由 UI 层拦截动作 —— 绝不能退化成用过滤后下标兜底，那正是本字段要消除的错章路径。
            int realIndex = findChapterIndexBySignature(section.getSignatureId());
            if (realIndex == GroupData.NO_CHAPTER_INDEX) {
                unboundCount++;
            }
            groupData.setChapterIndex(realIndex);

            groupDataList.add(groupData);

            // 创建 ItemData 列表
            List<ItemData> items = new ArrayList<>();
            if (section.getData() != null) {
                for (run.yigou.gxzy.data.model.DataItem dataItem : section.getData()) {
                    ItemData itemData = convertDataItemToItemData(dataItem);
                    items.add(itemData);
                }
            }
            itemDataList.add(items);
        }

        int totalMatches = searchKeyEntity.getSearchResTotalNum();
        EasyLog.print("=== 搜索完成 ===");
        EasyLog.print("匹配章节: " + groupDataList.size() + ", 总匹配数: " + totalMatches
                + ", 未绑定显示下标: " + unboundCount);

        return new Pair<>(groupDataList, itemDataList);
    }

    /**
     * 按 signatureId 在「显示列表」中反查下标（T6）。
     *
     * <p>基准是显示列表而非全量章节列表——两表填充顺序不保证一致（见类注释 T6），
     * 只有与适配器当前列表同坐标系，后续滚动 / 展开才不会越界。
     *
     * @param signatureId 章节签名 id
     * @return 显示列表中的下标；查不到返回 {@link GroupData#NO_CHAPTER_INDEX}
     */
    private int findChapterIndexBySignature(String signatureId) {
        if (displayedSections == null || signatureId == null || signatureId.isEmpty()) {
            return GroupData.NO_CHAPTER_INDEX;
        }
        for (int i = 0; i < displayedSections.size(); i++) {
            run.yigou.gxzy.data.model.HH2SectionData section = displayedSections.get(i);
            if (section != null && signatureId.equals(section.getSignatureId())) {
                return i;
            }
        }
        return GroupData.NO_CHAPTER_INDEX;
    }
    
    /**
     * 将 DataItem 转换为 ItemData (已高亮)
     * 
     * 注意：直接使用原始 CharSequence，不要创建新的 SpannableStringBuilder，
     * 否则会丢失 ClickableSpan！
     */
    private run.yigou.gxzy.ui.reader.adapter.model.ItemData convertDataItemToItemData(run.yigou.gxzy.data.model.DataItem dataItem) {
        ItemData itemData = new ItemData();
        
        // ✅ 直接使用原始对象，保留 ClickableSpan
        // 不要使用 new SpannableStringBuilder()，那会丢失 Span！
        if (dataItem.getAttributedText() != null) {
            CharSequence text = dataItem.getAttributedText();
            if (text instanceof android.text.SpannableStringBuilder) {
                itemData.setTextSpan((android.text.SpannableStringBuilder) text);
            } else {
                // 如果不是 SpannableStringBuilder，需要复制并保留 Span
                android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder(text);
                android.text.TextUtils.copySpansFrom(
                    (android.text.Spanned) text, 0, text.length(),
                    null, builder, 0);
                itemData.setTextSpan(builder);
            }
        }
        
        if (dataItem.getAttributedNote() != null) {
            CharSequence note = dataItem.getAttributedNote();
            if (note instanceof android.text.SpannableStringBuilder) {
                itemData.setNoteSpan((android.text.SpannableStringBuilder) note);
            } else if (note instanceof android.text.Spanned) {
                android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder(note);
                android.text.TextUtils.copySpansFrom(
                    (android.text.Spanned) note, 0, note.length(),
                    null, builder, 0);
                itemData.setNoteSpan(builder);
            } else {
                itemData.setNoteSpan(new android.text.SpannableStringBuilder(note));
            }
        }
        
        if (dataItem.getAttributedSectionVideo() != null) {
            CharSequence video = dataItem.getAttributedSectionVideo();
            if (video instanceof android.text.SpannableStringBuilder) {
                itemData.setVideoSpan((android.text.SpannableStringBuilder) video);
            } else if (video instanceof android.text.Spanned) {
                android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder(video);
                android.text.TextUtils.copySpansFrom(
                    (android.text.Spanned) video, 0, video.length(),
                    null, builder, 0);
                itemData.setVideoSpan(builder);
            } else {
                itemData.setVideoSpan(new android.text.SpannableStringBuilder(video));
            }
        }
        
        if (dataItem.getImageUrl() != null) {
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
            itemData.setTextSpan(highlightedText);
        }
        
        // 高亮注释
        if (dataItem.getAttributedNote() != null) {
            SpannableStringBuilder highlightedNote = 
                TextHighlighter.createHighlighted(dataItem.getAttributedNote(), keyword);
            itemData.setNoteSpan(highlightedNote);
        }
        
        // 高亮视频标注
        if (dataItem.getAttributedSectionVideo() != null) {
            SpannableStringBuilder highlightedVideo = 
                TextHighlighter.createHighlighted(dataItem.getAttributedSectionVideo(), keyword);
            itemData.setVideoSpan(highlightedVideo);
        }
        
        // 复制其他字段
        if (dataItem.getImageUrl() != null) {
        }
        
        itemData.setGroupPosition(dataItem.getGroupPosition());
        
        return itemData;
    }
}
