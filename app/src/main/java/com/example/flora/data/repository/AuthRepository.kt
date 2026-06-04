package com.example.flora.data.repository

import android.content.SharedPreferences
import com.example.flora.data.local.dao.UserDao
import com.example.flora.data.local.entity.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthRepository(
    private val userDao: UserDao,
    private val prefs: SharedPreferences,
) {

    suspend fun register(name: String, email: String, password: String): Result<User> {
        val trimmedName = name.trim()
        val normalizedEmail = email.trim().lowercase()

        if (trimmedName.length < 2) {
            return Result.failure(Exception("Name must be at least 2 characters"))
        }
        if (!normalizedEmail.contains("@")) {
            return Result.failure(Exception("Enter a valid email address"))
        }
        if (password.length < 8) {
            return Result.failure(Exception("Password must be at least 8 characters"))
        }
        if (userDao.getUserByEmail(normalizedEmail) != null) {
            return Result.failure(Exception("An account with this email already exists"))
        }

        val passwordRecord = PasswordSecurity.createHash(password)
        val joinDate = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        val user = User(
            name = trimmedName,
            email = normalizedEmail,
            passwordHash = passwordRecord.hash,
            salt = passwordRecord.salt,
            joinDate = joinDate,
        )

        val newId = userDao.insertUser(user).toInt()
        val savedUser = userDao.getUserById(newId)
            ?: return Result.failure(Exception("Failed to retrieve saved user"))

        saveLoggedInUserId(savedUser.id)
        return Result.success(savedUser)
    }

    suspend fun login(email: String, password: String): Result<User> {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(normalizedEmail)
            ?: return Result.failure(Exception("No account found with this email"))

        if (!PasswordSecurity.verify(password, user.passwordHash, user.salt)) {
            return Result.failure(Exception("Incorrect password. Please try again."))
        }

        if (PasswordSecurity.needsUpgrade(user.passwordHash)) {
            val passwordRecord = PasswordSecurity.createHash(password)
            userDao.updateUser(
                user.copy(
                    passwordHash = passwordRecord.hash,
                    salt = passwordRecord.salt
                )
            )
        }

        saveLoggedInUserId(user.id)
        return Result.success(user)
    }

    suspend fun updateProfile(
        userId: Int,
        name: String,
        email: String,
        profileImageUri: String? = null
    ): Result<User> {
        val user = userDao.getUserById(userId)
            ?: return Result.failure(Exception("User not found"))

        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail != user.email && userDao.getUserByEmail(normalizedEmail) != null) {
            return Result.failure(Exception("This email is already in use by another account"))
        }

        val trimmedName = name.trim()
        if (trimmedName.length < 2) {
            return Result.failure(Exception("Name must be at least 2 characters"))
        }

        val updated = user.copy(
            name = trimmedName,
            email = normalizedEmail,
            profileImageUri = profileImageUri ?: user.profileImageUri
        )
        userDao.updateUser(updated)
        return Result.success(updated)
    }

    suspend fun updatePassword(
        userId: Int,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        val user = userDao.getUserById(userId)
            ?: return Result.failure(Exception("User not found"))

        if (!PasswordSecurity.verify(currentPassword, user.passwordHash, user.salt)) {
            return Result.failure(Exception("Current password is incorrect"))
        }
        if (newPassword.length < 8) {
            return Result.failure(Exception("New password must be at least 8 characters"))
        }

        val passwordRecord = PasswordSecurity.createHash(newPassword)
        userDao.updateUser(
            user.copy(
                passwordHash = passwordRecord.hash,
                salt = passwordRecord.salt
            )
        )
        return Result.success(Unit)
    }

    /**
     * Self-service offline password reset — the app has no email server, so the
     * "forgot password" flow lets the user set a new password once they prove
     * the account exists (by typing its email). Not a substitute for a real
     * email-verification reset, but enough for a local-first study app.
     */
    suspend fun resetPasswordByEmail(email: String, newPassword: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.contains("@")) {
            return Result.failure(Exception("Enter a valid email address"))
        }
        if (newPassword.length < 8) {
            return Result.failure(Exception("New password must be at least 8 characters"))
        }
        val user = userDao.getUserByEmail(normalizedEmail)
            ?: return Result.failure(Exception("No account found with this email"))

        val passwordRecord = PasswordSecurity.createHash(newPassword)
        userDao.updateUser(
            user.copy(
                passwordHash = passwordRecord.hash,
                salt = passwordRecord.salt
            )
        )
        return Result.success(Unit)
    }

    fun logout() {
        prefs.edit().remove("logged_in_user_id").apply()
    }

    private fun saveLoggedInUserId(id: Int) {
        prefs.edit().putInt("logged_in_user_id", id).apply()
    }

    fun getLoggedInUserId(): Int? {
        val id = prefs.getInt("logged_in_user_id", -1)
        return if (id == -1) null else id
    }

    suspend fun getLoggedInUser(): User? {
        val id = getLoggedInUserId() ?: return null
        return userDao.getUserById(id)
    }
}
