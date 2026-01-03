package run.yigou.gxzy.greendao.util;

import androidx.annotation.NonNull;

import com.hjq.http.EasyHttp;
import run.yigou.gxzy.other.EasyLog;
import com.hjq.http.listener.OnHttpListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.greendao.entity.About;
import run.yigou.gxzy.greendao.entity.AiConfig;
import run.yigou.gxzy.greendao.entity.AiConfigBody;
import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.entity.BookChapter;
import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.entity.ZhongYaoAlia;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;
import run.yigou.gxzy.greendao.gen.BookChapterDao;
import run.yigou.gxzy.greendao.gen.ChapterDao;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;
import run.yigou.gxzy.greendao.gen.TabNavDao;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;
import run.yigou.gxzy.greendao.gen.YaoFangDao;
import run.yigou.gxzy.http.api.ChapterListApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.fragment.HomeFragment;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.DataBeans.YaoAlia;
import run.yigou.gxzy.ui.tips.DataBeans.YaoUse;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;

import run.yigou.gxzy.Security.SecurityUtils;
import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;

public class ConvertEntity {



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

    public static void saveTabNvaInDb(List<TabNav> bookNavList, HomeFragment homeFragment) {
        int order = 0;
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
                    }
                } else {
                    tabNavId = navList.get(0).getTabNavId();
                }

                for (TabNavBody item : nav.getNavList()) {


                    // 当前数据不存则,添加到数据库
                    ArrayList<TabNavBody> list = DbService.getInstance().mTabNavBodyService.find(TabNavBodyDao.Properties.BookNo.eq(item.getBookNo()));
                    if (list == null || list.isEmpty() || list.get(0).getChapterCount() != item.getChapterCount()) {
                        item.setTabNavId(tabNavId);
                        item.setTabNavBodyId(DbService.getInstance().mTabNavBodyService.getUUID());
                        try {
                            DbService.getInstance().mTabNavBodyService.addEntity(item);
                        } catch (Exception e) {
                            // 处理异常，比如记录日志、通知管理员等
                            EasyLog.print("Failed to addEntity: " + e.getMessage());
                            return;
                        }
                        // 获取章节列表
                        //todo 添加延时执行
                        ThreadUtil.runInBackground(() -> getChapterList(homeFragment, item));
                    }
                }
            }
        }
    }

    public static void getChapterList(HomeFragment homeFragment, TabNavBody item) {
        EasyHttp.get(homeFragment)
                .api(new ChapterListApi().setBookId(item.getBookNo()))
                .request(new OnHttpListener<HttpData<List<Chapter>>>() {
                    @Override
                    public void onHttpSuccess(HttpData<List<Chapter>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            ArrayList<Chapter> list = DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(item.getBookNo()));
                            if (list == null || list.isEmpty() || item.getChapterCount() != list.size()) {
                                if (list != null && item.getChapterCount() != list.size())
                                    DbService.getInstance().mChapterService.deleteAll(ChapterDao.Properties.BookId.eq(item.getBookNo()));
                                for (Chapter chapter : data.getData()) {
                                    try {
                                        DbService.getInstance().mChapterService.addEntity(chapter);
                                    } catch (Exception e) {
                                        // 处理异常，比如记录日志、通知管理员等
                                        EasyLog.print("Failed to addEntity: " + e.getMessage());
                                        return;
                                    }
                                }
                            }
                        }
                    }

                @Override
                public void onHttpFail(Throwable e) {
                        // super.onFail(e); 
                        EasyLog.print(e);
                    }
                });
    }

    public static void saveMingCiContent(List<MingCiContent> detailList) {
        DbService.getInstance().mBeiMingCiService.deleteAll();
        for (MingCiContent mingCiContent : detailList) {
            BeiMingCi beiMingCi = new BeiMingCi();
            beiMingCi.setText(SecurityUtils.rc4Encrypt(mingCiContent.getText()));
            beiMingCi.setName(mingCiContent.getName());
            beiMingCi.setMingCiList(String.join(",", mingCiContent.getYaoList()));
            beiMingCi.setSignature(mingCiContent.getSignature());
            beiMingCi.setSignatureId(mingCiContent.getSignatureId());
            beiMingCi.setImageUrl(mingCiContent.getImageUrl());
            beiMingCi.setID(mingCiContent.getID());
            try {
                DbService.getInstance().mBeiMingCiService.addEntity(beiMingCi);
            } catch (Exception e) {
                // 处理异常，比如记录日志、通知管理员等
                EasyLog.print("Failed to add entity: " + e.getMessage());
            }
        }
    }

    public static void saveYaoData(List<Yao> detailList) {
        //保存内容
        DbService.getInstance().mYaoService.deleteAll();
        for (Yao yao : detailList) {
            ZhongYao yao1 = new ZhongYao();
            yao1.setText(SecurityUtils.rc4Encrypt(yao.getText()));
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
            }
        }
    }

    public static void getFangDetailList(List<Fang> netFangDetailList, int bookId) {
        if (netFangDetailList == null || netFangDetailList.isEmpty() || bookId <= 0) {
            EasyLog.print("FangDetailList is empty or null.or  bookId <=0 .");
            return;
        }

        try {
            ArrayList<YaoFang> yaoFangList = DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId));
            for (YaoFang fang : yaoFangList) {
                DbService.getInstance().mYaoFangBodyService.deleteAll(YaoFangBodyDao.Properties.YaoFangID.eq(fang.getYaoFangID()));
                DbService.getInstance().mYaoFangService.deleteEntity(fang);
            }

            for (Fang fang : netFangDetailList) {
                StringBuilder chapterId = new StringBuilder(StringHelper.getUuid());
                YaoFang yaoFang = new YaoFang();
                yaoFang.setYaoCount(fang.getYaoCount());
                yaoFang.setName(fang.getName());
                yaoFang.setBookId(bookId);
                yaoFang.setID(fang.getID());
                yaoFang.setDrinkNum(fang.getDrinkNum());
                yaoFang.setText(SecurityUtils.rc4Encrypt(fang.getText()));
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

    public static List<DataItem> getBookChapterDetailList(Chapter chapter) {
        ArrayList<BookChapter> bookChapterList = DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId()));

        List<DataItem> dataList = new ArrayList<>();
        try {
            for (BookChapter bookChapter : bookChapterList) {
                if (bookChapter.getData() == null) {
                    continue; // 跳过无效的章节
                }
                for (BookChapterBody bookChapterBody : bookChapter.getData()) {
                    DataItem content = new DataItem();
                    content.setText(SecurityUtils.rc4Decrypt(bookChapterBody.getText()));
                    content.setNote(SecurityUtils.rc4Decrypt(bookChapterBody.getNote()));
                    content.setSectionvideo(SecurityUtils.rc4Decrypt(bookChapterBody.getSectionvideo()));
                    content.setID(bookChapterBody.getID());
                    content.setFangList(
                            (bookChapterBody.getFangList() != null && !bookChapterBody.getFangList().isEmpty())
                                    ? Arrays.asList(bookChapterBody.getFangList().split(","))
                                    : new ArrayList<>()
                    );
                    dataList.add(content);
                }
            }
            return dataList;
        } catch (Exception e) {
            // 增加详细日志记录
            EasyLog.print("Error processing detail list: " + e.getMessage() + ", bookChapterList size: " + bookChapterList.size());
            return dataList;
        }
    }

    public static boolean saveAiConfigList(List<AiConfig> aiConfigs) {
        if (aiConfigs == null || aiConfigs.isEmpty()) {
            EasyLog.print("AiConfig is empty or null.");
            return false;
        }
        
        try {
            DbService.getInstance().mAiConfigService.deleteAll();
            DbService.getInstance().mAiConfigBodyService.deleteAll();

            for (AiConfig aiConfig : aiConfigs) {
                String aiConfigId = StringHelper.getUuid();
                aiConfig.setAiConfigId(aiConfigId);
                if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty())
                    aiConfig.setApiKey(SecurityUtils.rc4Encrypt(aiConfig.getApiKey()));
                DbService.getInstance().mAiConfigService.addEntity(aiConfig);

                for (AiConfigBody aiConfigBody : aiConfig.getModelList()) {
                    aiConfigBody.setAiConfigBodyId(StringHelper.getUuid());
                    aiConfigBody.setAiConfigId(aiConfigId);
                    DbService.getInstance().mAiConfigBodyService.addEntity(aiConfigBody);
                }
            }
        } catch (Exception e) {
            EasyLog.print("Error processing detail list: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean saveBookChapterDetailList(Chapter chapter, List<HH2SectionData> netDetailList) {
        if (netDetailList == null || netDetailList.isEmpty() || chapter.getBookId() <= 0) {
            EasyLog.print("BookDetailList is empty or null. or  bookId <=0 .");
            return false;
        }
        
        try {
            ArrayList<BookChapter> bookChapterList = DbService.getInstance().mBookChapterService
                    .find(BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId()));
            for (BookChapter bookChapter : bookChapterList) {
                DbService.getInstance().mBookChapterBodyService
                        .deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(bookChapter.getBookChapterId()));
                DbService.getInstance().mBookChapterService.deleteEntity(bookChapter);
            }

            for (HH2SectionData hh2SectionData : netDetailList) {
                String chapterId = StringHelper.getUuid();

                BookChapter bookChapter = new BookChapter();
                bookChapter.setSection(hh2SectionData.getSection());
                bookChapter.setHeader(hh2SectionData.getHeader());
                bookChapter.setBookId(chapter.getBookId());
                bookChapter.setBookChapterId(chapterId);
                bookChapter.setSignature(hh2SectionData.getSignature());
                bookChapter.setSignatureId(hh2SectionData.getSignatureId());
                DbService.getInstance().mBookChapterService.addEntity(bookChapter);

                for (DataItem content : hh2SectionData.getData()) {
                    // 获取章节内容
                    BookChapterBody bookChapterBody = new BookChapterBody();
                    bookChapterBody.setBookChapterBodyId(StringHelper.getUuid());
                    bookChapterBody.setBookChapterId(chapterId);
                    bookChapterBody.setText(SecurityUtils.rc4Encrypt(content.getText()));
                    bookChapterBody.setNote(SecurityUtils.rc4Encrypt(content.getNote()));
                    bookChapterBody.setSectionvideo(SecurityUtils.rc4Encrypt(content.getSectionvideo()));
                    bookChapterBody.setID(content.getID());
                    bookChapterBody.setFangList(String.join(",", content.getFangList()));
                    bookChapterBody.setSignature(content.getSignature());
                    bookChapterBody.setSignatureId(content.getSignatureId());
                    DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                }
            }
        } catch (Exception e) {
            EasyLog.print("Error processing detail list: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean getBookDetailList(List<HH2SectionData> netDetailList, int bookId) {
        if (netDetailList == null || netDetailList.isEmpty() || bookId <= 0) {
            EasyLog.print("BookDetailList is empty or null. or  bookId <=0 .");
            return false;
        }
        
        try {
            ArrayList<BookChapter> bookChapterList = DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId));
            for (BookChapter bookChapter : bookChapterList) {
                DbService.getInstance().mBookChapterBodyService.deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(bookChapter.getBookChapterId()));
                DbService.getInstance().mBookChapterService.deleteEntity(bookChapter);
            }

            for (HH2SectionData hh2SectionData : netDetailList) {
                String chapterId = StringHelper.getUuid();

                BookChapter bookChapter = new BookChapter();
                bookChapter.setSection(hh2SectionData.getSection());
                bookChapter.setHeader(hh2SectionData.getHeader());
                bookChapter.setBookId(bookId);
                bookChapter.setBookChapterId(chapterId);
                bookChapter.setSignature(hh2SectionData.getSignature());
                bookChapter.setSignatureId(hh2SectionData.getSignatureId());
                DbService.getInstance().mBookChapterService.addEntity(bookChapter);

                for (DataItem content : hh2SectionData.getData()) {
                    // 获取章节内容
                    BookChapterBody bookChapterBody = new BookChapterBody();
                    bookChapterBody.setBookChapterBodyId(StringHelper.getUuid());
                    bookChapterBody.setBookChapterId(chapterId);
                    bookChapterBody.setText(SecurityUtils.rc4Encrypt(content.getText()));
                    bookChapterBody.setNote(SecurityUtils.rc4Encrypt(content.getNote()));
                    bookChapterBody.setSectionvideo(SecurityUtils.rc4Encrypt(content.getSectionvideo()));
                    bookChapterBody.setID(content.getID());
                    bookChapterBody.setFangList(String.join(",", content.getFangList()));
                    bookChapterBody.setSignature(content.getSignature());
                    bookChapterBody.setSignatureId(content.getSignatureId());
                    DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                }
            }
        } catch (Exception e) {
            EasyLog.print("Error processing detail list: " + e.getMessage());
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
                    content.setText(SecurityUtils.rc4Decrypt(bookChapterBody.getText()));
                    content.setNote(SecurityUtils.rc4Decrypt(bookChapterBody.getNote()));
                    content.setSectionvideo(SecurityUtils.rc4Decrypt(bookChapterBody.getSectionvideo()));
                    content.setID(bookChapterBody.getID());
                    content.setFangList(
                            (bookChapterBody.getFangList() != null && !bookChapterBody.getFangList().isEmpty())
                                    ? Arrays.asList(bookChapterBody.getFangList().split(","))
                                    : new ArrayList<>()
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
                fang.setText(SecurityUtils.rc4Decrypt(yaoFang.getText()));
                fang.setFangList((yaoFang.getFangList() != null && !yaoFang.getFangList().isEmpty())
                        ? Arrays.asList(yaoFang.getFangList().split(","))
                        : new ArrayList<>());
                fang.setYaoList((yaoFang.getYaoList() != null && !yaoFang.getYaoList().isEmpty())
                        ? Arrays.asList(yaoFang.getYaoList().split(","))
                        : new ArrayList<>());
                fang.setID(yaoFang.getID());
                
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
            yao1.setText(SecurityUtils.rc4Decrypt(yao.getText()));
            yao1.setName(yao.getName());

            if (yao.getYaoList() != null && !yao.getYaoList().isEmpty()) {
                yao1.setYaoList(Arrays.asList(yao.getYaoList().split("[,，。、.;]")));
            }

            yao1.setID(yao.getID());
            detailList.add(yao1);
        }

        return detailList;
    }

    public static ArrayList<MingCiContent> getMingCi() {
        ArrayList<MingCiContent> detailList = new ArrayList<>();
        ArrayList<BeiMingCi> beiMingCiList = DbService.getInstance().mBeiMingCiService.findAll();
        for (BeiMingCi beiMingCi : beiMingCiList) {
            MingCiContent birdContent = new MingCiContent();
            birdContent.setText(SecurityUtils.rc4Decrypt(beiMingCi.getText()));
            birdContent.setName(beiMingCi.getName());
            birdContent.setYaoList(
                    (beiMingCi.getMingCiList() != null && !beiMingCi.getMingCiList().isEmpty())
                            ? Arrays.asList(beiMingCi.getMingCiList().split(","))
                            : new ArrayList<>()
            );
            birdContent.setID(beiMingCi.getID());
            detailList.add(birdContent);
        }
        return detailList;
    }
}