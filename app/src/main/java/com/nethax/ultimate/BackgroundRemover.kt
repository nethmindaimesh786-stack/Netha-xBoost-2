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
internal fun BackgroundRemoverScreen(onBack: () -> Unit) {
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

