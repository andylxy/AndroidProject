---
name: project-architecture
description: 为本 Android 项目新增或修改功能时使用，约束 UI 页面、GreenDAO 数据模型、EasyHttp/HTTP 契约、组件依赖、权限缓存与文档同步。触发词：新增功能、页面规范、数据模型、接口设计、HTTP 契约、组件选型、依赖升级、ADR、项目路线。
argument-hint: 描述要新增/修改的功能或页面
user-invocable: true
---

# 项目架构约束

本技能是入口，不是规范本身。**先读对应文档，再动代码**；文档与代码冲突时，以代码事实为准，并当场修正文档。

## 何时使用

- 新增/改版页面、弹窗、列表、导航；
- 新增/修改 GreenDAO 实体、缓存、刷新策略；
- 新增/修改 HTTP 接口、分页、错误处理、SSE；
- 新增/升级第三方依赖，或在同类组件之间做选型；
- 调整权限闸门、登录态、数据更新频率；
- 需要判断某项改动属于哪个里程碑、是否超出范围。

不适用：纯文案修正、单行日志、与架构无关的构建脚本小修。

## 权威文档（按需读取）

| 主题 | 文档 |
| --- | --- |
| UI 结构、状态、组件复用、验收清单 | [../../../docs/architecture/ui-spec.md](../../../docs/architecture/ui-spec.md) |
| 实体、存储分层、缓存失效、迁移、权限状态 | [../../../docs/architecture/data-model.md](../../../docs/architecture/data-model.md) |
| Host、鉴权头、响应信封、错误码、分页、SSE | [../../../docs/architecture/http-api.md](../../../docs/architecture/http-api.md) |
| 组件版本、接入入口、官方地址、依赖准入 | [../../../docs/architecture/components.md](../../../docs/architecture/components.md) |
| 为什么这样定（UI 基线） | [../../../docs/architecture/adr/0003-ui-baseline.md](../../../docs/architecture/adr/0003-ui-baseline.md) |
| 为什么这样定（数据/缓存） | [../../../docs/architecture/adr/0004-data-cache-strategy.md](../../../docs/architecture/adr/0004-data-cache-strategy.md) |
| 为什么这样定（HTTP 契约） | [../../../docs/architecture/adr/0005-http-contract.md](../../../docs/architecture/adr/0005-http-contract.md) |
| 项目路线、技术方向、非目标 | [../../../AGENTS.md](../../../AGENTS.md) |
| GreenDAO 操作与升级 | [../../../docs/database/greendao-guide.md](../../../docs/database/greendao-guide.md) |

## 工作流

1. **归类**：判断改动属于 UI / 数据 / HTTP / 依赖 / 跨层；读对应文档与 ADR。涉及新增或升级组件时先查 `components.md`。
2. **定边界**：确认属于 M1/M2/M3 哪一项；超出当前里程碑的顺手改造不做。
3. **查现状**：用 `Grep`/`Read` 核实类、方法、资源、调用方真实存在；不凭记忆写符号。
4. **先计划**：跨文件改动先给出文件清单、接口签名、数据变更、验证方式；获授权后实施。
5. **实现**：遵守下方硬规则；最小改动，不引入平行实现。
6. **验证**：至少 `gradlew.bat testDebugUnitTest`；UI 改动补 `lintDebug`。
7. **同步文档**：同一变更内更新受影响的设计文档、ADR、索引；不留失真说明。

## 硬规则

### UI

- Java + View；沿用 `AppActivity` / `AppFragment` / `TitleBarFragment`。
- 列表用 `WrapRecyclerView` + `AppAdapter`；刷新用 `SmartRefreshLayout`；状态用 `StatusLayout` 或既有等价布局。
- 弹窗用 `library/ui-dialog`；Tips 浮窗用 `library/tips-widget`；文本渲染用 `TipsTextRenderer`。
- 文案进 `strings.xml`；颜色/尺寸用 `common_*` / `dp_*` / `sp_*`；禁止魔法值与硬编码中文。
- 每个网络页面必须有加载/内容/空/失败四态；点击必须有反馈；重复提交要有保护。
- 不新增 framework/base 类的平行实现；通用能力放对应 `library/*`。

### 数据

- 读取顺序：`GlobalDataHolder` → 本地库/文件 → 网络（本地为空或显式刷新）。
- 主线程禁止 SQLite；写库与跨表事务统一走 `DbService`（`mf-db-serial`）。
- 内容写入按范围“先清后写”，事务内完成；失败保留旧数据并记录 `EasyLog`。
- 内容书号统一用 `bookNo`；`Book.bookId` / `Chapter.id` 等本地代理键不跨层当请求参数。
- 新增实体/字段：改实体 → 生成 DAO → `EntityRegistrationHelper` → 递增 `DatabaseVersionManager.CURRENT_VERSION` → 迁移与升级测试 → 更新文档。
- 权限状态 fail-closed；网关只读进程内状态，不读 MMKV 缓存；字段缺失按失败处理。

### 依赖

- 先查 `components.md` 是否已有同类能力；同一能力只保留一个库（图片 Glide、网络 EasyHttp/OkHttp、JSON Gson + GsonFactory、事件 XBus、日志 EasyLog/Timber、分享友盟）。
- 新增/升级/移除依赖必须同批更新 `components.md`；不得只在 Gradle 改版本。
- 检查 minSdk 23、`arm64-v8a`、16 KB 页对齐；MMKV 2.x、Bugly、友盟有明确约束。
- 新依赖必须有官方仓库/官方文档依据；不得把凭证、Token、AccessKey 写入文档或源码。

### HTTP

- 一个端点一个 `IRequestApi` 类；`getApi()` 只写最后一段；GET 的 `getMethod()` 不加 `@Override`。
- 默认前缀 `/api/AppBookRequest/`；App 级端点用 `.server(new VersionRequestServer())`（`/api/`）。
- 响应只用 `HttpData` / `HttpListData`；成功 200、登录失效 1001；不解析 `msg` 做分支。
- 新列表：`page` 1-based，`pageSize` 默认 20、最大 100；用 `HttpListData` 与 `isLastPage()`。
- 新接口必须登记到 `http-api.md` §8，并补解析/错误映射测试。
- 不新增匿名兜底 Token；不打印 Token、口令、签名、AccessKey、API Key。
- SSE 只用于 AI；鉴权/防重放必须走 `InterceptorHelper.addSseSecurityHeaders`。

## 当前已知缺口（不要在功能票里假装已修）

- 登录/退出**未清理**搜索与名词权限缓存（`sFetchOk` / `sState` / MMKV / 拉取时钟），同进程换号可能串权限。M1 统一修复。
- `InterceptorHelper.loginBearer` 在未登录时回落到 `SecurityConfig.getApiKey()` 默认 microfeed 凭证，且 `SecurityConfig` 内置默认 AccessKey；与“不内置兜底凭证”目标冲突，清理需单独立项（见 `http-api.md` §4.2）。
- `DataUpdateFrequency` 到期只重新执行本地优先加载，**不会**自动从网络全量刷新。真正的网络刷新是 M2。
- 内容实体没有版本字段，不支持增量下发；版本化是 M3，需后端协同。
- 样式配置缓存无 TTL，离线继续使用旧配置；联网后靠服务端版本比较更新。
- 友盟 `UmengClient.init` 与 Bugly `CrashReport.initCrashReport` 均未出现在启动链；在补齐前，分享/登录与崩溃上报不可视为可用（见 `components.md` §5）。
- `library/crypto` 已被 `settings.gradle` 引入但 app 未依赖；新代码不得把它当作可用入口。

## 完成前自检

- [ ] 读了对应规范与 ADR，改动在里程碑边界内。
- [ ] 符号/路径/资源均已核实存在。
- [ ] UI 四态、权限闸门、生命周期、重复提交已处理。
- [ ] 数据写入线程、事务、缓存失效边界已说明。
- [ ] HTTP 契约、错误码、分页、SSE 未被破坏。
- [ ] 依赖变更已同步 `components.md`，未引入第二套同类库。
- [ ] 文档/ADR/索引已同步；`gradlew.bat testDebugUnitTest` 通过。
- [ ] 未提交任何密钥、Token、口令或用户隐私。
