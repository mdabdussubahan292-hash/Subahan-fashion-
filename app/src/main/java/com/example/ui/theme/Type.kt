package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val HindSiliguriFontFamily = FontFamily(
  Font(R.font.hind_siliguri, FontWeight.Normal)
)

val SubahanTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp
  ),
  headlineMedium = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp
  ),
  headlineSmall = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp
  ),
  titleLarge = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 22.sp
  ),
  titleMedium = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp
  ),
  titleSmall = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 19.sp
  ),
  bodySmall = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 16.sp
  ),
  labelLarge = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 18.sp
  ),
  labelMedium = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp
  ),
  labelSmall = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 14.sp
  )
)
