package run.yigou.gxzy.ui.tips.widget;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import run.yigou.gxzy.R;

/**
 * 药名弹窗
 * 显示药名详细信息的简单文本弹窗
 */
public class TipsLittleTextViewWindow extends TipsLittleWindow {

    private String yao;
    private TextView textView;

    /**
     * 设置药名
     */
    public void setYao(String str) {
        this.yao = str;
    }

    @Override
    protected WindowConfig getWindowConfig() {
        return new WindowConfig.Builder()
                .upLayout(R.layout.show_yao)
                .downLayout(R.layout.show_yao_2)
                .closeButton(R.id.maskbtnYao)
                .arrow(R.id.arrow, 30, 20)
                .enableCopy(false)  // 药名弹窗不需要复制按钮
                .enableMore(false)  // 药名弹窗不需要更多按钮
                .build();
    }

    @Override
    protected View createContentView(LayoutInflater inflater, ViewGroup container) {
        // 查找TextView
        textView = this.view.findViewById(R.id.content);
        return textView;
    }

    @Override
    protected void bindData() {
        // 绑定药名数据到TextView
        if (textView != null && yao != null) {
            textView.setText(yao);
        }
    }
}
