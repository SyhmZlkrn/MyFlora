package com.example.flora.ui.data

import androidx.compose.ui.graphics.Color

/**
 * UI-only gradient color pairs for plant cards.
 * All plant/task/disease data now comes from Room database.
 */
object MockData {

    val plantGradientColors = listOf(
        Pair(Color(0xFFE91E63), Color(0xFFAD1457)),
        Pair(Color(0xFF9C27B0), Color(0xFF6A1B9A)),
        Pair(Color(0xFFFF5722), Color(0xFFBF360C)),
        Pair(Color(0xFFE040FB), Color(0xFF8E24AA)),
        Pair(Color(0xFFFDD835), Color(0xFFF9A825)),
        Pair(Color(0xFFFF9800), Color(0xFFE65100)),
        Pair(Color(0xFF26C6DA), Color(0xFF00838F)),
        Pair(Color(0xFFEC407A), Color(0xFFC2185B))
    )
}
