package com.sfdnsapp.pro.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---- Dark scheme (اصلی، پیش‌فرض قبلی برند) ----
private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0E3846),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0F3D2E),
    onSecondaryContainer = Color(0xFFE8F5E9),
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = CyberBg,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = PingSlow,
    onError = Color.White
)

// ---- Light scheme (واقعی، نه فقط رنگ‌های تیره روی زمینه‌ی روشن) ----
// پس‌زمینه‌ی روشن و متن تیره با همون لهجه‌ی رنگی نئون به‌عنوان اکسنت،
// طوری که کنتراست درست باشه و آیکون‌های نوار وضعیت هم خونا بمونن.
private val CyberLightColorScheme = lightColorScheme(
    primary = Color(0xFF0E7490),        // cyan تیره‌تر برای کنتراست روی سفید
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF083344),
    secondary = Color(0xFF047857),      // green تیره‌تر
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFF6D28D9),
    onTertiary = Color.White,
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF11131A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11131A),
    surfaceVariant = Color(0xFFEDEFF5),
    onSurfaceVariant = Color(0xFF4B5265),
    outline = Color(0xFFD3D7E3),
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CyberDarkColorScheme else CyberLightColorScheme

    // آیکون‌های نوار وضعیت/ناوبری رو دقیقاً هماهنگ با تم واقعی (نه حدس
    // سیستم) تنظیم می‌کنیم؛ قبلاً enableEdgeToEdge() به‌تنهایی این کار رو
    // بر اساس isSystemInDarkTheme حدس می‌زد که با تم تیره‌ی همیشه-فعال قبلی
    // در تناقض بود و روی گوشی‌های حالت روشن، آیکون‌های نوار وضعیت روی
    // پس‌زمینه‌ی تیره نامرئی می‌شدند.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
