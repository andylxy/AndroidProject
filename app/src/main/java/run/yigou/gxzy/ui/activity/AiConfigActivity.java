package run.yigou.gxzy.ui.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.hjq.base.BaseDialog;
import com.lucas.xbus.XEventBus;

import java.util.ArrayList;

import run.yigou.gxzy.EventBus.ChatMessageBeanEvent;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.dialog.InputDialog;
import run.yigou.gxzy.ui.dialog.MenuDialog;
import run.yigou.gxzy.ui.fragment.TipsSettingFragment;
import run.yigou.gxzy.ui.tips.aimsg.AiConfig;
import run.yigou.gxzy.utils.ShareUtil;


public final class AiConfigActivity extends AppActivity {

    private RelativeLayout rl_proxy_address;
    private RelativeLayout rl_api_key;
    private RelativeLayout rl_assistant_name;
    private RelativeLayout rl_gpt_model;
    private RelativeLayout rl_donate;
    private RelativeLayout rl_share;
    private Button bt_open_main;
    private Switch sw_use_context;
    private TextView tv_model_proxy;
    private TextView tv_model_name;
    private TextView tv_assistant_name;
    private TextView tv_api_key;
    private Button bt_clear_msg;

    @Override
    protected int getLayoutId() {
        return R.layout.tips_ai_msg_activity_config;
    }

    @Override
    protected void initView() {

        rl_proxy_address = findViewById(R.id.rl_proxy_address);

        rl_api_key = findViewById(R.id.rl_api_key);

        rl_assistant_name = findViewById(R.id.rl_assistant_name);

        rl_gpt_model = findViewById(R.id.rl_gpt_model);

        rl_donate = findViewById(R.id.rl_donate);

        rl_share = findViewById(R.id.rl_share);
        bt_open_main = findViewById(R.id.bt_open_main);

        sw_use_context = findViewById(R.id.sw_use_context);

        tv_model_proxy = findViewById(R.id.tv_model_proxy);

        tv_model_name = findViewById(R.id.tv_model_name);

        tv_assistant_name = findViewById(R.id.tv_assistant_name);
        tv_api_key = findViewById(R.id.tv_api_key);
        bt_clear_msg = findViewById(R.id.bt_clear_msg);
        // 注册事件
        XEventBus.getDefault().register(AiConfigActivity.this);
        setOnClickListener(rl_proxy_address, rl_api_key, rl_assistant_name, rl_gpt_model, rl_donate, rl_share, bt_open_main, bt_clear_msg);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void initData() {

        sw_use_context.setChecked(AiConfig.getUseContext());
        sw_use_context.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            /**
             * @param buttonView The compound button view whose state has changed.
             * @param isChecked  The new checked state of buttonView.
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AiConfig.setUseContext(isChecked);
            }
        });
        tv_api_key.setText(AiConfig.getApiKey() != null ? AiConfig.getApiKey() : "设置API_KEY（如sk-xxx）");
        tv_model_proxy.setText("当前代理：" + AiConfig.getProxyAddress());
        tv_model_name.setText("当前模型：" + AiConfig.getGptModel());
        tv_assistant_name.setText(AiConfig.getAssistantName() != null ? AiConfig.getAssistantName() : "设置小助手昵称");
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.rl_proxy_address:
                showProxyAddressSelectionDialog();
                break;
            case R.id.rl_api_key:
                showAPIKeyDialog();
                break;
            case R.id.rl_assistant_name:
                showRenameDialog();
                break;
            case R.id.rl_gpt_model:
                showModelSelectionDialog();
                break;
            case R.id.rl_donate:
                showDonateDialog();
                break;
            case R.id.rl_share:
                showShareDialog();
                break;
            case R.id.bt_open_main:
                finish();
                break;
            case R.id.bt_clear_msg:
                showClearMsgDialog();
                break;
            default:
        }
    }

    private void showClearMsgDialog() {

        ArrayList<ChatMessageBean> list = DbService.getInstance().mChatMessageBeanService.findAll();
        if (list != null && !list.isEmpty()) {
            for (ChatMessageBean item : list) {
                if (item.getIsDelete() == ChatMessageBean.IS_Delete_NO) {
                    item.setIsDelete(ChatMessageBean.IS_Delete_YES);
                    DbService.getInstance().mChatMessageBeanService.updateEntity(item);
                }
            }
            //通知显示已经变更
            XEventBus.getDefault().post(new ChatMessageBeanEvent().setClear(true));
            ToastUtils.showLong("历史AI对话已清空");
            finish();
        }
    }


    @SuppressLint("SetTextI18n")
    private void showProxyAddressSelectionDialog() {
        final String[] items = new String[]{
                "https://api.siliconflow.cn","https://api.lkeap.cloud.tencent.com",
                "手动输入"
        };
        new MenuDialog.Builder(this).setList(items)
                .setListener((dialog, position, string) -> {
                    if (position == items.length - 1) { // 判断是否选择了“手动输入”选项
                        showProxyAddressDialog();
                    } else {
                        String selectedProxyAddress = items[position];
                        AiConfig.setProxyAddress(selectedProxyAddress);
                        tv_model_proxy.setText("当前代理：" + AiConfig.getProxyAddress());
                        ToastUtils.showLong("已选择代理地址: " + selectedProxyAddress);
                    }
                })
                .show();
    }

    private void showProxyAddressDialog() {

        // 输入对话框
        new InputDialog.Builder(this)
                // 标题可以不用填写
                .setTitle("手动输入模型代理地址")
                // 内容可以不用填写
                .setContent(AiConfig.getProxyAddress())
                // 提示可以不用填写
                .setHint("请输入代理地址")
                // 确定按钮文本
                .setConfirm(getString(R.string.common_confirm))
                // 设置 null 表示不显示取消按钮
                .setCancel(getString(R.string.common_cancel))
                // 设置点击按钮后不关闭对话框
                //.setAutoDismiss(false)
                .setListener(new InputDialog.OnListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onConfirm(BaseDialog dialog, String content) {

                        if (content != null && !content.isEmpty()) {
                            AiConfig.setProxyAddress(content.trim());
                            tv_model_proxy.setText("当前代理：" + AiConfig.getProxyAddress());
                            ToastUtils.showLong("已选择代理地址:  + Config.proxyAddress！");
                        } else {

                            ToastUtils.showLong("请勿为空！");
                        }

                    }

                    @Override
                    public void onCancel(BaseDialog dialog) {
                        ToastUtils.showLong("请勿为空！");
                    }

                })
                .show();
    }

    private void showAPIKeyDialog() {
        // 输入对话框
        new InputDialog.Builder(this)
                // 标题可以不用填写
                .setTitle("API_KEY")
                // 内容可以不用填写
                .setContent(AiConfig.getApiKey())
                // 提示可以不用填写
                .setHint("xx-xxxxxx")
                // 确定按钮文本
                .setConfirm(getString(R.string.common_confirm))
                // 设置 null 表示不显示取消按钮
                .setCancel(getString(R.string.common_cancel))
                // 设置点击按钮后不关闭对话框
                //.setAutoDismiss(false)
                .setListener(new InputDialog.OnListener() {

                    @Override
                    public void onConfirm(BaseDialog dialog, String content) {
                        AiConfig.setApiKey(content.trim());
                        tv_api_key.setText(AiConfig.getApiKey() != null ? AiConfig.getApiKey() : "设置API_KEY（如sk-xxx）");
                        ToastUtils.showLong("API_KEY设置成功！");
                    }

                    @Override
                    public void onCancel(BaseDialog dialog) {
                        ToastUtils.showLong("请勿为空！");
                    }
                })
                .show();
    }

    private void showRenameDialog() {
        new InputDialog.Builder(this)
                // 标题可以不用填写
                .setTitle("昵称")
                // 内容可以不用填写
                .setContent(AiConfig.getAssistantName())
                // 提示可以不用填写
                .setHint("请输入昵称")
                // 确定按钮文本
                .setConfirm(getString(R.string.common_confirm))
                // 设置 null 表示不显示取消按钮
                .setCancel(getString(R.string.common_cancel))
                // 设置点击按钮后不关闭对话框
                //.setAutoDismiss(false)
                .setListener(new InputDialog.OnListener() {

                    @Override
                    public void onConfirm(BaseDialog dialog, String content) {
                        if (content != null && !content.isEmpty()) {
                            AiConfig.setAssistantName(content.trim());
                            tv_assistant_name.setText(AiConfig.getAssistantName() != null ? AiConfig.getAssistantName() : "设置小助手昵称");
                            ToastUtils.showLong("昵称设置成功！");
                            XEventBus.getDefault().post(new ChatMessageBeanEvent().setAssistantName(true));
                        }else {
                            ToastUtils.showLong("昵称不能设为空！");
                        }
                    }

                })
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void showModelSelectionDialog() {
        final String[] items = new String[]{
               "deepseek-ai/DeepSeek-R1（推荐-流动硅基）","deepseek-r1（推荐-腾讯）", "手动输入"};// 预设的模型选项和手动输入选项

        new MenuDialog.Builder(this).setList(items)
                .setListener((dialog, position, string) -> {
                    if (position == items.length - 1) { // 判断是否选择了“手动输入”选项
                        showModelDialog();
                    } else {
                        String selectedModel = items[position];
                        if (selectedModel.contains("流动硅基")) {
                            selectedModel = selectedModel.replace("（推荐-流动硅基）", "");
                            selectedModel = selectedModel.replace("（流动硅基）", "");
                        }
                        if (selectedModel.contains("腾讯")) {
                            selectedModel = selectedModel.replace("（推荐-腾讯）", "");
                            selectedModel = selectedModel.replace("（腾讯）", "");
                        }
                        AiConfig.setGptModel(selectedModel);
                        tv_model_name.setText("当前模型：" + AiConfig.getGptModel());

                        ToastUtils.showLong("已选择模型: " + AiConfig.getGptModel());

                    }
                })
                .show();
    }

    private void showModelDialog() {

        // 输入对话框
        new InputDialog.Builder(this)
                // 标题可以不用填写
                .setTitle("手动输入模型名称")
                // 内容可以不用填写
                .setContent(AiConfig.getGptModel())
                // 提示可以不用填写
                .setHint("请输入模型名称")
                // 确定按钮文本
                .setConfirm(getString(R.string.common_confirm))
                // 设置 null 表示不显示取消按钮
                .setCancel(getString(R.string.common_cancel))
                // 设置点击按钮后不关闭对话框
                //.setAutoDismiss(false)
                .setListener(new InputDialog.OnListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onConfirm(BaseDialog dialog, String content) {
                        if (content != null) {
                            AiConfig.setGptModel(content.trim());
                            tv_model_name.setText("当前模型：" + AiConfig.getGptModel());
                            ToastUtils.showLong("已选择模型: " + AiConfig.getGptModel());
                        } else {
                            ToastUtils.showLong("请勿为空！");
                        }
                    }

                    @Override
                    public void onCancel(BaseDialog dialog) {
                        ToastUtils.showLong("请勿为空！");
                    }

                })
                .show();
    }

    private void showDonateDialog() {
        ToastUtils.showLong("捐赠");
    }

    private void showShareDialog() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType(ShareUtil.TEXT);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "我发现一个非常好用的聊天程序，文档地址: https://github.com/worktool/chatgpt-android");

        Intent chooserIntent = Intent.createChooser(shareIntent, "分享");
        startActivity(chooserIntent);
    }

    @Override
    protected void onDestroy() {
        // 注销事件
        XEventBus.getDefault().unregister(AiConfigActivity.this);
        super.onDestroy();
    }
}