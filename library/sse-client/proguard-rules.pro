# SSE Client 混淆规则

# OkHttp SSE
-keep class okhttp3.sse.** { *; }
-dontwarn okhttp3.sse.**

# Gson（compileOnly，保留反射需要的字段）
-keep class run.yigou.gxzy.sse.SseChunk { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
