package com.liuxinyang.ai_scheduleassistant.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 服务端地址等设置。
 *
 * 路线B: 手机连自己电脑上的服务。默认给的是这台电脑的 Tailscale 地址
 * (见 tailscale status: node / zaocha22959@163.com / 100.110.179.70)。
 * 不在同一个 tailnet 时可以改成局域网 IP, 例如 http://192.168.1.20:8800。
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

    companion object {
        const val DEFAULT_URL = "http://100.110.179.70:8800"
        private const val KEY_URL = "base_url"
        private const val KEY_THEME = "theme"

        /** 常用候选, 方便在设置里一键切换 */
        val PRESETS = listOf(
            "http://100.110.179.70:8800" to "本机 (Tailscale)",
            "http://100.110.209.18:8800" to "训练服务器 (Tailscale, 9/30 到期)",
            "http://127.0.0.1:8800"      to "本机回环 (仅模拟器)",
        )
    }
}
