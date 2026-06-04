package com.example.flora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flora.ui.components.FloraSecondaryButton
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.AuthViewModel

// Glass-styled text-field colours shared across auth screens
private val glassTextFieldColors: @Composable () -> androidx.compose.material3.TextFieldColors = {
    OutlinedTextFieldDefaults.colors(
        focusedTextColor          = Palette.textPrimary,
        unfocusedTextColor        = Palette.textSecondary,
        focusedBorderColor        = Palette.textPrimary.copy(alpha = 0.70f),
        unfocusedBorderColor      = Palette.textPrimary.copy(alpha = 0.30f),
        focusedLabelColor         = Palette.textPrimary.copy(alpha = 0.90f),
        unfocusedLabelColor       = Palette.textTertiary,
        cursorColor               = Palette.textPrimary,
        focusedLeadingIconColor   = Palette.textPrimary.copy(alpha = 0.90f),
        unfocusedLeadingIconColor = Palette.textTertiary,
        focusedTrailingIconColor  = Palette.textSecondary,
        unfocusedTrailingIconColor = Palette.textMuted,
        focusedContainerColor     = Palette.surfaceTintSoft.copy(alpha = 0.08f),
        unfocusedContainerColor   = Palette.surfaceTintSoft.copy(alpha = 0.05f),
        errorBorderColor          = Palette.danger,
        errorLabelColor           = Palette.danger,
        errorTextColor            = Palette.textPrimary,
        errorCursorColor          = Palette.danger,
        errorSupportingTextColor  = Palette.danger,
        errorLeadingIconColor     = Palette.danger,
    )
}

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError      by remember { mutableStateOf("") }
    var passwordError   by remember { mutableStateOf("") }
    var generalError    by remember { mutableStateOf("") }
    var isLoading       by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    if (showForgotDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            authViewModel = authViewModel,
            onDismiss = { showForgotDialog = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Logo section (brand image — leaf + wordmark baked in) ────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 56.dp, bottom = Space.section + Space.xs)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.example.flora.R.drawable.logo_myflora),
                    contentDescription = "MyFlora",
                    modifier = Modifier.size(200.dp),
                )
                Text(text = "Welcome back!", fontSize = Type.bodyLarge, color = Palette.textSecondary)
            }

            // ── Glass login card ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet))
                    .background(Brush.verticalGradient(listOf(Palette.surfaceTintStrong, Palette.surfaceTintSoft)))
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Palette.textPrimary.copy(alpha = 0.55f), Palette.textPrimary.copy(alpha = 0.15f))),
                        RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet)
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Space.xxl + Space.xs),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(Space.xs))
                    Text("Sign In", fontSize = Type.displayMedium, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                    Text(
                        "Enter your credentials to continue",
                        fontSize = Type.bodyMedium,
                        color = Palette.textTertiary,
                        modifier = Modifier.padding(top = Space.xs, bottom = Space.xl)
                    )

                    // General auth error banner
                    if (generalError.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radii.chip))
                                .background(Palette.danger.copy(alpha = 0.18f))
                                .border(1.dp, Palette.danger.copy(alpha = 0.40f), RoundedCornerShape(Radii.chip))
                                .padding(Space.md)
                        ) {
                            Text(
                                generalError,
                                color = Palette.danger,
                                fontSize = Type.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(Space.lg))
                    }

                    // Email field
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it; emailError = ""; generalError = "" },
                        label         = { Text("Email Address") },
                        leadingIcon   = { Icon(Icons.Filled.Email, contentDescription = null) },
                        isError       = emailError.isNotEmpty(),
                        supportingText = if (emailError.isNotEmpty()) {
                            { Text(emailError, color = Palette.danger) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(Radii.chip + Space.xs),
                        colors        = glassTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(Space.lg))

                    // Password field
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it; passwordError = ""; generalError = "" },
                        label         = { Text("Password") },
                        leadingIcon   = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon  = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError        = passwordError.isNotEmpty(),
                        supportingText = if (passwordError.isNotEmpty()) {
                            { Text(passwordError, color = Palette.danger) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(Radii.chip + Space.xs),
                        colors         = glassTextFieldColors()
                    )

                    Row(modifier = Modifier.fillMaxWidth().padding(top = Space.sm), horizontalArrangement = Arrangement.End) {
                        Text(
                            "Forgot Password?",
                            fontSize = Type.bodySmall,
                            color = Palette.textSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { showForgotDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(Space.section - Space.xs))

                    // Sign In button — calls real auth (Button kept to host loading spinner)
                    Button(
                        onClick = {
                            var valid = true
                            if (email.isBlank())          { emailError    = "Email is required";                          valid = false }
                            else if (!email.contains("@")){ emailError    = "Enter a valid email address";                valid = false }
                            if (password.isBlank())       { passwordError = "Password is required";                       valid = false }
                            if (!valid) return@Button

                            isLoading = true
                            generalError = ""
                            authViewModel.login(email.trim(), password) { error ->
                                isLoading = false
                                if (error == null) {
                                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                                } else {
                                    generalError = error
                                }
                            }
                        },
                        enabled  = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(Size.buttonPrimary),
                        shape    = RoundedCornerShape(Radii.chip + Space.xs),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Palette.accent.copy(alpha = 0.30f),
                            disabledContainerColor = Palette.surfaceTintSoft
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Palette.textPrimary,
                                modifier = Modifier.size(Size.iconLg - 2.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Sign In", fontSize = Type.titleSmall, fontWeight = FontWeight.SemiBold, color = Palette.accent)
                        }
                    }

                    Spacer(modifier = Modifier.height(Space.xl))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Palette.textPrimary.copy(alpha = 0.25f)))
                        Text(text = "  OR  ", fontSize = Type.caption, color = Palette.textTertiary)
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Palette.textPrimary.copy(alpha = 0.25f)))
                    }

                    Spacer(modifier = Modifier.height(Space.xl))

                    FloraSecondaryButton(
                        label = "Continue as Guest",
                        onClick = {
                            authViewModel.logout()
                            navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                        }
                    )

                    Spacer(modifier = Modifier.height(Space.xxl))

                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Don't have an account? ", fontSize = Type.bodyMedium, color = Palette.textTertiary)
                        Text(
                            "Register",
                            fontSize = Type.bodyMedium,
                            color = Palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { navController.navigate("register") }
                        )
                    }

                    Spacer(modifier = Modifier.height(Space.xl))
                }
            }
        }
    }
}

/**
 * Offline self-service password reset dialog. The app has no email server, so
 * "Forgot password" verifies the user knows the account email and lets them set
 * a new password locally. Not a real out-of-band reset, but enough for a
 * single-device study app.
 */
@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
) {
    var resetEmail by remember { mutableStateOf(initialEmail) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPwVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (success) "Password updated" else "Reset password",
                fontWeight = FontWeight.Bold,
                color = Palette.textPrimary,
            )
        },
        text = {
            if (success) {
                Text(
                    "Your password has been updated. Sign in with the new password.",
                    color = Palette.textSecondary,
                    fontSize = Type.bodySmall,
                )
            } else {
                Column {
                    Text(
                        "Enter the email on your account and choose a new password (min 8 characters).",
                        color = Palette.textTertiary,
                        fontSize = Type.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(Space.md))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it; error = "" },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Filled.Email, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.chip + Space.xs),
                        colors = glassTextFieldColors(),
                    )
                    Spacer(modifier = Modifier.height(Space.md))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; error = "" },
                        label = { Text("New Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { newPwVisible = !newPwVisible }) {
                                Icon(if (newPwVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                            }
                        },
                        visualTransformation = if (newPwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.chip + Space.xs),
                        colors = glassTextFieldColors(),
                    )
                    Spacer(modifier = Modifier.height(Space.md))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = "" },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        visualTransformation = if (newPwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radii.chip + Space.xs),
                        colors = glassTextFieldColors(),
                    )
                    if (error.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Space.sm))
                        Text(error, color = Palette.danger, fontSize = Type.caption)
                    }
                }
            }
        },
        confirmButton = {
            if (success) {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = Palette.accent, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    enabled = !submitting,
                    onClick = {
                        when {
                            resetEmail.isBlank() -> error = "Email is required"
                            !resetEmail.contains("@") -> error = "Enter a valid email address"
                            newPassword.length < 8 -> error = "Password must be at least 8 characters"
                            newPassword != confirmPassword -> error = "Passwords do not match"
                            else -> {
                                submitting = true
                                authViewModel.resetPassword(resetEmail, newPassword) { err ->
                                    submitting = false
                                    if (err == null) success = true else error = err
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (submitting) "Updating…" else "Reset",
                        color = Palette.accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            if (!success) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Palette.textTertiary)
                }
            }
        },
    )
}
