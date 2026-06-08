# 阶段 2 批次 5：AppAdapter 与 SingleClick 注解公共化计划

## 1. 目标

在不迁移剩余 Dialog / Popup、不改媒体 / AI Chat / Reader / Account 业务边界的前提下，解除 `library/ui-dialog` 后续迁移对 app 内 `AppAdapter` 和 `SingleClick` 注解的依赖阻塞。

本批次只做两个公共依赖抽取：

1. 将 `AppAdapter` 从 app 移入 `library/base`，包名改为 `com.hjq.base`。
2. 将 `SingleClick` 注解从 app 移入 `library/base` 的既有 `action` 包，包名改为 `com.hjq.base.action`。

`SingleClickAspect` 切面类仍保留在 app 模块，AspectJ 插件和 Gradle 配置不迁移、不新增、不改版本；仅把切面 Pointcut 和类型引用改为新的公共注解包。

## 2. 已确认事实

- `app/src/main/java/run/yigou/gxzy/app/AppAdapter.java`
  - 仅依赖 Android SDK、AndroidX 注解、`com.hjq.base.BaseAdapter` 和 Java 集合。
  - 无 `run.yigou.gxzy.R`、业务模型、全局状态、网络、数据库或 app 初始化依赖。
  - 当前有 27 个源码 import，覆盖通用 Dialog/Popup、业务 Dialog、媒体 Adapter、阅读/Tips Adapter、AI Chat Adapter 和 Demo Adapter。
- `app/src/main/java/run/yigou/gxzy/aop/SingleClick.java`
  - 仅是运行时方法注解。
  - 无 app 资源、业务模型、工具类或 Android 依赖。
  - 当前有 26 个源码 import。
- `app/src/main/java/run/yigou/gxzy/aop/SingleClickAspect.java`
  - 使用 AspectJ：`@Aspect`、`@Pointcut`、`@Around`。
  - 当前 Pointcut：`execution(@run.yigou.gxzy.aop.SingleClick * *(..))`。
  - 依赖 `timber.log.Timber`，因此不迁入 `library/base`。
- `app/build.gradle`
  - 当前应用 `android-aspectjx` 插件。
  - `aspectjx.include android.defaultConfig.applicationId`，只对 `run.yigou.gxzy` 包名范围做 AOP 处理。
  - app 已依赖 `project(':library:base')`。
- `library/ui-dialog/build.gradle`
  - 已依赖 `project(':library:base')`。
  - 后续 Dialog/Popup 迁移可直接引用 `com.hjq.base.AppAdapter` 和 `com.hjq.base.action.SingleClick`。
- `library/base/src/main/java/com/hjq/base/`
  - 已有 `BaseAdapter.java`、`PickerLayoutManager.java` 等共享类。
  - 已有 `action/` 包，适合放置 `SingleClick` 注解。

## 3. 架构概述

本批次采用“公共注解 + app 切面”的方式，避免 `library:* -> app`：

```text
library/base
 ├─ com.hjq.base.AppAdapter
 └─ com.hjq.base.action.SingleClick

app
 ├─ app / ui / feature 代码
 │   ├─ import com.hjq.base.AppAdapter
 │   └─ import com.hjq.base.action.SingleClick
 └─ run.yigou.gxzy.aop.SingleClickAspect
     └─ Pointcut: execution(@com.hjq.base.action.SingleClick * *(..))

library/ui-dialog
 └─ 后续可引用 AppAdapter / SingleClick，不依赖 app
```

依赖方向：

- 允许：`app -> library:base`
- 允许：`library:ui-dialog -> library:base`
- 禁止：`library:base -> app`
- 禁止：`library:ui-dialog -> app`

## 4. 文件路径与组件关系

### 4.1 新增文件

- 新增：`library/base/src/main/java/com/hjq/base/AppAdapter.java`
  - 包名：`com.hjq.base`
  - 类名保持：`AppAdapter`
  - 公开 API 保持不变：
    - `AppAdapter(@NonNull Context context)`
    - `getItemCount()`
    - `getCount()`
    - `setData(@Nullable List<T> data)`
    - `getData()`
    - `addData(List<T> data)`
    - `clearData()`
    - `containsItem(int position)`
    - `containsItem(T item)`
    - `getItem(int position)`
    - `setItem(int position, T item)`
    - `addItem(T item)`
    - `addItem(int position, T item)`
    - `removeItem(T item)`
    - `removeItem(int position)`
    - `getPageNumber()` / `setPageNumber(int number)`
    - `isLastPage()` / `setLastPage(boolean last)`
    - `getTag()` / `setTag(Object tag)`
    - `final class SimpleHolder extends ViewHolder`

- 新增：`library/base/src/main/java/com/hjq/base/action/SingleClick.java`
  - 包名：`com.hjq.base.action`
  - 注解名保持：`SingleClick`
  - 注解策略保持：`@Retention(RetentionPolicy.RUNTIME)`
  - 注解目标保持：`@Target(ElementType.METHOD)`
  - 参数保持：`long value() default 1000`

### 4.2 删除文件

- 删除：`app/src/main/java/run/yigou/gxzy/app/AppAdapter.java`
- 删除：`app/src/main/java/run/yigou/gxzy/aop/SingleClick.java`

### 4.3 修改文件：AppAdapter import 切换

以下文件仅将：

```java
import run.yigou.gxzy.app.AppAdapter;
```

替换为：

```java
import com.hjq.base.AppAdapter;
```

文件清单：

- `app/src/main/java/run/yigou/gxzy/ui/popup/ListPopup.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/ShareDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/SelectDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/PayPasswordDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/MenuDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/AlbumDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/AddressDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/VideoSelectAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/TipsUnitFragmentAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/TipsFangYaoAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/TabAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/StatusAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/SearchHistoryAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/SearchBookDetailAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/SearchBookAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/NavigationAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/ImageSelectAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/ImagePreviewAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/GuideAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/CopyAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/BookInfoAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/adapter/BookCollectCaseAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/tips/adapter/TipsAiChatAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/tips/adapter/ChatSummaryAdapter.java`
- `app/src/main/java/run/yigou/gxzy/ui/tips/adapter/ChatHistoryAdapter.java`

### 4.4 修改文件：SingleClick import 切换

以下文件仅将：

```java
import run.yigou.gxzy.aop.SingleClick;
```

替换为：

```java
import com.hjq.base.action.SingleClick;
```

文件清单：

- `app/src/main/java/run/yigou/gxzy/ui/fragment/MyFragmentPersonal.java`
- `app/src/main/java/run/yigou/gxzy/ui/fragment/MineFragment.java`
- `app/src/main/java/run/yigou/gxzy/ui/fragment/MessageFragment.java`
- `app/src/main/java/run/yigou/gxzy/ui/fragment/HomeFragment.java`
- `app/src/main/java/run/yigou/gxzy/ui/fragment/FindFragment.java`
- `app/src/main/java/run/yigou/gxzy/ui/fragment/BookCollectCaseFragment.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/UpdateDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/SelectDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/SafeDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/PayPasswordDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/MenuDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/AddressDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/VideoSelectActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/SettingActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/RegisterActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/PhoneResetActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/PersonalDataActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/PasswordResetActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/PasswordForgetActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/LoginActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/ImageSelectActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/GuideActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/DialogActivity.java`
- `app/src/main/java/run/yigou/gxzy/ui/activity/CrashActivity.java`

### 4.5 修改文件：SingleClickAspect

- 修改：`app/src/main/java/run/yigou/gxzy/aop/SingleClickAspect.java`
  - 新增 import：`com.hjq.base.action.SingleClick`
  - Pointcut 从 `execution(@run.yigou.gxzy.aop.SingleClick * *(..))` 改为 `execution(@com.hjq.base.action.SingleClick * *(..))`
  - `aroundJoinPoint(ProceedingJoinPoint joinPoint, SingleClick singleClick)` 签名不变。
  - 切面类包名保持 `run.yigou.gxzy.aop`。
  - `Timber` 日志和防重复点击逻辑不改。

### 4.6 修改文件：迁移文档

- 修改：`docs/ui-module-migration-plan.md`
  - 当前进度更新为阶段 2 批次 5 已完成。
  - 当前批次控制更新本批次允许改动、排除范围、验证方式和完成条件。
  - 新增“批次 5 结果”。
  - 更新阻塞：
    - `AppAdapter` 阻塞解除。
    - `SingleClick` 注解公共化阻塞解除。
    - `SingleClickAspect` / AspectJ 插件仍在 app，作为运行时织入边界记录。
  - 新增验证记录。
  - 新增 ADR：`AppAdapter` 与 `SingleClick` 放入 `library/base`，Aspect 保留 app。

## 5. 数据结构与行为影响

- `AppAdapter` 内部数据结构保持不变：
  - `List<T> mDataSet`
  - `int mPageNumber`
  - `boolean mLastPage`
  - `Object mTag`
- `AppAdapter` 数据增删改查、分页标记、`SimpleHolder` 行为不变。
- `SingleClick` 注解语义不变：默认防重复点击间隔仍为 1000 ms。
- `SingleClickAspect` 防重复点击逻辑不变：仍按方法签名和参数拼接 tag，仍使用 `Timber` 记录快速点击。
- 当前 app 页面、业务 Dialog、媒体 Adapter、阅读/Tips Adapter、AI Chat Adapter 的运行逻辑不改，只替换 import。
- 不新增持久化数据结构。
- 不修改网络、数据库、Manifest、资源或用户数据。

## 6. 错误处理与边界条件

- 保留 `AppAdapter#getItem(int position)` 在 `mDataSet == null` 时返回 null 的既有行为。
- 保留 `AppAdapter#addData` 对 null / empty 数据直接返回的既有行为。
- 保留 `SingleClickAspect` 对重复点击直接 return、不执行原方法的既有行为。
- 不新增异常捕获或日志，避免改变 UI 点击热路径行为。
- 若构建失败原因涉及 AspectJ 插件、织入范围或 Gradle 依赖变更，停止执行并回到计划模式，不在本批次擅自调整 Gradle。

## 7. 依赖管理

- 不修改 `settings.gradle`。
- 不修改 `app/build.gradle`。
- 不修改 `library/base/build.gradle`。
- 不修改 `library/ui-dialog/build.gradle`。
- 不新增第三方依赖。
- 依据：
  - `library/base` 已通过 `common.gradle` 获得 AndroidX appcompat/material 基础依赖，可解析 `Context`、AndroidX 注解等现有依赖。
  - `AppAdapter` 依赖的 `BaseAdapter` 位于同一包 `com.hjq.base`。
  - `SingleClick` 注解仅依赖 Java 注解包。
  - app 已依赖 `library:base`，所有 app 侧 import 可解析。
  - `library/ui-dialog` 已依赖 `library:base`，后续迁移类可解析公共 Adapter 和注解。

## 8. 实施边界

### 允许改动范围

- `library/base/src/main/java/com/hjq/base/AppAdapter.java`
- `library/base/src/main/java/com/hjq/base/action/SingleClick.java`
- `app/src/main/java/run/yigou/gxzy/app/AppAdapter.java`
- `app/src/main/java/run/yigou/gxzy/aop/SingleClick.java`
- `app/src/main/java/run/yigou/gxzy/aop/SingleClickAspect.java`
- 第 4.3 节列出的 27 个 `AppAdapter` import 使用文件。
- 第 4.4 节列出的 26 个 `SingleClick` import 使用文件。
- `docs/ui-module-migration-plan.md`

### 排除范围

- 不迁移 `SingleClickAspect` 类到任何 library 模块。
- 不修改 AspectJ 插件、`aspectjx` 配置、`aspectjrt` 依赖或混淆配置。
- 不迁移 `MenuDialog`、`SelectDialog`、`DateDialog`、`TimeDialog`、`ListPopup` 到 `library/ui-dialog`。
- 不恢复或新增 `MessageDialog` / `InputDialog` 的 `@SingleClick`。
- 不修改 `CommonDialog` / `WaitDialog` / `MessageDialog` / `InputDialog` 已迁移结果。
- 不修改媒体模块、AI Chat、Reader/Tips、Account 的业务逻辑或目录结构。
- 不修改 Manifest、README、AGENTS.md。
- 不做 Gradle 依赖版本调整。
- 不做无关 refactor、格式化、命名调整或注释重写。

### 新文件落位

- 允许新增 2 个 Java 文件：
  - `library/base/src/main/java/com/hjq/base/AppAdapter.java`
  - `library/base/src/main/java/com/hjq/base/action/SingleClick.java`
- 不允许新增其他源码、资源、文档或目录。

### 文档同步

- 必须更新现有 `docs/ui-module-migration-plan.md`。
- 不新增平行文档。

## 9. 验证方法

执行后验证：

1. 静态搜索：
   - `app/src/main/java` 中不再存在 `import run.yigou.gxzy.app.AppAdapter;`
   - `app/src/main/java` 中不再存在 `import run.yigou.gxzy.aop.SingleClick;`
   - `app/src/main/java/run/yigou/gxzy/app/AppAdapter.java` 不存在。
   - `app/src/main/java/run/yigou/gxzy/aop/SingleClick.java` 不存在。
   - `library/base/src/main/java/com/hjq/base/AppAdapter.java` 存在且包名为 `com.hjq.base`。
   - `library/base/src/main/java/com/hjq/base/action/SingleClick.java` 存在且包名为 `com.hjq.base.action`。
2. 静态检查：
   - `library/base` 中不得出现 `run.yigou.gxzy.R` 或 `run.yigou.gxzy.app`。
   - `library/base` 中不得出现 `timber.log.Timber` 或 `org.aspectj`。
   - `SingleClickAspect` Pointcut 必须为 `execution(@com.hjq.base.action.SingleClick * *(..))`。
3. 构建验证：
   - 运行 `gradlew.bat assembleDebug`。
4. 文档一致性：
   - `docs/ui-module-migration-plan.md` 的当前批次、阻塞、验证记录、ADR 与实际改动一致。

## 10. 风险与回退

- 风险 1：`SingleClick` 注解包名变化后，如果 AspectJ 织入范围对依赖库处理方式与预期不同，可能出现防重复点击运行时未织入。当前批次只通过编译和静态 Pointcut 验证，完整运行时点击验证需后续手测。
- 风险 2：`AppAdapter` import 涉及 27 个文件，虽然只改 import，但覆盖多个后续阶段代码；执行时必须只做机械替换，不改业务逻辑。
- 风险 3：`MessageDialog` / `InputDialog` 已在批次 3 移除 `@SingleClick`，本批次不恢复它们的防重复点击能力。

回退方式：

- 恢复 app 原 `AppAdapter.java` 和 `SingleClick.java`。
- 删除 `library/base` 中新增的 `AppAdapter.java` 和 `action/SingleClick.java`。
- 将 27 个 `AppAdapter` import 改回 `run.yigou.gxzy.app.AppAdapter`。
- 将 26 个 `SingleClick` import 改回 `run.yigou.gxzy.aop.SingleClick`。
- 将 `SingleClickAspect` Pointcut 改回 `execution(@run.yigou.gxzy.aop.SingleClick * *(..))`。

## 11. 原子化实施清单

1. 读取并确认 `AppAdapter.java`、`SingleClick.java`、`SingleClickAspect.java` 当前内容。
2. 在 `library/base/src/main/java/com/hjq/base/AppAdapter.java` 新增迁移后的 `AppAdapter`，包名改为 `com.hjq.base`，其余 API 和逻辑保持不变。
3. 在 `library/base/src/main/java/com/hjq/base/action/SingleClick.java` 新增迁移后的 `SingleClick` 注解，包名改为 `com.hjq.base.action`，注解语义保持不变。
4. 将 27 个 `import run.yigou.gxzy.app.AppAdapter;` 批量替换为 `import com.hjq.base.AppAdapter;`。
5. 将 26 个 `import run.yigou.gxzy.aop.SingleClick;` 批量替换为 `import com.hjq.base.action.SingleClick;`。
6. 修改 `SingleClickAspect.java`：导入 `com.hjq.base.action.SingleClick`，并将 Pointcut 改为 `execution(@com.hjq.base.action.SingleClick * *(..))`。
7. 删除 app 原 `AppAdapter.java`。
8. 删除 app 原 `SingleClick.java`。
9. 更新 `docs/ui-module-migration-plan.md`，记录批次 5 范围、实际改动、验证方式、阻塞解除和 ADR。
10. 执行静态搜索，确认旧 import、旧文件已移除，且 `library/base` 无 app / AspectJ / Timber 依赖。
11. 运行 `gradlew.bat assembleDebug`。
12. 若构建通过，记录验证结果；若构建失败且涉及 AspectJ / Gradle / 依赖范围，停止并回到计划模式，不擅自补依赖或改配置。
