package com.example.models

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRole.FARMER.name,
    val district: String = "",
    val phone: String = "",
    val preferredLanguage: String = AppLanguage.ENGLISH.code,
    val cropsGrown: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedRole(): UserRole = UserRole.fromRaw(role)
    fun resolvedLanguage(): AppLanguage = AppLanguage.fromCode(preferredLanguage)
}

@Keep
@IgnoreExtraProperties
data class ExpertDirectoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val specialty: String = "",
    val district: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Farm(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val areaAcres: Double = 1.0,
    val primaryCrop: String = CropType.PADDY.name,
    val sharedExpertIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCrop(): CropType = CropType.fromRaw(primaryCrop)
}

@Keep
@IgnoreExtraProperties
data class CropZone(
    @DocumentId val id: String = "",
    val farmId: String = "",
    val ownerId: String = "",
    val sharedExpertIds: List<String> = emptyList(),
    val name: String = "",
    val cropType: String = CropType.PADDY.name,
    val variety: String? = null,
    val plantingDateEpochMs: Long = 0L,
    val growthStage: String = GrowthStage.VEGETATIVE.name,
    val areaAcres: Double = 1.0,
    val minSoilMoisturePct: Double = 45.0,
    val maxSoilMoisturePct: Double = 75.0,
    val maxAirTempC: Double = 36.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCrop(): CropType = CropType.fromRaw(cropType)
    fun resolvedStage(): GrowthStage = GrowthStage.fromRaw(growthStage)
}

@Keep
@IgnoreExtraProperties
data class Esp32Device(
    @DocumentId val id: String = "",
    val farmId: String = "",
    val cropZoneId: String = "",
    val ownerId: String = "",
    val sharedExpertIds: List<String> = emptyList(),
    val label: String = "",
    val connectionStatus: String = DeviceConnectionStatus.UNCONNECTED.name,
    val lastSeenEpochMs: Long = 0L,
    val firmwareVersion: String = "v1.0.0",
    val credentialHint: String = "unprovisioned",
    val revoked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedStatus(): DeviceConnectionStatus = DeviceConnectionStatus.fromRaw(connectionStatus)
}

@Keep
@IgnoreExtraProperties
data class SensorReading(
    @DocumentId val id: String = "",
    val farmId: String = "",
    val cropZoneId: String = "",
    val deviceId: String = "",
    val ownerId: String = "",
    val sharedExpertIds: List<String> = emptyList(),
    val source: String = TelemetrySource.MANUAL_LOG.name,
    val soilMoisturePct: Double = 0.0,
    val soilTempC: Double = 0.0,
    val airTempC: Double = 0.0,
    val humidityPct: Double = 0.0,
    val lightLux: Double = 0.0,
    val rainfallMm: Double = 0.0,
    val waterLevelCm: Double? = null,
    val recordedEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun resolvedSource(): TelemetrySource = TelemetrySource.fromRaw(source)
}

@Keep
@IgnoreExtraProperties
data class CropScanRecord(
    @DocumentId val id: String = "",
    val farmId: String = "",
    val cropZoneId: String = "",
    val ownerId: String = "",
    val sharedExpertIds: List<String> = emptyList(),
    val cropType: String = CropType.PADDY.name,
    val imageUri: String = "",
    val identifiedCrop: String = "",
    val possibleCondition: String = "",
    val visibleSymptoms: List<String> = emptyList(),
    val confidencePct: Long = 0L,
    val severity: String = DiagnosisSeverity.UNKNOWN.name,
    val recommendedNextSteps: List<String> = emptyList(),
    val uncertaintyNotes: String = "",
    val expertConfirmed: Boolean = false,
    val expertNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCrop(): CropType = CropType.fromRaw(cropType)
    fun resolvedSeverity(): DiagnosisSeverity = DiagnosisSeverity.fromRaw(severity)
}

@Keep
@IgnoreExtraProperties
data class FarmAlert(
    @DocumentId val id: String = "",
    val farmId: String = "",
    val cropZoneId: String = "",
    val ownerId: String = "",
    val sharedExpertIds: List<String> = emptyList(),
    val alertType: String = AlertType.SOIL_MOISTURE.name,
    val severity: String = AlertSeverity.INFO.name,
    val title: String = "",
    val explanation: String = "",
    val acknowledged: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedType(): AlertType = AlertType.fromRaw(alertType)
    fun resolvedSeverity(): AlertSeverity = AlertSeverity.fromRaw(severity)
}

@Keep
@IgnoreExtraProperties
data class ConsultationMessage(
    @DocumentId val id: String = "",
    val farmId: String = "",
    val ownerId: String = "",
    val sharedExpertIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = UserRole.FARMER.name,
    val messageText: String = "",
    val scanRefId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)
