package com.sachlabel.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sachlabel.app.R
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
        stepLabel = stringResource(R.string.capture_step1_label),
        alignmentHint = stringResource(R.string.capture_align_front),
        liveOcrSample = "“100% Whole Wheat / No Preservatives”",
        tipText = stringResource(R.string.capture_tip_front),
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
        stepLabel = stringResource(R.string.capture_step2_label),
        alignmentHint = stringResource(R.string.capture_align_back),
        liveOcrSample = "“Ingredients: Wheat Flour, Sugar, Palm Oil...”",
        tipText = stringResource(R.string.capture_tip_back),
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

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Log.w("StitchCamera", "Camera permission denied by user")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
            title = stringResource(R.string.capture_title),
            subtitle = if (stepNumber == 1) stringResource(R.string.capture_step1_sub) else stringResource(R.string.capture_step2_sub),
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
                    title = stringResource(R.string.capture_step1_label),
                    isActive = stepNumber == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { if (stepNumber == 2 && onRetakePrevious != null) onRetakePrevious() }
                )
                StepTabItem(
                    stepNum = "2",
                    title = stringResource(R.string.capture_step2_label),
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
                if (hasCameraPermission) {
                    // CameraX Preview with key to rebind on lens facing or permission changes
                    key(cameraLensFacing, hasCameraPermission) {
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
                    }

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
                                    text = stringResource(R.string.capture_live_ocr),
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
                } else {
                    // Camera Permission Request Rationale UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PrimaryFixed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = PrimaryFixed,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.camera_permission_required),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.camera_permission_desc),
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.camera_permission_grant), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text(stringResource(R.string.camera_permission_settings), color = PrimaryFixed, fontSize = 12.sp)
                        }
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
                            Icon(Icons.Default.FlipCameraIos, contentDescription = stringResource(R.string.common_flip), tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(stringResource(R.string.common_flip), fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    }

                    // Main Shutter Button with Pulsing Ring
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(PrimaryFixed.copy(alpha = 0.25f))
                            .clickable {
                                if (!hasCameraPermission) {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                } else if (!isCapturing) {
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
                            // Quick snap fallback for emulator / testing: creates a valid synthetic package bitmap
                            capturedPath = createSyntheticDemoPackage(context, stepNumber)
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
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.common_demo), tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(stringResource(R.string.common_demo), fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
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
                        Text(stringResource(R.string.common_retake), fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { capturedPath?.let { onPhotoCaptured(it) } },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text(stringResource(R.string.common_use_photo), fontWeight = FontWeight.Bold)
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

/**
 * Generates an actual valid JPEG bitmap with realistic packaging text so that
 * emulator testing or quick demonstrations execute the 100% REAL CV and ML Kit OCR pipeline.
 */
private fun createSyntheticDemoPackage(context: Context, stepNumber: Int): String {
    val file = File(context.cacheDir, "demo_package_step${stepNumber}.jpg")
    val width = 800
    val height = 1100
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val bgPaint = android.graphics.Paint().apply {
        color = if (stepNumber == 1) android.graphics.Color.rgb(248, 244, 232) else android.graphics.Color.WHITE
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
    }

    if (stepNumber == 1) {
        // Front packaging: Brand + Prominent Marketing Claim
        textPaint.textSize = 34f
        textPaint.isFakeBoldText = true
        canvas.drawText("BRITANNIA NUTRICHOICE", 50f, 150f, textPaint)

        textPaint.textSize = 52f
        textPaint.color = android.graphics.Color.rgb(180, 30, 20)
        canvas.drawText("NO ADDED SUGAR", 50f, 320f, textPaint)

        textPaint.textSize = 28f
        textPaint.color = android.graphics.Color.DKGRAY
        textPaint.isFakeBoldText = false
        canvas.drawText("100% Whole Wheat Flour Digestive Biscuits", 50f, 420f, textPaint)

        textPaint.textSize = 22f
        canvas.drawText("Net Weight: 200g | 100% Vegetarian", 50f, 950f, textPaint)
    } else {
        // Back packaging: Ingredients list + Nutrition Table + Statutory Declarations
        textPaint.textSize = 26f
        textPaint.isFakeBoldText = true
        canvas.drawText("Ingredients:", 50f, 100f, textPaint)

        textPaint.textSize = 20f
        textPaint.isFakeBoldText = false
        canvas.drawText("Whole Wheat Flour (Maida) (60%), Edible Vegetable Oil (Palm),", 50f, 150f, textPaint)
        canvas.drawText("Maltodextrin, Invert Sugar Syrup, Raising Agents (INS 500ii),", 50f, 190f, textPaint)
        canvas.drawText("Iodised Salt, Emulsifier (INS 322).", 50f, 230f, textPaint)

        textPaint.textSize = 26f
        textPaint.isFakeBoldText = true
        canvas.drawText("Nutritional Information per 100g:", 50f, 330f, textPaint)

        textPaint.textSize = 20f
        textPaint.isFakeBoldText = false
        canvas.drawText("Energy: 440 kcal", 70f, 380f, textPaint)
        canvas.drawText("Protein: 8.0 g", 70f, 420f, textPaint)
        canvas.drawText("Carbohydrates: 68.0 g", 70f, 460f, textPaint)
        canvas.drawText("Total Sugars: 14.5 g", 70f, 500f, textPaint)
        canvas.drawText("Added Sugars: 0.0 g", 70f, 540f, textPaint)
        canvas.drawText("Total Fat: 16.0 g", 70f, 580f, textPaint)
        canvas.drawText("Trans Fat: 0.0 g", 70f, 620f, textPaint)
        canvas.drawText("Sodium: 310 mg", 70f, 660f, textPaint)

        textPaint.textSize = 18f
        canvas.drawText("FSSAI Lic. No.: 10015043001129", 50f, 750f, textPaint)
        canvas.drawText("*Contains naturally occurring sugars from cereal ingredients.", 50f, 820f, textPaint)
    }

    try {
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
        }
    } finally {
        bitmap.recycle()
    }
    return file.absolutePath
}
