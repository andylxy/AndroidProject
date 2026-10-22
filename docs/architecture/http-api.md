# HTTP 请求契约

- **状态**：已接受（accepted）
- **日期**：2026-10-22
- **适用范围**：`app` 模块经 EasyHttp 发起的全部 HTTP 请求，以及 AI 对话的 SSE 流
- **关联决策**：[ADR-0005：HTTP 契约](adr/0005-http-contract.md)
- **关联文档**：[UI 设计规范](ui-spec.md)、[数据模型](data-model.md)、[组件与依赖清单](components.md)、[防重放攻击客户端指南](../security/anti-replay-client-guide.md)

---

## 1. 范围与原则

本文件定义客户端与服务端之间的**请求/响应契约**：地址前缀、鉴权头、响应信封、错误语义、列表分页、SSE 与变更流程。

原则：

1. **一个端点一个 API 类**：请求参数、路径、方法与响应类型集中声明，不在业务代码里拼 URL 或写 Header。
2. **信封统一**：业务响应一律为 `{code, msg, data}`，客户端只依赖 `code` 与 HTTP 状态，不解析 `msg` 做分支。
3. **错误分层**：HTTP 状态、业务 `code`、传输异常分别处理；页面不重复实现全局错误。
4. **向后兼容**：新增字段/端点可以，改字段含义、改分页基准、改错误码必须走兼容流程。
5. **安全默认**：不新增匿名兜底凭证；存量默认凭证回退记录为已知缺口（§4.2）；不记录 Token、口令、签名密钥；Release 关闭调试日志。
6. **存量不动**：现有内容接口（`AppBookRequest` 命名空间下的历史端点）保持原样，不在本规范中批量重构；本规范约束**新增与改动**的接口。

---

## 2. 环境与 Host

环境由 Gradle 参数 `ServerType` 决定（`configs.gradle`），构建期写入 `BuildConfig.HOST_URL`：

| 构建/环境 | `ServerType` | `HOST_URL` | 说明 |
| --- | --- | --- | --- |
| Debug（测试服） | `test`（默认） | `http://192.168.2.158:4321` | microfeed；`MICROFEED_AUTH=true` |
| Preview（预发布） | `pre` | `https://shapi.881019.xyz:8443` | netcore 契约 |
| Release（正式） | `product` | `https://shapi.881019.xyz:8443` | netcore 契约；`LOG_ENABLE=false` |

- 业务代码统一通过 `AppConfig.getHostUrl()` 取 Host，禁止硬编码环境地址。
- 环境只能在构建时通过 `-P ServerType=...` 切换，**不提供运行时切换开关**。
- AI 对话使用独立 Host：Debug 为 `http://192.168.2.158:4321`，Preview/Release 为 `https://aime.881019.xyz:8443`（见 `AiStreamApi.getHost()`）。

### 2.1 两个路径前缀

| 前缀 | 实现 | 完整形态 | 适用 |
| --- | --- | --- | --- |
| `/api/AppBookRequest/` | `RequestServer` | `{HOST_URL}/api/AppBookRequest/{api}` | 书籍、章节、药物、方剂、名词、样式等业务内容 |
| `/api/` | `VersionRequestServer` | `{HOST_URL}/api/{api}` | `app/version`、`app/announcements`、`app/search-permission`、`app/mingci-permission` 等 App 级端点 |

- 默认使用 `RequestServer`（EasyHttp 全局配置），无需在 API 类中声明。
- 使用 `/api/` 前缀的请求必须在调用链上显式 `.server(new VersionRequestServer())`；否则会被拼成 `/api/AppBookRequest/api/...` 而 404。
- 新端点归属哪个前缀由服务端路由决定；客户端不得为了“统一”擅自改前缀。

---

## 3. 请求声明模式

### 3.1 API 类

每个端点一个类，放在 `app/src/main/java/run/yigou/gxzy/data/remote/api/<domain>/`：

```java
public final class ExampleApi implements IRequestApi {

    private String bookId;

    public ExampleApi setBookId(String bookId) {
        this.bookId = bookId;
        return this;
    }

    @Override
    public String getApi() {
        return "ExampleEndpoint"; // 只写最后一段，不带前导斜杠
    }

    // 可选：EasyHttp 通过反射查找，写了 @Override 会编译失败
    public String getMethod() {
        return "GET";
    }
}
```

规则：

- `getApi()` 只返回端点最后一段，不包含 `/api/...` 前缀，不拼接查询串。
- GET 端点声明 `getMethod()` 返回 `"GET"`；**不要加 `@Override`**（`IRequestApi` 未声明该方法，加上会编译失败）。POST 为默认，无需覆写。
- 参数以字段 + 链式 setter 提供；字段名必须与服务端契约一致，不使用 `@SerializedName` 随意改名。
- API 类不写业务判断、不弹 Toast、不操作数据库；只做参数承载与路径声明。
- 需要独立 Host 的端点实现 `IRequestHost`（参考 `AiStreamApi`）；不得在 API 类里修改全局 Host。
- 请求体统一 JSON（`RequestServer#getBodyType` 返回 `RequestBodyType.JSON`）。

### 3.2 调用方式

```java
EasyHttp.get(lifecycleOwner)
        .api(new ExampleApi().setBookId(bookId))
        .request(new HttpCallback<HttpData<List<Item>>>(lifecycleOwner) {
            @Override
            public void onSucceed(HttpData<List<Item>> data) {
                // 主线程回调；先判空，再更新内存/UI，落库走 DbService
            }

            @Override
            public void onFail(Exception e) {
                // 页面级可恢复错误在这里处理；全局错误已由 RequestHandler 处理
            }
        });
```

- 必须传 `LifecycleOwner`，页面销毁后不再回调；`null` 仅限 Application 级别、明确不需要生命周期绑定的请求（如样式配置）。
- `onSucceed` / `onFail` 在主线程执行；不得在其中做耗时 IO 或直接写数据库。
- 同一页面重复触发请求时，必须自行防抖/取消（分页页码锁、搜索在途标记、按钮禁用）。

---

## 4. 鉴权与公共 Header

### 4.1 Header 清单

由 `InterceptorHelper` 统一注入，业务 API 不重复添加：

| Header | 值 | 说明 |
| --- | --- | --- |
| `app` | `2` | 客户端类型 |
| `SessionId` | `SerialUtil.getSerial()` | 设备会话标识 |
| `Content-Type` | `application/json;charset=UTF-8` | JSON 请求 |
| `Accept` | `application/json, text/plain, */*` | 普通请求 |
| `X-Device-Id` | 设备唯一标识 | 设备登记/吊销判定 |
| `app-version` | 整数 `versionCode` | 版本门；缺失/非整数会被判 426 |
| `Authorization` | `Bearer mflc_…` | 优先当前登录用户凭证；未登录时回落到 `SecurityConfig.getApiKey()` 默认凭证 |
| `X-Timestamp` | 当前时间戳 | 防重放，始终发送 |
| `X-Nonce` | 随机串 | 防重放，始终发送 |
| `Signature` | `Signature <hmac>` | 仅当存在有效 AccessKey 时发送（netcore） |
| `X-AccessKeyId` | AccessKey ID | 同 `Signature` |

- **Bearer 回退（已知缺口）**：`InterceptorHelper.loginBearer` 优先返回当前 `UserInfo.token`；未登录时回落到 `SecurityConfig.getApiKey()` 的默认 microfeed 凭证，因此未登录请求仍会带 `Authorization`。该默认凭证硬编码在客户端，与“不内置匿名兜底凭证”的目标冲突；`AppConst.AllowAnonymous_Token` 则是已无发送路径的死常量。二者移除需单独立项，不得扩散到新链路，具体值不在文档中记录。
- **硬编码 AccessKey（已知缺口）**：`SecurityConfig` 内置默认 AccessKey ID/Secret 用于 netcore 签名；当前行为保持不变，清理需单独立项，具体值不在文档中记录。
- microfeed（测试服）校验 Bearer 与防重放头，不校验 HMAC；netcore（预发布/正式）在 AccessKey 有效时校验签名。差异由 `InterceptorHelper` 统一处理，API 类不得感知。
- 登录口令传输方式由 `AppConfig.isMicrofeedAuth()` 控制：microfeed 环境发送明文口令，netcore 环境使用 SM2 加密（`LoginActivity.buildLoginRequest`）。**该差异只允许存在于登录链路**，其他接口不得复制。

### 4.2 Token 与登录态

- Bearer 来源优先为当前 `UserInfo.token`；未登录时回落到 `SecurityConfig.getApiKey()` 默认凭证（见上方已知缺口）。
- 业务 `code=1001` 表示登录失效：`RequestHandler` 抛 `TokenException`，跳转 `LoginActivity` 并结束其他页面。
- 普通 HTTP 401（不带吊销头）不会自动跳登录，按响应异常处理；只有 `401 + X-Device-Revoked: 1` 触发设备吊销提示。
- 不做静默刷新 Token；刷新/重新登录由登录页完成。

---

## 5. 响应信封与错误语义

### 5.1 成功信封

```json
{
  "code": 200,
  "msg": "ok",
  "data": {}
}
```

- `HttpData<T>` 字段：`code`、`msg`、`data`；`isRequestSucceed()` 仅当 `code == 200`。
- `data` 可以为对象、数组或 `null`；客户端必须判空，不得假设一定存在。
- `msg` 仅供展示/排查，不作为机器分支依据。

### 5.2 错误映射

| 条件 | 客户端异常/处理 | UI 行为 |
| --- | --- | --- |
| HTTP 200 + `code=200` | 成功 | 正常渲染 |
| HTTP 200 + `code=1001` | `TokenException` | 跳登录页，结束其他页面 |
| HTTP 426 | `VersionGateException`，触发 `UpdateManager.onVersionTooLow()` | 强制升级提示；页面不重复弹错 |
| HTTP 401 + `X-Device-Revoked: 1` | `DeviceRevokedException`，触发 `DeviceNoticeManager.onDeviceRevoked()` | 设备被禁用提示；**不**跳登录 |
| 其他非 2xx | `ResponseException` | 页面按失败处理或统一提示 |
| HTTP 200 + 其他 `code` | `ResultException(msg)` | 页面按失败处理；不自行解释业务码 |
| JSON/字段解析失败 | `DataException` | 失败；不得用默认值伪装成功 |
| 连接/读写超时、IO | `TimeoutException` / `NetworkException` | 可重试提示 |
| 请求取消 | `CancelException` | 静默，不提示 |

补充约束：

- **权限端点静默失败**：`app/search-permission` / `app/mingci-permission` 的 401/403/429/5xx、解析失败、字段缺失一律不弹窗、不跳登录，按 fail-closed 处理并进入 30 分钟退避（见[数据模型](data-model.md) §6）。
- 页面不得把 `code` 与 HTTP 状态混用；新增业务码必须先登记到本表并说明客户端行为。
- 错误提示不得包含 URL、Header、Token、响应原文；调试信息只写 `EasyLog`。

---

## 6. 列表与分页契约

### 6.1 适用范围

仅约束**新增或改动**的列表端点。现有不分页接口（如 `getNav`、`GetBookChapter`）保持现状，不强制改造。

### 6.2 请求参数

| 参数 | 类型 | 必填 | 约定 |
| --- | --- | --- | --- |
| `page` | int | 是 | **从 1 开始**；不得传 0 |
| `pageSize` | int | 否 | 默认 20；最大 100；超出按 100 处理或由服务端拒绝，客户端不得传更大值 |
| 其他过滤字段 | — | 否 | 由具体端点定义，必须可组合、无副作用 |

### 6.3 响应结构

统一使用 `HttpListData<T>`（`app/src/main/java/run/yigou/gxzy/data/remote/model/HttpListData.java`）：

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "pageIndex": 1,
    "pageSize": 20,
    "totalNumber": 57,
    "items": []
  }
}
```

- `pageIndex` 必须回显请求页码（1-based）；`pageSize` 回显实际页大小；`totalNumber` 为总条数；`items` 为当前页数据。
- `isLastPage()` 的实现为 `ceil(totalNumber / pageSize) <= pageIndex`；客户端不得另写一套“items.size() < pageSize 即最后一页”的判断。
- `HttpListData` 是**唯一**的分页响应模型；禁止为某个端点新建平行分页类。
- 若服务端返回 0-based 页码或缺失字段，视为契约不符：记录日志并按失败处理，客户端不做 ±1 猜测。

### 6.4 客户端行为

- 第一页加载成功替换列表；后续页追加。
- `isLastPage()` 为真、`items` 为空、或加载失败时停止继续加载。
- 同一页码不得并发重复请求；刷新时取消/忽略在途的旧页码结果。
- 空列表是合法响应（`totalNumber=0`），展示空态而不是错误态。
- 分页只影响网络数据；本地缓存是否分页由具体数据源决定（当前内容表全量缓存）。

---

## 7. SSE 契约（仅 AI 对话）

### 7.1 端点

- 路径：`streamConversation`，POST JSON。
- Host：AI 独立 Host（见 §2）。
- Header：`Content-Type: application/json; charset=utf-8`、`Accept: text/event-stream`、`Cache-Control: no-cache`、`Connection: keep-alive`、`app: 2`、`SessionId`。
- 鉴权/防重放：由 `InterceptorHelper.addSseSecurityHeaders` 注入，与普通请求同源；SSE 绕过 EasyHttp 拦截器时**必须**调用该 helper，不得手写一套 Header。
- 请求体字段：`query`（用户问题）、`conversationId`（会话 ID）、`endUserId`（用户 ID）。

### 7.2 事件模型

`SseChunk` 字段：`type`、`content`、`error`、`isThinking`。已知 `type`：

| type | 含义 | 客户端处理 |
| --- | --- | --- |
| `start` | 流开始 | 进入“接收中”状态 |
| `chunk` | 普通内容块 | 追加到当前消息 |
| `thinking` | 思考过程 | 归入思考区，可折叠 |
| `answer` | 正式回答 | 追加到回答区 |
| `done` | 传输完成 | 结束流，落库/更新状态 |
| `error` | 服务端错误 | 展示错误并结束流 |

### 7.3 约束

- SSE 只用于 AI 对话；其他接口不得使用长连接/SSE。
- 超时：连接 30s、读写 300s（`AiStreamApi`）；超时按流失败处理，保留已接收内容。
- 页面销毁、会话切换、用户停止时必须取消当前流；不得让回调更新已销毁的 View。
- 流式过程中不得重复发送同一会话的并发请求；新问题必须等上一条结束或显式停止。
- `onComplete` 与 `type=done` 都可能表示结束，客户端必须做幂等收尾，避免重复落库。

---

## 8. 端点清单

### 8.1 既有端点（示例，保持现状）

| 方法 | 路径 | 说明 | 关键参数/响应 |
| --- | --- | --- | --- |
| GET | `/api/AppBookRequest/getNav` | 发现页导航与书籍 | `HttpData<List<TabNav>>` |
| GET | `/api/AppBookRequest/GetBookChapter` | 某书章节目录 | `bookId`；`HttpData<List<Chapter>>` |
| GET | `/api/AppBookRequest/GetTipsStyleConfig` | Tips 样式配置（全量） | `version`（本地计数器，非增量游标）；响应含服务端 `version` |
| GET | `/api/app/search-permission` | 搜索权限（`global` / `book`） | `VersionRequestServer`；静默失败 |
| GET | `/api/app/mingci-permission` | 名词解释权限（`allowed`） | `VersionRequestServer`；静默失败 |
| GET | `/api/app/announcements` | 公告 | `VersionRequestServer`；公开端点 |
| GET | `/api/app/version` | 版本门/升级信息 | `VersionRequestServer`；426 处理 |
| POST | `{AI_HOST}/streamConversation` | AI 流式问答 | SSE，见 §7 |

### 8.2 新增/改动端点

新增端点必须在同一次变更中补充：

1. 本文件的清单行（方法、路径、参数、响应、错误、权限要求）；
2. API 类与响应模型；
3. 至少一个解析/错误映射测试；
4. 若涉及分页，明确使用 `HttpListData`；
5. 若涉及新业务码，先更新 §5.2。

未登记的端点不得进入实现。

---

## 9. 变更与兼容流程

1. **新增字段**：服务端可加可选字段；客户端忽略未知字段（Gson 默认行为），不得因多字段解析失败。
2. **新增端点**：按 §8.2 登记；旧客户端不调用即不受影响。
3. **字段改名/改类型/改含义、分页基准变化、错误码语义变化**：属于破坏性变更，必须：
   - 与服务端确认兼容窗口；
   - 新增端点或版本化路径，不原地改语义；
   - 新增/修订 ADR；
   - 更新本文件与相关数据模型/UI 文档；
   - 提供旧客户端行为说明与回滚方案。
4. **版本门**：客户端始终发送整数 `app-version`；服务端以 426 表示低于地板。客户端不得自行推断版本是否兼容。
5. **删除端点**：先确认无调用方（grep API 类与字符串），再删代码与文档；不得保留“已废弃但无说明”的入口。

---

## 10. 安全约束

- 不在源码、Gradle 配置、文档示例中新增真实 Token、口令、AccessKey、私钥；`jks/`、`local.properties` 与构建凭据视为敏感信息。
- 不打印请求/响应中的 `Authorization`、`Signature`、`X-AccessKeyId`、登录口令、`UserInfo.token`、`AiConfig.ApiKey`。
- 测试服（microfeed）明文口令仅用于开发联调；Release 必须走 netcore + SM2，不得把测试配置带入正式包。
- 证书/域名变更必须同步 `configs.gradle` 与本文档；不得在代码中绕过 TLS 校验。
- 设备吊销（401 + `X-Device-Revoked: 1`）不是登录失效，不得跳登录页造成循环。

---

## 11. 新增接口检查清单

- [ ] 端点归属前缀正确（`AppBookRequest` / `VersionRequestServer` / AI Host）。
- [ ] 一个端点一个 API 类；GET 的 `getMethod()` 未加 `@Override`。
- [ ] 参数名、类型、必填性与服务端契约一致；分页使用 `page`（1-based）+ `pageSize`（默认 20，最大 100）。
- [ ] 响应使用 `HttpData` / `HttpListData`，未新建平行信封。
- [ ] 成功、1001、426、401 吊销、其他非 2xx、解析失败、超时均有明确处理。
- [ ] 请求绑定 LifecycleOwner；页面销毁可取消；重复提交有保护。
- [ ] 权限端点保持静默失败 + fail-closed + 30 分钟退避。
- [ ] 不新增匿名兜底凭证；不记录敏感 Header/字段。
- [ ] 已更新本文件、数据模型/UI 文档与 ADR（如涉及契约变更）。
- [ ] 至少通过 `gradlew.bat testDebugUnitTest`；涉及网络解析的改动有对应测试。

---

## 12. 与路线图的关系

- **M1**：本契约作为新增接口的准入基线；存量接口不动；权限缓存的登录/退出清理在数据层完成。
- **M2**：新列表端点统一分页；补齐错误空态与重试；内容刷新链路与服务端对齐。
- **M3**：内容版本号/增量下发、可观测性（请求耗时/失败率）、契约测试与发布门禁。

任何契约级改动必须先更新本文件与 ADR，再进入实现。
