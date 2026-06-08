# 阶段 2 批次 4：PickerLayoutManager 解耦与 Adapter/AOP 阻塞固化计划

## 1. 目标

本修订响应用户反馈“进入执行模式”：该反馈视为要求将本计划整理为可直接审批并进入执行的实施依据；计划范围不扩展，不加入 `AppAdapter`、AOP 或剩余 Dialog/Popup 迁移。

在不扩大到媒体、AI Chat、Reader/Tips、Account 或业务 Dialog 的前提下，先解除 `DateDialog` / `TimeDialog` 对 app 内 `PickerLayoutManager` 的依赖，为后续迁移日期/时间 Dialog 到 `library/ui-dialog` 做准备。

本批次不迁移 `AppAdapter`，因为它在 app 内共有 27 个 import，覆盖通用 Dialog、业务 Dialog、媒体 Adapter、阅读/Tips Adapter、AI Chat Adapter 和 Demo Adapter。直接迁移会跨越多个后续阶段，超出“阶段 2 Dialog/Popup 小批次”边界。

本批次不迁移 `SingleClick` / `SingleClickAspect` / AspectJ 配置，因为当前切面 Pointcut 绑定 `run.yigou.gxzy.aop.SingleClick`，且 `app/build.gradle` 的 `aspectjx.include` 绑定 applicationId，改动会影响 AOP 编织范围和运行行为，应单独计划。

## 2. 已确认事实

- `app/src/main/java/run/yigou/gxzy/manager/PickerLayoutManager.java`
  - 只依赖 Android SDK、AndroidX RecyclerView/Core。
  - 不依赖 `run.yigou.gxzy.R`、业务模型、app 初始化或其他 app 类。
  - 当前仅被 `DateDialog` 和 `TimeDialog` import。
- `app/src/main/java/run/yigou/gxzy/app/AppAdapter.java`
  - 只依赖 Android SDK、AndroidX 注解和 `com.hjq.base.BaseAdapter`。
  - 但全仓 import 较多，至少包括：`MenuDialog`、`SelectDialog`、`DateDialog`、`TimeDialog`、`ListPopup`、`ShareDialog`、`PayPasswordDialog`、`AlbumDialog`、`AddressDialog`、媒体 Adapter、阅读/Tips Adapter、AI Chat Adapter、Demo Adapter。
- `SingleClickAspect` 当前 Pointcut：`execution(@run.yigou.gxzy.aop.SingleClick * *(..))`。
- `library/base` 包名为 `com.hjq.base`，现有共享类直接放在：
  - `library/base/src/main/java/com/hjq/base/`
- `library/ui-dialog` 当前已依赖 `library:base`，所以后续 Dialog 可直接引用 `com.hjq.base.PickerLayoutManager`。

## 3. 架构概述

本批次采用“Picker 先抽、Adapter/AOP 后置”的依赖收敛方式：

```text
app
 ├─ DateDialog / TimeDialog
 │   └─ com.hjq.base.PickerLayoutManager
 └─ 其他仍在 app 的 Adapter/Dialog/Feature 代码

library/base
 └─ PickerLayoutManager

library/ui-dialog
 └─ 后续可通过既有 dependency 使用 PickerLayoutManager
```

目标依赖方向：

- 允许：`app -> library:base`
- 允许：`library:ui-dialog -> library:base`
- 禁止：`library:base -> app`
- 本批次不新增模块依赖。

## 4. 文件路径与组件关系

### 4.1 新增文件

- 新增：`library/base/src/main/java/com/hjq/base/PickerLayoutManager.java`
  - 包名：`com.hjq.base`
  - 类名保持：`PickerLayoutManager`
  - 公开 API 保持不变：
    - `getPickedPosition()`
    - `setOnPickerListener(@Nullable OnPickerListener listener)`
    - `interface OnPickerListener`
    - `static final class Builder`
    - `Builder#setOrientation(...)`
    - `Builder#setReverseLayout(...)`
    - `Builder#setMaxItem(...)`
    - `Builder#setScale(...)`
    - `Builder#setAlpha(...)`
    - `Builder#setOnPickerListener(...)`
    - `Builder#build()`
    - `Builder#into(RecyclerView recyclerView)`

### 4.2 删除文件

- 删除：`app/src/main/java/run/yigou/gxzy/manager/PickerLayoutManager.java`

### 4.3 修改文件

- 修改：`app/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
  - import 从 `run.yigou.gxzy.manager.PickerLayoutManager` 改为 `com.hjq.base.PickerLayoutManager`
  - 类逻辑、方法签名、资源引用不改。

- 修改：`app/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
  - import 从 `run.yigou.gxzy.manager.PickerLayoutManager` 改为 `com.hjq.base.PickerLayoutManager`
  - 类逻辑、方法签名、资源引用不改。

- 修改：`docs/ui-module-migration-plan.md`
  - 更新“当前进度”和“当前批次控制”为阶段 2 批次 4。
  - 新增批次 4 结果段落。
  - 新增或更新阻塞：
    - `AppAdapter` 因 27 个 app import 跨阶段，暂不迁移。
    - `SingleClick` / `SingleClickAspect` / AspectJ 配置仍需单独计划。
  - 新增验证记录。
  - 新增决策记录：Picker 先迁入 `library/base`，Adapter/AOP 后置。

## 5. 数据结构与行为影响

- `PickerLayoutManager` 内部字段、Builder 参数和监听接口保持原样。
- 日期/时间 Dialog 的滚动、缩放、透明度、居中选中、停止滚动回调逻辑不改变。
- 仅更换类所属模块和 import 路径。
- 不新增持久化数据结构。
- 不修改网络、数据库、全局状态或用户数据。

## 6. 错误处理与边界条件

- 保留 `PickerLayoutManager#getPickedPosition()` 在 `findSnapView(this)` 返回 null 时返回 0 的既有行为。
- 保留 `DateDialog` / `TimeDialog` 对非法日期/时间字符串的既有处理方式。
- 不新增异常捕获或日志，避免改变 UI 热路径行为。
- 若迁移后出现编译失败且原因不是 import 或包名替换导致，应停止执行，回到计划模式重新评估。

## 7. 依赖管理

- 不修改 `settings.gradle`。
- 不修改 `app/build.gradle`。
- 不修改 `library/base/build.gradle`。
- 不修改 `library/ui-dialog/build.gradle`。
- 依据：
  - `common.gradle` 已为库模块提供 AndroidX appcompat/material 基础依赖。
  - `PickerLayoutManager` 所需 RecyclerView 依赖当前已可由现有构建解析；若实际编译失败，再回到计划模式确认是否需要显式依赖 `androidx.recyclerview:recyclerview`。

## 8. 实施边界

### 允许改动范围

- `library/base/src/main/java/com/hjq/base/PickerLayoutManager.java`
- `app/src/main/java/run/yigou/gxzy/manager/PickerLayoutManager.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
- `docs/ui-module-migration-plan.md`

### 排除范围

- 不迁移 `AppAdapter`。
- 不迁移、复制、修改 `SingleClick`、`SingleClickAspect`、其他 AOP 注解或 AspectJ 配置。
- 不迁移 `MenuDialog`、`SelectDialog`、`DateDialog`、`TimeDialog`、`ListPopup` 到 `library/ui-dialog`。
- 不修改 `ShareDialog`、`PayPasswordDialog`、`AlbumDialog`、`AddressDialog` 等业务或待归属 Dialog。
- 不修改媒体模块、AI Chat、Reader/Tips、Account、Manifest、README、AGENTS.md。
- 不做 Gradle 依赖版本调整。
- 不做无关 refactor、格式化或注释重写。

### 新文件落位

- 允许新增 1 个 Java 文件：
  - `library/base/src/main/java/com/hjq/base/PickerLayoutManager.java`
- 不允许新增其他源码、资源、文档或目录。

### 文档同步

- 必须更新现有 `docs/ui-module-migration-plan.md`，不新增平行文档。

## 9. 验证方法

执行后验证：

1. 静态搜索：
   - `app/src/main/java` 中不再存在 `import run.yigou.gxzy.manager.PickerLayoutManager;`
   - `app/src/main/java/run/yigou/gxzy/manager/PickerLayoutManager.java` 不存在。
   - `library/base/src/main/java/com/hjq/base/PickerLayoutManager.java` 存在且包名为 `com.hjq.base`。
2. 静态检查：
   - `library/base` 中不得出现 `run.yigou.gxzy.R` 或 `run.yigou.gxzy.app`。
3. 构建验证：
   - 运行 `gradlew.bat assembleDebug`。
4. 文档一致性：
   - `docs/ui-module-migration-plan.md` 的当前批次、阻塞、验证记录、ADR 与实际改动一致。

## 10. 风险与回退

- 风险 1：`library/base` 当前是否显式包含 RecyclerView 依赖不完全可见；若构建失败，可能需要补充依赖，但这不在当前批准范围内，需回到计划模式。
- 风险 2：只解耦 Picker 后，`DateDialog` / `TimeDialog` 仍不能迁入 `library/ui-dialog`，因为仍依赖 `AppAdapter` 和 `SingleClick`。
- 回退方式：
  - 恢复 app 原 `PickerLayoutManager.java`。
  - 将 `DateDialog` / `TimeDialog` import 改回 `run.yigou.gxzy.manager.PickerLayoutManager`。
  - 移除 `library/base` 中新增的 `PickerLayoutManager.java`。

## 11. 原子化实施清单

1. 读取并确认 `app/src/main/java/run/yigou/gxzy/manager/PickerLayoutManager.java` 当前内容。
2. 在 `library/base/src/main/java/com/hjq/base/PickerLayoutManager.java` 新增迁移后的类，包名改为 `com.hjq.base`，其余 API 和逻辑保持不变。
3. 修改 `DateDialog.java` 的 `PickerLayoutManager` import 为 `com.hjq.base.PickerLayoutManager`。
4. 修改 `TimeDialog.java` 的 `PickerLayoutManager` import 为 `com.hjq.base.PickerLayoutManager`。
5. 删除 app 原 `PickerLayoutManager.java`。
6. 更新 `docs/ui-module-migration-plan.md`，记录批次 4 范围、实际改动、验证方式、阻塞和 ADR。
7. 执行静态搜索，确认旧 import 和旧文件已移除，且 `library/base` 无 app 反向依赖。
8. 运行 `gradlew.bat assembleDebug`。
9. 若构建通过，记录验证结果；若构建失败，停止并回到计划模式，不擅自补依赖。
