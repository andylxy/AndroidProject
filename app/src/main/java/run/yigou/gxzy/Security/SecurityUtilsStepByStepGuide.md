# Android 国密 SM2/SM4 加密使用详细指南

本文档旨在提供关于如何在 Android 项目中使用国密 SM2 和 SM4 算法的详细步骤说明。

## 目录

1. [准备工作](#准备工作)
2. [SM2 公钥加密使用指南](#sm2-公钥加密使用指南)
3. [SM4 对称加密使用指南](#sm4-对称加密使用指南)
4. [工具类说明](#工具类说明)
5. [常见问题](#常见问题)

## 准备工作

在开始使用 SM2/SM4 加密之前，请确保你的项目已经正确配置了 Bouncy Castle 加密库。

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

## SM2 公钥加密使用指南

SM2 是一种基于椭圆曲线的公钥加密算法，在 Android 客户端通常只使用公钥进行加密和验签操作。

### 第一步：生成或导入 SM2 密钥对

#### 方式一：使用分离坐标形式初始化公钥

如果你拥有公钥的 x 和 y 坐标，可以使用以下方式初始化：

```java
import run.yigou.gxzy.utils.SM2Util;

// 假设你已经有了公钥的 x 和 y 坐标（十六进制字符串）
String publicKeyX = "your_public_key_x_coordinate";
String publicKeyY = "your_public_key_y_coordinate";

// 初始化 SM2 工具类
SM2Util sm2Util = SM2Util.initSM2KeysWithXY(publicKeyX, publicKeyY);
```

#### 方式二：使用完整公钥形式初始化

如果你拥有完整格式的公钥（即 ASN.1 编码的公钥），可以使用以下方式初始化：

```java
import run.yigou.gxzy.utils.SM2Util;

// 假设你已经有了完整格式的公钥（十六进制字符串）
String fullPublicKey = "your_full_public_key";

// 初始化 SM2 工具类
SM2Util sm2Util = SM2Util.initSM2KeysWithFullPublicKey(fullPublicKey);
```

### 第二步：使用 SM2 进行数据加密

获取到 SM2Util 实例后，你可以使用它来加密数据：

```java
try {
    String plainText = "需要加密的数据";
    byte[] encryptedData = sm2Util.encrypt(plainText.getBytes("UTF-8"));
    
    // 将加密结果转换为十六进制字符串以便传输或存储
    String encryptedHex = bytesToHex(encryptedData);
    System.out.println("加密后的数据：" + encryptedHex);
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
    
    boolean isValid = sm2Util.verify(data.getBytes("UTF-8"), hexToBytes(signature));
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
import run.yigou.gxzy.http.security.SecurityConfig;

// 使用默认密钥进行 ECB 模式加密
String plainText = "需要加密的数据";
String encrypted = SecurityConfig.sm4EcbEncrypt(plainText);
System.out.println("ECB加密结果：" + encrypted);
```

#### CBC 模式（推荐）

CBC 模式更安全，每次加密即使相同明文也会产生不同的密文：

```java
import run.yigou.gxzy.http.security.SecurityConfig;

// 使用默认密钥和IV进行 CBC 模式加密
String plainText = "需要加密的数据";
String encrypted = SecurityConfig.sm4CbcEncrypt(plainText);
System.out.println("CBC加密结果：" + encrypted);
```

### 第二步：自定义密钥和 IV 进行加密

你可以使用自定义的密钥和初始向量（IV）进行加密：

```java
import run.yigou.gxzy.http.security.SecurityConfig;

// 自定义密钥和 IV（均为16字节）
String key = "your_custom_key_16bytes";
String iv = "your_custom_iv_16bytes";
String plainText = "需要加密的数据";

// 使用自定义密钥和 IV 进行 CBC 模式加密
String encrypted = SecurityConfig.sm4CbcEncrypt(plainText, key, iv);
System.out.println("自定义密钥CBC加密结果：" + encrypted);
```

### 第三步：解密数据

对于服务端返回的 SM4 加密数据，你可以使用相应的方法进行解密：

```java
import run.yigou.gxzy.http.security.SecurityConfig;

// 解密 ECB 模式加密的数据
String encryptedEcb = "ECB模式加密的数据";
String decryptedEcb = SecurityConfig.sm4EcbDecrypt(encryptedEcb);
System.out.println("ECB解密结果：" + decryptedEcb);

// 解密 CBC 模式加密的数据
String encryptedCbc = "CBC模式加密的数据";
String decryptedCbc = SecurityConfig.sm4CbcDecrypt(encryptedCbc);
System.out.println("CBC解密结果：" + decryptedCbc);
```

## 工具类说明

### SecurityConfig 类

这是主要的加密配置类，提供了便捷的 SM4 加密/解密方法：

- `sm4EcbEncrypt(String plainText)` - 使用默认密钥进行 ECB 模式加密
- `sm4EcbDecrypt(String cipherText)` - 使用默认密钥进行 ECB 模式解密
- `sm4CbcEncrypt(String plainText)` - 使用默认密钥和 IV 进行 CBC 模式加密
- `sm4CbcDecrypt(String cipherText)` - 使用默认密钥和 IV 进行 CBC 模式解密
- `sm4EcbEncrypt(String plainText, String key)` - 使用指定密钥进行 ECB 模式加密
- `sm4EcbDecrypt(String cipherText, String key)` - 使用指定密钥进行 ECB 模式解密
- `sm4CbcEncrypt(String plainText, String key, String iv)` - 使用指定密钥和 IV 进行 CBC 模式加密
- `sm4CbcDecrypt(String cipherText, String key, String iv)` - 使用指定密钥和 IV 进行 CBC 模式解密

### SM2Util 类

SM2 工具类提供了 SM2 公钥加密的相关功能：

- `initSM2KeysWithXY(String pubX, String pubY)` - 使用公钥坐标初始化
- `initSM2KeysWithFullPublicKey(String pubKey)` - 使用完整公钥初始化
- `encrypt(byte[] data)` - 公钥加密
- `verify(byte[] data, byte[] signature)` - 签名验证

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
- 密钥和 IV 的长度是否正确（应为16字节）
- 数据编码是否一致（建议统一使用 UTF-8）
- 是否在网络传输过程中数据发生了改变

## 示例代码

下面是一个完整的使用示例：

```java
public class CryptoExample {
    public static void main(String[] args) {
        try {
            // SM2 加密示例
            String publicKeyX = "your_public_key_x";
            String publicKeyY = "your_public_key_y";
            
            SM2Util sm2Util = SM2Util.initSM2KeysWithXY(publicKeyX, publicKeyY);
            String plainText = "Hello, SM2!";
            byte[] encrypted = sm2Util.encrypt(plainText.getBytes("UTF-8"));
            String encryptedHex = bytesToHex(encrypted);
            System.out.println("SM2 加密结果: " + encryptedHex);
            
            // SM4 加密示例
            String text = "Hello, SM4!";
            String encryptedText = SecurityConfig.sm4CbcEncrypt(text);
            String decryptedText = SecurityConfig.sm4CbcDecrypt(encryptedText);
            System.out.println("SM4 原文: " + text);
            System.out.println("SM4 解密结果: " + decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 辅助方法：字节数组转十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    // 辅助方法：十六进制字符串转字节数组
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
```

以上就是在 Android 项目中使用国密 SM2/SM4 算法的详细步骤。如有任何疑问，请参考源代码或联系技术支持。