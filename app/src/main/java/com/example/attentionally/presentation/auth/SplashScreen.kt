package com.example.attentionally.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.attentionally.presentation.navigation.Screen
import com.example.attentionally.domain.model.User

/**
 * SplashScreen - waits for user authentication state to be determined, then navigates accordingly.
 * No more hardcoded delays - proper auth state checking.
 */
@Composable
fun SplashScreen(
    navController: NavController,
    userState: User?,
    isAuthStateLoading: Boolean
) {
    // Navigate based on actual user state once it's determined
    LaunchedEffect(userState, isAuthStateLoading) {
        // Only navigate when auth state loading is complete
        if (!isAuthStateLoading) {
            if (userState != null) {
                // User is logged in, go to main screen
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            } else {
                // No user found, go to login
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Attention Ally Loading...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}