
package run.yigou.gxzy.manager;/*
 * 项目名: AndroidProject
 * 类名: ReferenceManager.java
 * 包名: ReferenceManager
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月23日 23:23:43
 * 上次修改时间: 2024年09月23日 23:23:43
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class ReferenceManager {
    private static ReferenceManager instance;
    private WeakHashMap<Object, WeakReference<?>> weakReferences = new WeakHashMap<>();

    private ReferenceManager() {}

    public static ReferenceManager getInstance() {
        if (instance == null) {
            synchronized (ReferenceManager.class) {
                if (instance == null) {
                    instance = new ReferenceManager();
                }
            }
        }
        return instance;
    }

    public <T> void addReference(Object key, T object) {
        weakReferences.put(key, new WeakReference<>(object));
    }

    public void removeReference(Object key) {
        weakReferences.remove(key);
    }
}
