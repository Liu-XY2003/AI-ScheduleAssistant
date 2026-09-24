package com.liuxinyang.ai_scheduleassistant.data

/**
 * 与服务端契约对应的数据模型。
 *
 * 服务端: 自己电脑上的 FastAPI (路线B: 手机 -> 自己的电脑 -> llama.cpp)
 * 契约:   schedule_command.v2.json (唯一事实源)
 *
 * 注意: 这里的字段只是"服务端已经解析完的结果", 手机端不做任何时间推导 ——
 * 绝对时间完全由服务端的 resolve_time() 决定, 保证手机和网页看到的一致。
 */

/** 一条日程 */
data class ScheduleEvent(
    val id: Int,
    val module: String,
    val title: String,
    val start: String?,        // ISO8601 本地时间, 可能为 null (备忘)
    val end: String?,          // 由 start + duration 推出
    val durationMin: Int?,
    val remind: Int?,
    val location: String?,
    val priority: String?,
    val notes: String?,
    val ambiguous: Boolean,    // 时间有歧义 (例如用户只说"九点")
) {
    val dateKey: String? get() = start?.take(10)          // yyyy-MM-dd
    val timeText: String get() = start?.drop(11)?.take(5) ?: ""
    val endTimeText: String? get() = end?.drop(11)?.take(5)
}

/** 一次执行里写入的条目 */
data class ExecutedItem(
    val id: Int,
    val module: String,
    val title: String,
    val start: String?,
    val ambiguous: Boolean,
    val isPast: Boolean,
    val reasons: List<String>,
)

/** /api/chat 的返回 */
data class ChatResult(
    val input: String,
    val modelRaw: String,
    val status: String?,            // ok / clarify / reject
    val execKind: String?,          // executed / need_clarify / rejected / failed
    val detail: String?,
    val items: List<ExecutedItem>,
    val genMs: Int,
    val totalMs: Int,
    val schemaErrors: List<String>,
    val error: String?,
) {
    val ok: Boolean get() = error == null && execKind != null
}

/** /api/health 的返回 */
data class Health(
    val ready: Boolean,
    val backend: String?,
    val events: Int,
    val error: String?,
    /** 后端设了令牌, 而本机没填或填错了。此时 events 无意义。 */
    val authRequired: Boolean = false,
)

/** 日程类型的中文名与配色分组 */
object Modules {
    private val cn = mapOf(
        "class" to "课程", "exam" to "考试", "meeting" to "会议",
        "assignment" to "作业", "alarm" to "闹钟",
        "reminder" to "提醒", "memo" to "备忘",
    )

    fun cn(module: String): String = cn[module] ?: module

    /** 用于 UI 上色, 取值 0..6 */
    fun colorIndex(module: String): Int = when (module) {
        "class" -> 0
        "exam" -> 1
        "meeting" -> 2
        "assignment" -> 3
        "alarm" -> 4
        "reminder" -> 5
        else -> 6
    }
}
