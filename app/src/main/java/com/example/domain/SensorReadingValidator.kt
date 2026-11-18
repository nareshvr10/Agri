package com.example.domain

import com.example.models.SensorReading

data class SensorValidationResult(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

object SensorReadingValidator {

    fun validate(
        reading: SensorReading,
        nowEpochMs: Long = System.currentTimeMillis()
    ): SensorValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (reading.farmId.isBlank()) errors += "Farm ID is required."
        if (reading.cropZoneId.isBlank()) errors += "Crop zone ID is required."
        if (reading.deviceId.isBlank()) errors += "Device ID is required."

        if (reading.recordedEpochMs <= 0L) {
            errors += "Invalid reading timestamp."
        } else if (reading.recordedEpochMs > nowEpochMs + 5 * 60_000L) {
            errors += "Timestamp cannot be more than 5 minutes in the future."
        }

        if (reading.soilMoisturePct !in 0.0..100.0) {
            errors += "Soil moisture must be between 0% and 100%."
        }
        if (reading.soilTempC !in -10.0..70.0) {
            errors += "Soil temperature must be between -10°C and 70°C."
        }
        if (reading.airTempC !in -10.0..65.0) {
            errors += "Air temperature must be between -10°C and 65°C."
        }
        if (reading.humidityPct !in 0.0..100.0) {
            errors += "Humidity must be between 0% and 100%."
        }
        if (reading.lightLux !in 0.0..200_000.0) {
            errors += "Light intensity must be between 0 and 200,000 lux."
        }
        if (reading.rainfallMm !in 0.0..1000.0) {
            errors += "Rainfall must be between 0 and 1000 mm."
        }
        reading.waterLevelCm?.let { level ->
            if (level !in 0.0..500.0) {
                errors += "Water level must be between 0 and 500 cm."
            }
        }

        // Cross-sensor plausibility warnings for unusual readings
        if (reading.airTempC > 45.0 || reading.soilTempC > 45.0) {
            warnings += "Extreme thermal reading detected; verify sensor shading and calibration."
        }
        if (reading.humidityPct < 15.0 && reading.rainfallMm > 5.0) {
            warnings += "Unusual combination: low humidity reported alongside rainfall."
        }
        if (reading.soilMoisturePct == 0.0 || reading.soilMoisturePct == 100.0) {
            warnings += "Soil moisture at extreme rail (${reading.soilMoisturePct}%); check probe contact."
        }

        return SensorValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
