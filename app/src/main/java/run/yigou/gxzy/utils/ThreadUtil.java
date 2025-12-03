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
import android.os.Looper;

import run.yigou.gxzy.manager.ThreadPoolManager;

/**
 * 线程工具类
 */
public class ThreadUtil {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 在主线程执行任务
     *
     * @param runnable 传入要执行的任务
     */
    public static void runOnUiThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        
        // 如果当前已经在主线程，则直接执行
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    /**
     * 在子线程中执行任务
     *
     * @param runnable 传入要执行的任务
     */
    public static void runInBackground(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        
        ThreadPoolManager.getInstance().execute(runnable);
    }
}