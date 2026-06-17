package run.yigou.gxzy.ui.reader.helper;

import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import run.yigou.gxzy.log.EasyLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import run.yigou.gxzy.ui.dialog.MenuDialog;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.ui.reader.data.BookData;
import run.yigou.gxzy.ui.reader.data.BookDataManager;
import run.yigou.gxzy.ui.reader.data.ChapterData;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.ui.reader.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.reader.repository.BookRepository;
import run.yigou.gxzy.text.ClickLink;
import run.yigou.gxzy.text.TipsTextRenderer;
import run.yigou.gxzy.ui.reader.search.TipsSearchEngine;

/**
 * Tips 模块核心辅助类 (Facade)
 * 职责：
 * 1. 管理 BookContext (供点击事件使用)
 * 2. 也是 TextRenderer, SearchEngine, UIHelper 的统一入口 (为了兼容)
 * 3. 处理复杂的 ClickLink 逻辑（已委托至 {@link TipsClickHandler}）
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

    public static @NonNull Pattern getPattern(String sanitizedTerm) {
        return TipsSearchEngine.getPattern(sanitizedTerm);
    }

    public static void createSingleDataCopy(DataItem dataItem, Pattern pattern) {
        TipsSearchEngine.createSingleDataCopy(dataItem, pattern);
    }

    public static List<? extends DataItem> filterFang(List<? extends DataItem> sectionData, String finalStr1) {
        return TipsSearchEngine.filterFang(sectionData, finalStr1);
    }

    // ================== UI Helper Delegation ==================

    public static Rect getTextRect(ClickableSpan clickableSpan, TextView textView) {
        return TipsUIHelper.getTextRect(clickableSpan, textView);
    }

    public static void copyToClipboard(Context context, String text) {
        TipsUIHelper.copyToClipboard(context, text);
    }

    // ================== Text Renderer Delegation ==================

    /**
     * @deprecated 请直接使用 {@link #renderText(String)}，该方法功能完全相同。
     *             保留仅为兼容旧调用方。
     */
    @Deprecated
    public static SpannableStringBuilder createSpannable(String text) {
        return renderText(text);
    }

    /**
     * 使用默认 ClickLink 渲染文本为富文本 SpannableStringBuilder。
     *
     * <p>委托到 {@link TipsClickHandler#renderText(String)}，
     * 后者内部使用 {@link TipsTextRenderer#renderText(String, ClickLink)} 配合默认点击处理器。
     *
     * @param str 原始文本字符串，支持标记语法
     * @return 带有 ClickableSpan 的 SpannableStringBuilder
     */
    public static SpannableStringBuilder renderText(String str) {
        return TipsClickHandler.renderText(str);
    }

    public static SpannableStringBuilder renderText(String str, ClickLink clickLink) {
        return TipsTextRenderer.renderText(str, clickLink);
    }

    public static int getColoredTextByStrClass(String s) {
        return TipsTextRenderer.getColoredTextByStrClass(s);
    }

    public static boolean isNumeric(String str) {
        return TipsTextRenderer.isNumeric(str);
    }

    public static void renderItemNumber(SpannableStringBuilder spannableStringBuilder) {
        TipsTextRenderer.renderItemNumber(spannableStringBuilder);
    }

    public static ArrayList<Integer> getAllSubStringPos(String str, String str2) {
        return TipsTextRenderer.getAllSubStringPos(str, str2);
    }

    // ================== High Level Logic (Context Aware) ==================

    /**
     * 根据给定的药物名称获取SpannableStringBuilder对象
     */
    public static SpannableStringBuilder getSpanString(String str) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        Map<String, String> yaoAliasDict = globalData.getYaoAliasDict();

        // 获取药物的别名，若无则保留原名
        String alias = yaoAliasDict.get(str);
        str = alias != null ? alias : str;

        // 遍历药物数据
        if (globalData.getYaoMap() == null) {
            return spannableStringBuilder.append(renderText("$r{药物数据未加载}"));
        }
        Map<String, Yao> yaoMap = globalData.getYaoMap();
        Yao yaoData = yaoMap.get(str);
        if (yaoData == null) {
            return spannableStringBuilder.append(renderText("$r{药物未找到资料}"));
        }

        String yaoName = yaoData.getName();
        String yaoAlias = yaoAliasDict.get(yaoName);
        yaoName = yaoAlias != null ? yaoAlias : yaoName;

        if (!yaoName.isEmpty()) {
            spannableStringBuilder.append(yaoData.getAttributedText());
        }

        if (spannableStringBuilder.length() == 0) {
            spannableStringBuilder.append(renderText("$r{药物未找到资料}"));
        }
        spannableStringBuilder.append("\n\n");


        // 处理方剂数据
        int sectionCount = 0;
        
        List<HH2SectionData> fangList = new ArrayList<>();
        if (sCurrentBookId != -1) {
            BookData bookData = BookDataManager.getInstance().getBookData(sCurrentBookId);
            if (bookData != null && bookData.getFangData() != null) {
                 ChapterData cd = bookData.getFangData();
                 HH2SectionData section = new HH2SectionData(cd.getContent(), 0, cd.getTitle());
                 fangList.add(section);
            }
        }
        
        for (HH2SectionData sectionData : fangList) {
            SpannableStringBuilder fangBuilder = new SpannableStringBuilder();

            List<? extends DataItem> filteredFang = filterFang(sectionData.getData(), str);
            String finalStr = str;
            // 对筛选结果进行排序
            if (filteredFang != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    filteredFang.sort((dataItem1, dataItem2) -> ((Fang) dataItem1).compare((Fang) dataItem2, finalStr));
                } else {
                    Collections.sort(filteredFang, (dataItem1, dataItem2) -> ((Fang) dataItem1).compare((Fang) dataItem2, finalStr));
                }
            }

            int matchedCount = 0;

            if (filteredFang != null) {
                for (DataItem dataItem : filteredFang) {
                    for (String name : dataItem.getYaoList()) {
                        String nameAlias = yaoAliasDict.get(name);
                        name = nameAlias != null ? nameAlias : name;

                        if (name.equals(str)) {
                            matchedCount++;
                            fangBuilder.append(renderText(((Fang) dataItem).getFangNameLinkWithYaoWeight(str)));
                            break; 
                        }
                    }
                }
            }

            if (matchedCount > 0) {
                if (sectionCount > 0) {
                    spannableStringBuilder.append("\n\n");
                }
                spannableStringBuilder.append(renderText(
                        String.format("$m{%s}-$m{含“$v{%s}”凡%d方：}", sectionData.getHeader(), str, matchedCount)));
                spannableStringBuilder.append("\n").append(fangBuilder);
                sectionCount++;
            }
        }

        return spannableStringBuilder;
    }

    // ================== Dialog Helper ==================

    /** 对话框类型：仅拷贝内容 */
    public static final int DIALOG_TYPE_COPY = 1;
    /** 对话框类型：拷贝内容 + 跳转到本章内容 */
    public static final int DIALOG_TYPE_COPY_AND_JUMP = 2;
    /** 对话框类型：重新下载本章节 */
    public static final int DIALOG_TYPE_REDOWNLOAD = 3;

    @IntDef({DIALOG_TYPE_COPY, DIALOG_TYPE_COPY_AND_JUMP, DIALOG_TYPE_REDOWNLOAD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogType {}

    private static final List<String> reData = Arrays.asList( "重新下本章节");
    private static final List<String> data = Arrays.asList("拷贝内容");
    private static final List<String> noFooterData = Arrays.asList("拷贝内容", "跳转到本章内容");

    /**
     * 显示菜单对话框。
     *
     * @param context 上下文
     * @param type    对话框类型，使用 {@link #DIALOG_TYPE_COPY} / {@link #DIALOG_TYPE_COPY_AND_JUMP} / {@link #DIALOG_TYPE_REDOWNLOAD}
     * @return MenuDialog.Builder 实例
     */
    public static MenuDialog.Builder showListDialog(Context context, @DialogType int type) {
        MenuDialog.Builder builder;
        switch (type) {
            case DIALOG_TYPE_REDOWNLOAD:
                builder = new MenuDialog.Builder(context).setList(reData);
                break;
            case DIALOG_TYPE_COPY_AND_JUMP:
                builder = new MenuDialog.Builder(context).setList(noFooterData);
                break;
            case DIALOG_TYPE_COPY:
            default:
                builder = new MenuDialog.Builder(context).setList(data);
        }

        return builder;

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
