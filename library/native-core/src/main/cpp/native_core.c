#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include "include/native_core.h"

/* ── JNI: NativeBridge.hmacSha256 ── */
JNIEXPORT jstring JNICALL
Java_run_yigou_gxzy_nativecore_NativeBridge_hmacSha256(
    JNIEnv *env, jclass clazz, jstring data, jstring key) {

    const char *data_utf = NULL, *key_utf = NULL;
    jstring result = NULL;
    uint8_t hmac_out[32];

    if (!data || !key) return NULL;

    data_utf = (*env)->GetStringUTFChars(env, data, NULL);
    key_utf = (*env)->GetStringUTFChars(env, key, NULL);
    if (!data_utf || !key_utf) goto cleanup;

    native_hmac_sha256(
        (const uint8_t *)key_utf, strlen(key_utf),
        (const uint8_t *)data_utf, strlen(data_utf),
        hmac_out);

    result = (*env)->NewStringUTF(env, native_base64_encode(hmac_out, 32));
    memset(hmac_out, 0, sizeof(hmac_out));

cleanup:
    if (data_utf) (*env)->ReleaseStringUTFChars(env, data, data_utf);
    if (key_utf) (*env)->ReleaseStringUTFChars(env, key, key_utf);
    return result;
}

/* ── native_sign_request ──
 * 构造 method + "\n" + host + "\n" + path + "\n" + timestamp + "\n" + nonce
 * → HMAC-SHA256 → Base64
 */
const char *native_sign_request(const char *method, const char *host,
    const char *path, const char *timestamp,
    const char *nonce, const char *secret) {

    static __thread char b64_buf[64];
    uint8_t hmac_out[32];
    char string_to_sign[1024];
    int written;

    written = snprintf(string_to_sign, sizeof(string_to_sign),
        "%s\n%s\n%s\n%s\n%s", method, host, path, timestamp, nonce);
    if (written < 0 || (size_t)written >= sizeof(string_to_sign))
        return NULL;

    native_hmac_sha256(
        (const uint8_t *)secret, strlen(secret),
        (const uint8_t *)string_to_sign, (size_t)written,
        hmac_out);

    const char *b64 = native_base64_encode(hmac_out, 32);
    if (!b64) {
        memset(hmac_out, 0, sizeof(hmac_out));
        return NULL;
    }
    strncpy(b64_buf, b64, sizeof(b64_buf) - 1);
    b64_buf[sizeof(b64_buf) - 1] = '\0';
    memset(hmac_out, 0, sizeof(hmac_out));
    return b64_buf;
}

/* ── JNI: NativeBridge.signRequest ── */
JNIEXPORT jstring JNICALL
Java_run_yigou_gxzy_nativecore_NativeBridge_signRequest(
    JNIEnv *env, jclass clazz,
    jstring method, jstring host, jstring path,
    jstring timestamp, jstring nonce, jstring secret) {

    const char *m = NULL, *h = NULL, *p = NULL;
    const char *ts = NULL, *n = NULL, *s = NULL;
    jstring result = NULL;

    if (!method || !host || !path || !timestamp || !nonce || !secret)
        return NULL;

    m = (*env)->GetStringUTFChars(env, method, NULL);
    h = (*env)->GetStringUTFChars(env, host, NULL);
    p = (*env)->GetStringUTFChars(env, path, NULL);
    ts = (*env)->GetStringUTFChars(env, timestamp, NULL);
    n = (*env)->GetStringUTFChars(env, nonce, NULL);
    s = (*env)->GetStringUTFChars(env, secret, NULL);
    if (!m || !h || !p || !ts || !n || !s) goto cleanup_sign;

    const char *sig = native_sign_request(m, h, p, ts, n, s);
    if (sig) {
        result = (*env)->NewStringUTF(env, sig);
    }

cleanup_sign:
    if (m) (*env)->ReleaseStringUTFChars(env, method, m);
    if (h) (*env)->ReleaseStringUTFChars(env, host, h);
    if (p) (*env)->ReleaseStringUTFChars(env, path, p);
    if (ts) (*env)->ReleaseStringUTFChars(env, timestamp, ts);
    if (n) (*env)->ReleaseStringUTFChars(env, nonce, n);
    if (s) (*env)->ReleaseStringUTFChars(env, secret, s);
    return result;
}

/*
 * Class:     run_yigou_gxzy_nativecore_NativeBridge
 * Method:    sm4Encrypt
 * Signature: ([B[B)[B
 *
 * SM4-CBC-PKCS7 加密，密钥由 native 层管理
 */
JNIEXPORT jbyteArray JNICALL
Java_run_yigou_gxzy_nativecore_NativeBridge_sm4Encrypt(
    JNIEnv *env, jclass clazz, jbyteArray data, jbyteArray iv) {

    jbyte *data_bytes = NULL;
    jbyteArray result = NULL;
    uint8_t key[16];
    uint8_t local_iv[16];
    uint8_t *out_buf = NULL;
    int in_len, out_len;

    if (!data) return NULL;

    data_bytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (!data_bytes) return NULL;
    in_len = (int)(*env)->GetArrayLength(env, data);

    /* 获取本地密钥（实际应从 key_store 获取并解混淆） */
    memcpy(key, native_get_sm4_key(), 16);
    if (iv) {
        jbyte *iv_bytes = (*env)->GetByteArrayElements(env, iv, NULL);
        if (iv_bytes) {
            memcpy(local_iv, iv_bytes, 16);
            (*env)->ReleaseByteArrayElements(env, iv, iv_bytes, JNI_ABORT);
        } else {
            memset(local_iv, 0, 16);
        }
    } else {
        /* 使用默认 IV */
        memcpy(local_iv, native_get_sm4_iv(), 16);
    }

    out_len = in_len + 32; /* 预留填充空间 */
    out_buf = (uint8_t *)malloc(out_len);
    if (!out_buf) goto cleanup;

    if (native_sm4_encrypt(key, 16, local_iv, 16,
            (const uint8_t *)data_bytes, in_len, out_buf, &out_len) == 0) {
        result = (*env)->NewByteArray(env, out_len);
        if (result) {
            (*env)->SetByteArrayRegion(env, result, 0, out_len, (jbyte *)out_buf);
        }
    }

    free(out_buf);

cleanup:
    if (data_bytes) {
        (*env)->ReleaseByteArrayElements(env, data, data_bytes, JNI_ABORT);
    }
    memset(key, 0, sizeof(key));
    return result;
}

/*
 * Class:     run_yigou_gxzy_nativecore_NativeBridge
 * Method:    sm2Encrypt
 * Signature: ([B)Ljava/lang/String;
 *
 * SM2 加密：公钥由 key_store.c 管理，通过 JNI 回调 Java 的
 * SM2CryptoUtil.encrypt() 完成实际加密，返回 Base64 密文字符串。
 */
JNIEXPORT jstring JNICALL
Java_run_yigou_gxzy_nativecore_NativeBridge_sm2Encrypt(
    JNIEnv *env, jclass clazz, jbyteArray data) {

    jbyte *data_bytes = NULL;
    jstring result = NULL;
    jclass sm2_class = NULL;
    jmethodID enc_method = NULL;
    jstring key_str = NULL;
    jstring data_str = NULL;
    const char *key_hex;
    int len;

    if (!data) return NULL;

    data_bytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (!data_bytes) return NULL;
    len = (int)(*env)->GetArrayLength(env, data);

    key_hex = native_get_sm2_public_key();
    if (!key_hex) goto cleanup;

    key_str = (*env)->NewStringUTF(env, key_hex);
    if (!key_str) goto cleanup;

    {
        char *plain_buf = (char *)malloc(len + 1);
        if (!plain_buf) goto cleanup;
        memcpy(plain_buf, data_bytes, len);
        plain_buf[len] = '\0';
        data_str = (*env)->NewStringUTF(env, plain_buf);
        free(plain_buf);
    }
    if (!data_str) goto cleanup;

    sm2_class = (*env)->FindClass(env, "run/yigou/gxzy/crypto/sm/SM2CryptoUtil");
    if (!sm2_class) goto cleanup;

    enc_method = (*env)->GetStaticMethodID(env, sm2_class, "encrypt",
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (!enc_method) goto cleanup;

    result = (jstring)(*env)->CallStaticObjectMethod(env, sm2_class, enc_method, key_str, data_str);

cleanup:
    if (data_bytes) {
        (*env)->ReleaseByteArrayElements(env, data, data_bytes, JNI_ABORT);
    }
    return result;
}

/*
 * Class:     run_yigou_gxzy_nativecore_NativeBridge
 * Method:    isDebuggerAttached
 * Signature: ()Z
 *
 * 通过 /proc/self/status TracerPid 检测调试器是否附加。
 */
JNIEXPORT jboolean JNICALL
Java_run_yigou_gxzy_nativecore_NativeBridge_isDebuggerAttached(
    JNIEnv *env, jclass clazz) {
    return (jboolean)native_is_debugger_attached();
}