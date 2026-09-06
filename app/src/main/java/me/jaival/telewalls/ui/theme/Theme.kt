package me.jaival.telewalls.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalReduceAnimations = staticCompositionLocalOf { false }

private val DarkMonetColorScheme = darkColorScheme(
    primary = MonetPrimary80,
    onPrimary = MonetPrimary40,
    primaryContainer = MonetPrimaryContainerDark,
    onPrimaryContainer = MonetOnPrimaryContainerDark,
    secondary = MonetSecondary80,
    onSecondary = MonetSecondary40,
    secondaryContainer = MonetSecondaryContainerDark,
    onSecondaryContainer = MonetOnSecondaryContainerDark,
    tertiary = MonetTertiary80,
    onTertiary = MonetTertiary40,
    tertiaryContainer = MonetTertiaryContainerDark,
    onTertiaryContainer = MonetOnTertiaryContainerDark,
    background = MonetBackgroundDark,
    onBackground = MonetOnBackgroundDark,
    surface = MonetSurfaceDark,
    onSurface = MonetOnSurfaceDark,
    surfaceVariant = MonetSurfaceVariantDark,
    onSurfaceVariant = MonetOnSurfaceVariantDark,
    surfaceContainer = MonetSurfaceContainerDark,
    surfaceContainerHigh = MonetSurfaceContainerHighDark,
    outline = MonetOutlineDark,
    outlineVariant = MonetOutlineVariantDark
)

private val LightMonetColorScheme = lightColorScheme(
    primary = MonetPrimary40,
    onPrimary = MonetPrimary80,
    primaryContainer = MonetPrimaryContainerLight,
    onPrimaryContainer = MonetOnPrimaryContainerLight,
    secondary = MonetSecondary40,
    onSecondary = MonetSecondary80,
    secondaryContainer = MonetSecondaryContainerDark,
    onSecondaryContainer = MonetOnSecondaryContainerDark,
    tertiary = MonetTertiary40,
    onTertiary = MonetTertiary80,
    background = MonetBackgroundLight,
    onBackground = MonetOnBackgroundLight,
    surface = MonetSurfaceLight,
    onSurface = MonetOnSurfaceLight,
    surfaceVariant = MonetSurfaceVariantLight,
    onSurfaceVariant = MonetOnSurfaceVariantLight,
    surfaceContainer = MonetSurfaceContainerLight,
    surfaceContainerHigh = MonetSurfaceContainerHighLight,
    outline = MonetOutlineLight,
    outlineVariant = MonetOutlineVariantLight
)

@Composable
fun TeleWallsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    reduceAnimations: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkMonetColorScheme
        else -> LightMonetColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalReduceAnimations provides reduceAnimations) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

