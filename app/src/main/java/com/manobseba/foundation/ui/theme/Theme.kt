package com.manobseba.foundation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manobseba.foundation.R

val NotoSansBengaliFont = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
    Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

val PlusJakartaFont = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

data class FoundationPalette(
    val key: String,
    val labelBn: String,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val deepGradientEnd: Color
)

val FOUNDATION_PALETTES = listOf(
    FoundationPalette("green", "ইসলামিক সবুজ", Color(0xFF087A43), Color(0xFFD1FAE5), Color(0xFF0E7490), Color(0xFF044927)),
    FoundationPalette("blue", "সমুদ্র নীল", Color(0xFF1D4ED8), Color(0xFFDBEAFE), Color(0xFF0284C7), Color(0xFF1E3A8A)),
    FoundationPalette("violet", "বেগুনি আভা", Color(0xFF6D28D9), Color(0xFFEDE9FE), Color(0xFF9333EA), Color(0xFF4C1D95)),
    FoundationPalette("rose", "রক্তিম গোলাপ", Color(0xFFBE123C), Color(0xFFFFE4E6), Color(0xFFE11D48), Color(0xFF881337)),
    FoundationPalette("orange", "সূর্যাস্ত কমলা", Color(0xFFC2410C), Color(0xFFFFEDD5), Color(0xFFD97706), Color(0xFF7C2D12)),
    FoundationPalette("teal", "নীলাভ টিল", Color(0xFF0F766E), Color(0xFFCCFBF1), Color(0xFF0369A1), Color(0xFF134E4A)),
    FoundationPalette("maroon", "রাজকীয় মেরুন", Color(0xFF7F1D1D), Color(0xFFFEE2E2), Color(0xFFB91C1C), Color(0xFF450A0A))
)

fun paletteForKey(key: String?): FoundationPalette =
    FOUNDATION_PALETTES.firstOrNull { it.key == key } ?: FOUNDATION_PALETTES.first()

private fun buildLightColors(p: FoundationPalette): ColorScheme = lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.deepGradientEnd,
    secondary = p.secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF4F7F5),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEBF1ED),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private fun buildDarkColors(p: FoundationPalette): ColorScheme = darkColorScheme(
    primary = p.primaryContainer,
    onPrimary = p.deepGradientEnd,
    primaryContainer = p.deepGradientEnd,
    onPrimaryContainer = p.primaryContainer,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = Color(0xFF0B1410),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF13201B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1D2E27),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val ManobsebaTypography = Typography(
    displaySmall = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = NotoSansBengaliFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

val ManobsebaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ManobsebaTheme(
    paletteKey: String = "green",
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = paletteForKey(paletteKey)
    val colors = if (darkTheme) buildDarkColors(palette) else buildLightColors(palette)
    MaterialTheme(
        colorScheme = colors,
        typography = ManobsebaTypography,
        shapes = ManobsebaShapes,
        content = content
    )
}
