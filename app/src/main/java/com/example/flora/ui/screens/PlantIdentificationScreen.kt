package com.example.flora.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.flora.util.BitmapStorage
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.flora.data.database.entities.Plant
import com.example.flora.ui.components.CameraPreview
import com.example.flora.ui.components.FloraPrimaryButton
import com.example.flora.ui.components.FloraSecondaryButton
import com.example.flora.ui.components.capturePhoto
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.AuthViewModel
import com.example.flora.ui.viewmodel.ClassificationViewModel
import com.example.flora.ui.viewmodel.IdentificationUiState
import com.example.flora.ui.viewmodel.PlantViewModel
import com.example.flora.ui.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantIdentificationScreen(
    navController: NavController,
    classificationViewModel: ClassificationViewModel,
    plantViewModel: PlantViewModel,
    authViewModel: AuthViewModel,
    weatherViewModel: WeatherViewModel
) {
    val context = LocalContext.current
    val identificationState by classificationViewModel.identificationState.collectAsState()
    val capturedImage by classificationViewModel.capturedImage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val weather by weatherViewModel.weather.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashOn by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = try {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
            if (bmp != null) {
                classificationViewModel.resetIdentification()
                scope.launch { classificationViewModel.identifyPlant(bmp) }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        // Reset state when entering screen
        classificationViewModel.resetIdentification()
    }

    val isScanning = identificationState is IdentificationUiState.Scanning
    val isResult = identificationState is IdentificationUiState.Result || identificationState is IdentificationUiState.Error

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineY by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "scanLine")
    val cornerAlpha by infiniteTransition.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "cornerAlpha")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            // Show frozen captured image during scanning/result, else live preview
            val frozen = capturedImage
            if (frozen != null && isScanning) {
                // Show frozen frame while API call in progress
                Image(
                    bitmap = frozen.asImageBitmap(),
                    contentDescription = "Captured plant",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!isResult) {
                CameraPreview(modifier = Modifier.fillMaxSize(), onCameraReady = { capture -> imageCapture = capture })
            } else {
                // Result shown — black bg, photo lives inside scroll column
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A2A1A), Color(0xFF0D1A0D)))), contentAlignment = Alignment.Center) {
                com.example.flora.ui.components.EmptyState(
                    icon = Icons.Filled.CameraAlt,
                    title = "Camera access required",
                    body = "MyFlora needs your camera to scan plants and diseases. Grant permission to continue.",
                    accent = Palette.accentAlt,
                    primaryLabel = "Grant permission",
                    onPrimary = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }

        // Scanner frame
        if (hasCameraPermission && !isResult) {
            Box(modifier = Modifier.align(Alignment.Center).offset(y = (-40).dp)) {
                ScannerFrame(240, Color(0xFF80DEEA).copy(cornerAlpha), if (isScanning) scanLineY else -1f)
            }
            // Scanning progress stays near the frame center; idle hint moved to bottom column
            if (isScanning) {
                Column(modifier = Modifier.align(Alignment.Center).offset(y = 160.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Analyzing plant…", fontSize = Type.bodyLarge, color = Palette.accentAlt, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(Space.md))
                    LinearProgressIndicator(modifier = Modifier.width(180.dp).clip(RoundedCornerShape(4.dp)), color = Palette.accentAlt, trackColor = Palette.surfaceTintStrong)
                }
            }
        }

        // Bottom area
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            // Shutter button
            AnimatedVisibility(visible = !isResult, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
                        .pointerInput(Unit) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragEnd = {
                                    if (total < -80f) {
                                        // swipe-left → diagnose
                                        navController.navigate("disease_diagnosis") {
                                            popUpTo("plant_identification") { inclusive = true }
                                        }
                                    }
                                },
                                onDragCancel = {},
                                onHorizontalDrag = { _, drag -> total += drag }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Idle hint — lives above the mode toggle so it never overlaps the pill on short screens
                    if (!isScanning) {
                        Text(
                            "Point camera at a plant",
                            fontSize = Type.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Keep the plant within the frame",
                            fontSize = Type.bodySmall,
                            color = Color.White.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = Space.xs, bottom = Space.md)
                        )
                    }
                    // Mode toggle pill removed — shutter row itself is swipe-aware (swipe-left ⇒ Diagnose).
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(0.18f))
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Button(
                            onClick = {
                                if (identificationState is IdentificationUiState.Idle) {
                                    val capture = imageCapture
                                    if (capture != null) {
                                        capture.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                                        classificationViewModel.resetIdentification()
                                        capturePhoto(context, capture) { bitmap ->
                                            if (bitmap != null) {
                                                scope.launch {
                                                    classificationViewModel.identifyPlant(bitmap)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(72.dp), shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF80DEEA).copy(0.90f))
                        ) {
                            Icon(Icons.Filled.CameraAlt, "Scan", tint = Color.Black, modifier = Modifier.size(32.dp))
                        }
                        IconButton(
                            onClick = { flashOn = !flashOn },
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(if (flashOn) Color(0xFFFFD54F).copy(0.35f) else Color.White.copy(0.18f)),
                        ) {
                            Icon(
                                if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                if (flashOn) "Flash on" else "Flash off",
                                tint = if (flashOn) Color(0xFFFFD54F) else Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(if (!isScanning) "Tap to Scan  •  Swipe ← for Diagnose" else "Scanning…", fontSize = 13.sp, color = Color.White.copy(0.75f))
                }
            }
        }
        
        // Result Area
        AnimatedVisibility(
            visible = isResult, 
            enter = slideInVertically { it }, 
            exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = identificationState) {
                is IdentificationUiState.Result -> {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        // Photo taken during scan — tap card handle to scroll up and reveal it
                        val frozen = capturedImage
                        if (frozen != null) {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                                Image(
                                    bitmap = frozen.asImageBitmap(),
                                    contentDescription = "Captured plant",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // fade bottom into card
                                Box(modifier = Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Transparent, Color(0xFF0A1A14))
                                    )
                                ))
                            }
                        } else {
                            Spacer(modifier = Modifier.height(260.dp))
                        }
                        val topRecognition = state.recognitions.first()
                        val species = state.topSpecies
                        val canSaveToPlants = currentUser != null
                        // Care profile for plants with no curated DB row — keeps the
                        // About copy, watering chip and sunlight chip all species-aware.
                        val fallbackProfile = com.example.flora.ml.PlantCareProfiles.forName(
                            scientificName = topRecognition.scientificName,
                            commonName = topRecognition.label,
                        )
                        GlassIdentificationResultCard(
                            commonName = topRecognition.label,
                            scientificName = species?.scientificName ?: "",
                            malayName = species?.malayName ?: "",
                            confidence = topRecognition.confidence,
                            description = species?.description ?: buildGenericAbout(
                                commonName = topRecognition.label,
                                scientificName = topRecognition.scientificName,
                            ),
                            careLevel = species?.careLevel ?: "Unknown",
                            isNative = species?.isNative ?: false,
                            wateringFrequency = species?.wateringFrequency ?: fallbackProfile.wateringSummary,
                            sunlight = species?.sunlight ?: fallbackProfile.sunlight,
                            canSaveToPlants = canSaveToPlants,
                            onScanAgain = { classificationViewModel.resetIdentification() },
                            onSaveToPlants = {
                                val userId = currentUser?.id ?: 0
                                if (userId > 0) {
                                    showSaveDialog = true
                                } else {
                                    classificationViewModel.resetIdentification()
                                    navController.navigate("login")
                                }
                            }
                        )

                        if (showSaveDialog) {
                            SavePlantDialog(
                                defaultName = topRecognition.label,
                                isRaining = weather.isRaining,
                                onDismiss = { showSaveDialog = false },
                                onConfirm = { locationInput, watered ->
                                    val userId = currentUser?.id ?: 0
                                    val now = System.currentTimeMillis()
                                    val savedPath = capturedImage?.let { BitmapStorage.save(context, it) }
                                    val freq = species?.wateringFrequency ?: ""
                                    val waterDays = when {
                                        // Curated DB freq strings (Malaysian species table).
                                        "Daily" in freq -> 1
                                        "1-2" in freq || "Every 2" in freq -> 2
                                        "3-5" in freq || "3-4" in freq -> 4
                                        "4-5" in freq -> 5
                                        "4-7" in freq -> 6
                                        // Fall back to genus-aware default from PlantCareDefaults.
                                        else -> com.example.flora.ml.PlantCareDefaults.wateringIntervalDays(
                                            scientificName = topRecognition.scientificName.ifBlank { species?.scientificName ?: "" },
                                            commonName = topRecognition.label
                                        )
                                    }
                                    val rainNote = if (weather.isRaining) " Auto-watered: rain detected (${weather.description})." else ""
                                    plantViewModel.addPlantWithStarterSchedule(
                                        Plant(
                                            userId = userId,
                                            name = topRecognition.label,
                                            species = species?.scientificName ?: topRecognition.label,
                                            malayName = species?.malayName ?: "",
                                            healthStatus = "Healthy",
                                            healthScore = 100,
                                            location = locationInput.ifBlank { "Unspecified" },
                                            notes = "Identified ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}. Confidence: ${(topRecognition.confidence * 100).toInt()}%.$rainNote",
                                            wateringFrequencyDays = waterDays,
                                            sunlight = species?.sunlight ?: "Full sun",
                                            fertilizer = "Monthly",
                                            lastWatered = if (watered) now else null,
                                            dateAdded = now,
                                            imageUri = savedPath
                                        )
                                    )
                                    showSaveDialog = false
                                    classificationViewModel.resetIdentification()
                                    navController.navigate("my_plants") {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
                is IdentificationUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet))
                                .background(Brush.verticalGradient(listOf(Palette.sheetTop, Palette.sheetBottom)))
                                .padding(Space.xxl)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(state.message, fontSize = Type.titleSmall, color = Palette.textPrimary, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(Space.lg))
                                FloraSecondaryButton(
                                    label = "Try again",
                                    onClick = { classificationViewModel.resetIdentification() },
                                    icon = Icons.Filled.Refresh
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        TopAppBar(
            title = { Text("Plant Identification", color = Color.White, fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(0.45f)),
            modifier = Modifier.align(Alignment.TopCenter)
        )


                // End of new Block handled previously
    }
}

@Composable
fun ScannerFrame(size: Int, cornerColor: Color, scanLineProgress: Float) {
    Box(modifier = Modifier.size(size.dp).drawBehind {
        val cornerLen = 50f; val strokeW = 5f
        drawLine(cornerColor, Offset(0f, cornerLen), Offset(0f, 0f), strokeW, StrokeCap.Round)
        drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLen, 0f), strokeW, StrokeCap.Round)
        drawLine(cornerColor, Offset(this.size.width - cornerLen, 0f), Offset(this.size.width, 0f), strokeW, StrokeCap.Round)
        drawLine(cornerColor, Offset(this.size.width, 0f), Offset(this.size.width, cornerLen), strokeW, StrokeCap.Round)
        drawLine(cornerColor, Offset(0f, this.size.height - cornerLen), Offset(0f, this.size.height), strokeW, StrokeCap.Round)
        drawLine(cornerColor, Offset(0f, this.size.height), Offset(cornerLen, this.size.height), strokeW, StrokeCap.Round)
        drawLine(cornerColor, Offset(this.size.width - cornerLen, this.size.height), Offset(this.size.width, this.size.height), strokeW, StrokeCap.Round)
        drawLine(cornerColor, Offset(this.size.width, this.size.height - cornerLen), Offset(this.size.width, this.size.height), strokeW, StrokeCap.Round)
        if (scanLineProgress >= 0f) {
            val y = scanLineProgress * this.size.height
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF80DEEA).copy(0.80f), Color(0xFF80DEEA), Color(0xFF80DEEA).copy(0.80f), Color.Transparent)), Offset(0f, y), Offset(this.size.width, y), 2f)
        }
    })
}

@Composable
private fun GlassIdentificationResultCard(
    commonName: String, scientificName: String, malayName: String,
    confidence: Float, description: String, careLevel: String,
    isNative: Boolean, wateringFrequency: String, sunlight: String,
    canSaveToPlants: Boolean,
    onScanAgain: () -> Unit, onSaveToPlants: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet))
            .background(Brush.verticalGradient(listOf(Palette.sheetTop, Palette.sheetBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Palette.accent.copy(alpha = 0.55f), Color.White.copy(alpha = 0.15f))), RoundedCornerShape(topStart = Radii.sheet, topEnd = Radii.sheet))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Space.xxl)) {
            Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Palette.textMuted).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(Space.lg))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(Size.avatarLg).clip(RoundedCornerShape(Radii.card - 4.dp)).background(Brush.verticalGradient(listOf(Palette.accentAlt.copy(alpha = 0.35f), Palette.accent.copy(alpha = 0.18f)))).border(1.dp, Palette.accentAlt.copy(alpha = 0.50f), RoundedCornerShape(Radii.card - 4.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Eco, null, tint = Palette.textPrimary, modifier = Modifier.size(Size.iconXl + 8.dp))
                }
                Spacer(modifier = Modifier.width(Space.lg))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(commonName, fontSize = Type.titleLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                        if (isNative) {
                            Spacer(modifier = Modifier.width(Space.sm))
                            com.example.flora.ui.components.StatusPill(text = "Native", color = Palette.accent)
                        }
                    }
                    if (scientificName.isNotEmpty()) Text(scientificName, fontSize = Type.bodySmall, color = Palette.textSecondary, fontStyle = FontStyle.Italic)
                    if (malayName.isNotEmpty()) Text(malayName, fontSize = Type.bodySmall, color = Palette.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(Space.lg))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = Palette.accent, modifier = Modifier.size(Size.iconSm + 2.dp))
                Spacer(modifier = Modifier.width(Space.xs + 2.dp))
                Text("Match confidence", fontSize = Type.bodySmall, color = Palette.textSecondary)
                Spacer(modifier = Modifier.weight(1f))
                Text("${(confidence * 100).toInt()}%", fontSize = Type.bodyMedium, fontWeight = FontWeight.Bold, color = Palette.accent)
            }
            Spacer(modifier = Modifier.height(Space.xs + 2.dp))
            LinearProgressIndicator(progress = { confidence }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Palette.accent, trackColor = Palette.surfaceTintMid)

            Spacer(modifier = Modifier.height(Space.lg))
            Text("About", fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
            Spacer(modifier = Modifier.height(Space.xs))
            Text(description, fontSize = Type.bodySmall, color = Palette.textPrimary.copy(alpha = 0.95f), lineHeight = Type.lineBody)

            Spacer(modifier = Modifier.height(Space.lg))
            Text("Care tips", fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
            Spacer(modifier = Modifier.height(Space.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                GlassCareChip(Icons.Filled.WaterDrop, wateringFrequency, Palette.info, Modifier.weight(1f))
                GlassCareChip(Icons.Filled.WbSunny, sunlight, Palette.warn, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(Space.lg))
            if (!canSaveToPlants) {
                Text(
                    "Guest mode is read-only. Sign in to save this plant and create its starter watering task.",
                    fontSize = Type.caption,
                    color = Palette.textSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(Space.md))
            }
            FloraPrimaryButton(
                label = if (canSaveToPlants) "Save to my plants" else "Sign in to save",
                onClick = onSaveToPlants,
                icon = Icons.Filled.Add,
                accent = Palette.accent
            )
            Spacer(modifier = Modifier.height(Space.sm))
            FloraSecondaryButton(
                label = "Scan another plant",
                onClick = onScanAgain,
                icon = Icons.Filled.Refresh
            )
            Spacer(modifier = Modifier.height(Space.lg))
        }
    }
}

@Composable
private fun SavePlantDialog(
    defaultName: String,
    isRaining: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (location: String, watered: Boolean) -> Unit
) {
    var location by remember { mutableStateOf("") }
    var watered by remember { mutableStateOf(isRaining) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.sheetTop,
        titleContentColor = Palette.textPrimary,
        textContentColor = Palette.textSecondary,
        title = { Text("Save $defaultName", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Where do you keep it?", fontSize = Type.bodySmall, color = Palette.textSecondary)
                Spacer(Modifier.height(Space.xs + 2.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text("e.g. Balcony, Kitchen window, Backyard", color = Palette.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Palette.textPrimary,
                        unfocusedTextColor = Palette.textPrimary,
                        focusedBorderColor = Palette.accent,
                        unfocusedBorderColor = Palette.surfaceTintStrong,
                        cursorColor = Palette.accent
                    )
                )
                Spacer(Modifier.height(Space.lg))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Already watered today?", fontSize = Type.bodyMedium, color = Palette.textPrimary)
                        if (isRaining) {
                            Text(
                                "Auto-on — rain detected in your area",
                                fontSize = Type.micro,
                                color = Palette.accentAlt
                            )
                        }
                    }
                    Switch(
                        checked = watered,
                        onCheckedChange = { watered = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Palette.accent,
                            checkedTrackColor = Palette.accent.copy(alpha = 0.30f),
                            uncheckedThumbColor = Palette.textSecondary,
                            uncheckedTrackColor = Palette.surfaceTintMid
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(location, watered) }) {
                Text("Save", color = Palette.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Palette.textSecondary)
            }
        }
    )
}

@Composable
private fun GlassCareChip(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(Radii.chip)).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(Radii.chip)).padding(horizontal = Space.md - 2.dp, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(Size.iconSm))
        Spacer(modifier = Modifier.width(Space.xs + 2.dp))
        Text(label, fontSize = Type.micro, color = Palette.textPrimary, lineHeight = 14.sp)
    }
}

/**
 * Fallback "About" copy for plants with no curated DB row. Resolves the
 * scientific/common name to a [com.example.flora.ml.PlantCareProfiles.CareProfile]
 * so the advice tracks the actual plant — light, watering, soil and feeding —
 * instead of one flat paragraph.
 */
private fun buildGenericAbout(commonName: String, scientificName: String): String {
    val p = com.example.flora.ml.PlantCareProfiles.forName(scientificName, commonName)
    return buildString {
        append(p.about)
        append("\n\nSunlight: ").append(p.sunlight)
        append("\nWatering: ").append(p.wateringSummary)
        append("\nSoil: ").append(p.soil)
        append("\nFeeding: ").append(p.feeding)
    }
}
