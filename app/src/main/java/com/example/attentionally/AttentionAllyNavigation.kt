package com.example.attentionally

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attentionally.presentation.auth.SplashScreen
import com.example.attentionally.presentation.auth.LoginScreen
import com.example.attentionally.presentation.auth.SignUpScreen
import com.example.attentionally.presentation.auth.GuestScreen
import com.example.attentionally.presentation.dashboard.MainScreen
import com.example.attentionally.presentation.auth.OnboardingStudentScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import org.koin.androidx.compose.koinViewModel
import com.example.attentionally.presentation.auth.AuthViewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.attentionally.data.User
import com.example.attentionally.data.UserRole
import com.google.firebase.auth.FirebaseAuth

/**
 * Navigation graph for Attention Ally - improved auth UX.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Guest : Screen("guest")
    object Main : Screen("main") // new route
    // Add additional screens as needed
}

@Composable
fun AttentionAllyNavigation(startDestination: String = Screen.Splash.route) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = koinViewModel()
    val userState by authViewModel.user.collectAsStateWithLifecycle(initialValue = null)

    // Decide startDestination depending on login state
    val firstDest = if (userState != null) Screen.Main.route else Screen.Login.route
    val (guestName, setGuestName) = rememberSaveable { mutableStateOf("") }
    val (guestUser, setGuestUser) = rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.mapSaver(
        save = { user -> mapOf("id" to user?.id, "name" to user?.name) },
        restore = { m ->
            if (m["id"] != null && m["name"] != null) {
                User(
                    id = m["id"] as? String ?: "",
                    name = m["name"] as? String ?: "",
                    role = UserRole.STUDENT
                )
            } else null
        }
    )) { mutableStateOf<User?>(null) }
    NavHost(navController = navController, startDestination = firstDest) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Login.route) {
            LoginScreen(
                onSignup = { navController.navigate(Screen.Signup.route) },
                onGuest = { navController.navigate(Screen.Guest.route) },
                onSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Signup.route) {
            SignUpScreen(
                onBack = { navController.popBackStack(Screen.Login.route, false) },
                onSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Guest.route) {
            GuestScreen(
                onBack = { navController.popBackStack(Screen.Login.route, false) },
                onSuccess = {
                    navController.navigate("onboard_student") {
                        popUpTo(0) { inclusive = false } // Let user go back to login if needed
                    }
                }
            )
        }
        // Onboarding screen after guest login
        composable("onboard_student") {
            OnboardingStudentScreen(
                onContinue = { name ->
                    val anonUid = FirebaseAuth.getInstance().currentUser?.uid
                        ?: "guest${System.currentTimeMillis()}"
                    setGuestUser(
                        User(
                            id = anonUid,
                            name = name,
                            role = UserRole.STUDENT
                        )
                    )
                    setGuestName(name)
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = false }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            val coroutineScope = rememberCoroutineScope()
            val userForMain = if (guestUser != null) guestUser else userState
            MainScreen(
                user = userForMain,
                onLogout = {
                    coroutineScope.launch {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        setGuestName("")
                        setGuestUser(null)
                    }
                }
            )
        }
        // Add future screens here
    }
}
