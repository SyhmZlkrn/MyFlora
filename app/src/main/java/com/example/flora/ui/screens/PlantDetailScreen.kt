package com.example.flora.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.flora.util.BitmapStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flora.data.database.entities.Plant
import com.example.flora.ui.components.EmptyState
import com.example.flora.ui.components.FloraPrimaryButton
import com.example.flora.ui.components.FloraSecondaryButton
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.data.MockData
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.AuthViewModel
import com.example.flora.ui.viewmodel.CareScheduleViewModel
import com.example.flora.ui.viewmodel.PlantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    navController: NavController,
    plantId: Int,
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel,
    careScheduleViewModel: CareScheduleViewModel,
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isGuest = currentUser == null

    var plant by remember { mutableStateOf<Plant?>(null) }
    val logs by plantViewModel.getLogsByPlant(plantId).collectAsState(initial = emptyList())
    // Pull the live care schedule so Quick Info's "Next watering" matches the
    // schedule screen instead of the static plant.lastWatered + freq estimate.
    val allTasks by careScheduleViewModel.allTasks.collectAsState()

    LaunchedEffect(plantId, currentUser?.id) {
        plant = plantViewModel.getPlantById(plantId)
    }

    if (plant == null) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = Space.section * 2)) {
            item {
                EmptyState(
                    icon = if (isGuest) Icons.Filled.Lock else Icons.Filled.Eco,
                    title = if (isGuest) "Sign in to view saved plants" else "Plant not available",
                    body = if (isGuest) "Guest mode does not include a personal plant collection." else "This plant either does not exist or belongs to another account.",
                    primaryLabel = if (isGuest) "Sign in" else "Go back",
                    onPrimary = { if (isGuest) navController.navigate("login") else navController.popBackStack() }
                )
            }
        }
        return
    }

    val p = plant!!

    val colorPair = MockData.plantGradientColors[p.id % MockData.plantGradientColors.size]
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Care Info", "Health Logs")

    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val newPath = withContext(Dispatchers.IO) { BitmapStorage.saveFromUri(context, uri) }
                if (newPath != null) {
                    plantViewModel.updatePlantImage(p.id, newPath)
                    plant = plantViewModel.getPlantById(p.id)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${p.name}?") },
            text = { Text("This permanently removes the plant, its photo, and all its health logs and schedules.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        plantViewModel.deletePlant(p)
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.danger)
                ) { Text("Delete", color = Palette.textPrimary) }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.surfaceTintMid)
                ) { Text("Cancel", color = Palette.textPrimary) }
            }
        )
    }

    if (showEditDialog) {
        EditPlantDialog(
            plant = p,
            onDismiss = { showEditDialog = false },
            onChangePhoto = {
                photoPicker.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onSave = { updated ->
                plantViewModel.updatePlant(updated)
                plant = updated
                showEditDialog = false
            },
        )
    }

    // Calendar-day delta so "Today/Yesterday" follow the calendar, not a 24h window.
    fun startOfDay(ts: Long): Long = java.util.Calendar.getInstance().apply {
        timeInMillis = ts
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val nowMs = System.currentTimeMillis()

    val lastWateredText = if (p.lastWatered != null) {
        val diff = ((startOfDay(nowMs) - startOfDay(p.lastWatered)) / 86_400_000L).toInt()
        when { diff <= 0 -> "Today"; diff == 1 -> "Yesterday"; else -> "$diff days ago" }
    } else "Not tracked"

    // Authoritative next-due comes from the live water CareSchedule for this plant.
    // After a tick the schedule advances by intervalDays — pulling from there keeps
    // Quick Info aligned with the Care Schedule screen instead of computing a stale
    // value off plant.lastWatered + freq (which "overdue"s the moment the gap is
    // even slightly exceeded, even if the schedule has already rolled forward).
    val waterTask = allTasks.firstOrNull {
        it.plantId == p.id && it.taskType.equals("Water", ignoreCase = true)
    }
    val nextWateringText = when {
        waterTask != null -> {
            val diff = ((startOfDay(waterTask.nextDueDate) - startOfDay(nowMs)) / 86_400_000L).toInt()
            when {
                diff < 0 -> "Overdue ${-diff}d"
                diff == 0 -> "Today"
                diff == 1 -> "Tomorrow"
                else -> "In $diff days"
            }
        }
        p.lastWatered != null -> {
            val next = p.lastWatered + p.wateringFrequencyDays * 86_400_000L
            val diff = ((startOfDay(next) - startOfDay(nowMs)) / 86_400_000L).toInt()
            when {
                diff < 0 -> "Overdue ${-diff}d"
                diff == 0 -> "Today"
                diff == 1 -> "Tomorrow"
                else -> "In $diff days"
            }
        }
        else -> "Unknown"
    }

    val dateAdded = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(p.dateAdded))

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        // Hero
        item {
            Box(modifier = Modifier.fillMaxWidth().height(Size.heroImage)) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colorPair.first, colorPair.second))), contentAlignment = Alignment.Center) {
                    val imagePath = p.imageUri
                    if (!imagePath.isNullOrBlank() && java.io.File(imagePath).exists()) {
                        coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(java.io.File(imagePath)).crossfade(true).build(),
                            contentDescription = p.name,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Filled.Eco, null, tint = Palette.textPrimary.copy(alpha = 0.35f), modifier = Modifier.size(130.dp))
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.20f)))))
                TopAppBar(
                    title = { }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.textPrimary) } },
                    actions = {
                        IconButton(onClick = {
                            photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Icon(Icons.Filled.CameraAlt, "Change photo", tint = Palette.textPrimary) }
                        IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Filled.Edit, "Edit plant", tint = Palette.textPrimary) }
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Filled.Delete, "Delete", tint = Palette.danger) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent), modifier = Modifier.align(Alignment.TopCenter)
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = Space.xl, end = Space.xl, bottom = Space.xl)) {
                    Text(p.name, fontSize = Type.displayMedium, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                    Text(p.species, fontSize = Type.bodyMedium, color = Palette.textSecondary, fontStyle = FontStyle.Italic)
                }
            }
        }

        // Chips
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                GlassChip(Icons.Filled.LocationOn, p.location, Palette.accent)
                GlassChip(Icons.Filled.CalendarMonth, dateAdded, Palette.accentAlt)
                if (p.malayName.isNotEmpty()) GlassChip(Icons.Filled.Eco, p.malayName, Color(0xFFCE93D8))
            }
        }

        // Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Palette.textPrimary, edgePadding = Space.lg,
                indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Palette.textPrimary, height = 2.dp) },
                divider = { Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.surfaceTintSoft)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == index) Palette.textPrimary else Palette.textTertiary) })
                }
            }
        }

        val activeDisease = logs.firstOrNull { it.logType == "Disease" && !it.isResolved }

        when (selectedTab) {
            0 -> {
                // Active disease banner (if any) — replaces the old "Health status" card.
                // Tap anywhere on the banner → switch to Care Info tab to view treatment.
                if (activeDisease != null) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Space.lg)
                                .clickable { selectedTab = 1 },
                            cornerRadius = Radii.card,
                        ) {
                            Column(modifier = Modifier.padding(Space.xl)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(44.dp).clip(CircleShape).background(Palette.danger.copy(alpha = 0.18f)).border(2.dp, Palette.danger.copy(alpha = 0.55f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.BugReport, null, tint = Palette.danger, modifier = Modifier.size(Size.iconLg))
                                    }
                                    Spacer(modifier = Modifier.width(Space.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Active disease", fontSize = Type.caption, color = Palette.textTertiary, fontWeight = FontWeight.SemiBold)
                                        Text(activeDisease.diseaseName ?: "Unknown", fontSize = Type.titleSmall, fontWeight = FontWeight.Bold, color = Palette.danger)
                                        Text("Tap for care guide", fontSize = Type.micro, color = Palette.textTertiary)
                                    }
                                    androidx.compose.material3.TextButton(onClick = { plantViewModel.markDiseaseResolved(activeDisease) }) {
                                        Text("Mark resolved", color = Palette.accent, fontSize = Type.caption, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg), cornerRadius = Radii.card) {
                        Column(modifier = Modifier.padding(Space.xl)) {
                            Text("Quick info", fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                            Spacer(modifier = Modifier.height(Space.md))
                            GlassInfoRow(Icons.Filled.WaterDrop, "Last watered", lastWateredText, Palette.info)
                            GlassInfoRow(Icons.Filled.CalendarMonth, "Next watering", nextWateringText, Palette.accent)
                            GlassInfoRow(Icons.Filled.LocationOn, "Location", p.location, Color(0xFFCE93D8))
                        }
                    }
                }
                if (p.notes.isNotEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(Space.lg), cornerRadius = Radii.card) {
                            Column(modifier = Modifier.padding(Space.xl)) {
                                Text("Notes", fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                                Spacer(modifier = Modifier.height(Space.sm))
                                Text(p.notes, fontSize = Type.bodyMedium, color = Palette.textSecondary, lineHeight = 21.sp)
                            }
                        }
                    }
                }
            }
            1 -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(Space.lg), cornerRadius = Radii.card) {
                        Column(modifier = Modifier.padding(Space.xl)) {
                            Text("Care requirements", fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                            Spacer(modifier = Modifier.height(Space.md))
                            GlassInfoRow(Icons.Filled.WaterDrop, "Watering", "Every ${p.wateringFrequencyDays} days", Palette.info)
                            GlassInfoRow(Icons.Filled.WbSunny, "Sunlight", p.sunlight, Palette.warn)
                            GlassInfoRow(Icons.Filled.Eco, "Fertilizer", p.fertilizer, Palette.accent)
                            GlassInfoRow(Icons.Filled.LocationOn, "Location", p.location, Color(0xFFCE93D8))
                        }
                    }
                }

                // If the plant has an unresolved disease, pull treatment + prevention
                // from the diseases catalog and render it as a Disease Care card.
                if (activeDisease != null) {
                    item {
                        var disease by remember(activeDisease.id, activeDisease.diseaseName) {
                            mutableStateOf<com.example.flora.data.database.entities.Disease?>(null)
                        }
                        LaunchedEffect(activeDisease.id, activeDisease.diseaseName) {
                            val name = activeDisease.diseaseName
                            if (!name.isNullOrBlank()) disease = plantViewModel.getDiseaseByName(name)
                        }
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm), cornerRadius = Radii.card) {
                            Column(modifier = Modifier.padding(Space.xl)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(Palette.danger.copy(alpha = 0.18f)).border(1.dp, Palette.danger.copy(alpha = 0.45f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.BugReport, null, tint = Palette.danger, modifier = Modifier.size(Size.iconMd))
                                    }
                                    Spacer(modifier = Modifier.width(Space.md))
                                    Column {
                                        Text("Disease care", fontSize = Type.caption, color = Palette.textTertiary, fontWeight = FontWeight.SemiBold)
                                        Text(activeDisease.diseaseName ?: "Unknown", fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.danger)
                                    }
                                }
                                Spacer(modifier = Modifier.height(Space.md))
                                val d = disease
                                if (d == null) {
                                    Text(
                                        "No catalog entry for this disease. Add a matching entry to the diseases table to show treatment guidance here.",
                                        fontSize = Type.bodySmall,
                                        color = Palette.textTertiary,
                                        lineHeight = 19.sp,
                                    )
                                } else {
                                    if (d.symptoms.isNotBlank()) {
                                        DiseaseCareBlock("Symptoms", d.symptoms, Palette.warn)
                                    }
                                    if (d.treatment.isNotBlank()) {
                                        DiseaseCareBlock("Treatment", d.treatment, Palette.accent)
                                    }
                                    if (d.prevention.isNotBlank()) {
                                        DiseaseCareBlock("Prevention", d.prevention, Palette.info)
                                    }
                                    if (d.affectedParts.isNotBlank()) {
                                        DiseaseCareBlock("Affected parts", d.affectedParts, Color(0xFFCE93D8))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                item {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

                    // Three log buckets — watering / fertilizer / disease.
                    val waterLogs = logs.filter { it.logType.equals("Water", ignoreCase = true) }
                    val fertLogs = logs.filter { it.logType.equals("Fertilize", ignoreCase = true) }
                    val diseaseLogs = logs.filter { it.logType.equals("Disease", ignoreCase = true) }

                    @androidx.compose.runtime.Composable
                    fun LogSection(title: String, sectionLogs: List<com.example.flora.data.database.entities.PlantHealthLog>, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
                        if (sectionLogs.isEmpty()) return
                        Text(
                            title,
                            fontSize = Type.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Palette.textPrimary,
                            modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm),
                        )
                        sectionLogs.take(10).forEach { log ->
                            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs), cornerRadius = Radii.card - 4.dp) {
                                Row(modifier = Modifier.fillMaxWidth().padding(Space.md + 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)).border(1.dp, accent.copy(alpha = 0.40f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(icon, null, tint = accent, modifier = Modifier.size(Size.iconLg - 4.dp))
                                    }
                                    Spacer(modifier = Modifier.width(Space.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            log.diseaseName ?: log.logType,
                                            fontSize = Type.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = accent,
                                        )
                                        Text(
                                            dateFormat.format(Date(log.timestamp)),
                                            fontSize = Type.caption,
                                            color = Palette.textTertiary,
                                        )
                                        if (!log.notes.isNullOrBlank()) {
                                            Text(log.notes, fontSize = Type.micro, color = Palette.textTertiary, maxLines = 2)
                                        }
                                    }
                                    androidx.compose.material3.IconButton(
                                        onClick = { plantViewModel.deleteHealthLog(log.id) },
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            "Delete log",
                                            tint = Palette.danger.copy(alpha = 0.75f),
                                            modifier = Modifier.size(Size.iconSm),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LogSection("Disease history", diseaseLogs, Palette.danger, Icons.Filled.BugReport)
                    LogSection("Watering history", waterLogs, Palette.info, Icons.Filled.WaterDrop)
                    LogSection("Fertilizer history", fertLogs, Palette.accent, Icons.Filled.Eco)

                    if (logs.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.History,
                            title = "No history yet",
                            body = "Tick care tasks or log a disease to build this plant's history."
                        )
                    }

                    Spacer(modifier = Modifier.height(Space.md))
                    Box(modifier = Modifier.padding(horizontal = Space.lg)) {
                        if (isGuest) {
                            FloraSecondaryButton(
                                label = "Sign in to view health logs",
                                onClick = { navController.navigate("login") },
                                icon = Icons.Filled.Lock
                            )
                        } else {
                            FloraPrimaryButton(
                                label = "View all health logs",
                                onClick = { navController.navigate("plant_logs/${p.id}") },
                                icon = Icons.Filled.History,
                                accent = Palette.accentAlt
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassChip(icon: ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(Radii.chip + 10.dp)).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(Radii.chip + 10.dp)).padding(horizontal = Space.md - 2.dp, vertical = Space.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(Size.iconSm - 2.dp))
        Spacer(modifier = Modifier.width(Space.xs))
        Text(label, fontSize = Type.caption, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GlassInfoRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs + 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(Size.iconMd))
        Spacer(modifier = Modifier.width(Space.md))
        Text(label, fontSize = Type.bodySmall, color = Palette.textTertiary, modifier = Modifier.width(100.dp))
        Text(value, fontSize = Type.bodySmall, color = Palette.textPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DiseaseCareBlock(title: String, body: String, accent: Color) {
    Spacer(modifier = Modifier.height(Space.sm))
    Text(title, fontSize = Type.bodySmall, fontWeight = FontWeight.Bold, color = accent)
    Spacer(modifier = Modifier.height(2.dp))
    Text(body, fontSize = Type.bodySmall, color = Palette.textSecondary, lineHeight = 19.sp)
}

@Composable
private fun EditPlantDialog(
    plant: Plant,
    onDismiss: () -> Unit,
    onChangePhoto: () -> Unit,
    onSave: (Plant) -> Unit,
) {
    var name by remember { mutableStateOf(plant.name) }
    var location by remember { mutableStateOf(plant.location) }
    var sunlight by remember { mutableStateOf(plant.sunlight) }
    var fertilizer by remember { mutableStateOf(plant.fertilizer) }
    var notes by remember { mutableStateOf(plant.notes) }
    var wateringDays by remember { mutableStateOf(plant.wateringFrequencyDays.toString()) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${plant.name}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onClick = onChangePhoto) {
                    Icon(Icons.Filled.CameraAlt, null, tint = Palette.accent)
                    Spacer(modifier = Modifier.width(Space.xs))
                    Text("Change photo", color = Palette.accent, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(Space.sm))
                EditField(label = "Name", value = name) { name = it }
                EditField(label = "Location", value = location) { location = it }
                EditField(
                    label = "Watering every (days)",
                    value = wateringDays,
                    keyboardType = KeyboardType.Number,
                ) { input -> wateringDays = input.filter { it.isDigit() }.take(3) }
                EditField(label = "Sunlight", value = sunlight) { sunlight = it }
                EditField(label = "Fertilizer", value = fertilizer) { fertilizer = it }
                EditField(label = "Notes", value = notes, singleLine = false) { notes = it }
                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Space.sm))
                    Text(error, color = Palette.danger, fontSize = Type.caption)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val freq = wateringDays.toIntOrNull()
                    when {
                        name.isBlank() -> error = "Name is required"
                        location.isBlank() -> error = "Location is required"
                        freq == null || freq < 1 -> error = "Watering days must be at least 1"
                        else -> onSave(
                            plant.copy(
                                name = name.trim(),
                                location = location.trim(),
                                sunlight = sunlight.trim().ifBlank { plant.sunlight },
                                fertilizer = fertilizer.trim().ifBlank { plant.fertilizer },
                                notes = notes.trim(),
                                wateringFrequencyDays = freq,
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Palette.accent),
            ) { Text("Save", color = Palette.textPrimary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.surfaceTintMid),
            ) { Text("Cancel", color = Palette.textPrimary) }
        },
    )
}

@Composable
private fun EditField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs),
        shape = RoundedCornerShape(Radii.chip),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Palette.textPrimary,
            unfocusedTextColor = Palette.textPrimary,
            focusedLabelColor = Palette.accent,
            unfocusedLabelColor = Palette.textTertiary,
            cursorColor = Palette.accent,
        ),
    )
}
