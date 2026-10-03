#!/usr/bin/env bash
#
# leak-verify.sh —— 判定 LeakCanary 的 "Found N objects retained" 是真泄漏还是误报。
#
# 背景：LeakCanary 报一次 retained 不等于有泄漏。本脚本用「可复现性 + 内存增长趋势」
# 两个硬判据代替肉眼读 logcat，避免为不可复现的告警去盲改代码。
#
# 判据（两者都满足才判为真泄漏）：
#   1) 可复现：同一路径连续 N 轮，每轮都出现 retained 事件；
#   2) 单调增长：每轮进出后 Java Heap / TOTAL PSS 随轮次线性上升。
# 反之（不出现 retained，或内存平稳）→ 判为 GC 时序误报，不应据此改代码。
#
# 用法：
#   ./scripts/leak-verify.sh --rounds 6 --enter "302 446" [--leave-back]
#     --rounds N        进出循环次数（默认 6）
#     --enter "X Y"     进入目标页的点击坐标（必填）
#     --leave-back      退出用返回键（不传则只进不出，便于手动退出后采样）
#     --launch-activity 启动器 Activity（默认 SplashActivity；目标页需 bookId 参数，不能用于启动）
#     --target-activity 判定「已进入目标页」的 Activity 短名（默认 TipsFragmentActivity）
#     --package         应用包名（默认 run.yigou.gxzy.debug）
#     --settleN         每轮等待秒数（默认 7）
#     --dumps-per-round 每轮内存采样次数（默认 1）
#
# 注意：本脚本只做只读观测（logcat / dumpsys meminfo / input tap），不改任何代码。
#
set -uo pipefail

PACKAGE="run.yigou.gxzy.debug"
ENTRY_ACTIVITY="run.yigou.gxzy.debug/run.yigou.gxzy.ui.main.SplashActivity"
TARGET_ACTIVITY="TipsFragmentActivity"
ROUNDS=6
ENTER_TAP=""
SETTLE=7
BACK_KEY="KEYCODE_BACK"
DO_BACK=0
DUMPS_PER_ROUND=1

while [ $# -gt 0 ]; do
  case "$1" in
    --rounds)ROUNDS="$2"; shift 2;;
    --enter)ENTER_TAP="$2"; shift 2;;
    --leave-back)DO_BACK=1; shift;;
    --target-activity)TARGET_ACTIVITY="$2"; shift 2;;
    --package)PACKAGE="$2"; shift 2;;
    --settle)SETTLE="$2"; shift 2;;
    --dumps-per-round)DUMPS_PER_ROUND="$2"; shift 2;;
    -h|--help)sed -n '2,32p' "$0"; exit 0;;
    *)echo "未知参数: $1" >&2; exit 2;;
  esac
done

if [ -z "$ENTER_TAP" ]; then
  echo "错误：必须用 --enter \"X Y\" 指定进入目标页的点击坐标" >&2
  exit 2
fi

# adb 需绝对路径：SDK 路径含空格，未必在 PATH 中。
SDK_DIR="${ANDROID_HOME:-D:/Program Files/Android/Sdk}"
if [ -x "$SDK_DIR/platform-tools/adb.exe" ]; then
  ADB="$SDK_DIR/platform-tools/adb.exe"
elif command -v adb >/dev/null 2>&1; then
  ADB="adb"
else
  echo "错误：找不到 adb。请设置 ANDROID_HOME 或把 platform-tools 加入 PATH。" >&2
  exit 3
fi

if ! "$ADB" get-state >/dev/null 2>&1; then
  echo "错误：没有处于device 状态的设备。adb devices 输出如下：" >&2
  "$ADB" devices >&2
  exit 3
fi

# 读取 meminfo 指标（KB）。字段名格式不统一：
#   "TOTAL PSS:   123456TOTAL RSS: ..."
#   "     Java Heap:    8192"
# 所以用正则按"指标名: 数值"提取，而不是靠 awk 的 $1 精确匹配。
# 用法: meminfo_get "TOTAL PSS" / meminfo_get "Java Heap"
meminfo_get() {
  "$ADB" shell dumpsys meminfo "$PACKAGE" 2>/dev/null | tr -d '\r' \
    | grep -oE "$1:[[:space:]]+[0-9]+" | head -1 \
    | grep -oE "[0-9]+$"
}

# 进程未运行时直接报错，避免把空值当成 0 误判
require_running() {
  if ! "$ADB" shell pidof "$PACKAGE" >/dev/null 2>&1; then
    return 1
  fi
  local pid
  pid=$("$ADB" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')
  [ -n "$pid" ]
}

round_summary() {
  local r="$1"
  local pss heap retained
  pss=$(meminfo_get "TOTAL PSS")
  heap=$(meminfo_get "Java Heap")
  retained=$("$ADB" logcat -d 2>/dev/null | grep -cE "Found [0-9]+ object(s)? retained")
  printf "round=%-3s pssKB=%-8s javaHeapKB=%-8s retainedTotal=%s\n" "$r" "${pss:-n/a}" "${heap:-n/a}" "$retained"
}

echo "=== LeakCanary 泄漏验证 ==="
echo "包名: $PACKAGE"
echo "轮次: $ROUNDS   进入坐标: $ENTER_TAP   每轮等待: ${SETTLE}s"
echo

"$ADB" shell am force-stop "$PACKAGE" >/dev/null 2>&1
"$ADB" logcat -c >/dev/null 2>&1
sleep 2
"$ADB" shell am start -n "$ENTRY_ACTIVITY" >/dev/null 2>&1
sleep 12

if ! require_running; then
  echo "错误：启动后进程不存在。请确认 --launch-activity 指向真正的启动器" >&2
  echo "（当前: $ENTRY_ACTIVITY），且若停在权限页需先手动过一遍首次启动流程。" >&2
  exit 4
fi

echo "--- 起始内存 ---"
round_summary 0
echo

for i in $(seq 1 "$ROUNDS"); do
  # Git Bash 会把 /sdcard 之类转换，input tap 本身不受影响
  "$ADB" shell input tap $ENTER_TAP >/dev/null 2>&1
  sleep "$SETTLE"

  # 确认真的进了目标页，否则这轮数据无意义
  focus=$("$ADB" shell dumpsys window 2>/dev/null | tr -d '\r' | grep -i "mCurrentFocus" | head -1)
  case "$focus" in
    *"$TARGET_ACTIVITY"*) reached=1;;
    *) reached=0;;
  esac

  # 退出放在采样之前：泄漏发生在 Activity 销毁之后，退出后再测内存才有意义
  if [ "$DO_BACK" -eq 1 ]; then
    "$ADB" shell input keyevent "$BACK_KEY" >/dev/null 2>&1
    settle_back=$((SETTLE - 2))
    [ "$settle_back" -gt 0 ] && sleep "$settle_back"
  fi

  # 本轮内的多次采样取最大值，减小单点噪声
  best_pss=0; best_heap=0
  for _ in $(seq 1 "$DUMPS_PER_ROUND"); do
    p=$(meminfo_get "TOTAL PSS"); h=$(meminfo_get "Java Heap")
    [ -n "${p:-}" ] && [ "$p" -gt "$best_pss" ] 2>/dev/null && best_pss=$p
    [ -n "${h:-}" ] && [ "$h" -gt "$best_heap" ] 2>/dev/null && best_heap=$h
  done

  retained=$("$ADB" logcat -d 2>/dev/null | grep -cE "Found [0-9]+ object(s)? retained")
  printf "round=%-3s entered=%-5s pssKB=%-8s javaHeapKB=%-8s retainedTotal=%s\n" \
    "$i" "$reached" "${best_pss:-n/a}" "${best_heap:-n/a}" "$retained"
done

echo
echo "=== 结论依据（原始计数）==="
total_retained=$("$ADB" logcat -d 2>/dev/null | grep -cE 'Found [0-9]+ object(s)? retained')
destroy_count=$("$ADB" logcat -d 2>/dev/null | tr -d '\r' | grep -c "$TARGET_ACTIVITY - onDestroy")
echo "retained 事件累计: $total_retained"
echo "目标页销毁次数:    $destroy_count （应约等于轮数，说明每轮都真的进出过）"
echo "最终 pssKB:       $(meminfo_get "TOTAL PSS")"
echo "最终 javaHeapKB:  $(meminfo_get "Java Heap")"
echo
if [ "$destroy_count" -lt "$ROUNDS" ]; then
  echo "⚠️ 警告：目标页销毁次数($destroy_count) < 轮次($ROUNDS)，说明有轮次没点进目标页，"
  echo "        本次数据不足以判定。请用 uiautomator dump 确认 --enter 坐标后重跑。"
fi
echo "判定规则："
echo "  retained=0 或销毁次数=0（数据无效） → 不可下结论，先修好测试路径"
echo "  retained>0 且 pss/heap 随轮次单调上升  → 真泄漏，按 dump 堆栈定位持有链"
echo "  retained>0 但内存平稳                → 一次性对象/噪声，不必盲改"
echo "  retained=0                → 误报（GC 时序），不要改代码"
echo
echo "提示：LeakCanary 少于 5 个泄漏对象时只报数不dump 堆，"
echo "      想看引用链需累积到 >=5 或在代码里调 LeakCanary.dump()。"