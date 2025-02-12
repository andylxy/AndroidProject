package run.yigou.gxzy.ui.fragment;

import android.content.SharedPreferences;
import android.view.View;

import com.hjq.widget.layout.SettingBar;
import com.hjq.widget.view.SwitchButton;
import com.lucas.xbus.XEventBus;

import run.yigou.gxzy.EventBus.TipsFragmentSettingEventNotification;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.common.BookArgs;
import run.yigou.gxzy.common.FragmentSetting;
import run.yigou.gxzy.common.ManagerSetting;
import run.yigou.gxzy.ui.activity.YaoUintActivity;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;


public final class TipsSettingFragment extends AppFragment<AppActivity> implements SwitchButton.OnCheckedChangeListener {

    private SettingBar sb_setting_sh;
    private SettingBar sb_setting_jk;
    private SettingBar sb_setting_yao_uint;
    private SwitchButton sb_setting_sh_switch;
    private SwitchButton sb_setting_jk_switch;
   // private SingletonNetData singletonNetData;

    private SettingBar sb_setting_shu_jie;
    private SwitchButton sb_setting_shu_jie_switch;

    private int bookId = 0;


    private FragmentSetting fragmentSetting;




    private BookArgs bookArgs;
    // 单例模式，确保实例的唯一性
    private static volatile TipsSettingFragment instance;

    // 私有构造函数，防止外部直接实例化
    private TipsSettingFragment() {
        try {
            // 构造函数中的初始化逻辑
            // 可以在这里添加一些基本的校验逻辑
        } catch (Exception e) {
            // 异常处理
            throw new RuntimeException("Failed to create TipsSettingFragment instance", e);
        }
    }

    public static synchronized TipsSettingFragment newInstance(BookArgs bookArgs) {
        if (instance == null) {
            instance = new TipsSettingFragment();
        }
        if (bookArgs != null)
            instance.bookArgs = bookArgs;
        return instance;
    }


    @Override
    protected int getLayoutId() {
        return R.layout.tips_setting_fragment;
    }

    @Override
    protected void initView() {
        sb_setting_yao_uint = findViewById(R.id.sb_setting_yao_uint);
        sb_setting_sh = findViewById(R.id.sb_setting_sh);
        sb_setting_jk = findViewById(R.id.sb_setting_jk);
        sb_setting_sh_switch = findViewById(R.id.sb_setting_sh_switch);
        sb_setting_jk_switch = findViewById(R.id.sb_setting_jk_switch);
        sb_setting_shu_jie = findViewById(R.id.sb_setting_shu_jie);
        sb_setting_shu_jie_switch = findViewById(R.id.sb_setting_shu_jie_switch);

        // 注册事件
        XEventBus.getDefault().register(TipsSettingFragment.this);
    }

    @Override
    protected void initData() {
        // 获取传递的书本编号

        if (bookArgs != null) {
            bookId = bookArgs.getBookNo();
        }

        //singletonNetData = TipsSingleData.getInstance().getMapBookContent(bookId);
        sb_setting_shu_jie.setLeftText("打开阅读后加入书架");
        // 设置切换按钮的监听
        sb_setting_shu_jie_switch.setOnCheckedChangeListener(this);
        sb_setting_sh_switch.setOnCheckedChangeListener(this);
        sb_setting_jk_switch.setOnCheckedChangeListener(this);
        sb_setting_yao_uint.setOnClickListener(this);
        showSettingSwitch();
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        switch (view.getId()) {
            case R.id.sb_setting_yao_uint:
                // 跳转到药单位设置页面
                startActivityForResult(YaoUintActivity.class, null);
                break;
            default:
                break;
        }
    }

    private void showSettingSwitch() {
        // 默认初始化设置

        fragmentSetting = AppApplication.getApplication().fragmentSetting;

        if (bookId != AppConst.ShangHanNo) {
            sb_setting_sh.setVisibility(View.GONE);
            sb_setting_jk.setVisibility(View.GONE);
        }else{
            sb_setting_sh.setVisibility(View.VISIBLE);
            sb_setting_jk.setVisibility(View.VISIBLE);
        }
        sb_setting_shu_jie_switch.setChecked(fragmentSetting.isShuJie());
        sb_setting_sh_switch.setChecked(fragmentSetting.isSong_ShangHan());
        sb_setting_jk_switch.setChecked(fragmentSetting.isSong_JinKui());
        if (fragmentSetting.isSong_ShangHan()) {
            sb_setting_sh.setLeftText("完整显示伤寒论・宋板");
        } else {
            sb_setting_sh.setLeftText("显示398条辨伤寒论・宋板");
        }
        if (fragmentSetting.isSong_JinKui()) {
            sb_setting_jk.setLeftText("默认显示金匮要略・宋板");
        } else {
            sb_setting_jk.setLeftText("不显示金匮要略・宋板");
        }
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
        TipsFragmentSettingEventNotification tipsFragmentSettingEventNotification = new TipsFragmentSettingEventNotification();
        if (button.getId() == R.id.sb_setting_shu_jie_switch) {
            //保存设置
            fragmentSetting.setShuJie(checked);
        }

        if (button.getId() == R.id.sb_setting_sh_switch) {
            if (checked) {
                sb_setting_sh.setLeftText("显示完整宋板伤寒论");

                tipsFragmentSettingEventNotification.setShanghan_Notification(true);
            } else {
                sb_setting_sh.setLeftText("只显示398条辨");

                tipsFragmentSettingEventNotification.setShanghan_Notification(false);
            }
            fragmentSetting.setSong_ShangHan(checked);
        }
        if (button.getId() == R.id.sb_setting_jk_switch) {
            if (checked) {
                sb_setting_jk.setLeftText("显示默认版金匮要略");

                tipsFragmentSettingEventNotification.setJinkui_Notification(true);
            } else {
                sb_setting_jk.setLeftText("不显示金匮要略");

                tipsFragmentSettingEventNotification.setJinkui_Notification(false);
            }
            fragmentSetting.setSong_JinKui(checked);
        }
        //保存设置
        ManagerSetting.saveFragmentSetting(fragmentSetting);
        //通知显示已经变更
        XEventBus.getDefault().post(tipsFragmentSettingEventNotification);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance =null;
        // 注销事件
        XEventBus.getDefault().unregister(TipsSettingFragment.this);
    }
}