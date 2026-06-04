package com.example.flora.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Flora design tokens — single source of truth.
 *
 * Spec: Flora Design System (liquid glass + Malaysian-garden gradient).
 * Every token maps 1:1 to `colors_and_type.css` CSS custom properties.
 * Screens should pull from here instead of declaring raw literals.
 */
object FloraDesign {

    /** 4-pt spacing scale. Use instead of raw `16.dp`. */
    object Space {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 20.dp
        val xxl: Dp = 24.dp
        val section: Dp = 32.dp
    }

    /** Corner radii. Keep small count so shapes stay consistent. */
    object Radii {
        val chip: Dp = 10.dp
        val card: Dp = 20.dp
        val sheet: Dp = 28.dp
        val pill: Dp = 999.dp
    }

    /** Touch-target heights. 44dp min per accessibility. */
    object Size {
        val touchMin: Dp = 44.dp
        val buttonPrimary: Dp = 52.dp
        val iconSm: Dp = 14.dp
        val iconMd: Dp = 18.dp
        val iconLg: Dp = 24.dp
        val iconXl: Dp = 32.dp
        val avatarSm: Dp = 36.dp
        val avatarMd: Dp = 42.dp
        val avatarLg: Dp = 64.dp
        val heroImage: Dp = 260.dp
    }

    /** Type scale — roles not sizes. */
    object Type {
        val displayLarge: TextUnit = 32.sp
        val displayMedium: TextUnit = 26.sp
        val titleLarge: TextUnit = 22.sp
        val titleMedium: TextUnit = 18.sp
        val titleSmall: TextUnit = 16.sp
        val bodyLarge: TextUnit = 15.sp
        val bodyMedium: TextUnit = 14.sp
        val bodySmall: TextUnit = 13.sp
        val caption: TextUnit = 12.sp
        val micro: TextUnit = 11.sp

        val lineBody: TextUnit = 20.sp
        val lineTitle: TextUnit = 28.sp
    }

    /** Semantic colors — accent, status, text, glass tints. */
    object Palette {
        val accent: Color = Color(0xFF69F0AE)       // primary action / success
        val accentAlt: Color = Color(0xFF80DEEA)    // secondary action / scan
        val warn: Color = Color(0xFFFFD54F)         // caution
        val danger: Color = Color(0xFFEF9A9A)       // error / disease
        val info: Color = Color(0xFF40C4FF)         // water / cool

        val textPrimary: Color = Color.White
        val textSecondary: Color = Color.White.copy(alpha = 0.75f)
        val textTertiary: Color = Color.White.copy(alpha = 0.55f)
        val textMuted: Color = Color.White.copy(alpha = 0.40f)

        val surfaceTintStrong: Color = Color.White.copy(alpha = 0.26f)
        val surfaceTintMid: Color = Color.White.copy(alpha = 0.18f)
        val surfaceTintSoft: Color = Color.White.copy(alpha = 0.12f)

        val sheetTop: Color = Color(0xFF0A1A14).copy(alpha = 0.96f)
        val sheetBottom: Color = Color(0xFF061510).copy(alpha = 0.98f)
    }

    /**
     * Full-screen background gradient stops for [com.example.flora.ui.components.GlassBackground].
     * Four-stop vertical gradient: forest → ocean → midnight (light) / near-black (dark).
     */
    object Background {
        // Light mode
        val light0: Color = Color(0xFF1A4731)
        val light1: Color = Color(0xFF2D6A4F)
        val light2: Color = Color(0xFF124D6E)
        val light3: Color = Color(0xFF0F2044)

        // Dark mode
        val dark0: Color = Color(0xFF030C06)
        val dark1: Color = Color(0xFF071410)
        val dark2: Color = Color(0xFF050B18)
        val dark3: Color = Color(0xFF02060F)

        fun stopsFor(isDark: Boolean): List<Color> =
            if (isDark) listOf(dark0, dark1, dark2, dark3)
            else listOf(light0, light1, light2, light3)
    }

    /**
     * Glass surface alpha stops (top → bottom) for card fill + border.
     * Animated via [com.example.flora.ui.components.GlassCard] on theme toggle.
     */
    object Glass {
        val fillTopLight: Float = 0.26f
        val fillBotLight: Float = 0.11f
        val fillTopDark: Float = 0.16f
        val fillBotDark: Float = 0.07f

        val borderTopLight: Float = 0.60f
        val borderBotLight: Float = 0.18f
        val borderTopDark: Float = 0.28f
        val borderBotDark: Float = 0.08f

        val borderWidth: Dp = 1.dp
    }

    /**
     * Animation durations (ms) + easings.
     * Keep movement silky & unhurried — nature's pace.
     */
    object Anim {
        const val THEME_MS: Int = 1800       // light/dark cross-fade (silky, per design chat)
        const val SPLASH_MS: Int = 700       // splash fade + scale
        const val NAV_INDICATOR_MS: Int = 250 // bottom nav underline
        const val NAV_OPACITY_MS: Int = 200  // bottom nav selection opacity
        const val FAST_MS: Int = 200         // quick state changes

        val easingStandard: Easing = FastOutSlowInEasing
        val easingEmphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    }

    /** Per-species plant-card gradient color (top of card). */
    object PlantGradient {
        val hibiscus: Color = Color(0xFFE91E63)
        val orchid: Color = Color(0xFF9C27B0)
        val ixora: Color = Color(0xFFFF5722)
        val bougainvillea: Color = Color(0xFFE040FB)
        val jasmine: Color = Color(0xFFFDD835)
        val frangipani: Color = Color(0xFFFF9800)
        val waterLily: Color = Color(0xFF26C6DA)
        val heliconia: Color = Color(0xFFEC407A)

        /** Lookup by common or scientific name (case-insensitive). Fallback = [Palette.accentAlt]. */
        fun forSpecies(name: String): Color {
            val n = name.lowercase()
            return when {
                "hibiscus" in n || "bunga raya" in n -> hibiscus
                "orchid" in n -> orchid
                "ixora" in n -> ixora
                "bougainvillea" in n -> bougainvillea
                "jasmine" in n || "melati" in n -> jasmine
                "frangipani" in n || "plumeria" in n -> frangipani
                "water lily" in n || "lotus" in n || "teratai" in n -> waterLily
                "heliconia" in n -> heliconia
                else -> Palette.accentAlt
            }
        }
    }

    /** Severity → color map for health status, disease severity, care urgency. */
    fun severityColor(level: String): Color = when (level.lowercase()) {
        "none", "healthy", "mild" -> Palette.accent
        "moderate", "warn", "warning" -> Palette.warn
        "severe", "critical", "overdue" -> Palette.danger
        else -> Palette.accentAlt
    }
}
