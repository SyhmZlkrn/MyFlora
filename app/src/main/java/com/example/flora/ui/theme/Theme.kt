package com.example.flora.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Liquid-glass light scheme — all surfaces transparent so the gradient background shows through.
// Primary = mint accent (#69F0AE), Secondary = cyan accent (#80DEEA), per Flora Design System.
private val FloraLightColorScheme = lightColorScheme(
    primary            = AccentMint,
    onPrimary          = Color.Black,
    primaryContainer   = Color(0x33FFFFFF),
    onPrimaryContainer = Color.White,
    secondary          = AccentCyan,
    onSecondary        = Color.Black,
    secondaryContainer = Color(0x22FFFFFF),
    onSecondaryContainer = Color.White.copy(alpha = 0.85f),
    tertiary           = AccentAmber,
    onTertiary         = Color.Black,
    background         = Color.Transparent,
    onBackground       = Color.White,
    surface            = Color.Transparent,
    onSurface          = Color.White,
    surfaceVariant     = Color(0x22FFFFFF),
    onSurfaceVariant   = Color.White.copy(alpha = 0.75f),
    error              = AccentRed,
    onError            = Color.Black,
    outline            = Color.White.copy(alpha = 0.35f),
    outlineVariant     = Color.White.copy(alpha = 0.18f),
)

// Liquid-glass dark scheme — same glass surfaces over a near-black gradient
private val FloraDarkColorScheme = darkColorScheme(
    primary            = AccentMint,
    onPrimary          = Color.Black,
    primaryContainer   = Color(0x22FFFFFF),
    onPrimaryContainer = Color.White,
    secondary          = AccentCyan,
    onSecondary        = Color.Black,
    secondaryContainer = Color(0x16FFFFFF),
    onSecondaryContainer = Color.White.copy(alpha = 0.75f),
    tertiary           = AccentAmber,
    onTertiary         = Color.Black,
    background         = Color.Transparent,
    onBackground       = Color.White,
    surface            = Color.Transparent,
    onSurface          = Color.White,
    surfaceVariant     = Color(0x18FFFFFF),
    onSurfaceVariant   = Color.White.copy(alpha = 0.65f),
    error              = AccentRed,
    onError            = Color.Black,
    outline            = Color.White.copy(alpha = 0.28f),
    outlineVariant     = Color.White.copy(alpha = 0.12f),
)

@Composable
fun FloraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FloraDarkColorScheme
        else      -> FloraLightColorScheme
    }

    // Make status bar transparent so the gradient background bleeds through
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            // Always use light icons (white) — the gradient is always dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
