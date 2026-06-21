/*
 * ???: AndroidProject
 * ??: BookRepository.java
 * ??: run.yigou.gxzy.ui.reader.repository
 * ?? : AI Assistant
 * ?????? : 2025?12?09?
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.repository;

import com.hjq.http.EasyHttp;
import run.yigou.gxzy.log.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.HashMap;
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
import run.yigou.gxzy.utils.DebugLog;


/**
 * ??????
 * 
 * ???
 * 1. ?????????
 * 2. ????????
 * 3. ???????????
 * 4. ????????
 */
public class BookRepository {

    private final DbService dbService;
    private final BookDataManager dataManager;
    private final GlobalDataHolder globalData;
    
    // ???????bookId -> ?????
    private final Map<Integer, List<Chapter>> chapterCache = new HashMap<>();

    /**
     * ????????
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
     * ??????
     * 
     * @param bookId ?? ID
     * @return ?????????? null
     */
    public TabNavBody getBookInfo(int bookId) {
        try {
            return globalData.getBookInfo(bookId);
        } catch (Exception e) {
            EasyLog.print("BookRepository", "????????: " + e.getMessage());
            return null;
        }
    }

    /**
     * ?????????????
     * 
     * @param bookId ?? ID
     * @return ????
     */
    public List<Chapter> getChapters(int bookId) {
        // ????
        if (chapterCache.containsKey(bookId)) {
            EasyLog.print("BookRepository", "???????: " + chapterCache.get(bookId).size() + " ?");
            return chapterCache.get(bookId);
        }

        try {
            // ??????
            ArrayList<Chapter> chapters = dbService.mChapterService.find(
                ChapterDao.Properties.BookId.eq(bookId)
            );

            if (chapters != null) {
                // ????
                chapterCache.put(bookId, chapters);
                EasyLog.print("BookRepository", "????????: " + chapters.size() + " ?");
                return chapters;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            EasyLog.print("BookRepository", "????????: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ??????
     * 
     * @param chapter ????
     * @param lifecycleOwner ???????
     * @param callback ????
     */
    public void downloadChapter(Chapter chapter, androidx.lifecycle.LifecycleOwner lifecycleOwner, DataCallback<HH2SectionData> callback) {
        if (chapter == null) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("??????"));
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
                                // ??????
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
                                callback.onFailure(new Exception("??????"));
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
     * ????????
     * 
     * @param bookId ?? ID
     * @param lifecycleOwner ???????
     * @param callback ????
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

                            // ????????????????
                            new Thread(() -> {
                                DataRepository.saveFangDetailList(fangList, bookId);
                            }).start();

                            if (callback != null) {
                                callback.onSuccess(fangList);
                            }

                        } else {
                            if (callback != null) {
                                callback.onFailure(new Exception("??????"));
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
     * ????????
     * 
     * @param bookNo ????
     * @return ????
     */
    public ArrayList<Book> queryBookshelf(int bookNo) {
        try {
            return dbService.mBookService.find(BookDao.Properties.BookNo.eq(bookNo));
        } catch (Exception e) {
            EasyLog.print("BookRepository", "??????: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ???????
     * 
     * @param book ????
     * @return true-??, false-??
     */
    public boolean addToBookshelf(Book book) {
        try {
            dbService.mBookService.addEntity(book);
            return true;
        } catch (Exception e) {
            EasyLog.print("BookRepository", "??????: " + e.getMessage());
            return false;
        }
    }

    /**
     * ????????
     * 
     * @param book ????
     * @return true-??, false-??
     */
    public boolean updateReadingProgress(Book book) {
        try {
            dbService.mBookService.updateEntity(book);
            return true;
        } catch (Exception e) {
            EasyLog.print("BookRepository", "????????: " + e.getMessage());
            return false;
        }
    }

    /**
     * ????
     */
    public void clearCache() {
        chapterCache.clear();
        EasyLog.print("BookRepository", "?????");
    }

    /**
     * ?????????
     * 
     * @param bookId ?? ID
     */
    public void clearCacheForBook(int bookId) {
        chapterCache.remove(bookId);
        EasyLog.print("BookRepository", "???????: bookId=" + bookId);
    }

    // ==================== ????? API ====================

    /**
     * ?????? ID
     */
    public String generateBookId() {
        return dbService.mBookService.getUUID();
    }

    /**
     * ???????????
     * ????????????????????
     * 
     * @param bookId ?? ID
     * @return ?????? null?
     */
    public BookData getBookData(int bookId) {
        // 1. ??????
        BookData cached = dataManager.getFromCache(bookId);
        if (cached != null && cached.isFullyLoaded()) {
            EasyLog.print("BookRepository", "???????: bookId=" + bookId);
            return cached;
        }

        // 2. ??????
        EasyLog.print("BookRepository", "????????: bookId=" + bookId);
        BookData bookData = loadBookDataFromDb(bookId);
        
        // 3. ????
        dataManager.putToCache(bookId, bookData);
        
        return bookData;
    }

    /**
     * ??????????
     */
    private BookData loadBookDataFromDb(int bookId) {
        BookData bookData = new BookData(bookId);
        
        try {
            // ??????
            List<Chapter> chapters = getChapters(bookId);
            if (!chapters.isEmpty()) {
                // ??????? ChapterData?????????????
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
            
            // ??????????????????
            for (Chapter chapter : chapters) {
                if (chapter.getIsDownload()) {
                    // ????????
                    loadChapterContent(bookData, chapter);
                }
            }
            
            // ? ????????????????????????
            loadFangDataToBookData(bookData, bookId);
            
            bookData.markAsFullyLoaded();
            
        } catch (Exception e) {
            EasyLog.print("BookRepository", "????????: " + e.getMessage());
        }
        
        return bookData;
    }

    /**
     * ????????????
     * 
     * @param bookData ????
     * @param chapter ????
     */
    public void loadChapterContent(BookData bookData, Chapter chapter) {
        try {
            // ????????????
            List<DataItem> content = ConvertEntity.getBookChapterDetailList(chapter);
            
            Long signatureId = chapter.getSignatureId();
            EasyLog.print("BookRepository", "??????: signatureId=" + signatureId + 
                ", contentSize=" + (content != null ? content.size() : 0));
            
            if (content != null && !content.isEmpty()) {
                // ????? ChapterData
                ChapterData chapterData = bookData.findChapterBySignature(signatureId);
                if (chapterData != null) {
                    chapterData.setContent(content);
                    EasyLog.print("BookRepository", "????????: signatureId=" + signatureId);
                } else {
                    EasyLog.print("BookRepository", "?????? ChapterData: signatureId=" + signatureId);
                }
            } else {
                EasyLog.print("BookRepository", "?????????: signatureId=" + signatureId);
            }
        } catch (Exception e) {
            EasyLog.print("BookRepository", "????????: " + e.getMessage());
        }
    }

    /**
     * ???????BookData???????????????
     * 
     * @param bookData ????
     * @param bookId ??ID
     */
    private void loadFangDataToBookData(BookData bookData, int bookId) {
        try {
            // ??????????
            ArrayList<run.yigou.gxzy.data.model.Fang> fangList = ConvertEntity.getFangDetailList(bookId);
            
            EasyLog.print("BookRepository", "??????: bookId=" + bookId + 
                ", fangSize=" + (fangList != null ? fangList.size() : 0));
            
            if (fangList != null && !fangList.isEmpty()) {
                // ????????
                List<DataItem> fangItemList = new ArrayList<>(fangList);
                
                // ??????????
                TabNavBody bookInfo = getBookInfo(bookId);
                String bookName = bookInfo != null ? bookInfo.getBookName() : "????";
                
                ChapterData fangChapterData = new ChapterData(
                    0L, 
                    bookName + "?", 
                    0, 
                    fangItemList
                );
                
                bookData.setFangData(fangChapterData);
                EasyLog.print("BookRepository", "? ????????BookData: " + fangList.size() + " ?");
            } else {
                EasyLog.print("BookRepository", "?????????: bookId=" + bookId);
            }
        } catch (Exception e) {
            EasyLog.print("BookRepository", "????????: " + e.getMessage());
        }
    }

    /**
     * ?????????????
     * 
     * @param chapter ????
     * @param bookData ????
     * @param callback ????
     */
    public void downloadChapterAsync(Chapter chapter, BookData bookData, 
                                    androidx.lifecycle.LifecycleOwner lifecycleOwner,
                                    DataCallback<ChapterData> callback) {
        if (chapter == null) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("??????"));
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
                                // ??????
                                DataRepository.saveBookChapterDetailList(chapter, data.getData());
                                chapter.setIsDownload(true);
                                dbService.mChapterService.updateEntity(chapter);

                                // ????? ChapterData
                                ChapterData chapterData = null;
                                if (bookData != null) {
                                    chapterData = bookData.findChapterBySignature(chapter.getSignatureId());
                                }
                                
                                if (chapterData == null) {
                                    // ???? ChapterData
                                    Long signatureId = chapter.getSignatureId();
                                    chapterData = new ChapterData(
                                        signatureId != null ? signatureId : 0,
                                        chapter.getChapterHeader() != null ? chapter.getChapterHeader() : "",
                                        chapter.getChapterSection()
                                    );
                                }
                                
                                // ?????? HH2SectionData ???
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
                                callback.onFailure(new Exception("??????"));
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
     * ???????
     * ????????????????????
     * 
     * @param bookId ?? ID
     * @param position ????
     * @param lifecycleOwner ??????(Fragment/Activity),????????
     * @param callback ????
     */
    public void loadChapterLazy(int bookId, int position, androidx.lifecycle.LifecycleOwner lifecycleOwner, DataCallback<ChapterData> callback) {
        try {
            BookData bookData = getBookData(bookId);
            ChapterData chapterData = bookData.getChapter(position);
            
            EasyLog.print("BookRepository", "?????: bookId=" + bookId + 
                ", position=" + position + ", chapterData=" + (chapterData != null));
            
            if (chapterData == null) {
                EasyLog.print("BookRepository", "?????: ?????");
                if (callback != null) {
                    callback.onFailure(new Exception("?????"));
                }
                return;
            }
            
            // ?????????
            if (chapterData.isContentLoaded() && !chapterData.isEmpty()) {
                // ????????
                EasyLog.print("BookRepository", "???????: signatureId=" + chapterData.getSignatureId());
                if (callback != null) {
                    callback.onSuccess(chapterData);
                }
                return;
            }
            
            EasyLog.print("BookRepository", "????????????: signatureId=" + chapterData.getSignatureId());
            
            // ????????? Chapter ??
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
                EasyLog.print("BookRepository", "?????: ???????");
                if (callback != null) {
                    callback.onFailure(new Exception("???????"));
                }
                return;
            }
            
            // ???????
            boolean isDownloaded = targetChapter.getIsDownload();
            EasyLog.print("BookRepository", "??????: isDownloaded=" + isDownloaded);
            
            if (isDownloaded) {
                // ??????????
                loadChapterContent(bookData, targetChapter);
                if (callback != null) {
                    callback.onSuccess(chapterData);
                }
            } else {
                // ?????????
                EasyLog.print("BookRepository", "????????????");
                downloadChapterAsync(targetChapter, bookData, lifecycleOwner, callback);
            }
            
        } catch (Exception e) {
            EasyLog.print("BookRepository", "?????: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * ??????????????
     * 
     * @param bookId ?? ID
     * @return ????
     */
    public TabNavBody getBookInfoFromGlobal(int bookId) {
        return globalData.getBookInfo(bookId);
    }

    /**
     * ?????????
     */
    public BookDataManager getDataManager() {
        return dataManager;
    }

    /**
     * ?????????
     */
    public GlobalDataHolder getGlobalData() {
        return globalData;
    }
}
