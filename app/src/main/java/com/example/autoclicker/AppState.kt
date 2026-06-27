package com.example.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppState {
    var isServiceEnabled by mutableStateOf(false)
        private set
    var hasOverlayPermission by mutableStateOf(false)
        private set
    var isOverlayActive by mutableStateOf(false)
    var isMultiTarget by mutableStateOf(false)
    var targets by mutableStateOf(listOf(ClickTarget(500f, 500f)))
    var intervalValue by mutableLongStateOf(1000L)
    var intervalUnit by mutableStateOf(TimeUnit.MILLISECONDS)
    var stopCondition by mutableStateOf(StopCondition.INDEFINITE)
    var stopValue by mutableLongStateOf(10L)
    var stopUnit by mutableStateOf(TimeUnit.SECONDS)
    var isClicking by mutableStateOf(false)
    var dismissedA11yDialog by mutableStateOf(false)
    var clickX by mutableStateOf("500")
    var clickY by mutableStateOf("500")
    var isDarkTheme by mutableStateOf(true)

    fun initTheme(context: Context) {
        val prefs = context.getSharedPreferences("autoclicker", Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean("dark_theme", true)
        Lang.setLang(prefs.getString("lang", "tr") ?: "tr")
    }

    fun toggleTheme(context: Context) {
        isDarkTheme = !isDarkTheme
        context.getSharedPreferences("autoclicker", Context.MODE_PRIVATE)
            .edit().putBoolean("dark_theme", isDarkTheme).apply()
        (context as? Activity)?.recreate()
    }

    fun toggleLang(context: Context) {
        Lang.toggle()
        context.getSharedPreferences("autoclicker", Context.MODE_PRIVATE)
            .edit().putString("lang", Lang.current).apply()
        (context as? Activity)?.recreate()
    }

    fun checkServiceState(context: Context) {
        isServiceEnabled = if (AutoClickerAccessibilityService.instance != null) true
        else try {
            val services = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            services?.contains("AutoClickerAccessibilityService") == true
        } catch (_: Exception) { false }
    }

    fun checkOverlayState(context: Context) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    fun addTarget() {
        val list = targets.toMutableList()
        val last = list.lastOrNull() ?: ClickTarget(500f, 500f)
        list.add(ClickTarget(last.x + 50f, last.y + 50f))
        targets = list
        AutoClickerAccessibilityService.targets = list
    }

    fun removeLastTarget() {
        if (targets.size <= 1) return
        val list = targets.dropLast(1)
        targets = list
        AutoClickerAccessibilityService.targets = list.toMutableList()
    }

    fun updateIntervalValue(value: Long) {
        intervalValue = value
        AutoClickerAccessibilityService.clickIntervalMs = toMs(value, intervalUnit)
    }

    fun updateIntervalUnit(unit: TimeUnit) {
        intervalUnit = unit
        AutoClickerAccessibilityService.clickIntervalMs = toMs(intervalValue, unit)
    }

    fun updateStopCondition(condition: StopCondition) {
        stopCondition = condition
        AutoClickerAccessibilityService.stopCondition = condition
    }

    fun updateStopValue(value: Long) {
        stopValue = value
        AutoClickerAccessibilityService.stopValue = value
    }

    fun updateStopUnit(unit: TimeUnit) {
        stopUnit = unit
        AutoClickerAccessibilityService.stopUnit = unit
    }

    private fun toMs(value: Long, unit: TimeUnit): Long = when (unit) {
        TimeUnit.MILLISECONDS -> value
        TimeUnit.SECONDS -> value * 1000
        TimeUnit.MINUTES -> value * 60000
    }

    fun setMode(multi: Boolean) {
        isMultiTarget = multi
        AutoClickerAccessibilityService.isMultiTarget = multi
    }

    fun toggleOverlay(ctx: Context) {
        if (isOverlayActive) {
            ctx.stopService(Intent(ctx, FloatingOverlayService::class.java))
            isOverlayActive = false
        } else {
            if (Settings.canDrawOverlays(ctx)) {
                ctx.startForegroundService(Intent(ctx, FloatingOverlayService::class.java))
                isOverlayActive = true
            } else {
                ctx.startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            }
        }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
