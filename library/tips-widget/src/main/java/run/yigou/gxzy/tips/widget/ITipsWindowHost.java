package run.yigou.gxzy.tips.widget;

import android.content.Intent;

import androidx.annotation.IdRes;

/**
 * Tips 弹窗框架宿主接口
 * 用于解耦 TipsLittleWindow 对 app 模块的硬依赖：
 *   1. wrapper 容器 View ID（替代硬编码 R.id.wrapper）
 *   2. "更多"按钮导航（替代硬编码 TipsFragmentActivity.class）
 */
public interface ITipsWindowHost {
    /** 提供弹窗 wrapper 容器的 View ID */
    @IdRes int getWrapperViewId();

    /** "更多"按钮点击时的导航行为 */
    void navigateToDetail(Intent intent);
}
