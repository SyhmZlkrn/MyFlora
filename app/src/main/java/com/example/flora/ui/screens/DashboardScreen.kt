package com.example.flora.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.example.flora.data.database.entities.CareSchedule
import com.example.flora.data.database.entities.Plant
import com.example.flora.ui.components.EmptyState
import com.example.flora.ui.components.GlassBox
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.components.SectionHeader
import com.example.flora.ui.data.MockData
import com.example.flora.ui.theme.FloraDesign
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.AuthViewModel
import com.example.flora.ui.viewmodel.CareScheduleViewModel
import com.example.flora.ui.viewmodel.PlantViewModel
import com.example.flora.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel,
    weatherViewModel: WeatherViewModel,
    careScheduleViewModel: CareScheduleViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val plants by plantViewModel.plants.collectAsState()
    // Same disease source MyPlants uses, so Dashboard stats and the plant
    // filter chips can never disagree.
    val activeDiseases by plantViewModel.activeDiseases.collectAsState()
    val weather by weatherViewModel.weather.collectAsState()
    val currentTime by weatherViewModel.currentTime.collectAsState()
    val currentDate by weatherViewModel.currentDate.collectAsState()
    val careTasks by careScheduleViewModel.allTasks.collectAsState()
    val userName = currentUser?.name ?: "Guest"

    val upcomingTasks = careTasks.filter { !it.isCompleted }.take(4)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            weatherViewModel.fetchWeather()
        }
    }

    LaunchedEffect(Unit) {
        weatherViewModel.fetchWeather()
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                weatherViewModel.fetchWeather()
                careScheduleViewModel.refreshIfNewDay()
                // Hold the spinner long enough for the network call to land.
                kotlinx.coroutines.delay(900)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Space.xxl)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Space.xl, vertical = Space.xl),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $userName! 🌿",
                        fontSize = Type.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Palette.textPrimary
                    )
                    Text(
                        text = currentDate.ifEmpty { "How are your plants today?" },
                        fontSize = Type.bodyMedium,
                        color = Palette.textSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.navigate("care_schedule") }) {
                        Icon(Icons.Filled.Notifications, "Notifications", tint = Palette.textPrimary, modifier = Modifier.size(Size.iconLg + 2.dp))
                    }
                    Spacer(modifier = Modifier.width(Space.xs))
                    Box(
                        modifier = Modifier
                            .size(Size.avatarMd)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Palette.surfaceTintStrong, Palette.surfaceTintSoft)))
                            .border(1.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                            .clickable { navController.navigate("user_profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarPath = currentUser?.profileImageUri
                        if (!avatarPath.isNullOrBlank()) {
                            AsyncImage(
                                model = File(avatarPath),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(Size.avatarMd).clip(CircleShape)
                            )
                        } else {
                            val initial = (currentUser?.name?.trim()?.firstOrNull() ?: 'G').uppercase()
                            Text(
                                initial,
                                fontSize = Type.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Palette.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // ── Weather + Stats row ───────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.lg)
                    .padding(bottom = Space.xs),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                GlassCard(modifier = Modifier.weight(1f), cornerRadius = Radii.card) {
                    Column(modifier = Modifier.padding(Space.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Cloud, null, tint = Color(0xFFB3E5FC), modifier = Modifier.size(Size.iconMd))
                            Spacer(modifier = Modifier.width(Space.xs + 2.dp))
                            Text(weather.cityName, fontSize = Type.caption, color = Palette.textSecondary)
                        }
                        Spacer(modifier = Modifier.height(Space.sm))
                        Text("${weather.temperature.toInt()}°C", fontSize = Type.displayMedium, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                        Text(weather.description.ifEmpty { "Loading..." }, fontSize = Type.caption, color = Palette.textTertiary)
                        Spacer(modifier = Modifier.height(Space.xs))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = Palette.textTertiary, modifier = Modifier.size(Size.iconSm - 2.dp))
                            Spacer(modifier = Modifier.width(Space.xs))
                            Text(currentTime, fontSize = Type.micro, color = Palette.textTertiary)
                        }
                        Spacer(modifier = Modifier.height(Space.xs))
                        Text(weatherViewModel.getWateringAdvice(), fontSize = Type.micro, color = Palette.accentAlt, fontWeight = FontWeight.Medium)
                    }
                }

                GlassCard(modifier = Modifier.weight(1f), cornerRadius = Radii.card) {
                    // Mirrors the MyPlants filter chips:
                    //   Sick     = plant has an unresolved Disease log
                    //   Critical = same, log older than 7 days
                    //   Healthy  = no active disease
                    val sevenDaysMs = 7L * 86_400_000L
                    val nowMs = System.currentTimeMillis()
                    val sickCount = plants.count { activeDiseases[it.id] != null }
                    val criticalCount = plants.count {
                        val d = activeDiseases[it.id]
                        d != null && (nowMs - d.timestamp) > sevenDaysMs
                    }
                    val healthyCount = plants.size - sickCount
                    Column(modifier = Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.sm + 2.dp)) {
                        GlassStatRow("Total Plants", "${plants.size}", Palette.accentAlt)
                        GlassStatRow("Healthy", "$healthyCount", Palette.accent)
                        GlassStatRow("Sick", "$sickCount", Palette.warn)
                        GlassStatRow("Critical", "$criticalCount", Palette.danger)
                    }
                }
            }
        }

        // ── Quick Actions ─────────────────────────────────────────────────
        item {
            SectionHeader(title = "Quick Actions")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                GlassQuickActionCard(Icons.Filled.CameraAlt, "Identify\nPlant", Palette.accentAlt, Modifier.weight(1f)) { navController.navigate("plant_identification") }
                GlassQuickActionCard(Icons.Filled.BugReport, "Diagnose\nDisease", Palette.danger, Modifier.weight(1f)) { navController.navigate("disease_diagnosis") }
                GlassQuickActionCard(Icons.Filled.CalendarMonth, "Care\nSchedule", Palette.info, Modifier.weight(1f)) { navController.navigate("care_schedule") }
            }
        }

        // ── My Plants header ──────────────────────────────────────────────
        item {
            SectionHeader(
                title = "My Plants",
                trailing = {
                    Text(
                        "See all",
                        fontSize = Type.bodyMedium,
                        color = Palette.accentAlt,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { navController.navigate("my_plants") }
                    )
                }
            )
        }

        // ── Plant carousel ────────────────────────────────────────────────
        item {
            if (plants.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Eco,
                    title = "No plants yet",
                    body = "Identify a plant to add it to your collection.",
                    primaryLabel = "Identify a plant",
                    onPrimary = { navController.navigate("plant_identification") }
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.md),
                    horizontalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    items(plants) { plant ->
                        val colorPair = MockData.plantGradientColors[plant.id % MockData.plantGradientColors.size]
                        GlassDashboardPlantCard(
                            plant = plant,
                            gradientStart = colorPair.first,
                            gradientEnd = colorPair.second,
                            onClick = { navController.navigate("plant_detail/${plant.id}") }
                        )
                    }
                }
            }
        }

        // ── Upcoming Tasks header ─────────────────────────────────────────
        item {
            SectionHeader(
                title = "Upcoming Tasks",
                trailing = {
                    Text(
                        "See all",
                        fontSize = Type.bodyMedium,
                        color = Palette.accentAlt,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { navController.navigate("care_schedule") }
                    )
                }
            )
        }

        // ── Task items ────────────────────────────────────────────────────
        if (upcomingTasks.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(Space.xxl), contentAlignment = Alignment.Center) {
                    Text("No upcoming tasks", fontSize = Type.bodyMedium, color = Palette.textTertiary)
                }
            }
        } else {
            items(upcomingTasks) { task ->
                val colorPair = MockData.plantGradientColors[task.colorIndex % MockData.plantGradientColors.size]
                GlassDashboardTaskItem(
                    task = task,
                    plantColor = colorPair.first,
                    modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.xs)
                )
            }
        }
    }
    }  // PullToRefreshBox close
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun GlassStatRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = Type.caption, color = Palette.textTertiary)
        Text(value, fontSize = Type.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun GlassQuickActionCard(icon: ImageVector, label: String, accentColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier.clickable { onClick() }, cornerRadius = Radii.card - 2.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = Space.lg, horizontal = Space.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.20f)).border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, label, tint = accentColor, modifier = Modifier.size(Size.iconLg + 2.dp))
            }
            Spacer(modifier = Modifier.height(Space.sm))
            Text(label, fontSize = Type.micro, fontWeight = FontWeight.Medium, color = accentColor, lineHeight = 15.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GlassDashboardPlantCard(plant: Plant, gradientStart: Color, gradientEnd: Color, onClick: () -> Unit) {
    val severity = FloraDesign.severityColor(plant.healthStatus)
    GlassBox(modifier = Modifier.width(120.dp).clickable { onClick() }, cornerRadius = Radii.card - 2.dp) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(topStart = Radii.card - 2.dp, topEnd = Radii.card - 2.dp))
                    .background(Brush.verticalGradient(listOf(gradientStart, gradientEnd))),
                contentAlignment = Alignment.Center
            ) {
                val imagePath = plant.imageUri
                if (!imagePath.isNullOrBlank() && File(imagePath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(File(imagePath)).crossfade(true).build(),
                        contentDescription = plant.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.Eco, plant.name, tint = Palette.textPrimary.copy(alpha = 0.90f), modifier = Modifier.size(Size.iconXl + 8.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd).padding(Space.xs + 2.dp).size(10.dp).clip(CircleShape)
                        .background(severity)
                )
            }
            Column(modifier = Modifier.padding(Space.md - 2.dp)) {
                Text(plant.name, fontSize = Type.bodySmall, fontWeight = FontWeight.Bold, color = Palette.textPrimary, maxLines = 1)
                Text(plant.healthStatus, fontSize = Type.micro, color = severity)
            }
        }
    }
}

@Composable
private fun GlassDashboardTaskItem(task: CareSchedule, plantColor: Color, modifier: Modifier = Modifier) {
    val taskIcon = when (task.taskType) {
        "Water", "Mist", "Water Change" -> Icons.Filled.WaterDrop
        "Fertilize" -> Icons.Filled.Eco
        "Prune" -> Icons.Filled.Eco
        else -> Icons.Filled.CheckCircle
    }
    val taskColor = when (task.taskType) {
        "Water", "Mist", "Water Change" -> Palette.info
        "Fertilize" -> Palette.accent
        else -> Palette.accentAlt
    }
    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(task.nextDueDate))

    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = Radii.chip + 4.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(Space.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(plantColor.copy(alpha = 0.25f)).border(1.dp, plantColor.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Eco, task.plantName, tint = Palette.textPrimary, modifier = Modifier.size(Size.iconLg - 2.dp))
            }
            Spacer(modifier = Modifier.width(Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.plantName, fontSize = Type.bodyMedium, fontWeight = FontWeight.SemiBold, color = Palette.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Icon(taskIcon, null, tint = taskColor, modifier = Modifier.size(Size.iconSm - 2.dp))
                    Text(task.taskType, fontSize = Type.caption, color = taskColor)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(dateStr, fontSize = Type.caption, fontWeight = FontWeight.Medium, color = Palette.textSecondary)
                Text(task.dueTime, fontSize = Type.micro, color = Palette.textTertiary)
            }
        }
    }
}
