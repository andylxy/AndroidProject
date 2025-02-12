
package run.yigou.gxzy.ui.tips.tipsutils;


import android.os.Build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class SingletonNetData {
    private static SingletonNetData data;


    protected List<String> allFang;
    private ArrayList<HH2SectionData> content;
    private ArrayList<HH2SectionData> searchResList;
    private ArrayList<HH2SectionData> fang;
    private int bookId;

   // private final ShowUpdateNotificationEvent mShowUpdateNotification = new ShowUpdateNotificationEvent();
    private static volatile SingletonNetData instance;

    public List<String> getAllFang() {
        return allFang;
    }

    private Map<String, String> yaoAliasDict;

    public Map<String, String> getYaoAliasDict() {
        return yaoAliasDict;
    }

//    public ShowUpdateNotificationEvent getShowUpdateNotification() {
//        return mShowUpdateNotification;
//    }


private final Map<Integer, Boolean> bookFang = new ConcurrentHashMap<>();

/**
 * 获取指定 bookFangId 的状态，默认为 false
 *
 * @param bookFangId 要查询的 bookFangId
 * @return 对应的布尔值，如果不存在则返回 false
 */
public Boolean getBookFang(Integer bookFangId) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return bookFang.getOrDefault(bookFangId, false);
    }
    return false;
}

/**
 * 设置指定 bookFangId 的状态为 true
 *
 * @param bookFangId 要设置的 bookFangId
 */
public void setBookFang(Integer bookFangId) {
    // 使用 putIfAbsent 避免不必要的覆盖操作
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        bookFang.putIfAbsent(bookFangId, true);
    }
}


    public void setYaoAliasDict(Map<String, String> yaoAliasDict) {
        if (yaoAliasDict != null)
            this.yaoAliasDict = yaoAliasDict;
        else this.yaoAliasDict = new HashMap<String, String>();

    }

    private Map<String, String> fangAliasDict;

    public void setFangAliasDict(Map<String, String> fangAliasDict) {
        if (fangAliasDict != null)
            this.fangAliasDict = fangAliasDict;
        else this.fangAliasDict = new HashMap<String, String>();
    }

    public Map<String, String> getFangAliasDict() {
        if (fangAliasDict == null) fangAliasDict = new HashMap<String, String>();
        return this.fangAliasDict;
    }


    public ArrayList<HH2SectionData> getContent() {
        if (this.content == null) this.content = new ArrayList<>();
        //如果实现数据监听处理接口.则由该接口处理后直接返回处理后的数据.
        if (mOnContentUpdateListener != null) {
            //如果实现通知接口,则通知数据已经更新
            ArrayList<HH2SectionData> updateList = mOnContentUpdateListener.contentDateUpdate(this.content);
            if (updateList == null) return new ArrayList<>();
            return updateList;
        }
        return this.content;
    }

    public void setContent(List<HH2SectionData> hh2SectionDataArrayList) {
        if (this.content == null) this.content = new ArrayList<>();
        this.content.clear();
        this.content.addAll(hh2SectionDataArrayList);
    }

    public ArrayList<HH2SectionData> getSearchResList() {
        if (this.searchResList == null) this.searchResList = new ArrayList<>();
        return searchResList;
    }

    public void setSearchResList(List<HH2SectionData> searchResList) {
        if (this.searchResList == null) this.searchResList = new ArrayList<>();
        //清空有搜索结果
        this.searchResList.clear();
        this.searchResList.addAll(searchResList);
    }

    public ArrayList<HH2SectionData> getFang() {
        if (this.fang == null) this.fang = new ArrayList<>();
        return this.fang;
    }

    public void setFang(HH2SectionData fang) {
        if (this.fang == null) this.fang = new ArrayList<>();
        this.fang.clear();
        this.fang.add(fang);

        // 初始化并填充 allFang 列表
        this.allFang = new ArrayList<>();
        for (HH2SectionData section : this.fang) {
            for (DataItem item : section.getData()) {
                this.allFang.add(item.getName());
                if (item.getFangList() != null && !item.getFangList().isEmpty()) {
                    // 如果有别名映射，则替换

                    for (String fangStr : item.getFangList()) {
                        String aliasedFangStr = this.fangAliasDict.get(fangStr);
                        if (aliasedFangStr == null) {
                            this.fangAliasDict.put(fangStr, item.getName());
                        }
                    }

                    this.allFang.addAll(item.getFangList());
                }
            }


        }
    }

    public int getBookId() {
        return bookId;
    }

    private SingletonNetData() {
        this.bookId = -1; // 默认值
    }

    private SingletonNetData(int bookId) {
        this.bookId = bookId;
    }

    public static SingletonNetData getInstance(int bookId) {
        if (instance == null || instance.bookId != bookId) {
            synchronized (SingletonNetData.class) {
                if (instance == null || instance.bookId != bookId) {
                    instance = new SingletonNetData(bookId);
                }
            }
        }
        return instance;
    }

    public static SingletonNetData getInstance() {
        return new SingletonNetData();
    }

    private OnContentUpdateListener mOnContentUpdateListener;

    public interface OnContentUpdateListener {
        ArrayList<HH2SectionData> contentDateUpdate(ArrayList<HH2SectionData> contentList);
    }

    public void setOnContentUpdateListener(OnContentUpdateListener mOnUpdateListener) {
        this.mOnContentUpdateListener = mOnUpdateListener;
    }


//    private OnContentGetHttpDataUpdateStatus mNotification;
//
//    public interface OnContentGetHttpDataUpdateStatus {
//        void onContentUpdateHttpDataStatus(boolean status);
//    }
//
//    public void setOnContentUpdateHttpDataNotification(OnContentGetHttpDataUpdateStatus mOnUpdateListener) {
//        this.mNotification = mOnUpdateListener;
//    }
//
//    public void showUpdateHttpDataNotification(boolean status) {
//        if (mNotification != null)
//            mNotification.onContentUpdateHttpDataStatus(status);
//
//    }


//    private OnContentShowStatusNotification mOnContentShowStatusNotification;
//
//    public interface OnContentShowStatusNotification {
//        void contentShowStatusNotification(int status);
//    }
//
//    public void setOnContentShowStatusNotification(OnContentShowStatusNotification mStatusNotification) {
//        this.mOnContentShowStatusNotification = mStatusNotification;
//    }
//
//    public void shanghanShowUpdateNotification() {
//        if (mOnContentShowStatusNotification != null)
//            mOnContentShowStatusNotification.contentShowStatusNotification(1);
//
//    }

}
