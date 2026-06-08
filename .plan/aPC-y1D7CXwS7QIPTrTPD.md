# 阶段 2 批次 6：迁移剩余通用 Dialog / Popup 计划

## 1. 目标

在阶段 2 前 5 个批次已完成 `library/ui-dialog` 模块接入、低风险 Dialog 迁移、`PickerLayoutManager` / `AppAdapter` / `SingleClick` 公共化之后，本批次迁移剩余通用 Dialog / Popup 到 `library/ui-dialog`：

- `MenuDialog`
- `SelectDialog`
- `DateDialog`
- `TimeDialog`
- `ListPopup`

本批次目标是解除阶段 2 通用 Dialog / Popup 的主要源码留存，继续保持：

- `library:*` 不依赖 `app`。
- 业务调用侧尽量不变。
- AspectJ 编织仍在 app 侧，不迁移 `SingleClickAspect`。
- 不迁移媒体、AI Chat、Reader/Tips、Account 或业务 Dialog。

## 2. 已确认事实

### 2.1 已完成前置条件

- `library/ui-dialog` 已存在，且 app 已依赖 `project(':library:ui-dialog')`。
- 已迁入 `library/ui-dialog` 的类保持原包名 `run.yigou.gxzy.ui.dialog`，并使用模块 R：`run.yigou.gxzy.ui.dialog.R`。
- `CommonDialog` 已在 `library/ui-dialog`，`SelectDialog` / `DateDialog` / `TimeDialog` 继承它。
- `AppAdapter` 已迁入 `library/base`：`com.hjq.base.AppAdapter`。
- `SingleClick` 注解已迁入 `library/base`：`com.hjq.base.action.SingleClick`。
- `PickerLayoutManager` 已迁入 `library/base`：`com.hjq.base.PickerLayoutManager`。
- `ArrowDrawable` 已位于 `library/base/src/main/java/com/hjq/base/ArrowDrawable.java`。
- `library/ui-dialog` 已依赖 `library:base` 和 `library:widget`。

### 2.2 候选类依赖

- `MenuDialog`
  - 依赖 `BaseDialog`、`BaseAdapter`、`AppAdapter`、`SingleClick`、`RecyclerView`、`menu_dialog.xml`、`menu_item.xml`。
  - 当前导入 `run.yigou.gxzy.R`，迁移后改为 `run.yigou.gxzy.ui.dialog.R`。
- `SelectDialog`
  - 依赖 `CommonDialog`、`BaseAdapter`、`AppAdapter`、`SingleClick`、`Toaster`、`select_dialog.xml`、`select_item.xml`、`checkbox_selector.xml`。
  - 当前导入 `run.yigou.gxzy.R`，迁移后改为 `run.yigou.gxzy.ui.dialog.R`。
  - 需要 `library/ui-dialog` 直接声明 Toaster 依赖，避免通过 app 反向获得。
- `DateDialog`
  - 依赖 `CommonDialog`、`AppAdapter`、`SingleClick`、`PickerLayoutManager`、`date_dialog.xml`、`picker_item.xml`。
  - 当前导入 `run.yigou.gxzy.R`，迁移后改为 `run.yigou.gxzy.ui.dialog.R`。
- `TimeDialog`
  - 依赖 `CommonDialog`、`AppAdapter`、`SingleClick`、`PickerLayoutManager`、`time_dialog.xml`、`picker_item.xml`。
  - 当前导入 `run.yigou.gxzy.R`，迁移后改为 `run.yigou.gxzy.ui.dialog.R`。
- `ListPopup`
  - 依赖 `BasePopupWindow`、`BaseAdapter`、`AnimAction`、`AppAdapter`、`ArrowDrawable`。
  - 当前包名为 `run.yigou.gxzy.ui.popup`。
  - 迁移后保持包名 `run.yigou.gxzy.ui.popup`，但导入 `run.yigou.gxzy.ui.dialog.R` 以引用 `library/ui-dialog` 模块 R。

### 2.3 调用侧引用

已发现调用侧 import / 使用：

- `StatusActivity.java` 使用 `MenuDialog`。
- `SettingActivity.java` 使用 `MenuDialog`。
- `DialogActivity.java` 使用 `MenuDialog`、`SelectDialog`、`DateDialog`、`TimeDialog`、`ListPopup`。
- `TipsNetHelper.java` 使用 `MenuDialog`。

由于迁移保持原包名，调用侧 import 不需要改动；本批次不迁移这些调用侧业务类。

### 2.4 资源来源

候选类和布局需要迁入 `library/ui-dialog` 的最小资源：

- layout：
  - `menu_dialog.xml`
  - `menu_item.xml`
  - `select_dialog.xml`
  - `select_item.xml`
  - `date_dialog.xml`
  - `time_dialog.xml`
  - `picker_item.xml`
- drawable：
  - `checkbox_selector.xml`
  - `checkbox_checked_ic.xml`
  - `checkbox_disable_ic.xml`
  - `compound_normal_ic.xml`

已存在于 `library/ui-dialog` 或其依赖中的资源：

- `transparent_selector.xml` 已在 `library/ui-dialog`。
- `black`、`black80`、`black50`、`black5`、`transparent` 已由 `library/base` 的 `colors.xml` 提供。
- `line_size`、`dialog_ui_round_size`、`dp_10`、`dp_15`、`sp_14` 已在 `library/ui-dialog`。
- `common_cancel`、`common_confirm`、`common_loading` 已在 `library/ui-dialog`。
- `common_line_color`、`common_confirm_text_color`、`common_cancel_text_color`、`white` 已在 `library/ui-dialog`。

需要补齐到 `library/ui-dialog` 的最小 values：

- `strings.xml`
  - `common_year` = `年`
  - `common_month` = `月`
  - `common_day` = `日`
  - `common_hour` = `时`
  - `common_minute` = `分`
  - `common_second` = `秒`
  - `time_title` = `请选择时间`
  - `select_min_hint` = `至少要选择 %d 项`
  - `select_max_hint` = `最多只能选择 %d 项`
- `dimens.xml`
  - `dp_12` = `12dp`
  - `dp_13` = `13dp`
  - `dp_20` = `20dp`
  - `dp_24` = `24dp`
  - `dp_280` = `280dp`
  - `sp_15` = `15sp`
  - `sp_16` = `16sp`
  - `sp_18` = `18sp`
- `colors.xml`
  - `common_accent_color` = `#5A8DDF`

## 3. 架构概述

批次 6 完成后：

```text
library/base
 ├─ BaseDialog / BasePopupWindow / BaseAdapter
 ├─ AppAdapter
 ├─ PickerLayoutManager
 ├─ ArrowDrawable
 └─ action/SingleClick

library/ui-dialog
 ├─ run.yigou.gxzy.ui.dialog.CommonDialog
 ├─ run.yigou.gxzy.ui.dialog.WaitDialog
 ├─ run.yigou.gxzy.ui.dialog.MessageDialog
 ├─ run.yigou.gxzy.ui.dialog.InputDialog
 ├─ run.yigou.gxzy.ui.dialog.MenuDialog
 ├─ run.yigou.gxzy.ui.dialog.SelectDialog
 ├─ run.yigou.gxzy.ui.dialog.DateDialog
 ├─ run.yigou.gxzy.ui.dialog.TimeDialog
 └─ run.yigou.gxzy.ui.popup.ListPopup

app
 ├─ 业务 Activity / Fragment / Helper 继续 import 原包名
 └─ run.yigou.gxzy.aop.SingleClickAspect 继续 app 侧织入
```

依赖方向：

- 允许：`app -> library:ui-dialog`
- 允许：`library:ui-dialog -> library:base`
- 允许：`library:ui-dialog -> library:widget`
- 允许：`library:ui-dialog -> com.github.getActivity:Toaster`
- 禁止：`library:ui-dialog -> app`
- 禁止：`library:base -> app`

## 4. 文件路径与组件关系

### 4.1 新增 / 迁移 Java 文件

从 app 迁移到 `library/ui-dialog`：

- `app/src/main/java/run/yigou/gxzy/ui/dialog/MenuDialog.java`
  -> `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/MenuDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/SelectDialog.java`
  -> `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/SelectDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
  -> `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
  -> `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/popup/ListPopup.java`
  -> `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/popup/ListPopup.java`

包名保持：

- Dialog：`package run.yigou.gxzy.ui.dialog;`
- Popup：`package run.yigou.gxzy.ui.popup;`

R import 规则：

- `MenuDialog`、`SelectDialog`、`DateDialog`、`TimeDialog`：
  - 删除 `import run.yigou.gxzy.R;`
  - 新增 `import run.yigou.gxzy.ui.dialog.R;`
- `ListPopup`：
  - 删除 `import run.yigou.gxzy.R;`
  - 新增 `import run.yigou.gxzy.ui.dialog.R;`

其他 import 保持现状，除非编译要求排序或去重。

### 4.2 删除 app 原 Java 文件

迁移后删除 app 原源码：

- `app/src/main/java/run/yigou/gxzy/ui/dialog/MenuDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/SelectDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
- `app/src/main/java/run/yigou/gxzy/ui/popup/ListPopup.java`

### 4.3 迁移资源文件

从 app 迁移到 `library/ui-dialog`：

- `app/src/main/res/layout/menu_dialog.xml`
  -> `library/ui-dialog/src/main/res/layout/menu_dialog.xml`
- `app/src/main/res/layout/menu_item.xml`
  -> `library/ui-dialog/src/main/res/layout/menu_item.xml`
- `app/src/main/res/layout/select_dialog.xml`
  -> `library/ui-dialog/src/main/res/layout/select_dialog.xml`
- `app/src/main/res/layout/select_item.xml`
  -> `library/ui-dialog/src/main/res/layout/select_item.xml`
- `app/src/main/res/layout/date_dialog.xml`
  -> `library/ui-dialog/src/main/res/layout/date_dialog.xml`
- `app/src/main/res/layout/time_dialog.xml`
  -> `library/ui-dialog/src/main/res/layout/time_dialog.xml`
- `app/src/main/res/layout/picker_item.xml`
  -> `library/ui-dialog/src/main/res/layout/picker_item.xml`
- `app/src/main/res/drawable/checkbox_selector.xml`
  -> `library/ui-dialog/src/main/res/drawable/checkbox_selector.xml`
- `app/src/main/res/drawable/checkbox_checked_ic.xml`
  -> `library/ui-dialog/src/main/res/drawable/checkbox_checked_ic.xml`
- `app/src/main/res/drawable/checkbox_disable_ic.xml`
  -> `library/ui-dialog/src/main/res/drawable/checkbox_disable_ic.xml`
- `app/src/main/res/drawable/compound_normal_ic.xml`
  -> `library/ui-dialog/src/main/res/drawable/compound_normal_ic.xml`

`transparent_selector.xml` 已存在于 `library/ui-dialog`，不重复新增；如后续发现内容不一致，不在本批次重写，只以可编译为目标。

### 4.4 删除 app 原资源文件

删除 app 原通用 Dialog / Popup 资源：

- `app/src/main/res/layout/menu_dialog.xml`
- `app/src/main/res/layout/menu_item.xml`
- `app/src/main/res/layout/select_dialog.xml`
- `app/src/main/res/layout/select_item.xml`
- `app/src/main/res/layout/date_dialog.xml`
- `app/src/main/res/layout/time_dialog.xml`

`picker_item.xml` 在 app 中还被 `unit_fragment.xml`、`status_fragment.xml`、`book_info_fragment.xml` 的 `tools:listitem` 引用；这些是设计期 tools 引用。本批次不迁移 Reader/Tips 或 Demo Fragment，因此不删除 app 原 `picker_item.xml`，避免影响布局预览或非迁移范围资源关系。

`checkbox_selector.xml` 及其 3 个图标先不从 app 删除，除非执行时静态搜索确认 app 源码和 app 资源不再直接引用它们。若仍有 app 侧引用，保留 app 原资源，模块内复制一份用于 `library/ui-dialog` 编译。

### 4.5 修改 `library/ui-dialog` values

- 修改：`library/ui-dialog/src/main/res/values/strings.xml`
  - 增加批次 6 所需 9 个字符串。
- 修改：`library/ui-dialog/src/main/res/values/dimens.xml`
  - 增加批次 6 所需 8 个尺寸。
- 修改：`library/ui-dialog/src/main/res/values/colors.xml`
  - 增加 `common_accent_color`。

### 4.6 修改 Gradle 依赖

- 修改：`library/ui-dialog/build.gradle`
  - 增加 Toaster 依赖，用于 `SelectDialog` 中 `com.hjq.toast.Toaster`。
  - 依赖写法优先按现有项目传统 Gradle 风格与 app 保持版本一致：
    - `implementation 'com.github.getActivity:Toaster:12.6'`
  - 不使用 `libs.toaster`，避免混用版本目录和现有模块的传统依赖风格。

不修改：

- `settings.gradle`
- `app/build.gradle`
- 根 `build.gradle`
- `common.gradle`
- `gradle/libs.versions.toml`

### 4.7 修改迁移文档

- 修改：`docs/ui-module-migration-plan.md`
  - 当前进度更新为阶段 2 批次 6 已完成或执行中。
  - 当前批次控制更新允许改动、排除范围、验证方式和完成条件。
  - 新增“批次 6 结果”。
  - 记录 `MenuDialog`、`SelectDialog`、`DateDialog`、`TimeDialog`、`ListPopup` 实际迁移路径。
  - 记录 Toaster 依赖补充。
  - 记录资源复制/保留策略。
  - 新增验证记录。
  - 新增 ADR：剩余通用 Dialog / Popup 迁入 `library/ui-dialog`，保留包名与 app 侧 AspectJ 编织。

## 5. 类 / 函数 / API 变更

本批次不改变对外 API。

以下 Builder / Listener 结构保持不变：

- `MenuDialog.Builder`
  - `setGravity(int gravity)`
  - `setList(int... ids)`
  - `setList(String... data)`
  - `setList(List data)`
  - `setCancel(@StringRes int id)`
  - `setCancel(CharSequence text)`
  - `setAutoDismiss(boolean dismiss)`
  - `setListener(OnListener listener)`
  - `onClick(View view)`
  - `onItemClick(RecyclerView recyclerView, View itemView, int position)`
  - `MenuDialog.OnListener<T>`
- `SelectDialog.Builder`
  - `setList(...)`
  - `setSelect(int... positions)`
  - `setMaxSelect(int count)`
  - `setMinSelect(int count)`
  - `setSingleSelect()`
  - `setListener(OnListener listener)`
  - `onClick(View view)`
  - `SelectDialog.OnListener<T>`
- `DateDialog.Builder`
  - 构造函数重载保持不变。
  - `setListener(OnListener listener)`
  - `setIgnoreDay()`
  - `setDate(long date)`
  - `setDate(String date)`
  - `setYear(...)` / `setMonth(...)` / `setDay(...)`
  - `onClick(View view)`
  - `onPicked(RecyclerView recyclerView, int position)`
  - `DateDialog.OnListener`
- `TimeDialog.Builder`
  - `setListener(OnListener listener)`
  - `setIgnoreSecond()`
  - `setTime(String time)`
  - `setHour(...)` / `setMinute(...)` / `setSecond(...)`
  - `onClick(View view)`
  - `TimeDialog.OnListener`
- `ListPopup.Builder`
  - `setGravity(int gravity)`
  - `setList(...)`
  - `setAutoDismiss(boolean dismiss)`
  - `setListener(OnListener listener)`
  - `onItemClick(RecyclerView recyclerView, View itemView, int position)`
  - `ListPopup.OnListener<T>`

## 6. 数据结构与行为影响

- 不新增持久化数据结构。
- 不修改 Dialog / Popup 的数据选择逻辑。
- 不修改 `AppAdapter` 数据结构。
- 不修改 `PickerLayoutManager` 滚动选择逻辑。
- 不修改 `SingleClick` 注解语义。
- `SelectDialog` 的 Toaster 行为保持原实现。
- `MenuDialog`、`DateDialog`、`TimeDialog` 的 `@SingleClick` 注解保留；注解来自 `library/base`。
- 由于 `SingleClickAspect` 的 `aspectjx.include` 当前只包含 app applicationId，迁入 `library/ui-dialog` 后这些库内 `@SingleClick` 是否被运行时织入存在不确定性；本批次只保证编译和静态依赖正确，运行时防重复点击需后续手测或单独规划 AspectJ 跨模块策略。

## 7. 错误处理与边界条件

- 保留原有异常和返回行为。
- 不新增 try/catch。
- 不新增日志，避免改变 UI 高频点击路径。
- 若构建失败原因是缺失资源，允许在本批次范围内补齐对应 `library/ui-dialog` 的最小资源。
- 若构建失败原因涉及 AspectJ 插件、织入范围、Manifest、业务资源归属或跨模块依赖新增超过本计划，停止执行并回到计划模式。
- 若发现某个候选 Dialog / Popup 引入 app 业务模型、app 单例、数据库、网络或业务资源，停止该类迁移并回到计划模式，不做局部绕过。

## 8. 依赖管理

`library/ui-dialog/build.gradle` 执行后依赖保持为：

- `implementation project(':library:base')`
- `implementation project(':library:widget')`
- `implementation 'com.airbnb.android:lottie:4.1.0'`
- `implementation 'com.github.getActivity:Toaster:12.6'`

不新增其他第三方依赖。

依据：

- `AppAdapter`、`PickerLayoutManager`、`ArrowDrawable`、`SingleClick` 均来自 `library/base`。
- `SmartTextView` 来自 `library/widget`。
- `SelectDialog` 直接使用 `Toaster`，因此 `library/ui-dialog` 必须直接声明 Toaster，不能依赖 app 间接提供。
- `library/ui-dialog` 已通过 `common.gradle` 获得 AndroidX appcompat/material 基础依赖。

## 9. 实施边界

### 9.1 允许改动范围

Java：

- `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/MenuDialog.java`
- `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/SelectDialog.java`
- `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
- `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
- `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/popup/ListPopup.java`
- app 原对应 5 个 Java 文件仅用于删除。

资源：

- `library/ui-dialog/src/main/res/layout/menu_dialog.xml`
- `library/ui-dialog/src/main/res/layout/menu_item.xml`
- `library/ui-dialog/src/main/res/layout/select_dialog.xml`
- `library/ui-dialog/src/main/res/layout/select_item.xml`
- `library/ui-dialog/src/main/res/layout/date_dialog.xml`
- `library/ui-dialog/src/main/res/layout/time_dialog.xml`
- `library/ui-dialog/src/main/res/layout/picker_item.xml`
- `library/ui-dialog/src/main/res/drawable/checkbox_selector.xml`
- `library/ui-dialog/src/main/res/drawable/checkbox_checked_ic.xml`
- `library/ui-dialog/src/main/res/drawable/checkbox_disable_ic.xml`
- `library/ui-dialog/src/main/res/drawable/compound_normal_ic.xml`
- `library/ui-dialog/src/main/res/values/strings.xml`
- `library/ui-dialog/src/main/res/values/dimens.xml`
- `library/ui-dialog/src/main/res/values/colors.xml`
- app 原对应布局资源按第 4.4 节规则删除或保留。

Gradle / 文档：

- `library/ui-dialog/build.gradle`
- `docs/ui-module-migration-plan.md`

### 9.2 排除范围

不修改：

- `SingleClickAspect`、AspectJ 插件、`aspectjx` 配置、`aspectjrt` 依赖。
- `settings.gradle`。
- `app/build.gradle`。
- 根 `build.gradle`、`common.gradle`、`gradle/libs.versions.toml`。
- Manifest。
- README、AGENTS.md。
- 媒体模块、AI Chat、Reader/Tips、Account。
- `AlbumDialog`、`ShareDialog`、`UpdateDialog`、`PayPasswordDialog`、`SafeDialog`、`AddressDialog`、`AgreementDialog`、`TipsDialog`、`CopyDialog`、`ChatSummaryListDialog`。
- `DialogActivity`、`StatusActivity`、`SettingActivity`、`TipsNetHelper` 等调用侧业务逻辑。
- Demo / 模板残留处理。

### 9.3 是否允许新增文件

允许新增文件仅限迁移目标落位：

- 5 个 Java 文件迁入 `library/ui-dialog`。
- 7 个 layout 文件迁入 `library/ui-dialog`。
- 4 个 drawable 文件迁入 `library/ui-dialog`。

不允许新增其他源码、资源、文档或目录。

### 9.4 新文件落位

- Dialog 继续放在：`library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/`
- Popup 放在：`library/ui-dialog/src/main/java/run/yigou/gxzy/ui/popup/`
- Layout 放在：`library/ui-dialog/src/main/res/layout/`
- Drawable 放在：`library/ui-dialog/src/main/res/drawable/`
- Values 继续合并到现有：`library/ui-dialog/src/main/res/values/`

### 9.5 文档同步

必须更新现有 `docs/ui-module-migration-plan.md`，不新增平行文档。

## 10. 验证方法

### 10.1 静态检查

执行后检查：

1. app 原 Java 文件不存在：
   - `app/src/main/java/run/yigou/gxzy/ui/dialog/MenuDialog.java`
   - `app/src/main/java/run/yigou/gxzy/ui/dialog/SelectDialog.java`
   - `app/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`
   - `app/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`
   - `app/src/main/java/run/yigou/gxzy/ui/popup/ListPopup.java`
2. `library/ui-dialog` 中存在迁移后 Java 文件。
3. 迁移后 Java 文件不再导入 `run.yigou.gxzy.R`。
4. 迁移后 Java 文件使用 `run.yigou.gxzy.ui.dialog.R`。
5. `library/ui-dialog` 中无以下 app 反向依赖：
   - `run.yigou.gxzy.R`
   - `run.yigou.gxzy.app`
   - `run.yigou.gxzy.aop`
6. `library/ui-dialog` 中允许出现：
   - `com.hjq.base.AppAdapter`
   - `com.hjq.base.PickerLayoutManager`
   - `com.hjq.base.action.SingleClick`
   - `com.hjq.toast.Toaster`
7. `library/ui-dialog/src/main/res` 中存在本批次新增 layout / drawable。
8. `library/ui-dialog/build.gradle` 包含 Toaster 依赖。
9. app 调用侧 import `run.yigou.gxzy.ui.dialog.*` / `run.yigou.gxzy.ui.popup.ListPopup` 不需要改动且仍可解析。
10. app 原布局资源删除仅限允许范围；`picker_item.xml` 和 checkbox 相关 drawable 如仍被 app 直接引用则保留。

### 10.2 构建验证

运行：

```bat
gradlew.bat assembleDebug
```

通过标准：

- exitCode = 0
- 输出包含 `BUILD SUCCESSFUL`

现有 AGP / Manifest / AspectJ D8 / deprecation warning 不在本批次处理范围。

### 10.3 文档一致性

检查 `docs/ui-module-migration-plan.md`：

- 当前进度、当前批次控制、批次 6 结果、验证记录、ADR 与实际改动一致。
- 不保留“剩余通用 Dialog / Popup 待迁移”的过时阻塞描述。
- 明确记录运行时 AOP 编织仍需手测或后续单独规划。

## 11. 风险与回退

### 11.1 风险

- 风险 1：`@SingleClick` 注解随类迁入 `library/ui-dialog` 后，当前 app 侧 AspectJ include 范围可能不织入 library class，运行时防重复点击可能不生效。本批次不改 AspectJ 配置。
- 风险 2：`SelectDialog` 增加 Toaster 依赖到 `library/ui-dialog`，若版本或仓库解析异常会导致构建失败；本计划使用 app 已验证版本 `12.6`。
- 风险 3：`ListPopup` 包名与模块 R 包不同，必须显式导入 `run.yigou.gxzy.ui.dialog.R`，否则资源无法解析。
- 风险 4：`picker_item.xml` 被 app 其他布局的 tools 引用，本批次不删除 app 原文件以降低非目标影响。
- 风险 5：checkbox drawable 若被 app 其他页面引用，删除会破坏 app 资源；执行时需先静态确认，不能盲删。

### 11.2 回退

若构建失败且无法在本批次边界内修复：

1. 恢复 5 个 Java 文件到 app 原路径。
2. 删除 `library/ui-dialog` 中新增的 5 个 Java 文件。
3. 恢复 app 原 layout / drawable 资源。
4. 删除 `library/ui-dialog` 中本批次新增 layout / drawable。
5. 移除 `library/ui-dialog/build.gradle` 中 Toaster 依赖。
6. 还原 `library/ui-dialog` values 增量。
7. 还原迁移文档批次 6 记录。

## 12. 原子化实施清单

1. 创建执行任务并标记为 in_progress。
2. 读取本计划、`docs/ui-module-migration-plan.md`、5 个候选 Java、相关 layout/drawable/values、`library/ui-dialog/build.gradle` 当前内容。
3. 在 `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/` 新增 `MenuDialog.java`，保持包名和 API，R import 改为 `run.yigou.gxzy.ui.dialog.R`。
4. 在 `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/` 新增 `SelectDialog.java`，保持包名和 API，R import 改为 `run.yigou.gxzy.ui.dialog.R`。
5. 在 `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/` 新增 `DateDialog.java`，保持包名和 API，R import 改为 `run.yigou.gxzy.ui.dialog.R`。
6. 在 `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/` 新增 `TimeDialog.java`，保持包名和 API，R import 改为 `run.yigou.gxzy.ui.dialog.R`。
7. 在 `library/ui-dialog/src/main/java/run/yigou/gxzy/ui/popup/` 新增 `ListPopup.java`，保持包名和 API，R import 改为 `run.yigou.gxzy.ui.dialog.R`。
8. 复制 7 个 layout 到 `library/ui-dialog/src/main/res/layout/`。
9. 复制 `checkbox_selector.xml`、`checkbox_checked_ic.xml`、`checkbox_disable_ic.xml`、`compound_normal_ic.xml` 到 `library/ui-dialog/src/main/res/drawable/`。
10. 修改 `library/ui-dialog/src/main/res/values/strings.xml`，补齐批次 6 所需字符串。
11. 修改 `library/ui-dialog/src/main/res/values/dimens.xml`，补齐批次 6 所需尺寸。
12. 修改 `library/ui-dialog/src/main/res/values/colors.xml`，补齐 `common_accent_color`。
13. 修改 `library/ui-dialog/build.gradle`，新增 `implementation 'com.github.getActivity:Toaster:12.6'`。
14. 删除 app 原 5 个 Java 文件。
15. 删除 app 原 6 个通用 Dialog layout：`menu_dialog.xml`、`menu_item.xml`、`select_dialog.xml`、`select_item.xml`、`date_dialog.xml`、`time_dialog.xml`。
16. 保留 app 原 `picker_item.xml`。
17. 静态搜索 checkbox drawable 是否仍被 app 直接引用；若无直接引用，可删除 app 原 checkbox 相关 4 个 drawable；若有引用，则保留 app 原文件。
18. 更新 `docs/ui-module-migration-plan.md`，记录批次 6 范围、实际迁移、资源策略、Toaster 依赖、验证方式、AOP 风险和 ADR。
19. 执行静态检查，确认 `library/ui-dialog` 无 app 反向依赖，迁移文件 R import 正确，app 原 Java 已删除。
20. 运行 `gradlew.bat assembleDebug`。
21. 若构建成功，更新文档验证记录中的实际构建结果；若构建失败且原因超出本计划，停止并回到计划模式。
22. 标记执行任务 completed，并汇总变更、验证结果和剩余风险。
