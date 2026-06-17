package run.yigou.gxzy.ui.reader.widget;

import android.content.Intent;

import run.yigou.gxzy.R;
import run.yigou.gxzy.tips.widget.WindowConfig;

/**
 * 药方弹窗
 * 显示药方详细信息的弹窗
 */
public class TipsLittleTableViewWindow extends TipsLittleRecyclerViewWindow {

    private String fang;

    /**
     * 设置药方名称
     */
    public void setFang(String str) {
        this.fang = str;
    }

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
                .enableMore(true)
                .wrapperView(R.id.wrapper)
                .build();
    }

    @Override
    protected void onMoreButtonClick(Intent intent) {
        // 自定义更多按钮行为
        intent.putExtra("title", fang);
        intent.putExtra("isFang", "true");
    }
}
