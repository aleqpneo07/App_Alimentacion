package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityLevel
import com.example.data.model.DailyMetric
import com.example.data.model.GoalType
import com.example.data.model.NutritionProfile
import com.example.ui.theme.AmberFatGold
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.NeonVoltGreen
import com.example.ui.theme.ObsidianBackground
import java.util.Locale

@Composable
fun GoalsAndStatsScreen(
    profile: NutritionProfile,
    recentMetrics: List<DailyMetric>,
    onSaveProfileGoals: (
        weightKg: Double,
        targetWeightKg: Double,
        heightCm: Long,
        age: Long,
        activityLevel: ActivityLevel,
        goalType: GoalType
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var weightText by remember(profile.weightKg) {
        mutableStateOf(String.format(Locale.US, "%.1f", profile.weightKg))
    }
    var targetWeightText by remember(profile.targetWeightKg) {
        mutableStateOf(String.format(Locale.US, "%.1f", profile.targetWeightKg))
    }
    var heightText by remember(profile.heightCm) {
        mutableStateOf(profile.heightCm.toString())
    }
    var ageText by remember(profile.age) {
        mutableStateOf(profile.age.toString())
    }
    var selectedActivity by remember(profile.activityLevel) {
        mutableStateOf(ActivityLevel.fromName(profile.activityLevel))
    }
    var selectedGoal by remember(profile.goalType) {
        mutableStateOf(GoalType.fromName(profile.goalType))
    }

    val previewProfile = remember(
        weightText,
        targetWeightText,
        heightText,
        ageText,
        selectedActivity,
        selectedGoal
    ) {
        NutritionProfile.calculateTargets(
            weightKg = weightText.replace(',', '.').toDoubleOrNull() ?: profile.weightKg,
            targetWeightKg = targetWeightText.replace(',', '.').toDoubleOrNull() ?: profile.targetWeightKg,
            heightCm = heightText.toLongOrNull() ?: profile.heightCm,
            age = ageText.toLongOrNull() ?: profile.age,
            activity = selectedActivity,
            goal = selectedGoal
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            Text(
                text = "Calculadora de Metas y Evolución",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Ajusta tus parámetros físicos para calcular automáticamente tus calorías y macros diarios (Fórmula Mifflin-St Jeor).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculator Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = AthleticOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Parámetros Corporales",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Peso actual (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetWeightText,
                            onValueChange = { targetWeightText = it },
                            label = { Text("Peso meta (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Altura (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Edad (años)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Nivel de Actividad Física:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActivityLevel.entries.forEach { level ->
                            FilterChip(
                                selected = selectedActivity == level,
                                onClick = { selectedActivity = level },
                                label = { Text(level.labelEs) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonVoltGreen.copy(alpha = 0.22f),
                                    selectedLabelColor = NeonVoltGreen
                                )
                            )
                        }
                    }
                    Text(
                        text = selectedActivity.descriptionEs,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Objetivo Nutricional:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoalType.entries.forEach { goal ->
                            FilterChip(
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal },
                                label = { Text(goal.labelEs) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AthleticOrange.copy(alpha = 0.25f),
                                    selectedLabelColor = AthleticOrange
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Preview Target Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianBackground)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TargetPreviewItem("META KCAL", "${previewProfile.dailyCalorieGoal}", AthleticOrange)
                        TargetPreviewItem("PROTEÍNA", "${previewProfile.proteinGoalG}g", NeonVoltGreen)
                        TargetPreviewItem("CARBOS", "${previewProfile.carbsGoalG}g", ElectricCyan)
                        TargetPreviewItem("GRASAS", "${previewProfile.fatGoalG}g", AmberFatGold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onSaveProfileGoals(
                                weightText.replace(',', '.').toDoubleOrNull() ?: profile.weightKg,
                                targetWeightText.replace(',', '.').toDoubleOrNull() ?: profile.targetWeightKg,
                                heightText.toLongOrNull() ?: profile.heightCm,
                                ageText.toLongOrNull() ?: profile.age,
                                selectedActivity,
                                selectedGoal
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AthleticOrange,
                            contentColor = ObsidianBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_goals_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar Objetivos en Firebase",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Weight & Water History Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = NeonVoltGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Historial de Peso e Hidratación",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (recentMetrics.isEmpty()) {
                        Text(
                            text = "Registra tu peso o vasos de agua en la pestaña 'Diario Hoy' para ver tu evolución diaria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentMetrics.forEach { metric ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianBackground)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = metric.dateKey,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", metric.loggedWeightKg)} kg",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = NeonVoltGreen
                                        )
                                        Text(
                                            text = "${metric.waterMl} ml agua",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ElectricCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TargetPreviewItem(label: String, value: String, color: Color) {
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
