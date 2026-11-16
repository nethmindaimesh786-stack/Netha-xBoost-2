package com.nethax.ultimate

import android.Manifest
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Html
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


internal data class BlackboxSample(
    val elapsedSeconds: Long,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val thermalLabel: String,
    val thermalLevel: Int,
    val networkLabel: String
)

private data class BlackboxLagMarker(
    val elapsedSeconds: Long,
    val sample: BlackboxSample
)

internal fun readBlackboxSample(
    context: Context,
    elapsedSeconds: Long
): BlackboxSample {
    val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val availableRamMb =
        (memoryInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)

    val totalRamMb =
        (memoryInfo.totalMem / (1024L * 1024L)).coerceAtLeast(0L)

    var thermalLabel = "NOT AVAILABLE"
    var thermalLevel = 0

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val powerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager

            thermalLevel = powerManager.currentThermalStatus

            thermalLabel = when (thermalLevel) {
                PowerManager.THERMAL_STATUS_NONE -> "NORMAL"
                PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
                PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
                PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
                else -> "UNKNOWN"
            }
        } catch (_: Exception) {
            thermalLabel = "NOT AVAILABLE"
            thermalLevel = 0
        }
    }

    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    val networkLabel = try {
        val activeNetwork = connectivityManager.activeNetwork

        if (activeNetwork == null) {
            "DISCONNECTED"
        } else {
            val capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork)

            if (capabilities == null) {
                "UNKNOWN"
            } else {
                val hasInternet =
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )

                val validated =
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )

                val transport = when {
                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_WIFI
                    ) -> "Wi-Fi"

                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_CELLULAR
                    ) -> "Mobile Data"

                    capabilities.hasTransport(
                        NetworkCapabilities.TRANSPORT_ETHERNET
                    ) -> "Ethernet"

                    else -> "Other Network"
                }

                when {
                    !hasInternet -> "$transport • NO INTERNET"
                    !validated -> "$transport • NOT VERIFIED"
                    else -> "$transport • CONNECTED"
                }
            }
        }
    } catch (_: Exception) {
        "UNKNOWN"
    }

    return BlackboxSample(
        elapsedSeconds = elapsedSeconds,
        availableRamMb = availableRamMb,
        totalRamMb = totalRamMb,
        thermalLabel = thermalLabel,
        thermalLevel = thermalLevel,
        networkLabel = networkLabel
    )
}

@Composable
internal fun BlackboxScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var isRecording by remember {
        mutableStateOf(false)
    }

    var sessionStartedAt by remember {
        mutableStateOf(0L)
    }

    var elapsedSeconds by remember {
        mutableStateOf(0L)
    }

    var latestSample by remember {
        mutableStateOf<BlackboxSample?>(null)
    }

    val samples = remember {
        androidx.compose.runtime.mutableStateListOf<BlackboxSample>()
    }

    val lagMarkers = remember {
        androidx.compose.runtime.mutableStateListOf<BlackboxLagMarker>()
    }

    var sessionFinished by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf("READY • Start a session to monitor device conditions.")
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                val elapsed = (
                    SystemClock.elapsedRealtime() - sessionStartedAt
                    ).coerceAtLeast(0L) / 1000L

                elapsedSeconds = elapsed

                val sample = readBlackboxSample(
                    context = context,
                    elapsedSeconds = elapsed
                )

                latestSample = sample

                if (samples.size < 600) {
                    samples.add(sample)
                }

                delay(2000L)
            }
        }
    }

    val current = latestSample

    val ramUsedProgress = current?.let {
        if (it.totalRamMb > 0L) {
            (
                1f - it.availableRamMb.toFloat() /
                    it.totalRamMb.toFloat()
                ).coerceIn(0f, 1f)
        } else {
            0f
        }
    } ?: 0f

    val ramUsedMb = current?.let {
        (it.totalRamMb - it.availableRamMb).coerceAtLeast(0L)
    } ?: 0L

    val ramColor = when {
        ramUsedProgress >= 0.90f -> Color(0xFFFF879A)
        ramUsedProgress >= 0.75f -> Color(0xFFFFC66D)
        else -> Green
    }

    val diagnosis = remember(samples.size, lagMarkers.size, latestSample) {
        buildBlackboxDiagnosis(samples.toList(), lagMarkers.toList())
    }

    ToolPage(
        title = "🕵️ NETHA X BLACKBOX",
        subtitle = "Smart Performance Detective • Session Diagnostics",
        onBack = {
            isRecording = false
            onBack()
        }
    ) {
        PanelCard {
            Text(
                "LIVE DEVICE MONITOR",
                color = Color(0xFFDCCBFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                if (isRecording) "● SESSION RECORDING" else "● MONITOR STANDBY",
                color = if (isRecording) Green else SoftText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "AVAILABLE RAM",
                    color = SoftText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    current?.let { "${it.availableRamMb} MB" } ?: "--",
                    color = Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            LinearProgressIndicator(
                progress = ramUsedProgress,
                modifier = Modifier.fillMaxWidth(),
                color = ramColor,
                trackColor = Color(0xFF29304B)
            )

            Text(
                current?.let {
                    "Estimated RAM in use: $ramUsedMb MB / ${it.totalRamMb} MB"
                } ?: "RAM information will appear when monitoring starts.",
                color = SoftText,
                fontSize = 10.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "THERMAL STATUS",
                    color = SoftText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    current?.thermalLabel ?: "--",
                    color = when {
                        (current?.thermalLevel ?: 0) >= 4 ->
                            Color(0xFFFF879A)

                        (current?.thermalLevel ?: 0) >= 2 ->
                            Color(0xFFFFC66D)

                        else -> Green
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "NETWORK",
                    color = SoftText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    current?.networkLabel ?: "--",
                    color = Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "Session time: ${formatSessionTime(elapsedSeconds)}",
                color = MainText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            StatusText(status, if (isRecording) Green else SoftText)
        }

        PanelCard {
            Text(
                "GAMING SESSION RECORDER",
                color = Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                "Records RAM availability, Android thermal status and network connectivity every 2 seconds while this screen is active.",
                color = SoftText,
                fontSize = 10.sp,
                lineHeight = 16.sp
            )

            GradientActionButton(
                text = if (isRecording) {
                    "■ STOP SESSION"
                } else {
                    "▶ START SESSION"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            ) {
                if (!isRecording) {
                    samples.clear()
                    lagMarkers.clear()
                    elapsedSeconds = 0L
                    sessionStartedAt = SystemClock.elapsedRealtime()
                    sessionFinished = false
                    latestSample = readBlackboxSample(context, 0L)
                    samples.add(latestSample!!)
                    isRecording = true
                    status = "RECORDING • Use MARK LAG when you notice a slowdown."
                } else {
                    isRecording = false
                    sessionFinished = true
                    status = "SESSION STOPPED • ${samples.size} samples recorded."
                }
            }

            GradientActionButton(
                text = "⚑ MARK LAG EVENT",
                modifier = Modifier.fillMaxWidth(),
                enabled = isRecording && current != null
            ) {
                val sample = latestSample

                if (sample != null) {
                    lagMarkers.add(
                        BlackboxLagMarker(
                            elapsedSeconds = elapsedSeconds,
                            sample = sample
                        )
                    )

                    status =
                        "LAG EVENT MARKED AT ${formatSessionTime(elapsedSeconds)}"
                }
            }

            Text(
                "Recorded samples: ${samples.size} / 600    •    Lag markers: ${lagMarkers.size}",
                color = SoftText,
                fontSize = 10.sp
            )
        }

        PanelCard {
            Text(
                "SMART DIAGNOSIS",
                color = Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                diagnosis,
                color = MainText,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )

            if (lagMarkers.isNotEmpty()) {
                Text(
                    "LAG EVENTS",
                    color = Color(0xFFFFC66D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                lagMarkers.takeLast(8).forEachIndexed { index, marker ->
                    Text(
                        "${index + 1}. ${formatSessionTime(marker.elapsedSeconds)}  •  RAM available: ${marker.sample.availableRamMb} MB  •  Thermal: ${marker.sample.thermalLabel}",
                        color = SoftText,
                        fontSize = 10.sp,
                        lineHeight = 16.sp
                    )
                }
            } else {
                Text(
                    "No lag events marked yet.",
                    color = SoftText,
                    fontSize = 10.sp
                )
            }
        }

        GradientActionButton(
            text = "↗ SHARE DIAGNOSTIC REPORT",
            modifier = Modifier.fillMaxWidth(),
            enabled = samples.isNotEmpty() && !isRecording
        ) {
            val report = buildBlackboxReport(
                samples = samples.toList(),
                markers = lagMarkers.toList(),
                elapsedSeconds = elapsedSeconds
            )

            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "NETHA X BLACKBOX Diagnostic Report"
                    )
                    putExtra(Intent.EXTRA_TEXT, report)
                }

                context.startActivity(
                    Intent.createChooser(
                        sendIntent,
                        "Share BLACKBOX Report"
                    )
                )
            } catch (error: Exception) {
                status =
                    "Could not open share menu: ${error.message ?: "Unknown error"}"
            }
        }

        GradientActionButton(
            text = "RESET SESSION",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRecording
        ) {
            samples.clear()
            lagMarkers.clear()
            latestSample = null
            elapsedSeconds = 0L
            sessionFinished = false
            status = "READY • Start a new session to monitor device conditions."
        }

        PanelCard {
            Text(
                "IMPORTANT MEASUREMENT LIMITS",
                color = Color(0xFFFFC66D),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "• RAM is the Android system's available-memory reading, not the exact memory usage of a selected game.\n" +
                    "• Thermal status comes from Android's device thermal API and may not expose every sensor temperature.\n" +
                    "• Network status shows connection availability/type, not real-time game ping or per-app traffic.\n" +
                    "• Android does not provide ordinary apps with another game's internal FPS or GPU readings.\n" +
                    "• Diagnosis shows possible contributing conditions, not guaranteed causes of lag.",
                color = SoftText,
                fontSize = 10.sp,
                lineHeight = 16.sp
            )
        }
    }
}

private fun buildBlackboxDiagnosis(
    samples: List<BlackboxSample>,
    markers: List<BlackboxLagMarker>
): String {
    if (samples.isEmpty()) {
        return "No session data available. Start a session and record some samples before running diagnosis."
    }

    if (markers.isEmpty()) {
        return "No lag events were marked. Record a session and press MARK LAG when you notice a slowdown. BLACKBOX can then compare the captured device conditions around those moments."
    }

    val findings = mutableListOf<String>()

    markers.forEachIndexed { index, marker ->
        val sample = marker.sample
        val totalRam = sample.totalRamMb.coerceAtLeast(1L)
        val availablePercent =
            sample.availableRamMb.toFloat() / totalRam.toFloat()

        val issues = mutableListOf<String>()

        if (
            sample.thermalLevel >= PowerManager.THERMAL_STATUS_MODERATE
        ) {
            issues.add(
                "Thermal status was ${sample.thermalLabel}; heat-related performance limitation is possible."
            )
        }

        if (
            sample.availableRamMb < 500L ||
            availablePercent < 0.10f
        ) {
            issues.add(
                "Available system RAM was low (${sample.availableRamMb} MB); memory pressure may have contributed."
            )
        }

        if (
            sample.networkLabel.contains("DISCONNECTED") ||
            sample.networkLabel.contains("NO INTERNET") ||
            sample.networkLabel.contains("NOT VERIFIED")
        ) {
            issues.add(
                "Network connectivity was not fully verified; a connection issue may have contributed to online-game lag."
            )
        }

        if (issues.isEmpty()) {
            issues.add(
                "No clear RAM, thermal or connectivity warning was captured at this marker. The cause may be outside the measurements available to this app."
            )
        }

        findings.add(
            "Lag marker ${index + 1} at ${formatSessionTime(marker.elapsedSeconds)}:\n" +
                issues.joinToString(separator = "\n") { "• $it" }
        )
    }

    return findings.joinToString(separator = "\n\n")
}

private fun buildBlackboxReport(
    samples: List<BlackboxSample>,
    markers: List<BlackboxLagMarker>,
    elapsedSeconds: Long
): String {
    val report = StringBuilder()

    report.appendLine("NETHA X BLACKBOX — DIAGNOSTIC REPORT")
    report.appendLine("===================================")
    report.appendLine("Session duration: ${formatSessionTime(elapsedSeconds)}")
    report.appendLine("Samples recorded: ${samples.size}")
    report.appendLine("Lag markers: ${markers.size}")
    report.appendLine()

    if (samples.isNotEmpty()) {
        val latest = samples.last()

        report.appendLine("LATEST DEVICE SAMPLE")
        report.appendLine("Available RAM: ${latest.availableRamMb} MB")
        report.appendLine("Total RAM: ${latest.totalRamMb} MB")
        report.appendLine("Thermal status: ${latest.thermalLabel}")
        report.appendLine("Network: ${latest.networkLabel}")
        report.appendLine()
    }

    report.appendLine("SMART DIAGNOSIS")
    report.appendLine(
        buildBlackboxDiagnosis(samples, markers)
    )

    report.appendLine()
    report.appendLine("SAMPLE HISTORY")
    samples.takeLast(30).forEach { sample ->
        report.appendLine(
            "${formatSessionTime(sample.elapsedSeconds)} | " +
                "RAM available ${sample.availableRamMb} MB | " +
                "Thermal ${sample.thermalLabel} | " +
                "Network ${sample.networkLabel}"
        )
    }

    report.appendLine()
    report.appendLine(
        "Note: This report contains Android system-level observations. It does not measure another app's internal FPS, GPU load, or game-server ping."
    )

    return report.toString()
}

private fun formatSessionTime(seconds: Long): String {
    val minutes = seconds / 60L
    val remainingSeconds = seconds % 60L

    return "%02d:%02d".format(minutes, remainingSeconds)
}
