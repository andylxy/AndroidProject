#include <jni.h>
#include <string.h>
#include <stdio.h>
#include "include/native_core.h"

/*
 * 密钥存储与混淆
 *
 * SM4 密钥拆分为多段，编译时通过 XOR 混淆，
 * 运行时还原。Java 侧始终无法直接读取密钥原文。
 *
 *   tools/obfuscate_keys.py 脚本可自动生成混淆后的字节数组
 *   用法: python tools/obfuscate_keys.py --key <hex> --iv <hex> --sm2pub <hex>
 * 当前使用测试密钥（仅供验证编译和接口正确性）
 */

/* 混淆后的 SM4 密钥（16 字节）—— XOR key: { 0xAB,0xCD,0xEF,0x01,0x23,0x45,0x67,0x89 } */
static const uint8_t kObfuscatedSm4Key[] = {
    0xFE, 0xED, 0xFA, 0xCE, 0xBE, 0xEF, 0xCA, 0xFE,
    0xDE, 0xAD, 0xBE, 0xEF, 0xAB, 0xCD, 0xEF, 0x01,
};

/* 混淆后的 SM4 IV（16 字节） */
static const uint8_t kObfuscatedSm4Iv[] = {
    0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF,
    0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10,
};

/*
 * SM2 公钥（16 进制字符串，以 04 开头）
 * 原始值：04CF658C65FB80CB5C7B91D3BD881521C2BD421202D29812785322F6366B8856B6
 *          2D38D3AB5B5C299B03DD2EC0033370875A2787C6222E801AF87DDA53093322E2
 * 拆分为两段 XOR 混淆后存储，运行时还原，防止静态分析一步提取。
 * 使用 tools/obfuscate_keys.py --sm2pub <hex> 重新生成。
 */
static const uint8_t kObfuscatedSm2PubFrag1[] = {
    0xAF, 0x02, 0x8A, 0x8D, 0x46, 0xBE, 0xE7, 0x42, 0xA2, 0xA7, 0x2B, 0x4B,
    0xCB, 0xDC, 0x27, 0x31, 0x69, 0x70, 0xAD, 0x13, 0x21, 0x97, 0xFF, 0x9B,
    0x86, 0x8F, 0x98, 0x6E, 0x40, 0x3F, 0xBA, 0x46,
};

static const uint8_t kObfuscatedSm2PubFrag2[] = {
    0x1D, 0xE0, 0xD7, 0xD2, 0x88, 0x1E, 0x3B, 0xA0, 0x65, 0xDF, 0x67, 0xB6,
    0xB6, 0x57, 0x01, 0x60, 0x2C, 0x97, 0xC8, 0x86, 0xE5, 0x67, 0x49, 0x09,
    0xE4, 0x24, 0xC7, 0x42, 0x25, 0x5D, 0x01, 0x32, 0x49,
};

static char kSm2PubKeyBuf[131]; /* 65 bytes hex = 130 chars + null */

const char *native_get_sm2_public_key(void) {
    if (kSm2PubKeyBuf[0] == '\0') {
        int i, offset = 0;
        uint8_t xor_key[] = { 0xAB, 0xCD, 0xEF, 0x01, 0x23, 0x45, 0x67, 0x89,
                              0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10 };
        int frag1_len = sizeof(kObfuscatedSm2PubFrag1);
        int frag2_len = sizeof(kObfuscatedSm2PubFrag2);

        for (i = 0; i < frag1_len; i++) {
            uint8_t plain = kObfuscatedSm2PubFrag1[i] ^ xor_key[i % sizeof(xor_key)];
            sprintf(kSm2PubKeyBuf + offset, "%02X", plain);
            offset += 2;
        }
        for (i = 0; i < frag2_len; i++) {
            uint8_t plain = kObfuscatedSm2PubFrag2[i] ^ xor_key[(frag1_len + i) % sizeof(xor_key)];
            sprintf(kSm2PubKeyBuf + offset, "%02X", plain);
            offset += 2;
        }
        kSm2PubKeyBuf[offset] = '\0';
    }
    return kSm2PubKeyBuf;
}

/* XOR 解混淆密钥 */
static void deobfuscate(const uint8_t *obfuscated, uint8_t *plain, int len) {
    int i;
    uint8_t xor_key[] = { 0xAB, 0xCD, 0xEF, 0x01, 0x23, 0x45, 0x67, 0x89 };
    for (i = 0; i < len; i++) {
        plain[i] = obfuscated[i] ^ xor_key[i % sizeof(xor_key)];
    }
}

const uint8_t *native_get_sm4_key(void) {
    return kObfuscatedSm4Key;
}

const uint8_t *native_get_sm4_iv(void) {
    return kObfuscatedSm4Iv;
}

/* ── API 签名密钥（运行时动态设置，XOR 混淆后存储）── */

static uint8_t kObfuscatedSigningKey[32];
static int kSigningKeyLen = 0;

void native_set_signing_key(const uint8_t *key, int len) {
    int i;
    if (len > 32) len = 32;
    uint8_t xor_key[] = { 0xAB, 0xCD, 0xEF, 0x01, 0x23, 0x45, 0x67, 0x89 };
    for (i = 0; i < len; i++) {
        kObfuscatedSigningKey[i] = key[i] ^ xor_key[i % sizeof(xor_key)];
    }
    kSigningKeyLen = len;
}

int native_get_signing_key(uint8_t *out, int *out_len) {
    int i;
    if (kSigningKeyLen == 0) {
        *out_len = 0;
        return -1;
    }
    uint8_t xor_key[] = { 0xAB, 0xCD, 0xEF, 0x01, 0x23, 0x45, 0x67, 0x89 };
    int len = kSigningKeyLen;
    for (i = 0; i < len; i++) {
        out[i] = kObfuscatedSigningKey[i] ^ xor_key[i % sizeof(xor_key)];
    }
    *out_len = len;
    return 0;
}

void native_clear_signing_key(void) {
    memset(kObfuscatedSigningKey, 0, sizeof(kObfuscatedSigningKey));
    kSigningKeyLen = 0;
}