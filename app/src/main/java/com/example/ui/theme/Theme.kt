package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightBrandScheme = lightColorScheme(
  primary = HotPinkPrimary,
  onPrimary = PureWhite,
  primaryContainer = SoftPinkContainer,
  onPrimaryContainer = OnPinkContainer,
  secondary = SunnyYellow,
  onSecondary = CharcoalText,
  secondaryContainer = SoftYellowContainer,
  onSecondaryContainer = OnYellowContainer,
  tertiary = EmeraldAccent,
  onTertiary = PureWhite,
  tertiaryContainer = SoftEmeraldContainer,
  background = LightGrayBg,
  onBackground = CharcoalText,
  surface = PureWhite,
  onSurface = CharcoalText,
  surfaceVariant = LightGrayBg,
  onSurfaceVariant = SlateMutedText,
  outline = CardBorderGray
)

private val DarkBrandScheme = darkColorScheme(
  primary = HotPinkPrimary,
  onPrimary = PureWhite,
  primaryContainer = CrimsonRedDark,
  onPrimaryContainer = PureWhite,
  secondary = SunnyYellow,
  onSecondary = CharcoalText,
  secondaryContainer = OnYellowContainer,
  onSecondaryContainer = SoftYellowContainer,
  tertiary = EmeraldAccent,
  onTertiary = PureWhite,
  background = DarkSurfaceBg,
  onBackground = DarkTextLight,
  surface = DarkCardSurface,
  onSurface = DarkTextLight,
  surfaceVariant = DarkCardSurface,
  onSurfaceVariant = CardBorderGray,
  outline = SlateMutedText
)

val SubahanShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp)
)

@Composable
fun SubahanFashionTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val scheme = if (darkTheme) DarkBrandScheme else LightBrandScheme
  MaterialTheme(
    colorScheme = scheme,
    typography = SubahanTypography,
    shapes = SubahanShapes,
    content = content
  )
}
