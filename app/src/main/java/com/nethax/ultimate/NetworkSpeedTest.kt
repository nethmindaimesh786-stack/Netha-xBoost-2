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


private data class RealNetworkSpeedResult(
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double
)

@Composable
private fun NetworkMetric(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            label,
            color = SoftText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            textAlign = TextAlign.Center
        )

        Text(
            value,
            color = color,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            unit,
            color = SoftText,
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun NetworkSpeedTestScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("READY TO TEST") }
    var status by remember {
        mutableStateOf("Press START REAL SPEED TEST to measure this connection.")
    }
    var progress by remember { mutableStateOf(0f) }
    var liveSpeed by remember { mutableStateOf(0.0) }
    var pingText by remember { mutableStateOf("--") }
    var jitterText by remember { mutableStateOf("--") }
    var downloadText by remember { mutableStateOf("--") }
    var uploadText by remember { mutableStateOf("--") }
    var downloadMbps by remember { mutableStateOf<Double?>(null) }

    val gaugeTarget = if (isRunning) {
        liveSpeed
    } else {
        downloadMbps ?: 0.0
    }

    val gaugeProgress by animateFloatAsState(
        targetValue = (gaugeTarget / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "network-speed-gauge"
    )

    ToolPage(
        title = "🌐 REAL NETWORK SPEED TEST",
        subtitle = "Live internet measurement • Download / Upload / Ping",
        onBack = onBack
    ) {
        PanelCard {
            Text(
                "LIVE CONNECTION SPEED",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFDCCBFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(164.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()

                        drawCircle(
                            color = Color(0xFF26304B),
                            style = Stroke(width = strokeWidth)
                        )

                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Purple, Cyan, Green, Purple)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * gaugeProgress,
                            useCenter = false,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when {
                                isRunning -> String.format("%.1f", liveSpeed)
                                downloadMbps != null -> String.format("%.1f", downloadMbps)
                                else -> "--"
                            },
                            color = MainText,
                            fontSize = 31.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            "Mbps",
                            color = Cyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            if (isRunning) "LIVE SPEED" else "DOWNLOAD",
                            color = SoftText,
                            fontSize = 8.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Text(
                phase,
                modifier = Modifier.fillMaxWidth(),
                color = if (phase == "TEST FAILED") {
                    Color(0xFFFF879A)
                } else {
                    Cyan
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            if (isRunning) {
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = Cyan,
                    trackColor = Color(0xFF29304B)
                )
            }

            StatusText(
                status,
                if (phase == "TEST FAILED") Color(0xFFFF879A) else SoftText
            )
        }

        PanelCard {
            Text(
                "TEST RESULTS",
                color = Color(0xFFDCCBFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NetworkMetric(
                    label = "PING",
                    value = pingText,
                    unit = "ms",
                    color = Cyan,
                    modifier = Modifier.weight(1f)
                )

                NetworkMetric(
                    label = "JITTER",
                    value = jitterText,
                    unit = "ms",
                    color = Purple,
                    modifier = Modifier.weight(1f)
                )

                NetworkMetric(
                    label = "DOWNLOAD",
                    value = downloadText,
                    unit = "Mbps",
                    color = Green,
                    modifier = Modifier.weight(1f)
                )

                NetworkMetric(
                    label = "UPLOAD",
                    value = uploadText,
                    unit = "Mbps",
                    color = Color(0xFFFFC66D),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        GradientActionButton(
            text = if (isRunning) "TEST IN PROGRESS..." else "⚡ START REAL SPEED TEST",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning
        ) {
            scope.launch {
                isRunning = true
                phase = "PREPARING TEST"
                status = "Connecting to the nearest available Cloudflare edge..."
                progress = 0f
                liveSpeed = 0.0
                pingText = "--"
                jitterText = "--"
                downloadText = "--"
                uploadText = "--"
                downloadMbps = null

                try {
                    val result = runRealNetworkSpeedTest(
                        onPhase = { nextPhase, nextProgress ->
                            withContext(Dispatchers.Main) {
                                phase = nextPhase
                                progress = nextProgress
                                status = when (nextPhase) {
                                    "MEASURING PING" -> "Sampling connection latency..."
                                    "TESTING DOWNLOAD" -> "Receiving real test data from the speed-test server..."
                                    "TESTING UPLOAD" -> "Sending test data to the speed-test server..."
                                    "TEST COMPLETED" -> "All measurements completed successfully."
                                    else -> nextPhase
                                }
                            }
                        },
                        onLiveSpeed = { speed, nextProgress ->
                            withContext(Dispatchers.Main) {
                                liveSpeed = speed
                                progress = nextProgress
                            }
                        }
                    )

                    pingText = String.format("%.0f", result.pingMs)
                    jitterText = String.format("%.0f", result.jitterMs)
                    downloadText = String.format("%.1f", result.downloadMbps)
                    uploadText = String.format("%.1f", result.uploadMbps)
                    downloadMbps = result.downloadMbps
                    phase = "TEST COMPLETED"
                    progress = 1f
                    status = "Real connection test completed. Results are estimates for this server route."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    phase = "TEST FAILED"
                    status = error.message
                        ?: "The speed test could not complete. Check your connection and try again."
                } finally {
                    isRunning = false
                    liveSpeed = 0.0
                }
            }
        }

        GradientActionButton(
            text = "RESET RESULTS",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning
        ) {
            phase = "READY TO TEST"
            status = "Press START REAL SPEED TEST to measure this connection."
            progress = 0f
            liveSpeed = 0.0
            pingText = "--"
            jitterText = "--"
            downloadText = "--"
            uploadText = "--"
            downloadMbps = null
        }

        PanelCard {
            Text(
                "HOW THIS TEST WORKS",
                color = Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                "• Uses real HTTPS requests to Cloudflare's speed-test edge.\n" +
                    "• Measures five latency samples and estimates jitter from their variation.\n" +
                    "• Measures download and upload throughput using transferred data.\n" +
                    "• Uses approximately 5 MB of test data per run (4 MB download + 1 MB upload).\n\n" +
                    "Ping is to the test server, not your Free Fire or other game server. Results can vary with signal strength, congestion, and server distance.",
                color = SoftText,
                fontSize = 10.sp,
                lineHeight = 16.sp
            )
        }
    }
}

private suspend fun runRealNetworkSpeedTest(
    onPhase: suspend (String, Float) -> Unit,
    onLiveSpeed: suspend (Double, Float) -> Unit
): RealNetworkSpeedResult {
    val pingSamples = mutableListOf<Long>()

    repeat(5) { index ->
        onPhase("MEASURING PING", (index / 5f) * 0.20f)
        pingSamples += measureCloudflarePingSample()
        onPhase("MEASURING PING", ((index + 1) / 5f) * 0.20f)
    }

    val averagePing = pingSamples.average()

    val jitter = if (pingSamples.size > 1) {
        pingSamples.zipWithNext { first, second ->
            kotlin.math.abs(second - first).toDouble()
        }.average()
    } else {
        0.0
    }

    onPhase("TESTING DOWNLOAD", 0.20f)

    val downloadSpeed = measureCloudflareDownload(4 * 1024 * 1024) {
            speed, fraction ->
        onLiveSpeed(speed, 0.20f + fraction * 0.45f)
    }

    onPhase("TESTING UPLOAD", 0.65f)

    val uploadSpeed = measureCloudflareUpload(1 * 1024 * 1024) {
            speed, fraction ->
        onLiveSpeed(speed, 0.65f + fraction * 0.33f)
    }

    onPhase("TEST COMPLETED", 1f)

    return RealNetworkSpeedResult(
        pingMs = averagePing,
        jitterMs = jitter,
        downloadMbps = downloadSpeed,
        uploadMbps = uploadSpeed
    )
}

private suspend fun measureCloudflarePingSample(): Long =
    withContext(Dispatchers.IO) {
        val url = URL(
            "https://speed.cloudflare.com/__down?bytes=0&n=${UUID.randomUUID()}"
        )

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Accept-Encoding", "identity")

            val startedAt = SystemClock.elapsedRealtime()
            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                throw IOException("Latency server returned HTTP $responseCode")
            }

            connection.inputStream.use { input ->
                val buffer = ByteArray(256)

                while (input.read(buffer) != -1) {
                    currentCoroutineContext().ensureActive()
                }
            }

            (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(1L)
        } finally {
            connection.disconnect()
        }
    }

private suspend fun measureCloudflareDownload(
    byteCount: Int,
    onProgress: suspend (Double, Float) -> Unit
): Double = withContext(Dispatchers.IO) {
    val url = URL(
        "https://speed.cloudflare.com/__down?bytes=$byteCount&n=${UUID.randomUUID()}"
    )

    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.useCaches = false
        connection.setRequestProperty("Cache-Control", "no-cache")
        connection.setRequestProperty("Accept-Encoding", "identity")

        val responseCode = connection.responseCode

        if (responseCode !in 200..299) {
            throw IOException("Download server returned HTTP $responseCode")
        }

        val startedAt = SystemClock.elapsedRealtime()
        var receivedBytes = 0L
        var lastUpdateAt = startedAt
        val buffer = ByteArray(16 * 1024)

        connection.inputStream.use { input ->
            while (true) {
                currentCoroutineContext().ensureActive()

                val read = input.read(buffer)
                if (read < 0) break

                receivedBytes += read

                val now = SystemClock.elapsedRealtime()
                val elapsedMs = (now - startedAt).coerceAtLeast(1L)

                val speed = receivedBytes * 8.0 /
                    (elapsedMs / 1000.0) / 1_000_000.0

                if (now - lastUpdateAt >= 120L) {
                    onProgress(
                        speed,
                        (receivedBytes.toFloat() / byteCount).coerceIn(0f, 1f)
                    )
                    lastUpdateAt = now
                }

                if (now - startedAt > 45_000L) {
                    throw IOException(
                        "Download test timed out. Try again with a stronger connection."
                    )
                }
            }
        }

        if (receivedBytes <= 0L) {
            throw IOException("The download test returned no data.")
        }

        val durationMs =
            (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(1L)

        val finalSpeed = receivedBytes * 8.0 /
            (durationMs / 1000.0) / 1_000_000.0

        onProgress(finalSpeed, 1f)
        finalSpeed
    } finally {
        connection.disconnect()
    }
}

private suspend fun measureCloudflareUpload(
    byteCount: Int,
    onProgress: suspend (Double, Float) -> Unit
): Double = withContext(Dispatchers.IO) {
    val url = URL(
        "https://speed.cloudflare.com/__up?n=${UUID.randomUUID()}"
    )

    val connection = url.openConnection() as HttpURLConnection

    val payload = ByteArray(byteCount) {
        index -> ((index * 31 + 17) and 0xFF).toByte()
    }

    try {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.doInput = true
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.useCaches = false
        connection.setRequestProperty(
            "Content-Type",
            "application/octet-stream"
        )
        connection.setRequestProperty("Cache-Control", "no-cache")
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.setFixedLengthStreamingMode(byteCount)

        val startedAt = SystemClock.elapsedRealtime()
        var sentBytes = 0
        var lastUpdateAt = startedAt

        connection.outputStream.use { output ->
            val chunkSize = 16 * 1024

            while (sentBytes < payload.size) {
                currentCoroutineContext().ensureActive()

                val length = minOf(chunkSize, payload.size - sentBytes)

                output.write(payload, sentBytes, length)
                sentBytes += length

                val now = SystemClock.elapsedRealtime()
                val elapsedMs = (now - startedAt).coerceAtLeast(1L)

                val speed = sentBytes * 8.0 /
                    (elapsedMs / 1000.0) / 1_000_000.0

                if (now - lastUpdateAt >= 120L) {
                    onProgress(
                        speed,
                        (sentBytes.toFloat() / byteCount).coerceIn(0f, 1f)
                    )
                    lastUpdateAt = now
                }

                if (now - startedAt > 45_000L) {
                    throw IOException(
                        "Upload test timed out. Try again with a stronger connection."
                    )
                }
            }
        }

        val responseCode = connection.responseCode

        if (responseCode !in 200..299) {
            throw IOException("Upload server returned HTTP $responseCode")
        }

        connection.inputStream.use { input ->
            val buffer = ByteArray(512)

            while (input.read(buffer) != -1) {
                currentCoroutineContext().ensureActive()
            }
        }

        val durationMs =
            (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(1L)

        val finalSpeed = sentBytes * 8.0 /
            (durationMs / 1000.0) / 1_000_000.0

        onProgress(finalSpeed, 1f)
        finalSpeed
    } finally {
        connection.disconnect()
    }
}
@Composable
