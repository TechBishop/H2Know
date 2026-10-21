package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark Mode (Default) — High-Contrast Civic Water Dashboard
val DarkSurface0 = Color(0xFF090D14)
val DarkSurface1 = Color(0xFF111823)
val DarkSurface2 = Color(0xFF192332)
val DarkSurface3 = Color(0xFF243246)
val DarkSurface4 = Color(0xFF2F4058)
val DarkInk1 = Color(0xFFF8FAFC)
val DarkInk2 = Color(0xFFCBD5E1)
val DarkInk3 = Color(0xFF94A3B8)
val DarkBrand = Color(0xFF38BDF8)
val DarkBrandSoft = Color(0x2E38BDF8)
val DarkBrandGlow = Color(0x4538BDF8)
val DarkSafe = Color(0xFF34D399)
val DarkSafeBg = Color(0x2934D399)
val DarkCaution = Color(0xFFFBBF24)
val DarkCautionBg = Color(0x29FBBF24)
val DarkDanger = Color(0xFFF87171)
val DarkDangerBg = Color(0x29F87171)
val DarkInfo = Color(0xFF38BDF8)
val DarkInfoBg = Color(0x2E38BDF8)
val DarkBorder = Color(0xFF29384D)
val DarkBorderStrong = Color(0xFF3E526E)
val DarkDonate = Color(0xFFFF7A77)
val DarkDonateBg = Color(0x2EFF7A77)

// Light Mode — High-Contrast Daylight Civic Palette
val LightSurface0 = Color(0xFFF1F5F9)
val LightSurface1 = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFE2E8F0)
val LightSurface3 = Color(0xFFCBD5E1)
val LightSurface4 = Color(0xFFB4C2D4)
val LightInk1 = Color(0xFF0F172A)
val LightInk2 = Color(0xFF334155)
val LightInk3 = Color(0xFF64748B)
val LightBrand = Color(0xFF0284C7)
val LightBrandSoft = Color(0x1F0284C7)
val LightBrandGlow = Color(0x400284C7)
val LightSafe = Color(0xFF059669)
val LightSafeBg = Color(0x1F059669)
val LightCaution = Color(0xFFD97706)
val LightCautionBg = Color(0x1FD97706)
val LightDanger = Color(0xFFDC2626)
val LightDangerBg = Color(0x1FDC2626)
val LightInfo = Color(0xFF0284C7)
val LightInfoBg = Color(0x1F0284C7)
val LightBorder = Color(0xFFCBD5E1)
val LightBorderStrong = Color(0xFF94A3B8)
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
