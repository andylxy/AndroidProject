#!/usr/bin/env python3
"""
密钥混淆脚本 — 编译时运行，将明文密钥转换为 XOR 混淆后的 C 字节数组。
用法:
    python obfuscate_keys.py --key "HEX_KEY_16_BYTES" --iv "HEX_IV_16_BYTES" --sm2pub "HEX_SM2_PUBLIC_KEY"

输出:
    生成 .c 代码片段，可直接嵌入 key_store.c
    或覆写 key_store.c 中的 kObfuscated* 数组。

依赖:
    Python 3.6+（Android 构建环境通常自带）
"""

import argparse
import hashlib

# XOR 混淆密钥（16 字节，编译时可更换）
XOR_KEY = bytes([0xAB, 0xCD, 0xEF, 0x01, 0x23, 0x45, 0x67, 0x89,
                 0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10])


def obfuscate(plain_hex: str) -> bytes:
    """将 16 进制字符串解为字节后 XOR 混淆"""
    raw = bytes.fromhex(plain_hex)
    result = bytearray()
    for i, b in enumerate(raw):
        result.append(b ^ XOR_KEY[i % len(XOR_KEY)])
    return bytes(result)


def format_c_array(name: str, data: bytes, indent: int = 4) -> str:
    """格式化为 C 静态数组定义"""
    prefix = " " * indent
    hex_str = ", ".join(f"0x{b:02X}" for b in data)
    # 每 12 个字节换行
    lines = []
    items = hex_str.split(", ")
    for i in range(0, len(items), 12):
        chunk = ", ".join(items[i:i+12])
        lines.append(f"{prefix}{chunk},")
    body = "\n".join(lines)
    return f"static const uint8_t kObfuscated{name}[] = {{\n{body}\n}};"


def main():
    parser = argparse.ArgumentParser(description="密钥混淆编译脚本")
    parser.add_argument("--key", help="SM4 密钥（16 字节，32 位 16 进制）")
    parser.add_argument("--iv", help="SM4 IV（16 字节，32 位 16 进制）")
    parser.add_argument("--sm2pub", help="SM2 公钥（130 位 16 进制，含 04）")
    args = parser.parse_args()

    if args.key:
        assert len(bytes.fromhex(args.key)) == 16, "SM4 key must be 16 bytes"
        print(format_c_array("Sm4Key", obfuscate(args.key)))
        print()

    if args.iv:
        assert len(bytes.fromhex(args.iv)) == 16, "SM4 IV must be 16 bytes"
        print(format_c_array("Sm4Iv", obfuscate(args.iv)))
        print()

    if args.sm2pub:
        raw = bytes.fromhex(args.sm2pub)
        # SM2 公钥较长，拆分为两段输出
        mid = len(raw) // 2
        frag1 = bytes(b ^ XOR_KEY[i % len(XOR_KEY)] for i, b in enumerate(raw[:mid]))
        frag2 = bytes(b ^ XOR_KEY[(mid + i) % len(XOR_KEY)] for i, b in enumerate(raw[mid:]))
        print("/* SM2 公钥拆分为两段 + XOR 混淆 */")
        print(format_c_array("Sm2PubFrag1", frag1))
        print()
        print(format_c_array("Sm2PubFrag2", frag2))

    if not any([args.key, args.iv, args.sm2pub]):
        parser.print_help()


if __name__ == "__main__":
    main()