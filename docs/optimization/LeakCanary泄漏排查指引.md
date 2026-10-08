# LeakCanary 泄漏告警排查指引

> 适用范围：debug 包（LeakCanary 只在 `debugImplementation` / `previewImplementation` 生效，release 包不含此库）。

## 一、先分清三类logcat 输出

排查内存泄漏时，`LeakCanary` 相关日志**绝大多数是正常噪音**，不要看到就改代码：

| logcat 输出 | 级别 | 性质 | 处理 |
|---|---|---|---|
| `LeakCanary: Watching instance of ...` | D | **正常**。登记监视某个对象，等它该被回收却没回收 | 忽略 |
| `LeakCanary: Found N objects retained` | D | **判定为泄漏**，但**需先验证可复现** | 走第二节流程 |
| `LeakCanary: Not showing notification: ... POST_NOTIFICATIONS` | **E** | **无害噪音**。想弹通知提醒，被 Android 13+ 通知权限拦 | 已处理，见第五节 |

其中只有最后那条 `E` 级日志属于「Android 版本适配问题」，与内存泄漏本身**无关**。

## 二、判定真泄漏的两个硬判据

单次 `Found N objects retained` **不足以判定泄漏**。必须同时满足：

1. **可复现**：同一条操作路径连续 N 轮，每轮都出现 retained 事件；
2. **单调增长**：每轮操作后 `Java Heap` / `TOTAL PSS` 随轮次**线性上升**。

真泄漏的对象会被引用链钉住，GC 收不掉，内存必然随操作次数累积。
只有一次告警而内存平稳，通常是 GC 时序造成的误报。

## 三、用脚本一键验证

`scripts/leak-verify.sh` 把上述判据自动化：

```bash
# 从首页点书进阅读页、再返回，重复 6 轮
./scripts/leak-verify.sh --rounds 6 --enter "302 446" --leave-back

# 只进不出（便于手动退出后采样内存）
./scripts/leak-verify.sh --rounds 10 --enter "302 446" --dumps-per-round 2
```

输出示例（健康）：

```
round=0   pssKB=118900   javaHeapKB=32   retainedTotal=0
round=1   entered=1     pssKB=116921   javaHeapKB=32   retainedTotal=0
round=2   entered=1     pssKB=117330   javaHeapKB=32   retainedTotal=0
round=3   entered=1     pssKB=117286   javaHeapKB=32   retainedTotal=0
```

关键列：
- `entered=1` —— 该轮确实点进了目标页；为 0 说明坐标失效，本轮数据无效；
- `retainedTotal` —— 累计 retained 事件数；
- `pssKB` / `javaHeapKB` —— 平稳波动 = 健康；单调上升 = 真泄漏。

脚本会校验「目标页销毁次数」，若小于轮数会警告，避免用无效数据下结论。

### 本机注意事项

- adb 不在 PATH，需`export ANDROID_HOME='D:\Program Files\Android\Sdk'`；
  脚本会自动解析 `$ANDROID_HOME/platform-tools/adb.exe`。
- 目标页 `TipsFragmentActivity` 需要 `bookId` extra，**不能用于启动**；
  脚本默认用启动器 `SplashActivity` 拉起进程。
- Git Bash 会把 adb 的 `/sdcard/...` 路径转换成 Windows 路径，
  `uiautomator dump` / `pull` 必须用**双斜杠**：`adb shell "uiautomator dump //sdcard/x.xml"`。
- `pm clear` 后首启会连弹系统权限框，按钮坐标每次不同，
  需 dump 后按文案（`允许|全部允许|同意|继续`）匹配中心点，不能硬编码。

## 四、真确认泄漏后怎么定位持有链

LeakCanary 有个门槛：泄漏对象 **少于 5 个时只报数、不 dump 堆**，所以 logcat 里永远看不到
「谁持有它」的引用链（日志里会写 `app is visible & < 5 threshold`）。

要让堆栈出来，两个办法：

1. **累积到 ≥ 5 个泄漏对象**再触发；
2. 在 debug 代码里主动调 `LeakCanary.dump()`。

拿到 hprof 后用 Android Studio 的 Memory Profiler / MAT 查看 Retained Count 与引用链。

### 已排除的常见嫌疑（本项目实测）

排查阅读页泄漏时，以下全局容器经检查**不是元凶**，可优先跳过：

| 嫌疑点 | 判定 |
|---|---|
| `DialogManager` 的 `static HashMap<LifecycleOwner, DialogManager>` | **否**。`onStateChanged` 在 `ON_DESTROY` 时有 `remove` + `removeObserver` |
| `GlobalDataHolder` | **否**。容器只以 `String`/`Integer` 为 key 存POJO，不持 Activity |
| `ChapterContentManager` 单例持有回调 | **否**。Fragment 侧是 `new ChapterContentManager()`，非单例；`cancelAll()` 会关线程池 |
| `TipsBookNetReadFragment.onDestroy()` |清理较完整：adapter / RecyclerView / EventBus `unregister` / 回调置 null |

真正需要怀疑的是：**异步请求回调里捕获了 `this`**（单例管理器 + lambda/匿名内部类）、
以及**静态 Map 以 Fragment/Activity 为 key**。

## 五、通知噪音已处理

`app/src/debug/AndroidManifest.xml` 中声明了 `POST_NOTIFICATIONS`：

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

因为主 manifest 未声明此权限，LeakCanary 会反复刷 `E` 级日志并拉起
`leakcanary.internal.RequestPermissionActivity` 干扰排查。
**仅 debug 源集声明，release 包不包含**，生产环境不会多申请任何权限。

## 六、版本号陷阱

`app/build.gradle` 写的是 `leakcanary-android:2.14`，但 Gradle 实际解析到的可能是
**更新的版本**（本项目实测被传递依赖升级到 `3.0-alpha-8`）。
排查前请到 Gradle 缓存里确认真实版本：

```bash
find ~/.gradle/caches -iname "*leakcanary-android*"
```

另外 `dumpThreshold` 是 App端 `LeakCanary.config` 的属性，**不读 manifest 的 `meta-data`**，
想调门槛得在代码里设config。