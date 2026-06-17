# 注释修复与补充计划

> 2026-06-12

## 实施策略

全量注释补充工程量大（312 Java 文件）。采用四批策略，每批完成后编译验证。

---

### Batch 1：修复 `?????` 损坏注释（约 30-40 文件）

修复因 ASCII 编码写入损坏的中文注释。需逐文件分析代码语义，重写注释。

**涉及目录及推断依据**：

| 目录 | 文件 | 推断策略 |
|------|------|----------|
| `event/` | 5 文件 | 事件类，从类名、字段名、方法名推断 |
| `base/action/` | StatusAction, TitleBarAction, ToastAction | UI 行为接口，从方法签名推断 |
| `base/constant/` | AppConst, LoginType | 常量类 |
| `base/args/` | BookArgs, FragmentSetting, ManagerSetting | 参数配置类 |
| `data/remote/api/` | ~22 文件 | API 接口，从 URL 路径和返回类型推断 |
| `data/remote/model/` | ~14 文件 | API 模型，从字段名推断 |
| `network/glide/` | GlideConfig | Glide 配置 |
| `network/security/` | RequestHelper | 请求辅助 |

**方法**：逐文件读取 → 分析语义 → 使用 `replace_string_in_file` 替换损坏行

---

### Batch 2：补充无 javadoc 的类（约 50-80 文件）

扫描出没有类级 javadoc 的文件，补充 `/** ... */` 说明。

**筛选条件**：
```
文件内容匹配 "public class" 或 "public interface"
但文件内容不匹配 "/**\n * "
```

**方法**：批量扫描 → 逐个补充

---

### Batch 3：补充公开方法注释

对 Batch 1+2 已处理的文件的 public 方法补充 javadoc。

**规则**：
- getter/setter 方法跳过（`get.*`, `set.*`, `is.*`）
- 重写方法（`@Override`）跳过
- 构造方法视情况补充
- 业务方法需要补充 `@param` 和 `@return`

---

### Batch 4：清理验证

| 步骤 | 内容 |
|------|------|
| 检查无 `?????` 残留 | `grep '????'` 确认 |
| 编译验证 | `gradlew.bat assembleDebug` |
| 更新记忆文件 | 记录完成状态 |

---

### 实施边界

| 项目 | 内容 |
|------|------|
| **改动范围** | `app/src/main/java/run/yigou/gxzy/` 下所有 Java 文件 |
| **修改内容** | 仅注释（javadoc + 行内注释），不修改代码逻辑 |
| **风格** | 中文注释，简洁扼要，说明意图而非复述代码 |
| **验证** | 编译通过即可 |
| **排除** | `greendao/gen/` 中自动生成的 DAO 文件（已移入 `data/local/gen/`）注释不改 |