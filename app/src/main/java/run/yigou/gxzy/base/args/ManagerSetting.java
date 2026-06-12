package run.yigou.gxzy.base.args;

import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.utils.CacheHelper;

public class ManagerSetting {

    public static void logout() {

    }

    /**
     * 获取片段设置
     * 从缓存中读取，如不存在则返回默认设置
     * @return FragmentSetting 片段设置对象
     */
    public static FragmentSetting getFragmentSetting() {
        FragmentSetting setting = (FragmentSetting) CacheHelper.readObject(AppConst.FILE_NAME_SETTING);
        if (setting == null){
            setting = getFragmentDefaultSetting();
            saveFragmentSetting(setting);
        }
        return setting;
    }

    /**
     * 保存片段设置到缓存
     * @param setting 片段设置对象
     */
    public static void saveFragmentSetting(FragmentSetting setting) {
        CacheHelper.saveObject(setting, AppConst.FILE_NAME_SETTING);
    }


    /**
     * 获取默认片段设置
     * @return 默认的 FragmentSetting 对象
     */
    private static FragmentSetting getFragmentDefaultSetting(){
        FragmentSetting setting = new FragmentSetting();
        setting.setSong_JinKui(true);
        setting.setSong_ShangHan(false);
        setting.setShuJie(false);
//        setting.setReadWordSize(20);
//        setting.setReadWordColor(R.color.sys_protect_eye_word);
//        setting.setBrightProgress(50);
//        setting.setBrightFollowSystem(true);
//        setting.setLanguage(Language.simplified);
//        setting.setFont(Font.??????);
//        setting.setAutoScrollSpeed(50);
        return setting;
    }

}

