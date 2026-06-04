package com.example.flora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flora.data.database.entities.Plant
import com.example.flora.ui.components.EmptyState
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.data.MockData
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.CareScheduleViewModel
import com.example.flora.ui.viewmodel.PlantViewModel
import com.example.flora.data.database.entities.CareSchedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPlantsScreen(
    navController: NavController,
    plantViewModel: PlantViewModel,
    careScheduleViewModel: CareScheduleViewModel,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Needs Watering", "Sick", "Critical")

    val allPlants by plantViewModel.plants.collectAsState()
    val careTasks by careScheduleViewModel.allTasks.collectAsState()
    val activeDiseases by plantViewModel.activeDiseases.collectAsState()

    // Multi-select edit mode. When `editMode` is on, cards toggle selection instead
    // of navigating, and the top bar exposes Select-All + bulk delete.
    var editMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    // Sick = has active (unresolved) disease log. Critical = sick for > 7 days.
    val sevenDaysMs = 7L * 86_400_000L
    val nowMs = System.currentTimeMillis()

    if (showBulkDeleteDialog) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Delete $count plant${if (count == 1) "" else "s"}?") },
            text = { Text("This permanently removes the selected plant${if (count == 1) "" else "s"}, plus their photos, health logs, and watering schedules. Cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        plantViewModel.deletePlants(selectedIds)
                        showBulkDeleteDialog = false
                        selectedIds = emptySet()
                        editMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.danger)
                ) { Text("Delete", color = Palette.textPrimary) }
            },
            dismissButton = {
                Button(
                    onClick = { showBulkDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.surfaceTintMid)
                ) { Text("Cancel", color = Palette.textPrimary) }
            }
        )
    }

    // Calendar-day delta between two timestamps (uses local TZ midnight boundaries).
    fun calendarDayDelta(fromTs: Long, toTs: Long): Int {
        fun startOfDay(ts: Long): Long = java.util.Calendar.getInstance().apply {
            timeInMillis = ts
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((startOfDay(toTs) - startOfDay(fromTs)) / 86_400_000L).toInt()
    }

    // Functional search + filter
    val filteredPlants = allPlants.filter { plant ->
        val matchesSearch = searchQuery.isBlank() ||
                plant.name.contains(searchQuery, ignoreCase = true) ||
                plant.species.contains(searchQuery, ignoreCase = true) ||
                plant.malayName.contains(searchQuery, ignoreCase = true)
        val disease = activeDiseases[plant.id]
        val isSick = disease != null
        val isCritical = disease != null && (nowMs - disease.timestamp) > sevenDaysMs
        val needsWaterToday = run {
            val task = careTasks.firstOrNull {
                it.plantId == plant.id && it.taskType.equals("Water", ignoreCase = true)
            }
            val nextDueAt = task?.nextDueDate
                ?: ((plant.lastWatered ?: plant.dateAdded) + plant.wateringFrequencyDays.coerceAtLeast(1) * 86_400_000L)
            calendarDayDelta(nowMs, nextDueAt) <= 0
        }
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "Needs Watering" -> needsWaterToday
            "Sick" -> isSick
            "Critical" -> isCritical
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editMode) "${selectedIds.size} selected" else "My Plants",
                        fontWeight = FontWeight.Bold,
                        color = Palette.textPrimary,
                    )
                },
                navigationIcon = {
                    if (editMode) {
                        IconButton(onClick = {
                            editMode = false
                            selectedIds = emptySet()
                        }) { Icon(Icons.Filled.Close, "Exit edit", tint = Palette.textPrimary) }
                    }
                },
                actions = {
                    if (editMode) {
                        val allSelected = filteredPlants.isNotEmpty() &&
                            filteredPlants.all { it.id in selectedIds }
                        IconButton(onClick = {
                            selectedIds = if (allSelected) {
                                selectedIds - filteredPlants.map { it.id }.toSet()
                            } else {
                                selectedIds + filteredPlants.map { it.id }
                            }
                        }) {
                            Icon(
                                Icons.Filled.SelectAll,
                                if (allSelected) "Deselect all" else "Select all",
                                tint = if (allSelected) Palette.accent else Palette.textPrimary,
                            )
                        }
                        IconButton(
                            onClick = { if (selectedIds.isNotEmpty()) showBulkDeleteDialog = true },
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                "Delete selected",
                                tint = if (selectedIds.isEmpty()) Palette.textMuted else Palette.danger,
                            )
                        }
                    } else {
                        Text("${allPlants.size} plants", fontSize = Type.bodySmall, color = Palette.textSecondary)
                        if (allPlants.isNotEmpty()) {
                            IconButton(onClick = { editMode = true }) {
                                Icon(Icons.Filled.Edit, "Edit plants", tint = Palette.textPrimary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("plant_identification") },
                containerColor = Palette.surfaceTintStrong,
                contentColor = Palette.textPrimary
            ) {
                Icon(Icons.Filled.Add, "Add Plant")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Glass search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search plants…", fontSize = Type.bodyMedium, color = Palette.textTertiary) },
                    leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = Palette.textSecondary) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md),
                    shape = RoundedCornerShape(Radii.card),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Palette.textPrimary,
                        unfocusedTextColor = Palette.textPrimary.copy(alpha = 0.85f),
                        focusedBorderColor = Palette.textPrimary.copy(alpha = 0.65f),
                        unfocusedBorderColor = Palette.surfaceTintStrong,
                        focusedContainerColor = Palette.surfaceTintSoft,
                        unfocusedContainerColor = Palette.surfaceTintSoft.copy(alpha = 0.07f),
                        cursorColor = Palette.textPrimary
                    )
                )
            }

            // Glass filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.lg),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    modifier = Modifier.padding(bottom = Space.sm)
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = Type.bodySmall, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White.copy(alpha = 0.35f),
                                selectedLabelColor = Palette.textPrimary,
                                containerColor = Color.White.copy(alpha = 0.10f),
                                labelColor = Palette.textTertiary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = isSelected,
                                borderColor = Palette.surfaceTintStrong,
                                selectedBorderColor = Color.White.copy(alpha = 0.55f)
                            )
                        )
                    }
                }
            }

            if (allPlants.isNotEmpty()) {
                item {
                    Text(
                        "${filteredPlants.size} plant${if (filteredPlants.size != 1) "s" else ""} found",
                        fontSize = Type.bodySmall,
                        color = Palette.textTertiary,
                        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.xs)
                    )
                }
            }

            items(filteredPlants) { plant ->
                val colorPair = MockData.plantGradientColors[plant.id % MockData.plantGradientColors.size]
                // Authoritative due date for "Water in Nd" / "Water today" comes
                // from the live Care Schedule task, not the Plant row. This way
                // the two screens never diverge after auto-renew or undo.
                val waterTask = careTasks.firstOrNull {
                    it.plantId == plant.id && it.taskType.equals("Water", ignoreCase = true)
                }
                val isSelected = plant.id in selectedIds
                val rowToggle = {
                    selectedIds = if (isSelected) selectedIds - plant.id else selectedIds + plant.id
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs + 2.dp),
                ) {
                    if (editMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { rowToggle() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Palette.accent,
                                uncheckedColor = Palette.textMuted,
                                checkmarkColor = Palette.textPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.width(Space.xs))
                    }
                    GlassPlantListCard(
                        plant = plant,
                        waterTask = waterTask,
                        activeDisease = activeDiseases[plant.id],
                        gradientStart = colorPair.first,
                        gradientEnd = colorPair.second,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (editMode) rowToggle()
                                else navController.navigate("plant_detail/${plant.id}")
                            }
                    )
                }
            }

            if (filteredPlants.isEmpty()) {
                item {
                    if (allPlants.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.Eco,
                            title = "No plants yet",
                            body = "Identify your first plant to start tracking its care, health, and watering schedule.",
                            primaryLabel = "Identify a plant",
                            onPrimary = { navController.navigate("plant_identification") }
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = "No matches",
                            body = "Try a different search or clear the filter to see all plants.",
                            accent = Palette.accentAlt
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassPlantListCard(
    plant: Plant,
    waterTask: CareSchedule?,
    activeDisease: com.example.flora.data.database.entities.PlantHealthLog? = null,
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    // Calendar-day delta so "yesterday/today" matches the calendar, not a 24h window.
    // Without this, watering at 11pm and reading the card at 8am next day would still
    // say "Watered today" (only 9h elapsed).
    fun startOfDay(ts: Long): Long = java.util.Calendar.getInstance().apply {
        timeInMillis = ts
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val lastWateredText = if (plant.lastWatered != null) {
        val diff = ((startOfDay(now) - startOfDay(plant.lastWatered)) / 86_400_000L).toInt()
        when {
            diff <= 0 -> "Watered today"
            diff == 1 -> "Watered yesterday"
            else -> "Watered ${diff}d ago"
        }
    } else "Not yet watered"

    // Pull the next-due timestamp from the live Care Schedule task so this card
    // always agrees with the Schedule screen. Fall back to plant.lastWatered +
    // wateringFrequencyDays only if there's no task row (shouldn't happen for
    // user-saved plants but defensive).
    val nextDueAt = waterTask?.nextDueDate
        ?: ((plant.lastWatered ?: plant.dateAdded) + plant.wateringFrequencyDays.coerceAtLeast(1) * 86_400_000L)
    // Calendar-day delta (not float-time delta) so May 13 → May 15 reads as 2 days,
    // matching the date headers on the Care Schedule screen.
    val daysUntilDue = ((startOfDay(nextDueAt) - startOfDay(now)) / 86_400_000L).toInt()
    val nextDueText: String
    val nextDueColor: Color
    when {
        daysUntilDue < 0 -> {
            nextDueText = "Overdue ${-daysUntilDue}d"
            nextDueColor = Palette.danger
        }
        daysUntilDue == 0 -> {
            nextDueText = "Water today"
            nextDueColor = Palette.warn
        }
        daysUntilDue == 1 -> {
            nextDueText = "Water in 1d"
            nextDueColor = Palette.info
        }
        else -> {
            nextDueText = "Water in ${daysUntilDue}d"
            nextDueColor = Palette.info
        }
    }

    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = Radii.card) {
        Row(modifier = Modifier.fillMaxWidth().padding(Space.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(Radii.chip + 4.dp)).background(Brush.verticalGradient(listOf(gradientStart, gradientEnd))),
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
                    Icon(Icons.Filled.Eco, plant.name, tint = Palette.textPrimary, modifier = Modifier.size(Size.avatarSm))
                }
            }
            Spacer(modifier = Modifier.width(Space.md + 2.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.name, fontSize = Type.titleSmall, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                Text(plant.species, fontSize = Type.caption, color = Palette.textTertiary, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 2.dp))
                if (plant.malayName.isNotEmpty()) {
                    Text(plant.malayName, fontSize = Type.caption, color = Palette.textTertiary)
                }
                Spacer(modifier = Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WaterDrop, null, tint = Palette.info, modifier = Modifier.size(Size.iconSm - 2.dp))
                        Spacer(modifier = Modifier.width(Space.xs))
                        Text(lastWateredText, fontSize = Type.micro, color = Palette.textTertiary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = Palette.accent, modifier = Modifier.size(Size.iconSm - 2.dp))
                        Spacer(modifier = Modifier.width(Space.xs))
                        Text(plant.location, fontSize = Type.micro, color = Palette.textTertiary)
                    }
                }
                Spacer(modifier = Modifier.height(Space.xs + 2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    // Next-due chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radii.chip))
                            .background(nextDueColor.copy(alpha = 0.18f))
                            .border(1.dp, nextDueColor.copy(alpha = 0.40f), RoundedCornerShape(Radii.chip))
                            .padding(horizontal = Space.sm, vertical = 2.dp),
                    ) {
                        Text(nextDueText, fontSize = Type.micro, fontWeight = FontWeight.SemiBold, color = nextDueColor)
                    }
                    // Active disease chip (if any logged + unresolved)
                    activeDisease?.let { d ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radii.chip))
                                .background(Palette.danger.copy(alpha = 0.18f))
                                .border(1.dp, Palette.danger.copy(alpha = 0.40f), RoundedCornerShape(Radii.chip))
                                .padding(horizontal = Space.sm, vertical = 2.dp),
                        ) {
                            Text(
                                "${d.diseaseName ?: "Disease"}",
                                fontSize = Type.micro,
                                fontWeight = FontWeight.SemiBold,
                                color = Palette.danger,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
