package run.yigou.gxzy.EventBus;

public class ShowUpdateNotificationEvent {


    private boolean updateNotification = false;
    private boolean allChapterNotification = false;
    private boolean chapterNotification = false;

    private Long chapterId;

    /**
     * 当前章节
     *
     * @return 返回当前章节id
     */
    public Long getChapterId() {
        return chapterId;
    }

    /**
     * 当前章节
     *
     * @param chapterId 章节id
     */
    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    /**
     *  所有章节
     */
    public boolean isAllChapterNotification() {
        return allChapterNotification;
    }

    /**
     * 所有章节
     *
     * @param allChapterNotification true 标记正在下载章节通知 ,false 标记未下载章节通知
     */
    public void setAllChapterNotification(boolean allChapterNotification) {
        this.allChapterNotification = allChapterNotification;
    }

    public boolean isChapterNotification() {
        return chapterNotification;
    }

    /**
     * 当前章节
     *
     * @param chapterNotification true 标记正在下载章节通知 ,false 标记未下载章节通知
     */
    public void setChapterNotification(boolean chapterNotification) {
        this.chapterNotification = chapterNotification;
    }

    /**
     * 获取是否在下载章节通知
     *
     * @return true 标记正在下载数据 ,false 标记未下载数据
     */
    public boolean isUpdateNotification() {
        return updateNotification;
    }

    /**
     * true 标记正在下载数据 ,false 标记未下载数据
     *
     * @param flag
     */
    public void setUpdateNotification(boolean flag) {
        this.updateNotification = flag;
    }
}
