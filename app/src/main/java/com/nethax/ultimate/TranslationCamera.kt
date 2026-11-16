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


@Composable
internal fun TranslationCameraScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraAlreadyGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    var cameraGranted by remember {
        mutableStateOf(cameraAlreadyGranted)
    }

    var cameraRunning by rememberSaveable {
        mutableStateOf(false)
    }

    var detectedText by remember {
        mutableStateOf("Point your camera at printed text.")
    }

    var translatedText by remember {
        mutableStateOf("සිංහල පරිවර්තනය මෙතැන පෙන්වනු ඇත.")
    }

    var status by remember {
        mutableStateOf("CAMERA NOT STARTED")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted

        if (granted) {
            cameraRunning = true
            status = "STARTING CAMERA..."
        } else {
            cameraRunning = false
            status = "CAMERA PERMISSION DENIED"
        }
    }

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(
        cameraRunning,
        cameraGranted,
        lifecycleOwner,
        previewView
    ) {
        var cameraProvider: ProcessCameraProvider? = null
        var imageAnalysis: ImageAnalysis? = null
        var textAnalyzer: TextFrameAnalyzer? = null

        val cameraExecutor: ExecutorService =
            Executors.newSingleThreadExecutor()

        var disposed = false

        if (cameraRunning && cameraGranted) {
            status = "CONNECTING TO CAMERA..."

            val providerFuture =
                ProcessCameraProvider.getInstance(context)

            providerFuture.addListener(
                {
                    if (disposed) return@addListener

                    try {
                        cameraProvider = providerFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.surfaceProvider =
                                    previewView.surfaceProvider
                            }

                        val analyzer = TextFrameAnalyzer(
                            onDetected = { detected ->
                                detectedText = detected
                            },
                            onTranslated = { translated ->
                                translatedText = translated
                            },
                            onStatus = { message ->
                                status = message
                            }
                        )

                        textAnalyzer = analyzer

                        val analysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                            )
                            .build()

                        imageAnalysis = analysis

                        analysis.setAnalyzer(
                            cameraExecutor,
                            analyzer
                        )

                        cameraProvider?.unbindAll()

                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )

                        status = "SCANNING TEXT..."
                    } catch (error: Exception) {
                        status =
                            "CAMERA ERROR: ${error.message ?: "Unable to start"}"

                        cameraRunning = false
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

        onDispose {
            disposed = true

            imageAnalysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            textAnalyzer?.close()
            cameraExecutor.shutdown()
        }
    }

    ToolPage(
        title = "🌐 TRANSLATION CAMERA",
        subtitle = "Live camera text recognition and Sinhala translation",
        onBack = {
            cameraRunning = false
            onBack()
        }
    ) {
        PanelCard {
            Text(
                "LIVE CAMERA PREVIEW",
                color = Color(0xFFDCCBFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF080D1A))
                    .border(
                        1.dp,
                        Color(0x779B5CFF),
                        RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (cameraRunning && cameraGranted) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📷", fontSize = 36.sp)

                        Text(
                            "Press START to activate camera",
                            color = SoftText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GradientActionButton(
                    "▶ START",
                    modifier = Modifier.weight(1f),
                    enabled = !cameraRunning
                ) {
                    if (cameraGranted) {
                        cameraRunning = true
                    } else {
                        permissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                }

                GradientActionButton(
                    "■ STOP",
                    modifier = Modifier.weight(1f),
                    enabled = cameraRunning
                ) {
                    cameraRunning = false
                    status = "CAMERA STOPPED"
                }
            }

            StatusText(status)
        }

        PanelCard {
            Text(
                "DETECTED TEXT",
                color = Color(0xFFDCCBFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Text(
                detectedText,
                color = MainText,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        PanelCard {
            Text(
                "SINHALA TRANSLATION",
                color = Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Text(
                translatedText,
                color = MainText,
                fontSize = 14.sp,
                lineHeight = 23.sp
            )
        }

        Text(
            "Text recognition runs on-device with Google ML Kit. Translation requires an internet connection.",
            color = SoftText,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private class TextFrameAnalyzer(
    private val onDetected: (String) -> Unit,
    private val onTranslated: (String) -> Unit,
    private val onStatus: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    private val languageIdentifier: LanguageIdentifier =
        LanguageIdentification.getClient()

    private val translationExecutor =
        Executors.newSingleThreadExecutor()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val processing = AtomicBoolean(false)

    @Volatile
    private var lastFrameAt = 0L

    @Volatile
    private var lastDetected = ""

    @Volatile
    private var isClosed = false

    override fun analyze(imageProxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()

        if (
            isClosed ||
            now - lastFrameAt < 2500L ||
            !processing.compareAndSet(false, true)
        ) {
            imageProxy.close()
            return
        }

        lastFrameAt = now

        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            processing.set(false)
            imageProxy.close()
            return
        }

        try {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    if (isClosed) return@addOnSuccessListener

                    val text = result.text.trim()

                    if (text.isBlank()) {
                        postStatus("LOOKING FOR TEXT...")
                    } else {
                        postDetected(text)

                        if (text != lastDetected) {
                            lastDetected = text

                            languageIdentifier.identifyLanguage(text)
                                .addOnSuccessListener { language ->
                                    if (isClosed) {
                                        return@addOnSuccessListener
                                    }

                                    if (
                                        language.isBlank() ||
                                        language == "und"
                                    ) {
                                        postStatus(
                                            "LANGUAGE COULD NOT BE DETECTED"
                                        )
                                    } else if (
                                        language.substringBefore('-')
                                            .equals("si", ignoreCase = true)
                                    ) {
                                        postTranslated(text)

                                        postStatus(
                                            "TEXT IS ALREADY IN SINHALA"
                                        )
                                    } else {
                                        postStatus(
                                            "TRANSLATING TO SINHALA..."
                                        )

                                        translateInBackground(
                                            text,
                                            language
                                        )
                                    }
                                }
                                .addOnFailureListener { error ->
                                    postStatus(
                                        "LANGUAGE DETECTION FAILED: ${error.message ?: "Try again"}"
                                    )
                                }
                        } else {
                            postStatus("TEXT DETECTED — NO CHANGE")
                        }
                    }
                }
                .addOnFailureListener { error ->
                    postStatus(
                        "OCR ERROR: ${error.message ?: "TRY AGAIN"}"
                    )
                }
                .addOnCompleteListener {
                    processing.set(false)
                    imageProxy.close()
                }
        } catch (error: Exception) {
            processing.set(false)
            imageProxy.close()

            postStatus(
                "FRAME ERROR: ${error.message ?: "TRY AGAIN"}"
            )
        }
    }

    private fun translateInBackground(
        text: String,
        language: String
    ) {
        translationExecutor.execute {
            try {
                val sourceLanguage =
                    language.substringBefore('-').lowercase()

                if (
                    text.toByteArray(Charsets.UTF_8).size > 450
                ) {
                    throw IOException(
                        "Text is too long. Focus on a shorter text."
                    )
                }

                val encodedText =
                    URLEncoder.encode(text, "UTF-8")

                val encodedPair =
                    URLEncoder.encode("$sourceLanguage|si", "UTF-8")

                val connection = (
                    URL(
                        "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$encodedPair"
                    ).openConnection() as HttpURLConnection
                    ).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000

                    setRequestProperty(
                        "User-Agent",
                        "NETHA-X-ULTIMATE"
                    )
                }

                try {
                    val responseCode = connection.responseCode

                    val responseText = (
                        if (responseCode in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream
                        }
                        )?.bufferedReader()?.use {
                        it.readText()
                    } ?: throw IOException(
                        "Empty translation response"
                    )

                    if (responseCode !in 200..299) {
                        throw IOException(
                            "Translation service error: $responseCode"
                        )
                    }

                    val json = JSONObject(responseText)

                    if (json.optInt("responseStatus") != 200) {
                        throw IOException(
                            json.optString(
                                "responseDetails",
                                "Translation unavailable"
                            )
                        )
                    }

                    val translated = json
                        .optJSONObject("responseData")
                        ?.optString("translatedText")
                        ?.takeIf { it.isNotBlank() }
                        ?: throw IOException(
                            "No translated text returned"
                        )

                    val clean = if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    ) {
                        Html.fromHtml(
                            translated,
                            Html.FROM_HTML_MODE_LEGACY
                        ).toString()
                    } else {
                        @Suppress("DEPRECATION")
                        Html.fromHtml(translated).toString()
                    }

                    postTranslated(clean)
                    postStatus("TRANSLATION COMPLETE")
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                postStatus(
                    "TRANSLATION FAILED: ${error.message ?: "Check internet connection"}"
                )
            }
        }
    }

    private fun postDetected(value: String) =
        mainHandler.post {
            if (!isClosed) onDetected(value)
        }

    private fun postTranslated(value: String) =
        mainHandler.post {
            if (!isClosed) onTranslated(value)
        }

    private fun postStatus(value: String) =
        mainHandler.post {
            if (!isClosed) onStatus(value)
        }

    fun close() {
        isClosed = true
        recognizer.close()
        languageIdentifier.close()
        translationExecutor.shutdownNow()
    }
}

