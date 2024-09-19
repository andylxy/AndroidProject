
package run.yigou.gxzy.ui.tips.tipsutils;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.ui.adapter.BookReadContenAdapter;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Yao;


public class Singleton_Net_Data {
    private static Singleton_Net_Data data;
    protected List<String> allFang;
    private ArrayList<HH2SectionData> content;
    private ArrayList<HH2SectionData> searchResList;
    private ArrayList<HH2SectionData> fang;

    public List<String> getAllFang() {
        return this.allFang;
    }

    private Map<String, String> yaoAliasDict;


    public Map<String, String> getYaoAliasDict() {
        return yaoAliasDict;
    }

    public void setYaoAliasDict(Map<String, String> yaoAliasDict) {
        if (yaoAliasDict != null)
            this.yaoAliasDict = yaoAliasDict;
        else this.yaoAliasDict = new HashMap<String, String>();

    }

    protected Map<String, String> fangAliasDict;

    public void setFangAliasDict(Map<String, String> fangAliasDict) {
        if (fangAliasDict != null)
            this.fangAliasDict = fangAliasDict;
        else this.fangAliasDict = new HashMap<String, String>();
    }

    public Map<String, String> getFangAliasDict() {
        return this.fangAliasDict;
    }


    public ArrayList<HH2SectionData> getContent() {
        if (this.content == null) this.content = new ArrayList<>();
//        //如果请求为显示伤寒398条,
//        if(bookId== AppConst.ShangHanNo && getShowShanghan() != AppConst.Show_Shanghan_AllSongBan){
//           return   new ArrayList<>(this.content.subList(8, 18)); // 创建新列表
//        }
        //如果实现数据监听处理接口.则由该接口处理后直接返回处理后的数据.
        if (mOnContentUpdateListener != null) {
            //如果实现通知接口,则通知数据已经更新
            mOnContentUpdateListener.contentDateUpdate(this.content);
            ArrayList<HH2SectionData> updateList = mOnContentUpdateListener.contentDateUpdate(this.content);
            //如果实现通知接口.则通知数据已经更新.
            //if (mOnUpdateStatusNotification !=null && !updateList.isEmpty()) mOnUpdateStatusNotification.contentUpdateStatus(true);
            if (updateList == null || updateList.isEmpty()) return new ArrayList<>();
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
        //  初始化并填充 allFang 列表
        this.allFang = new ArrayList<>();
        for (HH2SectionData section : this.fang) {
            for (DataItem item : section.getData()) {
                String str = item.getFangList().get(0);
                // 如果有别名映射，则替换
                String str2 = this.fangAliasDict.get(str);
                if (str2 != null) {
                    str = str2;
                }
                this.allFang.add(str);
            }
        }
    }


    private Singleton_Net_Data(int bookId) {
        this();
        this.bookId = bookId;

    }

    private Singleton_Net_Data() {
    }

    public int getBookId() {
        return bookId;
    }

    private int bookId;
    //todo 获取实例需优化
    public static Singleton_Net_Data getInstance(int bookId) {
        data = new Singleton_Net_Data(bookId);
        return data;
    }

    private OnContentUpdateListener mOnContentUpdateListener;

    public interface OnContentUpdateListener {
        ArrayList<HH2SectionData> contentDateUpdate(ArrayList<HH2SectionData> contentList);
    }

    public void setOnContentUpdateListener(OnContentUpdateListener mOnUpdateListener) {
        this.mOnContentUpdateListener = mOnUpdateListener;
    }

    private OnContentShowStatusNotification mOnContentShowStatusNotification;

    public interface OnContentShowStatusNotification {
        void contentShowStatusNotification(int status);
    }

    public void setOnContentShowStatusNotification(OnContentShowStatusNotification mStatusNotification) {
        this.mOnContentShowStatusNotification = mStatusNotification;
    }

    public  void shanghanShowUpdateNotification() {
        if (mOnContentShowStatusNotification != null)
            mOnContentShowStatusNotification.contentShowStatusNotification(1);

    }

//    private OnReceiveStatusNotification mOnReceiveStatusNotification;
//
//    public interface OnReceiveStatusNotification {
//        void receiveStatusNotification(boolean status);
//    }
//    public void setOnReceiveStatusNotification(OnReceiveStatusNotification mReceiveStatus) {
//        this.mOnReceiveStatusNotification = mReceiveStatus;
//    }

}
