package com.example.flora.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Background Gradient Stops (Flora Design System) ─────────────
// Light mode: forest → ocean → midnight
val BgLight0 = Color(0xFF1A4731)
val BgLight1 = Color(0xFF2D6A4F)
val BgLight2 = Color(0xFF124D6E)
val BgLight3 = Color(0xFF0F2044)

// Dark mode: deep near-black gradient
val BgDark0 = Color(0xFF030C06)
val BgDark1 = Color(0xFF071410)
val BgDark2 = Color(0xFF050B18)
val BgDark3 = Color(0xFF02060F)

// ─── Semantic Accents (matches FloraDesign.Palette) ──────────────
val AccentMint = Color(0xFF69F0AE)   // primary action / healthy
val AccentCyan = Color(0xFF80DEEA)   // secondary / scan / water
val AccentAmber = Color(0xFFFFD54F)  // caution
val AccentRed = Color(0xFFEF9A9A)    // error / disease
val AccentSky = Color(0xFF40C4FF)    // info / water

// ─── Status (health, disease severity) ───────────────────────────
val StatusHealthy = AccentMint
val StatusSick = AccentAmber
val StatusCritical = AccentRed
val StatusRecovering = AccentSky

// ─── Text on dark gradient ───────────────────────────────────────
val TextPrimary = Color.White
val TextSecondary = Color.White.copy(alpha = 0.75f)
val TextTertiary = Color.White.copy(alpha = 0.55f)
val TextMuted = Color.White.copy(alpha = 0.40f)

// ─── Plant Card Gradient Colors ──────────────────────────────────
val PlantColorHibiscus = Color(0xFFE91E63)
val PlantColorOrchid = Color(0xFF9C27B0)
val PlantColorIxora = Color(0xFFFF5722)
val PlantColorBougainvillea = Color(0xFFE040FB)
val PlantColorJasmine = Color(0xFFFDD835)
val PlantColorFrangipani = Color(0xFFFF9800)
val PlantColorWaterLily = Color(0xFF26C6DA)
val PlantColorHeliconia = Color(0xFFEC407A)

// ─── Legacy (kept for backward compat with old screens) ──────────
@Deprecated("Use AccentMint", ReplaceWith("AccentMint"))
val GreenPrimary = Color(0xFF4CAF50)
@Deprecated("Use AccentMint")
val GreenDark = Color(0xFF388E3C)
@Deprecated("Use AccentMint")
val GreenDarker = Color(0xFF2E7D32)
@Deprecated("Use AccentMint")
val GreenLight = Color(0xFF81C784)
@Deprecated("Use AccentMint")
val GreenLighter = Color(0xFFA5D6A7)
@Deprecated("Surface uses glass not solid")
val GreenBackground = Color(0xFFF1F8E9)
@Deprecated("Use AccentMint")
val LightGreen = Color(0xFF8BC34A)
@Deprecated("Use AccentAmber")
val AmberAccent = Color(0xFFFFC107)
@Deprecated("Use AccentAmber")
val AmberDark = Color(0xFFFFA000)
@Deprecated("Glass surfaces are transparent")
val CardBackground = Color(0xFFFFFFFF)
@Deprecated("Use GlassBackground")
val BackgroundLight = Color(0xFFF5F5F5)
@Deprecated("Use Palette.surfaceTintSoft")
val DividerColor = Color(0xFFE0E0E0)

// Material3 baseline palette (unused by Flora but kept to avoid import breaks)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)
