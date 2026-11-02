package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyMetric
import com.example.data.model.MealLog
import com.example.data.model.MealType
import com.example.data.model.NutritionProfile
import com.example.ui.theme.AmberFatGold
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.CoralAlertRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.NeonVoltGreen
import com.example.ui.theme.ObsidianBackground
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    userEmail: String?,
    selectedDateKey: String,
    profile: NutritionProfile,
    mealLogs: List<MealLog>,
    dailyMetric: DailyMetric,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    onOpenScanner: (MealType) -> Unit,
    onOpenAiPhoto: (MealType) -> Unit,
    onManualLogFood: (
        mealType: MealType,
        foodName: String,
        servingGrams: Int,
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int
    ) -> Unit,
    onDeleteMealLog: (String) -> Unit,
    onAddWaterMl: (Int) -> Unit,
    onUpdateWeightKg: (Double) -> Unit,
    onSignOut: () -> Unit
) {
    var manualEntryMealType by remember { mutableStateOf<MealType?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }

    val totalCalories = mealLogs.sumOf { it.calories }.toInt()
    val totalProtein = mealLogs.sumOf { it.proteinG }.toInt()
    val totalCarbs = mealLogs.sumOf { it.carbsG }.toInt()
    val totalFat = mealLogs.sumOf { it.fatG }.toInt()

    val goalCalories = profile.dailyCalorieGoal.toInt().coerceAtLeast(1000)
    val remainingCalories = goalCalories - totalCalories

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NUTRIPULSE AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = AthleticOrange
                        )
                        Text(
                            text = userEmail ?: "Cuenta Google sincronizada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Date Selector Bar
            item {
                Surface(
                    color = GraphiteCard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousDay,
                            modifier = Modifier.testTag("prev_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Día anterior"
                            )
                        }
                        TextButton(onClick = onTodayClick) {
                            Text(
                                text = "Fecha: $selectedDateKey",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onNextDay,
                            modifier = Modifier.testTag("next_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Día siguiente"
                            )
                        }
                    }
                }
            }

            // Hero Calorie & Macros Telemetry Card
            item {
                CalorieAndMacrosCard(
                    consumedCalories = totalCalories,
                    goalCalories = goalCalories,
                    remainingCalories = remainingCalories,
                    proteinG = totalProtein,
                    proteinGoalG = profile.proteinGoalG.toInt(),
                    carbsG = totalCarbs,
                    carbsGoalG = profile.carbsGoalG.toInt(),
                    fatG = totalFat,
                    fatGoalG = profile.fatGoalG.toInt()
                )
            }

            // Quick Actions Row (Scan Barcode/QR, AI Photo, Manual)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onOpenScanner(MealType.LUNCH) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonVoltGreen,
                            contentColor = ObsidianBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("dashboard_scan_barcode_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Código / QR", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onOpenAiPhoto(MealType.LUNCH) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AthleticOrange,
                            contentColor = ObsidianBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("dashboard_ai_photo_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Foto con IA", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Hydration & Weight Row
            item {
                HydrationAndWeightSection(
                    waterMl = dailyMetric.waterMl.toInt(),
                    waterGoalMl = profile.waterGoalMl.toInt(),
                    loggedWeightKg = dailyMetric.loggedWeightKg,
                    targetWeightKg = profile.targetWeightKg,
                    onAddWaterMl = onAddWaterMl,
                    onEditWeightClick = { showWeightDialog = true }
                )
            }

            // 4 Meal Slots (Breakfast, Lunch, Dinner, Snacks)
            items(MealType.entries.size) { index ->
                val mealType = MealType.entries[index]
                val logsForMeal = mealLogs.filter { it.mealType.equals(mealType.name, ignoreCase = true) }
                MealSectionCard(
                    mealType = mealType,
                    logs = logsForMeal,
                    dailyCalorieGoal = goalCalories,
                    onScanForMeal = { onOpenScanner(mealType) },
                    onAiPhotoForMeal = { onOpenAiPhoto(mealType) },
                    onManualAddForMeal = { manualEntryMealType = mealType },
                    onDeleteLog = onDeleteMealLog
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (manualEntryMealType != null) {
        ManualFoodEntryDialog(
            initialMealType = manualEntryMealType!!,
            onDismiss = { manualEntryMealType = null },
            onConfirm = { meal, name, grams, kcal, pro, carb, fat ->
                onManualLogFood(meal, name, grams, kcal, pro, carb, fat)
                manualEntryMealType = null
            }
        )
    }

    if (showWeightDialog) {
        WeightUpdateDialog(
            currentWeight = dailyMetric.loggedWeightKg,
            onDismiss = { showWeightDialog = false },
            onConfirm = { newWeight ->
                onUpdateWeightKg(newWeight)
                showWeightDialog = false
            }
        )
    }
}

@Composable
private fun CalorieAndMacrosCard(
    consumedCalories: Int,
    goalCalories: Int,
    remainingCalories: Int,
    proteinG: Int,
    proteinGoalG: Int,
    carbsG: Int,
    carbsGoalG: Int,
    fatG: Int,
    fatGoalG: Int
) {
    val progressTarget = (consumedCalories.toFloat() / goalCalories.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(),
        label = "calorieRingProgress"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GraphiteBorder, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Consumed column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$consumedCalories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CONSUMIDAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Central Calorie Ring
                Box(
                    modifier = Modifier.size(148.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringColor = if (remainingCalories >= 0) AthleticOrange else CoralAlertRed
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        drawArc(
                            color = GraphiteBorder,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = ringColor,
                            startAngle = 135f,
                            sweepAngle = 270f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${abs(remainingCalories)}",
                            style = MaterialTheme.typography.displayMedium,
                            color = if (remainingCalories >= 0) MaterialTheme.colorScheme.onSurface else CoralAlertRed
                        )
                        Text(
                            text = if (remainingCalories >= 0) "KCAL LIBRES" else "KCAL EXTRA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Goal column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$goalCalories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NeonVoltGreen
                    )
                    Text(
                        text = "OBJETIVO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3 Macronutrient Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroProgressColumn(
                    modifier = Modifier.weight(1f),
                    label = "Proteínas",
                    currentG = proteinG,
                    goalG = proteinGoalG,
                    color = NeonVoltGreen
                )
                MacroProgressColumn(
                    modifier = Modifier.weight(1f),
                    label = "Carbohidratos",
                    currentG = carbsG,
                    goalG = carbsGoalG,
                    color = ElectricCyan
                )
                MacroProgressColumn(
                    modifier = Modifier.weight(1f),
                    label = "Grasas",
                    currentG = fatG,
                    goalG = fatGoalG,
                    color = AmberFatGold
                )
            }
        }
    }
}

@Composable
private fun MacroProgressColumn(
    modifier: Modifier = Modifier,
    label: String,
    currentG: Int,
    goalG: Int,
    color: Color
) {
    val safeGoal = goalG.coerceAtLeast(1)
    val ratio = (currentG.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianBackground)
            .padding(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${currentG}g / ${safeGoal}g",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = GraphiteBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
private fun HydrationAndWeightSection(
    waterMl: Int,
    waterGoalMl: Int,
    loggedWeightKg: Double,
    targetWeightKg: Double,
    onAddWaterMl: (Int) -> Unit,
    onEditWeightClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Water Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(1.25f)
                .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalDrink,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Agua Hoy",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$waterMl / $waterGoalMl ml",
                    style = MaterialTheme.typography.titleLarge,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                val waterRatio = (waterMl.toFloat() / waterGoalMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { waterRatio },
                    color = ElectricCyan,
                    trackColor = GraphiteBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onAddWaterMl(-250) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("water_minus_250_button")
                    ) {
                        Text("-250")
                    }
                    Button(
                        onClick = { onAddWaterMl(250) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ObsidianBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("water_plus_250_button")
                    ) {
                        Text("+250ml", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Weight Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(0.95f)
                .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = NeonVoltGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Peso",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    IconButton(
                        onClick = onEditWeightClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_weight_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar peso",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${String.format(Locale.US, "%.1f", loggedWeightKg)} kg",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonVoltGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Meta: ${String.format(Locale.US, "%.1f", targetWeightKg)} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onEditWeightClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrar")
                }
            }
        }
    }
}

@Composable
private fun MealSectionCard(
    mealType: MealType,
    logs: List<MealLog>,
    dailyCalorieGoal: Int,
    onScanForMeal: () -> Unit,
    onAiPhotoForMeal: () -> Unit,
    onManualAddForMeal: () -> Unit,
    onDeleteLog: (String) -> Unit
) {
    val mealKcal = logs.sumOf { it.calories }.toInt()
    val recommendedKcal = (dailyCalorieGoal * mealType.targetPct).toInt()

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
                Column {
                    Text(
                        text = mealType.labelEs,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "$mealKcal / $recommendedKcal kcal recomendadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mealKcal > 0) AthleticOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onScanForMeal,
                        modifier = Modifier.testTag("meal_scan_${mealType.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear código para ${mealType.labelEs}",
                            tint = NeonVoltGreen
                        )
                    }
                    IconButton(
                        onClick = onAiPhotoForMeal,
                        modifier = Modifier.testTag("meal_ai_${mealType.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Analizar foto IA para ${mealType.labelEs}",
                            tint = AthleticOrange
                        )
                    }
                    IconButton(
                        onClick = onManualAddForMeal,
                        modifier = Modifier.testTag("meal_manual_${mealType.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir manual a ${mealType.labelEs}",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (logs.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sin alimentos registrados todavía. Usa el escáner QR/EAN, haz una foto con IA o pulsa +.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    logs.forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianBackground)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = log.foodName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (!log.nutriScore.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        NutriScoreBadge(grade = log.nutriScore)
                                    }
                                }
                                Text(
                                    text = "${log.brandOrSource} • ${log.servingGrams}g • P:${log.proteinG}g C:${log.carbsG}g G:${log.fatG}g",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!log.ingredientsSummary.isNullOrBlank()) {
                                    Text(
                                        text = "IA: ${log.ingredientsSummary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AthleticOrange
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${log.calories} kcal",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonVoltGreen,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onDeleteLog(log.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Eliminar ${log.foodName}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualFoodEntryDialog(
    initialMealType: MealType,
    onDismiss: () -> Unit,
    onConfirm: (MealType, String, Int, Int, Int, Int, Int) -> Unit
) {
    var selectedMeal by remember { mutableStateOf(initialMealType) }
    var foodName by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("150") }
    var calories by remember { mutableStateOf("250") }
    var protein by remember { mutableStateOf("20") }
    var carbs by remember { mutableStateOf("25") }
    var fat by remember { mutableStateOf("8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Alimento Manual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = selectedMeal == meal,
                            onClick = { selectedMeal = meal },
                            label = { Text(meal.labelEs) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AthleticOrange.copy(alpha = 0.25f),
                                selectedLabelColor = AthleticOrange
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Nombre del alimento o plato") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_food_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it.filter { c -> c.isDigit() } },
                        label = { Text("Kcal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = grams,
                        onValueChange = { grams = it.filter { c -> c.isDigit() } },
                        label = { Text("Gramos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { c -> c.isDigit() } },
                        label = { Text("Prot (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { c -> c.isDigit() } },
                        label = { Text("Carb (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { c -> c.isDigit() } },
                        label = { Text("Grasa (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedMeal,
                        foodName.ifBlank { "Alimento personalizado" },
                        grams.toIntOrNull() ?: 100,
                        calories.toIntOrNull() ?: 200,
                        protein.toIntOrNull() ?: 10,
                        carbs.toIntOrNull() ?: 20,
                        fat.toIntOrNull() ?: 5
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AthleticOrange,
                    contentColor = ObsidianBackground
                ),
                modifier = Modifier.testTag("confirm_manual_food_button")
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun WeightUpdateDialog(
    currentWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var weightText by remember { mutableStateOf(String.format(Locale.US, "%.1f", currentWeight)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Peso Corporal") },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Peso en kg (ej: 74.2)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weight_dialog_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = weightText.replace(',', '.').toDoubleOrNull() ?: currentWeight
                    onConfirm(parsed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonVoltGreen,
                    contentColor = ObsidianBackground
                )
            ) {
                Text("Guardar Peso", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
