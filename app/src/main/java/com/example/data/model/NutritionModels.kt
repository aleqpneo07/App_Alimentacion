package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToLong

enum class MealType(val labelEs: String, val targetPct: Float) {
    BREAKFAST("Desayuno", 0.25f),
    LUNCH("Comida", 0.35f),
    DINNER("Cena", 0.25f),
    SNACK("Snacks", 0.15f);

    companion object {
        fun fromName(value: String): MealType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LUNCH
    }
}

enum class ActivityLevel(val labelEs: String, val descriptionEs: String, val multiplier: Double) {
    SEDENTARY("Sedentario", "Trabajo de oficina, poco ejercicio", 1.2),
    LIGHT("Ligero", "Ejercicio ligero 1-3 días/semana", 1.375),
    MODERATE("Moderado", "Deporte moderado 3-5 días/semana", 1.55),
    ACTIVE("Activo", "Entrenamiento intenso 6-7 días/semana", 1.725),
    ATHLETE("Atleta", "Doble sesión o trabajo físico intenso", 1.9);

    companion object {
        fun fromName(value: String): ActivityLevel =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MODERATE
    }
}

enum class GoalType(val labelEs: String, val calorieDelta: Int, val badgeEs: String) {
    DEFICIT("Definición (-400 kcal)", -400, "Perder grasa"),
    MAINTAIN("Mantenimiento", 0, "Mantener peso"),
    SURPLUS("Volumen (+300 kcal)", 300, "Ganar músculo");

    companion object {
        fun fromName(value: String): GoalType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFICIT
    }
}

@IgnoreExtraProperties
data class NutritionProfile(
    @DocumentId val id: String = "main",
    val userId: String = "",
    val weightKg: Double = 75.0,
    val targetWeightKg: Double = 70.0,
    val heightCm: Long = 175L,
    val age: Long = 28L,
    val activityLevel: String = ActivityLevel.MODERATE.name,
    val goalType: String = GoalType.DEFICIT.name,
    val dailyCalorieGoal: Long = 2100L,
    val proteinGoalG: Long = 155L,
    val carbsGoalG: Long = 210L,
    val fatGoalG: Long = 65L,
    val waterGoalMl: Long = 2500L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf<String, Any?>(
            "userId" to ownerUid,
            "weightKg" to weightKg,
            "targetWeightKg" to targetWeightKg,
            "heightCm" to heightCm,
            "age" to age,
            "activityLevel" to activityLevel,
            "goalType" to goalType,
            "dailyCalorieGoal" to dailyCalorieGoal,
            "proteinGoalG" to proteinGoalG,
            "carbsGoalG" to carbsGoalG,
            "fatGoalG" to fatGoalG,
            "waterGoalMl" to waterGoalMl,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf<String, Any?>(
            "weightKg" to weightKg,
            "targetWeightKg" to targetWeightKg,
            "heightCm" to heightCm,
            "age" to age,
            "activityLevel" to activityLevel,
            "goalType" to goalType,
            "dailyCalorieGoal" to dailyCalorieGoal,
            "proteinGoalG" to proteinGoalG,
            "carbsGoalG" to carbsGoalG,
            "fatGoalG" to fatGoalG,
            "waterGoalMl" to waterGoalMl,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }

    companion object {
        fun calculateTargets(
            weightKg: Double,
            targetWeightKg: Double,
            heightCm: Long,
            age: Long,
            activity: ActivityLevel,
            goal: GoalType
        ): NutritionProfile {
            // Mifflin-St Jeor base BMR approximation
            val bmr = (10.0 * weightKg) + (6.25 * heightCm.toDouble()) - (5.0 * age.toDouble()) + 5.0
            val tdee = bmr * activity.multiplier
            val targetCalories = (tdee + goal.calorieDelta).roundToLong().coerceIn(1200L, 6000L)
            val proteinG = (weightKg * 2.0).roundToLong().coerceIn(60L, 350L)
            val fatG = ((targetCalories * 0.27) / 9.0).roundToLong().coerceIn(35L, 200L)
            val remainingKcal = (targetCalories - (proteinG * 4L) - (fatG * 9L)).coerceAtLeast(200L)
            val carbsG = (remainingKcal / 4L).coerceIn(50L, 800L)
            val waterMl = (weightKg * 35.0).roundToLong().coerceIn(1500L, 5000L)

            return NutritionProfile(
                weightKg = weightKg,
                targetWeightKg = targetWeightKg,
                heightCm = heightCm,
                age = age,
                activityLevel = activity.name,
                goalType = goal.name,
                dailyCalorieGoal = targetCalories,
                proteinGoalG = proteinG,
                carbsGoalG = carbsG,
                fatGoalG = fatG,
                waterGoalMl = waterMl
            )
        }
    }
}

@IgnoreExtraProperties
data class MealLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val mealType: String = MealType.LUNCH.name,
    val foodName: String = "",
    val brandOrSource: String = "Manual",
    val servingGrams: Long = 100L,
    val calories: Long = 0L,
    val proteinG: Long = 0L,
    val carbsG: Long = 0L,
    val fatG: Long = 0L,
    val barcode: String? = null,
    val ingredientsSummary: String? = null,
    val nutriScore: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf<String, Any?>(
            "userId" to ownerUid,
            "dateKey" to dateKey,
            "mealType" to mealType,
            "foodName" to foodName.take(200),
            "brandOrSource" to brandOrSource.take(150),
            "servingGrams" to servingGrams.coerceIn(1L, 5000L),
            "calories" to calories.coerceIn(0L, 10000L),
            "proteinG" to proteinG.coerceIn(0L, 1000L),
            "carbsG" to carbsG.coerceIn(0L, 1000L),
            "fatG" to fatG.coerceIn(0L, 1000L),
            "barcode" to barcode?.takeIf { it.isNotBlank() }?.take(64),
            "ingredientsSummary" to ingredientsSummary?.takeIf { it.isNotBlank() }?.take(1000),
            "nutriScore" to nutriScore?.takeIf { it.isNotBlank() }?.take(10),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class DailyMetric(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val waterMl: Long = 0L,
    val loggedWeightKg: Double = 75.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        return mapOf<String, Any?>(
            "userId" to ownerUid,
            "dateKey" to dateKey,
            "waterMl" to waterMl.coerceIn(0L, 15000L),
            "loggedWeightKg" to loggedWeightKg.coerceIn(20.0, 400.0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

data class ScannedProductInfo(
    val barcode: String,
    val name: String,
    val brand: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Int,
    val carbsPer100g: Int,
    val fatPer100g: Int,
    val servingGramsDefault: Int = 100,
    val nutriScore: String? = null,
    val imageUrl: String? = null
)

data class AiPlateAnalysisResult(
    val dishName: String,
    val estimatedTotalGrams: Int,
    val totalCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val detectedIngredients: List<String>,
    val confidenceNote: String
)
