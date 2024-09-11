package run.yigou.gxzy.tipsutils;


import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.tipsutils.DataBeans.Fang;
import run.yigou.gxzy.tipsutils.DataBeans.Yao;

/* loaded from: classes.dex */
public class SingletonData {
    private static SingletonData data;
    protected List<String> allFang;
    protected List<String> allYao;
    private ArrayList<HH2SectionData> content;
    public Activity curActivity;
    //public ShowFragment curFragment;
   // public TipsWindow curTipsWindow;
    private ArrayList<HH2SectionData> fang;
    protected Map<String, String> fangAliasDict;
    private View mask;
    protected int showJinkui;
    protected int showShanghan;
    private String[] yao;
    protected Map<String, String> yaoAliasDict;
    private ArrayList<HH2SectionData> yaoData;
    public final int Show_Shanghan_None = 0;
    public final int Show_Shanghan_398 = 1;
    public final int Show_Shanghan_AllSongBan = 2;
    public final int Show_Jinkui_None = 0;
    public final int Show_Jinkui_Default = 1;
    private final String preferenceKey = "shanghan3.1";
    //public List<LittleWindow> littleWindowStack = new ArrayList();
    public Gson gson = new Gson();
    public boolean isSeeingContextInSearchMode = false;
    //private ArrayList<ShowFang> showFangList = new ArrayList<>();

    public List<String> getAllYao() {
        return this.allYao;
    }

    public List<String> getAllFang() {
        return this.allFang;
    }

    public Map<String, String> getYaoAliasDict() {
        return this.yaoAliasDict;
    }

    public Map<String, String> getFangAliasDict() {
        return this.fangAliasDict;
    }

    public View getMask() {
        return this.mask;
    }

    public void setMask(View view) {
        this.mask = view;
    }

//    public boolean hasShowFang() {
//        return this.showFangList.size() == 1;
//    }
//
//    public int getShowFangNum() {
//        return this.showFangList.size();
//    }

//    public ShowFang getShowFang() {
//        return this.showFangList.get(this.showFangList.size() - 1);
//    }
//
//    public void pushShowFang(ShowFang showFang) {
//        this.showFangList.add(showFang);
//    }

//    public void popShowFang() {
//        if (this.showFangList.size() > 0) {
//            this.showFangList.remove(this.showFangList.size() - 1);
//        }
//    }

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
        return this.content;
    }

    public ArrayList<HH2SectionData> getFang() {
        return this.fang;
    }

    public String[] getYao() {
        return this.yao;
    }

    public ArrayList<HH2SectionData> getYaoData() {
        return this.yaoData;
    }

    private void initAlias() {
        this.yaoAliasDict = new HashMap<String, String>() { // from class: me.huanghai.searchController.SingletonData.1
            {
                put("术", "白术");
                put("朮", "白术");
                put("白朮", "白术");
                put("桂", "桂枝");
                put("桂心", "桂枝");
                put("肉桂", "桂枝");
                put("白芍药", "芍药");
                put("枣", "大枣");
                put("枣膏", "大枣");
                put("枣肉", "大枣");
                put("生姜汁", "生姜");
                put("姜", "生姜");
                put("生葛", "葛根");
                put("生地黄", "地黄");
                put("干地黄", "地黄");
                put("生地", "地黄");
                put("熟地", "地黄");
                put("生地黄汁", "地黄");
                put("地黄汁", "地黄");
                put("甘遂末", "甘遂");
                put("茵陈蒿末", "茵陈蒿");
                put("大附子", "附子");
                put("川乌", "乌头");
                put("粉", "白粉");
                put("白蜜", "蜜");
                put("食蜜", "蜜");
                put("杏子", "杏仁");
                put("葶苈", "葶苈子");
                put("香豉", "豉");
                put("肥栀子", "栀子");
                put("生狼牙", "狼牙");
                put("干苏叶", "苏叶");
                put("清酒", "酒");
                put("白酒", "酒");
                put("艾叶", "艾");
                put("乌扇", "射干");
                put("代赭石", "赭石");
                put("代赭", "赭石");
                put("煅灶下灰", "煅灶灰");
                put("干苏叶", "苏叶");
                put("蛇床子仁", "蛇床子");
                put("牡丹皮", "牡丹");
                put("小麦汁", "小麦");
                put("小麦粥", "小麦");
                put("麦粥", "小麦");
                put("大麦粥", "大麦");
                put("大麦粥汁", "大麦");
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
            }
        };
        this.fangAliasDict = new HashMap<String, String>() { // from class: me.huanghai.searchController.SingletonData.2
            {
                put("人参汤", "理中汤");
                put("芪芍桂酒汤", "黄芪芍药桂枝苦酒汤");
                put("膏发煎", "猪膏发煎");
                put("小柴胡", "小柴胡汤");
            }
        };
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

    private SingletonData() {
        this.showShanghan = 1;
        this.showJinkui = 1;
        SharedPreferences sharedPreferences =AppApplication.application.getSharedPreferences("shanghan3.1", 0);
        this.showShanghan = sharedPreferences.getInt("showShanghan", 1);
        this.showJinkui = sharedPreferences.getInt("showJinkui", 1);
        initAlias();
        reReadData();
        this.allFang = new ArrayList();
        Iterator<HH2SectionData> it = this.fang.iterator();
        while (it.hasNext()) {
            List<? extends DataItem> data2 = it.next().getData();
            Log.e("-d->", data2.get(0).toString());
            Iterator<? extends DataItem> it2 = data2.iterator();
            while (it2.hasNext()) {
                String str = it2.next().getFangList().get(0);
                String str2 = this.fangAliasDict.get(str);
                if (str2 != null) {
                    str = str2;
                }
                this.allFang.add(str);
            }
        }
        String readFile = FucUtil.readFile(AppApplication.application, "yao.json");
        this.yaoData = new ArrayList<>();
        this.yaoData.add(new HH2SectionData((List) this.gson.fromJson(readFile, new TypeToken<List<Yao>>() { // from class: me.huanghai.searchController.SingletonData.3
        }.getType()), 0, "伤寒金匮所有药物"));
        this.allYao = new ArrayList();
        Iterator<HH2SectionData> it3 = this.yaoData.iterator();
        while (it3.hasNext()) {
            Iterator<? extends DataItem> it4 = it3.next().getData().iterator();
            while (it4.hasNext()) {
                String str3 = it4.next().getYaoList().get(0);
                String str4 = this.yaoAliasDict.get(str3);
                if (str4 != null) {
                    str3 = str4;
                }
                this.allYao.add(str3);
            }
        }
    }

    public void reReadData() {
        reReadContent();
        reReadFang();
    }

//    public void reReadData(final Activity activity) {
//        final ProgressBar showProgressBar = Helper.showProgressBar(activity);
//        new Thread(new Runnable() { // from class: me.huanghai.searchController.SingletonData.4
//            @Override // java.lang.Runnable
//            public void run() {
//                SingletonData.this.reReadData();
//                activity.runOnUiThread(new Runnable() { // from class: me.huanghai.searchController.SingletonData.4.1
//                    @Override // java.lang.Runnable
//                    public void run() {
//                        Helper.removeFormWindow(activity, showProgressBar);
//                        for (Fragment fragment : TabController.fragments) {
//                            if (fragment instanceof MainFragment) {
//                                ((MainFragment) fragment).resetData(SingletonData.getInstance().getContent());
//                            } else if (fragment instanceof FangFragment) {
//                                ((FangFragment) fragment).resetData(SingletonData.getInstance().getFang());
//                            }
//                        }
//                    }
//                });
//            }
//        }).start();
//    }

    public void reReadContent() {
        this.content = null;
        String readFile = FucUtil.readFile(AppApplication.application, "shangHan_data.json");
        this.content = new ArrayList<>();
        if (this.showShanghan != 0) {
            List list = (List) this.gson.fromJson(readFile, new TypeToken<List<HH2SectionData>>() { // from class: me.huanghai.searchController.SingletonData.5
            }.getType());
            if (this.showShanghan == 1) {
                list = list.subList(8, 18);
            }
            this.content.addAll(list);
        }
        if (this.showJinkui != 0) {
            this.content.addAll((List) this.gson.fromJson(FucUtil.readFile(AppApplication.application, "jinKui_data.json"), new TypeToken<List<HH2SectionData>>() { // from class: me.huanghai.searchController.SingletonData.6
            }.getType()));
        }
        Iterator<HH2SectionData> it = this.content.iterator();
        while (it.hasNext()) {
            HH2SectionData next = it.next();
            List<? extends DataItem> data2 = next.getData();
            for (int i = 0; i < data2.size(); i++) {
                data2.get(i).setIndexPath(NSIndexPath.indexPathForRowInSection(i, next.getSection()));
            }
        }
    }

    public void reReadFang() {
        this.fang = new ArrayList<>();
        this.fang.add(new HH2SectionData((List) this.gson.fromJson(FucUtil.readFile(AppApplication.application, "shangHan_fang.json"), new TypeToken<List<Fang>>() { // from class: me.huanghai.searchController.SingletonData.7
        }.getType()), 0, "伤寒论方"));
        if (this.showJinkui != 0) {
            this.fang.add(new HH2SectionData((List) this.gson.fromJson(FucUtil.readFile(AppApplication.application, "jinKui_fang.json"), new TypeToken<List<Fang>>() { // from class: me.huanghai.searchController.SingletonData.8
            }.getType()), 1, "金匮要略方"));
        }
        for (int i = 0; i < this.content.size(); i++) {
            List<? extends DataItem> data2 = this.content.get(i).getData();
            for (int i2 = 0; i2 < data2.size(); i2++) {
                data2.get(i2).setIndexPath(NSIndexPath.indexPathForRowInSection(i2, i));
            }
        }
    }

    public static SingletonData getInstance() {
        if (data == null) {
            data = new SingletonData();
        }
        return data;
    }
}
