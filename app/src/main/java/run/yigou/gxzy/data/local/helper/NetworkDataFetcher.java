package run.yigou.gxzy.data.local.helper;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;

import java.util.List;

import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.gen.ChapterDao;
import run.yigou.gxzy.data.remote.api.ChapterListApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.log.EasyLog;
import androidx.lifecycle.LifecycleOwner;
import run.yigou.gxzy.data.local.entity.TabNavBody;

/**
 * 网络数据获取器
 * 
 * 负责从服务端获取数据并协调本地持久化。
 * 从 ConvertEntity 中拆分出来的网络请求职责，使 ConvertEntity 专注于纯数据转换。
 *
 * @see ConvertEntity 纯实体转换与加解密工具
 * @see DataRepository 数据库 CRUD 编排
 * @since 2024/06/21
 */
public final class NetworkDataFetcher {

    private static final String TAG = "NetworkDataFetcher";

    private NetworkDataFetcher() {
    }

    /**
     * 从网络获取章节列表并保存到数据库
     * 
     * 流程：发起 HTTP 请求 → 处理响应 → 通过 DataRepository 批量保存
     *
     * @param lifecycleOwner 生命周期宿主（用于绑定请求生命周期）
     * @param item           书籍导航项，包含 bookNo 等标识
     */
    public static void getChapterList(LifecycleOwner lifecycleOwner, TabNavBody item) {
        if (lifecycleOwner == null || item == null) {
            EasyLog.print(TAG, "参数无效，跳过章节列表获取");
            return;
        }

        EasyHttp.get(lifecycleOwner)
                .api(new ChapterListApi().setBookId(item.getBookNo()))
                .request(new HttpCallback<HttpData<List<Chapter>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<Chapter>> data) {
                        if (data == null || data.getData() == null || data.getData().isEmpty()) {
                            EasyLog.print(TAG, "书籍 " + item.getBookNo() + " 无章节数据");
                            return;
                        }

                        processChapterList(data.getData(), item);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "获取书籍 " + item.getBookNo() + " 章节列表失败: " + e.getMessage());
                        super.onFail(e);
                    }
                });
    }

    /**
     * 处理网络返回的章节列表，判断是否需要更新后批量保存
     */
    private static void processChapterList(List<Chapter> chapters, TabNavBody item) {
        try {
            // 查询已有章节
            java.util.ArrayList<Chapter> existingChapters = ConvertEntity.executeDatabaseOperation(
                () -> DbService.getInstance().mChapterService.find(
                    ChapterDao.Properties.BookId.eq(item.getBookNo())),
                "查询书籍" + item.getBookNo() + "的章节"
            );

            boolean needsUpdate = ConvertEntity.shouldUpdateChapters(existingChapters, item.getChapterCount());

            if (!needsUpdate) {
                EasyLog.print(TAG, "书籍 " + item.getBookNo() + " 章节数未变化，跳过更新");
                return;
            }

            // 清除旧数据
            if (existingChapters != null && !existingChapters.isEmpty()) {
                DbService.getInstance().mChapterService.deleteAll(
                    ChapterDao.Properties.BookId.eq(item.getBookNo()));
                EasyLog.print(TAG, "已删除书籍 " + item.getBookNo() + " 旧章节数据");
            }

            // 批量保存新章节
            int successCount = saveChaptersBatch(chapters, item.getBookNo());
            EasyLog.print(TAG, "保存书籍 " + item.getBookNo() + " 共 " + successCount + "/" + chapters.size() + " 章");

        } catch (Exception e) {
            EasyLog.print(TAG, "处理书籍 " + item.getBookNo() + " 章节列表失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存章节数据
     *
     * @param chapters 章节列表
     * @param bookId   书籍ID
     * @return 成功保存的数量
     */
    private static int saveChaptersBatch(List<Chapter> chapters, int bookId) {
        if (chapters == null || chapters.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Chapter chapter : chapters) {
            if (chapter == null) {
                continue;
            }

            try {
                ConvertEntity.executeDatabaseOperation(() -> {
                    DbService.getInstance().mChapterService.addEntity(chapter);
                    return true;
                }, "保存章节" + chapter.getId());
                successCount++;
            } catch (Exception e) {
                EasyLog.print(TAG, "保存章节失败: " + e.getMessage());
            }
        }

        return successCount;
    }
}
