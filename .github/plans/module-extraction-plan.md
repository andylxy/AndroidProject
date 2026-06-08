# AndroidProject 模块解耦与抽取实施计划

> **版本**: v1.1  
> **创建日期**: 2026-06-08  
> **更新日期**: 2026-06-09  
> **状态**: 阶段一~四已完成 ✅（BUILD SUCCESSFUL）  
> **关联分析**: 见 RESEARCH 阶段（耦合分析）和 INNOVATE 阶段（方案权衡）

---

## 一、包名约定规则

| 模块类型 | 包名前缀 | 说明 |
|----------|----------|------|
| 现有 Library（`:library:base`、`:library:widget`、`:library:umeng`） | `com.hjq.*` | 来自 AndroidProject 模板，保持不变 |
| 现有 Library（`:library:xbus`） | `com.lucas.*` | 第三方事件总线，保持不变 |
| **新建 Library** | `run.yigou.gxzy.*` | 与 app 模块统一，本项目自有代码 |
| app 模块 | `run.yigou.gxzy` | 保持不变 |

> **关键原则**：新建模块的 Java 包名使用 `run.yigou.gxzy.<模块名>`，与 app 模块同根。被抽取的代码原本就在 `run.yigou.gxzy.*` 下，因此 **包名基本不变，只改模块归属**，import 替换量最小化。

---

## 二、实施边界

| 维度 | 说明 |
|------|------|
| **允许新增** | 3 个新 Library 模块（`:library:log`、`:library:crypto`、`:library:sse-client`）及其完整目录结构、`build.gradle`、`AndroidManifest.xml` |
| **允许修改** | `settings.gradle`（include 新模块）、`:app/build.gradle`（依赖调整）、`:library:widget/build.gradle`（依赖补充）、`:library:base/build.gradle`（依赖补充）、受影响 Java 文件的 import 语句 |
| **排除范围** | 不修改业务逻辑、不修改 `:library:umeng` 和 `:library:xbus` 模块、不修改 `localMaven/`、不修改 AOP 切面实现（仅移动注解）、不修改 greenDAO 实体和服务 |
| **文档同步** | 需更新 `README.md` 中的模块架构说明 |
| **验证策略** | 每阶段完成后执行 `./gradlew :app:assembleDebug` 验证编译通过；全部完成后执行 `./gradlew :app:assembleRelease` |

---

## 三、目标架构

```
AndroidProject-old/
├── app/                          ← 纯业务层（页面、业务逻辑、API 定义）
│   └── src/main/java/run/yigou/gxzy/
│       ├── action/               ← TitleBar/Toast/Status Action 接口
│       ├── adapter/              ← 业务 Adapter
│       ├── aop/                  ← Aspect 切面实现（注解已移入 base）
│       ├── app/                  ← Application/Activity/Fragment 基类
│       ├── common/               ← 业务常量
│       ├── EventBus/             ← 业务事件定义
│       ├── greendao/             ← 数据库（实体/服务/工具）
│       ├── http/                 ← 网络层（API/Server/security/glide）
│       ├── manager/              ← 业务管理器
│       ├── openapi/              ← 开放 API 文档
│       ├── other/                ← 残留通用工具（有第三方依赖的）
│       ├── ui/                   ← 业务页面
│       ├── utils/                ← 残留工具类（DebugLog 等）
│       └── wxapi/                ← 微信回调
│
├── library/
│   ├── base/                     ← Activity/Fragment 基类 + Action 接口 + AOP 注解
│   │   └── src/main/java/com/hjq/base/
│   │       ├── *.java            ← BaseActivity, BaseFragment, BaseDialog...
│   │       ├── action/           ← ActivityAction, ClickAction...
│   │       └── aop/              ← 【新】CheckNet, Log, Permissions, SingleClick 注解
│   │
│   ├── widget/                   ← 所有自定义 View/Layout 组件
│   │   └── src/main/java/com/hjq/widget/
│   │       ├── view/             ← SwitchButton, SubmitButton... + 【迁移】BrowserView, PasswordView, PlayerView, PhotoViewPager
│   │       └── layout/           ← SettingBar, WrapRecyclerView... + 【迁移】StatusLayout, XCollapsingToolbarLayout, InterceptableScrollView
│   │
│   ├── umeng/                    ← 友盟 SDK 封装
│   │   └── src/main/java/com/hjq/umeng/
│   │
│   ├── xbus/                     ← 事件总线
│   │   └── src/main/java/com/lucas/xbus/
│   │
│   ├── log/                      ← 【新】统一日志库
│   │   └── src/main/java/run/yigou/gxzy/log/
│   │       └── EasyLog.java
│   │
│   ├── crypto/                   ← 【新】加密安全库
│   │   └── src/main/java/run/yigou/gxzy/crypto/
│   │       ├── SecurityUtils.java
│   │       ├── CryptogramUtil.java
│   │       ├── SM2Util.java
│   │       ├── SM4Util.java
│   │       ├── AESUtil.java
│   │       ├── MD5Util.java
│   │       ├── RC4Helper.java
│   │       └── sm/
│   │           ├── SM2CryptoUtil.java
│   │           ├── SM4CryptoUtil.java
│   │           └── Sm4Context.java
│   │
│   └── sse-client/               ← 【新】SSE 流式客户端
│       └── src/main/java/run/yigou/gxzy/sse/
│           ├── SseClient.java          ← 门面（新建）
│           ├── SseLogger.java          ← 日志接口（新建）
│           ├── SseClientHelper.java
│           ├── SseEventHandler.java
│           ├── SseRequestBuilder.java
│           ├── SseStreamCallback.java
│           ├── SseChunk.java
│           └── Tls12SocketFactory.java
```

---

## 四、分阶段实施清单

### 阶段一：Widget 补充迁移（最低风险）

**目标**：将 app 模块 `widget/` 下 7 个通用 UI 组件移入 `:library:widget`

#### 1.1 迁移文件清单

| # | 源路径（app 模块） | 目标路径（widget 模块） | 包名 | 状态 |
|---|-------------------|------------------------|------|------|
| 1 | `app/.../widget/BrowserView.java` | — | — | ❌ **不迁移**：深度耦合 app（引用 ImageSelectActivity、VideoSelectActivity、InputDialog、MessageDialog、TipsDialog、AppConfig、PermissionCallback 等） |
| 2 | `app/.../widget/InterceptableScrollView.java` | `library/widget/.../layout/InterceptableScrollView.java` | `com.hjq.widget.layout` | ✅ |
| 3 | `app/.../widget/PasswordView.java` | `library/widget/.../view/PasswordView.java` | `com.hjq.widget.view` | ✅ |
| 4 | `app/.../widget/PhotoViewPager.java` | `library/widget/.../view/PhotoViewPager.java` | `com.hjq.widget.view` | ✅ |
| 5 | `app/.../widget/PlayerView.java` | — | — | ❌ **不迁移**：深度耦合 app（引用 MessageDialog、R.layout.widget_player_view、R.id.* 等） |
| 6 | `app/.../widget/StatusLayout.java` | `library/widget/.../layout/StatusLayout.java` | `com.hjq.widget.layout` | ✅ |
| 7 | `app/.../widget/XCollapsingToolbarLayout.java` | `library/widget/.../layout/XCollapsingToolbarLayout.java` | `com.hjq.widget.layout` | ✅ |

#### 1.2 Gradle 变更

**文件**: `library/widget/build.gradle`

```groovy
dependencies {
    implementation project(':library:base')
    // 新增：PhotoViewPager 需要
    api 'com.github.Baseflow:PhotoView:2.3.0'
    // 新增：StatusLayout 需要 Lottie 动画
    api 'com.airbnb.android:lottie:4.1.0'
}
```

#### 1.3 R 资源处理

`StatusLayout` 和 `BrowserView` 引用 `run.yigou.gxzy.R`，迁移后需：
- 将引用的 layout/drawable/string 资源从 app 模块复制到 widget 模块的 `res/` 目录
- 将代码中的 `run.yigou.gxzy.R` 改为 `com.hjq.widget.R`
- 具体资源文件清单需在迁移时逐个确认

#### 1.4 Import 替换清单（10 处）

| 文件 | 旧 import | 新 import |
|------|-----------|-----------|
| `action/StatusAction.java` | `run.yigou.gxzy.widget.StatusLayout` | `com.hjq.widget.layout.StatusLayout` |
| `ui/fragment/HomeFragment.java` | `run.yigou.gxzy.widget.XCollapsingToolbarLayout` | `com.hjq.widget.layout.XCollapsingToolbarLayout` |
| `ui/fragment/BrowserFragment.java` | `run.yigou.gxzy.widget.StatusLayout` | `com.hjq.widget.layout.StatusLayout` |
| `ui/dialog/PayPasswordDialog.java` | `run.yigou.gxzy.widget.PasswordView` | `com.hjq.widget.view.PasswordView` |
| `ui/activity/ImageSelectActivity.java` | `run.yigou.gxzy.widget.StatusLayout` | `com.hjq.widget.layout.StatusLayout` |
| `ui/activity/StatusActivity.java` | `run.yigou.gxzy.widget.StatusLayout` | `com.hjq.widget.layout.StatusLayout` |
| `ui/activity/VideoPlayActivity.java` | `run.yigou.gxzy.widget.PlayerView` | `com.hjq.widget.view.PlayerView` |
| `ui/activity/VideoSelectActivity.java` | `run.yigou.gxzy.widget.StatusLayout` | `com.hjq.widget.layout.StatusLayout` |

> **注**：`BrowserView`（BrowserFragment、BrowserActivity）和 `PlayerView`（VideoSelectAdapter、VideoPlayActivity）因不迁移，对应 import 替换延后。

#### 1.5 已知问题与修复

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `PasswordView.java` R.dimen 解析失败 | widget 模块原本没有 dp_41/dp_44 | 在 dimens.xml 中添加 |
| `PasswordView.java` "程序包R不存在" | 子包 `com.hjq.widget.view` 无法直接访问父包 `com.hjq.widget.R` | 使用完整包名 `com.hjq.widget.R.dimen` |
| `PasswordView.java` "可能已分配变量mItemWidth" | final 字段在 try-catch 中赋值（多赋值路径） | 移除 final 修饰符 |

#### 1.6 验证

**widget 模块编译**：✅ 已通过
```bash
./gradlew :library:widget:assembleDebug
# BUILD SUCCESSFUL in 19s
```

**完整 app 模块编译**：✅ 已通过
```bash
./gradlew :app:assembleDebug
# BUILD SUCCESSFUL in 1m 4s
```

#### 1.7 Import 替换完成清单（8 处）

| # | 文件 | 替换结果 |
|---|------|----------|
| 1 | `action/StatusAction.java` | ✅ `StatusLayout` → `com.hjq.widget.layout.StatusLayout` |
| 2 | `ui/fragment/HomeFragment.java` | ✅ `XCollapsingToolbarLayout` → `com.hjq.widget.layout.XCollapsingToolbarLayout` |
| 3 | `ui/fragment/BrowserFragment.java` | ✅ `StatusLayout` → `com.hjq.widget.layout.StatusLayout` |
| 4 | `ui/dialog/PayPasswordDialog.java` | ✅ `PasswordView` → `com.hjq.widget.view.PasswordView` |
| 5 | `ui/activity/BrowserActivity.java` | ✅ `StatusLayout` → `com.hjq.widget.layout.StatusLayout` |
| 6 | `ui/activity/ImageSelectActivity.java` | ✅ `StatusLayout` → `com.hjq.widget.layout.StatusLayout` |
| 7 | `ui/activity/StatusActivity.java` | ✅ `StatusLayout` → `com.hjq.widget.layout.StatusLayout` |
| 8 | `ui/activity/VideoSelectActivity.java` | ✅ `StatusLayout` → `com.hjq.widget.layout.StatusLayout` |

#### 1.8 延期任务

| 延期原因 | 文件 | 原因 |
|----------|------|------|
| BrowserView 不迁移 | BrowserFragment.java, BrowserActivity.java | BrowserView 深度耦合 app 模块（引用 LoginActivity、AppConfig、DialogUtils 等），需要单独规划 |
| PlayerView 不迁移 | VideoSelectActivity.java | PlayerView 引用 `run.yigou.gxzy.R` 和 `VideoPlayActivity`，需要与 app 资源一起迁移或重构 |

---

---

### 阶段二：Other 工具迁移至 base（低风险）

**目标**：将 app 模块 `other/` 下无第三方依赖的通用工具移入 `:library:base`

#### 2.1 迁移判定表

| 文件 | 外部依赖 | 是否迁移 | 去向 |
|------|----------|----------|------|
| `KeyboardWatcher.java` | 仅 Android SDK | ✅ 迁移 | `com.hjq.base` |
| `DoubleClickHelper.java` | 仅 Android SDK | ✅ 迁移 | `com.hjq.base` |
| `GridSpaceDecoration.java` | RecyclerView（AndroidX） | ✅ 迁移 | `com.hjq.base` |
| `ArrowDrawable.java` | 仅 Android SDK（但引用 `run.yigou.gxzy.R`） | ✅ 迁移 | `com.hjq.base` |
| `PermissionCallback.java` | XXPermissions + Toaster + ActivityManager | ❌ 不迁移 | 留在 app |
| `CrashHandler.java` | CrashActivity + RestartActivity | ❌ 不迁移 | 留在 app |
| `TitleBarStyle.java` | TitleBar 库 | ❌ 不迁移 | 留在 app |
| `ToastStyle.java` | Toaster 库 | ❌ 不迁移 | 留在 app |
| `MaterialHeader.java` | SmartRefreshLayout | ❌ 不迁移 | 留在 app |
| `SmartBallPulseFooter.java` | SmartRefreshLayout | ❌ 不迁移 | 留在 app |
| `DebugLoggerTree.java` | Timber | ❌ 不迁移 | 留在 app |
| `ToastLogInterceptor.java` | EasyHttp + Toaster | ❌ 不迁移 | 留在 app |

#### 2.2 迁移文件清单

| # | 源路径 | 目标路径 | 新包名 |
|---|--------|----------|--------|
| 1 | `app/.../other/KeyboardWatcher.java` | `library/base/.../base/KeyboardWatcher.java` | `com.hjq.base` |
| 2 | `app/.../other/DoubleClickHelper.java` | `library/base/.../base/DoubleClickHelper.java` | `com.hjq.base` |
| 3 | `app/.../other/GridSpaceDecoration.java` | `library/base/.../base/GridSpaceDecoration.java` | `com.hjq.base` |
| 4 | `app/.../other/ArrowDrawable.java` | `library/base/.../base/ArrowDrawable.java` | `com.hjq.base` |

#### 2.3 ArrowDrawable 特殊处理

`ArrowDrawable` 引用 `run.yigou.gxzy.R`（颜色和尺寸资源），迁移后：
- 在 `library/base/src/main/res/values/` 创建对应资源，或将硬编码引用改为构造函数参数注入
- 代码中 `run.yigou.gxzy.R` → `com.hjq.base.R`

#### 2.4 Import 替换清单（4 处）

| 文件 | 旧 import | 新 import |
|------|-----------|-----------|
| `ui/popup/ListPopup.java` | `run.yigou.gxzy.other.ArrowDrawable` | `com.hjq.base.ArrowDrawable` |
| `ui/activity/HomeActivity.java` | `run.yigou.gxzy.other.DoubleClickHelper` | `com.hjq.base.DoubleClickHelper` |
| `ui/activity/ImageSelectActivity.java` | `run.yigou.gxzy.other.GridSpaceDecoration` | `com.hjq.base.GridSpaceDecoration` |
| `ui/activity/LoginActivity.java` | `run.yigou.gxzy.other.KeyboardWatcher` | `com.hjq.base.KeyboardWatcher` |
| `ui/activity/VideoSelectActivity.java` | `run.yigou.gxzy.other.GridSpaceDecoration` | `com.hjq.base.GridSpaceDecoration` |

> 注意：`CrashHandler` 和 `PermissionCallback` 留在 app 的 `other/` 包中，import 不变。

#### 2.5 验证

**base 模块编译**：✅ 已通过
```bash
./gradlew :library:base:assembleDebug
```

**完整 app 模块编译**：✅ 已通过
```bash
./gradlew :app:assembleDebug
# BUILD SUCCESSFUL in 53s
```

#### 2.6 迁移文件清单（4 个）

| # | 源文件 | 目标路径 | 状态 |
|---|--------|----------|------|
| 1 | `DoubleClickHelper.java` | `library/base/.../DoubleClickHelper.java` | ✅ |
| 2 | `GridSpaceDecoration.java` | `library/base/.../GridSpaceDecoration.java` | ✅ |
| 3 | `KeyboardWatcher.java` | `library/base/.../KeyboardWatcher.java` | ✅ |
| 4 | `ArrowDrawable.java` | `library/base/.../ArrowDrawable.java` | ✅ |

#### 2.7 Import 替换完成清单（5 处）

| # | 文件 | 替换结果 |
|---|------|----------|
| 1 | `ui/popup/ListPopup.java` | ✅ `ArrowDrawable` → `com.hjq.base.ArrowDrawable` |
| 2 | `ui/activity/HomeActivity.java` | ✅ `DoubleClickHelper` → `com.hjq.base.DoubleClickHelper` |
| 3 | `ui/activity/ImageSelectActivity.java` | ✅ `GridSpaceDecoration` → `com.hjq.base.GridSpaceDecoration` |
| 4 | `ui/activity/LoginActivity.java` | ✅ `KeyboardWatcher` → `com.hjq.base.KeyboardWatcher` |
| 5 | `ui/activity/VideoSelectActivity.java` | ✅ `GridSpaceDecoration` → `com.hjq.base.GridSpaceDecoration` |

---

---

### 阶段三：新建 `:library:log` 日志模块（中风险）

**目标**：将 `EasyLog` 抽取为独立模块

#### 3.1 新建模块结构

```
library/log/
├── build.gradle
├── proguard-rules.pro
└── src/main/
    ├── AndroidManifest.xml
    └── java/run/yigou/gxzy/log/
        └── EasyLog.java
```

#### 3.2 `library/log/build.gradle`

```groovy
apply plugin: 'com.android.library'
apply from: '../../common.gradle'

android {
    namespace 'run.yigou.gxzy.log'
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

// 零外部依赖，仅使用 android.util.Log
```

#### 3.3 `library/log/src/main/AndroidManifest.xml`

```xml
<manifest package="run.yigou.gxzy.log" />
```

#### 3.4 迁移文件

| # | 源路径 | 目标路径 | 包名（不变） |
|---|--------|----------|-------------|
| 1 | `app/.../utils/EasyLog.java` | `library/log/.../log/EasyLog.java` | `run.yigou.gxzy.log` |

> **注意**：包名从 `run.yigou.gxzy.utils` 变为 `run.yigou.gxzy.log`，需要替换 import。

#### 3.5 `settings.gradle` 变更

```groovy
include ':library:log'          // 新增
```

#### 3.6 `:app/build.gradle` 依赖变更

```groovy
dependencies {
    implementation project(':library:log')   // 新增
    // ...existing code...
}
```

#### 3.7 Import 替换清单（48 处）

所有 `import run.yigou.gxzy.utils.EasyLog;` → `import run.yigou.gxzy.log.EasyLog;`

涉及文件：
- `app/AppApplication.java`、`app/AppDataInitializer.java`
- `http/Server/RequestHandler.java`、`http/security/SecurityConfig.java`
- `http/sse/SseClientHelper.java`、`http/sse/AiStreamRequestBuilder.java`、`http/sse/AiStreamEventHandler.java`
- `http/api/AiStreamApi.java`
- `manager/AiChatManager.java`、`manager/ChatSessionManager.java`
- `greendao/util/ConvertEntity.java`
- `utils/RC4Helper.java`
- `ui/activity/AboutActivity.java`、`ui/activity/BookContentSearchActivity.java`、`ui/activity/YaoUintActivity.java`
- `ui/fragment/HomeFragment.java`、`ui/fragment/TipsBookNetReadFragment.java`、`ui/fragment/TipsFangYaoFragment.java`、`ui/fragment/TipsSettingFragment.java`、`ui/fragment/AiMsgFragment.java`、`ui/fragment/BookCollectCaseFragment.java`
- `ui/helper/ChatSidebarHelper.java`、`ui/helper/ChatSummaryHelper.java`
- `ui/adapter/SearchBookAdapter.java`
- `ui/tips/widget/TipsLittleWindow.java`、`ui/tips/widget/TipsLittleRecyclerViewWindow.java`、`ui/tips/widget/LocalLinkMovementMethod.java`
- `ui/tips/utils/SearchDataAdapter.java`、`ui/tips/utils/SearchCoordinator.java`、`ui/tips/utils/PopupDataAdapter.java`
- `ui/tips/tipsutils/TipsUIHelper.java`、`ui/tips/tipsutils/TipsSearchEngine.java`、`ui/tips/tipsutils/TipsNetHelper.java`、`ui/tips/tipsutils/ChapterDownloadManager.java`
- `ui/tips/repository/BookRepository.java`
- `ui/tips/presenter/TipsBookReadPresenter.java`、`ui/tips/presenter/AiMsgPresenter.java`
- `ui/tips/entity/GroupModel.java`、`ui/tips/entity/ChildEntity.java`
- `ui/tips/data/ChapterIndexBuilder.java`、`ui/tips/data/BookDataManager.java`
- `ui/tips/adapter/RefactoredPopupAdapter.java`
- `ui/tips/adapter/refactor/viewholder/TipsChildViewHolder.java`
- `ui/tips/adapter/refactor/RefactoredExpandableAdapter.java`
- `ui/tips/adapter/refactor/model/DataAdapter.java`
- `ui/tips/adapter/refactor/binder/ChildTextBinder.java`
- `ui/tips/adapter/refactor/event/ReadModeLongClickHandler.java`

> `DebugLog` 不迁移（依赖 `UtilCode` 和 `BuildConfig.DEBUG`），保留在 app 的 `utils/` 包中。

#### 3.8 验证

```bash
./gradlew :library:log:assembleDebug
./gradlew :app:assembleDebug
```

#### 3.9 完成清单 ✅

- [x] 创建 `library/log` 模块结构
- [x] 创建 `library/log/build.gradle`（零外部依赖）
- [x] 创建 `library/log/src/main/AndroidManifest.xml`
- [x] 创建 `library/log/src/main/java/run/yigou/gxzy/log/EasyLog.java`
- [x] 更新 `settings.gradle` 添加 `:library:log`
- [x] 更新 `app/build.gradle` 添加 `implementation project(':library:log')`
- [x] 48 处 import 替换（`utils.EasyLog` → `log.EasyLog`）
- [x] 移除 48 个文件的 UTF-8 BOM 字符（PowerShell 写入问题）
- [x] 编译验证：`BUILD SUCCESSFUL in 1m 5s`

---

### 阶段四：新建 `:library:crypto` 加密安全库（中风险）

**目标**：将 SM2/SM4/AES/RC4/MD5 等加密能力抽取为独立模块

#### 4.1 新建模块结构

```
library/crypto/
├── build.gradle
├── proguard-rules.pro
└── src/main/
    ├── AndroidManifest.xml
    └── java/run/yigou/gxzy/crypto/
        ├── SecurityUtils.java          ← 统一门面
        ├── CryptogramUtil.java
        ├── SM2Util.java
        ├── SM4Util.java
        ├── AESUtil.java
        ├── MD5Util.java
        ├── RC4Helper.java
        └── sm/
            ├── SM2CryptoUtil.java
            ├── SM4CryptoUtil.java
            └── Sm4Context.java
```

#### 4.2 `library/crypto/build.gradle`

```groovy
apply plugin: 'com.android.library'
apply from: '../../common.gradle'

android {
    namespace 'run.yigou.gxzy.crypto'
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    // 国密算法支持
    api 'org.bouncycastle:bcprov-jdk15to18:1.76'
    // RSA 工具
    api 'com.github.gzu-liyujiang:RSAUtils:2.0.0'
}
```

#### 4.3 `library/crypto/src/main/AndroidManifest.xml`

```xml
<manifest package="run.yigou.gxzy.crypto" />
```

#### 4.4 迁移文件清单

| # | 源路径（app 模块） | 目标路径（crypto 模块） | 旧包名 | 新包名 |
|---|-------------------|------------------------|--------|--------|
| 1 | `Security/SecurityUtils.java` | `crypto/.../SecurityUtils.java` | `run.yigou.gxzy.Security` | `run.yigou.gxzy.crypto` |
| 2 | `Security/Cryptogram/CryptogramUtil.java` | `crypto/.../CryptogramUtil.java` | `run.yigou.gxzy.Security.Cryptogram` | `run.yigou.gxzy.crypto` |
| 3 | `Security/Cryptogram/SM2Util.java` | `crypto/.../SM2Util.java` | `run.yigou.gxzy.Security.Cryptogram` | `run.yigou.gxzy.crypto` |
| 4 | `Security/Cryptogram/SM4Util.java` | `crypto/.../SM4Util.java` | `run.yigou.gxzy.Security.Cryptogram` | `run.yigou.gxzy.crypto` |
| 5 | `Security/Cryptogram/Sm/SM2CryptoUtil.java` | `crypto/.../sm/SM2CryptoUtil.java` | `run.yigou.gxzy.Security.Cryptogram.Sm` | `run.yigou.gxzy.crypto.sm` |
| 6 | `Security/Cryptogram/Sm/SM4CryptoUtil.java` | `crypto/.../sm/SM4CryptoUtil.java` | `run.yigou.gxzy.Security.Cryptogram.Sm` | `run.yigou.gxzy.crypto.sm` |
| 7 | `Security/Cryptogram/Sm/Sm4Context.java` | `crypto/.../sm/Sm4Context.java` | `run.yigou.gxzy.Security.Cryptogram.Sm` | `run.yigou.gxzy.crypto.sm` |
| 8 | `utils/AESUtil.java` | `crypto/.../AESUtil.java` | `run.yigou.gxzy.utils` | `run.yigou.gxzy.crypto` |
| 9 | `utils/MD5Util.java` | `crypto/.../MD5Util.java` | `run.yigou.gxzy.utils` | `run.yigou.gxzy.crypto` |
| 10 | `utils/RC4Helper.java` | `crypto/.../RC4Helper.java` | `run.yigou.gxzy.utils` | `run.yigou.gxzy.crypto` |

#### 4.5 SecurityUtils 硬编码公钥处理

当前 `SecurityUtils` 中硬编码了默认 SM2 公钥：
```java
private static final String DEFAULT_PUBLIC_KEY = "04CF658C65FB80CB5C7B91D3BD881521...";
```

迁移后改为配置注入：
```java
// 新增初始化方法
public static void init(String sm2PublicKey) {
    // 由 AppApplication 在启动时调用
}
```
移除 `DEFAULT_PUBLIC_KEY` 硬编码。

#### 4.6 `settings.gradle` 变更

```groovy
include ':library:crypto'       // 新增
```

#### 4.7 `:app/build.gradle` 依赖变更

```groovy
dependencies {
    implementation project(':library:crypto')   // 新增，替代直接依赖
    // 移除以下直接依赖（由 crypto 模块传递）：
    // implementation 'org.bouncycastle:bcprov-jdk15to18:1.76'
    // implementation 'com.github.gzu-liyujiang:RSAUtils:2.0.0'
}
```

#### 4.8 Import 替换清单（约 27 处）

| 旧 import | 新 import |
|-----------|-----------|
| `run.yigou.gxzy.Security.SecurityUtils` | `run.yigou.gxzy.crypto.SecurityUtils` |
| `run.yigou.gxzy.Security.Cryptogram.CryptogramUtil` | `run.yigou.gxzy.crypto.CryptogramUtil` |
| `run.yigou.gxzy.Security.Cryptogram.SM2Util` | `run.yigou.gxzy.crypto.SM2Util` |
| `run.yigou.gxzy.Security.Cryptogram.SM4Util` | `run.yigou.gxzy.crypto.SM4Util` |
| `run.yigou.gxzy.Security.Cryptogram.Sm.SM2CryptoUtil` | `run.yigou.gxzy.crypto.sm.SM2CryptoUtil` |
| `run.yigou.gxzy.utils.AESUtil` | `run.yigou.gxzy.crypto.AESUtil` |
| `run.yigou.gxzy.utils.MD5Util` | `run.yigou.gxzy.crypto.MD5Util` |
| `run.yigou.gxzy.utils.RC4Helper` | `run.yigou.gxzy.crypto.RC4Helper` |

外部引用文件：
- `app/AppApplication.java`
- `manager/ChatSessionManager.java`
- `greendao/util/ConvertEntity.java`
- `greendao/service/ChatSessionBeanService.java`
- `greendao/service/ChatMessageBeanService.java`
- `greendao/service/ChatSummaryBeanService.java`
- `ui/activity/LoginActivity.java`

内部交叉引用（crypto 模块内文件互相引用）也需同步更新。

#### 4.9 验证

```bash
./gradlew :library:crypto:assembleDebug    # ✅ BUILD SUCCESSFUL
./gradlew :app:assembleDebug               # ✅ BUILD SUCCESSFUL
```

**完成状态**: ✅ **阶段四已完成**（2026-06-09）  
**编译结果**: `:library:crypto:assembleDebug` 和 `:app:assembleDebug` 均 BUILD SUCCESSFUL  
**耗时**: ~22s

---

### 阶段五：新建 `:library:sse-client` SSE 客户端库（中风险）

**目标**：将 SSE 流式通信能力抽取为独立模块

#### 5.1 新建模块结构

```
library/sse-client/
├── build.gradle
├── proguard-rules.pro
└── src/main/
    ├── AndroidManifest.xml
    └── java/run/yigou/gxzy/sse/
        ├── SseClient.java              ← 门面类（新建，Builder 模式）
        ├── SseLogger.java              ← 日志接口（新建）
        ├── SseClientHelper.java        ← 从 http/sse/ 迁移
        ├── SseEventHandler.java        ← 从 http/sse/AiStreamEventHandler 重命名
        ├── SseRequestBuilder.java      ← 从 http/sse/AiStreamRequestBuilder 重命名
        ├── SseStreamCallback.java      ← 从 http/callback/ 迁移
        ├── SseChunk.java               ← 从 http/model/ 迁移
        └── Tls12SocketFactory.java     ← 从 http/sse/ 迁移（如果存在）
```

#### 5.2 解耦设计

当前 `AiStreamEventHandler` 依赖 `EasyLog` 和 `Gson`。迁移后通过接口注入：

```java
// SseClient 门面 API
public final class SseClient {
    public static class Builder {
        private OkHttpClient okHttpClient;      // 必须注入
        private SseLogger logger;               // 可选注入
        private Gson gson;                      // 可选注入

        public Builder okHttpClient(OkHttpClient client) { ... }
        public Builder logger(SseLogger logger) { ... }
        public Builder gson(Gson gson) { ... }
        public SseClient build() { ... }
    }

    public void connect(String url, String body, SseStreamCallback callback) { ... }
}

// 日志接口
public interface SseLogger {
    void log(String tag, String message);
}
```

#### 5.3 `library/sse-client/build.gradle`

```groovy
apply plugin: 'com.android.library'
apply from: '../../common.gradle'

android {
    namespace 'run.yigou.gxzy.sse'
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    // OkHttp SSE 支持
    api 'com.squareup.okhttp3:okhttp:5.3.0'
    api 'com.squareup.okhttp3:okhttp-sse:5.3.0'
    // JSON 解析（compileOnly，由 app 层提供具体实现）
    compileOnly 'com.google.code.gson:gson:2.8.8'
}
```

#### 5.4 `library/sse-client/src/main/AndroidManifest.xml`

```xml
<manifest package="run.yigou.gxzy.sse" />
```

#### 5.5 迁移文件清单

| # | 源路径（app 模块） | 目标路径（sse-client 模块） | 旧包名 | 新包名 |
|---|-------------------|---------------------------|--------|--------|
| 1 | `http/sse/SseClientHelper.java` | `sse-client/.../SseClientHelper.java` | `run.yigou.gxzy.http.sse` | `run.yigou.gxzy.sse` |
| 2 | `http/sse/AiStreamEventHandler.java` | `sse-client/.../SseEventHandler.java` | `run.yigou.gxzy.http.sse` | `run.yigou.gxzy.sse` |
| 3 | `http/sse/AiStreamRequestBuilder.java` | `sse-client/.../SseRequestBuilder.java` | `run.yigou.gxzy.http.sse` | `run.yigou.gxzy.sse` |
| 4 | `http/callback/SseStreamCallback.java` | `sse-client/.../SseStreamCallback.java` | `run.yigou.gxzy.http.callback` | `run.yigou.gxzy.sse` |
| 5 | `http/model/SseChunk.java` | `sse-client/.../SseChunk.java` | `run.yigou.gxzy.http.model` | `run.yigou.gxzy.sse` |
| 6 | —（新建） | `sse-client/.../SseClient.java` | — | `run.yigou.gxzy.sse` |
| 7 | —（新建） | `sse-client/.../SseLogger.java` | — | `run.yigou.gxzy.sse` |

#### 5.6 `settings.gradle` 变更

```groovy
include ':library:sse-client'   // 新增
```

#### 5.7 `:app/build.gradle` 依赖变更

```groovy
dependencies {
    implementation project(':library:sse-client')   // 新增
}
```

#### 5.8 Import 替换清单

| 文件 | 旧 import | 新 import |
|------|-----------|-----------|
| `manager/AiChatManager.java` | `run.yigou.gxzy.http.sse.*` | `run.yigou.gxzy.sse.*` |
| `manager/AiChatManager.java` | `run.yigou.gxzy.http.callback.SseStreamCallback` | `run.yigou.gxzy.sse.SseStreamCallback` |
| `manager/AiChatManager.java` | `run.yigou.gxzy.http.model.SseChunk` | `run.yigou.gxzy.sse.SseChunk` |
| `http/api/AiStreamApi.java` | SSE 相关引用 | `run.yigou.gxzy.sse.*` |

#### 5.9 验证

```bash
./gradlew :library:sse-client:assembleDebug
./gradlew :app:assembleDebug
```

---

### 阶段六：AOP 注解迁移至 base（低风险）

**目标**：将 AOP 注解移入 `:library:base`，切面实现暂留 app

#### 6.1 迁移文件清单

| # | 源路径 | 目标路径 | 旧包名 | 新包名 |
|---|--------|----------|--------|--------|
| 1 | `app/.../aop/CheckNet.java` | `library/base/.../base/aop/CheckNet.java` | `run.yigou.gxzy.aop` | `com.hjq.base.aop` |
| 2 | `app/.../aop/Log.java` | `library/base/.../base/aop/Log.java` | `run.yigou.gxzy.aop` | `com.hjq.base.aop` |
| 3 | `app/.../aop/Permissions.java` | `library/base/.../base/aop/Permissions.java` | `run.yigou.gxzy.aop` | `com.hjq.base.aop` |
| 4 | `app/.../aop/SingleClick.java` | `library/base/.../base/aop/SingleClick.java` | `run.yigou.gxzy.aop` | `com.hjq.base.aop` |

#### 6.2 方案调整 - AOP 保留在 app 模块

**偏差说明**：原计划将 AOP 迁移到 base 模块，但经过研究后决定**保留在 app 模块中**。

**原因**：
1. CheckNetAspect 依赖 `run.yigou.gxzy.R` 资源 ID（无法迁移到 base）
2. PermissionsAspect 依赖 `run.yigou.gxzy.other.PermissionCallback`（app 模块专属）
3. AOP 代码量小（9 个文件），抽取收益低
4. AspectJX 插件需要在 app 模块编译时织入

**处理**：
- AOP 注解和切面文件**留在 app 模块不动**
- 包名已经是 `run.yigou.gxzy.aop`（无需修改）
- import 语句无需变更
- aspectjx 配置无需变更

**涉及文件**（全部在 app 模块）：
- `app/.../aop/CheckNet.java` - 注解
- `app/.../aop/CheckNetAspect.java` - 切面
- `app/.../aop/Log.java` - 注解
- `app/.../aop/LogAspect.java` - 切面
- `app/.../aop/Permissions.java` - 注解
- `app/.../aop/PermissionsAspect.java` - 切面
- `app/.../aop/SingleClick.java` - 注解
- `app/.../aop/SingleClickAspect.java` - 切面
- `app/.../aop/ResultCallback.java` - 回调接口

#### 6.3 验证

```bash
./gradlew :app:assembleDebug
```

**验证结果**：✅ BUILD SUCCESSFUL - AOP 注解织入正常

---

## 五、收尾工作

| # | 任务 | 说明 |
|---|------|------|
| 1 | 更新 `README.md` | 更新模块架构说明，反映新的模块结构 |
| 2 | 删除空目录 | 删除 app 模块中已迁移的空包目录 |
| 3 | 全量编译验证 | `./gradlew :app:assembleRelease` |
| 4 | 更新 `settings.gradle` 注释 | 为新增模块添加说明注释 |

---

## 六、风险与回滚

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| R 资源迁移遗漏 | 编译失败 | 阶段一中逐文件确认 R 引用，编译验证 |
| EasyLog 替换遗漏 | 编译失败 | 全局搜索 `import run.yigou.gxzy.utils.EasyLog` 确认全部替换 |
| SecurityUtils 内部交叉引用 | 编译失败 | 迁移后先编译 crypto 模块，再编译 app |
| SSE 解耦后行为差异 | 运行时异常 | 保持原有逻辑不变，仅改变依赖注入方式 |
| AOP 注解路径变更导致织入失败 | 运行时切面不生效 | ✅ 已消除：AOP 保留在 app 模块，无需迁移，aspectjx 配置不变 |

---

## 七、计划偏差纠正机制

本计划文档是实施的**唯一权威依据**。执行过程中：

1. **每阶段开始前**：重新阅读本阶段计划，确认文件清单和 import 替换列表
2. **每阶段完成后**：在本计划中勾选已完成项，记录实际耗时和遇到的问题
3. **发现偏差时**：
   - 若偏差在本阶段内可修正 → 记录偏差原因，继续执行
   - 若偏差影响后续阶段 → 暂停执行，更新本计划文档，重新评估后续阶段
   - 若偏差涉及新增/删除模块 → 回到 INNOVATE 模式重新权衡
4. **计划更新**：任何对本文档的修改必须记录在版本历史中

---

## 八、版本历史

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| v1.0 | 2026-06-08 | 初始版本，包含 6 个阶段的完整实施计划 |
| v1.1 | 2026-06-08 | **阶段一偏差修正**：`BrowserView` 和 `PlayerView` 因深度耦合 app 模块（引用具体 Activity/Dialog/AppConfig），暂不迁移，留在 app 模块。阶段一迁移范围从 7 个缩减为 5 个组件。 |
| v1.2 | 2026-06-08 | **阶段六偏差修正**：AOP 注解模块（Log、CheckNet、SingleClick、Permissions）保留在 app 模块，不迁移到 base。原因：CheckNetAspect 依赖 R 资源，PermissionsAspect 依赖 app 模块的 PermissionCallback。 |
| v1.3 | 2026-06-08 | **阶段七收尾完成**：清理空目录（adapter、data、http/sse），更新 README.md 模块架构说明，更新 settings.gradle 注释，全量编译验证 BUILD SUCCESSFUL。 |
