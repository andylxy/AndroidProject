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
| 阶段 2 Dialog/Popup | 未开始 | - | - | - |
| 阶段 3 Media | 未开始 | - | - | - |
| 阶段 4 AI Chat | 未开始 | - | - | - |
| 阶段 5 Reader/Tips | 未开始 | - | - | - |
| 阶段 6 Account | 未开始 | - | - | - |

## 7. 当前批次控制

| 项目 | 内容 |
|---|---|
| 当前阶段 | 阶段 1：Demo / 模板残留隔离已完成 |
| 当前批次 | 批次 4：已从正式 Manifest 移出 `CopyActivityTest` 注册 |
| 允许改动文件 | `docs/ui-module-migration-plan.md`、`app/src/main/AndroidManifest.xml` |
| 排除文件 | Java、XML 资源、Gradle、README、AGENTS.md、Dialog/Popup、Media、AI Chat、Reader/Tips、Account |
| 验证方式 | Manifest 搜索确认无 `CopyActivityTest` 注册；运行 `gradlew.bat assembleDebug` |
| 完成条件 | `CopyActivityTest` 不再注册于正式 Manifest；构建验证结果写入本文档 |

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
- 批次 3：处理 Adapter / AOP / R 依赖。
- 批次 4：迁移资源。
- 批次 5：app 接入并验证。

### 完成门禁

- [ ] 未引入 `library:ui-dialog -> app`。
- [ ] 迁移类不依赖业务模型。
- [ ] 资源引用已切换到模块内 `R`。
- [ ] app 构建已验证。
- [ ] 通用 Dialog 调用路径已验证。

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

### 完成门禁

- [ ] 媒体 Activity Manifest 已迁移或明确保留原因。
- [ ] 未引入 `feature:media -> app`。
- [ ] 权限、Glide、线程依赖已处理。
- [ ] app 构建已验证。
- [ ] 图片、相机、裁剪、视频流程已验证。

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

## 17. 决策记录

| 编号 | 日期 | 决策 | 原因 | 影响 |
|---|---|---|---|---|
| ADR-001 | - | 使用 `docs/ui-module-migration-plan.md` 作为迁移控制文档 | 模块化会影响目录结构、构建模块、依赖关系和维护方式，需要项目级文档管理 | 后续执行以本文档为基准 |
| ADR-002 | - | 大清单改为阶段批次执行 | 降低跑偏风险，便于每批次验证和纠偏 | 每批次结束必须更新本文档 |
| ADR-003 | 2026-06-08 | 阶段 1 采用“隐藏正式入口”策略 | `DialogActivity` / `StatusActivity` 曾由 `MineFragment` 启动，但属于 Demo/案例入口，不应作为正式业务入口暴露 | 修改 `MineFragment` 入口显示/点击逻辑；Demo Activity 暂不删除、不迁移；`CopyActivityTest` 另起小批次处理 |
| ADR-004 | 2026-06-08 | 从正式 Manifest 移除 `CopyActivityTest` 注册 | 未发现业务代码启动引用，且 Manifest 注册复用“关于我们”label，符合 Demo/模板残留隔离目标 | 正式包不再注册 `CopyActivityTest`；类文件保留；不新增 debug Manifest；阶段 1 阻塞解除 |
