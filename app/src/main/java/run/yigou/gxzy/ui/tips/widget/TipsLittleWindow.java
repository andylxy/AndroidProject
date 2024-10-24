package run.yigou.gxzy.ui.tips.widget;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Build;

import com.hjq.http.EasyLog;

import timber.log.Timber;

/**
 * LittleWindow类是一个Fragment的子类，用于实现一个小窗口的功能
 * 它主要提供了搜索文本的管理以及自身的显示和隐藏功能
 */
public class TipsLittleWindow extends Fragment {

    /**
     * 获取Fragment的标签
     *
     * @return 返回Fragment的标签，用于标识这个Fragment
     */
    public String getTagName() {
        return "littleWindow";
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
            EasyLog.print("Fragment-FragmentManager is null, cannot dismiss fragment.");
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
                    EasyLog.print("TransactionManager事务提交成功，使用 commitNow()。");
                } catch (IllegalStateException e) {
                    EasyLog.print("TransactionManager使用 commitNow() 提交事务失败。");
                    // 尝试使用 commit() 方法
                    try {
                        transaction.commit();
                        EasyLog.print("TransactionManager事务提交成功，使用 commit()。");
                    } catch (IllegalStateException ex) {
                        EasyLog.print("TransactionManager使用 commit() 提交事务失败。");
                    }
                } catch (Exception e) {
                    EasyLog.print("TransactionManager提交事务时发生意外错误。");
                }
            } else {
                try {
                    transaction.commit();
                    EasyLog.print("TransactionManager事务提交成功，使用 commit()。");
                } catch (IllegalStateException e) {
                    EasyLog.print("TransactionManager使用 commit() 提交事务失败。");
                } catch (Exception e) {
                    EasyLog.print("TransactionManager提交事务时发生意外错误。");
                }
            }

                // 处理外部捕获的 IllegalStateException
        } catch (IllegalStateException e) {
            EasyLog.print("Fragment提交事务失败。");
        }

    }

}
