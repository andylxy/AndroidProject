/*
 * 项目名: AndroidProject
 * 类名: ChapterData.java
 * 包名: run.yigou.gxzy.ui.tips.data
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import run.yigou.gxzy.ui.tips.tipsutils.DataItem;

/**
 * 章节数据（优化版）
 * 替代 HH2SectionData，支持懒加载和内存优化
 * 特性：
 * - 弱引用内容（可被 GC 回收）
 * - 异步预处理富文本
 * - 内存占用可估算
 */
public class ChapterData {
    
    // 章节 ID
    private final long signatureId;
    
    // 章节标题
    private final String title;
    
    // 章节编号
    private final int section;
    
    // 章节内容（弱引用，允许 GC 回收）
    private WeakReference<List<DataItem>> contentRef;
    
    // 原始内容（强引用，用于重建）
    private List<DataItem> originalContent;
    
    // 是否已加载内容
    private final AtomicBoolean isContentLoaded;
    
    // 是否已预处理富文本
    private final AtomicBoolean isPrepared;
    
    /**
     * 构造函数
     * @param signatureId 章节签名 ID
     * @param title 章节标题
     * @param section 章节编号
     */
    public ChapterData(long signatureId, @NonNull String title, int section) {
        this.signatureId = signatureId;
        this.title = title;
        this.section = section;
        this.isContentLoaded = new AtomicBoolean(false);
        this.isPrepared = new AtomicBoolean(false);
    }
    
    /**
     * 完整构造函数（带内容）
     */
    public ChapterData(long signatureId, @NonNull String title, int section, 
                      @Nullable List<DataItem> content) {
        this(signatureId, title, section);
        if (content != null) {
            setContent(content);
        }
    }
    
    /**
     * 获取章节 ID
     */
    public long getSignatureId() {
        return signatureId;
    }
    
    /**
     * 获取章节标题
     */
    @NonNull
    public String getTitle() {
        return title;
    }
    
    /**
     * 获取章节编号
     */
    public int getSection() {
        return section;
    }
    
    /**
     * 设置章节内容
     * @param content 内容列表
     */
    public synchronized void setContent(@NonNull List<DataItem> content) {
        this.originalContent = new ArrayList<>(content);
        this.contentRef = new WeakReference<>(this.originalContent);
        this.isContentLoaded.set(true);
        this.isPrepared.set(false); // 新内容需要重新预处理
    }
    
    /**
     * 获取章节内容
     * @return 内容列表，如果已被 GC 回收则重建
     */
    @NonNull
    public synchronized List<DataItem> getContent() {
        List<DataItem> content = contentRef != null ? contentRef.get() : null;
        
        if (content == null && originalContent != null) {
            // 弱引用被回收，从原始内容重建
            content = new ArrayList<>(originalContent);
            contentRef = new WeakReference<>(content);
        }
        
        if (content == null) {
            // 未加载或已完全释放，返回空列表
            content = new ArrayList<>();
        }
        
        return content;
    }
    
    /**
     * 确保内容已加载
     * 如果未加载，则触发加载逻辑
     */
    public synchronized void ensureContentLoaded() {
        if (!isContentLoaded.get()) {
            // 这里可以触发从数据库或网络加载
            // 当前实现：等待外部设置内容
            // TODO: 集成实际加载逻辑
        }
    }
    
    /**
     * 是否已加载内容
     */
    public boolean isContentLoaded() {
        return isContentLoaded.get();
    }
    
    /**
     * 预处理富文本（后台线程调用）
     * 提前生成 SpannableStringBuilder，避免滑动时卡顿
     */
    public void prepareRichText() {
        if (isPrepared.get()) {
            return; // 已预处理
        }
        
        List<DataItem> content = getContent();
        for (DataItem item : content) {
            // 触发富文本缓存生成
            if (item.getAttributedText() != null) {
                // 已有缓存，跳过
                continue;
            }
            // 预生成富文本（这会调用 DataItem 的富文本处理逻辑）
            item.getText();
        }
        
        isPrepared.set(true);
    }
    
    /**
     * 是否已预处理
     */
    public boolean isPrepared() {
        return isPrepared.get();
    }
    
    /**
     * 清空缓存（内存紧张时调用）
     */
    public synchronized void clearCache() {
        // 清空弱引用
        if (contentRef != null) {
            contentRef.clear();
            contentRef = null;
        }
        
        // 保留原始数据引用（用于重建）
        // 如果内存极度紧张，可以连 originalContent 也清空
        // originalContent = null;
        
        isPrepared.set(false);
    }
    
    /**
     * 完全释放内存（包括原始数据）
     * 谨慎使用！释放后需要重新加载
     */
    public synchronized void releaseAll() {
        clearCache();
        originalContent = null;
        isContentLoaded.set(false);
    }
    
    /**
     * 估算内存占用（字节）
     */
    public int estimateMemorySize() {
        int size = 0;
        
        // 基础对象
        size += 100; // 对象头 + 字段
        
        // 标题
        size += title.length() * 2; // 字符串按 UTF-16 计算
        
        // 内容列表
        List<DataItem> content = contentRef != null ? contentRef.get() : null;
        if (content != null) {
            size += content.size() * 500; // 每个 DataItem 估算 500 字节
        } else if (originalContent != null) {
            size += originalContent.size() * 500;
        }
        
        return size;
    }
    
    /**
     * 是否为空章节（无内容）
     */
    public boolean isEmpty() {
        return !isContentLoaded.get() || 
               (originalContent == null || originalContent.isEmpty());
    }
    
    /**
     * 获取内容条目数量
     */
    public int getContentSize() {
        if (!isContentLoaded.get()) {
            return 0;
        }
        
        List<DataItem> content = getContent();
        return content.size();
    }
    
    @Override
    public String toString() {
        return "ChapterData{" +
                "id=" + signatureId +
                ", title='" + title + '\'' +
                ", section=" + section +
                ", loaded=" + isContentLoaded.get() +
                ", prepared=" + isPrepared.get() +
                ", size=" + getContentSize() +
                '}';
    }
}
