# -*- coding: utf-8 -*-
"""Build a valid gametest structure NBT for cbc_rs:empty.

Two bugs are fixed relative to the first attempt:
  1. gzip.open() double-compressed the payload. Minecraft reads gametest
     structures with NbtIo.readCompressed(), i.e. a single GZIP layer, so the
     nested stream produced "ReportedNbtException / EOFException".
  2. The first validator assumed list elements carry a type byte; NBT list
     elements are bare payloads. The reader below is written correctly and
     round-trips the payload before it is written to disk.
"""
import gzip
import struct
from pathlib import Path

PROJ = Path(r"C:\Users\Alan\Desktop\minecraft ALL\DSH Dev Region\createbigcannons-theroarofsteel")
OUT = PROJ / "src/main/resources/data/cbc_rs/structure/empty.nbt"

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11

DATA_VERSION_1_21_1 = 3955


def be_i32(v):
    return struct.pack(">i", v)


def utf(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def named(tag_type, name, payload):
    """A tagged entry inside a compound: <type><nameLen><name><payload>."""
    return bytes([tag_type]) + utf(name) + payload


def unnamed(tag_type, payload):
    """An entry inside a LIST: <payload> only.

    NBT list elements carry no name AND no per-element type byte - the element
    type is declared once in the list header. Passing a type byte here was the
    bug that made the payload unreadable.
    """
    return payload


def list_of(elem_type, payloads):
    return bytes([elem_type]) + be_i32(len(payloads)) + b"".join(payloads)


def build_payload():
    body = b""
    body += named(TAG_LIST, "size", list_of(TAG_INT, [be_i32(3)] * 3))
    body += named(TAG_LIST, "entities", list_of(TAG_COMPOUND, []))
    # A compound list element is the compound's entries followed by TAG_END.
    palette_entry = named(TAG_STRING, "Name", utf("minecraft:stone")) + bytes([TAG_END])
    body += named(TAG_LIST, "palette", list_of(TAG_COMPOUND, [palette_entry]))
    block = named(TAG_LIST, "pos", list_of(TAG_INT, [be_i32(1), be_i32(0), be_i32(1)]))
    block += named(TAG_INT, "state", be_i32(0))
    block += bytes([TAG_END])
    body += named(TAG_LIST, "blocks", list_of(TAG_COMPOUND, [block]))
    body += named(TAG_INT, "DataVersion", be_i32(DATA_VERSION_1_21_1))
    return bytes([TAG_COMPOUND]) + utf("") + body + bytes([TAG_END])


# ---------------------------------------------------------------- reader
def read_utf(buf, off):
    length = struct.unpack_from(">H", buf, off)[0]
    off += 2
    return buf[off:off + length].decode("utf-8"), off + length


def read_payload(buf, off, t):
    """Read a bare payload (no type byte, no name) of the given tag type."""
    if t == TAG_BYTE:
        return buf[off], off + 1
    if t == TAG_SHORT:
        return struct.unpack_from(">h", buf, off)[0], off + 2
    if t == TAG_INT:
        return struct.unpack_from(">i", buf, off)[0], off + 4
    if t == TAG_LONG:
        return struct.unpack_from(">q", buf, off)[0], off + 8
    if t == TAG_FLOAT:
        return struct.unpack_from(">f", buf, off)[0], off + 4
    if t == TAG_DOUBLE:
        return struct.unpack_from(">d", buf, off)[0], off + 8
    if t == TAG_STRING:
        return read_utf(buf, off)
    if t == TAG_BYTE_ARRAY:
        n = struct.unpack_from(">i", buf, off)[0]
        off += 4
        return list(buf[off:off + n]), off + n
    if t == TAG_INT_ARRAY:
        n = struct.unpack_from(">i", buf, off)[0]
        off += 4
        vals = list(struct.unpack_from(">{}i".format(n), buf, off)) if n else []
        return vals, off + 4 * n
    if t == TAG_LIST:
        elem = buf[off]
        n = struct.unpack_from(">i", buf, off + 1)[0]
        off += 5
        items = []
        for _ in range(n):
            # List elements are bare payloads of the declared element type.
            v, off = read_payload(buf, off, elem)
            items.append(v)
        return {"__list_type__": elem, "items": items}, off
    if t == TAG_COMPOUND:
        result = {}
        while True:
            child = buf[off]
            if child == TAG_END:
                return result, off + 1
            name, off = read_utf(buf, off + 1)
            v, off = read_payload(buf, off, child)
            result[name] = v
    raise ValueError("unhandled tag type {}".format(t))


def read_root(buf):
    t = buf[0]
    if t != TAG_COMPOUND:
        raise ValueError("root is not a compound: {}".format(t))
    _, off = read_utf(buf, 1)
    return read_payload(buf, off, TAG_COMPOUND)


payload = build_payload()
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(gzip.compress(payload, mtime=0))

raw = OUT.read_bytes()
decompressed = gzip.decompress(raw)
value, consumed = read_root(decompressed)
assert consumed == len(decompressed), "trailing bytes {} vs {}".format(consumed, len(decompressed))
assert value["size"] == {"__list_type__": TAG_INT, "items": [3, 3, 3]}
assert value["palette"]["items"][0]["Name"] == "minecraft:stone"
assert value["blocks"]["items"][0]["pos"]["items"] == [1, 0, 1]
assert value["blocks"]["items"][0]["state"] == 0
assert value["DataVersion"] == DATA_VERSION_1_21_1
assert value["entities"]["items"] == []

print("wrote", OUT.relative_to(PROJ))
print("gzip bytes:", len(raw), "| nbt bytes:", len(decompressed))
print("round-trip OK ->", value)
