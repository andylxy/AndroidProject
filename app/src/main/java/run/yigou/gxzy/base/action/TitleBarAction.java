package run.yigou.gxzy.base.action;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.hjq.bar.OnTitleBarListener;
import com.hjq.bar.TitleBar;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/08
 *    desc   : 标题栏操作接口
 */
public interface TitleBarAction extends OnTitleBarListener {

    @Nullable
    TitleBar getTitleBar();

    /**
     * 标题栏左侧点击事件
     *
     * @param view     被点击的 View
     */
    @Override
    default void onLeftClick(View view) {}

    /**
     * 标题栏标题点击事件
     *
     * @param view     被点击的 View
     */
    @Override
    default void onTitleClick(View view) {}

    /**
     * 标题栏右侧点击事件
     *
     * @param view     被点击的 View
     */
    @Override
    default void onRightClick(View view) {}

    /**
     * 设置标题栏标题（字符串资源）
     */
    default void setTitle(@StringRes int id) {
        if (getTitleBar() != null) {
            setTitle(getTitleBar().getResources().getString(id));
        }
    }

    /**
     * 设置标题（字符串文本）
     */
    default void setTitle(CharSequence title) {
        if (getTitleBar() != null) {
            getTitleBar().setTitle(title);
        }
    }

    /**
     * 设置标题栏左侧标题（字符串资源）
     */
    default void setLeftTitle(int id) {
        if (getTitleBar() != null) {
            getTitleBar().setLeftTitle(id);
        }
    }

    default void setLeftTitle(CharSequence text) {
        if (getTitleBar() != null) {
            getTitleBar().setLeftTitle(text);
        }
    }

    default CharSequence getLeftTitle() {
        if (getTitleBar() != null) {
            return getTitleBar().getLeftTitle();
        }
        return "";
    }

    /**
     * 设置标题栏右侧标题（字符串资源）
     */
    default void setRightTitle(int id) {
        if (getTitleBar() != null) {
            getTitleBar().setRightTitle(id);
        }
    }

    default void setRightTitle(CharSequence text) {
        if (getTitleBar() != null) {
            getTitleBar().setRightTitle(text);
        }
    }

    default CharSequence getRightTitle() {
        if (getTitleBar() != null) {
            return getTitleBar().getRightTitle();
        }
        return "";
    }

    /**
     * 设置标题栏左侧图标（字符串资源）
     */
    default void setLeftIcon(int id) {
        if (getTitleBar() != null) {
            getTitleBar().setLeftIcon(id);
        }
    }

    default void setLeftIcon(Drawable drawable) {
        if (getTitleBar() != null) {
            getTitleBar().setLeftIcon(drawable);
        }
    }

    @Nullable
    default Drawable getLeftIcon() {
        if (getTitleBar() != null) {
            return getTitleBar().getLeftIcon();
        }
        return null;
    }

    /**
     * 设置标题栏右侧图标（字符串资源）
     */
    default void setRightIcon(int id) {
        if (getTitleBar() != null) {
            getTitleBar().setRightIcon(id);
        }
    }

    default void setRightIcon(Drawable drawable) {
        if (getTitleBar() != null) {
            getTitleBar().setRightIcon(drawable);
        }
    }

    @Nullable
    default Drawable getRightIcon() {
        if (getTitleBar() != null) {
            return getTitleBar().getRightIcon();
        }
        return null;
    }

    /**
     * ?????? ViewGroup ??? TitleBar ???
     */
    default TitleBar obtainTitleBar(ViewGroup group) {
        if (group == null) {
            return null;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View view = group.getChildAt(i);
            if ((view instanceof TitleBar)) {
                return (TitleBar) view;
            }

            if (view instanceof ViewGroup) {
                TitleBar titleBar = obtainTitleBar((ViewGroup) view);
                if (titleBar != null) {
                    return titleBar;
                }
            }
        }
        return null;
    }
}
