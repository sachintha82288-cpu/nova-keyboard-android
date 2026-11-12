package com.nova.keyboard.theme

/** Ten visually distinct built-in themes (not one palette with ten names). */
object BuiltInThemes {

    private fun c(v: Long): Int = v.toInt()

    val amoledCyber = NovaTheme(
        id = "amoled_cyber", name = "AMOLED Cyber",
        background = c(0xFF000000), keyBackground = c(0xFF0C0F14), keyPressed = c(0xFF1B2430),
        keyBorder = c(0xFF16202B), textColor = c(0xFFE9FBFF), accent = c(0xFF3DE1FF),
        specialKey = c(0xFF11161D), specialText = c(0xFF8FD8EA), suggestionBar = c(0xFF05070A),
        suggestionText = c(0xFFB9E9F5), radiusDp = 12, borderWidthDp = 1, glow = true, builtIn = true
    )

    val neonFusion = NovaTheme(
        id = "neon_fusion", name = "Neon Fusion",
        background = c(0xFF0B0420), keyBackground = c(0xFF1A0B3C), keyPressed = c(0xFF3A1078),
        keyBorder = c(0xFF6A2BD9), textColor = c(0xFFF6E8FF), accent = c(0xFFFF3DCB),
        specialKey = c(0xFF2A0F52), specialText = c(0xFFFFA8EC), suggestionBar = c(0xFF12062B),
        suggestionText = c(0xFFE4B6FF), radiusDp = 20, borderWidthDp = 2, glow = true, builtIn = true
    )

    val liquidGlass = NovaTheme(
        id = "liquid_glass", name = "Liquid Glass",
        background = c(0xFF0E1A26), keyBackground = c(0x33FFFFFF), keyPressed = c(0x55A8D8FF),
        keyBorder = c(0x66FFFFFF), textColor = c(0xFFF2F8FF), accent = c(0xFF7FC8FF),
        specialKey = c(0x22FFFFFF), specialText = c(0xFFCFE6FF), suggestionBar = c(0x22000000),
        suggestionText = c(0xFFE6F3FF), radiusDp = 24, borderWidthDp = 1, keyAlpha = 0.35f,
        shadow = true, builtIn = true
    )

    val stealthBlack = NovaTheme(
        id = "stealth_black", name = "Stealth Black",
        background = c(0xFF0A0A0A), keyBackground = c(0xFF171717), keyPressed = c(0xFF2A2A2A),
        keyBorder = c(0xFF232323), textColor = c(0xFFEDEDED), accent = c(0xFF9AA0A6),
        specialKey = c(0xFF121212), specialText = c(0xFFB0B0B0), suggestionBar = c(0xFF080808),
        suggestionText = c(0xFFCFCFCF), radiusDp = 6, borderWidthDp = 1, builtIn = true
    )

    val midnightPurple = NovaTheme(
        id = "midnight_purple", name = "Midnight Purple",
        background = c(0xFF140B24), keyBackground = c(0xFF241640), keyPressed = c(0xFF3B2568),
        keyBorder = c(0xFF3A2A5C), textColor = c(0xFFEDE4FF), accent = c(0xFFB14BFF),
        specialKey = c(0xFF1C1130), specialText = c(0xFFC9A6FF), suggestionBar = c(0xFF180D2B),
        suggestionText = c(0xFFD8C2FF), radiusDp = 16, builtIn = true
    )

    val arcticBlue = NovaTheme(
        id = "arctic_blue", name = "Arctic Blue",
        background = c(0xFFEAF2FA), keyBackground = c(0xFFFFFFFF), keyPressed = c(0xFFD6E7F8),
        keyBorder = c(0xFFC3D8EE), textColor = c(0xFF0E2233), accent = c(0xFF1B7FD4),
        specialKey = c(0xFFDCE9F7), specialText = c(0xFF2A4A66), suggestionBar = c(0xFFF3F8FD),
        suggestionText = c(0xFF274A63), radiusDp = 14, shadow = true, builtIn = true
    )

    val crimsonDark = NovaTheme(
        id = "crimson_dark", name = "Crimson Dark",
        background = c(0xFF120607), keyBackground = c(0xFF221012), keyPressed = c(0xFF3E1A1E),
        keyBorder = c(0xFF3A1C20), textColor = c(0xFFFFECEC), accent = c(0xFFFF4D5E),
        specialKey = c(0xFF1A0A0C), specialText = c(0xFFFFB3BA), suggestionBar = c(0xFF0D0405),
        suggestionText = c(0xFFFFC9CE), radiusDp = 10, glow = true, builtIn = true
    )

    val minimalWhite = NovaTheme(
        id = "minimal_white", name = "Minimal White",
        background = c(0xFFFFFFFF), keyBackground = c(0xFFF4F4F4), keyPressed = c(0xFFE2E2E2),
        keyBorder = c(0xFFE0E0E0), textColor = c(0xFF111111), accent = c(0xFF111111),
        specialKey = c(0xFFEAEAEA), specialText = c(0xFF555555), suggestionBar = c(0xFFFAFAFA),
        suggestionText = c(0xFF333333), radiusDp = 8, builtIn = true
    )

    val developerTerminal = NovaTheme(
        id = "developer_terminal", name = "Developer Terminal",
        background = c(0xFF04120A), keyBackground = c(0xFF0A2314), keyPressed = c(0xFF12351F),
        keyBorder = c(0xFF1B4A2C), textColor = c(0xFF9CFFB8), accent = c(0xFF35FF7A),
        specialKey = c(0xFF071B10), specialText = c(0xFF66E58F), suggestionBar = c(0xFF03100A),
        suggestionText = c(0xFF7BF2A2), radiusDp = 4, borderWidthDp = 1, font = "mono", builtIn = true
    )

    val gamingRgb = NovaTheme(
        id = "gaming_rgb", name = "Gaming RGB",
        background = c(0xFF050510), keyBackground = c(0xFF12122A), keyPressed = c(0xFF23234A),
        keyBorder = c(0xFFFF2D6F), textColor = c(0xFFFFFFFF), accent = c(0xFFFF2D6F),
        specialKey = c(0xFF0C0C1C), specialText = c(0xFF3DFF7A), suggestionBar = c(0xFF07070F),
        suggestionText = c(0xFF7BE8FF), radiusDp = 10, borderWidthDp = 2, glow = true, builtIn = true
    )

    val all = listOf(
        amoledCyber, neonFusion, liquidGlass, stealthBlack, midnightPurple,
        arcticBlue, crimsonDark, minimalWhite, developerTerminal, gamingRgb
    )
}
