package com.example.flora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type

/**
 * Section header — consistent hierarchy across screens.
 * Optional trailing slot for actions ("See all", etc.).
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = Type.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Palette.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    fontSize = Type.caption,
                    color = Palette.textTertiary
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/**
 * Empty / onboarding state — shown when a list has no items or a feature has no data yet.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    accent: Color = Palette.accent,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(Size.avatarLg)
                .clip(RoundedCornerShape(Radii.card))
                .background(accent.copy(alpha = 0.18f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(Radii.card)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(Size.iconXl))
        }
        Spacer(Modifier.height(Space.lg))
        Text(
            title,
            fontSize = Type.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Palette.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            body,
            fontSize = Type.bodySmall,
            color = Palette.textTertiary,
            lineHeight = Type.lineBody,
            textAlign = TextAlign.Center
        )
        if (primaryLabel != null && onPrimary != null) {
            Spacer(Modifier.height(Space.lg))
            FloraPrimaryButton(label = primaryLabel, onClick = onPrimary, accent = accent)
        }
    }
}

/** Primary filled-action button. Use for "Save", "Continue", "Get started". */
@Composable
fun FloraPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = Palette.accent,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(Size.buttonPrimary),
        shape = RoundedCornerShape(Radii.chip),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = 0.30f),
            disabledContainerColor = Palette.surfaceTintSoft
        )
    ) {
        if (icon != null) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(Size.iconMd))
            Spacer(Modifier.width(Space.sm))
        }
        Text(label, color = accent, fontWeight = FontWeight.SemiBold, fontSize = Type.bodyLarge)
    }
}

/** Secondary / cancel button. White-tinted on glass. */
@Composable
fun FloraSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(Size.buttonPrimary),
        shape = RoundedCornerShape(Radii.chip),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.surfaceTintStrong)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Palette.textPrimary, modifier = Modifier.size(Size.iconMd))
            Spacer(Modifier.width(Space.sm))
        }
        Text(label, color = Palette.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = Type.bodyLarge)
    }
}

/** Small status pill — e.g. "Native", "Overdue", "Healthy". */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Palette.accent
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radii.chip))
            .background(color.copy(alpha = 0.22f))
            .border(1.dp, color.copy(alpha = 0.50f), RoundedCornerShape(Radii.chip))
            .padding(horizontal = Space.sm, vertical = Space.xs)
    ) {
        Text(text, fontSize = Type.micro, fontWeight = FontWeight.Bold, color = color)
    }
}
