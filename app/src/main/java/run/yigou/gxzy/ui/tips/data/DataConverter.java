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

/**
 * 数据转换器
 * 负责将新数据模型 (BookData/ChapterData) 转换为 UI 层需要的数据结构 (HH2SectionData)
 */
public class DataConverter {
    
    
    /**
     * 将 ChapterData 和 Chapter 实体转换为 HH2SectionData（UI 显示用）
     * @param chapterData 章节数据（可能为 null）
     * @param chapter 数据库实体
     * @return UI 显示用的章节数据
     */
    @NonNull
    public static HH2SectionData toHH2SectionData(@Nullable ChapterData chapterData, @NonNull Chapter chapter) {
        // 获取章节内容
        List<DataItem> content = (chapterData != null && chapterData.isContentLoaded()) 
            ? chapterData.getContent() 
            : new ArrayList<>();
        
        // 创建 HH2SectionData
        HH2SectionData section = new HH2SectionData(
            content,
            chapter.getChapterSection(),
            chapter.getChapterHeader()
        );
        
        // 设置签名 ID
        Long signatureId = chapter.getSignatureId();
        section.setSignatureId(signatureId != null ? signatureId : 0);
        
        return section;
    }
    
    /**
     * 批量转换：BookData + Chapter 列表 → HH2SectionData 列表
     * @param bookData 书籍数据（可能为 null）
     * @param chapters 数据库实体列表
     * @return UI 显示用的章节列表
     */
    @NonNull
    public static ArrayList<HH2SectionData> toHH2SectionDataList(@Nullable BookData bookData, @Nullable List<Chapter> chapters) {
        ArrayList<HH2SectionData> result = new ArrayList<>();
        
        if (chapters == null || chapters.isEmpty()) {
            return result;
        }
        
        for (Chapter chapter : chapters) {
            if (chapter == null) {
                continue;
            }
            
            // 查找对应的 ChapterData
            ChapterData chapterData = null;
            if (bookData != null) {
                Long signatureId = chapter.getSignatureId();
                if (signatureId != null) {
                    chapterData = bookData.findChapterBySignature(signatureId);
                }
            }
            
            // 转换并添加
            HH2SectionData section = toHH2SectionData(chapterData, chapter);
            result.add(section);
        }
        
        return result;
    }
}
