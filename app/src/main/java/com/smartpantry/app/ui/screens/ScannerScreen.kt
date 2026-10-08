package com.smartpantry.app.ui.screens

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartpantry.app.data.model.ScannedProduct
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    busy: Boolean,
    error: String?,
    pendingProduct: ScannedProduct?,
    onBack: () -> Unit,
    onBarcode: (String) -> Unit,
    onConfirm: (LocalDate, String, String?) -> Unit,
    onClearPending: () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканер продуктов") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                cameraPermission.status.isGranted -> {
                    BarcodeCamera(
                        enabled = pendingProduct == null && !busy,
                        onBarcode = onBarcode,
                        onDateHint = { /* surfaced in dialog via OCR field */ }
                    )
                }
                cameraPermission.status.shouldShowRationale -> {
                    PermissionMessage(
                        text = "Нужен доступ к камере, чтобы сканировать штрихкоды и сроки годности.",
                        onRequest = { cameraPermission.launchPermissionRequest() }
                    )
                }
                else -> {
                    PermissionMessage(
                        text = "Разрешите камеру в настройках приложения.",
                        onRequest = { cameraPermission.launchPermissionRequest() }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(20.dp)
            ) {
                Text(
                    "Наведите камеру на штрихкод. Затем укажите или распознайте срок годности.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.78f)
                    .height(160.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
            )

            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        }
    }

    pendingProduct?.let { product ->
        ConfirmScanDialog(
            product = product,
            error = error,
            onDismiss = onClearPending,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun PermissionMessage(text: String, onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Разрешить камеру") }
    }
}

@Composable
private fun BarcodeCamera(
    enabled: Boolean,
    onBarcode: (String) -> Unit,
    onDateHint: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val handled = remember { AtomicBoolean(false) }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            barcodeScanner.close()
            textRecognizer.close()
        }
    }

    LaunchedEffect(enabled) {
        if (enabled) handled.set(false)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    if (!enabled || handled.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val value = barcodes.firstOrNull {
                                !it.rawValue.isNullOrBlank() &&
                                    (it.format == Barcode.FORMAT_EAN_13 ||
                                        it.format == Barcode.FORMAT_EAN_8 ||
                                        it.format == Barcode.FORMAT_UPC_A ||
                                        it.format == Barcode.FORMAT_UPC_E ||
                                        it.format == Barcode.FORMAT_CODE_128 ||
                                        it.format == Barcode.FORMAT_QR_CODE)
                            }?.rawValue
                            if (!value.isNullOrBlank() && handled.compareAndSet(false, true)) {
                                onBarcode(value)
                            }
                        }
                        .addOnCompleteListener {
                            textRecognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    parseExpiryDate(visionText.text)?.let(onDateHint)
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmScanDialog(
    product: ScannedProduct,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String, String?) -> Unit
) {
    var name by remember(product) { mutableStateOf(product.name) }
    var quantity by remember { mutableStateOf("1") }
    var expiry by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var showDate by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Продукт найден") },
        text = {
            Column {
                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    if (product.brand.isNotBlank()) "${product.imageHint} ${product.brand}"
                    else product.imageHint,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Количество") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Штрихкод: ${product.barcode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showDate = true }) {
                    Text("Срок годности: ${expiry.format(formatter)}")
                }
                Text(
                    "Можно сфотографировать дату на упаковке и выбрать её вручную.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(expiry, quantity, name) },
                enabled = name.isNotBlank()
            ) { Text("В холодильник") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )

    if (showDate) {
        ExpiryDatePicker(
            initial = expiry,
            onDismiss = { showDate = false },
            onConfirm = {
                expiry = it
                showDate = false
            }
        )
    }
}

private val DATE_PATTERNS = listOf(
    Pattern.compile("(\\d{2})[./-](\\d{2})[./-](\\d{4})"),
    Pattern.compile("(\\d{2})[./-](\\d{2})[./-](\\d{2})"),
    Pattern.compile("(\\d{4})[./-](\\d{2})[./-](\\d{2})")
)

fun parseExpiryDate(text: String): LocalDate? {
    for (pattern in DATE_PATTERNS) {
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return try {
                when (pattern.pattern()) {
                    DATE_PATTERNS[0].pattern() -> {
                        val d = matcher.group(1)!!.toInt()
                        val m = matcher.group(2)!!.toInt()
                        val y = matcher.group(3)!!.toInt()
                        LocalDate.of(y, m, d)
                    }
                    DATE_PATTERNS[1].pattern() -> {
                        val d = matcher.group(1)!!.toInt()
                        val m = matcher.group(2)!!.toInt()
                        var y = matcher.group(3)!!.toInt()
                        y += if (y < 100) 2000 else 0
                        LocalDate.of(y, m, d)
                    }
                    else -> {
                        val y = matcher.group(1)!!.toInt()
                        val m = matcher.group(2)!!.toInt()
                        val d = matcher.group(3)!!.toInt()
                        LocalDate.of(y, m, d)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }
    return null
}
