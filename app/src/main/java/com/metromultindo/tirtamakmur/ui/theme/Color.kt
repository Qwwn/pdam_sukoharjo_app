package com.metromultindo.tirtamakmur.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF0277BD)
val PrimaryVariant = Color(0xFF01579B)
val SecondaryBlue = Color(0xFF29B6F6)
val Background = Color(0xFFFAFAFA)
val Surface = Color.White
val OnPrimary = Color.White
val OnSecondary = Color.Black
val OnBackground = Color(0xFF212121)
val OnSurface = Color(0xFF212121)
val Error = Color(0xFFB00020)
val OnError = Color.White

val LightAppColors = AppColorsScheme(
    primary = Color(0xFF1976D2),
    primaryDark = Color(0xFF1565C0),
    secondary = Color(0xFFF5F5F5),
    background = Color(0xFFF9F9F9),
    surfaceBackground = Color.White,
    cardBackground = Color.White,
    textPrimary = Color(0xFF333333),
    textSecondary = Color(0xFF666666),
    textLight = Color(0xFF999999),
    textOnPrimary = Color.White,
    success = Color(0xFF2E7D32),
    successLight = Color(0xFFE8F5E9),
    warning = Color(0xFFFFA000),
    warningLight = Color(0xFFFFF8E1),
    error = Color(0xFFC62828),
    errorLight = Color(0xFFFFEBEE),
    border = Color(0xFFDDDDDD),
    divider = Color(0xFFEEEEEE),
    ripple = Color(0x20000000),
    unpaidBadgeBackground = Color(0xFFFCECEC),
    paidBadgeBackground = Color(0xFFE1F5E7),
    unpaidTextColor = Color(0xFFB71C1C),
    paidTextColor = Color(0xFF1B5E20)
)


data class AppColorsScheme(
    val primary: Color,
    val primaryDark: Color,
    val secondary: Color,
    val background: Color,
    val surfaceBackground: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textLight: Color,
    val textOnPrimary: Color,
    val success: Color,
    val successLight: Color,
    val warning: Color,
    val warningLight: Color,
    val error: Color,
    val errorLight: Color,
    val border: Color,
    val divider: Color,
    val ripple: Color,
    val unpaidBadgeBackground: Color,
    val paidBadgeBackground: Color,
    val unpaidTextColor: Color,
    val paidTextColor: Color
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
