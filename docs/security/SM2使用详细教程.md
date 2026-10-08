# SM2 国密算法使用详细教程

本文档旨在提供关于如何在 Android 项目中使用 SM2 国密算法的详细步骤说明。

## 目录

1. [准备工作](#准备工作)
2. [初始化密钥对](#初始化密钥对)
3. [启用SM2算法](#启用sm2算法)
4. [使用SM2进行签名和验签](#使用sm2进行签名和验签)
5. [使用SM2进行加密和解密](#使用sm2进行加密和解密)
6. [获取公钥字符串](#获取公钥字符串)
7. [常见问题](#常见问题)

## 准备工作

在开始使用 SM2 算法之前，请确保你的项目已经正确配置了 Bouncy Castle 加密库。

### 添加依赖

在你的 `build.gradle` 文件中添加以下依赖项：

```gradle
dependencies {
    implementation 'org.bouncycastle:bcprov-jdk15on:1.70'
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

### 2.2 初始化密钥对

使用 SecurityUtils 类的 `initSM2Keys` 方法初始化密钥对：

#### 方法一：分别提供X和Y坐标
```java
// 在您的Application类或启动活动中初始化
SecurityUtils.initSM2Keys(publicKeyX, publicKeyY, privateKeyD);
```

#### 方法二：使用合并后的公钥字符串
```java
// 在您的Application类或启动活动中初始化
SecurityUtils.initSM2KeysWithCombinedPublicKey(publicKey, privateKeyD);
```

#### 方法三：使用完整公钥格式（类似前端sm-crypto）
```java
// 在您的Application类或启动活动中初始化
SecurityUtils.initSM2KeysWithFullPublicKey(fullPublicKey, privateKeyD);
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
        
        SecurityUtils.initSM2Keys(publicKeyX, publicKeyY, privateKeyD);
        
        // 或者使用方式二：合并X和Y坐标为一个字符串
        // String publicKey = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef,fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
        // String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        // SecurityUtils.initSM2KeysWithCombinedPublicKey(publicKey, privateKeyD);
        
        // 或者使用方式三：完整公钥格式（类似前端sm-crypto）
        // String fullPublicKey = "041234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdeffedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
        // String privateKeyD = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        // SecurityUtils.initSM2KeysWithFullPublicKey(fullPublicKey, privateKeyD);
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
        SecurityUtils.initSM2KeysWithCombinedPublicKey(publicKey, privateKeyD);
        
        // 启用SM2算法
        SecurityConfig.enableSM2();
    }
}
```

## 第四步：使用SM2进行签名和验签

### 4.1 使用SM2签名数据

使用 SecurityUtils 类的 `doSignature` 方法对数据进行签名：

```java
try {
    String dataToSign = "这是需要签名的数据";
    String signature = SecurityUtils.doSignature(dataToSign);
    Log.d("SM2", "签名结果: " + signature);
} catch (Exception e) {
    Log.e("SM2", "签名失败", e);
}
```

### 4.2 使用SM2验证签名

使用 SecurityUtils 类的 `doVerifySignature` 方法验证签名：

```java
try {
    String originalData = "这是需要签名的数据";
    String signature = "签名结果的十六进制字符串"; // 从前一步获取
    
    boolean isValid = SecurityUtils.doVerifySignature(originalData, signature);
    
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

使用 SecurityUtils 类的 `doSm2Encrypt` 方法加密数据：

```java
try {
    String plainText = "这是需要加密的数据";
    String encrypted = SecurityUtils.doSm2Encrypt(plainText);
    Log.d("SM2", "加密结果: " + encrypted);
} catch (Exception e) {
    Log.e("SM2", "加密失败", e);
}
```

### 5.2 使用SM2解密数据

使用 SecurityUtils 类的 `doSm2Decrypt` 方法解密数据：

```java
try {
    String encrypted = "加密结果的十六进制字符串"; // 从前一步获取
    
    String decrypted = SecurityUtils.doSm2Decrypt(encrypted);
    Log.d("SM2", "解密结果: " + decrypted);
} catch (Exception e) {
    Log.e("SM2", "解密失败", e);
}
```

## 获取公钥字符串

您可以使用以下方法获取当前设置的公钥字符串：

```java
// 获取公钥字符串（格式为"x,y"）
String publicKey = SecurityUtils.getInstance().getSM2Util().getPublicKeyString();
Log.d("SM2", "当前公钥: " + publicKey);

// 获取完整公钥字符串（以"04"开头的格式，类似前端sm-crypto）
String fullPublicKey = SecurityUtils.getInstance().getSM2Util().getFullPublicKeyString();
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

### 2. 当您显式调用 SecurityUtils 类的方法时

请注意以下几点：
- SM2加密和验签操作只需要公钥，可以在Android客户端安全使用
- SM2解密和签名操作需要私钥，在Android客户端通常无法使用（私钥应保存在服务端）
- 所有SM2操作都需要先初始化密钥对才能正常使用

### 3. 如何处理SM2算法异常？

在使用SM2算法时可能会遇到以下异常：
- `IllegalStateException`: 未初始化密钥对就尝试使用SM2功能
- `RuntimeException`: 加密/解密/签名/验签过程中发生错误

建议在使用SM2功能时始终使用try-catch语句捕获并处理异常。