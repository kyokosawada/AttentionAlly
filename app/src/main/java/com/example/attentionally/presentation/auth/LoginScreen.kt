package com.example.attentionally.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.example.attentionally.util.calculateWindowSizeClass
import com.example.attentionally.util.getAdaptiveValues
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.remember
import timber.log.Timber

/**
 * LoginScreen - Clean, accessible, adaptive for any device. Uses theme and utils for responsive UI.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onSuccess: () -> Unit = {},
    onSignup: () -> Unit = {},
    onGuest: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val windowSizeClass = calculateWindowSizeClass()
    val adaptive = windowSizeClass.getAdaptiveValues()
    val snackbarHostState = remember { SnackbarHostState() }
    // Show error as user-friendly snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            Timber.e(it)
            viewModel.clearError()
        }
    }
    // Trigger navigation effect on success
    LaunchedEffect(state.isSuccess) { if (state.isSuccess) onSuccess() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = {
                        TextButton(onClick = { snackbarHostState.currentSnackbarData?.dismiss() }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                ) {
                    Text(data.visuals.message, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptive.horizontalPadding)
            ) {
            Spacer(Modifier.height(32.dp * adaptive.spacingScale))
            Text(
                text = "Attention Ally",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(18.dp * adaptive.spacingScale))
            Card(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = adaptive.cardMaxWidth),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = adaptive.cardPadding, vertical = adaptive.cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(adaptive.logoSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(adaptive.logoSize * 0.6f)
                        )
                    }
                    Spacer(Modifier.height(adaptive.cardContentSpacing))
                    Text(
                        text = "Welcome back!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Please sign in to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 20.dp * adaptive.spacingScale)
                    )
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(adaptive.formFieldSpacing))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(adaptive.cardContentSpacing * 2))
                    Button(
                        onClick = viewModel::login,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(adaptive.buttonHeight),
                        shape = MaterialTheme.shapes.large,
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        else Text("Login")
                    }
                    Spacer(Modifier.height(adaptive.cardContentSpacing + adaptive.dividerSpacing))
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(adaptive.dividerSpacing))
                    TextButton(
                        onClick = onSignup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = adaptive.altAuthSpacing)
                    ) {
                        Text("Create an account", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = onGuest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue as Guest", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}
}