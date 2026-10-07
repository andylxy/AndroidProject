/*
 * 项目名: AndroidProject
 * 类名: LocalServices.java
 * 包名: run.yigou.gxzy.data.local.helper
 * 作者 : Zhs (xiaoyang_02@qq.com)
 *
 * 说明: 本地数据服务定位器（service locator）。
 * 原 DbService 把「19 个 *Service 单例引用」与「数据库执行/事务协调」混在一起（god-object），
 * 经 ADR-0001 (Q1=C) 拆分后，定位器职责归本类，DbService 只保留执行器与事务入口。
 * 调用方写法由 `DbService.getInstance().mXxxService` 改为 `LocalServices.getInstance().mXxxService`
 * （或直接 `XxxService.getInstance()`）。
 */
package run.yigou.gxzy.data.local.helper;

import run.yigou.gxzy.data.local.service.AboutService;
import run.yigou.gxzy.data.local.service.AiConfigBodyService;
import run.yigou.gxzy.data.local.service.AiConfigService;
import run.yigou.gxzy.data.local.service.BeiMingCiService;
import run.yigou.gxzy.data.local.service.BookChapterBodyService;
import run.yigou.gxzy.data.local.service.BookChapterService;
import run.yigou.gxzy.data.local.service.BookService;
import run.yigou.gxzy.data.local.service.ChapterService;
import run.yigou.gxzy.data.local.service.ChatMessageBeanService;
import run.yigou.gxzy.data.local.service.ChatSessionBeanService;
import run.yigou.gxzy.data.local.service.ChatSummaryBeanService;
import run.yigou.gxzy.data.local.service.SearchHistoryService;
import run.yigou.gxzy.data.local.service.TabNavBodyService;
import run.yigou.gxzy.data.local.service.TabNavService;
import run.yigou.gxzy.data.local.service.UserInfoService;
import run.yigou.gxzy.data.local.service.YaoAliasService;
import run.yigou.gxzy.data.local.service.YaoFangBodyService;
import run.yigou.gxzy.data.local.service.YaoFangService;
import run.yigou.gxzy.data.local.service.YaoService;

/**
 * 统一持有各本地数据 Service 的引用，便于「一个入口拿全本地数据」。
 * 真正的数据访问由各 {@code *Service} 单例负责；本类不做任何 DB 执行/事务工作。
 */
public class LocalServices {
    public final UserInfoService mUserInfoService;
    public final BookService mBookService;
    public final SearchHistoryService mSearchHistoryService;
    public final YaoService mYaoService;
    public final BeiMingCiService mBeiMingCiService;
    public final BookChapterService mBookChapterService;
    public final BookChapterBodyService mBookChapterBodyService;
    public final YaoFangService mYaoFangService;
    public final YaoFangBodyService mYaoFangBodyService;
    public final TabNavBodyService mTabNavBodyService;
    public final TabNavService mTabNavService;
    public final AboutService mAboutService;
    public final YaoAliasService mYaoAliasService;
    public final ChapterService mChapterService;
    public final ChatMessageBeanService mChatMessageBeanService;
    public final ChatSessionBeanService mChatSessionBeanService;
    public final AiConfigService mAiConfigService;
    public final AiConfigBodyService mAiConfigBodyService;
    public final ChatSummaryBeanService mChatSummaryBeanService;

    private LocalServices() {
        // 各 *Service 本身已是单例，这里只是统一持有引用（与原 DbService 行为一致）。
        mUserInfoService = UserInfoService.getInstance();
        mBookService = BookService.getInstance();
        mSearchHistoryService = SearchHistoryService.getInstance();
        mYaoService = YaoService.getInstance();
        mBeiMingCiService = BeiMingCiService.getInstance();
        mBookChapterService = BookChapterService.getInstance();
        mBookChapterBodyService = BookChapterBodyService.getInstance();
        mYaoFangService = YaoFangService.getInstance();
        mYaoFangBodyService = YaoFangBodyService.getInstance();
        mTabNavBodyService = TabNavBodyService.getInstance();
        mTabNavService = TabNavService.getInstance();
        mAboutService = AboutService.getInstance();
        mYaoAliasService = YaoAliasService.getInstance();
        mChapterService = ChapterService.getInstance();
        mChatMessageBeanService = ChatMessageBeanService.getInstance();
        mChatSessionBeanService = ChatSessionBeanService.getInstance();
        mAiConfigService = AiConfigService.getInstance();
        mAiConfigBodyService = AiConfigBodyService.getInstance();
        mChatSummaryBeanService = ChatSummaryBeanService.getInstance();
    }

    private volatile static LocalServices instance;

    public static LocalServices getInstance() {
        if (instance == null) {
            synchronized (LocalServices.class) {
                if (instance == null) {
                    instance = new LocalServices();
                }
            }
        }
        return instance;
    }
}
