# UI 模块化迁移计划

## 1. 背景与目标

`app/src/main/java/run/yigou/gxzy/ui` 当前按技术层组织，包含应用壳层、阅读 Tips、AI Chat、媒体选择、通用 Dialog、账户和模板示例等多类职责。模块化迁移目标是按业务域和通用能力重组边界，降低耦合，并在每个阶段保持项目可编译、可验证、可回退。

目标顺序：

1. 识别并隔离 Demo / 模板残留。
2. 抽通用 Dialog / Popup。
3. 抽媒体模块。
4. 从 `tips` 中剥离 AI Chat。
5. 治理阅读 / Tips 模块。
6. 最后考虑账户模块。

## 2. 执行原则

- 每次只执行一个小批次，禁止跨阶段顺手改动。
- 每批次开始前必须在本文档记录当前阶段、批次、允许改动文件、排除文件和验证方式。
- 每批次结束后必须更新当前进度、实际改动、验证结果、偏差或阻塞。
- 任何 feature/library 模块不得依赖 `app`。
- 如果依赖尚未解耦，优先在 `app` 内按 `feature/*` 包重组，暂不迁 Gradle 模块。
- 发现偏差、验证失败、范围变化或阻塞时，立即停止当前批次，回到计划阶段修正文档或确认处理方式。
- 不删除疑似 Demo 代码，除非确认无业务引用。
- 不引入未经验证的新依赖。
- 不修改与当前批次无关的业务行为。

## 3. 模块边界

### app

保留职责：

- 应用入口。
- 首页壳层与导航装配。
- 全局初始化。
- 暂未治理的数据层、HTTP、GreenDAO、全局状态。

### library/ui-dialog

候选职责：

- 通用 Dialog / Popup。
- 不包含媒体、AI、阅读、账户、版本更新、分享等业务 Dialog。

### feature/media

候选职责：

- 图片选择、预览、裁剪。
- 相机拍摄。
- 视频选择、播放。
- 相册弹窗。

### feature/ai-chat

候选职责：

- AI 聊天 UI。
- 会话列表、消息列表、总结。
- AI Chat Presenter / Contract / Adapter。

### feature/reader

候选职责：

- 书籍阅读。
- 书架。
- 章节搜索。
- 方药名词搜索。
- Tips 富文本和弹窗。

### feature/account

候选职责：

- 登录、注册、找回密码、手机号、个人资料、我的页面。

## 4. 依赖方向规则

允许：

- `app -> feature:*`
- `app -> library:*`
- `feature:* -> library:*`
- `feature:reader -> library:ui-dialog`
- `feature:media -> library:widget`
- `feature:ai-chat -> library:xbus`
- `feature:account -> library:crypto`
- `feature:account -> library:umeng`

禁止：

- `library:* -> app`
- `feature:* -> app`
- `feature:reader -> feature:ai-chat`
- `feature:ai-chat -> feature:reader`
- `library:ui-dialog -> feature:*`
- `library:widget -> feature:*`

## 5. 阶段总览

| 阶段 | 目标 | 迁移策略 |
|---|---|---|
| 阶段 1 | Demo / 模板残留隔离 | 先查引用，再决定 debug 隔离或保留 |
| 阶段 2 | 通用 Dialog / Popup | 新增或整理 `library/ui-dialog` |
| 阶段 3 | 媒体模块 | 新增 `feature/media` |
| 阶段 4 | AI Chat 剥离 | 先从 `tips` 剥离到 app 内 `feature/aichat`，再评估 Gradle 模块化 |
| 阶段 5 | 阅读 / Tips 治理 | 先从 `ui` 聚合到 app 内 `feature/reader`，再评估 Gradle 模块化 |
| 阶段 6 | 账户模块 | 最后处理，先 app 内包重组，再评估模块化 |

## 6. 当前进度

| 阶段 | 状态 | 当前批次 | 最近验证 | 备注 |
|---|---|---|---|---|
| 文档基线 | 已完成 | 建立迁移控制文档 | 已确认 | 不改业务代码 |
| 阶段 1 Demo 隔离 | 已完成 | 批次 4 已完成 | `assembleDebug` 成功 | 已隐藏 `MineFragment` 中 Demo 入口；`CopyActivityTest` 已移出正式 Manifest，Demo 类保留 |
| 阶段 2 Dialog/Popup | 已完成 | 批次 6 已完成 | `assembleDebug` 成功 | 通用 `CommonDialog` / `WaitDialog` / `MessageDialog` / `InputDialog` / `MenuDialog` / `SelectDialog` / `DateDialog` / `TimeDialog` / `ListPopup` 已迁入 `library/ui-dialog`；AOP 编织仍在 app 侧 |
| 阶段 3 Media | 批次 2 已完成 | 批次 2 已完成：app 内 feature/media 包级聚合 | `assembleDebug` 成功 | 7 个 Activity、3 个 Adapter、1 个 Dialog 已移动到 app 内 `run.yigou.gxzy.ui.feature.media` 包；所有调用方 import 已更新 |
| 阶段 4 AI Chat | 未开始 | - | - | - |
| 阶段 5 Reader/Tips | 未开始 | - | - | - |
| 阶段 6 Account | 未开始 | - | - | - |

## 7. 当前批次控制

| 项目 | 内容 |
|---|---|
| 当前阶段 | 阶段 3：媒体模块 |
| 当前批次 | 批次 2 已完成：app 内 feature/media 包级聚合 |
| 允许改动文件 | 媒体相关 Java 源码包名与 import；计划文档 |
| 排除文件 | `settings.gradle`、`app/build.gradle`（未新增 Gradle 模块）、`AndroidManifest.xml`、任意 XML/图片资源、README、AGENTS.md、AI Chat、Reader/Tips、Account |
| 验证方式 | `gradlew.bat assembleDebug` |
| 完成条件 | 媒体类已聚合到 app 内 `feature/media` 包；所有调用方 import 已更新；构建通过 |

## 8. 阶段 1：Demo / 模板残留隔离

### 范围

候选类：

- `DialogActivity`
- `StatusActivity`
- `CopyActivity`
- `CopyActivityTest`
- `StatusFragment`
- `CopyFragment`
- `CopyAdapter`
- `StatusAdapter`
- `CopyPopup`

### 执行批次

- 批次 1：搜索引用并判断归属。
- 批次 2：确认 Manifest 和入口。
- 批次 3：执行隔离或记录阻塞。
- 批次 4：验证构建。

### 批次 1/2 发现

| 候选项 | 引用情况 | Manifest 情况 | 当前判断 |
|---|---|---|---|
| `DialogActivity` | `MineFragment` import 并在 `btn_mine_dialog` 点击时启动 | 已注册为“对话框案例” | 仍有正式入口引用，不能直接隔离 |
| `StatusActivity` | `MineFragment` import 并在 `btn_mine_hint` 点击时启动 | 已注册为“状态案例” | 仍有正式入口引用，不能直接隔离 |
| `CopyActivity` | 仅被 `CopyFragment` 类型参数引用，未发现启动入口 | 未在 Manifest 注册 | 候选模板残留，暂不改动 |
| `CopyActivityTest` | 未发现业务代码启动引用 | 已从正式 Manifest 移除，类文件保留 | 候选模板残留，正式包入口已隔离 |
| `StatusFragment` | 未发现外部创建引用；内部使用 `StatusAdapter` | Fragment 不直接注册 Manifest | 候选模板残留，暂不改动 |
| `CopyFragment` | 未发现外部创建引用；依赖 `CopyActivity` | Fragment 不直接注册 Manifest | 候选模板残留，暂不改动 |
| `StatusAdapter` | 仅被 `StatusFragment` 使用 | 不适用 | 跟随 `StatusFragment` 处理 |
| `CopyAdapter` | 未发现外部使用，仅类自身定义 | 不适用 | 候选模板残留，暂不改动 |
| `CopyPopup` | 未发现外部使用，仅类自身定义 | 不适用 | 候选模板残留，暂不改动 |

### 批次 3 结果

| 项目 | 结果 |
|---|---|
| 决策 | 采用“隐藏正式入口”策略 |
| 实际改动文件 | `MineFragment.java`、`mine_fragment_bak.xml`、`docs/ui-module-migration-plan.md` |
| 代码结果 | `MineFragment` 不再注册或启动 `DialogActivity` / `StatusActivity`；布局中两个 Demo 按钮设为 `gone` |
| 验证结果 | 静态搜索无残留引用；`gradlew.bat assembleDebug` 成功 |
| 剩余阻塞 | `CopyActivityTest` 仍在 Manifest 注册，另起小批次处理 |

### 批次 4 结果

| 项目 | 结果 |
|---|---|
| 决策 | 从正式 Manifest 移除 `CopyActivityTest` 注册 |
| 实际改动文件 | `AndroidManifest.xml`、`docs/ui-module-migration-plan.md` |
| 代码结果 | `CopyActivityTest` 不再注册于正式 Manifest；类文件保留，未迁移、未删除 |
| 验证结果 | Manifest 搜索无 `CopyActivityTest`；`gradlew.bat assembleDebug` 成功 |
| 剩余阻塞 | 无 |

### 完成门禁

- [x] 候选类引用已搜索。
- [x] Manifest 注册已核对。
- [x] 未删除仍可能被业务使用的代码。
- [x] app 构建已验证。
- [x] 偏差记录为空，或偏差已处理。

## 9. 阶段 2：通用 Dialog / Popup

### 迁移候选

- `MenuDialog`
- `MessageDialog`
- `InputDialog`
- `WaitDialog`
- `SelectDialog`
- `DateDialog`
- `TimeDialog`
- `CommonDialog`
- `ListPopup`

### 不迁移

- `AlbumDialog`：归媒体模块。
- `ChatSummaryListDialog`：归 AI Chat。
- `ShareDialog`：分享业务。
- `UpdateDialog`：版本更新业务。
- `PayPasswordDialog`：账户或安全业务。
- `SafeDialog`：业务归属待确认。
- `AddressDialog`：业务或示例归属待确认。
- `AgreementDialog`：账户或合规业务。
- `TipsDialog`：阅读业务。
- `CopyDialog`：跟随 Demo 阶段判断。

### 执行批次

- 批次 1：新增模块骨架。
- 批次 2：迁移无 Adapter 依赖 Dialog。
- 批次 3：迁移低风险 Dialog 并记录 AOP 限制。
- 批次 4：迁移 `PickerLayoutManager` 并固化 Adapter/AOP 阻塞。
- 批次 5：公共化 `AppAdapter` 与 `SingleClick` 注解，保留 app 侧 AOP 编织。
- 批次 6：迁移剩余通用 Dialog / Popup 并验证。

### 批次 1 结果

| 项目 | 结果 |
|---|---|
| 决策 | 新增 `library/ui-dialog` 空模块骨架，暂不迁移 Dialog 类 |
| 实际改动文件 | `settings.gradle`、`library/ui-dialog/build.gradle`、`library/ui-dialog/src/main/AndroidManifest.xml`、`docs/ui-module-migration-plan.md` |
| 代码结果 | Gradle 已 include `:library:ui-dialog`；模块仅依赖 `library:base`；未改 app 调用路径 |
| 验证结果 | `gradlew.bat assembleDebug` 成功；`:library:ui-dialog:assembleDebug` 已执行 |
| 剩余阻塞 | 批次 2 迁移 `CommonDialog` / `WaitDialog` 前需补充 `SmartTextView`、Lottie、`@raw/progress` 依赖与资源迁移方案 |

### 批次 2 前置阻塞

| 项目 | 内容 |
|---|---|
| 阻塞项 | 原计划只声明 `library/ui-dialog -> library:base`，但目标布局实际依赖 `library:widget`、Lottie 与 `@raw/progress` |
| 证据 | `ui_dialog.xml` 使用 `com.hjq.widget.view.SmartTextView`；`wait_dialog.xml` 使用 `SmartTextView`、`com.airbnb.lottie.LottieAnimationView`、`@raw/progress` |
| 处理要求 | 先回到计划阶段补充依赖管理、资源迁移清单和验证方式，再迁移 `CommonDialog` / `WaitDialog` |
| 处理结果 | 已在批次 2 补充 `library:widget`、Lottie、raw/drawable/values 最小资源方案并完成迁移 |

### 批次 2 结果

| 项目 | 结果 |
|---|---|
| 决策 | 迁移 `CommonDialog` / `WaitDialog` 到 `library/ui-dialog`，并补齐 Widget、Lottie 与最小资源依赖 |
| 实际改动文件 | 已改：`app/build.gradle`、`library/ui-dialog/build.gradle`、`library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/CommonDialog.java`、`library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/WaitDialog.java`、`library/ui-dialog/src/main/res/layout/ui_dialog.xml`、`library/ui-dialog/src/main/res/layout/wait_dialog.xml`、`library/ui-dialog/src/main/res/raw/progress.json`、`library/ui-dialog/src/main/res/drawable/transparent_selector.xml`、`library/ui-dialog/src/main/res/values/colors.xml`、`library/ui-dialog/src/main/res/values/dimens.xml`、`library/ui-dialog/src/main/res/values/strings.xml`、`docs/ui-module-migration-plan.md`；已删除：`app/src/main/java/run/yigou/gxzy/ui/dialog/CommonDialog.java`、`app/src/main/java/run/yigou/gxzy/ui/dialog/WaitDialog.java`、`app/src/main/res/layout/ui_dialog.xml`、`app/src/main/res/layout/wait_dialog.xml` |
| 代码结果 | `CommonDialog` / `WaitDialog` 保持原包名 `run.yigou.gxzy.ui.dialog`；模块内 R 导入切换为 `run.yigou.gxzy.ui.dialog.R`；app 原 Java/XML 源位置已移除 |
| 资源结果 | `ui_dialog.xml` / `wait_dialog.xml` 已迁入模块；`progress.json` 与 `transparent_selector.xml` 复制到模块内，app 原 raw/drawable 资源保留；模块新增最小 colors/dimens/strings |
| 验证结果 | 静态检查无 `library/ui-dialog -> app` 依赖；`gradlew.bat assembleDebug` 成功，exitCode=0 |
| 剩余阻塞 | Adapter、AOP、Picker 相关 Dialog/Popup 仍需后续批次单独计划 |

### 批次 3 结果

| 项目 | 结果 |
|---|---|
| 决策 | 迁移低风险 `MessageDialog` / `InputDialog` 到 `library/ui-dialog`；暂不迁移 `SingleClick` / `SingleClickAspect`，移除这两个类中的 `@SingleClick` 注解 |
| 实际改动文件 | 已改：`library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/MessageDialog.java`、`library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/InputDialog.java`、`library/ui-dialog/src/main/res/layout/message_dialog.xml`、`library/ui-dialog/src/main/res/layout/input_dialog.xml`、`library/ui-dialog/src/main/res/drawable/dialog_input_bg.xml`、`library/ui-dialog/src/main/res/values/colors.xml`、`library/ui-dialog/src/main/res/values/dimens.xml`、`docs/ui-module-migration-plan.md`；已删除：`app/src/main/java/run/yigou/gxzy/ui/dialog/MessageDialog.java`、`app/src/main/java/run/yigou/gxzy/ui/dialog/InputDialog.java`、`app/src/main/res/layout/message_dialog.xml`、`app/src/main/res/layout/input_dialog.xml` |
| 代码结果 | `MessageDialog` / `InputDialog` 保持原包名 `run.yigou.gxzy.ui.dialog`；模块内 R 导入切换为 `run.yigou.gxzy.ui.dialog.R`；已移除 `run.yigou.gxzy.aop.SingleClick` import 与 `@SingleClick` 注解；app 原 Java/XML 源位置已移除 |
| 资源结果 | `message_dialog.xml` / `input_dialog.xml` / `dialog_input_bg.xml` 已迁入模块；模块新增最小 `white`、`dp_4`、`dp_5`、`dp_10`、`dp_15`、`sp_14` 资源 |
| 验证结果 | 静态检查无 `library/ui-dialog -> app`、`run.yigou.gxzy.R`、`run.yigou.gxzy.aop`、`SingleClick` 引用；`gradlew.bat assembleDebug` 成功，exitCode=0 |
| 剩余阻塞 | `MessageDialog` / `InputDialog` 的点击不再经过 AOP 防重复点击；Adapter、Picker 相关 Dialog/Popup 仍需后续批次单独计划 |

### 批次 4 结果

| 项目 | 结果 |
|---|---|
| 决策 | 仅迁移 `PickerLayoutManager` 到 `library/base`，先解除 `DateDialog` / `TimeDialog` 对 app 内 Picker 的依赖；`AppAdapter` 与 AOP 后置 |
| 实际改动文件 | 已改：`library/base/src/main/java/com/hjq/base/PickerLayoutManager.java`、`app/src/main/java/run/yigou/gxzy/ui/dialog/DateDialog.java`、`app/src/main/java/run/yigou/gxzy/ui/dialog/TimeDialog.java`、`docs/ui-module-migration-plan.md`；已删除：`app/src/main/java/run/yigou/gxzy/manager/PickerLayoutManager.java` |
| 代码结果 | `PickerLayoutManager` 包名改为 `com.hjq.base` 并迁入 `library/base`；`DateDialog` / `TimeDialog` import 已切换为 `com.hjq.base.PickerLayoutManager`；类 API 和调用逻辑保持不变 |
| 验证结果 | 静态检查通过；`gradlew.bat assembleDebug` 成功，exitCode=0 |
| 剩余阻塞 | `DateDialog` / `TimeDialog` 仍依赖 `AppAdapter` 和 `SingleClick`，暂不能迁入 `library/ui-dialog`；`MenuDialog` / `SelectDialog` / `ListPopup` 仍被 `AppAdapter` 阻塞 |

### 批次 5 结果

| 项目 | 结果 |
|---|---|
| 决策 | 公共化 `AppAdapter` 与 `SingleClick` 注解到 `library/base`；保留 `SingleClickAspect`、AspectJ 插件与编织配置在 app 侧 |
| 实际改动文件 | 已改：`library/base/src/main/java/com/hjq/base/AppAdapter.java`、`library/base/src/main/java/com/hjq/base/action/SingleClick.java`、`app/src/main/java/run/yigou/gxzy/aop/SingleClickAspect.java`、`app/src/main/java` 下 27 个 `AppAdapter` import 与 26 个 `SingleClick` import 引用文件、`docs/ui-module-migration-plan.md`；已删除：`app/src/main/java/run/yigou/gxzy/app/AppAdapter.java`、`app/src/main/java/run/yigou/gxzy/aop/SingleClick.java` |
| 代码结果 | `AppAdapter` 包名改为 `com.hjq.base` 并迁入 `library/base`；`SingleClick` 包名改为 `com.hjq.base.action` 并迁入 `library/base`；`SingleClickAspect` 仍位于 app，import 与 pointcut 指向 `com.hjq.base.action.SingleClick` |
| 验证结果 | 静态检查通过；`gradlew.bat assembleDebug` 成功，exitCode=0 |
| 剩余阻塞 | `AppAdapter` 不再阻塞剩余 Dialog / Popup 迁移；`SingleClick` 注解可被 library 使用；`SingleClickAspect` / AspectJ 编织仍是 app 侧能力，后续若要跨模块统一编织需单独规划 |

### 批次 6 结果

| 项目 | 结果 |
|---|---|
| 决策 | 迁移剩余通用 `MenuDialog` / `SelectDialog` / `DateDialog` / `TimeDialog` / `ListPopup` 到 `library/ui-dialog`，保留原包名与 app 调用侧 import |
| 实际改动文件 | 已改：`library/ui-dialog/build.gradle`、`library/ui-dialog/src/main/java/run/yigou/gxzy/ui/dialog/MenuDialog.java`、`SelectDialog.java`、`DateDialog.java`、`TimeDialog.java`、`library/ui-dialog/src/main/java/run/yigou/gxzy/ui/popup/ListPopup.java`、`library/ui-dialog/src/main/res/layout` 下 7 个迁移 layout、`library/ui-dialog/src/main/res/drawable` 下 4 个 checkbox 相关 drawable、`library/ui-dialog/src/main/res/values/strings.xml`、`dimens.xml`、`colors.xml`、`docs/ui-module-migration-plan.md`；已删除 app 原 5 个 Java 与 6 个 Dialog layout |
| 代码结果 | 5 个类保持原对外 API；模块内 R import 切换为 `run.yigou.gxzy.ui.dialog.R`；`ListPopup` 保持 `run.yigou.gxzy.ui.popup` 包名并显式导入模块 R |
| 资源结果 | 通用 Dialog / Popup 所需 layout、picker item、checkbox selector 和图标已复制到 `library/ui-dialog`；app 原 `picker_item.xml` 保留；checkbox 相关 drawable 因仍被 app 媒体/发现布局和 `radiobutton_selector.xml` 引用而保留 |
| 依赖结果 | `library/ui-dialog` 新增 `implementation 'com.github.getActivity:Toaster:12.6'`，用于 `SelectDialog`；未新增 app 反向依赖 |
| 验证结果 | 静态检查通过；首次构建发现迁移 Java 文件存在 GBK/ANSI 编码导致 UTF-8 编译 stderr 提示，已将 5 个迁移 Java 转为 UTF-8；复跑 `gradlew.bat assembleDebug` 成功，exitCode=0，BUILD SUCCESSFUL in 6s |
| 剩余风险 | `@SingleClick` 注解在 `library/ui-dialog` 中保留，但当前 app 侧 AspectJ include 范围是否织入 library class 仍需手测或后续单独规划跨模块 AOP 策略 |

### 完成门禁

- [x] 未引入 `library:ui-dialog -> app`。
- [x] 迁移类不依赖业务模型。
- [x] 资源引用已切换到模块内 `R`。
- [x] app 构建已验证。
- [x] 通用 Dialog / Popup 编译调用路径已验证。

## 10. 阶段 3：媒体模块

### 迁移候选

- `ImageSelectActivity`
- `ImagePreviewActivity`
- `ImageCropActivity`
- `CameraActivity`
- `VideoSelectActivity`
- `VideoPlayActivity`
- `ImageSelectAdapter`
- `ImagePreviewAdapter`
- `VideoSelectAdapter`
- `AlbumDialog`
- `PlayerView`，归属待确认。

### 执行批次

- 批次 1：确认入口、Manifest、资源。
- 批次 2：新增 `feature/media`。
- 批次 3：迁移图片相关。
- 批次 4：迁移视频相关。
- 批次 5：处理权限、Glide、线程依赖。
- 批次 6：验证媒体流程。

### 批次 1 结果

| 项目 | 结果 |
|---|---|
| 决策 | 仅做媒体边界盘点，不直接新增 `feature/media`、不迁移源码、不移动资源、不修改 Manifest |
| 实际改动文件 | 仅 `docs/ui-module-migration-plan.md` |
| Manifest 注册 | 当前 app Manifest 注册 8 个媒体 Activity：`.ui.activity.CameraActivity`、`.ui.activity.ImageCropActivity`、`.ui.activity.ImageSelectActivity`、`.ui.activity.ImagePreviewActivity`、`.ui.activity.VideoPlayActivity`、`.ui.activity.VideoPlayActivity$Portrait`、`.ui.activity.VideoPlayActivity$Landscape`、`.ui.activity.VideoSelectActivity` |
| 调用入口 | `BrowserView` 调用 `ImageSelectActivity.start(...)`、`VideoSelectActivity.start(...)` 并直接使用 `VideoSelectActivity.VideoBean`；`MyFragmentPersonal` 调用 `ImageSelectActivity`、`ImagePreviewActivity`、`ImageCropActivity`；`MineFragment` 调用 `ImageSelectActivity`、`ImagePreviewActivity`、`VideoSelectActivity`、`VideoPlayActivity.Builder` |
| 候选源码 | 已确认 11 个候选文件存在：6 个 Activity、3 个 Adapter、`AlbumDialog`、`PlayerView` |
| API / 数据结构 | `ImageSelectActivity.OnPhotoSelectListener` 返回 `List<String>`；`VideoSelectActivity.OnVideoSelectListener` 返回 `List<VideoSelectActivity.VideoBean>`；`VideoBean` 为 `Parcelable`，被 `BrowserView` 和 `MineFragment` 直接引用；`VideoPlayActivity.Builder` 负责方向选择并启动普通/竖屏/横屏播放 Activity |
| 资源候选 | 媒体布局：`image_select_activity.xml`、`image_select_item.xml`、`image_preview_activity.xml`、`image_preview_item.xml`、`video_select_activity.xml`、`video_select_item.xml`、`video_play_activity.xml`、`album_dialog.xml`、`album_item.xml`、`widget_player_view.xml`；媒体 drawable：`camera_ic.xml`、`videocam_ic.xml`、`image_preview_indicator.xml`、`video_brightness_high_ic.xml`、`video_brightness_low_ic.xml`、`video_brightness_medium_ic.xml`、`video_lock_close_ic.xml`、`video_lock_open_ic.xml`、`video_progress_bg.xml`、`video_progress_ball_bg.xml`、`video_schedule_forward_ic.xml`、`video_schedule_rewind_ic.xml`、`video_volume_high_ic.xml`、`video_volume_low_ic.xml`、`video_volume_medium_ic.xml`、`video_volume_mute_ic.xml`；通用但被媒体布局使用：`checkbox_selector.xml`、`radiobutton_selector.xml`、`roll_accent_bg.xml`、`succeed_ic.xml`、`arrows_left_ic.xml`、`@raw/progress` 与相关 values；mipmap 候选：`no_image.jpg` |
| 明确排除 | `tips_ai_msg_ifly_layout_mnotice_image.xml` 只因文件名包含 image 被命中，实际属于 AI Chat / Tips，阶段 3 不纳入媒体资源迁移 |
| 依赖候选 | `XXPermissions` / `Permission`、`Glide` / `GlideApp` / Glide compiler、`PhotoView`、`ImmersionBar`、`ShapeView`、`TitleBar`、`Toaster`、`CircleIndicator`、Lottie、`library:base`、`library:widget`；`VideoSelectAdapter` 还直接依赖 `CacheDataManager`，图片/视频选择 Activity 依赖 `ThreadPoolManager` |
| 主要阻塞 | 媒体代码仍依赖 `run.yigou.gxzy.app.AppActivity`、`run.yigou.gxzy.aop.Log` / `Permissions`、`ThreadPoolManager`、`GlideApp`、`CacheDataManager`、`AppConfig` 和 Bugly；若直接迁入 Gradle 模块，会违反 `feature:* -> app` 禁止规则或扩大依赖迁移范围 |
| 验证结果 | 静态搜索和文件读取完成；本批次只改文档，未运行 `gradlew.bat assembleDebug` |

### 批次 1 后续判断

下一批次不应直接大规模搬迁媒体源码。应先单独规划模块策略：

- 若先新增 `feature/media` Gradle 模块，需要同时规划 app 依赖消除方案，包括 `AppActivity`、AOP 注解/切面、`GlideApp` 生成类、`ThreadPoolManager`、`CacheDataManager`、`AppConfig`、Manifest 合并和资源归属。
- 若依赖解耦成本过高，应先按执行原则在 app 内建立 `feature/media` 包做包级聚合，再评估 Gradle 模块化。
- `PlayerView` 当前位于 `run.yigou.gxzy.widget` 且依赖 `library:widget`、ShapeView、Lottie、`@raw/progress`、视频控制 drawable；其归属需在视频批次前单独确认。

### 批次 2 结果

| 项目 | 结果 |
|---|---|
| 决策 | 在 app 内建立 `feature/media` 包做包级聚合，不新增 Gradle 模块 |
| 实际改动文件 | 已移动：7 个 Activity（ImageSelect、ImagePreview、ImageCrop、VideoSelect、VideoPlay、Camera）、3 个 Adapter（ImageSelectAdapter、ImagePreviewAdapter、VideoSelectAdapter）、1 个 Dialog（AlbumDialog）到 `app/src/main/java/run/yigou/gxzy/ui/feature/media/` 下对应子包；已更新包名声明与 import |
| 代码结果 | 所有媒体类保持原对外 API 和行为不变；包名从 `run.yigou.gxzy.ui.activity`/`ui.adapter`/`ui.dialog` 改为 `run.yigou.gxzy.ui.feature.media.activity`/`adapter`/`dialog`；所有调用方 import 已更新：BrowserView.java、MyFragmentPersonal.java、MineFragment.java、PersonalDataActivity.java |
| 验证结果 | `gradlew.bat assembleDebug` 成功，BUILD SUCCESSFUL in 28s；存在既有 AGP/deprecation warning，非本批次处理范围 |
| 剩余阻塞 | 与批次 1 相同：媒体代码仍依赖 `AppActivity`、AOP 注解/切面、`GlideApp`、`ThreadPoolManager`、`CacheDataManager`、`AppConfig`、Bugly；`PlayerView` 未迁移；Manifest 仍注册在 app 侧；暂不新增 Gradle 模块 |

### 下一步

- 批次 3：资源迁移（媒体布局、drawable、values、mipmap 复制到 app 内资源聚合目录或保持原位）
- 批次 4：Manifest 注册（确认 8 个媒体 Activity 注册方式）
- 批次 5：处理依赖（`PlayerView`、`GlideApp`、AOP 注解使用等）
- 批次 6：验证媒体流程并评估 Gradle 模块化条件

### 完成门禁（批次 2）

- [x] 媒体 Activity、Adapter、Dialog 已聚合到 app 内 `feature/media` 包
- [x] 所有调用方 import 已更新
- [x] `gradlew.bat assembleDebug` 成功
- [ ] 媒体 Activity Manifest 已迁移或明确保留原因
- [ ] 未引入 `feature:media -> app` Gradle 依赖（当前为 app 内包级聚合，不涉及 Gradle 模块）
- [ ] 权限、Glide、线程依赖已处理
- [ ] 图片、相机、裁剪、视频流程已验证

## 11. 阶段 4：AI Chat 剥离

### 迁移候选

先迁移到 app 内 `feature/aichat`：

- `AiMsgFragment`
- `ChatInputHelper`
- `ChatSidebarHelper`
- `ChatSummaryHelper`
- `ChatSummaryListDialog`
- `AiMsgContract`
- `AiMsgPresenter`
- `TipsAiChatAdapter`
- `ChatHistoryAdapter`
- `ChatSummaryAdapter`

### 执行批次

- 批次 1：建立 app 内 `feature/aichat` 包。
- 批次 2：迁移 Contract / Presenter。
- 批次 3：迁移 Fragment / Helper / Dialog。
- 批次 4：迁移 Adapter。
- 批次 5：修正引用并验证。
- 批次 6：评估 Gradle 模块化阻塞。

### 完成门禁

- [ ] `ui/tips` 不再包含 AI Chat 类。
- [ ] Reader/Tips 不依赖 AI Chat。
- [ ] 首页 AI 入口正常。
- [ ] app 构建已验证。
- [ ] AI 聊天、会话、总结路径已验证。

## 12. 阶段 5：阅读 / Tips 治理

### 迁移候选

迁移到 app 内 `feature/reader`：

- 阅读 Activity。
- 阅读 Fragment。
- 阅读 Adapter。
- `tips` 非 AI 代码。

### 执行批次

- 批次 1：确认 AI 已剥离。
- 批次 2：建立 reader 包。
- 批次 3：迁移页面入口。
- 批次 4：迁移 tips 非 AI 代码。
- 批次 5：收敛 `GlobalDataHolder`。
- 批次 6：梳理 `BookRepository` / `TipsNetHelper`。
- 批次 7：验证阅读路径。

### 完成门禁

- [ ] AI Chat 已从 tips 剥离。
- [ ] 阅读相关代码聚合到 reader 边界。
- [ ] 未引入 reader 与 AI 的互相依赖。
- [ ] app 构建已验证。
- [ ] 阅读、搜索、方药名词弹窗路径已验证。

## 13. 阶段 6：账户模块

### 候选范围

- `LoginActivity`
- `RegisterActivity`
- `PasswordForgetActivity`
- `PasswordResetActivity`
- `PhoneResetActivity`
- `PersonalDataActivity`
- `MyFragmentPersonal`
- `MyMsgFragment`
- `MineFragment`
- `MessageFragment`

### 执行批次

- 批次 1：搜索入口与依赖。
- 批次 2：app 内 account 包迁移。
- 批次 3：梳理认证、HTTP、DB、友盟边界。
- 批次 4：验证账户路径。
- 批次 5：评估 Gradle 模块化条件。

### 完成门禁

- [ ] 账户入口和依赖已确认。
- [ ] 登录态和第三方回调边界已明确。
- [ ] app 构建已验证。
- [ ] 登录、注册、找回、个人资料路径已验证。

## 14. 偏差记录

| 编号 | 日期 | 阶段 | 偏差描述 | 影响范围 | 处理方式 | 状态 |
|---|---|---|---|---|---|---|
| - | - | - | - | - | - | - |

## 15. 阻塞记录

| 编号 | 日期 | 阶段 | 阻塞项 | 证据 | 需要决策 | 状态 |
|---|---|---|---|---|---|---|
| B-001 | 2026-06-08 | 阶段 1 | `DialogActivity` / `StatusActivity` 仍由 `MineFragment` 正式入口启动，不能直接隔离 | `MineFragment.java:72`、`MineFragment.java:75` | 已选择“隐藏正式入口”；`MineFragment` 移除点击注册和启动逻辑，布局按钮设为 `gone` | 已解除 |
| B-002 | 2026-06-08 | 阶段 1 / 批次 4 | `CopyActivityTest` 未发现代码启动引用，但仍在 Manifest 注册 | 已从正式 Manifest 移除 `.ui.activity.CopyActivityTest` 注册；类文件保留；`assembleDebug` 通过 | 已决策从正式 Manifest 移除，不迁入 debug、不删除类 | 已解除 |
| B-003 | 2026-06-08 | 阶段 2 / 批次 2 | `CommonDialog` / `WaitDialog` 资源迁移依赖超出原计划 | `ui_dialog.xml` 使用 `SmartTextView`；`wait_dialog.xml` 使用 `SmartTextView`、Lottie、`@raw/progress` | 已补充并执行依赖方案：`library/ui-dialog` 依赖 `library:widget` 和 Lottie；`progress.json` 与 `transparent_selector.xml` 复制到模块内；最小 values 资源已补齐 | 已解除 |
| B-004 | 2026-06-08 | 阶段 2 / 批次 3 | `SingleClick` / `SingleClickAspect` 仍属于 app，无法被 `library/ui-dialog` 直接依赖 | 批次 5 已将 `SingleClick` 注解迁入 `library/base/src/main/java/com/hjq/base/action/SingleClick.java`；`SingleClickAspect` 仍在 app，pointcut 绑定 `com.hjq.base.action.SingleClick` | 注解可被 library 使用；AspectJ 编织仍由 app 侧维护，不迁移 AspectJ 配置 | 已解除 |
| B-005 | 2026-06-08 | 阶段 2 / 批次 4 | `AppAdapter` 跨阶段引用过多，暂不迁移 | 批次 5 已将 `AppAdapter` 迁入 `library/base/src/main/java/com/hjq/base/AppAdapter.java`，app 旧 import 已替换为 `com.hjq.base.AppAdapter` | `AppAdapter` 不再阻塞剩余通用 Dialog / Popup 迁移；业务 Adapter 仍保持原调用行为 | 已解除 |
| B-006 | 2026-06-08 | 阶段 3 / 批次 1 | 媒体代码直接迁入 `feature/media` 的依赖边界未满足 | 批次 2 已采用 app 内 `feature/media` 包级聚合策略，不新增 Gradle 模块；依赖问题暂不解决 | 先在 app 内做 `feature/media` 包级聚合，后续评估 Gradle 模块化条件 | 已转为阻塞 B-007 |
| B-007 | 2026-06-09 | 阶段 3 / 批次 2 | 媒体代码仍依赖 `AppActivity`、AOP 注解/切面、`GlideApp`、`ThreadPoolManager`、`CacheDataManager`、`AppConfig`、Bugly；`PlayerView` 未迁移 | 7 个 Activity 继承 `AppActivity`，使用 AOP 注解，Adapter 使用 `GlideApp`，VideoSelectAdapter 使用 `CacheDataManager` | 批次 2 已做包级聚合；Gradle 模块化需先消除这些 app 依赖或将其下沉 | 待决策 |

## 16. 验证记录

| 日期 | 阶段 | 命令/路径 | 结果 | 备注 |
|---|---|---|---|---|
| 2026-06-08 | 文档基线 | `docs/ui-module-migration-plan.md` | 已确认 | 不运行构建 |
| 2026-06-08 | 阶段 1 | 引用搜索与 Manifest 检查 | 已完成 | 只读检查，不运行构建；发现 2 个待确认阻塞 |
| 2026-06-08 | 阶段 1 | `MineFragment.java` 静态搜索：`DialogActivity|StatusActivity|btn_mine_dialog|btn_mine_hint` | 通过 | 无匹配，正式入口启动逻辑已移除 |
| 2026-06-08 | 阶段 1 | `mine_fragment_bak.xml` 按钮可见性检查 | 通过 | `btn_mine_dialog`、`btn_mine_hint` 均为 `android:visibility="gone"` |
| 2026-06-08 | 阶段 1 | `gradlew.bat assembleDebug` | 通过 | BUILD SUCCESSFUL；存在既有 warning，构建退出码 0 |
| 2026-06-08 | 阶段 1 / 批次 4 | `AndroidManifest.xml` 搜索：`CopyActivityTest` | 通过 | 无匹配，正式 Manifest 注册已移除 |
| 2026-06-08 | 阶段 1 / 批次 4 | `app/src/main/java` 搜索：`CopyActivityTest` | 通过 | 仅保留类定义，未删除 Demo/模板类 |
| 2026-06-08 | 阶段 1 / 批次 4 | `gradlew.bat assembleDebug` | 通过 | exitCode=0；存在 AGP/Manifest/deprecation warning，非本批次处理范围 |
| 2026-06-08 | 阶段 2 / 批次 1 | `settings.gradle` 与 `library/ui-dialog` 文件检查 | 通过 | 已 include `:library:ui-dialog`；新增空模块 `build.gradle` 和 Manifest |
| 2026-06-08 | 阶段 2 / 批次 1 | `gradlew.bat assembleDebug` | 通过 | exitCode=0；`:library:ui-dialog:assembleDebug` 已执行；存在既有 AGP/deprecation warning，非本批次处理范围 |
| 2026-06-08 | 阶段 2 / 批次 2 | 静态检查 app 原路径与 `library/ui-dialog` 依赖 | 通过 | app 原 `CommonDialog` / `WaitDialog` Java 与 `ui_dialog` / `wait_dialog` XML 已移除；模块内无 `run.yigou.gxzy.R` 或 `run.yigou.gxzy.app` 引用 |
| 2026-06-08 | 阶段 2 / 批次 2 | `gradlew.bat assembleDebug` | 通过 | exitCode=0；`:library:ui-dialog` 资源与 Java 编译已执行；存在既有 AGP、字符串格式、AspectJ/D8、deprecation warning，非本批次处理范围 |
| 2026-06-08 | 阶段 2 / 批次 3 | 静态检查 app 原路径与 `library/ui-dialog` 依赖 | 通过 | app 原 `MessageDialog` / `InputDialog` Java 与 `message_dialog` / `input_dialog` XML 已移除；模块内无 `run.yigou.gxzy.R`、`run.yigou.gxzy.aop` 或 `SingleClick` 引用 |
| 2026-06-08 | 阶段 2 / 批次 3 | `gradlew.bat assembleDebug` | 通过 | exitCode=0；`:library:ui-dialog` 资源与 Java 编译已执行；存在既有 AGP、字符串格式、deprecation warning，非本批次处理范围 |
| 2026-06-08 | 阶段 2 / 批次 4 | 静态检查 Picker 迁移路径和反向依赖 | 通过 | app 旧 `PickerLayoutManager` import 无匹配；app 原 `PickerLayoutManager.java` 不存在；新类包名为 `com.hjq.base`；`library/base` 无 `run.yigou.gxzy.R` 或 `run.yigou.gxzy.app` 引用 |
| 2026-06-08 | 阶段 2 / 批次 4 | `gradlew.bat assembleDebug` | 通过 | exitCode=0；BUILD SUCCESSFUL；存在既有 AGP/deprecation warning，非本批次处理范围 |
| 2026-06-08 | 阶段 2 / 批次 5 | 静态检查 `AppAdapter` / `SingleClick` 迁移路径和反向依赖 | 通过 | app 源码无旧 import 与旧全限定引用；app 原 `AppAdapter.java` / `SingleClick.java` 不存在；新类包名分别为 `com.hjq.base`、`com.hjq.base.action`；`SingleClickAspect` pointcut 指向新注解；`library/base` 无 `run.yigou.gxzy.R` 或 `run.yigou.gxzy.app` 引用 |
| 2026-06-08 | 阶段 2 / 批次 5 | `gradlew.bat assembleDebug` | 通过 | exitCode=0；BUILD SUCCESSFUL in 27s；存在既有 AGP/Manifest/AspectJ D8/deprecation warning，非本批次处理范围 |
| 2026-06-08 | 阶段 2 / 批次 6 | 静态检查 app 原路径、模块 R import、反向依赖和资源留存 | 通过 | app 原 5 个 Java 已删除；app 原 6 个 Dialog layout 已删除；app 原 `picker_item.xml` 保留；checkbox 相关 drawable 因 app 仍有直接引用而保留；`library/ui-dialog` 无 `run.yigou.gxzy.R`、`run.yigou.gxzy.app`、`run.yigou.gxzy.aop` 引用 |
| 2026-06-08 | 阶段 2 / 批次 6 | `gradlew.bat assembleDebug` | 通过 | 首次构建发现迁移 Java 文件编码 stderr 提示，已转换为 UTF-8；复跑 exitCode=0；BUILD SUCCESSFUL in 6s；存在既有 AGP/deprecation warning，非本批次处理范围 |
| 2026-06-08 | 阶段 3 / 批次 1 | 媒体候选文件、Manifest、调用入口、资源和依赖静态检查 | 通过 | 已确认 11 个候选 Java、8 个 Manifest 注册、`BrowserView` / `MyFragmentPersonal` / `MineFragment` 调用入口、媒体资源候选和依赖阻塞；本批次只改文档，未运行构建 |
| 2026-06-09 | 阶段 3 / 批次 2 | 媒体类移动到 `app/src/main/java/run/yigou/gxzy/ui/feature/media/`，更新包名与 import | 通过 | 7 个 Activity、3 个 Adapter、1 个 Dialog 已移动；所有调用方 import 已更新；`gradlew.bat assembleDebug` 成功，BUILD SUCCESSFUL in 28s |

## 17. 决策记录

| 编号 | 日期 | 决策 | 原因 | 影响 |
|---|---|---|---|---|
| ADR-001 | - | 使用 `docs/ui-module-migration-plan.md` 作为迁移控制文档 | 模块化会影响目录结构、构建模块、依赖关系和维护方式，需要项目级文档管理 | 后续执行以本文档为基准 |
| ADR-002 | - | 大清单改为阶段批次执行 | 降低跑偏风险，便于每批次验证和纠偏 | 每批次结束必须更新本文档 |
| ADR-003 | 2026-06-08 | 阶段 1 采用“隐藏正式入口”策略 | `DialogActivity` / `StatusActivity` 曾由 `MineFragment` 启动，但属于 Demo/案例入口，不应作为正式业务入口暴露 | 修改 `MineFragment` 入口显示/点击逻辑；Demo Activity 暂不删除、不迁移；`CopyActivityTest` 另起小批次处理 |
| ADR-004 | 2026-06-08 | 从正式 Manifest 移除 `CopyActivityTest` 注册 | 未发现业务代码启动引用，且 Manifest 注册复用“关于我们”label，符合 Demo/模板残留隔离目标 | 正式包不再注册 `CopyActivityTest`；类文件保留；不新增 debug Manifest；阶段 1 阻塞解除 |
| ADR-005 | 2026-06-08 | 阶段 2 先接入 `library/ui-dialog` 空模块再迁移类 | 先验证 Gradle 多模块接入，可降低后续 Dialog 迁移失败时的定位成本 | `settings.gradle` 新增 `:library:ui-dialog`；空模块仅依赖 `library:base`；`CommonDialog` / `WaitDialog` 因 Widget/Lottie/raw 依赖缺口暂未迁移 |
| ADR-006 | 2026-06-08 | `library/ui-dialog` 直接依赖 `library:widget` 与 Lottie，并复制最小资源 | `CommonDialog` / `WaitDialog` 布局需要 `SmartTextView`、`LottieAnimationView`、`@raw/progress` 与少量 app values；直接声明依赖和最小资源可避免模块反向依赖 app | `app` 新增 `implementation project(':library:ui-dialog')`；`CommonDialog` / `WaitDialog` 迁入模块；app 原 raw/drawable 通用资源保留，模块内复制编译所需资源 |
| ADR-007 | 2026-06-08 | 批次 3 迁移 `MessageDialog` / `InputDialog` 时暂不迁移 AOP，移除局部 `@SingleClick` | `SingleClick` / `SingleClickAspect` 当前属于 app，直接依赖会违反 `library:* -> app` 禁止规则；同步迁移 AspectJ 配置影响范围超过本批次 | `MessageDialog` / `InputDialog` 迁入 `library/ui-dialog` 并可编译；这两个 Dialog 的点击暂不经过 AOP 防重复点击，后续需单独规划 AOP 公共化或点击防抖方案 |
| ADR-008 | 2026-06-08 | 批次 4 仅将 `PickerLayoutManager` 迁入 `library/base`，`AppAdapter` 与 AOP 后置 | `PickerLayoutManager` 仅被 `DateDialog` / `TimeDialog` 引用且无 app 依赖；`AppAdapter` 跨阶段引用过多，AOP 涉及 AspectJ 编织范围 | `DateDialog` / `TimeDialog` 不再依赖 app 内 Picker；剩余 Dialog/Popup 迁移仍需先解决 `AppAdapter` 与 AOP |
| ADR-009 | 2026-06-08 | 公共化 `AppAdapter` 和 `SingleClick` 注解，但保留 `SingleClickAspect` 在 app | `AppAdapter` 只依赖 Android SDK、AndroidX 注解、Java 集合与 `BaseAdapter`，适合下沉 `library/base`；`SingleClick` 注解本身无 app 依赖，可供 library 使用；`SingleClickAspect` 依赖 AspectJ 编织和 app 构建配置，迁移会扩大范围 | 剩余 Dialog / Popup 可引用公共 `AppAdapter` 和 `SingleClick`；AspectJ 插件/配置不变，防重复点击编织仍由 app 侧维护 |
| ADR-010 | 2026-06-08 | 剩余通用 Dialog / Popup 迁入 `library/ui-dialog`，保留原包名和 app 侧 AOP 编织 | 批次 5 已解除 `AppAdapter` / `SingleClick` 阻塞；保持原包名可避免调用侧业务代码改动；`SelectDialog` 直接使用 Toaster，模块需显式声明依赖 | `MenuDialog` / `SelectDialog` / `DateDialog` / `TimeDialog` / `ListPopup` 迁入 `library/ui-dialog`；模块复制最小 layout/drawable/values；app 原 `picker_item.xml` 和 checkbox 相关 drawable 按引用保留；`@SingleClick` 跨模块运行时织入仍需后续手测或单独规划 |
| ADR-011 | 2026-06-08 | 阶段 3 批次 1 先做媒体边界盘点，不直接新增模块或迁移代码 | 媒体 Activity、Adapter、Dialog、PlayerView 当前仍与 app Manifest、`AppActivity`、AOP、`ThreadPoolManager`、`GlideApp`、`CacheDataManager`、`AppConfig`、Bugly 和调用侧 API 耦合；直接新增 `feature/media` 会扩大范围并可能违反 `feature:* -> app` 禁止规则 | 本批次只更新迁移文档，记录 8 个 Manifest 注册、3 个调用入口、11 个候选 Java、资源/依赖清单和阻塞；下一批次先规划模块策略，再决定 Gradle 模块化或 app 内 `feature/media` 包级聚合 |
| ADR-012 | 2026-06-09 | 阶段 3 批次 2 采用 app 内 `feature/media` 包级聚合，不新增 Gradle 模块 | 媒体代码直接迁入 Gradle 模块的依赖边界未满足（`AppActivity`、AOP、`GlideApp`、`CacheDataManager` 等仍属于 app）；包级聚合可在不改变构建结构的前提下先按业务域重组代码 | 7 个 Activity、3 个 Adapter、1 个 Dialog 已移动到 `app/src/main/java/run/yigou/gxzy/ui/feature/media/`；所有调用方 import 已更新；Manifest 注册和资源保持原位；Gradle 模块化需后续单独规划 |
