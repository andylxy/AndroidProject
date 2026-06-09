# 阶段 3 批次 1：Media 入口、Manifest、资源与依赖确认计划

## 1. 目标

阶段 2 Dialog / Popup 已完成后，按 `docs/ui-module-migration-plan.md` 进入阶段 3：媒体模块。

本批次只执行阶段 3 的第一个小批次：确认媒体相关入口、Manifest 注册、资源、依赖和迁移阻塞，为后续新增 `feature/media` 或先做 app 内包重组提供事实基线。

本批次不迁移源码、不新增 Gradle 模块、不修改 Manifest 注册、不移动资源、不改业务行为。

## 2. 已确认事实

### 2.1 阶段状态

`docs/ui-module-migration-plan.md` 当前显示：

- 阶段 2 Dialog/Popup：已完成，批次 6 已完成，最近验证为 `assembleDebug` 成功。
- 阶段 3 Media：未开始。
- 阶段 3 既定执行批次：
  - 批次 1：确认入口、Manifest、资源。
  - 批次 2：新增 `feature/media`。
  - 批次 3：迁移图片相关。
  - 批次 4：迁移视频相关。
  - 批次 5：处理权限、Glide、线程依赖。
  - 批次 6：验证媒体流程。

### 2.2 阶段 3 候选源码

已发现候选文件位于 app 内：

- Activity：
  - `app/src/main/java/run/yigou/gxzy/ui/activity/CameraActivity.java`
  - `app/src/main/java/run/yigou/gxzy/ui/activity/ImageCropActivity.java`
  - `app/src/main/java/run/yigou/gxzy/ui/activity/ImageSelectActivity.java`
  - `app/src/main/java/run/yigou/gxzy/ui/activity/ImagePreviewActivity.java`
  - `app/src/main/java/run/yigou/gxzy/ui/activity/VideoSelectActivity.java`
  - `app/src/main/java/run/yigou/gxzy/ui/activity/VideoPlayActivity.java`
- Adapter：
  - `app/src/main/java/run/yigou/gxzy/ui/adapter/ImageSelectAdapter.java`
  - `app/src/main/java/run/yigou/gxzy/ui/adapter/ImagePreviewAdapter.java`
  - `app/src/main/java/run/yigou/gxzy/ui/adapter/VideoSelectAdapter.java`
- Dialog：
  - `app/src/main/java/run/yigou/gxzy/ui/dialog/AlbumDialog.java`
- View：
  - `app/src/main/java/run/yigou/gxzy/widget/PlayerView.java`

### 2.3 Manifest 注册

`app/src/main/AndroidManifest.xml` 当前注册媒体 Activity：

- `.ui.activity.CameraActivity`
- `.ui.activity.ImageCropActivity`
- `.ui.activity.ImageSelectActivity`
- `.ui.activity.ImagePreviewActivity`
- `.ui.activity.VideoPlayActivity`
- `.ui.activity.VideoPlayActivity$Portrait`
- `.ui.activity.VideoPlayActivity$Landscape`
- `.ui.activity.VideoSelectActivity`

其中 `VideoPlayActivity` 有普通、自适应方向、竖屏和横屏注册关系，需要后续迁移时保持一致。

### 2.4 已发现调用入口

静态搜索已发现：

- `app/src/main/java/run/yigou/gxzy/widget/BrowserView.java`
  - import `ImageSelectActivity`
  - import `VideoSelectActivity`
  - 调用 `ImageSelectActivity.start(...)`
  - 调用 `VideoSelectActivity.start(...)`
  - 使用 `VideoSelectActivity.VideoBean`
- `app/src/main/java/run/yigou/gxzy/ui/fragment/MyFragmentPersonal.java`
  - import `ImageCropActivity`
  - import `ImagePreviewActivity`
  - import `ImageSelectActivity`
- `app/src/main/java/run/yigou/gxzy/ui/fragment/MineFragment.java`
  - import `ImagePreviewActivity`
  - import `ImageSelectActivity`
  - import `VideoPlayActivity`

这些调用侧不在本批次修改范围；本批次只记录入口关系。

### 2.5 已发现资源候选

资源候选包括但不限于：

- Layout：
  - `app/src/main/res/layout/image_select_activity.xml`
  - `app/src/main/res/layout/image_select_item.xml`
  - `app/src/main/res/layout/image_preview_activity.xml`
  - `app/src/main/res/layout/image_preview_item.xml`
  - `app/src/main/res/layout/video_select_activity.xml`
  - `app/src/main/res/layout/video_select_item.xml`
  - `app/src/main/res/layout/video_play_activity.xml`
  - `app/src/main/res/layout/album_dialog.xml`
  - `app/src/main/res/layout/album_item.xml`
  - `app/src/main/res/layout/widget_player_view.xml`
- Drawable：
  - `video_volume_mute_ic.xml`
  - `video_volume_medium_ic.xml`
  - `video_volume_low_ic.xml`
  - `video_volume_high_ic.xml`
  - `video_schedule_rewind_ic.xml`
  - `video_schedule_forward_ic.xml`
  - `video_progress_bg.xml`
  - `video_progress_ball_bg.xml`
  - 以及其他以 image/video/album/camera/crop/preview/player 命名的资源，执行时需完整枚举。
- Mipmap：
  - `no_image.jpg`

注意：`tips_ai_msg_ifly_layout_mnotice_image.xml` 名称包含 image，但属于 AI Chat / Tips 范围，本批次应排除为媒体迁移资源。

### 2.6 已发现外部依赖候选

`app/build.gradle` 当前已有媒体相关依赖候选：

- 权限：`com.github.getActivity:XXPermissions:18.6`
- 图片加载：`com.github.bumptech.glide:glide:4.12.0` 与 `annotationProcessor 'com.github.bumptech.glide:compiler:4.12.0'`
- 图片预览手势：`com.github.Baseflow:PhotoView:2.3.0`
- 沉浸式：`com.gyf.immersionbar:immersionbar:3.0.0`
- ShapeView：`com.github.getActivity:ShapeView:8.3`
- TitleBar / Toaster / base / widget 等公共 UI 基础依赖

本批次不新增或移动依赖，只记录依赖归属，为后续 `feature/media` 设计提供依据。

## 3. 架构判断

阶段 3 的目标模块为 `feature/media`，但当前媒体代码仍与 app 多处入口、Manifest、资源和第三方依赖绑定。

本批次只建立迁移事实基线。后续是否直接新增 Gradle 模块，需要基于本批次输出判断：

```text
app
 ├─ BrowserView / MineFragment / MyFragmentPersonal 等调用侧
 ├─ AndroidManifest.xml 当前注册媒体 Activity
 └─ 当前仍承载媒体 Activity / Adapter / Dialog / PlayerView / 资源

阶段 3 目标候选：feature/media
 ├─ image select / preview / crop / camera
 ├─ video select / play
 ├─ AlbumDialog
 └─ PlayerView 或其归属待定
```

依赖方向目标仍是：

- 允许：`app -> feature:media`
- 允许：`feature:media -> library:*`
- 禁止：`feature:media -> app`

## 4. 本批次文件路径与组件关系

### 4.1 只读核对文件

执行时读取或搜索以下范围：

- `docs/ui-module-migration-plan.md`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`
- `settings.gradle`
- 媒体候选 Java：
  - `CameraActivity.java`
  - `ImageCropActivity.java`
  - `ImageSelectActivity.java`
  - `ImagePreviewActivity.java`
  - `VideoSelectActivity.java`
  - `VideoPlayActivity.java`
  - `ImageSelectAdapter.java`
  - `ImagePreviewAdapter.java`
  - `VideoSelectAdapter.java`
  - `AlbumDialog.java`
  - `PlayerView.java`
- 调用侧候选：
  - `BrowserView.java`
  - `MyFragmentPersonal.java`
  - `MineFragment.java`
- `app/src/main/res` 中媒体命名资源、布局引用、drawable/mipmap 引用。

### 4.2 唯一允许修改文件

- `docs/ui-module-migration-plan.md`

修改目的：记录阶段 3 批次 1 的事实基线、入口、Manifest、资源、依赖、阻塞与后续批次建议。

## 5. 类 / 函数 / API 变更

本批次不修改任何 Java / XML / Gradle 业务实现，因此：

- 不改变类名。
- 不改变包名。
- 不改变 Activity 注册。
- 不改变 `start(...)` 方法签名。
- 不改变回调接口。
- 不改变 Adapter 数据结构。
- 不改变媒体选择、预览、裁剪、拍摄、播放行为。

需要记录但不实施的 API 关系：

- `ImageSelectActivity.start(...)`
- `VideoSelectActivity.start(...)`
- `ImageSelectActivity.OnPhotoSelectListener`
- `VideoSelectActivity.OnVideoSelectListener`
- `VideoSelectActivity.VideoBean`
- `VideoPlayActivity` 及内部方向 Activity 注册关系

## 6. 数据结构与行为影响

本批次不新增或修改数据结构。

仅记录后续需确认的数据结构：

- 图片选择结果数据结构。
- 视频选择结果数据结构，尤其 `VideoSelectActivity.VideoBean` 被 `BrowserView` 直接引用。
- 相册分组数据结构，`VideoSelectActivity` 已使用 `AlbumDialog.AlbumInfo`。
- `PlayerView` 对播放状态、进度、锁屏、横竖屏切换等内部状态的封装。

行为影响：无。

## 7. 错误处理与边界条件

本批次只做事实确认和文档同步。

若执行中发现：

- 媒体类依赖 app 单例、数据库、HTTP、业务模型或全局状态；
- 媒体资源与 AI Chat / Reader / Account 资源混用；
- Manifest 注册与类结构存在不一致；
- `PlayerView` 归属不适合 `feature/media`；

则只记录为阻塞或风险，不做迁移绕过。

## 8. 依赖管理

本批次不修改 Gradle 依赖。

执行时仅记录候选依赖：

- `XXPermissions`
- `Glide` / `Glide compiler`
- `PhotoView`
- `ImmersionBar`
- `ShapeView`
- `TitleBar`
- `Toaster`
- `library:base`
- `library:widget`
- 其他通过 import / XML 自定义 View 发现的依赖

后续批次若新增 `feature/media`，必须再单独规划依赖搬迁方式，不能在本批次直接实施。

## 9. 实施边界

### 9.1 允许改动范围

仅允许修改：

- `docs/ui-module-migration-plan.md`

允许在文档中新增或更新：

- 阶段 3 批次 1 结果。
- 媒体 Activity Manifest 注册清单。
- 媒体入口引用清单。
- 媒体源码候选清单。
- 媒体资源候选清单。
- 媒体依赖候选清单。
- 阻塞 / 风险 / 后续批次建议。
- ADR：阶段 3 先做媒体边界盘点，不直接新增模块或迁移代码。

### 9.2 排除范围

不修改：

- `settings.gradle`
- `app/build.gradle`
- `common.gradle`
- 根 `build.gradle`
- `gradle/libs.versions.toml`
- `AndroidManifest.xml`
- 任意 Java 源码
- 任意 XML 资源
- 任意图片、drawable、mipmap、raw 资源
- README、AGENTS.md
- 阶段 4 AI Chat、阶段 5 Reader/Tips、阶段 6 Account
- 阶段 2 已完成内容

### 9.3 是否允许新增文件

不允许新增任何源码、资源、模块或文档文件。

本计划文件本身除外；执行阶段只更新现有迁移文档。

### 9.4 新文件落位

本批次无新文件落位。

### 9.5 文档同步

必须更新现有：

- `docs/ui-module-migration-plan.md`

不得新增平行文档。

## 10. 验证方法

### 10.1 静态检查

执行后检查文档包含：

1. 阶段 3 当前批次控制切换为“批次 1：确认入口、Manifest、资源”。
2. Manifest 注册清单包含 8 个媒体 Activity 注册项。
3. 入口引用记录包含 `BrowserView`、`MyFragmentPersonal`、`MineFragment`。
4. 候选源码清单包含 11 个候选 Java 文件。
5. 资源候选清单明确排除 `tips_ai_msg_ifly_layout_mnotice_image.xml` 归入 AI Chat / Tips。
6. 依赖候选记录包含 `XXPermissions`、`Glide`、`PhotoView`、`ImmersionBar` 等。
7. 阻塞或风险记录说明：本批次不直接新增 `feature/media`，因为需要先确认依赖、Manifest、资源和调用侧边界。
8. ADR 记录本批次决策。

### 10.2 构建验证

本批次只改文档，不要求运行 `gradlew.bat assembleDebug`。

如执行者选择补充基线验证，可运行 `gradlew.bat assembleDebug`，但构建结果仅作为基线记录，不作为本批次必须条件。

### 10.3 文档一致性

检查 `docs/ui-module-migration-plan.md` 不再显示阶段 3 为完全未盘点状态；应明确阶段 3 批次 1 已完成事实确认或正在执行。

## 11. 风险与后续判断

### 11.1 风险

- 风险 1：媒体 Activity 当前在 Manifest 中集中注册，迁移到 Gradle 模块后需要重新设计 Manifest 合并与包名引用。
- 风险 2：`BrowserView` 属于 app widget 包，直接引用 `ImageSelectActivity`、`VideoSelectActivity` 和 `VideoSelectActivity.VideoBean`，后续模块化可能需要 API 包或调用侧改造。
- 风险 3：`MyFragmentPersonal` / `MineFragment` 属于账户或首页入口，后续迁移媒体时需避免扩展到账户、首页业务重构。
- 风险 4：`PlayerView` 位于 `run.yigou.gxzy.widget`，可能属于通用 widget 或 media 专属组件，归属需在后续批次单独确认。
- 风险 5：资源命名中包含 `image` 的文件不一定属于媒体模块，例如 AI Chat / Tips 的图片消息布局，资源迁移必须逐项确认。

### 11.2 后续建议

批次 1 完成后，下一批次不应直接大规模迁移全部媒体代码。建议阶段 3 批次 2 先规划模块策略：

- 若依赖可控：新增 `feature/media` 空模块并接入 app。
- 若依赖仍重：先在 app 内建立 `feature/media` 包做包级聚合，再评估 Gradle 模块化。

## 12. 原子化实施清单

1. 读取本计划和 `docs/ui-module-migration-plan.md`。
2. 读取 `AndroidManifest.xml` 中媒体 Activity 注册片段。
3. 搜索媒体候选 Activity / Adapter / Dialog / View 文件是否存在。
4. 搜索媒体 Activity 在 app 源码和 Manifest 中的引用入口。
5. 搜索媒体候选 Java 的资源引用、第三方依赖 import 和 app 内部依赖。
6. 枚举 `app/src/main/res` 中媒体命名资源，并标记疑似非媒体资源。
7. 读取 `app/build.gradle`，记录媒体候选第三方依赖。
8. 更新 `docs/ui-module-migration-plan.md`：阶段 3 当前批次控制、批次 1 结果、入口/Manifest/资源/依赖清单、风险和后续建议。
9. 新增 ADR：阶段 3 先做媒体边界盘点，不直接新增模块或迁移代码。
10. 静态检查文档内容与实际搜索结果一致。
11. 不运行构建，除非执行者需要补充基线验证。
12. 汇总批次 1 结论，并等待后续批次计划确认。
