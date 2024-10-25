package run.yigou.gxzy.ui.tips.Search;

import java.util.ArrayList;

import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;

public class SearchKey extends SearchKeyEntity {
    private String bookName;
    private int bookNo;
    private ArrayList<HH2SectionData> filteredData;

    /**
     *
     * @param searchKeyText 关键字
     * @param searchResTotalNum 搜索结果总数
     * @param bookName 书本名称
     * @param bookNo 书本编号
     * @param filteredData 搜索结果
     */
    public SearchKey(String searchKeyText, int searchResTotalNum, String bookName, int bookNo, ArrayList<HH2SectionData> filteredData) {
        super(new StringBuilder(searchKeyText), searchResTotalNum);
        this.bookName = bookName;
        this.bookNo = bookNo;
        this.filteredData = filteredData;
    }
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public int getBookNo() {
        return bookNo;
    }

    public void setBookNo(int bookNo) {
        this.bookNo = bookNo;
    }

    public ArrayList<HH2SectionData> getFilteredData() {
        return filteredData;
    }

    public void setFilteredData(ArrayList<HH2SectionData> filteredData) {
        this.filteredData = filteredData;
    }


}
