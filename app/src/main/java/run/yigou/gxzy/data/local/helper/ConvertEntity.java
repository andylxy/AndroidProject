package run.yigou.gxzy.data.local.helper;

import androidx.annotation.NonNull;

import com.hjq.http.EasyHttp;
import run.yigou.gxzy.log.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.data.local.entity.About;
import run.yigou.gxzy.data.local.entity.AiConfig;
import run.yigou.gxzy.data.local.entity.AiConfigBody;
import run.yigou.gxzy.data.local.entity.BeiMingCi;
import run.yigou.gxzy.data.local.entity.BookChapter;
import run.yigou.gxzy.data.local.entity.BookChapterBody;
import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.entity.TabNav;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.entity.YaoFang;
import run.yigou.gxzy.data.local.entity.YaoFangBody;
import run.yigou.gxzy.data.local.entity.ZhongYao;
import run.yigou.gxzy.data.local.entity.ZhongYaoAlia;
import run.yigou.gxzy.data.local.gen.BookChapterBodyDao;
import run.yigou.gxzy.data.local.gen.BookChapterDao;
import run.yigou.gxzy.data.local.gen.ChapterDao;
import run.yigou.gxzy.data.local.gen.TabNavBodyDao;
import run.yigou.gxzy.data.local.gen.TabNavDao;
import run.yigou.gxzy.data.local.gen.YaoFangBodyDao;
import run.yigou.gxzy.data.local.gen.YaoFangDao;
import run.yigou.gxzy.data.remote.api.ChapterListApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.ui.home.HomeFragment;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.YaoAlia;
import run.yigou.gxzy.data.model.YaoUse;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;

import run.yigou.gxzy.crypto.SecurityUtils;
import run.yigou.gxzy.utils.DebugLog;
import run.yigou.gxzy.utils.StringHelper;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * ConvertEntity ???????????
 * 
 * ???????????????
 * 
 * 1. ???????
 *    - ???????????????
 *    - ?? executeDatabaseOperation() ???????
 *    - ?????????????
 *    - ?????????????
 *    - ???????boolean?
 * 
 * 2. ???????
 *    - ??????????????
 *    - ?? executeDatabaseOperation() ???????
 *    - ?????????????
 *    - ?????????????
 *    - ???????ArrayList?
 * 
 * 3. ???????
 *    - ??????????????
 *    - ????????????
 *    - ?????????? encryptIfNotEmpty()
 *    - ??????? listToString() ??
 *    - ????????????null
 * 
 * ?????
 * 1. ???? executeDatabaseOperation() ???????
 * 2. ??????????????????
 * 3. ?? encryptIfNotEmpty() / decryptIfNotEmpty() ??????
 * 4. ?? splitStringToList() / listToString() ??????
 * 5. ???????????????????
 * 6. ?????????????false??
 * 7. ?????JavaDoc??
 * 8. ????????????????
 * 9. ??????????????
 * 10. ?catch???????????
 * 
 * ???????
 * - executeDatabaseOperation() - ??????????????????
 * - encryptIfNotEmpty() - ?????????????????
 * - decryptIfNotEmpty() - ?????????????????
 * - splitStringToList() - ??????????????????????
 * - listToString() - ??????????????
 * - StringHelper.getUuid() - ?????UUID???
 * - EasyLog.print() - ?????????
 */

/**
 * ?????????
 * 
 * ?????
 * 1. ???????????????
 * 2. ????/????
 * 3. ???????
 * 4. ??????
 * 
 * ?????
 * - ????????????????
 * - ?????????????????
 * - ?????????
 * 
 * @author Android ???
 * @author Zhs (xiaoyang_02@qq.com)
 * @since 2018/10/18
 */
public class ConvertEntity {
    
    /**
     * ????
     */
    private static final String TAG = "ConvertEntity";
    
    /**
     * ?????
     */
    private static final String LIST_SEPARATOR = ",";
    
    /**
     * ????????????????????
     */
    private static final String REGEX_SEPARATOR = "[,???.;]";
    
    /**
     * ??????????????
     * 
     * @param operation ?????
     * @param operationName ???????????
     * @param <T> ????
     * @return ??????????null
     */
    private static <T> T executeDatabaseOperation(DatabaseOperation<T> operation, String operationName) {
        try {
            return operation.execute();
        } catch (Exception e) {
            EasyLog.print(TAG, "??????? [" + operationName + "]: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * ???????
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * ???????
     * 
     * @param text ??????
     * @return ????????????????????
     */
    private static String encryptIfNotEmpty(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return SecurityUtils.rc4Encrypt(text);
    }
    
    /**
     * ???????
     * 
     * @param encryptedText ??????
     * @return ????????????????????
     */
    private static String decryptIfNotEmpty(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return "";
        }
        return SecurityUtils.rc4Decrypt(encryptedText);
    }
    
    /**
     * ?????????
     * 
     * @param text ???????
     * @param useRegex ????????????
     * @return ???????????????????
     */
    private static List<String> splitStringToList(String text, boolean useRegex) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String separator = useRegex ? REGEX_SEPARATOR : LIST_SEPARATOR;
        return Arrays.asList(text.split(separator));
    }
    
    /**
     * ?????????
     * 
     * @param list ??????
     * @return ?????????????????????
     */
    private static String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(LIST_SEPARATOR, list);
    }



/**
     * ????????????
     * 
     * @param yaoAliaList ??????
     */
    public static void saveYaoAlia(List<YaoAlia> yaoAliaList) {
        if (yaoAliaList == null || yaoAliaList.isEmpty()) {
            EasyLog.print(TAG, "?????????????");
            return;
        }
        
        try {
            DbService.getInstance().mYaoAliasService.deleteAll();
            
            int successCount = 0;
            for (YaoAlia yaoAlia : yaoAliaList) {
                if (yaoAlia == null) {
                    continue;
                }
                
                ZhongYaoAlia zhongYaoAlia = new ZhongYaoAlia();
                zhongYaoAlia.setName(yaoAlia.getName());
                zhongYaoAlia.setBieming(yaoAlia.getBieming());
                
                try {
                    DbService.getInstance().mYaoAliasService.addEntity(zhongYaoAlia);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "????????: " + e.getMessage());
                }
            }
            
            EasyLog.print(TAG, "???? " + successCount + "/" + yaoAliaList.size() + " ???????");
        } catch (Exception e) {
            EasyLog.print(TAG, "??????????: " + e.getMessage());
        }
    }

/**
     * ??????????
     * 
     * @return ???????????????????
     */
    public static List<ZhongYaoAlia> getYaoAlia() {
        return executeDatabaseOperation(
            () -> DbService.getInstance().mYaoAliasService.findAll(),
            "????????"
        );
    }

/**
     * ??????????
     * 
     * @param aboutList ??????
     */
    public static void saveAbout(List<About> aboutList) {
        if (aboutList == null || aboutList.isEmpty()) {
            EasyLog.print(TAG, "?????????????");
            return;
        }
        
        try {
            DbService.getInstance().mAboutService.deleteAll();
            
            int successCount = 0;
            for (About about : aboutList) {
                if (about == null) {
                    continue;
                }
                
                try {
                    DbService.getInstance().mAboutService.addEntity(about);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "????????: " + e.getMessage());
                }
            }
            
            EasyLog.print(TAG, "???? " + successCount + "/" + aboutList.size() + " ???????");
        } catch (Exception e) {
            EasyLog.print(TAG, "??????????: " + e.getMessage());
        }
    }

/**
     * ????????
     * 
     * @return ???????????????????
     */
    public static List<About> getAbout() {
        return executeDatabaseOperation(
            () -> DbService.getInstance().mAboutService.findAll(),
            "??????"
        );
    }

/**
     * ??????????
     * 
     * @param bookNavList ??????
     * @param homeFragment ?????????????
     */
    public static void saveTabNvaInDb(List<TabNav> bookNavList, HomeFragment homeFragment) {
        if (bookNavList == null || bookNavList.isEmpty()) {
            EasyLog.print(TAG, "?????????????");
            return;
        }
        
        if (homeFragment == null) {
            EasyLog.print(TAG, "HomeFragment???????????");
            return;
        }
        
        int order = 0;
        int processedNavCount = 0;
        int processedBodyCount = 0;
        
        for (TabNav nav : bookNavList) {
            if (nav == null || nav.getNavList() == null || nav.getNavList().isEmpty()) {
                continue; // ?????????
            }
            
            String tabNavId = processTabNav(nav, order);
            if (tabNavId == null) {
                continue;
            }
            order++;
            processedNavCount++;
            
            // ??????
            for (TabNavBody item : nav.getNavList()) {
                if (item == null) {
                    continue;
                }
                
                if (processTabNavBody(item, tabNavId, homeFragment)) {
                    processedBodyCount++;
                }
            }
        }
        
        EasyLog.print(TAG, "???????????? " + processedNavCount + " ????" + processedBodyCount + " ????");
    }
    
    /**
     * ????????
     * 
     * @param nav ????
     * @param order ????
     * @return ??ID??????????null
     */
    private static String processTabNav(TabNav nav, int order) {
        try {
            // ???????
            ArrayList<TabNav> existingNavList = executeDatabaseOperation(() ->
                DbService.getInstance().mTabNavService.find(TabNavDao.Properties.CaseId.eq(nav.getCaseId())),
                "????" + nav.getCaseId()
            );
            
            if (existingNavList != null && !existingNavList.isEmpty()) {
                EasyLog.print(TAG, "??????????ID: " + nav.getCaseId());
                return existingNavList.get(0).getTabNavId();
            }
            
            // ?????
            String tabNavId = StringHelper.getUuid();
            nav.setTabNavId(tabNavId);
            nav.setOrder(order);
            
            executeDatabaseOperation(() -> {
                DbService.getInstance().mTabNavService.addEntity(nav);
                return true;
            }, "????" + nav.getCaseId());
            
            EasyLog.print(TAG, "??????: " + nav.getCaseId());
            return tabNavId;
            
        } catch (Exception e) {
            EasyLog.print(TAG, "?????? " + nav.getCaseId() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * ????????
     * 
     * @param item ????
     * @param tabNavId ??ID
     * @param homeFragment ????
     * @return ??????
     */
    private static boolean processTabNavBody(TabNavBody item, String tabNavId, HomeFragment homeFragment) {
        try {
            // ??????????????????
            ArrayList<TabNavBody> existingBodyList = executeDatabaseOperation(() ->
                DbService.getInstance().mTabNavBodyService.find(TabNavBodyDao.Properties.BookNo.eq(item.getBookNo())),
                "??????" + item.getBookNo()
            );
            
            boolean needsUpdate = shouldUpdateTabNavBody(existingBodyList, item.getChapterCount());
            
            if (!needsUpdate) {
                EasyLog.print(TAG, "????????: " + item.getBookNo());
                return true;
            }
            
            // ?????????
            item.setTabNavId(tabNavId);
            item.setTabNavBodyId(StringHelper.getUuid());
            
            executeDatabaseOperation(() -> {
                DbService.getInstance().mTabNavBodyService.addEntity(item);
                return true;
            }, "??????" + item.getBookNo());
            
            EasyLog.print(TAG, "?????????????????: " + item.getBookNo());
            // ????????
            ThreadUtil.runInBackground(() -> getChapterList(homeFragment, item));
            return true;
            
        } catch (Exception e) {
            EasyLog.print(TAG, "???????? " + item.getBookNo() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * ????????????
     * 
     * @param existingBodyList ??????
     * @param newChapterCount ?????
     * @return ??????
     */
    private static boolean shouldUpdateTabNavBody(ArrayList<TabNavBody> existingBodyList, int newChapterCount) {
        if (existingBodyList == null || existingBodyList.isEmpty()) {
            return true; // ???????????
        }
        
        // ?????????????????????
        return existingBodyList.get(0).getChapterCount() != newChapterCount;
    }

/**
     * ???????????????
     * 
     * @param homeFragment ????
     * @param item ?????
     */
    public static void getChapterList(HomeFragment homeFragment, TabNavBody item) {
        if (homeFragment == null || item == null) {
            EasyLog.print(TAG, "?????????????");
            return;
        }
        
        EasyHttp.get(homeFragment)
                .api(new ChapterListApi().setBookId(item.getBookNo()))
                .request(new HttpCallback<HttpData<List<Chapter>>>(homeFragment) {
                    @Override
                    public void onSucceed(HttpData<List<Chapter>> data) {
                        if (data == null || data.getData() == null || data.getData().isEmpty()) {
                            EasyLog.print(TAG, "??" + item.getBookNo() + "???????");
                            return;
                        }
                        
                        processChapterList(data.getData(), item);
                    }

                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "????" + item.getBookNo() + "??????: " + e.getMessage());
                        super.onFail(e);
                    }
                });
    }
    
    /**
     * ????????
     * 
     * @param chapters ????
     * @param item ?????
     */
    private static void processChapterList(List<Chapter> chapters, TabNavBody item) {
        try {
            // ????????
            ArrayList<Chapter> existingChapters = executeDatabaseOperation(() ->
                DbService.getInstance().mChapterService.find(ChapterDao.Properties.BookId.eq(item.getBookNo())),
                "????" + item.getBookNo() + "?????"
            );
            
            boolean needsUpdate = shouldUpdateChapters(existingChapters, chapters.size(), item.getChapterCount());
            
            if (!needsUpdate) {
                EasyLog.print(TAG, "??" + item.getBookNo() + "?????????");
                return;
            }
            
            // ???????????
            if (existingChapters != null && !existingChapters.isEmpty()) {
                DbService.getInstance().mChapterService.deleteAll(ChapterDao.Properties.BookId.eq(item.getBookNo()));
                EasyLog.print(TAG, "?????" + item.getBookNo() + "??????");
            }
            
            // ???????
            int successCount = saveChaptersBatch(chapters, item.getBookNo());
            EasyLog.print(TAG, "??????" + item.getBookNo() + "? " + successCount + "/" + chapters.size() + " ???");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "????" + item.getBookNo() + "??????: " + e.getMessage());
        }
    }
    
    /**
     * ????????????
     * 
     * @param existingChapters ??????
     * @param newChapterCount ?????
     * @param expectedChapterCount ???????
     * @return ??????
     */
    private static boolean shouldUpdateChapters(ArrayList<Chapter> existingChapters, int newChapterCount, int expectedChapterCount) {
        if (existingChapters == null || existingChapters.isEmpty()) {
            return true; // ???????????
        }
        
        // ?????????????????????
        return existingChapters.size() != expectedChapterCount;
    }
    
    /**
     * ????????
     * 
     * @param chapters ????
     * @param bookId ??ID
     * @return ?????????
     */
    private static int saveChaptersBatch(List<Chapter> chapters, int bookId) {
        if (chapters == null || chapters.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        for (Chapter chapter : chapters) {
            if (chapter == null) {
                continue;
            }
            
            try {
                executeDatabaseOperation(() -> {
                    DbService.getInstance().mChapterService.addEntity(chapter);
                    return true;
                }, "????" + chapter.getId());
                successCount++;
            } catch (Exception e) {
                EasyLog.print(TAG, "??????: " + e.getMessage());
            }
        }
        
        return successCount;
    }

/**
     * ????????????
     * 
     * @param detailList ??????
     */
    public static void saveMingCiContent(List<MingCiContent> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            EasyLog.print(TAG, "?????????????");
            return;
        }
        
        try {
            DbService.getInstance().mBeiMingCiService.deleteAll();
            
            int successCount = 0;
            for (MingCiContent mingCiContent : detailList) {
                if (mingCiContent == null) {
                    continue;
                }
                
                BeiMingCi beiMingCi = new BeiMingCi();
                beiMingCi.setText(encryptIfNotEmpty(mingCiContent.getText()));
                beiMingCi.setName(mingCiContent.getName());
                beiMingCi.setMingCiList(listToString(mingCiContent.getYaoList()));
                beiMingCi.setSignature(mingCiContent.getSignature());
                beiMingCi.setSignatureId(mingCiContent.getSignatureId());
                beiMingCi.setImageUrl(mingCiContent.getImageUrl());
                beiMingCi.setID(mingCiContent.getID());
                
                try {
                    DbService.getInstance().mBeiMingCiService.addEntity(beiMingCi);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "????????: " + e.getMessage());
                }
            }
            
            EasyLog.print(TAG, "???? " + successCount + "/" + detailList.size() + " ???????");
        } catch (Exception e) {
            EasyLog.print(TAG, "??????????: " + e.getMessage());
        }
    }

/**
     * ??????????
     * 
     * @param detailList ??????
     */
    public static void saveYaoData(List<Yao> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            EasyLog.print(TAG, "?????????????");
            return;
        }
        
        try {
            DbService.getInstance().mYaoService.deleteAll();
            
            int successCount = 0;
            for (Yao yao : detailList) {
                if (yao == null) {
                    continue;
                }
                
                ZhongYao zhongYao = new ZhongYao();
                zhongYao.setText(encryptIfNotEmpty(yao.getText()));
                zhongYao.setName(yao.getName());
                zhongYao.setYaoList(listToString(yao.getYaoList()));
                zhongYao.setID(yao.getID());
                zhongYao.setSignature(yao.getSignature());
                zhongYao.setSignatureId(yao.getSignatureId());
                
                try {
                    DbService.getInstance().mYaoService.addEntity(zhongYao);
                    successCount++;
                } catch (Exception e) {
                    EasyLog.print(TAG, "????????: " + e.getMessage());
                }
            }
            
            EasyLog.print(TAG, "???? " + successCount + "/" + detailList.size() + " ?????");
        } catch (Exception e) {
            EasyLog.print(TAG, "????????: " + e.getMessage());
        }
    }

/**
     * ????????????
     * 
     * @param netFangDetailList ??????
     * @param bookId ??ID
     */
    public static void getFangDetailList(List<Fang> netFangDetailList, int bookId) {
        if (netFangDetailList == null || netFangDetailList.isEmpty() || bookId <= 0) {
            EasyLog.print(TAG, "???????????ID???????");
            return;
        }

        executeDatabaseOperation(() -> {
            // ????????
            ArrayList<YaoFang> yaoFangList = DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId));
            if (yaoFangList != null && !yaoFangList.isEmpty()) {
                for (YaoFang fang : yaoFangList) {
                    if (fang != null) {
                        DbService.getInstance().mYaoFangBodyService.deleteAll(YaoFangBodyDao.Properties.YaoFangID.eq(fang.getYaoFangID()));
                        DbService.getInstance().mYaoFangService.deleteEntity(fang);
                    }
                }
                EasyLog.print(TAG, "?????" + bookId + "??????");
            }

            // ????????
            int successCount = 0;
            for (Fang fang : netFangDetailList) {
                if (fang == null) {
                    continue;
                }
                
                String yaoFangId = StringHelper.getUuid();
                YaoFang yaoFang = convertFangToYaoFang(fang, bookId, yaoFangId);
                if (yaoFang != null) {
                    DbService.getInstance().mYaoFangService.addEntity(yaoFang);
                    successCount++;
                    
                    // ??????
                    saveYaoFangBodies(fang.getStandardYaoList(), yaoFangId);
                }
            }
            
            EasyLog.print(TAG, "??????" + bookId + "? " + successCount + "/" + netFangDetailList.size() + " ?????");
            return null;
            
        }, "????????");
    }
    
    /**
     * ?Fang???YaoFang
     * 
     * @param fang ????
     * @param bookId ??ID
     * @param yaoFangId ??ID
     * @return ????YaoFang??
     */
    private static YaoFang convertFangToYaoFang(Fang fang, int bookId, String yaoFangId) {
        if (fang == null || yaoFangId == null) {
            return null;
        }
        
        try {
            YaoFang yaoFang = new YaoFang();
            yaoFang.setYaoCount(fang.getYaoCount());
            yaoFang.setName(fang.getName());
            yaoFang.setBookId(bookId);
            yaoFang.setID(fang.getID());
            yaoFang.setDrinkNum(fang.getDrinkNum());
            yaoFang.setText(encryptIfNotEmpty(fang.getText()));
            yaoFang.setFangList(listToString(fang.getFangList()));
            yaoFang.setYaoList(listToString(fang.getYaoList()));
            yaoFang.setYaoFangID(yaoFangId);
            yaoFang.setSignature(fang.getSignature());
            yaoFang.setSignatureId(fang.getSignatureId());
            return yaoFang;
        } catch (Exception e) {
            EasyLog.print(TAG, "????????: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * ????????
     * 
     * @param yaoUseList ??????
     * @param yaoFangId ??ID
     */
    private static void saveYaoFangBodies(List<YaoUse> yaoUseList, String yaoFangId) {
        if (yaoUseList == null || yaoUseList.isEmpty() || yaoFangId == null) {
            return;
        }
        
        int successCount = 0;
        for (YaoUse content : yaoUseList) {
            if (content == null) {
                continue;
            }
            
            YaoFangBody yaoFangBody = convertYaoUseToYaoFangBody(content, yaoFangId);
            if (yaoFangBody != null) {
                DbService.getInstance().mYaoFangBodyService.addEntity(yaoFangBody);
                successCount++;
            }
        }
        
        if (successCount > 0) {
            EasyLog.print(TAG, "??????" + yaoFangId + "? " + successCount + "/" + yaoUseList.size() + " ????");
        }
    }

/**
     * ?YaoUse???YaoFangBody
     * 
     * @param content ??????
     * @param yaoFangId ??ID
     * @return ????YaoFangBody??
     */
    private static @NonNull YaoFangBody convertYaoUseToYaoFangBody(YaoUse content, String yaoFangId) {
        YaoFangBody yaoFangBody = new YaoFangBody();
        yaoFangBody.setYaoFangBodyId(StringHelper.getUuid());
        yaoFangBody.setYaoFangID(yaoFangId);
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

/**
     * ??????ID????????
     * 
     * @param chapter ????
     * @return ???????????????????
     */
    public static List<DataItem> getBookChapterDetailList(Chapter chapter) {
        if (chapter == null || chapter.getSignatureId() == null) {
            EasyLog.print(TAG, "??????");
            return new ArrayList<>();
        }
        
        ArrayList<BookChapter> bookChapterList = executeDatabaseOperation(() ->
            DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId())),
            "????" + chapter.getSignatureId() + "?????"
        );
        
        if (bookChapterList == null || bookChapterList.isEmpty()) {
            EasyLog.print(TAG, "??" + chapter.getSignatureId() + "???????");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            List<DataItem> dataList = new ArrayList<>();
            int validChapterCount = 0;
            int validBodyCount = 0;
            
            for (BookChapter bookChapter : bookChapterList) {
                if (bookChapter == null || bookChapter.getData() == null) {
                    continue; // ???????
                }
                validChapterCount++;
                
                for (BookChapterBody bookChapterBody : bookChapter.getData()) {
                    if (bookChapterBody == null) {
                        continue;
                    }
                    
                    DataItem content = convertBookChapterBodyToDataItem(bookChapterBody);
                    if (content != null) {
                        dataList.add(content);
                        validBodyCount++;
                    }
                }
            }
            
            EasyLog.print(TAG, "???? " + dataList.size() + " ????? (????:" + validChapterCount + ", ????:" + validBodyCount + ")");
            return dataList;
            
        }, "????????");
    }
    
    /**
     * ?BookChapterBody???DataItem
     * 
     * @param bookChapterBody ??????
     * @return ?????????????????null
     */
    private static DataItem convertBookChapterBodyToDataItem(BookChapterBody bookChapterBody) {
        if (bookChapterBody == null) {
            return null;
        }
        
        try {
            DataItem content = new DataItem();
            content.setText(decryptIfNotEmpty(bookChapterBody.getText()));
            content.setNote(decryptIfNotEmpty(bookChapterBody.getNote()));
            content.setSectionvideo(decryptIfNotEmpty(bookChapterBody.getSectionvideo()));
            content.setID(bookChapterBody.getID());
            content.setSignature(bookChapterBody.getSignature());
            content.setSignatureId(bookChapterBody.getSignatureId());
            content.setFangList(splitStringToList(bookChapterBody.getFangList(), false));
            return content;
        } catch (Exception e) {
            EasyLog.print(TAG, "????????: " + e.getMessage());
            return null;
        }
    }

/**
     * ??AI????????
     * 
     * @param aiConfigs AI????
     * @return ??????
     */
    public static boolean saveAiConfigList(List<AiConfig> aiConfigs) {
        if (aiConfigs == null || aiConfigs.isEmpty()) {
            EasyLog.print(TAG, "AI???????????");
            return false;
        }
        
        return executeDatabaseOperation(() -> {
            // ?????
            DbService.getInstance().mAiConfigService.deleteAll();
            DbService.getInstance().mAiConfigBodyService.deleteAll();
            
            int configCount = 0;
            int bodyCount = 0;
            
            for (AiConfig aiConfig : aiConfigs) {
                if (aiConfig == null) {
                    continue;
                }
                
                String aiConfigId = StringHelper.getUuid();
                aiConfig.setAiConfigId(aiConfigId);
                
                // ??API??
                if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty()) {
                    aiConfig.setApiKey(encryptIfNotEmpty(aiConfig.getApiKey()));
                }
                
                DbService.getInstance().mAiConfigService.addEntity(aiConfig);
                configCount++;

                // ?????
                if (aiConfig.getModelList() != null) {
                    for (AiConfigBody aiConfigBody : aiConfig.getModelList()) {
                        if (aiConfigBody == null) {
                            continue;
                        }
                        
                        aiConfigBody.setAiConfigBodyId(StringHelper.getUuid());
                        aiConfigBody.setAiConfigId(aiConfigId);
                        DbService.getInstance().mAiConfigBodyService.addEntity(aiConfigBody);
                        bodyCount++;
                    }
                }
            }
            
            EasyLog.print(TAG, "???? " + configCount + " ?AI???" + bodyCount + " ????");
            return true;
            
        }, "??AI????");
    }

/**
     * ????????????
     * 
     * @param chapter ????
     * @param netDetailList ????????
     * @return ??????
     */
    public static boolean saveBookChapterDetailList(Chapter chapter, List<HH2SectionData> netDetailList) {
        if (chapter == null || netDetailList == null || netDetailList.isEmpty() || chapter.getBookId() <= 0) {
            EasyLog.print(TAG, "?????chapter=" + chapter + ", netDetailList??=" + 
                       (netDetailList != null ? netDetailList.size() : "null") + ", bookId=" + chapter.getBookId());
            return false;
        }
        
        return executeDatabaseOperation(() -> {
            // ?????
            ArrayList<BookChapter> existingChapters = executeDatabaseOperation(() ->
                DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.SignatureId.eq(chapter.getSignatureId())),
                "??????" + chapter.getSignatureId()
            );
            
            if (existingChapters != null && !existingChapters.isEmpty()) {
                for (BookChapter existingChapter : existingChapters) {
                    if (existingChapter != null) {
                        DbService.getInstance().mBookChapterBodyService
                                .deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(existingChapter.getBookChapterId()));
                        DbService.getInstance().mBookChapterService.deleteEntity(existingChapter);
                    }
                }
                EasyLog.print(TAG, "??? " + existingChapters.size() + " ???????");
            }
            
            // ?????
            int sectionCount = 0;
            int contentCount = 0;
            
            for (HH2SectionData sectionData : netDetailList) {
                if (sectionData == null) {
                    continue;
                }
                
                String chapterId = StringHelper.getUuid();
                
                // ????
                BookChapter bookChapter = createBookChapter(chapter, sectionData, chapterId);
                if (bookChapter == null) {
                    continue;
                }
                
                executeDatabaseOperation(() -> {
                    DbService.getInstance().mBookChapterService.addEntity(bookChapter);
                    return true;
                }, "????" + bookChapter.getSignatureId());
                
                sectionCount++;
                
                // ??????
                if (sectionData.getData() != null) {
                    for (DataItem content : sectionData.getData()) {
                        if (content == null) {
                            continue;
                        }
                        
                        BookChapterBody bookChapterBody = createBookChapterBody(chapterId, content);
                        if (bookChapterBody != null) {
                            executeDatabaseOperation(() -> {
                                DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                                return true;
                            }, "??????" + content.getID());
                            
                            contentCount++;
                        }
                    }
                }
            }
            
            EasyLog.print(TAG, "???? " + sectionCount + " ????" + contentCount + " ????");
            return true;
            
        }, "????????");
    }
    
    /**
     * ??BookChapter??
     * 
     * @param chapter ????
     * @param sectionData ????
     * @param chapterId ??ID
     * @return ???BookChapter??
     */
    private static BookChapter createBookChapter(Chapter chapter, HH2SectionData sectionData, String chapterId) {
        if (chapter == null || sectionData == null || chapterId == null) {
            return null;
        }
        
        try {
            BookChapter bookChapter = new BookChapter();
            bookChapter.setSection(sectionData.getSection());
            bookChapter.setHeader(sectionData.getHeader());
            bookChapter.setBookId(chapter.getBookId());
            bookChapter.setBookChapterId(chapterId);
            bookChapter.setSignature(sectionData.getSignature());
            bookChapter.setSignatureId(sectionData.getSignatureId());
            return bookChapter;
        } catch (Exception e) {
            EasyLog.print(TAG, "????????: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * ??BookChapterBody??
     * 
     * @param chapterId ??ID
     * @param content ????
     * @return ???BookChapterBody??
     */
    private static BookChapterBody createBookChapterBody(String chapterId, DataItem content) {
        if (chapterId == null || content == null) {
            return null;
        }
        
        try {
            BookChapterBody bookChapterBody = new BookChapterBody();
            bookChapterBody.setBookChapterBodyId(StringHelper.getUuid());
            bookChapterBody.setBookChapterId(chapterId);
            bookChapterBody.setText(encryptIfNotEmpty(content.getText()));
            bookChapterBody.setNote(encryptIfNotEmpty(content.getNote()));
            bookChapterBody.setSectionvideo(encryptIfNotEmpty(content.getSectionvideo()));
            bookChapterBody.setID(content.getID());
            bookChapterBody.setSignature(content.getSignature());
            bookChapterBody.setSignatureId(content.getSignatureId());
            bookChapterBody.setFangList(listToString(content.getFangList()));
            return bookChapterBody;
        } catch (Exception e) {
            EasyLog.print(TAG, "??????????: " + e.getMessage());
            return null;
        }
    }

/**
     * ?????????????????ID?
     * 
     * @param netDetailList ????????
     * @param bookId ??ID
     * @return ??????
     */
    public static boolean getBookDetailList(List<HH2SectionData> netDetailList, int bookId) {
        if (netDetailList == null || netDetailList.isEmpty() || bookId <= 0) {
            EasyLog.print(TAG, "???????????ID???????");
            return false;
        }
        
        return executeDatabaseOperation(() -> {
            // ?????
            ArrayList<BookChapter> existingChapters = executeDatabaseOperation(() ->
                DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId)),
                "????" + bookId + "?????"
            );
            
            if (existingChapters != null && !existingChapters.isEmpty()) {
                for (BookChapter existingChapter : existingChapters) {
                    if (existingChapter != null) {
                        DbService.getInstance().mBookChapterBodyService
                                .deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(existingChapter.getBookChapterId()));
                        DbService.getInstance().mBookChapterService.deleteEntity(existingChapter);
                    }
                }
                EasyLog.print(TAG, "?????" + bookId + "? " + existingChapters.size() + " ???????");
            }
            
            // ?????
            int sectionCount = 0;
            int contentCount = 0;
            
            for (HH2SectionData sectionData : netDetailList) {
                if (sectionData == null) {
                    continue;
                }
                
                String chapterId = StringHelper.getUuid();
                
                // ????
                BookChapter bookChapter = createBookChapterByBookId(bookId, sectionData, chapterId);
                if (bookChapter == null) {
                    continue;
                }
                
                executeDatabaseOperation(() -> {
                    DbService.getInstance().mBookChapterService.addEntity(bookChapter);
                    return true;
                }, "??????" + bookChapter.getSignatureId());
                
                sectionCount++;
                
                // ??????
                if (sectionData.getData() != null) {
                    for (DataItem content : sectionData.getData()) {
                        if (content == null) {
                            continue;
                        }
                        
                        BookChapterBody bookChapterBody = createBookChapterBody(chapterId, content);
                        if (bookChapterBody != null) {
                            executeDatabaseOperation(() -> {
                                DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                                return true;
                            }, "????????" + content.getID());
                            
                            contentCount++;
                        }
                    }
                }
            }
            
            EasyLog.print(TAG, "??????" + bookId + "? " + sectionCount + " ????" + contentCount + " ????");
            return true;
            
        }, "????????");
    }
    
    /**
     * ??BookChapter???????ID?
     * 
     * @param bookId ??ID
     * @param sectionData ????
     * @param chapterId ??ID
     * @return ???BookChapter??
     */
    private static BookChapter createBookChapterByBookId(int bookId, HH2SectionData sectionData, String chapterId) {
        if (bookId <= 0 || sectionData == null || chapterId == null) {
            return null;
        }
        
        try {
            BookChapter bookChapter = new BookChapter();
            bookChapter.setSection(sectionData.getSection());
            bookChapter.setHeader(sectionData.getHeader());
            bookChapter.setBookId(bookId);
            bookChapter.setBookChapterId(chapterId);
            bookChapter.setSignature(sectionData.getSignature());
            bookChapter.setSignatureId(sectionData.getSignatureId());
            return bookChapter;
        } catch (Exception e) {
            EasyLog.print(TAG, "??????????: " + e.getMessage());
            return null;
        }
    }

/**
     * ????ID????????
     * 
     * @param bookId ??ID
     * @return ???????????????????
     */
    public static List<HH2SectionData> getBookChapterDetailList(int bookId) {
        if (bookId <= 0) {
            EasyLog.print(TAG, "??ID??: " + bookId);
            return new ArrayList<>();
        }
        
        ArrayList<BookChapter> bookChapterList = executeDatabaseOperation(() ->
            DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.BookId.eq(bookId)),
            "????" + bookId + "?????"
        );
        
        if (bookChapterList == null || bookChapterList.isEmpty()) {
            EasyLog.print(TAG, "??" + bookId + "???????");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            List<HH2SectionData> detailList = new ArrayList<>();
            int validChapterCount = 0;
            int validContentCount = 0;
            
            for (BookChapter bookChapter : bookChapterList) {
                if (bookChapter == null || bookChapter.getData() == null) {
                    continue; // ???????
                }

                List<DataItem> dataList = new ArrayList<>();
                for (BookChapterBody bookChapterBody : bookChapter.getData()) {
                    if (bookChapterBody == null) {
                        continue;
                    }
                    
                    DataItem content = convertBookChapterBodyToDataItem(bookChapterBody);
                    if (content != null) {
                        dataList.add(content);
                        validContentCount++;
                    }
                }
                
                if (!dataList.isEmpty()) {
                    detailList.add(new HH2SectionData(dataList, bookChapter.getSection(), bookChapter.getHeader()));
                    validChapterCount++;
                }
            }
            
            EasyLog.print(TAG, "??????" + bookId + "? " + detailList.size() + " ??? (????:" + validChapterCount + ", ????:" + validContentCount + ")");
            return detailList;
            
        }, "????????");
    }

/**
     * ????ID????????
     * 
     * @param bookId ??ID
     * @return ???????????????????
     */
    public static ArrayList<Fang> getFangDetailList(int bookId) {
        if (bookId <= 0) {
            EasyLog.print(TAG, "??ID??: " + bookId);
            return new ArrayList<>();
        }
        
        ArrayList<YaoFang> fangList = executeDatabaseOperation(
            () -> DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.BookId.eq(bookId)),
            "????" + bookId + "?????"
        );
        
        if (fangList == null || fangList.isEmpty()) {
            EasyLog.print(TAG, "??" + bookId + "???????");
            return new ArrayList<>();
        }

        return executeDatabaseOperation(() -> {
            ArrayList<Fang> detailList = new ArrayList<>();
            
            for (YaoFang yaoFang : fangList) {
                if (yaoFang == null) {
                    continue;
                }
                
                Fang fang = convertYaoFangToFang(yaoFang);
                if (fang != null) {
                    detailList.add(fang);
                }
            }
            
            EasyLog.print(TAG, "???? " + detailList.size() + "/" + fangList.size() + " ?????");
            return detailList;
            
        }, "??????");
    }
    
    /**
     * ?YaoFang?????Fang??
     * 
     * @param yaoFang YaoFang??
     * @return ????Fang??
     */
    private static Fang convertYaoFangToFang(YaoFang yaoFang) {
        if (yaoFang == null) {
            return null;
        }
        
        try {
            Fang fang = new Fang();
            fang.setYaoCount(yaoFang.getYaoCount());
            fang.setName(yaoFang.getName());
            fang.setID(yaoFang.getID());
            fang.setDrinkNum(yaoFang.getDrinkNum());
            fang.setText(decryptIfNotEmpty(yaoFang.getText()));
            fang.setFangList(splitStringToList(yaoFang.getFangList(), false));
            fang.setYaoList(splitStringToList(yaoFang.getYaoList(), false));
            fang.setID(yaoFang.getID());
            
            // ????????
            if (yaoFang.getStandardYaoList() != null) {
                for (YaoFangBody content : yaoFang.getStandardYaoList()) {
                    YaoUse yaoUse = convertYaoFangBodyToYaoUse(content);
                    if (yaoUse != null) {
                        fang.setStandardYaoList(yaoUse);
                    }
                }
            }
            
            return fang;
        } catch (Exception e) {
            EasyLog.print(TAG, "????????: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * ?YaoFangBody?????YaoUse??
     * 
     * @param content YaoFangBody??
     * @return ????YaoUse??
     */
    private static YaoUse convertYaoFangBodyToYaoUse(YaoFangBody content) {
        if (content == null) {
            return null;
        }
        
        try {
            YaoUse yaoUse = new YaoUse();
            yaoUse.setSuffix(content.getSuffix());
            yaoUse.setAmount(content.getAmount());
            yaoUse.setYaoID(content.getYaoID());
            yaoUse.setWeight(content.getWeight());
            yaoUse.setShowName(content.getShowName());
            yaoUse.setExtraProcess(content.getExtraProcess());
            yaoUse.setSignatureId(content.getSignatureId());
            yaoUse.setSignature(content.getSignature());
            return yaoUse;
        } catch (Exception e) {
            EasyLog.print(TAG, "????????: " + e.getMessage());
            return null;
        }
    }

/**
     * ????????
     * 
     * @return ???????????????????
     */
    public static ArrayList<Yao> getYaoData() {
        ArrayList<ZhongYao> yaoList = executeDatabaseOperation(
            () -> DbService.getInstance().mYaoService.findAll(),
            "??????"
        );
        
        if (yaoList == null || yaoList.isEmpty()) {
            EasyLog.print(TAG, "??????");
            return new ArrayList<>();
        }
        
        return executeDatabaseOperation(() -> {
            ArrayList<Yao> detailList = new ArrayList<>();
            
            for (ZhongYao yao : yaoList) {
                if (yao == null) {
                    continue;
                }
                
                Yao yao1 = convertZhongYaoToYao(yao);
                if (yao1 != null) {
                    detailList.add(yao1);
                }
            }
            
            EasyLog.print(TAG, "???? " + detailList.size() + "/" + yaoList.size() + " ?????");
            return detailList;
            
        }, "??????");
    }
    
    /**
     * ?ZhongYao?????Yao??
     * 
     * @param zhongYao ZhongYao??
     * @return ????Yao??
     */
    private static Yao convertZhongYaoToYao(ZhongYao zhongYao) {
        if (zhongYao == null) {
            return null;
        }
        
        try {
            Yao yao = new Yao();
            yao.setText(decryptIfNotEmpty(zhongYao.getText()));
            yao.setName(zhongYao.getName());
            yao.setYaoList(splitStringToList(zhongYao.getYaoList(), true));
            yao.setID(zhongYao.getID());
            yao.setSignature(zhongYao.getSignature());
            yao.setSignatureId(zhongYao.getSignatureId());
            return yao;
        } catch (Exception e) {
            EasyLog.print(TAG, "????????: " + e.getMessage());
            return null;
        }
    }

/**
     * ??????????
     * 
     * @return ???????????????????
     */
    public static ArrayList<MingCiContent> getMingCi() {
        ArrayList<BeiMingCi> beiMingCiList = executeDatabaseOperation(
            () -> DbService.getInstance().mBeiMingCiService.findAll(),
            "????????"
        );
        
        if (beiMingCiList == null || beiMingCiList.isEmpty()) {
            EasyLog.print(TAG, "????????");
            return new ArrayList<>();
        }
        
        return executeDatabaseOperation(() -> {
            ArrayList<MingCiContent> detailList = new ArrayList<>();
            
            for (BeiMingCi beiMingCi : beiMingCiList) {
                if (beiMingCi == null) {
                    continue;
                }
                
                MingCiContent content = convertBeiMingCiToMingCiContent(beiMingCi);
                if (content != null) {
                    detailList.add(content);
                }
            }
            
            EasyLog.print(TAG, "???? " + detailList.size() + "/" + beiMingCiList.size() + " ???????");
            return detailList;
            
        }, "????????");
    }
    
    /**
     * ?BeiMingCi?????MingCiContent??
     * 
     * @param beiMingCi BeiMingCi??
     * @return ????MingCiContent??
     */
    private static MingCiContent convertBeiMingCiToMingCiContent(BeiMingCi beiMingCi) {
        if (beiMingCi == null) {
            return null;
        }
        
        try {
            MingCiContent content = new MingCiContent();
            content.setText(decryptIfNotEmpty(beiMingCi.getText()));
            content.setName(beiMingCi.getName());
            content.setYaoList(splitStringToList(beiMingCi.getMingCiList(), false));
            content.setID(beiMingCi.getID());
            content.setSignature(beiMingCi.getSignature());
            content.setSignatureId(beiMingCi.getSignatureId());
            content.setImageUrl(beiMingCi.getImageUrl());
            return content;
        } catch (Exception e) {
            EasyLog.print(TAG, "????????: " + e.getMessage());
            return null;
        }
    }
}