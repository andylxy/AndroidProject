# TipsTextRenderConfig 配置中心使用指南

## 📖 目录

- [概述](#概述)
- [架构设计](#架构设计)
- [执行流程](#执行流程)
- [使用方式](#使用方式)
- [核心 API](#核心-api)
- [降级策略](#降级策略)
- [配置格式](#配置格式)
- [内容类型系统](#内容类型系统)
- [典型场景](#典型场景)
- [常见问题](#常见问题)
- [相关重构](#相关重构)

---

## 概述

`TipsTextRenderConfig` 是 Tips 文本渲染模块的**配置中心**，负责：

- ✅ 集中管理样式配置（颜色、字体、链接类型）
- ✅ 支持服务端动态下发配置
- ✅ 支持运行时修改配置
- ✅ 自动加载与降级机制
- ✅ 线程安全与版本控制

### 核心优势

| **特性** | **说明** |
|------|------|
| **零侵入** | 调用方无需关心配置加载，首次渲染自动触发 |
| **依赖倒置** | library 模块定义接口，app 模块实现，保持模块独立性 |
| **类型安全** | 使用 ContentTypes.@IntDef 编译期校验，消除 magic number |
| **统一类型系统** | ContentTypes 统一管理内容类型（药物/方剂/名词/扩展） |
| **自动降级** | 配置加载失败后降级到默认配置，保证功能可用性 |
| **线程安全** | 使用 ReadWriteLock 保护配置读写 |

---

## 架构设计

### 模块职责

```
┌─────────────────────────────────────────────────────┐
│  library 模块（定义接口）                             │
│  ┌───────────────────────────────────────────────┐  │
│  │ IStyleConfigProvider.java                     │  │
│  │ - loadConfig()                                │  │
│  │ - isConfigLoaded()                            │  │
│  │ - getConfigVersion()                          │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │ TipsTextRenderConfig.java                     │  │
│  │ - setProvider()                               │  │
│  │ - checkConfigBeforeUse()                      │  │
│  │ - getStyleConfig(marker)                      │  │
│  │ - applyServerConfig()                         │  │
│  │ - applyServerConfigGeneric()                  │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │ TipsTextRenderer.java                         │  │
│  │ - renderText() → 调用 getStyleConfig()        │  │
│  │ - applyStyle() → 创建 ProxyClickableSpan      │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │ ProxyClickableSpan.java (独立类)              │  │
│  │ - 代理 Android ClickableSpan                  │  │
│  │ - 转发点击事件到 ClickLink                    │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │ ItemNumberRenderer.java (独立类)              │  │
│  │ - 渲染项编号 "1、" "2、"                       │  │
│  │ - 可选启用/禁用                                │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  app 模块（实现接口）                                 │
│  ┌───────────────────────────────────────────────┐  │
│  │ AppStyleConfigProvider.java                   │  │
│  │ - 实现 IStyleConfigProvider                   │  │
│  │ - 请求 StyleConfigApi                         │  │
│  │ - 调用 applyServerConfig()                    │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │ StyleConfigApi.java                           │  │
│  │ - 继承 library/StyleConfigApiBean             │  │
│  │ - 定义服务端接口地址                          │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 依赖关系

```
library/text-renderer
  └─ IStyleConfigProvider（接口）
  └─ TipsTextRenderConfig（配置中心）
  └─ TipsTextRenderer（渲染引擎）

app
  └─ AppStyleConfigProvider（实现 IStyleConfigProvider）
  └─ StyleConfigApi（HTTP 请求，继承 library 的 Bean）
```

---

## 执行流程

### 完整调用链

```
应用启动
  ↓
AppApplication.onCreate()
  ├─ TipsTextRenderConfig 单例初始化
  ├─ loadDefaultConfigs() → configMap 预加载 13 个默认配置
  └─ setProvider(new AppStyleConfigProvider()) → 注册配置提供者

用户首次打开 Tips 页面
  ↓
TipsTextRenderer.renderText("$r{药材名称}", clickLink)
  ↓
TipsTextRenderConfig.getStyleConfig("r")
  ├─ checkConfigBeforeUse()
  │   ├─ provider.isConfigLoaded() = false（未加载）
  │   ├─ provider.loadConfig() → 触发异步请求
  │   └─ 返回 false（配置未就绪）
  └─ 返回触底配置 #CCCCCC（浅灰色）✨

[1-2 秒后]
  ↓
服务端配置返回
  ↓
AppStyleConfigProvider.onSucceed()
  ├─ TipsTextRenderConfig.applyServerConfig(styles)
  ├─ isLoaded = true
  └─ version++

用户第二次打开 Tips 页面
  ↓
TipsTextRenderer.renderText("$r{药材名称}", clickLink)
  ↓
TipsTextRenderConfig.getStyleConfig("r")
  ├─ checkConfigBeforeUse()
  │   └─ provider.isConfigLoaded() = true → 返回 true
  └─ 返回服务端配置的颜色（可能是 #E53935）✨
```

### 时序图

```
AppApplication      TipsTextRenderConfig      AppStyleConfigProvider      StyleConfigApi
      │                      │                          │                      │
      │──setProvider()──────>│                          │                      │
      │                      │                          │                      │
      │                      │<── 首次渲染调用 ──────────│                      │
      │                      │   getStyleConfig("r")    │                      │
      │                      │                          │                      │
      │                      │──checkConfigBeforeUse()─>│                      │
      │                      │                          │                      │
      │                      │<──loadConfig()───────────│                      │
      │                      │                          │──post()─────────────>│
      │                      │                          │                      │
      │                      │──返回默认配置 ────────────│                      │
      │                      │                          │                      │
      │                      │                          │<──onSucceed()────────│
      │                      │                          │                      │
      │                      │<──applyServerConfig()────│                      │
      │                      │   isLoaded = true        │                      │
      │                      │                          │                      │
      │                      │<── 第二次渲染调用 ────────│                      │
      │                      │   getStyleConfig("r")    │                      │
      │                      │                          │                      │
      │                      │──checkConfigBeforeUse()─>│                      │
      │                      │   isConfigLoaded()=true  │                      │
      │                      │                          │                      │
      │                      │──返回服务端配置 ──────────│                      │
```

---

## 使用方式

### 方式 1：自动加载（推荐）

**无需额外代码**，已自动集成：

```java
// 1. Application 中已注册（自动完成）
TipsTextRenderConfig.getInstance().setProvider(new AppStyleConfigProvider());

// 2. 渲染时自动触发加载（无需手动调用）
TipsTextRenderer.renderText("$r{文本}", clickLink);
// ↑ 首次渲染时自动请求服务端配置
// ↑ 配置返回前使用默认配置
// ↑ 配置返回后使用服务端配置
```

**适用场景**：绝大多数情况，无需额外代码。

---

### 方式 2：手动加载（可选）

如需在应用启动时立即加载（不等首次渲染）：

```java
// Application.onCreate() 中
TipsTextRenderConfig config = TipsTextRenderConfig.getInstance();
config.setProvider(new AppStyleConfigProvider());

// 立即触发加载（可选）
config.getProvider().loadConfig();
```

**适用场景**：希望在应用启动时就完成配置加载，避免首次渲染使用默认配置。

---

### 方式 3：直接应用配置（高级用法）

如需从本地缓存或其他来源应用配置：

#### 方式 3.1：使用 library 模块的 Bean

```java
List<StyleConfigApiBean.StyleItem> items = new ArrayList<>();

StyleConfigApiBean.StyleItem item = new StyleConfigApiBean.StyleItem();
item.setMarker("r");
item.setColor("#E53935");
item.setSmallFont(true);
item.setLinkType(0);
items.add(item);

TipsTextRenderConfig.getInstance().applyServerConfig(items);
```

#### 方式 3.2：使用通用接口（支持任意类型）

```java
TipsTextRenderConfig.getInstance().applyServerConfigGeneric(
    customItems,
    new TipsTextRenderConfig.ItemExtractor<CustomItem>() {
        @Override public String getMarker(CustomItem item) { return item.marker; }
        @Override public String getColor(CustomItem item) { return item.color; }
        @Override public boolean isSmallFont(CustomItem item) { return item.smallFont; }
        @Override public int getLinkType(CustomItem item) { return item.linkType; }
    }
);
```

**适用场景**：
- 从本地缓存加载配置
- 从其他数据源（如本地文件）加载配置
- 测试场景（构造测试数据）

---

## 核心 API

### TipsTextRenderConfig

| 方法 | 用途 | 调用时机 | 返回值 |
|------|------|----------|--------|
| `getInstance()` | 获取配置中心单例 | 任何需要访问配置的地方 | TipsTextRenderConfig |
| `setProvider()` | 注册配置提供者 | Application.onCreate() | void |
| `checkConfigBeforeUse()` | 检查并自动加载 | 首次渲染时自动调用 | boolean |
| `getStyleConfig(marker)` | 获取样式配置 | TipsTextRenderer 内部调用 | StyleConfig |
| `applyServerConfig()` | 应用服务端配置 | AppStyleConfigProvider 回调中调用 | void |
| `applyServerConfigGeneric()` | 通用适配接口 | 支持自定义数据类型 | void |
| `updateStyleConfig()` | 运行时更新配置 | 任何需要动态修改时 | void |

### IStyleConfigProvider

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `loadConfig()` | boolean | 触发异步加载，返回 true=已触发，false=失败 |
| `isConfigLoaded()` | boolean | 配置是否已加载完成 |
| `getConfigVersion()` | int | 配置版本号（0=未加载） |

### StyleConfig

| 字段 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| `color` | int | 颜色值（解析后的 int） | Color.RED |
| `isSmallFont` | boolean | 是否使用小字体 | true |
| `linkType` | int | 链接类型（0=无链接，1=药材，2=方剂，3=名词） | 0 |
| `topMargin` | int | 上边距（dp） | 2 |
| `bottomMargin` | int | 下边距（dp） | 2 |
| `horizontalMargin` | int | 水平边距（dp） | 4 |
| `verticalMargin` | int | 垂直边距（dp） | 0 |

---

## 降级策略

### 配置加载状态

| 场景 | 行为 | 日志 | 使用配置 |
|------|------|------|----------|
| **未设置 Provider** | 跳过检查 | `⚠️ 未设置 IStyleConfigProvider，使用默认配置` | 默认配置 |
| **配置未加载** | 触发异步加载 | `配置未加载，触发自动加载...` | 触底配置 #CCCCCC |
| **正在加载中** | 跳过重复触发 | `配置正在加载中，跳过重复触发` | 触底配置 #CCCCCC |
| **加载失败** | 保持默认配置，下次重试 | 静默处理 | 默认配置 |
| **配置已加载** | 使用服务端配置 | 无日志 | 服务端配置 |

### 三种配置的区别

| 配置类型 | 触发时机 | 颜色 | 字体 | 用途 |
|----------|----------|------|------|------|
| **触底配置** | 配置未加载/正在加载 | #CCCCCC（浅灰色） | 小字体 | 临时占位，视觉提示配置未就绪 |
| **默认配置** | 配置加载失败 | 预定义 13 个颜色 | 服务端定义 | 降级方案，保证功能可用 |
| **服务端配置** | 配置加载成功 | 服务端下发 | 服务端定义 | 最终目标配置 |

### 默认配置（13 个）

当服务端配置加载失败时，使用以下默认配置：

| marker | 颜色 | 小字体 | 链接类型 | 说明 |
|--------|------|--------|----------|------|
| `r` | #FF0000（红色） | true | 0 | 药材（红色） |
| `n` | #0000FF（蓝色） | false | 0 | 名词 |
| `f` | #0000FF（蓝色） | false | 2 | 方剂 |
| `a` | #808080（灰色） | true | 0 | 别名 |
| `m` | #FF0000（红色） | false | 0 | 标记 |
| `g` | #0080FF（浅蓝） | false | 3 | 功效 |
| `u` | #0000FF（蓝色） | false | 1 | 用途 |
| `v` | #0000FF（蓝色） | false | 0 | 版本 |
| `w` | #1CB55C（绿色） | true | 0 | 味道 |
| `q` | #3DC878（浅绿） | false | 0 | 气味 |
| `h` | #000000（黑色） | false | 0 | 寒热 |
| `x` | #EA8E3B（橙色） | false | 0 | 性味 |
| `y` | #9A764F（棕色） | false | 0 | 药性 |

---

## 配置格式

### 服务端返回格式

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "styles": [
      { 
        "marker": "r", 
        "color": "#E53935", 
        "isSmallFont": true, 
        "linkType": 0 
      },
      { 
        "marker": "u", 
        "color": "#1E88E5", 
        "isSmallFont": false, 
        "linkType": 1 
      },
      { 
        "marker": "f", 
        "color": "#43A047", 
        "isSmallFont": true, 
        "linkType": 2 
      }
    ]
  }
}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| `marker` | String | ✅ | 标记符（单个字母） | `"r"`, `"u"`, `"f"` |
| `color` | String | ✅ | 颜色值（HEX 格式） | `"#FF0000"`, `"#E53935"` |
| `isSmallFont` | boolean | ✅ | 是否使用小字体 | `true`, `false` |
| `linkType` | int | ✅ | 链接类型 | `0`=无链接, `1`=药材, `2`=方剂, `3`=名词 |

### 链接类型扩展

当前支持 3 种链接类型，未来可扩展（详见 [内容类型系统](#内容类型系统)）：

| linkType | 说明 | 点击行为 | ContentTypes 常量 |
|----------|------|----------|-------------------|
| 0 | 无链接 | 无交互 | - |
| 1 | 药材链接 | 打开药材详情 | `ContentTypes.YAO` |
| 2 | 方剂链接 | 打开方剂详情 | `ContentTypes.FANG` |
| 3 | 名词链接 | 打开名词详情 | `ContentTypes.MING_CI` |
| 4 | 汉制单位 | 仅列表展示（无点击） | `ContentTypes.HAN_ZHI_UNIT` |
| 5 | 视频（预留） | 由调用方实现 | `ContentTypes.VIDEO` |
| 6 | 音频（预留） | 由调用方实现 | `ContentTypes.AUDIO` |
| 7 | 图片（预留） | 由调用方实现 | `ContentTypes.IMAGE` |

---

## 内容类型系统

### ContentTypes 常量类

**位置**：`app/src/main/java/run/yigou/gxzy/ui/reader/constant/ContentTypes.java`

**职责**：统一定义内容类型常量，用于配置中心、点击路由、列表展示等场景。

### 设计原则

| 原则 | 说明 |
|------|------|
| **统一标准** | 配置中心 `linkType`、点击处理、列表展示共用一套常量 |
| **类型安全** | 使用 `@IntDef` 编译期校验，消除 magic number |
| **易于扩展** | 新增类型只需在 `ContentTypes` 加常量 |
| **性能优化** | 使用 `@IntDef` 而非枚举（Android 最佳实践） |

### 常量定义

```java
public final class ContentTypes {
    // 内容类型常量
    public static final int YAO = 1;           // 药物
    public static final int FANG = 2;          // 方剂
    public static final int MING_CI = 3;       // 名词
    public static final int HAN_ZHI_UNIT = 4;  // 汉制单位
    
    // 预留扩展类型
    public static final int VIDEO = 5;         // 视频
    public static final int AUDIO = 6;         // 音频
    public static final int IMAGE = 7;         // 图片
    
    // 类型注解（编译期校验）
    @IntDef({YAO, FANG, MING_CI, HAN_ZHI_UNIT, VIDEO, AUDIO, IMAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentType {}
}
```

### 使用示例

#### 1. 方法参数类型校验

```java
public void handleClick(@ContentTypes.ContentType int contentType) {
    switch (contentType) {
        case ContentTypes.YAO:
            // 处理药物点击
            break;
        case ContentTypes.FANG:
            // 处理方剂点击
            break;
    }
}
```

#### 2. 字段类型声明

```java
@ContentTypes.ContentType
private int contentType;
```

#### 3. Fragment 创建

```java
TipsFangYaoFragment fragment = TipsFangYaoFragment.newInstance(
    ContentTypes.FANG,  // 方剂列表
    bookId
);
```

#### 4. Adapter 中使用

```java
public class TipsFangYaoAdapter extends AppAdapter<String> {
    @ContentTypes.ContentType
    private final int contentType;
    
    public TipsFangYaoAdapter(Context context, @ContentTypes.ContentType int contentType) {
        this.contentType = contentType;
    }
    
    @Override
    public void onBindView(int position) {
        switch (contentType) {
            case ContentTypes.FANG:
                // 渲染方剂列表：$f{...}
                break;
            case ContentTypes.YAO:
                // 渲染药物列表：$u{...}
                break;
            case ContentTypes.HAN_ZHI_UNIT:
                // 渲染汉制单位：纯文本
                break;
        }
    }
}
```

### 类型映射关系

| ContentTypes 常量 | 值 | 配置中心 linkType | 文本标记 | 业务含义 |
|-------------------|---|-------------------|---------|----------|
| `YAO` | 1 | 1 | `$u{}` | 药物 |
| `FANG` | 2 | 2 | `$f{}` | 方剂 |
| `MING_CI` | 3 | 3 | `$m{}` | 名词 |
| `HAN_ZHI_UNIT` | 4 | - | - | 汉制单位 |
| `VIDEO` | 5 | 预留 | - | 视频 |
| `AUDIO` | 6 | 预留 | - | 音频 |
| `IMAGE` | 7 | 预留 | - | 图片 |

### 历史背景

**优化前的问题**：

1. **两套类型系统并存**
   - `TipsClickHandler.ClickType`（内部枚举）
   - `TipsFangYaoFragment.typeFangYao`（int 类型）
   - 二者值相同但语义相反（`typeFangYao=1` 表示方剂，`ClickType.YAO=1` 表示药物）

2. **Magic Number 问题**
   - 代码中大量使用 `1/2/3` 等数字，可读性差
   - 编译期无类型校验，容易传错

3. **接口臃肿**
   - `ClickLink` 接口定义了 4 个方法（3 个抽象 + 1 个默认）
   - 实际只使用 `onClickLink()`，其他 3 个方法已废弃

**优化后**：

- ✅ 统一使用 `ContentTypes` 常量类
- ✅ `ClickLink` 接口精简为 1 个方法
- ✅ 删除 `ClickType` 枚举和 `handleClickByLinkType()` 方法
- ✅ 使用 `@IntDef` 提供编译期类型校验

---

## 典型场景

### 场景 1：正常流程

```
启动 → configMap 预加载 13 个默认配置
     ↓
首次渲染 → checkConfigBeforeUse() 返回 false → 返回触底配置 #CCCCCC
         ↓
   触发异步加载
         ↓
   [1-2 秒后配置返回]
         ↓
   applyServerConfig() → configMap 更新为服务端配置
         ↓
下次渲染 → checkConfigBeforeUse() 返回 true → 返回服务端配置
```

**日志输出**：
```
已加载默认配置，共 13 项
已设置配置提供者: AppStyleConfigProvider
配置未加载，触发自动加载...
✅ 已触发配置加载，等待异步完成
开始应用服务端配置，共 13 项
✅ 服务端配置已应用（通用接口）
```

---

### 场景 2：离线场景

```
启动 → configMap 预加载 13 个默认配置
     ↓
首次渲染 → checkConfigBeforeUse() 返回 false → 返回触底配置 #CCCCCC
         ↓
   触发异步加载
         ↓
   [网络请求失败]
         ↓
   configMap 保持 13 个默认配置
         ↓
下次渲染 → checkConfigBeforeUse() 返回 true → 返回默认配置（如 #FF0000）
```

**日志输出**：
```
已设置配置提供者: AppStyleConfigProvider
配置未加载，触发自动加载...
✅ 已触发配置加载，等待异步完成
（无成功日志，配置未更新）
```

---

### 场景 3：预加载优化

```
启动 → setProvider()
       ↓
       立即调用 loadConfig()（可选）
       ↓
       [配置提前加载完成]
       ↓
       首次渲染 → 直接使用服务端配置（无降级）
```

**代码示例**：
```java
@Override
public void onCreate() {
    super.onCreate();
    
    TipsTextRenderConfig config = TipsTextRenderConfig.getInstance();
    config.setProvider(new AppStyleConfigProvider());
    
    // 预加载配置（可选）
    config.getProvider().loadConfig();
}
```

**日志输出**：
```
已设置配置提供者: AppStyleConfigProvider
配置未加载，触发自动加载...
✅ 已触发配置加载，等待异步完成
✅ 服务端配置已应用（通用接口）
（首次渲染时 isConfigLoaded()=true，直接使用服务端配置）
```

---

### 场景 4：运行时动态更新

```
运行中 → 调用 updateStyleConfig() → 立即生效
```

**代码示例**：
```java
// 从本地缓存加载配置
List<StyleConfigApiBean.StyleItem> cachedConfigs = loadFromCache();
TipsTextRenderConfig.getInstance().applyServerConfig(cachedConfigs);

// 下次渲染立即生效
TipsTextRenderer.renderText("$r{文本}", clickLink);
```

---

## 常见问题

### Q1：为什么首次渲染使用默认配置？

**A**：配置加载是异步操作，首次渲染时配置可能尚未返回。为保证功能可用，自动降级使用默认配置。配置返回后，下次渲染自动使用服务端配置。

**优化方案**：如需避免降级，可在 Application 中预加载配置（见场景 3）。

---

### Q2：如何确认配置是否加载成功？

**A**：通过日志观察：

```java
// 成功日志
✅ 服务端配置已应用（通用接口）

// 失败日志（静默处理，无日志）
```

也可通过代码检查：
```java
boolean loaded = TipsTextRenderConfig.getInstance()
    .getProvider()
    .isConfigLoaded();
```

---

### Q3：如何调试配置加载问题？

**A**：开启详细日志：

```java
// TipsTextRenderConfig 已内置日志
// 查看 Logcat 过滤条件：TipsTextRenderConfig
```

关键日志：
- `已设置配置提供者: xxx`
- `配置未加载，触发自动加载...`
- `✅ 已触发配置加载，等待异步完成`
- `✅ 服务端配置已应用（通用接口）`

---

### Q4：如何自定义链接类型？

**A**：使用 `ContentTypes` 常量，在 `TipsClickHandler` 中扩展链接类型处理：

```java
public class TipsClickHandler {
    public static final ClickLink DEFAULT_CLICK_LINK = new ClickLink() {
        @Override
        public void onClickLink(int linkType, TextView textView, ClickableSpan span) {
            handleClick(textView, span, linkType);
        }
    };
    
    static void handleClick(TextView textView, ClickableSpan clickableSpan, 
                           @ContentTypes.ContentType int contentType) {
        switch (contentType) {
            case ContentTypes.YAO:
                // 药材链接
                openHerbDetail(keyword);
                break;
            case ContentTypes.FANG:
                // 方剂链接
                openPrescriptionDetail(keyword);
                break;
            case ContentTypes.MING_CI:
                // 名词链接
                openTermDetail(keyword);
                break;
            case ContentTypes.VIDEO:
                // 自定义类型（视频）
                openVideoDetail(keyword);
                break;
            default:
                EasyLog.print("❌ 未知的contentType: " + contentType);
        }
    }
}
```

---

### Q5：配置中心是否线程安全？

**A**：是的，使用 `ReadWriteLock` 保护配置读写：

```java
private final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock();

public StyleConfig getStyleConfig(String marker) {
    configLock.readLock().lock();
    try {
        return configMap.get(marker);
    } finally {
        configLock.readLock().unlock();
    }
}

public void updateStyleConfig(List<StyleItem> items) {
    configLock.writeLock().lock();
    try {
        // 更新配置
    } finally {
        configLock.writeLock().unlock();
    }
}
```

---

### Q6：如何在测试环境中使用？

**A**：构造测试配置：

```java
@Test
public void testStyleConfig() {
    // 构造测试数据
    List<StyleConfigApiBean.StyleItem> items = new ArrayList<>();
    
    StyleConfigApiBean.StyleItem item = new StyleConfigApiBean.StyleItem();
    item.setMarker("test");
    item.setColor("#FF0000");
    item.setSmallFont(true);
    item.setLinkType(0);
    items.add(item);
    
    // 应用配置
    TipsTextRenderConfig.getInstance().applyServerConfig(items);
    
    // 验证
    StyleConfig config = TipsTextRenderConfig.getInstance().getStyleConfig("test");
    assertEquals(Color.RED, config.getColor());
    assertTrue(config.isSmallFont());
}
```

---

## 相关文件

| 文件路径 | 说明 |
|---------|------|
| `library/text-renderer/.../IStyleConfigProvider.java` | 配置提供者接口 |
| `library/text-renderer/.../TipsTextRenderConfig.java` | 配置中心实现 |
| `library/text-renderer/.../TipsTextRenderer.java` | 渲染引擎（核心） |
| `library/text-renderer/.../ProxyClickableSpan.java` | 点击代理（独立类） |
| `library/text-renderer/.../ItemNumberRenderer.java` | 项编号渲染器（独立类） |
| `library/text-renderer/.../RenderOptions.java` | 渲染选项（Builder 模式） |
| `library/text-utils/.../TextHighlighter.java` | 文本高亮工具（统一） |
| `app/.../config/AppStyleConfigProvider.java` | app 模块配置提供者 |
| `app/.../data/remote/api/StyleConfigApi.java` | HTTP 请求定义 |
| `app/.../app/AppApplication.java` | Application 注册 |

---

## 更新日志

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-06-23 | v1.0 | 初始版本，实现接口驱动配置中心架构 |
| 2026-06-23 | v1.1 | 优化 StyleConfigApiBean 继承复用，移除 ItemExtractor 适配 |
| 2026-06-22 | v1.2 | 职责分离重构：拆分 ProxyClickableSpan、ItemNumberRenderer，统一高亮逻辑，增加 RenderOptions |

---

## 相关重构

### TipsTextRenderer 职责分离（2026-06-22）

本次重构对 `TipsTextRenderer` 进行了职责分离和架构优化：

**主要变更**：
1. ✅ **ProxyClickableSpan 独立化**：从内部类提取为独立类，增加日志和 `getLinkType()` 方法
2. ✅ **ItemNumberRenderer 独立化**：项编号渲染逻辑分离，支持可选启用/禁用
3. ✅ **高亮逻辑统一**：删除 `TipsTextRenderer.highlightMatches()`，统一使用 `TextHighlighter`
4. ✅ **RenderOptions 增强**：引入 Builder 模式，支持可选渲染配置

**架构改进**：
```
重构前：TipsTextRenderer (160 行)
├── renderText()          ✅ 核心职责
├── applyStyle()          ✅ 核心职责
├── ProxyClickableSpan    ⚠️ 内部类（难测试）
├── renderItemNumber()    ⚠️ 业务规则硬编码
├── isNumeric()           ⚠️ 工具方法
└── highlightMatches()    ⚠️ 与 TextHighlighter 重复

重构后：
TipsTextRenderer (118 行) ↓ 26%
├── renderText()          ✅ 核心职责
└── applyStyle()          ✅ 核心职责

ProxyClickableSpan (76 行) ✅ 独立类
ItemNumberRenderer (85 行) ✅ 新增
RenderOptions (61 行)     ✅ 新增
TextHighlighter (136 行)  ✅ 统一
```

**向后兼容**：完全兼容，旧代码无需修改

**详细文档**：[TipsTextRenderer 重构总结](../optimization/TipsTextRenderer重构总结.md)

---

**文档维护**：如有问题或建议，请联系开发团队。
