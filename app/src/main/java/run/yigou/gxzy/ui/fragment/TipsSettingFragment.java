package run.yigou.gxzy.ui.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.hjq.widget.layout.SettingBar;
import com.hjq.widget.view.SwitchButton;

import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.ui.tips.tipsutils.Singleton_Net_Data;
import run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data;


public final class TipsSettingFragment extends AppFragment<AppActivity> implements SwitchButton.OnCheckedChangeListener {

    private SettingBar sb_setting_sh;
    private SettingBar sb_setting_jk;
    private SwitchButton sb_setting_sh_switch;
    private SwitchButton sb_setting_jk_switch;
    private Singleton_Net_Data singletonNetData;
    private int bookId = 0;
    //宋版伤寒,金匮显示设置
    private int showJinkui= AppConst.Show_Jinkui_Default;
    private int showShanghan= AppConst.Show_Shanghan_AllSongBan;
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
    public static TipsSettingFragment newInstance() {
        return new TipsSettingFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tips_setting_fragment;
    }

    @Override
    protected void initView() {
        sb_setting_sh = findViewById(R.id.sb_setting_sh);
        sb_setting_jk = findViewById(R.id.sb_setting_jk);
        sb_setting_sh_switch = findViewById(R.id.sb_setting_sh_switch);
        sb_setting_jk_switch = findViewById(R.id.sb_setting_jk_switch);
        // 注册事件
        //XEventBus.getDefault().register(this);
    }

    @Override
    protected void initData() {
        // 获取传递的书本编号
        Bundle args = getArguments();
        if (args != null) {
            bookId = args.getInt("bookNo", 0);
        }
        singletonNetData = Tips_Single_Data.getInstance().getMapBookContent(bookId);
        // 设置切换按钮的监听
        sb_setting_sh_switch.setOnCheckedChangeListener(this);
        sb_setting_jk_switch.setOnCheckedChangeListener(this);
        showSettingSwitch();
    }

    private void showSettingSwitch() {
        // 默认初始化设置
        // 从 SharedPreferences 中读取设置值
        SharedPreferences sharedPreferences = Tips_Single_Data.getInstance().getSharedPreferences();
        setShowShanghan(sharedPreferences.getInt(AppConst.Key_Shanghan, 0));
        setShowJinkui(sharedPreferences.getInt(AppConst.Key_Jinkui, 1));

        if (getShowShanghan() == AppConst.Show_Shanghan_AllSongBan) {
            sb_setting_sh_switch.setChecked(true);
            sb_setting_sh.setLeftText("完整显示伤寒论・宋板");
        } else {
            sb_setting_sh_switch.setChecked(false);
            sb_setting_sh.setLeftText("显示398条辨伤寒论・宋板");
        }
        if (getShowJinkui() ==AppConst.Show_Jinkui_Default) {
            sb_setting_jk_switch.setChecked(true);
            sb_setting_jk.setLeftText("默认显示金匮要略・宋板");
        } else {
            sb_setting_jk_switch.setChecked(false);
            sb_setting_jk.setLeftText("不显示金匮要略・宋板");
        }
    }
    // 获取 SharedPreferences 编辑器
    SharedPreferences.Editor editor = Tips_Single_Data.getInstance().getSharedPreferences().edit();

    public void savePreferences() {

        // 保存显示选项
        editor.putInt("showShanghan", getShowShanghan()); // 商汉显示选项
        editor.putInt("showJinkui", getShowJinkui()); // 金匮显示选项

        // 提交更改
        editor.apply(); // 异步保存更改
    }

    @Override
    public void onResume() {
        super.onResume();
        // 处理 Fragment 重新激活时的逻辑
        // 例如：更新 UI、重新加载数据等
        showSettingSwitch();
    }

    /**
     * {@link SwitchButton.OnCheckedChangeListener}
     */

    @Override
    public void onCheckedChanged(SwitchButton button, boolean checked) {
        if (button.getId() == R.id.sb_setting_sh_switch) {
            if (checked) {
                sb_setting_sh.setLeftText("显示完整宋板伤寒论");
                setShowShanghan(AppConst.Show_Shanghan_AllSongBan);
            } else {
                sb_setting_sh.setLeftText("只显示398条辨");
                setShowShanghan(AppConst.Show_Shanghan_398);
            }
        }
        if (button.getId() == R.id.sb_setting_jk_switch) {
            if (checked) {
                sb_setting_jk.setLeftText("显示默认版金匮要略");
                setShowJinkui(AppConst.Show_Jinkui_Default);
            } else {
                sb_setting_jk.setLeftText("不显示金匮要略");
                setShowJinkui(AppConst.Show_Jinkui_None);
            }
        }
        //保存设置
        savePreferences();
        //通知显示已经变更
       singletonNetData.shanghanShowUpdateNotification();
       // XEventBus.getDefault().post(new ShowUpdateNotificationEvent().setUpdateNotification(true));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销事件
        //XEventBus.getDefault().unregister(this);
        editor.clear(); // 清除所有数据
        editor.apply();
    }
}