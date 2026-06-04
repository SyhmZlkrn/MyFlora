package com.example.flora.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutFloraScreen(navController: NavController) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("About MyFlora", fontWeight = FontWeight.Bold, color = Palette.textPrimary) },
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
            // Brand logo (leaf + wordmark)
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.example.flora.R.drawable.logo_myflora),
                contentDescription = "MyFlora",
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Text(
                "Version 1.0.0",
                fontSize = Type.caption,
                color = Palette.textTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(Space.xl))

            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = Radii.card) {
                Column(modifier = Modifier.padding(Space.lg)) {
                    SectionTitle("What is MyFlora?")
                    BodyText(
                        "MyFlora is a Malaysian-focused plant care companion. Identify common ornamentals, " +
                            "diagnose leaf diseases, and stay on top of your watering schedule — all on your " +
                            "device. No accounts, no tracking, no cloud lookups."
                    )

                    Spacer(Modifier.height(Space.lg))
                    SectionTitle("How identification works")
                    BodyText(
                        "Plant ID uses an on-device EfficientNet-B3 model trained on the PlantNet-300K " +
                            "dataset (1,081 species). Disease detection runs a YOLOv8n model trained on the " +
                            "yolo-v8-solrb/plant-disease-ep6jy Roboflow dataset (20 classes covering common " +
                            "crops). Both run entirely offline."
                    )

                    Spacer(Modifier.height(Space.lg))
                    SectionTitle("Care schedule")
                    BodyText(
                        "When you save a plant, MyFlora builds a watering schedule tailored to its genus " +
                            "(Hibiscus every 2 days, Bougainvillea every 5, Lavender every 10, succulents " +
                            "every 12-14). Ticking a task in Care Schedule advances the next due date and " +
                            "logs the action against the plant."
                    )

                    Spacer(Modifier.height(Space.lg))
                    SectionTitle("Privacy")
                    BodyText(
                        "All identification and diagnosis runs locally. The only network calls are the " +
                            "weather API (OpenWeather) for your current location, used to skip watering on " +
                            "rainy days. Your photos and plant data never leave the device."
                    )

                    Spacer(Modifier.height(Space.lg))
                    SectionTitle("Credits")
                    BodyText(
                        "• PlantNet-300K dataset — Garcin et al., 2021\n" +
                            "• Roboflow Universe — plant disease dataset\n" +
                            "• Ultralytics YOLOv8\n" +
                            "• Wikimedia Commons — disease reference images\n" +
                            "• OpenWeather — current conditions API"
                    )
                }
            }

            Spacer(Modifier.height(Space.xl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Made with care in Malaysia",
                    fontSize = Type.caption,
                    color = Palette.textTertiary,
                )
            }
            Spacer(Modifier.height(Space.xl))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
    Spacer(Modifier.height(Space.xs))
}

@Composable
private fun BodyText(text: String) {
    Text(
        text,
        fontSize = Type.bodySmall,
        color = Palette.textPrimary.copy(alpha = 0.90f),
        lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
    )
}
