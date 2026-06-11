# 方案 B：gxzy 包结构分层标准化计划

> 创建日期：2026-06-11
> 状态：已批准待执行

---

## 1. 架构目标

```
run/yigou/gxzy/
├── aop/                    # AOP 切面（不变）
├── app/                    # Application 基类 + 全局初始化和配置（精简）
├── base/                   # 【新建】通用基类层
│   ├── action/             # UI 行为代理（原 action/）
│   ├── args/               # 模块参数配置（原 common/ 中的参数类）
│   └── constant/           # 常量与枚举（原 common/ 中的静态常量）
├── data/                   # 【新建】统一数据层
│   ├── model/              # 共享领域模型（原 model/）
│   ├── local/              # GreenDAO 数据库层（原 greendao/）
│   │   ├── entity/         #   数据库实体
│   │   ├── gen/            #   DAO 生成代码
│   │   ├── service/        #   数据服务层
│   │   └── helper/         #   数据库工具（原 greendao/util/）
│   └── remote/             # 远程 API 层（原 http/ 的数据部分）
│       ├── api/            #   API 接口定义
│       └── model/          #   API 请求/响应模型
├── event/                  # 【重命名】eventbus/ → event/
├── manager/                # 全局管理器（GlobalDataHolder 移出）
├── network/                # 【重命名】http/ → network/（纯网络基础设施）
│   ├── security/           # 安全/加密
│   ├── glide/              # 图片加载
│   └── server/             # 请求服务基础设施
├── ui/                     # UI 层（已部分重构）
├── utils/                  # 工具类（不变）
├── widget/                 # 通用自定义控件（不变）
└── wxapi/                  # 微信 SDK（不变）
```

---

## 2. 实施清单

### Batch A（低风险，优先执行）

#### Phase 1：预处理

| # | 操作 | 详情 | 涉及文件 |
|---|------|------|---------|
| 1.1 | 删除 `ui/tips/TipsWindowNetFragment.java` | ✅ 已确认：与 `ui/feature/reader/TipsWindowNetFragment.java` 内容完全相同，仅包名不同；`HomeFragment.java` 已引用 reader 版本，tips 版本无引用 | 删除 1 文件 |
| 1.2 | 移动 greendao/ 下说明文档 | 将 4 个 Markdown 文档移入 `docs/database/` | 移动 4 文件 |
| 1.3 | 创建 docs/database/ 目录 | 承接 GreenDAO 文档 | 新建 1 目录 |

#### Phase 2：base/ 层创建 + action/ 迁移 + common/ 解散

| # | 操作 | 涉及文件 | 依赖 |
|---|------|---------|------|
| 2.1 | 新建 `base/action/`，移动 3 文件 | `StatusAction.java`, `TitleBarAction.java`, `ToastAction.java` | 更新所有 import `action.*` → `base.action.*` |
| 2.2 | 新建 `base/constant/`，移动 2 文件 | `AppConst.java`, `LoginType.java` | 更新所有 import `common.AppConst\|LoginType` → `base.constant.*` |
| 2.3 | 新建 `base/args/`，移动 3 文件 | `BookArgs.java`, `FragmentSetting.java`, `ManagerSetting.java` | 更新所有 import |
| 2.4 | 删除空目录 `common/` | — | — |
| 2.5 | 删除空目录 `action/` | — | — |

#### Phase 3：eventbus/ → event/ + 事件后缀统一

| # | 操作 | 涉及文件 | 依赖 |
|---|------|---------|------|
| 3.1 | 重命名 `eventbus/` → `event/` | 目录重命名 | 更新 5 个引用 eventbus 的 import |
| 3.2 | 重命名 `LoginEventNotification` → `LoginEvent` | `LoginEventNotification.java` | 更新所有引用方 |
| 3.3 | 重命名 `ShowUpdateNotificationEvent` → `ShowUpdateEvent` | `ShowUpdateNotificationEvent.java` | 更新所有引用方 |

**不动**：`ChapterContentNotificationEvent`、`ChatMessageBeanEvent`、`TipsFragmentSettingEventNotification` 后缀已为 Event。

#### Phase 4：http/ → network/ + data/remote/ 拆分

| # | 操作 | 涉及文件 | 依赖 |
|---|------|---------|------|
| 4.1 | 新建 `network/security/`，迁移 3 文件 | `InterceptorHelper.java`, `RequestHelper.java`, `SecurityConfig.java` | 更新所有 import `http.security.*` → `network.security.*` |
| 4.2 | 新建 `network/glide/`，迁移 3 文件 | `GlideConfig.java`, `OkHttpFetcher.java`, `OkHttpLoader.java` | 更新所有 import `http.glide.*` → `network.glide.*` |
| 4.3 | 新建 `network/server/`，迁移 2 文件 | `RequestHandler.java`, `RequestServer.java` | 更新所有 import `http.server.*` → `network.server.*` |
| 4.4 | 新建 `data/remote/api/`，迁移 22 文件 | 全部 API 接口 | 更新所有 import `http.api.*` → `data.remote.api.*` |
| 4.5 | 新建 `data/remote/model/`，迁移 14 文件 | 全部网络模型 | 更新所有 import `http.model.*` → `data.remote.model.*` |
| 4.6 | 验证：删除空目录 `http/`，编译 | — | — |

#### Batch A 验证

| # | 操作 |
|---|------|
| VA.1 | 执行 `gradlew.bat assembleDebug` 确认 Batch A 编译通过 |

---

### Batch B（依赖 Batch A 编译通过后执行）

#### Phase 5：greendao/ → data/local/

| # | 操作 | 涉及文件 | 依赖 |
|---|------|---------|------|
| 5.1 | 新建 `data/local/entity/`，迁移 18 文件 | `About.java`, `Book.java`, `Chapter.java` 等全部实体 | 更新所有 import `greendao.entity.*` → `data.local.entity.*` |
| 5.2 | 新建 `data/local/gen/`，迁移 23 文件 | 全部 DAO 生成文件 | 更新 import（DaoMaster/DaoSession 被多处引用） |
| 5.3 | 新建 `data/local/service/`，迁移 20 文件 | 全部数据服务 | 更新所有 import |
| 5.4 | 新建 `data/local/helper/`，迁移 9 文件 | `AutoMigrationHelper.java` 等 | 更新所有 import |
| 5.5 | 迁移 `GreenDaoManager.java` → `data/local/Manager.java` | 1 文件 + 重命名 | 更新所有引用 |
| 5.6 | 删除空目录 `greendao/` | — | — |

#### Phase 6：model/ → data/model/

| # | 操作 | 涉及文件 |
|---|------|---------|
| 6.1 | 新建 `data/model/`，迁移 7 文件 | `DataItem.java`, `Fang.java`, `HH2SectionData.java`, `MingCiContent.java`, `Yao.java`, `YaoAlia.java`, `YaoUse.java` |
| 6.2 | 更新所有 import `model.*` → `data.model.*` | — |
| 6.3 | 删除空目录 `model/` | — |

#### Phase 7：manager/ 清理

| # | 操作 | 涉及文件 |
|---|------|---------|
| 7.1 | 迁移 `GlobalDataHolder.java` → `base/` | `GlobalDataHolder.java` 非 Manager 模式 |
| 7.2 | 更新所有 import `manager.GlobalDataHolder` → `base.GlobalDataHolder` | — |

#### Phase 8：Tips 重复清理

| # | 操作 | 涉及文件 |
|---|------|---------|
| 8.1 | 删除 `ui/tips/TipsWindowNetFragment.java` | Phase 1.1 已确认，重复文件 |
| 8.2 | 确认 `ui/tips/TipsDialog.java` 保留原位 | 通用弹窗组件 |

#### Phase 9：死代码清理

| # | 操作 | 涉及文件 | 依据 |
|---|------|---------|------|
| 9.1 | 删除 `adapter/PopupDataAdapter.java` | `PopupDataAdapter.java` | ✅ 已确认不被任何文件引用（仅自身出现） |
| 9.2 | 保留 `adapter/refactor/` 不变 | 31 文件完整子系统 | ✅ 活跃使用（RefactoredExpandableAdapter + RefactoredSearchAdapter） |
| 9.3 | 保留 `adapter/PopupHeaderViewHolder.java` | 1 文件 | ✅ 被 RefactoredPopupAdapter 使用 |

#### Phase 10：全量编译验证

| # | 操作 | 命令 |
|---|------|------|
| 10.1 | Debug 构建验证 | `gradlew.bat assembleDebug` |
| 10.2 | 单元测试 | `gradlew.bat testDebugUnitTest` |

#### Phase 11：文档同步

| # | 操作 | 涉及文件 |
|---|------|---------|
| 11.1 | 更新 AGENTS.md 中的目录结构描述 | `AGENTS.md` |
| 11.2 | 更新 README.md 中的路径说明 | `README.md` |
| 11.3 | 在 docs/ 记录本次重构说明 | `docs/package-restructure-plan.md`（本文档） |
| 11.4 | 将完成状态同步到 session 记忆 | `/memories/session/migration-progress.md` |

---

## 3. 实施边界

| 项目 | 内容 |
|------|------|
| **改动范围** | `app/src/main/java/run/yigou/gxzy/` 包重组 |
| **排除范围** | 不修改业务逻辑、API 签名、数据库表结构；`ui/widget/`、`utils/`、`wxapi/` 不动；`library/` 模块不动 |
| **是否允许新增文件** | 仅允许 `package-info.java`（可选），不新增业务文件 |
| **新增目录** | `base/action/`、`base/constant/`、`base/args/`、`data/`、`data/model/`、`data/local/` 及子目录、`data/remote/` 及子目录、`network/` 及子目录、`event/` |
| **删除目录** | `action/`、`eventbus/`、`http/`、`greendao/`、`model/`、`common/` |
| **删除文件** | `ui/tips/TipsWindowNetFragment.java`、`adapter/PopupDataAdapter.java` |
| **验证方法** | `gradlew.bat assembleDebug` 编译通过 + `gradlew.bat testDebugUnitTest` 测试通过 |
| **回滚策略** | 每个 Batch 完成后编译验证；若 Batch A 失败，`git checkout HEAD -- app/src/main/java/` 回退 |

---

## 4. 风险与注意事项

1. **Package 重命名影响面**：每个 Java 文件的 package 声明需更新，import 语句约 100-200 处。建议每个批处理后用全局搜索确认无残留旧 import
2. **GreenDAO 生成代码**：`greendao/gen/` 中的 DAO 文件为自动生成，修改 package 后需重新生成 DAO。建议在 Phase 5 后执行一次 Clean Build
3. **`GlobalDataHolder` 迁移**：仅 7 行 import 受影响，风险极低
4. **回滚保险**：每完成一个 Phase 提交一个 git commit，便于细粒度回退
5. **`refactor/` 目录保留**：31 文件 7 子包的活跃子系统，不动
6. **`PopupDataAdapter` 删除确认**：已确认无引用，但仍建议删除前做一次 grep 双重确认
