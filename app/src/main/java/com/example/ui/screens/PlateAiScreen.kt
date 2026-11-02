package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MealType
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.NeonVoltGreen
import com.example.ui.theme.ObsidianBackground
import com.example.ui.viewmodel.PlateAiUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateAiScreen(
    plateAiState: PlateAiUiState,
    defaultMealType: MealType,
    onAnalyzePhoto: (Bitmap, String) -> Unit,
    onResetAiState: () -> Unit,
    onLogAnalyzedPlate: (
        mealType: MealType,
        dishName: String,
        grams: Int,
        kcal: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        ingredientsSummary: String
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userHint by remember { mutableStateOf("") }
    var selectedMealType by remember(defaultMealType) { mutableStateOf(defaultMealType) }

    // Camera capture launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            onAnalyzePhoto(bitmap, userHint)
        }
    }

    // Zero-permission Android Photo Picker
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bmp != null) {
                    selectedBitmap = bmp
                    onAnalyzePhoto(bmp, userHint)
                }
            } catch (e: Exception) {
                // Ignore decoding error
            }
        }
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
                    modifier = Modifier.testTag("ai_plate_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al diario",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reconocimiento de Platos IA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Estimación visual con Gemini 3.8 Flash Multimodal (0 €)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AthleticOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Photo Selection & Hint Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Captura o selecciona la foto de tu comida",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gemini identificará cada ingrediente del plato, su peso aproximado en gramos y calculará las calorías y macronutrientes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userHint,
                        onValueChange = { userHint = it },
                        label = { Text("Contexto opcional (ej: sin salsa, 200g de arroz)") },
                        placeholder = { Text("Añade detalles si quieres afinar el cálculo...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_plate_hint_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { takePictureLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AthleticOrange,
                                contentColor = ObsidianBackground
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("take_plate_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hacer Foto", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pick_gallery_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galería")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sample Dish Photo Trigger (for instant testing on emulator)
                    OutlinedButton(
                        onClick = {
                            val sampleBmp = BitmapFactory.decodeResource(
                                context.resources,
                                R.drawable.img_nutrition_hero_1793650829286
                            )
                            if (sampleBmp != null) {
                                selectedBitmap = sampleBmp
                                onAnalyzePhoto(
                                    sampleBmp,
                                    userHint.ifBlank { "Bowl de salmón a la plancha con aguacate, quinoa y edamame" }
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_sample_bowl_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = NeonVoltGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Probar IA con plato de ejemplo (Salmón, Quinoa y Aguacate)",
                            color = NeonVoltGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI State Display
            when (plateAiState) {
                is PlateAiUiState.Idle -> {
                    // Waiting for photo
                }

                is PlateAiUiState.Analyzing -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AthleticOrange, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = plateAiState.bitmap.asImageBitmap(),
                                contentDescription = "Plato analizando",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(color = AthleticOrange)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Gemini 3.8 Flash está identificando ingredientes y porciones...",
                                style = MaterialTheme.typography.titleMedium,
                                color = AthleticOrange
                            )
                        }
                    }
                }

                is PlateAiUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = plateAiState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (plateAiState.bitmap != null) {
                                    Button(
                                        onClick = { onAnalyzePhoto(plateAiState.bitmap, userHint) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AthleticOrange,
                                            contentColor = ObsidianBackground
                                        )
                                    ) {
                                        Text("Reintentar Análisis")
                                    }
                                }
                                OutlinedButton(onClick = onResetAiState) {
                                    Text("Limpiar")
                                }
                            }
                        }
                    }
                }

                is PlateAiUiState.Analyzed -> {
                    val result = plateAiState.result
                    var dishNameInput by remember(result) { mutableStateOf(result.dishName) }
                    var gramsInput by remember(result) { mutableStateOf(result.estimatedTotalGrams.toString()) }
                    var kcalInput by remember(result) { mutableStateOf(result.totalCalories.toString()) }
                    var proteinInput by remember(result) { mutableStateOf(result.proteinGrams.toString()) }
                    var carbsInput by remember(result) { mutableStateOf(result.carbsGrams.toString()) }
                    var fatInput by remember(result) { mutableStateOf(result.fatGrams.toString()) }

                    LaunchedEffect(result) {
                        dishNameInput = result.dishName
                        gramsInput = result.estimatedTotalGrams.toString()
                        kcalInput = result.totalCalories.toString()
                        proteinInput = result.proteinGrams.toString()
                        carbsInput = result.carbsGrams.toString()
                        fatInput = result.fatGrams.toString()
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, AthleticOrange, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AthleticOrange
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RESULTADO DE VISIÓN ARTIFICIAL GEMINI",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AthleticOrange
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Image(
                                bitmap = plateAiState.bitmap.asImageBitmap(),
                                contentDescription = result.dishName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = dishNameInput,
                                onValueChange = { dishNameInput = it },
                                label = { Text("Nombre del plato identificado") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Ingredientes y pesos estimados:",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonVoltGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.detectedIngredients.forEach { ingredient ->
                                    Surface(
                                        color = ObsidianBackground,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp))
                                    ) {
                                        Text(
                                            text = ingredient,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            if (result.confidenceNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Nota IA: ${result.confidenceNote}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Valores nutricionales (puedes ajustarlos antes de guardar):",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = kcalInput,
                                    onValueChange = { kcalInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Calorías (kcal)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = gramsInput,
                                    onValueChange = { gramsInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Peso total (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = proteinInput,
                                    onValueChange = { proteinInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Proteína (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = carbsInput,
                                    onValueChange = { carbsInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Carbos (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = fatInput,
                                    onValueChange = { fatInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Grasas (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
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
                                    onLogAnalyzedPlate(
                                        selectedMealType,
                                        dishNameInput.ifBlank { result.dishName },
                                        gramsInput.toIntOrNull() ?: result.estimatedTotalGrams,
                                        kcalInput.toIntOrNull() ?: result.totalCalories,
                                        proteinInput.toIntOrNull() ?: result.proteinGrams,
                                        carbsInput.toIntOrNull() ?: result.carbsGrams,
                                        fatInput.toIntOrNull() ?: result.fatGrams,
                                        result.detectedIngredients.joinToString(", ")
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
                                    .testTag("confirm_log_ai_plate_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Registrar Plato en ${selectedMealType.labelEs}",
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
