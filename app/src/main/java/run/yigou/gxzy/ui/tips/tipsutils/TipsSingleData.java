/*
 * 项目名: AndroidProject
 * 类名: Tips_Single_Data.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月18日 08:08:21
 * 上次修改时间: 2024年09月18日 08:08:21
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

import android.content.Context;
import android.content.SharedPreferences;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.widget.TipsLittleWindow;


public class TipsSingleData {
    // private static Tips_Single_Data tips_Single_Data;

    private static volatile TipsSingleData instance;


    private  Set<String> allYao;
    private int curBookId;
    public List<TipsLittleWindow> tipsLittleWindowStack = new ArrayList();

    public Set<String> getAllYao() {
        return this.allYao;
    }

    private Map<Integer, TabNav> NavTabMap;

    public Map<Integer, TabNav> getNavTabMap() {
        if (NavTabMap == null) NavTabMap = new HashMap<>();
        return NavTabMap;
    }

    public Map<Integer, TabNavBody> getNavTabBodyMap() {
        if (NavTabBodyMap == null) NavTabBodyMap = new HashMap<>();
        return NavTabBodyMap;
    }

    private Map<Integer, SingletonNetData> bookContentMap;

    public Map<Integer, SingletonNetData> getMapBookContent() {
        return bookContentMap;
    }

    private Map<Integer, TabNavBody> NavTabBodyMap;

    /**
     * 获取当前打开书本ID
     *
     * @return 返回ID
     */
    public int getCurBookId() {
        return curBookId;
    }

    /**
     * 记录正在打开的书本的Id
     *
     * @param curBookId 需保存的Id
     */
    public void setCurBookId(int curBookId) {
        this.curBookId = curBookId;
    }


//    public SharedPreferences getSharedPreferences() {
//        return AppApplication.application.getSharedPreferences(AppConst.preferenceKey, Context.MODE_PRIVATE);
//    }

    /**
     * 药物数据
     */
    private HH2SectionData mingCiData;
    private Map<String, MingCiContent> mingCiContentMap;

    private Map<String, Yao> yaoMap;


    public Map<String, MingCiContent> getMingCiContentMap() {
        return mingCiContentMap;
    }

    public Map<String, Yao> getYaoMap() {
        return yaoMap;
    }

    public void setMingCiData(HH2SectionData mingCiData) {
        if (mingCiData == null) return;
        this.mingCiData = mingCiData;
        if (this.mingCiContentMap == null) this.mingCiContentMap = new HashMap<>();
        for (DataItem item : this.mingCiData.getData()) {
//                String name = ((MingCiContent)item).getName();
//                MingCiContent mingCiContent = ((MingCiContent)item);
            mingCiContentMap.put(((MingCiContent) item).getName(), (MingCiContent) item);
        }
    }

    public void setYaoData(HH2SectionData yaoData) {

        // 初始化并填充 allYao 集合
        if (this.allYao == null) {
            this.allYao = new CopyOnWriteArraySet<>();
        } else {
            this.allYao.clear();
        }

        if (this.yaoMap == null) {
            this.yaoMap = new ConcurrentHashMap<>();
        } else {
            this.yaoMap.clear();
        }

        if (yaoData.getData() == null) {
            return;
        }

        for (DataItem item : yaoData.getData()) {
//            List<String> yaoList =item.getYaoList() ;
//            if (yaoList == null || yaoList.isEmpty()) {
//                continue;
//            }

            // 如果有别名映射，则替换
            for (String aliaName : item.getYaoList()) {
                String alia = this.getYaoAliasDict().get(aliaName);
                if (alia != null) {
                        EasyLog.print(alia);
                        yaoMap.put(alia, (Yao) item);
                        this.allYao.add(alia);

                } else {
                        EasyLog.print(aliaName);
                        yaoMap.put(aliaName, (Yao) item);
                        this.allYao.add(aliaName);

                }
            }


                yaoMap.put(item.getName(), (Yao) item);
                this.allYao.add(item.getName());

        }
    }


    private SingletonNetData curSingletonData;

    /**
     * 当前书籍数据
     *
     * @return
     */
    public SingletonNetData getCurSingletonData() {
        return curSingletonData;
    }

    public void setCurSingletonData(SingletonNetData curSingletonData) {
        this.curSingletonData = curSingletonData;
    }

    private Map<String, String> fangAliasDict;
    private Map<String, String> yaoAliasDict;

    public Map<String, String> getFangAliasDict() {
        if (fangAliasDict == null) fangAliasDict = new HashMap<>();
        return fangAliasDict;
    }

    public Map<String, String> getYaoAliasDict() {
        if (yaoAliasDict == null) yaoAliasDict = new HashMap<>();
        return yaoAliasDict;
    }

    public SingletonNetData getMapBookContent(int bookId) {

        SingletonNetData singletonNetData = bookContentMap.get(bookId);
        if (singletonNetData == null) {
            singletonNetData = SingletonNetData.getInstance(bookId);
            bookContentMap.put(bookId, singletonNetData);
        }
        setCurBookId(bookId);
        setCurSingletonData(singletonNetData);
        return singletonNetData;
    }

    private void initAlias() {
        this.yaoAliasDict = new HashMap<String, String>() {
            {
//                put("术", "白术");
//                put("朮", "白术");
//                put("白朮", "白术");
//                put("桂", "桂枝");
//                put("桂心", "桂枝");
//                put("肉桂", "桂枝");
//                put("白芍药", "芍药");
//                put("枣", "大枣");
//                put("枣膏", "大枣");
//                put("枣肉", "大枣");
//                put("生姜汁", "生姜");
//                put("姜", "生姜");
//                put("生葛", "葛根");
//                put("生地黄", "地黄");
//                put("干地黄", "地黄");
//                put("生地", "地黄");
//                put("熟地", "地黄");
//                put("生地黄汁", "地黄");
//                put("地黄汁", "地黄");
//                put("甘遂末", "甘遂");
//                put("茵陈蒿末", "茵陈蒿");
//                put("大附子", "附子");
//                put("川乌", "乌头");
//                put("粉", "白粉");
//                put("白蜜", "蜜");
//                put("食蜜", "蜜");
//                put("杏子", "杏仁");
//                put("葶苈", "葶苈子");
//                put("香豉", "豉");
//                put("肥栀子", "栀子");
//                put("生狼牙", "狼牙");
//                put("清酒", "酒");
//                put("白酒", "酒");
//                put("艾叶", "艾");
//                put("乌扇", "射干");
//                put("代赭石", "赭石");
//                put("代赭", "赭石");
//                put("煅灶下灰", "煅灶灰");
//                put("干苏叶", "苏叶");
//                put("蛇床子仁", "蛇床子");
//                put("牡丹皮", "牡丹");
//                put("小麦汁", "小麦");
//                put("小麦粥", "小麦");
//                put("麦粥", "小麦");
//                put("大麦粥", "大麦");
//                put("大麦粥汁", "大麦");
//


                put("葱白", "葱");
                put("赤硝", "赤消");
                put("硝石", "赤消");
                put("消石", "赤消");
                put("芒消", "芒硝");

                put("法醋", "苦酒");
                put("大猪胆", "猪胆汁");
                put("大猪胆汁", "猪胆汁");

                put("鸡子白", "鸡子");
                put("太一禹余粮", "禹余粮");
                put("妇人中裈近隐处取烧作灰", "中裈灰");
                put("石苇", "石韦");
                put("灶心黄土", "灶中黄土");
                put("瓜子", "瓜瓣");
                put("括蒌根", "栝楼根");
                put("瓜蒌根", "栝楼根");
                put("括蒌实", "栝楼实");
                put("瓜蒌实", "栝楼实");
                put("栝楼", "栝楼实");
                put("浆水", "清浆水");
                put("川椒", "蜀椒");
                put("生竹茹", "竹茹");
                put("柏皮", "黄柏");
                put("猪胆", "猪胆汁");
            }
        };
        this.fangAliasDict = new HashMap<String, String>() {
            {
                put("人参汤", "理中汤");
                put("芪芍桂酒汤", "黄芪芍药桂枝苦酒汤");
                put("膏发煎", "猪膏发煎");
                put("小柴胡", "小柴胡汤");
            }
        };
    }


    private TipsSingleData() {
        bookContentMap = new HashMap<Integer, SingletonNetData>();
        //initAlias();

    }

    /**
     * 获取  Tips_Single_Data 单例对象
     *
     * @return 单例对象
     */
    public static TipsSingleData getInstance() {
        if (instance == null) {
            synchronized (TipsSingleData.class) {
                if (instance == null) {
                    instance = new TipsSingleData();
                }
            }
        }
        return instance;
    }


    public void onDestroy() {
        instance = null;
        curSingletonData = null;
        tipsLittleWindowStack.clear();
        tipsLittleWindowStack = null;
    }
}
