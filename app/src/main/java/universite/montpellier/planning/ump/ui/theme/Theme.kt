package universite.montpellier.planning.ump.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primaire,
    onPrimary = Color.White,
    primaryContainer = PrimaireConteneur,
    onPrimaryContainer = SurPrimaireConteneur,
    secondaryContainer = PrimaireConteneur,
    onSecondaryContainer = SurPrimaireConteneur,
    background = Fond,
    onBackground = Color(0xFF181A20),
    surface = Fond,
    onSurface = Color(0xFF181A20),
    surfaceVariant = SurfaceVariante,
    onSurfaceVariant = TexteSecondaire,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface,
    outline = Contour,
    outlineVariant = Contour,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaireSombre,
    onPrimary = Color(0xFF0B1A66),
    primaryContainer = PrimaireConteneurSombre,
    onPrimaryContainer = SurPrimaireConteneurSombre,
    secondaryContainer = PrimaireConteneurSombre,
    onSecondaryContainer = SurPrimaireConteneurSombre,
    background = FondSombre,
    onBackground = Color(0xFFE6E8F0),
    surface = FondSombre,
    onSurface = Color(0xFFE6E8F0),
    surfaceVariant = SurfaceVarianteSombre,
    onSurfaceVariant = TexteSecondaireSombre,
    surfaceContainerLow = SurfaceSombre,
    surfaceContainer = SurfaceSombre,
    surfaceContainerHigh = SurfaceVarianteSombre,
    outline = ContourSombre,
    outlineVariant = ContourSombre,
)

@Composable
fun UMPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
