package com.liuxinyang.ai_scheduleassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuxinyang.ai_scheduleassistant.data.Health
import com.liuxinyang.ai_scheduleassistant.data.Prefs
import com.liuxinyang.ai_scheduleassistant.ui.theme.ThemeChoice
import com.liuxinyang.ai_scheduleassistant.ui.theme.Tokens

@Composable
fun SettingsScreen(
    baseUrl: String,
    health: Health?,
    eventCount: Int,
    theme: String,
    onSave: (String) -> Unit,
    onTheme: (String) -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
) {
    var editing by remember(baseUrl) { mutableStateOf(baseUrl) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("外观", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text(
            "纸感是暖米白 + 衬线标题，白天看课表舒服；夜航是深色高对比，晚上不刺眼。" +
                "两套配色与网页版共用同一份 token。",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChoice.entries.forEach { choice ->
                ThemeSwatch(
                    choice = choice,
                    selected = theme == choice.key,
                    onClick = { onTheme(choice.key) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("服务端地址", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Text(
            "手机通过这个地址访问你电脑上的日程服务。\n" +
                "不在同一个 Tailscale 网络时，可以改成局域网 IP，例如 http://192.168.1.20:8800",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = editing,
            onValueChange = { editing = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Base URL") },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(editing) }, enabled = editing.isNotBlank()) {
                Text("保存并重连")
            }
            TextButton(onClick = onRefresh) { Text("重新检测") }
        }

        Text("常用地址", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Prefs.PRESETS.forEach { (url, label) ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                AssistChip(onClick = { editing = url }, label = { Text(url, fontSize = 11.sp) })
                Spacer(Modifier.padding(start = 6.dp))
                Text(label, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(4.dp))
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("连接状态", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                val ok = health?.ready == true
                Text(
                    if (ok) "● 已连接" else "● 未连接",
                    fontSize = 13.sp,
                    color = if (ok) Palette.Ok else Palette.Danger,
                )
                health?.backend?.let { Text("后端: $it", fontSize = 12.sp) }
                health?.error?.let { Text("错误: $it", fontSize = 12.sp, color = Palette.Danger) }
                Text("日程条数: $eventCount", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onReset) {
            Text("清空全部日程", color = Palette.Danger)
        }
        Text(
            "推理跑在你自己的电脑上（Qwen3-0.6B + LoRA，llama.cpp 约束解码）。" +
                "手机只负责输入和展示，所有时间解析都在电脑端完成。",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // 留够底部空间: enableEdgeToEdge + Scaffold 的 bottomBar 会盖住最后一段内容
        Spacer(Modifier.height(88.dp))
    }
}

/**
 * 主题预览块: 直接画出该主题的底色、边框和模块色, 点一下即切换。
 * 比只写文字选项直观 —— 不用切过去再切回来比较。
 */
@Composable
private fun ThemeSwatch(
    choice: ThemeChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // "跟随系统"没有固定配色, 预览就跟着系统的深浅走
    val p = when (choice) {
        ThemeChoice.PAPER -> Tokens.PAPER
        ThemeChoice.NIGHT -> Tokens.NIGHT
        ThemeChoice.SYSTEM -> if (isSystemInDarkTheme()) Tokens.NIGHT else Tokens.PAPER
    }
    val ring = if (selected) Palette.Accent else MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier
            .clip(shape)
            .background(p.bg)
            .clickable(onClick = onClick)
            .border(if (selected) 2.dp else 1.dp, ring, shape)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            p.modules.take(4).forEach { c ->
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(c)
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(p.card)
                .border(1.dp, p.line, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                choice.label,
                fontSize = 11.sp,
                color = p.fg,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
