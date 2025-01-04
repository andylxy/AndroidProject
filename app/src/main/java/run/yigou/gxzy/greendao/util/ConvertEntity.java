package run.yigou.gxzy.greendao.util;

import androidx.annotation.NonNull;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import run.yigou.gxzy.greendao.entity.About;
import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.entity.BookChapter;
import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.entity.ZhongYaoAlia;
import run.yigou.gxzy.greendao.gen.BeiMingCiDao;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;
import run.yigou.gxzy.greendao.gen.BookChapterDao;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;
import run.yigou.gxzy.greendao.gen.TabNavDao;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;
import run.yigou.gxzy.greendao.gen.YaoFangDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoDao;
import run.yigou.gxzy.greendao.service.AboutService;
import run.yigou.gxzy.ui.fragment.TipsWindowNetFragment;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.DataBeans.YaoAlia;
import run.yigou.gxzy.ui.tips.DataBeans.YaoUse;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.utils.RC4Helper;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;

public class ConvertEntity {


//            EasyHttp.post(ApplicationLifecycle.getInstance())
//                .api(new XxxApi())
//                .tag("abc")
//                .request(new OnHttpListener<HttpData<XxxBean>>() {
//
//                    @Override
//                    public void onHttpSuccess(@NonNull HttpData<XxxBean> result) {
//
//                    }
//
//                    @Override
//                    public void onHttpFail(@NonNull Throwable throwable) {
//
//                    }
//                });
//        Application可以传入 ApplicationLifecycle 这样写是完全可以的，但是不能在 Activity 或者 Service 中这样写，
//        因为这样可能会导致内存泄漏,将意味着框架无法自动把控请求的生命周期。
//
//        除了 Application，如果你在 Activity 或者 Service 中采用了 ApplicationLifecycle 的写法，那么为了避免内存泄漏或者崩溃的事情发生，
//        需要你在请求的时候设置对应的 Tag，然后在恰当的时机手动取消请求（一般在 Activity 或者 Service 销毁或者退出的时候取消请求）。
//
//        EasyHttp.cancelByTag("abc");


    /**
     * 初始化单个数据的提示信息
     * 该方法用于从数据库中加载导航信息和相关书籍内容，并将它们存储在内存中以便快速访问
     */
    public static void tipsSingleDataInit() {
        try {
            // 获取数据库服务实例
            DbService dbService = DbService.getInstance();
            // 检查数据库服务和导航服务是否已初始化
            if (dbService == null || dbService.mTabNavService == null) {
                return;
            }
            //
            ArrayList<Yao> yaoData = ConvertEntity.getYaoData();
            TipsSingleData.getInstance().setYaoData(new HH2SectionData(yaoData, 0, "常用本草药物"));
            ArrayList<MingCiContent> mingCiContentList = ConvertEntity.getMingCi();
            // 加载常用名词
            TipsSingleData.getInstance().setMingCiData(new HH2SectionData(mingCiContentList, 0, "医书相关的名词说明"));
            // 加载中药别名
            Map<String, String> yaoAliasDict = TipsSingleData.getInstance().getYaoAliasDict();
            for (ZhongYaoAlia yaoAlia : getYaoAlia()) {
                yaoAliasDict.put(yaoAlia.getBieming(), yaoAlia.getName());
            }
            // 从数据库中加载所有导航信息
            ArrayList<TabNav> navList = dbService.mTabNavService.findAll();
            // 检查导航信息是否已加载
            if (navList != null && !navList.isEmpty()) {
                // 获取单例数据对象
                TipsSingleData tipsSingleData = TipsSingleData.getInstance();
                // 同步以确保线程安全
                synchronized (tipsSingleData) {
                    // 获取导航信息和书籍内容的映射
                    Map<Integer, TabNav> navTabMap = tipsSingleData.getNavTabMap();
                    Map<Integer, TabNavBody> navTabBodyMap = tipsSingleData.getNavTabBodyMap();

                    // 遍历导航信息
                    for (TabNav nav : navList) {
                        // 将导航信息添加到映射中
                        navTabMap.put(nav.getOrder(), nav);
                        // 遍历导航下的书籍信息
                        for (TabNavBody item : nav.getNavList()) {
                            // 检查书籍编号是否有效
                            if (item.getBookNo() > 0) {
                                // 将书籍信息添加到映射中
                                navTabBodyMap.put(item.getBookNo(), item);
                                // 加载书籍内容和方剂数据
                                loadBookContent(tipsSingleData, item);
                                loadFangData(tipsSingleData, item);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 记录日志或进行其他异常处理
            e.printStackTrace();
        }
    }

    /**
     * 加载书籍内容
     * 该方法根据书籍编号获取书籍章节信息，并将其存储在内存中
     *
     * @param tipsSingleData 单例数据对象，用于存储书籍内容
     * @param item           导航信息中的书籍项
     */
    private static void loadBookContent(TipsSingleData tipsSingleData, TabNavBody item) {
        // 获取书籍章节列表
        List<HH2SectionData> bookChapterList = ConvertEntity.getBookChapterDetailList(item.getBookNo());
//        if (item.getBookNo() ==10001){
//            EasyLog.print("加载了10001");
//        }
        // 检查章节列表是否已加载
        if (bookChapterList != null && !bookChapterList.isEmpty()) {
            // 将章节列表存储在内存中
            tipsSingleData.getMapBookContent(item.getBookNo()).setContent(bookChapterList);
            tipsSingleData.getMapBookContent(item.getBookNo()).setFangAliasDict(tipsSingleData.getFangAliasDict());
            tipsSingleData.getMapBookContent(item.getBookNo()).setYaoAliasDict(tipsSingleData.getYaoAliasDict());
        }
    }

    /**
     * 加载方剂数据
     * 该方法根据书籍编号获取方剂信息，并将其存储在内存中
     *
     * @param tipsSingleData 单例数据对象，用于存储方剂数据
     * @param item           导航信息中的书籍项
     */
    private static void loadFangData(TipsSingleData tipsSingleData, TabNavBody item) {
        // 获取方剂列表
        ArrayList<Fang> fangList = ConvertEntity.getFangDetailList(item.getBookNo());
        // 检查方剂列表是否已加载
        if (!fangList.isEmpty()) {
            // 将方剂列表存储在内存中
            tipsSingleData.getMapBookContent(item.getBookNo()).setFang(new HH2SectionData(fangList, 0, item.getBookName() + "方"));
        }
    }


    public static void saveYaoAlia(List<YaoAlia> yaoAliaList) {
        DbService.getInstance().mYaoAliasService.deleteAll();
        for (YaoAlia yaoAlia : yaoAliaList) {
            ZhongYaoAlia zhongYaoAlia = new ZhongYaoAlia();
            zhongYaoAlia.setName(yaoAlia.getName());
            zhongYaoAlia.setBieming(yaoAlia.getBieming());
            DbService.getInstance().mYaoAliasService.addEntity(zhongYaoAlia);
        }
    }

    public static List<ZhongYaoAlia> getYaoAlia() {

        return DbService.getInstance().mYaoAliasService.findAll();

    }

    public static void saveAbout(List<About> aboutList) {
        DbService.getInstance().mAboutService.deleteAll();
        for (About about : aboutList) {
            DbService.getInstance().mAboutService.addEntity(about);
        }
    }

    public static List<About> getAbout() {


        return DbService.getInstance().mAboutService.findAll();

    }


    public static void saveTabNvaInDb(List<TabNav> bookNavList) {

       int order=0;
        for (TabNav nav : bookNavList) {
            // 内容列表存在才添加
            if (nav.getNavList() != null && !nav.getNavList().isEmpty()) {
               String tabNavId = DbService.getInstance().mTabNavService.getUUID();
                // 当前数据不存则,添加到数据库
                ArrayList<TabNav> navList = DbService.getInstance().mTabNavService.find(TabNavDao.Properties.CaseId.eq(nav.getCaseId()));
                if (navList == null || navList.isEmpty()) {
                    nav.setTabNavId(tabNavId);
                    nav.setOrder(order);
                    order++;
                    try {
                        DbService.getInstance().mTabNavService.addEntity(nav);
                    } catch (Exception e) {
                        // 处理异常，比如记录日志、通知管理员等
                        EasyLog.print("Failed to addEntity: " + e.getMessage());
                        // 根据具体情况决定是否需要重新抛出异常
                        //throw e;
                    }

                } else {
                    tabNavId = navList.get(0).getTabNavId();
                }

                for (TabNavBody item : nav.getNavList()) {

                    if (item.getBookNo() > 0)
                        TipsSingleData.getInstance().getNavTabBodyMap().put(item.getBookNo(), item);
                    // 当前数据不存则,添加到数据库
                    ArrayList<TabNavBody> list = DbService.getInstance().mTabNavBodyService.find(TabNavBodyDao.Properties.BookNo.eq(item.getBookNo()));
                    if (list == null || list.isEmpty()) {
                        item.setTabNavId(tabNavId);
                        item.setTabNavBodyId(DbService.getInstance().mTabNavBodyService.getUUID());
                        try {
                            DbService.getInstance().mTabNavBodyService.addEntity(item);
                        } catch (Exception e) {
                            // 处理异常，比如记录日志、通知管理员等
                            EasyLog.print("Failed to addEntity: " + e.getMessage());
                            // 根据具体情况决定是否需要重新抛出异常
                            //throw e;
                        }

                    }
                }

            }


        }
    }


    public static void saveMingCiContent(List<MingCiContent> detailList) {
        DbService.getInstance().mBeiMingCiService.deleteAll();
        for (MingCiContent mingCiContent : detailList) {

//            List<BeiMingCi> beiMingCiList = DbService.getInstance().mBeiMingCiService.getQueryBuilder().where(BeiMingCiDao.Properties.SignatureId.eq(mingCiContent.getSignatureId())).list();
//            if (beiMingCiList != null && !beiMingCiList.isEmpty()) {
//                BeiMingCi locBeiMingCi = beiMingCiList.get(0);
//                //有更新,与本地数据对比
//                if (!Objects.equals(locBeiMingCi.getSignature(), mingCiContent.getSignature())) {
//                    locBeiMingCi.setText(mingCiContent.getText());
//                    locBeiMingCi.setMingCiList(String.join(",", mingCiContent.getYaoList()));
//                    locBeiMingCi.setName(mingCiContent.getName());
//                    locBeiMingCi.setSignature(mingCiContent.getSignature());
//                    locBeiMingCi.setImageUrl(mingCiContent.getImageUrl());
//                    locBeiMingCi.setID(mingCiContent.getID());
//                    try {
//                        DbService.getInstance().mBeiMingCiService.updateEntity(locBeiMingCi);
//                    } catch (Exception e) {
//                        // 处理异常，比如记录日志、通知管理员等
//                        EasyLog.print("Failed to updateEntity: " + e.getMessage());
//                        // 根据具体情况决定是否需要重新抛出异常
//                        //throw e;
//                    }
//
//                }
//            } else {
//                BeiMingCi beiMingCi = new BeiMingCi();
//                beiMingCi.setText(mingCiContent.getText());
//                beiMingCi.setName(mingCiContent.getName());
//                beiMingCi.setMingCiList(String.join(",", mingCiContent.getYaoList()));
//                beiMingCi.setSignature(mingCiContent.getSignature());
//                beiMingCi.setSignatureId(mingCiContent.getSignatureId());
//                beiMingCi.setImageUrl(mingCiContent.getImageUrl());
//                beiMingCi.setID(mingCiContent.getID());
//                //yao1.setHeight(yao.getHeight());
//                try {
//                    DbService.getInstance().mBeiMingCiService.addEntity(beiMingCi);
//                } catch (Exception e) {
//                    // 处理异常，比如记录日志、通知管理员等
//                    EasyLog.print("Failed to add entity: " + e.getMessage());
//                    // 根据具体情况决定是否需要重新抛出异常
//                    //throw e;
//                }
//            }

            BeiMingCi beiMingCi = new BeiMingCi();
            beiMingCi.setText(RC4Helper.encrypt(mingCiContent.getText()));
            beiMingCi.setName(mingCiContent.getName());
            beiMingCi.setMingCiList(String.join(",", mingCiContent.getYaoList()));
            beiMingCi.setSignature(mingCiContent.getSignature());
            beiMingCi.setSignatureId(mingCiContent.getSignatureId());
            beiMingCi.setImageUrl(mingCiContent.getImageUrl());
            beiMingCi.setID(mingCiContent.getID());
            //yao1.setHeight(yao.getHeight());
            try {
                DbService.getInstance().mBeiMingCiService.addEntity(beiMingCi);
            } catch (Exception e) {
                // 处理异常，比如记录日志、通知管理员等
                EasyLog.print("Failed to add entity: " + e.getMessage());
                // 根据具体情况决定是否需要重新抛出异常
                //throw e;
            }

        }
    }


    public static void saveYaoData(List<Yao> detailList) {
        //保存内容
        DbService.getInstance().mYaoService.deleteAll();
        for (Yao yao : detailList) {
//            List<ZhongYao> zhongYaoList = DbService.getInstance().mYaoService.getQueryBuilder().where(ZhongYaoDao.Properties.SignatureId.eq(yao.getSignatureId())).list();
//            if (zhongYaoList != null && !zhongYaoList.isEmpty()) {
//                ZhongYao zhongYao = zhongYaoList.get(0);
//                //有更新,与本地数据对比
//                if (!Objects.equals(zhongYao.getSignature(), yao.getSignature())) {
//                    zhongYao.setText(yao.getText());
//                    zhongYao.setYaoList(String.join(",", yao.getYaoList()));
//                    zhongYao.setName(yao.getName());
//                    zhongYao.setSignature(yao.getSignature());
//                    zhongYao.setID(yao.getID());
//                    try {
//                        DbService.getInstance().mYaoService.updateEntity(zhongYao);
//                    } catch (Exception e) {
//                        // 处理异常，比如记录日志、通知管理员等
//                        EasyLog.print("Failed to updateEntity: " + e.getMessage());
//                        // 根据具体情况决定是否需要重新抛出异常
//                        //throw e;
//                    }
//                }
//            }
//            else {
//
//                ZhongYao yao1 = new ZhongYao();
//                yao1.setText(yao.getText());
//                yao1.setName(yao.getName());
//                yao1.setYaoList(String.join(",", yao.getYaoList()));
//                yao1.setID(yao.getID());
//                yao1.setSignature(yao.getSignature());
//                yao1.setSignatureId(yao.getSignatureId());
//                try {
//                    DbService.getInstance().mYaoService.addEntity(yao1);
//                } catch (Exception e) {
//                    // 处理异常，比如记录日志、通知管理员等
//                    EasyLog.print("Failed to add entity: " + e.getMessage());
//                    // 根据具体情况决定是否需要重新抛出异常
//                    //throw e;
//                }
//            }
//


            ZhongYao yao1 = new ZhongYao();
            yao1.setText(RC4Helper.encrypt(yao.getText()));
            yao1.setName(yao.getName());
            yao1.setYaoList(String.join(",", yao.getYaoList()));
            yao1.setID(yao.getID());
            yao1.setSignature(yao.getSignature());
            yao1.setSignatureId(yao.getSignatureId());
            try {
                DbService.getInstance().mYaoService.addEntity(yao1);
            } catch (Exception e) {
                // 处理异常，比如记录日志、通知管理员等
                EasyLog.print("Failed to add entity: " + e.getMessage());
                // 根据具体情况决定是否需要重新抛出异常
                //throw e;
            }


        }
    }


    public static void getFangDetailList(HH2SectionData locFangDetailList, List<Fang> netFangDetailList, int bookId) {
        if (netFangDetailList == null || netFangDetailList.isEmpty() || bookId <= 0 || locFangDetailList == null || locFangDetailList.getData() == null) {
            EasyLog.print("FangDetailList is empty or null.or  bookId <=0 .");
            return;
        }

//        try {
//            StringBuilder chapterId = new StringBuilder();
//            for (Fang fang : netFangDetailList) {
//                chapterId.setLength(0);
//                chapterId.append(StringHelper.getUuid());
//                ArrayList<YaoFang> yaoFangList = DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.SignatureId.eq(fang.getSignatureId()));
//                if (yaoFangList != null && !yaoFangList.isEmpty()) {
//                    YaoFang locYaoFang = yaoFangList.get(0);
//                    locYaoFang.setText(fang.getText());
//                    locYaoFang.setFangList(String.join(",", fang.getFangList()));
//                    locYaoFang.setYaoList(String.join(",", fang.getYaoList()));
//                    locYaoFang.setSignature(fang.getSignature());
//                    locYaoFang.setName(fang.getName());
//                    DbService.getInstance().mYaoFangService.updateEntity(locYaoFang);
//
//                } else {
//                    YaoFang yaoFang = new YaoFang();
//                    yaoFang.setYaoCount(fang.getYaoCount());
//                    yaoFang.setName(fang.getName());
//                    yaoFang.setBookId(bookId);
//                    yaoFang.setID(fang.getID());
//                    yaoFang.setDrinkNum(fang.getDrinkNum());
//                    yaoFang.setText(fang.getText());
//                    yaoFang.setFangList(String.join(",", fang.getFangList()));
//                    yaoFang.setYaoList(String.join(",", fang.getYaoList()));
//                    yaoFang.setYaoFangID(chapterId.toString());
//                    yaoFang.setSignature(fang.getSignature());
//                    yaoFang.setSignatureId(fang.getSignatureId());
//                    DbService.getInstance().mYaoFangService.addEntity(yaoFang);
//                }
//
//                for (YaoUse content : fang.getStandardYaoList()) {
//                    ArrayList<YaoFangBody> yaoUseList = DbService.getInstance().mYaoFangBodyService.find(YaoFangBodyDao.Properties.SignatureId.eq(content.getSignatureId()));
//                    if (yaoUseList != null && !yaoUseList.isEmpty()) {
//                        YaoFangBody locYaoFangBody = yaoUseList.get(0);
//                        locYaoFangBody.setAmount(content.getAmount());
//                        locYaoFangBody.setExtraProcess(content.getExtraProcess());
//                        locYaoFangBody.setShowName(content.getShowName());
//                        locYaoFangBody.setSignature(content.getSignature());
//                        locYaoFangBody.setSuffix(content.getSuffix());
//                        locYaoFangBody.setWeight(content.getWeight());
//                        DbService.getInstance().mYaoFangBodyService.updateEntity(locYaoFangBody);
//                    } else {
//                        YaoFangBody yaoFangBody = getYaoFangBody(content, chapterId);
//                        DbService.getInstance().mYaoFangBodyService.addEntity(yaoFangBody);
//                    }
//
//                }
//            }
//        } catch (Exception e) {
//            EasyLog.print("Error processing detail list: " + e.getMessage());
//            throw e;
//        }

        StringBuilder chapterId = new StringBuilder();
        try {

            ArrayList<YaoFang> yaoFangList = DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId));
            for (YaoFang fang : yaoFangList) {

                DbService.getInstance().mYaoFangBodyService.deleteAll(YaoFangBodyDao.Properties.YaoFangID.eq(fang.getYaoFangID()));
                DbService.getInstance().mYaoFangService.deleteEntity(fang);
            }

            for (Fang fang : netFangDetailList) {
                chapterId.setLength(0);
                chapterId.append(StringHelper.getUuid());
                YaoFang yaoFang = new YaoFang();
                yaoFang.setYaoCount(fang.getYaoCount());
                yaoFang.setName(fang.getName());
                yaoFang.setBookId(bookId);
                yaoFang.setID(fang.getID());
                yaoFang.setDrinkNum(fang.getDrinkNum());
                yaoFang.setText(RC4Helper.encrypt(fang.getText()));
                yaoFang.setFangList(String.join(",", fang.getFangList()));
                yaoFang.setYaoList(String.join(",", fang.getYaoList()));
                yaoFang.setYaoFangID(chapterId.toString());
                yaoFang.setSignature(fang.getSignature());
                yaoFang.setSignatureId(fang.getSignatureId());
                DbService.getInstance().mYaoFangService.addEntity(yaoFang);

                for (YaoUse content : fang.getStandardYaoList()) {
                    YaoFangBody yaoFangBody = getYaoFangBody(content, chapterId);
                    DbService.getInstance().mYaoFangBodyService.addEntity(yaoFangBody);
                }
            }
        } catch (Exception e) {
            EasyLog.print("Error processing detail list: " + e.getMessage());
            throw e;
        }

    }

    private static @NonNull YaoFangBody getYaoFangBody(YaoUse content, StringBuilder chapterId) {
        YaoFangBody yaoFangBody = new YaoFangBody();
        yaoFangBody.setYaoFangBodyId(StringHelper.getUuid());
        yaoFangBody.setYaoFangID(chapterId.toString());
        yaoFangBody.setSuffix(content.getSuffix());
        yaoFangBody.setAmount(content.getAmount());
        yaoFangBody.setYaoID(content.getYaoID());
        yaoFangBody.setWeight(content.getWeight());
        yaoFangBody.setShowName(content.getShowName());
        yaoFangBody.setExtraProcess(content.getExtraProcess());
        yaoFangBody.setSignatureId(content.getSignatureId());
        yaoFangBody.setSignature(content.getSignature());
        return yaoFangBody;
    }


    public static boolean getBookDetailList(List<HH2SectionData> netDetailList, int bookId) {
        if (netDetailList == null || netDetailList.isEmpty() || bookId <= 0) {
            EasyLog.print("BookDetailList is empty or null. or  bookId <=0 .");
            return false;
        }

//        try {
//            StringBuilder chapterId = new StringBuilder();
//            for (HH2SectionData hh2SectionData : netDetailList) {
//                chapterId.setLength(0);
//                chapterId.append(StringHelper.getUuid());
//
//                ArrayList<BookChapter> bookChapterList = DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.SignatureId.eq(hh2SectionData.getSignatureId()));
//                if (bookChapterList != null && !bookChapterList.isEmpty()) {
//
//                    BookChapter locBookChapter = bookChapterList.get(0);
//                    locBookChapter.setHeader(hh2SectionData.getHeader());
//                    locBookChapter.setSection(hh2SectionData.getSection());
//                    locBookChapter.setSignature(hh2SectionData.getSignature());
//                    DbService.getInstance().mBookChapterService.updateEntity(locBookChapter);
//                } else {
//                    BookChapter bookChapter = new BookChapter();
//                    bookChapter.setSection(hh2SectionData.getSection());
//                    bookChapter.setHeader(hh2SectionData.getHeader());
//                    bookChapter.setBookId(bookId);
//                    bookChapter.setBookChapterId(chapterId.toString());
//                    bookChapter.setSignature(hh2SectionData.getSignature());
//                    bookChapter.setSignatureId(hh2SectionData.getSignatureId());
//                    DbService.getInstance().mBookChapterService.addEntity(bookChapter);
//                }
//
//                for (DataItem content : hh2SectionData.getData()) {
//                    ArrayList<BookChapterBody> bookChapterBodyList = DbService.getInstance().mBookChapterBodyService.find(BookChapterBodyDao.Properties.SignatureId.eq(content.getSignatureId()));
//                    if (bookChapterBodyList != null && !bookChapterBodyList.isEmpty()) {
//                        BookChapterBody locBookChapterBody = bookChapterBodyList.get(0);
//                        //有更新,与本地数据对比
//                        if (!Objects.equals(locBookChapterBody.getSignature(), content.getSignature())) {
//                            locBookChapterBody.setText(content.getText());
//                            locBookChapterBody.setNote(content.getNote());
//                            locBookChapterBody.setSectionvideo(content.getSectionvideo());
//                            locBookChapterBody.setSignature(content.getSignature());
//                            DbService.getInstance().mBookChapterBodyService.updateEntity(locBookChapterBody);
//                        }
//                    } else {
//                        // 获取章节内容
//                        BookChapterBody bookChapterBody = new BookChapterBody();
//                        bookChapterBody.setBookChapterBodyId(StringHelper.getUuid());
//                        bookChapterBody.setBookChapterId(chapterId.toString());
//                        bookChapterBody.setText(content.getText());
//                        bookChapterBody.setNote(content.getNote());
//                        bookChapterBody.setSectionvideo(content.getSectionvideo());
//                        bookChapterBody.setID(content.getID());
//                        bookChapterBody.setFangList(String.join(",", content.getFangList()));
//                        bookChapterBody.setSignature(content.getSignature());
//                        bookChapterBody.setSignatureId(content.getSignatureId());
//                        DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
//                    }
//
//                }
//            }
//
//        } catch (Exception e) {
//            EasyLog.print("Error processing detail list: " + e.getMessage());
//            //  throw e;
//            return false;
//        }

        StringBuilder chapterId = new StringBuilder();
        try {

            ArrayList<BookChapter> bookChapterList = DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId));
            for (BookChapter bookChapter : bookChapterList) {
                DbService.getInstance().mYaoFangBodyService.deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(bookChapter.getBookChapterId()));
                DbService.getInstance().mBookChapterService.deleteEntity(bookChapter);
            }

            for (HH2SectionData hh2SectionData : netDetailList) {
                chapterId.setLength(0);
                chapterId.append(StringHelper.getUuid());

                BookChapter bookChapter = new BookChapter();
                bookChapter.setSection(hh2SectionData.getSection());
                bookChapter.setHeader(hh2SectionData.getHeader());
                bookChapter.setBookId(bookId);
                bookChapter.setBookChapterId(chapterId.toString());
                bookChapter.setSignature(hh2SectionData.getSignature());
                bookChapter.setSignatureId(hh2SectionData.getSignatureId());
                DbService.getInstance().mBookChapterService.addEntity(bookChapter);

                for (DataItem content : hh2SectionData.getData()) {

                    // 获取章节内容
                    BookChapterBody bookChapterBody = new BookChapterBody();
                    bookChapterBody.setBookChapterBodyId(StringHelper.getUuid());
                    bookChapterBody.setBookChapterId(chapterId.toString());
                    bookChapterBody.setText(RC4Helper.encrypt(content.getText()));
                    bookChapterBody.setNote(RC4Helper.encrypt(content.getNote()));
                    bookChapterBody.setSectionvideo(RC4Helper.encrypt(content.getSectionvideo()));
                    bookChapterBody.setID(content.getID());
                    bookChapterBody.setFangList(String.join(",", content.getFangList()));
                    bookChapterBody.setSignature(content.getSignature());
                    bookChapterBody.setSignatureId(content.getSignatureId());
                    DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                }
            }

        } catch (Exception e) {
            EasyLog.print("Error processing detail list: " + e.getMessage());
            //  throw e;
            return false;
        }
        return true;
    }

    public static List<HH2SectionData> getBookChapterDetailList(int bookId) {

        ArrayList<BookChapter> bookChapterList = DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId));

        List<HH2SectionData> detailList = new ArrayList<>();
        try {
            for (BookChapter bookChapter : bookChapterList) {
                if (bookChapter == null || bookChapter.getData() == null) {
                    continue; // 跳过无效的章节
                }

                List<DataItem> dataList = new ArrayList<>();
                for (BookChapterBody bookChapterBody : bookChapter.getData()) {
                    DataItem content = new DataItem();
                    content.setText(RC4Helper.decrypt(bookChapterBody.getText()));
                    content.setNote(RC4Helper.decrypt(bookChapterBody.getNote()));
                    content.setSectionvideo(RC4Helper.decrypt(bookChapterBody.getSectionvideo()));
                    content.setID(bookChapterBody.getID());
                    content.setFangList(
                            (bookChapterBody.getFangList() != null && !bookChapterBody.getFangList().isEmpty())
                                    ? Arrays.asList(bookChapterBody.getFangList().split(","))
                                    : Arrays.asList()
                    );
                    dataList.add(content);
                }

                detailList.add(new HH2SectionData(dataList, bookChapter.getSection(), bookChapter.getHeader()));
            }

            return detailList;
        } catch (Exception e) {
            // 增加详细日志记录
            EasyLog.print("Error processing detail list: " + e.getMessage() + ", bookChapterList size: " + bookChapterList.size());
            return detailList;
        }
    }

    public static ArrayList<Fang> getFangDetailList(int bookId) {
        ArrayList<YaoFang> fangList = DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId));
        ArrayList<Fang> detailList = new ArrayList<>();
        if (fangList == null || fangList.isEmpty()) {
            EasyLog.print("FandetailList is empty or null.");
            return detailList;
        }

        try {
            for (YaoFang yaoFang : fangList) {
                Fang fang = new Fang();
                fang.setYaoCount(yaoFang.getYaoCount());
                fang.setName(yaoFang.getName());
                fang.setID(yaoFang.getID());
                fang.setDrinkNum(yaoFang.getDrinkNum());
                fang.setText(RC4Helper.decrypt(yaoFang.getText()));
                fang.setFangList((yaoFang.getFangList() != null && !yaoFang.getFangList().isEmpty())
                        ? Arrays.asList(yaoFang.getFangList().split(","))
                        : Arrays.asList());
                fang.setYaoList((yaoFang.getYaoList() != null && !yaoFang.getYaoList().isEmpty())
                        ? Arrays.asList(yaoFang.getYaoList().split(","))
                        : Arrays.asList());
                fang.setID(yaoFang.getID());
                //
                for (YaoFangBody content : yaoFang.getStandardYaoList()) {
                    // 获取章节内容
                    YaoUse yaoUse = new YaoUse();
                    yaoUse.setSuffix(content.getSuffix());
                    yaoUse.setAmount(content.getAmount());
                    yaoUse.setYaoID(content.getYaoID());
                    yaoUse.setWeight(content.getWeight());
                    yaoUse.setShowName(content.getShowName());
                    yaoUse.setExtraProcess(content.getExtraProcess());
                    fang.setStandardYaoList(yaoUse);
                }
                detailList.add(fang);
            }
        } catch (Exception e) {
            EasyLog.print("Error processing detail list: " + e.getMessage());
            throw e;
        }
        return detailList;
    }

    public static ArrayList<Yao> getYaoData() {

        ArrayList<Yao> detailList = new ArrayList<>();
        ArrayList<ZhongYao> yaoList = DbService.getInstance().mYaoService.findAll();
        for (ZhongYao yao : yaoList) {
            Yao yao1 = new Yao();
            yao1.setText(RC4Helper.decrypt(yao.getText()));
            yao1.setName(yao.getName());

            if (yao.getYaoList() != null && !yao.getYaoList().isEmpty()) {
                //List<String> yaliasList =  Arrays.asList(yao.getYaoList().split("[,，。、.;]"));
                yao1.setYaoList(Arrays.asList(yao.getYaoList().split("[,，。、.;]")));
            }

            yao1.setID(yao.getID());
            //yao1.setHeight(yao.getHeight());
            detailList.add(yao1);
        }

        return detailList;
    }

    public static ArrayList<MingCiContent> getMingCi() {

        ArrayList<MingCiContent> detailList = new ArrayList<>();
        ArrayList<BeiMingCi> beiMingCiList = DbService.getInstance().mBeiMingCiService.findAll();
        for (BeiMingCi beiMingCi : beiMingCiList) {
            MingCiContent birdContent = new MingCiContent();
            birdContent.setText(RC4Helper.decrypt((beiMingCi.getText())));
            birdContent.setName(beiMingCi.getName());
            //birdContent.setMingCiList(String.join(",", beiMingCi.getMingCiList()));
            birdContent.setYaoList(
                    (beiMingCi.getMingCiList() != null && !beiMingCi.getMingCiList().isEmpty())
                            ? Arrays.asList(beiMingCi.getMingCiList().split(","))
                            : Arrays.asList()

            );
            birdContent.setID(beiMingCi.getID());
            //yao1.setHeight(yao.getHeight());
            detailList.add(birdContent);
        }
        return detailList;
    }


}
