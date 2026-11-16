package com.nethax.ultimate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

private val AimBg = Color(0xFFF5F8FF)
private val AimCard = Color(0xFFFFFFFF)
private val AimCyan = Color(0xFF2479F2)
private val AimGreen = Color(0xFF16875A)

private const val AIM_PREFS = "netha_aim_frame_status"
private const val AIM_RUNNING_KEY = "is_running"
private const val AIM_SIZE_KEY = "frame_size"
private const val AIM_ALPHA_KEY = "frame_alpha"
private const val AIM_X_KEY = "position_x"
private const val AIM_Y_KEY = "position_y"

@Composable
fun NethaXAimFrame(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    var overlayEnabled by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    val prefs = remember {
        context.getSharedPreferences(
            AIM_PREFS,
            Context.MODE_PRIVATE
        )
    }

    var isRunning by remember {
        mutableStateOf(
            prefs.getBoolean(AIM_RUNNING_KEY, false)
        )
    }

    var editMode by remember { mutableStateOf(false) }
    var positionEditMode by remember { mutableStateOf(false) }

    var frameSize by remember {
        mutableFloatStateOf(
            prefs.getFloat(AIM_SIZE_KEY, 180f).coerceIn(80f, 320f)
        )
    }

    var transparency by remember {
        mutableFloatStateOf(
            prefs.getFloat(AIM_ALPHA_KEY, 0.75f).coerceIn(0.15f, 1f)
        )
    }

    var positionX by remember {
        mutableFloatStateOf(
            prefs.getInt(AIM_X_KEY, 0).coerceIn(-600, 600).toFloat()
        )
    }

    var positionY by remember {
        mutableFloatStateOf(
            prefs.getInt(AIM_Y_KEY, 0).coerceIn(-600, 600).toFloat()
        )
    }

    // SAVE SETTINGS

    fun saveAimSettings() {
        prefs.edit()
            .putFloat(AIM_SIZE_KEY, frameSize)
            .putFloat(AIM_ALPHA_KEY, transparency)
            .putInt(AIM_X_KEY, positionX.toInt())
            .putInt(AIM_Y_KEY, positionY.toInt())
            .apply()
    }

    // RECEIVE SERVICE STATUS AND SETTINGS

    DisposableEffect(context) {

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                receivedContext: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action ==
                    NethaXAimFrameService.ACTION_STATUS_CHANGED
                ) {
                    isRunning = intent.getBooleanExtra(
                        NethaXAimFrameService.EXTRA_RUNNING,
                        false
                    )

                    frameSize = intent.getFloatExtra(
                        NethaXAimFrameService.EXTRA_SIZE,
                        prefs.getFloat(AIM_SIZE_KEY, 180f)
                    ).coerceIn(80f, 320f)

                    transparency = intent.getFloatExtra(
                        NethaXAimFrameService.EXTRA_ALPHA,
                        prefs.getFloat(AIM_ALPHA_KEY, 0.75f)
                    ).coerceIn(0.15f, 1f)

                    positionX = intent.getIntExtra(
                        NethaXAimFrameService.EXTRA_X,
                        prefs.getInt(AIM_X_KEY, 0)
                    ).coerceIn(-600, 600).toFloat()

                    positionY = intent.getIntExtra(
                        NethaXAimFrameService.EXTRA_Y,
                        prefs.getInt(AIM_Y_KEY, 0)
                    ).coerceIn(-600, 600).toFloat()
                }
            }
        }

        val filter = IntentFilter(
            NethaXAimFrameService.ACTION_STATUS_CHANGED
        )

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        // Load saved values when screen opens.

        isRunning = prefs.getBoolean(
            AIM_RUNNING_KEY,
            false
        )

        frameSize = prefs.getFloat(
            AIM_SIZE_KEY,
            180f
        ).coerceIn(80f, 320f)

        transparency = prefs.getFloat(
            AIM_ALPHA_KEY,
            0.75f
        ).coerceIn(0.15f, 1f)

        positionX = prefs.getInt(
            AIM_X_KEY,
            0
        ).coerceIn(-600, 600).toFloat()

        positionY = prefs.getInt(
            AIM_Y_KEY,
            0
        ).coerceIn(-600, 600).toFloat()

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    // OVERLAY PERMISSION

    val overlayPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            overlayEnabled = Settings.canDrawOverlays(context)
        }

    fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )

            overlayPermissionLauncher.launch(intent)
        } else {
            overlayEnabled = true
        }
    }

    // UPDATE FRAME

    fun updateAimFrame() {

        // Save even when the overlay is stopped.

        saveAimSettings()

        if (!isRunning || !Settings.canDrawOverlays(context)) {
            return
        }

        val intent = Intent(
            context,
            NethaXAimFrameService::class.java
        ).apply {
            action = NethaXAimFrameService.ACTION_UPDATE
            putExtra(NethaXAimFrameService.EXTRA_SIZE, frameSize)
            putExtra(NethaXAimFrameService.EXTRA_ALPHA, transparency)
            putExtra(NethaXAimFrameService.EXTRA_X, positionX.toInt())
            putExtra(NethaXAimFrameService.EXTRA_Y, positionY.toInt())
        }

        try {
            context.startService(intent)
        } catch (_: Exception) {
        }
    }

    // START AIM FRAME

    fun startAimFrame() {

        overlayEnabled = Settings.canDrawOverlays(context)

        if (!overlayEnabled) {
            requestOverlayPermission()
            return
        }

        saveAimSettings()

        val intent = Intent(
            context,
            NethaXAimFrameService::class.java
        ).apply {
            action = NethaXAimFrameService.ACTION_START
            putExtra(NethaXAimFrameService.EXTRA_SIZE, frameSize)
            putExtra(NethaXAimFrameService.EXTRA_ALPHA, transparency)
            putExtra(NethaXAimFrameService.EXTRA_X, positionX.toInt())
            putExtra(NethaXAimFrameService.EXTRA_Y, positionY.toInt())
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            editMode = false
            positionEditMode = false

        } catch (_: Exception) {
            isRunning = false
        }
    }

    // STOP AIM FRAME

    fun stopAimFrame() {

        val intent = Intent(
            context,
            NethaXAimFrameService::class.java
        ).apply {
            action = NethaXAimFrameService.ACTION_STOP
        }

        try {
            context.startService(intent)
        } catch (_: Exception) {
            isRunning = false
        }

        editMode = false
        positionEditMode = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AimBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // HEADER

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← BACK", color = AimCyan)
            }

            Spacer(Modifier.weight(1f))

            Text(
                "NETHA X",
                color = AimGreen,
                fontSize = 17.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "AIM FRAME",
            color = Color.White,
            fontSize = 27.sp
        )

        Text(
            "STATIC AIM GUIDE OVERLAY",
            color = AimCyan,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(22.dp))

        // STATUS CARD

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AimCard, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                "OVERLAY STATUS",
                color = AimCyan,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                if (isRunning) {
                    "AIM FRAME ACTIVE"
                } else {
                    "AIM FRAME STOPPED"
                },
                color = if (isRunning) AimGreen else Color.LightGray,
                fontSize = 17.sp
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (isRunning) {
                        stopAimFrame()
                    } else {
                        startAimFrame()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        Color(0xFF9B2636)
                    } else {
                        AimGreen
                    },
                    contentColor = Color.Black
                )
            ) {
                Text(
                    if (isRunning) {
                        "STOP AIM FRAME"
                    } else {
                        "START AIM FRAME"
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // PERMISSION CARD

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AimCard, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                "OVERLAY PERMISSION",
                color = AimCyan,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                if (overlayEnabled) {
                    "Permission Granted ✓"
                } else {
                    "Permission Required"
                },
                color = if (overlayEnabled) {
                    AimGreen
                } else {
                    Color(0xFFFFB74D)
                }
            )

            if (!overlayEnabled) {
                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        requestOverlayPermission()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GRANT OVERLAY PERMISSION")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // FRAME SIZE CONTROLS

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AimCard, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FRAME SIZE",
                    color = AimCyan,
                    fontSize = 13.sp
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "${frameSize.toInt()} px",
                    color = Color.White
                )
            }

            Spacer(Modifier.height(10.dp))

            Slider(
                value = frameSize,
                onValueChange = { newSize ->
                    if (editMode) {
                        frameSize = newSize
                        updateAimFrame()
                    }
                },
                valueRange = 80f..320f,
                enabled = editMode,
                colors = SliderDefaults.colors(
                    thumbColor = AimGreen,
                    activeTrackColor = AimCyan
                )
            )

            Button(
                onClick = {
                    if (editMode) {
                        editMode = false
                        updateAimFrame()
                    } else {
                        editMode = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editMode) {
                        AimGreen
                    } else {
                        Color(0xFF263B50)
                    },
                    contentColor = Color.White
                )
            ) {
                Text(
                    if (editMode) {
                        "🔒 LOCK SIZE"
                    } else {
                        "🔓 EDIT SIZE"
                    }
                )
            }

            Text(
                if (editMode) {
                    "Size editing enabled"
                } else {
                    "Size locked"
                },
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // POSITION CONTROLS

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AimCard, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                "FRAME POSITION",
                color = AimCyan,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "X POSITION",
                    color = Color.White,
                    fontSize = 12.sp
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "${positionX.toInt()} px",
                    color = AimGreen
                )
            }

            Slider(
                value = positionX,
                onValueChange = {
                    if (positionEditMode) {
                        positionX = it
                        updateAimFrame()
                    }
                },
                valueRange = -600f..600f,
                enabled = positionEditMode,
                colors = SliderDefaults.colors(
                    thumbColor = AimGreen,
                    activeTrackColor = AimCyan
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Y POSITION",
                    color = Color.White,
                    fontSize = 12.sp
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "${positionY.toInt()} px",
                    color = AimGreen
                )
            }

            Slider(
                value = positionY,
                onValueChange = {
                    if (positionEditMode) {
                        positionY = it
                        updateAimFrame()
                    }
                },
                valueRange = -600f..600f,
                enabled = positionEditMode,
                colors = SliderDefaults.colors(
                    thumbColor = AimGreen,
                    activeTrackColor = AimCyan
                )
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    if (positionEditMode) {
                        positionEditMode = false
                        updateAimFrame()
                    } else {
                        positionEditMode = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (positionEditMode) {
                        AimGreen
                    } else {
                        Color(0xFF263B50)
                    },
                    contentColor = Color.White
                )
            ) {
                Text(
                    if (positionEditMode) {
                        "🔒 LOCK POSITION"
                    } else {
                        "🔓 EDIT POSITION"
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    positionX = 0f
                    positionY = 0f
                    updateAimFrame()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !positionEditMode
            ) {
                Text(
                    "RESET CENTER",
                    color = AimCyan
                )
            }

            Text(
                if (positionEditMode) {
                    "Position editing enabled"
                } else {
                    "Position locked"
                },
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // TRANSPARENCY CONTROLS

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AimCard, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                "FRAME TRANSPARENCY",
                color = AimCyan,
                fontSize = 13.sp
            )

            Slider(
                value = transparency,
                onValueChange = {
                    transparency = it
                    updateAimFrame()
                },
                valueRange = 0.15f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = AimGreen,
                    activeTrackColor = AimCyan
                )
            )

            Text(
                "${(transparency * 100).toInt()}%",
                color = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Note: Frame position is calibrated manually. " +
                "It does not detect enemies or control game aiming.",
            color = Color.Gray,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(20.dp))
    }
}
