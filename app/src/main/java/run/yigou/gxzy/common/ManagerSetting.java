package run.yigou.gxzy.common;

import run.yigou.gxzy.utils.CacheHelper;

public class ManagerSetting {

    public static void logout() {

    }

    /**
     * 获取设置
     * @return
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
     * 保存设置
     * @param setting
     */
    public static void saveFragmentSetting(FragmentSetting setting) {
        CacheHelper.saveObject(setting, AppConst.FILE_NAME_SETTING);
    }


    /**
     * 默认设置
     * @return
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
//        setting.setFont(Font.默认字体);
//        setting.setAutoScrollSpeed(50);
        return setting;
    }

}

