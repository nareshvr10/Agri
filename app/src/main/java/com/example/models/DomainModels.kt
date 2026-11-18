package com.example.models

data class SensorMetricSpec(
    val key: String,
    val labelEn: String,
    val labelTa: String,
    val unit: String,
    val minValid: Double,
    val maxValid: Double
)

data class IrrigationRecommendation(
    val zoneId: String,
    val zoneName: String,
    val cropType: CropType,
    val growthStage: GrowthStage,
    val actionSummaryEn: String,
    val actionSummaryTa: String,
    val irrigationNeeded: Boolean,
    val estimatedVolumeNoteEn: String,
    val estimatedVolumeNoteTa: String,
    val reasoningPoints: List<String>,
    val confidenceNote: String,
    val dataFreshnessNote: String,
    val limitationsNote: String,
    val sourceLabel: String
)

data class WeatherSnapshot(
    val available: Boolean,
    val providerName: String,
    val locationLabel: String,
    val temperatureC: Double? = null,
    val humidityPct: Double? = null,
    val rainfallMm: Double? = null,
    val windSpeedKmh: Double? = null,
    val fetchedAtEpochMs: Long = 0L,
    val unavailableReason: String? = null
)

data class EnvironmentalRiskItem(
    val category: RiskCategory,
    val level: RiskLevel,
    val titleEn: String,
    val titleTa: String,
    val explanation: String
)

data class WeatherRiskAssessment(
    val weather: WeatherSnapshot,
    val risks: List<EnvironmentalRiskItem>
)

data class BackendSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val backendMode: BackendMode = BackendMode.FIREBASE_CLOUD,
    val fastApiBaseUrl: String = "",
    val weatherProviderMode: WeatherProviderMode = WeatherProviderMode.OPEN_METEO_LIVE,
    val customWeatherUrl: String = "",
    val onboardingCompleted: Boolean = false,
    val lastSyncedEpochMs: Long = 0L,
    val offlineModeBannerDismissed: Boolean = false,
    val selectedFarmId: String = ""
)

data class AiScanAnalysisResult(
    val identifiedCrop: String,
    val possibleCondition: String,
    val visibleSymptoms: List<String>,
    val confidencePct: Int,
    val severity: DiagnosisSeverity,
    val recommendedNextSteps: List<String>,
    val uncertaintyNotes: String,
    val isServiceAvailable: Boolean = true
)

data class CropGuideSpec(
    val cropType: CropType,
    val scientificName: String,
    val defaultMinMoisturePct: Double,
    val defaultMaxMoisturePct: Double,
    val defaultMaxAirTempC: Double,
    val typicalVarieties: List<String>,
    val stageSummaryEn: String,
    val stageSummaryTa: String
)
