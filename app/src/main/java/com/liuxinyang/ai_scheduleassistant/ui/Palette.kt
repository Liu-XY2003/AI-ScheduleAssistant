package com.liuxinyang.ai_scheduleassistant.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.liuxinyang.ai_scheduleassistant.data.Modules
import com.liuxinyang.ai_scheduleassistant.ui.theme.LocalPalette

/**
 * 取当前主题的配色 (与网页版同源, 见 theme/tokens.json)。
 *
 * 成员都是 @Composable 的: 主题一变调用点自动重组, 不用把 palette 手动透传下去。
 * 数值本身由 theme/gen_theme.py 生成到 Tokens.kt, 这里只做取用。
 */
object Palette {
    val Class: Color @Composable get() = of("class")
    val Exam: Color @Composable get() = of("exam")
    val Meeting: Color @Composable get() = of("meeting")
    val Assignment: Color @Composable get() = of("assignment")
    val Alarm: Color @Composable get() = of("alarm")
    val Reminder: Color @Composable get() = of("reminder")
    val Memo: Color @Composable get() = of("memo")

    val All: List<Color> @Composable get() = LocalPalette.current.modules

    @Composable
    fun of(module: String): Color =
        LocalPalette.current.modules[Modules.colorIndex(module)]

    // 语义色 (随主题变化)
    val Ok: Color @Composable get() = LocalPalette.current.ok
    val Warn: Color @Composable get() = LocalPalette.current.warn
    val Danger: Color @Composable get() = LocalPalette.current.danger
    val Accent: Color @Composable get() = LocalPalette.current.accent
    val Muted: Color @Composable get() = LocalPalette.current.muted
    val Card2: Color @Composable get() = LocalPalette.current.card2
    val Line: Color @Composable get() = LocalPalette.current.line
}
