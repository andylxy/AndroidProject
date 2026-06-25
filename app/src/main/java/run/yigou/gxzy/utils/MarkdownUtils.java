package run.yigou.gxzy.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

/**
 * Markdown 工具类
 * 提供 Markdown 文本转换和 Markwon 渲染器实例的共享工厂方法
 */
public class MarkdownUtils {

    private static volatile Markwon sMarkwon;

    /**
     * 获取 Markwon 单例实例（线程安全）
     * 使用双重检查锁定避免重复初始化
     * 
     * @param context Context 对象
     * @return Markwon 实例
     */
    public static Markwon getMarkwon(Context context) {
        if (sMarkwon == null) {
            synchronized (MarkdownUtils.class) {
                if (sMarkwon == null) {
                    sMarkwon = Markwon.builder(context)
                            .usePlugin(CorePlugin.create())
                            .usePlugin(HtmlPlugin.create())
                            .usePlugin(LinkifyPlugin.create())
                            .usePlugin(StrikethroughPlugin.create())
                            .usePlugin(TablePlugin.create(context))
                            .usePlugin(TaskListPlugin.create(context))
                            .usePlugin(ImagesPlugin.create())
                            .build();
                }
            }
        }
        return sMarkwon;
    }
    
    /**
     * 重置 Markwon 实例（用于测试或配置变更）
     */
    public static void resetMarkwon() {
        sMarkwon = null;
    }

    // 预编译正则表达式
    private static final java.util.regex.Pattern CODE_BLOCK = java.util.regex.Pattern.compile("```[\\s\\S]*?```");
    private static final java.util.regex.Pattern INLINE_CODE = java.util.regex.Pattern.compile("`([^`]+)`");
    private static final java.util.regex.Pattern BOLD_ASTERISK = java.util.regex.Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final java.util.regex.Pattern BOLD_UNDERSCORE = java.util.regex.Pattern.compile("__([^_]+)__");
    private static final java.util.regex.Pattern ITALIC_ASTERISK = java.util.regex.Pattern.compile("\\*([^*]+)\\*");
    private static final java.util.regex.Pattern ITALIC_UNDERSCORE = java.util.regex.Pattern.compile("_([^_]+)_");
    private static final java.util.regex.Pattern HEADER_START = java.util.regex.Pattern.compile("^#{1,6}\\s*");
    private static final java.util.regex.Pattern HEADER_NEWLINE = java.util.regex.Pattern.compile("\\n#{1,6}\\s*");
    private static final java.util.regex.Pattern LINK = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)");
    private static final java.util.regex.Pattern IMAGE = java.util.regex.Pattern.compile("!\\[([^\\]]*)\\]\\([^)]+\\)");
    private static final java.util.regex.Pattern LIST_START = java.util.regex.Pattern.compile("^[\\-*+]\\s+");
    private static final java.util.regex.Pattern LIST_NEWLINE = java.util.regex.Pattern.compile("\\n[\\-*+]\\s+");
    private static final java.util.regex.Pattern ORDERED_LIST_START = java.util.regex.Pattern.compile("^\\d+\\.\\s+");
    private static final java.util.regex.Pattern ORDERED_LIST_NEWLINE = java.util.regex.Pattern.compile("\\n\\d+\\.\\s+");
    private static final java.util.regex.Pattern BLOCKQUOTE_START = java.util.regex.Pattern.compile("^>\\s*");
    private static final java.util.regex.Pattern BLOCKQUOTE_NEWLINE = java.util.regex.Pattern.compile("\\n>\\s*");
    private static final java.util.regex.Pattern HORIZONTAL_RULE_START = java.util.regex.Pattern.compile("^[\\-*_]{3,}$");
    private static final java.util.regex.Pattern HORIZONTAL_RULE_NEWLINE = java.util.regex.Pattern.compile("\\n[\\-*_]{3,}\\n");
    private static final java.util.regex.Pattern MULTIPLE_NEWLINES = java.util.regex.Pattern.compile("\\n{3,}");

    /**
     * 将 Markdown 格式转换为纯文本
     */
    public static String convertMarkdownToPlainText(String markdown) {
        if (markdown == null) return "";
        
        String text = markdown;
        // 移除代码块 ```...```
        text = CODE_BLOCK.matcher(text).replaceAll("");
        // 移除行内代码 `...`
        text = INLINE_CODE.matcher(text).replaceAll("$1");
        // 移除粗体 **...** 或 __...__
        text = BOLD_ASTERISK.matcher(text).replaceAll("$1");
        text = BOLD_UNDERSCORE.matcher(text).replaceAll("$1");
        // 移除斜体 *...* 或 _..._
        text = ITALIC_ASTERISK.matcher(text).replaceAll("$1");
        text = ITALIC_UNDERSCORE.matcher(text).replaceAll("$1");
        // 移除标题 # ## ### 等
        text = HEADER_START.matcher(text).replaceAll("");
        text = HEADER_NEWLINE.matcher(text).replaceAll("\n");
        // 移除链接 [text](url)
        text = LINK.matcher(text).replaceAll("$1");
        // 移除图片 ![alt](url)
        text = IMAGE.matcher(text).replaceAll("$1");
        // 移除列表符号 - * + 
        text = LIST_START.matcher(text).replaceAll("");
        text = LIST_NEWLINE.matcher(text).replaceAll("\n");
        // 移除有序列表 1. 2. 等
        text = ORDERED_LIST_START.matcher(text).replaceAll("");
        text = ORDERED_LIST_NEWLINE.matcher(text).replaceAll("\n");
        // 移除引用 >
        text = BLOCKQUOTE_START.matcher(text).replaceAll("");
        text = BLOCKQUOTE_NEWLINE.matcher(text).replaceAll("\n");
        // 移除水平线 --- *** ___
        text = HORIZONTAL_RULE_START.matcher(text).replaceAll("");
        text = HORIZONTAL_RULE_NEWLINE.matcher(text).replaceAll("\n");
        // 清理多余空行
        text = MULTIPLE_NEWLINES.matcher(text).replaceAll("\n\n");
        
        return text.trim();
    }

    /**
     * 复制文本到剪贴板
     */
    public static void copyToClipboard(Context context, String text) {
        if (context == null || text == null) return;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("聊天内容", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }
}
