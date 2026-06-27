package com.example.autoclicker

import android.app.*
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var params: WindowManager.LayoutParams? = null
    private var rootView: ViewGroup? = null
    private val targetMarkers = mutableListOf<View>()
    private var modalActive = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragStartParamsX = 0
    private var dragStartParamsY = 0
    private var panelDraggable = true
    private var targetCountText: TextView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startTime: Int): Int {
        if (rootView == null) showOverlay()
        startForeground(1, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        removeAllTargetMarkers()
        rootView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        AutoClickerViewModel._isOverlayActive.value = false
        super.onDestroy()
    }

    // ──────────────────────────────────────────────
    //  Main overlay layout
    // ──────────────────────────────────────────────

    private fun showOverlay() {
        val isMulti = AutoClickerViewModel._isMultiTarget.value

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(8))
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundedBg(c(0x20, 0x20, 0x20, 240), dp(20))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        rootView!!.addView(createHandle())

        val btnSize = dp(48)
        val startBtn = circleBtn("▶", Color.WHITE, c(0x43, 0xA0, 0x47), btnSize)
        startBtn.setOnClickListener {
            val s = AutoClickerAccessibilityService.instance
            if (s == null) {
                Toast.makeText(this, "Servis bağlı değil", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
                if (AutoClickerAccessibilityService.isRunning) {
                    s.stopClicking()
                    AutoClickerViewModel._isClicking.value = false
                    panelDraggable = true
                    showTargetMarkers()
                    Toast.makeText(this, "Durduruldu", Toast.LENGTH_SHORT).show()
                } else {
                    val raw = AutoClickerViewModel._intervalValue.value
                    val unit = AutoClickerViewModel._intervalUnit.value
                    AutoClickerAccessibilityService.clickIntervalMs = when (unit) {
                        TimeUnit.MILLISECONDS -> raw
                        TimeUnit.SECONDS -> raw * 1000
                        TimeUnit.MINUTES -> raw * 60000
                    }
                    AutoClickerAccessibilityService.isMultiTarget = isMulti
                    AutoClickerAccessibilityService.targets = AutoClickerViewModel._targets.value.toMutableList()
                    AutoClickerAccessibilityService.stopCondition = AutoClickerViewModel._stopCondition.value
                    AutoClickerAccessibilityService.stopValue = AutoClickerViewModel._stopValue.value
                    AutoClickerAccessibilityService.stopUnit = AutoClickerViewModel._stopUnit.value
                    panelDraggable = false
                    removeAllTargetMarkers()
                Toast.makeText(this, "Başlatıldı", Toast.LENGTH_SHORT).show()
                s.startClicking()
                AutoClickerViewModel._isClicking.value = true
            }
        }
        rootView!!.addView(startBtn)

        if (isMulti) {
            rootView!!.addView(circleBtn("+", Color.WHITE, c(0x37, 0x5F, 0x5F), btnSize) {
                AutoClickerViewModel.addTarget()
                showTargetMarkers()
                updateTargetCount()
            })
            rootView!!.addView(circleBtn("−", c(0xEF, 0x9A, 0x9A), c(0x4F, 0x3F, 0x3F), btnSize) {
                AutoClickerViewModel.removeLastTarget()
                showTargetMarkers()
                updateTargetCount()
            })
        }

        rootView!!.addView(circleBtn("⚙", Color.WHITE, c(0x25, 0x65, 0xC0), btnSize) { enterSettingsMode() })
        rootView!!.addView(circleBtn("✕", c(0xEF, 0x9A, 0x9A), c(0x4E, 0x2E, 0x2E), btnSize) { stopSelf() })

        // Click counter
        val clickCountText = TextView(this).apply {
            text = "0 tık"
            setTextColor(c(0x90, 0xA4, 0xAE))
            textSize = 10f
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        }
        rootView!!.addView(clickCountText)

        if (isMulti) {
            targetCountText = TextView(this).apply {
                text = "${AutoClickerViewModel._targets.value.size} hedef"
                setTextColor(c(0x90, 0xA4, 0xAE))
                textSize = 10f
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) }
            }
            rootView!!.addView(targetCountText)
        }

        rootView!!.isClickable = true

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = dp(8); y = dp(80)
            gravity = Gravity.TOP or Gravity.LEFT
        }

        windowManager.addView(rootView, params)

        // Persistent target markers
        showTargetMarkers()

        startBtn.post(object : Runnable {
            override fun run() {
                if (AutoClickerAccessibilityService.isRunning) {
                    startBtn.text = "⏹"
                    startBtn.background = roundedBg(c(0xE5, 0x39, 0x35), dp(10))
                    clickCountText.text = "${AutoClickerAccessibilityService.clickCount} tık"
                } else {
                    startBtn.text = "▶"
                    startBtn.background = roundedBg(c(0x43, 0xA0, 0x47), dp(10))
                }
                startBtn.postDelayed(this, 500)
            }
        })
    }

    // ──────────────────────────────────────────────
    //  Target markers — one per target, always visible
    // ──────────────────────────────────────────────

    private fun createMarker(target: ClickTarget, index: Int): View {
        val d = resources.displayMetrics.density
        val viewSize = dp(48)
        val half = viewSize / 2f

        val accentColors = listOf(
            c(0xFF, 0x55, 0x55),  // kırmızı
            c(0x55, 0xBB, 0xFF),  // mavi
            c(0x66, 0xDD, 0x77),  // yeşil
            c(0xFF, 0xBB, 0x44),  // turuncu
            c(0xCC, 0x88, 0xFF),  // mor
        )
        val accent = accentColors[index % accentColors.size]

        val marker = object : View(this@FloatingOverlayService) {
            val bgFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL; color = Color.argb(20, 255, 255, 255)
            }
            val outerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; color = Color.argb(180, 255, 255, 255); strokeWidth = 1.5f * d
            }
            val crossStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; color = Color.argb(100, 255, 255, 255); strokeWidth = 1f * d
            }
            val innerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; color = accent; strokeWidth = 2.5f * d
            }
            val dotFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL; color = accent
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 11f * d; textAlign = Paint.Align.CENTER
            }

            override fun onDraw(canvas: Canvas) {
                val cx = width / 2f; val cy = height / 2f
                val r = half - 4f * d

                // Hafif parıltı
                canvas.drawCircle(cx, cy, r, bgFill)
                // Dış halka
                canvas.drawCircle(cx, cy, r, outerStroke)
                // Nişangah çizgileri
                canvas.drawLine(cx - r, cy, cx + r, cy, crossStroke)
                canvas.drawLine(cx, cy - r, cx, cy + r, crossStroke)
                // İç renkli halka
                canvas.drawCircle(cx, cy, 8f * d, innerRing)
                // Merkez nokta
                canvas.drawCircle(cx, cy, 2.5f * d, dotFill)
                // Sıra numarası (üstte)
                canvas.drawText("${index + 1}", cx, cy - r + 14f * d, textPaint)
            }
        }

        marker.layoutParams = ViewGroup.LayoutParams(viewSize, viewSize)

        marker.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    val lp = marker.layoutParams as? WindowManager.LayoutParams
                    dragStartParamsX = lp?.x ?: 0
                    dragStartParamsY = lp?.y ?: 0
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - dragStartX).toInt()
                    val dy = (event.rawY - dragStartY).toInt()
                    val newX = dragStartParamsX + dx
                    val newY = dragStartParamsY + dy
                    val lp = marker.layoutParams as? WindowManager.LayoutParams
                    if (lp != null) {
                        lp.x = newX; lp.y = newY
                        windowManager.updateViewLayout(marker, lp)
                    }
                    val cx = newX + half; val cy = newY + half
                    if (AutoClickerViewModel._isMultiTarget.value) {
                        val list = AutoClickerViewModel._targets.value.toMutableList()
                        if (index < list.size) {
                            list[index] = ClickTarget(cx.toFloat(), cy.toFloat())
                            AutoClickerViewModel._targets.value = list
                            AutoClickerAccessibilityService.targets = list
                        }
                    } else {
                        AutoClickerAccessibilityService.clickX = cx.toFloat()
                        AutoClickerAccessibilityService.clickY = cy.toFloat()
                        AutoClickerViewModel._clickX.value = cx.toString()
                        AutoClickerViewModel._clickY.value = cy.toString()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }

        return marker
    }

    private fun showTargetMarkers() {
        removeAllTargetMarkers()

        val isMulti = AutoClickerViewModel._isMultiTarget.value
        val targets = if (isMulti) AutoClickerViewModel._targets.value
        else listOf(ClickTarget(
            AutoClickerAccessibilityService.clickX, AutoClickerAccessibilityService.clickY
        ))

        val size = dp(48)
        val half = size / 2
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        targets.forEachIndexed { idx, t ->
            val marker = createMarker(t, idx)
            targetMarkers.add(marker)

            val markerParams = WindowManager.LayoutParams(
                size, size,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                x = t.x.toInt() - half
                y = t.y.toInt() - half
                gravity = Gravity.TOP or Gravity.LEFT
            }

            windowManager.addView(marker, markerParams)
        }
    }

    private fun removeAllTargetMarkers() {
        targetMarkers.forEach { try { windowManager.removeView(it) } catch (_: Exception) {} }
        targetMarkers.clear()
    }

    private fun updateTargetCount() {
        targetCountText?.text = "${AutoClickerViewModel._targets.value.size} hedef"
    }

    // ──────────────────────────────────────────────
    //  Settings modal (full‑screen, toggled by ⚙)
    // ──────────────────────────────────────────────

    private fun enterSettingsMode() {
        if (modalActive) return
        modalActive = true

        val ctx = this
        val isMulti = AutoClickerViewModel._isMultiTarget.value
        val screenW = resources.displayMetrics.widthPixels
        val cardW = minOf(dp(340), (screenW * 0.85f).toInt())

        // ── Backdrop + card container ──
        val modalRoot = FrameLayout(ctx).apply {
            setBackgroundColor(Color.argb(180, 0, 0, 0))
            setOnClickListener { dismissModal(this) }
            isFocusable = true
            isFocusableInTouchMode = true
        }

        // ── Card ──
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(c(0x1C, 0x1C, 0x1C), dp(20))
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setClipToOutline(true)
        }

        // Scrollable content inside card
        val scrollContent = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            isFillViewport = false
        }
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }

        // ── Header ──
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(20) }
        }
        header.addView(TextView(ctx).apply {
            text = "Ayarlar"
            setTextColor(Color.WHITE); textSize = 20f; typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(ctx).apply {
            text = "✕"; setTextColor(c(0x90, 0xA4, 0xAE)); textSize = 22f; gravity = Gravity.CENTER
            setPadding(dp(6), dp(2), dp(6), dp(2))
            setOnClickListener { dismissModal(modalRoot) }
        })
        content.addView(header)

        // ── Tıklama Aralığı ──
        content.addView(settingsLabel("Tıklama Aralığı"))
        val intervalCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundedBg(c(0x2A, 0x2A, 0x2A), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) }
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        val intervalEt = EditText(ctx).apply {
            setText(AutoClickerViewModel._intervalValue.value.toString())
            setTextColor(Color.WHITE); textSize = 15f
            inputType = InputType.TYPE_CLASS_NUMBER
            background = null
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            gravity = Gravity.CENTER_VERTICAL
            setHintTextColor(c(0x90, 0xA4, 0xAE))
            hint = "değer"
        }
        intervalCard.addView(intervalEt)
        intervalCard.addView(unitToggleBtn(
            when (AutoClickerViewModel._intervalUnit.value) {
                TimeUnit.MILLISECONDS -> "ms"; TimeUnit.SECONDS -> "sn"; TimeUnit.MINUTES -> "dk"
            }
        ) {
            val cur = AutoClickerViewModel._intervalUnit.value
            val next = when (cur) {
                TimeUnit.MILLISECONDS -> TimeUnit.SECONDS; TimeUnit.SECONDS -> TimeUnit.MINUTES; TimeUnit.MINUTES -> TimeUnit.MILLISECONDS
            }
            AutoClickerViewModel.setIntervalUnit(next)
            it.text = when (next) { TimeUnit.MILLISECONDS -> "ms"; TimeUnit.SECONDS -> "sn"; TimeUnit.MINUTES -> "dk" }
        })
        content.addView(intervalCard)
        intervalEt.setOnEditorActionListener { _, _, _ ->
            intervalEt.text.toString().toLongOrNull()?.let { AutoClickerViewModel.setIntervalValue(it) }; true
        }
        intervalEt.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) intervalEt.text.toString().toLongOrNull()?.let { AutoClickerViewModel.setIntervalValue(it) }
        }

        // ── Durdurma ──
        content.addView(settingsLabel("Durdurma"))

        val stopCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(c(0x2A, 0x2A, 0x2A), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        // ── Stop rows ──
        fun refreshStopCard() {
            var idx = 0
            for (i in 0 until stopCard.childCount) {
                val row = stopCard.getChildAt(i) as? LinearLayout ?: continue
                val rb = row.getChildAt(0) as? RadioButton ?: continue
                rb.isChecked = when (idx) { 0 -> AutoClickerViewModel._stopCondition.value == StopCondition.INDEFINITE; 1 -> AutoClickerViewModel._stopCondition.value == StopCondition.TIME; 2 -> AutoClickerViewModel._stopCondition.value == StopCondition.COUNT; else -> false }
                row.background = if (rb.isChecked) roundedBg(c(0x15, 0x65, 0xC0, 60), dp(10)) else null
                idx++
            }
        }

        // Indefinite
        stopCard.addView(stopRow("Süresiz", AutoClickerViewModel._stopCondition.value == StopCondition.INDEFINITE) {
            AutoClickerViewModel.setStopCondition(StopCondition.INDEFINITE); refreshStopCard()
        })

        // Time
        val timeRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { AutoClickerViewModel.setStopCondition(StopCondition.TIME); refreshStopCard() }
        }
        timeRow.addView(RadioButton(ctx).apply { isChecked = AutoClickerViewModel._stopCondition.value == StopCondition.TIME; isEnabled = false })
        timeRow.addView(TextView(ctx).apply { text = "Zaman: "; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER_VERTICAL })
        val timeEt = numberEditText(AutoClickerViewModel._stopValue.value.toString(), dp(64))
        timeEt.setOnEditorActionListener { _, _, _ -> timeEt.text.toString().toLongOrNull()?.let { AutoClickerViewModel.setStopValue(it) }; true }
        timeEt.setOnFocusChangeListener { _, h -> if (!h) timeEt.text.toString().toLongOrNull()?.let { AutoClickerViewModel.setStopValue(it) } }
        timeRow.addView(timeEt)
        timeRow.addView(unitToggleBtn(
            when (AutoClickerViewModel._stopUnit.value) { TimeUnit.MILLISECONDS -> "ms"; TimeUnit.SECONDS -> "sn"; TimeUnit.MINUTES -> "dk" }
        ) {
            val cur = AutoClickerViewModel._stopUnit.value
            val next = when (cur) { TimeUnit.MILLISECONDS -> TimeUnit.SECONDS; TimeUnit.SECONDS -> TimeUnit.MINUTES; TimeUnit.MINUTES -> TimeUnit.MILLISECONDS }
            AutoClickerViewModel.setStopUnit(next); it.text = when (next) { TimeUnit.MILLISECONDS -> "ms"; TimeUnit.SECONDS -> "sn"; TimeUnit.MINUTES -> "dk" }
            AutoClickerViewModel.setStopCondition(StopCondition.TIME); refreshStopCard()
        })
        if (AutoClickerViewModel._stopCondition.value == StopCondition.TIME) timeRow.background = roundedBg(c(0x15, 0x65, 0xC0, 60), dp(10))
        stopCard.addView(timeRow)

        // Count
        val countRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { AutoClickerViewModel.setStopCondition(StopCondition.COUNT); refreshStopCard() }
        }
        countRow.addView(RadioButton(ctx).apply { isChecked = AutoClickerViewModel._stopCondition.value == StopCondition.COUNT; isEnabled = false })
        countRow.addView(TextView(ctx).apply { text = "Tıklama: "; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER_VERTICAL })
        val countEt = numberEditText(AutoClickerViewModel._stopValue.value.toString(), dp(72))
        countEt.setOnEditorActionListener { _, _, _ -> countEt.text.toString().toLongOrNull()?.let { AutoClickerViewModel.setStopValue(it) }; true }
        countEt.setOnFocusChangeListener { _, h -> if (!h) countEt.text.toString().toLongOrNull()?.let { AutoClickerViewModel.setStopValue(it) } }
        countRow.addView(countEt)
        if (AutoClickerViewModel._stopCondition.value == StopCondition.COUNT) countRow.background = roundedBg(c(0x15, 0x65, 0xC0, 60), dp(10))
        stopCard.addView(countRow)

        content.addView(stopCard)

        // ── Pozisyon (single target only) ──
        if (!isMulti) {
            content.addView(settingsLabel("Hedef Pozisyon"))
            val posCard = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg(c(0x2A, 0x2A, 0x2A), dp(12))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) }
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            val posRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) }
            }
            posRow.addView(TextView(ctx).apply { text = "X"; setTextColor(c(0x90, 0xA4, 0xAE)); textSize = 13f; gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8) } })
            val xEt = numberEditText(AutoClickerViewModel._clickX.value, dp(80))
            posRow.addView(xEt)
            posRow.addView(TextView(ctx).apply { text = "Y"; setTextColor(c(0x90, 0xA4, 0xAE)); textSize = 13f; gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(14); rightMargin = dp(8) } })
            val yEt = numberEditText(AutoClickerViewModel._clickY.value, dp(80))
            posRow.addView(yEt)
            posCard.addView(posRow)
            xEt.setOnEditorActionListener { _, _, _ -> xEt.text.toString().toFloatOrNull()?.let { AutoClickerAccessibilityService.clickX = it; AutoClickerViewModel._clickX.value = xEt.text.toString() }; false }
            yEt.setOnEditorActionListener { _, _, _ -> yEt.text.toString().toFloatOrNull()?.let { AutoClickerAccessibilityService.clickY = it; AutoClickerViewModel._clickY.value = yEt.text.toString() }; false }
            posCard.addView(TextView(ctx).apply {
                text = "Hedef işaretini sürükleyin"
                setTextColor(c(0x90, 0xA4, 0xAE)); textSize = 11f
            })
            content.addView(posCard)
        }

        scrollContent.addView(content)
        card.addView(scrollContent)

        // Center card in backdrop
        val cardLp = FrameLayout.LayoutParams(cardW, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
        modalRoot.addView(card, cardLp)

        // ── Window params ──
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val modalParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = 0; y = 0
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        windowManager.addView(modalRoot, modalParams)

        // ── Entry animation ──
        card.alpha = 0f; card.scaleX = 0.92f; card.scaleY = 0.92f
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun dismissModal(view: ViewGroup) {
        modalActive = false
        try { windowManager.removeView(view) } catch (_: Exception) {}
        rootView?.visibility = View.VISIBLE
    }

    // ── Settings helpers ──

    private fun settingsLabel(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(c(0x90, 0xA4, 0xAE)); textSize = 11f; typeface = Typeface.DEFAULT_BOLD
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
    }

    private fun stopRow(label: String, checked: Boolean, onClick: () -> Unit): LinearLayout {
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            if (checked) background = roundedBg(c(0x15, 0x65, 0xC0, 50), dp(10))
            setOnClickListener { onClick() }
            addView(RadioButton(ctx).apply { isChecked = checked; isEnabled = false })
            addView(TextView(ctx).apply { text = label; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER_VERTICAL })
        }
    }

    private fun unitToggleBtn(text: String, onClick: (TextView) -> Unit): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.WHITE); textSize = 13f; gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(dp(40), dp(34))
        background = roundedBg(c(0x37, 0x4F, 0x4F), dp(8))
        setOnClickListener { onClick(this) }
    }

    private fun numberEditText(default: String, width: Int): EditText = EditText(this).apply {
        setText(default)
        setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.CENTER
        inputType = InputType.TYPE_CLASS_NUMBER
        background = roundedBg(c(0x2A, 0x2A, 0x2A), dp(8))
        layoutParams = LinearLayout.LayoutParams(width, dp(38)).apply { rightMargin = dp(4); gravity = Gravity.CENTER_VERTICAL }
    }

    // ──────────────────────────────────────────────
    //  View builders
    // ──────────────────────────────────────────────

    private fun createHandle(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(16)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(4)
                bottomMargin = dp(6)
            }
            background = roundedBg(c(0x61, 0x61, 0x61, 80), dp(8))
            setOnTouchListener { _, event ->
                if (!panelDraggable) return@setOnTouchListener false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartX = event.rawX.toInt().toFloat(); dragStartY = event.rawY.toInt().toFloat()
                        dragStartParamsX = params?.x ?: 0; dragStartParamsY = params?.y ?: 0
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - dragStartX).toInt()
                        val dy = (event.rawY - dragStartY).toInt()
                        params?.let { p ->
                            p.x = (dragStartParamsX + dx).coerceAtLeast(0)
                            p.y = (dragStartParamsY + dy).coerceAtLeast(0)
                            windowManager.updateViewLayout(rootView, p)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> true
                    else -> false
                }
            }
        }
    }

    private fun circleBtn(text: String, textColor: Int, bgColor: Int, size: Int, onClick: (() -> Unit)? = null): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                bottomMargin = dp(6); gravity = Gravity.CENTER_HORIZONTAL
            }
            this.text = text
            setTextColor(textColor)
            textSize = 20f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(bgColor, size / 2)
            elevation = dp(3).toFloat()
            if (onClick != null) setOnClickListener { onClick() }
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> animate().scaleX(0.84f).scaleY(0.84f).setDuration(80).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }
                }
                false
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Misc helpers
    // ──────────────────────────────────────────────

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun roundedBg(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply { setColor(color); setCornerRadius(radius.toFloat()) }
    }

    private fun c(r: Int, g: Int, b: Int, a: Int = 255): Int = Color.argb(a, r, g, b)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("overlay_channel", "Auto Clicker", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("Auto Clicker")
            .setContentText("Overlay aktif")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build()
}
