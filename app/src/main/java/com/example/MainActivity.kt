package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.DailyMetric
import com.example.data.model.MealType
import com.example.data.model.NutritionProfile
import com.example.data.repository.NutritionRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BarcodeScannerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsAndStatsScreen
import com.example.ui.screens.PlateAiScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.AthleticOrange
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonVoltGreen
import com.example.ui.theme.ObsidianBackground
import com.example.ui.viewmodel.NutritionViewModel
import com.example.ui.viewmodel.UiDataState
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NutriPulseTab {
    TODAY,
    SCANNER,
    AI_PHOTO,
    GOALS
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check initialization before UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NutriPulseRootNavigation()
                }
            }
        }
    }
}

@Composable
fun NutriPulseRootNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = { /* AuthStateListener updates currentUser automatically */ }
        )
    } else {
        NutriPulseAuthenticatedApp(
            currentUserId = user.uid,
            userEmail = user.email
        )
    }
}

@Composable
fun NutriPulseAuthenticatedApp(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: NutritionViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NutritionViewModel(
                    repository = NutritionRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    var activeTab by rememberSaveable { mutableStateOf(NutriPulseTab.TODAY) }
    var targetMealForAction by rememberSaveable { mutableStateOf(MealType.LUNCH) }

    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val profileUiState by viewModel.profileState.collectAsStateWithLifecycle()
    val mealLogsUiState by viewModel.dailyMealLogsState.collectAsStateWithLifecycle()
    val dailyMetricUiState by viewModel.dailyMetricState.collectAsStateWithLifecycle()
    val recentMetricsUiState by viewModel.recentMetricsState.collectAsStateWithLifecycle()
    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()
    val plateAiState by viewModel.plateAiState.collectAsStateWithLifecycle()

    val currentProfile = (profileUiState as? UiDataState.Success)?.data
        ?: NutritionProfile(userId = currentUserId)
    val currentMealLogs = (mealLogsUiState as? UiDataState.Success)?.data ?: emptyList()
    val currentMetric = (dailyMetricUiState as? UiDataState.Success)?.data
        ?: DailyMetric(
            userId = currentUserId,
            dateKey = selectedDateKey,
            waterMl = 0L,
            loggedWeightKg = currentProfile.weightKg
        )
    val recentMetrics = (recentMetricsUiState as? UiDataState.Success)?.data ?: emptyList()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = GraphiteCard,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == NutriPulseTab.TODAY,
                    onClick = { activeTab = NutriPulseTab.TODAY },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_today)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBackground,
                        selectedTextColor = AthleticOrange,
                        indicatorColor = AthleticOrange
                    ),
                    modifier = Modifier.testTag("nav_tab_today")
                )
                NavigationBarItem(
                    selected = activeTab == NutriPulseTab.SCANNER,
                    onClick = { activeTab = NutriPulseTab.SCANNER },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_scan)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBackground,
                        selectedTextColor = NeonVoltGreen,
                        indicatorColor = NeonVoltGreen
                    ),
                    modifier = Modifier.testTag("nav_tab_scan")
                )
                NavigationBarItem(
                    selected = activeTab == NutriPulseTab.AI_PHOTO,
                    onClick = { activeTab = NutriPulseTab.AI_PHOTO },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_ai_photo)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBackground,
                        selectedTextColor = AthleticOrange,
                        indicatorColor = AthleticOrange
                    ),
                    modifier = Modifier.testTag("nav_tab_ai_photo")
                )
                NavigationBarItem(
                    selected = activeTab == NutriPulseTab.GOALS,
                    onClick = { activeTab = NutriPulseTab.GOALS },
                    icon = { Icon(Icons.Default.TrackChanges, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_goals)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBackground,
                        selectedTextColor = NeonVoltGreen,
                        indicatorColor = NeonVoltGreen
                    ),
                    modifier = Modifier.testTag("nav_tab_goals")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = statusBanner != null) {
                if (statusBanner != null) {
                    Surface(
                        color = NeonVoltGreen.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusBanner!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonVoltGreen,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                            TextButton(
                                onClick = { viewModel.clearStatusBanner() },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Text("OK", color = NeonVoltGreen)
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    NutriPulseTab.TODAY -> {
                        DashboardScreen(
                            userEmail = userEmail,
                            selectedDateKey = selectedDateKey,
                            profile = currentProfile,
                            mealLogs = currentMealLogs,
                            dailyMetric = currentMetric,
                            onPreviousDay = { viewModel.shiftSelectedDate(-1) },
                            onNextDay = { viewModel.shiftSelectedDate(1) },
                            onTodayClick = { viewModel.selectToday() },
                            onOpenScanner = { meal ->
                                targetMealForAction = meal
                                activeTab = NutriPulseTab.SCANNER
                            },
                            onOpenAiPhoto = { meal ->
                                targetMealForAction = meal
                                activeTab = NutriPulseTab.AI_PHOTO
                            },
                            onManualLogFood = { meal, name, grams, kcal, pro, carb, fat ->
                                viewModel.logFoodItem(
                                    mealType = meal,
                                    foodName = name,
                                    brandOrSource = "Manual",
                                    servingGrams = grams,
                                    calories = kcal,
                                    proteinG = pro,
                                    carbsG = carb,
                                    fatG = fat
                                )
                            },
                            onDeleteMealLog = { logId -> viewModel.deleteMealLog(logId) },
                            onAddWaterMl = { delta -> viewModel.addWaterMl(delta) },
                            onUpdateWeightKg = { weight -> viewModel.updateLoggedWeight(weight) },
                            onSignOut = {
                                signOutUser(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = coroutineScope
                                )
                            }
                        )
                    }

                    NutriPulseTab.SCANNER -> {
                        BarcodeScannerScreen(
                            scannerState = scannerState,
                            defaultMealType = targetMealForAction,
                            onLookupBarcode = { code -> viewModel.lookupBarcode(code) },
                            onSearchByName = { query -> viewModel.searchOpenFoodFactsByName(query) },
                            onSelectProduct = { prod -> viewModel.selectProductFromSearch(prod) },
                            onResetScanner = { viewModel.resetScannerState() },
                            onLogProduct = { meal, product, grams, kcal, pro, carb, fat ->
                                viewModel.logFoodItem(
                                    mealType = meal,
                                    foodName = product.name,
                                    brandOrSource = "Open Food Facts (${product.brand})",
                                    servingGrams = grams,
                                    calories = kcal,
                                    proteinG = pro,
                                    carbsG = carb,
                                    fatG = fat,
                                    barcode = product.barcode,
                                    nutriScore = product.nutriScore
                                )
                                viewModel.resetScannerState()
                                activeTab = NutriPulseTab.TODAY
                            },
                            onBack = { activeTab = NutriPulseTab.TODAY }
                        )
                    }

                    NutriPulseTab.AI_PHOTO -> {
                        PlateAiScreen(
                            plateAiState = plateAiState,
                            defaultMealType = targetMealForAction,
                            onAnalyzePhoto = { bmp, hint -> viewModel.analyzePlatePhoto(bmp, hint) },
                            onResetAiState = { viewModel.resetPlateAiState() },
                            onLogAnalyzedPlate = { meal, name, grams, kcal, pro, carb, fat, ingredients ->
                                viewModel.logFoodItem(
                                    mealType = meal,
                                    foodName = name,
                                    brandOrSource = "Gemini AI Flash",
                                    servingGrams = grams,
                                    calories = kcal,
                                    proteinG = pro,
                                    carbsG = carb,
                                    fatG = fat,
                                    ingredientsSummary = ingredients
                                )
                                viewModel.resetPlateAiState()
                                activeTab = NutriPulseTab.TODAY
                            },
                            onBack = { activeTab = NutriPulseTab.TODAY }
                        )
                    }

                    NutriPulseTab.GOALS -> {
                        GoalsAndStatsScreen(
                            profile = currentProfile,
                            recentMetrics = recentMetrics,
                            onSaveProfileGoals = { weight, targetWeight, height, age, activity, goal ->
                                viewModel.recalculateAndSaveProfile(
                                    weightKg = weight,
                                    targetWeightKg = targetWeight,
                                    heightCm = height,
                                    age = age,
                                    activityLevel = activity,
                                    goalType = goal
                                )
                            },
                            onBack = { activeTab = NutriPulseTab.TODAY }
                        )
                    }
                }
            }
        }
    }
}
