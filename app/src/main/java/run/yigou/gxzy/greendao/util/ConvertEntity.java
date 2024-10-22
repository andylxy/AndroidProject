package run.yigou.gxzy.greendao.util;

import androidx.annotation.NonNull;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.entity.BookChapter;
import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;
import run.yigou.gxzy.greendao.gen.BookChapterDao;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;
import run.yigou.gxzy.greendao.gen.YaoFangDao;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.DataBeans.YaoUse;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.utils.StringHelper;

public class ConvertEntity {


    public static void getFangDetailList(HH2SectionData locFangDetailList, List<Fang> netFangDetailList, int bookId) {
        if (netFangDetailList == null || netFangDetailList.isEmpty() || bookId <= 0 || locFangDetailList == null|| locFangDetailList.getData() == null) {
            EasyLog.print("FangDetailList is empty or null.or  bookId <=0 .");
            return;
        }

        try {
            StringBuilder chapterId = new StringBuilder();
            for (Fang fang : netFangDetailList) {
                chapterId.setLength(0);
                chapterId.append(StringHelper.getUuid());
                ArrayList<YaoFang> yaoFangList = DbService.getInstance().mYaoFangService.find(YaoFangDao.Properties.SignatureId.eq(fang.getSignatureId()));
                if (yaoFangList != null && !yaoFangList.isEmpty()) {
                    YaoFang locYaoFang = yaoFangList.get(0);
                    locYaoFang.setText(fang.getText());
                    locYaoFang.setFangList(String.join(",", fang.getFangList()));
                    locYaoFang.setYaoList(String.join(",", fang.getYaoList()));
                    locYaoFang.setSignature(fang.getSignature());
                    locYaoFang.setName(fang.getName());
                    DbService.getInstance().mYaoFangService.updateEntity(locYaoFang);

                } else {
                    YaoFang yaoFang = new YaoFang();
                    yaoFang.setYaoCount(fang.getYaoCount());
                    yaoFang.setName(fang.getName());
                    yaoFang.setBookId(bookId);
                    yaoFang.setID(fang.getID());
                    yaoFang.setDrinkNum(fang.getDrinkNum());
                    yaoFang.setText(fang.getText());
                    yaoFang.setFangList(String.join(",", fang.getFangList()));
                    yaoFang.setYaoList(String.join(",", fang.getYaoList()));
                    yaoFang.setYaoFangID(chapterId.toString());
                    yaoFang.setSignature(fang.getSignature());
                    yaoFang.setSignatureId(fang.getSignatureId());
                    DbService.getInstance().mYaoFangService.addEntity(yaoFang);
                }

                for (YaoUse content : fang.getStandardYaoList()) {
                    ArrayList<YaoFangBody> yaoUseList = DbService.getInstance().mYaoFangBodyService.find(YaoFangBodyDao.Properties.SignatureId.eq(content.getSignatureId()));
                    if (yaoUseList != null && !yaoUseList.isEmpty()) {
                        YaoFangBody locYaoFangBody = yaoUseList.get(0);
                        locYaoFangBody.setAmount(content.getAmount());
                        locYaoFangBody.setExtraProcess(content.getExtraProcess());
                        locYaoFangBody.setShowName(content.getShowName());
                        locYaoFangBody.setSignature(content.getSignature());
                        locYaoFangBody.setSuffix(content.getSuffix());
                        locYaoFangBody.setWeight(content.getWeight());
                        DbService.getInstance().mYaoFangBodyService.updateEntity(locYaoFangBody);
                    } else {
                        YaoFangBody yaoFangBody = getYaoFangBody(content, chapterId);
                        DbService.getInstance().mYaoFangBodyService.addEntity(yaoFangBody);
                    }

                }
            }
        } catch (Exception e) {
            EasyLog.print("Error processing detail list: " + e.getMessage());
            throw e;
        }

//        List<Fang> locDetailList = (List<Fang>) locFangDetailList.getData();
//        // 处理 netDetailList 和 locDetailList
//        for (Fang netSection : netFangDetailList) {
//            boolean found = false;
//            for (Fang locSection : locDetailList) {
//                if (Objects.equals(locSection.getSignatureId(), netSection.getSignatureId())) {
//                    found = true;
//                    if (!Objects.equals(locSection.getSignature(), netSection.getSignature())) {
//                        locSection.setSignature(netSection.getSignature());
//                        locSection.setText(netSection.getText());
//                        locSection.setFangList(Collections.singletonList(String.join(",", netSection.getFangList())));
//                        locSection.setYaoList(Collections.singletonList(String.join(",", netSection.getYaoList())));
//                        locSection.setName(netSection.getName());
//                    }
//                    break;
//                }
//            }
//            if (!found) {
//                locDetailList.add(netSection);
//            }
//        }
//
//        // 处理 netDetailList 和 locDetailList 中的 getData() 返回的 List
//        for (Fang netSection : netFangDetailList) {
//            List<YaoUse> netListData = netSection.getStandardYaoList();
//            if (netListData != null && !netListData.isEmpty()) {
//                for (YaoUse netSubSection : netListData) {
//                    boolean subFound = false;
//                    for (Fang locSection : locDetailList) {
//                        List<YaoUse> locListData = locSection.getStandardYaoList();
//                        if (locListData != null&& !locListData.isEmpty()) {
//                            for (YaoUse locSubSection : locListData) {
//                                if (Objects.equals(locSubSection.getSignatureId(), netSubSection.getSignatureId())) {
//                                    subFound = true;
//                                    if (!Objects.equals(locSubSection.getSignature(), netSubSection.getSignature())) {
//                                        locSubSection.setSignature(netSubSection.getSignature());
//                                        locSubSection.setAmount(netSubSection.getAmount());
//                                        locSubSection.setExtraProcess(netSubSection.getExtraProcess());
//                                        locSubSection.setShowName(netSubSection.getShowName());
//                                        locSubSection.setSuffix(netSubSection.getSuffix());
//                                        locSubSection.setWeight(netSubSection.getWeight());
//                                    }
//                                    break;
//                                }
//                            }
//                        }
//                        if (subFound) {
//                            break;
//                        }
//                    }
//                    if (!subFound) {
//                        for (Fang locSection : locDetailList) {
//                            if (Objects.equals(locSection.getSignatureId(), netSection.getSignatureId())) {
//                                locSection.getStandardYaoList().add(netSubSection);
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//        }


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


    public static void getBookDetailList(List<HH2SectionData> locDetailList, List<HH2SectionData> netDetailList, int bookId) {
        if (netDetailList == null || netDetailList.isEmpty() || bookId <= 0) {
            EasyLog.print("BookDetailList is empty or null. or  bookId <=0 .");
            return;
        }

        try {
            StringBuilder chapterId = new StringBuilder();
            for (HH2SectionData hh2SectionData : netDetailList) {
                chapterId.setLength(0);
                chapterId.append(StringHelper.getUuid());

                ArrayList<BookChapter> bookChapterList = DbService.getInstance().mBookChapterService.find(BookChapterDao.Properties.SignatureId.eq(hh2SectionData.getSignatureId()));
                if (bookChapterList != null && !bookChapterList.isEmpty()) {

                    BookChapter locBookChapter = bookChapterList.get(0);
                    locBookChapter.setHeader(hh2SectionData.getHeader());
                    locBookChapter.setSection(hh2SectionData.getSection());
                    locBookChapter.setSignature(hh2SectionData.getSignature());
                    DbService.getInstance().mBookChapterService.updateEntity(locBookChapter);
                } else {
                    BookChapter bookChapter = new BookChapter();
                    bookChapter.setSection(hh2SectionData.getSection());
                    bookChapter.setHeader(hh2SectionData.getHeader());
                    bookChapter.setBookId(bookId);
                    bookChapter.setBookChapterId(chapterId.toString());
                    bookChapter.setSignature(hh2SectionData.getSignature());
                    bookChapter.setSignatureId(hh2SectionData.getSignatureId());
                    DbService.getInstance().mBookChapterService.addEntity(bookChapter);
                }

                for (DataItem content : hh2SectionData.getData()) {
                    ArrayList<BookChapterBody> bookChapterBodyList = DbService.getInstance().mBookChapterBodyService.find(BookChapterBodyDao.Properties.SignatureId.eq(content.getSignatureId()));
                    if (bookChapterBodyList != null && !bookChapterBodyList.isEmpty()) {
                        BookChapterBody locBookChapterBody = bookChapterBodyList.get(0);
                        //有更新,与本地数据对比
                        if (!Objects.equals(locBookChapterBody.getSignature(), content.getSignature())) {
                            locBookChapterBody.setText(content.getText());
                            locBookChapterBody.setNote(content.getNote());
                            locBookChapterBody.setSectionvideo(content.getSectionvideo());
                            locBookChapterBody.setSignature(content.getSignature());
                            DbService.getInstance().mBookChapterBodyService.updateEntity(locBookChapterBody);
                        }
                    } else {
                        // 获取章节内容
                        BookChapterBody bookChapterBody = new BookChapterBody();
                        bookChapterBody.setBookChapterBodyId(StringHelper.getUuid());
                        bookChapterBody.setBookChapterId(chapterId.toString());
                        bookChapterBody.setText(content.getText());
                        bookChapterBody.setNote(content.getNote());
                        bookChapterBody.setSectionvideo(content.getSectionvideo());
                        bookChapterBody.setID(content.getID());
                        bookChapterBody.setFangList(String.join(",", content.getFangList()));
                        bookChapterBody.setSignature(content.getSignature());
                        bookChapterBody.setSignatureId(content.getSignatureId());
                        DbService.getInstance().mBookChapterBodyService.addEntity(bookChapterBody);
                    }

                }
            }
        } catch (Exception e) {
            EasyLog.print("Error processing detail list: " + e.getMessage());
            //  throw e;
        }

//        // 处理 netDetailList 和 locDetailList
//        for (HH2SectionData netSection : netDetailList) {
//            boolean found = false;
//            for (HH2SectionData locSection : locDetailList) {
//                if (Objects.equals(locSection.getSignatureId(), netSection.getSignatureId())) {
//                    found = true;
//                    if (!Objects.equals(locSection.getSignature(), netSection.getSignature())) {
//                        locSection.setSignature(netSection.getSignature());
//                        locSection.setHeader(netSection.getHeader());
//                        locSection.setSection(netSection.getSection());
//                    }
//                    break;
//                }
//            }
//            if (!found) {
//                locDetailList.add(netSection);
//            }
//        }
//
//        // 处理 netDetailList 和 locDetailList 中的 getData() 返回的 List
//        for (HH2SectionData netSection : netDetailList) {
//            List<? extends DataItem> netListData = netSection.getData();
//            if (netListData != null) {
//                for (DataItem netSubSection : netListData) {
//                    boolean subFound = false;
//                    for (HH2SectionData locSection : locDetailList) {
//                        List<? extends DataItem> locListData = locSection.getData();
//                        if (locListData != null) {
//                            for (DataItem locSubSection : locListData) {
//                                if (Objects.equals(locSubSection.getSignatureId(), netSubSection.getSignatureId())) {
//                                    subFound = true;
//                                    if (!Objects.equals(locSubSection.getSignature(), netSubSection.getSignature())) {
//                                        locSubSection.setSignature(netSubSection.getSignature());
//                                        locSubSection.setText(netSubSection.getText());
//                                        locSubSection.setNote(netSubSection.getNote());
//                                        locSubSection.setSectionvideo(netSubSection.getSectionvideo());
//                                    }
//                                    break;
//                                }
//                            }
//                        }
//                        if (subFound) {
//                            break;
//                        }
//                    }
//                    if (!subFound) {
//                        for (HH2SectionData locSection : locDetailList) {
//                            if (Objects.equals(locSection.getSignatureId(), netSection.getSignatureId())) {
//                                ((List<DataItem>) locSection.getData()).add(netSubSection);
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//        }


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
                    content.setText(bookChapterBody.getText());
                    content.setNote(bookChapterBody.getNote());
                    content.setSectionvideo(bookChapterBody.getSectionvideo());
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
//                private int yaoCount;
//                private int height;
//                private String name;
//                private int ID;
//                private int drinkNum;
//                private String text;
//                private List<String> fangList;
//                private List<String> yaoList;
//                private List<StandardYaoList> standardYaoList;
                fang.setYaoCount(yaoFang.getYaoCount());
                fang.setName(yaoFang.getName());
                fang.setID(yaoFang.getID());
                fang.setDrinkNum(yaoFang.getDrinkNum());
                fang.setText(yaoFang.getText());
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
//                    int YaoID;
//                    String amount;
//                    String extraProcess;
//                    float maxWeight;
//                    String showName;
//                    String suffix;
//                    float weight;
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
            yao1.setText(yao.getText());
            yao1.setName(yao.getName());
            //yao1.setYaoList(String.join(",", yao.getYaoList()));
            yao1.setYaoList(
                    (yao.getYaoList() != null && !yao.getYaoList().isEmpty())
                            ? Arrays.asList(yao.getYaoList().split(","))
                            : Arrays.asList());
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
            birdContent.setText(beiMingCi.getText());
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
