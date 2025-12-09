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
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;

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
    }

    /**
     * 获取书籍信息
     * 
     * @param bookId 书籍 ID
     * @return 书籍信息，未找到返回 null
     */
    public TabNavBody getBookInfo(int bookId) {
        try {
            return TipsSingleData.getInstance().getNavTabBodyMap().get(bookId);
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
     * @param callback 下载回调
     */
    public void downloadChapter(Chapter chapter, DataCallback<HH2SectionData> callback) {
        if (chapter == null) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("章节对象为空"));
            }
            return;
        }

        try {
            EasyHttp.get(null)
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
     * @param callback 下载回调
     */
    public void downloadBookFang(int bookId, DataCallback<List<Fang>> callback) {
        try {
            EasyHttp.get(null)
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
}
