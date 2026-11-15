package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Geometric Twentieth-Century (Tw Cen MT) inspired sans-serif for the Melvore
 * brand wordmark and display titles, bundled locally in res/font/jost.ttf.
 */
@OptIn(ExperimentalTextApi::class)
val MelvoreBrandFontFamily = FontFamily(
    Font(
        resId = R.font.jost,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300))
    ),
    Font(
        resId = R.font.jost,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.jost,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.jost,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.jost,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.jost,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

/**
 * Clean editorial body font bundled locally in res/font/plus_jakarta_sans.ttf.
 */
@OptIn(ExperimentalTextApi::class)
val MelvoreBodyFontFamily = FontFamily(
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

data class MelvoreExtendedColors(
    val muted: Color,
    val borderSubtle: Color,
    val accentNavy: Color,
    val emerald: Color,
    val amber: Color,
    val crimson: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val darkEditorialBg: Color,
    val campaignCobalt: Color,
    val campaignBerry: Color,
    val campaignSun: Color,
    val campaignPlum: Color
)

val LocalMelvoreColors = staticCompositionLocalOf {
    MelvoreExtendedColors(
        muted = Color(0xFF666666),
        borderSubtle = Color(0x18000000),
        accentNavy = Color(0xFF141414),
        emerald = Color(0xFF047857),
        amber = Color(0xFFB45309),
        crimson = Color(0xFFB91C1C),
        glassSurface = Color(0xEBFFFFFF),
        glassBorder = Color(0x26000000),
        darkEditorialBg = Color(0xFF000000),
        campaignCobalt = Color(0xFF1E3A8A),
        campaignBerry = Color(0xFF9F1239),
        campaignSun = Color(0xFFB45309),
        campaignPlum = Color(0xFF27272A)
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF141414),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF1F1F1F),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF5C5C5C),
    outline = Color(0x1F000000),
    error = Color(0xFFB91C1C),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFFD4D4D4),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFF9A9A9A),
    outline = Color(0x24FFFFFF),
    error = Color(0xFFEF4444),
    onError = Color.Black
)

val MelvoreTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = MelvoreBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MelvoreBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = MelvoreBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = MelvoreBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp
    )
)

val MelvoreShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun MelvoreTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val extended = if (darkTheme) {
        MelvoreExtendedColors(
            muted = Color(0xFF9A9A9A),
            borderSubtle = Color(0x22FFFFFF),
            accentNavy = Color(0xFFE5E5E5),
            emerald = Color(0xFF34D399),
            amber = Color(0xFFFBBF24),
            crimson = Color(0xFFF87171),
            glassSurface = Color(0xE60A0A0A),
            glassBorder = Color(0x2EFFFFFF),
            darkEditorialBg = Color(0xFF0A0A0A),
            campaignCobalt = Color(0xFF93C5FD),
            campaignBerry = Color(0xFFFDA4AF),
            campaignSun = Color(0xFFFBBF24),
            campaignPlum = Color(0xFFD4D4D8)
        )
    } else {
        MelvoreExtendedColors(
            muted = Color(0xFF666666),
            borderSubtle = Color(0x18000000),
            accentNavy = Color(0xFF141414),
            emerald = Color(0xFF047857),
            amber = Color(0xFFB45309),
            crimson = Color(0xFFB91C1C),
            glassSurface = Color(0xEBFFFFFF),
            glassBorder = Color(0x24000000),
            darkEditorialBg = Color(0xFF000000),
            campaignCobalt = Color(0xFF1D4ED8),
            campaignBerry = Color(0xFFB91C1C),
            campaignSun = Color(0xFFB45309),
            campaignPlum = Color(0xFF27272A)
        )
    }

    CompositionLocalProvider(LocalMelvoreColors provides extended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = MelvoreTypography,
            shapes = MelvoreShapes,
            content = content
        )
    }
}
