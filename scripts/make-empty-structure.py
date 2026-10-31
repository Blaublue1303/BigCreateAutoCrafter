"""Writes an empty (all-air) structure template used as GameTest arena.

Usage: python3 -I scripts/make-empty-structure.py <size> <output.nbt>
Format: vanilla structure NBT, DataVersion 3955 (Minecraft 1.21.1), gzip-compressed.
"""
import gzip
import struct
import sys

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def name(n):
    b = n.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def int_tag(n, v):
    return bytes([TAG_INT]) + name(n) + struct.pack(">i", v)


def string_payload(v):
    return name(v)


def list_tag(n, elem_type, payloads):
    return bytes([TAG_LIST]) + name(n) + bytes([elem_type]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def compound_payload(*entries):
    return b"".join(entries) + bytes([TAG_END])


def main():
    size = int(sys.argv[1])
    out = sys.argv[2]
    air = compound_payload(bytes([TAG_STRING]) + name("Name") + string_payload("minecraft:air"))
    root = bytes([TAG_COMPOUND]) + name("") + compound_payload(
        int_tag("DataVersion", 3955),
        list_tag("size", TAG_INT, [struct.pack(">i", size)] * 3),
        list_tag("palette", TAG_COMPOUND, [air]),
        list_tag("blocks", TAG_END, []),
        list_tag("entities", TAG_END, []),
    )
    with gzip.open(out, "wb") as f:
        f.write(root)


if __name__ == "__main__":
    main()
