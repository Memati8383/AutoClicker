package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

enum class StopCondition { INDEFINITE, TIME, COUNT }
enum class TimeUnit { MILLISECONDS, SECONDS, MINUTES }
data class ClickTarget(val x: Float, val y: Float)

class AutoClickerAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutoClickerAccessibilityService? = null
            private set
        var isRunning = false
            private set

        var clickIntervalMs: Long = 1000L
        var targets = mutableListOf(ClickTarget(500f, 500f))
        var isMultiTarget = false
        var stopCondition = StopCondition.INDEFINITE
        var stopValue: Long = 10L
        var stopUnit = TimeUnit.SECONDS
        var clickCount = 0
        var clickX: Float = 500f
        var clickY: Float = 500f
        private var startTime = 0L
        private var stopTime = 0L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val clickRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            performNextClick()
            if (shouldStop()) {
                stopClicking()
                return
            }
            handler.postDelayed(this, clickIntervalMs)
        }
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        stopClicking()
        instance = null
        return super.onUnbind(intent)
    }

    fun startClicking() {
        if (isRunning) return
        clickCount = 0
        startTime = System.currentTimeMillis()
        if (stopCondition == StopCondition.TIME) {
            stopTime = startTime + convertToMs(stopValue, stopUnit)
        }
        isRunning = true
        handler.post(clickRunnable)
    }

    fun stopClicking() {
        isRunning = false
        handler.removeCallbacks(clickRunnable)
    }

    private fun shouldStop(): Boolean {
        return when (stopCondition) {
            StopCondition.INDEFINITE -> false
            StopCondition.TIME -> System.currentTimeMillis() >= stopTime
            StopCondition.COUNT -> clickCount >= stopValue
        }
    }

    private fun performNextClick() {
        val x: Float
        val y: Float
        if (!isMultiTarget) {
            x = clickX
            y = clickY
        } else {
            if (targets.isEmpty()) return
            val idx = clickCount % targets.size
            x = targets[idx].x
            y = targets[idx].y
        }
        clickCount++

        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x + 1f, y + 1f)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                if (clickCount == 1) android.widget.Toast.makeText(this@AutoClickerAccessibilityService, "✓ tık #1 ($x,$y)", android.widget.Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                android.widget.Toast.makeText(this@AutoClickerAccessibilityService, "✗ tık iptal! Servis sorunu", android.widget.Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun convertToMs(value: Long, unit: TimeUnit): Long = when (unit) {
        TimeUnit.MILLISECONDS -> value
        TimeUnit.SECONDS -> value * 1000
        TimeUnit.MINUTES -> value * 60000
    }
}
