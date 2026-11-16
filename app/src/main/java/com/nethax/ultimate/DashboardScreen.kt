package com.nethax.ultimate

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlinx.coroutines.delay

private val DashboardInk = Color(0xFF17213D)
private val DashboardMuted = Color(0xFF687895)
private val DashboardBlue = Color(0xFF2479F2)
private val DashboardPurple = Color(0xFF8054E9)
private val DashboardTeal = Color(0xFF0BAA9B)
private val DashboardPink = Color(0xFFE65DAF)
private val DashboardOrange = Color(0xFFE79A28)
private val DashboardCard = Color(0xFFFFFFFF)

@Composable
internal fun DashboardScreen(
    onBackground: () -> Unit,
    onTranslation: () -> Unit,
    onOptimizer: () -> Unit,
    onNetworkSpeed: () -> Unit,
    onBlackbox: () -> Unit,
    onFileRescue: () -> Unit,
    onGamingHud: () -> Unit,
    onAimFrame: () -> Unit
) {
    var dashboardVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val deviceSnapshot = remember(context) { readDeviceSnapshot(context) }

    LaunchedEffect(Unit) {
        dashboardVisible = true
    }

    val dashboardAlpha by animateFloatAsState(
        targetValue = if (dashboardVisible) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "dashboard-entry-alpha"
    )

    val dashboardOffset by animateFloatAsState(
        targetValue = if (dashboardVisible) 0f else 14f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "dashboard-entry-offset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = dashboardAlpha
                translationY = dashboardOffset
            }
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        DashboardHeader()

        WelcomePanel()

        DeviceStatusPanel(deviceSnapshot)

        SectionHeader(
            title = "OUR TOOLS",
            detail = "8 FEATURES"
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "✂️",
                title = "AI Background Remover",
                description = "Remove photo background",
                action = "OPEN TOOL",
                accent = DashboardPurple,
                animationDelayMillis = 60L,
                onClick = onBackground
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "🌐",
                title = "Translation Camera",
                description = "Live camera translator",
                action = "OPEN TOOL",
                accent = DashboardBlue,
                animationDelayMillis = 110L,
                onClick = onTranslation
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "⚡",
                title = "Mobile Optimizer",
                description = "Device maintenance",
                action = "BOOST DEVICE",
                accent = DashboardTeal,
                animationDelayMillis = 160L,
                onClick = onOptimizer
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "📶",
                title = "Network Speed Test",
                description = "Download • Upload • Ping",
                action = "TEST NETWORK",
                accent = DashboardBlue,
                animationDelayMillis = 210L,
                onClick = onNetworkSpeed
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "🕵️",
                title = "NETHA X BLACKBOX",
                description = "Smart performance analysis",
                action = "ANALYZE DEVICE",
                accent = DashboardOrange,
                animationDelayMillis = 260L,
                onClick = onBlackbox
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "📁",
                title = "SMART FILE RESCUE",
                description = "Scan • Organize • Recover",
                action = "OPEN FILE RESCUE",
                accent = DashboardPurple,
                animationDelayMillis = 310L,
                onClick = onFileRescue
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "🎮",
                title = "GAMING FLOATING HUD",
                description = "FPS • Battery • Temperature",
                action = "OPEN HUD",
                accent = DashboardPink,
                animationDelayMillis = 360L,
                onClick = onGamingHud
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "🎯",
                title = "NETHA X AIM FRAME",
                description = "Adjustable static aim guide",
                action = "OPEN AIM FRAME",
                accent = DashboardTeal,
                animationDelayMillis = 410L,
                onClick = onAimFrame
            )
        }

        ExperiencePanel()

        Text(
            "NETHA X ULTIMATE  •  VERSION 2.0 NATIVE",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 8.dp),
            color = DashboardMuted.copy(alpha = 0.78f),
            fontSize = 9.sp,
            letterSpacing = 1.1.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.netha_logo),
                contentDescription = "NETHA X ULTIMATE Logo",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    "NETHA X",
                    color = DashboardInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "ULTIMATE",
                    color = DashboardPurple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.4.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFE7F8F0))
                .border(1.dp, Color(0xFFBCEBD4), CircleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Color(0xFF17B978), CircleShape)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "READY",
                color = Color(0xFF16875A),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun WelcomePanel() {
    val shape = RoundedCornerShape(25.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .shadow(14.dp, shape, spotColor = DashboardBlue.copy(alpha = 0.18f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFE3F2FF),
                        Color(0xFFF0EDFF),
                        Color(0xFFFFF5FC)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFFB9DDFF),
                        Color(0xFFD5C4FF),
                        Color(0xFFF4C8E7)
                    )
                ),
                shape
            )
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .graphicsLayer { translationX = 55f; translationY = (-55f) }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x667F9FFF), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Image(
            painter = painterResource(R.drawable.netha_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(118.dp)
                .graphicsLayer { alpha = 0.48f },
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.78f)
                .padding(start = 20.dp, end = 8.dp)
        ) {
            Text(
                "WELCOME BACK",
                color = DashboardBlue,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Your Ultimate",
                color = DashboardInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                "Digital Companion",
                color = DashboardPurple,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Boost  •  Optimize  •  Enjoy",
                color = DashboardMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class DeviceSnapshot(
    val deviceName: String,
    val androidVersion: String,
    val ramUsed: Long,
    val ramTotal: Long,
    val storageFree: Long,
    val storageTotal: Long,
    val batteryPercent: Int,
    val batteryTemperature: Float?
)

private fun readDeviceSnapshot(context: Context): DeviceSnapshot {
    val memoryInfo = ActivityManager.MemoryInfo()
    val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    activityManager?.getMemoryInfo(memoryInfo)

    val totalRam = memoryInfo.totalMem.coerceAtLeast(0L)
    val usedRam = (totalRam - memoryInfo.availMem).coerceAtLeast(0L)

    val storage = StatFs(Environment.getDataDirectory().absolutePath)
    val storageFree = storage.availableBytes.coerceAtLeast(0L)
    val storageTotal = storage.totalBytes.coerceAtLeast(0L)

    val batteryIntent = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )
    val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPercent = if (batteryLevel >= 0 && batteryScale > 0) {
        (batteryLevel * 100) / batteryScale
    } else {
        -1
    }

    val temperatureRaw =
        batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val batteryTemperature =
        if (temperatureRaw >= 0) temperatureRaw / 10f else null

    return DeviceSnapshot(
        deviceName = Build.MODEL.orEmpty().ifBlank { "Android Device" },
        androidVersion = Build.VERSION.RELEASE.orEmpty(),
        ramUsed = usedRam,
        ramTotal = totalRam,
        storageFree = storageFree,
        storageTotal = storageTotal,
        batteryPercent = batteryPercent,
        batteryTemperature = batteryTemperature
    )
}

private fun formatGigabytes(bytes: Long): String {
    val value = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f GB", value)
}

@Composable
private fun DeviceStatusPanel(snapshot: DeviceSnapshot) {
    val shape = RoundedCornerShape(23.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, spotColor = DashboardBlue.copy(alpha = 0.12f))
            .clip(shape)
            .background(DashboardCard)
            .border(1.dp, Color(0xFFDDE9FA), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "DEVICE STATUS",
                    color = DashboardInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    snapshot.deviceName + "  •  Android " + snapshot.androidVersion,
                    color = DashboardMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                "LIVE INFO",
                color = DashboardBlue,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceMetric(
                modifier = Modifier.weight(1f),
                icon = "▦",
                label = "RAM",
                value = formatGigabytes(snapshot.ramTotal),
                detail = "Used " + formatGigabytes(snapshot.ramUsed),
                accent = DashboardBlue
            )
            DeviceMetric(
                modifier = Modifier.weight(1f),
                icon = "▤",
                label = "STORAGE FREE",
                value = formatGigabytes(snapshot.storageFree),
                detail = "of " + formatGigabytes(snapshot.storageTotal),
                accent = DashboardPurple
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceMetric(
                modifier = Modifier.weight(1f),
                icon = "▣",
                label = "BATTERY",
                value = if (snapshot.batteryPercent >= 0) {
                    snapshot.batteryPercent.toString() + "%"
                } else {
                    "N/A"
                },
                detail = "Current level",
                accent = DashboardTeal
            )
            DeviceMetric(
                modifier = Modifier.weight(1f),
                icon = "℃",
                label = "BATTERY TEMP",
                value = snapshot.batteryTemperature?.let {
                    String.format(Locale.US, "%.1f°C", it)
                } ?: "N/A",
                detail = "Temperature",
                accent = DashboardPink
            )
        }
    }
}

@Composable
private fun DeviceMetric(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    detail: String,
    accent: Color
) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(accent.copy(alpha = 0.075f))
            .border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(15.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                icon,
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = DashboardMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            value,
            color = DashboardInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            detail,
            color = DashboardMuted,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            color = DashboardInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp
        )

        Text(
            detail,
            color = DashboardBlue,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    description: String,
    action: String,
    accent: Color,
    animationDelayMillis: Long = 0L,
    onClick: () -> Unit
) {
    var entryVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (animationDelayMillis > 0L) delay(animationDelayMillis)
        entryVisible = true
    }

    val entryAlpha by animateFloatAsState(
        targetValue = if (entryVisible) 1f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "feature-entry-alpha"
    )

    val entryOffset by animateFloatAsState(
        targetValue = if (entryVisible) 0f else 12f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "feature-entry-offset"
    )

    val entryScale by animateFloatAsState(
        targetValue = if (entryVisible) 1f else 0.97f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "feature-entry-scale"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(140),
        label = "feature-press-scale"
    )

    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .height(158.dp)
            .graphicsLayer {
                scaleX = pressScale * entryScale
                scaleY = pressScale * entryScale
                alpha = entryAlpha
                translationY = entryOffset
            }
            .shadow(8.dp, shape, spotColor = accent.copy(alpha = 0.16f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.13f),
                        Color.White,
                        accent.copy(alpha = 0.07f)
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.27f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = 0.13f))
                .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 21.sp)
        }

        Column {
            Text(
                title,
                color = DashboardInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            Text(
                description,
                color = DashboardMuted,
                fontSize = 8.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            action + "  →",
            color = accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.35.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExperiencePanel() {
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, spotColor = DashboardPurple.copy(alpha = 0.12f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFEAF5FF),
                        Color(0xFFF2EDFF),
                        Color(0xFFFFF1F8)
                    )
                )
            )
            .border(1.dp, Color(0xFFD8E3FA), shape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "BUILT FOR YOUR DEVICE",
                color = DashboardPurple,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Spacer(Modifier.height(5.dp))

            Text(
                "One app. More possibilities.",
                color = DashboardInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                "Explore your NETHA X tools.",
                color = DashboardMuted,
                fontSize = 10.sp
            )
        }

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(DashboardBlue, DashboardPurple)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "NX",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
        }
    }
}
