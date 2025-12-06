# AccessKey 签名客户端指引

> 面向第三方或合作方，帮助快速对接 AccessKey + 防重放签名机制。本文仅涉及客户端如何构造请求，不包含任何服务端实现细节。

## 1. 接入流程概览

1. 向平台申请 `AccessKeyId/AccessKeySecret`，妥善保管 Secret，不在浏览器等不可信环境中明文保存。
2. 为每次调用生成时间戳 (`X-Timestamp`) 与随机数 (`X-Nonce`)，并据此构造签名原文。
3. 使用 HMAC-SHA256(签名原文, AccessKeySecret) 计算签名，并以 Base64 输出。
4. 在请求头中附带签名与标识信息，向目标 API 发送请求。

## 2. 必填请求头

| Header | 说明 |
| --- | --- |
| `Signature` 或 `X-Signature` | `Signature <base64>`，Base64 为 HMAC-SHA256 结果 |
| `X-AccessKeyId` | 发放给调用方的 AccessKeyId |
| `X-Timestamp` | 13 位毫秒时间戳，必须与服务器时间相差不超过 5 秒（默认值） |
| `X-Nonce` | 本次请求的随机字符串，10 秒内不可重复 |
| `Authorization` *(可选)* | 若接口还需要平台登录态，使用 `Bearer <jwt>` |

## 3. 签名步骤

1. 统一 HTTP 方法为全大写，例如 `POST`、`GET`。
2. `Host` 包含域名及端口（若端口非 80/443）。
3. `Path` 仅包含绝对路径，不含查询参数。
4. 构造五段式原文：

   ```text
   {Method}\n{Host}\n{Path}\n{Timestamp}\n{Nonce}
   ```

5. 使用 UTF-8 编码对原文进行 HMAC-SHA256，密钥为 AccessKeySecret。
6. 将输出转换为 Base64 字符串，前缀 `Signature` 加一个空格后放入 `Signature` 头。

## 4. 时间戳与 Nonce 规则

- `X-Timestamp` 必须是当前 UTC 毫秒时间戳；推荐使用 `Date.now()` 或 `System.currentTimeMillis()`。
- 服务端默认允许 ±5 秒漂移，可在双方协定后放宽；超出窗口会被判定为过期。
- `X-Nonce` 为 8~32 个字符的随机串，建议使用 UUID 或安全随机数。
- 同一 AccessKeyId 在 10 秒 TTL 内重复使用相同 Nonce 会直接被拒绝，因此每次调用都要重新生成。

## 5. 示例

### 5.1 cURL 调用

```bash
nonce=$(uuidgen | tr 'A-Z' 'a-z' | tr -d '-')
timestamp=$(date +%s%3N)
string_to_sign="POST\napi.example.com\n/api/open/template/postExample\n${timestamp}\n${nonce}"
signature=$(printf "%s" "$string_to_sign" | openssl dgst -binary -sha256 -hmac "$ACCESS_KEY_SECRET" | openssl base64 -A)

curl -X POST "https://api.example.com/api/open/template/postExample" \
  -H "Signature: Signature ${signature}" \
  -H "X-AccessKeyId: ${ACCESS_KEY_ID}" \
  -H "X-Timestamp: ${timestamp}" \
  -H "X-Nonce: ${nonce}" \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"demo"}'
```

### 5.2 JavaScript/Axios 助手

```javascript
import crypto from 'crypto-js';
import { v4 as uuid } from 'uuid';

function signRequest(config, accessKeyId, accessKeySecret, jwtToken) {
  const method = config.method.toUpperCase();
  const url = new URL(config.url, config.baseURL);
  const host = url.host;
  const path = url.pathname;
  const timestamp = Date.now().toString();
  const nonce = uuid().replace(/-/g, '');

  const stringToSign = `${method}\n${host}\n${path}\n${timestamp}\n${nonce}`;
  const signature = crypto.enc.Base64.stringify(crypto.HmacSHA256(stringToSign, accessKeySecret));

  config.headers.Signature = `Signature ${signature}`;
  config.headers['X-AccessKeyId'] = accessKeyId;
  config.headers['X-Timestamp'] = timestamp;
  config.headers['X-Nonce'] = nonce;

  if (jwtToken) {
    config.headers.Authorization = `Bearer ${jwtToken}`;
  }

  return config;
}
```

### 5.3 Kotlin + OkHttp 拦截器

```kotlin
class SignatureInterceptor(
    private val accessKeyId: String,
    private val accessKeySecret: String,
    private val jwtToken: String? = null
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val timestamp = System.currentTimeMillis().toString()
        val url = original.url

        val stringToSign = listOf(
            original.method.uppercase(Locale.ROOT),
            url.host + if (url.port != 80 && url.port != 443) ":${url.port}" else "",
            url.encodedPath,
            timestamp,
            nonce
        ).joinToString("\n")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(accessKeySecret.toByteArray(), "HmacSHA256"))
        val signature = Base64.encodeToString(mac.doFinal(stringToSign.toByteArray()), Base64.NO_WRAP)

        val newRequest = original.newBuilder()
            .header("Signature", "Signature $signature")
            .header("X-AccessKeyId", accessKeyId)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .apply { jwtToken?.let { header("Authorization", "Bearer $it") } }
            .build()

        return chain.proceed(newRequest)
    }
}
```

## 6. 常见错误

| 错误信息 | 排查思路 |
| --- | --- |
| `401 accessKey 无效` | 检查 AccessKeyId 是否存在/启用，密钥是否匹配 |
| `401 请求已过期` | 本地时间与服务器差异超过允许窗口；建议启用 NTP，同步后重试 |
| `401 重复的请求` | 10 秒内复用了同一 Nonce，改为每次调用都生成全新随机数 |
| `401 签名验证失败` | 确认签名原文顺序、换行符、Host+端口等完全一致，并确保使用 UTF-8/HMAC-SHA256/Base64 |

## 7. 最佳实践

- **时钟同步**：所有调用方服务器应配置 NTP，确保与标准时间偏差 < 1 秒。
- **Nonce 管理**：每次发起请求前都生成新的随机串，可附带时间戳以方便排查。
- **密钥安全**：AccessKeySecret 只保存在后端，移动端/浏览器通过受控网关代理签名。
- **重试策略**：若因网络失败需要重试，应重新生成时间戳与 Nonce，避免被判定为重放。
- **环境隔离**：不同环境（测试/生产）使用不同的 AccessKey，方便权限收敛与审计。
