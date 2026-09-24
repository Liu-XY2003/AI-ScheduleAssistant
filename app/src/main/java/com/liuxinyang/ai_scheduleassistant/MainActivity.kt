package com.liuxinyang.ai_scheduleassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuxinyang.ai_scheduleassistant.ui.CalendarScreen
import com.liuxinyang.ai_scheduleassistant.ui.HomeScreen
import com.liuxinyang.ai_scheduleassistant.ui.Palette
import com.liuxinyang.ai_scheduleassistant.ui.ScheduleViewModel
import com.liuxinyang.ai_scheduleassistant.ui.SettingsScreen
import com.liuxinyang.ai_scheduleassistant.ui.theme.ScheduleAssistantTheme
import com.liuxinyang.ai_scheduleassistant.ui.theme.ThemeChoice
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // VM 提到主题外层: 主题选择存在 VM 里, 换主题要立刻整棵树重组。
            val vm: ScheduleViewModel = viewModel()
            ScheduleAssistantTheme(choice = ThemeChoice.from(vm.theme)) {
                App(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(vm: ScheduleViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.toast) {
        vm.toast?.let {
            snackbar.showSnackbar(it)
            vm.toast = null
        }
    }

    // 切到日历/设置页时自动刷新。
    // 否则用户在电脑端(网页或别的设备)改了日程, 手机上看到的还是旧数据。
    LaunchedEffect(tab) {
        if (tab != 0) vm.refresh()
    }

    val connected = vm.health?.ready == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("我的日程", fontWeight = FontWeight.SemiBold) },
                actions = {
                    Text(
                        text = if (connected) "● 已连接" else "● 未连接",
                        color = if (connected) Palette.Ok else Palette.Danger,
                        fontSize = 12.sp,
                    )
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text("添加") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("日历") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("设置") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> HomeScreen(
                    busy = vm.busy,
                    result = vm.lastResult,
                    onSend = { vm.send(it) },
                )

                1 -> CalendarScreen(
                    month = vm.month,
                    selectedDay = vm.selectedDay,
                    events = vm.events,
                    memos = vm.memos(),
                    onPrevMonth = { vm.month = vm.month.minusMonths(1) },
                    onNextMonth = { vm.month = vm.month.plusMonths(1) },
                    onToday = {
                        val n = LocalDate.now()
                        vm.month = YearMonth.from(n)
                        vm.selectedDay = n
                    },
                    onSelectDay = { vm.selectedDay = it },
                    onDelete = { vm.delete(it) },
                )

                else -> SettingsScreen(
                    baseUrl = vm.baseUrl,
                    health = vm.health,
                    eventCount = vm.events.size,
                    theme = vm.theme,
                    onSave = { vm.applyBaseUrl(it) },
                    onTheme = { vm.applyTheme(it) },
                    onRefresh = { vm.refresh() },
                    onReset = { vm.reset() },
                )
            }
        }
    }
}
