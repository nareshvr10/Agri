package com.example.models

enum class CropType(val labelEn: String, val labelTa: String) {
    PADDY("Paddy (Rice)", "நெல்"),
    BANANA("Banana", "வாழை"),
    TOMATO("Tomato", "தக்காளி");

    companion object {
        fun fromRaw(raw: String?): CropType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PADDY
    }
}

enum class GrowthStage(val labelEn: String, val labelTa: String) {
    SEEDLING("Seedling / Nursery", "நாற்றுப் பருவம்"),
    VEGETATIVE("Vegetative", "வளர்ச்சிப் பருவம்"),
    FLOWERING("Flowering / Panicle", "பூக்கும் பருவம்"),
    FRUITING_MATURITY("Fruiting / Grain Fill", "காய் / கதிர் பிடிப்பு"),
    HARVEST_READY("Harvest Ready", "அறுவடைக்குத் தயார்");

    companion object {
        fun fromRaw(raw: String?): GrowthStage =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: VEGETATIVE
    }
}

enum class UserRole(val labelEn: String, val labelTa: String) {
    FARMER("Farmer", "விவசாயி"),
    EXPERT("Agricultural Expert", "வேளாண் நிபுணர்"),
    ADMIN("Administrator", "நிர்வாகி");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: FARMER
    }
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    TAMIL("ta", "தமிழ்");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

enum class TelemetrySource(val labelEn: String, val labelTa: String) {
    ESP32_VERIFIED("Live ESP32", "நேரடி ESP32"),
    MANUAL_LOG("Manual Field Log", "கைமுறை பதிவு"),
    UNCONNECTED("Device not connected", "சாதனம் இணைக்கப்படவில்லை");

    companion object {
        fun fromRaw(raw: String?): TelemetrySource =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MANUAL_LOG
    }
}

enum class DeviceConnectionStatus {
    UNCONNECTED,
    ONLINE,
    OFFLINE,
    REVOKED;

    companion object {
        fun fromRaw(raw: String?): DeviceConnectionStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNCONNECTED
    }
}

enum class AlertType {
    SOIL_MOISTURE,
    HIGH_TEMP,
    HEAVY_RAINFALL,
    SENSOR_OFFLINE,
    UNUSUAL_READING,
    CROP_HEALTH,
    IRRIGATION_ADVISORY;

    companion object {
        fun fromRaw(raw: String?): AlertType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SOIL_MOISTURE
    }
}

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL;

    companion object {
        fun fromRaw(raw: String?): AlertSeverity =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: INFO
    }
}

enum class DiagnosisSeverity {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL,
    UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): DiagnosisSeverity =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class BackendMode {
    FIREBASE_CLOUD,
    CUSTOM_FASTAPI
}

enum class WeatherProviderMode {
    DISABLED,
    OPEN_METEO_LIVE,
    CUSTOM_PROXY
}

enum class RiskCategory {
    DROUGHT,
    WATERLOGGING_FLOOD,
    HEAT_STRESS,
    DISEASE_FAVORING
}

enum class RiskLevel {
    LOW,
    MODERATE,
    HIGH,
    UNAVAILABLE
}
