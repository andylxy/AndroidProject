# 国密SM2算法使用详细教程

## 概述

本文档旨在详细介绍如何在您的Android项目中使用国密SM2算法。SM2是一种基于椭圆曲线的公钥密码算法，由国家密码管理局发布，用于替代RSA算法。它具有更高的安全性和更好的性能。

## 目录

1. [前置准备](#前置准备)
2. [第一步：配置依赖](#第一步配置依赖)
3. [第二步：初始化SM2密钥](#第二步初始化sm2密钥)
4. [第三步：启用SM2算法](#第三步启用sm2算法)
5. [第四步：使用SM2进行签名和验签](#第四步使用sm2进行签名和验签)
6. [第五步：使用SM2进行加密和解密](#第五步使用sm2进行加密和解密)
7. [获取公钥字符串](#获取公钥字符串)
8. [常见问题](#常见问题)

## 前置准备

在开始之前，请确保您已经：
1. 将SM2相关文件添加到项目中
2. 熟悉基本的Android开发知识
3. 了解公钥密码学的基本概念

## 第一步：配置依赖

我们已经在 [app/build.gradle](file:///D:/git/app/AndroidProject/app/build.gradle) 文件中添加了Bouncy Castle库的依赖，这是实现SM2算法的关键库：

```gradle
implementation 'org.bouncycastle:bcprov-jdk15to18:1.76'
```

如果您发现此依赖不存在，请手动添加。

## 第二步：初始化SM2密钥

在使用SM2算法之前，您需要初始化密钥对。在实际应用中，应该从安全的地方加载密钥，而不是硬编码。

### 2.1 获取密钥

首先，您需要获取SM2密钥对。这通常由您的后台服务或证书颁发机构提供。密钥包含：
- 公钥X坐标和Y坐标（可以分别提供或合并为一个字符串）
- 私钥D值

这些值通常以十六进制字符串的形式提供。

例如：
```java
// 方式一：分别提供X和Y坐标
String publicKeyX = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
String publicKeyY = "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

// 方式二：合并X和Y坐标为一个字符串（用逗号分隔）
String publicKey = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef,fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

// 方式三：使用完整公钥格式（类似前端sm-crypto库的方式）
String fullPublicKey = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdeffedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
```

### 2.2 初始化密钥对

使用 [CryptoUtil](file://D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/utils/CryptoUtil.java#L8-L163) 类的 `initSM2Keys` 方法初始化密钥对：

#### 方法一：分别提供X和Y坐标
```java
// 在您的Application类或启动活动中初始化
CryptoUtil.initSM2Keys(publicKeyX, publicKeyY, privateKeyD);
```

#### 方法二：使用合并后的公钥字符串
```java
// 在您的Application类或启动活动中初始化
CryptoUtil.initSM2Keys(publicKey, privateKeyD);
```

#### 方法三：使用完整公钥格式（类似前端sm-crypto）
```java
// 在您的Application类或启动活动中初始化
CryptoUtil.initSM2KeysWithFullPublicKey(fullPublicKey, privateKeyD);
```

完整示例：
```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化SM2密钥对（示例值，实际应从安全地方获取）
        // 方式一：分别提供X和Y坐标
        String publicKeyX = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        String publicKeyY = "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
        String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        
        CryptoUtil.initSM2Keys(publicKeyX, publicKeyY, privateKeyD);
        
        // 或者使用方式二：合并X和Y坐标为一个字符串
        // String publicKey = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef,fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
        // String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        // CryptoUtil.initSM2Keys(publicKey, privateKeyD);
        
        // 或者使用方式三：完整公钥格式（类似前端sm-crypto）
        // String fullPublicKey = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdeffedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
        // String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        // CryptoUtil.initSM2KeysWithFullPublicKey(fullPublicKey, privateKeyD);
    }
}
```

## 第三步：启用SM2算法

初始化密钥后，需要启用SM2算法：

```java
SecurityConfig.enableSM2();
```

完整示例：
```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化SM2密钥对（示例值，实际应从安全地方获取）
        String publicKey = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef,fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
        String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        
        // 初始化SM2密钥对
        CryptoUtil.initSM2Keys(publicKey, privateKeyD);
        
        // 启用SM2算法
        SecurityConfig.enableSM2();
    }
}
```

## 第四步：使用SM2进行签名和验签

### 4.1 使用SM2签名数据

使用 [CryptoUtil](file://D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/utils/CryptoUtil.java#L8-L163) 类的 `doSignature` 方法对数据进行签名：

```java
try {
    String dataToSign = "这是需要签名的数据";
    String signature = CryptoUtil.doSignature(dataToSign);
    Log.d("SM2", "签名结果: " + signature);
} catch (Exception e) {
    Log.e("SM2", "签名失败", e);
}
```

### 4.2 使用SM2验证签名

使用 [CryptoUtil](file://D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/utils/CryptoUtil.java#L8-L163) 类的 `doVerifySignature` 方法验证签名：

```java
try {
    String originalData = "这是需要签名的数据";
    String signature = "签名结果的十六进制字符串"; // 从前一步获取
    
    boolean isValid = CryptoUtil.doVerifySignature(originalData, signature);
    
    if (isValid) {
        Log.d("SM2", "签名验证成功");
    } else {
        Log.d("SM2", "签名验证失败");
    }
} catch (Exception e) {
    Log.e("SM2", "验证签名失败", e);
}
```

## 第五步：使用SM2进行加密和解密

### 5.1 使用SM2加密数据

使用 [CryptoUtil](file://D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/utils/CryptoUtil.java#L8-L163) 类的 `doSm2Encrypt` 方法加密数据：

```java
try {
    String plainText = "这是需要加密的数据";
    String encrypted = CryptoUtil.doSm2Encrypt(plainText);
    Log.d("SM2", "加密结果: " + encrypted);
} catch (Exception e) {
    Log.e("SM2", "加密失败", e);
}
```

### 5.2 使用SM2解密数据

使用 [CryptoUtil](file://D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/utils/CryptoUtil.java#L8-L163) 类的 `doSm2Decrypt` 方法解密数据：

```java
try {
    String encrypted = "加密结果的十六进制字符串"; // 从前一步获取
    
    String decrypted = CryptoUtil.doSm2Decrypt(encrypted);
    Log.d("SM2", "解密结果: " + decrypted);
} catch (Exception e) {
    Log.e("SM2", "解密失败", e);
}
```

## 获取公钥字符串

您可以使用以下方法获取当前设置的公钥字符串：

```java
// 获取公钥字符串（格式为"x,y"）
String publicKey = SM2SecurityConfig.getPublicKeyString();
Log.d("SM2", "当前公钥: " + publicKey);

// 获取完整公钥字符串（以"04"开头的格式，类似前端sm-crypto）
String fullPublicKey = SM2SecurityConfig.getFullPublicKeyString();
Log.d("SM2", "完整公钥: " + fullPublicKey);
```

## 常见问题

### 1. 如何生成SM2密钥对？

在实际生产环境中，SM2密钥对应由专业的密码服务或后台系统生成。如果您需要在本地生成密钥对进行测试，可以使用以下方法：

```java
// 注意：以下仅为演示目的，实际应用中不应在客户端生成密钥对
public void generateSM2KeyPair() {
    try {
        // 使用Bouncy Castle生成密钥对
        Security.addProvider(new BouncyCastleProvider());
        
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        keyPairGenerator.initialize(new ECGenParameterSpec("sm2p256v1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        // 获取公钥和私钥
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        
        // 提取参数用于初始化
        ECPoint w = publicKey.getW();
        BigInteger x = w.getAffineX();
        BigInteger y = w.getAffineY();
        BigInteger s = privateKey.getS();
        
        Log.d("SM2", "公钥X: " + x.toString(16));
        Log.d("SM2", "公钥Y: " + y.toString(16));
        Log.d("SM2", "私钥D: " + s.toString(16));
        
        // 组合成可使用的格式
        String combinedPublicKey = x.toString(16) + "," + y.toString(16);
        Log.d("SM2", "组合公钥: " + combinedPublicKey);
        
        // 完整公钥格式（类似前端sm-crypto）
        String fullPublicKey = "04" + String.format("%064x", x) + String.format("%064x", y);
        Log.d("SM2", "完整公钥: " + fullPublicKey);
    } catch (Exception e) {
        Log.e("SM2", "生成密钥对失败", e);
    }
}
```

### 2. SM2算法什么时候会被使用？

一旦您调用了 `SecurityConfig.enableSM2()`，并且设置了正确的密钥，系统会在以下情况下自动使用SM2算法：

1. HTTP请求签名（通过拦截器自动处理）
2. 当您显式调用 [CryptoUtil](file://D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/utils/CryptoUtil.java#L8-L163) 类的方法时

### 3. 如何禁用SM2算法？

如果您需要暂时禁用SM2算法，可以使用以下方法：

```java
SecurityConfig.disableSM2();
```

禁用后，系统将回退到原来的HMAC-SHA256签名算法。

### 4. 如何确认SM2算法正在工作？

您可以检查日志输出来确认SM2算法是否正常工作。当使用SM2签名时，您会看到类似以下的日志：

```
SM2签名字符串：
GET
api.example.com
/api/v1/users
1640995200000
abcd1234efgh5678

SM2签名结果：MEUCIQD...（签名内容）
```

### 5. 我应该在哪里存储密钥？

在生产环境中，永远不要将密钥硬编码在代码中。建议的安全存储方式包括：

1. Android Keystore系统
2. 安全的后端服务下发
3. 硬件安全模块(HSM)
4. 受信任的执行环境(TEE)

对于简单测试，您可以将密钥存储在assets目录的加密文件中，但这不适合生产环境。