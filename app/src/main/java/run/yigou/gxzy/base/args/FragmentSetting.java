package run.yigou.gxzy.base.args;

import java.io.Serializable;

public class FragmentSetting implements Serializable {

    // 步骤 0：固化 serialVersionUID（ADR-0002）。该类原无显式 UID，JVM 按字段名算默认 UID；
    // 删除 song_ShangHan / song_JinKui 两字段会改变 UID，导致旧缓存反序列化失败被 CacheHelper 清除、
    // 用户已保存的 shuJie 设置被静默重置。此处锁定「三字段版本」的 UID，删除字段后仍兼容旧缓存
    // （缺失字段取默认值 false），shuJie 零丢失。
    private static final long serialVersionUID = 7396263222773750605L;

    private boolean shuJie = false;

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
