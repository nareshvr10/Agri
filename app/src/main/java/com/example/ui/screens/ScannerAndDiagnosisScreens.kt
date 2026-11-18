package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.models.AppLanguage
import com.example.models.CropScanRecord
import com.example.models.CropType
import com.example.models.CropZone
import com.example.models.Farm
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.cropLabel
import com.example.ui.components.formatEpochShort
import com.example.ui.theme.HarvestAmberContainer
import com.example.viewmodel.ScannerUiState
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CropImageScannerScreen(
    selectedFarm: Farm?,
    zones: List<CropZone>,
    scannerState: ScannerUiState,
    language: AppLanguage,
    onAnalyze: (crop: CropType, bitmap: Bitmap, imageUri: String, farm: Farm?, zone: CropZone?) -> Unit,
    onOpenResults: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedCrop by remember(selectedFarm) {
        mutableStateOf(selectedFarm?.resolvedCrop() ?: CropType.PADDY)
    }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageUriString by remember { mutableStateOf("") }
    var showCameraPreview by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val farmZones = selectedFarm?.let { f -> zones.filter { it.farmId == f.id } }.orEmpty()
    val targetZone = farmZones.firstOrNull { it.resolvedCrop() == selectedCrop } ?: farmZones.firstOrNull()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) {
                capturedBitmap = bmp
                imageUriString = uri.toString()
                showCameraPreview = false
                cameraError = null
            } else {
                cameraError = "Unable to decode selected image."
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCameraPreview = true
            cameraError = null
        } else {
            cameraError = "Camera permission denied. Use Photo Picker to select a leaf image."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("crop_scanner_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = HarvestAmberContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ai_preliminary_disclaimer),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text("Select Crop Type", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CropType.entries.forEach { crop ->
                FilterChip(
                    selected = crop == selectedCrop,
                    onClick = { selectedCrop = crop },
                    label = { Text(cropLabel(crop, language)) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) showCameraPreview = !showCameraPreview
                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (showCameraPreview) "Hide Camera" else "CameraX")
            }
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_crop_photo_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Photo Picker")
            }
        }

        if (cameraError != null) {
            Text(cameraError.orEmpty(), color = MaterialTheme.colorScheme.error)
        }

        if (showCameraPreview) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                val previewView = remember { PreviewView(context) }
                DisposableEffect(lifecycleOwner) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    val executor = ContextCompat.getMainExecutor(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val capture = ImageCapture.Builder().build()
                            imageCapture = capture
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraX", "Camera bind failed", e)
                            cameraError = "Camera unavailable on this device; use Photo Picker."
                        }
                    }, executor)
                    onDispose {
                        try {
                            cameraProviderFuture.get().unbindAll()
                        } catch (_: Exception) {
                        }
                    }
                }
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        val file = File(context.cacheDir, "crop_scan_${System.currentTimeMillis()}.jpg")
                        val options = ImageCapture.OutputFileOptions.Builder(file).build()
                        capture.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                                    if (bmp != null) {
                                        capturedBitmap = bmp
                                        imageUriString = Uri.fromFile(file).toString()
                                        showCameraPreview = false
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    cameraError = exception.localizedMessage ?: "Capture failed."
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Text("Capture Leaf Photo")
                }
            }
        }

        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap!!.asImageBitmap(),
                contentDescription = "Selected crop leaf image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        } else if (!showCameraPreview) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Capture or select a clear leaf photo to run preliminary screening",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        val isAnalyzing = scannerState is ScannerUiState.Analyzing
        Button(
            onClick = {
                val bmp = capturedBitmap ?: return@Button
                onAnalyze(selectedCrop, bmp, imageUriString, selectedFarm, targetZone)
                onOpenResults()
            },
            enabled = capturedBitmap != null && !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("run_ai_scan_button")
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Analyzing...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze Crop Leaf")
            }
        }

        OutlinedButton(
            onClick = onOpenResults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Scan History & Results")
        }
    }
}

@Composable
fun AiDiagnosisResultsScreen(
    scannerState: ScannerUiState,
    scanHistory: List<CropScanRecord>,
    language: AppLanguage,
    onBackToScanner: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_results_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(color = HarvestAmberContainer, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_preliminary_disclaimer),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        when (scannerState) {
            is ScannerUiState.Analyzing -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(26.dp))
                            Text("Running multimodal crop screening...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            is ScannerUiState.Error -> {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Warning,
                        title = stringResource(R.string.status_ai_not_configured),
                        description = scannerState.message,
                        action = {
                            Button(onClick = onBackToScanner) { Text("Back to Scanner") }
                        }
                    )
                }
            }
            is ScannerUiState.ResultReady -> {
                val res = scannerState.analysis
                if (!res.isServiceAvailable) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.status_ai_not_configured),
                            description = res.uncertaintyNotes
                        )
                    }
                } else {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Latest Preliminary Assessment", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(res.possibleCondition, style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    text = "Identified Crop: ${res.identifiedCrop} • Confidence: ${res.confidencePct}% • Severity: ${res.severity.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (res.visibleSymptoms.isNotEmpty()) {
                                    Text("Visible Symptoms:", style = MaterialTheme.typography.titleMedium)
                                    res.visibleSymptoms.forEach { sym ->
                                        Text("• $sym", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                if (res.recommendedNextSteps.isNotEmpty()) {
                                    Text("Recommended Next Steps:", style = MaterialTheme.typography.titleMedium)
                                    res.recommendedNextSteps.forEach { step ->
                                        Text("• $step", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Uncertainty & Limitations: ${res.uncertaintyNotes}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            ScannerUiState.Idle -> Unit
        }

        item {
            Text("Saved Crop Scan History (${scanHistory.size})", style = MaterialTheme.typography.titleMedium)
        }

        if (scanHistory.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.CameraAlt,
                    title = "No Saved Scans Yet",
                    description = "Analyzed crop scans appear here with expert confirmation status."
                )
            }
        } else {
            items(scanHistory, key = { it.id }) { scan ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(scan.possibleCondition, style = MaterialTheme.typography.titleMedium)
                            if (scan.expertConfirmed) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Expert Confirmed", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Text(
                            text = "${cropLabel(scan.resolvedCrop(), language)} • Confidence ${scan.confidencePct}% • Severity ${scan.severity}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!scan.expertNotes.isNullOrBlank()) {
                            Text(
                                text = "Expert Note: ${scan.expertNotes}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = formatEpochShort(scan.createdAt?.toDate()?.time ?: 0L),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("CropScanner", "Bitmap decode failed", e)
        null
    }
}
