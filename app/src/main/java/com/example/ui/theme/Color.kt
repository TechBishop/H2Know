package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark Mode (Default) — High-Contrast Civic Water Dashboard
val DarkSurface0 = Color(0xFF0C0E11)
val DarkSurface1 = Color(0xFF14171C)
val DarkSurface2 = Color(0xFF1C2026)
val DarkSurface3 = Color(0xFF262B33)
val DarkSurface4 = Color(0xFF323844)
val DarkInk1 = Color(0xFFE8E6E2)
val DarkInk2 = Color(0xFFA8A49D)
val DarkInk3 = Color(0xFF7C7872)
val DarkBrand = Color(0xFF8EB6C6)
val DarkBrandSoft = Color(0x248EB6C6)
val DarkBrandGlow = Color(0x148EB6C6)
val DarkSafe = Color(0xFF9BB5A4)
val DarkSafeBg = Color(0x249BB5A4)
val DarkCaution = Color(0xFFCBB892)
val DarkCautionBg = Color(0x24CBB892)
val DarkDanger = Color(0xFFC9A09A)
val DarkDangerBg = Color(0x24C9A09A)
val DarkInfo = Color(0xFF8EB6C6)
val DarkInfoBg = Color(0x248EB6C6)
val DarkBorder = Color(0xFF2A2E35)
val DarkBorderStrong = Color(0xFF3A4048)
val DarkDonate = Color(0xFFFF7A77)
val DarkDonateBg = Color(0x2EFF7A77)

// Light Mode — High-Contrast Daylight Civic Palette
val LightSurface0 = Color(0xFFF4F2EE)
val LightSurface1 = Color(0xFFFFFDFA)
val LightSurface2 = Color(0xFFE7E4DE)
val LightSurface3 = Color(0xFFD9D5CE)
val LightSurface4 = Color(0xFFCBC6BE)
val LightInk1 = Color(0xFF1A1C1F)
val LightInk2 = Color(0xFF3E434A)
val LightInk3 = Color(0xFF6E6860)
val LightBrand = Color(0xFF3D6F82)
val LightBrandSoft = Color(0x1F3D6F82)
val LightBrandGlow = Color(0x143D6F82)
val LightSafe = Color(0xFF3E6B52)
val LightSafeBg = Color(0x1F3E6B52)
val LightCaution = Color(0xFF8A6A32)
val LightCautionBg = Color(0x1F8A6A32)
val LightDanger = Color(0xFF8C4E48)
val LightDangerBg = Color(0x1F8C4E48)
val LightInfo = Color(0xFF3D6F82)
val LightInfoBg = Color(0x1F3D6F82)
val LightBorder = Color(0xFFD9D5CE)
val LightBorderStrong = Color(0xFFB7B1A8)
val LightDonate = Color(0xFFFF5E5B)
val LightDonateBg = Color(0x26FF5E5B)

@Immutable
data class H2KnowPalette(
    val isDark: Boolean,
    val surface0: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val surface4: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val brand: Color,
    val brandSoft: Color,
    val brandGlow: Color,
    val safe: Color,
    val safeBg: Color,
    val caution: Color,
    val cautionBg: Color,
    val danger: Color,
    val dangerBg: Color,
    val info: Color,
    val infoBg: Color,
    val border: Color,
    val borderStrong: Color,
    val donate: Color,
    val donateBg: Color
)

val DarkH2KnowPalette = H2KnowPalette(
    isDark = true,
    surface0 = DarkSurface0,
    surface1 = DarkSurface1,
    surface2 = DarkSurface2,
    surface3 = DarkSurface3,
    surface4 = DarkSurface4,
    ink = DarkInk1,
    ink2 = DarkInk2,
    ink3 = DarkInk3,
    brand = DarkBrand,
    brandSoft = DarkBrandSoft,
    brandGlow = DarkBrandGlow,
    safe = DarkSafe,
    safeBg = DarkSafeBg,
    caution = DarkCaution,
    cautionBg = DarkCautionBg,
    danger = DarkDanger,
    dangerBg = DarkDangerBg,
    info = DarkInfo,
    infoBg = DarkInfoBg,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
    donate = DarkDonate,
    donateBg = DarkDonateBg
)

val LightH2KnowPalette = H2KnowPalette(
    isDark = false,
    surface0 = LightSurface0,
    surface1 = LightSurface1,
    surface2 = LightSurface2,
    surface3 = LightSurface3,
    surface4 = LightSurface4,
    ink = LightInk1,
    ink2 = LightInk2,
    ink3 = LightInk3,
    brand = LightBrand,
    brandSoft = LightBrandSoft,
    brandGlow = LightBrandGlow,
    safe = LightSafe,
    safeBg = LightSafeBg,
    caution = LightCaution,
    cautionBg = LightCautionBg,
    danger = LightDanger,
    dangerBg = LightDangerBg,
    info = LightInfo,
    infoBg = LightInfoBg,
    border = LightBorder,
    borderStrong = LightBorderStrong,
    donate = LightDonate,
    donateBg = LightDonateBg
)

val LocalH2KnowPalette = staticCompositionLocalOf { DarkH2KnowPalette }
