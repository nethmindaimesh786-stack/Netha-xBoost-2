package com.nethax.ultimate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

private data class RescueFile(
    val uri: String,
    val parentUri: String,
    val name: String,
    val mime: String,
    val size: Long,
    val hash: String
)

private data class RescueTrash(
    val id: String,
    val name: String,
    val mime: String,
    val parentUri: String,
    val backup: String,
    val size: Long
)

private fun rescueBinFolder(context: Context): File =
    File(context.filesDir, "smart_file_rescue_bin").apply { mkdirs() }

private fun loadRescueBin(context: Context): List<RescueTrash> = try {
    val array = JSONArray(
        context.getSharedPreferences("smart_file_rescue", Context.MODE_PRIVATE)
            .getString("bin", "[]")
    )
    (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        RescueTrash(
            item.optString("id"),
            item.optString("name"),
            item.optString("mime"),
            item.optString("parent"),
            item.optString("backup"),
            item.optLong("size")
        )
    }.filter { File(it.backup).exists() }
} catch (_: Exception) {
    emptyList()
}

private fun saveRescueBin(context: Context, items: List<RescueTrash>) {
    val array = JSONArray()
    items.forEach { item ->
        array.put(JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("mime", item.mime)
            put("parent", item.parentUri)
            put("backup", item.backup)
            put("size", item.size)
        })
    }
    context.getSharedPreferences("smart_file_rescue", Context.MODE_PRIVATE)
        .edit().putString("bin", array.toString()).apply()
}

private fun rescueHash(context: Context, uri: Uri): String? {
    val digest = MessageDigest.getInstance("SHA-256")
    val input = context.contentResolver.openInputStream(uri) ?: return null
    input.use { stream ->
        val buffer = ByteArray(65536)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun scanRescueFolder(context: Context, treeUri: Uri): List<RescueFile> {
    val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
    val rootDocument = DocumentsContract.buildDocumentUriUsingTree(
        treeUri, DocumentsContract.getTreeDocumentId(treeUri)
    )
    val queue = java.util.ArrayDeque<Pair<DocumentFile, Uri>>()
    queue.add(root to rootDocument)
    val results = mutableListOf<RescueFile>()
    var visited = 0

    while (queue.isNotEmpty() && visited < 20000) {
        val (directory, parentUri) = queue.removeFirst()
        val children = try { directory.listFiles() } catch (_: Exception) { emptyArray() }
        for (child in children) {
            visited++
            if (visited >= 20000) break
            if (child.isDirectory) {
                queue.add(child to child.uri)
            } else if (child.isFile) {
                val hash = try { rescueHash(context, child.uri) } catch (_: Exception) { null }
                results.add(
                    RescueFile(
                        child.uri.toString(),
                        parentUri.toString(),
                        child.name ?: "Unnamed file",
                        child.type ?: "application/octet-stream",
                        child.length().coerceAtLeast(0L),
                        hash ?: ""
                    )
                )
            }
        }
    }
    return results
}

private fun rescueFormatSize(bytes: Long): String = when {
    bytes < 1024L -> bytes.toString() + " B"
    bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}

private fun rescueCategory(file: RescueFile): String {
    val name = file.name.lowercase()
    return when {
        file.mime.startsWith("image/") -> "Images"
        file.mime.startsWith("video/") -> "Videos"
        file.mime.startsWith("audio/") -> "Audio"
        file.mime.contains("pdf") ||
            listOf(".doc", ".docx", ".txt", ".xls", ".xlsx", ".ppt", ".pptx").any { name.endsWith(it) } -> "Documents"
        listOf(".zip", ".rar", ".7z", ".apk").any { name.endsWith(it) } -> "Archives / APK"
        else -> "Other"
    }
}

private fun rescueCopy(context: Context, source: Uri, target: Uri): Boolean {
    return try {
        val input = context.contentResolver.openInputStream(source) ?: return false
        val output = context.contentResolver.openOutputStream(target, "w") ?: return false
        input.use { from -> output.use { to -> from.copyTo(to) } }
        true
    } catch (_: Exception) {
        false
    }
}

private fun rescueBackupAndDelete(context: Context, item: RescueFile): RescueTrash? {
    val backup = File(rescueBinFolder(context), UUID.randomUUID().toString() + ".bin")
    return try {
        val input = context.contentResolver.openInputStream(Uri.parse(item.uri)) ?: return null
        input.use { from -> backup.outputStream().use { to -> from.copyTo(to) } }
        val original = DocumentFile.fromSingleUri(context, Uri.parse(item.uri))
        if (original?.delete() != true) {
            backup.delete()
            return null
        }
        RescueTrash(
            UUID.randomUUID().toString(), item.name, item.mime,
            item.parentUri, backup.absolutePath, item.size
        )
    } catch (_: Exception) {
        backup.delete()
        null
    }
}

private fun restoreRescueFile(context: Context, item: RescueTrash): Boolean {
    return try {
        val newUri = DocumentsContract.createDocument(
            context.contentResolver, Uri.parse(item.parentUri), item.mime, item.name
        ) ?: return false
        val input = File(item.backup).inputStream()
        val output = context.contentResolver.openOutputStream(newUri, "w") ?: return false
        input.use { from -> output.use { to -> from.copyTo(to) } }
        File(item.backup).delete()
        true
    } catch (_: Exception) {
        false
    }
}

@Composable
internal fun SmartFileRescueScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folderUri by rememberSaveable { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var movingFiles by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose a folder to scan.") }
    val files = remember { mutableStateListOf<RescueFile>() }
    val selected = remember { mutableStateListOf<String>() }
    val recycleBin = remember {
        mutableStateListOf<RescueTrash>().apply { addAll(loadRescueBin(context)) }
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) { }
            folderUri = uri.toString()
            files.clear()
            selected.clear()
            status = "Folder selected. Press SCAN FOLDER."
        }
    }

    val destinationPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { destination ->
        if (destination != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    destination,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) { }
            scope.launch {
                working = true
                val chosen = files.filter { it.uri in selected }
                val result = withContext(Dispatchers.IO) {
                    val target = DocumentFile.fromTreeUri(context, destination)
                        ?: return@withContext Pair(0, 0)
                    var copied = 0
                    var failed = 0
                    chosen.forEach { item ->
                        val created = target.createFile(item.mime, item.name)
                        if (created != null && rescueCopy(
                                context, Uri.parse(item.uri), created.uri
                            )
                        ) {
                            if (movingFiles) {
                                DocumentFile.fromSingleUri(context, Uri.parse(item.uri))?.delete()
                            }
                            copied++
                        } else {
                            created?.delete()
                            failed++
                        }
                    }
                    Pair(copied, failed)
                }
                if (movingFiles && result.first > 0) {
                    val moved = chosen.take(result.first).map { it.uri }.toSet()
                    files.removeAll { it.uri in moved }
                    selected.removeAll { it in moved }
                }
                status = "Finished: " + result.first + " completed, " + result.second + " failed."
                working = false
            }
        }
    }

    val duplicates = files.filter { it.hash.isNotBlank() }
        .groupBy { it.hash }.values.filter { it.size > 1 }
    val largeFiles = files.filter { it.size >= 100L * 1024L * 1024L }
        .sortedByDescending { it.size }
    val categories = files.groupBy { rescueCategory(it) }

    ToolPage(
        title = "📁 SMART FILE RESCUE",
        subtitle = "Folder scanner • Duplicate cleaner • Smart organizer",
        onBack = onBack
    ) {
        PanelCard {
            Text("SAFE FOLDER ACCESS", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "Scans only folders you select through Android. No broad all-files permission is requested.",
                color = SoftText, fontSize = 10.sp
            )
            GradientActionButton(
                if (folderUri.isBlank()) "📂 CHOOSE FOLDER" else "📂 CHANGE FOLDER",
                Modifier.fillMaxWidth(), enabled = !scanning && !working
            ) { folderPicker.launch(null) }
            GradientActionButton(
                if (scanning) "SCANNING..." else "🔎 SCAN SELECTED FOLDER",
                Modifier.fillMaxWidth(), enabled = folderUri.isNotBlank() && !scanning && !working
            ) {
                scope.launch {
                    scanning = true
                    status = "Reading files and checking SHA-256..."
                    val found = withContext(Dispatchers.IO) {
                        scanRescueFolder(context, Uri.parse(folderUri))
                    }
                    files.clear()
                    files.addAll(found)
                    selected.clear()
                    status = "Scan complete: " + found.size + " files. Maximum 20,000 entries per scan."
                    scanning = false
                }
            }
            StatusText(status)
        }

        PanelCard {
            Text("SMART ORGANIZER / OVERVIEW", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Files: " + files.size + "  •  Selected: " + selected.size +
                "  •  Duplicate groups: " + duplicates.size, color = MainText, fontSize = 12.sp)
            Text("Large files (100 MB+): " + largeFiles.size, color = SoftText, fontSize = 10.sp)
            categories.forEach { entry ->
                Text(
                    entry.key + ": " + entry.value.size + " files • " +
                        rescueFormatSize(entry.value.sumOf { it.size }),
                    color = SoftText, fontSize = 10.sp
                )
            }
        }

        PanelCard {
            Text("DUPLICATE FILE CLEANER", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "Duplicates are matched by SHA-256 content hash, not filenames alone. Select which copies to remove in the file list.",
                color = SoftText, fontSize = 10.sp
            )
            duplicates.take(25).forEachIndexed { index, group ->
                Text(
                    (index + 1).toString() + ". " + group.first().name + " • " +
                        group.size + " matching copies • " + rescueFormatSize(group.first().size) + " each",
                    color = MainText, fontSize = 10.sp
                )
            }
            if (duplicates.isEmpty()) Text("No duplicate files detected.", color = SoftText, fontSize = 10.sp)
        }

        PanelCard {
            Text("LARGE FILE CLEANUP", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            largeFiles.take(25).forEach {
                Text(it.name + " • " + rescueFormatSize(it.size), color = MainText, fontSize = 10.sp)
            }
            if (largeFiles.isEmpty()) Text("No files over 100 MB found.", color = SoftText, fontSize = 10.sp)
        }

        PanelCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FILE SCANNER / BATCH SELECT", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("SELECT ALL", color = Green, fontSize = 9.sp, modifier = Modifier.clickable {
                    if (selected.size == files.size) selected.clear()
                    else {
                        selected.clear()
                        selected.addAll(files.map { it.uri })
                    }
                })
            }
            files.take(150).forEach { item ->
                val checked = item.uri in selected
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (checked) Color(0x332F9EC2) else Color.Transparent)
                        .clickable {
                            if (checked) selected.remove(item.uri) else selected.add(item.uri)
                        }.padding(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (checked) "☑" else "□", color = if (checked) Green else SoftText)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = MainText, fontSize = 10.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(rescueCategory(item) + " • " + rescueFormatSize(item.size),
                            color = SoftText, fontSize = 9.sp)
                    }
                }
            }
            if (files.size > 150) Text(
                "Showing first 150 entries. SELECT ALL still selects every scanned file.",
                color = SoftText, fontSize = 9.sp
            )
        }

        PanelCard {
            Text("BATCH FILE OPERATIONS", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(selected.size.toString() + " file(s) selected.", color = SoftText, fontSize = 10.sp)
            GradientActionButton("COPY SELECTED TO FOLDER", Modifier.fillMaxWidth(),
                enabled = selected.isNotEmpty() && !working && !scanning) {
                movingFiles = false
                destinationPicker.launch(null)
            }
            GradientActionButton("MOVE SELECTED TO FOLDER", Modifier.fillMaxWidth(),
                enabled = selected.isNotEmpty() && !working && !scanning) {
                movingFiles = true
                destinationPicker.launch(null)
            }
            GradientActionButton("DELETE SELECTED TO RECYCLE BIN", Modifier.fillMaxWidth(),
                enabled = selected.isNotEmpty() && !working && !scanning) {
                confirmDelete = true
            }
            if (confirmDelete) {
                Text(
                    "Confirm deleting " + selected.size +
                        " selected files? A private backup is created first.",
                    color = Color(0xFFFFC66D), fontSize = 10.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientActionButton("CANCEL", Modifier.weight(1f)) {
                        confirmDelete = false
                    }
                    GradientActionButton("CONFIRM", Modifier.weight(1f)) {
                        scope.launch {
                            working = true
                            confirmDelete = false
                            val chosen = files.filter { it.uri in selected }
                            val result = withContext(Dispatchers.IO) {
                                val saved = mutableListOf<RescueTrash>()
                                val removedUris = mutableSetOf<String>()
                                chosen.forEach { item ->
                                    val savedItem = rescueBackupAndDelete(context, item)
                                    if (savedItem != null) {
                                        saved.add(savedItem)
                                        removedUris.add(item.uri)
                                    }
                                }
                                Pair(saved, removedUris)
                            }
                            recycleBin.addAll(result.first)
                            saveRescueBin(context, recycleBin.toList())
                            files.removeAll { it.uri in result.second }
                            selected.removeAll { it in result.second }
                            status = result.first.size.toString() + " file(s) backed up in Recycle Bin."
                            working = false
                        }
                    }
                }
            }
        }

        PanelCard {
            Text("APP RECYCLE BIN", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "Only files deleted through this tool can be restored. App uninstall or clearing app data removes these private backups.",
                color = SoftText, fontSize = 10.sp
            )
            recycleBin.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = MainText, fontSize = 10.sp, maxLines = 2)
                        Text(rescueFormatSize(item.size), color = SoftText, fontSize = 9.sp)
                    }
                    GradientActionButton("RESTORE", Modifier.width(88.dp), enabled = !working) {
                        scope.launch {
                            working = true
                            val restored = withContext(Dispatchers.IO) {
                                restoreRescueFile(context, item)
                            }
                            if (restored) {
                                recycleBin.remove(item)
                                saveRescueBin(context, recycleBin.toList())
                                status = item.name + " restored."
                            } else {
                                status = "Restore failed. The original folder may no longer be available."
                            }
                            working = false
                        }
                    }
                }
            }
            if (recycleBin.isEmpty()) Text("Recycle Bin is empty.", color = SoftText, fontSize = 10.sp)
        }

        PanelCard {
            Text("IMPORTANT NOTES", color = Color(0xFFFFC66D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "• Only user-granted folders are scanned.\n" +
                    "• SHA-256 checks read file contents and may take time.\n" +
                    "• Recycle Bin is not system-wide recovery.\n" +
                    "• Files deleted outside this app cannot be restored here.",
                color = SoftText, fontSize = 10.sp
            )
        }
    }
}
