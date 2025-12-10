package run.yigou.gxzy.ui.tips.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import run.yigou.gxzy.R;

/**
 * 名词弹窗
 * 显示名词解释的弹窗
 */
public class TipsLittleMingCiViewWindow extends TipsLittleRecyclerViewWindow {

    @Override
    protected WindowConfig getWindowConfig() {
        return new WindowConfig.Builder()
                .upLayout(R.layout.show_fang)
                .downLayout(R.layout.show_fang_2)
                .closeButton(R.id.maskbtn)
                .copyButton(R.id.leftbtn)
                .moreButton(R.id.rightbtn)
                .arrow(R.id.arrow, 30, 20)
                .enableCopy(true)
                .enableMore(false) // 名词弹窗不启用更多按钮
                .build();
    }

    @Override
    protected View createContentView(LayoutInflater inflater, ViewGroup container) {
        // 调用父类创建RecyclerView
        return super.createContentView(inflater, container);
    }

    @Override
    protected void bindData() {
        // 调用父类绑定RecyclerView数据
        super.bindData();
    }
}
