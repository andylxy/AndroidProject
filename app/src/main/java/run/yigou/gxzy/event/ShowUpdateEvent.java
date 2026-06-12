package run.yigou.gxzy.event;

public class ShowUpdateEvent {


    private boolean updateNotification = false;
    private boolean allChapterNotification = false;
    private boolean chapterNotification = false;

    private Long chapterId;

    /**
     * 获取章节 ID
     *
     * @return 章节 ID
     */
    public Long getChapterId() {
        return chapterId;
    }

    /**
     * 设置章节 ID
     *
     * @param chapterId 章节 ID
     */
    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    /**
     * 是否通知全章节更新
     */
    public boolean isAllChapterNotification() {
        return allChapterNotification;
    }

    /**
     * 设置是否通知全章节更新
     *
     * @param allChapterNotification true 发送全章节通知, false 不发送全章节通知
     */
    public void setAllChapterNotification(boolean allChapterNotification) {
        this.allChapterNotification = allChapterNotification;
    }

    public boolean isChapterNotification() {
        return chapterNotification;
    }

    /**
     * 设置是否通知指定章节更新
     *
     * @param chapterNotification true 发送章节通知, false 不发送章节通知
     */
    public void setChapterNotification(boolean chapterNotification) {
        this.chapterNotification = chapterNotification;
    }

    /**
     * 是否启用更新通知
     *
     * @return true 启用更新通知, false 禁用更新通知
     */
    public boolean isUpdateNotification() {
        return updateNotification;
    }

    /**
     * true 启用更新通知, false 禁用更新通知
     *
     * @param flag
     */
    public void setUpdateNotification(boolean flag) {
        this.updateNotification = flag;
    }
}
