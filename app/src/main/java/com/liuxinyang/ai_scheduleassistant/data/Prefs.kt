package com.liuxinyang.ai_scheduleassistant.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 服务端地址等设置。
 *
 * 路线 B: 手机连自己电脑上的日程服务。
 * 首次使用请在「设置」里填**你自己电脑**的地址, 三选一:
 *
 *   · Tailscale 地址 `http://100.x.y.z:8800` —— 推荐, 端到端加密且不挑网络
 *   · 同一 WiFi 下的局域网地址 `http://192.168.x.y:8800`
 *   · USB 端口转发时的 `http://127.0.0.1:8800`（先跑 `adb reverse tcp:8800 tcp:8800`）
 *
 * 默认值给的是回环地址 —— 它只在上面第三种情况下能用, 但至少不会误导你
 * 去连一个别人的机器。
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("sched", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(v) = sp.edit().putString(KEY_URL, v.trim().trimEnd('/')).apply()

    /** 外观选择: system / paper / night。存本机, 不上传。 */
    var theme: String
        get() = sp.getString(KEY_THEME, "system") ?: "system"
        set(v) = sp.edit().putString(KEY_THEME, v).apply()

    /** 访问令牌。后端设了 SCHED_TOKEN 时才需要填。 */
    var token: String
        get() = sp.getString(KEY_TOKEN, "") ?: ""
        set(v) = sp.edit().putString(KEY_TOKEN, v.trim()).apply()

    companion object {
        private const val KEY_URL = "base_url"
        private const val KEY_THEME = "theme"
        private const val KEY_TOKEN = "token"

        const val DEFAULT_URL = "http://127.0.0.1:8800"

        /**
         * 设置页里的一键填入候选。
         * 中间两条是**示例**, 请替换成你电脑的实际地址。
         */
        val PRESETS = listOf(
            "http://127.0.0.1:8800"     to "本机回环（需 adb reverse）",
            "http://192.168.1.20:8800"  to "同一 WiFi（改成你电脑的 IP）",
            "http://100.100.100.100:8800" to "Tailscale（改成你的地址）",
        )
    }
}
