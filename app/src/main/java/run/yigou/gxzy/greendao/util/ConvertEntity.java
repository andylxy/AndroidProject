package run.yigou.gxzy.greendao.util;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.entity.BookChapter;
import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.gen.BookChapterDao;
import run.yigou.gxzy.greendao.gen.YaoFangDao;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.DataBeans.YaoUse;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;

public class ConvertEntity {
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
                    (yao.getYaoList()!= null && !yao.getYaoList().isEmpty())
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
        ArrayList<BeiMingCi> beiMingCiList = DbService.getInstance(). mBeiMingCiService.findAll();
        for (BeiMingCi beiMingCi : beiMingCiList) {
            MingCiContent birdContent = new MingCiContent();
            birdContent.setText(beiMingCi.getText());
            birdContent.setName(beiMingCi.getName());
            //birdContent.setMingCiList(String.join(",", beiMingCi.getMingCiList()));
            birdContent.setYaoList(
                    (beiMingCi.getMingCiList()!= null && !beiMingCi.getMingCiList().isEmpty())
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
