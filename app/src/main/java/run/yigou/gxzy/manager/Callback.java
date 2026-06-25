/*
 * 项目名: AndroidProject
 * 类名: Callback.java
 * 包名: run.yigou.gxzy.manager
 * 创建时间 : 2026年06月25日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.manager;

/**
 * 统一的数据回调接口
 * 
 * <p>用于 DataManager 网络请求和数据加载的统一回调。
 * 所有 DataManager（AccountDataManager、AppDataManager、BookDataManager 等）
 * 都应使用此回调接口，避免每个 Manager 定义自己的回调。
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * dataManager.loadData(this, new Callback&lt;User&gt;() {
 *     &#64;Override
 *     public void onSuccess(User data) {
 *         // 处理成功
 *     }
 *     
 *     &#64;Override
 *     public void onError(Exception e) {
 *         // 处理失败
 *     }
 * });
 * </pre>
 * 
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>调用方只暴露两个方法：onSuccess 和 onError</li>
 *   <li>所有底层细节（生命周期绑定、空值校验、类型转换）由 Manager 内部处理</li>
 *   <li>使用泛型 T 支持任意数据类型</li>
 * </ul>
 * 
 * @param <T> 数据类型，无数据时使用 Void
 */
public interface Callback<T> {
    
    /**
     * 数据加载成功
     * 
     * @param data 加载的数据，可能为 null
     */
    void onSuccess(T data);
    
    /**
     * 数据加载失败
     * 
     * @param e 异常信息
     */
    void onError(Exception e);
}
