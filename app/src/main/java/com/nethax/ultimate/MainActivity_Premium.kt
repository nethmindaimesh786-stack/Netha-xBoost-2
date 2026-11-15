package com.nethax.ultimate

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
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

private val Bg = Color(0xFF070713)
private val Panel = Color(0xFF11182B)
private val Purple = Color(0xFF9B5CFF)
private val Cyan = Color(0xFF35EAFF)
private val SoftText = Color(0xFFAAA9C1)
private val MainText = Color(0xFFF7F4FF)
private val Green = Color(0xFF00FFAE)
private val PanelBorder = Color(0x665C6FAD)

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

                        else -> DashboardScreen(
                            onBackground = { currentScreen = "background" },
                            onTranslation = { currentScreen = "translation" },
                            onOptimizer = { currentScreen = "optimizer" }
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
                        .border(
                            1.dp,
                            Color(0x559B76FF),
                            CircleShape
                        )
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
                        .border(
                            1.dp,
                            Color(0x4439CAFF),
                            CircleShape
                        )
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
        // Soft purple atmosphere.
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

        // Cyan atmosphere near the bottom.
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

        // A subtle central blue glow.
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
private fun DashboardScreen(
    onBackground: () -> Unit,
    onTranslation: () -> Unit,
    onOptimizer: () -> Unit
) {
    var dashboardVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dashboardVisible = true
    }

    val dashboardAlpha by animateFloatAsState(
        targetValue = if (dashboardVisible) 1f else 0f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "dashboard-entry-alpha"
    )

    val dashboardOffset by animateFloatAsState(
        targetValue = if (dashboardVisible) 0f else 18f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
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
            .padding(horizontal = 19.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(
                            14.dp,
                            RoundedCornerShape(14.dp),
                            spotColor = Purple
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Purple, Cyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "NX",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column {
                    Text(
                        "NETHA X",
                        color = MainText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )

                    Text(
                        "U L T I M A T E",
                        color = Color(0xFFAAA3C7),
                        fontSize = 9.sp,
                        letterSpacing = 2.3.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .shadow(
                        8.dp,
                        CircleShape,
                        spotColor = Green
                    )
                    .background(Green, CircleShape)
            )
        }

        WelcomePanel()

        Text(
            "YOUR TOOLS",
            color = Color(0xFFBDA5F9),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.3.sp
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
                action = "OPEN TOOL  →",
                animationDelayMillis = 80L,
                onClick = onBackground
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "🌐",
                title = "Translation Camera",
                description = "Live Camera Translator",
                action = "OPEN TOOL  →",
                animationDelayMillis = 170L,
                onClick = onTranslation
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "⚡",
                title = "Mobile Optimizer",
                description = "App Cache Cleaner",
                action = "BOOST DEVICE  →",
                animationDelayMillis = 260L,
                onClick = onOptimizer
            )

            Spacer(Modifier.weight(1f))
        }

        Text(
            "YOUR EXPERIENCE",
            color = Color(0xFFBDA5F9),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.3.sp
        )

        ExperiencePanel()

        Text(
            "NETHA X ULTIMATE  •  VERSION 2.0 NATIVE",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 18.dp),
            color = Color(0xFF716B8B),
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WelcomePanel() {
    val glowTransition = rememberInfiniteTransition(
        label = "welcome-panel-glow"
    )

    // Original animation and timing retained.
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "welcome-glow-alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(222.dp)
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(25.dp),
                spotColor = Purple
            )
            .clip(RoundedCornerShape(25.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF321B58),
                        Color(0xFF17284A),
                        Color(0xFF102C3B)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xCCB58AFF),
                        Color(0x8835EAFF),
                        Color(0x669B5CFF)
                    )
                ),
                shape = RoundedCornerShape(25.dp)
            )
            .padding(23.dp)
    ) {
        // Original animated glow layer.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glowAlpha }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x8835EAFF),
                            Color(0x2235EAFF),
                            Color.Transparent
                        )
                    )
                )
        )

        // Decorative upper-right lighting.
        Box(
            modifier = Modifier
                .size(145.dp)
                .align(Alignment.TopEnd)
                .offset(x = 45.dp, y = (-65).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x559B5CFF),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.CenterEnd)
                .offsetXCompat(50.dp)
                .border(
                    1.dp,
                    Color(0x55B58AFF),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(125.dp)
                .align(Alignment.CenterEnd)
                .offsetXCompat(27.dp)
                .border(
                    1.dp,
                    Color(0x4435EAFF),
                    CircleShape
                )
        )

        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                "W E L C O M E",
                color = Color(0xFFCFB6FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.8.sp
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "WELCOME TO",
                color = MainText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 31.sp
            )

            Text(
                "NETHA X",
                color = Cyan,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp
            )

            Spacer(Modifier.height(9.dp))

            Text(
                "Your next-level digital experience starts here.",
                color = Color(0xFFD0CBE3),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    description: String,
    action: String,
    animationDelayMillis: Long = 0L,
    onClick: () -> Unit
) {
    var entryVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (animationDelayMillis > 0L) {
            delay(animationDelayMillis)
        }
        entryVisible = true
    }

    // Original entry animations retained.
    val entryAlpha by animateFloatAsState(
        targetValue = if (entryVisible) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "feature-entry-alpha"
    )

    val entryOffset by animateFloatAsState(
        targetValue = if (entryVisible) 0f else 16f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "feature-entry-offset"
    )

    val entryScale by animateFloatAsState(
        targetValue = if (entryVisible) 1f else 0.96f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "feature-entry-scale"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Original tap animation retained.
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(160),
        label = "feature-press-scale"
    )

    Column(
        modifier = modifier
            .height(160.dp)
            .graphicsLayer {
                scaleX = pressScale * entryScale
                scaleY = pressScale * entryScale
                alpha = entryAlpha
                translationY = entryOffset
            }
            .shadow(
                elevation = 13.dp,
                shape = RoundedCornerShape(19.dp),
                spotColor = Purple
            )
            .clip(RoundedCornerShape(19.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF2A1B49),
                        Color(0xFF142C43),
                        Color(0xFF102331)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xAAAE82FF),
                        Color(0x7735EAFF),
                        Color(0x669B5CFF)
                    )
                ),
                shape = RoundedCornerShape(19.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 9.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(43.dp)
                .shadow(
                    9.dp,
                    RoundedCornerShape(13.dp),
                    spotColor = Cyan
                )
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0x889B5CFF),
                            Color(0x6635EAFF),
                            Color(0x4435EAFF)
                        )
                    )
                )
                .border(
                    1.dp,
                    Color(0x889BCAFF),
                    RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 21.sp)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            title,
            color = Color(0xFFE8DCFF),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            description,
            color = Color(0xFFBDBBD0),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(Modifier.height(7.dp))

        Text(
            action,
            color = Cyan,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
    }
}

@Composable
private fun ExperiencePanel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .shadow(
                elevation = 17.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Cyan
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF142C43),
                        Color(0xFF1B1538),
                        Color(0xFF101D32)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0x8835EAFF),
                        Color(0x779B5CFF),
                        Color(0x5535EAFF)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-95).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x4435EAFF),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 95.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x449B5CFF),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Canvas(modifier = Modifier.size(132.dp)) {
            drawCircle(
                Color(0x7735EAFF),
                style = Stroke(width = 1.2.dp.toPx())
            )

            drawCircle(
                Color(0x669B5CFF),
                radius = size.minDimension * 0.36f,
                style = Stroke(width = 1.2.dp.toPx())
            )

            drawCircle(
                Color(0x2235EAFF),
                radius = size.minDimension * 0.23f
            )
        }

        Text(
            "NX",
            color = Cyan,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp
        )
    }
}

@Composable
private fun ToolPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    var pageVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        pageVisible = true
    }

    val pageAlpha by animateFloatAsState(
        targetValue = if (pageVisible) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "tool-page-entry-alpha"
    )

    val pageOffset by animateFloatAsState(
        targetValue = if (pageVisible) 0f else 14f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "tool-page-entry-offset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = pageAlpha
                translationY = pageOffset
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            GradientActionButton(
                text = "‹",
                modifier = Modifier.width(48.dp),
                onClick = onBack
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color(0xFFD7C3FF),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    subtitle,
                    color = SoftText,
                    fontSize = 10.sp
                )
            }
        }

        content()

        Text(
            "NETHA X ULTIMATE  •  NATIVE MODE",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
            color = Color(0xFF716B8B),
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center
        )
    }
}

/*
 * FIXED:
 * ColumnScope receiver added to the content parameter.
 * This allows Modifier.align() inside PanelCard content.
 */
@Composable
private fun PanelCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Purple
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xF2222440),
                        Color(0xF2142639),
                        Color(0xF2112030)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0x779B78FF),
                        Color(0x5535EAFF),
                        Color(0x665C6FAD)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun GradientActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Original tap scale and 150 ms timing retained.
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(150),
        label = "button-press-scale"
    )

    Box(
        modifier = modifier
            .heightInCompat(45.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 13.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (enabled) Cyan else Color.Transparent
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFAD65FF),
                            Color(0xFF5575FF),
                            Color(0xFF00CFFF)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF55516D),
                            Color(0xFF45475B)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xCCFFFFFF),
                        Color(0x667DEBFF),
                        Color(0x669B5CFF)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = 17.dp,
                vertical = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatusText(
    text: String,
    color: Color = Cyan
) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BackgroundRemoverScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var resultBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isRemoving by remember { mutableStateOf(false) }

    var status by remember {
        mutableStateOf("Select a photo to get started.")
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImage = uri
        resultBytes = null

        status = if (uri != null) {
            "Photo selected successfully!"
        } else {
            "No photo selected."
        }
    }

    ToolPage(
        title = "✂️ AI BACKGROUND REMOVER",
        subtitle = "Remove photo backgrounds automatically",
        onBack = onBack
    ) {
        PanelCard {
            Text(
                "SELECT YOUR PHOTO",
                color = Color(0xFFDCCBFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (selectedImage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFF10152A))
                        .border(
                            BorderStroke(1.dp, Purple),
                            RoundedCornerShape(15.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📸", fontSize = 34.sp)

                        Text(
                            "Select an image from your device",
                            color = SoftText,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = selectedImage,
                    contentDescription = "Selected photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF10152A)),
                    contentScale = ContentScale.Fit
                )
            }

            GradientActionButton(
                "SELECT PHOTO",
                modifier = Modifier.fillMaxWidth()
            ) {
                imagePicker.launch("image/*")
            }

            if (selectedImage != null) {
                GradientActionButton(
                    text = if (isRemoving) {
                        "PROCESSING..."
                    } else {
                        "✂ REMOVE BACKGROUND"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRemoving
                ) {
                    val uri = selectedImage

                    if (uri != null) {
                        val apiKey = BuildConfig.POOF_API_KEY.trim()

                        if (
                            apiKey.isBlank() ||
                            apiKey == "PASTE_YOUR_NEW_POOF_API_KEY_HERE"
                        ) {
                            status =
                                "POOF API KEY NOT SET. Add POOF_API_KEY to your GitHub Actions secret or local Gradle property."
                        } else {
                            scope.launch {
                                isRemoving = true
                                status = "AI IS REMOVING THE BACKGROUND..."
                                resultBytes = null

                                try {
                                    resultBytes = withContext(Dispatchers.IO) {
                                        requestBackgroundRemoval(
                                            context,
                                            uri,
                                            apiKey
                                        )
                                    }

                                    status = "SUCCESS! BACKGROUND REMOVED."
                                } catch (error: Exception) {
                                    status =
                                        "BACKGROUND REMOVAL FAILED: ${error.message ?: "Please try again."}"
                                } finally {
                                    isRemoving = false
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isRemoving,
                enter = fadeIn(animationSpec = tween(180))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Cyan,
                    trackColor = Color(0xFF29304B)
                )
            }
        }

        AnimatedVisibility(
            visible = resultBytes != null,
            enter = fadeIn(animationSpec = tween(380)) +
                expandVertically(animationSpec = tween(380))
        ) {
            val imageBytes = resultBytes

            if (imageBytes != null) {
                PanelCard {
                    Text(
                        "BACKGROUND REMOVED",
                        color = Cyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    AsyncImage(
                        model = imageBytes,
                        contentDescription = "Background removed result",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )

                    GradientActionButton(
                        "⬇ SAVE PNG TO GALLERY",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        try {
                            val saved = saveImageToGallery(
                                context,
                                imageBytes
                            )

                            status = saved
                        } catch (error: Exception) {
                            status =
                                "SAVE FAILED: ${error.message ?: "Unknown error"}"
                        }
                    }
                }
            }
        }

        StatusText(status)

        Text(
            "API ACCESS NOTE: A valid Poof API key is required. Keep your key private; a key embedded in an APK can be extracted by others.",
            color = Color(0xFFB4AFC8),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        )
    }
}

private fun requestBackgroundRemoval(
    context: Context,
    imageUri: Uri,
    apiKey: String
): ByteArray {
    val resolver = context.contentResolver

    val mimeType = resolver.getType(imageUri) ?: "image/jpeg"

    val fileName = resolver.query(
        imageUri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getString(0)
        } else {
            null
        }
    } ?: "netha-upload-image"

    val imageBytes = resolver.openInputStream(imageUri)?.use { input ->
        readLimited(input, 20 * 1024 * 1024)
    } ?: throw IOException("Cannot read the selected image")

    if (imageBytes.isEmpty()) {
        throw IOException("Selected image is empty")
    }

    val boundary =
        "----NethaX${UUID.randomUUID().toString().replace("-", "")}"

    val connection = (
        URL("https://api.poof.bg/v1/remove")
            .openConnection() as HttpURLConnection
        ).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 20_000
        readTimeout = 90_000

        setRequestProperty("x-api-key", apiKey)

        setRequestProperty(
            "Content-Type",
            "multipart/form-data; boundary=$boundary"
        )

        setRequestProperty(
            "Accept",
            "image/*, application/json"
        )
    }

    try {
        DataOutputStream(connection.outputStream).use { output ->
            output.writeBytes("--$boundary\r\n")

            output.writeBytes(
                "Content-Disposition: form-data; name=\"file\"; filename=\"${fileName.replace("\"", "")}\"\r\n"
            )

            output.writeBytes("Content-Type: $mimeType\r\n\r\n")

            output.write(imageBytes)

            output.writeBytes("\r\n--$boundary--\r\n")

            output.flush()
        }

        val responseCode = connection.responseCode

        val responseStream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val responseBytes = responseStream?.use {
            readLimited(it, 30 * 1024 * 1024)
        } ?: throw IOException("The API returned an empty response")

        if (responseCode !in 200..299) {
            val responseText = responseBytes.toString(Charsets.UTF_8)

            val message = try {
                val json = JSONObject(responseText)

                json.optJSONObject("error")
                    ?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: json.optString("message")
                        .takeIf { it.isNotBlank() }
                    ?: "API request failed ($responseCode)"
            } catch (_: Exception) {
                responseText
                    .take(180)
                    .ifBlank {
                        "API request failed ($responseCode)"
                    }
            }

            throw IOException(message)
        }

        val contentType = connection.contentType.orEmpty()

        if (contentType.contains("application/json", ignoreCase = true)) {
            val json = JSONObject(
                responseBytes.toString(Charsets.UTF_8)
            )

            val resultUrl = json.optString("result_url")

            if (!resultUrl.startsWith("https://")) {
                val message = json.optJSONObject("error")
                    ?.optString("message")
                    ?: json.optString(
                        "message",
                        "API did not return a secure result image URL"
                    )

                throw IOException(message)
            }

            return downloadImageResult(resultUrl)
        }

        if (!contentType.startsWith("image/", ignoreCase = true)) {
            throw IOException("The API response was not an image")
        }

        return responseBytes
    } finally {
        connection.disconnect()
    }
}

private fun downloadImageResult(resultUrl: String): ByteArray {
    val connection = (
        URL(resultUrl).openConnection() as HttpURLConnection
        ).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 60_000
    }

    try {
        if (connection.responseCode !in 200..299) {
            throw IOException("Could not download the result image")
        }

        val type = connection.contentType.orEmpty()

        if (!type.startsWith("image/", ignoreCase = true)) {
            throw IOException("Result URL did not return an image")
        }

        return connection.inputStream.use {
            readLimited(it, 30 * 1024 * 1024)
        }
    } finally {
        connection.disconnect()
    }
}

private fun readLimited(
    input: java.io.InputStream,
    maxBytes: Int
): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)

    var total = 0

    while (true) {
        val count = input.read(buffer)

        if (count < 0) break

        total += count

        if (total > maxBytes) {
            throw IOException("File is too large")
        }

        output.write(buffer, 0, count)
    }

    return output.toByteArray()
}

private fun saveImageToGallery(
    context: Context,
    imageBytes: ByteArray
): String {
    val resolver = context.contentResolver

    val fileName = "NETHA-${System.currentTimeMillis()}.png"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                fileName
            )

            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/png"
            )

            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/NETHA X"
            )

            put(
                MediaStore.Images.Media.IS_PENDING,
                1
            )
        }

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IOException("Gallery file could not be created")

        try {
            resolver.openOutputStream(uri)?.use {
                it.write(imageBytes)
            } ?: throw IOException("Cannot write image to Gallery")

            val completed = ContentValues().apply {
                put(
                    MediaStore.Images.Media.IS_PENDING,
                    0
                )
            }

            resolver.update(uri, completed, null, null)

            return "Image saved to Gallery / Pictures / NETHA X"
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    val directory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "NETHA X"
    )

    if (!directory.exists() && !directory.mkdirs()) {
        throw IOException("App picture folder could not be created")
    }

    File(directory, fileName).writeBytes(imageBytes)

    return "Image saved in the app Pictures/NETHA X folder"
}

@Composable
private fun TranslationCameraScreen(onBack: () -> Unit) {
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

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val processing =
        AtomicBoolean(false)

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

    /*
     * FIXED:
     * ImageAnalysis.Analyzer does not declare close().
     * This is a normal cleanup function called from DisposableEffect.
     */
    fun close() {
        isClosed = true

        recognizer.close()

        languageIdentifier.close()

        translationExecutor.shutdownNow()
    }
}

@Composable
private fun MobileOptimizerScreen(onBack: () -> Unit) {
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
                "• Removes temporary cache files belonging to NETHA X ULTIMATE.\n• Does not delete your photos, documents, or personal files.\n• Android does not allow ordinary apps to silently clear other apps' private caches.",
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
): Modifier =
    this.offset(x = value)

private fun Modifier.heightInCompat(
    value: androidx.compose.ui.unit.Dp
): Modifier =
    this.height(value)
