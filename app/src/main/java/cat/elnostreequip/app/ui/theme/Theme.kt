package cat.elnostreequip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Colors corporatius d'El Nostre Equip ────────────────────────────────────
val EneBlue = Color(0xFF002070)
val EneBlueDark = Color(0xFF001650)
val EneGold = Color(0xFFE0A800)
val EneRed = Color(0xFFC81010)

/** Taronja expressiu (demana d'Izan): degradats i accents. */
val EneOrange = Color(0xFFFF7B00)
val EneOrangeLight = Color(0xFFFFA726)
val EneOrangeContainer = Color(0xFFFFE8D1)

/** Degradat taronja signatura. */
fun eneOrangeBrush(): Brush = Brush.linearGradient(
    listOf(EneOrange, EneOrangeLight)
)

fun eneOrangeBrushVertical(): Brush = Brush.verticalGradient(
    listOf(EneOrange, EneOrangeLight)
)

// ─── Esquemes de color ───────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary = EneBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = EneBlueDark,
    secondary = EneGold,
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFFFFF3D6),
    onSecondaryContainer = Color(0xFF5C4300),
    tertiary = EneOrange,
    onTertiary = Color.White,
    tertiaryContainer = EneOrangeContainer,
    onTertiaryContainer = Color(0xFF7A3A00),
    error = EneRed,
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF1A1C22),
    surface = Color.White,
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFF1EFE9),
    onSurfaceVariant = Color(0xFF5A5E6B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F4EE),
    surfaceContainer = Color(0xFFF1EDE5),
    surfaceContainerHigh = Color(0xFFEBE6DC),
    surfaceContainerHighest = Color(0xFFE5DFD4),
    outline = Color(0xFFC9C4B8),
    outlineVariant = Color(0xFFE3DED2)
)

/** Mode fosc esportiu: blau nit profund + taronja que esclata. */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB4FF),
    onPrimary = Color(0xFF00124A),
    primaryContainer = Color(0xFF1B2A5E),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = EneGold,
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF4A3A00),
    onSecondaryContainer = Color(0xFFFFE3A3),
    tertiary = EneOrangeLight,
    onTertiary = Color(0xFF2A1500),
    tertiaryContainer = Color(0xFF5C2E00),
    onTertiaryContainer = Color(0xFFFFD9B3),
    error = Color(0xFFFF8A80),
    background = Color(0xFF0A0E1A),
    onBackground = Color(0xFFE8EAF2),
    surface = Color(0xFF11162A),
    onSurface = Color(0xFFE8EAF2),
    surfaceVariant = Color(0xFF1C2340),
    onSurfaceVariant = Color(0xFFA8AEC6),
    surfaceContainerLowest = Color(0xFF060913),
    surfaceContainerLow = Color(0xFF11162A),
    surfaceContainer = Color(0xFF161D36),
    surfaceContainerHigh = Color(0xFF1C2340),
    surfaceContainerHighest = Color(0xFF232B4D),
    outline = Color(0xFF3A4468),
    outlineVariant = Color(0xFF252D4D)
)

// ─── Formes expressives ──────────────────────────────────────────────────────
private val EneShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// ─── Tipografia amb èmfasi ───────────────────────────────────────────────────
private val EneTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ElNostreEquipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = EneShapes,
        typography = EneTypography,
        content = content
    )
}
