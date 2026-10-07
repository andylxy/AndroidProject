/*
 * 项目名: AndroidProject
 * 类名: DbService.java
 * 包名: run.yigou.gxzy.data.local.helper.DbService
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月11日 22:22:07
 * 上次修改时间: 2024年03月11日 22:22:06
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.data.local.helper;

import org.greenrobot.greendao.database.Database;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import run.yigou.gxzy.data.local.GreenDaoManager;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.Callback;
import run.yigou.gxzy.utils.ThreadUtil;
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

    /**
     * 串行后台执行器：数据库落库任务必须**按提交顺序**执行，且不能占用主线程。
     *
     * <p>为什么不复用 {@code ThreadPoolManager}：那是缓存池（corePoolSize=0、maximumPoolSize=200、
     * SynchronousQueue），提交 N 个任务就会并发到 N 个线程上。启动时逐本书的章节落库是
     * 「按书删除 + 批量插入」写同一个 SQLite 库，并发写会互相抢锁（甚至 SQLiteDatabaseLockedException）。
     * 单线程执行器保证与原先「在主线程上串行执行」完全相同的执行顺序，只是换了线程。
     *
     * <p>无界队列：提交即受理，不会因池满抛 RejectedExecutionException。调用方可以依赖
     * 「任务一定被执行」这一点——例如把它当作完成回调的兜底通道。
     */
    private final ExecutorService mSerialExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mf-db-serial");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });

    /** 所有 service 共用的同一个数据库句柄，用于跨表事务（见 {@link #runInTransaction}）。 */
    private final Database mDatabase = GreenDaoManager.getDaoMaster().getDatabase();

    /**
     * 在串行后台线程里读数据，结果回主线程交付（"读后回 UI"的标准入口）。
     *
     * <p>为什么要有这个便捷入口：本轮把主线程 DB 调用迁后台时，"后台读 → 回主线程 → 应用结果"
     * 这段形状在 7 个文件里各抄了一遍（见 greendao 加固票 13）。差异只有两处：
     * 读什么、拿到结果后做什么。失败怎么算则由调用方在 {@code onError} 里自己决定
     * （章节读失败按空列表、设置缓存读失败只记日志……），本方法不做统一。
     *
     * <p>失败语义：task 内抛出的任何异常都会被 {@link EasyLog#print(Throwable)} 记录，
     * 然后交给 {@code callback.onError}。不吞异常、不静默——但也不会让异常跑到 worker 线程上去。
     *
     * <p>顺序：与其他 {@link #runInBackgroundSerial(Runnable)} 任务共用同一条串行线程，
     * 因此不会与其它落库任务并发写同一个库。
     *
     * @param reader   后台执行的读操作（可为 {@code null}，此时不读直接回调 onError）
     * @param callback 主线程回调，可为 {@code null}
     */
    public <T> void readInBackground(final Callable<T> reader, final Callback<T> callback) {
        if (callback == null) {
            return;
        }
        if (reader == null) {
            callback.onError(new IllegalArgumentException("reader 不能为 null"));
            return;
        }
        runInBackgroundSerial(new Runnable() {
            @Override
            public void run() {
                try {
                    final T value = reader.call();
                    ThreadUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(value);
                        }
                    });
                } catch (final Throwable t) {
                    EasyLog.print(t);
                    ThreadUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(t instanceof Exception
                                    ? (Exception) t : new RuntimeException(t));
                        }
                    });
                }
            }
        });
    }

    /**
     * 在串行后台线程中执行数据库任务（按提交顺序逐条执行，不占用主线程）。
     *
     * <p>这是本层对外的异步入口：需要「先删后插」的替换、或任何可能耗时较久的库操作，
     * 都应经此提交，而不是在调用方自己起线程——见
     * {@code docs/database/greendao-hardening-plan.md} 的 §3.3「P1-1：主线程 DB 访问」。
     *
     * @param task 待执行的数据库任务
     */
    public void runInBackgroundSerial(Runnable task) {
        if (task == null) {
            return;
        }
        mSerialExecutor.execute(task);
    }

    /**
     * 在同一个数据库事务里执行一段跨 service（跨表）的操作。
     *
     * <p>为什么需要它：{@code BaseService} 的事务方法只能覆盖它自己那一张表，而「全量覆盖导航」
     * 这类操作要同时改 {@code TabNav} 与 {@code TabNavBody} 两张表（分属两个 service）。
     * 它们共用同一个 {@code Database}，所以把整段操作包在一个事务里即可保证原子性。
     *
     * <p>不变量：任务内的删除与写入要么都提交、要么都回滚。**任务内抛出的异常必须能传出**——
     * 若中途把异常吞掉（例如走 {@code ConvertEntity.executeDatabaseOperation}），事务会照常提交，
     * 留下删了一半/写了一半的表。因此任务内的写操作请直接调用 service 方法，不要用吞异常的包装。
     *
     * <p>失败时异常向上抛给调用方（由调用方决定重试或降级），不会静默吞掉。
     *
     * @param task 待执行的数据库操作
     */
    public void runInTransaction(Runnable task) {
        if (task == null) {
            return;
        }
        mDatabase.beginTransaction();
        try {
            task.run();
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

}
