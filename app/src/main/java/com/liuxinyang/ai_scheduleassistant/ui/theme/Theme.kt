package com.liuxinyang.ai_scheduleassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 主题选项。SYSTEM 跟随系统深浅色, 另外两个是固定外观。 */
enum class ThemeChoice(val key: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    PAPER("paper", "纸感"),
    NIGHT("night", "夜航");

    companion object {
        fun from(key: String?): ThemeChoice =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/**
 * 当前主题的配色。
 *
 * 用 CompositionLocal 而不是把 palette 一层层往下传 —— 屏幕内部的配色调用点
 * (Palette.Ok / Palette.of(module) ...) 因此可以保持原样, 切换主题时自动重组。
 */
val LocalPalette = staticCompositionLocalOf { Tokens.default }

@Composable
fun ScheduleAssistantTheme(
    choice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
) {
    val palette = when (choice) {
        ThemeChoice.PAPER -> Tokens.PAPER
        ThemeChoice.NIGHT -> Tokens.NIGHT
        ThemeChoice.SYSTEM -> if (isSystemInDarkTheme()) Tokens.NIGHT else Tokens.PAPER
    }

    val scheme = palette.toColorScheme()

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = palette.toTypography(),
            content = content,
        )
    }
}

/**
 * 把 token 映射成 Material3 配色。
 *
 * 刻意不启用 dynamicColor: 动态取色会把壁纸颜色混进来, 那样"纸感/夜航"两套
 * 设计就名存实亡了 —— 同一台设备换个壁纸 App 就变样。品牌色由 token 说了算。
 *
 * ★ 这里把 M3 的槽位**全部**显式赋值。只给 background/surface 是不够的:
 *   NavigationBar 的选中指示器用的是 secondaryContainer, 不覆盖就会退回
 *   Material 基线紫色 —— 实测底栏会出现一块突兀的淡紫药丸。
 */
private fun ThemePalette.toColorScheme(): ColorScheme {
    val tint = { c: Color -> c }   // 只是让下面的分组更易读
    return if (isDark) {
        darkColorScheme(
            primary = accent, onPrimary = onAccent,
            primaryContainer = chipBg, onPrimaryContainer = fg,
            inversePrimary = accent,
            secondary = accent, onSecondary = onAccent,
            secondaryContainer = chipBg, onSecondaryContainer = fg,
            tertiary = accent, onTertiary = onAccent,
            tertiaryContainer = chipBg, onTertiaryContainer = fg,
            background = bg, onBackground = fg,
            surface = card, onSurface = fg,
            surfaceVariant = card2, onSurfaceVariant = muted,
            surfaceTint = accent,
            surfaceContainerLowest = bg,
            surfaceContainerLow = card,
            surfaceContainer = card2,
            surfaceContainerHigh = card2,
            surfaceContainerHighest = chipBg,
            inverseSurface = fg, inverseOnSurface = bg,
            error = danger, onError = Color.White,
            errorContainer = danger.copy(alpha = 0.18f), onErrorContainer = danger,
            outline = line, outlineVariant = line,
            scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = accent, onPrimary = onAccent,
            primaryContainer = tint(chipBg), onPrimaryContainer = fg,
            inversePrimary = accent,
            secondary = accent, onSecondary = onAccent,
            secondaryContainer = chipBg, onSecondaryContainer = fg,
            tertiary = accent, onTertiary = onAccent,
            tertiaryContainer = chipBg, onTertiaryContainer = fg,
            background = bg, onBackground = fg,
            surface = card, onSurface = fg,
            surfaceVariant = card2, onSurfaceVariant = muted,
            surfaceTint = accent,
            surfaceContainerLowest = card,
            surfaceContainerLow = card,
            surfaceContainer = card2,
            surfaceContainerHigh = card2,
            surfaceContainerHighest = chipBg,
            inverseSurface = fg, inverseOnSurface = bg,
            error = danger, onError = Color.White,
            errorContainer = danger.copy(alpha = 0.12f), onErrorContainer = danger,
            outline = line, outlineVariant = line,
            scrim = Color.Black,
        )
    }
}

/** 纸感用衬线标题, 夜航保持无衬线; 时间一律等宽数字, 免得跳动。 */
private fun ThemePalette.toTypography(): Typography {
    val titleFamily = if (key == "paper") FontFamily.Serif else FontFamily.Default
    val base = Typography()
    return base.copy(
        headlineSmall = base.headlineSmall.copy(fontFamily = titleFamily),
        titleLarge = base.titleLarge.copy(fontFamily = titleFamily),
        titleMedium = base.titleMedium.copy(fontFamily = titleFamily),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    )
}
