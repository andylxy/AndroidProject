/*
 * 项目名: AndroidProject
 * 类名: BookRepository.java
 * 包名: run.yigou.gxzy.ui.tips.repository
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.repository;

import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.gen.BookDao;
import run.yigou.gxzy.greendao.gen.ChapterDao;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.BookFangApi;
import run.yigou.gxzy.http.api.ChapterContentApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.data.BookData;
import run.yigou.gxzy.ui.tips.data.BookDataManager;
import run.yigou.gxzy.ui.tips.data.ChapterData;
import run.yigou.gxzy.ui.tips.data.DataConverter;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.utils.DebugLog;


/**
 * 书籍数据仓库
 * 
 * 职责：
 * 1. 封装所有数据库操作
 * 2. 封装所有网络请求
 * 3. 提供统一的数据访问接口
 * 4. 管理数据缓存策略
 */
public class BookRepository {

    private final DbService dbService;
    private final BookDataManager dataManager;
    private final GlobalDataHolder globalData;
    
    // 书籍内容缓存（bookId -> 章节列表）
    private final Map<Integer, List<Chapter>> chapterCache = new HashMap<>();

    /**
     * 数据加载回调接口
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
     * @return 书籍信息，未找到返回 null
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
     * 获取书籍章节列表（带缓存）
     * 
     * @param bookId 书籍 ID
     * @return 章节列表
     */
    public List<Chapter> getChapters(int bookId) {
        // 检查缓存
        if (chapterCache.containsKey(bookId)) {
            EasyLog.print("BookRepository", "从缓存加载章节: " + chapterCache.get(bookId).size() + " 个");
            return chapterCache.get(bookId);
        }

        try {
            // 从数据库加载
            ArrayList<Chapter> chapters = dbService.mChapterService.find(
                ChapterDao.Properties.BookId.eq(bookId)
            );

            if (chapters != null) {
                // 更新缓存
                chapterCache.put(bookId, chapters);
                EasyLog.print("BookRepository", "从数据库加载章节: " + chapters.size() + " 个");
                return chapters;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            EasyLog.print("BookRepository", "获取章节列表失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 下载章节内容
     * 
     * @param chapter 章节对象
     * @param lifecycleOwner 生命周期拥有者
     * @param callback 下载回调
     */
    public void downloadChapter(Chapter chapter, androidx.lifecycle.LifecycleOwner lifecycleOwner, DataCallback<HH2SectionData> callback) {
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
                                // 保存到数据库
                                ConvertEntity.saveBookChapterDetailList(chapter, data.getData());
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
                                callback.onFailure(new Exception("章节数据为空"));
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
     * 下载书籍药方数据
     * 
     * @param bookId 书籍 ID
     * @param lifecycleOwner 生命周期拥有者
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

                            // 保存药方数据到数据库（后台线程）
                            new Thread(() -> {
                                ConvertEntity.getFangDetailList(fangList, bookId);
                            }).start();

                            if (callback != null) {
                                callback.onSuccess(fangList);
                            }

                        } else {
                            if (callback != null) {
                                callback.onFailure(new Exception("药方数据为空"));
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
     * 查询书架中的书籍
     * 
     * @param bookNo 书籍编号
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
     * 更新书籍阅读进度
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
     * 清除缓存
     */
    public void clearCache() {
        chapterCache.clear();
        EasyLog.print("BookRepository", "缓存已清除");
    }

    /**
     * 清除指定书籍的缓存
     * 
     * @param bookId 书籍 ID
     */
    public void clearCacheForBook(int bookId) {
        chapterCache.remove(bookId);
        EasyLog.print("BookRepository", "书籍缓存已清除: bookId=" + bookId);
    }

    // ==================== 新数据模型 API ====================

    /**
     * 获取书籍数据（新模型）
     * 优先从内存缓存获取，未命中则从数据库加载
     * 
     * @param bookId 书籍 ID
     * @return 书籍数据（非 null）
     */
    public BookData getBookData(int bookId) {
        // 1. 检查内存缓存
        BookData cached = dataManager.getFromCache(bookId);
        if (cached != null && cached.isFullyLoaded()) {
            EasyLog.print("BookRepository", "从缓存加载书籍: bookId=" + bookId);
            return cached;
        }

        // 2. 从数据库加载
        EasyLog.print("BookRepository", "从数据库加载书籍: bookId=" + bookId);
        BookData bookData = loadBookDataFromDb(bookId);
        
        // 3. 放入缓存
        dataManager.putToCache(bookId, bookData);
        
        return bookData;
    }

    /**
     * 从数据库加载书籍数据
     */
    private BookData loadBookDataFromDb(int bookId) {
        BookData bookData = new BookData(bookId);
        
        try {
            // 加载章节列表
            List<Chapter> chapters = getChapters(bookId);
            if (!chapters.isEmpty()) {
                // 为每个章节创建 ChapterData（不加载内容，等待懒加载）
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
            
            // 加载章节内容（从数据库已下载的内容）
            for (Chapter chapter : chapters) {
                if (chapter.getIsDownload()) {
                    // 已下载，加载内容
                    loadChapterContent(bookData, chapter);
                }
            }
            
            bookData.markAsFullyLoaded();
            
        } catch (Exception e) {
            EasyLog.print("BookRepository", "加载书籍数据失败: " + e.getMessage());
        }
        
        return bookData;
    }

    /**
     * 加载章节内容（从数据库）
     * 
     * @param bookData 书籍数据
     * @param chapter 章节对象
     */
    public void loadChapterContent(BookData bookData, Chapter chapter) {
        try {
            // 从数据库加载章节详细内容
            List<DataItem> content = ConvertEntity.getBookChapterDetailList(chapter);
            
            Long signatureId = chapter.getSignatureId();
            EasyLog.print("BookRepository", "加载章节内容: signatureId=" + signatureId + 
                ", contentSize=" + (content != null ? content.size() : 0));
            
            if (content != null && !content.isEmpty()) {
                // 查找对应的 ChapterData
                ChapterData chapterData = bookData.findChapterBySignature(signatureId);
                if (chapterData != null) {
                    chapterData.setContent(content);
                    EasyLog.print("BookRepository", "章节内容加载成功: signatureId=" + signatureId);
                } else {
                    EasyLog.print("BookRepository", "未找到对应的 ChapterData: signatureId=" + signatureId);
                }
            } else {
                EasyLog.print("BookRepository", "数据库中无章节内容: signatureId=" + signatureId);
            }
        } catch (Exception e) {
            EasyLog.print("BookRepository", "加载章节内容失败: " + e.getMessage());
        }
    }

    /**
     * 异步下载章节内容（新模型）
     * 
     * @param chapter 章节对象
     * @param bookData 书籍数据
     * @param callback 下载回调
     */
    public void downloadChapterAsync(Chapter chapter, BookData bookData, 
                                    androidx.lifecycle.LifecycleOwner lifecycleOwner,
                                    DataCallback<ChapterData> callback) {
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
                                // 保存到数据库
                                ConvertEntity.saveBookChapterDetailList(chapter, data.getData());
                                chapter.setIsDownload(true);
                                dbService.mChapterService.updateEntity(chapter);

                                // 创建或更新 ChapterData
                                ChapterData chapterData = null;
                                if (bookData != null) {
                                    chapterData = bookData.findChapterBySignature(chapter.getSignatureId());
                                }
                                
                                if (chapterData == null) {
                                    // 创建新的 ChapterData
                                    Long signatureId = chapter.getSignatureId();
                                    chapterData = new ChapterData(
                                        signatureId != null ? signatureId : 0,
                                        chapter.getChapterHeader() != null ? chapter.getChapterHeader() : "",
                                        chapter.getChapterSection()
                                    );
                                }
                                
                                // 设置内容（从 HH2SectionData 获取）
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
                        } else {
                            if (callback != null) {
                                callback.onFailure(new Exception("章节数据为空"));
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
     * 懒加载章节内容
     * 如果章节内容未加载，则从数据库或网络加载
     * 
     * @param bookId 书籍 ID
     * @param position 章节位置
     * @param lifecycleOwner 生命周期对象(Fragment/Activity),用于绑定网络请求
     * @param callback 加载回调
     */
    public void loadChapterLazy(int bookId, int position, androidx.lifecycle.LifecycleOwner lifecycleOwner, DataCallback<ChapterData> callback) {
        try {
            BookData bookData = getBookData(bookId);
            ChapterData chapterData = bookData.getChapter(position);
            
            EasyLog.print("BookRepository", "懒加载章节: bookId=" + bookId + 
                ", position=" + position + ", chapterData=" + (chapterData != null));
            
            if (chapterData == null) {
                EasyLog.print("BookRepository", "懒加载失败: 章节不存在");
                if (callback != null) {
                    callback.onFailure(new Exception("章节不存在"));
                }
                return;
            }
            
            // 检查是否已加载内容
            if (chapterData.isContentLoaded() && !chapterData.isEmpty()) {
                // 已加载，直接返回
                EasyLog.print("BookRepository", "章节内容已加载: signatureId=" + chapterData.getSignatureId());
                if (callback != null) {
                    callback.onSuccess(chapterData);
                }
                return;
            }
            
            EasyLog.print("BookRepository", "章节内容未加载，开始加载: signatureId=" + chapterData.getSignatureId());
            
            // 从数据库查找对应的 Chapter 实体
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
                EasyLog.print("BookRepository", "懒加载失败: 未找到章节实体");
                if (callback != null) {
                    callback.onFailure(new Exception("未找到章节实体"));
                }
                return;
            }
            
            // 检查是否已下载
            boolean isDownloaded = targetChapter.getIsDownload();
            EasyLog.print("BookRepository", "章节下载状态: isDownloaded=" + isDownloaded);
            
            if (isDownloaded) {
                // 已下载，从数据库加载
                loadChapterContent(bookData, targetChapter);
                if (callback != null) {
                    callback.onSuccess(chapterData);
                }
            } else {
                // 未下载，从网络下载
                EasyLog.print("BookRepository", "章节未下载，开始网络下载");
                downloadChapterAsync(targetChapter, bookData, lifecycleOwner, callback);
            }
            
        } catch (Exception e) {
            EasyLog.print("BookRepository", "懒加载异常: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * 获取书籍信息（使用全局数据）
     * 
     * @param bookId 书籍 ID
     * @return 书籍信息
     */
    public TabNavBody getBookInfoFromGlobal(int bookId) {
        return globalData.getBookInfo(bookId);
    }

    /**
     * 获取书籍数据管理器
     */
    public BookDataManager getDataManager() {
        return dataManager;
    }

    /**
     * 获取全局数据持有者
     */
    public GlobalDataHolder getGlobalData() {
        return globalData;
    }
}
