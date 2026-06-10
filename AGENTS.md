# Repository Guidelines

## 项目结构与模块组织
本仓库是多模块 Android 项目，根目录 `settings.gradle` 引入 `app` 与 `library/` 下的公共模块：

- `app/`：应用主模块，包含业务代码、资源、Manifest、混淆配置和应用级 Gradle 配置。
- `library/base`：基类、通用工具和基础能力封装。
- `library/widget`：可复用自定义 View 与 UI 组件。
- `library/ui-dialog`：通用 Dialog 和 Popup 组件。
- `library/umeng`：友盟分享、推送等集成。
- `library/xbus`：轻量事件总线模块。
- `library/log`：日志工具模块。
- `library/crypto`：AES、RSA、SM2 等加密与安全工具。
- `library/sse-client`：Server-Sent Events 客户端实现。
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
