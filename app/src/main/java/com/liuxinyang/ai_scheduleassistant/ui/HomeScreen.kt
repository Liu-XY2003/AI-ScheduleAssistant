package com.liuxinyang.ai_scheduleassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuxinyang.ai_scheduleassistant.data.ChatResult
import com.liuxinyang.ai_scheduleassistant.data.Modules

private val EXAMPLES = listOf(
    "下下周二上午九点加一门计算机原理，在教三201",
    "加个明早八点的闹钟",
    "下个月3号上午九点项目评审",
    "今晚提醒我买牛奶",
    "周五之前要交操作系统实验报告",
    "今天天气怎么样",
)

@Composable
fun HomeScreen(
    busy: Boolean,
    result: ChatResult?,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("一句话加日程", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text("例如：下下周二上午九点加一门计算机原理，在教三201") },
            enabled = !busy,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onSend(text); text = "" },
                enabled = !busy && text.isNotBlank(),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("解析中")
                } else {
                    Text("添加")
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EXAMPLES.size) { i ->
                AssistChip(
                    onClick = { text = EXAMPLES[i] },
                    label = { Text(EXAMPLES[i], fontSize = 12.sp) },
                )
            }
        }

        if (result != null) ResultCard(result)

        // 留够底部空间, 避免结果卡片被底部导航栏盖住
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun ResultCard(r: ChatResult) {
    val (title, tone) = when {
        r.error != null                  -> "没能理解这句话" to Palette.Danger
        r.execKind == "executed"         -> "已添加" to Palette.Ok
        r.execKind == "need_clarify"     -> "需要补充信息" to Palette.Warn
        r.execKind == "rejected"         -> "这句话不是日程" to Palette.Warn
        r.execKind == "failed"           -> "执行失败" to Palette.Danger
        else                             -> "结果" to Palette.Memo
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(tone)
                Spacer(Modifier.size(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (r.totalMs > 0) {
                    Text("${r.totalMs}ms", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (r.detail != null && r.execKind != "executed") {
                Text(r.detail, style = MaterialTheme.typography.bodyMedium)
            }

            r.items.forEach { it ->
                Row(verticalAlignment = Alignment.Top) {
                    Dot(Palette.of(it.module))
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(it.title, fontWeight = FontWeight.Medium)
                        Text(
                            buildString {
                                append(Modules.cn(it.module))
                                if (it.start != null) append(" · ").append(fmt(it.start))
                                if (it.ambiguous) append(" · 时间有歧义")
                                if (it.isPast) append(" · 已过去")
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        it.reasons.forEach { rs ->
                            Text("· $rs", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (r.schemaErrors.isNotEmpty()) {
                Text("格式校验未通过：", fontSize = 12.sp, color = Palette.Danger)
                r.schemaErrors.forEach { Text("· $it", fontSize = 11.sp, color = Palette.Danger) }
            }

            Spacer(Modifier.height(2.dp))
            Text("模型输出", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(r.modelRaw.ifBlank { "(空)" }, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Dot(c: androidx.compose.ui.graphics.Color) {
    Spacer(
        Modifier
            .size(10.dp)
            .background(c, RoundedCornerShape(3.dp))
    )
}

/** "2026-10-06T09:00:00" -> "10-06 09:00" */
internal fun fmt(iso: String): String =
    if (iso.length >= 16) iso.substring(5, 10) + " " + iso.substring(11, 16) else iso

/** "2026-10-06T09:00:00" -> "09:00" */
internal fun fmtTime(iso: String?): String =
    if (iso != null && iso.length >= 16) iso.substring(11, 16) else ""
