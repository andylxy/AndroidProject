package run.yigou.gxzy.ui.reader.widget;

import run.yigou.gxzy.R;
import run.yigou.gxzy.tips.widget.WindowConfig;

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
                .wrapperView(R.id.wrapper)
                .build();
    }
}
