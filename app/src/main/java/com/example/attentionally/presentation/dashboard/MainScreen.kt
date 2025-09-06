package com.example.attentionally.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attentionally.domain.model.User

/**
 * Dummy main screen for authenticated users. Shows user info and logout option.
 */
@Composable
fun MainScreen(
    user: User?,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome, ${user?.name ?: "Unknown"}!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Role: ${user?.role?.name ?: "?"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(0.5f)
            ) { Text("Logout") }
        }
    }
}
