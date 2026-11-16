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


internal fun DashboardScreen(
    onBackground: () -> Unit,
    onTranslation: () -> Unit,
    onOptimizer: () -> Unit,
    onNetworkSpeed: () -> Unit,
    onBlackbox: () -> Unit,
    onFileRescue: () -> Unit
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
            .padding(
                start = 19.dp,
                end = 19.dp,
                top = 32.dp,
                bottom = 22.dp
            ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "⚡",
                title = "Mobile Optimizer",
                description = "App Cache Cleaner",
                action = "BOOST DEVICE  →",
                animationDelayMillis = 260L,
                onClick = onOptimizer
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "🌐",
                title = "Real Network Speed Test",
                description = "Live Download • Upload • Ping",
                action = "TEST NETWORK  →",
                animationDelayMillis = 350L,
                onClick = onNetworkSpeed
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "🕵️",
                title = "NETHA X BLACKBOX",
                description = "Smart Performance Detective",
                action = "ANALYZE DEVICE  →",
                animationDelayMillis = 440L,
                onClick = onBlackbox
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = "📁",
                title = "SMART FILE RESCUE",
                description = "Scan • Organize • Recover",
                action = "OPEN FILE RESCUE  →",
                animationDelayMillis = 520L,
                onClick = onFileRescue
            )
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
                .border(1.dp, Color(0x55B58AFF), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(125.dp)
                .align(Alignment.CenterEnd)
                .offsetXCompat(27.dp)
                .border(1.dp, Color(0x4435EAFF), CircleShape)
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
            .clip(RoundedCornerShape(19.dp))
            .background(Color(0xFF172536))
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
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF21445A))
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
