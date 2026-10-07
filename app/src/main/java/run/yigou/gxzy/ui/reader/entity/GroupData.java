package run.yigou.gxzy.ui.reader.entity;

/**
 * 组数据类
 * 封装Group级别的数据
 */
public class GroupData {
    /** 章节未绑定真实位置时的哨兵值（列表下标从 0 开始，故 -1 可安全表示「无」） */
    public static final int NO_CHAPTER_INDEX = -1;

    private String title;
    private boolean isExpanded;

    /**
     * 本章在「当前显示列表」中的下标（显示列表 = {@code presenter.getChapterContentList()}，
     * 可能是全量章节的过滤片段，如宋版伤寒的 {@code subList}）。
     *
     * <p>T6：为什么需要它 —— 列表有两套数据源且顺序不保证一致：
     * 非搜索列表来自 {@code CHAPTER} 表、搜索结果来自 {@code BOOK_CHAPTER} 表，
     * 两处查询都没有 {@code orderBy}，返回顺序由 SQLite 决定。因此搜索结果的
     * {@code groupPosition}（过滤后下标）不能直接当章节下标用，跳转会命中错章。
     *
     * <p>绑定规则：构造列表时由调用方按 {@code signatureId} 反查真实位置填入；
     * 查不到时填 {@link #NO_CHAPTER_INDEX}，UI 层必须据此拦截动作，
     * 不能退化成用 {@code groupPosition} 兜底 —— 那正是本字段要消除的错章路径。
     */
    private int chapterIndex = NO_CHAPTER_INDEX;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public void setChapterIndex(int chapterIndex) {
        this.chapterIndex = chapterIndex;
    }

}
