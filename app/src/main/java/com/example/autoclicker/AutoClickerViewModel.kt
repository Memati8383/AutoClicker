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

class AutoClickerViewModel(private val context: Context) {

    companion object {
        var _isServiceEnabled = mutableStateOf(false)
        var _hasOverlayPermission = mutableStateOf(false)
        var _isOverlayActive = mutableStateOf(false)
        var _isMultiTarget = mutableStateOf(false)
        var _targets = mutableStateOf(listOf(ClickTarget(500f, 500f)))
        var _intervalValue = mutableLongStateOf(1000L)
        var _intervalUnit = mutableStateOf(TimeUnit.MILLISECONDS)
        var _stopCondition = mutableStateOf(StopCondition.INDEFINITE)
        var _stopValue = mutableLongStateOf(10L)
        var _stopUnit = mutableStateOf(TimeUnit.SECONDS)
        var _isClicking = mutableStateOf(false)
        var _dismissedA11yDialog = mutableStateOf(false)
        var _clickX = mutableStateOf("500")
        var _clickY = mutableStateOf("500")
        var _isDarkTheme = mutableStateOf(true)

        fun initTheme(context: Context) {
            val prefs = context.getSharedPreferences("autoclicker", Context.MODE_PRIVATE)
            _isDarkTheme.value = prefs.getBoolean("dark_theme", true)
        }

        fun toggleTheme(context: Context) {
            _isDarkTheme.value = !_isDarkTheme.value
            context.getSharedPreferences("autoclicker", Context.MODE_PRIVATE)
                .edit().putBoolean("dark_theme", _isDarkTheme.value).apply()
            (context as? Activity)?.recreate()
        }

        fun checkServiceState(context: Context) {
            _isServiceEnabled.value = if (AutoClickerAccessibilityService.instance != null) true
            else try {
                val services = Settings.Secure.getString(
                    context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                services?.contains("AutoClickerAccessibilityService") == true
            } catch (_: Exception) { false }
        }

        fun checkOverlayState(context: Context) {
            _hasOverlayPermission.value = Settings.canDrawOverlays(context)
        }

        fun addTarget() {
            val list = _targets.value.toMutableList()
            val last = list.lastOrNull() ?: ClickTarget(500f, 500f)
            list.add(ClickTarget(last.x + 50f, last.y + 50f))
            _targets.value = list
            AutoClickerAccessibilityService.targets = list
        }

        fun removeLastTarget() {
            if (_targets.value.size <= 1) return
            val list = _targets.value.dropLast(1)
            _targets.value = list
            AutoClickerAccessibilityService.targets = list.toMutableList()
        }

        fun setIntervalValue(value: Long) {
            _intervalValue.value = value
            AutoClickerAccessibilityService.clickIntervalMs = toMs(value, _intervalUnit.value)
        }

        fun setIntervalUnit(unit: TimeUnit) {
            _intervalUnit.value = unit
            AutoClickerAccessibilityService.clickIntervalMs = toMs(_intervalValue.value, unit)
        }

        fun setStopCondition(condition: StopCondition) {
            _stopCondition.value = condition
            AutoClickerAccessibilityService.stopCondition = condition
        }

        fun setStopValue(value: Long) {
            _stopValue.value = value
            AutoClickerAccessibilityService.stopValue = value
        }

        fun setStopUnit(unit: TimeUnit) {
            _stopUnit.value = unit
            AutoClickerAccessibilityService.stopUnit = unit
        }

        private fun toMs(value: Long, unit: TimeUnit): Long = when (unit) {
            TimeUnit.MILLISECONDS -> value
            TimeUnit.SECONDS -> value * 1000
            TimeUnit.MINUTES -> value * 60000
        }
    }

    var isServiceEnabled by _isServiceEnabled; private set
    var hasOverlayPermission by _hasOverlayPermission; private set
    var isOverlayActive by _isOverlayActive; private set
    var isMultiTarget by _isMultiTarget; private set
    var targets by _targets; private set
    var intervalValue by _intervalValue; private set
    var intervalUnit by _intervalUnit; private set
    var stopCondition by _stopCondition; private set
    var stopValue by _stopValue; private set
    var stopUnit by _stopUnit; private set
    var isClicking by _isClicking; private set
    var dismissedA11yDialog by _dismissedA11yDialog
    var clickX by _clickX
    var clickY by _clickY

    fun checkAccessibilityService() {
        isServiceEnabled = isServiceEnabledCheck()
    }

    fun checkOverlayPermission() {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    fun openAccessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openOverlaySettings() {
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun updateInterval(ms: Long) {
        _intervalValue.value = ms
        AutoClickerAccessibilityService.clickIntervalMs = ms
    }

    fun updatePosition(x: String, y: String) {
        _clickX.value = x
        _clickY.value = y
        x.toFloatOrNull()?.let { AutoClickerAccessibilityService.clickX = it }
        y.toFloatOrNull()?.let { AutoClickerAccessibilityService.clickY = it }
    }

    fun setMode(multi: Boolean) {
        _isMultiTarget.value = multi
        AutoClickerAccessibilityService.isMultiTarget = multi
    }

    fun toggleOverlay(ctx: Context) {
        if (isOverlayActive) {
            ctx.stopService(Intent(ctx, FloatingOverlayService::class.java))
            _isOverlayActive.value = false
        } else {
            if (Settings.canDrawOverlays(ctx)) {
                ctx.startForegroundService(Intent(ctx, FloatingOverlayService::class.java))
                _isOverlayActive.value = true
            } else {
                openOverlaySettings()
            }
        }
    }

    fun toggleClicking() {
        val service = AutoClickerAccessibilityService.instance
        if (service == null) {
            checkAccessibilityService()
            return
        }
        if (isClicking) {
            service.stopClicking()
            _isClicking.value = false
        } else {
            AutoClickerAccessibilityService.clickIntervalMs = intervalValue
            AutoClickerAccessibilityService.isMultiTarget = isMultiTarget
            AutoClickerAccessibilityService.targets = _targets.value.toMutableList()
            AutoClickerAccessibilityService.stopCondition = stopCondition
            AutoClickerAccessibilityService.stopValue = stopValue
            AutoClickerAccessibilityService.stopUnit = stopUnit
            service.startClicking()
            _isClicking.value = true
        }
    }

    private fun isServiceEnabledCheck(): Boolean {
        if (AutoClickerAccessibilityService.instance != null) return true
        return try {
            val services = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            services?.contains("AutoClickerAccessibilityService") == true
        } catch (_: Exception) { false }
    }
}
