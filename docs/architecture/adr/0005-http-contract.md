# ADR-0005：HTTP 契约——沿用 EasyHttp 与统一信封，新增列表统一分页

- **状态**：已接受（accepted）
- **日期**：2026-10-22
- **决策者**：用户
- **影响域**：`data/remote/api`、`network/server`、`network/security`、`network/exception`、AI SSE
- **编号说明**：本编号属本仓 `docs/architecture/adr/` 序列。`manager/update`、`network/exception` 注释中的 `ADR-0005` 指 **microfeed 仓**的版本门决策，不在本仓，阅读代码注释时勿与本篇混淆。

---

## 1. 背景（Context）

客户端网络层现状：

- EasyHttp 13.5（本地 AAR）+ OkHttp 5.3；端点以手写 `IRequestApi` 类声明，一个端点一个类。
- 默认前缀 `RequestServer`：`{HOST_URL}/api/AppBookRequest/`；App 级端点使用 `VersionRequestServer`：`{HOST_URL}/api/`。
- 响应统一 `HttpData{code,msg,data}`；业务成功码 200，登录失效码 1001。
- 公共 Header、Bearer、防重放、设备标识与版本码由 `InterceptorHelper` 注入；SSE 通过 `addSseSecurityHeaders` 复用同一套逻辑。
- 错误由 `RequestHandler` 统一映射：426 → 升级门，1001 → `TokenException` 跳登录，401 + `X-Device-Revoked: 1` → 设备吊销，其他非 2xx → `ResponseException`。
- AI 对话是唯一的 SSE 场景，使用独立 Host、长超时与 `SseChunk` 事件模型。

问题：

1. 仓库定义了 `HttpListData`，但当前没有端点使用，分页参数/页码基准没有成文约定；
2. 历史端点风格不一（GET/POST、命名、是否分页），缺少“新增接口必须怎样”的准入基线；
3. 错误码、静默失败、SSE 结束语义分散在代码注释里，新人容易重复实现；
4. 若现在迁移 Retrofit/OpenAPI，会牵动所有端点、拦截器与错误处理，风险高于收益。

---

## 2. 决策（Decision）

**沿用 EasyHttp + 手写 API 类 + 统一信封；只对新增/改动端点施加严格契约；存量端点保持原样。**

具体：

1. **不迁移网络框架**：本轮不引入 Retrofit、OpenAPI 代码生成、GraphQL 或第二套 OkHttp 客户端。
2. **保持两个前缀**：业务内容走 `RequestServer`（`/api/AppBookRequest/`），App 级端点走 `VersionRequestServer`（`/api/`）；新端点归属由服务端路由决定，客户端不擅自统一。
3. **保持信封与错误语义**：
   - 成功 `HTTP 200 + code=200`；
   - `code=1001` → 登录失效；
   - HTTP 426 → 版本门；
   - HTTP 401 + `X-Device-Revoked: 1` → 设备吊销；
   - 其他非 2xx / 非 200 code → 页面级失败；
   - 不新增第二套响应包装。
4. **新增列表统一分页**：
   - 请求：`page` 从 1 开始，`pageSize` 默认 20、最大 100；
   - 响应：`HttpListData.ListBean{pageIndex,pageSize,totalNumber,items}`；
   - 客户端用 `isLastPage()` 判断结束，不另写 `items.size() < pageSize`；
   - 存量不分页端点不强制改造。
5. **SSE 仅用于 AI 对话**：沿用 `AiStreamApi` + `SseClient`、`SseChunk` 的 `start/chunk/thinking/answer/done/error` 事件模型；其他接口不得使用长连接。
6. **权限端点静默失败**：搜索/名词权限接口的 401/403/429/5xx、解析失败、字段缺失不弹窗、不跳登录，按 fail-closed + 30 分钟退避处理。
7. **契约变更流程**：破坏性变更必须新增端点/版本化路径、后端协同、更新 ADR 与文档；不得原地改字段含义。
8. **安全默认**：不新增匿名兜底凭证；存量 `SecurityConfig.getApiKey()` 默认 microfeed 凭证回退与内置默认 AccessKey 记录为已知缺口（见 `http-api.md` §4.2），移除需单独立项；不记录 Token/口令/签名；Release 关闭调试日志。

---

## 3. 被否决的替代方案

| 方案 | 否决理由 |
| --- | --- |
| 迁移 Retrofit + 注解式接口 | 需要重写全部 API 类、拦截器与错误处理，且当前手写模式已能表达 GET/POST、独立 Host 与 SSE；收益不足以覆盖迁移风险 |
| OpenAPI 生成客户端 | 依赖后端维护完整 spec；生成代码难以承载现有统一错误处理/权限静默失败逻辑 |
| 把所有端点统一到 `/api/` 前缀 | 会破坏存量路由与线上兼容；服务端也未提供统一命名空间 |
| 新列表使用新的分页信封（如 `records/total/current`） | 项目已有 `HttpListData`，再建一套会造成双模型与解析分叉 |
| 存量接口一次性改成分页 | 影响所有调用方与后端，超出本轮范围；与“存量不动”原则冲突 |
| 用业务 `msg` 做错误分支 | 文案可变化、不可枚举；必须以 HTTP 状态与 `code` 为准 |
| 为权限端点复用全局 401 跳登录 | 权限端点的 401 是“无权限/未登录”的静默信号，跳登录会打断冷启动并造成循环 |

---

## 4. 后果（Consequences）

### 4.1 正面

- 新接口有统一准入基线：路径、参数、信封、错误、分页、SSE 均可逐项验收。
- 不承担全量网络层迁移风险，M1 可在不改存量行为的前提下落地。
- `HttpListData` 从“定义了但没人用”变为新列表端点的唯一模型，避免平行实现。
- 错误处理和权限静默失败的边界被明确记录，减少页面重复实现。

### 4.2 负面 / 风险

| 风险 | 评估与处置 |
| --- | --- |
| 存量接口风格继续不一致 | 明确“存量不动”；需要改造时单独开票，按本 ADR 的兼容流程执行 |
| 服务端分页基准不是 1-based | 契约要求 1-based；若后端返回 0-based，按契约不符失败并记录日志，不做 ±1 猜测 |
| `pageSize` 超限行为不统一 | 客户端不传超过 100；服务端可截断或拒绝，响应必须回显实际 `pageSize` |
| SSE 结束语义重复 | 客户端对 `onComplete` 与 `type=done` 做幂等收尾，避免重复落库 |
| 权限静默失败被误认为“功能坏了” | UI 保持 fail-closed 并提示无权限；日志记录失败原因，不向用户暴露内部细节 |
| 明文口令仅测试服使用 | Release 强制 netcore + SM2；构建配置与文档双重约束，禁止带入正式包 |

### 4.3 对现有代码的影响

本 ADR 本身**不修改代码**。它约束：

- 新增 API 类、列表端点、SSE 使用；
- 错误码/响应信封/分页字段的任何变更；
- 网络相关文档与测试的同步。

---

## 5. 落地与验证

1. `docs/architecture/http-api.md` 与本 ADR 同批提交，并在 `docs/README.md` 建立索引。
2. 新端点必须登记到 `http-api.md` §8，并提供解析/错误映射测试。
3. 权限端点变更时，必须验证：
   - 401/403/429/5xx 静默失败；
   - 字段缺失按失败处理；
   - 30 分钟退避与一次启动一拉不被破坏。
4. 分页端点必须验证：首页替换、后续追加、`isLastPage` 停止、空列表空态、重复页码不并发。
5. 常规门禁：`gradlew.bat testDebugUnitTest`；涉及 UI 时补 `lintDebug`。

---

## 6. 关联文档

- [HTTP 请求契约](../http-api.md)
- [数据模型与缓存策略](../data-model.md)
- [UI 设计规范](../ui-spec.md)
- [防重放攻击客户端指南](../../security/anti-replay-client-guide.md)
- [ADR-0002：移除宋版伤寒章节截取](0002-shanghan-filter-removal.md)
