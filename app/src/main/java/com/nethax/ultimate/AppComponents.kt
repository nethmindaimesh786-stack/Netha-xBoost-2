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


internal fun ToolPage(
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

@Composable
internal fun PanelCard(
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
internal fun GradientActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

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
            .padding(horizontal = 17.dp, vertical = 10.dp),
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
internal fun StatusText(
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

// Small modifier helpers keep the layout declarations easy to scan.

internal fun Modifier.offsetXCompat(
    value: androidx.compose.ui.unit.Dp
): Modifier = this.offset(x = value)

internal fun Modifier.heightInCompat(
    value: androidx.compose.ui.unit.Dp
): Modifier = this.height(value)


// ------------------------------------------------------------
