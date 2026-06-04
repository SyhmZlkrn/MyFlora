package com.example.flora.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.Icons
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flora.ui.components.GlassBox
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type

import com.example.flora.ui.viewmodel.AuthViewModel
import com.example.flora.ui.viewmodel.CareScheduleViewModel
import com.example.flora.ui.viewmodel.PlantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    isDarkMode: Boolean,
    onDarkModeToggle: () -> Unit,
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel,
    careScheduleViewModel: CareScheduleViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isGuest = currentUser == null

    // Signed-in user data
    val displayName  = currentUser?.name  ?: "Guest"
    val displayEmail = currentUser?.email ?: "guest@flora.app"
    val displayJoin  = if (currentUser?.joinDate?.isNotEmpty() == true)
        "Member since ${currentUser!!.joinDate}"
    else ""

    // Real stats from database
    val plants by plantViewModel.plants.collectAsState()
    val activeDiseases by plantViewModel.activeDiseases.collectAsState()
    val tasks by careScheduleViewModel.allTasks.collectAsState()
    // Match MyPlants + Dashboard: sick = plant has an unresolved Disease log.
    val sickCount      = plants.count { activeDiseases[it.id] != null }
    val healthyCount   = plants.size - sickCount
    // Tasks Done mirrors today's progress counter from Care Schedule.
    val doneToday by careScheduleViewModel.doneToday.collectAsState()
    val completedTasks = doneToday

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("flora_prefs", android.content.Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notif_master", true)) }
    var careRemindersEnabled by remember { mutableStateOf(prefs.getBoolean("notif_care_reminders", true)) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, color = Palette.textPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = Space.xxl)
        ) {
            if (isGuest) {
                // ── Guest Profile Header ────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Space.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Size.avatarLg + Space.section).clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Palette.surfaceTintStrong, Palette.surfaceTintSoft)))
                                .border(2.dp, Brush.verticalGradient(listOf(Palette.textPrimary.copy(alpha = 0.75f), Palette.textPrimary.copy(alpha = 0.20f))), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "Guest", tint = Palette.textTertiary, modifier = Modifier.size(Size.iconXl + Space.lg))
                        }
                        Spacer(modifier = Modifier.height(Space.md + 2.dp))
                        Text("Guest", fontSize = Type.titleLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                        Text("guest@flora.app", fontSize = Type.bodyMedium, color = Palette.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        Text("Sign in to access your full profile", fontSize = Type.bodySmall, color = Palette.textTertiary, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(Space.xl))
                        // Sign In button
                        GlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.section)
                                .clickable { navController.navigate("login") },
                            cornerRadius = Radii.card - 4.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(Space.md + 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, null, tint = Palette.accentAlt, modifier = Modifier.size(Size.iconLg - 4.dp))
                                Spacer(modifier = Modifier.width(Space.sm + 2.dp))
                                Text("Sign In to Flora", fontSize = Type.bodyLarge, fontWeight = FontWeight.SemiBold, color = Palette.accentAlt)
                            }
                        }
                    }
                }
            } else {
                // ── Signed-in Profile Header ────────────────────────────────
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Space.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val profilePath = currentUser?.profileImageUri
                        val profileFile = profilePath?.let { File(it) }
                        val profileBmp = if (profileFile != null && profileFile.exists()) {
                            remember(profilePath) { BitmapFactory.decodeFile(profileFile.absolutePath) }
                        } else null
                        val hasPhoto = profileBmp != null

                        Box(
                            modifier = Modifier
                                .size(Size.avatarLg + Space.section).clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Palette.surfaceTintStrong, Palette.surfaceTintSoft)))
                                .border(2.dp, Brush.verticalGradient(listOf(Palette.textPrimary.copy(alpha = 0.75f), Palette.textPrimary.copy(alpha = 0.20f))), CircleShape)
                                .then(if (hasPhoto) Modifier.clickable { showEnlargedPhoto = true } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileBmp != null) {
                                Image(
                                    bitmap = profileBmp.asImageBitmap(),
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(displayName.take(1).uppercase(), fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                            }
                        }

                        // Enlarged profile photo dialog
                        if (showEnlargedPhoto && profileBmp != null) {
                            Dialog(
                                onDismissRequest = { showEnlargedPhoto = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(0.85f))
                                        .clickable { showEnlargedPhoto = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = profileBmp.asImageBitmap(),
                                        contentDescription = "Enlarged profile photo",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .clip(RoundedCornerShape(Radii.card - 4.dp))
                                    )
                                    // Close button at top-right
                                    IconButton(
                                        onClick = { showEnlargedPhoto = false },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(Space.lg)
                                            .statusBarsPadding()
                                            .size(Size.avatarSm + Space.xs)
                                            .clip(CircleShape)
                                            .background(Palette.surfaceTintSoft)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Close",
                                            tint = Palette.textPrimary,
                                            modifier = Modifier.size(Size.iconLg - 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(Space.md + 2.dp))
                        Text(displayName,  fontSize = Type.titleLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                        Text(displayEmail, fontSize = Type.bodyMedium, color = Palette.textSecondary, modifier = Modifier.padding(top = 2.dp))
                        if (displayJoin.isNotEmpty()) {
                            Text(displayJoin, fontSize = Type.bodySmall, color = Palette.accentAlt, modifier = Modifier.padding(top = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(Space.lg))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radii.card))
                                .background(Brush.verticalGradient(listOf(Palette.surfaceTintStrong, Palette.surfaceTintSoft)))
                                .border(1.dp, Brush.verticalGradient(listOf(Palette.textPrimary.copy(alpha = 0.55f), Palette.textPrimary.copy(alpha = 0.15f))), RoundedCornerShape(Radii.card))
                                .clickable { navController.navigate("edit_profile") }
                                .padding(horizontal = Space.lg + 2.dp, vertical = Space.sm + 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Edit, null, tint = Palette.textPrimary, modifier = Modifier.size(Size.iconSm))
                            Spacer(modifier = Modifier.width(Space.xs + 2.dp))
                            Text("Edit Profile", fontSize = Type.bodySmall, color = Palette.textPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ── Stats row ───────────────────────────────────────────────
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs), cornerRadius = Radii.card) {
                        Row(modifier = Modifier.fillMaxWidth().padding(Space.xl), horizontalArrangement = Arrangement.SpaceEvenly) {
                            GlassProfileStat("${plants.size}", "Total\nPlants",   Palette.accentAlt)
                            Box(modifier = Modifier.width(1.dp).height(Space.section + Space.lg).background(Palette.textMuted.copy(alpha = 0.20f)))
                            GlassProfileStat("$healthyCount",   "Healthy\nPlants", Palette.accent)
                            Box(modifier = Modifier.width(1.dp).height(Space.section + Space.lg).background(Palette.textMuted.copy(alpha = 0.20f)))
                            GlassProfileStat("$completedTasks", "Tasks\nDone",     Palette.info)
                            Box(modifier = Modifier.width(1.dp).height(Space.section + Space.lg).background(Palette.textMuted.copy(alpha = 0.20f)))
                            GlassProfileStat("$sickCount", "Need\nCare", if (sickCount > 0) Palette.danger else Palette.accent)
                        }
                    }
                }
            }

            // ── Appearance (shared) ─────────────────────────────────────────
            item { GlassSectionHeader("Appearance") }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg), cornerRadius = Radii.card - 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.lg, vertical = Space.md + 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Size.avatarSm)
                                .clip(RoundedCornerShape(Radii.chip))
                                .background(Color(0xFF7C83FD).copy(alpha = 0.18f))
                                .border(1.dp, Color(0xFF7C83FD).copy(alpha = 0.35f), RoundedCornerShape(Radii.chip)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DarkMode, null, tint = Color(0xFF7C83FD), modifier = Modifier.size(Size.iconLg - 4.dp))
                        }
                        Spacer(modifier = Modifier.width(Space.md + 2.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dark Mode", fontSize = Type.bodyMedium, fontWeight = FontWeight.Medium, color = Palette.textPrimary)
                            Text("Switch to midnight forest palette", fontSize = Type.caption, color = Palette.textTertiary)
                        }
                        SunMoonToggle(checked = isDarkMode, onToggle = onDarkModeToggle)
                    }
                }
            }

            if (!isGuest) {
                // ── Notifications (signed-in only) ──────────────────────────
                item { GlassSectionHeader("Notifications") }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg), cornerRadius = Radii.card - 2.dp) {
                        GlassToggleRow(
                            Icons.Filled.Notifications, "Push Notifications", "Master switch for all alerts",
                            notificationsEnabled,
                            {
                                notificationsEnabled = it
                                prefs.edit().putBoolean("notif_master", it).apply()
                                if (!it) com.example.flora.notifications.FloraNotifications.cancelAllCareReminders(context)
                            },
                            Palette.info, true,
                        )
                        GlassToggleRow(
                            Icons.Filled.Eco, "Care Reminders", "Watering and fertilizing alerts",
                            careRemindersEnabled,
                            {
                                careRemindersEnabled = it
                                prefs.edit().putBoolean("notif_care_reminders", it).apply()
                                if (!it) com.example.flora.notifications.FloraNotifications.cancelAllCareReminders(context)
                            },
                            Palette.accent, false,
                        )
                    }
                }
            }

            // ── App (shared) ────────────────────────────────────────────────
            item { GlassSectionHeader("App") }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg), cornerRadius = Radii.card - 2.dp) {
                    GlassNavRow(Icons.Filled.Share, "Share App", accentColor = Color(0xFFCE93D8), showDivider = true) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Try Flora — Malaysian Plant Care")
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "I'm using Flora to identify plants, diagnose diseases and keep my watering schedule on track. Check it out!",
                            )
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Flora"))
                    }
                    GlassNavRow(Icons.Filled.Policy, "Privacy Policy", accentColor = Color(0xFFB0BEC5), showDivider = true) {
                        navController.navigate("privacy_policy")
                    }
                    GlassNavRow(Icons.Filled.Help, "Help & Support", accentColor = Palette.accentAlt, showDivider = true) {
                        navController.navigate("help_support")
                    }
                    GlassNavRow(Icons.Filled.Info, "About Flora", subtitle = "Version 1.0.0", accentColor = Palette.accent, showDivider = false) {
                        navController.navigate("about_flora")
                    }
                }
            }

            if (!isGuest) {
                // ── Sign out (signed-in only) ───────────────────────────────
                item { GlassSectionHeader("Account") }
                item {
                    GlassBox(
                        modifier = Modifier
                            .fillMaxWidth().padding(horizontal = Space.lg)
                            .clickable {
                                authViewModel.logout()
                                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                            },
                        cornerRadius = Radii.card - 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Space.lg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Logout, null, tint = Palette.danger, modifier = Modifier.size(Size.iconLg - 4.dp))
                            Spacer(modifier = Modifier.width(Space.sm + 2.dp))
                            Text("Sign Out", fontSize = Type.bodyLarge, fontWeight = FontWeight.SemiBold, color = Palette.danger)
                        }
                    }
                    Spacer(modifier = Modifier.height(Space.sm))
                }
            }
        }
    }
}

@Composable
private fun GlassProfileStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = Type.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = Palette.textTertiary, textAlign = TextAlign.Center, lineHeight = Type.bodySmall)
    }
}

@Composable
private fun GlassSectionHeader(title: String) {
    Text(title.uppercase(), fontSize = Type.caption, fontWeight = FontWeight.Bold, color = Palette.textTertiary,
        modifier = Modifier.padding(start = Space.xl, top = Space.xxl - 2.dp, bottom = Space.sm), letterSpacing = 1.sp)
}

@Composable
private fun GlassToggleRow(icon: ImageVector, label: String, subtitle: String = "", checked: Boolean, onToggle: (Boolean) -> Unit, accentColor: Color, showDivider: Boolean) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md + 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(Size.avatarSm).clip(RoundedCornerShape(Radii.chip)).background(accentColor.copy(alpha = 0.18f)).border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(Radii.chip)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(Size.iconLg - 4.dp))
            }
            Spacer(modifier = Modifier.width(Space.md + 2.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = Type.bodyMedium, fontWeight = FontWeight.Medium, color = Palette.textPrimary)
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = Type.caption, color = Palette.textTertiary)
            }
            Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Palette.textPrimary, checkedTrackColor = accentColor.copy(alpha = 0.70f), uncheckedThumbColor = Palette.textSecondary, uncheckedTrackColor = Palette.surfaceTintSoft))
        }
        if (showDivider) Box(modifier = Modifier.fillMaxWidth().padding(start = Space.section + Space.xxl + 2.dp).height(1.dp).background(Palette.textMuted.copy(alpha = 0.12f)))
    }
}

@Composable
private fun GlassNavRow(icon: ImageVector, label: String, subtitle: String = "", accentColor: Color, showDivider: Boolean, onClick: () -> Unit = {}) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Space.lg, vertical = Space.md + 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(Size.avatarSm).clip(RoundedCornerShape(Radii.chip)).background(accentColor.copy(alpha = 0.18f)).border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(Radii.chip)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(Size.iconLg - 4.dp))
            }
            Spacer(modifier = Modifier.width(Space.md + 2.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = Type.bodyMedium, fontWeight = FontWeight.Medium, color = Palette.textPrimary)
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = Type.caption, color = Palette.textTertiary)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Palette.textMuted, modifier = Modifier.size(Size.iconLg - 4.dp))
        }
        if (showDivider) Box(modifier = Modifier.fillMaxWidth().padding(start = Space.section + Space.xxl + 2.dp).height(1.dp).background(Palette.textMuted.copy(alpha = 0.12f)))
    }
}

/**
 * Animated Sun/Moon sliding toggle for the Dark Mode setting.
 *
 * Layout: [☀ ─────────────── ●  🌙]
 *   • Light mode (checked = false): thumb rests on the left  (sun side, 4 dp offset)
 *   • Dark  mode (checked = true) : thumb rests on the right (moon side, 32 dp offset)
 *
 * The active icon is full-opacity; the inactive one fades to 30 %.
 * Track colour smoothly cross-fades between a day-blue and a night-indigo.
 */
@Composable
private fun SunMoonToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Thumb slides from left (light) to right (dark)
    // Container 56×28 dp, thumb 20×20 dp, 4 dp padding each edge → travel = 56−20−4−4 = 28 dp
    val thumbOffset by animateDpAsState(
        targetValue   = if (checked) 32.dp else 4.dp,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label         = "smThumb"
    )
    val trackColor by animateColorAsState(
        targetValue   = if (checked) Color(0xFF3D3BAA) else Color(0xFF1A6CA8),
        animationSpec = tween(350),
        label         = "smTrack"
    )

    Box(
        modifier = modifier
            .width(56.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor.copy(alpha = 0.80f))
            .border(1.dp, Palette.textPrimary.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .clickable { onToggle() }
    ) {
        // Sun icon – left / light-mode side
        Icon(
            imageVector        = Icons.Filled.WbSunny,
            contentDescription = "Light mode",
            tint               = Color(0xFFFDD835).copy(alpha = if (!checked) 1f else 0.30f),
            modifier           = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 5.dp)
                .size(14.dp)
        )
        // Moon icon – right / dark-mode side
        Icon(
            imageVector        = Icons.Filled.DarkMode,
            contentDescription = "Dark mode",
            tint               = Color(0xFFCE93D8).copy(alpha = if (checked) 1f else 0.30f),
            modifier           = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 5.dp)
                .size(14.dp)
        )
        // Sliding white thumb (drawn on top so it covers the active icon)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        )
    }
}
