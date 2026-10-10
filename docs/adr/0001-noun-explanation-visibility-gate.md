# 名词解释（$g）显示权限闸门 — 设计方案（锁定版）

> 状态：**方案已锁定 + 已量化**（grill 第四轮 + code-review 双轴复核 + 量化补充，2026-10-09）。**待"进入执行"才写代码。**  
> 决策依据（已逐条核对真实代码，非假设）：
>
> - microfeed 仓 `.scratch/search-permission/DESIGN.md` §5（后端权限端点蓝图）
> - 同仓 Android：`SearchPermissionApi` / `SearchPermissionManager` / `SearchPermissionStore`（权限拉取+判定范式）
> - 同仓 Android：`AnnouncementApi` / `AnnouncementManager` / `AnnouncementStore`（TTL/重载范式，G2 参考）
> - microfeed：`src/server/tcm/reads.ts:773 getAppAllTerms` + `src/pages/api/AppBookRequest/GetAllMingCi` + `src/server/api/access.ts:51,81`（内容接口现状）

---

## 1. 已核实代码事实（grill 验证，非臆测）

- **`$g{}` 是配置驱动标记，非硬编码特例**：`TipsTextRenderConfig` 默认 `g` → 半透明蓝（`Color.argb(230,0,128,255)`）、`linkType=3`（名词）。用户确认：**名词文本永远渲染、可点，不可隐藏**。
- **"本地名词缓存列表" = `GlobalDataHolder.mingCiContentMap`**（`Map<String, MingCiContent>`，`isMingCiDataLoaded()` 标志）—— 内存里的名词解释表，所有 `$g` 点击的唯一数据源。
- **两个加载入口都必须闸**（否则会漏）：
  - `AppDataInitializer.loadMingCiData()`（`app/src/main/java/.../app/AppDataInitializer.java:195`）—— 冷启动，本地 DB → 内存。
  - `AppDataManager.loadMingCiData()` → `useLocalMingCi` → `requestMingCiDataFromNetwork`（`app/src/main/java/.../manager/data/AppDataManager.java:566-633`）—— 含本地读 + 网络 `MingCiContentApi` + 落库三条分支。
- **点击 `$g` 链路**：`MingCiSearchStrategy.search` → `isDataLoaded()` 假 → `notLoaded()`（现有默认行为，**G4 保留**）。
- **后端内容接口现状**：`GET /api/AppBookRequest/GetAllMingCi`（`access.ts:51,81` 登记于 `APP_BOOK_REQUEST_CONTENT_SUFFIXES`），实现 `getAppAllTerms`（`reads.ts:773`），**登录可见、匿名不可达**。

---

## 2. 锁定的分叉（用户拍板，2026-10-09）

| #  | 决策点     | 结论                                                                                                                             |
| -- | ------- | ------------------------------------------------------------------------------------------------------------------------------ |
| G1 | 权限来源    | **后端新接口**，参考 search-permission（`app/search-permission` 范式）。                                                                    |
| G2 | TTL 与重载 | 参考**公告管理方式**：成功缓存按**本地自然日**有效（次日 resume 重验一次），失败 **30 min 退避**（`FetchClockStore.RETRY_INTERVAL_MS = 1,800,000 ms`，与公告/搜索共用单源）。 |
| G3 | 粒度      | **全局单开关**（一个布尔 `allowed`）。                                                                                                     |
| G4 | 无权限点击表现 | **不处理**；列表空 → 默认 `notLoaded()` 行为（当前功能）。                                                                                       |
| G5 | 范围收口    | 闸 `loadMingCiData`（两个入口）；"后端检查接口权限是否返回数据"。                                                                                     |

---

## 3. 推荐架构

### 3.1 后端（microfeed）— 镜像 search-permission

1. **新权限码 `app:mingci:view`**（G3 单开关 → 一个码），三处镜像（新功能权限清单）：
   - migration `00xx_ext_mingci_permission_code.sql` 插 `ext_permissions`（id = `p_app_mingci_view`）；
   - `src/shared/Constants.ts` 的 `PERMISSION_CODES` 加一条；
   - `src/server/rbac/seed.ts` 的 `RBAC_PERMISSIONS` 加一条（bootstrapAdmin 依赖，漏一则全新安装 500）。
   - **无菜单行**：是 App 能力权限，非后台页面权限，不进 `ext_menu` / `ext_menu_permissions`。
2. **新端点 `GET /api/app/mingci-permission`**，注册方式**镜像 search-permission 的真实接线**（非 DESIGN.md 草案措辞）：
   - `src/server/api/access.ts` 的 `apiPathDetails` 内**新增专属特殊分支**（参照 `search-permission` 在 access.ts:158-164 的写法，`pathname === "${LEGACY_API_BASE_PATH}app/mingci-permission/"` → 返回 `{canonicalPath, kind:"integration", legacy:false}`）——**不要**塞进 `integrationSuffix`（其 `!legacy` 块无 `app/` 分支，`app/mingci-permission/` 不会被匹配，会 404 且误发 Deprecation 头）。
   - `src/server/api/api-permissions.ts` 的 `DOMAIN_RULES` 增加 `{prefix:"app/mingci-permission", read:"app:mobile:access", write:"app:mobile:access"}`（与 search-permission 一致，让请求走完整鉴权链：限流/封禁/设备吊销/`app:mobile:access`；不带凭证到处理器后保底 401）。
3. **端点返回值 `{ allowed: bool }`**，status 语义同 search-permission：
   - `200 {allowed}` = 拉取成功；角色不含码 → `allowed=false` 是**有效业务答案**（不重试）；
   - `401`（未登录/凭证无效/设备吊销/封禁）、`403`（无 `app:mobile:access`）、`429`（限流）、`5xx` = 失败（客户端 fail-closed + 退避）；
   - `no-store`；不记登录日志；`*` 通配与 guard 语义一致（持有通配即 `allowed=true`）。
4. **纵深防御（R1 锁定"两者都做" → 必做，与 G5 措辞一致）**：在 **`GetAllMingCi` 处理器内部**判 `app:mingci:view`——缺码时 `return []`（空数组），有码才 `appEnvelope(getAppAllTerms())`。代码形态（伪码）：在 `GetAllMingCi` 处理器鉴权通过后、调用 `getAppAllTerms()` 前，用请求上下文已解析的权限集合判断 `app:mingci:view`；缺失即 `return appEnvelope([])`。该判定为**进程内纯布尔**（0 网络开销、< 1 ms），不改变现有 200 信封与匿名 401 语义。**不要**在 `DOMAIN_RULES` 给该路由加 `app:mingci:view` read 规则：`requiredApiPermission` 按 `suffix.startsWith(rule.prefix)` 首匹配返回**单一**码（api-permissions.ts:150-152），且 `AppBookRequest/` 更宽前缀先命中 → 加具体规则要么被忽略、要么替换现有 `app:mobile:access` 门且缺码返 **403**，与"返回空数组/无回归"矛盾。`DOMAIN_RULES` 保留 `AppBookRequest/`→`app:mobile:access`（让请求到达处理器再做内部判定）。当前唯一调用方是 Android 的 `AppDataManager.requestMingCiDataFromNetwork`，收到空 → map 空 → 默认行为，**无回归**。
5. **不进 OpenAPI 清单**（App 内部通道，同 search/announcement 豁免；AGENTS.md 点名清单补该路径）。

### 3.2 Android — 镜像 `SearchPermissionManager` + 公告 TTL

**新增：**

- `MingCiPermissionApi`（`data/remote/api/.../MingCiPermissionApi.java`）→ `GET /api/app/mingci-permission`（照抄 `SearchPermissionApi`）。
- `MingCiPermissionState`（model）→ `{ allowed: boolean }`（比 search 的两个布尔更简单，G3 单开关）。
- `MingCiPermissionStore`（MMKV）→ 缓存 `allowed` + `lastSuccess`/`lastAttempt` 节流时钟（照抄 `SearchPermissionStore` / 复用 `AnnouncementStore.RETRY_INTERVAL_MS` 节流）。
- `MingCiPermissionManager`（照抄 `SearchPermissionManager` 结构）：
  - `sFetchOk`（**进程内、不持久化、fail-closed**，INV-2）：仅 `fetchAndEvaluate` 成功落盘后置 `true`，任何失败置回 `false`。
  - `sAllowed`（进程内持有，网关直接读，不碰磁盘）。
  - `registerForegroundCheck()`：`LaunchOnceGate` 进程级一次（INV-3）+ `ActivityManager` 前台/resume 回调触发首次拉取。
  - `fetchAndEvaluate`：`EasyHttp.get` + `VersionRequestServer` + `MingCiPermissionApi`；`onSucceed` → 写盘 + `markFetchSuccess()` + `sFetchOk=true` + **触发懒加载**；`onFail` → **INV-1 静默**（只 log、不 toast）+ `sFetchOk=false` + 30min 退避重试（共用 `RetryScheduler`）。
  - `isAllowed()` 网关：**先判 `sFetchOk`，为假直接 `false`（不读缓存）**，再判 `sAllowed`（INV-2 顺序，缺字段也按关闭）。
  - **G2 TTL**：`shouldFetchNow` 节流（参考 `AnnouncementStore.RETRY_INTERVAL_MS`）—— TTL 过期后在 resume/前台回调重验；离线/退避期不重复打请求。

**修改（闸两个加载入口）：**

- `AppDataInitializer.loadMingCiData()`：包 `if (MingCiPermissionManager.isAllowed()) { ...原逻辑... }`；否则 map 保持空（→ G4 默认行为）。
- `AppDataManager.loadMingCiData()`：入口处 `if (!MingCiPermissionManager.isAllowed()) { callback.onSuccess(null); return; }`（同时闸住本地读 + 网络两条分支）。
- **懒加载触发**（关键时序，单飞）：`MingCiPermissionManager` 持有 `CopyOnWriteArrayList<Runnable> sOnAllowedListeners`。**注册点**：`AppDataManager` 初始化时 `addOnAllowedListener(() -> loadMingCiData(...))`。在 `onSucceed` 且 `allowed=true` 时（首拉成功、或 `false→true` 翻转），若 `GlobalDataHolder.mingCiContentMap` 尚未装载，以 `sLazyLoadTriggered`（AtomicBoolean，进程内单飞）守卫调用一次 listener → 补载列表；派发成本 **< 1 ms、恰好 1 次/进程**。这样"冷启动先 fail-closed，权限确认后再补载"，且不会因多次 resume 重复触发全量加载。
- **权限翻转 false**（建议必做，非可选）：`GlobalDataHolder.getInstance().reloadMingCiData()` 清空，保证不残留旧数据；同时 `sLazyLoadTriggered` 复位，待权限再次翻转回 true 时能重新懒加载。

---

## 3.3 量化指标（SLI / SLO / 预算）

> 所有数值均取自本仓既有范式（`FetchClockStore.RETRY_INTERVAL_MS`、`AnnouncementStore.shouldFetchNow`），非估算；实施时以单测断言锁定。

### 3.3.1 时间预算

| 指标                       | 目标值                                                           | 依据                                                          |
| ------------------------ | ------------------------------------------------------------- | ----------------------------------------------------------- |
| 退避间隔 `RETRY_INTERVAL_MS` | **1,800,000 ms（30 min）**                                      | `FetchClockStore.RETRY_INTERVAL_MS = 30*60*1000`，与公告/搜索共用单源 |
| 权限缓存 TTL                 | **1 本地自然日**（次日 resume 重验 1 次）                                 | 镜像 `AnnouncementStore.isSameLocalDay`，非 24h 固定窗口            |
| 权限拉取触发                   | App 启动 foreground 回调即拉（无额外延迟）                                 | 同 `SearchPermissionManager.onLaunchReady` 立即拉               |
| 列表填充延迟 SLO               | 自 `allowed=true` 起 **P95 ≤ 3,000 ms** 内 `mingCiContentMap` 装满 | 一次额外 `GetAllMingCi` 往返（典型 Wi-Fi RTT ≤ 2s）                   |
| `isAllowed()` 调用成本       | **O(1)、< 1 ms、零分配、无磁盘/网络**                                    | 纯进程内 `sFetchOk` 布尔 + `sState` 字段读；仅在 2 个加载入口调用，绝不在渲染期调用     |
| 冷启动可见性窗口                 | 用户启动后 **≥ 3 s** 的任意 `$g` 点击均可出解释                              | 前 3 s 内点击走默认 `notLoaded`（瞬时、≤ 1 次启动）                        |

### 3.3.2 容量 / 资源预算

| 指标                         | 目标值                                                  |
| -------------------------- | ---------------------------------------------------- |
| `mingCiContentMap` 常驻内存    | **≤ 10 MB**（单本/全书名词条目，量级数百~数千；无分页、单次拉取；超界由服务端响应规模决定） |
| `MingCiPermissionState` 报文 | **< 200 B**（1 布尔 + 信封）                               |
| MMKV 缓存条目                  | 3（allowed + `lastSuccessAt` + `lastAttemptAt`），可忽略   |

### 3.3.3 频率 / 配额

| 行为                     | 配额                                                                              |
| ---------------------- | ------------------------------------------------------------------------------- |
| 权限拉取成功频率               | **≤ 1 次/本地自然日**（公告式每日闸门）+ **≤ 1 次/30min 退避窗**（失败重试）                             |
| 进程内拉取链                 | `LaunchOnceGate` 保证仅 1 次启动期拉取链；`RetryScheduler` 单飞保证退避期仅 1 个定时器                 |
| 无权限时 `GetAllMingCi` 调用 | **0 次**（闸在 `loadMingCiData` 入口，省带宽，即本功能核心收益）                                    |
| 有权限时列表加载               | 每次冷启动 **1 次** `GetAllMingCi`；权限翻转时 +1 次懒加载；会话内复用内存表，**每 `$g` 点击 0 次网络**（只读 map） |

### 3.3.4 正确性 SLI / SLO

| SLI                  | SLO                                                     | 测量方式                                                           |
| -------------------- | ------------------------------------------------------- | -------------------------------------------------------------- |
| 授予 + 重启后 `$g` 点击出解释率 | **100%**（启动 ≥3s 后）                                      | 联调：授予→重启→连续点 20 个 `$g` 全出解释                                    |
| 摘除 / 断网冷启动 `$g` 显示率  | **0%**（fail-closed，默认 `notLoaded`）                      | 联调：摘除→重启→点 `$g` 全按默认不显示；无崩溃、无 StrictMode                       |
| 两加载入口均被闸             | **0** 个 `loadMingCiData`/`getMingCi` 调用越过 `isAllowed()` | 联调：grep/断点确认；单元：桩 `isAllowed=false` 时入口立即 `onSuccess(null)` 返回 |
| 权限被服务端回收后的生效时延       | **≤ 1 本地自然日**（下次 resume 重验；若进程持续前台最长滞后 1 自然日）           | 联调：role 撤销→次日/下次 resume→map 清空→0% 显示                           |
| `isAllowed` 线程安全     | 任意线程调用无锁竞争、无 NPE                                        | 单元：主线程 + 后台线程并发读 1000 次一致                                      |

### 3.3.5 代码量预算

| 端       | 新增                                                                                                                          | 规模                                                     |
| ------- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| Android | `MingCiPermissionApi` + `MingCiPermissionState` + `MingCiPermissionStore` + `MingCiPermissionManager` 4 文件                  | **≈ +250~300 LOC**，严格镜像 search-permission 四件套          |
| 后端      | 端点 `mingci-permission.ts` + `resolve.ts`；`Constants.ts`/`seed.ts`/migration 三镜像各 1 行；`access.ts`/`api-permissions.ts` 各 1 处 | **≈ +1 文件 + 数行改动**                                     |
| 不引入     | 任何超出"每功能独立 manager"惯例的新抽象                                                                                                   | 复用 `FetchClockStore`/`RetryScheduler`/`LaunchOnceGate` |

---

## 4. 为什么是这个位置

| 候选位置                                                           | 否决理由                                           |
| -------------------------------------------------------------- | ---------------------------------------------- |
| 渲染期跳过整段令牌（`renderText` 循环）                                     | 用户明确"**该文本不可以不显示**"——名词本身永远可见。                 |
| 点击期拦截（`TipsClickHandler.handleClick` / `MingCiSearchStrategy`） | 你要的是"拦显示/拦加载"，不是"拦动作"；名词仍可见可点。                 |
| 配置伪装（`getStyleConfig("g")` 透明色/无链接）                            | 样式 ≠ 可见性，透明色仍占位可选中，做不到"什么都不显示"。                |
| **列表加载处（本方案）**                                                 | 权限管的是背后的**数据列表**，名词文本照常渲染；点了因底表空而"什么都不显示"（G4）。 |

---

## 5. 实施步骤（"进入执行"后使用）

**后端（microfeed）：**

1. migration `00xx` + `Constants.ts` + `seed.ts` 三镜像；跑 `rbac.test.ts` 及相关一致性测试（确认无菜单行不红）。
2. `access.ts` integrationSuffix + `api-permissions.ts` DOMAIN_RULES 注册 `mingci-permission`；中间件测试补 401/403/429 用例。
3. 端点 `src/pages/api/app/mingci-permission.ts` + `src/server/app-mingci-permission/resolve.ts`；端点测试（200+false、通配、401 透传、5xx 不伪装、no-store）。
4. （纵深防御，必做）`GetAllMingCi` 加 `app:mingci:view` read 规则 + 空数组返回。
5. AGENTS.md OpenAPI 豁免清单补 `app/mingci-permission`（AGENTS.md:122-123 当前仅列 `AppBookRequest/*`、`/api/app/version`、`/api/app/announcements`、`/api/app/search-permission`，不含本端点；不补会违"API 与 OpenAPI 同步"标准，见 AGENTS.md:109-110）。

**Android：**  
6\. model `MingCiPermissionState` + `MingCiPermissionApi` + `MingCiPermissionStore`(MMKV) + `MingCiPermissionManager`（抄 search，单布尔）。  
7\. JVM 单测：节流 + 网关纯函数 + fail-closed 顺序。  
8\. 闸 `AppDataInitializer.loadMingCiData` + `AppDataManager.loadMingCiData`；接懒加载 Listener。  
9\. `AppApplication` 启动挂 `MingCiPermissionManager.registerForegroundCheck()`（同 search-permission 接线点）。  
10\. 联调（adb + 后端，按 logcat 循环修复）：授予/摘除 → 重启 → `$g` 显示/不显示；断网冷启动 fail-closed；TTL 过期重验一致。

### 5.1 量化测试清单（断言数，实施时以单测锁定）

- **后端（vitest，≈ 12 例）**：
  - 权限码三镜像一致性（migration/Constants/seed 三处同 id 与码）：1


- 端点 `mingci-permission`：200+allowed、200+false（有效业务答案不重试）、通配 `*`→true、401 透传、403、429、5xx 不伪装、no-store 头：8
- `GetAllMingCi` 服务端：含 `app:mingci:view`→满数组；不含→`[]` 空（非 403）：2
- `access.ts` `apiPathDetails` 对 `mingci-permission` 返回 `legacy:false`：1
- **Android（JVM 单测，≈ 10 例）**：
  - `shouldFetchNow`（公告式）：同日成功→false、同日失败→30min 退避、跨午夜→true、时钟回拨→放行：4
  - `isAllowed` fail-closed 顺序：`sFetchOk=false`→false（忽略缓存）、`sState=null`→false、`allowed=true`→true：3
  - `onSucceed` 写盘 + 标记 + **恰好触发 1 次**懒加载 Listener：1
  - `onFail` 静默 + `sFetchOk=false` + `RetryScheduler` 单飞（仅 1 个定时器）：1
  - `GetAllMingCi` 返回空→map 空→默认 `notLoaded` 路径：1
- **总计 ≈ 22 自动化断言** + 联调 checklist（授予/摘除/断网/TTL 共 4 场景 × 重启验证）。

---

## 6. 风险 / 待确认

- **R1（已锁定：两者都做）**：后端"新增 `mingci-permission` 布尔端点"（纯镜像 search，G1 主路径：权限拉取+判定）+ "GetAllMingCi 服务端按 `app:mingci:view` 权限返空"纵深防御，**两者均必做**（无互斥，纵深防御）。前者是客户端权限判定的数据来源；后者是服务端兜底——即使客户端越权/缓存逃逸/旧版本，服务端也不吐名词数据。
- **R2 时序**：冷启动 `AppDataInitializer` 早于权限拉取 → 首拉前 `isAllowed()=false` → 不预载；权限确认后靠懒加载补。量化目标：自 `allowed=true` 起 `mingCiContentMap` **P95 ≤ 3s** 装满（§3.3.1）；启动 ≥3s 后的 `$g` 点击 100% 出解释，前 3s 内点击走默认 `notLoaded`（瞬时、≤1 次启动，可接受）。
- **R3** `GetAllMingCi` 当前在 `/api/AppBookRequest/` 命名空间；若做 R1 的服务端 read 规则，需确认是否影响其它调用方（当前仅 Android `AppDataManager` 一处）。
- **R4（review 修正，2026-10-09）**：code-review 双轴复核发现后端两处接线事实错误，已就地修正：① 端点注册须走 `apiPathDetails` 专属分支（镜像真实 `search-permission`，非 `integrationSuffix`）；② `GetAllMingCi` 纵深防御的 `app:mingci:view` 判定须放**处理器内部**返空数组，不可改 `DOMAIN_RULES`（否则 403 而非空数组）。OpenAPI 豁免为明确待办（步5）。

---

## 7. 验证

对照 §3.3 量化指标做验证：

- **单元（≈22 断言，见 §5.1）**：网关纯函数（节流 `shouldFetchNow` 4 例 + fail-closed 顺序 3 例）、懒加载单飞（1 例）、`onFail` 静默+单飞（1 例）、`GetAllMingCi` 空数组路径（1 例）；后端端点 8 例 + 服务端空数组 2 例 + 权限码镜像 1 例 + `apiPathDetails` 1 例。
- **联调（按 logcat 循环修复，对照 §3.3.4 SLI）**：
  - 授予→重启→连续点 20 个 `$g`：**100%** 出解释；列表填充自 `allowed=true` 起 **P95 ≤ 3s**。
  - 摘除/断网冷启动：**0%** 显示（fail-closed），无崩溃、无 StrictMode DiskReadViolation。
  - 两加载入口均被闸：grep 确认 **0** 个 `loadMingCiData`/`getMingCi` 调用越过 `isAllowed()`；桩 `isAllowed=false` 时入口立即 `onSuccess(null)` 返回。
  - 权限被服务端回收：role 撤销 → 下次 resume/次日 → map 清空 → **0%** 显示（生效时延 ≤ 1 本地自然日）。
  - TTL 过期重验一致：次日启动重新拉权限，结果与缓存一致，无多余请求（≤ 1 次/日）。
