package com.sachlabel.app.ui.screens

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sachlabel.app.ui.components.SachLabelHeader
import com.sachlabel.app.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Stitch Screen 4 — Front Camera Capture
 */
@Composable
fun CaptureFrontScreen(
    onPhotoCaptured: (String) -> Unit,
    onBack: () -> Unit
) {
    StitchCameraScreen(
        stepNumber = 1,
        stepLabel = "Front Claim",
        alignmentHint = "Align Front Packaging Claim here",
        liveOcrSample = "“100% Whole Wheat / No Preservatives”",
        tipText = "Ensure the main product claim is centered and clear.",
        onPhotoCaptured = onPhotoCaptured,
        onBack = onBack
    )
}

/**
 * Stitch Screen 5 — Back Camera Capture
 */
@Composable
fun CaptureBackScreen(
    onPhotoCaptured: (String) -> Unit,
    onRetakeFront: () -> Unit,
    onBack: () -> Unit
) {
    StitchCameraScreen(
        stepNumber = 2,
        stepLabel = "Back Fine Print",
        alignmentHint = "Align Ingredients List & Nutrition Table here",
        liveOcrSample = "“Ingredients: Wheat Flour, Sugar, Palm Oil...”",
        tipText = "Ensure ingredient list and nutrition table are in good lighting for instant OCR verification.",
        onPhotoCaptured = onPhotoCaptured,
        onRetakePrevious = onRetakeFront,
        onBack = onBack
    )
}

@Composable
private fun StitchCameraScreen(
    stepNumber: Int,
    stepLabel: String,
    alignmentHint: String,
    liveOcrSample: String,
    tipText: String,
    onPhotoCaptured: (String) -> Unit,
    onRetakePrevious: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var capturedPath by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
    ) {
        // Stitch Top Header
        SachLabelHeader(
            title = "Scan Product",
            subtitle = if (stepNumber == 1) "Step 1: Front Claim" else "Step 2: Back Evidence",
            showBackButton = true,
            onBackClick = onBack,
            trailingContent = {
                // Flashlight toggle
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Pill Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(SurfaceContainerLow)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.3f), CircleShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StepTabItem(
                    stepNum = "1",
                    title = "Front Claim",
                    isActive = stepNumber == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { if (stepNumber == 2 && onRetakePrevious != null) onRetakePrevious() }
                )
                StepTabItem(
                    stepNum = "2",
                    title = "Back Fine Print",
                    isActive = stepNumber == 2,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4:5 Viewfinder Container with Reticle and Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black)
                    .border(2.dp, Color.White, RoundedCornerShape(26.dp))
            ) {
                // CameraX Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            try {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.Builder().requireLensFacing(cameraLensFacing).build(),
                                    preview,
                                    capture
                                )
                                cameraControl = camera.cameraControl
                            } catch (e: Exception) {
                                Log.e("StitchCamera", "Camera bind failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Reticle Corner Brackets Overlay
                ReticleOverlay(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )

                // Top Alignment Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(CircleShape)
                        .background(Color(0xD9151D1A))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = PrimaryFixed,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = alignmentHint,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom Live OCR Detection Preview Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.95f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE OCR RECOGNITION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = liveOcrSample,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shutter Controls Row
            if (capturedPath == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flip Camera
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            cameraLensFacing = if (cameraLensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerLow)
                                .border(1.dp, OutlineVariant.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FlipCameraIos, contentDescription = "Flip", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("Flip", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    }

                    // Main Shutter Button with Pulsing Ring
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(PrimaryFixed.copy(alpha = 0.25f))
                            .clickable {
                                if (!isCapturing) {
                                    isCapturing = true
                                    capturePhoto(
                                        context = context,
                                        imageCapture = imageCapture,
                                        executor = cameraExecutor,
                                        onSuccess = { path ->
                                            capturedPath = path
                                            isCapturing = false
                                        },
                                        onError = { isCapturing = false }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .shadow(8.dp, CircleShape, spotColor = PrimaryGreen.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Capture", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                        }
                    }

                    // Demo Shortcut
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            // Quick snap fallback for emulator / testing
                            val mockFile = File(context.cacheDir, "mock_snap_${stepNumber}.jpg").apply { writeText("mock") }
                            capturedPath = mockFile.absolutePath
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerLow)
                                .border(1.dp, OutlineVariant.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Demo Snap", tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("Demo", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                // Confirm or Retake Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { capturedPath = null },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Retake Photo", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { capturedPath?.let { onPhotoCaptured(it) } },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Use This Photo", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pro Tip Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(PrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                    }
                    Text(
                        text = tipText,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }
}

@Composable
private fun StepTabItem(
    stepNum: String,
    title: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isActive) PrimaryGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color.White.copy(alpha = 0.25f) else SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNum,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else TextSecondary
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) Color.White else TextSecondary
            )
        }
    }
}

@Composable
private fun ReticleOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.5.dp.toPx()
        val cornerLength = 32.dp.toPx()
        val color = Color(0xFFA9F3C5)

        // Top Left
        drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth, StrokeCap.Round)

        // Top Right
        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth, StrokeCap.Round)

        // Bottom Left
        drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth, StrokeCap.Round)

        // Bottom Right
        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth, StrokeCap.Round)
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: ExecutorService,
    onSuccess: (String) -> Unit,
    onError: () -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "sachlabel_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture?.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onSuccess(photoFile.absolutePath)
            }
            override fun onError(exc: ImageCaptureException) {
                Log.e("StitchCamera", "Photo capture failed", exc)
                onError()
            }
        }
    )
}
