package com.example.flora.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.flora.ui.components.GlassBackground
import com.example.flora.ui.components.LocalIsDarkMode
import com.example.flora.ui.screens.CareScheduleScreen
import com.example.flora.ui.screens.DashboardScreen
import com.example.flora.ui.screens.EditProfileScreen
import com.example.flora.ui.screens.DiseaseDiagnosisScreen
import com.example.flora.ui.screens.LoginScreen
import com.example.flora.ui.screens.MyPlantsScreen
import com.example.flora.ui.screens.OnboardingScreen
import com.example.flora.ui.screens.PlantDetailScreen
import com.example.flora.ui.screens.PlantIdentificationScreen
import com.example.flora.ui.screens.PlantLogsScreen
import com.example.flora.ui.screens.RegisterScreen
import com.example.flora.ui.screens.SplashScreen
import com.example.flora.ui.screens.UserProfileScreen
import com.example.flora.ui.screens.PrivacyPolicyScreen
import com.example.flora.ui.screens.HelpSupportScreen
import com.example.flora.ui.screens.AboutFloraScreen
import com.example.flora.ui.viewmodel.AuthViewModel
import com.example.flora.ui.viewmodel.CareScheduleViewModel
import com.example.flora.ui.viewmodel.ClassificationViewModel
import com.example.flora.ui.viewmodel.PlantViewModel
import com.example.flora.ui.viewmodel.WeatherViewModel

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home     : BottomNavItem("dashboard",           Icons.Filled.Home,          "Home")
    object Identify : BottomNavItem("plant_identification", Icons.Filled.CameraAlt,    "Identify")
    object Schedule : BottomNavItem("care_schedule",        Icons.Filled.CalendarMonth, "Schedule")
    object Plants   : BottomNavItem("my_plants",            Icons.Filled.LocalFlorist,  "Plants")
    object Profile  : BottomNavItem("user_profile",         Icons.Filled.Person,        "Profile")
}

@Composable
fun FloraNavGraph(
    navController: NavHostController,
    isDarkMode: Boolean,
    onDarkModeToggle: () -> Unit,
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel,
    weatherViewModel: WeatherViewModel,
    careScheduleViewModel: CareScheduleViewModel,
    classificationViewModel: ClassificationViewModel
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val authRoutes = setOf("splash", "login", "register")
    val showBottomBar = currentRoute != null && currentRoute !in authRoutes

    GlassBackground(isDarkMode = isDarkMode) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    FloraBottomNavBar(navController = navController, currentRoute = currentRoute)
                }
            }
        ) { paddingValues ->
            NavHost(
                navController    = navController,
                startDestination = "splash",
                modifier         = Modifier.padding(paddingValues)
            ) {
                composable("splash")   { SplashScreen(navController, authViewModel) }
                composable("login")    { LoginScreen(navController, authViewModel) }
                composable("register") { RegisterScreen(navController, authViewModel) }

                composable("dashboard") {
                    DashboardScreen(
                        navController = navController,
                        authViewModel = authViewModel,
                        plantViewModel = plantViewModel,
                        weatherViewModel = weatherViewModel,
                        careScheduleViewModel = careScheduleViewModel
                    )
                }
                composable("plant_identification") {
                    PlantIdentificationScreen(
                        navController = navController,
                        classificationViewModel = classificationViewModel,
                        plantViewModel = plantViewModel,
                        authViewModel = authViewModel,
                        weatherViewModel = weatherViewModel
                    )
                }
                composable("disease_diagnosis") {
                    DiseaseDiagnosisScreen(
                        navController = navController,
                        classificationViewModel = classificationViewModel,
                        plantViewModel = plantViewModel
                    )
                }
                composable("my_plants") {
                    MyPlantsScreen(
                        navController = navController,
                        plantViewModel = plantViewModel,
                        careScheduleViewModel = careScheduleViewModel,
                    )
                }
                composable("plant_detail/{plantId}") { backStackEntry ->
                    val plantId = backStackEntry.arguments?.getString("plantId")?.toIntOrNull() ?: 0
                    PlantDetailScreen(
                        navController = navController,
                        plantId = plantId,
                        authViewModel = authViewModel,
                        plantViewModel = plantViewModel,
                        careScheduleViewModel = careScheduleViewModel,
                    )
                }
                composable("care_schedule") {
                    CareScheduleScreen(
                        navController = navController,
                        careScheduleViewModel = careScheduleViewModel
                    )
                }
                composable("plant_logs/{plantId}") { backStackEntry ->
                    val plantId = backStackEntry.arguments?.getString("plantId")?.toIntOrNull() ?: 0
                    PlantLogsScreen(
                        navController = navController,
                        plantId = plantId,
                        authViewModel = authViewModel,
                        plantViewModel = plantViewModel
                    )
                }
                composable("user_profile") {
                    UserProfileScreen(
                        navController         = navController,
                        isDarkMode            = isDarkMode,
                        onDarkModeToggle      = onDarkModeToggle,
                        authViewModel         = authViewModel,
                        plantViewModel        = plantViewModel,
                        careScheduleViewModel = careScheduleViewModel
                    )
                }
                composable("edit_profile") {
                    EditProfileScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }
                composable("privacy_policy") { PrivacyPolicyScreen(navController) }
                composable("help_support") { HelpSupportScreen(navController) }
                composable("about_flora") { AboutFloraScreen(navController) }
            }
        }
    }
}

@Composable
fun FloraBottomNavBar(navController: NavController, currentRoute: String?) {
    val isDark      = LocalIsDarkMode.current
    val topAlpha    = if (isDark) 0.18f else 0.30f
    val bottomAlpha = if (isDark) 0.09f else 0.15f
    val borderTop   = if (isDark) 0.30f else 0.65f
    val borderBot   = if (isDark) 0.09f else 0.20f

    // Order: Home, Schedule, [Identify FAB center], Plants, Profile
    val items = listOf(
        BottomNavItem.Home, BottomNavItem.Schedule,
        BottomNavItem.Identify,
        BottomNavItem.Plants, BottomNavItem.Profile
    )

    fun go(item: BottomNavItem) {
        if (currentRoute != item.route) {
            if (item.route == "dashboard") {
                navController.popBackStack("dashboard", inclusive = false)
            } else {
                navController.navigate(item.route) {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState    = true
                }
            }
        }
    }

    // Outer Box is NOT clipped so the raised FAB can overflow above the bar.
    // The bar gradient Row lives inside with its own clip; the FAB is a sibling
    // aligned to top-center and lifted with negative offset.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            // Extra top padding reserves visual space for the raised FAB so it
            // doesn't collide with screen content above.
            .padding(top = 28.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(28.dp), false, Color(0x35000000), Color(0x65000000))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(topAlpha), Color.White.copy(bottomAlpha))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(borderTop), Color.White.copy(borderBot))), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                if (item is BottomNavItem.Identify) {
                    // Reserve the center slot — actual FAB is drawn as a sibling
                    // overlay so it can overflow above the clipped bar.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    )
                } else {
                    val contentAlpha by animateFloatAsState(if (selected) 1f else 0.45f, tween(200), label = "nav_alpha_${item.label}")
                    val indicatorWidth by animateDpAsState(if (selected) 16.dp else 0.dp, tween(250, easing = FastOutSlowInEasing), label = "nav_ind_${item.label}")
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { go(item) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(item.icon, item.label, tint = Color.White.copy(contentAlpha), modifier = Modifier.size(22.dp))
                        Text(item.label, fontSize = 10.sp, color = Color.White.copy(contentAlpha))
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.height(2.dp).width(indicatorWidth).clip(RoundedCornerShape(1.dp)).background(Color.White))
                    }
                }
            }
        }

        // Raised FAB overlay — sits above the bar, centered horizontally.
        // With outer top padding of 28.dp, TopCenter lands at the bar's top edge;
        // negative offset lifts the FAB so ~half its height rises above the bar.
        val identify = BottomNavItem.Identify
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-11).dp)
                .size(62.dp)
                .shadow(14.dp, CircleShape, false, Color(0x40000000), Color(0x70000000))
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF80DEEA), Color(0xFF26C6DA))
                    )
                )
                .border(2.dp, Color.White.copy(0.85f), CircleShape)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { go(identify) },
            contentAlignment = Alignment.Center
        ) {
            Icon(identify.icon, identify.label, tint = Color.Black, modifier = Modifier.size(28.dp))
        }
    }
}
