package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.models.CropScanRecord
import com.example.models.CropZone
import com.example.models.Esp32Device
import com.example.models.Farm
import com.example.models.FarmAlert
import com.example.models.SensorReading
import com.google.firebase.Timestamp

@Entity(tableName = "farms")
data class FarmEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val areaAcres: Double,
    val primaryCrop: String,
    val sharedExpertIdsCsv: String,
    val cachedAtEpochMs: Long
) {
    fun toDomain(): Farm = Farm(
        id = id,
        ownerId = ownerId,
        name = name,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        areaAcres = areaAcres,
        primaryCrop = primaryCrop,
        sharedExpertIds = sharedExpertIdsCsv.split(",").filter { it.isNotBlank() },
        updatedAt = Timestamp(cachedAtEpochMs / 1000L, 0)
    )

    companion object {
        fun fromDomain(farm: Farm, nowMs: Long = System.currentTimeMillis()): FarmEntity = FarmEntity(
            id = farm.id,
            ownerId = farm.ownerId,
            name = farm.name,
            locationName = farm.locationName,
            latitude = farm.latitude,
            longitude = farm.longitude,
            areaAcres = farm.areaAcres,
            primaryCrop = farm.primaryCrop,
            sharedExpertIdsCsv = farm.sharedExpertIds.joinToString(","),
            cachedAtEpochMs = nowMs
        )
    }
}

@Entity(tableName = "crop_zones")
data class CropZoneEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val ownerId: String,
    val sharedExpertIdsCsv: String,
    val name: String,
    val cropType: String,
    val variety: String?,
    val plantingDateEpochMs: Long,
    val growthStage: String,
    val areaAcres: Double,
    val minSoilMoisturePct: Double,
    val maxSoilMoisturePct: Double,
    val maxAirTempC: Double,
    val cachedAtEpochMs: Long
) {
    fun toDomain(): CropZone = CropZone(
        id = id,
        farmId = farmId,
        ownerId = ownerId,
        sharedExpertIds = sharedExpertIdsCsv.split(",").filter { it.isNotBlank() },
        name = name,
        cropType = cropType,
        variety = variety,
        plantingDateEpochMs = plantingDateEpochMs,
        growthStage = growthStage,
        areaAcres = areaAcres,
        minSoilMoisturePct = minSoilMoisturePct,
        maxSoilMoisturePct = maxSoilMoisturePct,
        maxAirTempC = maxAirTempC
    )

    companion object {
        fun fromDomain(zone: CropZone, nowMs: Long = System.currentTimeMillis()): CropZoneEntity = CropZoneEntity(
            id = zone.id,
            farmId = zone.farmId,
            ownerId = zone.ownerId,
            sharedExpertIdsCsv = zone.sharedExpertIds.joinToString(","),
            name = zone.name,
            cropType = zone.cropType,
            variety = zone.variety,
            plantingDateEpochMs = zone.plantingDateEpochMs,
            growthStage = zone.growthStage,
            areaAcres = zone.areaAcres,
            minSoilMoisturePct = zone.minSoilMoisturePct,
            maxSoilMoisturePct = zone.maxSoilMoisturePct,
            maxAirTempC = zone.maxAirTempC,
            cachedAtEpochMs = nowMs
        )
    }
}

@Entity(tableName = "esp32_devices")
data class Esp32DeviceEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val cropZoneId: String,
    val ownerId: String,
    val sharedExpertIdsCsv: String,
    val label: String,
    val connectionStatus: String,
    val lastSeenEpochMs: Long,
    val firmwareVersion: String,
    val credentialHint: String,
    val revoked: Boolean
) {
    fun toDomain(): Esp32Device = Esp32Device(
        id = id,
        farmId = farmId,
        cropZoneId = cropZoneId,
        ownerId = ownerId,
        sharedExpertIds = sharedExpertIdsCsv.split(",").filter { it.isNotBlank() },
        label = label,
        connectionStatus = connectionStatus,
        lastSeenEpochMs = lastSeenEpochMs,
        firmwareVersion = firmwareVersion,
        credentialHint = credentialHint,
        revoked = revoked
    )

    companion object {
        fun fromDomain(device: Esp32Device): Esp32DeviceEntity = Esp32DeviceEntity(
            id = device.id,
            farmId = device.farmId,
            cropZoneId = device.cropZoneId,
            ownerId = device.ownerId,
            sharedExpertIdsCsv = device.sharedExpertIds.joinToString(","),
            label = device.label,
            connectionStatus = device.connectionStatus,
            lastSeenEpochMs = device.lastSeenEpochMs,
            firmwareVersion = device.firmwareVersion,
            credentialHint = device.credentialHint,
            revoked = device.revoked
        )
    }
}

@Entity(tableName = "sensor_readings")
data class SensorReadingEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val cropZoneId: String,
    val deviceId: String,
    val ownerId: String,
    val sharedExpertIdsCsv: String,
    val source: String,
    val soilMoisturePct: Double,
    val soilTempC: Double,
    val airTempC: Double,
    val humidityPct: Double,
    val lightLux: Double,
    val rainfallMm: Double,
    val waterLevelCm: Double?,
    val recordedEpochMs: Long
) {
    fun toDomain(): SensorReading = SensorReading(
        id = id,
        farmId = farmId,
        cropZoneId = cropZoneId,
        deviceId = deviceId,
        ownerId = ownerId,
        sharedExpertIds = sharedExpertIdsCsv.split(",").filter { it.isNotBlank() },
        source = source,
        soilMoisturePct = soilMoisturePct,
        soilTempC = soilTempC,
        airTempC = airTempC,
        humidityPct = humidityPct,
        lightLux = lightLux,
        rainfallMm = rainfallMm,
        waterLevelCm = waterLevelCm,
        recordedEpochMs = recordedEpochMs
    )

    companion object {
        fun fromDomain(r: SensorReading): SensorReadingEntity = SensorReadingEntity(
            id = r.id,
            farmId = r.farmId,
            cropZoneId = r.cropZoneId,
            deviceId = r.deviceId,
            ownerId = r.ownerId,
            sharedExpertIdsCsv = r.sharedExpertIds.joinToString(","),
            source = r.source,
            soilMoisturePct = r.soilMoisturePct,
            soilTempC = r.soilTempC,
            airTempC = r.airTempC,
            humidityPct = r.humidityPct,
            lightLux = r.lightLux,
            rainfallMm = r.rainfallMm,
            waterLevelCm = r.waterLevelCm,
            recordedEpochMs = r.recordedEpochMs
        )
    }
}

@Entity(tableName = "crop_scans")
data class CropScanEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val cropZoneId: String,
    val ownerId: String,
    val sharedExpertIdsCsv: String,
    val cropType: String,
    val imageUri: String,
    val identifiedCrop: String,
    val possibleCondition: String,
    val visibleSymptomsJoined: String,
    val confidencePct: Long,
    val severity: String,
    val nextStepsJoined: String,
    val uncertaintyNotes: String,
    val expertConfirmed: Boolean,
    val expertNotes: String?,
    val cachedAtEpochMs: Long
) {
    fun toDomain(): CropScanRecord = CropScanRecord(
        id = id,
        farmId = farmId,
        cropZoneId = cropZoneId,
        ownerId = ownerId,
        sharedExpertIds = sharedExpertIdsCsv.split(",").filter { it.isNotBlank() },
        cropType = cropType,
        imageUri = imageUri,
        identifiedCrop = identifiedCrop,
        possibleCondition = possibleCondition,
        visibleSymptoms = visibleSymptomsJoined.split("||").filter { it.isNotBlank() },
        confidencePct = confidencePct,
        severity = severity,
        recommendedNextSteps = nextStepsJoined.split("||").filter { it.isNotBlank() },
        uncertaintyNotes = uncertaintyNotes,
        expertConfirmed = expertConfirmed,
        expertNotes = expertNotes
    )

    companion object {
        fun fromDomain(s: CropScanRecord, nowMs: Long = System.currentTimeMillis()): CropScanEntity = CropScanEntity(
            id = s.id,
            farmId = s.farmId,
            cropZoneId = s.cropZoneId,
            ownerId = s.ownerId,
            sharedExpertIdsCsv = s.sharedExpertIds.joinToString(","),
            cropType = s.cropType,
            imageUri = s.imageUri,
            identifiedCrop = s.identifiedCrop,
            possibleCondition = s.possibleCondition,
            visibleSymptomsJoined = s.visibleSymptoms.joinToString("||"),
            confidencePct = s.confidencePct,
            severity = s.severity,
            nextStepsJoined = s.recommendedNextSteps.joinToString("||"),
            uncertaintyNotes = s.uncertaintyNotes,
            expertConfirmed = s.expertConfirmed,
            expertNotes = s.expertNotes,
            cachedAtEpochMs = nowMs
        )
    }
}

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val cropZoneId: String,
    val ownerId: String,
    val sharedExpertIdsCsv: String,
    val alertType: String,
    val severity: String,
    val title: String,
    val explanation: String,
    val acknowledged: Boolean,
    val cachedAtEpochMs: Long
) {
    fun toDomain(): FarmAlert = FarmAlert(
        id = id,
        farmId = farmId,
        cropZoneId = cropZoneId,
        ownerId = ownerId,
        sharedExpertIds = sharedExpertIdsCsv.split(",").filter { it.isNotBlank() },
        alertType = alertType,
        severity = severity,
        title = title,
        explanation = explanation,
        acknowledged = acknowledged,
        createdAt = Timestamp(cachedAtEpochMs / 1000L, 0)
    )

    companion object {
        fun fromDomain(a: FarmAlert, nowMs: Long = System.currentTimeMillis()): AlertEntity = AlertEntity(
            id = a.id,
            farmId = a.farmId,
            cropZoneId = a.cropZoneId,
            ownerId = a.ownerId,
            sharedExpertIdsCsv = a.sharedExpertIds.joinToString(","),
            alertType = a.alertType,
            severity = a.severity,
            title = a.title,
            explanation = a.explanation,
            acknowledged = a.acknowledged,
            cachedAtEpochMs = a.createdAt?.toDate()?.time ?: nowMs
        )
    }
}

@Entity(tableName = "pending_sync_queue")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0L,
    val entityType: String,
    val documentId: String,
    val operation: String,
    val payloadJson: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
