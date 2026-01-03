package run.yigou.gxzy.ui.tips.tipsutils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.Pair;
import android.widget.TextView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.other.EasyLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import run.yigou.gxzy.ui.dialog.MenuDialog;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.data.BookData;
import run.yigou.gxzy.ui.tips.data.BookDataManager;
import run.yigou.gxzy.ui.tips.data.ChapterData;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
import run.yigou.gxzy.ui.tips.entity.GroupData;
import run.yigou.gxzy.ui.tips.entity.ItemData;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.repository.BookRepository;
import run.yigou.gxzy.ui.tips.utils.SearchDataAdapter;
import run.yigou.gxzy.ui.tips.widget.TipsLittleMingCiViewWindow;
import run.yigou.gxzy.ui.tips.widget.TipsLittleTableViewWindow;
import run.yigou.gxzy.utils.DebugLog;

/**
 * Tips 模块核心辅助类 (Facade)
 * 职责：
 * 1. 管理 BookContext (供点击事件使用)
 * 2. 也是 TextRenderer, SearchEngine, UIHelper 的统一入口 (为了兼容)
 * 3. 处理复杂的 ClickLink 逻辑
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

    public static SpannableStringBuilder createSpannable(String text) {
        // 使用默认的 renderText (带点击事件)
        return renderText(text);
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

        Map<String, String> yaoAliasDict = GlobalDataHolder.getInstance().getYaoAliasDict();

        // 获取药物的别名，若无则保留原名
        String alias = yaoAliasDict.get(str);
        str = alias != null ? alias : str;

        if (true) {
            // 遍历药物数据
            if (GlobalDataHolder.getInstance().getYaoMap() == null) {
                // throw new IllegalArgumentException("GlobalDataHolder or yaoMap is null");
                return spannableStringBuilder.append(TipsNetHelper.renderText("$r{药物数据未加载}"));
            }
            Map<String, Yao> yaoMap = GlobalDataHolder.getInstance().getYaoMap();
            Yao yaoData = yaoMap.get(str);
            if (yaoData == null) {
                return spannableStringBuilder.append(TipsNetHelper.renderText("$r{药物未找到资料}"));
            }

            String yaoName = yaoData.getName();
            String yaoAlias = yaoAliasDict.get(yaoName);
            yaoName = yaoAlias != null ? yaoAlias : yaoName;

            if (!yaoName.isEmpty()) {
                spannableStringBuilder.append(yaoData.getAttributedText());
            }

            if (spannableStringBuilder.length() == 0) {
                spannableStringBuilder.append(TipsNetHelper.renderText("$r{药物未找到资料}"));
            }
            spannableStringBuilder.append("\n\n");
        }

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
                    for (String yaoName : dataItem.getYaoList()) {
                        String yaoAlias = yaoAliasDict.get(yaoName);
                        yaoName = yaoAlias != null ? yaoAlias : yaoName;

                        if (yaoName.equals(str)) {
                            matchedCount++;
                            fangBuilder.append(TipsNetHelper.renderText(((Fang) dataItem).getFangNameLinkWithYaoWeight(str)));
                            break; 
                        }
                    }
                }
            }

            if (matchedCount > 0) {
                if (sectionCount > 0) {
                    spannableStringBuilder.append("\n\n");
                }
                spannableStringBuilder.append(TipsNetHelper.renderText(
                        String.format("$m{%s}-$m{含“$v{%s}”凡%d方：}", sectionData.getHeader(), str, matchedCount)));
                spannableStringBuilder.append("\n").append(fangBuilder);
                sectionCount++;
            }
        }

        return spannableStringBuilder;
    }

    public static SpannableStringBuilder renderText(String str) {
        return renderText(str, new ClickLink() {

            @Override
            public void clickYaoLink(TextView textView, ClickableSpan clickableSpan) {
                EasyLog.print("=== clickYaoLink() 新架构 ===");
                
                String keyword = textView.getText()
                        .subSequence(textView.getSelectionStart(), textView.getSelectionEnd())
                        .toString();
                EasyLog.print("药物: " + keyword);
                
                if (sBookRepository == null || sCurrentBookId == -1) {
                    EasyLog.print("❌ BookRepository未设置，无法搜索");
                    return;
                }
                
                SearchDataAdapter adapter = new SearchDataAdapter(sBookRepository, sCurrentBookId);
                Pair<List<GroupData>, List<List<ItemData>>> data = 
                        adapter.searchYaoContent(keyword.trim());
                
                EasyLog.print("Groups: " + data.first.size());
                
                Rect textRect = TipsNetHelper.getTextRect(clickableSpan, textView);
                
                TipsLittleTableViewWindow window = new TipsLittleTableViewWindow();
                window.setData(textView.getContext(), data);
                window.setFang(keyword); 
                window.setRect(textRect);
                
                if (textView.getContext() instanceof Activity) {
                    window.show(((Activity) textView.getContext()).getFragmentManager());
                }
                
                EasyLog.print("=== clickYaoLink() 完成 ===");
            }

            @Override
            public void clickFangLink(TextView textView, ClickableSpan clickableSpan) {
                EasyLog.print("=== clickFangLink() 新架构 ===");
                
                String keyword = textView.getText()
                        .subSequence(textView.getSelectionStart(), textView.getSelectionEnd())
                        .toString();
                EasyLog.print("方剂: " + keyword);
                
                if (sBookRepository == null || sCurrentBookId == -1) {
                    EasyLog.print("❌ BookRepository未设置，无法搜索");
                    return;
                }
                
                SearchDataAdapter adapter = new SearchDataAdapter(sBookRepository, sCurrentBookId);
                Pair<List<GroupData>, List<List<ItemData>>> data = 
                        adapter.searchFangContent(keyword.trim());
                
                EasyLog.print("Groups: " + data.first.size());
                
                Rect textRect = TipsNetHelper.getTextRect(clickableSpan, textView);
                
                TipsLittleTableViewWindow window = new TipsLittleTableViewWindow();
                window.setData(textView.getContext(), data);
                window.setFang(keyword);
                window.setRect(textRect);
                
                Context context = textView.getContext();
                if (context instanceof Activity) {
                    window.show(((Activity) context).getFragmentManager());
                } else {
                    EasyLog.print("❌ Context不是Activity!");
                }
                
                EasyLog.print("=== clickFangLink() 完成 ===");
            }

            @Override
            public void clickMingCiLink(TextView textView, ClickableSpan clickableSpan) {
                EasyLog.print("=== clickMingCiLink() 新架构 ===");
                
                String keyword = textView.getText()
                        .subSequence(textView.getSelectionStart(), textView.getSelectionEnd())
                        .toString();
                EasyLog.print("名词: " + keyword);
                
                if (sBookRepository == null || sCurrentBookId == -1) {
                    EasyLog.print("❌ BookRepository未设置，无法搜索");
                    return;
                }
                
                SearchDataAdapter adapter = new SearchDataAdapter(sBookRepository, sCurrentBookId);
                Pair<List<GroupData>, List<List<ItemData>>> data = 
                        adapter.searchMingCiContent(keyword.trim());
                
                EasyLog.print("Groups: " + data.first.size());
                
                Rect textRect = TipsNetHelper.getTextRect(clickableSpan, textView);
                
                TipsLittleMingCiViewWindow window = new TipsLittleMingCiViewWindow();
                window.setData(textView.getContext(), data);
                window.setRect(textRect);
                
                if (textView.getContext() instanceof Activity) {
                    window.show(((Activity) textView.getContext()).getFragmentManager());
                }
                
                EasyLog.print("=== clickMingCiLink() 完成 ===");
            }

        });
    }

    // ================== Dialog Helper (Keep) ==================

    private static final List<String> reData = Arrays.asList( "重新下本章节");
    private static final List<String> data = Arrays.asList("拷贝内容");
    private static final List<String> noFooterData = Arrays.asList("拷贝内容", "跳转到本章内容");

    public static MenuDialog.Builder showListDialog(Context context, int type) {
        MenuDialog.Builder builder;
        switch (type) {
            case 3:
                builder = new MenuDialog.Builder(context).setList(reData);
                break;
            case 2:
                builder = new MenuDialog.Builder(context).setList(noFooterData);
                break;
            case 1:
            default:
                builder = new MenuDialog.Builder(context).setList(data);
        }

        return builder;

    }

    // ================== Utils (Keep) ==================

    @FunctionalInterface
    public interface Condition<T> {
        boolean test(T t);
    }

    public static <T> boolean some(List<T> list, Condition<T> condition) {
        if (list == null || condition == null) {
            return false;
        }
        for (T element : list) {
            if (condition.test(element)) {
                return true;
            }
        }
        return false;
    }
}
