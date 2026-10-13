package cat.elnostreequip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Blanc-i-negre del València, amb taronja valencianista d'accent.
private val ValencianOrange = Color(0xFFF28C28)
private val ValencianBlack = Color(0xFF141414)

private val LightColors = lightColorScheme(
    primary = Color(0xFF9C4A00),
    onPrimary = Color.White,
    secondary = ValencianOrange,
    background = Color(0xFFFFFBF7),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = ValencianOrange,
    onPrimary = ValencianBlack,
    secondary = ValencianOrange,
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
