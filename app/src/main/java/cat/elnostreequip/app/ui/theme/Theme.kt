package cat.elnostreequip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors corporatius d'El Nostre Equip (del logo oficial).
private val EneBlue = Color(0xFF002070)
private val EneGold = Color(0xFFE0A800)
private val EneRed = Color(0xFFC81010)

private val LightColors = lightColorScheme(
    primary = EneBlue,
    onPrimary = Color.White,
    secondary = EneGold,
    onSecondary = Color(0xFF1A1A1A),
    tertiary = EneRed,
    background = Color(0xFFFFFBF7),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA8FF),
    onPrimary = Color(0xFF00124A),
    secondary = EneGold,
    tertiary = Color(0xFFFF8A80),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun ElNostreEquipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
