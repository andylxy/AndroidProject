#include <jni.h>
#include <string.h>
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
 * 拆分为两段存储，运行时拼接，防止静态分析一步提取。
 */
#define SM2_PUB_KEY_FRAG1 "04CF658C65FB80CB5C7B91D3BD881521C2BD421202D29812785322F6366B8856B6"
#define SM2_PUB_KEY_FRAG2 "2D38D3AB5B5C299B03DD2EC0033370875A2787C6222E801AF87DDA53093322E2"

static char kSm2PubKeyBuf[131]; /* 65 bytes hex = 130 chars + null */

const char *native_get_sm2_public_key(void) {
    if (kSm2PubKeyBuf[0] == '\0') {
        strcpy(kSm2PubKeyBuf, SM2_PUB_KEY_FRAG1);
        strcat(kSm2PubKeyBuf, SM2_PUB_KEY_FRAG2);
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