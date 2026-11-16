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

internal val Bg = Color(0xFF070713)
internal val Panel = Color(0xFF11182B)
internal val Purple = Color(0xFF9B5CFF)
internal val Cyan = Color(0xFF35EAFF)
internal val SoftText = Color(0xFFAAA9C1)
internal val MainText = Color(0xFFF7F4FF)
internal val Green = Color(0xFF00FFAE)
internal val PanelBorder = Color(0x665C6FAD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(7, 7, 19)
        window.navigationBarColor = android.graphics.Color.rgb(7, 7, 19)
        window.decorView.systemUiVisibility = 0

        setContent {
            NethaApp()
        }
    }
}

@Composable
private fun NethaApp() {
    var currentScreen by rememberSaveable { mutableStateOf("home") }
    var splashVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3700)
        splashVisible = false
    }

    BackHandler(enabled = !splashVisible && currentScreen != "home") {
        currentScreen = "home"
    }

    if (splashVisible) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackdrop()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                NativeSplashScreen()
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Bg
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppBackdrop()

                AnimatedContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    targetState = currentScreen,
                    label = "netha-screen-transition"
                ) { screen ->
                    when (screen) {
                        "background" -> BackgroundRemoverScreen(
                            onBack = { currentScreen = "home" }
                        )

                        "translation" -> TranslationCameraScreen(
                            onBack = { currentScreen = "home" }
                        )

                        "optimizer" -> MobileOptimizerScreen(
                            onBack = { currentScreen = "home" }
                        )

                        "network" -> NetworkSpeedTestScreen(
                            onBack = { currentScreen = "home" }
                        )

                        "blackbox" -> BlackboxScreen(
                            onBack = { currentScreen = "home" }
                        )

                        "file_rescue" -> SmartFileRescueScreen(
                            onBack = { currentScreen = "home" }
                        )

                        else -> DashboardScreen(
                            onBackground = { currentScreen = "background" },
                            onTranslation = { currentScreen = "translation" },
                            onOptimizer = { currentScreen = "optimizer" },
                            onNetworkSpeed = { currentScreen = "network" },
                            onBlackbox = { currentScreen = "blackbox" },
                            onFileRescue = { currentScreen = "file_rescue" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeSplashScreen() {
    val orbit = rememberInfiniteTransition(label = "splash-orbit")

    val rotation by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(
                15000,
                easing = androidx.compose.animation.core.LinearEasing
            )
        ),
        label = "splash-rotation"
    )

    val reverseRotation by orbit.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(
                12000,
                easing = androidx.compose.animation.core.LinearEasing
            )
        ),
        label = "splash-reverse-rotation"
    )

    val pulse by orbit.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(1250),
            RepeatMode.Reverse
        ),
        label = "splash-logo-pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(158.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .rotate(rotation)
                        .border(1.dp, Color(0x559B76FF), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 26.dp, y = 12.dp)
                            .size(9.dp)
                            .shadow(
                                8.dp,
                                CircleShape,
                                spotColor = Cyan
                            )
                            .background(Cyan, CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .rotate(reverseRotation)
                        .border(1.dp, Color(0x4439CAFF), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                        .shadow(
                            25.dp,
                            RoundedCornerShape(30.dp),
                            spotColor = Purple
                        )
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF8B38FF),
                                    Color(0xFF376EFF),
                                    Color(0xFF16DFFF)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color(0x99FFFFFF),
                            RoundedCornerShape(30.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "NX",
                        color = Color.White,
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    )
                }
            }

            Spacer(Modifier.height(34.dp))

            Text(
                "T H E   N E X T   G E N E R A T I O N",
                color = Color(0xFFA99BDA),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(13.dp))

            Text(
                "NETHA X",
                color = MainText,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                "ULTIMATE",
                color = Cyan,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "A NEW EXPERIENCE BEGINS",
                color = Color(0xFFAAA5C5),
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .width(95.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Purple,
                                Cyan,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Text(
            "D E S I G N E D   F O R   E X C E L L E N C E",
            color = Color(0xFF77728F),
            fontSize = 8.sp,
            letterSpacing = 1.4.sp
        )
    }
}

@Composable
private fun AppBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF100B25),
                        Bg,
                        Color(0xFF061B29),
                        Color(0xFF090817)
                    ),
                    start = Offset.Zero,
                    end = Offset(1100f, 1900f)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(310.dp)
                .align(Alignment.TopStart)
                .offset(x = (-95).dp, y = (-105).dp)
                .graphicsLayer { alpha = 0.28f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF9B5CFF),
                            Color(0x553F2A86),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 115.dp, y = 105.dp)
                .graphicsLayer { alpha = 0.25f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF00D9FF),
                            Color(0x44305E9B),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(230.dp)
                .align(Alignment.Center)
                .graphicsLayer { alpha = 0.09f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF655BFF),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
