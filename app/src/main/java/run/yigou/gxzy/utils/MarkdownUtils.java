package run.yigou.gxzy.utils;

/**
 * Markdown 工具类
 */
public class MarkdownUtils {

    /**
     * 将 Markdown 格式转换为纯文本
     */
    public static String convertMarkdownToPlainText(String markdown) {
        if (markdown == null) return "";
        
        String text = markdown;
        // 移除代码块 ```...```
        text = text.replaceAll("```[\\s\\S]*?```", "");
        // 移除行内代码 `...`
        text = text.replaceAll("`([^`]+)`", "$1");
        // 移除粗体 **...** 或 __...__
        text = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        text = text.replaceAll("__([^_]+)__", "$1");
        // 移除斜体 *...* 或 _..._
        text = text.replaceAll("\\*([^*]+)\\*", "$1");
        text = text.replaceAll("_([^_]+)_", "$1");
        // 移除标题 # ## ### 等
        text = text.replaceAll("^#{1,6}\\s*", "");
        text = text.replaceAll("\\n#{1,6}\\s*", "\n");
        // 移除链接 [text](url)
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        // 移除图片 ![alt](url)
        text = text.replaceAll("!\\[([^\\]]*)\\]\\([^)]+\\)", "$1");
        // 移除列表符号 - * + 
        text = text.replaceAll("^[\\-*+]\\s+", "");
        text = text.replaceAll("\\n[\\-*+]\\s+", "\n");
        // 移除有序列表 1. 2. 等
        text = text.replaceAll("^\\d+\\.\\s+", "");
        text = text.replaceAll("\\n\\d+\\.\\s+", "\n");
        // 移除引用 >
        text = text.replaceAll("^>\\s*", "");
        text = text.replaceAll("\\n>\\s*", "\n");
        // 移除水平线 --- *** ___
        text = text.replaceAll("^[\\-*_]{3,}$", "");
        text = text.replaceAll("\\n[\\-*_]{3,}\\n", "\n");
        // 清理多余空行
        text = text.replaceAll("\\n{3,}", "\n\n");
        
        return text.trim();
    }
}
