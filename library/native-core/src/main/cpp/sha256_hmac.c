/*
 * 自包含 SHA-256 + HMAC-SHA256 实现（纯 C，零外部依赖）
 *
 * 基于 FIPS 180-4 标准实现 SHA-256 压缩函数，
 * 基于 RFC 2104 实现 HMAC 构造。
 * 适用于 Android NDK 环境，无需链接 OpenSSL。
 */

#include <string.h>
#include <stdint.h>
#include "include/native_core.h"

/* ── SHA-256 常量 ── */
static const uint32_t K[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
    0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
    0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
    0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
    0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
    0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
    0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
    0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
    0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
};

#define ROTR(x, n) (((x) >> (n)) | ((x) << (32 - (n))))
#define CH(x, y, z) (((x) & (y)) ^ (~(x) & (z)))
#define MAJ(x, y, z) (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))
#define EP0(x) (ROTR(x, 2) ^ ROTR(x, 13) ^ ROTR(x, 22))
#define EP1(x) (ROTR(x, 6) ^ ROTR(x, 11) ^ ROTR(x, 25))
#define SIG0(x) (ROTR(x, 7) ^ ROTR(x, 18) ^ ((x) >> 3))
#define SIG1(x) (ROTR(x, 17) ^ ROTR(x, 19) ^ ((x) >> 10))

typedef struct {
    uint8_t data[64];
    uint64_t bitlen;
    uint32_t state[8];
} SHA256_CTX;

static void sha256_transform(SHA256_CTX *ctx, const uint8_t block[64]) {
    uint32_t W[64];
    uint32_t A, B, C, D, E, F, G, H, T1, T2;
    int i;

    for (i = 0; i < 16; i++) {
        W[i] = ((uint32_t)block[4*i] << 24) | ((uint32_t)block[4*i+1] << 16)
             | ((uint32_t)block[4*i+2] << 8) | (uint32_t)block[4*i+3];
    }
    for (i = 16; i < 64; i++) {
        W[i] = SIG1(W[i-2]) + W[i-7] + SIG0(W[i-15]) + W[i-16];
    }

    A = ctx->state[0]; B = ctx->state[1]; C = ctx->state[2]; D = ctx->state[3];
    E = ctx->state[4]; F = ctx->state[5]; G = ctx->state[6]; H = ctx->state[7];

    for (i = 0; i < 64; i++) {
        T1 = H + EP1(E) + CH(E, F, G) + K[i] + W[i];
        T2 = EP0(A) + MAJ(A, B, C);
        H = G; G = F; F = E; E = D + T1;
        D = C; C = B; B = A; A = T1 + T2;
    }

    ctx->state[0] += A; ctx->state[1] += B; ctx->state[2] += C; ctx->state[3] += D;
    ctx->state[4] += E; ctx->state[5] += F; ctx->state[6] += G; ctx->state[7] += H;
}

static void sha256_init(SHA256_CTX *ctx) {
    ctx->bitlen = 0;
    ctx->state[0] = 0x6a09e667;
    ctx->state[1] = 0xbb67ae85;
    ctx->state[2] = 0x3c6ef372;
    ctx->state[3] = 0xa54ff53a;
    ctx->state[4] = 0x510e527f;
    ctx->state[5] = 0x9b05688c;
    ctx->state[6] = 0x1f83d9ab;
    ctx->state[7] = 0x5be0cd19;
}

static void sha256_update(SHA256_CTX *ctx, const uint8_t *in, size_t len) {
    size_t i;
    for (i = 0; i < len; i++) {
        ctx->data[ctx->bitlen / 8 % 64] = in[i];
        ctx->bitlen += 8;
        if (ctx->bitlen % (64 * 8) == 0) {
            sha256_transform(ctx, ctx->data);
        }
    }
}

static void sha256_final(SHA256_CTX *ctx, uint8_t hash[32]) {
    uint64_t bitlen_big;
    int i;

    /* PKCS#7 填充 */
    ctx->data[ctx->bitlen / 8 % 64] = 0x80;
    i = (int)(ctx->bitlen / 8 % 64) + 1;
    while (i < 64) {
        ctx->data[i++] = 0;
    }

    if (ctx->bitlen / 8 % 64 >= 56) {
        sha256_transform(ctx, ctx->data);
        memset(ctx->data, 0, 56);
    }

    /* 附加原始长度（大端序） */
    bitlen_big = ctx->bitlen;
    for (i = 63; i >= 56; i--) {
        ctx->data[i] = (uint8_t)(bitlen_big & 0xff);
        bitlen_big >>= 8;
    }
    sha256_transform(ctx, ctx->data);

    /* 输出 */
    for (i = 0; i < 8; i++) {
        hash[4*i]   = (uint8_t)(ctx->state[i] >> 24);
        hash[4*i+1] = (uint8_t)(ctx->state[i] >> 16);
        hash[4*i+2] = (uint8_t)(ctx->state[i] >> 8);
        hash[4*i+3] = (uint8_t)(ctx->state[i]);
    }
}

/* ── 公开 API：计算 HMAC-SHA256，输出原始 32 字节 ── */
void native_hmac_sha256(const uint8_t *key, size_t key_len,
                        const uint8_t *data, size_t data_len,
                        uint8_t out[32]) {
    SHA256_CTX ctx;
    uint8_t k_ipad[64], k_opad[64];
    uint8_t hash[32];
    size_t i;

    /* 密钥长度 > 64 时先 Hash */
    uint8_t key_hash[32];
    if (key_len > 64) {
        sha256_init(&ctx);
        sha256_update(&ctx, key, key_len);
        sha256_final(&ctx, key_hash);
        key = key_hash;
        key_len = 32;
    }

    memset(k_ipad, 0x36, 64);
    memset(k_opad, 0x5c, 64);
    for (i = 0; i < key_len; i++) {
        k_ipad[i] ^= key[i];
        k_opad[i] ^= key[i];
    }

    /* inner: SHA256(key XOR ipad || data) */
    sha256_init(&ctx);
    sha256_update(&ctx, k_ipad, 64);
    sha256_update(&ctx, data, data_len);
    sha256_final(&ctx, hash);

    /* outer: SHA256(key XOR opad || inner_hash) */
    sha256_init(&ctx);
    sha256_update(&ctx, k_opad, 64);
    sha256_update(&ctx, hash, 32);
    sha256_final(&ctx, out);

    memset(&ctx, 0, sizeof(ctx));
    memset(k_ipad, 0, 64);
    memset(k_opad, 0, 64);
}