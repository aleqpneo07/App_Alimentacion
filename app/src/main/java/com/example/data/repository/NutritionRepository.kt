package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DailyMetric
import com.example.data.model.MealLog
import com.example.data.model.NutritionProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NutritionRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String = requireUserId()): Flow<NutritionProfile?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("profile").document("main")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            NutritionProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveProfile(profile: NutritionProfile): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("profile").document("main")
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/profile/main")
            Result.failure(e)
        }
    }

    fun observeMealLogsForDate(dateKey: String, userId: String = requireUserId()): Flow<List<MealLog>> = flow {
        val collectionPath = "users/$userId/mealLogs"
        emitAll(
            db.collection("users").document(userId)
                .collection("mealLogs")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MealLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, collectionPath)
                    }
                    throw error
                }
        )
    }

    fun observeAllMealLogs(userId: String = requireUserId()): Flow<List<MealLog>> = flow {
        val collectionPath = "users/$userId/mealLogs"
        emitAll(
            db.collection("users").document(userId)
                .collection("mealLogs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MealLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, collectionPath)
                    }
                    throw error
                }
        )
    }

    suspend fun addMealLog(mealLog: MealLog): Result<String> {
        return try {
            val uid = requireUserId()
            val collectionRef = db.collection("users").document(uid).collection("mealLogs")
            val docRef = collectionRef.add(mealLog.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/mealLogs")
            Result.failure(e)
        }
    }

    suspend fun getMealLogById(ownerUserId: String, logId: String): Result<MealLog?> {
        val docRef = db.collection("users").document(ownerUserId)
            .collection("mealLogs").document(logId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(
                snapshot.toObject(
                    MealLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMealLog(logId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("mealLogs").document(logId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/mealLogs/$logId")
            Result.failure(e)
        }
    }

    fun observeDailyMetric(dateKey: String, userId: String = requireUserId()): Flow<DailyMetric?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("dailyMetrics").document(dateKey)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            DailyMetric::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeRecentDailyMetrics(userId: String = requireUserId()): Flow<List<DailyMetric>> = flow {
        val collectionPath = "users/$userId/dailyMetrics"
        emitAll(
            db.collection("users").document(userId)
                .collection("dailyMetrics")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyMetric::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.dateKey }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, collectionPath)
                    }
                    throw error
                }
        )
    }

    suspend fun updateDailyMetric(
        dateKey: String,
        waterMl: Long,
        loggedWeightKg: Double
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("dailyMetrics").document(dateKey)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updatePayload = mapOf(
                    "waterMl" to waterMl.coerceIn(0L, 15000L),
                    "loggedWeightKg" to loggedWeightKg.coerceIn(20.0, 400.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val metric = DailyMetric(
                    userId = uid,
                    dateKey = dateKey,
                    waterMl = waterMl,
                    loggedWeightKg = loggedWeightKg
                )
                docRef.set(metric.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/dailyMetrics/$dateKey")
            Result.failure(e)
        }
    }
}
