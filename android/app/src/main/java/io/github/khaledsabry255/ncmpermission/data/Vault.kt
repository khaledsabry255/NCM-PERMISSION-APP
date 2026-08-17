package io.github.khaledsabry255.ncmpermission.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The database key sealed with the PIN, byte-for-byte the same blob the web
 * app carries: PBKDF2-SHA256 (300k iterations) derives an AES-256 key, and the
 * payload is AES-GCM with the 16-byte tag appended — which is exactly the
 * layout WebCrypto produces and what javax.crypto expects.
 *
 * Changing the PIN means re-sealing this blob; editing the digits alone does
 * nothing, because the ciphertext simply stops decrypting.
 */
object Vault {
    private const val SALT = "1ZNV5xAf3NZ1xK1iZmAnSA=="
    private const val IV = "8Fn1FfSAB6PgT89a"
    private const val DATA =
        "WnVezfySqOjP/as8v7Qy1gBat4ogZnEmLwVjm8EgGDw3t8z1caZC5iFs9pQCsU1aeLDQoYcwCPgINotqnOhl" +
        "T1d1rvn7iZLC2LvidByLEBK0bt7Ol3Q6xpSxOc14s6YBGB85mstlpbzbUSub8ZE5V2UW5n+e+HQJfSzFxHyZ" +
        "O+Ku43kMV2mGc38T9eM39HN+m5jh3jIoHVFk86jnCrFYpwidRzLKaO47k1odbdxWj9xhCThjWIjPisB6bnNt" +
        "+vgHmAG2NbvVT7xCrtVFgxrM/g8Opv/ltrR7p235EuJ3EQ4="

    private const val ITERATIONS = 300_000
    private const val TAG_BITS = 128

    private fun decode(s: String): ByteArray = Base64.decode(s, Base64.DEFAULT)

    /** Returns the API key, or null when the PIN is wrong. */
    fun unlock(pin: String): String? = try {
        val spec = PBEKeySpec(pin.toCharArray(), decode(SALT), ITERATIONS, 256)
        val bits = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(bits, "AES"),
            GCMParameterSpec(TAG_BITS, decode(IV))
        )
        String(cipher.doFinal(decode(DATA)), Charsets.UTF_8)
    } catch (e: Exception) {
        null      // wrong PIN: GCM refuses to authenticate the payload
    }
}
