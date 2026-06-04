package com.example.flora.data.repository

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordSecurity {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_BYTES = 16

    private val secureRandom = SecureRandom()
    private val legacyHashRegex = Regex("^[a-f0-9]{64}$")

    data class PasswordRecord(
        val hash: String,
        val salt: String
    )

    fun createHash(password: String): PasswordRecord {
        val saltBytes = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val salt = Base64.getEncoder().encodeToString(saltBytes)
        return PasswordRecord(
            hash = pbkdf2(password, saltBytes),
            salt = salt
        )
    }

    fun verify(password: String, storedHash: String, storedSalt: String): Boolean {
        if (needsUpgrade(storedHash)) {
            return verifyLegacy(password, storedSalt, storedHash)
        }

        return try {
            val derived = pbkdf2(password, Base64.getDecoder().decode(storedSalt))
            MessageDigest.isEqual(
                derived.toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8)
            )
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun needsUpgrade(storedHash: String): Boolean = legacyHashRegex.matches(storedHash)

    private fun verifyLegacy(password: String, salt: String, storedHash: String): Boolean {
        val legacyHash = MessageDigest.getInstance("SHA-256")
            .digest((password + salt).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(
            legacyHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    private fun pbkdf2(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return SecretKeyFactory.getInstance(ALGORITHM)
            .generateSecret(spec)
            .encoded
            .let(Base64.getEncoder()::encodeToString)
    }
}
