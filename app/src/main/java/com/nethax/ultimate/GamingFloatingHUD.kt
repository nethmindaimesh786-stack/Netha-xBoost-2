package com.nethax.ultimate

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private val HudGreen = Color(0xFF16875A)
private val HudCyan = Color(0xFF2479F2)
private val HudPanel = Color(0xFFFFFFFF)

private data class LaunchableGame(
    val name: String,
    val packageName: String
)

@Composable
internal fun GamingFloatingHUDScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preferences = remember {
        context.getSharedPreferences(
            GamingFloatingHUDService.PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    var battery by remember { mutableIntStateOf(-1) }
    var temperature by remember { mutableFloatStateOf(-1f) }

    // Service එකෙන් ලැබෙන Timer එක
    var playTime by remember { mutableLongStateOf(0L) }
    var gameRunning by remember { mutableStateOf(false) }

    var hudActive by remember { mutableStateOf(false) }
    var hudStatus by remember {
        mutableStateOf("HUD READY")
    }

    var overlayAllowed by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    var usageAllowed by remember {
        mutableStateOf(hasUsageAccess(context))
    }

    var installedGames by remember {
        mutableStateOf<List<LaunchableGame>>(emptyList())
    }

    var selectedPackage by remember {
        mutableStateOf(
            preferences.getString(
                GamingFloatingHUDService.PREF_SELECTED_GAME,
                null
            )
        )
    }

    var showGamePicker by remember {
        mutableStateOf(false)
    }

    var loadingGames by remember {
        mutableStateOf(true)
    }

    var hudMessage by remember {
        mutableStateOf("HUD READY")
    }

    // --------------------------------------------------
    // START HUD SERVICE
    // --------------------------------------------------

    fun startHudService() {
        try {
            val serviceIntent = Intent(
                context,
                GamingFloatingHUDService::class.java
            ).apply {
                action = GamingFloatingHUDService.ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            hudMessage = "START REQUEST SENT"

        } catch (_: Exception) {
            hudMessage = "COULD NOT START HUD"
        }
    }

    // --------------------------------------------------
    // ANDROID 13+ NOTIFICATION PERMISSION
    // --------------------------------------------------

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (!granted) {
                hudMessage = "NOTIFICATION DENIED - STARTING HUD"
            }

            // Permission denied වුණත් Android FGS එක start කළ හැකියි.
            startHudService()
        }

    val selectedGame = installedGames.firstOrNull {
        it.packageName == selectedPackage
    }

    // --------------------------------------------------
    // LOAD INSTALLED APPS
    // --------------------------------------------------

    LaunchedEffect(Unit) {
        loadingGames = true

        installedGames = withContext(Dispatchers.IO) {
            loadLaunchableGames(context)
        }

        loadingGames = false
    }

    // --------------------------------------------------
    // BATTERY INFORMATION
    // --------------------------------------------------

    DisposableEffect(context) {

        val receiver = object : BroadcastReceiver() {

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

                battery = if (level >= 0 && scale > 0) {
                    level * 100 / scale
                } else {
                    -1
                }

                val temp = intent.getIntExtra(
                    BatteryManager.EXTRA_TEMPERATURE,
                    -1
                )

                temperature =
                    if (temp >= 0) temp / 10f else -1f
            }
        }

        val filter = IntentFilter(
            Intent.ACTION_BATTERY_CHANGED
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

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    // --------------------------------------------------
    // REFRESH PERMISSIONS WHEN SCREEN RESUMES
    // --------------------------------------------------

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {
                overlayAllowed =
                    Settings.canDrawOverlays(context)

                usageAllowed =
                    hasUsageAccess(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // --------------------------------------------------
    // SERVICE SYNCHRONIZATION
    // --------------------------------------------------

    LaunchedEffect(Unit) {

        while (true) {

            playTime = preferences.getLong(
                GamingFloatingHUDService.PREF_PLAY_TIME,
                0L
            )

            gameRunning = preferences.getBoolean(
                GamingFloatingHUDService.PREF_GAME_RUNNING,
                false
            )

            hudActive = preferences.getBoolean(
                GamingFloatingHUDService.PREF_HUD_RUNNING,
                false
            )

            hudStatus = preferences.getString(
                GamingFloatingHUDService.PREF_STATUS,
                "HUD READY"
            ) ?: "HUD READY"

            selectedPackage = preferences.getString(
                GamingFloatingHUDService.PREF_SELECTED_GAME,
                null
            )

            overlayAllowed =
                Settings.canDrawOverlays(context)

            usageAllowed =
                hasUsageAccess(context)

            delay(500L)
        }
    }

    // --------------------------------------------------
    // UI
    // --------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070713))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // HEADER

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(onClick = onBack) {
                Text(
                    "← BACK",
                    color = HudCyan
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                "NETHA X",
                color = HudGreen,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "GAMING FLOATING HUD",
            color = HudGreen,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "GAMING PERFORMANCE MONITOR",
            color = Color.Gray,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(22.dp))

        // GAME SELECTOR

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    HudPanel,
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {

            Text(
                "SELECT YOUR GAME",
                color = HudGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                selectedGame?.name ?: "No game selected",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            if (selectedGame != null) {
                Text(
                    selectedGame.packageName,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    showGamePicker = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HudGreen,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    "CHOOSE INSTALLED GAME",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // LIVE INFORMATION

        HudInfoCard(
            "BATTERY",
            if (battery >= 0) "$battery%" else "N/A"
        )

        HudInfoCard(
            "BATTERY TEMPERATURE",
            if (temperature >= 0f) {
                String.format(
                    Locale.US,
                    "%.1f °C",
                    temperature
                )
            } else {
                "N/A"
            }
        )

        HudInfoCard(
            "GAME FPS",
            "N/A"
        )

        HudInfoCard(
            "PLAY TIME",
            formatPlayTime(playTime)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            when {
                selectedGame == null ->
                    "● SELECT A GAME FIRST"

                !usageAllowed ->
                    "● USAGE ACCESS REQUIRED"

                !hudActive ->
                    "● HUD NOT RUNNING"

                gameRunning ->
                    "● SELECTED GAME ACTIVE"

                else ->
                    "● WAITING FOR SELECTED GAME"
            },
            color = if (gameRunning) HudGreen else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            hudStatus,
            color = HudCyan,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(20.dp))

        // USAGE PERMISSION

        PermissionCard(
            title = "USAGE ACCESS",
            description = if (usageAllowed) {
                "Permission enabled"
            } else {
                "Required to detect the selected game"
            },
            granted = usageAllowed,
            buttonText = "OPEN SETTINGS",
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_USAGE_ACCESS_SETTINGS
                    )
                )
            }
        )

        Spacer(Modifier.height(12.dp))

        // OVERLAY PERMISSION

        PermissionCard(
            title = "FLOATING OVERLAY",
            description = if (overlayAllowed) {
                "Overlay permission enabled"
            } else {
                "Required to display HUD over games"
            },
            granted = overlayAllowed,
            buttonText = "GRANT PERMISSION",
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )

                context.startActivity(intent)
            }
        )

        Spacer(Modifier.height(20.dp))

        // START HUD

        Button(
            onClick = {

                overlayAllowed =
                    Settings.canDrawOverlays(context)

                usageAllowed =
                    hasUsageAccess(context)

                when {

                    selectedPackage.isNullOrBlank() -> {
                        hudMessage = "SELECT A GAME FIRST"
                        showGamePicker = true
                    }

                    !usageAllowed -> {
                        hudMessage =
                            "ENABLE USAGE ACCESS FIRST"

                        context.startActivity(
                            Intent(
                                Settings.ACTION_USAGE_ACCESS_SETTINGS
                            )
                        )
                    }

                    !overlayAllowed -> {
                        hudMessage =
                            "ENABLE OVERLAY PERMISSION FIRST"

                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse(
                                    "package:${context.packageName}"
                                )
                            )
                        )
                    }

                    else -> {

                        if (
                            Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            startHudService()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !hudActive,
            colors = ButtonDefaults.buttonColors(
                containerColor = HudGreen,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF203B35),
                disabledContentColor = HudGreen
            )
        ) {
            Text(
                if (hudActive) "HUD ACTIVE" else "START HUD",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(10.dp))

        // STOP HUD

        OutlinedButton(
            onClick = {

                try {
                    val serviceIntent = Intent(
                        context,
                        GamingFloatingHUDService::class.java
                    )

                    context.stopService(serviceIntent)

                    hudMessage = "STOP REQUEST SENT"

                } catch (_: Exception) {
                    hudMessage = "COULD NOT STOP HUD"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = hudActive,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFF7777),
                disabledContentColor = Color.Gray
            )
        ) {
            Text(
                "STOP HUD",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            hudMessage,
            color = HudCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "FPS is not available through a universal public " +
                "Android API. Select your game to track play time.",
            color = Color.Gray,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }

    // --------------------------------------------------
    // GAME SELECTION DIALOG
    // --------------------------------------------------

    if (showGamePicker) {

        AlertDialog(
            onDismissRequest = {
                showGamePicker = false
            },
            containerColor = HudPanel,
            title = {
                Text(
                    "SELECT INSTALLED GAME",
                    color = HudGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {

                when {

                    loadingGames -> {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = HudGreen
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Loading installed apps...",
                                color = Color.White
                            )
                        }
                    }

                    installedGames.isEmpty() -> {

                        Text(
                            "No launchable apps found. Check app visibility setup.",
                            color = Color.White
                        )
                    }

                    else -> {

                        LazyColumn(
                            modifier = Modifier.heightIn(
                                max = 380.dp
                            )
                        ) {

                            items(
                                installedGames,
                                key = { it.packageName }
                            ) { app ->

                                val isSelected =
                                    app.packageName == selectedPackage

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {

                                            selectedPackage =
                                                app.packageName

                                            preferences.edit()
                                                .putString(
                                                    GamingFloatingHUDService
                                                        .PREF_SELECTED_GAME,
                                                    app.packageName
                                                )
                                                .putLong(
                                                    GamingFloatingHUDService
                                                        .PREF_PLAY_TIME,
                                                    0L
                                                )
                                                .putBoolean(
                                                    GamingFloatingHUDService
                                                        .PREF_GAME_RUNNING,
                                                    false
                                                )
                                                .apply()

                                            showGamePicker = false

                                            hudMessage =
                                                "GAME SELECTED: ${app.name}"
                                        }
                                        .background(
                                            if (isSelected) {
                                                Color(0xFF203B35)
                                            } else {
                                                Color.Transparent
                                            },
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(12.dp)
                                ) {

                                    Text(
                                        app.name,
                                        color = if (isSelected) {
                                            HudGreen
                                        } else {
                                            Color.White
                                        },
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        app.packageName,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }

                                HorizontalDivider(
                                    color = Color(0xFF263044)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        showGamePicker = false
                    }
                ) {
                    Text(
                        "CLOSE",
                        color = HudCyan
                    )
                }
            }
        )
    }
}

// --------------------------------------------------
// LOAD INSTALLED LAUNCHER APPS
// --------------------------------------------------

private suspend fun loadLaunchableGames(
    context: Context
): List<LaunchableGame> {

    return try {

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        @Suppress("DEPRECATION")
        val results = context.packageManager
            .queryIntentActivities(intent, 0)

        results
            .mapNotNull { resolveInfo ->

                val appInfo = resolveInfo.activityInfo
                    ?.applicationInfo ?: return@mapNotNull null

                if (appInfo.packageName == context.packageName) {
                    return@mapNotNull null
                }

                val label = resolveInfo.loadLabel(
                    context.packageManager
                ).toString()

                LaunchableGame(
                    name = label,
                    packageName = appInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy {
                it.name.lowercase(Locale.getDefault())
            }

    } catch (_: Exception) {
        emptyList()
    }
}

// --------------------------------------------------
// HUD INFORMATION CARD
// --------------------------------------------------

@Composable
private fun HudInfoCard(
    title: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(
                HudPanel,
                RoundedCornerShape(14.dp)
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            title,
            color = Color.LightGray,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            value,
            color = HudGreen,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --------------------------------------------------
// PERMISSION CARD
// --------------------------------------------------

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                HudPanel,
                RoundedCornerShape(14.dp)
            )
            .padding(15.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                if (granted) "ENABLED" else "DISABLED",
                color = if (granted) {
                    HudGreen
                } else {
                    Color(0xFFFF7777)
                },
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(5.dp))

        Text(
            description,
            color = Color.Gray,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) {
                    Color(0xFF203B35)
                } else {
                    HudGreen
                },
                contentColor = if (granted) {
                    HudGreen
                } else {
                    Color.Black
                }
            )
        ) {
            Text(
                buttonText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --------------------------------------------------
// USAGE ACCESS CHECK
// --------------------------------------------------

private fun hasUsageAccess(
    context: Context
): Boolean {

    return try {

        val appOps = context.getSystemService(
            Context.APP_OPS_SERVICE
        ) as android.app.AppOpsManager

        val mode =
            if (Build.VERSION.SDK_INT >= 29) {

                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )

            } else {

                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }

        mode == android.app.AppOpsManager.MODE_ALLOWED

    } catch (_: Exception) {
        false
    }
}

// --------------------------------------------------
// TIMER FORMAT
// --------------------------------------------------

private fun formatPlayTime(
    milliseconds: Long
): String {

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
