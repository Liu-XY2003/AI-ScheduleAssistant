package com.liuxinyang.ai_scheduleassistant.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 访问自己电脑上的日程服务。
 *
 * 用 OkHttp + org.json: 只用最少的外部依赖, 避免和很新的 AGP/Compose 版本打架。
 * 所有网络调用都在 IO 线程, 抛出的异常统一转成 Result.failure。
 */
object Api {

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        // 推理可能耗时(尤其冷启动要编译 shader), 读超时给足
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun base(url: String) = url.trimEnd('/')

    private fun get(url: String): JSONObject {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: ${body.take(200)}")
            return JSONObject(body)
        }
    }

    private fun postJson(url: String, payload: JSONObject): JSONObject {
        val req = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON))
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: ${body.take(200)}")
            return JSONObject(body)
        }
    }

    // ---------------------------------------------------------------- 接口

    suspend fun health(baseUrl: String): Result<Health> = withContext(Dispatchers.IO) {
        runCatching {
            val j = get("${base(baseUrl)}/api/health")
            Health(
                ready = j.optBoolean("ready", false),
                backend = str(j, "backend"),
                events = j.optInt("events", 0),
                error = str(j, "error"),
            )
        }
    }

    suspend fun events(baseUrl: String): Result<List<ScheduleEvent>> = withContext(Dispatchers.IO) {
        runCatching {
            val arr = get("${base(baseUrl)}/api/events").optJSONArray("events") ?: JSONArray()
            (0 until arr.length()).map { i -> parseEvent(arr.getJSONObject(i)) }
        }
    }

    suspend fun chat(baseUrl: String, text: String): Result<ChatResult> = withContext(Dispatchers.IO) {
        runCatching {
            val j = postJson("${base(baseUrl)}/api/chat", JSONObject().put("text", text))
            parseChat(j)
        }
    }

    suspend fun delete(baseUrl: String, id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${base(baseUrl)}/api/events/$id")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            }
        }
    }

    suspend fun reset(baseUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postJson("${base(baseUrl)}/api/reset", JSONObject())
            Unit
        }
    }

    // ---------------------------------------------------------------- 解析

    /**
     * 安全取字符串。
     *
     * 坑: JSONObject.optString(key, fallback) 遇到 JSON 的 null 会返回**字符串 "null"**
     * (因为 JSONObject.NULL.toString() 就是 "null"), 而不是 fallback。
     * 结果就是界面上出现 "错误: null"、"📍 null" 这种鬼东西。
     * 必须先用 isNull() 判断。
     */
    private fun str(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        val s = o.optString(key, "").trim()
        return s.ifBlank { null }.takeIf { it != "null" }
    }

    private fun parseEvent(o: JSONObject) = ScheduleEvent(
        id = o.optInt("id"),
        module = o.optString("module"),
        title = o.optString("title"),
        start = str(o, "start"),
        end = str(o, "end"),
        durationMin = if (o.isNull("duration_min")) null else o.optInt("duration_min"),
        remind = if (o.isNull("remind")) null else o.optInt("remind"),
        location = str(o, "location"),
        priority = str(o, "priority"),
        notes = str(o, "notes"),
        ambiguous = o.optBoolean("ambiguous", false),
    )

    private fun parseChat(j: JSONObject): ChatResult {
        val exec = j.optJSONObject("execution")
        val items = mutableListOf<ExecutedItem>()
        exec?.optJSONArray("items")?.let { arr ->
            for (i in 0 until arr.length()) {
                val it = arr.getJSONObject(i)
                val reasons = mutableListOf<String>()
                it.optJSONArray("reasons")?.let { rs ->
                    for (k in 0 until rs.length()) reasons.add(rs.getString(k))
                }
                items.add(
                    ExecutedItem(
                        id = it.optInt("id"),
                        module = it.optString("module"),
                        title = it.optString("title"),
                        start = str(it, "start"),
                        ambiguous = it.optBoolean("ambiguous", false),
                        isPast = it.optBoolean("is_past", false),
                        reasons = reasons,
                    )
                )
            }
        }
        val schemaErrors = mutableListOf<String>()
        j.optJSONArray("schema_error")?.let { arr ->
            for (i in 0 until arr.length()) schemaErrors.add(arr.getString(i))
        }
        val env = j.optJSONObject("envelope")
        return ChatResult(
            input = j.optString("input"),
            modelRaw = j.optString("model_raw", ""),
            status = env?.let { str(it, "status") },
            execKind = exec?.let { str(it, "kind") },
            detail = exec?.let { str(it, "detail") },
            items = items,
            genMs = j.optInt("gen_ms", 0),
            totalMs = j.optInt("total_ms", 0),
            schemaErrors = schemaErrors,
            error = str(j, "error"),
        )
    }
}
