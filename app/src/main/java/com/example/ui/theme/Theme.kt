package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val VibrantColorScheme =
  lightColorScheme(
    primary = VibrantPurple,
    secondary = VibrantPurpleOnContainer,
    tertiary = VibrantRed,
    background = VibrantBg,
    surface = VibrantCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = VibrantText,
    onSurface = VibrantText,
    surfaceVariant = VibrantNavBarBg,
    onSurfaceVariant = VibrantMutedText,
    outline = VibrantCardBorder,
    primaryContainer = VibrantPurpleContainer,
    onPrimaryContainer = VibrantPurpleOnContainer,
    error = VibrantRed,
    errorContainer = VibrantRedContainer,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Force vibrant theme by default
  dynamicColor: Boolean = false, // Disable dynamic colors to keep safety branding consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = VibrantColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

