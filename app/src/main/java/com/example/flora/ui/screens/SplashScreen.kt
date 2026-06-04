package com.example.flora.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.example.flora.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.flora.ui.viewmodel.AuthState
import com.example.flora.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, authViewModel: AuthViewModel) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.7f) }

    val authState by authViewModel.authState.collectAsState()

    // Animate the logo in, then hold so the brand reads before transitioning.
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(900))
        scale.animateTo(1f, animationSpec = tween(900))
    }

    // Minimum on-screen hold so the splash isn't a flash even on a hot start.
    // Auth check usually finishes well inside this window; we just wait it out.
    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Authenticated -> {
                delay(1800)
                navController.navigate("dashboard") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            AuthState.Unauthenticated -> {
                delay(2200)
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            AuthState.Loading -> { /* wait */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha.value).scale(scale.value)
        ) {
            // Brand logo (leaf mark + wordmark, baked into one PNG asset).
            Image(
                painter = painterResource(R.drawable.logo_myflora),
                contentDescription = "MyFlora",
                modifier = Modifier.size(260.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your Plant Care Companion",
                fontSize = 16.sp,
                color = Color.White.copy(0.80f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
            )
        }

        Text(
            text     = "Version 1.0",
            fontSize = 12.sp,
            color    = Color.White.copy(0.45f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).alpha(alpha.value)
        )
    }
}
