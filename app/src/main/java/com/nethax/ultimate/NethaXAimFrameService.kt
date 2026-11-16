package com.nethax.ultimate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.content.pm.ServiceInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

class NethaXAimFrameService : Service() {

    companion object {
        const val ACTION_START = "com.nethax.ultimate.AIM_START"
        const val ACTION_STOP = "com.nethax.ultimate.AIM_STOP"
        const val ACTION_UPDATE = "com.nethax.ultimate.AIM_UPDATE"

        const val ACTION_STATUS_CHANGED =
            "com.nethax.ultimate.AIM_STATUS_CHANGED"

        const val EXTRA_RUNNING = "aim_frame_running"
        const val EXTRA_SIZE = "aim_frame_size"
        const val EXTRA_ALPHA = "aim_frame_alpha"
        const val EXTRA_X = "aim_frame_x"
        const val EXTRA_Y = "aim_frame_y"

        private const val NOTIFICATION_ID = 8201
        private const val CHANNEL_ID = "netha_aim_frame_channel"

        private const val PREFS_NAME = "netha_aim_frame_status"
        private const val PREF_RUNNING = "is_running"
        private const val PREF_SIZE = "frame_size"
        private const val PREF_ALPHA = "frame_alpha"
        private const val PREF_X = "position_x"
        private const val PREF_Y = "position_y"
    }

    private lateinit var windowManager: WindowManager

    private var frameView: AimFrameView? = null
    private var frameParams: WindowManager.LayoutParams? = null

    private var controlsView: View? = null
    private var controlsParams: WindowManager.LayoutParams? = null

    private var frameSize = 180f
    private var frameAlpha = 0.75f
    private var positionX = 0
    private var positionY = 0

    private var isMinimized = false
    private var controlsExpanded = false

    private var bubbleX = 16
    private var bubbleY = 120

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        loadSavedSettings()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_STOP -> {
                stopAimFrame()
                return START_NOT_STICKY
            }

            ACTION_UPDATE -> {
                readSettings(intent)

                if (!isMinimized && frameView != null) {
                    if (!updateFrame()) {
                        stopAimFrame()
                    } else {
                        broadcastCurrentState()
                    }
                }
            }

            ACTION_START -> {
                readSettings(intent)

                if (!Settings.canDrawOverlays(this)) {
                    stopAimFrame()
                    return START_NOT_STICKY
                }

                isMinimized = false
                startAimFrame()
            }

            else -> return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    // SETTINGS STORAGE

    private fun loadSavedSettings() {

        val prefs = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        frameSize = prefs.getFloat(
            PREF_SIZE,
            180f
        ).coerceIn(80f, 320f)

        frameAlpha = prefs.getFloat(
            PREF_ALPHA,
            0.75f
        ).coerceIn(0.15f, 1f)

        positionX = prefs.getInt(
            PREF_X,
            0
        ).coerceIn(-600, 600)

        positionY = prefs.getInt(
            PREF_Y,
            0
        ).coerceIn(-600, 600)
    }

    private fun saveSettings() {

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).edit()
            .putFloat(PREF_SIZE, frameSize)
            .putFloat(PREF_ALPHA, frameAlpha)
            .putInt(PREF_X, positionX)
            .putInt(PREF_Y, positionY)
            .apply()
    }

    private fun readSettings(intent: Intent) {

        val prefs = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        frameSize = intent.getFloatExtra(
            EXTRA_SIZE,
            prefs.getFloat(PREF_SIZE, frameSize)
        ).coerceIn(80f, 320f)

        frameAlpha = intent.getFloatExtra(
            EXTRA_ALPHA,
            prefs.getFloat(PREF_ALPHA, frameAlpha)
        ).coerceIn(0.15f, 1f)

        positionX = intent.getIntExtra(
            EXTRA_X,
            prefs.getInt(PREF_X, positionX)
        ).coerceIn(-600, 600)

        positionY = intent.getIntExtra(
            EXTRA_Y,
            prefs.getInt(PREF_Y, positionY)
        ).coerceIn(-600, 600)

        saveSettings()
    }

    // STATUS SYNC

    private fun setRunningStatus(running: Boolean) {

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).edit()
            .putBoolean(PREF_RUNNING, running)
            .putFloat(PREF_SIZE, frameSize)
            .putFloat(PREF_ALPHA, frameAlpha)
            .putInt(PREF_X, positionX)
            .putInt(PREF_Y, positionY)
            .apply()

        broadcastStatus(running)
    }

    private fun broadcastCurrentState() {

        val running = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).getBoolean(PREF_RUNNING, false)

        broadcastStatus(running)
    }

    private fun broadcastStatus(running: Boolean) {

        val statusIntent = Intent(ACTION_STATUS_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_RUNNING, running)
            putExtra(EXTRA_SIZE, frameSize)
            putExtra(EXTRA_ALPHA, frameAlpha)
            putExtra(EXTRA_X, positionX)
            putExtra(EXTRA_Y, positionY)
        }

        sendBroadcast(statusIntent)
    }

    // START AIM FRAME

    private fun startAimFrame() {

        createNotificationChannel()

        try {
            val notification = createNotification()

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification
                )
            }
        } catch (_: Exception) {
            stopAimFrame()
            return
        }

        val success = if (frameView == null) {
            showFrame()
        } else {
            updateFrame()
        }

        if (!success) {
            stopAimFrame()
            return
        }

        controlsExpanded = false

        if (!showBubble()) {
            stopAimFrame()
            return
        }

        setRunningStatus(true)
    }

    // FRAME OVERLAY

    private fun showFrame(): Boolean {

        if (frameView != null) return true

        val size = frameSize.toInt()

        val view = AimFrameView(this).apply {
            setFrameAppearance(frameAlpha)
        }

        val type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = positionX
            y = positionY
        }

        return try {
            windowManager.addView(view, params)

            frameView = view
            frameParams = params

            true
        } catch (_: Exception) {
            frameView = null
            frameParams = null
            false
        }
    }

    private fun updateFrame(): Boolean {

        val view = frameView ?: return false
        val params = frameParams ?: return false

        params.width = frameSize.toInt()
        params.height = frameSize.toInt()
        params.x = positionX
        params.y = positionY

        view.setFrameAppearance(frameAlpha)

        return try {
            windowManager.updateViewLayout(view, params)
            saveSettings()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun hideFrame() {

        frameView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }

        frameView = null
        frameParams = null
    }

    // FLOATING BALL

    private fun showBubble(): Boolean {

        removeControls()

        return try {

            val bubble = TextView(this).apply {
                text = "N"
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(0, 170, 205))
                    setStroke(dp(2), Color.CYAN)
                }

                elevation = dp(8).toFloat()
            }

            val type = if (Build.VERSION.SDK_INT >= 26) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                dp(50),
                dp(50),
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = bubbleX
                y = bubbleY
            }

            bubble.setOnTouchListener(
                createDragListener(bubble, params)
            )

            bubble.setOnClickListener {
                showExpandedControls()
            }

            windowManager.addView(bubble, params)

            controlsView = bubble
            controlsParams = params

            true

        } catch (_: Exception) {
            removeControls()
            false
        }
    }

    // DRAG FLOATING BALL

    private fun createDragListener(
        view: View,
        params: WindowManager.LayoutParams
    ): View.OnTouchListener {

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        return View.OnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY

                    if (
                        kotlin.math.abs(dx) > dp(5) ||
                        kotlin.math.abs(dy) > dp(5)
                    ) {
                        moved = true
                    }

                    if (moved) {

                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()

                        try {
                            windowManager.updateViewLayout(
                                view,
                                params
                            )
                        } catch (_: Exception) {
                        }
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    bubbleX = params.x
                    bubbleY = params.y

                    if (!moved) {
                        view.performClick()
                    }

                    true
                }

                MotionEvent.ACTION_CANCEL -> true

                else -> false
            }
        }
    }

    // EXPANDED CONTROLS

    private fun showExpandedControls() {

        controlsExpanded = true

        if (!createControls()) {
            controlsExpanded = false

            if (!showBubble()) {
                stopAimFrame()
            }
        }
    }

    private fun createControls(): Boolean {

        removeControls()

        return try {

            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(
                    dp(7),
                    dp(5),
                    dp(7),
                    dp(5)
                )

                background = GradientDrawable().apply {
                    setColor(Color.rgb(8, 17, 29))
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(1), Color.CYAN)
                }

                elevation = dp(8).toFloat()
            }

            // COLLAPSE TO BALL

            addControlButton(
                panel,
                "●",
                Color.rgb(0, 170, 205),
                true
            ) {
                controlsExpanded = false

                if (!showBubble()) {
                    stopAimFrame()
                }
            }

            if (isMinimized) {

                addControlButton(
                    panel,
                    "RESTORE",
                    Color.rgb(0, 217, 255)
                ) {
                    restoreAimFrame()
                }

                addControlButton(
                    panel,
                    "CLOSE",
                    Color.rgb(190, 45, 55)
                ) {
                    stopAimFrame()
                }

            } else {

                addControlButton(
                    panel,
                    "CENTER",
                    Color.rgb(0, 190, 220)
                ) {
                    centerAimFrame()
                }

                addControlButton(
                    panel,
                    "MINIMIZE",
                    Color.rgb(35, 65, 85)
                ) {
                    minimizeAimFrame()
                }

                addControlButton(
                    panel,
                    "CLOSE",
                    Color.rgb(190, 45, 55)
                ) {
                    stopAimFrame()
                }
            }

            val type = if (Build.VERSION.SDK_INT >= 26) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = bubbleX
                y = bubbleY
            }

            windowManager.addView(panel, params)

            controlsView = panel
            controlsParams = params

            true

        } catch (_: Exception) {
            removeControls()
            false
        }
    }

    private fun addControlButton(
        panel: LinearLayout,
        label: String,
        backgroundColor: Int,
        circular: Boolean = false,
        onClick: () -> Unit
    ) {

        val button = Button(this).apply {

            text = label
            textSize = if (circular) 13f else 10f
            setTextColor(Color.WHITE)
            setAllCaps(false)

            background = GradientDrawable().apply {
                if (circular) {
                    shape = GradientDrawable.OVAL
                } else {
                    cornerRadius = dp(9).toFloat()
                }

                setColor(backgroundColor)
            }

            setPadding(
                dp(8),
                dp(2),
                dp(8),
                dp(2)
            )

            setOnClickListener {
                onClick()
            }
        }

        val buttonParams = LinearLayout.LayoutParams(
            if (circular) {
                dp(42)
            } else {
                LinearLayout.LayoutParams.WRAP_CONTENT
            },
            dp(42)
        ).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

        panel.addView(button, buttonParams)
    }

    // CENTER FRAME

    private fun centerAimFrame() {

        positionX = 0
        positionY = 0

        saveSettings()

        if (!updateFrame()) {
            stopAimFrame()
        } else {
            broadcastCurrentState()
        }
    }

    // MINIMIZE FRAME

    private fun minimizeAimFrame() {

        if (isMinimized) return

        hideFrame()

        isMinimized = true
        controlsExpanded = false

        if (!showBubble()) {
            stopAimFrame()
            return
        }

        saveSettings()
        broadcastCurrentState()
    }

    // RESTORE FRAME

    private fun restoreAimFrame() {

        if (!isMinimized) return

        if (!Settings.canDrawOverlays(this)) {
            stopAimFrame()
            return
        }

        if (!showFrame()) {
            stopAimFrame()
            return
        }

        isMinimized = false
        controlsExpanded = false

        if (!showBubble()) {
            stopAimFrame()
            return
        }

        saveSettings()
        broadcastCurrentState()
    }

    // REMOVE CONTROLS

    private fun removeControls() {

        controlsView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }

        controlsView = null
        controlsParams = null
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    // STOP SERVICE

    private fun stopAimFrame() {

        hideFrame()
        removeControls()

        isMinimized = false
        controlsExpanded = false

        setRunningStatus(false)

        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    // NOTIFICATION

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "NETHA X AIM FRAME",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AIM FRAME overlay is running"
                setShowBadge(false)
            }

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        val stopIntent = Intent(
            this,
            NethaXAimFrameService::class.java
        ).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            8202,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("NETHA X AIM FRAME")
            .setContentText("Static aim guide overlay is active")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "STOP",
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {

        hideFrame()
        removeControls()

        isMinimized = false
        controlsExpanded = false

        setRunningStatus(false)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // AIM FRAME DRAWING VIEW

    private class AimFrameView(
        context: android.content.Context
    ) : View(context) {

        private val framePaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * resources.displayMetrics.density
            color = Color.CYAN
        }

        private val centerPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        private var frameOpacity = 0.75f

        fun setFrameAppearance(alpha: Float) {
            frameOpacity = alpha.coerceIn(0.15f, 1f)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {

            super.onDraw(canvas)

            val side = min(width, height).toFloat()
            val inset = 5f * resources.displayMetrics.density

            framePaint.alpha =
                (255 * frameOpacity).toInt()

            centerPaint.alpha =
                (255 * frameOpacity).toInt()

            val rect = RectF(
                inset,
                inset,
                side - inset,
                side - inset
            )

            canvas.drawRect(rect, framePaint)

            canvas.drawCircle(
                width / 2f,
                height / 2f,
                3f * resources.displayMetrics.density,
                centerPaint
            )
        }
    }
}
