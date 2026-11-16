package com.nethax.ultimate

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class GamingFloatingHUDService : Service() {

    companion object {
        const val ACTION_START = "com.nethax.ultimate.GAMING_HUD_START"
        const val ACTION_STOP = "com.nethax.ultimate.GAMING_HUD_STOP"

        const val PREFS_NAME = "gaming_hud_prefs"
        const val PREF_SELECTED_GAME = "selected_game_package"
        const val PREF_PLAY_TIME = "play_time_ms"
        const val PREF_GAME_RUNNING = "game_running"
        const val PREF_HUD_RUNNING = "hud_running"
        const val PREF_STATUS = "hud_status"

        private const val CHANNEL_ID = "netha_gaming_hud"
        private const val NOTIFICATION_ID = 8101
        private const val UPDATE_INTERVAL = 1000L
    }

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var windowManager: WindowManager

    private val handler = Handler(Looper.getMainLooper())

    // Full HUD and minimized circle
    private var hudView: LinearLayout? = null
    private var miniView: TextView? = null

    private var hudParams: WindowManager.LayoutParams? = null
    private var miniParams: WindowManager.LayoutParams? = null

    private var hudAttached = false
    private var miniAttached = false

    private var batteryPercent = 0
    private var batteryTemperature = 0f

    private var foregroundPackage: String? = null
    private var selectedGamePackage: String? = null

    private var lastUsageQueryTime = 0L

    private var playTimeMs = 0L
    private var gameStartElapsed = 0L

    private var gameRunning = false
    private var receiverRegistered = false
    private var isRunning = false

    private lateinit var batteryReceiver: BroadcastReceiver

    // --------------------------------------------------
    // UPDATE LOOP
    // --------------------------------------------------

    private val updateTask = object : Runnable {
        override fun run() {
            if (!isRunning) return

            checkSelectedGame()
            updateSavedState()
            updateHUD()

            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }

    // --------------------------------------------------
    // SERVICE CREATE
    // --------------------------------------------------

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        windowManager = getSystemService(
            WINDOW_SERVICE
        ) as WindowManager

        selectedGamePackage = prefs.getString(
            PREF_SELECTED_GAME,
            null
        )

        batteryPercent = prefs.getInt("battery_percent", 0)
        batteryTemperature = prefs.getFloat("battery_temperature", 0f)

        createNotificationChannel()
        registerBatteryReceiver()
    }

    // --------------------------------------------------
    // SERVICE START COMMAND
    // --------------------------------------------------

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                if (!isRunning) {
                    startHUDService()
                }
            }
        }

        return START_NOT_STICKY
    }

    // --------------------------------------------------
    // START SERVICE
    // --------------------------------------------------

    private fun startHUDService() {
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
            prefs.edit()
                .putBoolean(PREF_HUD_RUNNING, false)
                .putBoolean(PREF_GAME_RUNNING, false)
                .putString(PREF_STATUS, "SERVICE START FAILED")
                .apply()

            stopSelf()
            return
        }

        isRunning = true

        playTimeMs = 0L
        gameStartElapsed = 0L
        gameRunning = false
        foregroundPackage = null

        selectedGamePackage = prefs.getString(
            PREF_SELECTED_GAME,
            null
        )

        lastUsageQueryTime =
            System.currentTimeMillis() - 30000L

        prefs.edit()
            .putLong(PREF_PLAY_TIME, 0L)
            .putBoolean(PREF_GAME_RUNNING, false)
            .putBoolean(PREF_HUD_RUNNING, true)
            .putString(PREF_STATUS, "WAITING FOR GAME")
            .apply()

        if (Settings.canDrawOverlays(this)) {
            addOverlay()
        } else {
            saveStatus("OVERLAY PERMISSION REQUIRED")
        }

        handler.removeCallbacks(updateTask)
        handler.post(updateTask)
    }

    // --------------------------------------------------
    // NOTIFICATION CHANNEL
    // --------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gaming Floating HUD",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Gaming performance monitor"
            }

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }
    }

    // --------------------------------------------------
    // NOTIFICATION
    // --------------------------------------------------

    private fun createNotification(): Notification {
        val stopIntent = Intent(
            this,
            GamingFloatingHUDService::class.java
        ).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            8102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("NETHA X ULTIMATE")
            .setContentText("Gaming Floating HUD is running")
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "STOP HUD",
                stopPendingIntent
            )
            .build()
    }

    // --------------------------------------------------
    // BATTERY INFORMATION
    // --------------------------------------------------

    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) {
                    return
                }

                val level = intent.getIntExtra(
                    BatteryManager.EXTRA_LEVEL,
                    -1
                )

                val scale = intent.getIntExtra(
                    BatteryManager.EXTRA_SCALE,
                    -1
                )

                if (level >= 0 && scale > 0) {
                    batteryPercent = (level * 100) / scale
                }

                val temperature = intent.getIntExtra(
                    BatteryManager.EXTRA_TEMPERATURE,
                    -1
                )

                batteryTemperature =
                    if (temperature >= 0) {
                        temperature / 10f
                    } else {
                        0f
                    }

                prefs.edit()
                    .putInt("battery_percent", batteryPercent)
                    .putFloat("battery_temperature", batteryTemperature)
                    .apply()

                updateHUD()
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                batteryReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(batteryReceiver, filter)
        }

        receiverRegistered = true
    }

    // --------------------------------------------------
    // USAGE ACCESS CHECK
    // --------------------------------------------------

    private fun hasUsageAccess(): Boolean {
        return try {
            val appOps = getSystemService(
                APP_OPS_SERVICE
            ) as AppOpsManager

            val mode =
                if (Build.VERSION.SDK_INT >= 29) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        packageName
                    )
                }

            mode == AppOpsManager.MODE_ALLOWED

        } catch (_: Exception) {
            false
        }
    }

    // --------------------------------------------------
    // GAME DETECTION AND TIMER
    // --------------------------------------------------

    private fun checkSelectedGame() {
        val latestSelectedGame = prefs.getString(
            PREF_SELECTED_GAME,
            null
        )

        if (latestSelectedGame != selectedGamePackage) {
            selectedGamePackage = latestSelectedGame

            playTimeMs = 0L
            gameStartElapsed = 0L
            gameRunning = false
            foregroundPackage = null

            lastUsageQueryTime =
                System.currentTimeMillis() - 30000L

            saveTimerState()
        }

        if (selectedGamePackage.isNullOrBlank()) {
            pauseGameTimer()
            saveStatus("SELECT A GAME FIRST")
            return
        }

        if (!hasUsageAccess()) {
            pauseGameTimer()
            saveStatus("USAGE ACCESS REQUIRED")
            return
        }

        try {
            val usageStatsManager = getSystemService(
                USAGE_STATS_SERVICE
            ) as UsageStatsManager

            val currentTime = System.currentTimeMillis()

            val events = usageStatsManager.queryEvents(
                lastUsageQueryTime,
                currentTime
            )

            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                val eventPackage =
                    event.packageName ?: continue

                val resumedEvent: Int
                val pausedEvent: Int

                if (Build.VERSION.SDK_INT >= 29) {
                    resumedEvent =
                        UsageEvents.Event.ACTIVITY_RESUMED

                    pausedEvent =
                        UsageEvents.Event.ACTIVITY_PAUSED
                } else {
                    @Suppress("DEPRECATION")
                    val oldResumed =
                        UsageEvents.Event.MOVE_TO_FOREGROUND

                    @Suppress("DEPRECATION")
                    val oldPaused =
                        UsageEvents.Event.MOVE_TO_BACKGROUND

                    resumedEvent = oldResumed
                    pausedEvent = oldPaused
                }

                when (event.eventType) {
                    resumedEvent -> {
                        foregroundPackage = eventPackage
                    }

                    pausedEvent -> {
                        if (foregroundPackage == eventPackage) {
                            foregroundPackage = null
                        }
                    }

                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (foregroundPackage == eventPackage) {
                            foregroundPackage = null
                        }
                    }
                }
            }

            lastUsageQueryTime = currentTime

            if (foregroundPackage == selectedGamePackage) {
                resumeGameTimer()
                saveStatus("SELECTED GAME ACTIVE")
            } else {
                pauseGameTimer()
                saveStatus("WAITING FOR SELECTED GAME")
            }

        } catch (_: Exception) {
            pauseGameTimer()
            saveStatus("GAME DETECTION FAILED")
        }
    }

    // --------------------------------------------------
    // TIMER RESUME
    // --------------------------------------------------

    private fun resumeGameTimer() {
        if (!gameRunning) {
            gameRunning = true
            gameStartElapsed = SystemClock.elapsedRealtime()
            saveTimerState()
        }
    }

    // --------------------------------------------------
    // TIMER PAUSE
    // --------------------------------------------------

    private fun pauseGameTimer() {
        if (gameRunning) {
            val elapsed =
                SystemClock.elapsedRealtime() - gameStartElapsed

            playTimeMs += elapsed.coerceAtLeast(0L)

            gameRunning = false
            gameStartElapsed = 0L

            saveTimerState()
        }
    }

    // --------------------------------------------------
    // CURRENT PLAY TIME
    // --------------------------------------------------

    private fun getCurrentPlayTime(): Long {
        return if (gameRunning) {
            playTimeMs +
                (
                    SystemClock.elapsedRealtime() -
                        gameStartElapsed
                    ).coerceAtLeast(0L)
        } else {
            playTimeMs
        }
    }

    // --------------------------------------------------
    // SAVE TIMER STATE
    // --------------------------------------------------

    private fun saveTimerState() {
        prefs.edit()
            .putLong(PREF_PLAY_TIME, getCurrentPlayTime())
            .putBoolean(PREF_GAME_RUNNING, gameRunning)
            .apply()
    }

    private fun saveStatus(status: String) {
        prefs.edit()
            .putString(PREF_STATUS, status)
            .apply()
    }

    private fun updateSavedState() {
        prefs.edit()
            .putLong(PREF_PLAY_TIME, getCurrentPlayTime())
            .putBoolean(PREF_GAME_RUNNING, gameRunning)
            .putBoolean(PREF_HUD_RUNNING, isRunning)
            .apply()
    }

    // --------------------------------------------------
    // FLOATING HUD
    // --------------------------------------------------

    private fun addOverlay() {
        if (
            hudView != null ||
            !Settings.canDrawOverlays(this)
        ) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(12),
                dp(9),
                dp(12),
                dp(9)
            )

            background = GradientDrawable().apply {
                setColor(Color.argb(230, 8, 16, 25))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(0, 217, 255))
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Drag handle
        val dragHandle = TextView(this).apply {
            text = "NETHA HUD"
            textSize = 13f
            setTextColor(Color.rgb(0, 217, 255))

            setPadding(
                0,
                0,
                dp(12),
                dp(5)
            )
        }

        // Minimize button
        val minimizeButton = TextView(this).apply {
            text = "−"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

            setPadding(
                dp(5),
                0,
                dp(5),
                dp(5)
            )

            setOnClickListener {
                minimizeOverlay()
            }
        }

        // Close button
        val closeButton = TextView(this).apply {
            text = "×"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

            setPadding(
                dp(5),
                0,
                dp(4),
                dp(5)
            )

            setOnClickListener {
                stopSelf()
            }
        }

        headerRow.addView(
            dragHandle,
            LinearLayout.LayoutParams(
                0,
                dp(30),
                1f
            )
        )

        headerRow.addView(
            minimizeButton,
            LinearLayout.LayoutParams(
                dp(30),
                dp(30)
            )
        )

        headerRow.addView(
            closeButton,
            LinearLayout.LayoutParams(
                dp(30),
                dp(30)
            )
        )

        val details = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)

            setLineSpacing(
                dp(3).toFloat(),
                1f
            )

            tag = "hud_details"
        }

        container.addView(headerRow)
        container.addView(details)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(120)
        }

        // Drag full HUD from title
        dragHandle.setOnTouchListener(
            object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(
                    view: View,
                    event: MotionEvent
                ): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX +
                                (event.rawX - initialTouchX).toInt()

                            params.y = initialY +
                                (event.rawY - initialTouchY).toInt()

                            try {
                                windowManager.updateViewLayout(
                                    container,
                                    params
                                )
                            } catch (_: Exception) {
                            }

                            return true
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            return true
                        }
                    }

                    return false
                }
            }
        )

        try {
            windowManager.addView(container, params)

            hudView = container
            hudParams = params
            hudAttached = true

            updateHUD()

        } catch (_: Exception) {
            hudView = null
            hudParams = null
            hudAttached = false

            saveStatus("OVERLAY DISPLAY FAILED")
        }
    }

    // --------------------------------------------------
    // MINIMIZE HUD TO CIRCLE
    // --------------------------------------------------

    private fun minimizeOverlay() {
        val fullView = hudView ?: return
        val fullParams = hudParams ?: return

        if (!hudAttached || miniView != null) return

        val circle = TextView(this).apply {
            text = "N"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(0, 217, 255))
            gravity = Gravity.CENTER

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(235, 8, 16, 25))
                setStroke(dp(2), Color.rgb(0, 217, 255))
            }
        }

        val circleParams = WindowManager.LayoutParams(
            dp(52),
            dp(52),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = fullParams.x
            y = fullParams.y
        }

        // Tap to restore; drag to move the circle
        circle.setOnTouchListener(
            object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var moved = false

                private val touchSlop =
                    ViewConfiguration.get(this@GamingFloatingHUDService)
                        .scaledTouchSlop

                override fun onTouch(
                    view: View,
                    event: MotionEvent
                ): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = circleParams.x
                            initialY = circleParams.y

                            initialTouchX = event.rawX
                            initialTouchY = event.rawY

                            moved = false
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - initialTouchX
                            val dy = event.rawY - initialTouchY

                            if (
                                kotlin.math.abs(dx) > touchSlop ||
                                kotlin.math.abs(dy) > touchSlop
                            ) {
                                moved = true
                            }

                            if (moved) {
                                circleParams.x =
                                    initialX + dx.toInt()

                                circleParams.y =
                                    initialY + dy.toInt()

                                try {
                                    windowManager.updateViewLayout(
                                        circle,
                                        circleParams
                                    )
                                } catch (_: Exception) {
                                }
                            }

                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (!moved) {
                                restoreOverlay()
                            }
                            return true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            return true
                        }
                    }

                    return false
                }
            }
        )

        try {
            // Add circle first, then remove full HUD
            windowManager.addView(circle, circleParams)

            miniView = circle
            miniParams = circleParams
            miniAttached = true

            try {
                windowManager.removeView(fullView)
                hudAttached = false
            } catch (_: Exception) {
                removeMiniView()
                saveStatus("HUD MINIMIZE FAILED")
            }

        } catch (_: Exception) {
            miniView = null
            miniParams = null
            miniAttached = false

            saveStatus("MINIMIZE FAILED")
        }
    }

    // --------------------------------------------------
    // RESTORE FULL HUD
    // --------------------------------------------------

    private fun restoreOverlay() {
        val fullView = hudView ?: return
        val fullParams = hudParams ?: return

        removeMiniView()

        if (hudAttached) return

        try {
            windowManager.addView(fullView, fullParams)
            hudAttached = true
            updateHUD()

        } catch (_: Exception) {
            hudAttached = false
            saveStatus("HUD RESTORE FAILED")
        }
    }

    // --------------------------------------------------
    // REMOVE MINI CIRCLE
    // --------------------------------------------------

    private fun removeMiniView() {
        val view = miniView ?: return

        if (miniAttached) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }

        miniView = null
        miniParams = null
        miniAttached = false
    }

    // --------------------------------------------------
    // UPDATE HUD INFORMATION
    // --------------------------------------------------

    private fun updateHUD() {
        val view = hudView ?: return

        val details = view.findViewWithTag<TextView>(
            "hud_details"
        ) ?: return

        val time = formatTime(getCurrentPlayTime())

        val gameState =
            if (gameRunning) "PLAYING" else "PAUSED"

        val temperatureText =
            if (batteryTemperature > 0f) {
                String.format(
                    Locale.US,
                    "%.1f°C",
                    batteryTemperature
                )
            } else {
                "N/A"
            }

        details.text = """
            BATTERY   : $batteryPercent%
            TEMP      : $temperatureText
            FPS       : N/A
            PLAY TIME : $time
            STATUS    : $gameState
        """.trimIndent()
    }

    // --------------------------------------------------
    // REMOVE ALL OVERLAYS
    // --------------------------------------------------

    private fun removeOverlay() {
        removeMiniView()

        val view = hudView

        if (view != null && hudAttached) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }

        hudView = null
        hudParams = null
        hudAttached = false
    }

    // --------------------------------------------------
    // TIME FORMAT
    // --------------------------------------------------

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000L

        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    }

    // --------------------------------------------------
    // DP CONVERSION
    // --------------------------------------------------

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    // --------------------------------------------------
    // SERVICE DESTROY
    // --------------------------------------------------

    override fun onDestroy() {
        handler.removeCallbacks(updateTask)

        pauseGameTimer()
        isRunning = false

        prefs.edit()
            .putLong(PREF_PLAY_TIME, getCurrentPlayTime())
            .putBoolean(PREF_HUD_RUNNING, false)
            .putBoolean(PREF_GAME_RUNNING, false)
            .putString(PREF_STATUS, "HUD STOPPED")
            .apply()

        removeOverlay()

        if (receiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {
            }

            receiverRegistered = false
        }

        stopForeground(true)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
