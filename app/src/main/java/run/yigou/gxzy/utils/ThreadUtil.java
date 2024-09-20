/*
 * 项目名: AndroidProject
 * 类名: ThreadUtil.java
 * 包名: run.yigou.gxzy.utils.ThreadUtil
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月20日 09:34:25
 * 上次修改时间: 2024年09月20日 09:34:25
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.utils;

import android.os.Handler;

import run.yigou.gxzy.manager.ThreadPoolManager;

public class ThreadUtil {

    private static Handler handler = new Handler();
    /**
     * 主线程执行 runOnUiThread
     *
     * @param runnable 传入要执行的任务
     */
    public static void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    /**
     * 子线程中执行任务 ThreadPoolManager
     * @param runnable 传入要执行的任务
     */
    public static void runInBackground(Runnable runnable) {
        ThreadPoolManager.getInstance().execute(runnable);
    }


}
