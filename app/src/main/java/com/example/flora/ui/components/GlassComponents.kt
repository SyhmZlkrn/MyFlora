package com.example.flora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.example.flora.ui.theme.FloraDesign

/** CompositionLocal that carries the dark-mode toggle state through the tree. */
val LocalIsDarkMode = compositionLocalOf { false }

private val themeAnimSpec = tween<Color>(
    durationMillis = FloraDesign.Anim.THEME_MS,
    easing = FloraDesign.Anim.easingStandard,
)
private val alphaAnimSpec = tween<Float>(
    durationMillis = FloraDesign.Anim.THEME_MS,
    easing = FloraDesign.Anim.easingStandard,
)

/**
 * Full-screen gradient background.
 * All 4 stops animate smoothly between light and dark palettes via [FloraDesign.Anim.THEME_MS]
 * whenever [isDarkMode] changes — silky cross-fade.
 *
 * Stops sourced from [FloraDesign.Background].
 */
@Composable
fun GlassBackground(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    showParticles: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val light = FloraDesign.Background
    val c0 by animateColorAsState(if (isDarkMode) light.dark0 else light.light0, themeAnimSpec, label = "bgC0")
    val c1 by animateColorAsState(if (isDarkMode) light.dark1 else light.light1, themeAnimSpec, label = "bgC1")
    val c2 by animateColorAsState(if (isDarkMode) light.dark2 else light.light2, themeAnimSpec, label = "bgC2")
    val c3 by animateColorAsState(if (isDarkMode) light.dark3 else light.light3, themeAnimSpec, label = "bgC3")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c0, c1, c2, c3)))
    ) {
        if (showParticles) {
            // Drift botanical leaf silhouettes behind content — subtle, 22-38s cycles
            FloatingParticles(
                tint = Color.White,
                particleCount = 16,
            )
        }
        content()
    }
}

/**
 * Liquid-glass Card — Column-based.
 * Fill alpha gradient 0.26→0.11 light, 0.16→0.07 dark.
 * Border alpha gradient 0.60→0.18 light, 0.28→0.08 dark.
 * All alphas animate on theme toggle.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = FloraDesign.Radii.card,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val g = FloraDesign.Glass
    val topAlpha    by animateFloatAsState(if (isDark) g.fillTopDark else g.fillTopLight, alphaAnimSpec, label = "cardTop")
    val bottomAlpha by animateFloatAsState(if (isDark) g.fillBotDark else g.fillBotLight, alphaAnimSpec, label = "cardBot")
    val borderTop   by animateFloatAsState(if (isDark) g.borderTopDark else g.borderTopLight, alphaAnimSpec, label = "cardBorderTop")
    val borderBot   by animateFloatAsState(if (isDark) g.borderBotDark else g.borderBotLight, alphaAnimSpec, label = "cardBorderBot")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(listOf(Color.White.copy(topAlpha), Color.White.copy(bottomAlpha))))
            .border(
                g.borderWidth,
                Brush.verticalGradient(listOf(Color.White.copy(borderTop), Color.White.copy(borderBot))),
                RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

/**
 * Liquid-glass Box — Box-based version of [GlassCard].
 * Identical alpha animation, suited for non-stacked content.
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = FloraDesign.Radii.card,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val g = FloraDesign.Glass
    val topAlpha    by animateFloatAsState(if (isDark) g.fillTopDark else g.fillTopLight, alphaAnimSpec, label = "boxTop")
    val bottomAlpha by animateFloatAsState(if (isDark) g.fillBotDark else g.fillBotLight, alphaAnimSpec, label = "boxBot")
    val borderTop   by animateFloatAsState(if (isDark) g.borderTopDark else g.borderTopLight, alphaAnimSpec, label = "boxBorderTop")
    val borderBot   by animateFloatAsState(if (isDark) g.borderBotDark else g.borderBotLight, alphaAnimSpec, label = "boxBorderBot")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(listOf(Color.White.copy(topAlpha), Color.White.copy(bottomAlpha))))
            .border(
                g.borderWidth,
                Brush.verticalGradient(listOf(Color.White.copy(borderTop), Color.White.copy(borderBot))),
                RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}
