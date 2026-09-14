# -*- coding: utf-8 -*-
"""Prints the SHA-256 of the signing certificate(s) inside an APK's v2/v3
signing block, so two APKs can be compared without apksigner or a JDK."""
import struct, hashlib, sys, zipfile

V2, V3, V31 = 0x7109871A, 0xF05368C0, 0x1B93AD61

def u32(b, o): return struct.unpack_from('<I', b, o)[0]
def u64(b, o): return struct.unpack_from('<Q', b, o)[0]

def seq(b, o, end):
    """Yield (start, length) of each uint32-length-prefixed item in [o, end)."""
    while o < end:
        n = u32(b, o); yield o + 4, n; o += 4 + n

def certs(path):
    d = open(path, 'rb').read()
    eocd = d.rfind(b'PK\x05\x06')
    cd_off = u32(d, eocd + 16)
    if d[cd_off - 16:cd_off] != b'APK Sig Block 42':
        return {}
    size = u64(d, cd_off - 24)
    start = cd_off - size - 8
    o, end = start + 8, cd_off - 24
    found = {}
    while o < end:
        ln = u64(d, o); pid = u32(d, o + 8); val = o + 12; vend = o + 8 + ln
        if pid in (V2, V3, V31):
            name = {V2: 'v2', V3: 'v3', V31: 'v3.1'}[pid]
            (sgs, sgn), = [x for x in seq(d, val, vend)][:1]
            for s, n in seq(d, sgs, sgs + sgn):            # each signer
                sd, sdn = next(seq(d, s, s + n))           # signed data
                p = sd
                dg = u32(d, p); p += 4 + dg                # digests
                cl = u32(d, p); p += 4                     # certificates
                for cs, cn in seq(d, p, p + cl):
                    found.setdefault(name, []).append(hashlib.sha256(d[cs:cs + cn]).hexdigest())
        o = vend
    return found

for path in sys.argv[1:]:

    print(path)
    for k, v in certs(path).items():
        for h in v: print('   %-5s %s' % (k, h))
