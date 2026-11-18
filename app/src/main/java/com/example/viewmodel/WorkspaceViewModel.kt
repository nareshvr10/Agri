package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlertRepository
import com.example.data.ConsultationRepository
import com.example.data.FarmRepository
import com.example.data.ScanRepository
import com.example.data.SensorRepository
import com.example.data.UserAndAdminRepository
import com.example.data.WeatherRepository
import com.example.domain.EnvironmentalRiskEvaluator
import com.example.domain.IrrigationAdvisorEngine
import com.example.domain.SensorReadingValidator
import com.example.models.AlertSeverity
import com.example.models.AlertType
import com.example.models.AppLanguage
import com.example.models.BackendSettings
import com.example.models.ConsultationMessage
import com.example.models.CropScanRecord
import com.example.models.CropType
import com.example.models.CropZone
import com.example.models.Esp32Device
import com.example.models.ExpertDirectoryEntry
import com.example.models.Farm
import com.example.models.FarmAlert
import com.example.models.GrowthStage
import com.example.models.IrrigationRecommendation
import com.example.models.SensorReading
import com.example.models.UiState
import com.example.models.UserProfile
import com.example.models.UserRole
import com.example.models.WeatherRiskAssessment
import com.example.models.WeatherSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModel(
    private val currentUserId: String,
    private val authEmail: String,
    private val authDisplayName: String,
    private val farmRepository: FarmRepository,
    private val sensorRepository: SensorRepository,
    private val alertRepository: AlertRepository,
    private val scanRepository: ScanRepository,
    private val userAndAdminRepository: UserAndAdminRepository,
    private val consultationRepository: ConsultationRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        userAndAdminRepository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = unwrapFirestoreError(error)
                if (fsErr == null && error is CancellationException) throw error
                emit(UiState.Error((fsErr ?: error).message ?: "Failed to load user profile"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val activeRoleFlow: Flow<UserRole?> = userProfileState.map { state ->
        (state as? UiState.Success)?.data?.resolvedRole()
    }

    val farmsState: StateFlow<UiState<List<Farm>>> = activeRoleFlow
        .flatMapLatest { role ->
            if (role == null) flowOf(UiState.Success(emptyList()))
            else combine(
                farmRepository.observeFarms(currentUserId, role).map<List<Farm>, Result<List<Farm>>> { Result.success(it) }
                    .catch { err ->
                        val fsErr = unwrapFirestoreError(err)
                        if (fsErr == null && err is CancellationException) throw err
                        emit(Result.failure(fsErr ?: err))
                    },
                farmRepository.observeCachedFarms()
            ) { remote, cached ->
                remote.fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = { err ->
                        if (cached.isNotEmpty()) UiState.Success(cached)
                        else UiState.Error(err.message ?: "Unable to load farms")
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val zonesState: StateFlow<List<CropZone>> = activeRoleFlow
        .flatMapLatest { role ->
            if (role == null) flowOf(emptyList())
            else combine(
                farmRepository.observeCropZones(currentUserId, role).catch { err ->
                    val fsErr = unwrapFirestoreError(err)
                    if (fsErr == null && err is CancellationException) throw err
                    Log.w(TAG, "Zones remote stream fallback to cache", fsErr ?: err)
                },
                farmRepository.observeCachedZones()
            ) { remote, cached -> remote.ifEmpty { cached } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val devicesState: StateFlow<List<Esp32Device>> = activeRoleFlow
        .flatMapLatest { role ->
            if (role == null) flowOf(emptyList())
            else combine(
                sensorRepository.observeDevices(currentUserId, role).catch { err ->
                    val fsErr = unwrapFirestoreError(err)
                    if (fsErr == null && err is CancellationException) throw err
                },
                sensorRepository.observeCachedDevices()
            ) { remote, cached -> remote.ifEmpty { cached } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val readingsState: StateFlow<List<SensorReading>> = activeRoleFlow
        .flatMapLatest { role ->
            if (role == null) flowOf(emptyList())
            else combine(
                sensorRepository.observeReadings(currentUserId, role).catch { err ->
                    val fsErr = unwrapFirestoreError(err)
                    if (fsErr == null && err is CancellationException) throw err
                },
                sensorRepository.observeCachedReadings()
            ) { remote, cached -> remote.ifEmpty { cached } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val alertsState: StateFlow<List<FarmAlert>> = activeRoleFlow
        .flatMapLatest { role ->
            if (role == null) flowOf(emptyList())
            else combine(
                alertRepository.observeAlerts(currentUserId, role).catch { err ->
                    val fsErr = unwrapFirestoreError(err)
                    if (fsErr == null && err is CancellationException) throw err
                },
                alertRepository.observeCachedAlerts()
            ) { remote, cached -> remote.ifEmpty { cached } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val scansState: StateFlow<List<CropScanRecord>> = activeRoleFlow
        .flatMapLatest { role ->
            if (role == null) flowOf(emptyList())
            else combine(
                scanRepository.observeScans(currentUserId, role).catch { err ->
                    val fsErr = unwrapFirestoreError(err)
                    if (fsErr == null && err is CancellationException) throw err
                },
                scanRepository.observeCachedScans()
            ) { remote, cached -> remote.ifEmpty { cached } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val expertsState: StateFlow<List<ExpertDirectoryEntry>> =
        userAndAdminRepository.observeExpertDirectory()
            .catch { err ->
                val fsErr = unwrapFirestoreError(err)
                if (fsErr == null && err is CancellationException) throw err
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminUsersState: StateFlow<List<UserProfile>> = activeRoleFlow
        .flatMapLatest { role ->
            if (role == UserRole.ADMIN) {
                userAndAdminRepository.observeAllUsersForAdmin().catch { err ->
                    val fsErr = unwrapFirestoreError(err)
                    if (fsErr == null && err is CancellationException) throw err
                    emit(emptyList())
                }
            } else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _weatherSnapshot = MutableStateFlow(
        WeatherSnapshot(
            available = false,
            providerName = "Not Loaded",
            locationLabel = "-",
            unavailableReason = "Select a farm and tap Refresh to query configured weather provider."
        )
    )
    val weatherSnapshot: StateFlow<WeatherSnapshot> = _weatherSnapshot.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun registerFarmerProfile(
        displayName: String,
        district: String,
        phone: String,
        language: AppLanguage,
        cropsGrown: List<CropType>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val isNew = (userProfileState.value as? UiState.Success)?.data == null
            val profile = UserProfile(
                userId = currentUserId,
                displayName = displayName.ifBlank { authDisplayName.ifBlank { "Farmer" } },
                email = authEmail,
                role = UserRole.FARMER.name,
                district = district,
                phone = phone,
                preferredLanguage = language.code,
                cropsGrown = cropsGrown.map { it.name }
            )
            userAndAdminRepository.upsertFarmerProfile(profile, isNew = isNew)
                .onSuccess {
                    _actionMessage.value = "Profile saved."
                    onSuccess()
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to save profile." }
        }
    }

    fun createFarm(
        name: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        areaAcres: Double,
        primaryCrop: CropType,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val farm = Farm(
                ownerId = currentUserId,
                name = name,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                areaAcres = areaAcres,
                primaryCrop = primaryCrop.name
            )
            farmRepository.createFarm(farm)
                .onSuccess { id ->
                    _actionMessage.value = "Farm created."
                    onCreated(id)
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to create farm." }
        }
    }

    fun shareFarmWithExpert(farm: Farm, expertUid: String) {
        viewModelScope.launch {
            farmRepository.shareFarmWithExpert(farm.id, expertUid, farm.sharedExpertIds)
                .onSuccess { _actionMessage.value = "Farm shared with expert." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to share farm." }
        }
    }

    fun createCropZone(
        farm: Farm,
        name: String,
        cropType: CropType,
        variety: String?,
        plantingDateEpochMs: Long,
        growthStage: GrowthStage,
        areaAcres: Double,
        minMoisture: Double,
        maxMoisture: Double,
        maxAirTemp: Double,
        onCreated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val zone = CropZone(
                farmId = farm.id,
                ownerId = currentUserId,
                sharedExpertIds = farm.sharedExpertIds,
                name = name,
                cropType = cropType.name,
                variety = variety,
                plantingDateEpochMs = plantingDateEpochMs,
                growthStage = growthStage.name,
                areaAcres = areaAcres,
                minSoilMoisturePct = minMoisture,
                maxSoilMoisturePct = maxMoisture,
                maxAirTempC = maxAirTemp
            )
            farmRepository.createCropZone(zone)
                .onSuccess {
                    _actionMessage.value = "Crop zone added."
                    onCreated()
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to add crop zone." }
        }
    }

    fun updateZoneThresholds(
        zoneId: String,
        minMoisture: Double,
        maxMoisture: Double,
        maxAirTemp: Double,
        isOnline: Boolean
    ) {
        viewModelScope.launch {
            farmRepository.updateZoneThresholds(zoneId, minMoisture, maxMoisture, maxAirTemp, isOnline)
                .onSuccess {
                    _actionMessage.value = if (isOnline) "Thresholds updated." else "Queued offline — will sync when online."
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Update failed." }
        }
    }

    fun registerEsp32Device(
        farm: Farm,
        zone: CropZone,
        deviceId: String,
        label: String,
        firmwareVersion: String
    ) {
        viewModelScope.launch {
            val device = Esp32Device(
                id = deviceId.trim(),
                farmId = farm.id,
                cropZoneId = zone.id,
                ownerId = currentUserId,
                sharedExpertIds = farm.sharedExpertIds,
                label = label,
                firmwareVersion = firmwareVersion,
                credentialHint = "dev-${deviceId.trim().takeLast(4)}"
            )
            sensorRepository.registerEsp32Device(device)
                .onSuccess { _actionMessage.value = "ESP32 node registered (awaiting backend telemetry)." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Device registration failed." }
        }
    }

    fun revokeEsp32Device(deviceId: String) {
        viewModelScope.launch {
            sensorRepository.revokeDevice(deviceId)
                .onSuccess { _actionMessage.value = "Device credentials revoked." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Revocation failed." }
        }
    }

    fun recordManualReading(
        farm: Farm,
        zone: CropZone,
        soilMoisturePct: Double,
        soilTempC: Double,
        airTempC: Double,
        humidityPct: Double,
        lightLux: Double,
        rainfallMm: Double,
        waterLevelCm: Double?
    ) {
        viewModelScope.launch {
            val reading = SensorReading(
                farmId = farm.id,
                cropZoneId = zone.id,
                deviceId = "manual_field_entry",
                ownerId = currentUserId,
                sharedExpertIds = farm.sharedExpertIds,
                soilMoisturePct = soilMoisturePct,
                soilTempC = soilTempC,
                airTempC = airTempC,
                humidityPct = humidityPct,
                lightLux = lightLux,
                rainfallMm = rainfallMm,
                waterLevelCm = waterLevelCm,
                recordedEpochMs = System.currentTimeMillis()
            )
            val validation = SensorReadingValidator.validate(reading)
            if (!validation.valid) {
                _actionMessage.value = validation.errors.firstOrNull() ?: "Invalid sensor reading."
                return@launch
            }
            sensorRepository.recordManualReading(reading)
                .onSuccess {
                    _actionMessage.value = "Manual field log recorded."
                    checkAndRaiseThresholdAlerts(farm, zone, reading, validation.warnings)
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to record reading." }
        }
    }

    private suspend fun checkAndRaiseThresholdAlerts(
        farm: Farm,
        zone: CropZone,
        reading: SensorReading,
        warnings: List<String>
    ) {
        if (reading.soilMoisturePct < zone.minSoilMoisturePct) {
            alertRepository.createAlert(
                FarmAlert(
                    farmId = farm.id,
                    cropZoneId = zone.id,
                    ownerId = currentUserId,
                    sharedExpertIds = farm.sharedExpertIds,
                    alertType = AlertType.SOIL_MOISTURE.name,
                    severity = AlertSeverity.WARNING.name,
                    title = "Low soil moisture in ${zone.name}",
                    explanation = "Moisture ${reading.soilMoisturePct}% is below configured minimum ${zone.minSoilMoisturePct}%."
                )
            )
        }
        if (reading.airTempC > zone.maxAirTempC) {
            alertRepository.createAlert(
                FarmAlert(
                    farmId = farm.id,
                    cropZoneId = zone.id,
                    ownerId = currentUserId,
                    sharedExpertIds = farm.sharedExpertIds,
                    alertType = AlertType.HIGH_TEMP.name,
                    severity = AlertSeverity.CRITICAL.name,
                    title = "High temperature alert in ${zone.name}",
                    explanation = "Air temperature ${reading.airTempC}°C exceeded limit ${zone.maxAirTempC}°C."
                )
            )
        }
        if (reading.rainfallMm >= 30.0) {
            alertRepository.createAlert(
                FarmAlert(
                    farmId = farm.id,
                    cropZoneId = zone.id,
                    ownerId = currentUserId,
                    sharedExpertIds = farm.sharedExpertIds,
                    alertType = AlertType.HEAVY_RAINFALL.name,
                    severity = AlertSeverity.WARNING.name,
                    title = "Heavy rainfall recorded in ${zone.name}",
                    explanation = "Rainfall ${reading.rainfallMm} mm recorded. Inspect field drainage."
                )
            )
        }
        if (warnings.isNotEmpty()) {
            alertRepository.createAlert(
                FarmAlert(
                    farmId = farm.id,
                    cropZoneId = zone.id,
                    ownerId = currentUserId,
                    sharedExpertIds = farm.sharedExpertIds,
                    alertType = AlertType.UNUSUAL_READING.name,
                    severity = AlertSeverity.INFO.name,
                    title = "Unusual sensor reading in ${zone.name}",
                    explanation = warnings.joinToString(" ")
                )
            )
        }
    }

    fun acknowledgeAlert(alertId: String, isOnline: Boolean) {
        viewModelScope.launch {
            alertRepository.acknowledgeAlert(alertId, isOnline)
                .onSuccess {
                    _actionMessage.value = if (isOnline) "Alert acknowledged." else "Queued acknowledgement for sync."
                }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to acknowledge alert." }
        }
    }

    fun refreshWeather(farm: Farm?, settings: BackendSettings) {
        viewModelScope.launch {
            if (farm == null) {
                _weatherSnapshot.value = WeatherSnapshot(
                    available = false,
                    providerName = settings.weatherProviderMode.name,
                    locationLabel = "No farm selected",
                    unavailableReason = "Add or select a farm with coordinates to fetch weather."
                )
                return@launch
            }
            _weatherSnapshot.value = weatherRepository.fetchWeather(
                latitude = farm.latitude,
                longitude = farm.longitude,
                locationLabel = farm.locationName,
                settings = settings
            )
        }
    }

    fun computeIrrigationForZone(zone: CropZone): IrrigationRecommendation {
        val zoneReadings = readingsState.value.filter { it.cropZoneId == zone.id }
        return IrrigationAdvisorEngine.recommend(zone, zoneReadings, _weatherSnapshot.value)
    }

    fun evaluateWeatherRisk(farm: Farm?): WeatherRiskAssessment {
        val crop = farm?.resolvedCrop() ?: CropType.PADDY
        val latestReading = farm?.let { f ->
            readingsState.value.filter { it.farmId == f.id }.maxByOrNull { it.recordedEpochMs }
        }
        return EnvironmentalRiskEvaluator.evaluate(crop, _weatherSnapshot.value, latestReading)
    }

    fun verifyScanAsExpert(scanId: String, confirmed: Boolean, notes: String) {
        viewModelScope.launch {
            scanRepository.verifyScanAsExpert(scanId, confirmed, notes)
                .onSuccess { _actionMessage.value = "Expert verification saved." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to verify scan." }
        }
    }

    fun adminUpdateUserRole(targetUser: UserProfile, newRole: UserRole) {
        viewModelScope.launch {
            userAndAdminRepository.updateUserRoleAsAdmin(targetUser.userId, newRole, targetUser)
                .onSuccess { _actionMessage.value = "Updated ${targetUser.displayName} role to ${newRole.name}." }
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Role update failed." }
        }
    }

    fun observeConsultation(farmId: String, role: UserRole): Flow<List<ConsultationMessage>> =
        if (farmId.isBlank()) flowOf(emptyList())
        else consultationRepository.observeMessagesForFarm(farmId, currentUserId, role).catch { err ->
            val fsErr = unwrapFirestoreError(err)
            if (fsErr == null && err is CancellationException) throw err
            emit(emptyList())
        }

    fun sendConsultationMessage(farm: Farm, text: String, scanRefId: String? = null) {
        viewModelScope.launch {
            val profile = (userProfileState.value as? UiState.Success)?.data ?: return@launch
            val msg = ConsultationMessage(
                farmId = farm.id,
                ownerId = farm.ownerId,
                sharedExpertIds = farm.sharedExpertIds,
                senderId = currentUserId,
                senderName = profile.displayName.ifBlank { "Member" },
                senderRole = profile.role,
                messageText = text,
                scanRefId = scanRefId
            )
            consultationRepository.sendMessage(msg)
                .onFailure { _actionMessage.value = it.localizedMessage ?: "Failed to send message." }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "WorkspaceVM"
    }
}
