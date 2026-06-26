/*
 * Base64 编码（纯 C，零外部依赖）
 * 用于将 HMAC-SHA256 的二进制签名结果编码为字符串格式。
 */

#include <string.h>
#include "include/native_core.h"

static const char b64_table[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

/* ── 公开 API：将二进制数据编码为 Base64 字符串 ──
 * 返回指针指向静态缓冲区，调用方应及时使用。
 */
const char *native_base64_encode(const uint8_t *in, size_t len) {
    static char buf[128]; /* 够容纳 HMAC-SHA256 输出（32 字节 → 44 字符） */
    size_t i, idx = 0;

    if (!in || len == 0) return "";

    for (i = 0; i < len; i += 3) {
        uint32_t val = (uint32_t)in[i] << 16;
        if (i + 1 < len) val |= (uint32_t)in[i + 1] << 8;
        if (i + 2 < len) val |= (uint32_t)in[i + 2];

        if (idx < sizeof(buf)) buf[idx++] = b64_table[(val >> 18) & 0x3f];
        if (idx < sizeof(buf)) buf[idx++] = b64_table[(val >> 12) & 0x3f];
        if (idx < sizeof(buf)) buf[idx++] = (i + 1 < len) ? b64_table[(val >> 6) & 0x3f] : '=';
        if (idx < sizeof(buf)) buf[idx++] = (i + 2 < len) ? b64_table[val & 0x3f] : '=';
    }
    if (idx < sizeof(buf)) buf[idx] = '\0';
    return buf;
}