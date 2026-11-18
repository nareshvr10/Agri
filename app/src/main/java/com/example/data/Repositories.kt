package com.example.data

import android.graphics.Bitmap
import com.example.models.AiScanAnalysisResult
import com.example.models.BackendSettings
import com.example.models.ConsultationMessage
import com.example.models.CropScanRecord
import com.example.models.CropType
import com.example.models.CropZone
import com.example.models.Esp32Device
import com.example.models.ExpertDirectoryEntry
import com.example.models.Farm
import com.example.models.FarmAlert
import com.example.models.SensorReading
import com.example.models.UserProfile
import com.example.models.UserRole
import com.example.models.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

interface FarmRepository {
    fun observeFarms(userId: String, role: UserRole): Flow<List<Farm>>
    fun observeCachedFarms(): Flow<List<Farm>>
    fun observeCropZones(userId: String, role: UserRole): Flow<List<CropZone>>
    fun observeCachedZones(): Flow<List<CropZone>>
    suspend fun createFarm(farm: Farm): Result<String>
    suspend fun shareFarmWithExpert(farmId: String, expertUid: String, currentShared: List<String>): Result<Unit>
    suspend fun createCropZone(zone: CropZone): Result<String>
    suspend fun updateZoneThresholds(
        zoneId: String,
        minMoisture: Double,
        maxMoisture: Double,
        maxAirTemp: Double,
        isOnline: Boolean
    ): Result<Unit>
}

interface SensorRepository {
    fun observeDevices(userId: String, role: UserRole): Flow<List<Esp32Device>>
    fun observeCachedDevices(): Flow<List<Esp32Device>>
    fun observeReadings(userId: String, role: UserRole): Flow<List<SensorReading>>
    fun observeCachedReadings(): Flow<List<SensorReading>>
    suspend fun registerEsp32Device(device: Esp32Device): Result<String>
    suspend fun revokeDevice(deviceId: String): Result<Unit>
    suspend fun recordManualReading(reading: SensorReading): Result<String>
}

interface AlertRepository {
    fun observeAlerts(userId: String, role: UserRole): Flow<List<FarmAlert>>
    fun observeCachedAlerts(): Flow<List<FarmAlert>>
    suspend fun createAlert(alert: FarmAlert): Result<String>
    suspend fun acknowledgeAlert(alertId: String, isOnline: Boolean): Result<Unit>
}

interface ScanRepository {
    fun observeScans(userId: String, role: UserRole): Flow<List<CropScanRecord>>
    fun observeCachedScans(): Flow<List<CropScanRecord>>
    suspend fun saveScan(scan: CropScanRecord): Result<String>
    suspend fun verifyScanAsExpert(scanId: String, confirmed: Boolean, notes: String): Result<Unit>
}

interface AiDiagnosisRepository {
    suspend fun analyzeCropImage(
        cropType: CropType,
        bitmap: Bitmap,
        settings: BackendSettings
    ): Result<AiScanAnalysisResult>
}

interface WeatherRepository {
    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        locationLabel: String,
        settings: BackendSettings
    ): WeatherSnapshot
}

interface UserAndAdminRepository {
    fun observeUserProfile(userId: String): Flow<UserProfile?>
    fun observeExpertDirectory(): Flow<List<ExpertDirectoryEntry>>
    fun observeAllUsersForAdmin(): Flow<List<UserProfile>>
    suspend fun upsertFarmerProfile(profile: UserProfile, isNew: Boolean): Result<Unit>
    suspend fun updateUserRoleAsAdmin(targetUserId: String, newRole: UserRole, userProfile: UserProfile?): Result<Unit>
}

interface ConsultationRepository {
    fun observeMessagesForFarm(farmId: String, userId: String, role: UserRole): Flow<List<ConsultationMessage>>
    suspend fun sendMessage(message: ConsultationMessage): Result<Unit>
}
