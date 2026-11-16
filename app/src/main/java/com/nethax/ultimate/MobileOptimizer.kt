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


internal fun MobileOptimizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRunning by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf(
            "Ready. Press START CACHE CLEANING to continue."
        )
    }

    var headline by remember {
        mutableStateOf("INITIALIZING CLEANER")
    }

    var detail by remember {
        mutableStateOf(
            "This tool clears NETHA X temporary cache files."
        )
    }

    var succeeded by remember {
        mutableStateOf(false)
    }

    val infinite = rememberInfiniteTransition(
        label = "optimizer-energy"
    )

    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(
                1100,
                easing = FastOutSlowInEasing
            ),
            RepeatMode.Reverse
        ),
        label = "optimizer-pulse"
    )

    ToolPage(
        title = "⚡ MOBILE OPTIMIZER",
        subtitle = "Native cache cleanup and device guidance",
        onBack = onBack
    ) {
        PanelCard {
            Text(
                "🧹",
                fontSize = 30.sp,
                modifier = Modifier.align(
                    Alignment.CenterHorizontally
                )
            )

            Text(
                "APP CACHE CLEANER",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFDCCBFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.2.sp
            )

            StatusText(
                status,
                if (succeeded) Green else Cyan
            )
        }

        PanelCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(145.dp)
                        .graphicsLayer {
                            scaleX = if (isRunning) pulse else 1f
                            scaleY = if (isRunning) pulse else 1f
                        }
                ) {
                    drawCircle(
                        Color(0x229B5CFF),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    drawCircle(
                        Color(0x6635EAFF),
                        radius = size.minDimension * 0.39f,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Purple,
                                Cyan,
                                Green,
                                Purple
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = if (isRunning) 270f else 230f,
                        useCenter = false,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                Text(
                    if (isRunning) {
                        "⚡"
                    } else if (succeeded) {
                        "✓"
                    } else {
                        "NX"
                    },
                    color = if (succeeded) Green else Cyan,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                headline,
                modifier = Modifier.fillMaxWidth(),
                color = if (succeeded) Green else MainText,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Text(
                detail,
                modifier = Modifier.fillMaxWidth(),
                color = SoftText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            AnimatedVisibility(
                visible = isRunning,
                enter = fadeIn(animationSpec = tween(180))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Cyan,
                    trackColor = Color(0xFF29304B)
                )
            }
        }

        GradientActionButton(
            text = if (isRunning) {
                "PLEASE WAIT..."
            } else {
                "⚡ START CACHE CLEANING"
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning
        ) {
            scope.launch {
                isRunning = true
                succeeded = false

                headline = "SYSTEM CLEANING"

                detail =
                    "Removing temporary files created by NETHA X..."

                status = "CLEANING APP CACHE..."

                val start = SystemClock.elapsedRealtime()

                try {
                    val freedBytes = withContext(Dispatchers.IO) {
                        clearAppOwnedCache(context)
                    }

                    val remaining =
                        5_000L - (
                            SystemClock.elapsedRealtime() - start
                        )

                    if (remaining > 0) {
                        delay(remaining)
                    }

                    succeeded = true

                    headline = "CLEANING COMPLETED"

                    detail =
                        "Freed approximately ${formatBytes(freedBytes)} of NETHA X temporary cache."

                    status =
                        "NETHA X APP CACHE CLEANED SUCCESSFULLY."
                } catch (error: Exception) {
                    val remaining =
                        5_000L - (
                            SystemClock.elapsedRealtime() - start
                        )

                    if (remaining > 0) {
                        delay(remaining)
                    }

                    headline = "CLEANING FAILED"

                    detail =
                        error.message ?: "Unable to clean app cache."

                    status = detail
                } finally {
                    isRunning = false
                }
            }
        }

        GradientActionButton(
            "RESET",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning
        ) {
            succeeded = false

            headline = "INITIALIZING CLEANER"

            detail =
                "This tool clears NETHA X temporary cache files."

            status =
                "Ready. Press START CACHE CLEANING to continue."
        }

        PanelCard {
            Text(
                "WHAT THIS TOOL DOES",
                color = Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                "• Removes temporary cache files belonging to NETHA X ULTIMATE.\n" +
                    "• Does not delete your photos, documents, or personal files.\n" +
                    "• Android does not allow ordinary apps to silently clear other apps' private caches.",
                color = SoftText,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
        }

        Text(
            "Note: Clearing cache does not guarantee higher gaming FPS. Avoid granting broad file access for a cache-cleaning feature.",
            color = Color(0xFFB4AFC8),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun clearAppOwnedCache(context: Context): Long {
    val cacheDirectories = listOfNotNull(
        context.cacheDir,
        context.externalCacheDir
    )

    val before = cacheDirectories.sumOf { directory ->
        directorySize(directory)
    }

    cacheDirectories.forEach { directory ->
        directory.listFiles()?.forEach { child ->
            if (!child.deleteRecursively() && child.exists()) {
                throw IOException(
                    "Could not remove ${child.name}"
                )
            }
        }
    }

    val after = cacheDirectories.sumOf { directory ->
        directorySize(directory)
    }

    return (before - after).coerceAtLeast(0L)
}

private fun directorySize(file: File): Long {
    if (!file.exists()) return 0L

    if (file.isFile) return file.length()

    return file.listFiles()?.sumOf {
        directorySize(it)
    } ?: 0L
}

private fun formatBytes(value: Long): String {
    if (value < 1024) {
        return "$value B"
    }

    val kb = value / 1024.0

    if (kb < 1024) {
        return "%.1f KB".format(kb)
    }

    return "%.2f MB".format(kb / 1024.0)
}

// Small modifier helpers keep the layout declarations easy to scan.

private fun Modifier.offsetXCompat(
    value: androidx.compose.ui.unit.Dp
): Modifier = this.offset(x = value)

private fun Modifier.heightInCompat(
    value: androidx.compose.ui.unit.Dp
): Modifier = this.height(value)


// ------------------------------------------------------------
// NETHA X BLACKBOX — SMART PERFORMANCE DETECTIVE
// ------------------------------------------------------------
