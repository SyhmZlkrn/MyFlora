package com.example.flora.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flora.data.local.AppDatabase
import com.example.flora.data.local.entity.User
import com.example.flora.data.repository.AuthRepository
import com.example.flora.notifications.FloraNotifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Represents the current authentication status. */
sealed class AuthState {
    object Loading       : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}

/**
 * Activity-scoped ViewModel that owns the authentication state.
 * Create it once in MainActivity and pass it (or observe its state) wherever needed.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val db   = AppDatabase.getInstance(application)
    private val prefs = application.getSharedPreferences("flora_prefs", Context.MODE_PRIVATE)
    val repository   = AuthRepository(db.userDao(), prefs)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init { checkSession() }

    // ── Session check ──────────────────────────────────────────────────────

    private fun checkSession() {
        viewModelScope.launch {
            val user = repository.getLoggedInUser()
            _currentUser.value = user
            _authState.value   = if (user != null) AuthState.Authenticated else AuthState.Unauthenticated
        }
    }

    // ── Auth actions ───────────────────────────────────────────────────────

    /**
     * Attempt to log in. [onResult] is called on the main thread:
     *   - null  → success (authState becomes Authenticated)
     *   - String → error message to show in the UI
     */
    fun login(email: String, password: String, onResult: (error: String?) -> Unit) {
        viewModelScope.launch {
            repository.login(email, password)
                .onSuccess { user ->
                    // Wipe any reminders left from previous account.
                    FloraNotifications.cancelAllCareReminders(getApplication())
                    _currentUser.value = user
                    _authState.value   = AuthState.Authenticated
                    onResult(null)
                }
                .onFailure { e ->
                    onResult(e.message ?: "Login failed. Please try again.")
                }
        }
    }

    /**
     * Attempt to register a new account. [onResult] follows the same contract as [login].
     */
    fun register(name: String, email: String, password: String, onResult: (error: String?) -> Unit) {
        viewModelScope.launch {
            repository.register(name, email, password)
                .onSuccess { user ->
                    FloraNotifications.cancelAllCareReminders(getApplication())
                    _currentUser.value = user
                    _authState.value   = AuthState.Authenticated
                    onResult(null)
                }
                .onFailure { e ->
                    onResult(e.message ?: "Registration failed. Please try again.")
                }
        }
    }

    /** Update the logged-in user's display name, email, and/or profile image. */
    fun updateProfile(name: String, email: String, profileImageUri: String? = null, onResult: (error: String?) -> Unit) {
        val userId = _currentUser.value?.id
            ?: return onResult("No user is currently logged in")
        viewModelScope.launch {
            repository.updateProfile(userId, name, email, profileImageUri)
                .onSuccess { updatedUser ->
                    _currentUser.value = updatedUser
                    onResult(null)
                }
                .onFailure { e -> onResult(e.message ?: "Profile update failed") }
        }
    }

    /** Change the logged-in user's password after verifying the current one. */
    fun updatePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (error: String?) -> Unit
    ) {
        val userId = _currentUser.value?.id
            ?: return onResult("No user is currently logged in")
        viewModelScope.launch {
            repository.updatePassword(userId, currentPassword, newPassword)
                .onSuccess { onResult(null) }
                .onFailure { e -> onResult(e.message ?: "Password update failed") }
        }
    }

    /**
     * Offline self-service password reset. Verifies the email exists, then writes
     * a new password hash. No email-link verification — fine for a local-only app.
     */
    fun resetPassword(email: String, newPassword: String, onResult: (error: String?) -> Unit) {
        viewModelScope.launch {
            repository.resetPasswordByEmail(email, newPassword)
                .onSuccess { onResult(null) }
                .onFailure { e -> onResult(e.message ?: "Password reset failed") }
        }
    }

    /** Log out the current user and clear the session. */
    fun logout() {
        repository.logout()
        // Clear notifications so the next user doesn't see this user's reminders.
        FloraNotifications.cancelAllCareReminders(getApplication())
        _currentUser.value = null
        _authState.value   = AuthState.Unauthenticated
    }
}
