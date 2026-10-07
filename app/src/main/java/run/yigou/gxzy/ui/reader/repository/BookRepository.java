/*
 * 项目名: AndroidProject
 * 类名: BookRepository.java
 * 包名: run.yigou.gxzy.ui.reader.repository
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.repository;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import run.yigou.gxzy.log.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.data.local.entity.Book;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.gen.BookDao;
import run.yigou.gxzy.data.local.gen.ChapterDao;
import run.yigou.gxzy.data.local.helper.DataRepository;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.local.helper.LocalServices;
import run.yigou.gxzy.data.remote.api.BookFangApi;
import run.yigou.gxzy.manager.chapter.ChapterContentManager;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.ui.reader.data.BookData;
import run.yigou.gxzy.ui.reader.data.BookDataManager;
import run.yigou.gxzy.ui.reader.data.ChapterData;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;
import run.yigou.gxzy.manager.Callback;
import run.yigou.gxzy.utils.ThreadUtil;


/**
 * 书籍数据仓库
 * 
 * 核心职责:
 * 1. 管理书籍章节数据的加载和缓存
 * 2. 处理章节和方剂的下载
 * 3. 提供本地和远程数据访问
 * 4. 管理 BookData 缓存
 */
public class BookRepository {

    private final LocalServices dbService;
    private final BookDataManager dataManager;
    private final GlobalDataHolder globalData;
    
    // 章节缓存 bookId -> 章节列表（线程安全）
    private final Map<String, List<Chapter>> chapterCache = new ConcurrentHashMap<>();

    public BookRepository() {
        this.dbService = LocalServices.getInstance();
        this.dataManager = BookDataManager.getInstance();
        this.globalData = GlobalDataHolder.getInstance();
    }

    /**
     * 获取书籍信息
     * 
     * @param bookId 书籍 ID
     * @return 书籍信息对象，可能为 null
     */
    public TabNavBody getBookInfo(String bookId) {
        try {
            return globalData.getBookInfo(bookId);
        } catch (Exception e) {
            EasyLog.print("BookRepository", "获取书籍信息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取章节列表（带缓存）
     * 
     * @param bookId 书籍 ID
     * @return 章节列表
     */
    public List<Chapter> getChapters(String bookId) {
        List<Chapter> cached = chapterCache.get(bookId);
        if (cached != null) {
            return cached;
        }

        try {
            ArrayList<Chapter> chapters = dbService.mChapterService.find(
                ChapterDao.Properties.BookId.eq(bookId)
            );

            if (chapters != null) {
                chapterCache.put(bookId, chapters);
                return chapters;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            EasyLog.print("BookRepository", "数据库加载失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 异步获取章节列表（数据库读取发生在串行后台线程，结果回主线程）。
     *
     * <p>为什么单独给一个异步入口：{@link #getChapters(String)} 是同步返回值型的，
     * 调用方拿到列表后还要接着做一连串 UI 工作，把方法体整体挪到后台会破坏返回值约定。
     * 新方法用 {@code *Async} 命名，是为了让调用方一眼看出"结果不再同步返回"。
     *
     * <p>读失败按"没有章节"回调（空列表），与同步版失败返回空列表的语义一致。
     *
     * @param bookId   书籍 ID
     * @param callback 结果回调（主线程），可为 null
     */
    public void getChaptersAsync(final String bookId, final Callback<List<Chapter>> callback) {
        final List<Chapter> cached = chapterCache.get(bookId);
        if (cached != null) {
            deliverOnUi(callback, cached);
            return;
        }
        DbService.getInstance().runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                try {
                    deliverOnUi(callback, getChapters(bookId));
                } catch (Throwable t) {
                    // 读失败按"没有章节"回调，而不是走 onError：
                    // 同步版 getChapters 的失败语义就是返回空列表，调用方（Presenter）
                    // 对两种情况的处理都是"章节列表为空"提示，改成 onError 会变成另一种文案。
                    EasyLog.print(t);
                    deliverOnUi(callback, new ArrayList<Chapter>());
                }
            }
        });
    }

    /**
     * 异步查询书架书籍（数据库读取发生在串行后台线程，结果回主线程）。
     *
     * @param bookNo   书号
     * @param callback 结果回调（主线程），可为 null
     */
    public void queryBookshelfAsync(final String bookNo, final Callback<ArrayList<Book>> callback) {
        DbService.getInstance().runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                try {
                    deliverOnUi(callback, queryBookshelf(bookNo));
                } catch (Throwable t) {
                    // 同 getChaptersAsync：同步版失败返回空列表，这里保持同一种语义。
                    EasyLog.print(t);
                    deliverOnUi(callback, new ArrayList<Book>());
                }
            }
        });
    }

    /** 后台任务的结果统一回主线程。 */
    private static <T> void deliverOnUi(final Callback<T> callback, final T data) {
        ThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onSuccess(data);
                }
            }
        });
    }

    /**
     * 下载章节内容
     * 委托给 ChapterContentManager 统一管理
     * 
     * @param chapter 章节对象
     * @param lifecycleOwner 生命周期所有者
     * @param callback 下载回调
     */
    public void downloadChapter(Chapter chapter, androidx.lifecycle.LifecycleOwner lifecycleOwner, Callback<HH2SectionData> callback) {
        // 委托给 ChapterContentManager 统一管理
        ChapterContentManager.getInstance().fetchChapterContent(lifecycleOwner, chapter, 
            new ChapterContentManager.ContentCallback() {
                @Override
                public void onSuccess(Chapter c, HH2SectionData data) {
                    if (callback != null) {
                        callback.onSuccess(data);
                    }
                }
                @Override
                public void onFailure(Chapter c, Exception e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * 下载书籍方剂
     * 
     * @param bookId 书籍 ID
     * @param lifecycleOwner 生命周期所有者
     * @param callback 下载回调
     */
    public void downloadBookFang(String bookId, androidx.lifecycle.LifecycleOwner lifecycleOwner, Callback<List<Fang>> callback) {
        try {
            EasyHttp.get(lifecycleOwner)
                .api(new BookFangApi().setBookId(bookId))
                .request(new HttpCallback<HttpData<List<Fang>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<Fang>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            List<Fang> fangList = data.getData();

                            // 异步保存方剂数据到本地
                            new Thread(() -> {
                                try {
                                    DataRepository.saveFangDetailList(fangList, bookId);
                                } catch (Exception e) {
                                    EasyLog.print("BookRepository", "异步保存方剂数据失败: " + e.getMessage());
                                }
                            }).start();

                            if (callback != null) {
                                callback.onSuccess(fangList);
                            }

                        } else {
                            if (callback != null) {
                                callback.onError(new Exception("方剂内容为空"));
                            }
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                });

        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    /**
     * 查询书架书籍
     * 
     * @param bookNo 书号
     * @return 书籍列表
     */
    public ArrayList<Book> queryBookshelf(String bookNo) {
        try {
            return dbService.mBookService.find(BookDao.Properties.BookNo.eq(bookNo));
        } catch (Exception e) {
            EasyLog.print("BookRepository", "查询书架失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 添加书籍到书架
     * 
     * @param book 书籍对象
     * @return true-成功, false-失败
     */
    public boolean addToBookshelf(Book book) {
        try {
            dbService.mBookService.addEntity(book);
            return true;
        } catch (Exception e) {
            EasyLog.print("BookRepository", "添加书架失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 加入书架（写入发生在串行后台线程）。
     *
     * <p>为什么与 {@link #addToBookshelf(Book)} 分开重命名：后者返回 boolean 表示"写成功了没"，
     * 挪到后台之后就没有这个值可用了。名字里带 Async 是要让调用方一眼看出结果不再同步返回——
     * 比"返回一个其实恒为 true 的布尔"诚实。
     *
     * <p>任务内必须自己 try/catch：串行入口用 {@code Executor.execute} 提交，异常不进 Future，
     * 只会杀掉 worker 线程。
     *
     * @param book 书籍对象
     */
    public void addToBookshelfAsync(final Book book) {
        DbService.getInstance().runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                try {
                    dbService.mBookService.addEntity(book);
                } catch (Throwable t) {
                    EasyLog.print("BookRepository", "添加书架失败: " + t.getMessage());
                    EasyLog.print(t);
                }
            }
        });
    }

    /**
     * 更新阅读进度
     * 
     * @param book 书籍对象
     * @return true-成功, false-失败
     */
    public boolean updateReadingProgress(Book book) {
        try {
            dbService.mBookService.updateEntity(book);
            return true;
        } catch (Exception e) {
            EasyLog.print("BookRepository", "更新阅读进度失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 更新阅读进度（写入发生在串行后台线程）。
     *
     * <p>命名理由与 {@link #addToBookshelfAsync(Book)} 相同：调用方不再能拿到"是否写成功"，
     * 方法名必须把这一点讲清楚。
     *
     * @param book 书籍对象
     */
    public void updateReadingProgressAsync(final Book book) {
        DbService.getInstance().runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                try {
                    dbService.mBookService.updateEntity(book);
                } catch (Throwable t) {
                    EasyLog.print("BookRepository", "更新阅读进度失败: " + t.getMessage());
                    EasyLog.print(t);
                }
            }
        });
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        chapterCache.clear();
        EasyLog.print("BookRepository", "清空所有缓存");
    }

    /**
     * 清空指定书籍缓存
     * 
     * @param bookId 书籍 ID
     */
    public void clearCacheForBook(String bookId) {
        chapterCache.remove(bookId);
        EasyLog.print("BookRepository", "清空书籍缓存: bookId=" + bookId);
    }

    // ==================== 懒加载 API ====================

    /**
     * 生成书籍唯一ID
     */
    public String generateBookId() {
        return dbService.mBookService.getUUID();
    }

    /**
     * 获取书籍数据（懒加载模式）
     * 如果缓存未命中，触发懒加载流程
     * 
     * @param bookId 书籍 ID
     * @return 书籍数据对象，可能为 null
     */
    public BookData getBookData(String bookId) {
        BookData cached = dataManager.getFromCache(bookId);
        if (cached != null && cached.isFullyLoaded()) {
            return cached;
        }

        BookData bookData = loadBookDataFromDb(bookId);
        dataManager.putToCache(bookId, bookData);
        
        return bookData;
    }

    /**
     * 从数据库加载书籍数据
     */
    private BookData loadBookDataFromDb(String bookId) {
        BookData bookData = new BookData(bookId);
        
        try {
            // 获取章节列表
            List<Chapter> chapters = getChapters(bookId);
            if (!chapters.isEmpty()) {
                // 创建 ChapterData 对象并加入 BookData
                List<ChapterData> chapterDataList = new ArrayList<>();
                for (Chapter chapter : chapters) {
                String signatureId = chapter.getSignatureId();
                String title = chapter.getChapterHeader() != null ? chapter.getChapterHeader() : "";
                Integer section = chapter.getChapterSection();
                
                ChapterData chapterData = new ChapterData(
                    signatureId != null ? signatureId : "",
                    title,
                    section != null ? section : 0
                );
                    
                    chapterDataList.add(chapterData);
                }
                
                bookData.setChapters(chapterDataList);
            }
            
            // 加载已下载的章节内容
            for (Chapter chapter : chapters) {
                if (chapter.getIsDownload()) {
                    // 加载章节内容
                    loadChapterContent(bookData, chapter);
                }
            }
            
            // 加载方剂数据到 BookData（如果是方剂类书籍）
            loadFangDataToBookData(bookData, bookId);
            
            bookData.markAsFullyLoaded();
            
        } catch (Exception e) {
            EasyLog.print("BookRepository", "加载书籍数据失败: " + e.getMessage());
        }
        
        return bookData;
    }

    /**
     * 加载章节内容到 BookData
     * 
     * @param bookData 书籍数据
     * @param chapter 章节对象
     */
    public void loadChapterContent(BookData bookData, Chapter chapter) {
        try {
            List<DataItem> content = DataRepository.getBookChapterDetailList(chapter);
            
            String signatureId = chapter.getSignatureId();
            if (content != null && !content.isEmpty()) {
                ChapterData chapterData = bookData.findChapterBySignature(signatureId);
                if (chapterData != null) {
                    chapterData.setContent(content);
                }
            }
        } catch (Exception e) {
            EasyLog.print("BookRepository", "加载章节内容失败: " + e.getMessage());
        }
    }

    /**
     * 将方剂数据加载到 BookData（仅对方剂类书籍有效）
     * 
     * @param bookData 书籍数据
     * @param bookId 书籍ID
     */
    private void loadFangDataToBookData(BookData bookData, String bookId) {
        try {
            // 获取方剂列表
            ArrayList<Fang> fangList = DataRepository.getFangDetailList(bookId);
            if (fangList != null && !fangList.isEmpty()) {
                // 转换为 DataItem 列表
                List<DataItem> fangItemList = new ArrayList<>(fangList);
                
                // 使用书籍名称作为方剂章节标题
                TabNavBody bookInfo = getBookInfo(bookId);
                String bookName = bookInfo != null ? bookInfo.getBookName() : "方剂";
                
                ChapterData fangChapterData = new ChapterData(
                    "", 
                    bookName + "方剂", 
                    0, 
                    fangItemList
                );
                
                bookData.setFangData(fangChapterData);
            }
        } catch (Exception e) {
            EasyLog.print("BookRepository", "加载方剂数据失败: " + e.getMessage());
        }
    }

    /**
     * 异步下载章节内容并加载
     * 委托给 ChapterContentManager 统一管理
     * 
     * @param chapter 章节对象
     * @param bookData 书籍数据
     * @param callback 下载回调
     */
    public void downloadChapterAsync(Chapter chapter, BookData bookData, 
                                    androidx.lifecycle.LifecycleOwner lifecycleOwner,
                                    Callback<ChapterData> callback) {
        // 委托给 ChapterContentManager 统一管理
        ChapterContentManager.getInstance().fetchChapterContent(lifecycleOwner, chapter, 
            new ChapterContentManager.ContentCallback() {
                @Override
                public void onSuccess(Chapter c, HH2SectionData sectionData) {
                    try {
                        // 查找或创建 ChapterData
                        ChapterData chapterData = null;
                        if (bookData != null) {
                            chapterData = bookData.findChapterBySignature(chapter.getSignatureId());
                        }
                        if (chapterData == null) {
                            String signatureId = chapter.getSignatureId();
                            chapterData = new ChapterData(
                                signatureId != null ? signatureId : "",
                                chapter.getChapterHeader() != null ? chapter.getChapterHeader() : "",
                                chapter.getChapterSection()
                            );
                        }
                        // 将 HH2SectionData 转换为内容列表
                        if (sectionData.getData() != null) {
                            List<DataItem> content = new ArrayList<>();
                            for (Object item : sectionData.getData()) {
                                if (item instanceof DataItem) {
                                    content.add((DataItem) item);
                                }
                            }
                            chapterData.setContent(content);
                        }
                        if (callback != null) {
                            callback.onSuccess(chapterData);
                        }
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }

                @Override
                public void onFailure(Chapter c, Exception e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            });
    }

    /**
     * 懒加载章节内容
     * 根据章节下载状态自动选择加载或下载方式
     * 
     * @param bookId 书籍 ID
     * @param position 章节位置
     * @param lifecycleOwner 生命周期所有者（Fragment/Activity），可为 null
     * @param callback 回调接口
     */
    public void loadChapterLazy(String bookId, int position, LifecycleOwner lifecycleOwner, Callback<ChapterData> callback) {
        try {
            BookData bookData = getBookData(bookId);
            ChapterData chapterData = bookData.getChapter(position);
            
            if (chapterData == null) {
                if (callback != null) {
                    callback.onError(new Exception("未找到章节数据"));
                }
                return;
            }
            
            // 内容已就绪，直接返回
            if (chapterData.isContentLoaded() && !chapterData.isEmpty()) {
                if (callback != null) {
                    callback.onSuccess(chapterData);
                }
                return;
            }
            
            // 通过签名ID找到对应的 Chapter 对象
            List<Chapter> chapters = getChapters(bookId);
            Chapter targetChapter = null;
            for (Chapter chapter : chapters) {
                if (chapter.getSignatureId() != null && 
                    chapter.getSignatureId().equals(chapterData.getSignatureId())) {
                    targetChapter = chapter;
                    break;
                }
            }
            
            if (targetChapter == null) {
                if (callback != null) {
                    callback.onError(new Exception("未找到目标章节"));
                }
                return;
            }
            
            if (targetChapter.getIsDownload()) {
                loadChapterContent(bookData, targetChapter);
                if (callback != null) {
                    callback.onSuccess(chapterData);
                }
            } else {
                downloadChapterAsync(targetChapter, bookData, lifecycleOwner, callback);
            }
            
        } catch (Exception e) {
            EasyLog.print("BookRepository", "懒加载失败: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

}
