cmake_minimum_required(VERSION 3.22.1)
project("native_core")

add_library(native_core SHARED
    native_core.c
    key_store.c
    sm4_impl.c
    sha256_hmac.c
    base64.c
)

target_include_directories(native_core PRIVATE include)