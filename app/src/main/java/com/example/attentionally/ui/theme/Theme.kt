package com.example.attentionally.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Attention Ally Material3 Theme - autism-friendly palette.
 * - Blues and greens are calming and focus-friendly; harsh reds avoided
 * - Soft yellow (sparingly) for accents, error uses softened orange
 * - Light mode uses maximum whitespace/minimal stimulation, dark mode adjusts hues
 * See PRD sec. 8: Material3 Expressive, ASD-aware.
 */

// --- Light Color Scheme ---
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2), // Blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF63A4FF),
    onPrimaryContainer = Color(0xFF222222),
    secondary = Color(0xFF66BB6A), // Green
    onSecondary = Color(0xFF222222),
    secondaryContainer = Color(0xFFA5D6A7),
    onSecondaryContainer = Color(0xFF222222),
    tertiary = Color(0xFFF6BB00), // Gentle Yellow Accents
    onTertiary = Color(0xFF222222),
    error = Color(0xFFFF7043), // Soft Orange/Red
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF222222),
    surface = Color(0xFFE3F2FD),
    onSurface = Color(0xFF222222)
)

// --- Dark Color Scheme ---
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF63A4FF), // Lighter Blue
    onPrimary = Color(0xFF222222),
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFEEEEEE),
    secondary = Color(0xFFA5D6A7), // Lighter Green
    onSecondary = Color(0xFF222222),
    secondaryContainer = Color(0xFF388E3C),
    onSecondaryContainer = Color(0xFFEDEDED),
    tertiary = Color(0xFFFBEA94), // Lighter Yellow Accents
    onTertiary = Color(0xFF222222),
    error = Color(0xFFFF7043),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFF232938), // Dark blue-gray
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF263238), // Indigo/blue-gray for surface
    onSurface = Color(0xFFEDEDED)
)

@Composable
fun AttentionAllyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(), // Default; adjust for accessibility/fonts later
        shapes = Shapes(),
        content = content
    )
}