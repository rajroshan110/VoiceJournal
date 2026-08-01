package dev.voicejournal.ui.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * VoiceJournal 4dp Grid Spacing Tokens
 * Single source of truth for all paddings, margins, and gaps.
 */
object Spacing {
    /** 2.dp — Micro spacing between tight inline icon & text elements */
    val SpaceX3s: Dp = 2.dp

    /** 4.dp — xs: Minimal chip inner vertical padding, tight gaps */
    val SpaceX2s: Dp = 4.dp

    /** 8.dp — sm: Standard gap between chips, internal card elements */
    val SpaceXs: Dp = 8.dp

    /** 12.dp — md: Medium card padding, list item gaps */
    val SpaceMd: Dp = 12.dp

    /** 16.dp — lg: Standard screen horizontal padding, card content padding */
    val SpaceLg: Dp = 16.dp

    /** 24.dp — xl: Section spacing, drawer padding */
    val SpaceXl: Dp = 24.dp

    /** 32.dp — 2xl: Hero header spacing, empty state top spacing */
    val Space2Xl: Dp = 32.dp

    /** 48.dp — 3xl: Large section gaps, bottom bar offsets */
    val Space3Xl: Dp = 48.dp
}
