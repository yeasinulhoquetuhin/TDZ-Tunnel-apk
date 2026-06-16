package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// "30x Advanced Light Theme" with glassmorphing and rich accent colors
private val UltraLightColorScheme = lightColorScheme(
    primary = Color(0xFF0052FF), // Vibrant Blue
    secondary = Color(0xFF6B11FF), // Deep Violet
    tertiary = Color(0xFF00D1FF), // Cyan Accent
    background = Color(0xFFF4F7FB), // Very soft blue-grey white
    surface = Color(0xFFFFFFFF), // Pure White
    surfaceVariant = Color(0xFFE8EEF5), // Light Glass Background
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0A1428), // Deep Navy for text
    onSurface = Color(0xFF0A1428), // Deep Navy for text
    onSurfaceVariant = Color(0xFF4A5568), // Grey for secondary text
    outline = Color(0xFFC4D1EA),
    error = StatusDisconnected
)

@Composable
fun MyApplicationTheme(
    primaryColorHex: String = "#0052FF",
    darkTheme: Boolean = false, // Forced Light Mode as requested
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(primaryColorHex))
    } catch (e: Exception) {
        Color(0xFF0052FF)
    }
    
    val dynamicColors = UltraLightColorScheme.copy(
        primary = primaryColor,
        tertiary = primaryColor.copy(alpha = 0.8f)
    )

    // Apply the glass background color modifier based on the primary color logic?
    // We can just inject dynamicColors into MaterialTheme

    MaterialTheme(
        colorScheme = dynamicColors,
        typography = Typography,
        content = content
    )
}

// Global modifier for adding dynamic glassmorphic backgrounds to Cards and surfaces
fun Modifier.glassmorphicBackground(
    primaryColor: Color,
    alphaBase: Float = 0.05f
): Modifier = this
    .clip(RoundedCornerShape(16.dp))
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = alphaBase + 0.03f),
                primaryColor.copy(alpha = alphaBase),
                Color.White.copy(alpha = 0.5f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f),
                Color.White.copy(alpha = 0.1f)
            )
        ),
        shape = RoundedCornerShape(16.dp)
    )
