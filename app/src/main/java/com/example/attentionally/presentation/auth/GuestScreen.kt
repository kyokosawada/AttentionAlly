package com.example.attentionally.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarDuration
import timber.log.Timber
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.example.attentionally.util.calculateWindowSizeClass
import com.example.attentionally.util.getAdaptiveValues

/**
 * GuestScreen polished for Material3/UX consistency. Shows guest icon, subtitle, big buttons, themed loaders.
 */
@Composable
fun GuestScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onSuccess: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var consentChecked by remember { mutableStateOf(false) }
    val windowSizeClass = calculateWindowSizeClass()
    val adaptive = windowSizeClass.getAdaptiveValues()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.isSuccess) { if (state.isSuccess) onSuccess() }
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
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
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(18.dp * adaptive.spacingScale))
                Card(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = adaptive.cardMaxWidth),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = adaptive.cardPadding,
                            vertical = adaptive.cardPadding
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(adaptive.logoSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Face,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(adaptive.logoSize * 0.6f)
                            )
                        }
                        Spacer(Modifier.height(adaptive.cardContentSpacing))
                        Text(
                            "Continue as Guest",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "Minimal features. Parental consent required.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 20.dp * adaptive.spacingScale)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = consentChecked,
                                onCheckedChange = { consentChecked = it })
                            Text("I have parental/guardian consent.")
                        }
                        Spacer(Modifier.height(adaptive.cardContentSpacing * 2))
                        Button(
                            onClick = { viewModel.signInAnonymously(consentChecked) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(adaptive.buttonHeight),
                            shape = MaterialTheme.shapes.large,
                            enabled = consentChecked && !state.isLoading
                        ) {
                            if (state.isLoading) CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            else Text("Sign In as Guest")
                        }
                        Spacer(Modifier.height(adaptive.cardContentSpacing + adaptive.dividerSpacing))
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(adaptive.dividerSpacing))
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = adaptive.altAuthSpacing)
                        ) {
                            Text("Back to Login", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}