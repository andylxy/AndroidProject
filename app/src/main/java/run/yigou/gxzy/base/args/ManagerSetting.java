package run.yigou.gxzy.base.args;

import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.utils.CacheHelper;

public class ManagerSetting {

    public static void logout() {

    }

    /**
     * ??????
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
     * ??????
     * @param setting
     */
    public static void saveFragmentSetting(FragmentSetting setting) {
        CacheHelper.saveObject(setting, AppConst.FILE_NAME_SETTING);
    }


    /**
     * ??????
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
//        setting.setFont(Font.??????);
//        setting.setAutoScrollSpeed(50);
        return setting;
    }

}

