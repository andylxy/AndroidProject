/*
 * 项目名: AndroidProject
 * 类名: BookRepository.java
 * 包名: run.yigou.gxzy.ui.reader.repository
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.repository;

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
import run.yigou.gxzy.data.local.helper.ConvertEntity;
import run.yigou.gxzy.data.local.helper.DataRepository;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.remote.api.BookFangApi;
import run.yigou.gxzy.data.remote.api.ChapterContentApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.ui.reader.data.BookData;
import run.yigou.gxzy.ui.reader.data.BookDataManager;
import run.yigou.gxzy.ui.reader.data.ChapterData;
import run.yigou.gxzy.ui.reader.data.DataConverter;
import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;


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

    private final DbService dbService;
    private final BookDataManager dataManager;
    private final GlobalDataHolder globalData;
    
    // 章节缓存 bookId -> 章节列表（线程安全）
    private final Map<Integer, List<Chapter>> chapterCache = new ConcurrentHashMap<>();

    /**
     * 数据回调接口
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onFailure(Exception e);
    }

    public BookRepository() {
        this.dbService = DbService.getInstance();
        this.dataManager = BookDataManager.getInstance();
        this.globalData = GlobalDataHolder.getInstance();
    }

    /**
     * 获取书籍信息
     * 
     * @param bookId 书籍 ID
     * @return 书籍信息对象，可能为 null
     */
    public TabNavBody getBookInfo(int bookId) {
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
    public List<Chapter> getChapters(int bookId) {
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
     * 下载章节内容
     * 
     * @param chapter 章节对象
     * @param lifecycleOwner 生命周期所有者
     * @param callback 下载回调
     */
    public void downloadChapter(Chapter chapter, androidx.lifecycle.LifecycleOwner lifecycleOwner, DataCallback<HH2SectionData> callback) {
        performChapterDownload(chapter, lifecycleOwner, callback);
    }

    /**
     * 章节下载核心实现（网络请求 + DB 持久化）
     * 由 downloadChapter 和 downloadChapterAsync 共享
     */
    private void performChapterDownload(Chapter chapter, androidx.lifecycle.LifecycleOwner lifecycleOwner,
                                         DataCallback<HH2SectionData> callback) {
        if (chapter == null) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("章节对象为空"));
            }
            return;
        }

        try {
            EasyHttp.get(lifecycleOwner)
                .api(new ChapterContentApi()
                    .setContentId(chapter.getChapterSection())
                    .setSignatureId(chapter.getSignatureId())
                    .setBookId(chapter.getBookId()))
                .request(new HttpCallback<HttpData<List<HH2SectionData>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<HH2SectionData>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            HH2SectionData sectionData = data.getData().get(0);
                            try {
                                DataRepository.saveBookChapterDetailList(chapter, data.getData());
                                chapter.setIsDownload(true);
                                dbService.mChapterService.updateEntity(chapter);
                                if (callback != null) {
                                    callback.onSuccess(sectionData);
                                }
                            } catch (Exception e) {
                                if (callback != null) {
                                    callback.onFailure(e);
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onFailure(new Exception("章节内容为空"));
                            }
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * 下载书籍方剂
     * 
     * @param bookId 书籍 ID
     * @param lifecycleOwner 生命周期所有者
     * @param callback 下载回调
     */
    public void downloadBookFang(int bookId, androidx.lifecycle.LifecycleOwner lifecycleOwner, DataCallback<List<Fang>> callback) {
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
                                callback.onFailure(new Exception("方剂内容为空"));
                            }
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });

        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * 查询书架书籍
     * 
     * @param bookNo 书号
     * @return 书籍列表
     */
    public ArrayList<Book> queryBookshelf(int bookNo) {
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
    public void clearCacheForBook(int bookId) {
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
    public BookData getBookData(int bookId) {
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
    private BookData loadBookDataFromDb(int bookId) {
        BookData bookData = new BookData(bookId);
        
        try {
            // 获取章节列表
            List<Chapter> chapters = getChapters(bookId);
            if (!chapters.isEmpty()) {
                // 创建 ChapterData 对象并加入 BookData
                List<ChapterData> chapterDataList = new ArrayList<>();
                for (Chapter chapter : chapters) {
                    Long signatureId = chapter.getSignatureId();
                    String title = chapter.getChapterHeader() != null ? chapter.getChapterHeader() : "";
                    Integer section = chapter.getChapterSection();
                    
                    ChapterData chapterData = new ChapterData(
                        signatureId != null ? signatureId : 0,
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
            List<DataItem> content = ConvertEntity.getBookChapterDetailList(chapter);
            
            Long signatureId = chapter.getSignatureId();
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
    private void loadFangDataToBookData(BookData bookData, int bookId) {
        try {
            // 获取方剂列表
            ArrayList<Fang> fangList = ConvertEntity.getFangDetailList(bookId);
            if (fangList != null && !fangList.isEmpty()) {
                // 转换为 DataItem 列表
                List<DataItem> fangItemList = new ArrayList<>(fangList);
                
                // 使用书籍名称作为方剂章节标题
                TabNavBody bookInfo = getBookInfo(bookId);
                String bookName = bookInfo != null ? bookInfo.getBookName() : "方剂";
                
                ChapterData fangChapterData = new ChapterData(
                    0L, 
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
     * 
     * @param chapter 章节对象
     * @param bookData 书籍数据
     * @param callback 下载回调
     */
    public void downloadChapterAsync(Chapter chapter, BookData bookData, 
                                    androidx.lifecycle.LifecycleOwner lifecycleOwner,
                                    DataCallback<ChapterData> callback) {
        performChapterDownload(chapter, lifecycleOwner, new DataCallback<HH2SectionData>() {
            @Override
            public void onSuccess(HH2SectionData sectionData) {
                try {
                    // 查找或创建 ChapterData
                    ChapterData chapterData = null;
                    if (bookData != null) {
                        chapterData = bookData.findChapterBySignature(chapter.getSignatureId());
                    }
                    if (chapterData == null) {
                        Long signatureId = chapter.getSignatureId();
                        chapterData = new ChapterData(
                            signatureId != null ? signatureId : 0,
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
                        callback.onFailure(e);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (callback != null) {
                    callback.onFailure(e);
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
    public void loadChapterLazy(int bookId, int position, androidx.lifecycle.LifecycleOwner lifecycleOwner, DataCallback<ChapterData> callback) {
        try {
            BookData bookData = getBookData(bookId);
            ChapterData chapterData = bookData.getChapter(position);
            
            if (chapterData == null) {
                if (callback != null) {
                    callback.onFailure(new Exception("未找到章节数据"));
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
                    chapter.getSignatureId() == chapterData.getSignatureId()) {
                    targetChapter = chapter;
                    break;
                }
            }
            
            if (targetChapter == null) {
                if (callback != null) {
                    callback.onFailure(new Exception("未找到目标章节"));
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
                callback.onFailure(e);
            }
        }
    }

}
