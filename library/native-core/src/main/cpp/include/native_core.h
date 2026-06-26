#ifndef NATIVE_CORE_H
#define NATIVE_CORE_H

#include <jni.h>
#include <stdint.h>
#include <stddef.h>

/* ── SM4 加密 ──
 * SM4-CBC-PKCS7 填充加密，密钥在 native 层管理
 */
int native_sm4_encrypt(const uint8_t *key, int key_len,
    const uint8_t *iv, int iv_len,
    const uint8_t *in, int in_len,
    uint8_t *out, int *out_len);

/* ── SM2 加密 ──
 * 需要引入 GmSSL 或类似国密库实现
 */
jbyteArray native_sm2_encrypt(JNIEnv *env, const uint8_t *data, int len);

/* ── 密钥管理 ──
 * 运行时还原混淆后的 SM4 密钥
 */
const uint8_t *native_get_sm4_key(void);
const uint8_t *native_get_sm4_iv(void);
const char *native_get_sm2_public_key(void);

/* ── 反调试检测 ──
 * 通过 ptrace /proc/self/status TracerPid 检测调试器
 */
int native_is_debugger_attached(void);

#endif /* NATIVE_CORE_H */