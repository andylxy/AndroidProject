# 签名认证与防重放攻击机制详解

## 目录

- [1. 概述](#1-概述)
- [2. 签名认证原理](#2-签名认证原理)
- [3. 防重放攻击机制](#3-防重放攻击机制)
- [4. 客户端实现](#4-客户端实现)
- [5. 服务器端实现](#5-服务器端实现)
- [6. 缓存策略](#6-缓存策略)
- [7. 安全建议](#7-安全建议)

## 1. 概述

开放接口采用签名认证机制来确保接口调用的安全性。通过时间戳和 Nonce 的组合使用，有效防止重放攻击，保障系统安全。

## 2. 签名认证原理

### 2.1 认证特性说明

```csharp
[FixedToken]
```

将 `FixedTokenAttribute` 应用于需要加签的接口或控制器，即可同时启用 JWT 二次校验、AccessKey 签名验证与防重放逻辑。如需全局启用，可在 `Program.cs` 中通过 `options.Filters.Add<FixedTokenAttribute>();` 注册全局过滤器。

### 2.2 签名算法流程

1. 客户端使用 AccessKeyID 和 AccessKeySecret 生成签名
2. 请求中携带签名信息
3. 服务端验证签名有效性
4. 验证通过后执行业务逻辑

## 3. 防重放攻击机制

### 3.1 时间戳验证

- 客户端生成当前时间戳
- 服务器验证时间戳是否在有效时间窗口内（通常为±5分钟）
- 超出时间窗口的请求直接拒绝

### 3.2 Nonce 唯一性验证

- 客户端生成随机数（Nonce）
- 服务器检查该 Nonce 是否已使用
- 已使用的 Nonce 在一定时间内不允许重复使用

### 3.3 综合防护

通过时间戳和 Nonce 的结合使用，确保请求的唯一性和时效性，有效防止重放攻击。

## 4. 客户端实现

### 4.1 生成时间戳和 Nonce

```javascript
// 生成时间戳（毫秒级）
const timestamp = Date.now().toString();

// 生成随机数（Nonce）
const nonce = Math.random().toString(36).substring(2, 15) + 
              Math.random().toString(36).substring(2, 15);
```

### 4.2 构造签名字符串

```javascript
const stringToSign = `${method}
${host}
${path}
${queryString}
${body}
${timestamp}
${nonce}`;
```

### 4.3 生成签名

```javascript
const signature = hmacSha256(stringToSign, secretKey);
```

### 4.4 发送请求

```javascript
const response = await fetch(url, {
    method: method,
    headers: {
        'Authorization': `Signature ${signature}`,
        'X-AccessKeyId': accessKeyId,
        'X-Timestamp': timestamp,
        'X-Nonce': nonce
    }
});
```

## 5. 服务器端实现

开源项目现已在 `SimpleEasy.WebApi` 中集成 `FixedTokenAttribute` 过滤器，统一完成 JWT 校验、AccessKey 签名校验和防重放逻辑。以下内容基于当前代码。

### 5.1 FixedTokenAttribute 关键实现

```csharp
public class FixedTokenAttribute : Attribute, IAsyncAuthorizationFilter, IAllowAnonymous
{
    public async Task OnAuthorizationAsync(AuthorizationFilterContext context)
    {
        // 先校验当前用户的 JWT 与启用状态
        // ...existing code...

        await CheckSignature(context, userAccount);
    }

    private async Task<AccessKeyCredential> CheckSignature(AuthorizationFilterContext context, string? currentUserAccount)
    {
        var request = context.HttpContext.Request;
        var headers = request.Headers;
        var options = context.HttpContext.RequestServices
            .GetRequiredService<IOptions<SignatureOptions>>().Value;

        // 读取签名头：Signature/X-Signature、X-AccessKeyId、X-Timestamp、X-Nonce
        // ...header validation code...

        var provider = context.HttpContext.RequestServices.GetRequiredService<IAccessKeyProvider>();
        var credential = await provider.GetAsync(accessKeyId);

        // 校验AccessKey状态、绑定用户
        // ...state checks...

        // 时间戳窗口校验（默认 ±5 分钟，可在 appsettings 的 SignatureOptions 中配置）
        // ...timestamp check...

        // 通过 ISimpleCacheService 记录 signature_nonce:{accessKeyId}:{nonce}
        var cache = context.HttpContext.RequestServices.GetRequiredService<ISimpleCacheService>();
        if (cache.ContainsKey(nonceKey)) throw Oops.ThrowUnauthorizedException("重复的请求");
        cache.Set(nonceKey, 1, nonceExpire);

        // 规范化签名串：Method\nHost\nPath\nQuery\nBody\nTimestamp\nNonce
        var stringToSign = $"{method}\n{host}\n{path}\n{query}\n{body}\n{timestamp}\n{nonce}";
        var computedSignature = ComputeHmacSha256(stringToSign, credential.AccessKeySecret);

        if (!string.Equals(signature, computedSignature, StringComparison.Ordinal))
            throw Oops.ThrowUnauthorizedException("签名验证失败");

        context.HttpContext.Items[nameof(AccessKeyCredential)] = credential;
        return credential;
    }
}
```

要点：

- 头部统一采用 `Signature`/`X-Signature`、`X-AccessKeyId`、`X-Timestamp`（毫秒）、`X-Nonce`；依旧兼容历史 `accessKey` 头。
- 签名串与客户端一致，空 Query/Body 以空字符串参与签名，确保幂等。
- Nonce 缓存优先走 `ISimpleCacheService`（Redis），缓存键格式 `signature_nonce:{accessKeyId}:{nonce}`。
- 校验通过后把 `AccessKeyCredential` 放入 `HttpContext.Items`，业务层可读取调用者信息。

### 5.2 DbAccessKeyProvider 与缓存

```csharp
public class DbAccessKeyProvider : IAccessKeyProvider
{
    public async Task<AccessKeyCredential?> GetAsync(string accessKeyId)
    {
        var cacheKey = $"signature:accesskey:{accessKeyId}";
        if (_cache.TryGetValue(cacheKey, out AccessKeyCacheEntry cached))
            return cached.HasCredential ? cached.Credential : null;

        var entity = await _sqlSugar.Queryable<SysAccessKey>()
            .FirstAsync(x => x.AccessKeyId == accessKeyId);

        if (entity == null)
        {
            _cache.Set(cacheKey, AccessKeyCacheEntry.CreateMiss(), TimeSpan.FromMinutes(1));
            return null;
        }

        var credential = new AccessKeyCredential { /* 映射字段 */ };
        _cache.Set(cacheKey, AccessKeyCacheEntry.CreateHit(credential), TimeSpan.FromMinutes(2));
        return credential;
    }
}
```

AccessKey 会在缓存中保存命中/未命中结果，避免高并发下的数据库穿透。同时实体包含 `BindingUserAccount` 与 `ExpiredTime`，由 `FixedTokenAttribute` 做二次校验。

### 5.3 SignatureOptions 配置

在 `appsettings.json` 添加：

```json
"SignatureOptions": {
  "AllowedDriftMinutes": 5,
  "NonceCachePrefix": "signature_nonce",
  "NonceTtlMinutes": 10
}
```

`Program.cs` 中：

```csharp
builder.Services.Configure<SignatureOptions>(builder.Configuration.GetSection("SignatureOptions"));
builder.Services.AddScoped<IAccessKeyProvider, DbAccessKeyProvider>();
```

### 5.4 服务端接入步骤

1. **注册依赖**：在 `Program.cs` 中配置 `SignatureOptions`、`DbAccessKeyProvider`、`ISimpleCacheService`（Redis/Tenants）。
2. **创建 AccessKey**：通过后台管理或脚本写入 `SysAccessKey`，设置绑定账号及有效期。
3. **启用过滤器**：
   - 某个接口需要防护时，加 `[FixedToken]`（或 `[FixedTokenAttribute]`）。
   - 如需全局生效，可在 `services.AddControllers(o => o.Filters.Add<FixedTokenAttribute>());` 注册。
4. **验证结果**：使用本文第 11 节的 MVP 检查项，确认签名正确、时间戳/Nonce 校验、绑定账号等逻辑全部生效。

完成以上步骤后，服务器即可与客户端示例保持一致，实现签名认证 + 防重放的统一口径。

### 5.5 SignatureOnlyAttribute（第三方接口）

若接口只需要 AccessKey 签名校验、无需平台 JWT，可直接使用 `[SignatureOnly]` 特性：

```csharp
[Route("api/open/[controller]")]
[ApiController]
[SignatureOnly]
public class WebhookController : ControllerBase
{
    [HttpPost("notify")]
    public IActionResult Notify([FromServices] IHttpContextAccessor accessor)
    {
        var credential = accessor.HttpContext?.Items[nameof(AccessKeyCredential)] as AccessKeyCredential;
        // credential 可用于审计、限流或业务绑定
        return Ok(new { message = "ok", accessKey = credential?.AccessKeyId });
    }
}
```

- `[SignatureOnly]` 与 `[FixedToken]` 共用 `SignatureValidationHelper`，同样校验时间戳、Nonce、防重放。
- 因为跳过 JWT，第三方系统只需携带签名头部即可；如需额外鉴别调用方，可根据 `credential.OwnerType/OwnerId` 做权限判断。
- 客户端签名示例（cURL/Android/Vue 等）完全通用，若无需 JWT，可移除 `Authorization: Bearer ...` 头。

## 6. 缓存策略

### 6.1 缓存键设计

```text
signature_nonce:{accessKeyId}:{nonce}
```

### 6.2 过期时间设置

- 与时间戳验证窗口保持一致（通常为5分钟）
- 确保过期的 Nonce 记录能及时清理

### 6.3 缓存系统选择

- 推荐使用 Redis 等高性能缓存系统
- 合理设置缓存容量，避免内存溢出

## 7. 安全建议

### 7.1 时间窗口设置

- 合理设置时间窗口大小（推荐5分钟）
- 时间窗口过短影响用户体验，过长增加安全风险

### 7.2 Nonce 生成

- 使用足够随机的 Nonce 生成算法
- 确保 Nonce 的唯一性和不可预测性

### 7.3 密钥管理

- 定期轮换 AccessKeySecret
- 妥善保管密钥信息，避免泄露

### 7.4 监控和日志

- 记录认证失败的请求
- 监控异常行为，及时发现潜在攻击
- 定期分析安全日志

通过以上机制，SimpleEasy.WebApi 能够有效防止重放攻击，保障开放接口的安全性。

## 8. AccessKey 设计与管理

- **数据模型**：`SysAccessKey` 表包含 `AccessKeyId`、`AccessKeySecret`、`OwnerType`、`OwnerId`、`BindingUserId`、`BindingUserAccount`、`IsEnabled`、`ExpiredTime` 等字段，可追溯凭证归属。
- **绑定策略**：当 `OwnerType = User` 时，AccessKey 与平台用户绑定，`BindingUserAccount` 必须与登录账号一致；当 `OwnerType = App/Service` 时，AccessKey 表示第三方系统身份，可搭配业务自定义的 `OwnerId`。
- **生命周期**：创建 AccessKey → 配置访问范围 → 发放给调用方 → 定期轮换/禁用 → 记录 `LastUsedTime` 便于审计。

## 9. 服务端校验流程（FixedTokenAttribute）

1. 解析 JWT，确认平台用户有效。
2. 读取 Header：`Signature/X-Signature`、`X-AccessKeyId`、`X-Timestamp`、`X-Nonce`。
3. 通过 `IAccessKeyProvider` 查库获取 AccessKey，并校验启用/过期状态及绑定用户一致性。
4. 校验时间戳是否落在 `SignatureOptions.AllowedDriftMinutes` 窗口内。
5. 使用 Redis(优先) 或内存缓存记录 `signature_nonce:{accessKeyId}:{nonce}`，拒绝重复请求。
6. 构造 `Method\nHost\nPath\nQuery\nBody\nTimestamp\nNonce` 字符串，使用 AccessKeySecret 计算 HMAC-SHA256 签名。
7. 签名一致则放行，并将 `AccessKeyCredential` 写入 `HttpContext.Items` 供业务使用。

## 10. 第三方集成示例

### 10.1 AccessKey 申请流程

1. 管理员在后台创建 AccessKey，选择绑定的用户或第三方系统，生成随机的 `AccessKeyId/Secret`。
2. 将密钥安全地发放给集成方，并约定签名算法、时间戳精度及重试策略。
3. 集成方存储 AccessKeySecret（不可硬编码在前端），调用接口时同时携带 JWT（如需用户身份）与签名头。

### 10.2 cURL 示例

```bash
curl -X POST "https://api.example.com/api/open/template/postExample" \
    -H "Authorization: Bearer <jwt-token>" \
    -H "Signature: Signature <base64-sign>" \
    -H "X-AccessKeyId: demo-client" \
    -H "X-Timestamp: 1733300000000" \
    -H "X-Nonce: 9f40d5d3f7e54c4a" \
    -H "Content-Type: application/json" \
    -d '{"id":1,"name":"demo"}'
```

> `Signature` 由客户端按照 *Integration Guide* 中的字符串格式和 AccessKeySecret 计算获得。

### 10.3 故障排查

- `401 accessKey 无效`：确认 AccessKeyId 正确且在后台启用。
- `401 accessKey 与登陆用户不匹配`：检查是否使用了其他用户的 AccessKey。
- `401 请求已过期`：确保客户端时间与服务器同步，并在 5 分钟窗口内发送请求。
- `401 重复的请求`：每次请求必须使用全新的 Nonce。

### 10.4 Android (Kotlin + Retrofit) 示例

```kotlin
class SignatureInterceptor(
    private val accessKeyId: String,
    private val accessKeySecret: String,
    private val jwtToken: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val timestamp = System.currentTimeMillis().toString()

        val bodyString = original.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } ?: ""

        val url = original.url
        val stringToSign = listOf(
            original.method.uppercase(Locale.ROOT),
            url.host + if (url.port != 80 && url.port != 443) ":${url.port}" else "",
            url.encodedPath,
            url.query ?: "",
            bodyString,
            timestamp,
            nonce
        ).joinToString("\n")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(accessKeySecret.toByteArray(), "HmacSHA256"))
        val signature = Base64.encodeToString(mac.doFinal(stringToSign.toByteArray()), Base64.NO_WRAP)

        val newRequest = original.newBuilder()
            .addHeader("Authorization", "Bearer $jwtToken")
            .addHeader("Signature", "Signature $signature")
            .addHeader("X-AccessKeyId", accessKeyId)
            .addHeader("X-Timestamp", timestamp)
            .addHeader("X-Nonce", nonce)
            .build()

        return chain.proceed(newRequest)
    }
}
```

将 `SignatureInterceptor` 加入 Retrofit/OkHttp 客户端即可自动在每个请求上附加签名。

### 10.5 Vue 调用示例（基于 Axios）

```javascript
import axios from 'axios';
import { v4 as uuid } from 'uuid';
import crypto from 'crypto-js';

function buildSignature(config, accessKeySecret) {
  const method = config.method.toUpperCase();
  const url = new URL(config.url, config.baseURL);
  const host = url.host;
  const path = url.pathname;
  const queryString = url.search ? url.search.substring(1) : '';
  const body = config.data ? JSON.stringify(config.data) : '';
  const timestamp = Date.now().toString();
  const nonce = uuid().replace(/-/g, '');

  const stringToSign = `${method}\n${host}\n${path}\n${queryString}\n${body}\n${timestamp}\n${nonce}`;
  const hash = crypto.HmacSHA256(stringToSign, accessKeySecret);
  const signature = crypto.enc.Base64.stringify(hash);

  return { signature, timestamp, nonce };
}

const client = axios.create({ baseURL: '/api' });

client.interceptors.request.use((config) => {
  const accessKeyId = import.meta.env.VITE_ACCESS_KEY_ID;
  const accessKeySecret = import.meta.env.VITE_ACCESS_KEY_SECRET;
  const jwtToken = localStorage.getItem('token');

  const { signature, timestamp, nonce } = buildSignature(config, accessKeySecret);

  config.headers.Authorization = `Bearer ${jwtToken}`;
  config.headers.Signature = `Signature ${signature}`;
  config.headers['X-AccessKeyId'] = accessKeyId;
  config.headers['X-Timestamp'] = timestamp;
  config.headers['X-Nonce'] = nonce;

  return config;
});

export default client;
```

> Vue/Node 端务必将 AccessKeySecret 存放在后端或安全配置中，前端仅通过代理或 API 网关触发签名以避免密钥泄露。

### 10.6 Android (Java + OkHttp) 示例

```java
public final class SignatureInterceptor implements Interceptor {
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String jwtToken;

    public SignatureInterceptor(String accessKeyId, String accessKeySecret, String jwtToken) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.jwtToken = jwtToken;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());

        String bodyString = "";
        RequestBody body = original.body();
        if (body != null) {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            bodyString = buffer.readUtf8();
        }

        HttpUrl url = original.url();
        String host = url.port() == 80 || url.port() == 443
                ? url.host()
                : url.host() + ":" + url.port();
        String stringToSign = String.join("\n",
                original.method().toUpperCase(Locale.ROOT),
                host,
                url.encodedPath(),
                url.query() == null ? "" : url.query(),
                bodyString,
                timestamp,
                nonce);

        String signature = sign(stringToSign, accessKeySecret);

        Request newRequest = original.newBuilder()
                .header("Authorization", "Bearer " + jwtToken)
                .header("Signature", "Signature " + signature)
                .header("X-AccessKeyId", accessKeyId)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .build();

        return chain.proceed(newRequest);
    }

    private static String sign(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(result, Base64.NO_WRAP);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Sign failed", e);
        }
    }
}
```

在 OkHttpClient 构建时 `new OkHttpClient.Builder().addInterceptor(new SignatureInterceptor(...)).build();`，即可复用签名逻辑。

## 11. 验证步骤（MVP Checklist）

1. **签名成功**：使用合法 JWT 与 AccessKey 发送请求，返回 200 或业务码。
2. **签名错误**：修改 `Signature` 任意字符，应返回 `401 签名验证失败`。
3. **时间戳过期**：将 `X-Timestamp` 调整为 10 分钟前，返回 `401 请求已过期`。
4. **重复 Nonce**：在 5 分钟内重复发送同一 Nonce，返回 `401 重复的请求`。
5. **禁用 AccessKey**：在后台禁用 AccessKey，再次调用返回 `401 accessKey 已禁用`。
6. **用户绑定校验**：将 AccessKey 绑定到指定账号，使用其他账号的 JWT 调用返回 `401 accessKey 与登陆用户不匹配`。
7. **缓存一致性**：如部署多实例，切换到 Redis 存储 Nonce 并重复上述用例，确保结果一致。
