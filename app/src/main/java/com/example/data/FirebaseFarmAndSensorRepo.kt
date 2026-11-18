package com.example.data

import android.content.Context
import com.example.database.AgroGuardDatabase
import com.example.database.AgroGuardSyncWorker
import com.example.database.CropZoneEntity
import com.example.database.Esp32DeviceEntity
import com.example.database.FarmEntity
import com.example.database.PendingSyncEntity
import com.example.database.SensorReadingEntity
import com.example.domain.RolePermissionGuard
import com.example.domain.SensorReadingValidator
import com.example.models.CropZone
import com.example.models.DeviceConnectionStatus
import com.example.models.Esp32Device
import com.example.models.Farm
import com.example.models.SensorReading
import com.example.models.TelemetrySource
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

class FirebaseFarmRepository(
    private val db: FirebaseFirestore,
    private val localDb: AgroGuardDatabase,
    private val settings: SettingsDataStore,
    private val appContext: Context
) : FarmRepository {

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    private fun scopedQuery(collection: String, userId: String, role: UserRole): Query =
        when (role) {
            UserRole.ADMIN -> db.collection(collection)
            UserRole.EXPERT -> db.collection(collection).whereArrayContains("sharedExpertIds", userId)
            UserRole.FARMER -> db.collection(collection).whereEqualTo("ownerId", userId)
        }

    override fun observeFarms(userId: String, role: UserRole): Flow<List<Farm>> = flow {
        require(userId.isNotBlank()) { "Authenticated UID required." }
        emitAll(
            scopedQuery("farms", userId, role)
                .snapshots()
                .map { snap ->
                    val list = snap.toObjects(Farm::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                    localDb.farmDao().upsertAll(list.map { FarmEntity.fromDomain(it) })
                    settings.updateLastSynced(System.currentTimeMillis())
                    list
                }
                .catch { error ->
                    unwrapAndHandle(error, OperationType.LIST, "farms", userId)
                    throw error
                }
        )
    }

    override fun observeCachedFarms(): Flow<List<Farm>> =
        localDb.farmDao().observeFarms().map { rows -> rows.map { it.toDomain() } }

    override fun observeCropZones(userId: String, role: UserRole): Flow<List<CropZone>> = flow {
        require(userId.isNotBlank()) { "Authenticated UID required." }
        emitAll(
            scopedQuery("crop_zones", userId, role)
                .snapshots()
                .map { snap ->
                    val list = snap.toObjects(CropZone::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                    localDb.cropZoneDao().upsertAll(list.map { CropZoneEntity.fromDomain(it) })
                    list
                }
                .catch { error ->
                    unwrapAndHandle(error, OperationType.LIST, "crop_zones", userId)
                    throw error
                }
        )
    }

    override fun observeCachedZones(): Flow<List<CropZone>> =
        localDb.cropZoneDao().observeAllZones().map { rows -> rows.map { it.toDomain() } }

    override suspend fun createFarm(farm: Farm): Result<String> {
        var path = "farms"
        return try {
            val uid = requireUserId()
            require(farm.name.trim().length in 2..80) { "Farm name must be 2–80 characters." }
            require(farm.locationName.trim().length in 2..120) { "Location name must be 2–120 characters." }
            require(farm.areaAcres > 0.0 && farm.areaAcres <= 50000.0) { "Area must be positive." }
            require(farm.latitude in -90.0..90.0 && farm.longitude in -180.0..180.0) { "Invalid coordinates." }

            val docRef = db.collection("farms").document()
            path = docRef.path
            val payload = mapOf(
                "ownerId" to uid,
                "name" to farm.name.trim(),
                "locationName" to farm.locationName.trim(),
                "latitude" to farm.latitude,
                "longitude" to farm.longitude,
                "areaAcres" to farm.areaAcres,
                "primaryCrop" to farm.resolvedCrop().name,
                "sharedExpertIds" to farm.sharedExpertIds.take(10),
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

    override suspend fun shareFarmWithExpert(
        farmId: String,
        expertUid: String,
        currentShared: List<String>
    ): Result<Unit> {
        val path = "farms/$farmId"
        return try {
            requireUserId()
            require(farmId.isNotBlank() && expertUid.isNotBlank()) { "Farm ID and Expert UID required." }
            val updated = (currentShared + expertUid).distinct().take(10)
            db.collection("farms").document(farmId)
                .update(
                    mapOf(
                        "sharedExpertIds" to updated,
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

    override suspend fun createCropZone(zone: CropZone): Result<String> {
        var path = "crop_zones"
        return try {
            val uid = requireUserId()
            require(zone.farmId.isNotBlank()) { "Select a farm first." }
            require(zone.name.trim().length in 2..80) { "Zone name must be 2–80 characters." }
            require(zone.areaAcres > 0.0 && zone.areaAcres <= 50000.0) { "Area must be positive." }
            require(zone.minSoilMoisturePct in 0.0..100.0 && zone.maxSoilMoisturePct in zone.minSoilMoisturePct..100.0) {
                "Invalid moisture threshold range."
            }
            require(zone.maxAirTempC in 10.0..60.0) { "Max air temperature must be 10–60°C." }

            val docRef = db.collection("crop_zones").document()
            path = docRef.path
            val payload = mapOf(
                "farmId" to zone.farmId,
                "ownerId" to uid,
                "sharedExpertIds" to zone.sharedExpertIds.take(10),
                "name" to zone.name.trim(),
                "cropType" to zone.resolvedCrop().name,
                "variety" to zone.variety?.trim()?.takeIf { it.isNotBlank() }?.take(80),
                "plantingDateEpochMs" to zone.plantingDateEpochMs.coerceAtLeast(1L),
                "growthStage" to zone.resolvedStage().name,
                "areaAcres" to zone.areaAcres,
                "minSoilMoisturePct" to zone.minSoilMoisturePct,
                "maxSoilMoisturePct" to zone.maxSoilMoisturePct,
                "maxAirTempC" to zone.maxAirTempC,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateZoneThresholds(
        zoneId: String,
        minMoisture: Double,
        maxMoisture: Double,
        maxAirTemp: Double,
        isOnline: Boolean
    ): Result<Unit> {
        val path = "crop_zones/$zoneId"
        return try {
            requireUserId()
            require(minMoisture in 0.0..100.0 && maxMoisture in minMoisture..100.0) { "Invalid moisture range." }
            require(maxAirTemp in 10.0..60.0) { "Invalid temperature limit." }

            if (!isOnline) {
                val payload = JSONObject().apply {
                    put("minSoilMoisturePct", minMoisture)
                    put("maxSoilMoisturePct", maxMoisture)
                    put("maxAirTempC", maxAirTemp)
                }.toString()
                localDb.pendingSyncDao().enqueue(
                    PendingSyncEntity(
                        entityType = "ZONE_THRESHOLDS",
                        documentId = zoneId,
                        operation = "UPDATE",
                        payloadJson = payload
                    )
                )
                AgroGuardSyncWorker.enqueueWhenConnected(appContext)
                return Result.success(Unit)
            }

            db.collection("crop_zones").document(zoneId)
                .update(
                    mapOf(
                        "minSoilMoisturePct" to minMoisture,
                        "maxSoilMoisturePct" to maxMoisture,
                        "maxAirTempC" to maxAirTemp,
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

    private fun unwrapAndHandle(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}

class FirebaseSensorRepository(
    private val db: FirebaseFirestore,
    private val localDb: AgroGuardDatabase
) : SensorRepository {

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    private fun scopedQuery(collection: String, userId: String, role: UserRole): Query =
        when (role) {
            UserRole.ADMIN -> db.collection(collection)
            UserRole.EXPERT -> db.collection(collection).whereArrayContains("sharedExpertIds", userId)
            UserRole.FARMER -> db.collection(collection).whereEqualTo("ownerId", userId)
        }

    override fun observeDevices(userId: String, role: UserRole): Flow<List<Esp32Device>> = flow {
        require(userId.isNotBlank())
        emitAll(
            scopedQuery("esp32_devices", userId, role)
                .snapshots()
                .map { snap ->
                    val devices = snap.toObjects(Esp32Device::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    localDb.esp32DeviceDao().upsertAll(devices.map { Esp32DeviceEntity.fromDomain(it) })
                    devices
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, "esp32_devices")
                    }
                    throw error
                }
        )
    }

    override fun observeCachedDevices(): Flow<List<Esp32Device>> =
        localDb.esp32DeviceDao().observeAllDevices().map { list -> list.map { it.toDomain() } }

    override fun observeReadings(userId: String, role: UserRole): Flow<List<SensorReading>> = flow {
        require(userId.isNotBlank())
        emitAll(
            scopedQuery("sensor_readings", userId, role)
                .snapshots()
                .map { snap ->
                    val readings = snap.toObjects(SensorReading::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.recordedEpochMs }
                    localDb.sensorReadingDao().upsertAll(readings.map { SensorReadingEntity.fromDomain(it) })
                    readings
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, "sensor_readings")
                    }
                    throw error
                }
        )
    }

    override fun observeCachedReadings(): Flow<List<SensorReading>> =
        localDb.sensorReadingDao().observeAllReadings().map { list -> list.map { it.toDomain() } }

    override suspend fun registerEsp32Device(device: Esp32Device): Result<String> {
        var path = "esp32_devices"
        return try {
            val uid = requireUserId()
            require(device.id.matches(Regex("^[a-zA-Z0-9_\\-]{3,64}$"))) {
                "Device ID must be 3–64 alphanumeric, '-' or '_' characters."
            }
            require(device.label.trim().length in 2..80) { "Device label must be 2–80 characters." }

            val docRef = db.collection("esp32_devices").document(device.id)
            path = docRef.path
            val payload = mapOf(
                "farmId" to device.farmId,
                "cropZoneId" to device.cropZoneId,
                "ownerId" to uid,
                "sharedExpertIds" to device.sharedExpertIds.take(10),
                "label" to device.label.trim(),
                "connectionStatus" to DeviceConnectionStatus.UNCONNECTED.name,
                "lastSeenEpochMs" to 0L,
                "firmwareVersion" to device.firmwareVersion.ifBlank { "v1.0.0" }.take(32),
                "credentialHint" to device.credentialHint.ifBlank { "provision-via-backend" }.take(40),
                "revoked" to false,
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

    override suspend fun revokeDevice(deviceId: String): Result<Unit> {
        val path = "esp32_devices/$deviceId"
        return try {
            requireUserId()
            db.collection("esp32_devices").document(deviceId)
                .update(
                    mapOf(
                        "connectionStatus" to DeviceConnectionStatus.REVOKED.name,
                        "revoked" to true,
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

    override suspend fun recordManualReading(reading: SensorReading): Result<String> {
        var path = "sensor_readings"
        return try {
            val uid = requireUserId()
            val enforcedSource = TelemetrySource.MANUAL_LOG
            require(RolePermissionGuard.canClientSubmitTelemetrySource(enforcedSource)) {
                "Phone clients cannot submit trusted ESP32 telemetry."
            }
            val normalized = reading.copy(
                ownerId = uid,
                deviceId = reading.deviceId.ifBlank { "manual_field_entry" },
                source = enforcedSource.name
            )
            val validation = SensorReadingValidator.validate(normalized)
            require(validation.valid) { validation.errors.joinToString(" ") }

            val docRef = db.collection("sensor_readings").document()
            path = docRef.path
            val payload = mapOf(
                "farmId" to normalized.farmId,
                "cropZoneId" to normalized.cropZoneId,
                "deviceId" to normalized.deviceId,
                "ownerId" to uid,
                "sharedExpertIds" to normalized.sharedExpertIds.take(10),
                "source" to enforcedSource.name,
                "soilMoisturePct" to normalized.soilMoisturePct,
                "soilTempC" to normalized.soilTempC,
                "airTempC" to normalized.airTempC,
                "humidityPct" to normalized.humidityPct,
                "lightLux" to normalized.lightLux,
                "rainfallMm" to normalized.rainfallMm,
                "waterLevelCm" to normalized.waterLevelCm,
                "recordedEpochMs" to normalized.recordedEpochMs,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
