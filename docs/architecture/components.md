# 组件与依赖清单

- **状态**：已接受（accepted）
- **日期**：2026-10-22
- **适用范围**：`app` 与 `library/*` 的实际依赖、接入入口与官方资料
- **关联文档**：[UI 设计规范](ui-spec.md)、[数据模型与缓存](data-model.md)、[HTTP 契约](http-api.md)、[GreenDAO 使用与升级指南](../database/greendao-guide.md)、[项目路线](../../AGENTS.md)

---

## 1. 定位与规则

本文件回答三个问题：项目用了什么组件、在代码里从哪里接入、去哪个官方地址查用法。单个组件的深度配置仍以 `docs/component/` 与各模块代码为准。

使用规则：

1. **版本只认 Gradle**：`app/build.gradle`、`common.gradle`、`library/*/build.gradle` 是唯一版本事实来源。`gradle/libs.versions.toml` 当前没有被任何构建脚本引用（无 `libs.*` 别名），属于未启用的版本目录，不能当作现状。
2. **同一能力只保留一个库**：网络只 EasyHttp/OkHttp，图片只 Glide，JSON 只 Gson + GsonFactory，事件只 XBus，日志只 EasyLog/Timber，分享只友盟。新增第二套必须先改 ADR。
3. **依赖变更必须同批同步本文件**：新增、升级、移除依赖时，更新对应表格与版本号，并检查 minSdk 23、`arm64-v8a`、16 KB 页对齐约束（MMKV 2.x、Bugly、友盟均有明确限制）。
4. **禁止写入任何凭证**：Token、AccessKey、签名密钥、AppKey/AppSecret 只存在于 Gradle 配置或安全类中，本文件不记录具体值。
5. **优先复用内部模块**：通用能力先放对应 `library/*`，不新建与现有模块职责重叠的平行入口。

---

## 2. 内部模块（`library/*`）

| 模块 | 职责与关键类 | 接入方式 | 上游 / 来源 | 使用参考 |
| --- | --- | --- | --- | --- |
| `library/base` | `com.hjq.base` 基类：`BaseActivity`、`BaseFragment`、`BaseAdapter`、`AppAdapter`、`BaseDialog`、`BasePopupWindow`；`run.yigou.gxzy.base.util` 下 `MD5Util`、`RC4Helper` | app 直接 `implementation project(':library:base')`；页面继承 `AppActivity` / `AppFragment` | [getActivity/AndroidProject](https://github.com/getActivity/AndroidProject) 13.1 基础上本地改造 | [ui-spec.md](ui-spec.md) §2、§7 |
| `library/widget` | `WrapRecyclerView`、`StatusLayout`、`XCollapsingToolbarLayout`、`NestedViewPager`、`SettingBar`、`CountdownView`、`PasswordEditText` 等自定义 View | XML 直接引用；`StatusLayout` 负责加载/内容/空/失败四态 | 同上 | [ui-spec.md](ui-spec.md) §2、§7 |
| `library/ui-dialog` | `CommonDialog`、`MessageDialog`、`InputDialog`、`SelectDialog`、`MenuDialog`、`WaitDialog`、`DateDialog`、`TimeDialog`；`ListPopup` | 各弹窗通过 `Builder` 构造，`Builder` 继承 `CommonDialog.Builder`；底部菜单使用 `ListPopup` | 同上 | [ui-spec.md](ui-spec.md) §7 |
| `library/tips-widget` | `TipsLittleWindow`、`WindowConfig`、`LocalLinkMovementMethod`、`TipsArrowView` | `ui/reader/widget/TipsLittleTextViewWindow`、`TipsLittleRecyclerViewWindow` 继承并装配 Tips 浮窗 | 本地实现 | [ui-spec.md](ui-spec.md) §7、[TipsTextRenderConfig 指南](../component/TipsTextRenderConfig配置中心使用指南.md) |
| `library/text-renderer` | `TipsTextRenderer`、`ClickLink`、`TipsTextRenderConfig`、`RenderOptions`、`ItemNumberRenderer` | `TipsClickHandler` 是统一渲染入口；app 通过 `AppStyleConfigProvider` 注入样式配置 | 本地实现，依赖 `text-utils` | [TipsTextRenderConfig 指南](../component/TipsTextRenderConfig配置中心使用指南.md) |
| `library/text-utils` | `SearchMatcher`、`TextHighlighter` | `SearchCoordinator`、`TipsSearchEngine` 调用，负责匹配与高亮 | 本地实现，零外部依赖 | [ui-spec.md](ui-spec.md) §5.4、§7 |
| `library/log` | `EasyLog`（`print` / `json` / `printJson`） | 全局日志入口；工具类与后台任务统一调用 `EasyLog` | 本地实现，零外部依赖 | [ui-spec.md](ui-spec.md) §2 |
| `library/xbus` | `XEventBus`、`@Subscribe`、`@SubscribedMethod` | `AppApplication.registryByReflect()` 注册；页面用 `@Subscribe` 接收事件 | [LucasXu01/XBus](https://github.com/LucasXu01/XBus) | 代码内 `AppApplication`、`TipsBookNetReadFragment`、`AiMsgFragment` |
| `library/sse-client` | `SseClient`、`SseClientHelper`、`SseStreamCallback`、`SseEventHandler` | `AiStreamApi` 发起 AI 对话 SSE；`SseClient` 接受可注入的 `SseLogger`，未注入时 `SseEventHandler` 回退 `EasyLog` | 本地实现，基于 OkHttp SSE | [http-api.md](http-api.md) §7 |
| `library/native-core` | `NativeBridge`：SM2/SM4、请求签名、密钥保护、反调试 | `SecurityUtils`、`SecurityConfig` 调用；CMake 仅构建 `arm64-v8a` | 本地 NDK（`src/main/cpp`） | [SM2 使用详细教程](../security/SM2使用详细教程.md) |
| `library/umeng` | `UmengClient`、`UmengShare`、`UmengLogin`、`Platform` | `ShareDialog` 调 `UmengClient.share`；启动链**未见** `UmengClient.init` | AndroidProject + 友盟 SDK | [友盟分享文档](https://developer.umeng.com/docs/128606/detail/193879) |
| `library/crypto` | `AESUtil`、`RC4Helper`、`SecurityUtils`、`SM2Util`、`SM4Util` | **app 未声明依赖**；业务实际使用 app 内 `run.yigou.gxzy.security.SecurityUtils` 与 `crypto/sm/` 实现。属库存模块，新代码不得引用 | 本地实现 + BouncyCastle | [SM2 使用详细教程](../security/SM2使用详细教程.md) |

> `library/crypto` 虽在 `settings.gradle` 中 `include`，但 `app/build.gradle` 没有 `project(':library:crypto')`。在完成模块收敛前，不要把它当作可用入口。

---

## 3. 外部组件

### 3.1 UI 与基础能力

| 组件（版本） | 用途与接入方式 | 本地入口 | 项目地址 / 使用参考 |
| --- | --- | --- | --- |
| AndroidX AppCompat 1.3.1、Material 1.4.0 | 主题兼容、`AppCompatActivity`、Material 控件 | `common.gradle`；`AppTheme`（`res/values/styles.xml`） | [androidx](https://github.com/androidx/androidx)、[material-components-android](https://github.com/material-components/material-components-android)；[ui-spec.md](ui-spec.md) §2 |
| AndroidX Fragment 1.5.0 | `library/tips-widget` 的 `api` 依赖，替代废弃的 `android.app.Fragment` | `library/tips-widget/build.gradle` | [Fragment 发布说明](https://developer.android.com/jetpack/androidx/releases/fragment) |
| MultiDex 2.0.1 | 突破 64K 方法数；`attachBaseContext` 调 `MultiDex.install` | `AppApplication.attachBaseContext` | [MultiDex 文档](https://developer.android.com/studio/build/multidex) |
| TitleBar 9.2 | 全局标题栏；`TitleBar.setDefaultStyle(new TitleBarStyle())`；页面继承 `TitleBarFragment` | `AppApplication.initUIComponents`、`app/TitleBarFragment`、`app/TitleBarStyle` | [TitleBar](https://github.com/getActivity/TitleBar)；[TitleBar 优化指南](../optimization/TitleBar优化指南.md) |
| Toaster 12.6 | Toast；启动初始化、自定义样式与日志拦截 | `AppApplication.initUtilsAndServices`、`app/ToastStyle`、`app/ToastLogInterceptor`、`base/action/ToastAction` | [Toaster](https://github.com/getActivity/Toaster) |
| ShapeView 8.3 | XML 形状/圆角/描边控件（`ShapeTextView`、`ShapeButton`、`ShapeImageView`、`ShapeFrameLayout` 等） | `res/layout/*.xml` 直接引用 `com.hjq.shape.*` | [ShapeView](https://github.com/getActivity/ShapeView) |
| XXPermissions 18.6 | 运行时权限；AOP `@Permissions` 与启动页权限申请 | `aop/PermissionsAspect`、`ui/splash/StartupPermissions`、`dialog/UpdateDialog` | [XXPermissions](https://github.com/getActivity/XXPermissions) |
| GsonFactory 9.6 | Gson 解析容错；统一注册解析异常回调 | `AppApplication.initDataParsingAndLogging`、`network/server/RequestHandler` | [GsonFactory](https://github.com/getActivity/GsonFactory) |
| GroupedRecyclerViewAdapter 2.4.3 | 分组列表（书网 Tips 列表） | `ui/reader/bookread/TipsBookNetReadFragment`、`ui/reader/adapter/BaseRefactoredAdapter` | [GroupedRecyclerViewAdapter](https://github.com/donkingliang/GroupedRecyclerViewAdapter) |
| SmartRefreshLayout 2.0.3（kernel + material header） | 下拉刷新/上拉加载；全局默认 Header、Footer 与行为 | `AppApplication.initUIComponents`、`widget/MaterialHeader`、`widget/SmartBallPulseFooter` | [SmartRefreshLayout](https://github.com/scwang90/SmartRefreshLayout) |
| Lottie 4.1.0 | JSON 动画：启动页、状态空态、更新弹窗 | `ui/splash/SplashActivity`、`widget/StatusLayout`、`dialog/UpdateDialog` | [lottie-android](https://github.com/airbnb/lottie-android) |
| PhotoView 2.3.0 | 图片手势缩放 | `ui/media/adapter/ImagePreviewAdapter`、`image_preview_item.xml` | [PhotoView](https://github.com/Baseflow/PhotoView) |
| CircleIndicator 2.1.6 | ViewPager 指示器 | `ui/splash/GuideActivity`、`ui/media/activity/ImagePreviewActivity` | [CircleIndicator](https://github.com/ongakuer/CircleIndicator) |
| ImmersionBar 3.0.0 | 沉浸式状态栏/导航栏 | `AppActivity`、`TitleBarFragment`、`SplashActivity`、`LoginActivity` | [ImmersionBar](https://github.com/gyf-dev/ImmersionBar) |
| Glide 4.12.0 + compiler 4.12.0 | 图片加载；自定义 OkHttp 集成与低内存回调 | `network/glide/GlideConfig`、`OkHttpFetcher`、`OkHttpLoader`、`ui/media/image/GlideImageLoader` | [Glide](https://github.com/bumptech/glide)、[中文文档](https://github.com/Muyangmin/glide-docs-cn)；[ui-spec.md](ui-spec.md) §2 |
| Markwon 4.6.2（core/html/image/linkify/strikethrough/tables/tasklist） | AI 消息 Markdown 渲染 | `utils/MarkdownUtils`、`ui/reader/ai/AiMsgFragment`、`dialog/ChatSummaryListDialog` | [Markwon](https://github.com/noties/Markwon) |
| AndroidUtilCode（utilcodex）1.31.1 | 通用工具；`Utils.init`、`GsonUtils`、`LogUtils` | `AppApplication.initThirdPartySDKs`、`utils/DebugLog` | [AndroidUtilCode](https://github.com/Blankj/AndroidUtilCode) |
| AspectJ runtime 1.9.6 + AspectJX 2.0.10 | 编译期切面：`@SingleClick`、`@CheckNet`、`@Permissions`、`@Log` | `aop/*Aspect`；根 `build.gradle` 引入 AspectJX，`app/build.gradle` 仅 include 应用包名 | [AspectJ](https://github.com/eclipse-aspectj/aspectj)、[AspectJX](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx) |

### 3.2 数据与网络

| 组件（版本） | 用途与接入方式 | 本地入口 | 项目地址 / 使用参考 |
| --- | --- | --- | --- |
| GreenDAO 3.3.0 | ORM 与 DAO 生成；`greendao { schemaVersion 1; daoPackage '...data.local.gen' }`；19 个实体表 | `data/local/entity`、`data/local/gen`、`data/local/helper/LocalServices`、`DbService`、`MySQLiteOpenHelper`、`MigrationOrchestrator` | [greenDAO](https://github.com/greenrobot/greenDAO)；[GreenDAO 指南](../database/greendao-guide.md)；[data-model.md](data-model.md) §2、§9 |
| MMKV 2.0.0 | KV 缓存：更新频率、搜索/名词权限状态、设备标识、公告队列等；启动时 `MMKV.initialize` | `AppApplication.initUtilsAndServices`、`app/DataPreferences`、`manager/search/SearchPermissionStore`、`manager/mingci/MingCiPermissionStore`、`network/security/DeviceIdStore` | [MMKV](https://github.com/Tencent/MMKV)；[data-model.md](data-model.md) §2 |
| Gson 2.8.8 | JSON 解析；DTO 字段用 `@SerializedName` 对齐服务端 | `data/remote/model`、`network/server/RequestHandler` | [Gson](https://github.com/google/gson)；[http-api.md](http-api.md) §5 |
| EasyHttp 13.5（本地 AAR） | HTTP 请求封装；`EasyConfig.with(...).setServer(...).setHandler(...)`；双前缀 `RequestServer` / `VersionRequestServer` | `AppApplication.initNetworkFramework`、`network/server/RequestServer`、`VersionRequestServer`、`RequestHandler`、`network/security/InterceptorHelper` | [EasyHttp](https://github.com/getActivity/EasyHttp)；[http-api.md](http-api.md) §2～§6 |
| OkHttp 5.3.0 + okhttp-sse 5.3.0 + logging-interceptor 5.3.0 | 底层 HTTP 与 SSE；EasyHttp 与 AI 流共用；`configurations.all` 强制统一到 5.3.0 | `AppApplication.initNetworkFramework`、`data/remote/api/ai/AiStreamApi`、`library/sse-client` | [OkHttp](https://github.com/square/okhttp)、[okhttp-sse](https://github.com/square/okhttp/tree/master/okhttp-sse)；[http-api.md](http-api.md) §7 |

### 3.3 安全与加密

| 组件（版本） | 用途与接入方式 | 本地入口 | 项目地址 / 使用参考 |
| --- | --- | --- | --- |
| BouncyCastle 1.76（`bcprov-jdk15to18`） | SM2/SM4 Java 侧能力、证书与密钥处理 | `network/security/SecurityConfig`、`crypto/sm/*` | [bc-java](https://github.com/bcgit/bc-java)；[SM2 使用详细教程](../security/SM2使用详细教程.md) |
| RSAUtils 2.0.0 | RSA/RC4 工具封装 | `library/base/.../RC4Helper`、`library/crypto/.../RC4Helper` | [RSAUtils](https://github.com/gzu-liyujiang/RSAUtils) |

> 防重放、Bearer、设备标识等请求侧安全逻辑属于代码契约，不是独立组件；统一见 `InterceptorHelper` 与 [http-api.md](http-api.md) §4。

### 3.4 日志、质量与分享

| 组件（版本） | 用途与接入方式 | 本地入口 | 项目地址 / 使用参考 |
| --- | --- | --- | --- |
| Timber 4.7.1 | 调试日志树；仅在 `LOG_ENABLE` 为 true 时 `Timber.plant(new DebugLoggerTree())` | `AppApplication.initDataParsingAndLogging`、`app/DebugLoggerTree` | [Timber](https://github.com/JakeWharton/timber) |
| Bugly 3.4.4 + nativecrash 3.9.2 | 崩溃上报；`CrashReport.postCatchedException` 分散在 `CacheDataManager`、`AddressDialog`、`PermissionsAspect` 等处 | **启动链未见 `CrashReport.initCrashReport`**；`MineFragment` 有 `closeBugly` 开关 | [Bugly 文档](https://bugly.qq.com/docs/user-guide/instruction-manual-android/)；16 KB 页对齐说明见 `app/build.gradle` 注释 |
| LeakCanary 2.14 | Debug/Preview 内存泄漏检测 | `debugImplementation` / `previewImplementation` | [LeakCanary](https://github.com/square/leakcanary)；[排查指引](../optimization/LeakCanary泄漏排查指引.md) |
| 友盟 common 9.9.0、asms 1.8.5、share-core 7.1.5、share-wx 7.1.5、share-qq 7.1.5 | 统计与分享；`UmengClient.share` 被 `ShareDialog` 调用 | `library/umeng/UmengClient`、`dialog/ShareDialog`；**启动链未见 `UmengClient.init`** | [统计文档](https://developer.umeng.com/docs/119267/detail/118584)、[分享文档](https://developer.umeng.com/docs/128606/detail/193879) |
| QQ OpenSDK 3.53.0 | QQ 分享/登录通道 | `library/umeng` 的 `Platform.QQ` | [QQ 互联文档](https://wiki.open.qq.com/wiki/QQ%E7%94%A8%E6%88%B7%E8%83%BD%E5%8A%9B) |
| 微信 SDK 6.7.9（`wechat-sdk-android-without-mta`） | 微信分享/登录通道 | `library/umeng` 的 `Platform.WX` | [微信开放平台](https://developers.weixin.qq.com/doc/oplatform/Mobile_App/Access_Guide/Android.html) |

### 3.5 测试

| 组件（版本） | 用途与接入方式 | 本地入口 | 项目地址 / 使用参考 |
| --- | --- | --- | --- |
| JUnit 4.13.1 | JVM 单元测试 | `app/src/test/java` | [JUnit 4](https://github.com/junit-team/junit4) |
| Robolectric 4.10.3 | 在 JVM 上提供真实 SQLite，覆盖事务原子性/回滚；需 `includeAndroidResources true` | `app/build.gradle` 的 `testOptions` | [Robolectric](https://github.com/robolectric/robolectric)；[AGENTS.md](../../AGENTS.md)「测试指南」 |

### 3.6 常用调用入口（使用方法的索引）

| 场景 | 推荐入口 | 不要这样做 |
| --- | --- | --- |
| 业务 HTTP 请求 | 新建/复用 `IRequestApi` 类，经 EasyHttp + `RequestServer` 发出；鉴权/防重放头由 `InterceptorHelper` 统一注入 | 在页面拼 URL、手写 Header 或另起网络层 |
| App 级 HTTP 请求 | 调用链显式 `.server(new VersionRequestServer())`，走 `/api/` 前缀 | 忘记切换前缀，落到 `/api/AppBookRequest/api/...` |
| 读数据库 | 从 `LocalServices` 取 Service；查询用 `DbService.readInBackground(...)` | 主线程直接调 DAO，或在 Activity/Fragment 持有 DAO |
| 写数据库/事务 | `DbService.runInBackgroundSerial(...)`，事务内先清后写 | 绕过串行执行器并发写库 |
| 读写 KV | `DataPreferences` 或对应 `*Store`（内部封装 MMKV） | 在业务代码散落 `MMKV.mmkvWithID(...)` |
| 加载图片 | `Glide.with(...)` 或 `GlideImageLoader`；OkHttp 集成见 `GlideConfig` | 引入第二套图片库 |
| 发/收事件 | `XEventBus.getDefault().post(...)` + `@Subscribe` | 新增 EventBus/RxBus 等平行总线 |
| 渲染 Tips 文本 | `TipsTextRenderer.renderText(...)` 或 `TipsClickHandler` | 页面内自行解析 `$g{}` 等标记 |
| AI SSE | `AiStreamApi` → `SseClient.execute(...)`，安全头走 `InterceptorHelper.addSseSecurityHeaders` | 用 OkHttp 手写 EventSource |
| 运行时权限 | AOP `@Permissions` 或 `XXPermissions`；启动权限见 `StartupPermissions` | 各页面手写 `requestPermissions` 分散处理 |
| 轻提示 | `Toaster.show(...)` 或 `ToastAction` | 直接 `Toast.makeText` 扩散样式 |

---

## 4. 本地二进制与仓库

| 文件 / 目录 | 说明 | 使用约束 |
| --- | --- | --- |
| `app/libs/EasyHttp-13.5.aar` | EasyHttp 13.5 的本地 AAR。JitPack 未生成 13.5 的 Gradle Module Metadata，直接写坐标会解析失败，因此改由 `common.gradle` 的 `fileTree(include: ['*.jar', '*.aar'], dir: 'libs')` 引入 | 不得再声明 `com.github.getActivity:EasyHttp:13.5` 坐标，否则重复类；升级时替换 AAR 并同步本表 |
| `app/libs/ZHConverter.jar` | 历史遗留的繁简转换包 | **全仓无代码引用**；删除前需确认无反射/动态加载使用 |
| `localMaven/` | 本地 Maven 产物目录 | 当前为空，未被构建使用 |
| `jks/` | 签名材料 | 敏感目录，不复制、不提交密码 |

---

## 5. 接入状态与已知缺口

- **友盟未初始化**：`library/umeng` 已接入 `UmengClient` / `ShareDialog`，但启动链没有找到 `UmengClient.init`。在补齐初始化前，分享/登录能力不可视为可用。
- **Bugly 未初始化**：多处调用 `CrashReport.postCatchedException`，但启动链没有找到 `CrashReport.initCrashReport`。崩溃上报是否生效需在真机确认。
- **`library/crypto` 未接线**：模块被 `settings.gradle` 引入，但 app 未依赖；业务使用的是 app 内 `security` 与 `crypto/sm` 包。
- **`ZHConverter.jar` 未使用**：无引用，属待清理资产。
- **`gradle/libs.versions.toml` 未启用**：当前构建未使用版本目录，版本以各 Gradle 文件为准。
- **16 KB 页对齐**：MMKV 2.x 仅支持 `arm64-v8a`；Bugly 旧版分组与友盟 `libumeng-spy.so` 存在对齐风险，发布 Google Play 前必须复核（见 `app/build.gradle` 与 `library/umeng/build.gradle` 注释）。
- **GreenDAO 三个版本号**：插件 `schemaVersion = 1` 只影响生成代码；运行时升级由 `DatabaseVersionManager.CURRENT_VERSION = 2` 触发；SQLite 实际值是 `PRAGMA user_version = 2`。详见 [GreenDAO 指南](../database/greendao-guide.md) 的「三个版本号」。

---

## 6. 组件变更流程

1. **新增**：确认官方来源与许可证 → 检查 minSdk、ABI、16 KB 页对齐与现有库冲突 → 在对应 Gradle 文件声明 → 更新本文件表格 → 更新 `docs/README.md` 索引与变更日志 → 运行 `gradlew.bat testDebugUnitTest`，UI 相关补 `lintDebug`。
2. **升级**：先读官方 Release Notes 与迁移说明 → 确认 API/行为变更影响到的本地入口 → 修改版本 → 更新本文件版本号与「接入状态」→ 回归受影响链路。
3. **移除**：确认无代码/反射/资源引用 → 删除依赖与本地二进制 → 更新本文件与索引 → 记录替代方案。
4. **禁止**：为同一能力再引入一套库；使用未经验证的依赖；把凭证写入文档或源码。

---

## 7. 变更记录

| 日期 | 说明 |
| --- | --- |
| 2026-10-22 | 初始版本：登记内部模块、外部组件、本地二进制、接入入口、官方地址与已知缺口 |
