# ==================== 基础配置 ====================

# 【关键】禁用代码优化（官方建议，防止 R8 过度优化）
# Debug 模式 R8 不优化，Release 模式会激进优化，导致反射失败
#-dontoptimize

# 保留源文件名和行号（便于调试崩溃日志）
#-keepattributes SourceFile,LineNumberTable

# 保留泛型签名（Gson 反序列化必需）
-keepattributes Signature

# 保留注解（反射和框架需要）
-keepattributes *Annotation*

# ==================== EasyHttp 框架官方规则 ====================

# EasyHttp 核心库
-keep class com.hjq.http.** {*;}

# 【关键】泛型监听器 - 必须保留，否则泛型解析失败
-keep class * implements com.hjq.http.listener.OnHttpListener {
    *;
}

# 【关键】响应类规则
-keep class * extends com.hjq.http.model.ResponseClass {
    *;
}

# ==================== 项目数据模型规则（API + 响应类）====================

# HTTP 包下的所有类（API、响应模型、实体类）
# 注意：必须保留所有成员，EasyHttp 通过反射访问字段和方法
-keep class run.yigou.gxzy.http.** { *; }

# GreenDAO 数据库实体类
-keep class run.yigou.gxzy.greendao.** { *; }

# Tips 模块全局数据持有者（单例模式，包含 Map 操作）
#-keep class run.yigou.gxzy.ui.tips.data.GlobalDataHolder { *; }

# Tips 模块数据模型（响应解析和存储）
#-keep class run.yigou.gxzy.ui.tips.DataBeans.** { *; }
#-keep class run.yigou.gxzy.ui.tips.tipsutils.** { *; }
#-keep class run.yigou.gxzy.ui.tips.entity.** { *; }
#-keep class run.yigou.gxzy.ui.tips.data.** { *; }

# ==================== 项目功能类规则 ====================

# 【关键】应用数据初始化器（负责加载全局数据）
#-keep class run.yigou.gxzy.app.AppDataInitializer { *; }

# 国密算法
#-keep class run.yigou.gxzy.Security.** { *; }

# AOP 日志注解
-keepclassmembernames class ** {
    @run.yigou.gxzy.aop.Log <methods>;
}

# ==================== SSL/TLS 和加密库规则 ====================

# 【关键】BouncyCastle 加密库（BKS KeyStore 提供者）
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keepnames class org.bouncycastle.** { *; }

# 【关键】Apache Harmony 安全提供者
-keep class org.apache.harmony.** { *; }
-dontwarn org.apache.harmony.**

# 【关键】Java 安全和加密相关类
-keep class javax.crypto.** { *; }
-keep class javax.net.ssl.** { *; }
-keep class java.security.** { *; }
-dontwarn javax.crypto.**
-dontwarn javax.net.ssl.**

# 【关键】网络安全配置（Android Network Security Config）
-keep class android.security.net.config.** { *; }

# ==================== Android 标准规则 ====================

# 枚举类型
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable 接口
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Serializable 接口
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}