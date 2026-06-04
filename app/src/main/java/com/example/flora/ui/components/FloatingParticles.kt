package com.example.flora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Floating botanical particles rendered behind glass surfaces.
 *
 * Subtle leaf silhouettes drift upward with slight horizontal sway,
 * gentle rotation, and fade in/out at the edges.
 *
 * Animation cycles 22-38s per particle per the Flora Design System chat spec —
 * "slow, unhurried, nature's pace".
 *
 * GPU cost: single Canvas, ~16 particles, simple path draws. Negligible.
 */
@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 16,
    tint: Color = Color.White,
    seed: Int = 42,
) {
    // Stable particle set across recompositions
    val particles = remember(particleCount, seed) {
        val rng = Random(seed)
        List(particleCount) {
            Particle(
                startX = rng.nextFloat(),
                phase = rng.nextFloat(),
                cycleMs = rng.nextLong(22_000, 38_000),
                sizeDp = rng.nextFloat() * 10f + 12f,   // 12..22
                swayAmp = rng.nextFloat() * 60f + 30f,  // 30..90
                swayFreq = rng.nextFloat() * 0.7f + 0.6f,
                rotationStart = rng.nextFloat() * 360f,
                rotationPerCycle = (rng.nextFloat() * 2f - 1f) * 140f, // ±140°
                alpha = rng.nextFloat() * 0.05f + 0.05f,  // 0.05..0.10 — very subtle
            )
        }
    }

    // Single frame-driven time source shared by all particles
    val timeMs by produceState(0L) {
        while (true) withFrameMillis { value = it }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val t = ((timeMs % p.cycleMs) / p.cycleMs.toFloat() + p.phase) % 1f
            // Vertical: bottom (y=h) to top (y=-size)
            val y = h - (h + p.sizeDp * density) * t
            val sway = sin((t * 2.0 * PI * p.swayFreq).toFloat()) * p.swayAmp
            val x = p.startX * w + sway
            // Edge fade: triangular 0→1→0 over cycle
            val fade = 1f - kotlin.math.abs(t - 0.5f) * 2f
            val alpha = p.alpha * fade

            if (alpha > 0.005f) {
                val rot = p.rotationStart + p.rotationPerCycle * t
                translate(left = x, top = y) {
                    rotate(rot, pivot = Offset.Zero) {
                        drawLeaf(tint.copy(alpha = alpha), p.sizeDp * density)
                    }
                }
            }
        }
    }
}

/** A single drifting particle (botanical leaf). */
private data class Particle(
    val startX: Float,
    val phase: Float,
    val cycleMs: Long,
    val sizeDp: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val rotationStart: Float,
    val rotationPerCycle: Float,
    val alpha: Float,
)

/**
 * Stylised leaf — two curved sides meeting at tip and base,
 * plus a central midrib stroke. Drawn in local coordinates.
 */
private fun DrawScope.drawLeaf(color: Color, size: Float) {
    val w = size
    val h = size * 1.6f
    val path = Path().apply {
        moveTo(0f, 0f)
        // right side curve
        cubicTo(w * 0.9f, h * 0.2f, w * 0.5f, h * 0.7f, 0f, h)
        // left side curve (mirror)
        cubicTo(-w * 0.5f, h * 0.7f, -w * 0.9f, h * 0.2f, 0f, 0f)
        close()
    }
    drawPath(path, color = color)
    // midrib
    drawPath(
        path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, h)
        },
        color = color.copy(alpha = (color.alpha * 0.6f).coerceAtMost(1f)),
        style = Stroke(width = size * 0.06f),
    )
}

