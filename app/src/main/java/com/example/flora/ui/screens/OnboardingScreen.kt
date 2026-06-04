package com.example.flora.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.components.LocalIsDarkMode
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import kotlinx.coroutines.launch

// ─── First-run persistence ─────────────────────────────────────────
private const val PREFS_NAME = "flora_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

object OnboardingPrefs {
    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }
}

// ─── Slide content ─────────────────────────────────────────────────
private data class OnboardingSlide(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val body: String,
)

private val slides = listOf(
    OnboardingSlide(
        icon = Icons.Filled.CameraAlt,
        accent = Color(0xFF80DEEA),      // cyan — scan
        title = "Snap any plant",
        body = "Our on-device AI names 1,081 species in a second. Works fully offline — your photos never leave your phone."
    ),
    OnboardingSlide(
        icon = Icons.Filled.WaterDrop,
        accent = Color(0xFF40C4FF),      // sky blue — water
        title = "Never miss a watering",
        body = "Smart schedules adapt to your Malaysian climate and each plant's unique needs. Gentle reminders, not nags."
    ),
    OnboardingSlide(
        icon = Icons.Filled.BugReport,
        accent = Color(0xFFEF9A9A),      // soft red — diagnose
        title = "Spot trouble early",
        body = "Detect diseases from a single leaf photo. Get care tips before your plant turns south."
    ),
)

// ─── Screen ────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(pageCount = { slides.size })

    fun finish() {
        OnboardingPrefs.markDone(context)
        navController.navigate("login") {
            popUpTo("onboarding") { inclusive = true }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Top bar — Skip link top-right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xxl, vertical = Space.xl)
                .padding(top = Space.section),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tiny wordmark on left
            Text(
                "MyFlora",
                fontSize = Type.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Palette.textPrimary,
                letterSpacing = 2.sp,
            )
            if (pager.currentPage < slides.lastIndex) {
                Text(
                    "Skip",
                    fontSize = Type.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Palette.textSecondary,
                    modifier = Modifier.clickable { finish() },
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
        }

        // Pager — fills above the bottom sheet
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 280.dp, top = 88.dp),
        ) { page ->
            SlideContent(slide = slides[page])
        }

        // Bottom glass sheet with indicator + CTA
        BottomSheet(
            currentPage = pager.currentPage,
            pageCount = slides.size,
            onNext = {
                if (pager.currentPage < slides.lastIndex) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else {
                    finish()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Hero — 140dp glass circle with tinted icon
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            slide.accent.copy(alpha = 0.28f),
                            slide.accent.copy(alpha = 0.08f),
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(
                            slide.accent.copy(alpha = 0.70f),
                            slide.accent.copy(alpha = 0.20f),
                        )
                    ),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                slide.icon,
                contentDescription = slide.title,
                tint = slide.accent,
                modifier = Modifier.size(66.dp),
            )
        }

        Spacer(Modifier.height(Space.section + Space.sm))

        Text(
            slide.title,
            fontSize = Type.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Palette.textPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            lineHeight = Type.lineTitle,
        )

        Spacer(Modifier.height(Space.md))

        Text(
            slide.body,
            fontSize = Type.bodyLarge,
            color = Palette.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun BottomSheet(
    currentPage: Int,
    pageCount: Int,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkMode.current
    val topAlpha    = if (isDark) 0.16f else 0.26f
    val bottomAlpha = if (isDark) 0.07f else 0.11f
    val borderTop   = if (isDark) 0.28f else 0.60f
    val borderBot   = if (isDark) 0.08f else 0.18f

    // Sheet container — 28dp radius top only, flush to bottom
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = topAlpha),
                        Color.White.copy(alpha = bottomAlpha),
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = borderTop),
                        Color.White.copy(alpha = borderBot),
                    )
                ),
                RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Space.xxl, vertical = Space.section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // Page indicator — 3 pills, active = 28dp wide mint, inactive = 8dp white 0.35
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { i ->
                    val isActive = i == currentPage
                    val w by animateDpAsState(
                        targetValue = if (isActive) 28.dp else 8.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "ind_w_$i",
                    )
                    val alpha by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.35f,
                        animationSpec = tween(durationMillis = 300),
                        label = "ind_a_$i",
                    )
                    val color = if (isActive) Palette.accent else Color.White.copy(alpha = alpha)
                    Box(
                        modifier = Modifier
                            .width(w)
                            .height(8.dp)
                            .clip(RoundedCornerShape(Radii.pill))
                            .background(color),
                    )
                }
            }

            Spacer(Modifier.height(Space.section))

            // Primary CTA — "Next" or "Get Started"
            val isLast = currentPage == pageCount - 1
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Size.buttonPrimary),
                shape = RoundedCornerShape(Radii.chip + 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.accent.copy(alpha = 0.28f),
                ),
            ) {
                Text(
                    if (isLast) "Get Started" else "Next",
                    color = Palette.accent,
                    fontSize = Type.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                )
                if (!isLast) {
                    Spacer(Modifier.width(Space.sm))
                    Icon(
                        Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Palette.accent,
                        modifier = Modifier.size(Size.iconMd),
                    )
                }
            }

            Spacer(Modifier.height(Space.md))

            // Secondary — small caption row
            Text(
                text = if (isLast) "Tap to begin your plant journey" else "Swipe or tap Next",
                fontSize = Type.caption,
                color = Palette.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

