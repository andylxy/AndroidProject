package run.yigou.gxzy.greendao.gen;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import run.yigou.gxzy.greendao.entity.AliaZhongYao;
import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.BookChapter;
import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.entity.SearchHistory;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.entity.ZhongYao;

import run.yigou.gxzy.greendao.gen.AliaZhongYaoDao;
import run.yigou.gxzy.greendao.gen.BeiMingCiDao;
import run.yigou.gxzy.greendao.gen.BookDao;
import run.yigou.gxzy.greendao.gen.BookChapterDao;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;
import run.yigou.gxzy.greendao.gen.SearchHistoryDao;
import run.yigou.gxzy.greendao.gen.TabNavDao;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;
import run.yigou.gxzy.greendao.gen.UserInfoDao;
import run.yigou.gxzy.greendao.gen.YaoFangDao;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig aliaZhongYaoDaoConfig;
    private final DaoConfig beiMingCiDaoConfig;
    private final DaoConfig bookDaoConfig;
    private final DaoConfig bookChapterDaoConfig;
    private final DaoConfig bookChapterBodyDaoConfig;
    private final DaoConfig searchHistoryDaoConfig;
    private final DaoConfig tabNavDaoConfig;
    private final DaoConfig tabNavBodyDaoConfig;
    private final DaoConfig userInfoDaoConfig;
    private final DaoConfig yaoFangDaoConfig;
    private final DaoConfig yaoFangBodyDaoConfig;
    private final DaoConfig zhongYaoDaoConfig;

    private final AliaZhongYaoDao aliaZhongYaoDao;
    private final BeiMingCiDao beiMingCiDao;
    private final BookDao bookDao;
    private final BookChapterDao bookChapterDao;
    private final BookChapterBodyDao bookChapterBodyDao;
    private final SearchHistoryDao searchHistoryDao;
    private final TabNavDao tabNavDao;
    private final TabNavBodyDao tabNavBodyDao;
    private final UserInfoDao userInfoDao;
    private final YaoFangDao yaoFangDao;
    private final YaoFangBodyDao yaoFangBodyDao;
    private final ZhongYaoDao zhongYaoDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        aliaZhongYaoDaoConfig = daoConfigMap.get(AliaZhongYaoDao.class).clone();
        aliaZhongYaoDaoConfig.initIdentityScope(type);

        beiMingCiDaoConfig = daoConfigMap.get(BeiMingCiDao.class).clone();
        beiMingCiDaoConfig.initIdentityScope(type);

        bookDaoConfig = daoConfigMap.get(BookDao.class).clone();
        bookDaoConfig.initIdentityScope(type);

        bookChapterDaoConfig = daoConfigMap.get(BookChapterDao.class).clone();
        bookChapterDaoConfig.initIdentityScope(type);

        bookChapterBodyDaoConfig = daoConfigMap.get(BookChapterBodyDao.class).clone();
        bookChapterBodyDaoConfig.initIdentityScope(type);

        searchHistoryDaoConfig = daoConfigMap.get(SearchHistoryDao.class).clone();
        searchHistoryDaoConfig.initIdentityScope(type);

        tabNavDaoConfig = daoConfigMap.get(TabNavDao.class).clone();
        tabNavDaoConfig.initIdentityScope(type);

        tabNavBodyDaoConfig = daoConfigMap.get(TabNavBodyDao.class).clone();
        tabNavBodyDaoConfig.initIdentityScope(type);

        userInfoDaoConfig = daoConfigMap.get(UserInfoDao.class).clone();
        userInfoDaoConfig.initIdentityScope(type);

        yaoFangDaoConfig = daoConfigMap.get(YaoFangDao.class).clone();
        yaoFangDaoConfig.initIdentityScope(type);

        yaoFangBodyDaoConfig = daoConfigMap.get(YaoFangBodyDao.class).clone();
        yaoFangBodyDaoConfig.initIdentityScope(type);

        zhongYaoDaoConfig = daoConfigMap.get(ZhongYaoDao.class).clone();
        zhongYaoDaoConfig.initIdentityScope(type);

        aliaZhongYaoDao = new AliaZhongYaoDao(aliaZhongYaoDaoConfig, this);
        beiMingCiDao = new BeiMingCiDao(beiMingCiDaoConfig, this);
        bookDao = new BookDao(bookDaoConfig, this);
        bookChapterDao = new BookChapterDao(bookChapterDaoConfig, this);
        bookChapterBodyDao = new BookChapterBodyDao(bookChapterBodyDaoConfig, this);
        searchHistoryDao = new SearchHistoryDao(searchHistoryDaoConfig, this);
        tabNavDao = new TabNavDao(tabNavDaoConfig, this);
        tabNavBodyDao = new TabNavBodyDao(tabNavBodyDaoConfig, this);
        userInfoDao = new UserInfoDao(userInfoDaoConfig, this);
        yaoFangDao = new YaoFangDao(yaoFangDaoConfig, this);
        yaoFangBodyDao = new YaoFangBodyDao(yaoFangBodyDaoConfig, this);
        zhongYaoDao = new ZhongYaoDao(zhongYaoDaoConfig, this);

        registerDao(AliaZhongYao.class, aliaZhongYaoDao);
        registerDao(BeiMingCi.class, beiMingCiDao);
        registerDao(Book.class, bookDao);
        registerDao(BookChapter.class, bookChapterDao);
        registerDao(BookChapterBody.class, bookChapterBodyDao);
        registerDao(SearchHistory.class, searchHistoryDao);
        registerDao(TabNav.class, tabNavDao);
        registerDao(TabNavBody.class, tabNavBodyDao);
        registerDao(UserInfo.class, userInfoDao);
        registerDao(YaoFang.class, yaoFangDao);
        registerDao(YaoFangBody.class, yaoFangBodyDao);
        registerDao(ZhongYao.class, zhongYaoDao);
    }
    
    public void clear() {
        aliaZhongYaoDaoConfig.clearIdentityScope();
        beiMingCiDaoConfig.clearIdentityScope();
        bookDaoConfig.clearIdentityScope();
        bookChapterDaoConfig.clearIdentityScope();
        bookChapterBodyDaoConfig.clearIdentityScope();
        searchHistoryDaoConfig.clearIdentityScope();
        tabNavDaoConfig.clearIdentityScope();
        tabNavBodyDaoConfig.clearIdentityScope();
        userInfoDaoConfig.clearIdentityScope();
        yaoFangDaoConfig.clearIdentityScope();
        yaoFangBodyDaoConfig.clearIdentityScope();
        zhongYaoDaoConfig.clearIdentityScope();
    }

    public AliaZhongYaoDao getAliaZhongYaoDao() {
        return aliaZhongYaoDao;
    }

    public BeiMingCiDao getBeiMingCiDao() {
        return beiMingCiDao;
    }

    public BookDao getBookDao() {
        return bookDao;
    }

    public BookChapterDao getBookChapterDao() {
        return bookChapterDao;
    }

    public BookChapterBodyDao getBookChapterBodyDao() {
        return bookChapterBodyDao;
    }

    public SearchHistoryDao getSearchHistoryDao() {
        return searchHistoryDao;
    }

    public TabNavDao getTabNavDao() {
        return tabNavDao;
    }

    public TabNavBodyDao getTabNavBodyDao() {
        return tabNavBodyDao;
    }

    public UserInfoDao getUserInfoDao() {
        return userInfoDao;
    }

    public YaoFangDao getYaoFangDao() {
        return yaoFangDao;
    }

    public YaoFangBodyDao getYaoFangBodyDao() {
        return yaoFangBodyDao;
    }

    public ZhongYaoDao getZhongYaoDao() {
        return zhongYaoDao;
    }

}
