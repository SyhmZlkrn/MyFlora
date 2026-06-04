package com.example.flora.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.flora.data.database.entities.Disease
import com.example.flora.ui.components.FloraSecondaryButton
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.theme.FloraDesign
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.ClassificationViewModel
import com.example.flora.ui.viewmodel.DiagnosedDisease
import com.example.flora.ui.viewmodel.DiagnosisUiState
import com.example.flora.ui.viewmodel.PlantViewModel

/**
 * Disease menu — scrollable 2-column grid of every condition in the DB.
 * Filterable by category tab + free-text search. Tap a card to see full
 * treatment / prevention info. No camera, no on-device inference — the user
 * picks the matching condition directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseDiagnosisScreen(
    navController: NavController,
    classificationViewModel: ClassificationViewModel,
    plantViewModel: PlantViewModel,
) {
    val diagnosisState by classificationViewModel.diagnosisState.collectAsState()
    val myPlants by plantViewModel.plants.collectAsState()
    val allDiseases by classificationViewModel.allDiseases.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CATEGORY_ALL) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isResult = diagnosisState is DiagnosisUiState.Result || diagnosisState is DiagnosisUiState.Error

    // Distinct categories from the DB + "All" first.
    val categories = remember(allDiseases) {
        listOf(CATEGORY_ALL) + allDiseases.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filtered = remember(allDiseases, query, selectedCategory) {
        val q = query.trim().lowercase()
        allDiseases
            .filter { selectedCategory == CATEGORY_ALL || it.category == selectedCategory }
            .filter {
                q.isEmpty() ||
                    it.name.lowercase().contains(q) ||
                    it.symptoms.lowercase().contains(q) ||
                    it.affectedParts.lowercase().contains(q)
            }
            .sortedBy { it.name }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Disease Library", fontWeight = FontWeight.Bold, color = Palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost, modifier = Modifier.statusBarsPadding()) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Category tabs ──
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Space.lg, vertical = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    lazyRowItems(items = categories, key = { it }) { cat ->
                        CategoryChip(
                            label = cat,
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                        )
                    }
                }

                // ── Search field ──
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by symptom or name", color = Palette.textTertiary, fontSize = Type.bodySmall) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Palette.textSecondary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, "Clear", tint = Palette.textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Palette.textPrimary,
                        unfocusedTextColor = Palette.textPrimary,
                        focusedBorderColor = Palette.danger,
                        unfocusedBorderColor = Palette.surfaceTintStrong,
                        cursorColor = Palette.danger,
                    ),
                )

                // ── Grid: 2 columns ──
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No diseases match this filter.",
                            fontSize = Type.bodyMedium,
                            color = Palette.textTertiary,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Space.lg),
                        horizontalArrangement = Arrangement.spacedBy(Space.md),
                        verticalArrangement = Arrangement.spacedBy(Space.md),
                    ) {
                        items(items = filtered, key = { it.id }) { disease ->
                            DiseaseGridCard(
                                disease = disease,
                                onClick = { classificationViewModel.diagnoseByDisease(disease) },
                            )
                        }
                    }
                }
            }

            // ── Result sheet overlay ──
            AnimatedVisibility(
                visible = isResult,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                when (val state = diagnosisState) {
                    is DiagnosisUiState.Result -> {
                        ResultSheet(
                            matches = state.matches,
                            myPlants = myPlants,
                            onLogForPlant = { plantId, d ->
                                classificationViewModel.logDiagnosisForPlant(plantId, d)
                                val plantName = myPlants.firstOrNull { it.id == plantId }?.name ?: "plant"
                                scope.launch {
                                    snackbarHost.showSnackbar(
                                        message = "Logged ${d.displayName} for $plantName",
                                    )
                                }
                            },
                            onReset = { classificationViewModel.resetDiagnosis() },
                        )
                    }
                    is DiagnosisUiState.Error -> {
                        Box(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet))
                                .background(Brush.verticalGradient(listOf(Palette.sheetTop, Palette.sheetBottom)))
                                .padding(Space.xxl),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(state.message, fontSize = Type.titleSmall, color = Palette.textPrimary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(Space.lg))
                                FloraSecondaryButton(
                                    label = "Back to list",
                                    onClick = { classificationViewModel.resetDiagnosis() },
                                    icon = Icons.Filled.Refresh,
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radii.chip))
            .background(if (selected) Palette.danger.copy(alpha = 0.85f) else Palette.surfaceTintSoft)
            .border(
                1.dp,
                if (selected) Palette.danger else Palette.surfaceTintStrong,
                RoundedCornerShape(Radii.chip),
            )
            .clickable { onClick() }
            .padding(horizontal = Space.md, vertical = Space.sm),
    ) {
        Text(
            label,
            color = if (selected) Color.Black else Palette.textPrimary,
            fontSize = Type.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DiseaseGridCard(disease: Disease, onClick: () -> Unit) {
    val accent = when (disease.category) {
        "Fungal" -> Color(0xFFA98BD8)
        "Bacterial" -> Color(0xFFE57373)
        "Viral" -> Color(0xFFFFB74D)
        "Pest" -> Color(0xFF81C784)
        "Environmental" -> Color(0xFF64B5F6)
        "Nutritional" -> Color(0xFFFFD54F)
        else -> Palette.accentAlt
    }
    val imageUrl = com.example.flora.ml.DiseaseImageRefs.urlFor(disease.name)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = Radii.card - 4.dp,
    ) {
        Column(modifier = Modifier.padding(Space.sm)) {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(Radii.card - 6.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(Radii.card - 6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "${disease.name} reference",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Filled.BugReport,
                        null,
                        tint = accent,
                        modifier = Modifier.size(Size.iconXl),
                    )
                }
            }
            Spacer(Modifier.height(Space.sm))
            Text(
                disease.name,
                fontSize = Type.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Palette.textPrimary,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = Space.xs),
            )
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radii.chip - 2.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = Space.sm, vertical = 2.dp),
            ) {
                Text(
                    disease.category,
                    fontSize = Type.micro,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun ResultSheet(
    matches: List<DiagnosedDisease>,
    myPlants: List<com.example.flora.data.database.entities.Plant>,
    onLogForPlant: (Int, DiagnosedDisease) -> Unit,
    onReset: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet))
            .background(Brush.verticalGradient(listOf(Palette.sheetTop, Palette.sheetBottom)))
            .border(1.dp, Palette.danger.copy(alpha = 0.40f), RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Space.xxl)) {
            Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Palette.textMuted).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(Space.lg))

            matches.forEachIndexed { idx, d ->
                DiseaseDetailCard(d, myPlants, onLogForPlant)
                if (idx < matches.size - 1) Spacer(Modifier.height(Space.md))
            }

            Spacer(modifier = Modifier.height(Space.lg))
            FloraSecondaryButton(
                label = "Back to list",
                onClick = onReset,
                icon = Icons.Filled.Refresh,
            )
            Spacer(modifier = Modifier.height(Space.lg))
        }
    }
}

@Composable
private fun DiseaseDetailCard(
    d: DiagnosedDisease,
    myPlants: List<com.example.flora.data.database.entities.Plant>,
    onLogForPlant: (Int, DiagnosedDisease) -> Unit,
) {
    val severityColor = FloraDesign.severityColor(d.severity)
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.card - 4.dp))
            .background(Palette.surfaceTintSoft.copy(alpha = 0.06f))
            .border(1.dp, severityColor.copy(alpha = 0.40f), RoundedCornerShape(Radii.card - 4.dp))
            .padding(Space.lg),
    ) {
        // Example photo banner.
        val refUrl = com.example.flora.ml.DiseaseImageRefs.urlFor(d.displayName)
        if (refUrl != null) {
            AsyncImage(
                model = refUrl,
                contentDescription = "${d.displayName} example",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(Radii.card - 6.dp))
                    .background(Palette.surfaceTintSoft),
            )
            Spacer(Modifier.height(Space.md))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(severityColor.copy(alpha = 0.20f))
                    .border(2.dp, severityColor.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (d.displayName == "Healthy Plant") Icons.Filled.Healing else Icons.Filled.BugReport,
                    null, tint = severityColor, modifier = Modifier.size(Size.iconLg + 2.dp),
                )
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(d.displayName, fontSize = Type.titleSmall, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = severityColor, modifier = Modifier.size(Size.iconSm - 2.dp))
                    Spacer(Modifier.width(Space.xs))
                    Text(d.severity, fontSize = Type.micro, color = severityColor)
                }
            }
        }

        d.disease?.let { row ->
            if (row.symptoms.isNotBlank()) {
                Spacer(Modifier.height(Space.md))
                Text("Symptoms", fontSize = Type.caption, color = Palette.textTertiary, fontWeight = FontWeight.SemiBold)
                Text(row.symptoms, fontSize = Type.bodySmall, color = Palette.textPrimary.copy(alpha = 0.92f), lineHeight = 18.sp)
            }
            if (row.treatment.isNotBlank()) {
                Spacer(Modifier.height(Space.sm))
                Text("Treatment", fontSize = Type.caption, color = Palette.textTertiary, fontWeight = FontWeight.SemiBold)
                Text(row.treatment, fontSize = Type.bodySmall, color = Palette.textPrimary.copy(alpha = 0.92f), lineHeight = 18.sp)
            }
            if (row.prevention.isNotBlank()) {
                Spacer(Modifier.height(Space.sm))
                Text("Prevention", fontSize = Type.caption, color = Palette.textTertiary, fontWeight = FontWeight.SemiBold)
                Text(row.prevention, fontSize = Type.bodySmall, color = Palette.textPrimary.copy(alpha = 0.92f), lineHeight = 18.sp)
            }
            if (row.affectedParts.isNotBlank()) {
                Spacer(Modifier.height(Space.sm))
                Text("Affected parts", fontSize = Type.caption, color = Palette.textTertiary, fontWeight = FontWeight.SemiBold)
                Text(row.affectedParts, fontSize = Type.bodySmall, color = Palette.textPrimary.copy(alpha = 0.92f), lineHeight = 18.sp)
            }
        }

        if (myPlants.isNotEmpty()) {
            Spacer(Modifier.height(Space.md))
            Box {
                Button(
                    onClick = { menuOpen = true },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(Radii.chip + 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = severityColor.copy(alpha = 0.22f)),
                ) {
                    Text(
                        "Log for one of my plants",
                        color = severityColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = Type.bodySmall,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    myPlants.forEach { plant ->
                        DropdownMenuItem(
                            text = { Text(plant.name + " (" + plant.location + ")") },
                            onClick = {
                                onLogForPlant(plant.id, d)
                                menuOpen = false
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val CATEGORY_ALL = "All"
