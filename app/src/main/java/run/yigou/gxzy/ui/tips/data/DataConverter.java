/*
 * 项目名: AndroidProject
 * 类名: DataConverter.java
 * 包名: run.yigou.gxzy.ui.tips.data
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;

/**
 * 数据转换适配器
 * 负责新旧数据模型之间的转换
 * 用于过渡期的兼容性处理
 */
public class DataConverter {
    
    /**
     * 将 SingletonNetData 转换为 BookData
     * @param oldData 旧数据模型
     * @param bookId 书籍 ID
     * @return 新数据模型
     */
    @NonNull
    public static BookData fromSingletonNetData(@Nullable SingletonNetData oldData, int bookId) {
        BookData bookData = new BookData(bookId);
        
        if (oldData == null) {
            return bookData;
        }
        
        // 转换章节列表
        ArrayList<HH2SectionData> oldContent = oldData.getContent();
        if (oldContent != null && !oldContent.isEmpty()) {
            List<ChapterData> chapterList = new ArrayList<>();
            
            for (int i = 0; i < oldContent.size(); i++) {
                HH2SectionData section = oldContent.get(i);
                ChapterData chapterData = fromHH2SectionData(section);
                chapterList.add(chapterData);
            }
            
            bookData.setChapters(chapterList);
        }
        
        // 转换方剂数据
        ArrayList<HH2SectionData> oldFang = oldData.getFang();
        if (oldFang != null && !oldFang.isEmpty()) {
            ChapterData fangData = fromHH2SectionData(oldFang.get(0));
            bookData.setFangData(fangData);
        }
        
        return bookData;
    }
    
    /**
     * 将 HH2SectionData 转换为 ChapterData
     * @param oldSection 旧章节数据
     * @return 新章节数据
     */
    @NonNull
    public static ChapterData fromHH2SectionData(@NonNull HH2SectionData oldSection) {
        long signatureId = oldSection.getSignatureId();
        String title = oldSection.getHeader() != null ? oldSection.getHeader() : "";
        int section = oldSection.getSection();
        
        // 转换内容
        List<DataItem> content = null;
        if (oldSection.getData() != null) {
            content = new ArrayList<>();
            for (Object item : oldSection.getData()) {
                if (item instanceof DataItem) {
                    content.add((DataItem) item);
                }
            }
        }
        
        return new ChapterData(signatureId, title, section, content);
    }
    
    /**
     * 将 BookData 转换为 SingletonNetData（向下兼容）
     * @param newData 新数据模型
     * @return 旧数据模型
     */
    @NonNull
    public static SingletonNetData toSingletonNetData(@NonNull BookData newData) {
        SingletonNetData oldData = SingletonNetData.getInstance(newData.getBookId());
        
        // 转换章节列表
        List<ChapterData> chapterList = newData.getAllChapters();
        if (!chapterList.isEmpty()) {
            List<HH2SectionData> oldContent = new ArrayList<>();
            
            for (ChapterData chapter : chapterList) {
                HH2SectionData section = toHH2SectionData(chapter);
                oldContent.add(section);
            }
            
            oldData.setContent(oldContent);
        }
        
        // 转换方剂数据
        ChapterData fangData = newData.getFangData();
        if (fangData != null) {
            HH2SectionData oldFang = toHH2SectionData(fangData);
            oldData.setFang(oldFang);
        }
        
        return oldData;
    }
    
    /**
     * 将 ChapterData 转换为 HH2SectionData（向下兼容）
     * @param newChapter 新章节数据
     * @return 旧章节数据
     */
    @NonNull
    public static HH2SectionData toHH2SectionData(@NonNull ChapterData newChapter) {
        List<DataItem> content = newChapter.getContent();
        HH2SectionData section = new HH2SectionData(
            content,
            newChapter.getSection(),
            newChapter.getTitle()
        );
        section.setSignatureId(newChapter.getSignatureId());
        
        return section;
    }
    
    /**
     * 从 Chapter 实体创建 ChapterData
     * @param chapter 数据库实体
     * @return 章节数据
     */
    @NonNull
    public static ChapterData fromChapterEntity(@NonNull Chapter chapter) {
        Long signatureId = chapter.getSignatureId();
        String title = chapter.getChapterHeader() != null ? chapter.getChapterHeader() : "";
        Integer section = chapter.getChapterSection();
        
        ChapterData chapterData = new ChapterData(
            signatureId != null ? signatureId : 0,
            title,
            section != null ? section : 0
        );
        
        // 注意：这里不加载内容，等待懒加载
        // 如果需要立即加载，可以调用 loadChapterContent
        
        return chapterData;
    }
    
    /**
     * 批量转换 Chapter 实体列表
     * @param chapters 数据库实体列表
     * @return 章节数据列表
     */
    @NonNull
    public static List<ChapterData> fromChapterEntities(@Nullable List<Chapter> chapters) {
        List<ChapterData> result = new ArrayList<>();
        
        if (chapters == null || chapters.isEmpty()) {
            return result;
        }
        
        for (Chapter chapter : chapters) {
            if (chapter != null) {
                result.add(fromChapterEntity(chapter));
            }
        }
        
        return result;
    }
    
    /**
     * 将 ChapterData 列表转换为 HH2SectionData 列表
     * @param chapterList 新章节列表
     * @return 旧章节列表
     */
    @NonNull
    public static List<HH2SectionData> toHH2SectionDataList(@Nullable List<ChapterData> chapterList) {
        List<HH2SectionData> result = new ArrayList<>();
        
        if (chapterList == null || chapterList.isEmpty()) {
            return result;
        }
        
        for (ChapterData chapter : chapterList) {
            if (chapter != null) {
                result.add(toHH2SectionData(chapter));
            }
        }
        
        return result;
    }
    
    /**
     * 将 HH2SectionData 列表转换为 ChapterData 列表
     * @param sectionList 旧章节列表
     * @return 新章节列表
     */
    @NonNull
    public static List<ChapterData> fromHH2SectionDataList(@Nullable List<HH2SectionData> sectionList) {
        List<ChapterData> result = new ArrayList<>();
        
        if (sectionList == null || sectionList.isEmpty()) {
            return result;
        }
        
        for (HH2SectionData section : sectionList) {
            if (section != null) {
                result.add(fromHH2SectionData(section));
            }
        }
        
        return result;
    }
}
