package com.example.flora.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import androidx.navigation.NavController
import com.example.flora.data.database.entities.CareSchedule
import com.example.flora.ui.components.EmptyState
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.data.MockData
import com.example.flora.ui.theme.FloraDesign.Palette
import com.example.flora.ui.theme.FloraDesign.Radii
import com.example.flora.ui.theme.FloraDesign.Size
import com.example.flora.ui.theme.FloraDesign.Space
import com.example.flora.ui.theme.FloraDesign.Type
import com.example.flora.ui.viewmodel.CareScheduleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tasks scheduled beyond this many days are hidden from the main schedule view. */
private const val VISIBLE_WINDOW_DAYS = 7

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScheduleScreen(navController: NavController, careScheduleViewModel: CareScheduleViewModel) {
    val tasks by careScheduleViewModel.allTasks.collectAsState()
    val doneToday by careScheduleViewModel.doneToday.collectAsState()
    val recent by careScheduleViewModel.recent.collectAsState()
    var showHistory by remember { mutableStateOf(false) }
    var pendingEarlyTask by remember { mutableStateOf<CareSchedule?>(null) }
    var pendingUndoRecord by remember { mutableStateOf<CareScheduleViewModel.CompletionRecord?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Catch midnight rollover when the screen is open across days.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        careScheduleViewModel.refreshIfNewDay()
    }

    // "Current Progress" = today's completion ratio that survives auto-renew.
    // doneToday is persisted in prefs per calendar day; remainingToday is computed live.
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 23)
        set(java.util.Calendar.MINUTE, 59)
        set(java.util.Calendar.SECOND, 59)
        set(java.util.Calendar.MILLISECOND, 999)
    }
    val endOfToday = cal.timeInMillis
    val remainingToday = tasks.count { it.nextDueDate <= endOfToday && !it.isCompleted }
    val totalToday = doneToday + remainingToday
    val progress = if (totalToday > 0) doneToday.toFloat() / totalToday else 1f

    // Only show tasks due in the next [VISIBLE_WINDOW_DAYS] days. After ticking,
    // a task with a longer interval (e.g. Lavender 10d) advances past the window
    // and disappears from view, matching the "this task is handled" expectation.
    val now = System.currentTimeMillis()
    val visibleCutoff = now + VISIBLE_WINDOW_DAYS * 86_400_000L
    val visibleTasks = tasks.filter { it.nextDueDate <= visibleCutoff }

    // Group tasks by date
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val todayStr = dateFormat.format(Date())
    val tomorrowStr = dateFormat.format(Date(System.currentTimeMillis() + 86400000))
    val groupedTasks = visibleTasks.groupBy { task ->
        val taskDate = dateFormat.format(Date(task.nextDueDate))
        when (taskDate) {
            todayStr -> "Today"
            tomorrowStr -> "Tomorrow"
            else -> taskDate
        }
    }
    val hiddenFutureCount = tasks.size - visibleTasks.size

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Care Schedule", fontWeight = FontWeight.Bold, color = Palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Palette.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(Icons.Filled.History, "Recent ticks", tint = Palette.textPrimary)
                            if (recent.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Palette.accent),
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = Space.xxl)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(Space.lg), cornerRadius = Radii.card) {
                    Column(modifier = Modifier.padding(Space.xl)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Current Progress", fontSize = Type.bodyLarge, fontWeight = FontWeight.Bold, color = Palette.textPrimary)
                            Text("$doneToday / $totalToday", fontSize = Type.bodyMedium, fontWeight = FontWeight.Bold, color = Palette.accentAlt)
                        }
                        Spacer(modifier = Modifier.height(Space.sm + 2.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)), color = Palette.accent, trackColor = Palette.surfaceTintMid)
                        Spacer(modifier = Modifier.height(Space.xs + 2.dp))
                        Text(
                            if (totalToday == 0) "All caught up for today" else "${(progress * 100).toInt()}% done today",
                            fontSize = Type.caption,
                            color = Palette.textTertiary,
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.CalendarMonth,
                        title = "No tasks scheduled",
                        body = "Add plants to your collection — Flora auto-builds a watering schedule for each one.",
                        accent = Palette.accentAlt,
                        primaryLabel = "Identify a plant",
                        onPrimary = { navController.navigate("plant_identification") }
                    )
                }
            } else if (visibleTasks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.CheckCircle,
                        title = "Nothing to do this week",
                        body = "All your plants are taken care of for the next $VISIBLE_WINDOW_DAYS days. Enjoy.",
                        accent = Palette.accent,
                    )
                }
            }

            groupedTasks.forEach { (date, dateTasks) ->
                item { GlassDateHeader(date) }
                items(items = dateTasks, key = { it.id }) { task ->
                    val colorPair = MockData.plantGradientColors[task.colorIndex % MockData.plantGradientColors.size]
                    val isEarly = task.nextDueDate > endOfToday
                    GlassScheduleTaskCard(
                        task = task,
                        isDone = task.isCompleted,
                        plantColor = colorPair.first,
                        skipAnimation = isEarly,
                        onComplete = {
                            if (isEarly) pendingEarlyTask = task
                            else careScheduleViewModel.completeAndAdvance(task.id)
                        },
                        modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.xs)
                    )
                }
            }

            if (hiddenFutureCount > 0) {
                item {
                    Text(
                        "+$hiddenFutureCount task${if (hiddenFutureCount == 1) "" else "s"} scheduled beyond ${VISIBLE_WINDOW_DAYS}d",
                        fontSize = Type.caption,
                        color = Palette.textTertiary,
                        modifier = Modifier.fillMaxWidth().padding(Space.lg),
                    )
                }
            }
        }
    }

    pendingEarlyTask?.let { task ->
        val daysEarly = ((task.nextDueDate - System.currentTimeMillis()) / 86_400_000.0).toInt().coerceAtLeast(1)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingEarlyTask = null },
            title = { Text("Water ${task.plantName} early?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This task is due in ${daysEarly}d. If you water now, the schedule will reset and the next " +
                        "watering will be ${task.intervalDays}d from today."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    careScheduleViewModel.completeAndAdvance(task.id)
                    pendingEarlyTask = null
                }) { Text("Water now", color = Palette.accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingEarlyTask = null }) {
                    Text("Cancel", color = Palette.textTertiary)
                }
            },
            containerColor = Palette.sheetTop,
        )
    }

    pendingUndoRecord?.let { record ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingUndoRecord = null },
            title = { Text("Undo ${record.taskType.lowercase()} for ${record.plantName}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This restores the original due date, removes the health log entry, and rolls back the plant's last-watered timestamp."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    careScheduleViewModel.undoCompletion(record)
                    pendingUndoRecord = null
                }) { Text("Undo", color = Palette.accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingUndoRecord = null }) {
                    Text("Cancel", color = Palette.textTertiary)
                }
            },
            containerColor = Palette.sheetTop,
        )
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            sheetState = sheetState,
            containerColor = Palette.sheetTop,
        ) {
            RecentTicksSheet(
                records = recent,
                onUndo = { pendingUndoRecord = it },
                onClose = { showHistory = false },
            )
        }
    }
}

@Composable
private fun RecentTicksSheet(
    records: List<CareScheduleViewModel.CompletionRecord>,
    onUndo: (CareScheduleViewModel.CompletionRecord) -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(Space.lg)) {
        Text(
            "Recently completed",
            fontSize = Type.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Palette.textPrimary,
        )
        Spacer(modifier = Modifier.height(Space.xs))
        Text(
            "Tap Undo to roll a task back to its original due date.",
            fontSize = Type.caption,
            color = Palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(Space.md))

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = Space.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No tasks completed yet. Tick something to see it here.",
                    fontSize = Type.bodySmall,
                    color = Palette.textTertiary,
                )
            }
        } else {
            records.forEach { rec ->
                RecentTickRow(rec, onUndo = { onUndo(rec) })
                Spacer(modifier = Modifier.height(Space.sm))
            }
        }

        Spacer(modifier = Modifier.height(Space.md))
        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Close", color = Palette.accentAlt, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(Space.lg))
    }
}

@Composable
private fun RecentTickRow(
    record: CareScheduleViewModel.CompletionRecord,
    onUndo: () -> Unit,
) {
    val taskColor: Color = when (record.taskType) {
        "Water", "Mist", "Water Change" -> Palette.info
        "Fertilize" -> Palette.accent
        "Prune" -> Color(0xFFCE93D8)
        else -> Palette.accentAlt
    }
    val taskIcon: ImageVector = when (record.taskType) {
        "Water", "Mist", "Water Change" -> Icons.Filled.WaterDrop
        "Fertilize" -> Icons.Filled.Eco
        "Prune" -> Icons.Filled.LocalFlorist
        else -> Icons.Filled.CheckCircle
    }
    val ago = formatTimeAgo(record.completedAt)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = Radii.card - 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(taskColor.copy(alpha = 0.18f))
                    .border(1.dp, taskColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(taskIcon, null, tint = taskColor, modifier = Modifier.size(Size.iconSm))
            }
            Spacer(modifier = Modifier.width(Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.plantName,
                    fontSize = Type.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.textPrimary,
                )
                Text(
                    "${record.taskType} • $ago • every ${record.intervalDays}d",
                    fontSize = Type.caption,
                    color = Palette.textTertiary,
                )
            }
            TextButton(onClick = onUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    null,
                    tint = Palette.accent,
                    modifier = Modifier.size(Size.iconSm),
                )
                Spacer(modifier = Modifier.width(Space.xs))
                Text("Undo", color = Palette.accent, fontWeight = FontWeight.Bold, fontSize = Type.caption)
            }
        }
    }
}

private fun formatTimeAgo(then: Long): String {
    val deltaSec = (System.currentTimeMillis() - then) / 1000
    return when {
        deltaSec < 60 -> "just now"
        deltaSec < 3_600 -> "${deltaSec / 60} min ago"
        deltaSec < 86_400 -> "${deltaSec / 3_600} h ago"
        else -> "${deltaSec / 86_400} d ago"
    }
}

@Composable
private fun GlassDateHeader(date: String) {
    Row(modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.md), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Palette.accentAlt))
        Spacer(modifier = Modifier.width(Space.sm))
        Text(date, fontSize = Type.bodyMedium, fontWeight = FontWeight.Bold, color = Palette.accentAlt)
        Spacer(modifier = Modifier.width(Space.sm))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Palette.accentAlt.copy(alpha = 0.25f)))
    }
}

/**
 * Care-schedule task card with a celebration animation on completion:
 *   1. Plant icon scales up
 *   2. A task-typed droplet (water / leaf / sparkle) falls into the icon
 *   3. Three concentric rings expand outward
 *   4. Card slides right with fade
 * After animation, [onComplete] fires → ViewModel advances the date and the card
 * reappears in its new group (or disappears from "Today").
 */
@Composable
private fun GlassScheduleTaskCard(
    task: CareSchedule,
    isDone: Boolean,
    plantColor: Color,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    skipAnimation: Boolean = false,
) {
    val taskIcon: ImageVector = when (task.taskType) {
        "Water", "Mist", "Water Change" -> Icons.Filled.WaterDrop
        "Fertilize" -> Icons.Filled.Eco
        "Prune" -> Icons.Filled.LocalFlorist
        else -> Icons.Filled.CheckCircle
    }
    val taskColor: Color = when (task.taskType) {
        "Water", "Mist", "Water Change" -> Palette.info
        "Fertilize" -> Palette.accent
        "Prune" -> Color(0xFFCE93D8)
        else -> Palette.accentAlt
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var isAnimating by remember { mutableStateOf(false) }

    // Animatables drive the celebration sequence.
    val iconScale = remember { Animatable(1f) }      // icon pulse
    val dropY = remember { Animatable(-1f) }         // -1 = hidden above, 0 = at icon
    val ringRadius1 = remember { Animatable(0f) }
    val ringRadius2 = remember { Animatable(0f) }
    val ringRadius3 = remember { Animatable(0f) }
    val cardOffsetX = remember { Animatable(0f) }    // card slide-out (dp)
    val cardAlpha = remember { Animatable(1f) }

    val triggerAnimation: () -> Unit = lambda@ {
        // For early-water taps, defer to parent (dialog confirms first; no animation).
        if (skipAnimation) {
            onComplete()
            return@lambda
        }
        if (!isAnimating) {
            isAnimating = true
            // Fire the DB advance IMMEDIATELY — animation is purely visual feedback.
            // This way the progress bar bumps right away and the task moves to its new
            // date group as soon as Room emits, even if this composable is later disposed.
            onComplete()
            scope.launch {
                // Reset visuals in case the card was recycled with stale animation state.
                iconScale.snapTo(1f); dropY.snapTo(-1f)
                ringRadius1.snapTo(0f); ringRadius2.snapTo(0f); ringRadius3.snapTo(0f)
                cardOffsetX.snapTo(0f); cardAlpha.snapTo(1f)

                // 1+2. Icon pulse + droplet falls onto icon (parallel).
                val pulse = async {
                    iconScale.animateTo(1.18f, tween(260, easing = FastOutSlowInEasing))
                    iconScale.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                }
                val fall = async {
                    dropY.animateTo(0f, tween(520, easing = LinearOutSlowInEasing))
                }
                listOf(pulse, fall).awaitAll()

                // 3. Three rings expand staggered.
                val r1 = async { ringRadius1.animateTo(1f, tween(760, easing = LinearOutSlowInEasing)) }
                delay(160)
                val r2 = async { ringRadius2.animateTo(1f, tween(760, easing = LinearOutSlowInEasing)) }
                delay(160)
                val r3 = async { ringRadius3.animateTo(1f, tween(760, easing = LinearOutSlowInEasing)) }
                listOf(r1, r2, r3).awaitAll()

                // 4. Card slides right + fades out.
                val slide = async { cardOffsetX.animateTo(240f, tween(520, easing = FastOutSlowInEasing)) }
                val fade = async { cardAlpha.animateTo(0f, tween(520)) }
                listOf(slide, fade).awaitAll()

                isAnimating = false
                // Defensive reset — by now LazyColumn has likely re-positioned the card.
                cardOffsetX.snapTo(0f); cardAlpha.snapTo(1f)
                dropY.snapTo(-1f)
                ringRadius1.snapTo(0f); ringRadius2.snapTo(0f); ringRadius3.snapTo(0f)
            }
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isDone) 0.55f else 1f)
            .graphicsLayer {
                translationX = cardOffsetX.value
                alpha = cardAlpha.value
            },
        cornerRadius = Radii.card - 4.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(Space.md), verticalAlignment = Alignment.CenterVertically) {
            // ── Plant icon + celebration overlay (drop + rings) ──
            // Outer Box is unclipped so rings/droplet can extend past the icon's circle.
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .drawWithContent {
                        drawContent()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val iconR = with(density) { 23.dp.toPx() }
                        val maxR = iconR * 2.2f
                        val strokeW = with(density) { 2.dp.toPx() }

                        // Concentric rings expanding past the icon edge.
                        fun ring(progress: Float) {
                            if (progress <= 0f) return
                            val r = iconR + (maxR - iconR) * progress
                            val a = (1f - progress).coerceIn(0f, 1f) * 0.85f
                            drawCircle(
                                color = taskColor.copy(alpha = a),
                                radius = r,
                                center = Offset(cx, cy),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW),
                            )
                        }
                        ring(ringRadius1.value)
                        ring(ringRadius2.value)
                        ring(ringRadius3.value)

                        // Falling droplet: -1 hidden above box, 0 lands at icon center.
                        val dropProgress = dropY.value
                        if (dropProgress > -1f) {
                            val travelStartY = -size.height * 0.2f
                            val yPos = travelStartY + (cy - travelStartY) * (dropProgress + 1f).coerceIn(0f, 1f)
                            val impact = (dropProgress + 1f).coerceIn(0f, 1f)
                            val dropRx = with(density) { 5.dp.toPx() } * (1f - impact * 0.3f)
                            val dropRy = with(density) { 6.dp.toPx() } * (1f + impact * 0.2f)
                            drawOval(
                                color = taskColor,
                                topLeft = Offset(cx - dropRx, yPos - dropRy),
                                size = androidx.compose.ui.geometry.Size(dropRx * 2, dropRy * 2),
                            )
                            if (dropProgress > -0.2f) {
                                val sparkle = (dropProgress + 0.2f).coerceIn(0f, 1f)
                                val sparkleR = iconR * 1.5f * sparkle
                                val sparkleA = (1f - sparkle).coerceIn(0f, 1f) * 0.9f
                                val sparkleSize = with(density) { 2.dp.toPx() }
                                for (i in 0 until 6) {
                                    val angle = Math.PI.toFloat() * 2f * i / 6f
                                    val sx = cx + cos(angle) * sparkleR
                                    val sy = cy + sin(angle) * sparkleR
                                    drawCircle(
                                        color = taskColor.copy(alpha = sparkleA),
                                        radius = sparkleSize,
                                        center = Offset(sx, sy),
                                    )
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                        }
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(plantColor, plantColor.copy(alpha = 0.60f))))
                        .border(1.dp, Palette.textPrimary.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Eco, null, tint = Palette.textPrimary, modifier = Modifier.size(Size.iconLg + 2.dp))
                }
            }
            Spacer(modifier = Modifier.width(Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.plantName,
                    fontSize = Type.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.textPrimary,
                    textDecoration = if (isAnimating) TextDecoration.LineThrough else TextDecoration.None,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Icon(taskIcon, null, tint = taskColor, modifier = Modifier.size(Size.iconSm - 2.dp))
                    Text(
                        if (isAnimating) "every ${task.intervalDays}d" else task.taskType,
                        fontSize = Type.caption,
                        color = taskColor,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = Palette.textMuted, modifier = Modifier.size(Size.iconSm - 3.dp))
                    Spacer(modifier = Modifier.width(Space.xs))
                    Text(task.dueTime, fontSize = Type.caption, color = Palette.textTertiary, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.width(Space.xs))
            Checkbox(
                checked = isAnimating,
                onCheckedChange = { triggerAnimation() },
                enabled = !isAnimating,
                colors = CheckboxDefaults.colors(checkedColor = Palette.accent, uncheckedColor = Palette.textMuted, checkmarkColor = Palette.textPrimary),
            )
        }
    }
}
