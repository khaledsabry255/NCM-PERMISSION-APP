# -*- coding: utf-8 -*-
"""Re-signs an APK with APK Signature Scheme v2 using a PKCS#12 key, then
verifies the result independently (digests recomputed from the output file,
signature checked against the embedded certificate).

    python apksign.py in.apk out.apk key.p12 password-file

v2 alone is enough here: minSdk is 24 (Android 7.0, where v2 arrived) and
Android accepts v2 for targetSdk 30+. Any existing signing block is dropped
and rebuilt, so a CI build signed with a throwaway debug key comes out carrying
only the permanent key.
"""
import hashlib, struct, sys
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, padding, rsa
from cryptography.hazmat.primitives.serialization import pkcs12

MAGIC = b'APK Sig Block 42'
V2_ID = 0x7109871A
CHUNK = 1024 * 1024
ALG_RSA_SHA256 = 0x0103          # RSASSA-PKCS1-v1_5 with SHA2-256
ALG_ECDSA_SHA256 = 0x0201        # ECDSA with SHA2-256

u32 = lambda n: struct.pack('<I', n)
u64 = lambda n: struct.pack('<Q', n)
lp = lambda b: u32(len(b)) + b   # uint32 length-prefixed


def zip_layout(data):
    eocd = data.rfind(b'PK\x05\x06')
    if eocd < 0 or len(data) - eocd > 65535 + 22:
        raise SystemExit('no end-of-central-directory record')
    cd_off = struct.unpack_from('<I', data, eocd + 16)[0]
    entries_end = cd_off
    if data[cd_off - 16:cd_off] == MAGIC:                 # an old signing block
        blk = struct.unpack_from('<Q', data, cd_off - 24)[0]
        entries_end = cd_off - blk - 8
        if struct.unpack_from('<Q', data, entries_end)[0] != blk:
            raise SystemExit('malformed existing signing block')
    return entries_end, cd_off, eocd


def chunked_digest(sections):
    digests, count = [], 0
    for sec in sections:
        for i in range(0, len(sec), CHUNK):
            chunk = sec[i:i + CHUNK]
            digests.append(hashlib.sha256(b'\xa5' + u32(len(chunk)) + chunk).digest())
            count += 1
    return hashlib.sha256(b'\x5a' + u32(count) + b''.join(digests)).digest()


def sections_for_digest(entries, cd, eocd_rec):
    # The EOCD is digested as if the central directory started right after the
    # entries - i.e. as if the signing block were not there.
    e = bytearray(eocd_rec)
    struct.pack_into('<I', e, 16, len(entries))
    return [entries, cd, bytes(e)]


def sign(src, dst, p12_path, pw_path):
    data = open(src, 'rb').read()
    entries_end, cd_off, eocd = zip_layout(data)
    entries, cd, eocd_rec = data[:entries_end], data[cd_off:eocd], data[eocd:]

    pw = open(pw_path, 'rb').read().splitlines()[0].strip()
    key, cert, _ = pkcs12.load_key_and_certificates(open(p12_path, 'rb').read(), pw)
    pw = None
    cert_der = cert.public_bytes(serialization.Encoding.DER)
    spki = key.public_key().public_bytes(serialization.Encoding.DER,
                                         serialization.PublicFormat.SubjectPublicKeyInfo)
    if isinstance(key, rsa.RSAPrivateKey):
        alg = ALG_RSA_SHA256
        do_sign = lambda b: key.sign(b, padding.PKCS1v15(), hashes.SHA256())
    elif isinstance(key, ec.EllipticCurvePrivateKey):
        alg = ALG_ECDSA_SHA256
        do_sign = lambda b: key.sign(b, ec.ECDSA(hashes.SHA256()))
    else:
        raise SystemExit('unsupported key type')

    digest = chunked_digest(sections_for_digest(entries, cd, eocd_rec))
    signed_data = (lp(lp(u32(alg) + lp(digest)))          # digests
                   + lp(lp(cert_der))                     # certificates
                   + lp(b'')                              # additional attributes
                   + u32(0))                              # reserved, as apksigner writes it
    signer = lp(signed_data) + lp(lp(u32(alg) + lp(do_sign(signed_data)))) + lp(spki)
    v2_value = lp(lp(signer))

    pair = u64(4 + len(v2_value)) + u32(V2_ID) + v2_value
    size = len(pair) + 8 + len(MAGIC)
    block = u64(size) + pair + u64(size) + MAGIC

    new_eocd = bytearray(eocd_rec)
    struct.pack_into('<I', new_eocd, 16, len(entries) + len(block))
    open(dst, 'wb').write(entries + block + cd + bytes(new_eocd))
    return hashlib.sha256(cert_der).hexdigest()


def verify(path):
    """Reads the file back from disk and checks it the way a verifier would."""
    data = open(path, 'rb').read()
    entries_end, cd_off, eocd = zip_layout(data)
    assert data[cd_off - 16:cd_off] == MAGIC, 'no signing block'
    blk = struct.unpack_from('<Q', data, cd_off - 24)[0]
    o, end = entries_end + 8, cd_off - 24
    v2 = None
    while o < end:
        ln = struct.unpack_from('<Q', data, o)[0]
        pid = struct.unpack_from('<I', data, o + 8)[0]
        if pid == V2_ID: v2 = data[o + 12:o + 8 + ln]
        o += 8 + ln
    assert v2 is not None, 'no v2 block'

    def items(b):
        out, p = [], 0
        while p < len(b):
            n = struct.unpack_from('<I', b, p)[0]; out.append(b[p + 4:p + 4 + n]); p += 4 + n
        return out

    (signers,) = items(v2)
    (signer,) = items(signers)
    signed_data, sigs, spki = items(signer)
    dg_seq, cert_seq, _attrs = items(signed_data)[:3]   # apksigner may append a reserved field
    (dg,) = items(dg_seq); alg = struct.unpack_from('<I', dg)[0]; (want,) = items(dg[4:])
    (cert_der,) = items(cert_seq)
    (sg,) = items(sigs); salg = struct.unpack_from('<I', sg)[0]; (sig,) = items(sg[4:])
    assert alg == salg, 'digest and signature algorithms differ'

    got = chunked_digest(sections_for_digest(
        data[:entries_end], data[cd_off:eocd], data[eocd:]))
    assert got == want, 'content digest does not match'

    from cryptography import x509
    pub = x509.load_der_x509_certificate(cert_der).public_key()
    assert pub.public_bytes(serialization.Encoding.DER,
                            serialization.PublicFormat.SubjectPublicKeyInfo) == spki, \
        'public key does not match certificate'
    if alg == ALG_RSA_SHA256:
        pub.verify(sig, signed_data, padding.PKCS1v15(), hashes.SHA256())
    else:
        pub.verify(sig, signed_data, ec.ECDSA(hashes.SHA256()))
    return hashlib.sha256(cert_der).hexdigest()


if __name__ == '__main__':
    src, dst, p12, pwf = sys.argv[1:5]
    signed_with = sign(src, dst, p12, pwf)
    checked = verify(dst)
    print('signed   :', signed_with)
    print('verified :', checked)
    print('OK' if signed_with == checked else 'MISMATCH')
