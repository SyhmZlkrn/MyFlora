package com.example.flora.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.AuthViewModel

// Six avatar accent colours the user can pick from
private val avatarPalette = listOf(
    Color(0xFF7C83FD), // indigo
    Color(0xFF80DEEA), // teal
    Color(0xFF69F0AE), // mint
    Color(0xFFEF9A9A), // coral
    Color(0xFFCE93D8), // lavender
    Color(0xFFFDD835)  // amber
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    // Redirect guests — they should not reach this screen
    if (currentUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo("edit_profile") { inclusive = true }
            }
        }
        return
    }

    val context = LocalContext.current

    // ── Profile info state ────────────────────────────────────────────────
    var name  by remember { mutableStateOf(currentUser?.name  ?: "") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var selectedColorIdx by remember { mutableStateOf(0) }

    // ── Gallery photo picker ──────────────────────────────────────────────
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedImagePath by remember { mutableStateOf(currentUser?.profileImageUri) }

    // Load existing profile image from internal storage on entry
    if (profileBitmap == null && savedImagePath != null) {
        val file = File(savedImagePath!!)
        if (file.exists()) {
            profileBitmap = BitmapFactory.decodeFile(file.absolutePath)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                val bmp = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bmp != null) {
                    // Copy to internal storage so it persists
                    val file = File(context.filesDir, "profile_${currentUser?.id ?: 0}.jpg")
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    savedImagePath = file.absolutePath
                    profileBitmap = bmp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var profileSaving  by remember { mutableStateOf(false) }
    var profileMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // ── Password change state ─────────────────────────────────────────────
    var currentPw  by remember { mutableStateOf("") }
    var newPw      by remember { mutableStateOf("") }
    var confirmPw  by remember { mutableStateOf("") }
    var showCurrentPw  by remember { mutableStateOf(false) }
    var showNewPw      by remember { mutableStateOf(false) }
    var showConfirmPw  by remember { mutableStateOf(false) }
    var passwordSaving  by remember { mutableStateOf(false) }
    var passwordMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // ── Helpers ───────────────────────────────────────────────────────────
    fun passwordStrength(pw: String): Triple<Float, Color, String> = when {
        pw.length >= 12 && pw.any { it.isUpperCase() } && pw.any { it.isDigit() } ->
            Triple(1f, Palette.accent, "Strong")
        pw.length >= 8 ->
            Triple(0.6f, Palette.warn, "Fair")
        pw.isNotEmpty() ->
            Triple(0.3f, Palette.danger, "Weak")
        else -> Triple(0f, Color.Transparent, "")
    }

    val avatarColor = avatarPalette[selectedColorIdx]
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = Palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.textPrimary)
                    }
                },
                colors  = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = Space.section)
        ) {

            // ── Avatar section ────────────────────────────────────────────
            item {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(Space.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large avatar circle — tap to open gallery
                    // Outer Box is NOT clipped so the edit badge can overflow
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clickable { pickImageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        // Avatar circle (clipped)
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(avatarColor.copy(alpha = 0.90f), avatarColor.copy(alpha = 0.50f))
                                    )
                                )
                                .border(
                                    3.dp,
                                    Brush.verticalGradient(
                                        listOf(Palette.textPrimary.copy(alpha = 0.80f), Palette.textPrimary.copy(alpha = 0.25f))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileBitmap != null) {
                                Image(
                                    bitmap             = profileBitmap!!.asImageBitmap(),
                                    contentDescription = "Profile photo",
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(initial, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                            }
                        }
                        // Edit badge — sits outside the clipped circle at bottom-end
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp, bottom = 2.dp)
                                .size(Size.avatarSm - 6.dp)
                                .clip(CircleShape)
                                .background(Palette.accentAlt.copy(alpha = 0.95f))
                                .border(2.dp, Palette.textPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit photo",
                                tint               = Palette.textPrimary,
                                modifier           = Modifier.size(Size.iconSm)
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        "Tap to change photo",
                        fontSize = Type.caption,
                        color    = Palette.textTertiary
                    )
                    Spacer(Modifier.height(Space.md + 2.dp))
                    Text(
                        "Choose Avatar Color",
                        fontSize = Type.bodySmall,
                        color    = Palette.textTertiary
                    )
                    Spacer(Modifier.height(Space.sm + 2.dp))
                    // Color picker row
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm + 2.dp)) {
                        avatarPalette.forEachIndexed { idx, color ->
                            val isSelected = idx == selectedColorIdx
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 34.dp else 28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected)
                                            Modifier.border(2.dp, Palette.textPrimary, CircleShape)
                                        else
                                            Modifier.border(1.dp, Palette.textPrimary.copy(alpha = 0.30f), CircleShape)
                                    )
                                    .clickable { selectedColorIdx = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        null,
                                        tint     = Palette.textPrimary,
                                        modifier = Modifier.size(Size.iconMd - 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Profile info section ──────────────────────────────────────
            item {
                Text(
                    "PROFILE INFO",
                    fontSize    = Type.caption,
                    fontWeight  = FontWeight.Bold,
                    color       = Palette.textTertiary,
                    modifier    = Modifier.padding(start = Space.xl, bottom = Space.sm),
                    letterSpacing = 1.sp
                )
                GlassCard(
                    modifier     = Modifier.fillMaxWidth().padding(horizontal = Space.lg),
                    cornerRadius = Radii.card - 2.dp
                ) {
                    Column(modifier = Modifier.padding(Space.lg)) {

                        // Name field
                        GlassTextField(
                            value       = name,
                            onValueChange = { name = it },
                            label       = "Display Name",
                            leadingIcon = {
                                Icon(Icons.Filled.Person, null, tint = Palette.accentAlt, modifier = Modifier.size(Size.iconLg - 4.dp))
                            }
                        )
                        Spacer(Modifier.height(Space.md))

                        // Email field
                        GlassTextField(
                            value         = email,
                            onValueChange = { email = it },
                            label         = "Email Address",
                            keyboardType  = KeyboardType.Email,
                            leadingIcon   = {
                                Icon(Icons.Filled.Email, null, tint = Palette.accentAlt, modifier = Modifier.size(Size.iconLg - 4.dp))
                            }
                        )
                        Spacer(Modifier.height(Space.lg))

                        // Feedback banner
                        AnimatedVisibility(
                            visible = profileMessage != null,
                            enter   = expandVertically(),
                            exit    = shrinkVertically()
                        ) {
                            profileMessage?.let { (isSuccess, msg) ->
                                FeedbackBanner(isSuccess = isSuccess, message = msg)
                                Spacer(Modifier.height(Space.md))
                            }
                        }

                        // Save Profile button
                        GlassActionButton(
                            text     = if (profileSaving) "Saving…" else "Save Profile",
                            color    = Palette.accentAlt,
                            enabled  = !profileSaving,
                            onClick  = {
                                profileSaving  = true
                                profileMessage = null
                                authViewModel.updateProfile(name, email, savedImagePath) { error ->
                                    profileSaving  = false
                                    profileMessage = if (error == null)
                                        Pair(true,  "Profile updated successfully!")
                                    else
                                        Pair(false, error)
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(Space.xl)) }

            // ── Change password section ───────────────────────────────────
            item {
                Text(
                    "CHANGE PASSWORD",
                    fontSize    = Type.caption,
                    fontWeight  = FontWeight.Bold,
                    color       = Palette.textTertiary,
                    modifier    = Modifier.padding(start = Space.xl, bottom = Space.sm),
                    letterSpacing = 1.sp
                )
                GlassCard(
                    modifier     = Modifier.fillMaxWidth().padding(horizontal = Space.lg),
                    cornerRadius = Radii.card - 2.dp
                ) {
                    Column(modifier = Modifier.padding(Space.lg)) {

                        // Current password
                        GlassTextField(
                            value         = currentPw,
                            onValueChange = { currentPw = it },
                            label         = "Current Password",
                            isPassword    = !showCurrentPw,
                            leadingIcon   = {
                                Icon(Icons.Filled.Lock, null, tint = Color(0xFFCE93D8), modifier = Modifier.size(Size.iconLg - 4.dp))
                            },
                            trailingIcon  = {
                                IconButton(onClick = { showCurrentPw = !showCurrentPw }) {
                                    Icon(
                                        if (showCurrentPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        null,
                                        tint     = Palette.textTertiary,
                                        modifier = Modifier.size(Size.iconMd)
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(Space.md))

                        // New password
                        GlassTextField(
                            value         = newPw,
                            onValueChange = { newPw = it },
                            label         = "New Password",
                            isPassword    = !showNewPw,
                            leadingIcon   = {
                                Icon(Icons.Filled.Lock, null, tint = Palette.accentAlt, modifier = Modifier.size(Size.iconLg - 4.dp))
                            },
                            trailingIcon  = {
                                IconButton(onClick = { showNewPw = !showNewPw }) {
                                    Icon(
                                        if (showNewPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        null,
                                        tint     = Palette.textTertiary,
                                        modifier = Modifier.size(Size.iconMd)
                                    )
                                }
                            }
                        )

                        // Password strength indicator
                        if (newPw.isNotEmpty()) {
                            val (fraction, barColor, label) = passwordStrength(newPw)
                            Spacer(Modifier.height(Space.xs + 2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Palette.surfaceTintSoft)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(barColor)
                                    )
                                }
                                Spacer(Modifier.width(Space.sm))
                                Text(label, fontSize = Type.micro, color = barColor, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(Space.md))

                        // Confirm new password
                        val confirmError = confirmPw.isNotEmpty() && confirmPw != newPw
                        GlassTextField(
                            value         = confirmPw,
                            onValueChange = { confirmPw = it },
                            label         = "Confirm New Password",
                            isPassword    = !showConfirmPw,
                            isError       = confirmError,
                            errorText     = "Passwords do not match",
                            leadingIcon   = {
                                Icon(Icons.Filled.Lock, null, tint = Palette.accentAlt, modifier = Modifier.size(Size.iconLg - 4.dp))
                            },
                            trailingIcon  = {
                                IconButton(onClick = { showConfirmPw = !showConfirmPw }) {
                                    Icon(
                                        if (showConfirmPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        null,
                                        tint     = Palette.textTertiary,
                                        modifier = Modifier.size(Size.iconMd)
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(Space.lg))

                        // Feedback banner
                        AnimatedVisibility(
                            visible = passwordMessage != null,
                            enter   = expandVertically(),
                            exit    = shrinkVertically()
                        ) {
                            passwordMessage?.let { (isSuccess, msg) ->
                                FeedbackBanner(isSuccess = isSuccess, message = msg)
                                Spacer(Modifier.height(Space.md))
                            }
                        }

                        // Change password button
                        GlassActionButton(
                            text    = if (passwordSaving) "Updating…" else "Change Password",
                            color   = Color(0xFFCE93D8),
                            enabled = !passwordSaving && !confirmError &&
                                      currentPw.isNotEmpty() && newPw.isNotEmpty() && confirmPw.isNotEmpty(),
                            onClick = {
                                if (newPw != confirmPw) {
                                    passwordMessage = Pair(false, "Passwords do not match")
                                    return@GlassActionButton
                                }
                                passwordSaving  = true
                                passwordMessage = null
                                authViewModel.updatePassword(currentPw, newPw) { error ->
                                    passwordSaving = false
                                    if (error == null) {
                                        passwordMessage = Pair(true, "Password changed successfully!")
                                        currentPw = ""; newPw = ""; confirmPw = ""
                                    } else {
                                        passwordMessage = Pair(false, error)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(Space.sm)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composables
// ─────────────────────────────────────────────────────────────────────────────

/** Glass-styled outlined text field that matches the app's liquid-glass design. */
@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean    = false,
    isError: Boolean       = false,
    errorText: String      = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            label         = { Text(label, fontSize = Type.bodySmall) },
            leadingIcon   = leadingIcon,
            trailingIcon  = trailingIcon,
            isError       = isError,
            singleLine    = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
            shape         = RoundedCornerShape(Radii.chip + Space.xs),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedTextColor        = Palette.textPrimary,
                unfocusedTextColor      = Palette.textPrimary,
                focusedBorderColor      = Palette.textPrimary.copy(alpha = 0.70f),
                unfocusedBorderColor    = Palette.textPrimary.copy(alpha = 0.25f),
                errorBorderColor        = Palette.danger,
                focusedLabelColor       = Palette.textSecondary,
                unfocusedLabelColor     = Palette.textMuted,
                errorLabelColor         = Palette.danger,
                cursorColor             = Palette.textPrimary,
                focusedContainerColor   = Palette.surfaceTintSoft.copy(alpha = 0.08f),
                unfocusedContainerColor = Palette.surfaceTintSoft.copy(alpha = 0.05f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (isError && errorText.isNotEmpty()) {
            Text(
                text     = errorText,
                fontSize = Type.micro,
                color    = Palette.danger,
                modifier = Modifier.padding(start = Space.xs, top = 2.dp)
            )
        }
    }
}

/** A glass-styled primary action button. */
@Composable
private fun GlassActionButton(
    text: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.chip + Space.xs))
            .background(
                if (enabled)
                    Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.35f)))
                else
                    Brush.horizontalGradient(listOf(Palette.surfaceTintSoft, Palette.surfaceTintSoft.copy(alpha = 0.08f)))
            )
            .border(
                1.dp,
                if (enabled) color.copy(alpha = 0.60f) else Palette.surfaceTintSoft.copy(alpha = 0.15f),
                RoundedCornerShape(Radii.chip + Space.xs)
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = Space.md + 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            fontSize   = Type.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color      = if (enabled) Palette.textPrimary else Palette.textMuted
        )
    }
}

/** Small success / error banner shown below form fields. */
@Composable
private fun FeedbackBanner(isSuccess: Boolean, message: String) {
    val bgColor by animateColorAsState(
        targetValue   = if (isSuccess) Palette.accent.copy(alpha = 0.18f) else Palette.danger.copy(alpha = 0.18f),
        animationSpec = tween(300),
        label         = "bannerBg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSuccess) Palette.accent.copy(alpha = 0.55f) else Palette.danger.copy(alpha = 0.55f),
        animationSpec = tween(300),
        label         = "bannerBorder"
    )
    val textColor = if (isSuccess) Palette.accent else Palette.danger

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.chip))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(Radii.chip))
            .padding(horizontal = Space.md, vertical = Space.sm + 2.dp)
    ) {
        Text(message, fontSize = Type.bodySmall, color = textColor, fontWeight = FontWeight.Medium)
    }
}
