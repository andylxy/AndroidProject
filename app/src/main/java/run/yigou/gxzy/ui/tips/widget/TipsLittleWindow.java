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

      boolean committed = false;

      try {
          // 异步提交事务，兼容不同版本
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
              transaction.commitNow();
              EasyLog.print("TransactionManager事务提交成功，使用 commitNow()。");
              committed = true;
          } else {
              transaction.commit();
              EasyLog.print("TransactionManager事务提交成功，使用 commit()。");
          }
      } catch (IllegalStateException e) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !committed) {
              try {
                  transaction.commit();
                  EasyLog.print("TransactionManager事务提交成功，使用 commit()。");
              } catch (Exception ex) {
                  EasyLog.print("TransactionManager提交事务失败。");
              }
          } else {
              EasyLog.print("TransactionManager提交事务失败。");
          }
      } catch (Exception e) {
          EasyLog.print("TransactionManager提交事务失败。");
      }
  }

}
