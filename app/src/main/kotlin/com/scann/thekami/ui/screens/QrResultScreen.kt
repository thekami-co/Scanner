package com.scann.thekami.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.scann.thekami.ui.components.bottomNavClearance
import com.scann.thekami.ui.components.bottomNavHeight
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// QR card: outer = inner + padding (12 + 24 = 36)
private val QrContainerPadding = 24.dp
private val QrInnerRadius = 12.dp
private val QrContainerRadius = QrInnerRadius + QrContainerPadding

// Shared radius so Save/Share match each other and the rest of the app
private val ActionButtonRadius = 18.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrResultScreen(
    qrBitmap: Bitmap,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val imageBitmap = remember(qrBitmap) { qrBitmap.asImageBitmap() }

    BackHandler(onBack = onBack)

    fun showMessage(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun saveQrToGallery() {
        try {
            val filename = "Scann_QR_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Scann")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    showMessage("Saved to Pictures/Scann")
                } else {
                    showMessage("Failed to save QR code")
                }
            } else {
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Scann"
                )
                picturesDir.mkdirs()
                FileOutputStream(File(picturesDir, filename)).use { out ->
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                showMessage("Saved to Pictures/Scann")
            }
        } catch (e: Exception) {
            showMessage("Failed to save: ${e.message}")
        }
    }

    // Android 9 and below need WRITE_EXTERNAL_STORAGE to save into Pictures
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) saveQrToGallery() else showMessage("Storage permission is needed to save")
    }

    fun onSaveClick() {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

        if (needsPermission) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveQrToGallery()
        }
    }

    fun shareQr() {
        try {
            val shareDir = File(context.cacheDir, "qr_share").apply { mkdirs() }
            val file = File(shareDir, "scann_qr.png")
            FileOutputStream(file).use { out ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share QR Code"))
        } catch (e: Exception) {
            showMessage("Couldn't share QR code")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        // The bottom nav bar overlays this screen, so lift the snackbar above it
        snackbarHost = {
            SnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = bottomNavHeight()))
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .padding(bottom = bottomNavClearance()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // QR codes need a light quiet zone, so the card stays white in dark mode too
            Surface(
                shape = RoundedCornerShape(QrContainerRadius),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = imageBitmap,
                        filterQuality = FilterQuality.None,
                        contentDescription = "Generated QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(QrContainerPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = ::onSaveClick,
                    shape = RoundedCornerShape(ActionButtonRadius),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
                Button(
                    onClick = ::shareQr,
                    shape = RoundedCornerShape(ActionButtonRadius),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}
