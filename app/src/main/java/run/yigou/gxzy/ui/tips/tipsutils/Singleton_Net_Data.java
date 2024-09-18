
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
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Yao;


public class Singleton_Net_Data {
    private static Singleton_Net_Data data;
    protected List<String> allFang;
    private ArrayList<HH2SectionData> content;
    private ArrayList<HH2SectionData> searchResList;
    private ArrayList<HH2SectionData> fang;

    protected int showJinkui;
    protected int showShanghan;
    private final String preferenceKey = "shanghan3.1";
    public List<String> getAllFang() {
        return this.allFang;
    }

    private Map<String, String> yaoAliasDict;
    public Map<String, String> getYaoAliasDict() {
        return yaoAliasDict;
    }
    public void setYaoAliasDict(Map<String, String> yaoAliasDict) {
        if (yaoAliasDict !=null)
            this.yaoAliasDict = yaoAliasDict;
        else  this.yaoAliasDict = new HashMap<String, String>() ;

    }
    protected Map<String, String> fangAliasDict;

    public void setFangAliasDict(Map<String, String> fangAliasDict) {
        if (fangAliasDict !=null)
            this.fangAliasDict = fangAliasDict;
        else  this.fangAliasDict = new HashMap<String, String>() ;
    }
    public Map<String, String> getFangAliasDict() {
        return this.fangAliasDict;
    }


    public int getShowShanghan() {
        return this.showShanghan;
    }

    public void setShowShanghan(int i) {
        this.showShanghan = i;
    }

    public int getShowJinkui() {
        return this.showJinkui;
    }

    public void setShowJinkui(int i) {
        this.showJinkui = i;
    }

    public ArrayList<HH2SectionData> getContent() {
        if (this.content==null) this.content = new ArrayList<>();
        return this.content;
    }
    public void setContent(List<HH2SectionData> hh2SectionDataArrayList) {
          if (this.content==null) this.content = new ArrayList<>();
          this.content.addAll(hh2SectionDataArrayList);
    }
    public ArrayList<HH2SectionData> getSearchResList() {
        if (this.searchResList ==null) this.searchResList = new ArrayList<>();
        return searchResList;
    }

    public void setSearchResList(List<HH2SectionData> searchResList) {
        if (this.searchResList ==null) this.searchResList = new ArrayList<>();
        //清空有搜索结果
        this.searchResList.clear();
        this.searchResList.addAll(searchResList) ;
    }
    public ArrayList<HH2SectionData> getFang() {
        if (this.fang ==null) this.fang = new ArrayList<>();
        return this.fang;
    }

    public void setFang(HH2SectionData fang) {
        if (this.fang ==null) this.fang = new ArrayList<>();
        this.fang .add(fang);
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

    public void savePreferences() {
        SharedPreferences.Editor edit = AppApplication.application.getSharedPreferences("shanghan3.1", 0).edit();
        edit.putInt("showShanghan", this.showShanghan);
        edit.putInt("showJinkui", this.showJinkui);
        edit.commit();
    }

    public int getFontScaleProgress() {
        return AppApplication.application.getSharedPreferences("shanghan3.1", 0).getInt("fontScale", 0);
    }

    public void setFontScaleProgress(int i) {
        SharedPreferences.Editor edit = AppApplication.application.getSharedPreferences("shanghan3.1", 0).edit();
        edit.putInt("fontScale", i);
        edit.commit();
    }

    public float getFontScale() {
        double fontScaleProgress = getFontScaleProgress() * 3;
        Double.isNaN(fontScaleProgress);
        return (float) ((fontScaleProgress / 100.0d) + 1.0d);
    }

    public Singleton_Net_Data(int bookId) {
        this();
        this.bookId = bookId;
    }

    private Singleton_Net_Data() {
        // 默认初始化设置
        this.showShanghan = 1;
        this.showJinkui = 1;
        // 从 SharedPreferences 中读取设置值
        SharedPreferences sharedPreferences = AppApplication.application.getSharedPreferences("shanghan3.1", Context.MODE_PRIVATE);
        this.showShanghan = sharedPreferences.getInt("showShanghan", 1);
        this.showJinkui = sharedPreferences.getInt("showJinkui", 1);
    }

    public int getBookId() {
        return bookId;
    }

    private  int bookId;
    public static Singleton_Net_Data getInstance(int bookId) {
            data = new Singleton_Net_Data(bookId);
        return data;
    }
}
