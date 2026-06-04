package com.example.flora.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.navigation.NavController
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = Palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Space.lg),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Radii.card) {
                Column(modifier = Modifier.padding(Space.lg)) {
                    Section("Effective date: 13 May 2026")
                    Section("Summary")
                    Body(
                        "MyFlora is built privacy-first. Your photos, plant collection, and watering history " +
                            "live entirely on this device and are not synced or shared with anyone."
                    )

                    Section("What stays on your device")
                    Body(
                        "• Account info (name, email, hashed password)\n" +
                            "• Plants you save (name, location, photo, watering history)\n" +
                            "• Care schedule and completion history\n" +
                            "• Notification preferences\n\n" +
                            "Everything above is stored in a local Room SQLite database. If you uninstall " +
                            "MyFlora, all of it is wiped."
                    )

                    Section("Network calls")
                    Body(
                        "MyFlora only contacts the internet in two cases:\n" +
                            "1. OpenWeather API — your approximate latitude/longitude is sent to fetch current " +
                            "conditions. Used to skip watering reminders on rainy days. Not retained.\n" +
                            "2. Optional cloud disease/identification fallback (currently disabled in code). " +
                            "When enabled, photos are sent to PlantNet's API for identification."
                    )

                    Section("What we do NOT collect")
                    Body(
                        "• Analytics, telemetry, or crash reports\n" +
                            "• Advertising identifiers\n" +
                            "• Location history\n" +
                            "• Contact list or other photos"
                    )

                    Section("Permissions explained")
                    Body(
                        "• Camera — required to capture plant and leaf photos for identification\n" +
                            "• Photo library access — pick existing photos as input\n" +
                            "• Approximate location — fetch local weather (Malaysia-focused)\n" +
                            "• Notifications — care reminders you can disable anytime in Profile"
                    )

                    Section("Your controls")
                    Body(
                        "• Delete plants individually from the Plants screen\n" +
                            "• Turn off all notifications from Profile → Notifications\n" +
                            "• Uninstall the app to delete the local database entirely"
                    )

                    Section("Contact")
                    Body(
                        "Questions or concerns? Reach out via Help & Support inside the app, or email " +
                            "muhdsyahmiz2004@gmail.com."
                    )
                    Spacer(Modifier.height(Space.lg))
                }
            }
        }
    }
}

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(Space.md))
    Text(text, fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
    Spacer(Modifier.height(Space.xs))
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        fontSize = Type.bodySmall,
        color = Palette.textPrimary.copy(alpha = 0.90f),
        lineHeight = TextUnit(20f, TextUnitType.Sp),
    )
}
