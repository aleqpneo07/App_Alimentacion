package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.MealLog
import com.example.data.model.MealType
import com.example.data.model.NutritionProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NutritionRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addMealLog_andObserve_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NutritionRepository(firestore)

        val log = MealLog(
            dateKey = "2026-11-02",
            mealType = MealType.BREAKFAST.name,
            foodName = "Tortilla de Avena y Claras",
            brandOrSource = "Gemini AI",
            servingGrams = 220L,
            calories = 340L,
            proteinG = 28L,
            carbsG = 36L,
            fatG = 8L,
            ingredientsSummary = "Claras de huevo (150g), Avena (50g), Arándanos (20g)"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMealLog(log)
        }
        assertTrue("Expected addMealLog to succeed", createResult.isSuccess)
        val createdId = createResult.getOrThrow()
        assertTrue(createdId.isNotBlank())

        val emittedLogs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMealLogsForDate("2026-11-02", aliceUid)
                .first { list -> list.any { it.id == createdId } }
        }
        assertTrue(emittedLogs.any { it.id == createdId && it.foodName == "Tortilla de Avena y Claras" })
    }

    @Test
    fun saveProfile_andUpdateDailyMetric_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NutritionRepository(firestore)

        val profile = NutritionProfile(
            userId = aliceUid,
            weightKg = 73.5,
            targetWeightKg = 69.0,
            heightCm = 176L,
            age = 29L,
            dailyCalorieGoal = 2050L,
            proteinGoalG = 150L,
            carbsGoalG = 205L,
            fatGoalG = 62L,
            waterGoalMl = 2500L
        )

        val saveProfileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveProfile(profile)
        }
        assertTrue("Expected saveProfile to succeed", saveProfileResult.isSuccess)

        val metricResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateDailyMetric("2026-11-02", waterMl = 1500L, loggedWeightKg = 73.4)
        }
        assertTrue("Expected updateDailyMetric to succeed", metricResult.isSuccess)
    }

    @Test
    fun getMealLogById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NutritionRepository(firestore)

        val logId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addMealLog(
                MealLog(
                    dateKey = "2026-11-02",
                    mealType = MealType.LUNCH.name,
                    foodName = "Pechuga de Pollo",
                    brandOrSource = "Manual",
                    servingGrams = 200L,
                    calories = 330L,
                    proteinG = 62L,
                    carbsG = 0L,
                    fatG = 7L
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = NutritionRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getMealLogById(aliceUid, logId)
        }
        assertTrue("Expected cross-user read to fail", crossResult.isFailure)
        val firestoreEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeMealLogs_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = NutritionRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAllMealLogs("some_user_uid").first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@nutripulse.test"
        const val BOB_EMAIL = "bob@nutripulse.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
