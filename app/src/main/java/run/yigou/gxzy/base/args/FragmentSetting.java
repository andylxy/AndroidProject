package run.yigou.gxzy.base.args;

import java.io.Serializable;

public class FragmentSetting implements Serializable {


    private boolean song_ShangHan = false;
    private boolean song_JinKui = false;
    private boolean shuJie = false;

    /**
     * ????????????
     * false ?????????398???true??????????
     * @return
     */
    public boolean isSong_ShangHan() {
        return song_ShangHan;
    }

    public void setSong_ShangHan(boolean song_ShangHan) {
        this.song_ShangHan = song_ShangHan;
    }

    /**
     *   ????????????
     *  false ???????????????true?????????
     * @return
     */
    public boolean isSong_JinKui() {
        return song_JinKui;
    }

    public void setSong_JinKui(boolean song_JinKui) {
        this.song_JinKui = song_JinKui;
    }

    /**
     *  ??????????????
     * @return
     */
    public boolean isShuJie() {
        return shuJie;
    }

    public void setShuJie(boolean shuJie) {
        this.shuJie = shuJie;
    }



}
