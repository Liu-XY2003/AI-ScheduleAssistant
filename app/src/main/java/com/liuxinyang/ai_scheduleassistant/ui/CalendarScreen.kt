package com.liuxinyang.ai_scheduleassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuxinyang.ai_scheduleassistant.data.Modules
import com.liuxinyang.ai_scheduleassistant.data.ScheduleEvent
import java.time.LocalDate
import java.time.YearMonth

private val WEEK_CN = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun CalendarScreen(
    month: YearMonth,
    selectedDay: LocalDate,
    events: List<ScheduleEvent>,
    memos: List<ScheduleEvent>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onDelete: (Int) -> Unit,
) {
    val byDay = events.filter { it.dateKey != null }.groupBy { it.dateKey!! }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ---- 月份导航
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPrevMonth) { Text("‹", fontSize = 20.sp) }
            Text(
                "${month.year} 年 ${month.monthValue} 月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            TextButton(onClick = onNextMonth) { Text("›", fontSize = 20.sp) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onToday) { Text("今天") }
        }

        // ---- 星期表头
        Row(Modifier.fillMaxWidth()) {
            WEEK_CN.forEach { w ->
                Text(
                    w, Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- 月历格子 (周一为一周起点)
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - 1 + 7) % 7      // Monday=0
        val gridStart = first.minusDays(lead.toLong())
        val today = LocalDate.now()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until 6) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (col in 0 until 7) {
                        val day = gridStart.plusDays((row * 7 + col).toLong())
                        DayCell(
                            day = day,
                            inMonth = day.month == month.month,
                            isToday = day == today,
                            isSelected = day == selectedDay,
                            events = byDay[day.toString()].orEmpty(),
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectDay(day) },
                        )
                    }
                }
            }
        }

        // ---- 当天详情
        val dayEvents = events.filter { it.dateKey == selectedDay.toString() }
            .sortedBy { it.start ?: "" }

        Text(
            "${selectedDay.monthValue} 月 ${selectedDay.dayOfMonth} 日 星期" +
                WEEK_CN[(selectedDay.dayOfWeek.value - 1 + 7) % 7],
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (dayEvents.isEmpty()) {
            Text("这天没有日程", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            dayEvents.forEach { EventRow(it, onDelete) }
        }

        // ---- 备忘
        if (memos.isNotEmpty()) {
            Spacer(Modifier.size(4.dp))
            Text("备忘（无时间）", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            memos.forEach { EventRow(it, onDelete) }
        }
        // 留够底部空间, 避免最后一条被底部导航栏盖住
        Spacer(Modifier.size(88.dp))
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    events: List<ScheduleEvent>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val border = when {
        isSelected -> Palette.Class
        isToday -> Palette.Class.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    Column(
        modifier
            .aspectRatio(0.78f)
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp),
    ) {
        Text(
            "${day.dayOfMonth}",
            fontSize = 11.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isToday -> Palette.Class
                inMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
        )
        events.take(3).forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    Modifier
                        .size(4.dp)
                        .background(Palette.of(e.module), RoundedCornerShape(1.dp))
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    e.title,
                    fontSize = 8.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (events.size > 3) {
            Text("+${events.size - 3}", fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EventRow(e: ScheduleEvent, onDelete: (Int) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .size(width = 4.dp, height = 38.dp)
                    .background(Palette.of(e.module), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                if (e.start != null) {
                    Text(
                        if (e.end != null) "${fmtTime(e.start)} – ${fmtTime(e.end)}"
                        else fmtTime(e.start),
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = Palette.Class,
                    )
                }
                Text(e.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                val meta = buildString {
                    append(Modules.cn(e.module))
                    if (e.location != null) append(" · 📍").append(e.location)
                    if (e.remind != null) append(" · 提前").append(e.remind).append("分")
                    if (e.priority == "high") append(" · 重要")
                    if (e.ambiguous) append(" · 时间有歧义")
                    if (e.notes != null) append(" · ").append(e.notes)
                }
                Text(meta, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onDelete(e.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", Modifier.size(18.dp))
            }
        }
    }
}
