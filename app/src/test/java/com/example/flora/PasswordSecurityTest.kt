package com.example.flora

import com.example.flora.data.repository.PasswordSecurity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordSecurityTest {

    @Test
    fun createHash_verifiesCorrectPassword() {
        val record = PasswordSecurity.createHash("SuperSecure123")

        assertTrue(PasswordSecurity.verify("SuperSecure123", record.hash, record.salt))
    }

    @Test
    fun createHash_rejectsWrongPassword() {
        val record = PasswordSecurity.createHash("SuperSecure123")

        assertFalse(PasswordSecurity.verify("WrongPassword", record.hash, record.salt))
    }

    @Test
    fun createHash_usesRandomSalt() {
        val first = PasswordSecurity.createHash("SuperSecure123")
        val second = PasswordSecurity.createHash("SuperSecure123")

        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
    }
}
