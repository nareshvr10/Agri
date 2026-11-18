package com.example.domain

import com.example.models.CropType
import com.example.models.EnvironmentalRiskItem
import com.example.models.RiskCategory
import com.example.models.RiskLevel
import com.example.models.SensorReading
import com.example.models.WeatherRiskAssessment
import com.example.models.WeatherSnapshot

object EnvironmentalRiskEvaluator {

    fun evaluate(
        cropType: CropType,
        weather: WeatherSnapshot,
        latestReading: SensorReading?
    ): WeatherRiskAssessment {
        val temp = weather.takeIf { it.available }?.temperatureC ?: latestReading?.airTempC
        val humidity = weather.takeIf { it.available }?.humidityPct ?: latestReading?.humidityPct
        val rain = weather.takeIf { it.available }?.rainfallMm ?: latestReading?.rainfallMm
        val soilMoisture = latestReading?.soilMoisturePct

        if (temp == null && humidity == null && rain == null && soilMoisture == null) {
            return WeatherRiskAssessment(
                weather = weather,
                risks = listOf(
                    RiskCategory.DROUGHT,
                    RiskCategory.WATERLOGGING_FLOOD,
                    RiskCategory.HEAT_STRESS,
                    RiskCategory.DISEASE_FAVORING
                ).map { category ->
                    EnvironmentalRiskItem(
                        category = category,
                        level = RiskLevel.UNAVAILABLE,
                        titleEn = defaultTitleEn(category),
                        titleTa = defaultTitleTa(category),
                        explanation = "Live weather provider and active sensor readings are unavailable. Connect weather or ESP32 sensors to evaluate risk."
                    )
                }
            )
        }

        val droughtLevel = when {
            soilMoisture != null && soilMoisture < 25.0 && (rain ?: 0.0) < 1.0 -> RiskLevel.HIGH
            (soilMoisture != null && soilMoisture < 38.0) || ((humidity ?: 60.0) < 35.0 && (temp ?: 28.0) > 35.0) -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        val floodLevel = when {
            (rain ?: 0.0) >= 40.0 || (soilMoisture != null && soilMoisture >= 92.0 && cropType != CropType.PADDY) -> RiskLevel.HIGH
            (rain ?: 0.0) >= 15.0 || (soilMoisture != null && soilMoisture >= 85.0 && cropType != CropType.PADDY) -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        val heatThreshold = CropGuideCatalog.specFor(cropType).defaultMaxAirTempC
        val heatLevel = when {
            (temp ?: 0.0) >= heatThreshold + 3.0 -> RiskLevel.HIGH
            (temp ?: 0.0) >= heatThreshold -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        val diseaseLevel = when {
            (humidity ?: 0.0) >= 85.0 && (temp ?: 0.0) in 20.0..32.0 -> RiskLevel.HIGH
            (humidity ?: 0.0) >= 75.0 && (rain ?: 0.0) > 2.0 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        val items = listOf(
            EnvironmentalRiskItem(
                category = RiskCategory.DROUGHT,
                level = droughtLevel,
                titleEn = defaultTitleEn(RiskCategory.DROUGHT),
                titleTa = defaultTitleTa(RiskCategory.DROUGHT),
                explanation = "Evaluated from soil moisture (${soilMoisture?.let { "${"%.0f".format(it)}%" } ?: "N/A"}) and rainfall (${rain?.let { "${"%.1f".format(it)} mm" } ?: "N/A"})."
            ),
            EnvironmentalRiskItem(
                category = RiskCategory.WATERLOGGING_FLOOD,
                level = floodLevel,
                titleEn = defaultTitleEn(RiskCategory.WATERLOGGING_FLOOD),
                titleTa = defaultTitleTa(RiskCategory.WATERLOGGING_FLOOD),
                explanation = "Evaluated from rainfall (${rain?.let { "${"%.1f".format(it)} mm" } ?: "N/A"}) and crop tolerance (${cropType.labelEn})."
            ),
            EnvironmentalRiskItem(
                category = RiskCategory.HEAT_STRESS,
                level = heatLevel,
                titleEn = defaultTitleEn(RiskCategory.HEAT_STRESS),
                titleTa = defaultTitleTa(RiskCategory.HEAT_STRESS),
                explanation = "Current air temperature (${temp?.let { "${"%.1f".format(it)}°C" } ?: "N/A"}) vs ${cropType.labelEn} threshold (${heatThreshold.toInt()}°C)."
            ),
            EnvironmentalRiskItem(
                category = RiskCategory.DISEASE_FAVORING,
                level = diseaseLevel,
                titleEn = defaultTitleEn(RiskCategory.DISEASE_FAVORING),
                titleTa = defaultTitleTa(RiskCategory.DISEASE_FAVORING),
                explanation = "High humidity (${humidity?.let { "${"%.0f".format(it)}%" } ?: "N/A"}) combined with warm temperatures can favour foliar fungal/bacterial pressure."
            )
        )
        return WeatherRiskAssessment(weather = weather, risks = items)
    }

    private fun defaultTitleEn(category: RiskCategory): String = when (category) {
        RiskCategory.DROUGHT -> "Drought Risk"
        RiskCategory.WATERLOGGING_FLOOD -> "Waterlogging / Flood Risk"
        RiskCategory.HEAT_STRESS -> "Heat Stress Risk"
        RiskCategory.DISEASE_FAVORING -> "Disease-Favouring Conditions"
    }

    private fun defaultTitleTa(category: RiskCategory): String = when (category) {
        RiskCategory.DROUGHT -> "வறட்சி அபாயம்"
        RiskCategory.WATERLOGGING_FLOOD -> "நீர் தேங்குதல் / வெள்ள அபாயம்"
        RiskCategory.HEAT_STRESS -> "அதிக வெப்ப அழுத்தம்"
        RiskCategory.DISEASE_FAVORING -> "நோய் பரவும் சூழல்"
    }
}
