# AI 日程助手 · 安卓客户端

一句话加日程。**推理跑在你自己的电脑上，手机只做输入和展示。**

---

## 架构（路线 B）

```
┌─────────────────┐   HTTP    ┌──────────────────────────────┐
│  安卓 App        │ ────────► │  你的电脑 (Ryzen AI 9 365)    │
│  Compose UI     │           │  ├ FastAPI  :8800            │
│  只做输入/展示    │           │  └ llama-server :18080       │
└─────────────────┘           │     Qwen3-0.6B + LoRA        │
   APK 仅 18 MB                │     + json_schema 约束解码    │
                              └──────────────────────────────┘
```

**为什么不在手机端跑模型**：手机端侧推理预计要 2–4 秒，而连自己电脑是 **850ms**；
而且端侧要把 378MB 模型塞进 APK、还要 NDK 交叉编译。手机和电脑本来就在同一个
Tailscale 网络里，直接连是最省事的。

**所有时间解析都在电脑端完成** —— 手机不做任何日期推导，
所以手机和网页版看到的绝对时间必然一致。

---

## 环境要求

| 项 | 值 |
|---|---|
| JDK | **17**（AGP 9.3 要求；本机 `JAVA_HOME` 默认指向 Zulu 8，构建时必须覆盖） |
| Android SDK | `D:\sdk`（platform android-37.0 / build-tools 36.1.0） |
| Gradle | 9.5.0（wrapper 已配好） |
| minSdk / targetSdk | 26 / 37 |

---

## 构建

```powershell
cd D:\Projects\AI_ScheduleAssistant
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path = "C:\Program Files\Java\jdk-17\bin;" + $env:Path
.\gradlew.bat assembleDebug
```

产物：`app\build\outputs\apk\debug\app-debug.apk`（约 18 MB）

## 安装到手机

手机打开「开发者选项 → USB 调试」，USB 连接后：

```powershell
& 'D:\sdk\platform-tools\adb.exe' install -r `
  'D:\Projects\AI_ScheduleAssistant\app\build\outputs\apk\debug\app-debug.apk'
```

或者直接把 `app-debug.apk` 传到手机上点击安装。

> 模拟器用法相同；但本机 SDK **还没装 system-image**，要用模拟器得先在
> Android Studio 里下载一个（约 1GB）。

## 连接电脑端服务

**前提**：电脑上先跑起来

```powershell
D:\Projects\qwen-local-tool-agent\local\run_local.ps1
```

然后 App 里进「设置」填服务地址：

| 手机在哪 | 填什么 |
|---|---|
| 同一个 Tailscale 网络 | `http://100.110.179.70:8800` ← **默认值** |
| 同一个 WiFi / 热点 | `http://10.38.252.36:8800` |
| USB 端口转发（`adb reverse tcp:8800 tcp:8800`） | `http://127.0.0.1:8800` |

点「保存并重连」，标题栏右上角出现 **● 已连接** 就通了。

---

## 界面

三个 Tab：

| Tab | 内容 |
|---|---|
| **添加** | 输入框 + 常用示例（点一下填入）+ 结果卡片 |
| **日历** | 月视图（按类型着色）、点日期看当天详情、左滑删除、下方备忘区 |
| **设置** | 外观（主题预览卡）、服务地址、连接状态、清空日程 |

### 两套主题

| 纸感 (Paper) | 夜航 (Night Ops) |
|---|---|
| ![纸感](docs/screenshots/android-paper-home.png) | ![夜航](docs/screenshots/android-night-calendar.png) |

- **纸感**：暖米白 `#FAF8F5` + 衬线标题 + 淡彩事件块，白天看课表舒服
- **夜航**：近黑 `#0E1116` + 无衬线 + 左侧 3px 色条，信息密度高

设置页有三块**实时预览**卡片（直接画出该主题的底色/边框/模块色），点一下即切换，
可选「跟随系统」。**显式选择会覆盖系统深浅色设置。**

配色不是手写的：`ui/theme/Tokens.kt` 由**后端仓库**的 `theme/gen_theme.py`
从 `theme/tokens.json` 生成。网页版和这个 App 共用同一份 token，所以两边颜色永远一致。
要改配色请改 `tokens.json` 然后重跑生成器，**不要直接编辑 `Tokens.kt` 或 `Palette.kt`**。

结果卡片会把**整条链路**摊开给你看：

```
● 已添加                                          1043ms
  ▎ 计算机原理
    课程 · 10-06 09:00
  ▎ 闹钟
    闹钟 · 09-26 08:00 · 时间有歧义
    · 用户只说「08:00」未说上午下午, 按作息规则消歧

  模型输出
  {"status":"ok","commands":[...]}
```

> 截图放 `docs/screenshots/`。它们是从模拟器 `adb shell screencap` 抓的真机画面。

---

## 代码结构

```
app/src/main/java/com/liuxinyang/ai_scheduleassistant/
├─ MainActivity.kt            Scaffold + 三个 Tab
├─ data/
│  ├─ Models.kt               数据模型 + 日程类型中文名/配色
│  ├─ Api.kt                  OkHttp 客户端 (health/chat/events/delete/reset)
│  └─ Prefs.kt                服务地址持久化 (SharedPreferences)
└─ ui/
   ├─ ScheduleViewModel.kt    状态管理 (StateFlow + 协程)
   ├─ HomeScreen.kt           输入 + 结果卡片
   ├─ CalendarScreen.kt       月历 + 当天详情 + 备忘
   ├─ SettingsScreen.kt       服务地址配置
   ├─ Palette.kt              配色
   └─ theme/                  主题 (脚手架自带)
```

**依赖很少**：只加了 OkHttp 和 lifecycle-viewmodel-compose。
JSON 用 Android 内置的 `org.json`，避免和很新的 AGP/Compose 版本打架。

---

## 模拟器调试环境（已配好）

```powershell
# 1. 装系统镜像（已装好: android-36 google_apis x86_64）
D:\sdk\cmdline-tools\latest\bin\android.exe --sdk=D:\sdk sdk install "system-images;android-36;google_apis;x86_64"

# 2. 建 AVD（已建好: 名字 sched, Pixel 6）
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
'no' | & 'D:\sdk\cmdline-tools\latest\bin\avdmanager.bat' create avd `
    -n sched -k "system-images;android-36;google_apis;x86_64" -d "pixel_6" --force

# 3. 启动模拟器（脚本会等开机完成）
D:\Projects\qwen-local-tool-agent\local\start_emulator.ps1

# 4. 装 APK + 启动 + 截图
D:\Projects\qwen-local-tool-agent\local\install_and_launch.ps1
```

> **坑**: 新版 Android CLI 的 `android emulator create` 不接受指定镜像，
> 会去下载另一个 1.8GB 的 playstore 版本（而且很慢）。用 `avdmanager` 才能指定已装好的镜像。
>
> **坑**: `adb exec-out screencap -p > file.png` 在 PowerShell 里会**损坏二进制流**。
> 正确做法是 `adb shell screencap -p /sdcard/x.png` 然后 `adb pull`。

## 调试记录（模拟器上发现并修掉的 3 个 bug）

| # | 现象 | 根因 | 修法 |
|---|---|---|---|
| 1 | 界面上出现 `错误: null`、`📍 null · null` | `JSONObject.optString(key, fallback)` 遇到 JSON `null` 会返回**字符串 `"null"`**（因为 `JSONObject.NULL.toString()` 就是 "null"），而不是 fallback | 加 `str()` 辅助函数，先 `isNull()` 判断再用 |
| 2 | 设置页底部"清空全部日程"被导航栏遮住 | `enableEdgeToEdge` + Scaffold 的 bottomBar 会盖住 scroll 内容末尾 | 三个页面底部各加 88dp 留白 |
| 3 | 服务端 10 条日程，App 只显示 1 条 | App 只在启动时刷新，切 Tab 不刷新 | 切到日历/设置页时自动刷新 + 顶栏加刷新按钮 |

## 已知限制

| 限制 | 说明 |
|---|---|
| 只能**添加** | `action` 目前只有 `add`（模型训练范围所限）。改/删只能在日历里删 |
| 电脑必须开机 | 路线B 的固有前提 |
| 明文 HTTP | Tailscale 本身端到端加密，所以放开了 `usesCleartextTraffic` |
| 结果卡片不持久 | 旋转屏幕 Tab 保留（`rememberSaveable`），但进程被杀后结果卡片会丢 |
| 无提醒推送 | 日程存在电脑端，手机不会主动提醒 |
| 日历不显示跨天事件 | 只按开始日期归到某一天 |

---

## 验收标准（改代码后可自查）

在电脑上跑本地服务，然后：

```powershell
cd D:\Projects\qwen-local-tool-agent\bridge
$env:SCHED_API='http://127.0.0.1:8800'
python acceptance_v2.py       # 应输出 总通过 21/21
```

App 里的表现应当与 `http://127.0.0.1:8800` 的网页版**完全一致**（同一个后端）。
