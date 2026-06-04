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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flora.data.database.entities.Plant
import com.example.flora.data.database.entities.PlantHealthLog
import com.example.flora.ui.components.GlassBox
import com.example.flora.ui.components.GlassCard
import com.example.flora.ui.viewmodel.AuthViewModel
import com.example.flora.ui.viewmodel.PlantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantLogsScreen(
    navController: NavController,
    plantId: Int,
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isGuest = currentUser == null

    var plant by remember { mutableStateOf<Plant?>(null) }
    val logs by plantViewModel.getLogsByPlant(plantId).collectAsState(initial = emptyList())

    LaunchedEffect(plantId, currentUser?.id) {
        plant = plantViewModel.getPlantById(plantId)
    }

    val p = plant

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Health Logs", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        if (p != null) Text(p.name, fontSize = 12.sp, color = Color.White.copy(0.65f))
                    }
                },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        if (isGuest) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(0.20f), Color.White.copy(0.08f))))
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.50f), Color.White.copy(0.15f))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Color(0xFF80DEEA), modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Sign In Required", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Health logs are available for signed-in users.", fontSize = 14.sp, color = Color.White.copy(0.65f), textAlign = TextAlign.Center, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                GlassBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 16.dp) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("login") }.padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Login, null, tint = Color(0xFF80DEEA), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sign In", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF80DEEA))
                    }
                }
            }
        } else if (p == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(0.20f), Color.White.copy(0.08f))))
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.50f), Color.White.copy(0.15f))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Eco, null, tint = Color(0xFF80DEEA), modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Plant not available", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("This plant either does not exist or belongs to another account.", fontSize = 14.sp, color = Color.White.copy(0.65f), textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(vertical = 16.dp)) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), cornerRadius = 20.dp) {
                        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            LogStat(logs.size.toString(), "Total\nLogs", Color(0xFF80DEEA))
                            Box(Modifier.width(1.dp).height(40.dp).background(Color.White.copy(0.15f)))
                            LogStat(logs.count { it.diseaseName != null }.toString(), "Disease\nEvents", Color(0xFFEF9A9A))
                        }
                    }
                }
                item { Text("Activity Timeline", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

                if (logs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No health logs recorded yet", fontSize = 14.sp, color = Color.White.copy(0.50f))
                        }
                    }
                }

                itemsIndexed(logs) { index, log ->
                    GlassTimelineItem(log = log, isFirst = index == 0, isLast = index == logs.size - 1)
                }
            }
        }
    }
}

@Composable
private fun LogStat(count: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = Color.White.copy(0.55f), textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
private fun GlassTimelineItem(log: PlantHealthLog, isFirst: Boolean, isLast: Boolean) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val logColor: Color = when (log.logType) {
        "Watering" -> Color(0xFF40C4FF); "Health Check" -> Color(0xFF69F0AE)
        "Disease Detected" -> Color(0xFFEF9A9A); "Treatment Applied" -> Color(0xFFFFD54F)
        "Fertilizing" -> Color(0xFFA5D6A7); "Pruning" -> Color(0xFFCE93D8)
        "Repotting" -> Color(0xFFBCAAA4); else -> Color(0xFF80DEEA)
    }
    val logIcon: ImageVector = when (log.logType) {
        "Watering" -> Icons.Filled.WaterDrop; "Health Check" -> Icons.Filled.CheckCircle
        "Disease Detected" -> Icons.Filled.BugReport; "Treatment Applied" -> Icons.Filled.Healing
        "Fertilizing" -> Icons.Filled.Eco; "Pruning" -> Icons.Filled.LocalFlorist
        "Repotting" -> Icons.Filled.LocalFlorist; else -> Icons.Filled.CalendarMonth
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(Modifier.width(2.dp).height(if (isFirst) 12.dp else 8.dp).background(if (isFirst) Color.Transparent else Color.White.copy(0.18f)))
            Box(Modifier.size(32.dp).clip(CircleShape).background(logColor.copy(0.18f)).border(1.dp, logColor.copy(0.45f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(logIcon, null, tint = logColor, modifier = Modifier.size(18.dp))
            }
            if (!isLast) Box(Modifier.width(2.dp).height(52.dp).background(Color.White.copy(0.18f)))
        }
        Spacer(modifier = Modifier.width(12.dp))
        GlassCard(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 8.dp), cornerRadius = 14.dp) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log.logType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = logColor)
                        if (log.diseaseName != null) {
                            Box(
                                modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(4.dp))
                                    .background(if (log.isResolved) Color(0xFF69F0AE).copy(0.15f) else Color(0xFFEF9A9A).copy(0.15f))
                                    .border(1.dp, if (log.isResolved) Color(0xFF69F0AE).copy(0.40f) else Color(0xFFEF9A9A).copy(0.40f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(if (log.isResolved) "Resolved: ${log.diseaseName}" else "${log.diseaseName}", fontSize = 11.sp,
                                    color = if (log.isResolved) Color(0xFF69F0AE) else Color(0xFFEF9A9A), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(dateFormat.format(Date(log.timestamp)), fontSize = 11.sp, color = Color.White.copy(0.50f))
                        Text("${log.healthScore}%", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = when { log.healthScore >= 80 -> Color(0xFF69F0AE); log.healthScore >= 60 -> Color(0xFFFFD54F); else -> Color(0xFFEF9A9A) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(log.notes ?: "", fontSize = 13.sp, color = Color.White.copy(0.65f), lineHeight = 18.sp)
            }
        }
    }
}
