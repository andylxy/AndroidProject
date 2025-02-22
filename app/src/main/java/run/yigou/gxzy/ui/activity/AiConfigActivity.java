package run.yigou.gxzy.ui.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.hjq.base.BaseDialog;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.lucas.xbus.XEventBus;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import run.yigou.gxzy.EventBus.ChatMessageBeanEvent;
import run.yigou.gxzy.R;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.greendao.entity.AiConfig;
import run.yigou.gxzy.greendao.entity.AiConfigBody;
import run.yigou.gxzy.greendao.entity.ChatMessageBean;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.AiConfigApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.ui.dialog.InputDialog;
import run.yigou.gxzy.ui.dialog.MenuDialog;
import run.yigou.gxzy.ui.dialog.MessageDialog;
import run.yigou.gxzy.ui.tips.aimsg.AiConfigHelper;
import run.yigou.gxzy.ui.tips.aimsg.AiHelper;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;
import run.yigou.gxzy.utils.DateHelper;
import run.yigou.gxzy.utils.RC4Helper;
import run.yigou.gxzy.utils.ShareUtil;
import run.yigou.gxzy.utils.ThreadUtil;


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

    private List<AiConfig> aiConfigList;
    private final List<String> proxyAddressSelectionList = new ArrayList<>();
    private List<String> modelSelectionList = new ArrayList<>();

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
        sw_use_context.setChecked(AiConfigHelper.getUseContext());
        sw_use_context.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AiConfigHelper.setUseContext(isChecked);
            }
        });
        if (AiConfigHelper.getApiKey() != null && !AiConfigHelper.getApiKey().isEmpty()) {
            tv_api_key.setText(apiKeyShow(RC4Helper.decrypt(AiConfigHelper.getApiKey())));
        } else {
            tv_api_key.setText("设置API_KEY（如sk-xxx）");
        }

        tv_model_name.setText("当前模型：" + AiConfigHelper.getGptModel());
        tv_model_proxy.setText("当前Ai " + AiConfigHelper.getProvideAi() + "：" + AiConfigHelper.getProxyAddress());
        tv_assistant_name.setText(AiConfigHelper.getAssistantName() != null ? AiConfigHelper.getAssistantName() : "设置小助手昵称");
        getAiConfigList();

    }


    private void getAiConfigList() {

        aiConfigList = DbService.getInstance().mAiConfigService.findAll();

        if (calculateDaysDifference(AiConfigHelper.getConfigCronJob(), DateHelper.getYearMonthDay1()) >=15 || aiConfigList == null || aiConfigList.isEmpty()) {
            EasyHttp.get(this)
                    .api(new AiConfigApi())
                    .request(new HttpCallback<HttpData<List<run.yigou.gxzy.greendao.entity.AiConfig>>>(this) {
                        @Override
                        public void onSucceed(HttpData<List<run.yigou.gxzy.greendao.entity.AiConfig>> data) {
                            if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                                aiConfigList = data.getData();
                                // TipsSingleData.getInstance().setAiConfigList(data.getData());
                                // 保存到数据库
                                ThreadUtil.runInBackground(() -> {
                                    setProxyAddress();
                                    ConvertEntity.saveAiConfigList(aiConfigList);
                                });
                                AiConfigHelper.setConfigCronJob(DateHelper.getYearMonthDay1());
                            }
                        }
                    });
        } else {
            setProxyAddress();
        }
    }


    private void setProxyAddress() {
        if (aiConfigList != null && !aiConfigList.isEmpty()) {
            proxyAddressSelectionList.clear();
            for (AiConfig aiConfig : aiConfigList) {
                proxyAddressSelectionList.add(aiConfig.getProvideAi());
                if (AiConfigHelper.getProvideAi() != null && !AiConfigHelper.getProvideAi().isEmpty() && AiConfigHelper.getProvideAi().equals(aiConfig.getProvideAi())) {
                    // 判断是否为当前Ai,则设置当前Ai模型
                    if (aiConfig.getModelList() != null && !aiConfig.getModelList().isEmpty()) {
                        modelSelectionList.clear();
                        for (AiConfigBody aiConfigBody : aiConfig.getModelList()) {
                            modelSelectionList.add(aiConfigBody.getGptModelName());
                        }
                    }
                   String aiName =  aiConfig.getProvideAi();
                    modelSelectionList.add("手动输入");
                }
            }

            proxyAddressSelectionList.add("手动输入");
        }
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
                showBtOpenMain();
                break;
            case R.id.bt_clear_msg:
                showClearMsgDialog();
                break;
            default:
        }
    }

    private void showBtOpenMain() {
        StringBuilder text = new StringBuilder();
        boolean isNull = false;
        if (AiConfigHelper.getAssistantName() == null || AiConfigHelper.getAssistantName().isEmpty()) {
            text.append("小助手昵称为空");
            isNull = true;
        }
        if (AiConfigHelper.getApiKey() == null || AiConfigHelper.getApiKey().isEmpty()) {
            text.append("\nAPI_KEY为空");
            isNull = true;
        }
        if (AiConfigHelper.getGptModel() == null || AiConfigHelper.getGptModel().isEmpty()) {
            text.append("\nGPT模型为空");
            isNull = true;
        }
        if (AiConfigHelper.getProxyAddress() == null || AiConfigHelper.getProxyAddress().isEmpty()) {
            text.append("\nAI调用地址为空");
            isNull = true;
        }
        if (isNull) {
            // 消息对话框
            new MessageDialog.Builder(getActivity())
                    // 标题可以不用填写
                    .setTitle("配置AI参数")
                    // 内容必须要填写
                    .setMessage(text)
                    // 确定按钮文本
                    .setConfirm(getString(R.string.common_confirm))
                    // 设置 null 表示不显示取消按钮
                    .setCancel(getString(R.string.common_cancel))
                    // 设置点击按钮后不关闭对话框
                    //.setAutoDismiss(false)
                    .setListener(new MessageDialog.OnListener() {

                        @Override
                        public void onConfirm(BaseDialog dialog) {
                            return;
                        }

                        @Override
                        public void onCancel(BaseDialog dialog) {
                            finish();
                        }
                    })
                    .show();
        } else {
            finish();
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

        new MenuDialog.Builder(this).setList(proxyAddressSelectionList)
                .setListener((dialog, position, string) -> {
                    if (position == proxyAddressSelectionList.size() - 1) { // 判断是否选择了“手动输入”选项
                        showProxyAddressDialog();
                    } else {
                        String selectedProxyAddress = proxyAddressSelectionList.get(position);


                        for (AiConfig aiConfig : aiConfigList) {
                            if (aiConfig.getProvideAi().equals(selectedProxyAddress)) {
                                modelSelectionList.clear();
                                for (AiConfigBody aiConfigBody : aiConfig.getModelList()) {
                                    modelSelectionList.add(aiConfigBody.getGptModelName());
                                }

                                AiConfigHelper.setProxyAddress(aiConfig.getAiUrl());
                                AiConfigHelper.setProvideAi(aiConfig.getProvideAi());

                                if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isEmpty()) {
                                    AiConfigHelper.setApiKey(aiConfig.getApiKey());
                                    tv_api_key.setText(apiKeyShow(RC4Helper.decrypt(aiConfig.getApiKey())));
                                } else {
                                    AiConfigHelper.setApiKey("");
                                }
                                AiConfigHelper.setGptModel("");
                                tv_model_proxy.setText("当前Ai " + AiConfigHelper.getProvideAi() + "：" + aiConfig.getAiUrl());
                                modelSelectionList.add("手动输入");
                                tv_model_name.setText("当前模型：空");
                                break;
                            }

                        }

                        ToastUtils.showLong("已选择AI模型: " + selectedProxyAddress);
                    }
                })
                .show();
    }

    private String apiKeyShow(String apiKey) {


        String displayApiKey;

        if (apiKey == null || apiKey.isEmpty()) {
            return "";
        }

        if (apiKey.length() <= 10) {
            // 如果 apiKey 长度小于等于10，直接显示
            displayApiKey = apiKey;
        } else {
            // 获取前5个字符
            String start = apiKey.substring(0, 5);
            // 获取后5个字符
            String end = apiKey.substring(apiKey.length() - 5);
            // 计算中间需要隐藏的字符数量
            int middleLength = apiKey.length() - 10;
            // 生成中间的 ** 字符串
            StringBuilder middle = new StringBuilder();
            for (int i = 0; i < middleLength; i++) {
                middle.append("*");
            }
            // 拼接结果
            displayApiKey = start + middle.toString() + end;
        }

        return displayApiKey;


    }

    private void showProxyAddressDialog() {

        // 输入对话框
        new InputDialog.Builder(this)
                // 标题可以不用填写
                .setTitle("手动输入模型代理地址")
                // 内容可以不用填写
                .setContent(AiConfigHelper.getProxyAddress())
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
                            AiConfigHelper.setProxyAddress(content.trim());
                            tv_model_proxy.setText("当前代理：" + AiConfigHelper.getProxyAddress());
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
                .setContent((AiConfigHelper.getApiKey() != null && !AiConfigHelper.getApiKey().isEmpty()) ? apiKeyShow(RC4Helper.decrypt(AiConfigHelper.getApiKey())) : "设置API_KEY（如sk-xxx）")
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
                        if (content != null && !content.isEmpty()) {
                            AiConfigHelper.setApiKey(RC4Helper.encrypt(AiConfigHelper.getApiKey()));
                            tv_api_key.setText(content);
                            ToastUtils.showLong("API_KEY设置成功！");
                        }
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
                .setContent(AiConfigHelper.getAssistantName())
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
                            AiConfigHelper.setAssistantName(content.trim());
                            tv_assistant_name.setText(AiConfigHelper.getAssistantName() != null ? AiConfigHelper.getAssistantName() : "设置小助手昵称");
                            ToastUtils.showLong("昵称设置成功！");
                            XEventBus.getDefault().post(new ChatMessageBeanEvent().setAssistantName(true));
                        } else {
                            ToastUtils.showLong("昵称不能设为空！");
                        }
                    }

                })
                .show();
    }


    @SuppressLint("SetTextI18n")
    private void showModelSelectionDialog() {

//        if (modelSelectionList.isEmpty()) {
//            ToastUtils.showLong("请先选择模型!");
//            return;
//        }
        new MenuDialog.Builder(this).setList(modelSelectionList)
                .setListener((dialog, position, string) -> {
                    if (position == modelSelectionList.size() - 1) { // 判断是否选择了“手动输入”选项
                        showModelDialog();
                    } else {
                        String selectedModel = modelSelectionList.get(position);

                        selectedModel = selectedModel.replace("（推荐）", "");

                        AiConfigHelper.setGptModel(selectedModel);
                        tv_model_name.setText("当前模型：" + AiConfigHelper.getGptModel());

                        ToastUtils.showLong("已选择模型: " + AiConfigHelper.getGptModel());

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
                .setContent(AiConfigHelper.getGptModel())
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
                            AiConfigHelper.setGptModel(content.trim());
                            tv_model_name.setText("当前模型：" + AiConfigHelper.getGptModel());
                            ToastUtils.showLong("已选择模型: " + AiConfigHelper.getGptModel());
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

    // 定义函数，接受传入的时间字符串，计算日期差异
    public static long calculateDaysDifference(String startDateStr, String endDateStr) {
        try {
            // 定义日期格式，符合 "yyyy-MM-dd" 格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            // 解析传入的日期字符串为 Date 对象
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);

            // 获取两个日期之间的时间差（毫秒）
            long diffInMillies = endDate.getTime() - startDate.getTime();

            // 将毫秒差异转换为天数
            return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // 返回-1，表示发生了错误
    }

    @Override
    protected void onDestroy() {
        // 注销事件
        XEventBus.getDefault().unregister(AiConfigActivity.this);
        super.onDestroy();
    }
}