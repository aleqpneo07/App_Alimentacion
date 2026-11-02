package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.MealType
import com.example.data.model.ScannedProductInfo
import com.example.ui.theme.AmberFatGold
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.NeonVoltGreen
import com.example.ui.theme.ObsidianBackground
import com.example.ui.viewmodel.ScannerUiState
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private data class SampleBarcodeItem(val label: String, val ean: String)

private val sampleSpanishBarcodes = listOf(
    SampleBarcodeItem("Copos de Avena", "4008713700626"),
    SampleBarcodeItem("Gazpacho Alvalle", "8410179100050"),
    SampleBarcodeItem("Leche Pascual", "8410066000050"),
    SampleBarcodeItem("Nutella", "3017620422003"),
    SampleBarcodeItem("Coca-Cola Zero", "5449000131805")
)

@Composable
fun BarcodeScannerScreen(
    scannerState: ScannerUiState,
    defaultMealType: MealType,
    onLookupBarcode: (String) -> Unit,
    onSearchByName: (String) -> Unit,
    onSelectProduct: (ScannedProductInfo) -> Unit,
    onResetScanner: () -> Unit,
    onLogProduct: (
        mealType: MealType,
        product: ScannedProductInfo,
        grams: Int,
        kcal: Int,
        protein: Int,
        carbs: Int,
        fat: Int
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraActive by remember { mutableStateOf(hasCameraPermission) }
    var manualBarcodeInput by remember { mutableStateOf("") }
    var textSearchQuery by remember { mutableStateOf("") }
    var selectedMealType by remember(defaultMealType) { mutableStateOf(defaultMealType) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        isCameraActive = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("scanner_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al diario",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Escáner EAN / QR & Buscador",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Conectado gratis en vivo a Open Food Facts API",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonVoltGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Camera Scanner Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Lector de cámara",
                                tint = AthleticOrange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lector con Cámara (EAN-13 / QR)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (hasCameraPermission) {
                            OutlinedButton(
                                onClick = { isCameraActive = !isCameraActive },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("toggle_camera_button")
                            ) {
                                Text(if (isCameraActive) "Pausar cámara" else "Activar cámara")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (hasCameraPermission && isCameraActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, NeonVoltGreen, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraBarcodePreview(
                                onBarcodeDetected = { code ->
                                    manualBarcodeInput = code
                                    onLookupBarcode(code)
                                }
                            )
                            // Reticle overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(95.dp)
                                    .border(
                                        width = 2.dp,
                                        color = AthleticOrange.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            )
                            Surface(
                                color = ObsidianBackground.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Apunta al código de barras o QR del envase",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonVoltGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianBackground)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!hasCameraPermission) "Permiso de cámara requerido para escanear" else "Cámara en pausa",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "También puedes escribir el código EAN o buscar por nombre debajo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (!hasCameraPermission) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        isCameraActive = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AthleticOrange,
                                    contentColor = ObsidianBackground
                                ),
                                modifier = Modifier.testTag("enable_camera_permission_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Abrir cámara")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Abrir Cámara")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual barcode entry + Quick Sample Spanish Products
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcodeInput,
                            onValueChange = { manualBarcodeInput = it },
                            label = { Text("Código EAN-13 / QR") },
                            placeholder = { Text("Ej: 8410179100050") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { onLookupBarcode(manualBarcodeInput) }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("barcode_input_field")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onLookupBarcode(manualBarcodeInput) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonVoltGreen,
                                contentColor = ObsidianBackground
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("lookup_barcode_button")
                        ) {
                            Text("Consultar", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Prueba rápida con códigos reales (Open Food Facts):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleSpanishBarcodes.forEach { sample ->
                            FilterChip(
                                selected = manualBarcodeInput == sample.ean,
                                onClick = {
                                    manualBarcodeInput = sample.ean
                                    onLookupBarcode(sample.ean)
                                },
                                label = { Text("${sample.label} (${sample.ean.takeLast(4)})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AthleticOrange.copy(alpha = 0.22f),
                                    selectedLabelColor = AthleticOrange
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text Search in Open Food Facts
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Buscador por Nombre (Supermercados España/Europa)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textSearchQuery,
                            onValueChange = { textSearchQuery = it },
                            label = { Text("Buscar alimento o marca") },
                            placeholder = { Text("Ej: yogur proteico, avena, atún...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { onSearchByName(textSearchQuery) }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("off_search_input")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onSearchByName(textSearchQuery) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AthleticOrange,
                                contentColor = ObsidianBackground
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("off_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Scanner / Search Result State
            when (scannerState) {
                is ScannerUiState.Idle -> {
                    // Ready state info
                }

                is ScannerUiState.Loading -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = NeonVoltGreen,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = scannerState.statusMessage,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                is ScannerUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = scannerState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = onResetScanner) {
                                Text("Limpiar mensaje")
                            }
                        }
                    }
                }

                is ScannerUiState.SearchResults -> {
                    Text(
                        text = "Resultados en Open Food Facts (${scannerState.products.size}):",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonVoltGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    scannerState.products.forEach { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, GraphiteBorder, RoundedCornerShape(14.dp))
                                .clickable { onSelectProduct(item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${item.brand} • ${item.caloriesPer100g} kcal / 100g (P:${item.proteinPer100g}g C:${item.carbsPer100g}g G:${item.fatPer100g}g)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (item.nutriScore != null) {
                                    NutriScoreBadge(grade = item.nutriScore)
                                }
                            }
                        }
                    }
                }

                is ScannerUiState.ProductFound -> {
                    val product = scannerState.product
                    var gramsSlider by remember(product.barcode) {
                        mutableFloatStateOf(product.servingGramsDefault.toFloat().coerceIn(10f, 600f))
                    }
                    val grams = gramsSlider.roundToInt().coerceAtLeast(1)
                    val factor = grams / 100.0f
                    val calcKcal = (product.caloriesPer100g * factor).roundToInt()
                    val calcProtein = (product.proteinPer100g * factor).roundToInt()
                    val calcCarbs = (product.carbsPer100g * factor).roundToInt()
                    val calcFat = (product.fatPer100g * factor).roundToInt()

                    Card(
                        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, NeonVoltGreen, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PRODUCTO DETECTADO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonVoltGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        text = "${product.brand} • EAN: ${product.barcode}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (product.nutriScore != null) {
                                    NutriScoreBadge(grade = product.nutriScore)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Portion slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cantidad consumida:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "$grams g",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = AthleticOrange
                                )
                            }
                            Slider(
                                value = gramsSlider,
                                onValueChange = { gramsSlider = it },
                                valueRange = 10f..600f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AthleticOrange,
                                    activeTrackColor = AthleticOrange
                                )
                            )

                            // Quick gram presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(30, 50, 100, 150, 250).forEach { preset ->
                                    FilterChip(
                                        selected = grams == preset,
                                        onClick = { gramsSlider = preset.toFloat() },
                                        label = { Text("${preset}g") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Calculated Macros Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ObsidianBackground)
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                MacroStatPill("KCAL", "$calcKcal", AthleticOrange)
                                MacroStatPill("PROTEÍNA", "${calcProtein}g", NeonVoltGreen)
                                MacroStatPill("CARBOS", "${calcCarbs}g", ElectricCyan)
                                MacroStatPill("GRASAS", "${calcFat}g", AmberFatGold)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Guardar en comida:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MealType.entries.forEach { meal ->
                                    FilterChip(
                                        selected = selectedMealType == meal,
                                        onClick = { selectedMealType = meal },
                                        label = { Text(meal.labelEs) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AthleticOrange.copy(alpha = 0.25f),
                                            selectedLabelColor = AthleticOrange
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    onLogProduct(
                                        selectedMealType,
                                        product,
                                        grams,
                                        calcKcal,
                                        calcProtein,
                                        calcCarbs,
                                        calcFat
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonVoltGreen,
                                    contentColor = ObsidianBackground
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("confirm_log_scanned_product")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Registrar en ${selectedMealType.labelEs} ($calcKcal kcal)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun NutriScoreBadge(grade: String) {
    val bgColor = when (grade.uppercase()) {
        "A" -> Color(0xFF038141)
        "B" -> Color(0xFF85BB2F)
        "C" -> Color(0xFFFECB02)
        "D" -> Color(0xFFEE8100)
        else -> Color(0xFFE63E11)
    }
    val textColor = if (grade.uppercase() == "C") Color.Black else Color.White
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "Nutri-Score ${grade.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MacroStatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CameraBarcodePreview(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val reader = remember {
        MultiFormatReader().apply {
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
            hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128
            )
            setHints(hints)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                decodeBarcodeFromImageProxy(imageProxy, reader) { detectedCode ->
                                    previewView.post {
                                        onBarcodeDetected(detectedCode)
                                    }
                                }
                            }
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraBarcodePreview", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun decodeBarcodeFromImageProxy(
    imageProxy: ImageProxy,
    reader: MultiFormatReader,
    onFound: (String) -> Unit
) {
    try {
        val plane = imageProxy.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        val width = imageProxy.width
        val height = imageProxy.height
        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result = reader.decodeWithState(bitmap)
        val text = result.text
        if (!text.isNullOrBlank()) {
            onFound(text)
        }
    } catch (ignored: Exception) {
        // Frame did not contain a barcode
    } finally {
        reader.reset()
        imageProxy.close()
    }
}
