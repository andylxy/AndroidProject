# Repository Guidelines

## 项目结构与模块组织
本仓库是多模块 Android 项目，根目录 `settings.gradle` 引入 `app` 与 `library/` 下的公共模块：

- `app/`：应用主模块，包含业务代码、资源、Manifest、混淆配置和应用级 Gradle 配置。
- `library/base`：基类、通用工具和基础能力封装。
- `library/widget`：可复用自定义 View 与 UI 组件。
- `library/ui-dialog`：通用 Dialog 和 Popup 组件。
- `library/umeng`：友盟分享与登录集成（无推送模块）。
- `library/xbus`：轻量事件总线模块。
- `library/log`：日志工具模块（`EasyLog`）。
- `library/crypto`：AES、RSA、SM2 等加密工具（当前 app 未直接依赖，属库存模块）。
- `library/native-core`：NDK 核心能力（SM2/SM4、请求签名、密钥保护、反调试）。
- `library/sse-client`：Server-Sent Events 客户端实现。
- `library/text-utils`：文本搜索与高亮工具模块（`SearchMatcher`、`TextHighlighter`）。
- `library/text-renderer`：Tips 模块文本渲染引擎（`TipsTextRenderer`、`ClickLink`），依赖 `text-utils`。
- `library/tips-widget`：Tips 弹窗框架模块（`TipsLittleWindow`、`WindowConfig`、`LocalLinkMovementMethod`、`TipsArrowView`），依赖 `library:log`。
- `picture/`、`jks/`、`localMaven/`：项目图片资料、签名材料和本地 Maven 产物。

各模块遵循标准 Android 目录：`src/main/java`、`src/main/res`、`src/test`，需要仪器测试时使用 `src/androidTest`。

## 构建、测试与开发命令
在仓库根目录使用 Gradle Wrapper：

- `gradlew.bat clean`：清理根目录 `build/` 输出。
- `gradlew.bat assembleDebug`：构建 Debug APK，默认使用测试服配置。
- `gradlew.bat assemblePreview`：构建预发布变体。
- `gradlew.bat assembleRelease`：构建开启混淆与资源压缩的 Release APK。
- `gradlew.bat testDebugUnitTest`：运行 Debug 变体的本地 JVM 单元测试。
- `gradlew.bat lintDebug`：按项目 lint 配置运行 Android Lint。

可通过 `-P ServerType="test|pre|product"` 覆盖服务器环境选择。

## 编码风格与命名约定
项目主要使用 Java Android 代码，Android Gradle Plugin 版本为 7.0.4，compile SDK 为 34，并兼容 Java 8。统一使用 4 空格缩进。Java 包路径应匹配模块职责，类名按角色命名，例如 `HomeActivity`、`OrderAdapter`、`SecurityUtils`。

注释应简短且信息密度高，用于解释意图、不变量、边界条件或不明显的行为；避免只复述代码本身。

## 测试指南
本地 JVM 测试放在 `src/test/java`，Android 仪器测试放在 `src/androidTest/java`。测试类按被测对象命名，例如 `CryptoUtilsTest`。修改解析、加密、SSE、持久化或核心业务逻辑时，应补充或更新测试。提交前至少运行 `gradlew.bat testDebugUnitTest`。

## 提交与 Pull Request 规范
近期提交采用 Conventional Commit 风格，例如 `refactor(base): ...`、`docs(project): ...`。提交信息使用 `type(scope): summary`，常见类型包括 `feat`、`fix`、`refactor`、`docs`、`test`、`chore`。

Pull Request 应包含简短说明、影响模块、已执行的构建/测试命令、关联 Issue；涉及 UI 变更时附截图或录屏。

## 安全与配置提示
不要向源码新增密钥或凭据。`jks/` 中的签名文件、Gradle 配置中的凭据以及 `local.properties` 都应视为敏感信息。避免记录 token、私钥或用户数据，尤其注意 Release 构建中 `LOG_ENABLE` 默认关闭。

## 项目路线与技术方向

### 项目定位
本应用是《伤寒杂病论·桂本》的中医经典阅读与检索应用（包名 `run.yigou.gxzy`），核心能力为书籍/章节阅读、全局与书内搜索、方药/药物/名词解释及 AI 对话。`microfeed` 指认证与安全适配链路，不是信息流功能。

### 技术基线（当前周期不迁移）
- **UI**：Java + Android View。主壳 `HomeActivity` 使用自定义底部导航 + `ViewPager`；阅读容器 `TipsFragmentActivity` 使用 ViewPager2 动态装配 Fragment；列表统一 `WrapRecyclerView` + `AppAdapter` + `SmartRefreshLayout`；优先复用 `library/base`、`widget`、`ui-dialog`、`tips-widget`、`text-renderer`、`text-utils`。
- **数据**：本地优先。GreenDAO 3.3（`myzhongyi.db`，运行库版本 `DatabaseVersionManager.CURRENT_VERSION = 2`，19 个实体）+ `GlobalDataHolder` + MMKV + 文件缓存；读取顺序为内存 → 本地 → 网络；数据库操作必须走 `DbService` 串行执行器，主线程禁止直接访问数据库。
- **网络**：EasyHttp 13.5 + OkHttp 5.3 + 手写 `IRequestApi`；统一信封 `HttpData{code,msg,data}`；双前缀 `RequestServer`（`/api/AppBookRequest/`）与 `VersionRequestServer`（`/api/`）；AI 对话是唯一 SSE 场景。
- **权限**：搜索与名词解释权限 fail-closed；失败 30 分钟退避、一次启动只拉一次；MMKV 仅作持久化/诊断，不作为放行依据。
- **组件**：UI/数据/网络/安全/日志/分享的具体版本、接入入口与官方地址见 [docs/architecture/components.md](docs/architecture/components.md)。同一能力只保留一个库；新增、升级、移除依赖必须同批更新该清单。
- **构建**：AGP 7.0.4、compileSdk 34、Java 8；通过 `-PServerType=test|pre|product` 切换环境。

### 文档入口（变更前必读）
- UI 规范：[docs/architecture/ui-spec.md](docs/architecture/ui-spec.md)（ADR-0003）
- 数据模型与缓存：[docs/architecture/data-model.md](docs/architecture/data-model.md)（ADR-0004）
- HTTP 契约：[docs/architecture/http-api.md](docs/architecture/http-api.md)（ADR-0005）
- 组件与依赖：[docs/architecture/components.md](docs/architecture/components.md)
- 架构技能：[.qoder/skills/project-architecture/SKILL.md](.qoder/skills/project-architecture/SKILL.md)
- 文档索引：[docs/README.md](docs/README.md)

### 路线图
- **M1（当前）**：名词解释链路作为首个按 UI 规范落地的端到端样板；补齐登录/退出权限清理（重置 `sFetchOk`/`sState`、清除两个权限 MMKV 缓存与拉取时钟、退出后立即隐藏权限入口），并覆盖登录→退出→换号测试；新增/改动页面按 `ui-spec.md` 验收，新增列表端点按 `HttpListData` 的 1-based 分页契约实现。
- **M2**：实现“更新频率到期后的网络全量刷新”，明确失败回滚与整表替换；UI 规范覆盖发现→阅读→搜索主链路并收敛存量页面；补列表落库与分页；评估内存态与数据库不一致的边界。
- **M3**：与服务端协同内容版本号/增量下发；评估 WAL、索引与启动 IO；治理样式配置缓存 TTL 与可观测性；继续收敛存量页面。

### 非目标（本周期）
- 不迁移 Kotlin / Compose / ViewModel / Room / Retrofit / OpenAPI。
- 不做高保真视觉重设计、平板/折叠屏专属布局或完整暗色主题。
- 不引入内容版本化、离线写回、第二套网络层或平行缓存体系。
- 不为存量端点或页面做与当前需求无关的顺手重构。

### 变更门禁
- **UI**：对照 `ui-spec.md` 检查四态、组件复用、资源引用与权限闸门；涉及界面时附截图或录屏。
- **数据**：对照 `data-model.md` 说明写入时机、读取时机、失效/清除边界与失败行为；缓存类必须显式声明 `serialVersionUID`。
- **接口**：对照 `http-api.md` 登记端点、分页、错误映射与安全头；破坏性变更必须新增版本化路径、后端协同并更新 ADR。
- **依赖**：新增、升级、移除组件时对照 `components.md` 更新版本、入口与已知缺口；同一能力不得引入第二套库，并检查 minSdk 23、`arm64-v8a` 与 16 KB 页对齐约束。
- **安全**：不新增硬编码凭证；`SecurityConfig` 的默认凭证回退与内置默认 AccessKey 属已知缺口（见 `http-api.md` §4.2），清理需单独立项。
- **文档**：代码、配置或行为变更必须同批更新上述文档与 `docs/README.md`；文档未同步视为变更未完成。

---

## 工作流协议（RIPER-5）

### 背景

为避免在未获授权时做出破坏性更改，必须遵循下述协议。

语言设置：除非用户另有指示，所有常规交互响应都使用中文；模式声明（例如 `[MODE: RESEARCH]`）和特定格式化输出（如代码块、清单）保持英文，以确保格式一致。

### 核心原则

- 先分析，再规划，再实施；未获授权不得实施。
- 先确认事实与归属，再决定修改位置与方式。
- 先收敛用户边界，再讨论是否需要扩范围。
- 分析深度必须与问题重要性相匹配，并始终与原始需求保持清晰联系。
- 安全、范围控制和授权限制高于便利性、简洁性与响应速度偏好。
- 各模式分析或执行完成后，必须给出明确的下一步操作提示；涉及模式切换时，优先直接给出可执行的提示语，例如"如确认方案，输入'进入执行模式'"。

### 模式1：研究

`[MODE: RESEARCH]`

目的：信息收集和深入理解。

允许：阅读文件、提出澄清问题、理解代码结构、分析系统架构与约束、输出基于现有事实的判断与风险观察。

禁止：建议、规划、实施、任何具体实施方案或可直接落地的改动路径。

要求：只寻求理解已存在的内容；若上下文不足，必须明确标记未知项和待确认信息，不得自行补全需求。

输出要求：只输出观察与问题，使用 markdown 分点列出；结尾给出下一步提示。

### 模式2：创新

`[MODE: INNOVATE]`

目的：探索候选方案与权衡。

允许：讨论多种解决路径、比较优缺点、探索架构替代方案。

禁止：具体规划、实施细节、任何代码编写、承诺唯一方案。

要求：基于研究结果整理候选方案、适用条件和取舍依据；本模式下不得进行代码更改。

### 模式3：计划

`[MODE: PLAN]`

目的：创建详尽、可执行的技术规范。

允许：带有精确文件路径的详细计划、精确的函数名称与签名、具体的更改规范、与改动范围匹配的验证策略。

禁止：实施、示例代码、跳过或缩略规范。

必须包含：文件路径与组件关系、需要修改的函数/类/签名、数据结构更改、错误处理策略、依赖管理说明、验证方法。

输出要求：结尾必须给出顺序化、原子化的"实施清单"（编号清单，每个原子操作单独成项）；必须单列"实施边界"，明确改动范围和排除范围；多步骤、跨文件任务应使用内置任务清单工具跟踪进度。

### 模式4：执行

`[MODE: EXECUTE]`

目的：准确实施已批准计划。

允许：只实施计划中明确列出的内容、按清单顺序执行并标记完成状态、实施后更新任务进度。

禁止：偏离计划、计划外改进、创造性添加、跳过或缩略实现。

进入要求：只有收到明确的"进入执行模式"命令后才能进入；当用户直接要求修改/修复/实现/创建且范围清楚时，可视为执行授权；若边界不清楚，必须先回到 `PLAN` 或 `RESEARCH` 澄清。

代码质量标准：

- 提供足够说明变更的最小必要上下文，并保持可审阅性。
- 代码和函数必须具备清晰、准确且与实现同步的注释说明。
- 修复/补丁/兼容性处理必须补充注释，说明修补原因、解决的问题和适用边界。
- 关键位置必须添加可观测运行过程的日志；若项目已存在统一日志入口，必须优先使用，不得扩散到底层实现。

偏差处理：如发现需要偏离计划，必须立即回到 `PLAN` 模式。

### 模式5：审查

`[MODE: REVIEW]`

目的：验证实施与计划是否一致。

必须执行：逐项核对需求和清单、验证所有清单项是否正确完成、检查错误/缺陷/安全影响、指出计划本身的遗漏或风险。

偏差格式：`检测到偏差：[偏差的确切描述]`

结论格式：`实施与计划完全匹配` 或 `实施偏离计划`

### 模式切换规则

- 只有明确的模式切换信号才能切换模式（`进入研究模式`、`进入创新模式`、`进入计划模式`、`进入执行模式`、`进入审查模式`）。
- 无明确切换信号时，保持当前模式。
- `EXECUTE` 发现偏差或验证失败时，可安全回退到 `PLAN`。

### 任务分级规则

- 简单问答、事实查询、只读分析，可直接回应，不必机械进入全流程。
- 多方案探索、架构取舍，应进入 `INNOVATE`。
- 涉及文件修改、跨文件影响的任务，必须先进入 `PLAN`；实施前必须获得执行授权。

### 查证与澄清规则

- 遇到不确定的函数、包或扩展点时，不得猜测，必须先补齐信息。
- 必须先判断目标属于仓库内实现、第三方依赖还是框架能力，再决定修改位置。
- 对第三方依赖，必须先查找官方仓库/官方文档等一手资料。
- 仅当官方资料均无法提供所需信息时，才允许将反编译作为最后方案。
- 当需求边界、影响范围或执行授权不明确时，必须先澄清。

### 用户边界与方案收敛规则

- 必须在用户限定的边界内规划和实施，不得自动扩大修改范围。
- 可以提出更优雅的替代方案，但必须先说明差异和额外影响；未经确认不得自动替换。
- 若修复根因必须扩范围，必须先回到 `PLAN` 说明并等待确认。

### 专项规则按需加载

- 涉及新增/修改功能、页面、数据模型、HTTP 接口或架构取舍时，加载 [project-architecture](.qoder/skills/project-architecture/SKILL.md)。
- 涉及代码、配置、依赖、注释、重构或行为修复时，加载 [.qoder/instructions/code-governance.instructions.md](.qoder/instructions/code-governance.instructions.md)。
- 涉及 README、说明文档、示例、路径或配置说明变更时，加载 [.qoder/instructions/docs-sync.instructions.md](.qoder/instructions/docs-sync.instructions.md)。
- 涉及新建、移动、重命名文件或目录时，加载 [.qoder/instructions/file-placement.instructions.md](.qoder/instructions/file-placement.instructions.md)。
- 涉及 `AGENTS.md`、`.qoder/` 目录或指令文件本身时，加载 [.qoder/instructions/instructions-customization.instructions.md](.qoder/instructions/instructions-customization.instructions.md)。

### 文档同步要求

- 新增、修改、删除代码/配置/依赖/接口/目录结构或行为时，必须同步检查并更新受影响文档。
- 缺陷修复/兼容性修补也必须同步更新问题背景、行为变化、排查方式等文档说明。
- 文档内容必须与当前代码、配置、目录结构和实际行为保持匹配。

### 文件落位要求

- 新增共享基础设施能力文件，必须优先放入该项目或模块既有共享目录。
- 不得创建职责重复的平行入口或模糊命名文件。

### 禁止行为

- 不得使用未经验证的依赖项。
- 不得留下不完整功能、代码占位符或过时方案。
- 不得包含未经验证的实现依据（非官方仓库、博客示例、问答帖子）。
- 不得修改不相关代码。
- 不得在需求边界或执行授权不明确时以"合理推断"为由直接实施。
- 不得提交缺少明确注释的关键代码或修复类代码。
- 不得在关键代码路径省略必要日志，或新增与项目统一日志方案不一致的并行实现。
- 不得保留与实现不一致的文档、注释或说明。
