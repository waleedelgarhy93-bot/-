package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Academic Ledger Palette — Deep Emerald Teal, Amber Gold & Alabaster
val EmeraldPrimary = Color(0xFF0A5C50)
val EmeraldPrimaryDark = Color(0xFF074239)
val EmeraldOnPrimary = Color(0xFFFFFFFF)
val EmeraldPrimaryContainer = Color(0xFFC8EFE7)
val EmeraldOnPrimaryContainer = Color(0xFF03312A)

val AmberSecondary = Color(0xFFC97808)
val AmberOnSecondary = Color(0xFFFFFFFF)
val AmberSecondaryContainer = Color(0xFFFEF0D1)
val AmberOnSecondaryContainer = Color(0xFF452400)

val IndigoTertiary = Color(0xFF3E44B8)
val IndigoOnTertiary = Color(0xFFFFFFFF)
val IndigoTertiaryContainer = Color(0xFFE3E5FF)
val IndigoOnTertiaryContainer = Color(0xFF14186B)

val AlabasterBackground = Color(0xFFF6F4EE)
val AlabasterOnBackground = Color(0xFF191D1C)
val WarmSurface = Color(0xFFFFFFFF)
val WarmSurfaceVariant = Color(0xFFECE8DE)
val WarmOnSurfaceVariant = Color(0xFF4E5653)
val WarmOutline = Color(0xFFD7D2C6)
val WarmOutlineVariant = Color(0xFFE6E2D8)

val CrimsonError = Color(0xFFBF1E3C)
val CrimsonOnError = Color(0xFFFFFFFF)
val CrimsonErrorContainer = Color(0xFFFFE2E7)
val CrimsonOnErrorContainer = Color(0xFF4A0010)

// Dark mode counterparts
val DarkEmeraldPrimary = Color(0xFF5AD2BE)
val DarkEmeraldOnPrimary = Color(0xFF003730)
val DarkEmeraldContainer = Color(0xFF094B41)
val DarkEmeraldOnContainer = Color(0xFFB5F2E6)

val DarkAmberSecondary = Color(0xFFF6B44A)
val DarkAmberOnSecondary = Color(0xFF412600)
val DarkAmberContainer = Color(0xFF5E3900)
val DarkAmberOnContainer = Color(0xFFFFDFB0)

val DarkIndigoTertiary = Color(0xFFB7BEFF)
val DarkIndigoContainer = Color(0xFF282E8A)
val DarkIndigoOnContainer = Color(0xFFE3E5FF)

val DarkBackground = Color(0xFF101514)
val DarkOnBackground = Color(0xFFEAECE9)
val DarkSurface = Color(0xFF171F1D)
val DarkSurfaceVariant = Color(0xFF232E2B)
val DarkOnSurfaceVariant = Color(0xFFB5C0BC)
val DarkOutline = Color(0xFF3B4945)

// Semantic status tokens for attendance pills, payments, siblings, and suspension
object LedgerStatusColors {
    val PaidText = Color(0xFF146C38)
    val PaidContainer = Color(0xFFDCFCE7)
    val PaidBorder = Color(0xFF86EFAC)

    val UnpaidText = Color(0xFFB91C3B)
    val UnpaidContainer = Color(0xFFFFE4E8)
    val UnpaidBorder = Color(0xFFFDA4AF)

    val PresentFill = Color(0xFF15803D)
    val PresentBg = Color(0xFFDCFCE7)
    val PresentBorder = Color(0xFF4ADE80)

    val AbsentFill = Color(0xFFBE123C)
    val AbsentBg = Color(0xFFFFE4E6)
    val AbsentBorder = Color(0xFFFB7185)

    val ExcusedFill = Color(0xFFB45309)
    val ExcusedBg = Color(0xFFFEF3C7)
    val ExcusedBorder = Color(0xFFFBBF24)

    val UnmarkedFill = Color(0xFF64748B)
    val UnmarkedBg = Color(0xFFF1F5F9)
    val UnmarkedBorder = Color(0xFFCBD5E1)

    val SiblingText = Color(0xFF3730A3)
    val SiblingContainer = Color(0xFFE0E7FF)

    val SuspendedText = Color(0xFF92400E)
    val SuspendedContainer = Color(0xFFFEF3C7)
}
