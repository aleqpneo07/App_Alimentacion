package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.PlateAiAnalyzer
import com.example.data.model.ActivityLevel
import com.example.data.model.AiPlateAnalysisResult
import com.example.data.model.DailyMetric
import com.example.data.model.GoalType
import com.example.data.model.MealLog
import com.example.data.model.MealType
import com.example.data.model.NutritionProfile
import com.example.data.model.ScannedProductInfo
import com.example.data.remote.OpenFoodFactsClient
import com.example.data.repository.NutritionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiDataState<out T> {
    data object Loading : UiDataState<Nothing>
    data class Success<T>(val data: T) : UiDataState<T>
    data class Error(val message: String) : UiDataState<Nothing>
}

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data class Loading(val statusMessage: String) : ScannerUiState
    data class ProductFound(val product: ScannedProductInfo) : ScannerUiState
    data class SearchResults(val query: String, val products: List<ScannedProductInfo>) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

sealed interface PlateAiUiState {
    data object Idle : PlateAiUiState
    data class Analyzing(val bitmap: Bitmap) : PlateAiUiState
    data class Analyzed(val bitmap: Bitmap, val result: AiPlateAnalysisResult) : PlateAiUiState
    data class Error(val message: String, val bitmap: Bitmap? = null) : PlateAiUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(
    private val repository: NutritionRepository,
    private val currentUserId: String,
    private val openFoodFactsClient: OpenFoodFactsClient = OpenFoodFactsClient(),
    private val plateAiAnalyzer: PlateAiAnalyzer = PlateAiAnalyzer()
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _selectedDateKey = MutableStateFlow(dateFormatter.format(Date()))
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _scannerState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val scannerState: StateFlow<ScannerUiState> = _scannerState.asStateFlow()

    private val _plateAiState = MutableStateFlow<PlateAiUiState>(PlateAiUiState.Idle)
    val plateAiState: StateFlow<PlateAiUiState> = _plateAiState.asStateFlow()

    val profileState: StateFlow<UiDataState<NutritionProfile>> =
        repository.observeProfile(currentUserId)
            .map<NutritionProfile?, UiDataState<NutritionProfile>> { profile ->
                UiDataState.Success(profile ?: NutritionProfile(userId = currentUserId))
            }
            .catch { error ->
                Log.w(TAG, "Error observing profile", error)
                emit(UiDataState.Error(error.localizedMessage ?: "Error al cargar el perfil nutricional"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiDataState.Loading
            )

    val dailyMealLogsState: StateFlow<UiDataState<List<MealLog>>> =
        _selectedDateKey
            .flatMapLatest { dateKey ->
                repository.observeMealLogsForDate(dateKey, currentUserId)
                    .map<List<MealLog>, UiDataState<List<MealLog>>> { UiDataState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing meal logs for $dateKey", error)
                        emit(UiDataState.Error(error.localizedMessage ?: "Error al cargar comidas"))
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiDataState.Loading
            )

    val dailyMetricState: StateFlow<UiDataState<DailyMetric>> =
        _selectedDateKey
            .flatMapLatest { dateKey ->
                repository.observeDailyMetric(dateKey, currentUserId)
                    .map<DailyMetric?, UiDataState<DailyMetric>> { metric ->
                        val fallbackWeight = (profileState.value as? UiDataState.Success)?.data?.weightKg ?: 75.0
                        UiDataState.Success(
                            metric ?: DailyMetric(
                                userId = currentUserId,
                                dateKey = dateKey,
                                waterMl = 0L,
                                loggedWeightKg = fallbackWeight
                            )
                        )
                    }
                    .catch { error ->
                        Log.w(TAG, "Error observing daily metric for $dateKey", error)
                        emit(UiDataState.Error(error.localizedMessage ?: "Error al cargar métricas diarias"))
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiDataState.Loading
            )

    val recentMetricsState: StateFlow<UiDataState<List<DailyMetric>>> =
        repository.observeRecentDailyMetrics(currentUserId)
            .map<List<DailyMetric>, UiDataState<List<DailyMetric>>> { UiDataState.Success(it.take(14)) }
            .catch { error ->
                Log.w(TAG, "Error observing recent metrics", error)
                emit(UiDataState.Error(error.localizedMessage ?: "Error al cargar historial"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiDataState.Loading
            )

    fun shiftSelectedDate(daysDelta: Int) {
        try {
            val current = dateFormatter.parse(_selectedDateKey.value) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = current
                add(Calendar.DAY_OF_YEAR, daysDelta)
            }
            _selectedDateKey.value = dateFormatter.format(cal.time)
        } catch (e: Exception) {
            _selectedDateKey.value = dateFormatter.format(Date())
        }
    }

    fun selectToday() {
        _selectedDateKey.value = dateFormatter.format(Date())
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun logFoodItem(
        mealType: MealType,
        foodName: String,
        brandOrSource: String,
        servingGrams: Int,
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        barcode: String? = null,
        ingredientsSummary: String? = null,
        nutriScore: String? = null
    ) {
        viewModelScope.launch {
            val entry = MealLog(
                userId = currentUserId,
                dateKey = _selectedDateKey.value,
                mealType = mealType.name,
                foodName = foodName.trim().ifEmpty { "Alimento" },
                brandOrSource = brandOrSource.trim().ifEmpty { "Manual" },
                servingGrams = servingGrams.toLong().coerceIn(1L, 5000L),
                calories = calories.toLong().coerceIn(0L, 10000L),
                proteinG = proteinG.toLong().coerceIn(0L, 1000L),
                carbsG = carbsG.toLong().coerceIn(0L, 1000L),
                fatG = fatG.toLong().coerceIn(0L, 1000L),
                barcode = barcode,
                ingredientsSummary = ingredientsSummary,
                nutriScore = nutriScore
            )
            val result = repository.addMealLog(entry)
            if (result.isSuccess) {
                _statusBannerMessage.value = "Añadido a ${mealType.labelEs}: ${entry.foodName} (${entry.calories} kcal)"
            } else {
                _statusBannerMessage.value = "Error al guardar alimento: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteMealLog(logId: String) {
        viewModelScope.launch {
            val result = repository.deleteMealLog(logId)
            if (result.isSuccess) {
                _statusBannerMessage.value = "Registro eliminado"
            } else {
                _statusBannerMessage.value = "No se pudo eliminar: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addWaterMl(deltaMl: Int) {
        viewModelScope.launch {
            val currentMetric = (dailyMetricState.value as? UiDataState.Success)?.data
            val currentWater = currentMetric?.waterMl ?: 0L
            val currentWeight = currentMetric?.loggedWeightKg
                ?: (profileState.value as? UiDataState.Success)?.data?.weightKg
                ?: 75.0
            val newWater = (currentWater + deltaMl).coerceIn(0L, 15000L)
            repository.updateDailyMetric(
                dateKey = _selectedDateKey.value,
                waterMl = newWater,
                loggedWeightKg = currentWeight
            )
        }
    }

    fun updateLoggedWeight(newWeightKg: Double) {
        viewModelScope.launch {
            val currentMetric = (dailyMetricState.value as? UiDataState.Success)?.data
            val currentWater = currentMetric?.waterMl ?: 0L
            val clampedWeight = newWeightKg.coerceIn(30.0, 300.0)
            val res = repository.updateDailyMetric(
                dateKey = _selectedDateKey.value,
                waterMl = currentWater,
                loggedWeightKg = clampedWeight
            )
            if (res.isSuccess) {
                _statusBannerMessage.value = "Peso del día actualizado a ${String.format(Locale.US, "%.1f", clampedWeight)} kg"
            }
        }
    }

    fun recalculateAndSaveProfile(
        weightKg: Double,
        targetWeightKg: Double,
        heightCm: Long,
        age: Long,
        activityLevel: ActivityLevel,
        goalType: GoalType
    ) {
        viewModelScope.launch {
            val calculated = NutritionProfile.calculateTargets(
                weightKg = weightKg,
                targetWeightKg = targetWeightKg,
                heightCm = heightCm,
                age = age,
                activity = activityLevel,
                goal = goalType
            ).copy(userId = currentUserId)

            val res = repository.saveProfile(calculated)
            if (res.isSuccess) {
                _statusBannerMessage.value = "Metas actualizadas: ${calculated.dailyCalorieGoal} kcal/día"
            } else {
                _statusBannerMessage.value = "Error al guardar metas: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun lookupBarcode(barcode: String) {
        val cleaned = barcode.trim()
        if (cleaned.isEmpty()) return
        // Avoid duplicate triggers if already loading or showing same barcode
        val current = _scannerState.value
        if (current is ScannerUiState.Loading) return
        if (current is ScannerUiState.ProductFound && current.product.barcode == cleaned) return

        viewModelScope.launch {
            _scannerState.value = ScannerUiState.Loading("Consultando código $cleaned en Open Food Facts...")
            val result = openFoodFactsClient.fetchProductByBarcode(cleaned)
            result.fold(
                onSuccess = { product ->
                    _scannerState.value = ScannerUiState.ProductFound(product)
                },
                onFailure = { error ->
                    _scannerState.value = ScannerUiState.Error(
                        error.localizedMessage ?: "No se encontró el producto con código $cleaned"
                    )
                }
            )
        }
    }

    fun searchOpenFoodFactsByName(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _scannerState.value = ScannerUiState.Error("Escribe al menos 2 letras para buscar en Open Food Facts.")
            return
        }
        viewModelScope.launch {
            _scannerState.value = ScannerUiState.Loading("Buscando \"$trimmed\" en Open Food Facts España...")
            val result = openFoodFactsClient.searchProductsByName(trimmed)
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _scannerState.value = ScannerUiState.Error("No se encontraron productos para \"$trimmed\".")
                    } else {
                        _scannerState.value = ScannerUiState.SearchResults(trimmed, list)
                    }
                },
                onFailure = { error ->
                    _scannerState.value = ScannerUiState.Error(
                        error.localizedMessage ?: "Error de conexión con Open Food Facts"
                    )
                }
            )
        }
    }

    fun selectProductFromSearch(product: ScannedProductInfo) {
        _scannerState.value = ScannerUiState.ProductFound(product)
    }

    fun resetScannerState() {
        _scannerState.value = ScannerUiState.Idle
    }

    fun analyzePlatePhoto(bitmap: Bitmap, userHint: String = "") {
        viewModelScope.launch {
            _plateAiState.value = PlateAiUiState.Analyzing(bitmap)
            val result = plateAiAnalyzer.analyzePlatePhoto(bitmap, userHint)
            result.fold(
                onSuccess = { analysis ->
                    _plateAiState.value = PlateAiUiState.Analyzed(bitmap, analysis)
                },
                onFailure = { error ->
                    _plateAiState.value = PlateAiUiState.Error(
                        message = error.localizedMessage ?: "Error al analizar la foto con Gemini AI.",
                        bitmap = bitmap
                    )
                }
            )
        }
    }

    fun resetPlateAiState() {
        _plateAiState.value = PlateAiUiState.Idle
    }

    private companion object {
        const val TAG = "NutritionViewModel"
    }
}
