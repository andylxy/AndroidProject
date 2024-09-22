package run.yigou.gxzy.ui.tips.widget;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Build;
import android.util.Log;

import timber.log.Timber;

/**
 * LittleWindow类是一个Fragment的子类，用于实现一个小窗口的功能
 * 它主要提供了搜索文本的管理以及自身的显示和隐藏功能
 */
public class LittleWindow extends Fragment {
    // 用于存储搜索文本的字符串
    protected String searchText;

    /**
     * 获取Fragment的标签
     *
     * @return 返回Fragment的标签，用于标识这个Fragment
     */
    public String getTagName() {
        return "littleWindow";
    }

    /**
     * 获取当前的搜索文本
     *
     * @return 返回当前存储的搜索文本字符串
     */
    public String getSearchString() {
        return this.searchText;
    }

    /**
     * 设置搜索文本
     *
     * @param str 要设置的搜索文本字符串
     */
    public void setSearchText(String str) {
        this.searchText = str;
    }

    
    /**
     * 显示这个Fragment
     *
     * @param fragmentManager FragmentManager对象，用于管理Fragment的事务
     *                        通过这个方法，Fragment可以被添加到活动的Fragment列表中
     */
    public void show(FragmentManager fragmentManager) {
        // 开始一个新的事务
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // 检查事务是否创建成功
        if (transaction == null) {
            throw new IllegalStateException("无法创建FragmentTransaction");
        }

        String tag = getTagName();
        transaction.add(this, tag);
        transaction.addToBackStack(tag);

        // 使用commitAllowingStateLoss来避免状态丢失问题
        transaction.commitAllowingStateLoss();
    }


    /**
     * 隐藏（移除）这个Fragment
     * <p>
     * 通过移除事务将Fragment从当前的活动列表中移除，并且退回到之前的BackStack状态
     */
    public void dismiss() {
        // 获取FragmentManager
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null) {
            Timber.tag("Fragment").e("FragmentManager is null, cannot dismiss fragment.");
            return;
        }

        // 异步提交事务
        fragmentManager.popBackStack();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(this);

        try {
            // 异步提交事务，兼容不同版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    transaction.commitNow();
                    Timber.tag("TransactionManager").i("事务提交成功，使用 commitNow()。");
                } catch (IllegalStateException e) {
                    Timber.tag("TransactionManager").e(e, "使用 commitNow() 提交事务失败。");
                    // 尝试使用 commit() 方法
                    try {
                        transaction.commit();
                        Timber.tag("TransactionManager").i("事务提交成功，使用 commit()。");
                    } catch (IllegalStateException ex) {
                        Timber.tag("TransactionManager").e(ex, "使用 commit() 提交事务失败。");
                    }
                } catch (Exception e) {
                    Timber.tag("TransactionManager").e(e, "提交事务时发生意外错误。");
                }
            } else {
                try {
                    transaction.commit();
                    Timber.tag("TransactionManager").i("事务提交成功，使用 commit()。");
                } catch (IllegalStateException e) {
                    Timber.tag("TransactionManager").e(e, "使用 commit() 提交事务失败。");
                } catch (Exception e) {
                    Timber.tag("TransactionManager").e(e, "提交事务时发生意外错误。");
                }
            }

                // 处理外部捕获的 IllegalStateException
        } catch (IllegalStateException e) {
            Timber.tag("Fragment").e(e, "提交事务失败。");
        }

    }

}
