# Android 安全工具类使用详细指南

本文档旨在提供关于如何在 Android 项目中使用 SecurityUtils 类进行各种安全操作的详细步骤说明，包括国密 SM2/SM4 算法、MD5 哈希算法、AES 加密算法和 RC4 加密算法。

## 目录

1. [准备工作](#准备工作)
2. [SM2 公钥加密使用指南](#sm2-公钥加密使用指南)
3. [SM4 对称加密使用指南](#sm4-对称加密使用指南)
4. [MD5 哈希算法使用指南](#md5-哈希算法使用指南)
5. [AES 对称加密使用指南](#aes-对称加密使用指南)
6. [RC4 对称加密使用指南](#rc4-对称加密使用指南)
7. [工具类说明](#工具类说明)
8. [常见问题](#常见问题)

## 准备工作

在开始使用 SecurityUtils 加密功能之前，请确保你的项目已经正确配置了 Bouncy Castle 加密库。

### 添加依赖

在你的 `build.gradle` 文件中添加以下依赖项：

```gradle
dependencies {
    implementation 'org.bouncycastle:bcprov-jdk15on:1.70'
    implementation 'com.github.gzu-liyujiang:RSAUtils:2.0.0'
}
```

### 初始化 Bouncy Castle Provider

在应用启动时（例如在 Application 类的 onCreate 方法中）添加 Bouncy Castle Provider：

```java
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Security.addProvider(new BouncyCastleProvider());
    }
}
```

## SM2 公钥加密使用指南

SM2 是一种基于椭圆曲线的公钥加密算法，在 Android 客户端通常只使用公钥进行加密和验签操作。

### 第一步：生成或导入 SM2 密钥对

#### 方式一：使用分离坐标形式初始化公钥

如果你拥有公钥的 x 和 y 坐标，可以使用以下方式初始化：

```java
import run.yigou.gxzy.security.SecurityUtils;

// 假设你已经有了公钥的 x 和 y 坐标（十六进制字符串）
String publicKeyX = "your_public_key_x_coordinate";
String publicKeyY = "your_public_key_y_coordinate";

// 初始化 SM2 公钥
SecurityUtils.initSM2PublicKey(publicKeyX, publicKeyY);
```

#### 方式二：使用完整公钥形式初始化

如果你拥有完整格式的公钥（即 ASN.1 编码的公钥），可以使用以下方式初始化：

```java
import run.yigou.gxzy.security.SecurityUtils;

// 假设你已经有了完整格式的公钥（十六进制字符串）
String fullPublicKey = "your_full_public_key";

// 初始化 SM2 公钥
SecurityUtils.initSM2PublicKeyWithFullFormat(fullPublicKey);
```

### 第二步：使用 SM2 进行数据加密

获取到 SM2Util 实例后，你可以使用它来加密数据：

```java
try {
    String plainText = "需要加密的数据";
    String encryptedData = SecurityUtils.doSm2Encrypt(plainText);
    System.out.println("加密后的数据：" + encryptedData);
} catch (Exception e) {
    e.printStackTrace();
}
```

### 第三步：使用 SM2 进行签名验证

在某些情况下，你可能需要验证服务器返回数据的签名：

```java
try {
    String data = "需要验证签名的数据";
    String signature = "数据的签名值（十六进制字符串）";
    
    boolean isValid = SecurityUtils.doVerifySignature(data, signature);
    if (isValid) {
        System.out.println("签名验证成功");
    } else {
        System.out.println("签名验证失败");
    }
} catch (Exception e) {
    e.printStackTrace();
}
```

## SM4 对称加密使用指南

SM4 是一种分组对称加密算法，支持 ECB 和 CBC 两种工作模式。

### 第一步：选择合适的加密模式

#### ECB 模式（不推荐用于敏感数据）

ECB 模式是最简单的加密模式，相同明文会产生相同密文：

```java
import run.yigou.gxzy.security.SecurityUtils;

// 使用默认密钥进行 ECB 模式加密
String plainText = "需要加密的数据";
String encrypted = SecurityUtils.doSm4Encrypt(plainText);
System.out.println("ECB加密结果：" + encrypted);
```

#### CBC 模式（推荐）

CBC 模式更安全，每次加密即使相同明文也会产生不同的密文：

```java
import run.yigou.gxzy.security.SecurityUtils;

// 使用默认密钥和IV进行 CBC 模式加密
String plainText = "需要加密的数据";
String encrypted = SecurityUtils.doSm4CbcEncrypt(plainText);
System.out.println("CBC加密结果：" + encrypted);
```

### 第二步：自定义密钥和 IV 进行加密

你可以使用自定义的密钥和初始向量（IV）进行加密：

```java
import run.yigou.gxzy.security.SecurityUtils;

// 自定义密钥和 IV（均为16字节）
String key = "your_custom_key_16bytes";
String iv = "your_custom_iv_16bytes";
String plainText = "需要加密的数据";

// 使用自定义密钥和 IV 进行 CBC 模式加密
String encrypted = SecurityUtils.doSm4CbcEncrypt(plainText, key, iv);
System.out.println("自定义密钥CBC加密结果：" + encrypted);
```

### 第三步：解密数据

对于服务端返回的 SM4 加密数据，你可以使用相应的方法进行解密：

```java
import run.yigou.gxzy.security.SecurityUtils;

// 解密 ECB 模式加密的数据
String encryptedEcb = "ECB模式加密的数据";
String decryptedEcb = SecurityUtils.doSm4Decrypt(encryptedEcb);
System.out.println("ECB解密结果：" + decryptedEcb);

// 解密 CBC 模式加密的数据
String encryptedCbc = "CBC模式加密的数据";
String decryptedCbc = SecurityUtils.doSm4CbcDecrypt(encryptedCbc);
System.out.println("CBC解密结果：" + decryptedCbc);
```

## MD5 哈希算法使用指南

MD5 是一种广泛使用的哈希算法，可用于生成数据摘要。

### 计算字符串的 MD5 值

```java
import run.yigou.gxzy.security.SecurityUtils;

String text = "需要计算MD5的数据";
String md5Value = SecurityUtils.calcMd5(text);
System.out.println("MD5值：" + md5Value);
```

## AES 对称加密使用指南

AES 是一种广泛使用的对称加密算法，安全性高。

### 第一步：使用默认密钥进行 AES 加密/解密

```java
import run.yigou.gxzy.security.SecurityUtils;

// 使用默认密钥进行 AES 加密
String plainText = "需要加密的数据";
String encrypted = SecurityUtils.aesEncrypt(plainText);
System.out.println("AES加密结果：" + encrypted);

// 使用默认密钥进行 AES 解密
String decrypted = SecurityUtils.aesDecrypt(encrypted);
System.out.println("AES解密结果：" + decrypted);
```

### 第二步：使用自定义密钥进行 AES 加密/解密

```java
import run.yigou.gxzy.security.SecurityUtils;

// 使用自定义密钥进行 AES 加密
String plainText = "需要加密的数据";
String key = "your_custom_key_16bytes";
String encrypted = SecurityUtils.aesEncrypt(plainText, key);
System.out.println("自定义密钥AES加密结果：" + encrypted);

// 使用自定义密钥进行 AES 解密
String decrypted = SecurityUtils.aesDecrypt(encrypted, key);
System.out.println("自定义密钥AES解密结果：" + decrypted);
```

### 第三步：文件加密/解密

```java
import run.yigou.gxzy.security.SecurityUtils;
import java.io.File;

// 加密文件
File sourceFile = new File("/path/to/source/file.txt");
File encryptedFile = SecurityUtils.aesEncryptFile(sourceFile, "/path/to/output/", "encrypted_file.txt", "your_key");

// 解密文件
File decryptedFile = SecurityUtils.aesDecryptFile(encryptedFile, "/path/to/output/", "decrypted_file.txt", "your_key");
```

## RC4 对称加密使用指南

RC4 是一种流加密算法，速度快，适合实时通信场景。

### 第一步：使用默认密钥进行 RC4 加密/解密

```java
import run.yigou.gxzy.security.SecurityUtils;

// 使用默认密钥进行 RC4 加密
String plainText = "需要加密的数据";
String encrypted = SecurityUtils.rc4Encrypt(plainText);
System.out.println("RC4加密结果：" + encrypted);

// 使用默认密钥进行 RC4 解密
String decrypted = SecurityUtils.rc4Decrypt(encrypted);
System.out.println("RC4解密结果：" + decrypted);
```

### 第二步：使用自定义密钥进行 RC4 加密/解密

```java
import run.yigou.gxzy.security.SecurityUtils;

// 使用自定义密钥进行 RC4 加密
String plainText = "需要加密的数据";
String key = "your_custom_rc4_key";
String encrypted = SecurityUtils.rc4Encrypt(plainText, key);
System.out.println("自定义密钥RC4加密结果：" + encrypted);

// 使用自定义密钥进行 RC4 解密
String decrypted = SecurityUtils.rc4Decrypt(encrypted, key);
System.out.println("自定义密钥RC4解密结果：" + decrypted);
```

## 工具类说明

### SecurityUtils 类

这是主要的安全工具类，提供了所有加密/解密、签名/验签等相关功能：

- `initSM2PublicKey(String publicKeyX, String publicKeyY)` - 使用公钥坐标初始化SM2公钥
- `initSM2PublicKeyWithFullFormat(String fullPublicKey)` - 使用完整公钥初始化SM2公钥
- `doSm2Encrypt(String msgString)` - SM2公钥加密
- `doSm2Decrypt(String encryptedData)` - SM2私钥解密（仅服务端使用）
- `doSignature(String data)` - SM2私钥签名（仅服务端使用）
- `doVerifySignature(String data, String signature)` - SM2公钥验签
- `doSm4Encrypt(String msgString)` - SM4 ECB模式加密（使用默认密钥）
- `doSm4Decrypt(String encryptedData)` - SM4 ECB模式解密（使用默认密钥）
- `doSm4CbcEncrypt(String msgString)` - SM4 CBC模式加密（使用默认密钥和IV）
- `doSm4CbcDecrypt(String encryptedData)` - SM4 CBC模式解密（使用默认密钥和IV）
- `doSm4Encrypt(String msgString, String key)` - SM4 ECB模式加密（使用自定义密钥）
- `doSm4Decrypt(String encryptedData, String key)` - SM4 ECB模式解密（使用自定义密钥）
- `doSm4CbcEncrypt(String msgString, String key, String iv)` - SM4 CBC模式加密（使用自定义密钥和IV）
- `doSm4CbcDecrypt(String encryptedData, String key, String iv)` - SM4 CBC模式解密（使用自定义密钥和IV）
- `calcMd5(String originString)` - 计算字符串的MD5值
- `aesEncrypt(String data)` - AES加密（使用默认密钥）
- `aesDecrypt(String base64Data)` - AES解密（使用默认密钥）
- `aesEncrypt(String data, String secretKey)` - AES加密（使用自定义密钥）
- `aesDecrypt(String base64Data, String secretKey)` - AES解密（使用自定义密钥）
- `aesEncryptFile(File sourceFile, String dir, String toFileName, String secretKey)` - AES文件加密
- `aesDecryptFile(File sourceFile, String dir, String toFileName, String secretKey)` - AES文件解密
- `rc4Encrypt(String data)` - RC4加密（使用默认密钥）
- `rc4Decrypt(String base64Data)` - RC4解密（使用默认密钥）
- `rc4Encrypt(String data, String secretKey)` - RC4加密（使用自定义密钥）
- `rc4Decrypt(String base64Data, String secretKey)` - RC4解密（使用自定义密钥）

## 国密算法技术架构说明

### SM2/SM4 模块架构

本项目采用模块化设计，将国密算法实现分为以下层次：

#### 核心实现层 (`run.yigou.gxzy.Security.Cryptogram.Sm`)
- **SM2CryptoUtil.java**: SM2 椭圆曲线加密核心算法实现
  - 提供曲线参数配置（使用国密标准 SM2 曲线）
  - 实现密钥生成、加密、解密功能
  - 严格遵循 C# 原版实现逻辑
  
- **SM4CryptoUtil.java**: SM4 分组对称加密核心算法实现
  - S盒变换、轮函数、密钥扩展
  - 支持 ECB/CBC 工作模式
  - 提供加密/解密、填充处理
  
- **Sm4Context.java**: SM4 加密上下文类
  - 保存加密/解密状态
  - 管理密钥和模式参数

#### 包装门面层 (`run.yigou.gxzy.Security.Cryptogram`)
- **SM2Util.java**: SM2 高级封装类
  - 管理全局公私钥配置
  - 提供简化的加密/解密接口
  - 自动处理密文格式（04前缀）
  
- **SM4Util.java**: SM4 高级封装类
  - 管理全局密钥和IV配置
  - 支持多种输出格式（Hex/Base64）
  - 提供ECB/CBC模式选择

- **CryptogramUtil.java**: 统一加密门面
  - 提供最高层次的简化接口
  - 统一错误处理和日志记录
  - 适配所有加密场景

#### 统一管理层 (`SecurityUtils.java`)
- 整合所有加密算法（国密SM2/SM4、AES、MD5、RC4）
- 提供一致的API风格
- 单例模式管理配置和状态

### 从 C# 到 Java 的转换说明

本次升级将 .NET 平台的 `SimpleEasy.Base.Cryptogram` 模块完整转换为 Android Java 实现：

**转换文件映射：**
- `Sm/SM2CryptoUtil.cs` → `Sm/SM2CryptoUtil.java` ✅
- `Sm/SM4CryptoUtil.cs` → `Sm/SM4CryptoUtil.java` ✅
- `Sm/Sm4Context.cs` → `Sm/Sm4Context.java` ✅
- `SM2Util.cs` → `SM2Util.java` ✅
- `SM4Util.cs` → `SM4Util.java` ✅
- `CryptogramUtil.cs` → `CryptogramUtil.java` ✅

**转换原则：**
1. **功能对等**: 严格保持原 C# 版本的加密/解密逻辑，确保算法一致性
2. **依赖替换**: 使用 BouncyCastle 替代 .NET 的加密库
3. **平台适配**: 适配 Android 平台特性（日志、字符编码等）
4. **代码规范**: 遵循 Java 编码规范，添加详细中文注释

**已移除的旧实现：**
- ❌ `app/src/main/java/run/yigou/gxzy/utils/CryptoUtil.java` (已删除)
- ❌ `app/src/main/java/run/yigou/gxzy/utils/SM2Util.java` (已删除)
- ❌ `app/src/main/java/run/yigou/gxzy/utils/SM4Util.java` (已删除)

## 常见问题

### 1. 为什么 SM2 只能使用公钥而不能使用私钥？

在 Android 客户端，出于安全性考虑，私钥通常保存在服务端。客户端只需要使用公钥进行加密和验签操作即可。

### 2. SM4 ECB 模式和 CBC 模式有什么区别？

- ECB (Electronic Codebook) 模式：简单快速，但安全性较低，相同明文会产生相同密文
- CBC (Cipher Block Chaining) 模式：安全性更高，每次加密即使相同明文也会产生不同密文，推荐使用

### 3. 如何生成 SM2 密钥对？

密钥对应在服务端生成，客户端只需要公钥部分。如果你需要生成测试用的密钥对，可以在服务端使用相关工具生成。

### 4. 加密后的数据如何传输？

建议将加密后的二进制数据转换为十六进制字符串或 Base64 字符串后再进行网络传输。

### 5. 出现加密/解密异常怎么办？

请检查以下几点：
- 是否正确初始化了 Bouncy Castle Provider
- 密钥和 IV 的长度是否正确（SM4密钥应为32位十六进制字符串，即16字节）
- 数据编码是否一致（建议统一使用 UTF-8）
- 是否在网络传输过程中数据发生了改变
- 检查 `SecurityUtils` 是否已正确初始化（调用 `initSecurityManager()`）

### 6. 新版本与旧版本有什么区别？

**新版本优势：**
- ✅ 模块化设计，职责清晰，易于维护
- ✅ 严格遵循国密标准算法实现
- ✅ 完整的错误处理和日志记录
- ✅ 详细的中文注释和文档
- ✅ 与 .NET 服务端实现完全兼容

**迁移指南：**
旧代码：
```java
// 旧版本
CryptoUtil.initSM2PublicKey(publicKeyX, publicKeyY);
String encrypted = CryptoUtil.doSm2Encrypt(plainText);
```

新代码：
```java
// 新版本（API保持兼容）
SecurityUtils.initSM2PublicKey(publicKeyX, publicKeyY);
String encrypted = SecurityUtils.doSm2Encrypt(plainText);
```

### 7. 性能优化建议

- 避免在主线程进行大量加密/解密操作
- 对于批量数据处理，考虑使用线程池
- SM2/SM4 算法计算密集，建议使用异步方式调用
- 缓存配置好的密钥，避免重复初始化

## 示例代码

下面是一个完整的使用示例：

```java
public class SecurityExample {
    public static void main(String[] args) {
        try {
            // 初始化SM2公钥
            String fullPublicKey = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" +
                                  "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
            SecurityUtils.initSM2PublicKeyWithFullFormat(fullPublicKey);
            
            // SM2 加密示例
            String plainText = "Hello, SM2!";
            String encrypted = SecurityUtils.doSm2Encrypt(plainText);
            System.out.println("SM2 加密结果: " + encrypted);
            
            // SM4 加密示例
            String text = "Hello, SM4!";
            String encryptedText = SecurityUtils.doSm4CbcEncrypt(text);
            String decryptedText = SecurityUtils.doSm4CbcDecrypt(encryptedText);
            System.out.println("SM4 原文: " + text);
            System.out.println("SM4 解密结果: " + decryptedText);
            
            // MD5 示例
            String md5Text = "Hello, MD5!";
            String md5Value = SecurityUtils.calcMd5(md5Text);
            System.out.println("MD5 值: " + md5Value);
            
            // AES 示例
            String aesText = "Hello, AES!";
            String aesEncrypted = SecurityUtils.aesEncrypt(aesText);
            String aesDecrypted = SecurityUtils.aesDecrypt(aesEncrypted);
            System.out.println("AES 原文: " + aesText);
            System.out.println("AES 解密结果: " + aesDecrypted);
            
            // RC4 示例
            String rc4Text = "Hello, RC4!";
            String rc4Encrypted = SecurityUtils.rc4Encrypt(rc4Text);
            String rc4Decrypted = SecurityUtils.rc4Decrypt(rc4Encrypted);
            System.out.println("RC4 原文: " + rc4Text);
            System.out.println("RC4 解密结果: " + rc4Decrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

以上就是在 Android 项目中使用 SecurityUtils 类进行各种安全操作的详细步骤。如有任何疑问，请参考源代码或联系技术支持。