# 数据模型与缓存策略

- **状态**：已接受（accepted）
- **日期**：2026-10-22
- **适用范围**：`app` 模块的本地持久化、内存缓存与权限/配置状态缓存
- **关联决策**：[ADR-0004：本地优先与缓存策略](adr/0004-data-cache-strategy.md)
- **关联文档**：[UI 设计规范](ui-spec.md)、[HTTP 契约](http-api.md)、[组件与依赖清单](components.md)、[GreenDAO 使用与升级指南](../database/greendao-guide.md)

---

## 1. 定位与原则

本应用的核心内容（书籍、章节、药物、方剂、名词解释）以**本地数据库为第一读取源**，网络用于首次填充与后续刷新。用户在无网络或弱网时仍应能打开已缓存的书、查词与搜索已下载内容。

数据层遵循以下原则：

1. **本地优先**：页面先读内存/本地库，网络只在本地无数据或刷新策略要求时使用。
2. **网络结果整表替换**：内容类接口返回全量快照，落库时按范围清空后写入，不做字段级合并。
3. **单线程写库**：所有数据库读写经 `DbService` 的串行执行器（线程名 `mf-db-serial`），禁止主线程直接访问。
4. **权限 fail-closed**：权限状态未知、拉取失败或字段缺失时一律按“无权限”处理。
5. **不做离线编辑与多端同步**：本地只缓存服务端内容；用户产生的数据仅限书架、阅读位置、搜索历史、AI 会话与设置。
6. **缓存必须可失效**：任何持久化状态都要说明写入时机、读取时机、失效/清除边界；没有失效策略的缓存不得新增。

本轮**不引入** Room、DataStore、内容版本号/增量下发、离线写回。相关内容版本化能力列入 M3，见 §12。

---

## 2. 存储分层

| 层 | 技术 | 实例/文件 | 保存内容 | 生命周期 |
| --- | --- | --- | --- | --- |
| 内存 | `GlobalDataHolder` 单例 | — | 导航、药物、名词、别名、AI 配置等热数据 | 进程内；可 `reload*` 重建 |
| 关系库 | GreenDAO 3.3 + SQLite | `myzhongyi.db`（`AppConst.dbName`） | 19 个实体表：内容、书架、章节缓存、AI 会话、用户、搜索历史 | 安装期持久化；运行库版本 2（`DatabaseVersionManager.CURRENT_VERSION`） |
| KV | MMKV 2.0 | `data_update_config` | 更新频率、上次更新时间 | 安装期持久化 |
| KV | MMKV 2.0 | `search_permission_state` | 最近一次搜索权限 + 拉取时钟 | 安装期持久化；仅诊断/持久化层 |
| KV | MMKV 2.0 | `mingci_permission_state` | 最近一次名词权限 + 拉取时钟 | 同上 |
| 文件 | `CacheHelper`（Java 序列化，应用私有目录） | `style_config_cache` | 样式配置 + 服务端版本号 | 安装期持久化；覆盖写 |
| 文件 | `CacheHelper` | `AppConst.FILE_NAME_SETTING` | `FragmentSetting`（术解等阅读设置） | 安装期持久化；覆盖写 |

> 所有数据库实体位于 `app/src/main/java/run/yigou/gxzy/data/local/entity/`，DAO 由 GreenDAO 生成在 `data/local/gen/`。`LocalServices` 是 Service 定位器，`DbService` 只负责执行器与事务入口（见 `docs/database/adr/0001-dbservice-design.md`）。
>
> GreenDAO 的插件 `schemaVersion`、运行时 `CURRENT_VERSION` 与 SQLite `PRAGMA user_version` 是三个不同概念，新增实体时只递增 `CURRENT_VERSION`；详见 [组件与依赖清单](components.md) §5 与 [GreenDAO 指南](../database/greendao-guide.md)。

---

## 3. 读取与写入路径

### 3.1 读取优先级

```
GlobalDataHolder（内存，已加载时）
        ↓ 未命中
本地 SQLite / 文件缓存
        ↓ 无数据
网络请求（仅本地为空，或显式刷新）
```

- UI 层优先通过 `GlobalDataHolder` 和 Repository/Service 读取；不得在 Activity/Fragment 中直接持有 DAO。
- 主线程禁止 `findAll()`、`insert()`、`delete()` 等数据库操作；统一使用 `DbService.readInBackground(...)` 或 `runInBackgroundSerial(...)`。
- 本地读取失败按“本地无数据”处理并记录日志，允许回落到网络；不得让异常直接冒到 UI 导致白屏。

### 3.2 写入路径

网络成功后的标准顺序：

1. 主线程更新 `GlobalDataHolder`（保证 UI 立即可用）；
2. 通过 `DbService` 的串行执行器异步落库；
3. 落库失败记录 `EasyLog`，保留旧数据（事务回滚），不回滚已更新的内存态、不自动重试。

内容类写入以“清空 + 批量插入”为主（如 `DataRepository.clearAndSaveNavTabs` / `saveYaoData`），必须在事务中完成；禁止逐条 `insertOrReplace` 造成半更新。

### 3.3 当前刷新语义（重要）

`AppDataManager.loadAllDataIfNeeded` 在 `DataUpdateFrequency` 到期后会重新执行加载序列，但每个数据源仍然是**本地优先**：

- 本地有数据 → 直接使用本地，不请求网络；
- 本地为空 → 才请求网络并落库。

因此当前实现更准确的描述是“**按频率重新加载**”，而不是“按频率从网络全量刷新”。真正的网络全量刷新、服务端内容版本比对属于 M2/M3 能力，本轮不改变现有行为。新增功能不得把“到期”误解为“已拿到最新内容”。

---

## 4. 实体目录

19 个 GreenDAO 实体按用途分组如下。字段只列与业务契约相关的核心项；完整字段以实体类为准。

### 4.1 导航与发现

| 实体 | 主键 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| `TabNav` | `tabNavId`（String） | `caseId`、`name`、`order`、`navList` | 发现页一级分类；`order` 决定展示顺序 |
| `TabNavBody` | `tabNavBodyId`（String） | `tabNavId`、`Id`、`bookNo`、`imageUrl`、`bookName`、`chengShu`、`author`、`desc`、`caseTag`、`chapterCount` | 分类下的书籍卡片；`bookNo` 是内容书号，也是跨表关联键 |

- 关系：`TabNav.navList` 通过 `TabNavBody.tabNavId` 关联（`@ToMany`）。
- `TabNavBody.bookNo` 必须与 `Chapter.bookId`、`Book.bookNo`、阅读入口传入的 `bookId` 使用同一取值；禁止混用本地自增 ID。
- 展示顺序以 `TabNav.order` 为准，不依赖数据库默认返回顺序。

### 4.2 书架、章节元数据与章节内容

| 实体 | 主键 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| `Book` | `bookId`（String，本地生成） | `bookNo`、`bookName`、`author`、`historiographerNumb`、`sortCode`、`lastReadPosition` | 书架条目与阅读进度；`bookNo` 才是内容书号 |
| `Chapter` | `id`（Long，自增） | `bookId`、`chapterSection`、`chapterHeader`、`signatureId`、`isDownload` | 某本书的章节目录；`bookId` 存的是 `bookNo` |
| `BookChapter` | `bookChapterId`（String） | `bookId`、`section`、`header`、`signatureId`、`signature`、`data` | 已缓存的章节内容头；按 `signatureId` 定位具体内容 |
| `BookChapterBody` | `bookChapterBodyId`（String） | `bookChapterId`、`ID`、`text`、`note`、`sectionvideo`、`height`、`fangList`、`signatureId`、`signature` | 章节正文分段；`data` 通过 `bookChapterId` 关联 |

- `Book.bookId` 是本地生成的代理键（`DataRepository.generateBookId()`），只用于书架表内部；所有内容请求使用 `Book.bookNo`。
- `Chapter.id` 是本地自增键，`chapterSection` 才是业务章节序号；排序与跳转使用 `chapterSection`，不要用 `id` 推断顺序。
- 章节内容缓存以 `signatureId` 为主匹配键；`isDownload` 仅表示本地是否已有内容，不代表服务端最新。
- `BookChapterBody.text` 可能包含渲染标记（如 `$g{}`），渲染规则由 `TipsTextRenderer` 负责，数据层不做解析。

### 4.3 药物、别名与方剂

| 实体 | 主键 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| `ZhongYao` | `yaoId`（Long，自增） | `ID`、`name`、`yaoList`、`height`、`text`、`signatureId`、`signature` | 药物正文；`name` 为检索键，`yaoList` 为别名列表 |
| `ZhongYaoAlia` | `yaoAliaId`（Long，自增） | `id`、`name`、`bieming`、`height`、`text` | 药物别名表；与 `ZhongYao` 通过名称/别名建立内存映射 |
| `YaoFang` | `yaoFangID`（String） | `bookId`、`yaoCount`、`height`、`name`、`ID`、`drinkNum`、`text`、`fangList`、`yaoList`、`signatureId`、`signature`、`standardYaoList` | 方剂主表；`bookId` 关联所属书籍 |
| `YaoFangBody` | `yaoFangBodyId`（String） | `yaoFangID`、`suffix`、`amount`、`yaoID`、`weight`、`showName`、`extraProcess` | 方剂组成明细 |

- 关系：`YaoFang.standardYaoList` 通过 `YaoFangBody.yaoFangID` 关联。
- 药物检索使用内存别名映射（`GlobalDataHolder.yaoAliasDict`），不要每次查询都扫表。
- `YaoFangBody.yaoID` 与 `ZhongYao.ID` 的对应关系以服务端下发为准，客户端不做隐式重排。

### 4.4 名词解释

| 实体 | 主键 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| `BeiMingCi` | `beiMingCiId`（Long，自增） | `ID`、`mingCiList`、`name`、`height`、`text`、`signatureId`、`signature`、`imageUrl` | 名词解释正文；`mingCiList` 为同义词/索引词 |

- 名词数据加载受 `MingCiPermissionManager.isAllowed()` 控制；无权限时清空 `GlobalDataHolder.mingCiContentMap`，UI 保持现有“未加载”表现。
- `$g{}` 点击查询的是内存 Map（键为名词名/别名），数据层不参与权限判定。

### 4.5 AI 会话与配置

| 实体 | 主键 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| `ChatSessionBean` | `id`（Long，自增） | `title`、`conversationId`、`endUserId`、`preview`、`createTime`、`updateTime`、`isDelete`、`messages` | AI 会话；`conversationId` 为服务端会话标识 |
| `ChatMessageBean` | `Id`（Long，自增） | `sessionId`、`type`、`nick`、`pic_url`、`content`、`createDate`、`isDelete` | 会话消息；`type` 1=发送、2=接收、3=系统、4=思考、5=总结 |
| `ChatSummaryBean` | `id`（Long，自增） | `sessionId`、`title`、`content`、`createTime` | 会话总结 |
| `AiConfig` | `AiConfigId`（String） | `ProvideAi`、`ApiKey`、`AiUrl`、`ModelList` | AI 服务配置；`ApiKey` 属敏感信息 |
| `AiConfigBody` | `AiConfigBodyId`（String） | `AiConfigId`、`GptModelName` | 可用模型列表 |

- 关系：`ChatSessionBean.messages` 通过 `ChatMessageBean.sessionId` 关联；`ChatSummaryBean.sessionId` 指向同一会话。
- `ChatMessageBean` 中 `isMore`、`isGreyMode`、`simpleQa`、`save`、`continuesListen`、`isThinkingCollapsed`、`isStreaming` 均为 `@Transient`，仅服务 UI，不落库。
- `ChatMessageBean.isDelete` 采用 `IS_Delete_YES=0` / `IS_Delete_NO=1` 的既有约定，查询时必须按该约定过滤，不要按常见 0/1 语义反向理解。
- `AiConfig.ApiKey`、`UserInfo.accessKeySecret` / `token` 禁止写入日志、埋点、崩溃上报或导出文件。

### 4.6 用户、搜索历史与关于

| 实体 | 主键 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| `UserInfo` | `id`（String） | `accessKeyId`、`accessKeySecret`、`token`、`userName`、`img`、`userLoginAccount` | 登录用户；`token` 用于 Bearer 鉴权 |
| `SearchHistory` | `id`（String） | `content`、`createDate` | 搜索历史；写入时去重/覆盖 |
| `About` | `aboutId`（Long，自增） | `name`、`text` | 关于页内容 |

- 登录/退出会增删 `UserInfo`；权限缓存不会随登录态自动清除，这是当前已知缺口，见 §6.4。
- 搜索历史属于用户本机数据，不上传、不参与内容刷新。

---

## 5. 数据加载与刷新

### 5.1 冷启动

`AppDataInitializer.initializeIfNeeded` 以 `TabNav` 表是否非空判断“本地是否有数据”：

- 有本地数据：在后台读取导航、药物、名词、药物别名、方剂别名到 `GlobalDataHolder`；
- 无本地数据：等待 UI 侧 `AppDataManager` 触发网络加载。

名词加载前必须检查 `MingCiPermissionManager.isAllowed()`；无权限时清空内存名词表，不读取本地名词表。

### 5.2 UI 触发的加载序列

`AppDataManager.loadAllDataIfNeeded` 的依赖顺序固定为：

```
导航数据
   ↓
药物数据 ∥ 名词数据（并行）
   ↓
方剂别名（依赖导航结果）
```

- 并行的两个任务通过计数器汇合；任一失败则整体回调失败。
- 导航网络成功后：内存同步 → `clearAndSaveNavTabsAsync` 异步落库；方剂别名读取的是 `GlobalDataHolder`，不依赖落库完成。
- 药物/名词网络成功后：内存同步 → `runInBackgroundSerial` 落库。
- 整个序列成功后才写入 `DataPreferences.setLastUpdateTime(now)`；序列执行期间重复调用会因 `isLoading` 直接返回。

### 5.3 更新频率

`DataUpdateFrequency`（MMKV：`data_update_config` / `update_frequency`）：

| 枚举 | 文案 | 行为 |
| --- | --- | --- |
| `EVERY_START` | 每次启动 | 每次进程启动都重新加载 |
| `DAILY` | 每天一次 | 距上次成功加载超过 24h 重新加载 |
| `WEEKLY` | 每周一次 | 默认值；超过 7 天重新加载 |
| `MONTHLY` | 每月一次 | 超过 30 天重新加载 |
| `MANUAL` | 手动更新 | 自动判定永不触发，仅手动刷新 |

注意：

- `last_update_time` 是“加载序列完成时间”，不是“网络数据版本时间”。
- 缓存过期只触发重新加载；如 §3.3 所述，本地有数据时不会自动请求网络。
- 修改频率立即生效；页面不得自行维护第二套过期计时。

### 5.4 书籍与章节按需加载

- 导航落库后，`DataRepository` 会为每本书请求章节目录（`ChapterListApi`），章节数变化时整表替换并记录日志。
- 阅读时按章节 `signatureId` 读取 `BookChapter` / `BookChapterBody`；缺失则触发内容下载并缓存。
- 书籍下载、章节下载必须经 `ChapterContentManager` / Repository，不得在 UI 层直接发请求或写 DAO。

### 5.5 AI 会话

- 会话与消息在发送/接收过程中写入 `ChatSessionBean` / `ChatMessageBean`；总结写入 `ChatSummaryBean`。
- 流式接收中的临时状态只存在于内存（`@Transient` 字段），不落库。
- 删除会话采用 `isDelete` 标记，不物理删除消息；查询必须过滤已删除项。

### 5.6 搜索历史

`SearchHistoryService.addOrUpadteHistory(keyword)` 负责去重写入；清空操作在后台线程执行。历史记录只保存关键词与时间，不保存搜索结果。

---

## 6. 权限状态模型

权限状态是“网络增量、进程内判定、失败关闭”的独立状态，不属于内容数据。

### 6.1 数据形态

| 状态 | 字段 | 完整性判定 |
| --- | --- | --- |
| `SearchPermissionState` | `Boolean global`、`Boolean book` | 任一为 `null` → `isComplete()=false` → 按拉取失败处理 |
| `MingCiPermissionState` | `Boolean allowed` | `null` → 不完整 → 按拉取失败处理 |

使用包装类型是为了区分“服务端明确拒绝”与“响应缺字段”；后者必须走失败与退避，不能当作有效拒绝把功能永久锁死。

### 6.2 判定与拉取

- 判定入口：`SearchPermissionManager.isSearchAllowed(SearchEntry)`、`MingCiPermissionManager.isAllowed()`。
- 进程内闸门：`sFetchOk` 为假直接返回无权限，**不读取 MMKV 缓存**；只有拉取成功并落盘后才置真。
- 一次启动一拉：`LaunchOnceGate` 保证每个进程只主动拉一次；回到前台不重复拉取。
- 失败退避：`FetchClockStore.RETRY_INTERVAL_MS = 30 分钟`；只有“上次尝试失败”才退避，成功后立即恢复。
- 失败静默：网络错误、解析失败、字段缺失、401/403/429/5xx 均不弹窗、不打断用户；仅记录日志并推进退避。
- 触发时机：`SplashActivity` 注册前台检查；冷启动/回前台时由 Manager 自行判定是否请求。

### 6.3 缓存语义

- MMKV ID：`search_permission_state` / `mingci_permission_state`；键统一为 `cache`。
- 只保存最近一次成功状态，覆盖写入，无 TTL。
- 缓存用于持久化与诊断，**不是放行依据**；网关只读进程内状态。
- 拉取时钟与权限缓存共用同一 MMKV ID，由 `FetchClockStore` 读写。

### 6.4 已知缺口与 M1 修复契约

当前登录/退出路径（`MyFragmentPersonal.handleLogout`、`SettingActivity`、`LoginActivity` 的 `UserInfo` 增删）**没有清除权限状态**：

- 进程内 `sFetchOk` / `sState` 不会重置；
- MMKV 中的旧账号权限与拉取时钟会保留；
- 同一进程内切换账号后，可能出现“新账号沿用旧账号权限”的窗口。

M1 必须引入统一的账号会话边界清理（建议放在账号管理/登录态变更的唯一入口）：

1. 将 `sFetchOk=false`、`sState=null`；
2. 清除两个权限 MMKV 的 `cache` 与拉取时钟；
3. 下次前台/启动时重新拉取；
4. 退出登录后立即隐藏依赖权限的入口（搜索仍按 fail-closed 拦截，名词表清空）。

在修复落地前，本文档与代码保持一致：不得假设退出登录会自动清除权限。

---

## 7. 样式配置缓存

- 文件：`style_config_cache`，通过 `CacheHelper` 序列化，内容为 `AppStyleConfigProvider.StyleConfigCache{version, configs}`。
- 启动：`loadCacheConfigAsync` 在后台读盘；`HIT` 应用缓存，`MISS`/`FAILED` 回落内置默认配置并应由调用方请求服务端。
- 网络：`loadConfig` 请求 `GetTipsStyleConfig`；请求参数 `version` 是**本地自增计数器**，服务端不据此做增量下发。
- 服务端版本：响应中的 `version`（服务端 `MAX(updated_at_ms)`）用于比较；与内存 `serverVersion` 相同则跳过应用与落盘，不同则应用并异步写缓存。
- 缓存无 TTL；离线时继续使用旧缓存，联网后由版本比较决定是否更新。
- `StyleConfigCache` 与 `StyleConfig` 必须保持 `Serializable` 且显式 `serialVersionUID`，否则 `CacheHelper` 写盘会失败。

---

## 8. 阅读设置缓存

- `FragmentSetting` 通过 `CacheHelper` 写入 `AppConst.FILE_NAME_SETTING`，保存术解等阅读设置。
- 该类必须保留显式 `serialVersionUID`；删除/重命名字段前先评估旧缓存反序列化行为（见 ADR-0002 §5.2、§6 步骤 0）。
- 设置读取发生在冷启动与页面初始化，允许短暂默认值；不得因为读盘失败而阻塞主线程。

---

## 9. 数据库并发、事务与迁移

### 9.1 并发约束

- 所有写操作与跨表事务提交到 `DbService` 的单线程执行器（`mf-db-serial`）。
- `readInBackground` 用于异步读；`runInBackgroundSerial` 用于异步写；`runInTransaction` 用于跨表事务。
- 未启用 WAL（仅 `getWritableDatabase()`），写事务会阻塞读；因此“所有 DB 操作走同一线程”是硬约束。
- `BaseService.replaceAllInTx` / `replaceWhereInTx` 会在内部自行开事务；调用方不得在其外层再嵌套事务或并发写同一张表。

### 9.2 Schema 与迁移

- 当前 schema 版本：`DatabaseVersionManager.CURRENT_VERSION = 2`。
- 迁移由 `MigrationOrchestrator` 负责，使用 SQLite `PRAGMA user_version` 与 `SCHEMA_HISTORY` 记录；失败记录在独立连接上，避免污染主事务。
- `GreenDaoUpgrade.smartMigrate` 负责表结构比对与补列/建表。
- 新增/修改实体的标准步骤：
  1. 修改实体与注解；
  2. 重新生成 DAO；
  3. 在 `EntityRegistrationHelper` 注册新 DAO（如新增实体）；
  4. 递增 `CURRENT_VERSION` 并补充迁移逻辑；
  5. 覆盖“旧版本库升级到新版本”的测试；
  6. 同步更新本文档与 `docs/database/greendao-guide.md`。
- 不得通过删除数据库文件来“解决”迁移问题；不得在 Release 构建中执行破坏性降级。

### 9.3 索引

现有索引维护由 `DbIndexMaintenance` 管理。新增索引前先确认查询模式与表规模；`@Index` 注解会改变生成 schema，必须与版本号、迁移和文档同步，不得只改注解不升版本。

---

## 10. 不变量与约束

1. **内容书号唯一**：`TabNavBody.bookNo`、`Book.bookNo`、`Chapter.bookId`、阅读入口 `bookId` 必须同值。
2. **本地代理键不跨层**：`Book.bookId`、`Chapter.id`、`ChatSessionBean.id` 等本地键不得直接作为服务端请求参数。
3. **排序显式化**：分类用 `TabNav.order`，书架用 `Book.sortCode`，章节用 `chapterSection`；禁止依赖 `findAll()` 的默认顺序。
4. **空值语义**：权限字段 `null` = 契约不完整 = 失败；内容列表空 = 无数据；两者不得混用。
5. **原子替换**：导航、药物、方剂等全量数据必须在事务内“先清后写”；中途异常整体回滚。
6. **敏感字段**：`UserInfo.token`、`UserInfo.accessKeySecret`、`AiConfig.ApiKey` 不得出现在日志、UI、导出文件或崩溃上报中。
7. **序列化缓存**：写入 `CacheHelper` 的类必须显式声明 `serialVersionUID`；字段变更需评估旧缓存兼容。
8. **线程**：主线程禁止任何 SQLite 调用；UI 回调必须回到主线程后再更新 View。
9. **无版本字段**：当前内容实体没有 `version` / `ordinal` 字段，不得假设服务端会做增量合并。

---

## 11. 新增数据能力检查清单

- [ ] 明确数据归属：内存 / GreenDAO / MMKV / 文件，是否真的需要新存储。
- [ ] 明确真源与刷新方式：本地优先还是网络优先？全量替换还是增量？
- [ ] 主键策略：服务端稳定业务键 vs 本地自增代理键；是否会被当作请求参数。
- [ ] 关系与排序：`@ToMany` 外键、排序字段、空值语义。
- [ ] 写入线程与事务：是否经 `DbService`，是否可能嵌套事务。
- [ ] 失败行为：读失败、写失败、解析失败分别如何处理；是否保留旧数据。
- [ ] 失效/清除边界：登录退出、权限变化、缓存过期、版本升级。
- [ ] 迁移：`CURRENT_VERSION`、`EntityRegistrationHelper`、升级测试、文档同步。
- [ ] 安全：是否包含 Token、密钥、口令或用户隐私；日志是否脱敏。
- [ ] 单元测试：纯逻辑（节流、字段完整性、排序）优先覆盖。

---

## 12. 与路线图的关系

- **M1**：落地权限缓存的登录/退出清理；为权限状态、退避与缓存边界补测试；不改变内容刷新行为。
- **M2**：补齐“到期后网络刷新”的真实链路，明确整表替换与失败回滚；统一列表数据的落库策略。
- **M3**：与服务端协同引入内容版本号/增量下发；评估 WAL、索引与启动 IO；完善可观测性。

任何改变真源、刷新语义或缓存失效边界的改动，必须先更新本文件并新增/修订 ADR，再进入实现。
