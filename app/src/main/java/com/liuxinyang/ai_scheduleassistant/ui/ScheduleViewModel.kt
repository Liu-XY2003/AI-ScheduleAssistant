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
import com.liuxinyang.ai_scheduleassistant.data.UnauthorizedException
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

    /** 访问令牌 (后端设了 SCHED_TOKEN 时才需要)。 */
    var token by mutableStateOf(prefs.token)
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

    fun applyToken(t: String) {
        prefs.token = t
        token = prefs.token
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            health = Api.health(baseUrl, token).getOrNull()
            Api.events(baseUrl, token)
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
                .onFailure {
                    health = health?.copy(ready = false, error = authMessage(it))
                }
        }
    }

    /** 把 401 翻译成用户能照做的提示, 而不是一句 "HTTP 401"。 */
    private fun authMessage(e: Throwable): String =
        if (e is UnauthorizedException) "后端需要访问令牌，请在设置里填写"
        else (e.message ?: "未知错误")

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || busy) return
        viewModelScope.launch {
            busy = true
            lastResult = null
            Api.chat(baseUrl, token, t)
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
                    toast = "请求失败: ${authMessage(e)}"
                    health = health?.copy(ready = false, error = authMessage(e))
                }
            busy = false
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            Api.delete(baseUrl, token, id)
                .onSuccess { refresh() }
                .onFailure { toast = "删除失败: ${authMessage(it)}" }
        }
    }

    fun reset() {
        viewModelScope.launch {
            Api.reset(baseUrl, token)
                .onSuccess { refresh() }
                .onFailure { toast = "清空失败: ${authMessage(it)}" }
        }
    }

    fun eventsOn(day: LocalDate): List<ScheduleEvent> {
        val key = day.toString()
        return events.filter { it.dateKey == key }
            .sortedBy { it.start ?: "" }
    }

    fun memos(): List<ScheduleEvent> = events.filter { it.start == null }
}
