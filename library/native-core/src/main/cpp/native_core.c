#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include "include/native_core.h"

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