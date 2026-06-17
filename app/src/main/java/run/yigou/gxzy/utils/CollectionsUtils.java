/*
 * 项目名: AndroidProject
 * 类名: CollectionsUtils.java
 * 包名: run.yigou.gxzy.utils
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.utils;

import java.util.List;

/**
 * 集合工具类，提供通用的集合操作辅助方法。
 *
 * <p>职责：
 * <ul>
 *   <li>提供泛型条件判断接口 {@link Condition}</li>
 *   <li>提供列表元素存在性检查方法 {@link #some(List, Condition)}</li>
 * </ul>
 *
 * <p>从 {@code TipsNetHelper} 中提取，解耦数据层（如 {@code Fang}）对 UI 层 helper 的依赖。
 */
public class CollectionsUtils {

    /**
     * 泛型条件判断接口，用于对集合元素进行自定义条件测试。
     *
     * @param <T> 被测试的元素类型
     */
    @FunctionalInterface
    public interface Condition<T> {
        /**
         * 测试给定元素是否满足条件。
         *
         * @param t 待测试的元素
         * @return true 表示满足条件，false 表示不满足
         */
        boolean test(T t);
    }

    /**
     * 检查列表中是否存在至少一个元素满足指定条件。
     *
     * <p>类似于 JavaScript 的 {@code Array.prototype.some()} 方法。
     * 如果列表为 null 或条件为 null，安全返回 false。
     *
     * @param <T>       列表元素类型
     * @param list      待检查的列表，可为 null
     * @param condition 条件判断接口，可为 null
     * @return true 表示存在至少一个满足条件的元素，false 表示不存在或参数无效
     */
    public static <T> boolean some(List<T> list, Condition<T> condition) {
        if (list == null || condition == null) {
            return false;
        }
        for (T element : list) {
            if (condition.test(element)) {
                return true;
            }
        }
        return false;
    }
}
