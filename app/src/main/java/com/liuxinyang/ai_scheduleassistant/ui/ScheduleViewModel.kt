package com.liuxinyang.ai_scheduleassistant.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liuxinyang.ai_scheduleassistant.data.Api
import com.liuxinyang.ai_scheduleassistant.data.ChatResult
import com.liuxinyang.ai_scheduleassistant.data.Health
import com.liuxinyang.ai_scheduleassistant.data.Prefs
import com.liuxinyang.ai_scheduleassistant.data.ScheduleEvent
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    var baseUrl by mutableStateOf(prefs.baseUrl)
        private set

    /** 外观: system / paper / night。改动立刻生效并落盘。 */
    var theme by mutableStateOf(prefs.theme)
        private set

    var health by mutableStateOf<Health?>(null)
        private set
    var events by mutableStateOf<List<ScheduleEvent>>(emptyList())
        private set

    var busy by mutableStateOf(false)
        private set
    var lastResult by mutableStateOf<ChatResult?>(null)
        private set
    var toast by mutableStateOf<String?>(null)

    // 日历浏览状态
    var month by mutableStateOf(YearMonth.now())
    var selectedDay by mutableStateOf(LocalDate.now())

    init {
        refresh()
    }

    fun applyBaseUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        if (cleaned.isBlank()) return
        prefs.baseUrl = cleaned
        baseUrl = prefs.baseUrl
        refresh()
    }

    fun applyTheme(key: String) {
        prefs.theme = key
        theme = prefs.theme
    }

    fun refresh() {
        viewModelScope.launch {
            health = Api.health(baseUrl).getOrNull()
            Api.events(baseUrl)
                .onSuccess { list ->
                    events = list
                    // 有日程时把日历跳到最近一条, 方便一眼看到
                    list.mapNotNull { it.start }.minOrNull()?.let {
                        runCatching {
                            val d = LocalDate.parse(it.take(10))
                            month = YearMonth.from(d)
                            selectedDay = d
                        }
                    }
                }
                .onFailure { health = health?.copy(ready = false, error = it.message) }
        }
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || busy) return
        viewModelScope.launch {
            busy = true
            lastResult = null
            Api.chat(baseUrl, t)
                .onSuccess { r ->
                    lastResult = r
                    // 写入成功后跳到目标日期并刷新列表
                    r.items.firstOrNull()?.start?.let { s ->
                        runCatching {
                            val d = LocalDate.parse(s.take(10))
                            month = YearMonth.from(d)
                            selectedDay = d
                        }
                    }
                    refresh()
                }
                .onFailure { e ->
                    toast = "请求失败: ${e.message}"
                    health = health?.copy(ready = false, error = e.message)
                }
            busy = false
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            Api.delete(baseUrl, id)
                .onSuccess { refresh() }
                .onFailure { toast = "删除失败: ${it.message}" }
        }
    }

    fun reset() {
        viewModelScope.launch {
            Api.reset(baseUrl)
                .onSuccess { refresh() }
                .onFailure { toast = "清空失败: ${it.message}" }
        }
    }

    fun eventsOn(day: LocalDate): List<ScheduleEvent> {
        val key = day.toString()
        return events.filter { it.dateKey == key }
            .sortedBy { it.start ?: "" }
    }

    fun memos(): List<ScheduleEvent> = events.filter { it.start == null }
}
