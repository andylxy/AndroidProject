package run.yigou.gxzy.ui.reader.helper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.data.model.HH2SectionData;
import run.yigou.gxzy.ui.reader.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.reader.repository.BookRepository;
import run.yigou.gxzy.ui.reader.search.TipsSearchEngine;

/**
 * Tips 模块上下文管理
 *
 * <p>职责：
 * <ol>
 *   <li>管理 BookRepository + BookId 上下文（供 {@link TipsClickHandler} 点击事件使用）</li>
 *   <li>提供 {@link #getSearchHh2SectionData} 搜索引擎委托</li>
 * </ol>
 */
public class TipsNetHelper {

    // 【新架构】BookRepository上下文
    private static BookRepository sBookRepository = null;
    private static int sCurrentBookId = -1;

    /**
     * 设置当前BookRepository上下文（在TipsBookReadPresenter中调用）
     */
    public static void setBookContext(BookRepository repository, int bookId) {
        sBookRepository = repository;
        sCurrentBookId = bookId;
        EasyLog.print("=== TipsNetHelper.setBookContext ===");
        EasyLog.print("BookId: " + bookId);
        EasyLog.print("Repository: " + (repository != null ? "已设置" : "null"));
    }

    // ================== Search Engine Delegation ==================

    public static @NonNull ArrayList<HH2SectionData> getSearchHh2SectionData(SearchKeyEntity searchKeyEntity,
                                                                             List<HH2SectionData> contentList,
                                                                             Map<String, String> yaoAliasDict,
                                                                             Map<String, String> fangAliasDict) {
        return TipsSearchEngine.getSearchHh2SectionData(searchKeyEntity, contentList, yaoAliasDict, fangAliasDict);
    }
    
    // ================== Package-Private Context Accessors ==================

    /**
     * 获取当前 BookRepository 上下文（package-private，供同包 TipsClickHandler 使用）。
     */
    static BookRepository getBookRepository() {
        return sBookRepository;
    }

    /**
     * 获取当前书籍 ID（package-private，供同包 TipsClickHandler 使用）。
     */
    static int getCurrentBookId() {
        return sCurrentBookId;
    }
}
