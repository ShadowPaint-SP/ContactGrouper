package de.drvlabs.contactgrouper.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.pow

const val MinimumReadableContrastRatio = 4.5

fun visibleGroupColor(groupColor: Color, backdrop: Color): Color {
    return groupColor.compositeOver(backdrop).copy(alpha = 1f)
}

fun readableContentColorFor(background: Color): Color {
    val blackContrast = contrastRatio(Color.Black, background)
    val whiteContrast = contrastRatio(Color.White, background)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

fun contrastRatio(foreground: Color, background: Color): Double {
    val foregroundLuminance = relativeLuminance(foreground)
    val backgroundLuminance = relativeLuminance(background)
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05) / (darker + 0.05)
}

fun relativeLuminance(color: Color): Double {
    val red = color.red.toLinearSrgb()
    val green = color.green.toLinearSrgb()
    val blue = color.blue.toLinearSrgb()
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

private fun Float.toLinearSrgb(): Double {
    val channel = coerceIn(0f, 1f).toDouble()
    return if (channel <= 0.04045) {
        channel / 12.92
    } else {
        ((channel + 0.055) / 1.055).pow(2.4)
    }
}
