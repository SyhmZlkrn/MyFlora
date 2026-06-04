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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.AuthViewModel

private val glassFieldColors: @Composable () -> androidx.compose.material3.TextFieldColors = {
    OutlinedTextFieldDefaults.colors(
        focusedTextColor           = Palette.textPrimary,
        unfocusedTextColor         = Palette.textSecondary,
        focusedBorderColor         = Palette.textPrimary.copy(alpha = 0.70f),
        unfocusedBorderColor       = Palette.textPrimary.copy(alpha = 0.30f),
        focusedLabelColor          = Palette.textPrimary.copy(alpha = 0.90f),
        unfocusedLabelColor        = Palette.textTertiary,
        cursorColor                = Palette.textPrimary,
        focusedLeadingIconColor    = Palette.textPrimary.copy(alpha = 0.90f),
        unfocusedLeadingIconColor  = Palette.textTertiary,
        focusedTrailingIconColor   = Palette.textSecondary,
        unfocusedTrailingIconColor = Palette.textMuted,
        focusedContainerColor      = Palette.surfaceTintSoft.copy(alpha = 0.08f),
        unfocusedContainerColor    = Palette.surfaceTintSoft.copy(alpha = 0.05f),
        errorBorderColor           = Palette.danger,
        errorLabelColor            = Palette.danger,
        errorTextColor             = Palette.textPrimary,
        errorCursorColor           = Palette.danger,
        errorSupportingTextColor   = Palette.danger,
        errorLeadingIconColor      = Palette.danger,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, authViewModel: AuthViewModel) {
    var fullName               by remember { mutableStateOf("") }
    var email                  by remember { mutableStateOf("") }
    var password               by remember { mutableStateOf("") }
    var confirmPassword        by remember { mutableStateOf("") }
    var passwordVisible        by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var nameError              by remember { mutableStateOf("") }
    var emailError             by remember { mutableStateOf("") }
    var passwordError          by remember { mutableStateOf("") }
    var confirmPasswordError   by remember { mutableStateOf("") }
    var generalError           by remember { mutableStateOf("") }
    var isLoading              by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontWeight = FontWeight.Bold, color = Palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Palette.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Space.xl, vertical = Space.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radii.sheet))
                    .background(Brush.verticalGradient(listOf(Palette.surfaceTintStrong, Palette.surfaceTintSoft)))
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Palette.textPrimary.copy(alpha = 0.55f), Palette.textPrimary.copy(alpha = 0.15f))),
                        RoundedCornerShape(Radii.sheet)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(Space.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Join Flora", fontSize = Type.titleLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                    Text(
                        "Create your plant care account",
                        fontSize = Type.bodyMedium,
                        color = Palette.textTertiary,
                        modifier = Modifier.padding(top = Space.xs, bottom = Space.xl)
                    )

                    // General error banner
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

                    OutlinedTextField(
                        value = fullName, onValueChange = { fullName = it; nameError = ""; generalError = "" },
                        label = { Text("Full Name") }, leadingIcon = { Icon(Icons.Filled.Person, null) },
                        isError = nameError.isNotEmpty(),
                        supportingText = if (nameError.isNotEmpty()) { { Text(nameError, color = Palette.danger) } } else null,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radii.chip + Space.xs), colors = glassFieldColors()
                    )
                    Spacer(modifier = Modifier.height(Space.md))

                    OutlinedTextField(
                        value = email, onValueChange = { email = it; emailError = ""; generalError = "" },
                        label = { Text("Email Address") }, leadingIcon = { Icon(Icons.Filled.Email, null) },
                        isError = emailError.isNotEmpty(),
                        supportingText = if (emailError.isNotEmpty()) { { Text(emailError, color = Palette.danger) } } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radii.chip + Space.xs), colors = glassFieldColors()
                    )
                    Spacer(modifier = Modifier.height(Space.md))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it; passwordError = ""; generalError = "" },
                        label = { Text("Password") }, leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError.isNotEmpty(),
                        supportingText = if (passwordError.isNotEmpty()) { { Text(passwordError, color = Palette.danger) } } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radii.chip + Space.xs), colors = glassFieldColors()
                    )
                    Spacer(modifier = Modifier.height(Space.md))

                    OutlinedTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it; confirmPasswordError = ""; generalError = "" },
                        label = { Text("Confirm Password") }, leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = confirmPasswordError.isNotEmpty(),
                        supportingText = if (confirmPasswordError.isNotEmpty()) { { Text(confirmPasswordError, color = Palette.danger) } } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radii.chip + Space.xs), colors = glassFieldColors()
                    )

                    Spacer(modifier = Modifier.height(Space.section - Space.xs))

                    Button(
                        onClick = {
                            var valid = true
                            if (fullName.isBlank())         { nameError            = "Name is required";                       valid = false }
                            if (email.isBlank())            { emailError           = "Email is required";                      valid = false }
                            else if (!email.contains("@")) { emailError           = "Enter a valid email address";             valid = false }
                            if (password.isBlank())         { passwordError        = "Password is required";                   valid = false }
                            else if (password.length < 8)  { passwordError        = "Password must be at least 8 characters"; valid = false }
                            if (confirmPassword != password){ confirmPasswordError = "Passwords do not match";                 valid = false }
                            if (!valid) return@Button

                            isLoading    = true
                            generalError = ""
                            authViewModel.register(fullName.trim(), email.trim(), password) { error ->
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
                            CircularProgressIndicator(color = Palette.textPrimary, modifier = Modifier.size(Size.iconLg - 2.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create Account", fontSize = Type.titleSmall, fontWeight = FontWeight.SemiBold, color = Palette.accent)
                        }
                    }

                    Spacer(modifier = Modifier.height(Space.xl))
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Already have an account? ", fontSize = Type.bodyMedium, color = Palette.textTertiary)
                        Text(
                            "Sign In",
                            fontSize = Type.bodyMedium,
                            color = Palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { navController.popBackStack() }
                        )
                    }
                    Spacer(modifier = Modifier.height(Space.sm))
                }
            }
            Spacer(modifier = Modifier.height(Space.xxl))
        }
    }
}