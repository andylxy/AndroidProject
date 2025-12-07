# Android国密算法工具类使用说明

## 概述

本文档介绍了如何在Android项目中使用类似于前端`sm-crypto`库的国密算法工具类。提供了SM2和SM4算法的便捷使用方式，使开发者能够在Android平台上轻松实现国密加密、解密、签名和验签功能。

特别注意：在Android客户端场景下，通常只有公钥，主要用于加密数据发送给服务端。

## 目录

1. [集成说明](#集成说明)
2. [SM2算法使用](#sm2算法使用)
   - [初始化公钥（客户端常用）](#初始化公钥客户端常用)
   - [初始化密钥对（服务端或特殊场景使用）](#初始化密钥对服务端或特殊场景使用)
   - [加密解密](#加密解密)
   - [签名验签](#签名验签)
3. [SM4算法使用](#sm4算法使用)
   - [ECB模式](#ecb模式)
   - [CBC模式](#cbc模式)
4. [统一接口使用](#统一接口使用)

## 集成说明

1. 确保已在项目中添加Bouncy Castle依赖：

```gradle
implementation 'org.bouncycastle:bcprov-jdk15to18:1.76'
```

2. 将以下工具类文件添加到项目中：
   - SM2Util.java
   - SM4Util.java
   - CryptoUtil.java

3. 在Application或启动时初始化Bouncy Castle提供者：

```java
Security.addProvider(new BouncyCastleProvider());
```

## SM2算法使用

### 初始化公钥（客户端常用）

在Android客户端场景下，通常只需要公钥来加密数据发送给服务端：

#### 方式一：使用分离的坐标形式

```java
// 使用分离的坐标形式初始化公钥
String publicKeyX = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
String publicKeyY = "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";

SM2Util.getInstance().initPublicKey(publicKeyX, publicKeyY);
```

#### 方式二：使用组合公钥形式（x,y格式）

```java
// 使用组合公钥形式初始化公钥
String publicKey = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef,fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";

SM2Util.getInstance().initPublicKeyWithCombinedFormat(publicKey);
```

#### 方式三：使用完整公钥形式（类似sm-crypto）

```java
// 使用完整公钥形式初始化公钥（类似前端sm-crypto库的使用方式）
String publicKey = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" +
                  "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";

SM2Util.getInstance().initPublicKeyWithFullFormat(publicKey);
```

### 初始化密钥对（服务端或特殊场景使用）

仅在需要完整密钥对时使用（例如本地解密数据）：

#### 方式一：使用分离的坐标形式

```java
// 使用分离的坐标形式初始化密钥对
String publicKeyX = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
String publicKeyY = "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

SM2Util.getInstance().initKeys(publicKeyX, publicKeyY, privateKeyD);
```

#### 方式二：使用组合公钥形式（x,y格式）

```java
// 使用组合公钥形式初始化密钥对
String publicKey = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef,fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

SM2Util.getInstance().initKeysWithCombinedPublicKey(publicKey, privateKeyD);
```

#### 方式三：使用完整公钥形式（类似sm-crypto）

```java
// 使用完整公钥形式初始化密钥对（类似前端sm-crypto库的使用方式）
String publicKey = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" +
                  "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

SM2Util.getInstance().initKeysWithFullPublicKey(publicKey, privateKeyD);
```

### 加密解密

```java
// SM2加密（使用公钥加密，适用于Android客户端场景）
String plainText = "需要加密的数据";
String encrypted = SM2Util.getInstance().doEncrypt(plainText);
Log.d("SM2", "加密结果：" + encrypted);

// SM2解密（需要私钥，Android客户端通常无法使用）
String decrypted = SM2Util.getInstance().doDecrypt(encrypted);
Log.d("SM2", "解密结果：" + decrypted);
```

### 签名验签

```java
// SM2签名（需要私钥，Android客户端通常无法使用）
String dataToSign = "需要签名的数据";
String signature = SM2Util.getInstance().doSignature(dataToSign);
Log.d("SM2", "签名结果：" + signature);

// SM2验签（只需要公钥，适用于Android客户端场景）
boolean isValid = SM2Util.getInstance().doVerifySignature(dataToSign, signature);
Log.d("SM2", "验签结果：" + (isValid ? "有效" : "无效"));
```

## SM4算法使用

### ECB模式

```java
// SM4 ECB加密（使用默认密钥）
String plainText = "需要加密的数据";
String encrypted = SM4Util.encryptECB(plainText);
Log.d("SM4", "ECB加密结果：" + encrypted);

// SM4 ECB解密（使用默认密钥）
String decrypted = SM4Util.decryptECB(encrypted);
Log.d("SM4", "ECB解密结果：" + decrypted);

// SM4 ECB加密（自定义密钥）
String key = "0123456789abcdeffedcba9876543210";
String encryptedWithKey = SM4Util.encryptECB(plainText, key);
Log.d("SM4", "ECB加密结果（自定义密钥）：" + encryptedWithKey);

// SM4 ECB解密（自定义密钥）
String decryptedWithKey = SM4Util.decryptECB(encryptedWithKey, key);
Log.d("SM4", "ECB解密结果（自定义密钥）：" + decryptedWithKey);
```

### CBC模式

```java
// SM4 CBC加密（使用默认密钥和IV）
String plainText = "需要加密的数据";
String encrypted = SM4Util.encryptCBC(plainText);
Log.d("SM4", "CBC加密结果：" + encrypted);

// SM4 CBC解密（使用默认密钥和IV）
String decrypted = SM4Util.decryptCBC(encrypted);
Log.d("SM4", "CBC解密结果：" + decrypted);

// SM4 CBC加密（自定义密钥和IV）
String key = "0123456789abcdeffedcba9876543210";
String iv = "fedcba98765432100123456789abcdef";
String encryptedWithKey = SM4Util.encryptCBC(plainText, key, iv);
Log.d("SM4", "CBC加密结果（自定义密钥和IV）：" + encryptedWithKey);

// SM4 CBC解密（自定义密钥和IV）
String decryptedWithKey = SM4Util.decryptCBC(encryptedWithKey, key, iv);
Log.d("SM4", "CBC解密结果（自定义密钥和IV）：" + decryptedWithKey);
```

## 统一接口使用

为了更接近前端`sm-crypto`库的使用体验，提供了统一的CryptoUtil工具类：

### Android客户端常用方式（仅使用公钥）

```java
// 初始化SM2公钥（类似前端方式）
String publicKey = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" +
                  "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
CryptoUtil.initSM2PublicKeyWithFullFormat(publicKey);

// SM2加密（客户端常用）
String encrypted = CryptoUtil.doSm2Encrypt("待加密数据");

// SM2验签（客户端常用）
boolean isValid = CryptoUtil.doVerifySignature("待验证数据", "签名值");
```

### 特殊场景使用（需要完整密钥对）

```java
// 初始化SM2密钥对（类似前端方式）
String publicKey = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" +
                  "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
CryptoUtil.initSM2KeysWithFullPublicKey(publicKey, privateKeyD);

// SM2加密
String encrypted = CryptoUtil.doSm2Encrypt("待加密数据");

// SM2解密
String decrypted = CryptoUtil.doSm2Decrypt(encrypted);

// SM2签名
String signature = CryptoUtil.doSignature("待签名数据");

// SM2验签
boolean isValid = CryptoUtil.doVerifySignature("待签名数据", signature);

// SM4 ECB加密（使用默认密钥）
String sm4Encrypted = CryptoUtil.doSm4Encrypt("待加密数据");

// SM4 ECB解密（使用默认密钥）
String sm4Decrypted = CryptoUtil.doSm4Decrypt(sm4Encrypted);

// SM4 CBC加密（使用默认密钥和IV）
String sm4CbcEncrypted = CryptoUtil.doSm4CbcEncrypt("待加密数据");

// SM4 CBC解密（使用默认密钥和IV）
String sm4CbcDecrypted = CryptoUtil.doSm4CbcDecrypt(sm4CbcEncrypted);

// SM4 ECB加密（自定义密钥）
String sm4EncryptedWithKey = CryptoUtil.doSm4Encrypt("待加密数据", "your_custom_key");

// SM4 ECB解密（自定义密钥）
String sm4DecryptedWithKey = CryptoUtil.doSm4Decrypt(sm4EncryptedWithKey, "your_custom_key");

// SM4 CBC加密（自定义密钥和IV）
String sm4CbcEncryptedWithKey = CryptoUtil.doSm4CbcEncrypt("待加密数据", "your_custom_key", "your_custom_iv");

// SM4 CBC解密（自定义密钥和IV）
String sm4CbcDecryptedWithKey = CryptoUtil.doSm4CbcDecrypt(sm4CbcEncryptedWithKey, "your_custom_key", "your_custom_iv");
```

## 注意事项

1. 请妥善保管密钥，不要硬编码在代码中
2. 生产环境中应从安全的地方加载密钥
3. 所有加密操作都是线程安全的
4. 加密结果均为十六进制字符串格式
5. Android客户端通常只需公钥进行加密和验签，无需持有私钥
6. 私钥仅在特殊场景（如本地解密数据）下使用
7. SM2加密使用公钥加密，私钥解密（符合Android客户端使用场景）