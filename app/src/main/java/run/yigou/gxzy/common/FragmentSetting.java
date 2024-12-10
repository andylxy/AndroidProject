package run.yigou.gxzy.common;

import java.io.Serializable;

public class FragmentSetting implements Serializable {


    private boolean song_ShangHan = false;
    private boolean song_JinKui = false;
    private boolean shuJie = false;

    /**
     * 宋版伤寒金匮显示
     * false 默认显示伤害398条，true显示所有伤寒
     * @return
     */
    public boolean isSong_ShangHan() {
        return song_ShangHan;
    }

    public void setSong_ShangHan(boolean song_ShangHan) {
        this.song_ShangHan = song_ShangHan;
    }

    /**
     *   宋版伤寒金匮显示
     *  false 默认不显示金匮要略，true显示金匮要略
     * @return
     */
    public boolean isSong_JinKui() {
        return song_JinKui;
    }

    public void setSong_JinKui(boolean song_JinKui) {
        this.song_JinKui = song_JinKui;
    }

    /**
     *  阅读后是否加入书架
     * @return
     */
    public boolean isShuJie() {
        return shuJie;
    }

    public void setShuJie(boolean shuJie) {
        this.shuJie = shuJie;
    }



}
