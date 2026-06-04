package com.example.flora.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.flora.notifications.CareReminderWorker
import com.example.flora.notifications.FloraNotifications
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.flora.data.database.PlantCareDatabase
import com.example.flora.ui.components.LocalIsDarkMode
import com.example.flora.ui.navigation.FloraNavGraph
import com.example.flora.ui.theme.FloraTheme
import com.example.flora.ui.viewmodel.AuthViewModel
import com.example.flora.ui.viewmodel.CareScheduleViewModel
import com.example.flora.ui.viewmodel.ClassificationViewModel
import com.example.flora.ui.viewmodel.PlantViewModel
import com.example.flora.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val plantViewModel: PlantViewModel by viewModels()
    private val weatherViewModel: WeatherViewModel by viewModels()
    private val careScheduleViewModel: CareScheduleViewModel by viewModels()
    private val classificationViewModel: ClassificationViewModel by viewModels()

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore result — worker still runs regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("flora_prefs", Context.MODE_PRIVATE)

        // Re-seed reference data after migration wipe
        PlantCareDatabase.getInstance(applicationContext).seedReferenceDataIfEmpty()

        // Notification channels + background worker
        FloraNotifications.ensureChannels(applicationContext)
        CareReminderWorker.schedule(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val deeplinkRoute = intent?.getStringExtra("deeplink")

        setContent {
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

            // Wire plantViewModel userId when auth state changes
            val currentUser by authViewModel.currentUser.collectAsState()
            LaunchedEffect(currentUser) {
                val userId = currentUser?.id
                plantViewModel.setUserId(userId)
                careScheduleViewModel.setUserId(userId)
                if (userId != null) {
                    plantViewModel.seedSamplePlantsIfNeeded()
                }
            }

            FloraTheme(dynamicColor = false, darkTheme = isDarkMode) {
                val navController = rememberNavController()
                LaunchedEffect(deeplinkRoute, currentUser) {
                    if (deeplinkRoute != null && currentUser != null) {
                        navController.navigate(deeplinkRoute) {
                            launchSingleTop = true
                        }
                    }
                }
                CompositionLocalProvider(LocalIsDarkMode provides isDarkMode) {
                    FloraNavGraph(
                        navController = navController,
                        isDarkMode = isDarkMode,
                        onDarkModeToggle = {
                            isDarkMode = !isDarkMode
                            prefs.edit().putBoolean("dark_mode", isDarkMode).apply()
                        },
                        authViewModel = authViewModel,
                        plantViewModel = plantViewModel,
                        weatherViewModel = weatherViewModel,
                        careScheduleViewModel = careScheduleViewModel,
                        classificationViewModel = classificationViewModel
                    )
                }
            }
        }
    }
}
