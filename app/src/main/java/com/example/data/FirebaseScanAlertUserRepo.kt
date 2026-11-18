package com.example.data

import android.content.Context
import com.example.database.AgroGuardDatabase
import com.example.database.AgroGuardSyncWorker
import com.example.database.AlertEntity
import com.example.database.CropScanEntity
import com.example.database.PendingSyncEntity
import com.example.models.ConsultationMessage
import com.example.models.CropScanRecord
import com.example.models.ExpertDirectoryEntry
import com.example.models.FarmAlert
import com.example.models.UserProfile
import com.example.models.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class FirebaseAlertRepository(
    private val db: FirebaseFirestore,
    private val localDb: AgroGuardDatabase,
    private val appContext: Context
) : AlertRepository {

    private val auth = Firebase.auth

    private fun scopedQuery(userId: String, role: UserRole): Query =
        when (role) {
            UserRole.ADMIN -> db.collection("alerts")
            UserRole.EXPERT -> db.collection("alerts").whereArrayContains("sharedExpertIds", userId)
            UserRole.FARMER -> db.collection("alerts").whereEqualTo("ownerId", userId)
        }

    override fun observeAlerts(userId: String, role: UserRole): Flow<List<FarmAlert>> = flow {
        require(userId.isNotBlank())
        emitAll(
            scopedQuery(userId, role)
                .snapshots()
                .map { snap ->
                    val list = snap.toObjects(FarmAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    localDb.alertDao().upsertAll(list.map { AlertEntity.fromDomain(it) })
                    list
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, "alerts")
                    }
                    throw error
                }
        )
    }

    override fun observeCachedAlerts(): Flow<List<FarmAlert>> =
        localDb.alertDao().observeAllAlerts().map { list -> list.map { it.toDomain() } }

    override suspend fun createAlert(alert: FarmAlert): Result<String> {
        var path = "alerts"
        return try {
            val uid = auth.currentUser?.uid ?: error("Sign in required.")
            val docRef = db.collection("alerts").document()
            path = docRef.path
            val payload = mapOf(
                "farmId" to alert.farmId,
                "cropZoneId" to alert.cropZoneId,
                "ownerId" to uid,
                "sharedExpertIds" to alert.sharedExpertIds.take(10),
                "alertType" to alert.resolvedType().name,
                "severity" to alert.resolvedSeverity().name,
                "title" to alert.title.trim().take(120),
                "explanation" to alert.explanation.trim().take(500),
                "acknowledged" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun acknowledgeAlert(alertId: String, isOnline: Boolean): Result<Unit> {
        val path = "alerts/$alertId"
        return try {
            auth.currentUser?.uid ?: error("Sign in required.")
            if (!isOnline) {
                val payload = JSONObject().put("acknowledged", true).toString()
                localDb.pendingSyncDao().enqueue(
                    PendingSyncEntity(
                        entityType = "ALERT_ACK",
                        documentId = alertId,
                        operation = "UPDATE",
                        payloadJson = payload
                    )
                )
                AgroGuardSyncWorker.enqueueWhenConnected(appContext)
                return Result.success(Unit)
            }
            db.collection("alerts").document(alertId)
                .update(mapOf("acknowledged" to true, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}

class FirebaseScanRepository(
    private val db: FirebaseFirestore,
    private val localDb: AgroGuardDatabase
) : ScanRepository {

    private val auth = Firebase.auth

    private fun scopedQuery(userId: String, role: UserRole): Query =
        when (role) {
            UserRole.ADMIN -> db.collection("crop_scans")
            UserRole.EXPERT -> db.collection("crop_scans").whereArrayContains("sharedExpertIds", userId)
            UserRole.FARMER -> db.collection("crop_scans").whereEqualTo("ownerId", userId)
        }

    override fun observeScans(userId: String, role: UserRole): Flow<List<CropScanRecord>> = flow {
        require(userId.isNotBlank())
        emitAll(
            scopedQuery(userId, role)
                .snapshots()
                .map { snap ->
                    val list = snap.toObjects(CropScanRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    localDb.cropScanDao().upsertAll(list.map { CropScanEntity.fromDomain(it) })
                    list
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, "crop_scans")
                    }
                    throw error
                }
        )
    }

    override fun observeCachedScans(): Flow<List<CropScanRecord>> =
        localDb.cropScanDao().observeAllScans().map { list -> list.map { it.toDomain() } }

    override suspend fun saveScan(scan: CropScanRecord): Result<String> {
        var path = "crop_scans"
        return try {
            val uid = auth.currentUser?.uid ?: error("Sign in required.")
            val docRef = db.collection("crop_scans").document()
            path = docRef.path
            val payload = mapOf(
                "farmId" to scan.farmId,
                "cropZoneId" to scan.cropZoneId,
                "ownerId" to uid,
                "sharedExpertIds" to scan.sharedExpertIds.take(10),
                "cropType" to scan.resolvedCrop().name,
                "imageUri" to scan.imageUri.take(500),
                "identifiedCrop" to scan.identifiedCrop.take(100),
                "possibleCondition" to scan.possibleCondition.take(160),
                "visibleSymptoms" to scan.visibleSymptoms.take(10),
                "confidencePct" to scan.confidencePct.coerceIn(0L, 100L),
                "severity" to scan.resolvedSeverity().name,
                "recommendedNextSteps" to scan.recommendedNextSteps.take(10),
                "uncertaintyNotes" to scan.uncertaintyNotes.take(600),
                "expertConfirmed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun verifyScanAsExpert(scanId: String, confirmed: Boolean, notes: String): Result<Unit> {
        val path = "crop_scans/$scanId"
        return try {
            auth.currentUser?.uid ?: error("Sign in required.")
            db.collection("crop_scans").document(scanId)
                .update(
                    mapOf(
                        "expertConfirmed" to confirmed,
                        "expertNotes" to notes.trim().take(600),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}

class FirebaseUserAndAdminRepository(
    private val db: FirebaseFirestore
) : UserAndAdminRepository {

    private val auth = Firebase.auth

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank())
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, "users/$userId")
                    }
                    throw error
                }
        )
    }

    override fun observeExpertDirectory(): Flow<List<ExpertDirectoryEntry>> = flow {
        emitAll(
            db.collection("expert_directory")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ExpertDirectoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "expert_directory")
                    throw error
                }
        )
    }

    override fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users")
                    throw error
                }
        )
    }

    override suspend fun upsertFarmerProfile(profile: UserProfile, isNew: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sign in required."))
        val path = "users/$uid"
        return try {
            require(profile.displayName.trim().length in 1..80) { "Display name is required (1–80 chars)." }
            val docRef = db.collection("users").document(uid)
            if (isNew) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to profile.displayName.trim(),
                    "email" to profile.email.trim().take(120),
                    "role" to UserRole.FARMER.name,
                    "district" to profile.district.trim().take(80),
                    "phone" to profile.phone.trim().take(25),
                    "preferredLanguage" to profile.resolvedLanguage().code,
                    "cropsGrown" to profile.cropsGrown.take(5),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to profile.displayName.trim(),
                        "district" to profile.district.trim().take(80),
                        "phone" to profile.phone.trim().take(25),
                        "preferredLanguage" to profile.resolvedLanguage().code,
                        "cropsGrown" to profile.cropsGrown.take(5),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateUserRoleAsAdmin(
        targetUserId: String,
        newRole: UserRole,
        userProfile: UserProfile?
    ): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            auth.currentUser?.uid ?: error("Sign in required.")
            require(newRole == UserRole.FARMER || newRole == UserRole.EXPERT) {
                "Admin promotion is restricted to backend provisioning."
            }
            db.collection("users").document(targetUserId)
                .update(mapOf("role" to newRole.name, "updatedAt" to FieldValue.serverTimestamp()))
                .await()

            if (newRole == UserRole.EXPERT && userProfile != null) {
                db.collection("expert_directory").document(targetUserId)
                    .set(
                        mapOf(
                            "userId" to targetUserId,
                            "displayName" to userProfile.displayName.ifBlank { "Agricultural Expert" }.take(80),
                            "specialty" to "Paddy, Banana & Tomato Agronomy",
                            "district" to userProfile.district.ifBlank { "Regional" }.take(80),
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}

class FirebaseConsultationRepository(
    private val db: FirebaseFirestore
) : ConsultationRepository {

    override fun observeMessagesForFarm(
        farmId: String,
        userId: String,
        role: UserRole
    ): Flow<List<ConsultationMessage>> = flow {
        require(farmId.isNotBlank() && userId.isNotBlank())
        val query = when (role) {
            UserRole.ADMIN -> db.collection("consultations").whereEqualTo("farmId", farmId)
            UserRole.EXPERT -> db.collection("consultations").whereArrayContains("sharedExpertIds", userId)
            UserRole.FARMER -> db.collection("consultations").whereEqualTo("ownerId", userId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(ConsultationMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.farmId == farmId }
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "consultations")
                    throw error
                }
        )
    }

    override suspend fun sendMessage(message: ConsultationMessage): Result<Unit> {
        var path = "consultations"
        return try {
            val docRef = db.collection("consultations").document()
            path = docRef.path
            val payload = mapOf(
                "farmId" to message.farmId,
                "ownerId" to message.ownerId,
                "sharedExpertIds" to message.sharedExpertIds.take(10),
                "senderId" to message.senderId,
                "senderName" to message.senderName.take(80),
                "senderRole" to message.senderRole,
                "messageText" to message.messageText.trim().take(1000),
                "scanRefId" to message.scanRefId?.takeIf { it.isNotBlank() },
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
