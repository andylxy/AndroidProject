/*
 * 项目名: AndroidProject
 * 类名: DbDao.java
 * 包名: run.yigou.gxzy.greendao.util.DbDao
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月11日 22:22:07
 * 上次修改时间: 2024年03月11日 22:22:06
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.greendao.util;

import run.yigou.gxzy.greendao.service.AboutService;
import run.yigou.gxzy.greendao.service.AiConfigBodyService;
import run.yigou.gxzy.greendao.service.AiConfigService;
import run.yigou.gxzy.greendao.service.BeiMingCiService;
import run.yigou.gxzy.greendao.service.BookChapterBodyService;
import run.yigou.gxzy.greendao.service.BookChapterService;
import run.yigou.gxzy.greendao.service.BookService;
import run.yigou.gxzy.greendao.service.ChapterService;
import run.yigou.gxzy.greendao.service.ChatMessageBeanService;
import run.yigou.gxzy.greendao.service.ChatSessionBeanService;
import run.yigou.gxzy.greendao.service.ChatSummaryBeanService;
import run.yigou.gxzy.greendao.service.SearchHistoryService;
import run.yigou.gxzy.greendao.service.TabNavBodyService;
import run.yigou.gxzy.greendao.service.TabNavService;
import run.yigou.gxzy.greendao.service.UserInfoService;
import run.yigou.gxzy.greendao.service.YaoAliasService;
import run.yigou.gxzy.greendao.service.YaoFangBodyService;
import run.yigou.gxzy.greendao.service.YaoFangService;
import run.yigou.gxzy.greendao.service.YaoService;

/**
 * 版本:  1.0
 * 描述: 统一创建数据Dao.需要的时候直接引用
 */
public class DbService {
    public UserInfoService mUserInfoService;
    public BookService mBookService;
    public SearchHistoryService mSearchHistoryService;
    public YaoService mYaoService;
    public BeiMingCiService mBeiMingCiService;
    public BookChapterService mBookChapterService;
    public BookChapterBodyService mBookChapterBodyService;
    public YaoFangService mYaoFangService;
    public YaoFangBodyService mYaoFangBodyService;
    public TabNavBodyService mTabNavBodyService;
    public TabNavService mTabNavService;
    public AboutService mAboutService;
    public YaoAliasService  mYaoAliasService;
    public ChapterService mChapterService;
    public ChatMessageBeanService mChatMessageBeanService;
    public ChatSessionBeanService mChatSessionBeanService;
    public AiConfigService mAiConfigService;
    public AiConfigBodyService mAiConfigBodyService;
    public ChatSummaryBeanService mChatSummaryBeanService;

    private DbService() {
        // 防止反射攻击
        if (instance != null) {
            throw new IllegalStateException("Singleton instance already created!");
        }
        mUserInfoService =  UserInfoService.getInstance();
        mBookService =  BookService.getInstance();
        mSearchHistoryService =   SearchHistoryService.getInstance();
        mYaoService =  YaoService.getInstance();
        mBeiMingCiService =  BeiMingCiService.getInstance();
        mBookChapterService = BookChapterService.getInstance();
        mBookChapterBodyService =  BookChapterBodyService.getInstance();
        mYaoFangService =  YaoFangService.getInstance();
        mYaoFangBodyService =  YaoFangBodyService.getInstance();
        mTabNavBodyService =  TabNavBodyService.getInstance();
        mTabNavService =  TabNavService.getInstance();
        mAboutService =  AboutService.getInstance();
        mYaoAliasService =  YaoAliasService.getInstance();
        mChapterService =  ChapterService.getInstance();
        mChatMessageBeanService =  ChatMessageBeanService.getInstance();
        mChatSessionBeanService =  ChatSessionBeanService.getInstance();
        mAiConfigService =  AiConfigService.getInstance();
        mAiConfigBodyService =  AiConfigBodyService.getInstance();
        mChatSummaryBeanService =  ChatSummaryBeanService.getInstance();
    }

    private volatile static DbService instance;
    public static DbService getInstance() {
        if (instance == null) {
            synchronized (DbService.class) {
                if (instance == null) {
                    instance = new DbService();
                }
            }
        }
        return instance;
    }

}